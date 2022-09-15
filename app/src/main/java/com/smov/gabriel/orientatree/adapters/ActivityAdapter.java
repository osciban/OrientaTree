package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.ui.NowActivity;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.utils.MySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity homeActivity;

    private ArrayList<ActivityLOD> activities;

    public ActivityAdapter(android.app.Activity homeActivity, Context context, ArrayList<ActivityLOD> activities) {
        this.homeActivity = homeActivity;
        this.context = context;
        this.activities = activities;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_activity, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        ActivityLOD activity = activities.get(position);
        String user_id = holder.mAuth.getCurrentUser().getUid();


        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);

        //obtener startTime getPlannerId y nombre
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        holder.title_textView.setText(activity.getName());
        holder.date_textView.setText("Fecha: " + dateAsString);

        if (activity.getPlanner_id().equals(user_id)) {
            holder.role_textView.setText("Organizador/a");
        } else if (activity.getParticipants() != null) {
            if (activity.getParticipants().contains(user_id)) {
                holder.role_textView.setText("Participante");
            }
        } else {
            holder.role_textView.setText("");
        }

        holder.row_activity_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUINowActivity(activity);
            }
        });

        holder.template_textView.setText(activity.getName());

        /*holder.db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Template template = documentSnapshot.toObject(Template.class);
                        holder.template_textView.setText(template.getName());

                    }
                });
        */
        // get and set the activity picture
        DownloadImageFromPath(holder,activity);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseStorage storage;
        StorageReference storageReference;

        FirebaseAuth mAuth;

        FirebaseFirestore db;

        LinearLayout row_activity_layout;
        TextView title_textView, date_textView, template_textView, role_textView;
        ImageView rowImage_imageView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title_textView = itemView.findViewById(R.id.title_textView);
            date_textView = itemView.findViewById(R.id.date_textView);
            template_textView = itemView.findViewById(R.id.template_textView);
            role_textView = itemView.findViewById(R.id.role_textView);
            row_activity_layout = itemView.findViewById(R.id.row_activity_layout);
            rowImage_imageView = itemView.findViewById(R.id.rowImage_imageView);

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            db = FirebaseFirestore.getInstance();

            mAuth = FirebaseAuth.getInstance();
        }
    }


    private void updateUINowActivity(ActivityLOD activity) {
        Intent intent = new Intent(context, NowActivity.class);
        intent.putExtra("activity", activity);
        homeActivity.startActivityForResult(intent, 1); // this is to allow us to come back from the activity
    }

    public void DownloadImageFromPath(MyViewHolder holder,ActivityLOD activity){


        /*
        * SELECT DISTINCT ?image WHERE {
        *  ?activity
        *   rdf:ID activity.getID();
        *   schema:image ?image.
        * } ORDER BY DESC(?image)
        *
         */


        String url = "http://192.168.137.1:8890/sparql?query=SELECT+DISTINCT+?image+WHERE+{+?activity+rdf:ID+"
                +'\"'+activity.getId()+'\"'+";+schema:image+?image.+}+ORDER+BY+DESC(?image)&format=json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String path= response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("image").getString("value");
                            activity.setImage(path);
                            Glide.with(context)
                                    .load(path)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                                    .skipMemoryCache(true) // prevent caching
                                    .into(holder.rowImage_imageView);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TAG","norespone");

                    }
                });

        MySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }
}



