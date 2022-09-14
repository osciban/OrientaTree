package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CompoundButton;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.databinding.ActivityTrackBinding;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.Location;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Template;

import org.jetbrains.annotations.NotNull;
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

public class TrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityTrackBinding binding;

    // UI elements
    private Toolbar toolbar;
    private Slider track_slider;
    private TextView trackHour_textView;
    private SwitchMaterial trackCompleto_switch;

    // useful model objects

    private ActivityLOD activity;
    private Map templateMap;
    //private Participation participation;

    // useful IDs
    private String userID;
    private String activityID;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm:ss";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    // arraylist with the locations
    private ArrayList<Location> locations;

    // objects needed to show the track
    private PolylineOptions polylineOptions;
    private Polyline polyline1; // (partial track)
    private Polyline polyline2; // (complete track)

    // max number of points that are shown at the same time in the track
    private static final int RANGE = 60;

    // Firebase services
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackMap);
        mapFragment.getMapAsync(this);

        // getting the intent
        Intent intent = getIntent();
        activity = (ActivityLOD) intent.getSerializableExtra("activity");
        userID = intent.getExtras().getString("participantID");

        // get the important IDs
        if (activity != null /*&& participation != null*/) {
            activityID = activity.getId();
            // userID = participation.getParticipant();
        }

        // binding UI elements
        toolbar = findViewById(R.id.track_toolbar);
        track_slider = findViewById(R.id.track_slider);
        trackHour_textView = findViewById(R.id.trackHour_textView);
        trackCompleto_switch = findViewById(R.id.trackComplete_switch);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        track_slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull @NotNull Slider slider, float value, boolean fromUser) {
                if (polyline1 != null && locations != null) {
                    ArrayList<LatLng> points = new ArrayList<>();
                    int index = (int) value;
                    for (int i = index - RANGE; i < index; i++) {
                        if (i >= 0) {
                            if (locations.get(i) != null) {
                                LatLng p = new LatLng(locations.get(i).getLocation().getLatitude(),
                                        locations.get(i).getLocation().getLongitude());
                                points.add(p);
                            }
                        }
                    }
                    LatLng p = new LatLng(locations.get(index).getLocation().getLatitude(),
                            locations.get(index).getLocation().getLongitude());
                    points.add(p);
                    polyline1.setPoints(points);
                    trackHour_textView.setText(df_hour.format(locations.get(index).getTime()));
                }
            }
        });

        trackCompleto_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (polyline2 != null && locations != null) {
                        polyline2.setVisible(true);
                    }
                } else {
                    if (polyline2 != null && locations != null) {
                        polyline2.setVisible(false);
                    }
                }
            }
        });
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
             *       rdf:ID activityID.
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

            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FsouthEastLat+%3FsouthEastLong+%3FnorthWestLat+%3FnorthWestLong+WHERE%7B%0D%0A+%3Factivity%0D%0A++ot%3AlocatedIn+%3Fmap%3B%0D%0A++rdf%3AID+%22" + activityID + "%22.%0D%0A%3Fmap%0D%0A++ot%3AnorthWestCorner+%3FnorthPoint%3B%0D%0A++ot%3AsouthEastCorner+%3FsouthPoint.%0D%0A%3FnorthPoint%0D%0A++geo%3Alat+%3FnorthWestLat%3B%0D%0A++geo%3Along+%3FnorthWestLong.%0D%0A%3FsouthPoint%0D%0A++geo%3Alat+%3FsouthEastLat%3B%0D%0A++geo%3Along+%3FsouthEastLong.%0D%0A%7D%0D%0A&format=json";
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            System.out.println("URL TrackActivity:" + url);

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

                    /*LatLngBounds overlay_bounds = new LatLngBounds(
                            new LatLng(templateMap.getOverlay_corners().get(0).getLatitude(),
                                    templateMap.getOverlay_corners().get(0).getLongitude()),       // South west corner
                            new LatLng(templateMap.getOverlay_corners().get(1).getLatitude(),
                                    templateMap.getOverlay_corners().get(1).getLongitude()));
                    */
                        // set image as overlay
                        GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                .image(image)
                                .positionFromBounds(overlay_bounds);

                        // set the overlay on the map
                        mMap.addGroundOverlay(overlayMap);

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, 17));
                        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, templateMap.getInitial_zoom()));


                        // setting maximum and minimum zoom the user can perform on the map
                    /*mMap.setMinZoomPreference(templateMap.getMin_zoom());
                    mMap.setMaxZoomPreference(templateMap.getMax_zoom());*/
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

                        if (userID != null && activityID != null) {
                            getLocations();


                        } else {
                            Toast.makeText(TrackActivity.this, "Algo salió mal al obtener los datos de la participación. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        System.out.println(("noresponse"));
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

        //obtener id del mapa
        /*if (template != null) {
            db.collection("maps").document(template.getMap_id())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            // getting the map
                            templateMap = documentSnapshot.toObject(Map.class);

                            // where to center the map at the outset
                            LatLng center_map = new LatLng(templateMap.getCentering_point().getLatitude(),
                                    templateMap.getCentering_point().getLongitude());

                            // get the map image from a file and reduce its size
                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                            //File mypath = new File(directory, activity.getId() + ".png");
                            File mypath = new File(directory, activity.getTemplate() + ".png");
                            Bitmap image_bitmap = decodeFile(mypath, 540, 960);
                            BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

                            LatLngBounds overlay_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getOverlay_corners().get(0).getLatitude(),
                                            templateMap.getOverlay_corners().get(0).getLongitude()),       // South west corner
                                    new LatLng(templateMap.getOverlay_corners().get(1).getLatitude(),
                                            templateMap.getOverlay_corners().get(1).getLongitude()));

                            // set image as overlay
                            GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                    .image(image)
                                    .positionFromBounds(overlay_bounds);

                            // set the overlay on the map
                            mMap.addGroundOverlay(overlayMap);

                            mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, templateMap.getInitial_zoom()));

                            // setting maximum and minimum zoom the user can perform on the map
                            mMap.setMinZoomPreference(templateMap.getMin_zoom());
                            mMap.setMaxZoomPreference(templateMap.getMax_zoom());

                            // setting bounds for the map so that user can not navigate other places
                            LatLngBounds map_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getMap_corners().get(0).getLatitude(),
                                            templateMap.getMap_corners().get(0).getLongitude()), // SW bounds
                                    new LatLng(templateMap.getMap_corners().get(1).getLatitude(),
                                            templateMap.getMap_corners().get(1).getLongitude())  // NE bounds
                            );
                            mMap.setLatLngBoundsForCameraTarget(map_bounds);

                            //Obtener localizaciones

                            // get the locations, create the polyline, enable the slider
                            if(userID != null && activityID != null) {
                                //initialize arrayList containing the locations
                                locations = new ArrayList<>();
                                // get the locations
                                db.collection("activities").document(activityID)
                                        .collection("participations").document(userID)
                                        .collection("locations")
                                        .orderBy("time", Query.Direction.ASCENDING)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                                    Location location = documentSnapshot.toObject(Location.class);
                                                    locations.add(location);
                                                }
                                                if(locations != null && locations.size() >= 1) {
                                                    // setting partial track
                                                    // set slider parameters
                                                    track_slider.setValueFrom(0);
                                                    track_slider.setValueTo(locations.size() - 1);
                                                    if(locations.get(0) != null) {
                                                        trackHour_textView.setText(df_hour.format(locations.get(0).getTime()));
                                                        polylineOptions = new PolylineOptions()
                                                                .add(new LatLng(locations.get(0).getLocation().getLatitude(),
                                                                        locations.get(0).getLocation().getLongitude()));
                                                        polyline1 = mMap.addPolyline(polylineOptions); // draw point at the start (partial track)
                                                        polyline1.setWidth(15);
                                                        if(polyline1 != null) {
                                                            track_slider.setEnabled(true); // enable slider
                                                        }
                                                        // setting complete track
                                                        polyline2 = mMap.addPolyline(polylineOptions); // draw point at the start (complete track)
                                                        polyline2.setVisible(false);
                                                        polyline2.setColor(R.color.primary_color);
                                                        polyline2.setWidth(10);
                                                        ArrayList<LatLng> points = new ArrayList<>();
                                                        for (Location location : locations) {
                                                            LatLng p = new LatLng(location.getLocation().getLatitude(),
                                                                    location.getLocation().getLongitude());
                                                            points.add(p);
                                                        }
                                                        polyline2.setPoints(points);
                                                        trackCompleto_switch.setEnabled(true);
                                                    }
                                                } else {
                                                    Toast.makeText(TrackActivity.this, "No se han encontrado datos que mostrar", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull @NotNull Exception e) {
                                                Toast.makeText(TrackActivity.this, "Algo salió mal al descargar los datos de la participación. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(TrackActivity.this, "Algo salió mal al obtener los datos de la participación. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }*/
    }

    private void getLocations() {

        /*
        * SELECT DISTINCT ?lat ?long ?time WHERE{
        *   ?activity
        *       rdf:ID activityID.
        *   ?person
        *       ot:userName userID.
        *   ?track
        *       ot:from ?activity;
        *       ot:belongsTo ?person;
        *       ot:composedBy ?point.
        *   ?point
        *       geo:lat ?lat;
        *       geo:long ?long;
        *       ot:time ?time.
        * }ORDER BY ASC(?time)
        *
        * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Flat+%3Flong+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activityID + "%22.%0D%0A%3Fperson%0D%0A++ot%3AuserName+%22" + userID + "%22.%0D%0A%3Ftrack%0D%0A+ot%3Afrom+%3Factivity%3B%0D%0A+ot%3AbelongsTo+%3Fperson%3B%0D%0A+ot%3AcomposedBy+%3Fpoint.%0D%0A%3Fpoint%0D%0A+geo%3Alat+%3Flat%3B%0D%0A+geo%3Along+%3Flong%3B%0D%0A+ot%3Atime+%3Ftime.%0D%0A%7D+ORDER+BY+ASC%28%3Ftime%29&format=json";
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        System.out.println("URL TrackActivity2:" + url);
        locations = new ArrayList<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                    for (int i = 0; i < result.length(); i++) {
                        JSONObject aux = result.getJSONObject(i);
                        Double latitude=aux.getJSONObject("lat").getDouble("value");
                        Double longitude=aux.getJSONObject("long").getDouble("value");
                        Date time=Date.from(ZonedDateTime.parse((aux.getJSONObject("time").getString("value")+"[Europe/Madrid]")).toInstant());
                        locations.add(new Location(new GeoPoint(latitude,longitude),time));
                    }

                    if(locations != null && locations.size() >= 1) {
                        // setting partial track
                        // set slider parameters
                        track_slider.setValueFrom(0);
                        track_slider.setValueTo(locations.size() - 1);
                        if(locations.get(0) != null) {
                            trackHour_textView.setText(df_hour.format(locations.get(0).getTime()));
                            polylineOptions = new PolylineOptions()
                                    .add(new LatLng(locations.get(0).getLocation().getLatitude(),
                                            locations.get(0).getLocation().getLongitude()));
                            polyline1 = mMap.addPolyline(polylineOptions); // draw point at the start (partial track)
                            polyline1.setWidth(15);
                            if(polyline1 != null) {
                                track_slider.setEnabled(true); // enable slider
                            }
                            // setting complete track
                            polyline2 = mMap.addPolyline(polylineOptions); // draw point at the start (complete track)
                            polyline2.setVisible(false);
                            polyline2.setColor(R.color.primary_color);
                            polyline2.setWidth(10);
                            ArrayList<LatLng> points = new ArrayList<>();
                            for (Location location : locations) {
                                LatLng p = new LatLng(location.getLocation().getLatitude(),
                                        location.getLocation().getLongitude());
                                points.add(p);
                            }
                            polyline2.setPoints(points);
                            trackCompleto_switch.setEnabled(true);
                        }
                    } else {
                        Toast.makeText(TrackActivity.this, "No se han encontrado datos que mostrar", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    System.out.println(("noresponse"));
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