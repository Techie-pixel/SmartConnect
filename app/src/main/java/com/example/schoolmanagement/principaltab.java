package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.database.DatabaseError;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class principaltab extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private TextView adminWelcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principaltab);

        // Start ChatNotificationService for e-commerce notifications
        Intent serviceIntent = new Intent(this, ChatNotificationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Principal Dashboard");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        adminWelcomeText = findViewById(R.id.adminWelcomeText);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // Ensure hamburger icon visible
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        String principalId = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE).getString("principalId", null);
        if (principalId != null && !principalId.isEmpty()) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Principals").child(principalId);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        if (adminWelcomeText != null) {
                            adminWelcomeText.setText("Welcome, " + name);
                        }
                        View header = navigationView.getHeaderView(0);
                        TextView username = header.findViewById(R.id.username);
                        if (username != null) {
                            username.setText("Welcome, " + name);
                            username.setTextColor(getResources().getColor(android.R.color.white));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                }
            });
        }

        LinearLayout profilecard = findViewById(R.id.profilecard);
        LinearLayout gallery = findViewById(R.id.gallery);
        LinearLayout ecommerce = findViewById(R.id.ecommerce);
        LinearLayout cardTeachers = findViewById(R.id.cardTeachers);

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        // Animate toolbar
        UIAnimator.animateToolbar(toolbar, 100);

        // Animate welcome text
        UIAnimator.animateTextView(adminWelcomeText, 300);

        // Animate dashboard cards with staggered delay
        UIAnimator.animateCard(profilecard, 400);
        UIAnimator.animateCard(ecommerce, 550);
        UIAnimator.animateCard(gallery, 700);
        UIAnimator.animateCard(cardTeachers, 850);

        profilecard.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(principaltab.this, principleprofile.class)), 150);
        });

        gallery.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(principaltab.this, peinciplegallery.class)), 150);
        });

        ecommerce.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(principaltab.this, PrincipalEcommerceActivity.class)), 150);
        });

        cardTeachers.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(principaltab.this, teacherslist.class)), 150);
        });
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
        if (id == R.id.principal_home) {
            drawerLayout.closeDrawers();
            android.widget.Toast.makeText(this, "Already on Home Page", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.principal_timetable) {
            startActivity(new Intent(this, principletimetable.class));
        } else if (id == R.id.principal_exam) {
            startActivity(new Intent(this, PrincipalExamActivity.class));
        } else if (id == R.id.principal_notices) {
            startActivity(new Intent(this, PrincipalNoticesActivity.class));
        } else if (id == R.id.principal_calender) {
            startActivity(new Intent(this, PrincipalCalendarActivity.class));
        } else if (id == R.id.principal_payment) {
            startActivity(new Intent(this, PrincipalPaymentActivity.class));
        } else if (id == R.id.principal_feedback) {
            startActivity(new Intent(this, principalfeedback.class));
        } else if (id == R.id.principal_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Principal");
            String principalId = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE).getString("principalId",
                    "unknown");
            caIntent.putExtra("senderUid", principalId);
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
