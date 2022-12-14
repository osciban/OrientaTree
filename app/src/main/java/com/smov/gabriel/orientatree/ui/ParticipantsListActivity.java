package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ParticipantAdapter;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.utils.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ParticipantsListActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private ActivityLOD activity;
    //private Template template;

    private RecyclerView participantsList_recyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participation> participations;
    private ConstraintLayout emptyState_layout;
    private TextView emptyStateMessage_textView;
    private TextView participantsListparticipants_textView;

    // needed to pass it to the adapter so that cards can be clicked and head to a new activity
    private ParticipantsListActivity participantsListActivity;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants_list);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        // binding interface elements
        participantsList_recyclerView = findViewById(R.id.participantsList_recyclerView);
        emptyState_layout = findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = findViewById(R.id.emptyStateMessage_textView);
        participantsListparticipants_textView = findViewById(R.id.participantsListparticipants_textView);

        participantsListActivity = (ParticipantsListActivity) this;

        // setting the AppBar
        toolbar = findViewById(R.id.participantsList_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get the activity
        Intent intent = getIntent();
        activity = (ActivityLOD) intent.getSerializableExtra("activity");
        participations = new ArrayList<>();

        if (activity != null) {

            participations = new ArrayList<>();

            /*
             * SELECT DISTINCT ?participantName ?time ?completed WHERE{
             *   ?track
             *       ot:from ?activity;
             *       ot:belongsTo ?participant;
             *       ot:completed ?completed;
             *       ot:composedBy ?point
             *   ?participant
             *       ot:userName ?participantName.
             *   ?activity
             *       rdf:ID acitivity.getId().
             *   ?point
             *       ot:time ?time
             * } ORDER BY ASC(?participantName)
             */

            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FparticipantName+%3Ftime+%3Fcompleted+WHERE%7B%0D%0A%3Ftrack%0D%0A++ot%3Afrom+%3Factivity%3B%0D%0A++ot%3AbelongsTo+%3Fparticipant%3B%0D%0A++ot%3Acompleted+%3Fcompleted%3B%0D%0A++ot%3AcomposedBy+%3Fpoint.%0D%0A%3Fparticipant%0D%0A++ot%3AuserName+%3FparticipantName.%0D%0A%3Factivity%0D%0A+rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Fpoint%0D%0A++ot%3Atime+%3Ftime.%0D%0A%7D+ORDER+BY+ASC%28%3FparticipantName%29%2C++%3Ftime&format=json";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                String idParticipant = "";
                                Participation p = new Participation();
                                boolean flag = true;
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    String idParticipanNew = aux.getJSONObject("participantName").getString("value");
                                    if (!idParticipanNew.equals(idParticipant)) {
                                        if (!flag) {
                                            participations.add(p);
                                        }
                                        idParticipant = idParticipanNew;
                                        p = new Participation();
                                        String startTime = aux.getJSONObject("time").getString("value");
                                        p.setParticipant(idParticipant);
                                        p.setStartTime(Date.from(ZonedDateTime.parse((startTime + "[Europe/Madrid]")).toInstant()));
                                    } else {
                                        flag = false;
                                        String endTime = aux.getJSONObject("time").getString("value");
                                        p.setFinishTime(Date.from(ZonedDateTime.parse((endTime + "[Europe/Madrid]")).toInstant()));
                                    }
                                    if (i == result.length() - 1) {
                                        participations.add(p);
                                    }

                                }
                                Collections.sort(participations, new Participation());
                                if (participations.size() < 1) {
                                    emptyStateMessage_textView.setText("Parece que esta actividad no tiene participantes");
                                    emptyState_layout.setVisibility(View.VISIBLE);
                                } else {
                                    emptyStateMessage_textView.setText("");
                                    emptyState_layout.setVisibility(View.GONE);
                                }
                            /*if(template.getType() == TemplateType.DEPORTIVA) {
                                participantsListparticipants_textView.setText("Clasificaci??n: ");
                            } else {*/
                                participantsListparticipants_textView.setText("Participantes: (" + participations.size() + ")");
                                //}
                                participantAdapter = new ParticipantAdapter(participantsListActivity, ParticipantsListActivity.this,
                                        participations, activity);
                                participantsList_recyclerView.setAdapter(participantAdapter);
                                participantsList_recyclerView.setLayoutManager(new LinearLayoutManager(ParticipantsListActivity.this));


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
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
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