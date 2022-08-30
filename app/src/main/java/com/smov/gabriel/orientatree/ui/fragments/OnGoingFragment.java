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
import com.google.gson.JsonObject;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateColor;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.ui.HomeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnGoingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnGoingFragment extends Fragment implements View.OnClickListener {

    private RecyclerView onGoing_recyclerView;
    private ActivityAdapter activityAdapter;

    private SwipeRefreshLayout onGoing_pull_layout;

    private ArrayList<ActivityLOD> first_selection;
    private ArrayList<ActivityLOD> ultimate_selection;
    private ArrayList<ActivityLOD> no_duplicates_activities; // to remove duplicates due to being both organizer and participant

    private HomeActivity homeActivity;

    private ConstraintLayout no_activities_layout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OnGoingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnGoingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnGoingFragment newInstance(String param1, String param2) {
        OnGoingFragment fragment = new OnGoingFragment();
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
        View view = inflater.inflate(R.layout.fragment_on_going, container, false);

        homeActivity = (HomeActivity) getActivity();

        onGoing_pull_layout = view.findViewById(R.id.onGoing_pull_layout);

        onGoing_pull_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivities(view);
                onGoing_pull_layout.setRefreshing(false);
            }
        });

        no_activities_layout = view.findViewById(R.id.onGoing_empty_layout);

        getActivities(view);

        return view;
    }

    private void getActivities(View view) {

        // as I don't know how to query to Firestore within a range of Dates... I provisionally implement
        // that logic on the client... that's why I need two ArrayLists
        first_selection = new ArrayList<>(); // this one stores a first selection
        ultimate_selection = new ArrayList<>(); // and this one stores the ultimate one
        no_duplicates_activities = new ArrayList<>();

        long millis = System.currentTimeMillis();

        ZonedDateTime date2 = ZonedDateTime.now();
        String[] fecha = date2.toString().split("\\[");
        String fechaToUrl = fecha[0];




        Date date = new Date(millis);

        /*
        Actividades donde la fecha de fin es mayor que la fecha actual y el organizador es el id del usuario

        ok

        */

        laterAndOrganizedActivities(fechaToUrl,homeActivity.userID);

        homeActivity.db.collection("activities")
                .whereGreaterThanOrEqualTo("finishTime", date)
                .whereEqualTo("planner_id", homeActivity.userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // here we have all the activities that did not finish yet in the first array
                            Activity activity = document.toObject(Activity.class);
                            //first_selection.add(activity);
                        }

                        /*
                          Actividades cuya fecha de fin es mayor que la fecha actual y al menos uno de los participantes es el id del usuario
                         */



                        laterAndParticipantActivities(fechaToUrl,homeActivity.userID);
                        homeActivity.db.collection("activities")
                                .whereGreaterThanOrEqualTo("finishTime", date)
                                .whereArrayContains("participants", homeActivity.userID)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Activity activity = document.toObject(Activity.class);
                                            //first_selection.add(activity);
                                        }
                                        for (ActivityLOD activity : first_selection) {
                                            // and here we polish the selection by not choosing those that have not started neither, and
                                            // henceforth, they are future activities
                                            if (date.after(activity.getStartTime())) {
                                                ultimate_selection.add(activity);
                                            }
                                        }
                                        // removing duplicates due to being both organizer and participant
                                        for (ActivityLOD a : ultimate_selection) {
                                            boolean isFound = false;
                                            for (ActivityLOD b : no_duplicates_activities) {
                                                if (b.equals(a)) {
                                                    isFound = true;
                                                    break;
                                                }
                                            }
                                            if (!isFound) no_duplicates_activities.add(a);
                                        }
                                        Collections.sort(no_duplicates_activities, new ActivityLOD());
                                        if (no_duplicates_activities.size() < 1) {
                                            no_activities_layout.setVisibility(View.VISIBLE);
                                        } else {
                                            no_activities_layout.setVisibility(View.GONE);
                                        }
                                        activityAdapter = new ActivityAdapter(homeActivity, getContext(), no_duplicates_activities);
                                        onGoing_recyclerView = view.findViewById(R.id.onGoing_recyclerView);
                                        onGoing_recyclerView.setAdapter(activityAdapter);
                                        onGoing_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    }
                                });
                    }
                });
    }

    @Override
    public void onClick(View v) {

    }

    public void laterAndOrganizedActivities(String date, String userId){

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+DISTINCT+?norms+?abstract+?name+?endlong+?endlat+?startlat+?startlong+?endTime+?startTime+WHERE+{+?activity+ot:norms+?norms;+rdfs:label+?name;+rdfs:comment+?abstract;+ot:startPoint+?start;+ot:endPoint+?end;+ot:startTime+?startTime;+ot:endTime+?endTime;+dc:creator+?user.+FILTER+(?endTime+>+"+
                '\"'+date+'\"'+
                ")+?user+ot:userName+?userName.+FILTER+(?userName+=+"+
                '\"'+userId+'\"'+
                "+)+?end+geo:long+?endlong;+geo:lat+?endlat.?start+geo:long+?startlong;+geo:lat+?startlat.+}+ORDER+BY+DESC(?name)"+
                "&format=json";



        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result= response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux=result.getJSONObject(i);
                                String norms=aux.getJSONObject("norms").getString("value");
                                String name=aux.getJSONObject("name").getString("value"); //Esto revisar
                                String startLong=aux.getJSONObject("startlong").getString("value");
                                String endLong=aux.getJSONObject("endlong").getString("value");
                                String startLat=aux.getJSONObject("startlat").getString("value");
                                String endLat=aux.getJSONObject("endlat").getString("value");
                                String startTime=aux.getJSONObject("startTime").getString("value");
                                String endTime=aux.getJSONObject("endTime").getString("value");
                                String userName=aux.getJSONObject("userName").getString("value");
                                String descripcion=aux.getJSONObject("abstract").getString("value");
                                /*Template template=new Template();
                                template.setDescription(descripcion);
                                template.setNorms(norms);
                                template.setEnd_lat(Double.parseDouble(endLat));
                                template.setStart_lat(Double.parseDouble(startLat));
                                template.setEnd_lng(Double.parseDouble(endLong));
                                template.setStart_lng(Double.parseDouble(endLong));*/
                                /*String template_id, String name, TemplateType type, TemplateColor color, String location,
                                 String map_id,
                                String password*/


                                ActivityLOD activity = new ActivityLOD();
                                //String id, String key, String template, boolean score, boolean location_help
                                activity.setTitle(name);

                                activity.setStartTime(Date.from(ZonedDateTime.parse((startTime+"[Europe/Madrid]")).toInstant()));
                                activity.setFinishTime(Date.from(ZonedDateTime.parse((endTime+"[Europe/Madrid]")).toInstant()));
                                activity.setPlanner_id(userName);
                                first_selection.add(activity);
                                System.out.println("Response: " + name);

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

    public void laterAndParticipantActivities(String date, String userId){

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = "http://192.168.137.1:8890/sparql?query=SELECT+DISTINCT+?parName+?norms+?name+?abstract+?endlong+?endlat+?startlat+?startlong+?endTime+?startTime+WHERE+{+?activity+ot:norms+?norms;+rdfs:comment+?abstract;+rdfs:label+?name;+ot:startPoint+?start;+ot:endPoint+?end;+ot:startTime+?startTime;+ot:endTime+?endTime;+dc:creator+?user.+FILTER+(?endTime+>+"+
                '\"'+date+'\"'+
                ")+?track+ot:from+?activity;+ot:belongsTo+?participants.+?user+ot:userName+?userName.+?participants+ot:userName+?parName.+FILTER+(?parName+=+"+
                '\"'+userId+'\"'+
                "+)+?end+geo:long+?endlong;+geo:lat+?endlat.?start+geo:long+?startlong;+geo:lat+?startlat.+}+ORDER+BY+DESC(?name)"+
                "&format=json";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result= response.getJSONObject("results").getJSONArray("bindings");
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux=result.getJSONObject(i);
                                //String norms=result.getJSONObject("norms").getString("value");
                                String name=aux.getJSONObject("name").getString("value"); //Esto revisar
                                //String startLong=aux.getJSONObject("startlong").getString("value");
                                //String endLong=aux.getJSONObject("endlong").getString("value");
                                //String starLat=aux.getJSONObject("startlat").getString("value");
                                //String endLat=aux.getJSONObject("endlat").getString("value");
                                String startTime=aux.getJSONObject("startTime").getString("value");
                                String endTime=aux.getJSONObject("endTime").getString("value");
                                String userName=aux.getJSONObject("userName").getString("value");
                                String descripcion=aux.getJSONObject("abstract").getString("value");
                                ActivityLOD activity = new ActivityLOD();
                                //String id, String key, String template, boolean score, boolean location_help
                                activity.setTitle(name);
                                activity.setStartTime(Date.from(ZonedDateTime.parse((startTime+"[Europe/Madrid]")).toInstant()));
                                activity.setFinishTime(Date.from(ZonedDateTime.parse((endTime+"[Europe/Madrid]")).toInstant()));
                                activity.setPlanner_id(userName);
                                first_selection.add(activity);
                                System.out.println("Response: " + name);

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


}



