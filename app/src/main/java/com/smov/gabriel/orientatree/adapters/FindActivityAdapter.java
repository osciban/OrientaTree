package com.smov.gabriel.orientatree.adapters;

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

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.ui.FindActivityActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class FindActivityAdapter extends RecyclerView.Adapter<FindActivityAdapter.MyViewHolder> {

    private Context context;

    private ActivityLOD activity;

    private ArrayList<ActivityLOD> activities;
    private int position;
    FirebaseFirestore db;

    public FindActivityAdapter(Context context, ArrayList<ActivityLOD> activities) {
        this.context = context;
        this.activities = activities;
    }

    @NonNull
    @Override
    public FindActivityAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_find_activity, parent, false);
        return new FindActivityAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindActivityAdapter.MyViewHolder holder, int position) {
        this.position = position;
        ActivityLOD activity = activities.get(position);

        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        // display title and date
        holder.find_title_textView.setText(activity.getTitle());
        holder.find_date_textView.setText("Fecha: " + dateAsString);

        // check that the current user is not the organizer of the activity
        if (!activity.getPlanner_id().equals(holder.userID)) {
            // if he/she is not the organizer:
            // get the activity's participants


            RequestQueue queue = Volley.newRequestQueue(context);

            /*
             * SELECT DISTINCT ?participantName WHERE{
             *   ?activity
             *      rdf:ID activity.getId().
             *   ?track
             *      ot:from ?activity;
             *      ot:belongsTo ?persona.
             *   ?persona
             *      ot:userName ?participantName.
             * }
             *
             */

            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3FparticipantName+WHERE%7B%0D%0A%3Factivity%0D%0A++rdf%3AID+%22" + activity.getId() + "%22.%0D%0A%3Ftrack%0D%0A++ot%3Afrom+%3Factivity%3B%0D%0A++ot%3AbelongsTo+%3Fpersona.%0D%0A%3Fpersona%0D%0A++ot%3AuserName+%3FparticipantName.%0D%0A%7D" +
                    "&format=json";



            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray result = response.getJSONObject("results").getJSONArray("bindings");
                                for (int i = 0; i < result.length(); i++) {
                                    JSONObject aux = result.getJSONObject(i);
                                    String participant = aux.getJSONObject("participantName").getString("value");
                                    activity.getParticipants().add(participant);
                                }

                                ArrayList<String> participants = activity.getParticipants();

                                if (participants != null) {

                                    if (participants.contains(holder.userID)) {
                                        // if current user is a participant
                                        holder.subscribe_button.setText("Inscrito/a");
                                        holder.subscribe_button.setEnabled(false);
                                    } else {
                                        // if current user is not a participant
                                        holder.subscribe_button.setText("Inscribirme");
                                        holder.subscribe_button.setEnabled(true);
                                    }
                                }

                                // show the subscribe button (only if the current user is not the organizer of the activity)
                                holder.subscribe_button.setVisibility(View.VISIBLE);
                                holder.findActivity_separator.setVisibility(View.VISIBLE);

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
        // if the current user is the organizer, no button will be displayed since it just remains "gone"

        // get and set the activity picture
        Glide.with(context)
                .load(activity.getImage())
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.find_row_imageView);

        holder.subscribe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkKeyDialog(activity, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        // Firebase services
        FirebaseStorage storage;
        StorageReference storageReference;
        FirebaseAuth mAuth;
        FirebaseFirestore db;

        // useful IDs
        String userID;

        // UI elements
        TextView find_title_textView, find_date_textView;
        ImageView find_row_imageView;
        Button subscribe_button;
        View findActivity_separator;
        CircularProgressIndicator progressIndicator;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            // binding UI elements
            find_title_textView = itemView.findViewById(R.id.find_row_title_textView);
            find_date_textView = itemView.findViewById(R.id.find_row_date_textView);
            find_row_imageView = itemView.findViewById(R.id.find_row_imageView);
            subscribe_button = itemView.findViewById(R.id.subscribe_button);
            findActivity_separator = itemView.findViewById(R.id.finActivity_separator);
            progressIndicator = itemView.findViewById(R.id.findActivity_progressIndicator);

            // initializing Firebase services
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            // getting IDs
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    private void checkKeyDialog(ActivityLOD activity, @NonNull MyViewHolder holder) {

        final EditText input = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Introduzca la clave de acceso (4 caracteres)")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input_key = input.getText().toString().trim();
                        if (input_key.equals(/*activity.getKey())*/"")) { //Deleted password for demo

                            /*
                            * SELECT DISTINCT ?activity ?person WHERE {
                            *   ?person
                            *       ot:userName holder.userID.
                            *   ?activity
                            *       rdf:ID activity.getID().
                            * }
                            *
                            */

                            RequestQueue queue = Volley.newRequestQueue(context);

                            String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=SELECT+DISTINCT+%3Factivity+%3Fperson+WHERE%7B%0D%0A%3Fperson+%0D%0Aot%3AuserName+" + "\"" + holder.userID + "\"" + ".%0D%0A%3Factivity+%0D%0Ardf%3AID+" + "\"" + activity.getId() + "\".%0D%0A%7D%0D%0A+"
                                    + "&format=json";


                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                JSONObject result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0);
                                                String personIRI = result.getJSONObject("person").getString("value");
                                                String activityIRI = result.getJSONObject("activity").getString("value");

                                                insertParticipation(personIRI.split("#")[1], activityIRI.split("#")[1], holder, activity);


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
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Clave incorrecta")
                                    .setMessage("La clave introducida para esa actividad es incorrecta")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void insertParticipation(String personIRI, String activityIRI, @NonNull MyViewHolder holder, ActivityLOD activity) {
        RequestQueue queue = Volley.newRequestQueue(context);


        /*
         * INSERT DATA {
         *   GRAPH <http://localhost:8890/DAV> {
         *       ot:TrackIRI ot:belongsTo ot:PersonIRI;
         *       ot:from ot:ActivityIRI.
         *   }
         * }
         * */

        String url = "http://192.168.137.1:8890/sparql?default-graph-uri=&query=INSERT+DATA+%7B%0D%0AGRAPH+%3Chttp%3A%2F%2Flocalhost%3A8890%2FDAV%3E+%7B%0D%0Aot%3A" + UUID.randomUUID().toString() + "+ot%3AbelongsTo+ot:" + personIRI + "%3B%0D%0A+ot%3Afrom+ot:" + activityIRI + ";+ot:trackState+\"NOT_YET\";+ot:completed+false+%0D%0A+%7D%0D%0A%7D&format=json";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = response.getJSONObject("results").getJSONArray("bindings").getJSONObject(0);
                            String okMessage = result.getJSONObject("callret-0").getString("value");
                            if (okMessage.equals("Insert into <http://localhost:8890/DAV>, 4 (or less) triples -- done")) {
                                holder.progressIndicator.setVisibility(View.GONE);
                                holder.subscribe_button.setText("Inscrito/a");
                                holder.subscribe_button.setEnabled(false);
                                Toast.makeText(context, "La inscripción se ha completado correctamente", Toast.LENGTH_LONG).show();
                            } else {
                                holder.progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(context, "La inscripción no pudo completarse. Vuelve a intentarlo", Toast.LENGTH_LONG).show();
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
    }
}
