package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ReachAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationLOD;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ReachesActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private RecyclerView reaches_recyclerView;
    private ArrayList<BeaconReachedLOD> reaches;
    private ReachAdapter reachAdapter;
    private ConstraintLayout emptyState_layout;
    private TextView emptyStateMessage_textView;
    private ExtendedFloatingActionButton reachesTrack_fab;

    private ActivityLOD activity;
    private Participation participation;

    private ReachesActivity reachesActivity;

    // useful ID strings
    private String activityID;
    private String userID; // currently logged user's ID

    private String participantID; // ID received within the intent
    // (only used if we are the organizer trying to see certain participant's information)

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaches);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        reachesActivity = this;

        reaches_recyclerView = findViewById(R.id.reaches_recyclerView);
        emptyState_layout = findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = findViewById(R.id.emptyStateMessage_textView);
        reachesTrack_fab = findViewById(R.id.reachesTrack_fab);

        // set the AppBar
        toolbar = findViewById(R.id.reaches_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get from the intent the activity
        Intent intent = getIntent();
        activity = (ActivityLOD) intent.getSerializableExtra("activity");
        participation = (Participation) intent.getSerializableExtra("participation");
        participantID = intent.getExtras().getString("participantID");

        // get current user id
        userID = mAuth.getCurrentUser().getUid();

        if (activity != null) {
            activityID = activity.getId();
            String participant_searched;
            if (activity.getPlanner_id().equals(userID)) {
                // if we are the organizer
                if (participantID != null) {
                    // we should have received from the intent the participant ID
                    participant_searched = participantID;
                    if (activity.getStartTime().before(new Date(System.currentTimeMillis()))) {
                        reachesTrack_fab.setEnabled(true);
                        reachesTrack_fab.setVisibility(View.VISIBLE);
                    }
                } else {
                    // if we haven't, finish and tell the user
                    Toast.makeText(reachesActivity, "Algo salió mal al mostrar las balizas", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // if we are not the organizer, then we are a user trying to watch its own beacons
                participant_searched = userID;
            }
            System.out.println("HASTA AQUI OK");

            ArrayList<BeaconReachedLOD> reaches = new ArrayList<>();

            String score = "scorePartof";
            if (!activity.isScore()) {
                score = "linealPartOf";
            }
            /*
             * SELECT DISTINCT ?beaconID ?time WHERE{
             *   ?activity
             *       rdf:ID activity.getId().
             *   ?beacon
             *       rdf:ID beaconID;
             *       ot:score(socrePartof/linealPartOf) ?activity.
             *   ?personAnswer
             *       ot:toThe ?beacon;
             *       ot:of ?person;
             *       ot:answerTime ?time.
             *   ?person
             *       ot:userName userID.
             * }
             *
             * */

            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FbeaconID+?time+WHERE%7B%0D%0A%3Factivity%0D%0Ardf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fbeacon%0D%0Ardf%3AID+%3FbeaconID%3B%0D%0Aot%3A" + score + "%3Factivity.%0D%0A%3FpersonAnswer%0D%0Aot%3AtoThe+%3Fbeacon%3B%0D%0Aot%3Aof+%3Fperson;+ot:answerTime+?time.%0D%0A%3Fperson%0D%0Aot%3AuserName+%22" + userID + "%22.%0D%0A%7D&format=json";
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            System.out.println("URL MapActivityA:" + url);

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
                                    String time = aux.getJSONObject("time").getString("value");
                                    BeaconReachedLOD reach = new BeaconReachedLOD();
                                    reach.setBeacon_id(beaconID);
                                    reach.setAnswered(false);
                                    System.out.println(participation + "holaje");
                                    reach.setReachMoment(Date.from(ZonedDateTime.parse(time).toInstant()));
                                    reachesAux.add(reach);
                                }
                                String score = "scorePartof";
                                if (!activity.isScore()) {
                                    score = "linealPartOf";
                                }

                                /*
                                 * SELECT DISTINCT ?correctanswer ?answer ?beaconID ?time WHERE{
                                 *   ?activity
                                 *       rdf:ID acitvity.getId().
                                 *   ?beacon
                                 *       ot:score(socrePartof/linealPartOf) ?activity;
                                 *       rdf:ID beaconID;
                                 *       ot:about ?object.
                                 *   ?personAnswer
                                 *       ot:toThe ?beacon;
                                 *       ot:answerResource ?answer;
                                 *       ot:answerTime ?time;
                                 *       ot:of ?person.
                                 *   ?person
                                 *       ot:userName participant_searched.
                                 *   ?objectproperty
                                 *       ot:relatedTo ?object;
                                 *       ot:answer correctanswer.
                                 * }
                                 *
                                 * */
                                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fcorrectanswer+%3Fanswer+%3FbeaconID+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fbeacon%0D%0A++ot%3A" + score + "+%3Factivity%3B%0D%0A++rdf%3AID+%3FbeaconID%3B%0D%0A++ot%3Aabout+%3Fobject.%0D%0A%3Fpersonanswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3AanswerResource+%3Fanswer%3B%0D%0A+ot%3AanswerTime+%3Ftime%3B%0D%0A+ot%3Aof+%3Fperson.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + participant_searched + "%22.%0D%0A+%3Fobjectproperty%0D%0A++ot%3ArelatedTo+%3Fobject%3B%0D%0A++ot%3Aanswer+%3Fcorrectanswer.%0D%0A%7D&format=json";
                                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                                System.out.println("URL MapActivity2:" + url);
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
                                                    if (reaches.size() < 1) {
                                                        emptyStateMessage_textView.setText("No hay balizas alcanzadas");
                                                        emptyState_layout.setVisibility(View.VISIBLE);
                                                    } else {
                                                        emptyStateMessage_textView.setText("");
                                                        emptyState_layout.setVisibility(View.GONE);
                                                    }
                                                    if (reaches.size() < 1) {
                                                        emptyStateMessage_textView.setText("No hay balizas alcanzadas");
                                                        emptyState_layout.setVisibility(View.VISIBLE);
                                                    } else {
                                                        emptyStateMessage_textView.setText("");
                                                        emptyState_layout.setVisibility(View.GONE);
                                                    }
                                                    Collections.sort(reaches, new BeaconReachedLOD());

                                                    reachAdapter = new ReachAdapter(reachesActivity, ReachesActivity.this, reaches,
                                                            activity, participant_searched);
                                                    reaches_recyclerView.setAdapter(reachAdapter);
                                                    reaches_recyclerView.setLayoutManager(new LinearLayoutManager(ReachesActivity.this));
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


            //BALIZAS ALCANZADAS POR UN USUARIO
            /*String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fcorrectanswer+%3Fanswer+%3FbeaconID+%3Ftime+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activityID + "%22.%0D%0A%3Fbeacon%0D%0A++ot%3AscorePartof+%3Factivity%3B%0D%0A++rdf%3AID+%3FbeaconID%3B%0D%0A++ot%3Aabout+%3Fobject.%0D%0A%3Fpersonanswer%0D%0A+ot%3AtoThe+%3Fbeacon%3B%0D%0A+ot%3AanswerResource+%3Fanswer%3B%0D%0A+ot%3AanswerTime+%3Ftime%3B%0D%0A+ot%3Aof+%3Fperson.%0D%0A%3Fperson%0D%0A+ot%3AuserName+%22" + participant_searched + "%22.%0D%0A+%3Fobjectproperty%0D%0A++ot%3ArelatedTo+%3Fobject%3B%0D%0A++ot%3Aanswer+%3Fcorrectanswer.%0D%0A%7D&format=json";
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            System.out.println("URL ReachesActivity:" + url);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                reaches = new ArrayList<>();
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);

                                    String correctanswer=aux.getJSONObject("correctanswer").getString("value");
                                    String answer=aux.getJSONObject("answer").getString("value"); //Esto revisar
                                    String beaconID=aux.getJSONObject("beaconID").getString("value");
                                    String time=aux.getJSONObject("time").getString("value");

                                    BeaconReachedLOD reach  = new BeaconReachedLOD();
                                    if(answer.equals(correctanswer)){
                                        reach.setAnswer_right(true);
                                    }else{
                                        reach.setAnswer_right(false);
                                    }
                                    reach.setReachMoment(Date.from(ZonedDateTime.parse((time+"[Europe/Madrid]")).toInstant()));
                                    reach.setBeacon_id(beaconID);
                                    reach.setWritten_answer(answer);

                                    //añadir reach
                                    reaches.add(reach);
                                }
                                if (reaches.size() < 1) {
                                    emptyStateMessage_textView.setText("No hay balizas alcanzadas");
                                    emptyState_layout.setVisibility(View.VISIBLE);
                                } else {
                                    emptyStateMessage_textView.setText("");
                                    emptyState_layout.setVisibility(View.GONE);
                                }
                                Collections.sort(reaches, new BeaconReachedLOD());

                                reachAdapter = new ReachAdapter(reachesActivity, ReachesActivity.this, reaches,
                                        activity, participant_searched);
                                reaches_recyclerView.setAdapter(reachAdapter);
                                reaches_recyclerView.setLayoutManager(new LinearLayoutManager(ReachesActivity.this));
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
            queue.add(jsonObjectRequest);*/


            // get the participant's reaches with realtime updates
        /*db.collection("activities").document(activityID)
                .collection("participations").document(participant_searched)
                .collection("beaconReaches")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        reaches = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            BeaconReachedLOD reach = doc.toObject(BeaconReachedLOD.class);
                            reaches.add(reach);
                        }
                        // show or hide the empty state with its message
                        if (reaches.size() < 1) {
                            emptyStateMessage_textView.setText("No hay balizas alcanzadas");
                            emptyState_layout.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateMessage_textView.setText("");
                            emptyState_layout.setVisibility(View.GONE);
                        }
                        Collections.sort(reaches, new BeaconReachedLOD());
                        reachAdapter = new ReachAdapter(reachesActivity, ReachesActivity.this, reaches,
                                templateID, activity, template, participant_searched);
                        reaches_recyclerView.setAdapter(reachAdapter);
                        reaches_recyclerView.setLayoutManager(new LinearLayoutManager(ReachesActivity.this));
                    }
                });*/
        } else {
            Toast.makeText(this, "Algo salió mal al cargar los datos", Toast.LENGTH_SHORT).show();
            finish();
        }

        reachesTrack_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    System.out.println("Paso 1");
                    if (mapDownloaded()) {
                        System.out.println("Paso 2");
                        // if we already have the map downloaded
                        updateUITrackMap();
                    } else {
                        System.out.println("Paso 2bis");
                       /* // if we don't have the map downloaded
                        final ProgressDialog pd = new ProgressDialog(ReachesActivity.this);
                        pd.setTitle("Cargando el mapa...");
                        pd.show();
                        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                        try {
                            // try to read the map image from Firebase into a file
                            File localFile = File.createTempFile("images", "png");
                            reference.getFile(localFile)
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            // we downloaded the map successfully
                                            // read the downloaded file into a bitmap
                                            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                            // save the bitmap to a file
                                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                            // path to /data/data/yourapp/app_data/imageDir
                                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                            // Create imageDir
                                            //File mypath = new File(directory, activity.getId() + ".png");
                                            File mypath = new File(directory, activity.getTemplate() + ".png");
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(mypath);
                                                // Use the compress method on the BitMap object to write image to the OutputStream
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(ReachesActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
                                            } finally {
                                                try {
                                                    fos.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            pd.dismiss();
                                            if (mapDownloaded()) {
                                                updateUITrackMap();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            Toast.makeText(ReachesActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(@NonNull @NotNull FileDownloadTask.TaskSnapshot snapshot) {
                                            double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                            if (progressPercent <= 90) {
                                                pd.setMessage("Progreso: " + (int) progressPercent + "%");
                                            } else {
                                                pd.setMessage("Descargado. Espera unos instantes mientras el mapa se guarda en el dispositivo");
                                            }
                                        }
                                    });
                        } catch (IOException e) {
                            pd.dismiss();
                        }*/
                        // if the map is not yet downloaded
                        final ProgressDialog pd = new ProgressDialog(ReachesActivity.this);
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
                        System.out.println("El mapa:" + url);
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
                                                System.out.println("Paso 3");
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
                                                    System.out.println("Paso 4 ");
                                                    updateUITrackMap();
                                                }
                                            }

                                        } catch (JSONException | IOException e) {
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
                }
            }
        });

    }

    private void updateUITrackMap() {
        Intent intent = new Intent(ReachesActivity.this, TrackActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("participantID", participantID);
        startActivity(intent);
    }

    // allow to go back when pressing the AppBar back arrow
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
        //File mypath = new File(directory, activity.getId() + ".png");
        File mypath = new File(directory, activity.getId() + ".png");
        if (mypath.exists()) {
            res = true;
        }
        return res;
    }
}