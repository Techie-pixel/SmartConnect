package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherNoticesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private LinearLayout noticesContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnAll, btnUnread, btnRead;

    private DatabaseReference teacherNoticesRef, readsRef;
    private String teacherId;

    private String currentFilter = "all";

    private final List<NoticeData> allNotices = new ArrayList<>();
    private final Set<String> readNoticeIds = new HashSet<>();

    private static class NoticeData {
        String id;
        String noticeInfo;
        String image;
        long timestamp;
        String standard;
        String streams;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_notices);

        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        teacherId = prefs.getString("teacherId", "");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notices");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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

        noticesContainer = findViewById(R.id.noticesContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        btnRead = findViewById(R.id.btnRead);

        teacherNoticesRef = FirebaseDatabase.getInstance().getReference("Notices").child("TeacherNotices");
        readsRef = FirebaseDatabase.getInstance().getReference("NoticeReads");

        setupFilterButtons();
        loadNotices();
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                currentFilter = "all";
                updateFilterUI();
                renderNotices();
            }, 100);
        });
        btnUnread.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                currentFilter = "unread";
                updateFilterUI();
                renderNotices();
            }, 100);
        });
        btnRead.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                currentFilter = "read";
                updateFilterUI();
                renderNotices();
            }, 100);
        });
    }

    private void updateFilterUI() {
        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                "all".equals(currentFilter) ? 0xFF3B5BDB : 0xFF1A2E55));
        btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                "unread".equals(currentFilter) ? 0xFF3B5BDB : 0xFF1A2E55));
        btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                "read".equals(currentFilter) ? 0xFF3B5BDB : 0xFF1A2E55));
    }

    private void loadNotices() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        teacherNoticesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allNotices.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NoticeData nd = new NoticeData();
                    nd.id = child.getKey();
                    nd.noticeInfo = child.child("noticeInfo").getValue(String.class);
                    nd.image = child.child("image").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    nd.timestamp = ts != null ? ts : 0;
                    nd.standard = child.child("standard").getValue(String.class);
                    nd.streams = child.child("streams").getValue(String.class);
                    allNotices.add(nd);
                }
                allNotices.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                loadReadStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TeacherNoticesActivity.this,
                        "Error loading notices", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadStatus() {
        if (teacherId == null || teacherId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            renderNotices();
            return;
        }

        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readNoticeIds.clear();
                for (NoticeData nd : allNotices) {
                    DataSnapshot readSnap = snapshot.child(nd.id).child(teacherId);
                    if (readSnap.exists()) {
                        readNoticeIds.add(nd.id);
                    }
                }
                progressBar.setVisibility(View.GONE);
                renderNotices();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                renderNotices();
            }
        });
    }

    private void renderNotices() {
        noticesContainer.removeAllViews();

        List<NoticeData> filtered = new ArrayList<>();
        for (NoticeData nd : allNotices) {
            boolean isRead = readNoticeIds.contains(nd.id);
            if ("all".equals(currentFilter) ||
                    ("read".equals(currentFilter) && isRead) ||
                    ("unread".equals(currentFilter) && !isRead)) {
                filtered.add(nd);
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (NoticeData nd : filtered) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_notice, noticesContainer, false);

            TextView tvTarget = card.findViewById(R.id.tvNoticeTarget);
            TextView tvDate = card.findViewById(R.id.tvNoticeDate);
            TextView tvInfo = card.findViewById(R.id.tvNoticeInfo);
            ImageView ivImage = card.findViewById(R.id.ivNoticeImage);
            TextView tvBadge = card.findViewById(R.id.tvUnreadBadge);
            Button btnMark = card.findViewById(R.id.btnMarkRead);

            String targetLabel = "For Teachers";
            if (nd.standard != null && nd.streams != null) {
                targetLabel += " | Std " + nd.standard + " - " + nd.streams;
            }
            tvTarget.setText(targetLabel);
            tvDate.setText(sdf.format(new Date(nd.timestamp)));

            if (nd.noticeInfo != null && !nd.noticeInfo.isEmpty()) {
                tvInfo.setText(nd.noticeInfo);
                tvInfo.setVisibility(View.VISIBLE);
            } else {
                tvInfo.setVisibility(View.GONE);
            }

            if (nd.image != null && !nd.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(nd.image, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivImage.setImageBitmap(bmp);
                        ivImage.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    ivImage.setVisibility(View.GONE);
                }
            }

            boolean isRead = readNoticeIds.contains(nd.id);
            if (isRead) {
                tvBadge.setVisibility(View.GONE);
                btnMark.setText("✓ Read");
                btnMark.setEnabled(false);
                btnMark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF555555));
            } else {
                tvBadge.setVisibility(View.VISIBLE);
                btnMark.setText("✓ Mark as Read");
                btnMark.setEnabled(true);
                btnMark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));

                final String noticeId = nd.id;
                btnMark.setOnClickListener(v -> {
                    UIAnimator.animateClick(v);
                    v.postDelayed(() -> markAsRead(noticeId), 150);
                });
            }

            noticesContainer.addView(card);
        }
    }

    private void markAsRead(String noticeId) {
        if (teacherId == null || teacherId.isEmpty()) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        readsRef.child(noticeId).child(teacherId).setValue(true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.teacher_home)
            startActivity(new Intent(this, teachertab.class));
        else if (id == R.id.teacher_assignments)
            startActivity(new Intent(this, teacherassignments.class));
        else if (id == R.id.teacher_homework)
            startActivity(new Intent(this, teacherhomework.class));
        else if (id == R.id.teacher_syllabus)
            startActivity(new Intent(this, teachersyllabus.class));
        else if (id == R.id.teacher_timetable)
            startActivity(new Intent(this, teachertimetable.class));
        else if (id == R.id.teacher_exam)
            startActivity(new Intent(this, teacherexam.class));
        else if (id == R.id.teacher_calendar)
            startActivity(new Intent(this, teachercalender.class));
        else if (id == R.id.teacher_attendance)
            startActivity(new Intent(this, teacherattendance.class));
        else if (id == R.id.teacher_feedback)
            startActivity(new Intent(this, teacherfeedback.class));
        else if (id == R.id.teacher_notices) {
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_fees)
            startActivity(new Intent(this, TeacherFeesActivity.class));
        else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            caIntent.putExtra("senderUid", getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown"));
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
