package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.BeaconLOD;
import com.smov.gabriel.orientatree.model.BeaconReachedLOD;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ReachAdapter extends RecyclerView.Adapter<ReachAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity reachesActivity;
    private int position;

    private ActivityLOD activity;
    private ArrayList<BeaconReachedLOD> reaches;

    private String participantID;

    public ReachAdapter(android.app.Activity reachesActivity, Context context,
                        ArrayList<BeaconReachedLOD> reaches,
                        ActivityLOD activity,
                        String participantID) {
        this.context = context;
        this.reachesActivity = reachesActivity;
        this.reaches = reaches;

        this.activity = activity;

        this.participantID = participantID;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_reach, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {

        this.position = position;
        BeaconReachedLOD reach = reaches.get(position);

        // useful IDs
        String beaconID = reach.getBeacon_id();

        // pattern to format the our at which the beacon was reached
        String pattern = "HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        holder.reachTime_textView.setText("Alcanzada: " + df.format(reach.getReachMoment()));

        // get the beacon to set the name and the numbeR

        /*
         * SELECT DISTINCT ?beaconname ?number WHERE{
         *   ?beacon
         *       rdf:ID reach.getBeacon_id();
         *       rdfs:label ?beaconname;
         *       ot:order ?number.
         * }
         * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Fbeaconname+%3Fnumber+WHERE%7B%0D%0A%3Fbeacon%0D%0A+rdf%3AID+%22" + reach.getBeacon_id() + "%22%3B%0D%0A+rdfs%3Alabel+%3Fbeaconname%3B%0D%0A+ot%3Aorder+%3Fnumber.%0D%0A%7D&format=json";
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                            BeaconLOD beacon = new BeaconLOD();
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject aux = result.getJSONObject(i);
                                String beaconname = aux.getJSONObject("beaconname").getString("value");
                                String number = aux.getJSONObject("number").getString("value");
                                beacon = new BeaconLOD();
                                beacon.setName(beaconname);
                                beacon.setBeacon_id(reach.getBeacon_id());
                                beacon.setNumber(Integer.parseInt(number));
                            }

                            holder.reachTitle_textView.setText(beacon.getName());
                            holder.reachNumber_textView.setText("Baliza número " + beacon.getNumber());


                            holder.row_reach_layout.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    //if (template.getType() == TemplateType.EDUCATIVA /*&& !reach.isGoal()*/) {
                                    // if template DEPORTIVA we don't do anything
                                    // same if it is goal
                                    updateUIChallengeActivity(beaconID, activity);
                                    //}
                                }
                            });


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





    }

    private void updateUIChallengeActivity(String beaconID, ActivityLOD activity) {
        Intent intent = new Intent(context, ChallengeActivity.class);
        intent.putExtra("beaconID", beaconID);
        intent.putExtra("activity", activity);
        intent.putExtra("participantID", participantID);
        reachesActivity.startActivityForResult(intent, 1);
    }

    @Override
    public int getItemCount() {
        return reaches.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView reachState_textView, reachTitle_textView,
                reachNumber_textView, reachTime_textView;

        MaterialCardView row_reach_cardView;

        FirebaseFirestore db;

        LinearLayout row_reach_layout;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            // start Firebase services
            db = FirebaseFirestore.getInstance();

            // bind UI elements
            reachState_textView = itemView.findViewById(R.id.reachState_textView);
            reachTitle_textView = itemView.findViewById(R.id.reachTitle_textView);
            reachNumber_textView = itemView.findViewById(R.id.reachNumber_textView);
            reachTime_textView = itemView.findViewById(R.id.reachTime_textView);
            row_reach_layout = itemView.findViewById(R.id.row_reach_layout);
            row_reach_cardView = itemView.findViewById(R.id.row_reach_cardView);

        }
    }
}
