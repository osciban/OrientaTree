package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconLOD;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.ui.fragments.ChallengeQuizFragment;
import com.smov.gabriel.orientatree.ui.fragments.ChallengeTextFragment;
import com.smov.gabriel.orientatree.utils.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChallengeActivity extends AppCompatActivity {

    private ImageView challenge_imageView;
    private TextView challengeTitle_textView, challengeText_textView,
            challengeQuestion_textView;

    private Toolbar toolbar;

    public ActivityLOD activity;
    public BeaconLOD beacon;
    private Template template;
    public boolean organizer = false; // flag to signal if the logged user is the organizer of the activity

    // some useful IDs
    private String beaconID;
    private String templateID;
    public String activityID;
    public String userID;

    public FirebaseFirestore db;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        // get data from the intent
        beaconID = getIntent().getExtras().getString("beaconID");

        activity = (ActivityLOD) getIntent().getSerializableExtra("activity");
        if (activity != null) {
            activityID = activity.getId();
        }
        String tempUserID = getIntent().getExtras().getString("participantID");
        if (activity.getPlanner_id().equals(userID)) {
            // if the current user is the organizer...
            organizer = true;
            if (tempUserID != null) {
                // if we got the participant id from the intent right...
                userID = tempUserID;
            } else {
                Toast.makeText(this, "Algo sali?? mal al obtener los datos", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            organizer = false;
        }

        // set the toolbar
        toolbar = findViewById(R.id.challenge_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // bind view elements
        challenge_imageView = findViewById(R.id.challenge_imageView);
        challengeTitle_textView = findViewById(R.id.challengeTitle_textView);
        challengeText_textView = findViewById(R.id.challengeText_textView);
        challengeQuestion_textView = findViewById(R.id.challengeQuestion_textView);
        beacon = new BeaconLOD();
        beacon.setBeacon_id(beaconID);
        // get the beacon from Firestore using the data that we received from the intent
        if (beaconID != null) {

            /*
            * SELECT DISTINCT ?beaconname ?type ?question ?image WHERE {
            *   ?beacon
            *       rdf:ID beaconID;
            *       rdfs:label ?beaconname;
            *       schema:image ?image
            *       ot:develop ?task.
            *   ?task
            *       clp:answerType ?type
            *       clp:associatedTextResource ?question.
            * }
            * */
            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fbeaconname+%3Ftype+%3Fquestion+%3Fimage+WHERE%7B%0D%0A%3Fbeacon%0D%0A+rdf%3AID+%22" + beaconID + "%22%3B%0D%0A+rdfs%3Alabel+%3Fbeaconname%3B%0D%0A+schema%3Aimage+%3Fimage%3B%0D%0A+ot%3Adevelop+%3Ftask.%0D%0A%0D%0A%3Ftask%0D%0A+clp%3AanswerType+%3Ftype%3B%0D%0A+clp%3AassociatedTextResource+%3Fquestion.%0D%0A%0D%0A%7D&format=json";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    String beaconname = aux.getJSONObject("beaconname").getString("value");
                                    String type = aux.getJSONObject("type").getString("value");
                                    String image = aux.getJSONObject("image").getString("value");
                                    String question = aux.getJSONObject("question").getString("value");
                                    beacon.setName(beaconname);
                                    beacon.setImage(image);
                                    beacon.setQuestion(question);
                                    beacon.setType(type);
                                }

                                challengeTitle_textView.setText(beacon.getName());

                                getQuestionAndText();

                                switch (beacon.getType()) {
                                    case "MCQ":
                                        // show quiz fragment
                                        if (savedInstanceState == null) {
                                            getPossibleAnswers();

                                        }
                                        break;
                                    case "Respuesta Corta":
                                        //show short answer fragment
                                        if (savedInstanceState == null) {
                                            getTextAnswer();


                                        }
                                        break;

                                    default:
                                        break;
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


        }


    }

    private void getTextAnswer() {

        /*

        * SELECT DISTINCT ?answer WHERE {
        *   ?beacon
        *       rdf:ID beaconID;
        *       ot:about ?object.
        *   ?objectproperty
        *       ot:answer ?answer;
        *       ot:relatedTo ?object.
        * }
        *
        * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fanswer+WHERE%7B%0D%0A%3Fbeacon%0D%0Ardf%3AID+\"" + beaconID + "\"%3B%0D%0Aot%3Aabout+%3Fobject.%0D%0A%3Fobjectproperty%0D%0Aot%3Aanswer+%3Fanswer%3B%0D%0Aot%3ArelatedTo+%3Fobject.%0D%0A%7D&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            beacon.setWritten_right_answer(result.getJSONObject(0).getJSONObject("answer").getString("value"));

                            showFragmentText();
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

    private void getPossibleAnswers() {

        /*
        *
        * SELECT DISTINCT ?answer ?possibleAnswer WHERE{
        *   ?beacon
        *       rdf:ID beaconID;
        *       ot:about ?object.
        *   ?objectproperty
        *       ot:relatedTo ?object;
        *       ot:answer ?answer;
        *       ot:distractor ?possibleAnswer.
        * }
        *
        */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fanswer+%3FpossibleAnswer+WHERE%7B%0D%0A+%3Fbeacon%0D%0A++rdf%3AID+%22" + beaconID + "%22%3B%0D%0A++ot%3Aabout+%3Fobject.%0D%0A+%3Fobjectproperty%0D%0A++ot%3ArelatedTo+%3Fobject%3B%0D%0A++ot%3Aanswer+%3Fanswer%3B%0D%0A++ot%3Adistractor+%3FpossibleAnswer.%0D%0A%7D%0D%0A&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            beacon.setWritten_right_answer(result.getJSONObject(0).getJSONObject("answer").getString("value"));
                            ArrayList<String> distractors = new ArrayList<>();
                            distractors.add(beacon.getWritten_right_answer());
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                distractors.add(aux.getJSONObject("possibleAnswer").getString("value"));

                            }
                            beacon.setPossible_answers(distractors);
                            showFragmentQuiz();
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

    private void getQuestionAndText() {
        String url = "";

        if (beacon.getQuestion().contains("superobject")) {

            /*
            * SELECT DISTINCT ?superobjectText ?objectText ?text ?propertyText WHERE {
            *   ?beacon
            *       rdf:ID beaconID;
            *       ot:about object.
            *   ?object
            *       skos:narrower ?superobject;
            *       ot:inQuestion ?objectText.
            *   ?superobject
            *       ot:inQuestion ?superobjectText;
            *       dbo:abstract ?text.
            *   ?objectProperty
            *       ot:relatedTo ?object;
            *       ot:inQuestion ?propertyText.
            * }
            *
            */

            url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FsuperobjectText+%3FobjectText+%3Ftext+%3FpropertyText+WHERE%7B%0D%0A%3Fbeacon%0D%0A+rdf%3AID+%22" + beaconID + "%22%3B%0D%0A+ot%3Aabout+%3Fobject.%0D%0A%3Fobject%0D%0A+skos%3Anarrower+%3Fsuperobject%3B%0D%0A+ot%3AinQuestion+%3FobjectText.%0D%0A%3Fsuperobject%0D%0A+ot%3AinQuestion+%3FsuperobjectText%3B%0D%0A+dbo%3Aabstract+%3Ftext.%0D%0A%3FobjectProperty%0D%0A+ot%3ArelatedTo+%3Fobject%3B%0D%0A+ot%3AinQuestion+%3FpropertyText.%0D%0A%7D&format=json";
        } else {

            /*
             * SELECT DISTINCT ?objectText ?text ?propertyText WHERE {
             *   ?beacon
             *       rdf:ID beaconID;
             *       ot:about object.
             *   ?object
             *       ot:inQuestion ?objectText;
             *       dbo:abstract ?text.
             *   ?objectProperty
             *       ot:relatedTo ?object;
             *       ot:inQuestion ?propertyText.
             * }
             *
             *
             */

            url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FobjectText+%3Ftext+%3FpropertyText+WHERE%7B%0D%0A%3Fbeacon%0D%0A+rdf%3AID+%22" + beaconID + "%22%3B%0D%0A+ot%3Aabout+%3Fobject.%0D%0A%3Fobject%0D%0A+ot%3AinQuestion+%3FobjectText%3B%0D%0A+dbo%3Aabstract+%3Ftext.%0D%0A%3FobjectProperty%0D%0A+ot%3ArelatedTo+%3Fobject%3B%0D%0A+ot%3AinQuestion+%3FpropertyText.%0D%0A%7D&format=json";
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");

                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                String objectText = aux.getJSONObject("objectText").getString("value");
                                String text = aux.getJSONObject("text").getString("value");
                                String propertyText = aux.getJSONObject("propertyText").getString("value");

                                beacon.setQuestion(beacon.getQuestion().replace("<object>", objectText).replace("<property>", propertyText));

                                beacon.setText(text);
                                if (beacon.getQuestion().contains("superobject")) {
                                    String superobjectText = aux.getJSONObject("superobjectText").getString("value");
                                    beacon.setQuestion(beacon.getQuestion().replace("<superobject>", superobjectText));
                                }

                            }
                            challengeText_textView.setText(beacon.getText());
                            challengeQuestion_textView.setText(beacon.getQuestion());
                            // get the image of the beacon
                            Glide.with(getApplicationContext())
                                    .load(beacon.getImage())
                                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                                    .skipMemoryCache(true) // prevent caching
                                    .into(challenge_imageView);


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

    private void showFragmentText() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.challenge_fragmentContainer, ChallengeTextFragment.class, null)
                .commit();
    }

    private void showFragmentQuiz() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.challenge_fragmentContainer, ChallengeQuizFragment.class, null)
                .commit();
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
}