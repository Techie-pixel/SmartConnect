package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

public class studenthomework extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private boolean singleCategory = false;
    private String fixedStd = "";
    private String fixedStream = "";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_studenthomework);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);

        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        Intent i = getIntent();
        if (i != null && i.getBooleanExtra("singleCategory", false)) {
            singleCategory = true;
            fixedStd = i.getStringExtra("std") != null ? i.getStringExtra("std") : "";
            fixedStream = i.getStringExtra("stream") != null ? i.getStringExtra("stream") : "";
        } else {
            // Fallback to UserPrefs
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            fixedStd = prefs.getString("Standard", "");
            fixedStream = prefs.getString("Stream", "");
            singleCategory = !fixedStd.isEmpty() && !fixedStream.isEmpty();
        }

        setupTabs();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Add pulse animation to homework icon
        ImageView homeworkIcon = findViewById(R.id.homeworkIcon);
        if (homeworkIcon != null) {
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            homeworkIcon.startAnimation(pulse);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Homework");
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
        if (toggle.getDrawerArrowDrawable() != null) {
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
                android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

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

    @Override
    public void onBackPressed() {
        // Go back to home page like notices and e-commerce do
        Intent intent = new Intent(this, elevensciencehomepage.class);
        
        // Check prefs for current standard and stream
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String standard = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        
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
    }

    private void setupTabs() {
        if (singleCategory && !fixedStd.isEmpty() && !fixedStream.isEmpty()) {
            viewPager.setAdapter(new SinglePagerAdapter(this, fixedStd, fixedStream));
            tabLayout.setVisibility(View.GONE);
        } else {
            viewPager.setAdapter(new HomeworkPagerAdapter(this));
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("11 Science");
                        break;
                    case 1:
                        tab.setText("11 Commerce");
                        break;
                    case 2:
                        tab.setText("11 Arts");
                        break;
                    case 3:
                        tab.setText("12 Science");
                        break;
                    case 4:
                        tab.setText("12 Commerce");
                        break;
                    case 5:
                        tab.setText("12 Arts");
                        break;
                }
            }).attach();
        }
    }

    static class HomeworkPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public HomeworkPagerAdapter(@NonNull AppCompatActivity act) {
            super(act);
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return HomeworkListFragment.newInstance("11", "Science");
                case 1:
                    return HomeworkListFragment.newInstance("11", "Commerce");
                case 2:
                    return HomeworkListFragment.newInstance("11", "Arts");
                case 3:
                    return HomeworkListFragment.newInstance("12", "Science");
                case 4:
                    return HomeworkListFragment.newInstance("12", "Commerce");
                default:
                    return HomeworkListFragment.newInstance("12", "Arts");
            }
        }

        @Override
        public int getItemCount() {
            return 6;
        }
    }

    static class SinglePagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final String std;
        private final String stream;

        public SinglePagerAdapter(@NonNull AppCompatActivity act, String std, String stream) {
            super(act);
            this.std = std;
            this.stream = stream;
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return HomeworkListFragment.newInstance(std, stream);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }
}