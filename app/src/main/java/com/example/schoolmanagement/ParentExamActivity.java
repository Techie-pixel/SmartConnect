package com.example.schoolmanagement;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ParentExamActivity extends AppCompatActivity {

    private LinearLayout examContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTitle;
    private TextView chipAll, chipUnitTest, chipMidTerm, chipFinalExam, chipPractical;
    private ImageView logoImg;

    private String childStdStream;
    private String parentId;
    private String currentFilter = "All";

    private static final String[] EXAM_TYPE_KEYS = { "UnitTest", "MidTerm", "FinalExam", "Practical" };
    private static final String[] EXAM_TYPE_LABELS = { "Unit Test", "Mid Term", "Final Exam", "Practical" };

    private final List<ExamData> allExams = new ArrayList<>();
    private final Set<String> readExamIds = new HashSet<>();
    private DatabaseReference examRef, readsRef;

    private static class ExamData {
        String id;
        String examInfo;
        String examType;
        String image;
        long timestamp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_exam);

        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        String std = parentPrefs.getString("studentStandard", "");
        String stream = parentPrefs.getString("studentStream", "");
        parentId = parentPrefs.getString("parentKey", "");
        childStdStream = std + "_" + stream;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Exam - " + std + " " + stream);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        logoImg = findViewById(R.id.logoImg);
        tvTitle = findViewById(R.id.tvTitle);
        examContainer = findViewById(R.id.examContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        chipAll = findViewById(R.id.chipAll);
        chipUnitTest = findViewById(R.id.chipUnitTest);
        chipMidTerm = findViewById(R.id.chipMidTerm);
        chipFinalExam = findViewById(R.id.chipFinalExam);
        chipPractical = findViewById(R.id.chipPractical);

        examRef = FirebaseDatabase.getInstance().getReference("Exam");
        readsRef = FirebaseDatabase.getInstance().getReference("ExamReads");

        applyAnimations(toolbar);
        setupFilterChips();
        loadExams();
    }

    private void applyAnimations(Toolbar toolbar) {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);
        if (tvTitle != null)
            UIAnimator.animateTextView(tvTitle, 300);

        View[] chips = { chipAll, chipUnitTest, chipMidTerm, chipFinalExam, chipPractical };
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] != null) {
                UIAnimator.animateTextView((TextView) chips[i], 400 + (i * 50));
            }
        }
    }

    private void setupFilterChips() {
        TextView[] chips = { chipAll, chipUnitTest, chipMidTerm, chipFinalExam, chipPractical };
        String[] filters = { "All", "Unit Test", "Mid Term", "Final Exam", "Practical" };

        for (int i = 0; i < chips.length; i++) {
            final String filter = filters[i];
            chips[i].setOnClickListener(v -> {
                currentFilter = filter;
                updateChipUI(chips, (TextView) v);
                renderExams();
            });
        }
    }

    private void updateChipUI(TextView[] chips, TextView selected) {
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.chip_unselected);
        }
        selected.setBackgroundResource(R.drawable.chip_selected);
    }

    private void loadExams() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        examContainer.removeAllViews();

        final int totalNodes = EXAM_TYPE_KEYS.length;
        final AtomicInteger loaded = new AtomicInteger(0);

        for (int i = 0; i < EXAM_TYPE_KEYS.length; i++) {
            final String examTypeKey = EXAM_TYPE_KEYS[i];
            final String examTypeLabel = EXAM_TYPE_LABELS[i];

            examRef.child(examTypeKey).child(childStdStream)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            allExams.removeIf(e -> examTypeLabel.equals(e.examType));

                            for (DataSnapshot child : snapshot.getChildren()) {
                                ExamData ed = new ExamData();
                                ed.id = child.getKey();
                                ed.examInfo = child.child("examInfo").getValue(String.class);
                                ed.examType = child.child("examType").getValue(String.class);
                                if (ed.examType == null)
                                    ed.examType = examTypeLabel;
                                ed.image = child.child("image").getValue(String.class);
                                Long ts = child.child("timestamp").getValue(Long.class);
                                ed.timestamp = ts != null ? ts : 0;
                                allExams.add(ed);
                            }

                            if (loaded.incrementAndGet() >= totalNodes) {
                                allExams.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                                loadReadStatus();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (loaded.incrementAndGet() >= totalNodes) {
                                progressBar.setVisibility(View.GONE);
                                renderExams();
                            }
                        }
                    });
        }
    }

    private void loadReadStatus() {
        if (parentId == null || parentId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            renderExams();
            return;
        }

        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readExamIds.clear();
                for (ExamData ed : allExams) {
                    if (snapshot.child(ed.id).child(parentId).exists()) {
                        readExamIds.add(ed.id);
                    }
                }
                progressBar.setVisibility(View.GONE);
                renderExams();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                renderExams();
            }
        });
    }

    private void renderExams() {
        examContainer.removeAllViews();

        List<ExamData> filtered = new ArrayList<>();
        for (ExamData ed : allExams) {
            if ("All".equals(currentFilter) || currentFilter.equals(ed.examType)) {
                filtered.add(ed);
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (ExamData ed : filtered) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_exam_card, examContainer, false);

            TextView tvExamType = card.findViewById(R.id.tvExamType);
            TextView tvExamDate = card.findViewById(R.id.tvExamDate);
            TextView tvExamInfo = card.findViewById(R.id.tvExamInfo);
            ImageView ivExamImage = card.findViewById(R.id.ivExamImage);
            TextView tvUnreadBadge = card.findViewById(R.id.tvUnreadBadge);
            Button btnMarkRead = card.findViewById(R.id.btnMarkRead);
            Button btnDownload = card.findViewById(R.id.btnDownload);

            tvExamType.setText(ed.examType != null ? ed.examType : "Exam");
            tvExamDate.setText(sdf.format(new Date(ed.timestamp)));

            if (ed.examInfo != null && !ed.examInfo.isEmpty()) {
                tvExamInfo.setText(ed.examInfo);
                tvExamInfo.setVisibility(View.VISIBLE);
            }

            if (ed.image != null && !ed.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(ed.image, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivExamImage.setImageBitmap(bmp);
                        ivExamImage.setVisibility(View.VISIBLE);
                        btnDownload.setVisibility(View.VISIBLE);
                        btnDownload.setOnClickListener(v -> saveExamImage(ed.image));
                    }
                } catch (Exception e) {
                    ivExamImage.setVisibility(View.GONE);
                    btnDownload.setVisibility(View.GONE);
                }
            } else {
                ivExamImage.setVisibility(View.GONE);
                btnDownload.setVisibility(View.GONE);
            }

            boolean isRead = readExamIds.contains(ed.id);
            if (isRead) {
                tvUnreadBadge.setVisibility(View.GONE);
                btnMarkRead.setText("✓ Read");
                btnMarkRead.setEnabled(false);
                btnMarkRead.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF555555));
            } else {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                btnMarkRead.setText("✓ Mark as Read");
                btnMarkRead.setEnabled(true);
                btnMarkRead.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2E7D32));
                final String examId = ed.id;
                btnMarkRead.setOnClickListener(v -> markAsRead(examId));
            }

            examContainer.addView(card);
        }
    }

    private void markAsRead(String examId) {
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        readsRef.child(examId).child(parentId).setValue(true)
                .addOnSuccessListener(v -> Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark", Toast.LENGTH_SHORT).show());
    }

    private void saveExamImage(String base64Image) {
        try {
            byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bitmap != null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "Exam_" + timeStamp + ".jpg";
                File storageDir = new File(getExternalFilesDir(null), "Exams");
                if (!storageDir.exists())
                    storageDir.mkdirs();
                File imageFile = new File(storageDir, imageFileName);
                OutputStream fOut = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
                Toast.makeText(this, "Image saved to: " + imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
