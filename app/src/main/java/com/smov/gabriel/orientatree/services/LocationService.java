package com.smov.gabriel.orientatree.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconLOD;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.utils.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LocationService extends Service {

    // allows to know from the activity whether the service is being executed or no
    public static boolean executing = false;

    private boolean initialDataSet = false; // flag to signal if we already have al the initial data needed to play
    private boolean uploadingReach = false; // flag to signal if we are trying to upload a reach and therefore the others must wait

    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "Location Service";

    private static final float LOCATION_PRECISION = 25f;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private FirebaseFirestore db;

    private FirebaseAuth mAuth;

    private String userID;

    private ActivityLOD activity;
    // NEW
    private Template template;
    private ArrayList<BeaconLOD> beacons; // all the beacons
    private Set<String> beaconsReached; // set containing the ids of the beacons already reached
    private String score;
    private String trackIRI = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executing = true;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startMyOwnForeground();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        requestLocationUpdates();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // get the current user id
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        // get firestore instance
        db = FirebaseFirestore.getInstance();

        // get the activity on which the user is taking part and its beacons
        if (intent != null) {
            // (this step is needed because onStart is executed twice)
            // NEW
            Template templateTemp = (Template) intent.getSerializableExtra("template");
            if (templateTemp != null) {
                template = templateTemp;
            }
            ActivityLOD activityTemp = (ActivityLOD) intent.getSerializableExtra("activity");
            if (activityTemp != null) {
                activity = activityTemp; // here we have the activity
                score = "linealPartOf";
                if (activity.isScore()) {
                    score = "scorePartof";
                }
                beacons = new ArrayList<>();
                beaconsReached = new HashSet<>();

                /*
                 * SELECT DISTINCT ?lat ?long ?name ?number ?idBeacon WHERE{
                 *   ?beacon
                 *       rdf:ID ?idBeacon;
                 *       ot:order ?number;
                 *       ot:score(linearPartOf/scorePartof) ?activity;
                 *       ot:ubicatedIn ?point.
                 *   ?point
                 *       geo:lat ?lat;
                 *       geo:long ?long.
                 *   ?activity
                 *       rdf:ID activity.getID().
                 * }
                 *
                 * */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+?lat+?long+?name+?number+%3FidBeacon+WHERE+%7B%0D%0A%3Fbeacon%0D%0A++rdf%3AID+%3FidBeacon%3B%0D%0A+ot:order+?number;+rdfs:label+?name;+ot%3A" + score + "+%3Factivity;+ot:ubicatedIn+?point.+?point+geo:lat+?lat;+geo:long+?long.+%0D%0A%3Factivity+%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%7D&format=json";


                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                    for (int i = 0; i < result.length(); i++) {
                                        JSONObject aux = result.getJSONObject(i);
                                        String idBeacon = aux.getJSONObject("idBeacon").getString("value");
                                        Double latitude = aux.getJSONObject("lat").getDouble("value");
                                        Double longitude = aux.getJSONObject("long").getDouble("value");
                                        String name = aux.getJSONObject("name").getString("value");
                                        int number = aux.getJSONObject("number").getInt("value");
                                        BeaconLOD beacon = new BeaconLOD();
                                        beacon.setName(name);
                                        beacon.setNumber(number);
                                        beacon.setBeacon_id(idBeacon);
                                        beacon.setLocation(new GeoPoint(latitude, longitude));
                                        beacons.add(beacon);
                                    }
                                    activity.setBeaconSize(beacons.size());
                                    Collections.sort(beacons, new BeaconLOD());
                                    // DEBUG
                                    Log.d(TAG, "Iniciando el servicio y obteniendo los datos. La actividad tiene " + beacons.size() + " balizas:\n");
                                    for (BeaconLOD beacon : beacons) {
                                        Log.d(TAG, beacon.getBeacon_id() + "\n");
                                    }
                                    

                                    /*
                                     * SELECT DISTINCT ?idBeacon WHERE{
                                     *   ?beacon
                                     *       rdf:ID ?idBeacon;
                                     *       ot:score(linearPartOf/scorePartof) ?activity.
                                     *   ?reached
                                     *       ot:of ?person;
                                     *       ot:toThe ?beacon.
                                     *   ?activity
                                     *       rdf:ID activity.getId().
                                     *   ?person
                                     *       ot:userName userID.
                                     * }
                                     * */


                                    String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FidBeacon+WHERE+%7B%0D%0A%3Fbeacon%0D%0A++rdf%3AID+%3FidBeacon%3B%0D%0A++ot%3A" + score + "+%3Factivity.%0D%0A%3Freached%0D%0A++ot%3Aof+%3Fperson%3B%0D%0A++ot%3AtoThe+%3Fbeacon.%0D%0A%3Factivity+%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + userID + "%22.%0D%0A%7D&format=json";
                                    Log.d("TAG","norespone");
                                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    try {
                                                        JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                                        for (int i = 0; i < result.length(); i++) {
                                                            JSONObject aux = result.getJSONObject(i);
                                                            String idBeacon = aux.getJSONObject("idBeacon").getString("value");
                                                            beaconsReached.add(idBeacon);
                                                        }
                                                        Log.d(TAG, "De esas balizas " + beaconsReached.size() + " han sido alcanzadas:\n");
                                                        if (beaconsReached.size() > 0) {
                                                            for (String reachedID : beaconsReached) {
                                                                Log.d(TAG, reachedID + "\n");
                                                            }
                                                        }
                                                        // DEBUG
                                                        Log.d(TAG, "Por lo tanto, quedan por alcanzar " + (beacons.size() - beaconsReached.size()) + ":\n");
                                                        for (BeaconLOD beacon : beacons) {
                                                            if (!beaconsReached.contains(beacon.getBeacon_id())) {
                                                                Log.d(TAG, beacon.getBeacon_id());
                                                            }
                                                        }
                                                        //
                                                        initialDataSet = true; // we have all the initial data ready

                                                    } catch (JSONException e) {
                                                        Log.d("TAG","norespone");
                                                        e.printStackTrace();
                                                    }

                                                }
                                            }, new Response.ErrorListener() {

                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    Log.d("TAG","norespone");

                                                }
                                            });
                                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

                                } catch (JSONException e) {
                                    Log.d("TAG","norespone");
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("TAG","norespone");

                            }
                        });
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        executing = false;
        removeLocationUpdates();
        stopForeground(true);
    }

    private void onNewLocation(Location location) {

        // DEBUG
        Log.d(TAG, "Ejecutando onNewLocation...\n New location: " + location.getLatitude() + " " +
                location.getLongitude() + "\n");
        if (initialDataSet) {
            Log.d(TAG, "Ya tenemos los datos iniciales");
        } else {
            Log.d(TAG, "Aún no están los datos iniciales");
        }
        //
        mLocation = location;

        // get current time
        long millis = System.currentTimeMillis();
        Date current_time = new Date(millis);

        // get current location
        double lat1 = location.getLatitude();
        double lng1 = location.getLongitude();

        // check if we already have the initial data needed to play
        if (activity != null && initialDataSet) {

            // DEBUG
            Log.d(TAG, "Son las: " + current_time.toString());
            Log.d(TAG, "La actividad termina a las: " + activity.getFinishTime().toString());
            //

            // check if the activity has already finished
            if (current_time.after(activity.getFinishTime())) {
                // DEBUG
                Log.d(TAG, "El tiempo se ha terminado. A continuacion finalizamos la actividad.");
                //
                // change the state and set the finish time to that of the activity, because it means that
                // the user did not get to the end of the activity

                /*
                 * DELETE DATA {
                 *   GRAPH <http://localhost:8890/DAV> {
                 *      ot:trackIRI
                 *          ot:trackState "NOW".
                 *   }
                 * }
                 * INSERT DATA{
                 *   GRAPH <http://localhost:8890/DAV> {
                 *      ot:trackIRI
                 *          ot:trackState "FINISHED".
                 *   }
                 * }
                 */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=DELETE+DATA+%7B%0D%0A++GRAPH+<http%3A%2F%2Flocalhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + trackIRI + "%0D%0A+ot%3AtrackState+\"NOW\".%0D%0A++%7D+%0D%0A%7D+%0D%0AINSERT+DATA%7B%0D%0AGRAPH+<http%3A%2F%2Flocalhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + trackIRI + "%0D%0A+ot%3AtrackState+\"FINISHED\".%0D%0A++%7D+%0D%0A%7D+%0D%0A&format=json";

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                    if (result.contains("Delete from <http://localhost:8890/DAV>, 1 (or less) triples -- done")) {
                                        Log.d(TAG, "Se acabó el tiempo. Actividad finalizada.");
                                        //
                                        String name = "Actividad terminada";
                                        int number = beacons.size() + 2;
                                        String message = "Se agotó el tiempo para terminar la actividad";
                                        sendBeaconNotification(message, name, number);
                                        stopSelf();
                                    }

                                } catch (JSONException e) {
                                    Log.d("TAG","norespone");
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("TAG","norespone");

                            }
                        });
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);;
            


            } else {
                // there is still time, so we continue playing
                // DEBUG
                Log.d(TAG, "Aun queda tiempo de actividad, así que seguimos jugando");
                //
                float distanceGoal = getDistance(lat1, activity.getEnd_lat(), lng1, activity.getEnd_lng());
                if (((beacons.size() - beaconsReached.size()) < 1)
                        && distanceGoal <= LOCATION_PRECISION && !uploadingReach) {
                    // no beacons left, and reached the goal, so we finish the activity
                    // DEBUG
                    Log.d(TAG, "No quedan balizas por alcanzar y hemos llegado a meta, terminar la actividad");
                    //
                    uploadingReach = true;

                    /*
                     * DELETE DATA {
                     *   GRAPH <http://localhost:8890/DAV> {
                     *      ot:trackIRI
                     *          ot:trackState "NOW";
                     *          ot:completed false.
                     *   }
                     * }
                     * INSERT DATA{
                     *   GRAPH <http://localhost:8890/DAV> {
                     *      ot:trackIRI
                     *          ot:trackState "FINISHED";
                     *          ot:completed true.
                     *   }
                     * }
                     */

                    String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=DELETE+DATA+%7B%0D%0A++GRAPH+<http%3A%2F%2Flocalhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + trackIRI + "%0D%0A+ot%3AtrackState+\"NOW\";+ot:completed+false.%0D%0A++%7D+%0D%0A%7D+%0D%0AINSERT+DATA%7B%0D%0AGRAPH+<http%3A%2F%2Flocalhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + trackIRI + "%0D%0A+ot%3AtrackState+\"FINISHED\";+ot:completed+true.%0D%0A++%7D+%0D%0A%7D+%0D%0A&format=json";

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                        if (result.contains("Delete from <http://localhost:8890/DAV>, 2 (or less) triples -- done")) {
                                            uploadingReach = false;
                                            // DEBUG
                                            Log.d(TAG, "Actividad terminada. Todas las balizas alcanzadas. Llegada a meta.");
                                            //
                                            String name = "Meta";
                                            int number = beacons.size() + 1;
                                            String message = "Has completado la actividad";
                                            sendBeaconNotification(message, name, number);
                                            stopSelf();
                                        } else {
                                            uploadingReach = false;
                                        }

                                    } catch (JSONException e) {
                                        Log.d("TAG","norespone");
                                        e.printStackTrace();
                                        uploadingReach = false;
                                    }

                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d("TAG","norespone");
                                    uploadingReach = false;
                                }
                            });
                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);


                } else {
                    // there are still beacons to be reached, so we play
                    // update the current location (needed to see the track later)
                    updateCurrentLocation(lat1, lng1, current_time);
                    if (activity.isScore()) {
                        playScore(lat1, lng1, current_time);
                    } else {
                        playClassical(lat1, lng1, current_time);
                    }
                }
            }
        } else {
            // DEBUG
            Log.d(TAG, "La actividad es nula o aún no tenemos los datos iniciales, por lo que no hacemos nada");
            //
        }

    }

    private void updateCurrentLocation(double lat1, double lng1, Date current_time) {
        // update current location (needed to see the track later)
        com.smov.gabriel.orientatree.model.Location location = new com.smov.gabriel.orientatree.model.Location();
        location.setTime(current_time);
        GeoPoint point = new GeoPoint(lat1, lng1);
        location.setLocation(point);
        String locationID = UUID.randomUUID().toString();


        /*
         *
         * SELECT DISTINCT ?track {
         *  ?track
         *    ot:from ?activity;
         *    ot:belongsTo ?person.
         *  ?activity
         *   rdf:ID activity.getID().
         *  ?person
         *   ot:userName userID.
         * }
         *
         * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Ftrack+%7B%0D%0A++%3Ftrack%0D%0A+ot%3Afrom+%3Factivity%3B%0D%0A+ot%3AbelongsTo+%3Fperson.%0D%0A++%3Factivity%0D%0A+++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A++%3Fperson%0D%0A+++ot%3AuserName+%22" + userID + "%22.%0D%0A%7D&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                trackIRI = aux.getJSONObject("track").getString("value").split("#")[1];
                            }

                            /*
                             * INSERT DATA {
                             *  GRAPH <http:localhost:8890/DAV> {
                             *    ot:locationID geo:long point.getLongitude();
                                        geo:lat  point.getLatitude();
                                        ot:time ZonedDateTime.now().
                                  ot:trackIRI ot:composedby ot:locationID.
                                }
                             * }
                             */

                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA+%7B%0D%0AGRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + locationID + "+geo%3Along+" + point.getLongitude() + "%3B%0D%0A+geo%3Alat+" + point.getLatitude() + "%3B%0D%0A+ot%3Atime+\"" + ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")) + "\".%0D%0A+ot%3A" + trackIRI + "+ot%3AcomposedBy+ot%3A" + locationID + ".%0D%0A++%7D+%0D%0A%7D+&format=json";

                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                // not uploading any more
                                                if (result.equals("Insert into <http://localhost:8890/DAV>, 4 (or less) triples -- done")) {

                                                    Log.d(TAG, "Location subida");
                                                }


                                            } catch (JSONException e) {
                                                Log.d("TAG","norespone");
                                                e.printStackTrace();
                                            }

                                        }
                                    }, new Response.ErrorListener() {

                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.d("TAG","norespone");
                                        }
                                    });
                            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

                        } catch (JSONException e) {
                            Log.d("TAG","norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG","norespone");

                    }
                });
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void playScore(double lat1, double lng1, Date current_time) {
        Log.d(TAG, "Jugamos Score");
        //
        for (BeaconLOD beacon : beacons) {
            if (!beaconsReached.contains(beacon.getBeacon_id())) {
                // for each beacon not yet reached get the distance to the current position
                double lat2 = beacon.getLocation().getLatitude();
                double lng2 = beacon.getLocation().getLongitude();
                float dist = getDistance(lat1, lat2, lng1, lng2);
                if (dist <= LOCATION_PRECISION && !uploadingReach) {
                    // DEBUG
                    Log.d(TAG, "Score: Estamos cerca de alguna baliza");
                    //
                    if ((beacons.size() - beaconsReached.size()) > 0) {
                        // DEBUG
                        Log.d(TAG, "Score: Aún no estamos buscando la meta, y hemos alcanzado una baliza que no es la meta");
                        //
                        BeaconReachedLOD beaconReached = new BeaconReachedLOD(current_time, beacon.getBeacon_id(),
                                false); // create a new BeaconReached
                        uploadingReach = true; // uploading...


                        /*
                         * SELECT DISTINCT ?person ?beacon WHERE{
                         *   ?beacon
                         *       rdf:ID beacon.getBeacon_id().
                         *   ?person
                         *       ot:userName userID.
                         * }
                         *
                         */
                        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fperson+%3Fbeacon+WHERE+%7B%0D%0A%3Fbeacon%0D%0A++rdf%3AID+%22" + beacon.getBeacon_id() + "%22.%0D%0A%3Fperson%0D%0A++ot%3AuserName+%22" + userID + "%22.%0D%0A%0D%0A%7D&format=json";
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                            String personIRI = "";
                                            String beaconIRI = "";
                                            for (int i = 0; i < result.length(); i++) {
                                                JSONObject aux = result.getJSONObject(i);
                                                personIRI = aux.getJSONObject("person").getString("value").split("#")[1];
                                                beaconIRI = aux.getJSONObject("beacon").getString("value").split("#")[1];
                                            }

                                            String fecha = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")).toString();

                                            /*
                                             * INSERT DATA {
                                             *   GRAPH <http://localhost:DAV> {
                                             *       ot:UUID.randomUUID().toString()
                                             *           ot:of ot:personIRI;
                                             *           ot:answerTime fecha;
                                             *           ot:toThe ot:beaconIRI.
                                             *   }
                                             * }
                                             */

                                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA+%7B%0D%0AGRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0Aot%3A" + UUID.randomUUID().toString() + "+ot%3Aof+ot%3A" + personIRI + "%3B%0D%0Aot%3AanswerTime+%3C" + fecha + "%3E%3B%0D%0Aot%3AtoThe+ot%3A" + beaconIRI + ".%0D%0A%7D%7D%0D%0A%0D%0A&format=json";

                                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                        @Override
                                                        public void onResponse(JSONObject response) {
                                                            try {
                                                                String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                                // not uploading any more
                                                                if (result.equals("Insert into <http://localhost:8890/DAV>, 3 (or less) triples -- done")) {

                                                                    beaconsReached.add(beacon.getBeacon_id()); // add the current beacon id to the beacons reached during the service
                                                                    // DEBUG
                                                                    Log.d(TAG, "Score: Añadiendo la baliza " + beacon.getBeacon_id() + " al conjunto de alcanzadas " +
                                                                            "que ahora tiene " + beaconsReached.size() + " elementos");
                                                                    //
                                                                    uploadingReach = false; // not uploading any more
                                                                    String name = beacon.getName();
                                                                    int number = beacon.getNumber();
                                                                    String message = "Has alcanzado la baliza " + name;
                                                                    sendBeaconNotification(message, name, number);
                                                                }
                                                                uploadingReach = false; // not uploading any more


                                                            } catch (JSONException e) {
                                                                Log.d("TAG","norespone");
                                                                e.printStackTrace();
                                                            }

                                                        }
                                                    }, new Response.ErrorListener() {

                                                        @Override
                                                        public void onErrorResponse(VolleyError error) {

                                                            uploadingReach = false; // not uploading any more
                                                            // don't update nextBeacon, so we will try it again in the next location update
                                                        }
                                                    });
                                            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);


                                        } catch (JSONException e) {
                                            Log.d("TAG","norespone");
                                            ;
                                            e.printStackTrace();
                                        }

                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("TAG","norespone");

                                    }
                                });
                        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                    }
                    break;
                } else {
                    // DEBUG
                    Log.d(TAG, "Score: no estamos cerca de ninguna baliza.");
                    //
                }
            }
        }

    }

    private void playClassical(double lat1, double lng1, Date current_time) {
        // DEBUG
        Log.d(TAG, "Jugamos Clásica");
        //
        if (beaconsReached.size() < beacons.size()) {
            // get the beacons that we have to reach next
            int searchedBeaconIndex = beaconsReached.size();
            BeaconLOD searchedBeacon = beacons.get(searchedBeaconIndex);
            // DEBUG
            Log.d(TAG, "Clásica: Ahora mismo hay " + searchedBeaconIndex + " balizas alcanzadas " +
                    "por lo tanto, la siguiente que tenemos que alcanzar es: " + searchedBeacon.getName());
            //
            double lat2 = searchedBeacon.getLocation().getLatitude();
            double lng2 = searchedBeacon.getLocation().getLongitude();
            float dist = getDistance(lat1, lat2, lng1, lng2);

            if (dist <= LOCATION_PRECISION && !uploadingReach) {
                // DEBUG
                Log.d(TAG, "Clásica: Hemos alcanzado: " + searchedBeacon.getName() + "Empezamos a actualizar");
                //
                // if we are close enough and not in the middle of an uploading operation...
                BeaconReachedLOD beaconReached = new BeaconReachedLOD(current_time, searchedBeacon.getBeacon_id(),
                        false/*, searchedBeacon.isGoal()*/); // create a new BeaconReached
                uploadingReach = true; // uploading...

                /*
                 * SELECT DISTINCT ?person ?beacon WHERE{
                 *   ?beacon
                 *       rdf:ID searchedBeacon.getBeacon_id().
                 *   ?person
                 *       ot:userName userID.
                 * }
                 */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fperson+%3Fbeacon+WHERE+%7B%0D%0A%3Fbeacon%0D%0A++rdf%3AID+%22" + searchedBeacon.getBeacon_id() + "%22.%0D%0A%3Fperson%0D%0A++ot%3AuserName+%22" + userID + "%22.%0D%0A%0D%0A%7D&format=json";
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                    String personIRI = "";
                                    String beaconIRI = "";
                                    for (int i = 0; i < result.length(); i++) {
                                        JSONObject aux = result.getJSONObject(i);
                                        personIRI = aux.getJSONObject("person").getString("value").split("#")[1];
                                        beaconIRI = aux.getJSONObject("beacon").getString("value").split("#")[1];
                                    }

                                    String fecha = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")).toString();


                                    /*
                                     * INSERT DATA {
                                     *   GRAPH <http://localhost:DAV> {
                                     *       ot:UUID.randomUUID().toString()
                                     *           ot:of ot:personIRI;
                                     *           ot:answerTime fecha;
                                     *           ot:toThe ot:beaconIRI.
                                     *   }
                                     * }
                                     *
                                     */

                                    String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA+%7B%0D%0AGRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0Aot%3A" + UUID.randomUUID().toString() + "+ot%3Aof+ot%3A" + personIRI + "%3B%0D%0Aot%3AanswerTime+%3C" + fecha + "%3E%3B%0D%0Aot%3AtoThe+ot%3A" + beaconIRI + ".%0D%0A%7D%7D%0D%0A%0D%0A&format=json";


                                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    try {
                                                        String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                        // not uploading any more
                                                        if (result.equals("Insert into <http://localhost:8890/DAV>, 3 (or less) triples -- done")) {

                                                            Log.d(TAG, "Clásica: Alcanzada " + searchedBeacon.getName());
                                                            //
                                                            beaconsReached.add(searchedBeacon.getBeacon_id()); // update the set with the reaches
                                                            String name = searchedBeacon.getName();
                                                            int number = searchedBeacon.getNumber();
                                                            String message = "Has alcanzado la baliza " + name;

                                                            sendBeaconNotification(message, name, number);
                                                        }
                                                        uploadingReach = false;


                                                    } catch (JSONException e) {
                                                        Log.d("TAG","norespone");
                                                        e.printStackTrace();
                                                    }

                                                }
                                            }, new Response.ErrorListener() {

                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    Log.d("TAG","norespone");

                                                }
                                            });
                                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);


                                } catch (JSONException e) {
                                    Log.d("TAG","norespone");
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("TAG","norespone");

                            }
                        });
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

            }
        } else {
            // DEBUG
            Log.d(TAG, "Clásica: No quedan balizas por alcanzar");
            //
        }
    }

    // displays the notification of the foreground service
    private void startMyOwnForeground() {

        String ON_GOING_NOTIFICATION_CHANNEL_ID = "orientatree.foregroundService";
        int ON_GOING_NOTIFICATION_CHANNEL_NUMBER = 1111;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "Rastreo de la ubicación";
            String description = "Notificación que se muestra mientras el el servicio que rastrea la ubicación está en marcha";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(ON_GOING_NOTIFICATION_CHANNEL_ID, channelName, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ON_GOING_NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("Participando en la actividad")
                    .setContentText("Rastreando la ubicación en busca de balizas cercanas")
                    .setSmallIcon(R.drawable.ic_map)
                    .setColor(getColor(R.color.primary_color))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();

            //startForeground(2, notification);
            startForeground(ON_GOING_NOTIFICATION_CHANNEL_NUMBER, notification);
        } else {
            Notification notification =
                    new Notification.Builder(this, ON_GOING_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("Participando en la actividad")
                            .setContentText("Rastreando la ubicación en busca de balizas cercanas")
                            .setSmallIcon(R.drawable.ic_map)
                            .setColor(getColor(R.color.primary_color))
                            .setPriority(Notification.PRIORITY_LOW)
                            .build();
            //startForeground(1, notification);
            startForeground(ON_GOING_NOTIFICATION_CHANNEL_NUMBER + 1, notification);
        }
    }

    // send the notification of a regular beacon
    private void sendBeaconNotification(String message, String title, int number) {

        // 1 create the channel if needed, and set the intent for the action
        String BEACON_NOTIFICATION_CHANNEL_ID = "orientatree.beaconNotification"; // name of the channel for beacon notifications

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificaciones de llegada a las balizas";
            String description = "Notificaciones que aparecen al llegar a una baliza";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(BEACON_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(alarmSound, audioAttributes);
            channel.enableLights(true);
            channel.setLightColor(getColor(R.color.secondary_color));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000, 1000});
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // 2.0 create the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BEACON_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flag)
                    .setColor(getColor(R.color.secondary_color))
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setLights(getColor(R.color.secondary_color), 3000, 3000)
                    .setContentText(message);

            // 3.0 show the notification
            notificationManager.notify(number, builder.build());

        } else {
            // 2.1 create the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BEACON_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flag)
                    .setColor(getColor(R.color.secondary_color))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                    .setLights(getColor(R.color.secondary_color), 3000, 3000)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);

            // 3.1 show the notification
            NotificationManagerCompat nManager = NotificationManagerCompat.from(this);
            nManager.notify(number, builder.build());
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // starts asking for location updates
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        startService(new Intent(getApplicationContext(), LocationService.class));
        try {
            fusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    // stops the service from asking for new location updates
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            fusedLocationClient.removeLocationUpdates(mLocationCallback);
            stopSelf();
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    // reckons the distance between two points in meters
    private float getDistance(double lat1, double lat2, double lng1, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double p = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(p), Math.sqrt(1 - p));
        float dist = (float) (earthRadius * c);
        return dist;
    }
}
