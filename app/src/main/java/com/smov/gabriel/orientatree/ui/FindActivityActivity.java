package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.FindActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

public class FindActivityActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ActionBar ab;

    private CircularProgressIndicator progressIndicator;

    private RecyclerView find_activity_recyclerview;
    private FindActivityAdapter findActivityAdapter;
    private ArrayList<ActivityLOD> activities;
    private TextView emptyStateMessage_textView;

    private FirebaseFirestore db;

    private ConstraintLayout no_activities_id_layout;
    private ConstraintLayout helper_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_activity);

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.find_activity_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        progressIndicator = findViewById(R.id.find_progress_circular);

        activities = new ArrayList<>();

        find_activity_recyclerview = findViewById(R.id.find_activity_recyclerview);
        emptyStateMessage_textView = findViewById(R.id.emptyStateMessage_textView);

        no_activities_id_layout = findViewById(R.id.peacockHead_emptyState);
        helper_layout = findViewById(R.id.find_helper_layout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                progressIndicator.setVisibility(View.VISIBLE);
                helper_layout.setVisibility(View.GONE);
                activities = new ArrayList<>();
                long millis = System.currentTimeMillis();
                Date current_date = new Date(millis);
                System.out.println("Entro a menu 7");
                /*db.collection("activities")
                        .whereEqualTo("visible_id", query)
                        .whereGreaterThan("finishTime", current_date)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                progressIndicator.setVisibility(View.GONE);
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Activity activity = document.toObject(Activity.class);
                                    activities.add(activity);
                                }
                                if(activities.size() < 1 ) {
                                    emptyStateMessage_textView.setText("Vaya, parece que no hay coincidencias");
                                    no_activities_id_layout.setVisibility(View.VISIBLE);
                                } else {
                                    emptyStateMessage_textView.setText("");
                                    no_activities_id_layout.setVisibility(View.GONE);
                                }
                                findActivityAdapter = new FindActivityAdapter(FindActivityActivity.this, activities);
                                find_activity_recyclerview.setAdapter(findActivityAdapter);
                                find_activity_recyclerview.setLayoutManager(new LinearLayoutManager(FindActivityActivity.this));
                            }
                        });*/
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                /*
                * SELECT DISTINCT ?userName ?name ?startTime ?endTime ?image WHERE{
                *   ?organizer
                *      ot:userName ?userName.
                *   ?activity
                *      dc:creator ?organizer;
                *      rdf:id query;
                *      ot:startTime ?startTime;
                *      schema:image ?image;
                *      ot:endTime ?endTime;
                *      rdfs:label ?name.
                * }
                * */

                String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FuserName+%3Fname+%3FstartTime+%3FendTime+%3Fimage+WHERE%7B%0D%0A%3Forganizer%0D%0A+ot%3AuserName+%3FuserName.%0D%0A%3Factivity%0D%0A+dc%3Acreator+%3Forganizer%3B%0D%0A+rdf%3AID+" + '\"' + query + '\"' + ";+ot%3AstartTime+%3FstartTime%3B%0D%0A+schema%3Aimage+%3Fimage%3B%0D%0A+ot%3AendTime+%3FendTime%3B%0D%0A+rdfs%3Alabel+%3Fname.%0D%0A%7D%0D%0A&+"
                        + "&format=json";

                System.out.println("URL:" + url);

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                    progressIndicator.setVisibility(View.GONE);
                                    for (int i = 0; i < result.length(); i++) {
                                        JSONObject aux = result.getJSONObject(i);

                                        String name = aux.getJSONObject("name").getString("value");
                                        Date startTime = Date.from(ZonedDateTime.parse(aux.getJSONObject("startTime").getString("value")).toInstant());
                                        Date endTime = Date.from(ZonedDateTime.parse(aux.getJSONObject("endTime").getString("value")).toInstant());
                                        String image = aux.getJSONObject("image").getString("value");
                                        String organizer = aux.getJSONObject("userName").getString("value");

                                        if (endTime.compareTo(current_date) > 0) {
                                            ActivityLOD activity = new ActivityLOD();
                                            activity.setFinishTime(endTime);
                                            activity.setStartTime(startTime);
                                            activity.setName(name);
                                            activity.setTitle(name);
                                            activity.setImage(image);
                                            activity.setId(query);
                                            activity.setPlanner_id(organizer);
                                            activity.setParticipants(new ArrayList<>());

                                            activities.add(activity);
                                        }

                                    }

                                    if (activities.size() < 1) {
                                        emptyStateMessage_textView.setText("Vaya, parece que no hay coincidencias");
                                        no_activities_id_layout.setVisibility(View.VISIBLE);

                                    } else {
                                        emptyStateMessage_textView.setText("");
                                        no_activities_id_layout.setVisibility(View.GONE);


                                    }
                                    findActivityAdapter = new FindActivityAdapter(FindActivityActivity.this, activities);
                                    find_activity_recyclerview.setAdapter(findActivityAdapter);
                                    find_activity_recyclerview.setLayoutManager(new LinearLayoutManager(FindActivityActivity.this));


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
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }
}