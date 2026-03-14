package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

public class userprofile extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    EditText editTextName, editTextEmail, editTextMobile, editTextStandard, editTextStream;
    Button btnLogout;
    DatabaseReference reference;
    FirebaseAuth auth;

    CircleImageView profileImage;
    TextView changePhoto;

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userprofile);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        profileImage.setOnClickListener(v -> showImageDialog());
        changePhoto.setOnClickListener(v -> showImageDialog());

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("Students");

        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();

            reference.child(uid).get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("Name").getValue(String.class);
                    String email = dataSnapshot.child("Email").getValue(String.class);
                    String mobile = dataSnapshot.child("Mobile").getValue(String.class);
                    String standard = dataSnapshot.child("Standard").getValue(String.class);
                    String stream = dataSnapshot.child("Stream").getValue(String.class);
                    String base64Img = dataSnapshot.child("ProfileImageBase64").getValue(String.class);

                    editTextName.setText("Your Name: " + (name != null ? name : ""));
                    editTextEmail.setText("Your Email: " + (email != null ? email : ""));
                    editTextMobile.setText("Your Mobile: " + (mobile != null ? mobile : ""));
                    editTextStandard.setText("Your Standard: " + (standard != null ? standard : ""));
                    editTextStream.setText("Your Stream: " + (stream != null ? stream : ""));

                    // LOAD PROFILE IMAGE FROM BASE64 OR DEFAULT
                    if (base64Img != null && !base64Img.isEmpty()) {
                        byte[] imgBytes = android.util.Base64.decode(base64Img, android.util.Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                        profileImage.setImageBitmap(bmp);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_default_profile);
                    }
                } else {
                    Toast.makeText(this, "No profile found!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
        }

        btnLogout.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            showLogoutDialog();
        });

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        UIAnimator.animateEditText(editTextName, 200);
        UIAnimator.animateEditText(editTextEmail, 300);
        UIAnimator.animateEditText(editTextMobile, 400);
        UIAnimator.animateEditText(editTextStandard, 500);
        UIAnimator.animateEditText(editTextStream, 600);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        profileImage = findViewById(R.id.profileImage);
        changePhoto = findViewById(R.id.changePhoto);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextMobile = findViewById(R.id.editTextMobile);
        editTextStandard = findViewById(R.id.editTextStandard);
        editTextStream = findViewById(R.id.editTextStream);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void setupNavigationDrawer() {
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if(toggle.getDrawerArrowDrawable() != null) {
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        }
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            userProfileImage = headerView.findViewById(R.id.userimage);
            if (usernameText != null) {
                android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                String studentName = prefs.getString("studentName", "");
                usernameText.setText("Welcome, " + studentName);
            }
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Clear local session cache first!
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            auth.signOut();
            Toast.makeText(userprofile.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(userprofile.this, loginchoice.class));
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showImageDialog() {
        String[] options = { "Camera", "Gallery", "Delete" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else if (which == 1) { // Gallery
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            } else if (which == 2) { // Delete
                deleteProfileImage();
            }
        });
        builder.show();
    }

    // Handle image pick result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap photo = null;
            if (requestCode == CAMERA_REQUEST) {
                photo = (Bitmap) data.getExtras().get("data");
            }
            if (requestCode == GALLERY_REQUEST) {
                Uri imageUri = data.getData();
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (photo != null) {
                profileImage.setImageBitmap(photo);
                uploadImageToDatabase(photo);
            }
        }
    }

    // Upload Base64 string to Database
    private void uploadImageToDatabase(Bitmap bitmap) {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // compress for smaller size
        byte[] imageBytes = baos.toByteArray();
        String base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

        reference.child(uid).child("ProfileImageBase64").setValue(base64String)
                .addOnSuccessListener(
                        aVoid -> Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    // Delete Base64 string from Database
    private void deleteProfileImage() {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();
        reference.child(uid).child("ProfileImageBase64").removeValue();
        profileImage.setImageResource(R.drawable.ic_default_profile);
        Toast.makeText(this, "Profile image removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

        @Override
    public boolean onNavigationItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        android.content.Intent intent = null;

        if (id == R.id.homesection) {
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            if ("11".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, elevensciencehomepage.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, elevencommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, elevenartshome.class);
            } else if ("12".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, twelvesciencehome.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, twelvecommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, twelveartshome.class);
            }
            if (intent == null) intent = new android.content.Intent(this, elevencommercehome.class);
        } else if (id == R.id.profile) {
            intent = new android.content.Intent(this, userprofile.class);
        } else if (id == R.id.feedback) {
            intent = new android.content.Intent(this, studentfeedback.class);
        } else if (id == R.id.gallery) {
            intent = new android.content.Intent(this, studentgallery.class);
        } else if (id == R.id.syllabus) {
            intent = new android.content.Intent(this, studentsyllabus.class);
        } else if (id == R.id.assignment) {
            intent = new android.content.Intent(this, studentassignment.class);
        } else if (id == R.id.exam) {
            intent = new android.content.Intent(this, studentexam.class);
        } else if (id == R.id.calender) {
            intent = new android.content.Intent(this, studentcalender.class);
        } else if (id == R.id.payment) {
            intent = new android.content.Intent(this, studentpayment.class);
        } else if (id == R.id.table) {
            intent = new android.content.Intent(this, studenttimetable.class);
        } else if (id == R.id.homework) {
            intent = new android.content.Intent(this, studenthomework.class);
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            intent.putExtra("singleCategory", true);
            intent.putExtra("std", std);
            intent.putExtra("stream", stream);
        } else if (id == R.id.student) {
            intent = new android.content.Intent(this, studentslist.class);
        } else if (id == R.id.student_attendance) {
            intent = new android.content.Intent(this, studentattendance.class);
        } else if (id == R.id.contact_admin) {
            intent = new android.content.Intent(this, ContactAdminActivity.class);
            intent.putExtra("senderRole", "Student");
            String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentUid", "unknown");
            intent.putExtra("senderUid", uid);
        }

        // Avoid opening the exact same intent class
        if (intent != null && intent.getComponent() != null && this.getClass().getName().equals(intent.getComponent().getClassName())) {
            android.widget.Toast.makeText(this, "Already on this page", android.widget.Toast.LENGTH_SHORT).show();
            if (drawerLayout != null) drawerLayout.closeDrawers();
            return true;
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            if (!this.getClass().getSimpleName().contains("home") && !this.getClass().getSimpleName().contains("homepage")) {
                finish();
            }
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        return true;
    }
@Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}