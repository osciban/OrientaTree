package com.smov.gabriel.orientatree.lodadapters;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.ui.NowActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity homeActivity;

    private ArrayList<Activity> activities;
    private int position;

    public ActivityAdapter(android.app.Activity homeActivity, Context context, ArrayList<Activity> activities) {
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
        this.position = position;
        Activity activity = activities.get(position);

        String planner_id = activity.getPlanner_id();
        String user_id = holder.mAuth.getCurrentUser().getUid();

        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        holder.title_textView.setText(activity.getTitle());
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
        //Actividades organizadas por el usuario
        holder.db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Template template = documentSnapshot.toObject(Template.class);
                        holder.template_textView.setText(template.getName());
                        System.out.println("EEEEEEEEENAME"+template.getName());
                        System.out.println("EEEEEEEEEEENDLONG"+template.getEnd_lng());
                        System.out.println("EEEEEEEEENENDLAT"+template.getEnd_lat());
                        System.out.println("EEEEEEEEEPASSWD"+template.getPassword());
                        System.out.println("EEEEEEEEESTARTLONG"+template.getStart_lng());
                        System.out.println("EEEEEEEEESTARLAT"+template.getStart_lat());
                        System.out.println("EEEEEEEEETYPE"+template.getType());
                    }
                });

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.rowImage_imageView);
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

    private void updateUINowActivity(Activity activity) {
        Intent intent = new Intent(context, NowActivity.class);
        intent.putExtra("activity", activity);
        homeActivity.startActivityForResult(intent, 1); // this is to allow us to come back from the activity
        new JsonTask().execute("https://192.168.137.1:8890/sparql?query=SELECT%20DISTINCT%20?norms%20?name%20?endlong%20?endlat%20?startlat%20?startlong%20WHERE%20{%20?activity%20ot:norms%20?norms;%20rdfs:label%20?name;%20ot:startPoint%20?start;%20ot:endPoint%20?end;%20dc:creator%20?user.%20?user%20ot:userName%20%22example1%22.%20?end%20geo:long%20?endlong;%20geo:lat%20?endlat.%20?start%20geo:long%20?startlong;%20geo:lat%20?startlat.%20}%20ORDER%20BY%20DESC(?norms)&format=json");
    }

    private class JsonTask extends AsyncTask<String, String, String> {


        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }
                System.out.println("AAAAAAAAA"+buffer.toString());
                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


    }

}
