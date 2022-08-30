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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.ui.HomeActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProgrammedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProgrammedFragment extends Fragment implements View.OnClickListener {

    private RecyclerView programmed_recyclerView;
    private ActivityAdapter activityAdapter;
    private ArrayList<Activity> all_activities;
    private  ArrayList<Activity> no_duplicates_activities; // to remove duplicates due to being both organizer and participant

    private SwipeRefreshLayout programmed_pull_layout;

    private HomeActivity homeActivity;

    private ConstraintLayout no_activities_layout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProgrammedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProgrammedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProgrammedFragment newInstance(String param1, String param2) {
        ProgrammedFragment fragment = new ProgrammedFragment();
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
        View view = inflater.inflate(R.layout.fragment_programmed, container, false);

        homeActivity = (HomeActivity)getActivity();

        programmed_pull_layout = view.findViewById(R.id.programmed_pull_layout);
        programmed_pull_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivities(view);
                programmed_pull_layout.setRefreshing(false);
            }
        });

        no_activities_layout = view.findViewById(R.id.programmed_empty_layout);

        getActivities(view);

        return view;
    }

    private void getActivities(View view) {
        all_activities = new ArrayList<>();
        no_duplicates_activities = new ArrayList<>();

        long millis=System.currentTimeMillis();
        Date date = new Date(millis );

        homeActivity.db.collection("activities")
                .whereGreaterThan("startTime", date)
                .whereEqualTo("planner_id", homeActivity.userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Activity activity = document.toObject(Activity.class);
                            all_activities.add(activity);
                        }
                        homeActivity.db.collection("activities")
                                .whereGreaterThan("startTime", date)
                                .whereArrayContains("participants", homeActivity.userID)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Activity activity = document.toObject(Activity.class);
                                            all_activities.add(activity);
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
                                        //activityAdapter = new ActivityAdapter(homeActivity, getContext(), no_duplicates_activities);
                                        programmed_recyclerView = view.findViewById(R.id.programmed_recyclerView);
                                        programmed_recyclerView.setAdapter(activityAdapter);
                                        programmed_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    }
                                });
                    }
                });
    }

    @Override
    public void onClick(View v) {

    }
}