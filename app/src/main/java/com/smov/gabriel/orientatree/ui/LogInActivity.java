package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.smov.gabriel.orientatree.R;

import org.jetbrains.annotations.NotNull;

public class LogInActivity extends AppCompatActivity {

    private TextInputLayout email_textfield, password_textfield;
    private Button logIn_button;
    private CircularProgressIndicator progress_circular;
    private MaterialButton logInPassword_button, logInRegistry_button, logInConfirmation_email;

    private Toolbar toolbar;

    //private ConstraintLayout logIn_layout;

    private String email, password;
    private String name;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            updateUIHome();
        }

        email_textfield = findViewById(R.id.email_textfield);
        password_textfield = findViewById(R.id.password_textfield);
        logIn_button = findViewById(R.id.logIn_button);
        progress_circular = findViewById(R.id.progress_circular);
        logInPassword_button = findViewById(R.id.logInPassword_button);
        logInRegistry_button = findViewById(R.id.logInRegistry_button);
        logInConfirmation_email = findViewById(R.id.logInConfirmation_button);

        // needed to show the snackbar at right place
        final View viewPos = findViewById(R.id.logIn_coordinator_snackBar);

        toolbar = findViewById(R.id.logIn_toolbar);
        setSupportActionBar(toolbar);

        logIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = email_textfield.getEditText().getText().toString().trim();
                password = password_textfield.getEditText().getText().toString().trim();
                boolean any_error = false;
                if(TextUtils.isEmpty(email)) {
                    email_textfield.setError("e-mail obligatorio");
                    any_error = true;
                } else {
                    if(email_textfield.isErrorEnabled()) {
                        email_textfield.setErrorEnabled(false);
                    }
                }
                if(TextUtils.isEmpty(password)) {
                    password_textfield.setError("password obligatorio");
                    any_error = true;
                } else {
                    if(password_textfield.isErrorEnabled()) {
                        password_textfield.setErrorEnabled(false);
                    }
                }
                if(any_error == true) {
                    return;
                }
                progress_circular.setVisibility(View.VISIBLE);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            progress_circular.setVisibility(View.GONE);
                            FirebaseUser user = mAuth.getCurrentUser();
                            name = user.getDisplayName();
                            if(name == null || name.equals("")) {
                                name = "usuario";
                                Toast.makeText(LogInActivity.this, "No pudieron obtenerse los datos del usuario", Toast.LENGTH_SHORT).show();
                            }
                            if(user.isEmailVerified()) {
                                // if the user is verified
                                updateUIWelcome();
                            } else {
                               // if the user is not verified
                                showVerificationSnackBar(viewPos);
                            }
                        } else {
                            try {
                                throw task.getException();
                            }
                            catch (FirebaseAuthInvalidUserException | FirebaseAuthInvalidCredentialsException e) {
                                progress_circular.setVisibility(View.GONE);
                                showSnackBar(viewPos);
                            }
                            catch (Exception e) {
                                progress_circular.setVisibility(View.GONE);
                                Toast.makeText(LogInActivity.this, "Algo no funcionó: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });

        logInRegistry_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUISignUp();
            }
        });

        logInConfirmation_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerificationEmail();
            }
        });

        logInPassword_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = email_textfield.getEditText().getText().toString().trim();
                if(TextUtils.isEmpty(email)) {
                    email_textfield.setError("e-mail obligatorio");
                }else {
                    if(email_textfield.isErrorEnabled()) {
                        email_textfield.setErrorEnabled(false);
                    }
                    new MaterialAlertDialogBuilder(LogInActivity.this)
                            .setMessage("Enviar correo de recuperación de contraseña a " + 
                                    email)
                            .setTitle("Recuperación de contraseña")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(mAuth != null) {
                                        progress_circular.setVisibility(View.VISIBLE);
                                        mAuth.sendPasswordResetEmail(email)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        progress_circular.setVisibility(View.GONE);
                                                        Toast.makeText(LogInActivity.this, "E-mail de recuperación enviado", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull @NotNull Exception e) {
                                                        progress_circular.setVisibility(View.GONE);
                                                        Toast.makeText(LogInActivity.this, "Error al enviar el e-mail de recuperación, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(LogInActivity.this, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .show();
                }
            }
        });

    }

    private void showSnackBar(View viewPos) {
        Snackbar.make(viewPos, "Usuario o password incorrectos", Snackbar.LENGTH_LONG)
            .setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            })
             .setDuration(8000)
             .show();
    }

    private void showVerificationSnackBar(View viewPos) {
        Snackbar.make(viewPos, "Su correo no está verificado. Envie email de verificación", Snackbar.LENGTH_LONG)
                .setAction("Enviar", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendVerificationEmail();
                    }
                })
                .setDuration(8000)
                .show();
    }

    private void updateUIHome() {
        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void sendVerificationEmail() {
        progress_circular.setVisibility(View.VISIBLE);
        logInConfirmation_email.setEnabled(true);
        logInConfirmation_email.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                progress_circular.setVisibility(View.GONE);
                Toast.makeText(LogInActivity.this, "Mensaje de confirmación enviado", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                progress_circular.setVisibility(View.GONE);
                Toast.makeText(LogInActivity.this, "No se pudo enviar mensaje de confirmación. " +
                        "espere unos minutos y vuelva a intentarlo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWelcome() {
        Intent intent = new Intent(LogInActivity.this, WelcomeActivity.class);
        Bundle b = new Bundle();
        b.putString("name", name);
        b.putInt("previousActivity", 1); // flag to signal in next activity wether we come from log-in or sign-up
        intent.putExtras(b);
        startActivity(intent);
        finish();
    }

    private void updateUISignUp() {
        Intent intent = new Intent(LogInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }
}