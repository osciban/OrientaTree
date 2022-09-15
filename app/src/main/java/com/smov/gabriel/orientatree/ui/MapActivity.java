package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ReachAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback/*, GoogleMap.OnMapLongClickListener*/ {

    private TextView reachesMap_textView, map_timer_textView,
            mapBeaconsBadge_textView;
    private MaterialButton mapBeacons_button;
    private FloatingActionButton map_fab, mapLocationOff_fab;
    private CircularProgressIndicator map_progressIndicator;
    private Toolbar toolbar;

    private GoogleMap mMap;

    private Map templateMap;
    private Participation participation;

    private Template template;
    private ActivityLOD activity;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;

    private int num_beacons; // number of beacons that this activity has
    private int beacons_reached; // number of beacons that have already been reached by the participant

    // useful IDs
    private String userID;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1110;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // get current user's ID
        userID = mAuth.getCurrentUser().getUid();

        //get map file
        Intent intent = getIntent();
        template = (Template) intent.getSerializableExtra("template");
        activity = (ActivityLOD) intent.getSerializableExtra("activity");

        // bind UI elements
        mapBeacons_button = findViewById(R.id.beaconsMap_button);
        reachesMap_textView = findViewById(R.id.reachesMap_textView);
        map_timer_textView = findViewById(R.id.map_timer_textView);
        map_fab = findViewById(R.id.map_fab);
        map_progressIndicator = findViewById(R.id.map_progressIndicator);
        mapLocationOff_fab = findViewById(R.id.mapLocationOff_fab);
        toolbar = findViewById(R.id.map_toolbar);
        mapBeaconsBadge_textView = findViewById(R.id.mapBeaconsBadge_textView);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set listener to the beacons button
        mapBeacons_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches(activity);
            }
        });
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        // realtime listener to display the timer
        if (activity != null /*&& template != null*/ && userID != null
            /*&& template.getType() == TemplateType.DEPORTIVA*/) {

            /*
             * SELECT DISTINCT ?state ?time WHERE{
             *   ?activity
             *       rdf:ID activity.getID().
             *   ?track
             *       ot:from ?activity;
             *       ot:trackState ?state;
             *       ot:composedBy ?point.
             *   ?point
             *       ot:time ?time.
             * } ORDER BY(?time)
             *
             */
            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fstate+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Ftrack%0D%0Aot%3Afrom+%3Factivity%3B%0D%0Aot%3AtrackState+%3Fstate%3B%0D%0Aot%3AcomposedBy+%3Fpoint.%0D%0A%3Fpoint%0D%0Aot%3Atime+%3Ftime.%0D%0A%7D+ORDER+BY+ASC%28%3Ftime%29+&format=json";


            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                ParticipationState pstate = ParticipationState.FINISHED;
                                participation = new Participation();
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    if (i == 0) {

                                        String start = aux.getJSONObject("time").getString("value");
                                        String state = aux.getJSONObject("state").getString("value");
                                        switch (state) {
                                            case "FINISHED":
                                                pstate = pstate.FINISHED;
                                                break;
                                            case "NOT_YET":
                                                pstate = pstate.NOT_YET;
                                                break;
                                            case "NOW":
                                                pstate = pstate.NOW;
                                                break;
                                            default:
                                                break;
                                        }
                                        participation.setState(pstate);
                                        participation.setStartTime((Date.from(ZonedDateTime.parse((start)).toInstant())));

                                    } else if (i == result.length() - 1) {
                                        String end = aux.getJSONObject("time").getString("value");
                                        participation.setFinishTime((Date.from(ZonedDateTime.parse((end)).toInstant())));
                                    }
                                }
                                if (participation != null) {
                                    Date current_time = new Date(System.currentTimeMillis());
                                    if (participation.getStartTime() != null) {
                                        Date start_time = participation.getStartTime();
                                        switch (participation.getState()) {
                                            case NOT_YET:
                                                break;
                                            case NOW:
                                                // taking part now, so we display the current time record
                                                long diff_to_now = Math.abs(start_time.getTime() - current_time.getTime()) / 1000;
                                                time = (double) diff_to_now;
                                                // set the timer
                                                if (time < 86400) { // the maximum time it can display is 23:59:59...
                                                    timer = new Timer();
                                                    timerTask = new TimerTask() {
                                                        @Override
                                                        public void run() {
                                                            MapActivity.this.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    time++;
                                                                    map_timer_textView.setText(getTimerText());
                                                                }
                                                            });
                                                        }

                                                    };
                                                    timer.scheduleAtFixedRate(timerTask, 0, 1000);
                                                }
                                                break;
                                            case FINISHED:
                                                // participation finished, so we show the total time (static, not counting)
                                                if (timerTask != null) {
                                                    // stop the timer
                                                    timerTask.cancel();
                                                }
                                                if (participation.getFinishTime() != null) {
                                                    Date finish_time = participation.getFinishTime();
                                                    long diff_to_finish = Math.abs(start_time.getTime() - finish_time.getTime()) / 1000;
                                                    double total_time = (double) diff_to_finish;
                                                    map_timer_textView.setText(getTimerText(total_time));
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {


                        }
                    });
            queue.add(jsonObjectRequest);

            if (activity.isLocation_help()) {
                // if location is allowed in this activity...
                map_fab.setEnabled(true);
                map_fab.setVisibility(View.VISIBLE);
            }
        }

        // realtime listener to display the number of beacons already reached
        if (activity != null) {
            //if (activity.getBeaconSize() != null) {
            num_beacons = activity.getBeaconSize();

            String score = "scorePartof";
            if (!activity.isScore()) {
                score = "linealPartOf";
            }
            ArrayList<BeaconReachedLOD> reaches = new ArrayList<>();

            /*
             * SELECT DISTINCT ?beaconID WHERE{
             *   ?activity
             *       rdf:ID activity.getID().
             *   ?beacon
             *       rdf:ID ?beaconID;
             *       ot:score(scorePartof/linealPartOf) ?activity.
             *   ?personAnswer
             *       ot:toThe ?beacon;
             *       ot:of ?person.
             *   ?person
             *       ot:userName userID.
             * }
             *
             * */
            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FbeaconID+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fbeacon%0D%0Ardf%3AID+%3FbeaconID%3B%0D%0Aot%3A" + score + "%3Factivity.%0D%0A%3FpersonAnswer%0D%0Aot%3AtoThe+%3Fbeacon%3B%0D%0Aot%3Aof+%3Fperson.%0D%0A%3Fperson%0D%0Aot%3AuserName+%22" + userID + "%22.%0D%0A%7D&format=json";


            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        ArrayList<BeaconReachedLOD> reachesAux = new ArrayList<>();

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);

                                    String beaconID = aux.getJSONObject("beaconID").getString("value");
                                    BeaconReachedLOD reach = new BeaconReachedLOD();
                                    reach.setBeacon_id(beaconID);
                                    reach.setAnswered(false);
                                    reachesAux.add(reach);
                                }
                                String score = "scorePartof";
                                if (!activity.isScore()) {
                                    score = "linealPartOf";
                                }

                                /*
                                 * SELECT DISTINCT ?correctanswer ?answer ?beaconID ?time WHERE{
                                 *   ?activity
                                 *       rdf:ID activity.getId().
                                 *   ?beacon
                                 *       ot:score(scorePartof/linealPartOf) ?activity;
                                 *       rdf:ID ?beaconID;
                                 *       ot:about ?object.
                                 *   ?personanswer
                                 *       ot:toThe ?beacon;
                                 *       ot:answerResource ?answer;
                                 *       ot:answerTime ?time;
                                 *       ot:of ?person.
                                 *   ?person
                                 *       ot:userName userID.
                                 *   ?objectproperty
                                 *       ot:relatedTo ?object;
                                 *       ot:answer ?correctanswer.
                                 * }
                                 * */

                                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fcorrectanswer+%3Fanswer+%3FbeaconID+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fbeacon%0D%0A++ot%3A" + score + "+%3Factivity%3B%0D%0A++rdf%3AID+%3FbeaconID%3B%0D%0A++ot%3Aabout+%3Fobject.%0D%0A%3Fpersonanswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3AanswerResource+%3Fanswer%3B%0D%0A+ot%3AanswerTime+%3Ftime%3B%0D%0A+ot%3Aof+%3Fperson.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + userID + "%22.%0D%0A+%3Fobjectproperty%0D%0A++ot%3ArelatedTo+%3Fobject%3B%0D%0A++ot%3Aanswer+%3Fcorrectanswer.%0D%0A%7D&format=json";

                                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                            @Override
                                            public void onResponse(JSONObject response) {
                                                try {

                                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                                                    for (int i = 0; i < result.length(); i++) {
                                                        JSONObject aux = result.getJSONObject(i);

                                                        String correctanswer = aux.getJSONObject("correctanswer").getString("value");
                                                        String answer = aux.getJSONObject("answer").getString("value"); //Esto revisar
                                                        String beaconID = aux.getJSONObject("beaconID").getString("value");
                                                        String time = aux.getJSONObject("time").getString("value");

                                                        BeaconReachedLOD reach = new BeaconReachedLOD();
                                                        if (answer.equals(correctanswer)) {
                                                            reach.setAnswer_right(true);
                                                            reach.setAnswered(true);
                                                        } else if (answer != null) {
                                                            reach.setAnswer_right(false);
                                                            reach.setAnswered(true);
                                                        } else {
                                                            reach.setAnswered(false);
                                                        }
                                                        reach.setReachMoment(Date.from(ZonedDateTime.parse(time).toInstant()));
                                                        reach.setBeacon_id(beaconID);
                                                        reach.setWritten_answer(answer);

                                                        //añadir reach
                                                        reaches.add(reach);
                                                    }
                                                    for (BeaconReachedLOD a : reachesAux) {
                                                        boolean flag = true;
                                                        for (BeaconReachedLOD b : reaches) {
                                                            if (a.getBeacon_id().equals(b.getBeacon_id())) {
                                                                flag = false;
                                                                break;
                                                            }
                                                        }
                                                        if (flag) {
                                                            reaches.add(a);
                                                        }
                                                    }

                                                    beacons_reached = reaches.size();
                                                    reachesMap_textView.setText(beacons_reached + "/"
                                                            + num_beacons);
                                                    // count how many of them are not answered yet
                                                    if (beacons_reached > 0 /*&& template.getType() == TemplateType.EDUCATIVA*/) {
                                                        int not_answered = 0;
                                                        for (BeaconReachedLOD beaconReached : reaches) {
                                                            if (!beaconReached.isAnswered()) {
                                                                not_answered++;
                                                            }
                                                        }
                                                        if (not_answered > 0) {
                                                            mapBeaconsBadge_textView.setText(String.valueOf(not_answered));
                                                            mapBeaconsBadge_textView.setVisibility(View.VISIBLE);
                                                        } else {
                                                            // no beacons reached without being answered
                                                            mapBeaconsBadge_textView.setVisibility(View.INVISIBLE);
                                                        }
                                                    } else {
                                                        // no beacons reached
                                                        mapBeaconsBadge_textView.setVisibility(View.INVISIBLE);
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        }, new Response.ErrorListener() {

                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                // TODO: Handle error

                                            }
                                        });
                                queue.add(jsonObjectRequest);


                            } catch (JSONException e) {
                                System.err.println(("noresponse"));
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error

                        }
                    });
            queue.add(jsonObjectRequest);


        }


        map_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableLocation();
            }
        });

        mapLocationOff_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableLocation();
            }
        });
    }

    private void disableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                // hide location off fab
                mapLocationOff_fab.setVisibility(View.GONE);
                mapLocationOff_fab.setEnabled(false);
                // show location fab
                map_fab.setEnabled(true);
                map_fab.setVisibility(View.VISIBLE);
                // disable location
                mMap.setMyLocationEnabled(false);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                // hide location fab
                map_fab.setVisibility(View.GONE);
                map_fab.setEnabled(false);
                // show location off fab
                mapLocationOff_fab.setEnabled(true);
                mapLocationOff_fab.setVisibility(View.VISIBLE);
                // enable location
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateUIReaches(ActivityLOD activity) {
        Intent intent = new Intent(MapActivity.this, ReachesActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("participation", participation);
        recreate();
        startActivity(intent);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // our map...
        mMap = googleMap;

        // setting styles...
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
        }

        if (activity != null) {
            templateMap = new Map();

            /*
             * SELECT DISTINCT ?southEastLat ?southEastLong ?northWestLat ?northWestLong WHERE{
             *   ?activity
             *       ot:locatedIn ?map;
             *       rdf:ID activity.getId().
             *   ?map
             *       ot:northWestCorner ?northPoint;
             *       ot:southEastCorner ?southPoint.
             *   ?northPoint
             *       geo:lat ?northWestLat;
             *       geo:long ?northWestLong.
             *   ?southPoint
             *       geo:lat ?southEastLat;
             *       geo:long ?southEastLong.
             * }
             * */
            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FsouthEastLat+%3FsouthEastLong+%3FnorthWestLat+%3FnorthWestLong+WHERE%7B%0D%0A+%3Factivity%0D%0A++ot%3AlocatedIn+%3Fmap%3B%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fmap%0D%0A++ot%3AnorthWestCorner+%3FnorthPoint%3B%0D%0A++ot%3AsouthEastCorner+%3FsouthPoint.%0D%0A%3FnorthPoint%0D%0A++geo%3Alat+%3FnorthWestLat%3B%0D%0A++geo%3Along+%3FnorthWestLong.%0D%0A%3FsouthPoint%0D%0A++geo%3Alat+%3FsouthEastLat%3B%0D%0A++geo%3Along+%3FsouthEastLong.%0D%0A%7D%0D%0A&format=json";
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    try {
                        ArrayList<GeoPoint> corners = new ArrayList<>();
                        JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject aux = result.getJSONObject(i);
                            Double southEastLat = aux.getJSONObject("southEastLat").getDouble("value");
                            Double southEastLong = aux.getJSONObject("southEastLong").getDouble("value");
                            Double northWestLat = aux.getJSONObject("northWestLat").getDouble("value");
                            Double northWestLong = aux.getJSONObject("northWestLong").getDouble("value");

                            corners.add(new GeoPoint(southEastLat, southEastLong));
                            corners.add(new GeoPoint(northWestLat, northWestLong));
                        }
                        templateMap.setMap_corners(corners);
                        LatLng center_map = new LatLng((templateMap.getMap_corners().get(0).getLatitude() + templateMap.getMap_corners().get(1).getLatitude()) / 2,
                                (templateMap.getMap_corners().get(0).getLongitude() + templateMap.getMap_corners().get(1).getLongitude()) / 2);

                        //LatLng center_map = new LatLng(templateMap.getCentering_point().getLatitude(),
                        //        templateMap.getCentering_point().getLongitude());

                        // get the map image from a file and reduce its size
                        ContextWrapper cw = new ContextWrapper(getApplicationContext());
                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                        //File mypath = new File(directory, activity.getId() + ".png");
                        File mypath = new File(directory, activity.getId() + ".png");
                        Bitmap image_bitmap = decodeFile(mypath, 540, 960);
                        BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

                        LatLngBounds overlay_bounds = new LatLngBounds(
                                new LatLng(41.644229,
                                        -4.733275),       // South west corner
                                new LatLng(41.647881,
                                        -4.728202));


                        // set image as overlay
                        GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                .image(image)
                                .positionFromBounds(overlay_bounds);

                        // set the overlay on the map
                        mMap.addGroundOverlay(overlayMap);

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, templateMap.getInitial_zoom()));

                        // setting maximum and minimum zoom the user can perform on the map
                        mMap.setMinZoomPreference(16);
                        mMap.setMaxZoomPreference(20);

                        // setting bounds for the map so that user can not navigate other places
                        LatLngBounds map_bounds = new LatLngBounds(
                                new LatLng(templateMap.getMap_corners().get(0).getLatitude(),
                                        templateMap.getMap_corners().get(0).getLongitude()), // SW bounds
                                new LatLng(templateMap.getMap_corners().get(1).getLatitude(),
                                        templateMap.getMap_corners().get(1).getLongitude())  // NE bounds
                        );
                        mMap.setLatLngBoundsForCameraTarget(map_bounds);
                    } catch (JSONException e) {
                        System.err.println(("noresponse"));
                        e.printStackTrace();
                    }
                }


            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO: Handle error

                }
            });
            queue.add(jsonObjectRequest);

        } else {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // decode image from file
    private Bitmap decodeFile(File f, int width, int height) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            int scale = calculateInSampleSize(o, width, height);

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    // used when we need a timer because the participation is not finished
    private String getTimerText() {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    // used when the activity is already finished, and we do not need a timer any more
    private String getTimerText(double time) {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    private String formatTime(int seconds, int minutes, int hours) {
        return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    //havePermissions = true;
                    Toast.makeText(this, "Ahora ya puedes comenzar la actividad", Toast.LENGTH_SHORT).show();
                } else {
                    //showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}