package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class elevencommercehome extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;
    private ViewFlipper imageViewFlipper;

    private LinearLayout assignment;
    private LinearLayout notices;
    private LinearLayout eventCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elevencommercehome);

        // Start ChatNotificationService for e-commerce notifications
        Intent svcIntent = new Intent(this, ChatNotificationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svcIntent);
        } else {
            startService(svcIntent);
        }

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupViewFlipper();
        setupUserProfile();
        setupCardClickListeners(); // FIXED: was calling setupCardClickListeners() but method was named setupCards()

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Animate toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            UIAnimator.animateToolbar(toolbarView, 200);
        }
        
        // Animate menu items after a delay
        mainContent.postDelayed(() -> animateMenuItems(), 500);
    }

    private void animateMenuItems() {
        // Find and animate all menu items
        LinearLayout assignment = findViewById(R.id.assignment);
        LinearLayout notices = findViewById(R.id.notices);
        LinearLayout event = findViewById(R.id.event);
        
        if (assignment != null) {
            UIAnimator.animateLinearLayoutItems(assignment, 700, 100);
        }
        if (notices != null) {
            UIAnimator.animateLinearLayoutItems(notices, 800, 100);
        }
        if (event != null) {
            UIAnimator.animateLinearLayoutItems(event, 900, 100);
        }
        
        // Add click animations to menu items
        setupClickAnimation(assignment);
        setupClickAnimation(notices);
        setupClickAnimation(event);
    }
    
    private void setupClickAnimation(View view) {
        if (view != null) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false; // Very important: return false so the real OnClickListener still fires!
            });
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        imageViewFlipper = findViewById(R.id.viewFlipper);

        assignment = findViewById(R.id.assignment);
        notices = findViewById(R.id.notices);
        eventCard = findViewById(R.id.event);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("11th Commerce Dashboard");
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

    private void setupViewFlipper() {
        if (imageViewFlipper != null) {
            imageViewFlipper.setFlipInterval(3000);
            imageViewFlipper.setAutoStart(true);
            imageViewFlipper.setInAnimation(this, android.R.anim.slide_in_left);
            imageViewFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
            imageViewFlipper.startFlipping();
        }
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            userProfileImage = headerView.findViewById(R.id.userimage);
            
            // First try SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String studentName = prefs.getString("studentName", "");
            
            // If not found in prefs, fetch from Firebase
            if (studentName == null || studentName.isEmpty()) {
                fetchStudentNameFromFirebase();
            } else {
                usernameText.setText("Welcome, " + studentName);
            }
        }
    }
    
    private void fetchStudentNameFromFirebase() {
        String studentId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentId", "");
        if (!studentId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Students")
                    .child(studentId)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    usernameText.setText("Welcome, " + name);
                                    // Save to prefs for future use
                                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("studentName", name)
                                            .apply();
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                            usernameText.setText("Welcome, Student");
                        }
                    });
        } else {
            usernameText.setText("Welcome, Student");
        }
    }

    // FIXED: Renamed from setupCards() to setupCardClickListeners() to match the onCreate() call
    private void setupCardClickListeners() {
        if (assignment != null) {
            assignment.setOnClickListener(v -> {
                Intent intent = new Intent(elevencommercehome.this, studentassignment.class);
                intent.putExtra("singleCategory", true);
                intent.putExtra("std", "11");
                intent.putExtra("stream", "Commerce");
                startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
            });
        }

        if (notices != null) {
            notices.setOnClickListener(v -> {
                Intent noticeIntent = new Intent(this, StudentNoticesActivity.class);
                noticeIntent.putExtra("std", "11");
                noticeIntent.putExtra("stream", "Commerce");
                startActivity(noticeIntent);
            });
        }

        if (eventCard != null) {
            eventCard.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentEcommerceActivity.class));
            });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageViewFlipper != null && imageViewFlipper.isFlipping()) {
            imageViewFlipper.stopFlipping();
        }
    }
}