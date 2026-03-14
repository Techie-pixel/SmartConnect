package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class studentattendance extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private String studentId, studentStandard, studentStream, studentName;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private TextView usernameText;
    private ImageView userProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentattendance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("My Attendance");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if(toggle.getDrawerArrowDrawable() != null) {
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        }
        navigationView.setNavigationItemSelectedListener(this);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        studentId = prefs.getString("studentId", "");
        studentStandard = prefs.getString("Standard", "");
        studentStream = prefs.getString("Stream", "");
        studentName = prefs.getString("studentName", "Student");

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new StudentAttendancePagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(pos == 0 ? "My Attendance" : "Class Attendance")).attach();

        setupUserProfile();

        android.widget.LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Animate toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            UIAnimator.animateToolbar(toolbarView, 200);
        }
        
        // Animate tab layout after delay
        mainContent.postDelayed(() -> {
            View tabLayoutView = findViewById(R.id.tabLayout);
            if (tabLayoutView != null) {
                UIAnimator.animateImageView(tabLayoutView, 500);
            }
        }, 400);
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            
            // First try SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String cachedName = prefs.getString("studentName", "");
            
            // If not found in prefs, fetch from Firebase
            if (cachedName == null || cachedName.isEmpty()) {
                fetchStudentNameFromFirebase();
            } else {
                usernameText.setText("Welcome, " + cachedName);
            }
        }
    }
    
    private void fetchStudentNameFromFirebase() {
        String studentIdLocal = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentId", "");
        if (!studentIdLocal.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Students")
                    .child(studentIdLocal)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    usernameText.setText("Welcome, " + name);
                                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("studentName", name)
                                            .apply();
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            usernameText.setText("Welcome, Student");
                        }
                    });
        } else {
            usernameText.setText("Welcome, Student");
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
        if (drawerLayout != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    private class StudentAttendancePagerAdapter extends FragmentStateAdapter {
        StudentAttendancePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle args = new Bundle();
            args.putString("studentId", studentId);
            args.putString("standard", studentStandard);
            args.putString("stream", studentStream);
            args.putString("studentName", studentName);
            Fragment f = (position == 0) ? new MyAttendanceFragment() : new ClassAttendanceSummaryFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // ──── My Attendance Fragment ───────────────────────────────────────
    public static class MyAttendanceFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_class_attendance, container, false);
            RecyclerView rv = v.findViewById(R.id.rvClassAttendance);
            TextView tvEmpty = v.findViewById(R.id.tvClassEmpty);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));

            String sid = getArguments() != null ? getArguments().getString("studentId", "") : "";
            String std = getArguments() != null ? getArguments().getString("standard", "") : "";
            String stream = getArguments() != null ? getArguments().getString("stream", "") : "";

            List<AttendanceDateAdapter.DateRecord> records = new ArrayList<>();
            AttendanceDateAdapter adapter = new AttendanceDateAdapter(records, getContext());
            rv.setAdapter(adapter);

            String path = std + "_" + stream;
            FirebaseDatabase.getInstance().getReference("Attendance").child(path)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            records.clear();
                            int p = 0, a = 0, l = 0;
                            for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                                String subjectName = subjectSnap.getKey();
                                for (DataSnapshot dateSnap : subjectSnap.getChildren()) {
                                    DataSnapshot stSnap = dateSnap.child(sid);
                                    if (!stSnap.exists())
                                        continue;
                                    String status = stSnap.child("status").getValue(String.class);
                                    String subject = stSnap.child("subject").getValue(String.class);
                                    if (subject == null) {
                                        subject = subjectName;
                                    }
                                    if (status == null)
                                        status = "Present";
                                    if ("Present".equalsIgnoreCase(status))
                                        p++;
                                    else if ("Absent".equalsIgnoreCase(status))
                                        a++;
                                    else if ("Late".equalsIgnoreCase(status))
                                        l++;
                                    AttendanceDateAdapter.DateRecord dr = new AttendanceDateAdapter.DateRecord();
                                    dr.date = dateSnap.getKey() != null ? dateSnap.getKey().replace("_", " ") : "?";
                                    dr.status = status;
                                    dr.subject = subject != null ? subject : "";
                                    records.add(0, dr);
                                }
                            }
                            // Sort by date could be added here if necessary, but records will be primarily
                            // grouped by subject and then reverse date
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });

            return v;
        }
    }

    // ──── Class Attendance Summary Fragment (read-only) ───────────────
    public static class ClassAttendanceSummaryFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_class_attendance, container, false);
            RecyclerView rv = v.findViewById(R.id.rvClassAttendance);
            TextView tvEmpty = v.findViewById(R.id.tvClassEmpty);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));

            String std = getArguments() != null ? getArguments().getString("standard", "") : "";
            String stream = getArguments() != null ? getArguments().getString("stream", "") : "";

            List<teacherattendance.ClassAttendanceAdapter.StudentSummary> summaryList = new ArrayList<>();
            teacherattendance.ClassAttendanceAdapter adapter = new teacherattendance.ClassAttendanceAdapter(summaryList,
                    getContext());
            rv.setAdapter(adapter);

            String path = std + "_" + stream;
            FirebaseDatabase.getInstance().getReference("Attendance").child(path)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            java.util.Map<String, teacherattendance.ClassAttendanceAdapter.StudentSummary> map = new java.util.HashMap<>();
                            for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                                for (DataSnapshot dateSnap : subjectSnap.getChildren()) {
                                    for (DataSnapshot stSnap : dateSnap.getChildren()) {
                                        String sid2 = stSnap.getKey();
                                        if (sid2 == null)
                                            continue;
                                        String name = stSnap.child("name").getValue(String.class);
                                        String status = stSnap.child("status").getValue(String.class);
                                        if (!map.containsKey(sid2)) {
                                            teacherattendance.ClassAttendanceAdapter.StudentSummary ss = new teacherattendance.ClassAttendanceAdapter.StudentSummary();
                                            ss.studentId = sid2;
                                            ss.name = name != null ? name : "Student";
                                            map.put(sid2, ss);
                                        }
                                        teacherattendance.ClassAttendanceAdapter.StudentSummary ss = map.get(sid2);
                                        if ("Present".equalsIgnoreCase(status))
                                            ss.present++;
                                        else if ("Absent".equalsIgnoreCase(status))
                                            ss.absent++;
                                        else if ("Late".equalsIgnoreCase(status))
                                            ss.late++;
                                    }
                                }
                            }
                            summaryList.clear();
                            summaryList.addAll(map.values());
                            summaryList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(summaryList.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
            return v;
        }
    }

    // ──── Adapter for My Attendance date records ──────────────────────
    static class AttendanceDateAdapter extends RecyclerView.Adapter<AttendanceDateAdapter.VH> {
        static class DateRecord {
            String date, status, subject;
        }

        private final List<DateRecord> list;
        private final android.content.Context ctx;

        AttendanceDateAdapter(List<DateRecord> list, android.content.Context ctx) {
            this.list = list;
            this.ctx = ctx;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_attendance_record, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DateRecord r = list.get(pos);
            h.tvName.setText(r.subject.isEmpty() ? r.date : r.date + "  (" + r.subject + ")");
            h.tvDate.setText("");
            h.tvStatus.setText(r.status);
            // Color
            if ("Present".equalsIgnoreCase(r.status)) {
                h.tvStatus.setBackgroundColor(0xFF1B5E20);
                h.tvStatus.setTextColor(0xFFA5D6A7);
            } else if ("Absent".equalsIgnoreCase(r.status)) {
                h.tvStatus.setBackgroundColor(0xFFB71C1C);
                h.tvStatus.setTextColor(0xFFFFCDD2);
            } else {
                h.tvStatus.setBackgroundColor(0xFFE65100);
                h.tvStatus.setTextColor(0xFFFFE0B2);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvStatus;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvDate = v.findViewById(R.id.tvAttendanceDate);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }
}
