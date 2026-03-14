package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class studentfeedback extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    EditText editName, editEmail, editContact, textfeedback;
    Button btnSubmit;

    DatabaseReference feedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentfeedback);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        // Firebase Database reference (Folder name "Feedback")
        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback");

        // ✅ Submit feedback to Firebase
        btnSubmit.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String contact = editContact.getText().toString().trim();
            String feedback = textfeedback.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || contact.isEmpty() || feedback.isEmpty()) {
                Toast.makeText(studentfeedback.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, String> feedbackData = new HashMap<>();
            feedbackData.put("Name", name);
            feedbackData.put("Email", email);
            feedbackData.put("Contact", contact);
            feedbackData.put("Feedback", feedback);

            feedbackRef.push().setValue(feedbackData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(studentfeedback.this, "Feedback submitted successfully!", Toast.LENGTH_SHORT)
                                .show();
                        editName.setText("");
                        editEmail.setText("");
                        editContact.setText("");
                        textfeedback.setText("");
                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(studentfeedback.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        
        // Add animations to UI elements
        animateUIElements();
    }
    
    private void animateUIElements() {
        // Animate toolbar
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            UIAnimator.animateToolbar(toolbar, 200);
        }
        
        // Animate EditTexts sequentially
        if (editName != null) {
            UIAnimator.animateEditText(editName, 400);
        }
        if (editEmail != null) {
            UIAnimator.animateEditText(editEmail, 500);
        }
        if (editContact != null) {
            UIAnimator.animateEditText(editContact, 600);
        }
        if (textfeedback != null) {
            UIAnimator.animateEditText(textfeedback, 700);
        }
        
        // Animate button
        if (btnSubmit != null) {
            UIAnimator.animateButton(btnSubmit, 800);
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editContact = findViewById(R.id.editContact);
        textfeedback = findViewById(R.id.textfeedback);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Feedback");
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