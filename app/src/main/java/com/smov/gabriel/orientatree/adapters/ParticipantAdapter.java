package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.ActivityLOD;
import com.smov.gabriel.orientatree.ui.ReachesActivity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.User;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity participantsListActivity;
    private ArrayList<Participation> participants;
    private int position;
    private Participation participation;

    private ActivityLOD activity;

    public ParticipantAdapter(android.app.Activity pActivity, Context context, ArrayList<Participation> participants,
                               ActivityLOD activity) {
        this.context = context;
        this.participants = participants;
        this.activity = activity;
        this.participantsListActivity = pActivity;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_participant, parent, false);
        return new ParticipantAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {

        this.position = position ;

        participation = participants.get(position);
        String userID = participation.getParticipant();

        Date start = participation.getStartTime();
        Date finish = participation.getFinishTime();

        Date activityFinish = activity.getFinishTime();
        Date current_time = new Date(System.currentTimeMillis());

        FirebaseUser current_user = holder.mAuth.getCurrentUser();
        String current_userID = current_user.getUid();

        // formatting date in order to display it on card
        String pattern = "HH:mm:ss";
        DateFormat df_hour = new SimpleDateFormat(pattern);

        holder.db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null) {
                            holder.name_textView.setText(user.getName());
                            if(user.isHasPhoto()) {
                                StorageReference ref = holder.storageReference.child("profileImages/" + user.getId());
                                Glide.with(context)
                                        .load(ref)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                                        .skipMemoryCache(true) // prevent caching
                                        .into(holder.participant_circleImageView);
                            }
                        }
                    }
                });

        // set participation state and timing
        if(participation.isCompleted()) {
            holder.participantState_textView.setText("Completada");
        } else {
            // if participation not completed
            if(current_time.after(activityFinish)) {
                // if time to finish is over
                holder.participantState_textView.setText("Terminada");
            } else {
                // if activity did not finish yet
                switch (participation.getState()) {
                    case NOT_YET:
                        // activity not finished and participant not started
                        holder.participantState_textView.setText("Sin comenzar");
                        break;
                    case NOW:
                        // activity not finished and participant started
                        holder.participantState_textView.setText("Comenzada");
                        break;
                    case FINISHED:
                        // if participation is finished even if the activity is not over
                        holder.participantState_textView.setText("Terminada");
                        break;
                }
            }
        }

        // set start and finish
        if(start != null) {
            holder.start_textView.setText("Hora salida: " + df_hour.format(start));
        } else {
            holder.start_textView.setText("Hora salida: ---");
        }
        if(finish != null) {
            holder.finish_textView.setText("Hora fin: " + df_hour.format(finish));
        } else {
            holder.finish_textView.setText("Hora fin: ---");
        }

        /*if(template.getType() == TemplateType.DEPORTIVA) {
            if(participation.isCompleted()) {
                holder.participantPosition_textView.setText(String.valueOf(position + 1));
            } else {
                holder.participantPosition_textView.setText("--");
            }
            if(finish != null && start != null) {
                long diff_millis = Math.abs(finish.getTime() - start.getTime());
                holder.participantTotalTime_textView.setText(formatMillis(diff_millis));
            } else {
                holder.participantTotalTime_textView.setText("");
            }

            if(position <= 2 && participation.isCompleted()) {
                holder.participantClassification_layout.setBackgroundColor(Color.parseColor("#A5E887"));
            } else if(position >= 2 && participation.isCompleted()) {
                holder.participantClassification_layout.setBackgroundColor(Color.parseColor("#FFA233"));
            }

            holder.participantClassification_layout.setVisibility(View.VISIBLE);
        }*/

        holder.row_participant_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches(userID, current_userID);
            }
        });

    }

    // update UI only if we are the participant, or we are the organizer of the activity
    private void updateUIReaches(String participantID, String current_userID) {
        if( activity != null
                && (participantID.equals(current_userID)
                    || current_userID.equals(activity.getPlanner_id()))) {
            Intent intent = new Intent(context, ReachesActivity.class);
            intent.putExtra("activity", activity);
            intent.putExtra("participation", participation);
            intent.putExtra("participantID", participantID);
            participantsListActivity.startActivityForResult(intent, 1);
        }
    }

    private String formatMillis (long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String time = hours % 24 + "h" + minutes % 60 + "m" + seconds % 60 + "s";
        return time;
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseFirestore db;

        FirebaseAuth mAuth;

        FirebaseStorage storage;
        StorageReference storageReference;

        LinearLayout row_participant_layout, participantClassification_layout;

        CircleImageView participant_circleImageView;
        TextView name_textView, start_textView, finish_textView, participantState_textView,
            participantTotalTime_textView, participantPosition_textView;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            db = FirebaseFirestore.getInstance();

            mAuth = FirebaseAuth.getInstance();

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            participant_circleImageView = itemView.findViewById(R.id.participant_row_circleImageView);
            name_textView = itemView.findViewById(R.id.participantName_textView);
            participantState_textView = itemView.findViewById(R.id.participantState_textView);
            start_textView = itemView.findViewById(R.id.participantStart_row_textView);
            finish_textView = itemView.findViewById(R.id.participantFinish_row_textView);
            row_participant_layout = itemView.findViewById(R.id.row_participant_layout);
            participantPosition_textView = itemView.findViewById(R.id.participantPosition_textView);
            participantTotalTime_textView = itemView.findViewById(R.id.participantTotalTime_textView);
            participantClassification_layout = itemView.findViewById(R.id.participantClassification_layout);
        }
    }
}
