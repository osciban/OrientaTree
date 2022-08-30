package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.services.LocationService;

import org.jetbrains.annotations.NotNull;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private Button updateButton;
    private MaterialButton editPicture_button;
    private TextInputLayout name_textInputLayout, surname_textInputLayout;
    private TextInputEditText editName_editText, editSurname_editText;
    private CircleImageView profileCircleImageView;
    private CircularProgressIndicator profile_progressIndicator;

    // to show the navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    // user name, e-mail and image that the navigation drawer displays
    private TextView name_textView;
    private TextView email_textView;
    private CircleImageView profile_circleImageView;
    // user data stored in Auth user, and that is shown in the navigation drawer

    private String userID, userEmail, userName, userSurname;

    private User currentUser;

    private Uri galleryImageUri;

    private boolean imageChanged = false;

    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private final int GALLERY_IMAGE_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        /** secure against not logged access **/
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("onReceive", "Logout in progress");
                //At this point you should start the login activity and finish this one
                updateUIIdentification();
            }
        }, intentFilter);
        //** **//

        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userID = mAuth.getCurrentUser().getUid();
        userEmail = mAuth.getCurrentUser().getEmail();
        userName = mAuth.getCurrentUser().getDisplayName();

        toolbar = findViewById(R.id.editProfile_toolbar);
        setSupportActionBar(toolbar);

        updateButton = findViewById(R.id.updeteProfile_button);
        editPicture_button = findViewById(R.id.editPicture_button);
        name_textInputLayout = findViewById(R.id.editName_TextField);
        surname_textInputLayout = findViewById(R.id.editSurname_TextField);
        profileCircleImageView = findViewById(R.id.editProfile_circleImageView);
        editName_editText = findViewById(R.id.editName_editText);
        editSurname_editText = findViewById(R.id.editSurname_editText);
        profile_progressIndicator = findViewById(R.id.profile_progressIndicator);

        // setting the navigation drawer...
        drawerLayout = findViewById(R.id.drawer_layout_profile);
        navigationView = findViewById(R.id.nav_view_profile);
        // setting the navigation drawer's heading
        View hView = navigationView.getHeaderView(0);
        name_textView = hView.findViewById(R.id.name_textView);
        email_textView = hView.findViewById(R.id.email_textView);
        profile_circleImageView = hView.findViewById(R.id.profile_circleImageView);
        name_textView.setText(userName);
        email_textView.setText(userEmail);
        if (user.getPhotoUrl() != null) {
            StorageReference ref = storageReference.child("profileImages/" + userID);
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profileCircleImageView); // set in the activity picture
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profile_circleImageView); // set into drawer picture
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

        editName_editText.setText(userName);
        System.out.println("Entro a menu 6");
        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser.getSurname() != null) {
                            editSurname_editText.setText(currentUser.getSurname());
                            userSurname = currentUser.getSurname();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "No se pudieron recuperar los datos, vuelva a intentarlo en unos instantes", Toast.LENGTH_SHORT).show();
                    }
                });

        editPicture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // choose picture and set it on imageViews, but not upload it yet
                choosePicture();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageChanged) { // if user changed the picture, upload it
                    currentUser.setHasPhoto(true);
                    uploadPicture();
                }
                String name = name_textInputLayout.getEditText().getText().toString().trim();
                String surname = surname_textInputLayout.getEditText().getText().toString().trim();
                if (!name.equals(userName) || !surname.equals(userSurname)) {
                    if(name.length() <= 0) {
                        name_textInputLayout.setError("Campo obligatorio");
                        name_textInputLayout.setErrorEnabled(true);
                        return;
                    } else {
                        name_textInputLayout.setErrorEnabled(false);
                    }
                    if(surname.length() <= 0) {
                        surname_textInputLayout.setError("Campo obligatorio");
                        surname_textInputLayout.setErrorEnabled(true);
                        return;
                    } else {
                        surname_textInputLayout.setErrorEnabled(false);
                    }
                    // update name and surname
                    profile_progressIndicator.setVisibility(View.VISIBLE);
                    // update the information in Auth
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    // set the data that is stored in the user object of Firebase Auth
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // if Firebase Auth data successfully set
                                        // update Firestore object data
                                        System.out.println("Entro a menu 8");
                                        db.collection("users").document(userID)
                                                .update("name", name,
                                                        "surname", surname)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        profile_progressIndicator.setVisibility(View.GONE);
                                                        userName = name;
                                                        userSurname = surname;
                                                        Toast.makeText(EditProfileActivity.this, "Nombre y apellidos actualizados", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull @NotNull Exception e) {
                                                        profile_progressIndicator.setVisibility(View.GONE);
                                                        Toast.makeText(EditProfileActivity.this, "Algo salió mal al actualizar el nombre y los apellidos. Vuelva a intentarlo", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_overflow_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile_log_out_item:
                logOut();
                break;
            case R.id.delete_account_item:
                deleteAccount();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GALLERY_IMAGE_CODE) {
            if (data != null && data.getData() != null) {
                switch (resultCode) {
                    case RESULT_OK:
                        galleryImageUri = data.getData();
                        profileCircleImageView.setImageURI(galleryImageUri); // set image view
                        profile_circleImageView.setImageURI(galleryImageUri); // set drawer image view
                        imageChanged = true;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // update image Uri
    private void setUserProfileUrl(Uri uri) {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        user.updateProfile(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "No se pudo actualizar la imagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // choose picture from galery
    private void choosePicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, GALLERY_IMAGE_CODE);
    }

    // upload picture to Firebase Storage
    private void uploadPicture() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Uploading image...");
        pd.show();
        StorageReference ref = storageReference.child("profileImages/" + userID);
        ref.putFile(galleryImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        pd.dismiss();
                        setUserProfileUrl(galleryImageUri);
                        imageChanged = false;
                        // update the attribute to signal if the user has or not photo
                        db.collection("users").document(userID)
                                .update("hasPhoto", currentUser.isHasPhoto())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                                        // TODO
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(EditProfileActivity.this, "No se pudo actualizar la imagen", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        pd.setMessage("Percentage: " + (int) progressPercent + "%");
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.my_activities_item:
                updateUIHome();
                break;
            case R.id.organize_activity_item:
                updateUIFindTemplate();
                break;
            case R.id.credits_item:
                updateUICredits();
                break;
            case R.id.profile_settings_item:
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
                            stopService(new Intent(EditProfileActivity.this, LocationService.class));
                        }
                        updateUIIdentification();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteAccount() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("¿Realmente desea eliminar su perfil y todos sus datos de manera permanente? " +
                        "(la acción no es reversible)")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // on first place we try to delete the profile picture
                        deleteProfilePicture();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteProfilePicture() {
        profile_progressIndicator.setVisibility(View.VISIBLE);
        StorageReference ref = storageReference.child("profileImages/" + userID);
        ref.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<Void> task) {
                // both if the deletion was successful or not, we delete the user data in Firestore
                deleteUserData();
            }
        });
    }

    private void deleteUserData() {
        db.collection("users").document(userID)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                        // both if the deletion was successful or not, we delete the user in Auth
                        deleteAuth();
                    }
                });
    }

    private void deleteAuth() {
        FirebaseUser user = mAuth.getCurrentUser();
        user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                profile_progressIndicator.setVisibility(View.GONE);
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("com.package.ACTION_LOGOUT");
                sendBroadcast(broadcastIntent);
                updateUIIdentification();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                profile_progressIndicator.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Algo salió mal al eliminar su cuenta. Vuelva a intentarlo", Toast.LENGTH_SHORT).show();
                Log.d("TAG", e.toString());
                Log.d("TAG", e.getMessage());
            }
        });
    }

    private void updateUIFindTemplate() {
        Intent intent = new Intent(EditProfileActivity.this, FindTemplateActivity.class);
        startActivity(intent);
    }

    private void updateUIHome() {
        Intent intent = new Intent(EditProfileActivity.this, HomeActivity.class);
        startActivity(intent);
    }

    private void updateUIIdentification() {
        Intent intent = new Intent(EditProfileActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUICredits() {
        Intent intent = new Intent(EditProfileActivity.this, CreditsActivity.class);
        startActivity(intent);
    }
}