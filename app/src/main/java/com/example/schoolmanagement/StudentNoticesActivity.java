package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

public class StudentNoticesActivity extends AppCompatActivity {
    private LinearLayout noticesContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnAll, btnUnread, btnRead;

    private DatabaseReference noticesRef, readsRef;
    private String studentStdStream; // e.g. "11_Science"
    private String studentUserId;

    private String currentFilter = "all"; // "all", "unread", "read"

    // Cached data
    private final List<NoticeData> allNotices = new ArrayList<>();
    private final Set<String> readNoticeIds = new HashSet<>();

    private static class NoticeData {
        String id;
        String noticeInfo;
        String image;
        long timestamp;

        NoticeData() {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_notices);

        // Get student info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        studentUserId = prefs.getString("studentId", "");
        studentStdStream = std + "_" + stream;

        // Also accept from intent (override)
        if (getIntent().hasExtra("std") && getIntent().hasExtra("stream")) {
            std = getIntent().getStringExtra("std");
            stream = getIntent().getStringExtra("stream");
            studentStdStream = std + "_" + stream;
        }

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
        }

        noticesContainer = findViewById(R.id.noticesContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        btnRead = findViewById(R.id.btnRead);

        noticesRef = FirebaseDatabase.getInstance().getReference("Notices").child(studentStdStream);
        readsRef = FirebaseDatabase.getInstance().getReference("NoticeReads");

        setupFilterButtons();

        addTouchAnimation(btnAll);
        addTouchAnimation(btnUnread);
        addTouchAnimation(btnRead);

        Animation animAll = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        btnAll.startAnimation(animAll);
        Animation animUnread = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animUnread.setStartOffset(100);
        btnUnread.startAnimation(animUnread);
        Animation animRead = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        animRead.setStartOffset(200);
        btnRead.startAnimation(animRead);

        loadNotices();
    }

    private void addTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterUI();
            renderNotices();
        });
        btnUnread.setOnClickListener(v -> {
            currentFilter = "unread";
            updateFilterUI();
            renderNotices();
        });
        btnRead.setOnClickListener(v -> {
            currentFilter = "read";
            updateFilterUI();
            renderNotices();
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

        noticesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allNotices.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    if ("TeacherNotices".equals(child.getKey()))
                        continue;
                    NoticeData nd = new NoticeData();
                    nd.id = child.getKey();
                    nd.noticeInfo = child.child("noticeInfo").getValue(String.class);
                    nd.image = child.child("image").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    nd.timestamp = ts != null ? ts : 0;
                    allNotices.add(nd);
                }
                // Sort newest first
                allNotices.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                loadReadStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentNoticesActivity.this,
                        "Error loading notices", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadStatus() {
        if (studentUserId == null || studentUserId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            renderNotices();
            return;
        }

        // Check all notice IDs for read status
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readNoticeIds.clear();
                for (NoticeData nd : allNotices) {
                    DataSnapshot readSnap = snapshot.child(nd.id).child(studentUserId);
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

            tvTarget.setText(studentStdStream.replace("_", " "));
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
                addTouchAnimation(btnMark);
                btnMark.setOnClickListener(v -> markAsRead(noticeId));
            }

            noticesContainer.addView(card);
        }
    }

    private void markAsRead(String noticeId) {
        if (studentUserId == null || studentUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        readsRef.child(noticeId).child(studentUserId).setValue(true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Go back to home page based on student's standard and stream
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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
    }
}
