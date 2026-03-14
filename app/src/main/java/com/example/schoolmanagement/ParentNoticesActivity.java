package com.example.schoolmanagement;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

public class ParentNoticesActivity extends AppCompatActivity {
    private LinearLayout noticesContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTitle;
    private Button btnAll, btnUnread, btnRead;
    private Toolbar toolbar;
    private ImageView logoImg;

    private DatabaseReference noticesRef, readsRef;
    private String studentStdStream;
    private String parentId;

    private String currentFilter = "all";
    private final List<NoticeData> allNotices = new ArrayList<>();
    private final Set<String> readNoticeIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_notices);

        SharedPreferences prefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        parentId = prefs.getString("parentKey", "");
        String std = prefs.getString("studentStandard", "");
        String stream = prefs.getString("studentStream", "");
        studentStdStream = std + "_" + stream;

        initializeViews();
        setupToolbar();
        applyAnimations();

        noticesRef = FirebaseDatabase.getInstance().getReference("Notices").child(studentStdStream);
        readsRef = FirebaseDatabase.getInstance().getReference("NoticeReads");

        loadNotices();

        btnAll.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            setFilter("all");
        });
        btnUnread.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            setFilter("unread");
        });
        btnRead.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            setFilter("read");
        });
    }

    private void applyAnimations() {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);
        if (tvTitle != null)
            UIAnimator.animateTextView(tvTitle, 300);

        Button[] buttons = { btnAll, btnUnread, btnRead };
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                UIAnimator.animateButton(buttons[i], 400 + (i * 100));
            }
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        logoImg = findViewById(R.id.logoImg);
        tvTitle = findViewById(R.id.tvTitle);
        noticesContainer = findViewById(R.id.noticesContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        btnRead = findViewById(R.id.btnRead);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notices");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        renderNotices();
    }

    private void updateFilterButtons() {
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
                    if ("TeacherNotices".equals(child.getKey())) continue;
                    NoticeData nd = new NoticeData();
                    nd.id = child.getKey();
                    nd.noticeInfo = child.child("info").getValue(String.class);
                    nd.image = child.child("image").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    nd.timestamp = ts != null ? ts : 0;
                    allNotices.add(nd);
                }
                allNotices.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                loadReadStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ParentNoticesActivity.this, "Error loading notices", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadStatus() {
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readNoticeIds.clear();
                for (NoticeData nd : allNotices) {
                    if (snapshot.child(nd.id).child(parentId).exists()) {
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
            if ("all".equals(currentFilter) || ("read".equals(currentFilter) && isRead) || ("unread".equals(currentFilter) && !isRead)) {
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
            tvInfo.setText(nd.noticeInfo);

            boolean isRead = readNoticeIds.contains(nd.id);
            tvBadge.setVisibility(isRead ? View.GONE : View.VISIBLE);
            btnMark.setVisibility(isRead ? View.GONE : View.VISIBLE);

            if (nd.image != null && !nd.image.isEmpty()) {
                byte[] decoded = Base64.decode(nd.image, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                ivImage.setImageBitmap(bmp);
                ivImage.setVisibility(View.VISIBLE);
            }

            btnMark.setOnClickListener(v -> markAsRead(nd.id));
            noticesContainer.addView(card);
        }
    }

    private void markAsRead(String noticeId) {
        readsRef.child(noticeId).child(parentId).setValue(true);
    }

    public static class NoticeData {
        public String id;
        public String noticeInfo;
        public String image;
        public long timestamp;
    }
}
