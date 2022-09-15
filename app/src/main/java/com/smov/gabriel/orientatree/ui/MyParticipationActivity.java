package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;

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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

public class MyParticipationActivity extends AppCompatActivity {

    // UI elements
    private Toolbar toolbar;
    private TextView myParticipationStart_textView, myParticipationFinish_textView,
            myParticipationTotal_textView, myParticipationBeacons_textView,
            myParticipationCompleted_textView;
    private MaterialButton myParticipationBeacons_button, myParticipationTrack_button,
            myParticipationInscription_button, myParticipationDelete_button;
    private CircularProgressIndicator myParticipation_progressIndicator;

    // model objects
    private Participation participation;
    private ActivityLOD activity;
    private ArrayList<BeaconReached> reaches;

    // useful IDs
    private String userID;
    private String activityID;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm:ss";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_participation);

        // get the intent
        Intent intent = getIntent();
        participation = (Participation) intent.getSerializableExtra("participation");
        activity = (ActivityLOD) intent.getSerializableExtra("activity");

        // binding UI elements
        toolbar = findViewById(R.id.myParticipation_toolbar);
        myParticipationStart_textView = findViewById(R.id.myParticipationStart_textView);
        myParticipationFinish_textView = findViewById(R.id.myParticipationFinish_textView);
        myParticipationTotal_textView = findViewById(R.id.myParticipationTotal_textView);
        myParticipationBeacons_textView = findViewById(R.id.myParticipationBeacons_textView);
        myParticipationTrack_button = findViewById(R.id.myParticipationTrack_button);
        myParticipationBeacons_button = findViewById(R.id.myParticipationBeacons_button);
        myParticipationDelete_button = findViewById(R.id.myParticipationDelete_button);
        myParticipationInscription_button = findViewById(R.id.myParticipationInscription_button);
        myParticipation_progressIndicator = findViewById(R.id.myParticipation_progressIndicator);
        myParticipationCompleted_textView = findViewById(R.id.myParticipationCompleted_textView);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // setting useful IDs
        userID = mAuth.getCurrentUser().getUid();
        activityID = activity.getId();

        // setting the AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // setting the info
        // check that we received properly the participation and the activity


        if (participation != null && activityID != null) {
            // check if it has already started
            if (participation.getState() != ParticipationState.NOT_YET) {
                Date start_time = participation.getStartTime();
                Date finish_time = participation.getFinishTime();
                if (start_time != null) {
                    myParticipationStart_textView.setText(df_hour.format(start_time));
                } else {
                    myParticipationStart_textView.setText("Nada que mostrar");
                }
                if (finish_time != null) {
                    myParticipationFinish_textView.setText(df_hour.format(finish_time));
                } else {
                    myParticipationFinish_textView.setText("Nada que mostrar");
                }
                if ((start_time != null && finish_time != null)
                        && start_time.before(finish_time)) {
                    long diff_millis = Math.abs(finish_time.getTime() - start_time.getTime());
                    myParticipationTotal_textView.setText(formatMillis(diff_millis));
                } else {
                    myParticipationTotal_textView.setText("Nada que mostrar");
                }
                if (participation.isCompleted()) {
                    myParticipationCompleted_textView.setText("Sí");
                } else {
                    myParticipationCompleted_textView.setText("No");
                }
                // get the reaches
                reaches = new ArrayList<>();

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());


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
                 *
                 * */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fcorrectanswer+%3Fanswer+%3FbeaconID+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fbeacon%0D%0A++ot%3A"+score+"+%3Factivity%3B%0D%0A++rdf%3AID+%3FbeaconID%3B%0D%0A++ot%3Aabout+%3Fobject.%0D%0A%3Fpersonanswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3AanswerResource+%3Fanswer%3B%0D%0A+ot%3AanswerTime+%3Ftime%3B%0D%0A+ot%3Aof+%3Fperson.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + userID + "%22.%0D%0A+%3Fobjectproperty%0D%0A++ot%3ArelatedTo+%3Fobject%3B%0D%0A++ot%3Aanswer+%3Fcorrectanswer.%0D%0A%7D&format=json";

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    ArrayList<BeaconReachedLOD> reaches = new ArrayList<>();
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
                                        reach.setReachMoment(Date.from(ZonedDateTime.parse((time + "[Europe/Madrid]")).toInstant()));
                                        reach.setBeacon_id(beaconID);
                                        reach.setWritten_answer(answer);

                                        //añadir reach
                                        reaches.add(reach);
                                    }
                                    if (activity.getBeaconSize() > 0) {
                                        int num_reaches = reaches.size();
                                        int number_of_beacons = activity.getBeaconSize();
                                        myParticipationBeacons_textView.setText(num_reaches + "/" + number_of_beacons);
                                    } else {
                                        Toast.makeText(MyParticipationActivity.this, "No se pudo recuperar la información de las balizas alcanzadas", Toast.LENGTH_SHORT).show();
                                    }


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
                // if the participation has not yet started
                // all fields empty
                myParticipationStart_textView.setText("Nada que mostrar");
                myParticipationFinish_textView.setText("Nada que mostrar");
                myParticipationTotal_textView.setText("Nada que mostrar");
                myParticipationBeacons_textView.setText("Nada que mostrar");
                // check if we should enable the button to cancel the inscription
                if (inscriptionCancelable()) {
                    myParticipationInscription_button.setEnabled(true);
                }
            }
            // check if we should allow the user to see the track
            if (participation.getState() == ParticipationState.FINISHED
                    || participation.getFinishTime() != null
                    || (activity.getFinishTime().before(new Date(System.currentTimeMillis()))
                    && participation.getStartTime() != null)) {
                // if the participation is finished or if there is a finish time or if the activity has finished
                // we enable the track button
                myParticipationTrack_button.setEnabled(true);
            } else {
                myParticipationTrack_button.setEnabled(false);
            }
        } else {
            // if we couldn't receive right the participation
            Toast.makeText(this, "Ocurrió un error al leer la información. Salga e inténtelo de nuevo", Toast.LENGTH_SHORT).show();
        }

        // beacons listener
        myParticipationBeacons_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches();
            }
        });

        // track listener
        myParticipationTrack_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null && participation != null) {
                    if (mapDownloaded()) {
                        // if we already have the map downloaded
                        updateUITrackMap();
                    } else {
                        // if the map is not yet downloaded
                        final ProgressDialog pd = new ProgressDialog(MyParticipationActivity.this);
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
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

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

                                                pd.dismiss();
                                                if (mapDownloaded()) {
                                                    updateUITrackMap();
                                                }
                                            }

                                        } catch (JSONException | IOException e) {
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
                }
            }
        });

        // here the second action should be performed by a cloud functions
        myParticipationInscription_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check that it is possible to cancel the inscription
                if (inscriptionCancelable()) {
                    new MaterialAlertDialogBuilder(MyParticipationActivity.this)
                            .setTitle("Eliminar mi inscripción")
                            .setTitle("¿Estás seguro/a de que quieres desinscribirte" +
                                    " de esta actividad?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    myParticipation_progressIndicator.setVisibility(View.VISIBLE);
                                    ArrayList<String> participants = activity.getParticipants();
                                    participants.remove(userID);
                                    // update the list with the participants in the activity
                                    db.collection("activities").document(activityID)
                                            .update("participants", participants)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    // updated the list, now
                                                    // remove the participation document
                                                    db.collection("activities").document(activityID)
                                                            .collection("participations").document(userID)
                                                            .delete()
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void unused) {
                                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                                    updateUIHome();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                                    // we couldn't remove the participation object
                                                                    // at this point we updated the participants list but did not remove the participation object
                                                                    // on the eyes of the user there would be no difference, the only problem is that the information
                                                                    // still occupies space in the database
                                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                                    updateUIHome();
                                                                }
                                                            });
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    // we couldn't update the activity
                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(MyParticipationActivity.this, "Algo falló al eliminar su subscripción, vuelva a intentarlo", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            })
                            .show();
                } else {
                    // if not cancelable any more
                    myParticipationInscription_button.setEnabled(false);
                    Toast.makeText(MyParticipationActivity.this, "No se pudo completar la acción." +
                            " La actividad está en curso o ya has comenzado tu participación", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void updateUITrackMap() {
        Intent intent = new Intent(MyParticipationActivity.this, TrackActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("participantID", participation.getParticipant());
        startActivity(intent);
    }

    private void updateUIReaches() {
        if (activity != null && userID != null
                && (userID.equals(participation.getParticipant()))) {
            Intent intent = new Intent(MyParticipationActivity.this, ReachesActivity.class);
            intent.putExtra("activity", activity);
            intent.putExtra("participation",participation);
            intent.putExtra("participation", participation);
            intent.putExtra("participantID", userID);
            startActivity(intent);
        }
    }

    private void updateUIHome() {
        Intent intent = new Intent(MyParticipationActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String time = hours % 24 + "h " + minutes % 60 + "m " + seconds % 60 + "s";
        return time;
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

    public boolean mapDownloaded() {
        boolean res = false;
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getId() + ".png");
        if (mypath.exists()) {
            res = true;
        }
        return res;
    }

    // allows us to know whether is possible to cancel a subscription in an activity or not
    // given that the participation must have not been yet started and that the
    // activity must be in the future
    private boolean inscriptionCancelable() {
        boolean res = false;
        // first we check that neither the activity nor the participation are null
        if (activity == null || participation == null) {
            // if any of the are null, we return false
            return res;
        } else {
            if (participation.getState() == ParticipationState.NOT_YET) {
                res = true;

            }
        }
        return res;
    }
}