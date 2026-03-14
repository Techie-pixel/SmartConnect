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

public class teacherfeedback extends AppCompatActivity
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
        setContentView(R.layout.activity_teacherfeedback);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        // Firebase Database reference (Folder name "Feedback")
        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback");

        // Submit feedback to Firebase
        btnSubmit.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                String name = editName.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String contact = editContact.getText().toString().trim();
                String feedback = textfeedback.getText().toString().trim();

                if (name.isEmpty() || email.isEmpty() || contact.isEmpty() || feedback.isEmpty()) {
                    Toast.makeText(teacherfeedback.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                HashMap<String, String> feedbackData = new HashMap<>();
                feedbackData.put("Name", name);
                feedbackData.put("Email", email);
                feedbackData.put("Contact", contact);
                feedbackData.put("Feedback", feedback);

                feedbackRef.push().setValue(feedbackData)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(teacherfeedback.this, "Feedback submitted successfully!", Toast.LENGTH_SHORT)
                                    .show();
                            editName.setText("");
                            editEmail.setText("");
                            editContact.setText("");
                            textfeedback.setText("");
                        })
                        .addOnFailureListener(e -> Toast
                                .makeText(teacherfeedback.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }, 150);
        });
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

        UIAnimator.animateEditText(editName, 200);
        UIAnimator.animateEditText(editEmail, 300);
        UIAnimator.animateEditText(editContact, 400);
        UIAnimator.animateEditText(textfeedback, 500);
        UIAnimator.animateButton(btnSubmit, 600);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Feedback");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
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
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            userProfileImage = headerView.findViewById(R.id.userimage);
            if (usernameText != null) {
                usernameText.setText("Welcome, Teacher");
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.teacher_home) {
            intent = new Intent(this, teachertab.class);
            startActivity(intent);
            finish();
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_assignments) {
            intent = new Intent(this, teacherassignments.class);
        } else if (id == R.id.teacher_homework) {
            intent = new Intent(this, teacherhomework.class);
        } else if (id == R.id.teacher_syllabus) {
            intent = new Intent(this, teachersyllabus.class);
        } else if (id == R.id.teacher_timetable) {
            intent = new Intent(this, teachertimetable.class);
        } else if (id == R.id.teacher_exam) {
            intent = new Intent(this, teacherexam.class);
        } else if (id == R.id.teacher_calendar) {
            intent = new Intent(this, teachercalender.class);
        } else if (id == R.id.teacher_attendance) {
            intent = new Intent(this, teacherattendance.class);
        } else if (id == R.id.teacher_feedback) {
            drawerLayout.closeDrawers();
            Toast.makeText(this, "Already on Feedback", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.teacher_notices) {
            intent = new Intent(this, TeacherNoticesActivity.class);
        } else if (id == R.id.teacher_fees) {
            intent = new Intent(this, TeacherFeesActivity.class);
        } else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            caIntent.putExtra("senderUid", getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown"));
            startActivity(caIntent);
            drawerLayout.closeDrawers();
            return true;
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawers();
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
