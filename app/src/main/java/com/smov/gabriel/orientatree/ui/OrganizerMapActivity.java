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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

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

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.databinding.ActivityOrganizerMapBinding;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.utils.MySingleton;
import com.smov.gabriel.orientatree.utils.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class OrganizerMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityOrganizerMapBinding binding;

    private Toolbar toolbar;
    private ExtendedFloatingActionButton organizerMapParticipants_fab;

    private ActivityLOD activity;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityOrganizerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.organizer_map);
        mapFragment.getMapAsync(this);

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.organizerMap_toolbar);
        organizerMapParticipants_fab = findViewById(R.id.organizerMapParticipants_fab);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get the activity
        Intent intent = getIntent();
        activity = (ActivityLOD) intent.getSerializableExtra("activity");
        //template = (Template) intent.getSerializableExtra("template");

        if (activity != null) {
            // if these attributes are not null, it means that we came from the now activity
            // in which case we should offer the option of watching the participants
            organizerMapParticipants_fab.setEnabled(true);
            organizerMapParticipants_fab.setVisibility(View.VISIBLE);
        } // in other case, we come from the template and we don't have to give that option

        organizerMapParticipants_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    updateUIParticipants();
                } else {
                    Toast.makeText(OrganizerMapActivity.this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
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

            //Obtener Mapa
            /*
             * SELECT ?northwestlat ?northwestlong ?southeastlat ?southeastlong WHERE{
             *   ?activity
             *       rdf:ID activity.getId();
             *       ot:locatedIn ?map.
             *   ?map
             *       ot:northWestCorner ?nwc;
             *       ot:southEastCorner ?sec.
             *   ?nwc
             *       geo:lat ?northwestlat;
             *       geo:long ?northwestlong.
             *   ?sec
             *       geo:lat ?southeastlat;
             *       geo:long ?southeastlong.
             * }
             *
             *
             * +ot:southEastCorner+?sec.+?nwc+geo:lat+?northwestlat;+geo:long+?northwestlong.+?sec+geo:lat+?southeastlat;+geo:long+?southeastlong.+}&format=json";
             */

            String url = "http://192.168.137.1:8890/sparql?query=SELECT+?northwestlat+?northwestlong+?southeastlat+?southeastlong+WHERE+{+?activity+rdf:ID+\"" + activity.getId() + "\";+ot:locatedIn+?map.+?map+ot:northWestCorner+?nwc;+ot:southEastCorner+?sec.+?nwc+geo:lat+?northwestlat;+geo:long+?northwestlong.+?sec+geo:lat+?southeastlat;+geo:long+?southeastlong.+}&format=json";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                Double northwestlat = 0.0;
                                Double northwestlong = 0.0;
                                Double southeastlat = 0.0;
                                Double southeastlong = 0.0;
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    northwestlat = Double.parseDouble(aux.getJSONObject("northwestlat").getString("value"));
                                    northwestlong = Double.parseDouble(aux.getJSONObject("northwestlong").getString("value"));
                                    southeastlat = Double.parseDouble(aux.getJSONObject("southeastlat").getString("value"));
                                    southeastlong = Double.parseDouble(aux.getJSONObject("southeastlong").getString("value"));

                                }

                                LatLng center_map = new LatLng(41.6457,
                                        -4.73006);
                                // get the map image from a file and reduce its size
                                ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                //File mypath = new File(directory, activity.getId() + ".png");
                                File mypath = new File(directory, activity.getId() + ".png");
                                Bitmap image_bitmap = Utilities.decodeFile(mypath, 540, 960,getApplicationContext());
                                BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

                                LatLngBounds overlay_bounds = new LatLngBounds(
                                        new LatLng(41.644229, -4.733275),       // South west corner
                                        new LatLng(41.647881, -4.728202));

                                // set image as overlay
                                GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                        .image(image)
                                        .positionFromBounds(overlay_bounds);

                                // set the overlay on the map
                                mMap.addGroundOverlay(overlayMap);

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, 17));

                                // setting maximum and minimum zoom the user can perform on the map
                                mMap.setMinZoomPreference(16);
                                mMap.setMaxZoomPreference(20);

                                // setting bounds for the map so that user can not navigate other places
                                LatLngBounds map_bounds = new LatLngBounds(new LatLng(southeastlat,
                                        southeastlong),
                                        new LatLng(northwestlat,
                                                northwestlong) // SW bounds
                                        // NE bounds
                                );
                                mMap.setLatLngBoundsForCameraTarget(map_bounds);

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






        } else {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
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

    private void updateUIParticipants() {
        Intent intent = new Intent(OrganizerMapActivity.this, ParticipantsListActivity.class);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }
}