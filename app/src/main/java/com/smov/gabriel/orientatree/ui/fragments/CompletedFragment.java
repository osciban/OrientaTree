package com.smov.gabriel.orientatree.ui.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.ui.HomeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CompletedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CompletedFragment extends Fragment {

    private RecyclerView completed_recyclerView;
    private ActivityAdapter activityAdapter;
    private ArrayList<ActivityLOD> all_activities;
    private ArrayList<ActivityLOD> no_duplicates_activities; // to remove duplicates due to being both organizer and participant

    private SwipeRefreshLayout completed_pull_layout;

    private HomeActivity homeActivity;

    private ConstraintLayout no_activities_layout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CompletedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CompletedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CompletedFragment newInstance(String param1, String param2) {
        CompletedFragment fragment = new CompletedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_completed, container, false);

        homeActivity = (HomeActivity) getActivity();

        completed_pull_layout = view.findViewById(R.id.completed_pull_layout);
        completed_pull_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivities(view);
                completed_pull_layout.setRefreshing(false);
            }
        });

        no_activities_layout = view.findViewById(R.id.completed_empty_layout);

        getActivities(view);

        return view;
    }

    private void getActivities(View view) {
        all_activities = new ArrayList<>();
        no_duplicates_activities = new ArrayList<>();

        long millis = System.currentTimeMillis();

        Date date = new Date(millis);


        earlierAndOrganizedActivities(date, homeActivity.userID, view);


/*
        homeActivity.db.collection("activities")
                .whereLessThan("finishTime", date)
                .whereEqualTo("planner_id", homeActivity.userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Activity activity = document.toObject(Activity.class);
                            //all_activities.add(activity);
                        }

                        earlierAndParticipantActivities(fechaToUrl,homeActivity.userID);
                        homeActivity.db.collection("activities")
                                .whereLessThan("finishTime", date)
                                .whereArrayContains("participants", homeActivity.userID)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Activity activity = document.toObject(Activity.class);
                                            //all_activities.add(activity);
                                        }
                                        // removing duplicates due to being both organizer and participant
                                        for(Activity a : all_activities) {
                                            boolean isFound = false;
                                            for (Activity b : no_duplicates_activities) {
                                                if(b.equals(a)) {
                                                    isFound = true;
                                                    break;
                                                }
                                            }
                                            if(!isFound) no_duplicates_activities.add(a);
                                        }
                                        Collections.sort(no_duplicates_activities, new Activity());
                                        if(no_duplicates_activities.size() < 1) {
                                            no_activities_layout.setVisibility(View.VISIBLE);
                                        } else {
                                            no_activities_layout.setVisibility(View.GONE);
                                        }
                                        System.out.println("numero"+no_duplicates_activities.size());
                                        activityAdapter = new ActivityAdapter(homeActivity, getContext(), no_duplicates_activities);
                                        completed_recyclerView = view.findViewById(R.id.completed_recyclerView);
                                        completed_recyclerView.setAdapter(activityAdapter);
                                        completed_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    }
                                });
                    }
                });*/
    }


    public void earlierAndOrganizedActivities(Date date, String userId, View view) {

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+DISTINCT+?endTime+?userName+?id+?name+?startTime+WHERE+{+?activity++rdf:ID+?id;+rdfs:label+?name;+ot:startTime+?startTime;+ot:endTime+?endTime;+dc:creator+?user.+?user+ot:userName+?userName.+FILTER+(?userName+=+" +
                '\"' + userId + '\"' +
                "+)+}+ORDER+BY+DESC(?name)" +
                "&format=json";

        System.out.println("URL:" + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);

                                String id = aux.getJSONObject("id").getString("value");
                                String name = aux.getJSONObject("name").getString("value"); //Esto revisar
                                String startTime = aux.getJSONObject("startTime").getString("value");
                                String userName = aux.getJSONObject("userName").getString("value");
                                String endTime = aux.getJSONObject("endTime").getString("value");
                                ActivityLOD activity = new ActivityLOD();

                                activity.setId(id);
                                activity.setFinishTime(Date.from(ZonedDateTime.parse((endTime + "[Europe/Madrid]")).toInstant()));
                                activity.setStartTime(Date.from(ZonedDateTime.parse((startTime + "[Europe/Madrid]")).toInstant()));
                                activity.setName(name);
                                activity.setPlanner_id(userName);
                                all_activities.add(activity);
                                System.out.println("Response: " + id);

                            }
                            earlierAndParticipantActivities(date, homeActivity.userID, view);

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

    public void earlierAndParticipantActivities(Date date, String userId, View view) {

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+DISTINCT+?endTime+?userName+?id+?name+?startTime+WHERE+{+?activity+rdfs:label+?name;+rdf:ID+?id;+ot:startTime+?startTime;+ot:endTime+?endTime;+dc:creator+?user.+?track+ot:from+?activity;+ot:belongsTo+?participants.+?user+ot:userName+?userName.+?participants+ot:userName+?parName.+FILTER+(?parName+=+" +
                '\"' + userId + '\"' +
                "+)+}+ORDER+BY+DESC(?name)" +
                "&format=json";

        System.out.println("URL:" + url);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ArrayList<String> participants = new ArrayList<>();
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);

                                String id = aux.getJSONObject("id").getString("value");
                                String name = aux.getJSONObject("name").getString("value"); //Esto revisar
                                String startTime = aux.getJSONObject("startTime").getString("value");
                                String endTime = aux.getJSONObject("endTime").getString("value");
                                String userName = aux.getJSONObject("userName").getString("value");
                                ActivityLOD activity = new ActivityLOD();

                                activity.setId(id);
                                activity.setStartTime(Date.from(ZonedDateTime.parse((startTime + "[Europe/Madrid]")).toInstant()));
                                activity.setName(name);
                                activity.setFinishTime(Date.from(ZonedDateTime.parse((endTime + "[Europe/Madrid]")).toInstant()));
                                activity.setPlanner_id(userName);
                                participants.add(homeActivity.userID);
                                activity.setParticipants(participants);
                                all_activities.add(activity);
                                System.out.println("Response: " + id);

                            }

                            System.out.println("Tamaño1:" + all_activities.size());
                            for (int i = 0; i < all_activities.size(); i++) {

                                ActivityLOD a = all_activities.get(i);
                                System.out.println("Actual" + date.toString());

                                /*if (a.getStartTime().compareTo(date) > 0) {
                                    System.out.println("no deberia");
                                    //all_activities.remove(a);
                                    //no_duplicates_activities.remove(a);
                                } else if (a.getFinishTime().compareTo(date) > 0) {
                                    System.out.println(a.getFinishTime()+"aaa");
                                    //all_activities.remove(a);
                                    //no_duplicates_activities.remove(a);
                                }*/
                                if (a.getStartTime().compareTo(date) < 0 && a.getFinishTime().compareTo(date) < 0) {
                                    boolean isFound = false;
                                    for (ActivityLOD b : no_duplicates_activities) {
                                        if (b.equals(a)) {
                                            System.out.println("HOLAAAA");
                                            isFound = true;
                                            break;
                                        }
                                    }
                                    if (!isFound)

                                        no_duplicates_activities.add(a);
                                }
                            }

                            Collections.sort(no_duplicates_activities, new ActivityLOD());

                            if (no_duplicates_activities.size() < 1) {
                                no_activities_layout.setVisibility(View.VISIBLE);
                            } else {
                                no_activities_layout.setVisibility(View.GONE);
                            }


                            activityAdapter = new ActivityAdapter(homeActivity, getContext(), no_duplicates_activities);
                            completed_recyclerView = view.findViewById(R.id.completed_recyclerView);
                            completed_recyclerView.setAdapter(activityAdapter);
                            completed_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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

}