package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ContactAdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText etName, etEmail, etMobile, etStd, etStream, etMessage;
    private LinearLayout tilStd, tilStream;
    private MaterialButton btnSubmit, btnMyMessages;
    private DatabaseReference contactRef;
    private String senderRole, senderUid;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_admin);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);

        // Apply animations
        UIAnimator.animateToolbar(toolbar, 100);
        ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 200);
        
        LinearLayout contentLayout = findViewById(R.id.contentLayout);
        if (contentLayout != null) {
            UIAnimator.animateLinearLayoutItems(contentLayout, 300, 150);
        }

        senderRole = getIntent().getStringExtra("senderRole");
        senderUid = getIntent().getStringExtra("senderUid");

        if (senderRole == null)
            senderRole = "Unknown";
        if (senderUid == null)
            senderUid = "unknown";

        navigationView.getMenu().clear();
        if ("Student".equals(senderRole)) {
            navigationView.inflateMenu(R.menu.drawer_menu);
        } else if ("Teacher".equals(senderRole)) {
            navigationView.inflateMenu(R.menu.teacher_drawer_menu);
        } else if ("Principal".equals(senderRole)) {
            navigationView.inflateMenu(R.menu.principal_drawer_menu);
        } else {
            navigationView.inflateMenu(R.menu.drawer_menu);
        }

        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etStd = findViewById(R.id.etStd);
        etStream = findViewById(R.id.etStream);
        etMessage = findViewById(R.id.etMessage);
        tilStd = findViewById(R.id.tilStd);
        tilStream = findViewById(R.id.tilStream);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnMyMessages = findViewById(R.id.btnMyMessages);

        contactRef = FirebaseDatabase.getInstance().getReference("ContactAdmin");

        senderRole = getIntent().getStringExtra("senderRole");
        senderUid = getIntent().getStringExtra("senderUid");

        if (senderRole == null)
            senderRole = "Unknown";
        if (senderUid == null)
            senderUid = "unknown";

        // Show std/stream only for students
        if ("Student".equals(senderRole)) {
            tilStd.setVisibility(View.VISIBLE);
            tilStream.setVisibility(View.VISIBLE);

            // Auto-fill from prefs
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String std = prefs.getString("Standard", "");
            String stream = prefs.getString("Stream", "");
            String name = prefs.getString("studentName", "");
            String email = prefs.getString("studentEmail", "");
            String mobile = prefs.getString("studentMobile", "");
            if (!std.isEmpty())
                etStd.setText(std);
            if (!stream.isEmpty())
                etStream.setText(stream);
            if (!email.isEmpty())
                etEmail.setText(email);
            if (!mobile.isEmpty())
                etMobile.setText(mobile);
        } else if ("Teacher".equals(senderRole)) {
            android.content.SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            String name = prefs.getString("teacherName", "");
            String email = prefs.getString("teacherEmail", "");
            String mobile = prefs.getString("teacherMobile", "");
            if (!name.isEmpty())
                etName.setText(name);
            if (!email.isEmpty())
                etEmail.setText(email);
            if (!mobile.isEmpty())
                etMobile.setText(mobile);
        } else if ("Principal".equals(senderRole)) {
            android.content.SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
            String name = prefs.getString("principalName", "");
            String email = prefs.getString("principalEmail", "");
            String mobile = prefs.getString("principalMobile", "");
            if (!name.isEmpty())
                etName.setText(name);
            if (!email.isEmpty())
                etEmail.setText(email);
            if (!mobile.isEmpty())
                etMobile.setText(mobile);
        } else if ("Parent".equals(senderRole)) {
            android.content.SharedPreferences prefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
            String name = prefs.getString("parentName", "");
            String email = prefs.getString("parentEmail", "");
            String mobile = prefs.getString("parentMobile", "");
            if (!name.isEmpty())
                etName.setText(name);
            if (!email.isEmpty())
                etEmail.setText(email);
            if (!mobile.isEmpty())
                etMobile.setText(mobile);
        }

        btnSubmit.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            submitMessage();
        });

        btnMyMessages.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, MyContactMessagesActivity.class);
                intent.putExtra("senderUid", senderUid);
                intent.putExtra("senderRole", senderRole);
                startActivity(intent);
            }, 100);
        });

        android.view.View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        UIAnimator.animateEditText(etName, 200);
        UIAnimator.animateEditText(etEmail, 300);
        UIAnimator.animateEditText(etMobile, 400);
        UIAnimator.animateEditText(etStd, 500);
        UIAnimator.animateEditText(etStream, 600);
        UIAnimator.animateEditText(etMessage, 700);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Contact Admin");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        // Hide drawer toggle if sender is Parent
        if ("Parent".equals(senderRole)) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            toggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            if (toggle.getDrawerArrowDrawable() != null) {
                toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
            }
        }
    }

    private void setupNavigationDrawer() {
        if (!"Parent".equals(senderRole)) {
            navigationView.setNavigationItemSelectedListener(this);
        } else {
            navigationView.setVisibility(View.GONE);
        }
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            userProfileImage = headerView.findViewById(R.id.userimage);
            if (usernameText != null) {
                if ("Student".equals(senderRole)) {
                    android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String studentName = prefs.getString("studentName", "");
                    usernameText.setText("Welcome, " + studentName);
                } else if ("Teacher".equals(senderRole)) {
                    android.content.SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
                    String teacherName = prefs.getString("teacherName", "");
                    usernameText.setText("Welcome, " + teacherName);
                } else if ("Principal".equals(senderRole)) {
                    android.content.SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
                    String principalName = prefs.getString("principalName", "");
                    usernameText.setText("Welcome, " + principalName);
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        
        if ("Student".equals(senderRole)) {
            if (id == R.id.homesection) {
                String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
                String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
                if ("11".equals(std)) {
                    if ("Science".equals(stream)) intent = new Intent(this, elevensciencehomepage.class);
                    else if ("Commerce".equals(stream)) intent = new Intent(this, elevencommercehome.class);
                    else if ("Arts".equals(stream)) intent = new Intent(this, elevenartshome.class);
                } else if ("12".equals(std)) {
                    if ("Science".equals(stream)) intent = new Intent(this, twelvesciencehome.class);
                    else if ("Commerce".equals(stream)) intent = new Intent(this, twelvecommercehome.class);
                    else if ("Arts".equals(stream)) intent = new Intent(this, twelveartshome.class);
                }
                if (intent == null) intent = new Intent(this, elevencommercehome.class);
            } else if (id == R.id.profile) {
                intent = new Intent(this, userprofile.class);
            } else if (id == R.id.feedback) {
                intent = new Intent(this, studentfeedback.class);
            } else if (id == R.id.gallery) {
                intent = new Intent(this, studentgallery.class);
            } else if (id == R.id.syllabus) {
                intent = new Intent(this, studentsyllabus.class);
            } else if (id == R.id.assignment) {
                intent = new Intent(this, studentassignment.class);
            } else if (id == R.id.exam) {
                intent = new Intent(this, studentexam.class);
            } else if (id == R.id.calender) {
                intent = new Intent(this, studentcalender.class);
            } else if (id == R.id.payment) {
                intent = new Intent(this, studentpayment.class);
            } else if (id == R.id.table) {
                intent = new Intent(this, studenttimetable.class);
            } else if (id == R.id.homework) {
                intent = new Intent(this, studenthomework.class);
            } else if (id == R.id.student) {
                intent = new Intent(this, studentslist.class);
            } else if (id == R.id.student_attendance) {
                intent = new Intent(this, studentattendance.class);
            } else if (id == R.id.contact_admin) {
                intent = new Intent(this, ContactAdminActivity.class);
                intent.putExtra("senderRole", "Student");
                String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentUid", "unknown");
                intent.putExtra("senderUid", uid);
            }
        } else if ("Teacher".equals(senderRole)) {
            if (id == R.id.teacher_home) {
                intent = new Intent(this, teachertab.class);
            } else if (id == R.id.teacher_assignments) {
                intent = new Intent(this, teacherassignments.class);
            } else if (id == R.id.teacher_homework) {
                intent = new Intent(this, teacherhomework.class);
            } else if (id == R.id.teacher_timetable) {
                intent = new Intent(this, teachertimetable.class);
            } else if (id == R.id.teacher_calendar) {
                intent = new Intent(this, teachercalender.class);
            } else if (id == R.id.teacher_attendance) {
                intent = new Intent(this, teacherattendance.class);
            } else if (id == R.id.teacher_syllabus) {
                intent = new Intent(this, teachersyllabus.class);
            } else if (id == R.id.teacher_exam) {
                intent = new Intent(this, teacherexam.class);
            } else if (id == R.id.teacher_feedback) {
                intent = new Intent(this, teacherfeedback.class);
            } else if (id == R.id.teacher_notices) {
                intent = new Intent(this, TeacherNoticesActivity.class);
            } else if (id == R.id.teacher_fees) {
                intent = new Intent(this, TeacherFeesActivity.class);
            } else if (id == R.id.teacher_contact_admin) {
                drawerLayout.closeDrawers();
                return true;
            }
        } else if ("Principal".equals(senderRole)) {
            if (id == R.id.principal_home) {
                intent = new Intent(this, principaltab.class);
            } else if (id == R.id.principal_notices) {
                intent = new Intent(this, PrincipalNoticesActivity.class);
            } else if (id == R.id.principal_calender) {
                intent = new Intent(this, PrincipalCalendarActivity.class);
            } else if (id == R.id.principal_payment) {
                intent = new Intent(this, PrincipalPaymentActivity.class);
            } else if (id == R.id.principal_exam) {
                intent = new Intent(this, PrincipalExamActivity.class);
            } else if (id == R.id.principal_feedback) {
                intent = new Intent(this, principalfeedback.class);
            } else if (id == R.id.principal_timetable) {
                intent = new Intent(this, principletimetable.class);
            } else if (id == R.id.principal_contact_admin) {
                drawerLayout.closeDrawers();
                return true;
            }
        }

        if (intent != null && intent.getComponent() != null && this.getClass().getName().equals(intent.getComponent().getClassName())) {
            android.widget.Toast.makeText(this, "Already on this page", android.widget.Toast.LENGTH_SHORT).show();
            if (drawerLayout != null) drawerLayout.closeDrawers();
            return true;
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        return true;
    }

    private void submitMessage() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("senderName", name);
        data.put("senderEmail", email);
        data.put("senderMobile", mobile);
        data.put("senderRole", senderRole);
        data.put("senderUid", senderUid);
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("read", false);

        if ("Student".equals(senderRole)) {
            String std = etStd.getText() != null ? etStd.getText().toString().trim() : "";
            String stream = etStream.getText() != null ? etStream.getText().toString().trim() : "";
            data.put("std", std);
            data.put("stream", stream);
        }

        contactRef.push().setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Message sent to Admin!", Toast.LENGTH_SHORT).show();
                    if (etMessage != null)
                        etMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Go back to home page based on user role
        if ("Student".equals(senderRole)) {
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String standard = prefs.getString("Standard", "");
            String stream = prefs.getString("Stream", "");
            
            Intent intent = new Intent(this, elevensciencehomepage.class);
            
            if ("11".equals(standard)) {
                if ("Science".equals(stream)) {
                    intent = new Intent(this, elevensciencehomepage.class);
                } else if ("Commerce".equals(stream)) {
                    intent = new Intent(this, elevencommercehome.class);
                } else if ("Arts".equals(stream)) {
                    intent = new Intent(this, elevenartshome.class);
                }
            } else if ("12".equals(standard)) {
                if ("Science".equals(stream)) {
                    intent = new Intent(this, twelvesciencehome.class);
                } else if ("Commerce".equals(stream)) {
                    intent = new Intent(this, twelvecommercehome.class);
                } else if ("Arts".equals(stream)) {
                    intent = new Intent(this, twelveartshome.class);
                }
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
