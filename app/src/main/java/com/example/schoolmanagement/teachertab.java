package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class teachertab extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextView tvWelcomeName, tvStandard11, tvStream, tvSubject;
    private LinearLayout cardProfile, cardTeachers, cardEcommerce;
    private DatabaseReference teachersRef;
    private static final String TAG = "teachertab";

    private String teacherId = "";
    private String teacherSubject = "";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachertab);

        // Start ChatNotificationService for e-commerce notifications
        Intent serviceIntent = new Intent(this, ChatNotificationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // ── Toolbar + Drawer (same as principal) ──
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Teacher Dashboard");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        // ── Views ──
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        tvStandard11 = findViewById(R.id.tvStandard11);
        tvStream = findViewById(R.id.tvStream);
        tvSubject = findViewById(R.id.tvSubject);

        cardProfile = findViewById(R.id.cardProfile);
        cardTeachers = findViewById(R.id.cardTeachers);
        cardEcommerce = findViewById(R.id.cardEcommerce);

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        // Animate logo
        ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) {
            UIAnimator.animateImageView(logoImg, 100);
            UIAnimator.startPulseAnimation(logoImg);
        }

        // Animate text views
        UIAnimator.animateTextView(tvWelcomeName, 200);
        UIAnimator.animateTextView(tvStandard11, 300);
        UIAnimator.animateTextView(tvStream, 400);
        UIAnimator.animateTextView(tvSubject, 500);

        // Animate cards
        UIAnimator.animateImageView(cardProfile, 600);
        UIAnimator.animateImageView(cardTeachers, 700);
        UIAnimator.animateImageView(cardEcommerce, 800);

        // ── Firebase ──
        teachersRef = FirebaseDatabase.getInstance().getReference().child("Teachers");

        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);

        teacherId = getIntent().getStringExtra("teacherId");
        // Always prefer explicit id from intent; if not present, fall back to latest
        // saved
        if (teacherId == null || teacherId.isEmpty()) {
            teacherId = tPrefs.getString("teacherId", null);
        } else {
            // overwrite saved id to avoid stale data if a different teacher logs in
            tPrefs.edit().putString("teacherId", teacherId).apply();
        }

        // Read the subject the teacher selected at login
        teacherSubject = tPrefs.getString("teacherSubject", "");

        Log.d(TAG, "teacherId = " + teacherId + ", teacherSubject = " + teacherSubject);

        if (teacherId == null || teacherId.isEmpty()) {
            Toast.makeText(this, "teacherId not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Card click listeners ──
        cardProfile.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, teacherprofile.class);
                intent.putExtra("teacherId", teacherId);
                startActivity(intent);
            }, 150);
        });

        cardTeachers.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, teacherslist.class)), 150);
        });

        cardEcommerce.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, teacherecommerce.class)), 150);
        });

        // Load teacher info from Firebase (no cache; always current)
        loadTeacherData(teacherId);
    }

    // ── Firebase: Teachers/{teacherId}/{pushKey} → {name, class, stream, subject}
    // Match the push-key whose "subject" matches what was selected at login
    private void loadTeacherData(String teacherId) {
        teachersRef.child(teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(teachertab.this,
                                    "Teacher not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Find the correct push-key child matching the logged-in subject
                        DataSnapshot dataSnapshot = snapshot;
                        DataSnapshot firstPushChild = null;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.getKey() != null && child.getKey().startsWith("-")) {
                                if (firstPushChild == null) {
                                    firstPushChild = child; // remember first as fallback
                                }
                                // Match by subject selected at login
                                String childSubject = child.child("subject").getValue(String.class);
                                if (teacherSubject != null && !teacherSubject.isEmpty()
                                        && teacherSubject.equals(childSubject)) {
                                    dataSnapshot = child;
                                    firstPushChild = null; // no need for fallback
                                    break;
                                }
                            }
                        }
                        // If no exact match found, fall back to first push child
                        if (firstPushChild != null) {
                            dataSnapshot = firstPushChild;
                        }

                        String name = dataSnapshot.child("name").getValue(String.class);
                        String cls = dataSnapshot.child("class").getValue(String.class);
                        String stream = dataSnapshot.child("stream").getValue(String.class);
                        String subject = dataSnapshot.child("subject").getValue(String.class);

                        Log.d(TAG, "name=" + name + " class=" + cls + " stream=" + stream + " subject=" + subject);

                        tvWelcomeName.setText(name != null ? "Welcome, " + name : "Welcome");
                        tvStandard11.setText(cls != null ? "Class: " + cls : "Class: -");
                        tvStream.setText(stream != null ? "Stream: " + stream : "Stream: -");
                        tvSubject.setText(subject != null ? "Subject: " + subject : "Subject: -");

                        // Also set nav header username
                        if (name != null && navigationView.getHeaderView(0) != null) {
                            TextView username = navigationView.getHeaderView(0).findViewById(R.id.username);
                            if (username != null) {
                                username.setText("Welcome, " + name);
                                username.setTextColor(getResources().getColor(android.R.color.white));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(teachertab.this,
                                "Failed: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onCancelled: ", error.toException());
                    }
                });
    }

    // ── Navigation drawer ──
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.teacher_home) {
            Toast.makeText(this, "Already on Home Page", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            startActivity(new Intent(this, teacherhomework.class));
        } else if (id == R.id.teacher_syllabus) {
            startActivity(new Intent(this, teachersyllabus.class));
        } else if (id == R.id.teacher_timetable) {
            startActivity(new Intent(this, teachertimetable.class));
        } else if (id == R.id.teacher_exam) {
            startActivity(new Intent(this, teacherexam.class));
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_feedback) {
            startActivity(new Intent(this, teacherfeedback.class));
        } else if (id == R.id.teacher_notices) {
            startActivity(new Intent(this, TeacherNoticesActivity.class));
        } else if (id == R.id.teacher_fees) {
            startActivity(new Intent(this, TeacherFeesActivity.class));
        } else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            String tid = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown");
            caIntent.putExtra("senderUid", tid);
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
