package com.smov.gabriel.orientatree.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.smov.gabriel.orientatree.R;

public class MainActivity extends AppCompatActivity {

    private TextView orientaTree_textView;
    private ImageView treeLogo_imageView;

    private Animation animationUp;
    private Animation animationDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prevent using dark theme which causes a bug
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Set status bar to white in this activity
        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.white));
        //

        //getSupportActionBar().hide(); // Hide action bar

        animationUp = AnimationUtils.loadAnimation(this, R.anim.animation_up);
        animationDown = AnimationUtils.loadAnimation(this, R.anim.animation_down);

        treeLogo_imageView = findViewById(R.id.treeLogo_imageView);
        orientaTree_textView = findViewById(R.id.orientaTree_textView);

        treeLogo_imageView.setAnimation(animationUp);
        orientaTree_textView.setAnimation(animationDown);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2500);
    }
}