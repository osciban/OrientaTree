package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;

import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.helpers.ActivityTime;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.services.LocationService;
import com.smov.gabriel.orientatree.utils.MySingleton;
import com.smov.gabriel.orientatree.utils.Utilities;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Date;
import java.util.UUID;

public class NowActivity extends AppCompatActivity {

    // UI elements
    private TextView nowType_textView, nowTitle_textView, nowTime_textView, nowOrganizer_textView,
            nowTemplate_textView, nowDescription_textView, nowNorms_textView,
            nowLocation_textView, nowMode_textView, nowState_textView;
    private ExtendedFloatingActionButton nowParticipant_extendedFab, nowSeeParticipants_extendedFab,
            nowDownloadMap_extendedFab;
    private MaterialButton nowCredentials_button, nowMap_button;
    private Toolbar toolbar;
    private ImageView now_imageView;
    private CoordinatorLayout now_coordinatorLayout;
    private CircularProgressIndicator now_progressIndicator;

    // declaring Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // some model objects required
    private ActivityLOD activity;
    private Template template;
    private User user;
    private Participation participation;

    // some useful IDs
    private String userID;
    private String organizerID;

    // here we represent whether the current user is the organizer of the activity or not
    private boolean isOrganizer = false;

    // formatters
    // to format the way hours are displayed
    private String pattern_hour = "HH:mm";
    private DateFormat df_hour = new SimpleDateFormat(pattern_hour);
    // to format the way dates are displayed
    private String pattern_day = "dd/MM/yyyy";
    private DateFormat df_date = new SimpleDateFormat(pattern_day);

    // location permissions
    // constant that represents query for fine location permission
    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;
    // this is true or false depending on whether we have location permissions or not
    private boolean havePermissions = false;

    private static final String TAG = "NOW ACTIVITY";

    // needed to check that the user is at the start spot
    private FusedLocationProviderClient fusedLocationClient;

    // intent to the location service that runs in foreground while the activity is on
    private Intent locationServiceIntent;

    // threshold precision in meters to consider that the user is at the start spot
    private static final float LOCATION_PRECISION = 1000f;

    /* here we store the information of the time of te activity so that we know if it was in the past
     * or it is taking place now, or if it is in the future */
    private ActivityTime activityTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_now);

        // get the activity from the intent
        Intent intent = getIntent();
        activity = (ActivityLOD) intent.getSerializableExtra("activity");
        // get if the activity is in the past, present or future
        activityTime = getActivityTime();

        // binding UI elements
        toolbar = findViewById(R.id.now_toolbar);
        nowCredentials_button = findViewById(R.id.nowCredentials_button); // only visible to organizer
        nowParticipant_extendedFab = findViewById(R.id.nowParticipant_extendedFab); // only visible to participant
        nowSeeParticipants_extendedFab = findViewById(R.id.nowSeeParticipants_extendedFab); // only visible to organizer
        nowType_textView = findViewById(R.id.nowType_textView);
        nowTitle_textView = findViewById(R.id.nowTitle_textView);
        nowTime_textView = findViewById(R.id.nowTime_textView);
        nowOrganizer_textView = findViewById(R.id.nowOrganizer_textView);
        nowTemplate_textView = findViewById(R.id.nowTemplate_textView);
        nowDescription_textView = findViewById(R.id.nowDescription_textView);
        nowNorms_textView = findViewById(R.id.nowNorms_textView);
        nowLocation_textView = findViewById(R.id.nowLocation_textView);
        now_imageView = findViewById(R.id.now_imageView);
        now_coordinatorLayout = findViewById(R.id.now_coordinatorLayout);
        nowState_textView = findViewById(R.id.nowState_textView);
        nowMode_textView = findViewById(R.id.nowMode_textView);
        now_progressIndicator = findViewById(R.id.now_progressIndicator);
        nowDownloadMap_extendedFab = findViewById(R.id.nowDownloadMap_extendedFab);
        nowMap_button = findViewById(R.id.nowMap_button);

        // set the toolbar
        toolbar = findViewById(R.id.now_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        switch (activityTime) {
            case PAST:
                getSupportActionBar().setTitle("Actividad terminada");
                break;
            case ONGOING:
                getSupportActionBar().setTitle("Actividad en curso");
                break;
            case FUTURE:
                getSupportActionBar().setTitle("Actividad prevista");
                break;
            default:
                break;
        }

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // location services initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // set intent to location foreground service
        locationServiceIntent = new Intent(this, LocationService.class);

        // get the current user's ID
        userID = mAuth.getCurrentUser().getUid();


        recuperarDatosScore();

    }

    private void updateUIOrganizerMap() {
        Intent intent = new Intent(NowActivity.this, OrganizerMapActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void updateUIParticipants() {
        Intent intent = new Intent(NowActivity.this, ParticipantsListActivity.class);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void updateUIMap() {
        Intent intent = new Intent(NowActivity.this, MapActivity.class);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void updateUIHome() {
        Intent intent = new Intent(NowActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIMyParticipation() {
        Intent intent = new Intent(NowActivity.this, MyParticipationActivity.class);
        intent.putExtra("participation", participation);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void showSnackBar(String message) {
        if (now_coordinatorLayout != null) {
            Snackbar.make(now_coordinatorLayout, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Do nothing, just dismiss
                        }
                    })
                    .setDuration(8000)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    havePermissions = true;
                    Toast.makeText(this, "Ahora ya puedes usar toda la funcionalidad", Toast.LENGTH_SHORT).show();
                } else {
                    showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (activity != null && userID != null) {
            if (!activity.getPlanner_id().equals(userID)) {
                getMenuInflater().inflate(R.menu.now_overflow_menu, menu);
                // check if we have to enable the abandon activity option
                Date current_time = new Date(System.currentTimeMillis());
                if (!current_time.after(activity.getStartTime())
                        || !current_time.before(activity.getFinishTime())) {
                    menu.getItem(1).setEnabled(false);
                    menu.getItem(1).setVisible(false);
                } else {
                    menu.getItem(1).setEnabled(true);
                    menu.getItem(1).setVisible(true);
                }
            } else {
                getMenuInflater().inflate(R.menu.now_overflow_organizer_menu, menu);
            }
        } else {
            Toast.makeText(this, "Se produjo un error al carga las opciones del menú", Toast.LENGTH_SHORT).show();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (activity != null && userID != null) {
            if (!activity.getPlanner_id().equals(userID)
                    && participation != null) {
                switch (item.getItemId()) {
                    case R.id.participation_activity:
                        updateUIMyParticipation();
                        break;
                    case R.id.quit_activity:
                        abandonActivity();
                        break;
                    default:
                        break;
                }
            } else if (activity.getPlanner_id().equals(userID)) {
                switch (item.getItemId()) {
                    case R.id.organizer_remove_activity:
                        removeActivity();
                        break;
                    default:
                        break;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void abandonActivity() {
        new MaterialAlertDialogBuilder(NowActivity.this)
                .setTitle("Abandonar actividad")
                .setMessage("¿Estás seguro/a de que quieres abandonar esta actividad en curso?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Date current_time = new Date(System.currentTimeMillis());
                        if (current_time.after(activity.getStartTime())
                                && current_time.before(activity.getFinishTime())
                                && participation != null) {
                            switch (participation.getState()) {
                                case NOT_YET:
                                    new MaterialAlertDialogBuilder(NowActivity.this)
                                            .setTitle("La acción no se puede realizar")
                                            .setMessage("Tu participación aún no ha comenzado. Si no quieres " +
                                                    "tomar parte de la actividad, puedes desinscribirte en " +
                                                    "Mi Participación")
                                            .setPositiveButton("OK", null)
                                            .show();
                                    break;
                                case NOW:
                                    now_progressIndicator.setVisibility(View.VISIBLE);

                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .update("state", ParticipationState.FINISHED,
                                                    "finishTime", current_time,
                                                    "completed", false)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    now_progressIndicator.setVisibility(View.GONE);
                                                    // once updated the document of the participation, check
                                                    // if we also need to finish the service
                                                    if (LocationService.executing) {
                                                        stopService(new Intent(NowActivity.this, LocationService.class));
                                                    }
                                                    showSnackBar("Has abandonado la actividad.");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    now_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(NowActivity.this, "Algo salió mal al terminar la actividad. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    break;
                                case FINISHED:
                                    Toast.makeText(NowActivity.this, "La acción no se pudo completar" +
                                            " porque ya has terminado tu participación", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } else {
                            // the activity is not on going any more
                            Toast.makeText(NowActivity.this, "La acción no se pudo completar. ", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void removeActivity() {
        new MaterialAlertDialogBuilder(NowActivity.this)
                .setTitle("Eliminar actividad")
                .setMessage("Se borrará la actividad y todos sus datos. Los participantes tampoco " +
                        "podrán acceder a ella. ¿Estás seguro/a de que quieres continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        now_progressIndicator.setVisibility(View.VISIBLE);
                        if (activity.getId() != null) {

                            db.collection("activities").document(activity.getId())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            now_progressIndicator.setVisibility(View.GONE);
                                            updateUIHome();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull @NotNull Exception e) {
                                            now_progressIndicator.setVisibility(View.GONE);
                                            Toast.makeText(NowActivity.this, "Se produjo un error al eliminar la actividad", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(NowActivity.this, "No se pudo eliminar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private ActivityTime getActivityTime() {
        ActivityTime activityTime = ActivityTime.FUTURE;
        if (activity != null) {
            Date current_date = new Date(System.currentTimeMillis());
            if (activity.getStartTime().after(current_date)) {
                activityTime = ActivityTime.FUTURE;
            } else if (activity.getFinishTime().before(current_date)) {
                activityTime = ActivityTime.PAST;
            } else {
                activityTime = ActivityTime.ONGOING;
            }
        }
        return activityTime;
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

    private void enableRightParticipantOptions() {
        switch (participation.getState()) {
            case NOT_YET:
                nowState_textView.setText("Estado: no comenzada");
                if (activityTime == ActivityTime.ONGOING) {
                    nowParticipant_extendedFab.setEnabled(true);
                    nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                    nowParticipant_extendedFab.setText("Comenzar");
                }
                break;
            case NOW:
                nowState_textView.setText("Estado: aún no terminada");
                // allow to click the see map button
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                if (!LocationService.executing) {
                    if (activityTime == ActivityTime.ONGOING) {
                        nowParticipant_extendedFab.setEnabled(true);
                        nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                        nowParticipant_extendedFab.setText("Continuar");
                    }
                }
                break;
            case FINISHED:
                nowState_textView.setText("Estado: terminada");
                // allow to click the see map button
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                nowParticipant_extendedFab.setEnabled(false);
                nowParticipant_extendedFab.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }



    public void recuperarDatosScore() {

        /*
         * SELECT ?norms ?location ?description WHERE{
         *   ?beacon
         *       ot:scorePartof ?activity.
         *    ?activity
         *       rdf:ID activity.getId()
         *       ot:norms ?norms;
         *       ot:locatedIn ?map;
         *       rdfs:comment ?description.
         *    ?map
         *       ot:location ?location.
         * } ORDER BY DESC (?score)
         */

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+?norms+?location+?description+WHERE{+?beacon+ot:scorePartof+?activity.+?activity+rdf:ID+\"" + activity.getId() + "\";+ot:norms+?norms;+ot:locatedIn+?map;+rdfs:comment+?description.+?map+ot:location+?location.+}+ORDER+BY+DESC(?score)&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            if (result.length() > 0) {
                                activity.setScore(true);

                                JSONObject aux = result.getJSONObject(0);
                                String norms = aux.getJSONObject("norms").getString("value");
                                String description = aux.getJSONObject("description").getString("value");
                                String location = aux.getJSONObject("location").getString("value");
                                activity.setNorms(norms);
                                activity.setDescription(description);
                                activity.setLocation(location);
                                activity.setBeaconSize(result.length());
                                siguiente();

                            } else {
                                recuperarDatosLineal();
                            }


                        } catch (JSONException e) {
                            Log.d("TAG", "norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", "norespone");

                    }
                });
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void siguiente() {
        if (activity != null) {
            organizerID = activity.getPlanner_id();
            if (organizerID.equals(userID)
                    && !activity.getParticipants().contains(userID)) {
                // if the current user is the organizer
                isOrganizer = true;
            } else if (!organizerID.equals(userID)
                    && activity.getParticipants().contains(userID)) {
                // if the current user is a participant...
                isOrganizer = false;
            } else {
                Toast.makeText(this, "Algo salió mal al " +
                        "obtener el organizador de la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                return;
            }
            // if the activity is not null, set the UI, otherwise tell the user and do nothing
            nowTitle_textView.setText(activity.getName());
            if (activity.isScore()) {
                nowMode_textView.append("score");
            } else {
                nowMode_textView.append("orientación clásica");
            }
            String timeString = "";
            // append start and finish hours
            timeString = timeString + df_hour.format(activity.getStartTime()) + " - " +
                    df_hour.format(activity.getFinishTime());
            // append date
            timeString = timeString + " (" + df_date.format(activity.getStartTime()) + ")";
            nowTime_textView.setText(timeString);
            // get and set the activity image

            Glide.with(this)
                    .load(activity.getImage())
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(now_imageView);
            // get the template of the activity


            // get the data from the template
            nowType_textView.setText("");

            nowDescription_textView.setText(activity.getDescription());
            nowTemplate_textView.append(activity.getName());
            nowLocation_textView.append(activity.getLocation());
            nowNorms_textView.setText(activity.getNorms());
            // now that we have all the data from both the activity and the template, perform specific
            // actions depending on whether the user is the organizer or a participant
            if (isOrganizer) {
                // if organizer:
                // 1) disable options that are in any case only for participants
                nowParticipant_extendedFab.setEnabled(false);
                nowParticipant_extendedFab.setVisibility(View.GONE);
                nowState_textView.setVisibility(View.GONE);
                // 2) enable options that are in any case enabled for organizer
                // always enable the button to see the credentials
                // always enable see map button
                nowCredentials_button.setEnabled(true);
                nowCredentials_button.setVisibility(View.VISIBLE);
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                // 2.1) check if we need to change the text of the see participants FAB

                // always enable the see participants FAB
                nowSeeParticipants_extendedFab.setEnabled(true);
                nowSeeParticipants_extendedFab.setVisibility(View.VISIBLE);
                continuar();
            } else {
                // if participant:
                // 1) disable organizer options
                nowCredentials_button.setEnabled(false);
                nowCredentials_button.setVisibility(View.GONE);
                // enable or disable FABS depending on the time
                switch (activityTime) {
                    case PAST:
                        nowParticipant_extendedFab.setEnabled(false);
                        nowParticipant_extendedFab.setVisibility(View.GONE);
                        nowState_textView.setVisibility(View.GONE);
                        break;
                    case ONGOING:
                        nowSeeParticipants_extendedFab.setEnabled(false);
                        nowSeeParticipants_extendedFab.setVisibility(View.GONE);
                        break;
                    case FUTURE:
                        nowSeeParticipants_extendedFab.setEnabled(false);
                        nowSeeParticipants_extendedFab.setVisibility(View.GONE);
                        nowParticipant_extendedFab.setEnabled(false);
                        nowParticipant_extendedFab.setVisibility(View.GONE);
                        nowState_textView.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
                // 2) set listener to the participations collection to know which participant options
                // should be enabled




                /*
                 * SELECT DISTINCT ?state ?time ?completed WHERE{
                 *  ?activity
                 *    rdf:ID activity.getID().
                 *  ?track
                 *    ot:from ?activity;
                 *    ot:trackState ?state;
                 *    ot:completed ?completed;
                 *    ot:composedBy ?point.
                 *  ?point
                 *    ot:time ?time
                 * } ORDER BY ASC(?time)
                 */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fstate+%3Ftime+%3Fcompleted+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Ftrack%0D%0Aot%3Afrom+%3Factivity%3B%0D%0Aot%3AtrackState+%3Fstate%3B%0D%0Aot%3Acompleted+%3Fcompleted%3B%0D%0Aot%3AcomposedBy+%3Fpoint.%0D%0A%3Fpoint%0D%0Aot%3Atime+%3Ftime.%0D%0A%7D+ORDER+BY+ASC%28%3Ftime%29+&format=json";
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                    ParticipationState pstate = ParticipationState.FINISHED;
                                    participation = new Participation();
                                    participation.setParticipant(userID);
                                    if (result.length() == 0) {
                                        getNotStartedParticipation();
                                    } else {
                                        for (int i = 0; i < result.length(); i++) {
                                            JSONObject aux = result.getJSONObject(i);
                                            if (i == 0) {
                                                String start = aux.getJSONObject("time").getString("value");
                                                String state = aux.getJSONObject("state").getString("value");
                                                String completed = aux.getJSONObject("completed").getString("value");
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
                                                participation.setCompleted(completed.equals("1"));
                                                participation.setStartTime((Date.from(ZonedDateTime.parse((start + "[Europe/Madrid]")).toInstant())));

                                            } else if (i == result.length() - 1 && participation.getState().equals(ParticipationState.FINISHED)) {
                                                String end = aux.getJSONObject("time").getString("value");
                                                participation.setFinishTime((Date.from(ZonedDateTime.parse((end + "[Europe/Madrid]")).toInstant())));
                                            }
                                        }
                                    }
                                    if (participation != null) {
                                        if (activityTime == ActivityTime.ONGOING) {
                                            if (Utilities.mapDownloaded(activity,getApplicationContext())) {
                                                // if map already downloaded
                                                enableRightParticipantOptions();
                                            } else {
                                                // if map not yet downloaded
                                                // we only enable the option of downloading the map
                                                nowDownloadMap_extendedFab.setEnabled(true);
                                                nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                                switch (participation.getState()) {
                                                    case NOT_YET:
                                                        nowState_textView.setText("Estado: no comenzada");
                                                        break;
                                                    case NOW:
                                                        nowState_textView.setText("Estado: aún no terminada");
                                                        break;
                                                    case FINISHED:
                                                        nowState_textView.setText("Estado: terminada");
                                                        break;
                                                }

                                            }
                                        } else {
                                            enableRightParticipantOptions();
                                        }
                                    } else {
                                        Toast.makeText(NowActivity.this, "Algo salió mal al obtener la participación. " +
                                                "Sal y vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                    }
                                    continuar();
                                } catch (JSONException e) {
                                    Log.d("TAG", "norespone");
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {


                            }
                        });
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);


                // in order to track location we have to check if we have at least one of the following permissions...
                // so if the user is a participant we make this checking
                if (ActivityCompat.checkSelfPermission(NowActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(NowActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // if we don't...
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    // if we do...
                    havePermissions = true;
                }
            }
            // get the organizer for we need his/her name and surname
            db.collection("users").

                    document(organizerID)
                            .

                    get()
                            .

                    addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            user = documentSnapshot.toObject(User.class);
                            nowOrganizer_textView.append(user.getName() + " " + user.getSurname());
                        }
                    });


        } else {
            continuar();
        }

    }

    private void getNotStartedParticipation() {
        /*
         * SELECT DISTINCT ?state ?completed WHERE{
         *   ?activity
         *       rdf:ID activity.getId().
         *   ?track
         *       ot:from ?activity;
         *       ot:trackState ?state;
         *       ot:completed ?completed.
         *  }ORDER BY(?time)
         */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fstate+%3Fcompleted+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Ftrack%0D%0Aot%3Afrom+%3Factivity%3B%0D%0Aot%3AtrackState+%3Fstate%3B%0D%0Aot%3Acompleted+%3Fcompleted.%0D%0A%7D+ORDER+BY+ASC%28%3Ftime%29+&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            ParticipationState pstate = ParticipationState.FINISHED;
                            participation = new Participation();
                            participation.setParticipant(userID);
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                if (i == 0) {
                                    String state = aux.getJSONObject("state").getString("value");
                                    String completed = aux.getJSONObject("completed").getString("value");
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
                                    participation.setCompleted(completed.equals("1"));


                                }
                            }
                        } catch (JSONException e) {
                            Log.d("TAG", "norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {


                    }
                });
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

    }

    public void continuar() {

        // participant FAB listener
        nowParticipant_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (havePermissions) {
                    // if the user has given the permissions required...
                    // check that the activity has not finished yet (it could have, in the meantime while
                    // the user was reading the rules, for example)
                    Date current_time = new Date(System.currentTimeMillis());
                    if (current_time.before(activity.getFinishTime())) {
                        // if the activity has not yet finished
                        new MaterialAlertDialogBuilder(NowActivity.this)
                                .setMessage("¿Deseas comenzar/retomar la actividad? Solo deberías " +
                                        "hacerlo si el/la organizador/a ya te ha dado la salida")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Comenzar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        now_progressIndicator.setVisibility(View.VISIBLE);
                                        // 1) get location
                                        try {
                                            fusedLocationClient.getLastLocation()
                                                    .addOnSuccessListener(NowActivity.this, new OnSuccessListener<Location>() {
                                                        @Override
                                                        public void onSuccess(Location location) {
                                                            String score = "scorePartof";
                                                            if (!activity.isScore()) {
                                                                score = "linealPartOf";
                                                            }

                                                            /*
                                                             * SELECT DISTINCT ?beaconId ?latitude ?longitude WHERE {
                                                             *   ?activity
                                                             *       rdf:ID activity.getId();
                                                             *       ot:startPoint ?start.
                                                             *   ?beacon
                                                             *       rdf:ID ?beaconId;
                                                             *       ot:score(scorePartof/linearPartof) ?activity.
                                                             *   ?start
                                                             *       geo:lat ?latitude;
                                                             *       geo:long ?longitude.
                                                             * }
                                                             *
                                                             * */

                                                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+?beaconId+%3Flatitude+%3Flongitude+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22%3B%0D%0Aot%3AstartPoint+%3Fstart.+?beacon+rdf:ID+?beaconId;+ot:" + score + "+?activity.%0D%0A%3Fstart%0D%0Ageo%3Alat+%3Flatitude%3B%0D%0Ageo%3Along+%3Flongitude.%0D%0A%7D+&format=&format=json";

                                                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                                        @Override
                                                                        public void onResponse(JSONObject response) {
                                                                            try {
                                                                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                                                                if (result.length() > 0) {
                                                                                    JSONObject aux = result.getJSONObject(0);
                                                                                    Double startLat = aux.getJSONObject("latitude").getDouble("value");
                                                                                    Double startLong = aux.getJSONObject("longitude").getDouble("value");
                                                                                    activity.setStart_lat(startLat);
                                                                                    activity.setStart_lng(startLong);
                                                                                    activity.setBeaconSize(result.length());
                                                                                }
                                                                                if (location != null) {
                                                                                    // 2) check that we are close to the start spot or that we already started the activity
                                                                                    // (in such case, we don't have to check that we are at the start spot)
                                                                                    float diff = getDistance(location.getLatitude(), activity.getStart_lat(),
                                                                                            location.getLongitude(), activity.getStart_lng());

                                                                                    if ((diff <= LOCATION_PRECISION
                                                                                            && participation.getState() == ParticipationState.NOT_YET)
                                                                                            || participation.getState() == ParticipationState.NOW) {
                                                                                        // 3) if we are near enough, or if we had already started, continue to charge the map
                                                                                        if (!LocationService.executing) {
                                                                                            // now we have to do different things depending on whether the participation
                                                                                            // is at NOT_YET or at NOW
                                                                                            switch (participation.getState()) {
                                                                                                case NOT_YET:
                                                                                                    // get current time

                                                                                                    // update the start time

                                                                                                    String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Ftrack+WHERE%7B%0D%0A%3Ftrack%0D%0A+ot%3AbelongsTo+%3Fperson%3B%0D%0A+ot%3Afrom+%3Factivity.+%0D%0A%3Factivity%0D%0A+rdf%3AID+\"" + activity.getId() + "\".%0D%0A%3Fperson%0D%0A+ot%3AuserName+\"" + userID + "\".%0D%0A%7D&format=json";

                                                                                                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                                                                                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                                                                                @Override
                                                                                                                public void onResponse(JSONObject response) {
                                                                                                                    try {
                                                                                                                        JSONObject result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0);
                                                                                                                        String trackID = result.getJSONObject("track").getString("value").split("#")[1];
                                                                                                                        String pointID = UUID.randomUUID().toString();
                                                                                                                        String fecha = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z")).toString();

                                                                                                                        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA+%7B%0D%0AGRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0Aot%3A" + pointID + "+geo%3Along+" + activity.getStart_lng() + "%3B%0D%0Ageo%3Alat+" + activity.getStart_lat() + "%3B%0D%0Aot%3Atime+%3C" + fecha + "%3E.%0D%0Aot:" + trackID + "+ot%3AcomposedBy+ot%3A" + pointID + ";+ot:trackState+\"NOW\"+.%0D%0A%7D%7D%0D%0A%0D%0A&format=json";

                                                                                                                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                                                                                                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                                                                                                                    @Override
                                                                                                                                    public void onResponse(JSONObject response) {
                                                                                                                                        try {
                                                                                                                                            String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                                                                                                                                            if (result.equals("Insert into <http://localhost:8890/DAV>, 5 (or less) triples -- done")) {
                                                                                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                                                                                // hide the button
                                                                                                                                                nowParticipant_extendedFab.setEnabled(false);
                                                                                                                                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                                                                                // start service
                                                                                                                                                locationServiceIntent.putExtra("activity", activity);
                                                                                                                                                locationServiceIntent.putExtra("template", template);
                                                                                                                                                startService(locationServiceIntent);
                                                                                                                                                // enable see map button (just in case that the user wants
                                                                                                                                                // to go back and forth between this and the map activity)
                                                                                                                                                participation.setState(ParticipationState.NOW);
                                                                                                                                                nowMap_button.setEnabled(true);
                                                                                                                                                nowMap_button.setVisibility(View.VISIBLE);
                                                                                                                                                deletePreviousState(trackID);
                                                                                                                                                // update UI
                                                                                                                                                updateUIMap();
                                                                                                                                            } else {
                                                                                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                                                                                showSnackBar("Error al comenzar la actividad. Inténtalo de nuevo.");
                                                                                                                                            }


                                                                                                                                        } catch (JSONException e) {
                                                                                                                                            Log.d("TAG", "norespone");
                                                                                                                                            e.printStackTrace();
                                                                                                                                        }

                                                                                                                                    }
                                                                                                                                }, new Response.ErrorListener() {

                                                                                                                                    @Override
                                                                                                                                    public void onErrorResponse(VolleyError error) {
                                                                                                                                        Log.d("TAG", "norespone");

                                                                                                                                    }
                                                                                                                                });
                                                                                                                        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

                                                                                                                    } catch (JSONException e) {
                                                                                                                        Log.d("TAG", "norespone");
                                                                                                                        e.printStackTrace();
                                                                                                                    }

                                                                                                                }
                                                                                                            }, new Response.ErrorListener() {

                                                                                                                @Override
                                                                                                                public void onErrorResponse(VolleyError error) {
                                                                                                                    Log.d("TAG", "norespone");

                                                                                                                }
                                                                                                            });
                                                                                                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                                                                                                    break;
                                                                                                case NOW:
                                                                                                    now_progressIndicator.setVisibility(View.GONE);
                                                                                                    // hide button
                                                                                                    nowParticipant_extendedFab.setEnabled(false);
                                                                                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                                    // start service
                                                                                                    locationServiceIntent.putExtra("activity", activity);
                                                                                                    locationServiceIntent.putExtra("template", template);
                                                                                                    startService(locationServiceIntent);
                                                                                                    // update UI
                                                                                                    updateUIMap();
                                                                                                    break;
                                                                                                default:
                                                                                                    now_progressIndicator.setVisibility(View.GONE);
                                                                                                    Toast.makeText(NowActivity.this, "Parece que la actividad ya ha terminado", Toast.LENGTH_SHORT).show();
                                                                                                    break;
                                                                                            }
                                                                                        } else {
                                                                                            now_progressIndicator.setVisibility(View.GONE);
                                                                                            Toast.makeText(NowActivity.this, "No se pudo iniciar la actividad... ya hay un servicio ejecutándose", Toast.LENGTH_SHORT).show();
                                                                                        }
                                                                                    } else {
                                                                                        // too far from the start spot
                                                                                        now_progressIndicator.setVisibility(View.GONE);
                                                                                        int diff_meters = (int) diff;
                                                                                        showSnackBar("Estás demasiado lejos de la salida (" + diff_meters + "). Acércate" +
                                                                                                " a ella y vuelve a intentarlo");
                                                                                    }
                                                                                } else {
                                                                                    now_progressIndicator.setVisibility(View.GONE);
                                                                                    Toast.makeText(NowActivity.this, "Hubo algún problema al obtener la ubicación. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                                                }

                                                                            } catch (JSONException e) {
                                                                                Log.d("TAG", "norespone");
                                                                                e.printStackTrace();
                                                                            }

                                                                        }
                                                                    }, new Response.ErrorListener() {

                                                                        @Override
                                                                        public void onErrorResponse(VolleyError error) {
                                                                            Log.d("TAG", "norespone");

                                                                        }
                                                                    });
                                                            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

                                                        }
                                                    });
                                        } catch (
                                                SecurityException e) {
                                            showSnackBar("Parece que hay algún problema con los permisos de ubicación");
                                        }
                                    }
                                })
                                .show();
                    } else {
                        // if the activity has already finished
                        nowParticipant_extendedFab.setEnabled(false);
                        nowParticipant_extendedFab.setVisibility(View.GONE);
                        showSnackBar("Esta actividad ya ha terminado");
                    }
                } else {
                    // if we don't have the permissions we ask for them
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        });

        // organizer FAB listener
        nowSeeParticipants_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    updateUIParticipants();
                } else {
                    Toast.makeText(NowActivity.this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // download map listener
        nowDownloadMap_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog pd = new ProgressDialog(NowActivity.this);
                pd.setTitle("Cargando el mapa...");
                pd.show();
                //

                /*
                 *
                 * SELECT ?image WHERE {
                 *   ?activity
                 *       rdf:ID activity.getId();
                 *       ot:locatedIn ?map.
                 *   ?map
                 *       schema:image ?image.
                 * }
                 * */

                String url = "http://192.168.137.1:8890/sparql?query=SELECT+?image+WHERE+{+?activity+rdf:ID+\"" + activity.getId() + "\";+ot:locatedIn+?map.+?map+schema:image+?image.+}&format=json";

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                                    for (int i = 0; i < result.length(); i++) {
                                        JSONObject aux = result.getJSONObject(i);
                                        String image = aux.getJSONObject("image").getString("value");
                                        URL url = new URL(image);

                                        InputStream inputStream = url.openStream();
                                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                        ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                        File filePath = new File(directory, activity.getId() + ".png"); //Cambiar a ID activity
                                        OutputStream outputStream = new FileOutputStream(filePath);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                        outputStream.flush();
                                        outputStream.close();

                                        nowDownloadMap_extendedFab.setVisibility(View.GONE);
                                        nowDownloadMap_extendedFab.setEnabled(false);
                                        enableRightParticipantOptions();
                                        pd.dismiss();
                                    }

                                } catch (JSONException | IOException e) {
                                    Log.d("TAG", "norespone");
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("TAG", "norespone");

                            }
                        });
                MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);

            }
        });

        // see map button listener
        nowMap_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.getPlanner_id().equals(userID)) {
                    // if current user is the organizer
                    if (Utilities.mapDownloaded(activity,getApplicationContext())) {
                        // if the map is already downloaded
                        updateUIOrganizerMap();
                    } else {
                        // if the map is not yet downloaded
                        final ProgressDialog pd = new ProgressDialog(NowActivity.this);
                        pd.setTitle("Cargando el mapa...");
                        pd.show();

                        /*
                         *
                         * SELECT ?image WHERE {
                         *   ?activity
                         *       rdf:ID activity.getId();
                         *       ot:locatedIn ?map.
                         *   ?map
                         *       schema:image ?image.
                         * }
                         * */

                        String url = "http://192.168.137.1:8890/sparql?query=SELECT+?image+WHERE+{+?activity+rdf:ID+\"" + activity.getId() + "\";+ot:locatedIn+?map.+?map+schema:image+?image.+}&format=json";


                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                                            for (int i = 0; i < result.length(); i++) {
                                                JSONObject aux = result.getJSONObject(i);
                                                String image = aux.getJSONObject("image").getString("value");
                                                URL url = new URL(image);

                                                InputStream inputStream = url.openStream();
                                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                                ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                                File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                                File filePath = new File(directory, activity.getId() + ".png"); //Cambiar a ID activity
                                                OutputStream outputStream = new FileOutputStream(filePath);
                                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                                outputStream.flush();
                                                outputStream.close();

                                                updateUIOrganizerMap();
                                                pd.dismiss();
                                            }

                                        } catch (JSONException | IOException e) {
                                            Log.d("TAG", "norespone");
                                            e.printStackTrace();
                                        }

                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("TAG", "norespone");

                                    }
                                });
                        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
                    }
                } else if (activity.getParticipants().contains(userID)) {
                    // if current user is a participant
                    switch (participation.getState()) {
                        case NOT_YET:
                            break;
                        case NOW:
                            if (!LocationService.executing) {
                                // if the service is not being executed now, show dialog to alert
                                new MaterialAlertDialogBuilder(NowActivity.this)
                                        .setTitle("Aviso sobre el mapa")
                                        .setMessage("Esta acción te mostrará el mapa de la actividad, pero " +
                                                "el servicio que rastrea tu ubicación no está activo, por lo que" +
                                                " no se registrará tu paso por las balizas. Si lo que quieres es retomar " +
                                                "la actividad, cancela esta acción y pulsa el botón de Continuar")
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton("Ver mapa", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Utilities.mapDownloaded(activity,getApplicationContext())) {
                                                    updateUIMap();
                                                } else {
                                                    // if for some reason the map is not downloaded then
                                                    // show the download button instead and warn the user
                                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                                    nowParticipant_extendedFab.setEnabled(false);
                                                    nowDownloadMap_extendedFab.setEnabled(true);
                                                    nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                                    showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                                    nowMap_button.setVisibility(View.GONE);
                                                    nowMap_button.setEnabled(false);
                                                }
                                            }
                                        })
                                        .show();
                            } else {
                                // if the service is being executed now
                                if (Utilities.mapDownloaded(activity,getApplicationContext())) {
                                    updateUIMap();
                                } else {
                                    // if for some reason the map is not downloaded then
                                    // show the download button instead and warn the user
                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                    nowParticipant_extendedFab.setEnabled(false);
                                    nowDownloadMap_extendedFab.setEnabled(true);
                                    nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                    showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                    nowMap_button.setVisibility(View.GONE);
                                    nowMap_button.setEnabled(false);
                                }
                            }
                            break;
                        case FINISHED:
                            if (Utilities.mapDownloaded(activity,getApplicationContext())) {
                                updateUIMap();
                            } else {
                                // if for some reason the map is not downloaded then
                                // show the download button instead and warn the user
                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                nowParticipant_extendedFab.setEnabled(false);
                                nowDownloadMap_extendedFab.setEnabled(true);
                                nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                nowMap_button.setVisibility(View.GONE);
                                nowMap_button.setEnabled(false);
                            }
                            break;
                    }
                }
            }
        });

        // credentials button listener
        nowCredentials_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(NowActivity.this)
                        .setTitle("Claves de acceso a la actividad")
                        .setMessage("Identificador: " + activity.getId() +
                                "\nContraseña: " + activity.getKey())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void deletePreviousState(String trackID) {
        /*
         * DELETE DATA {
         *   GRAPH <http:localhost:8890/DAV> {
         *     ot:trackID
         *          ot:trackState "NOT_YET".
         *   }
         * }
         */
        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=DELETE+DATA+%7B%0D%0A+GRAPH+<http%3A//localhost%3A8890%2FDAV>+%7B%0D%0A+ot%3A" + trackID + "%0D%0A+ot%3AtrackState+\"NOT_YET\".%0D%0A++%7D+%0D%0A%7D+&format=json";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("callret-0").getString("value");
                            if (result.equals("Delete from <http://localhost:8890/DAV>, 1 (or less) triples -- done")) {
                                Log.d(TAG, "Actualizado el estado");
                            }

                        } catch (JSONException e) {
                            Log.d("TAG", "norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", "norespone");

                    }
                });
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    public void recuperarDatosLineal() {

        /*
         * SELECT ?norms ?location ?description WHERE {
         *   ?beacon
         *       ot:linealPartOf ?activity.
         *   ?activity
         *       rdf:ID  activity.getId();
         *       ot:norms ?norms;
         *       ot:locatedIn ?map;
         *       rdfs:comment ?description.
         *   ?map
         *      ot:location ?location.
         * } ORDER BY DESC(?lineal)
         *
         * */

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+?norms+?location+?description+WHERE{+?beacon+ot:linealPartOf+?activity.+?activity+rdf:ID+\"" + activity.getId() + "\";+ot:norms+?norms;+ot:locatedIn+?map;+rdfs:comment+?description.+?map+ot:location+?location.+}+ORDER+BY+DESC(?lineal)+&format=json";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            if (result.length() > 0) {
                                activity.setScore(false);

                                JSONObject aux = result.getJSONObject(0);
                                String norms = aux.getJSONObject("norms").getString("value");
                                String description = aux.getJSONObject("description").getString("value");
                                String location = aux.getJSONObject("location").getString("value");
                                activity.setNorms(norms);
                                activity.setDescription(description);
                                activity.setLocation(location);
                                activity.setBeaconSize(result.length());
                                siguiente();

                            }
                        } catch (JSONException e) {
                            Log.d("TAG", "norespone");
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG", "norespone");

                    }
                });
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }
}
