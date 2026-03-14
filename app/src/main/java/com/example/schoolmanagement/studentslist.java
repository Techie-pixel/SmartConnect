package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class studentslist extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    ListView mainListView;
    EditText searchBar;

    ArrayList<StudentModel> studentList;
    StudentAdapter adapter;

    ArrayList<RecentChatModel> recentChats;
    RecentChatAdapter recentChatAdapter;

    Button allBtn, sciBtn, comBtn, artBtn, eleventhBtn, twelfthBtn, recentChatsBtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentslist);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        studentList = new ArrayList<>();
        adapter = new StudentAdapter(this, studentList);

        recentChats = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(this, recentChats);

        mainListView.setAdapter(adapter);
        loadAllStudents();

        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        allBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("all");
        });
        sciBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("Science");
        });
        comBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("Commerce");
        });
        artBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("Arts");
        });
        eleventhBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("11");
        });
        twelfthBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(adapter);
            adapter.filterCategory("12");
        });

        recentChatsBtn.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
            mainListView.setAdapter(recentChatAdapter);
            loadRecentChats();
        });

        mainListView.setOnItemClickListener((parent, view, position, id) -> {
            if (mainListView.getAdapter() == recentChatAdapter) {
                RecentChatModel rc = recentChats.get(position);
                Intent intent = new Intent(studentslist.this, studentchat.class);
                intent.putExtra("otherUid", rc.getUid());
                startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
            }
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        searchBar = findViewById(R.id.searchBar);
        mainListView = findViewById(R.id.mainListView);
        allBtn = findViewById(R.id.allBtn);
        sciBtn = findViewById(R.id.sciBtn);
        comBtn = findViewById(R.id.comBtn);
        artBtn = findViewById(R.id.artBtn);
        eleventhBtn = findViewById(R.id.eleventhBtn);
        twelfthBtn = findViewById(R.id.twelfthBtn);
        recentChatsBtn = findViewById(R.id.recentChatsBtn);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Students");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    // Students loader
    private void loadAllStudents() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Students");

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                studentList.clear();
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    String uid = snap.getKey();
                    String name = snap.child("Name").getValue(String.class);
                    String std = snap.child("Standard").getValue(String.class);
                    String stream = snap.child("Stream").getValue(String.class);
                    String profileBase64 = snap.child("ProfileImageBase64").getValue(String.class);

                    studentList.add(new StudentModel(uid, name, std, stream, profileBase64));
                }
                adapter.updateFullList();
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Recent chats loader - lambda safe final implementation!
    private void loadRecentChats() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        recentChats.clear();
        chatsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot chatSnap : snapshot.getChildren()) {
                String chatKey = chatSnap.getKey();
                if (chatKey != null && chatKey.contains(myUid) && chatKey.split("_").length == 2) {
                    final String[] split = chatKey.split("_");
                    final String otherUid = split[0].equals(myUid) ? split[1] : split[0];

                    // Use array for lastMsg (effectively final for lambda)
                    final String[] lastMsgArr = { "" };
                    DataSnapshot messagesSnap = chatSnap.child("messages");
                    if (messagesSnap.getChildrenCount() > 0) {
                        for (DataSnapshot msgSnap : messagesSnap.getChildren()) {
                            String text = msgSnap.child("text").getValue(String.class);
                            if (text != null && !text.isEmpty())
                                lastMsgArr[0] = text;
                        }
                    }

                    DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Students")
                            .child(otherUid);
                    studentRef.get().addOnSuccessListener(stSnap -> {
                        String name = stSnap.child("Name").getValue(String.class);
                        recentChats.add(new RecentChatModel(otherUid, name != null ? name : "Unknown", lastMsgArr[0]));
                        recentChatAdapter.notifyDataSetChanged();
                    });
                }
            }
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