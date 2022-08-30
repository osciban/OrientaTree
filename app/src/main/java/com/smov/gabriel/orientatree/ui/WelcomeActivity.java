package com.smov.gabriel.orientatree.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.smov.gabriel.orientatree.R;

public class WelcomeActivity extends AppCompatActivity {

    private TextView welcome_textView, message_textView;

    private Animation animationFadeIn;

    private String name;

    private int previousActivity; // flag para saber si venimos de login o sign up... el mensaje mostrado sera distinto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

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

        Bundle bundle = this.getIntent().getExtras();
        name = bundle.getString("name");
        previousActivity = bundle.getInt("previousActivity");

        welcome_textView = findViewById(R.id.welcome_textView);
        message_textView = findViewById(R.id.message_textView);

        welcome_textView.setText("Hola " + name + ",");

        message_textView.setText("bienvenido/a a OrientaTree");

        /*switch (previousActivity) {
            case 0:
                message_textView.setText("hemos completado tu registro");
                break;
            case 1:
                message_textView.setText("nos alegra verte de nuevo");
                break;
            default:
                message_textView.setText("nos alegra verte de nuevo");
                break;
        }*/

        animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.animation_fade_in);

        welcome_textView.setAnimation(animationFadeIn);
        message_textView.setAnimation(animationFadeIn);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);

    }
}