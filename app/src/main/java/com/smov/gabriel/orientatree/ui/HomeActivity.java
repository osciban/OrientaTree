package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.services.LocationService;
import com.smov.gabriel.orientatree.ui.fragments.CompletedFragment;
import com.smov.gabriel.orientatree.ui.fragments.OnGoingFragment;
import com.smov.gabriel.orientatree.ui.fragments.ProgrammedFragment;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;

    // to show the navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // user name, e-mail and image that the navigation drawer displays
    private TextView name_textView;
    private TextView email_textView;
    private CircleImageView profile_circleImageView;

    // to show the tabs
    private ViewPager viewPager;
    private TabLayout tabLayout;

    // fragment of each tab
    private CompletedFragment completedFragment;
    private OnGoingFragment onGoingFragment;
    private ProgrammedFragment programmedFragment;

    // useful to reset the last tab when coming back from another activity
    // not always working... don't know why
    private int tabSelected = 0;

    private FloatingActionButton fab;

    // user data stored in Auth user
    public String userID, userEmail, userName;

    // Firebase services
    public FirebaseAuth mAuth;
    public FirebaseFirestore db;
    public FirebaseStorage storage;
    public StorageReference storageReference;
    public FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        userID = user.getUid();
        userName = user.getDisplayName();
        userEmail = user.getEmail();

        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        fab = findViewById(R.id.floating_action_button);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIFindActivity();
            }
        });

        toolbar = findViewById(R.id.home_toolbar);

        setSupportActionBar(toolbar);

        // navigation drawer...
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        View hView = navigationView.getHeaderView(0);
        // setting navigation drawer header...
        name_textView = hView.findViewById(R.id.name_textView);
        email_textView = hView.findViewById(R.id.email_textView);
        profile_circleImageView = hView.findViewById(R.id.profile_circleImageView);
        name_textView.setText(userName);
        email_textView.setText(userEmail);

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

        // tabs...
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        // fragment initialization
        completedFragment = new CompletedFragment();
        onGoingFragment = new OnGoingFragment();
        programmedFragment = new ProgrammedFragment();
        // binding tabs and fragments
        tabLayout.setupWithViewPager(viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.adFragment(completedFragment, "pasadas");
        viewPagerAdapter.adFragment(onGoingFragment, "en curso");
        viewPagerAdapter.adFragment(programmedFragment, "previstas");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setTabTextColors(R.color.black, R.color.black); // tab text color black, both selected and unselected

        // perform some other actions when clicking specific tab item (like showing or hiding fab)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        tabSelected = 0;
                        break;
                    case 1:
                        tabSelected = 1;
                        break;
                    case 2:
                        tabSelected = 2;
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                /*switch (tab.getPosition()) {
                    case 1:
                        fab.show();
                        break;
                }*/
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // dowloading the profile pic and show in navigation drawer...
        if (user.getPhotoUrl() != null) {
            StorageReference ref = storageReference.child("profileImages/" + userID);
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profile_circleImageView);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        viewPager.setCurrentItem(tabSelected);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile_settings_item:
                updateUIEditProfile();
                break;
            case R.id.organize_activity_item:
                updateUIFindTemplate();
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
                            stopService(new Intent(HomeActivity.this, LocationService.class));
                        }
                        updateUIIdentification();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    // inner created class, needed for tabs
    private class ViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments = new ArrayList<>();
        private List<String> fragmentTitles = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        public void adFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            fragmentTitles.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitles.get(position);
        }
    }

    public void updateUIIdentification() {
        Intent intent = new Intent(HomeActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIEditProfile() {
        Intent intent = new Intent(HomeActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void updateUIFindTemplate() {
        Intent intent = new Intent(HomeActivity.this, FindTemplateActivity.class);
        startActivity(intent);
    }

    private void updateUIFindActivity() {
        Intent intent = new Intent(HomeActivity.this, FindActivityActivity.class);
        startActivity(intent);
    }

    private void updateUICredits() {
        Intent intent = new Intent(HomeActivity.this, CreditsActivity.class);
        startActivity(intent);
    }
}