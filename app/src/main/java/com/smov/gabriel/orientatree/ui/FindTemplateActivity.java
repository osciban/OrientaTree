package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.adapters.TemplateAdapter;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.services.LocationService;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindTemplateActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private Toolbar toolbar;
    private ActionBar ab;

    // to show the navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    // user name, e-mail and image that the navigation drawer displays
    private TextView name_textView;
    private TextView email_textView;
    private CircleImageView profile_circleImageView;
    // user data stored in Auth user, and that is shown in the navigation drawer
    String userID, userEmail, userName;

    private RecyclerView template_recyclerview;
    private TemplateAdapter templateAdapter;
    private ArrayList<Template> templates;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_template);

        /** secure against not logged access **/
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("onReceive","Logout in progress");
                //At this point you should start the login activity and finish this one
                updateUIIdentification();
            }
        }, intentFilter);
        //** **//

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        userID = mAuth.getCurrentUser().getUid();
        userEmail = mAuth.getCurrentUser().getEmail();
        userName = mAuth.getCurrentUser().getDisplayName();

        toolbar = findViewById(R.id.new_activity_toolbar);
        setSupportActionBar(toolbar);
        //ab = getSupportActionBar();
        //ab.setHomeAsUpIndicator(R.drawable.ic_close);
        //ab.setDisplayHomeAsUpEnabled(true);

        // setting the navigation drawer...
        drawerLayout = findViewById(R.id.drawer_layout_template);
        navigationView = findViewById(R.id.nav_view_template);
        // setting the navigation drawer's heading
        View hView =  navigationView.getHeaderView(0);
        name_textView = hView.findViewById(R.id.name_textView);
        email_textView = hView.findViewById(R.id.email_textView);
        profile_circleImageView = hView.findViewById(R.id.profile_circleImageView);
        name_textView.setText(userName);
        email_textView.setText(userEmail);
        // dowloading the profile pic and show in navigation drawer...
        if(mAuth.getCurrentUser().getPhotoUrl() != null) {
            StorageReference ref = storageReference.child("profileImages/" + userID);
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profile_circleImageView);
        }
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.openNavDrawer,
                R.string.closeNavDrawer
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        templates = new ArrayList<>();

        // get the templates...
        db.collection("templates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Template template = document.toObject(Template.class);
                            templates.add(template);
                        }
                        templateAdapter = new TemplateAdapter(FindTemplateActivity.this, FindTemplateActivity.this, templates);
                        template_recyclerview = findViewById(R.id.template_recyclerview);
                        template_recyclerview.setAdapter(templateAdapter);
                        template_recyclerview.setLayoutManager(new GridLayoutManager(FindTemplateActivity.this, 2));
                    }
                });
    }

    // show the search menu and perform its logic
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                templateAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    // navigation drawer actions...
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.my_activities_item:
                updateUIHome();
                break;
            case R.id.organize_activity_item:
                break;
            case R.id.profile_settings_item:
                updateUIEditProfile();
                break;
            case R.id.credits_item:
                updateUICredits();
                break;
            case R.id.log_out_item:
                logOut();
                break;
        }
        //close navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    private void logOut() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("¿Desea salir de su sesión?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAuth.signOut();
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("com.package.ACTION_LOGOUT");
                        sendBroadcast(broadcastIntent);
                        if(LocationService.executing) {
                            stopService(new Intent(FindTemplateActivity.this, LocationService.class));
                        }
                        updateUIIdentification();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateUICredits() {
        Intent intent = new Intent(FindTemplateActivity.this, CreditsActivity.class);
        startActivity(intent);
    }

    private void updateUIIdentification() {
        Intent intent = new Intent(FindTemplateActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIEditProfile() {
        Intent intent = new Intent(FindTemplateActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void updateUIHome() {
        Intent intent = new Intent(FindTemplateActivity.this, HomeActivity.class);
        startActivity(intent);
    }
}