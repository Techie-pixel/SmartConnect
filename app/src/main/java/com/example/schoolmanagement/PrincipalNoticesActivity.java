package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PrincipalNoticesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private RadioGroup standardRadioGroup, shareTeacherRadioGroup;
    private RadioButton radio11, radio12, radioAll, radioYes, radioNo;
    private CheckBox checkScience, checkCommerce, checkArts, checkAllStreams;
    private EditText noticeInfoEdit;
    private Button cameraBtn, galleryBtn, submitBtn, removeImageBtn;
    private ImageView previewImage;
    private LinearLayout imagePreviewContainer;
    private LinearLayout historyContainer;
    private ProgressBar historyProgressBar;
    private TextView tvNoHistory;

    private DatabaseReference noticesReference, teacherNoticesReference;
    private String base64Image = null;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // All possible std_stream combinations
    private static final String[] STD_STREAM_KEYS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    // To track already-added IDs (avoid duplicates across std_stream nodes)
    private final List<String> addedIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_notices);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            setTitle("Notices");
            drawerLayout = findViewById(R.id.main);
            navigationView = findViewById(R.id.navigationview);
            toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(this);
            }
        }

        // Apply toolbar and logo animations
        UIAnimator.animateToolbar(toolbar, 100);
        android.widget.ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 200);

        initializeViews();
        
        // Staggered animation for all items
        LinearLayout contentLayout = findViewById(R.id.contentLayout);
        if (contentLayout != null) {
            UIAnimator.animateLinearLayoutItems(contentLayout, 300, 100);
        }

        setupFirebase();
        setupListeners();
        loadHistory();
    }

    private void initializeViews() {
        standardRadioGroup = findViewById(R.id.standardRadioGroup);
        radio11 = findViewById(R.id.radio11);
        radio12 = findViewById(R.id.radio12);
        radioAll = findViewById(R.id.radioAll);

        shareTeacherRadioGroup = findViewById(R.id.shareTeacherRadioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);

        checkScience = findViewById(R.id.checkScience);
        checkCommerce = findViewById(R.id.checkCommerce);
        checkArts = findViewById(R.id.checkArts);
        checkAllStreams = findViewById(R.id.checkAllStreams);

        noticeInfoEdit = findViewById(R.id.noticeInfoEdit);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        submitBtn = findViewById(R.id.submitBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);
        previewImage = findViewById(R.id.previewImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        historyContainer = findViewById(R.id.historyContainer);
        historyProgressBar = findViewById(R.id.historyProgressBar);
        tvNoHistory = findViewById(R.id.tvNoHistory);
    }

    private void setupFirebase() {
        noticesReference = FirebaseDatabase.getInstance().getReference("Notices");
        teacherNoticesReference = FirebaseDatabase.getInstance().getReference("Notices").child("TeacherNotices");
    }

    private void setupListeners() {
        checkAllStreams.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkScience.setChecked(false);
                checkCommerce.setChecked(false);
                checkArts.setChecked(false);
                checkScience.setEnabled(false);
                checkCommerce.setEnabled(false);
                checkArts.setEnabled(false);
            } else {
                checkScience.setEnabled(true);
                checkCommerce.setEnabled(true);
                checkArts.setEnabled(true);
            }
        });

        CompoundButton.OnCheckedChangeListener individualStreamListener = (buttonView, isChecked) -> {
            if (isChecked && checkAllStreams.isChecked())
                checkAllStreams.setChecked(false);
        };

        checkScience.setOnCheckedChangeListener(individualStreamListener);
        checkCommerce.setOnCheckedChangeListener(individualStreamListener);
        checkArts.setOnCheckedChangeListener(individualStreamListener);

        cameraBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> openCamera(), 150); });
        galleryBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> openGallery(), 150); });
        removeImageBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> removeImage(), 150); });
        submitBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> submitNotice(), 150); });

        // Apply animations
        UIAnimator.animateEditText(noticeInfoEdit, 200);
        UIAnimator.animateButton(cameraBtn, 350);
        UIAnimator.animateButton(galleryBtn, 500);
        UIAnimator.animateButton(submitBtn, 650);
    }

    // ==================== HISTORY ====================
    // Structure: Notices/{std_stream}/{id} -> {noticeInfo, image, timestamp}

    private void loadHistory() {
        historyProgressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        historyContainer.removeAllViews();
        addedIds.clear();

        final int totalNodes = STD_STREAM_KEYS.length;
        final AtomicInteger loadedNodes = new AtomicInteger(0);
        final Map<String, DataSnapshot> allEntries = new HashMap<>();
        final Map<String, String> idToStdStream = new HashMap<>();

        for (String stdStream : STD_STREAM_KEYS) {
            noticesReference.child(stdStream)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Remove old entries for this stdStream key
                            List<String> toRemove = new ArrayList<>();
                            for (Map.Entry<String, String> e : idToStdStream.entrySet()) {
                                if (e.getValue().equals(stdStream))
                                    toRemove.add(e.getKey());
                            }
                            for (String key : toRemove) {
                                allEntries.remove(key);
                                idToStdStream.remove(key);
                            }

                            for (DataSnapshot child : snapshot.getChildren()) {
                                String id = child.getKey();
                                // Skip the TeacherNotices child node
                                if ("TeacherNotices".equals(id))
                                    continue;
                                if (!allEntries.containsKey(id)) {
                                    allEntries.put(id, child);
                                    idToStdStream.put(id, stdStream);
                                }
                            }

                            if (loadedNodes.incrementAndGet() >= totalNodes) {
                                historyProgressBar.setVisibility(View.GONE);
                                renderHistory(allEntries, idToStdStream);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedNodes.incrementAndGet();
                            if (loadedNodes.get() >= totalNodes) {
                                historyProgressBar.setVisibility(View.GONE);
                                renderHistory(allEntries, idToStdStream);
                            }
                        }
                    });
        }
    }

    private void renderHistory(Map<String, DataSnapshot> allEntries, Map<String, String> idToStdStream) {
        historyContainer.removeAllViews();

        if (allEntries.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }
        tvNoHistory.setVisibility(View.GONE);

        // Sort by timestamp descending (newest first)
        List<Map.Entry<String, DataSnapshot>> sortedList = new ArrayList<>(allEntries.entrySet());
        sortedList.sort((a, b) -> {
            Long tsA = a.getValue().child("timestamp").getValue(Long.class);
            Long tsB = b.getValue().child("timestamp").getValue(Long.class);
            if (tsA == null)
                tsA = 0L;
            if (tsB == null)
                tsB = 0L;
            return Long.compare(tsB, tsA); // descending
        });

        for (Map.Entry<String, DataSnapshot> entry : sortedList) {
            String id = entry.getKey();
            DataSnapshot snap = entry.getValue();
            String stdStream = idToStdStream.get(id);

            String noticeInfo = snap.child("noticeInfo").getValue(String.class);
            String image = snap.child("image").getValue(String.class);
            Long timestamp = snap.child("timestamp").getValue(Long.class);

            // Parse std and stream from key e.g. "11_Science"
            String std = "", stream = "";
            if (stdStream != null && stdStream.contains("_")) {
                std = stdStream.split("_")[0];
                stream = stdStream.split("_")[1];
            }

            addHistoryCard(id, std, stream, noticeInfo, image, timestamp, stdStream);
        }
    }

    private void addHistoryCard(String id, String std, String stream,
            String info, String image, Long timestamp, String stdStream) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_history_card, historyContainer, false);

        TextView tvStandard = cardView.findViewById(R.id.tvStandard);
        TextView tvExtraInfo = cardView.findViewById(R.id.tvExtraInfo);
        TextView tvInfo = cardView.findViewById(R.id.tvInfo);
        TextView tvTimestamp = cardView.findViewById(R.id.tvTimestamp);
        ImageView ivHistoryImage = cardView.findViewById(R.id.ivHistoryImage);
        ImageButton btnDelete = cardView.findViewById(R.id.btnDelete);
        Button btnSeen = cardView.findViewById(R.id.btnSeen);
        Button btnUnseen = cardView.findViewById(R.id.btnUnseen);

        tvStandard.setText("Standard: " + std + "  |  Stream: " + stream);

        // Hide the old extra info text
        tvExtraInfo.setVisibility(View.GONE);

        // Dynamically update Seen / Unseen counts
        fetchSeenUnseenCounts(id, stdStream, btnSeen, btnUnseen);

        // Button clicks open dialog
        btnSeen.setOnClickListener(v -> showReadReceiptsDialog(id, stdStream, "seen"));
        btnUnseen.setOnClickListener(v -> showReadReceiptsDialog(id, stdStream, "unseen"));

        if (info != null && !info.isEmpty()) {
            tvInfo.setVisibility(View.VISIBLE);
            tvInfo.setText(info);
        } else {
            tvInfo.setVisibility(View.GONE);
        }

        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(timestamp)));
        }

        if (image != null && !image.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(image, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivHistoryImage.setImageBitmap(bmp);
                    ivHistoryImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                ivHistoryImage.setVisibility(View.GONE);
            }
        }

        btnDelete.setOnClickListener(v -> showDeleteDialog(id, stdStream, cardView));
        historyContainer.addView(cardView);
    }

    private void fetchSeenUnseenCounts(String noticeId, String stdStream, Button btnSeen, Button btnUnseen) {
        DatabaseReference readsRef = FirebaseDatabase.getInstance().getReference("NoticeReads").child(noticeId);
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot readsSnap) {
                Set<String> readUids = new HashSet<>();
                for (DataSnapshot child : readsSnap.getChildren()) {
                    readUids.add(child.getKey());
                }

                // Now fetch target users to calculate unseen
                findTargetStreamsForNotice(noticeId, targetStreams -> {
                    boolean hasTeachers = targetStreams.contains("TeacherNotices");
                    Set<String> studentStreams = new HashSet<>(targetStreams);
                    studentStreams.remove("TeacherNotices");

                    FirebaseDatabase.getInstance().getReference("Students")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    int totalStudents = 0;
                                    Set<String> countedUids = new HashSet<>();
                                    for (DataSnapshot s : snap.getChildren()) {
                                        String st = s.child("Standard").getValue(String.class);
                                        String sr = s.child("Stream").getValue(String.class);
                                        String key = st + "_" + sr;
                                        if (studentStreams.contains(key) && !countedUids.contains(s.getKey())) {
                                            countedUids.add(s.getKey());
                                            totalStudents++;
                                        }
                                    }

                                    if (hasTeachers) {
                                        final int studentCount = totalStudents;
                                        FirebaseDatabase.getInstance().getReference("Teachers")
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot tSnap) {
                                                        int total = studentCount + (int) tSnap.getChildrenCount();
                                                        int seenCount = readUids.size();
                                                        int unseenCount = Math.max(0, total - seenCount);
                                                        btnSeen.setText("\uD83D\uDC41 Seen (" + seenCount + ")");
                                                        btnUnseen.setText("\uD83D\uDEAB Unseen (" + unseenCount + ")");
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError e) {
                                                    }
                                                });
                                    } else {
                                        int seenCount = readUids.size();
                                        int unseenCount = Math.max(0, totalStudents - seenCount);
                                        btnSeen.setText("\uD83D\uDC41 Seen (" + seenCount + ")");
                                        btnUnseen.setText("\uD83D\uDEAB Unseen (" + unseenCount + ")");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError e) {
                                }
                            });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showReadReceiptsDialog(String noticeId, String stdStream, String initialTab) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_read_receipts);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnShowRead = dialog.findViewById(R.id.btnShowRead);
        Button btnShowUnread = dialog.findViewById(R.id.btnShowUnread);
        ProgressBar pbLoading = dialog.findViewById(R.id.pbLoadingReceipts);
        TextView tvEmpty = dialog.findViewById(R.id.tvEmptyReceipts);
        LinearLayout llContainer = dialog.findViewById(R.id.llReceiptsContainer);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        pbLoading.setVisibility(View.VISIBLE);

        final List<String> readNames = new ArrayList<>();
        final List<String> unreadNames = new ArrayList<>();

        // 1) Fetch who has read it
        FirebaseDatabase.getInstance().getReference("NoticeReads").child(noticeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot readsSnap) {
                        Set<String> readUids = new HashSet<>();
                        for (DataSnapshot child : readsSnap.getChildren()) {
                            readUids.add(child.getKey());
                        }

                        // 2) Find which std_stream nodes contain this notice to gather ALL target
                        // students
                        findTargetStreamsForNotice(noticeId, targetStreams -> {
                            boolean hasTeachers = targetStreams.remove("TeacherNotices");

                            // Fetch all students matching any of the target streams
                            FirebaseDatabase.getInstance().getReference("Students")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot studentsSnap) {
                                            Set<String> addedUids = new HashSet<>();
                                            for (DataSnapshot s : studentsSnap.getChildren()) {
                                                String st = s.child("Standard").getValue(String.class);
                                                String sr = s.child("Stream").getValue(String.class);
                                                String studentStdStream = st + "_" + sr;
                                                if (targetStreams.contains(studentStdStream)) {
                                                    String uid = s.getKey();
                                                    if (addedUids.contains(uid))
                                                        continue;
                                                    addedUids.add(uid);
                                                    String name = getStudentName(s);
                                                    if (readUids.contains(uid))
                                                        readNames.add(name);
                                                    else
                                                        unreadNames.add(name);
                                                }
                                            }

                                            if (hasTeachers) {
                                                // Also fetch teachers
                                                FirebaseDatabase.getInstance().getReference("Teachers")
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(
                                                                    @NonNull DataSnapshot teachersSnap) {
                                                                for (DataSnapshot t : teachersSnap.getChildren()) {
                                                                    String uid = t.getKey();
                                                                    // Teachers are stored as
                                                                    // Teachers/{id}/{subjectKey}/{fields}
                                                                    // so we need to dig into the first subject child to
                                                                    // get the name
                                                                    String name = null;
                                                                    for (DataSnapshot subjectChild : t.getChildren()) {
                                                                        name = subjectChild.child("name")
                                                                                .getValue(String.class);
                                                                        if (name == null)
                                                                            name = subjectChild.child("Name")
                                                                                    .getValue(String.class);
                                                                        if (name != null)
                                                                            break;
                                                                    }
                                                                    if (name == null)
                                                                        name = "Unknown Teacher";
                                                                    if (readUids.contains(uid))
                                                                        readNames.add(name);
                                                                    else
                                                                        unreadNames.add(name);
                                                                }
                                                                pbLoading.setVisibility(View.GONE);
                                                                setupDialogTabs(btnShowRead, btnShowUnread, llContainer,
                                                                        tvEmpty, readNames, unreadNames, initialTab);
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError e) {
                                                            }
                                                        });
                                            } else {
                                                pbLoading.setVisibility(View.GONE);
                                                setupDialogTabs(btnShowRead, btnShowUnread, llContainer, tvEmpty,
                                                        readNames, unreadNames, initialTab);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError e) {
                                        }
                                    });
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private String getStudentName(DataSnapshot s) {
        String name = s.child("name").getValue(String.class);
        if (name == null)
            name = s.child("Name").getValue(String.class);
        if (name == null)
            name = s.child("studentName").getValue(String.class);
        if (name == null)
            name = "Unknown Student";
        return name;
    }

    private interface TargetStreamsCallback {
        void onResult(Set<String> targetStreams);
    }

    private void findTargetStreamsForNotice(String noticeId, TargetStreamsCallback callback) {
        Set<String> found = new HashSet<>();
        AtomicInteger checked = new AtomicInteger(0);
        int totalToCheck = STD_STREAM_KEYS.length + 1; // +1 for TeacherNotices

        for (String key : STD_STREAM_KEYS) {
            noticesReference.child(key).child(noticeId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists())
                                found.add(key);
                            if (checked.incrementAndGet() >= totalToCheck)
                                callback.onResult(found);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (checked.incrementAndGet() >= totalToCheck)
                                callback.onResult(found);
                        }
                    });
        }
        // Check TeacherNotices
        teacherNoticesReference.child(noticeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                            found.add("TeacherNotices");
                        if (checked.incrementAndGet() >= totalToCheck)
                            callback.onResult(found);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (checked.incrementAndGet() >= totalToCheck)
                            callback.onResult(found);
                    }
                });
    }

    private void setupDialogTabs(Button btnRead, Button btnUnread, LinearLayout container, TextView tvEmpty,
            List<String> readNames, List<String> unreadNames, String initialTab) {
        // Initial state based on which button was tapped
        if ("unseen".equals(initialTab)) {
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, unreadNames, "All users have read this.");
        } else {
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, readNames, "No one has read this yet.");
        }

        btnRead.setOnClickListener(v -> {
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, readNames, "No one has read this yet.");
        });

        btnUnread.setOnClickListener(v -> {
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, unreadNames, "All users have read this.");
        });
    }

    private void displayNamesInDialog(LinearLayout container, TextView tvEmpty, List<String> names,
            String emptyMessage) {
        container.removeAllViews();
        if (names.isEmpty()) {
            tvEmpty.setText(emptyMessage);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (String name : names) {
                TextView tvName = new TextView(this);
                tvName.setText("• " + name);
                tvName.setTextColor(0xFFFFFFFF);
                tvName.setTextSize(16f);
                tvName.setPadding(0, 8, 0, 8);
                container.addView(tvName);
            }
        }
    }

    private void showDeleteDialog(String noticeId, String stdStream, View cardView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice? It will be removed for everyone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotice(noticeId, stdStream, cardView))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteNotice(String noticeId, String stdStream, View cardView) {
        // Delete from ALL 6 std_stream nodes
        for (String key : STD_STREAM_KEYS) {
            noticesReference.child(key).child(noticeId).removeValue();
        }
        // Also delete from flat TeacherNotices node
        teacherNoticesReference.child(noticeId).removeValue();
        // Clean up read receipts
        FirebaseDatabase.getInstance().getReference("NoticeReads").child(noticeId).removeValue();
        historyContainer.removeView(cardView);
        if (historyContainer.getChildCount() == 0) {
            tvNoHistory.setVisibility(View.VISIBLE);
        }
        Toast.makeText(PrincipalNoticesActivity.this,
                "Notice deleted successfully!", Toast.LENGTH_SHORT).show();
    }

    // ==================== UPLOAD ====================

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
        } else {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    private void removeImage() {
        base64Image = null;
        previewImage.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            try {
                if (requestCode == CAMERA_REQUEST) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST) {
                    Uri imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                }
                if (bitmap != null) {
                    Bitmap compressed = compressBitmap(bitmap, 800, 800);
                    base64Image = encodeImageToBase64(compressed);
                    previewImage.setImageBitmap(compressed);
                    imagePreviewContainer.setVisibility(View.VISIBLE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        float ratio = (float) width / height;
        if (ratio > 1) {
            width = maxWidth;
            height = (int) (width / ratio);
        } else {
            height = maxHeight;
            width = (int) (height * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
        return Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
    }

    private void submitNotice() {
        int selectedStandardId = standardRadioGroup.getCheckedRadioButtonId();
        if (selectedStandardId == -1) {
            Toast.makeText(this, "Please select a standard", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkScience.isChecked() && !checkCommerce.isChecked() &&
                !checkArts.isChecked() && !checkAllStreams.isChecked()) {
            Toast.makeText(this, "Please select at least one stream", Toast.LENGTH_SHORT).show();
            return;
        }
        int shareTeacherSelectedId = shareTeacherRadioGroup.getCheckedRadioButtonId();
        if (shareTeacherSelectedId == -1) {
            Toast.makeText(this, "Please select Yes or No for sharing with teachers", Toast.LENGTH_SHORT).show();
            return;
        }
        String noticeInfo = noticeInfoEdit.getText().toString().trim();
        if (noticeInfo.isEmpty() && base64Image == null) {
            Toast.makeText(this, "Please enter notice information or add an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedStandard = selectedStandardId == R.id.radio11 ? "11"
                : selectedStandardId == R.id.radio12 ? "12" : "All";

        List<String> selectedStreams = new ArrayList<>();
        if (checkAllStreams.isChecked())
            selectedStreams.add("All");
        else {
            if (checkScience.isChecked())
                selectedStreams.add("Science");
            if (checkCommerce.isChecked())
                selectedStreams.add("Commerce");
            if (checkArts.isChecked())
                selectedStreams.add("Arts");
        }

        boolean shareWithTeachers = radioYes.isChecked();
        submitBtn.setEnabled(false);
        saveNoticeToFirebase(selectedStandard, selectedStreams, noticeInfo, shareWithTeachers);
    }

    private void saveNoticeToFirebase(String standard, List<String> streams,
            String noticeInfo, boolean shareWithTeachers) {
        String noticeId = noticesReference.push().getKey();
        long timestamp = System.currentTimeMillis();

        List<String> standards = new ArrayList<>();
        if (standard.equals("All")) {
            standards.add("11");
            standards.add("12");
        } else
            standards.add(standard);

        List<String> streamsList = new ArrayList<>();
        if (streams.contains("All")) {
            streamsList.add("Science");
            streamsList.add("Commerce");
            streamsList.add("Arts");
        } else
            streamsList.addAll(streams);

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("noticeInfo", noticeInfo);
        studentData.put("image", base64Image != null ? base64Image : "");
        studentData.put("timestamp", timestamp);

        // Save student nodes
        for (String std : standards) {
            for (String stream : streamsList) {
                noticesReference.child(std + "_" + stream).child(noticeId)
                        .setValue(studentData);
            }
        }

        // Save teacher data flat under Notices/TeacherNotices/{id}
        if (shareWithTeachers) {
            Map<String, Object> teacherData = new HashMap<>();
            teacherData.put("noticeInfo", noticeInfo);
            teacherData.put("image", base64Image != null ? base64Image : "");
            teacherData.put("timestamp", timestamp);
            teacherData.put("standard", standard);
            teacherData.put("streams", streams.contains("All") ? "All" : String.join(", ", streams));
            teacherNoticesReference.child(noticeId)
                    .setValue(teacherData);
        }

        // Show success after a short delay to allow Firebase writes to propagate
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(PrincipalNoticesActivity.this,
                    "Notice uploaded successfully!", Toast.LENGTH_SHORT).show();
            clearForm();
            submitBtn.setEnabled(true);
        }, 1000);
    }

    private void clearForm() {
        standardRadioGroup.clearCheck();
        checkScience.setChecked(false);
        checkCommerce.setChecked(false);
        checkArts.setChecked(false);
        checkAllStreams.setChecked(false);
        shareTeacherRadioGroup.clearCheck();
        noticeInfoEdit.setText("");
        base64Image = null;
        previewImage.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.principal_home)
            startActivity(new Intent(this, principaltab.class));
        else if (id == R.id.principal_timetable)
            startActivity(new Intent(this, principletimetable.class));
        else if (id == R.id.principal_exam)
            startActivity(new Intent(this, PrincipalExamActivity.class));
        else if (id == R.id.principal_notices) {
            /* current */ } else if (id == R.id.principal_calender)
            startActivity(new Intent(this, PrincipalCalendarActivity.class));
        else if (id == R.id.principal_payment)
            startActivity(new Intent(this, PrincipalPaymentActivity.class));
        else if (id == R.id.principal_feedback)
            startActivity(new Intent(this, principalfeedback.class));
        else if (id == R.id.principal_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Principal");
            String principalId = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE).getString("principalId", "unknown");
            caIntent.putExtra("senderUid", principalId);
            startActivity(caIntent);
        }
        if (drawerLayout != null)
            drawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera();
            else
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
