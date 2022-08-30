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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.FindActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;

import java.util.ArrayList;
import java.util.Date;

public class FindActivityActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ActionBar ab;

    private CircularProgressIndicator progressIndicator;

    private RecyclerView find_activity_recyclerview;
    private FindActivityAdapter findActivityAdapter;
    private ArrayList<Activity> activities;
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
        SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                progressIndicator.setVisibility(View.VISIBLE);
                helper_layout.setVisibility(View.GONE);
                activities = new ArrayList<>();
                long millis=System.currentTimeMillis();
                Date current_date = new Date(millis );
                System.out.println("Entro a menu 7");
                db.collection("activities")
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
                        });
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