package com.smov.gabriel.orientatree.adapters;

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
import com.smov.gabriel.orientatree.model.Participation;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FindActivityAdapter extends RecyclerView.Adapter<FindActivityAdapter.MyViewHolder> {

    private Context context;

    private ArrayList<Activity> activities;
    private int position;

    public FindActivityAdapter(Context context, ArrayList<Activity> activities) {
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
        this.position = position ;
        Activity activity = activities.get(position);

        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        // display title and date
        holder.find_title_textView.setText(activity.getTitle());
        holder.find_date_textView.setText("Fecha: " + dateAsString);

        // check that the current user is not the organizer of the activity
        if(!activity.getPlanner_id().equals(holder.userID)) {
            // if he/she is not the organizer:
            // get the activity's participants
            ArrayList<String> participants = activity.getParticipants();
            if(participants != null) {
                if(participants.contains(holder.userID)) {
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
        }
        // if the current user is the organizer, no button will be displayed since it just remains "gone"

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
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

    private void checkKeyDialog(Activity activity, @NonNull FindActivityAdapter.MyViewHolder holder) {
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
                        if(input_key.equals(activity.getKey())) {
                            activity.addParticipant(holder.userID);
                            holder.progressIndicator.setVisibility(View.VISIBLE);
                            System.out.println("Entro a menu 1");
                            holder.db.collection("activities").document(activity.getId())
                                    .set(activity)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Participation participation = new Participation(holder.userID);
                                            holder.db.collection("activities").document(activity.getId())
                                                    .collection("participations").document(holder.userID)
                                                    .set(participation)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            holder.progressIndicator.setVisibility(View.GONE);
                                                            holder.subscribe_button.setText("Inscrito/a");
                                                            holder.subscribe_button.setEnabled(false);
                                                            Toast.makeText(context, "La inscripción se ha completado correctamente", Toast.LENGTH_LONG).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull @NotNull Exception e) {
                                                            holder.progressIndicator.setVisibility(View.GONE);
                                                            Toast.makeText(context, "La inscripción no pudo completarse. Vuelve a intentarlo", Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        }
                                    });
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
}
