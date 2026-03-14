package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.io.InputStream;
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

public class teachersyllabus extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "TeacherSyllabus";
    private static final int REQ_CAMERA = 200;
    private static final int REQ_MEDIA_PERMISSION = 501;

    private RadioGroup standardRadioGroup, shareTeacherRadioGroup;
    private RadioButton radio11, radio12, radioYes, radioNo;
    private CheckBox checkScience, checkCommerce, checkArts;
    private EditText syllabusInfoEdit;
    private Button cameraBtn, galleryBtn, submitBtn, removeImageBtn;
    private ImageView previewImage;
    private LinearLayout imagePreviewContainer;
    private LinearLayout historyContainer;
    private ProgressBar historyProgressBar;
    private TextView tvNoHistory;

    private String teacherStandard = "";
    private String teacherStream = "";
    private String teacherId = "";

    private DatabaseReference syllabusRef, teacherSyllabusRef;
    private Uri imageUri = null;
    private String base64Image = null;

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraUri;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // All possible std_stream combinations
    private static final String[] STD_STREAM_KEYS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    private final List<String> addedIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachersyllabus);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Syllabus");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        initializeViews();
        setupFirebase();
        loadTeacherPrefs();
        setupPickerLaunchers();
        setupListeners();
        loadHistory();
    }

    private void initializeViews() {
        standardRadioGroup = findViewById(R.id.standardRadioGroup);
        radio11 = findViewById(R.id.radio11);
        radio12 = findViewById(R.id.radio12);

        shareTeacherRadioGroup = findViewById(R.id.shareTeacherRadioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);

        checkScience = findViewById(R.id.checkScience);
        checkCommerce = findViewById(R.id.checkCommerce);
        checkArts = findViewById(R.id.checkArts);

        syllabusInfoEdit = findViewById(R.id.syllabusInfoEdit);
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
        syllabusRef = FirebaseDatabase.getInstance().getReference("Syllabus");
        teacherSyllabusRef = FirebaseDatabase.getInstance().getReference("Syllabus").child("TeacherSyllabus");
    }

    private void loadTeacherPrefs() {
        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        teacherStandard = tPrefs.getString("teacherStandard", "");
        teacherStream = tPrefs.getString("teacherStream", "");
        teacherId = tPrefs.getString("teacherId", "");

        if (!teacherStandard.isEmpty()) {
            if (teacherStandard.equals("11")) {
                radio11.setChecked(true);
                radio12.setEnabled(false);
            } else if (teacherStandard.equals("12")) {
                radio12.setChecked(true);
                radio11.setEnabled(false);
            }
        }

        if (!teacherStream.isEmpty()) {
            if (teacherStream.equalsIgnoreCase("Science")) {
                checkScience.setChecked(true);
                checkCommerce.setEnabled(false);
                checkArts.setEnabled(false);
            } else if (teacherStream.equalsIgnoreCase("Commerce")) {
                checkCommerce.setChecked(true);
                checkScience.setEnabled(false);
                checkArts.setEnabled(false);
            } else if (teacherStream.equalsIgnoreCase("Arts")) {
                checkArts.setChecked(true);
                checkScience.setEnabled(false);
                checkCommerce.setEnabled(false);
            }
        }
    }

    private void setupListeners() {
        cameraBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::pickFromCamera, 150);
        });
        galleryBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::pickFromGallery, 150);
        });
        removeImageBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::removeImage, 150);
        });
        submitBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::submitSyllabus, 150);
        });
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int img = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
            return img == PackageManager.PERMISSION_GRANTED;
        } else {
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_MEDIA_IMAGES },
                    REQ_MEDIA_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQ_MEDIA_PERMISSION);
        }
    }

    private void pickFromGallery() {
        if (!hasMediaPermission()) {
            requestMediaPermission();
            return;
        }
        filePickerLauncher.launch(new String[] { "image/*" });
    }

    private void pickFromCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.CAMERA },
                    REQ_CAMERA);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Syllabus_Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Syllabus Photo");

        cameraUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (cameraUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPickerLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not take persistable permission", e);
                        }
                        handleImageSelection(uri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cameraUri != null) {
                        handleImageSelection(cameraUri);
                    }
                });
    }

    private void handleImageSelection(Uri uri) {
        this.imageUri = uri;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Compress & Base64
            int maxSize = 800; // slightly higher quality for syllabus
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float ratio = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                int width = Math.round((float) ratio * bitmap.getWidth());
                int height = Math.round((float) ratio * bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            previewImage.setImageBitmap(bitmap);
            imagePreviewContainer.setVisibility(View.VISIBLE);

            if (inputStream != null)
                inputStream.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void removeImage() {
        imageUri = null;
        base64Image = null;
        imagePreviewContainer.setVisibility(View.GONE);
    }

    private void submitSyllabus() {
        String info = syllabusInfoEdit.getText().toString().trim();
        if (info.isEmpty() && base64Image == null) {
            Toast.makeText(this, "Please add some info or an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String std = radio11.isChecked() ? "11" : (radio12.isChecked() ? "12" : "");
        if (std.isEmpty()) {
            Toast.makeText(this, "Please select a standard", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> streams = new ArrayList<>();
        if (checkScience.isChecked())
            streams.add("Science");
        if (checkCommerce.isChecked())
            streams.add("Commerce");
        if (checkArts.isChecked())
            streams.add("Arts");

        if (streams.isEmpty()) {
            Toast.makeText(this, "Please select at least one stream", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean shareTeachers = radioYes.isChecked();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading Syllabus...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String pushId = syllabusRef.push().getKey();
        if (pushId == null) {
            progressDialog.dismiss();
            return;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("syllabusInfo", info);
        data.put("image", base64Image != null ? base64Image : "");
        data.put("timestamp", System.currentTimeMillis());
        data.put("teacherId", teacherId); // link to teacher who uploaded

        int totalUploads = streams.size() + (shareTeachers ? 1 : 0);
        AtomicInteger completedUploads = new AtomicInteger(0);
        AtomicInteger failedUploads = new AtomicInteger(0);

        for (String stream : streams) {
            String path = std + "_" + stream;
            syllabusRef.child(path).child(pushId).setValue(data)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful())
                            completedUploads.incrementAndGet();
                        else
                            failedUploads.incrementAndGet();
                        checkUploadStatus(progressDialog, completedUploads, failedUploads, totalUploads);
                    });
        }

        if (shareTeachers) {
            teacherSyllabusRef.child(pushId).setValue(data)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful())
                            completedUploads.incrementAndGet();
                        else
                            failedUploads.incrementAndGet();
                        checkUploadStatus(progressDialog, completedUploads, failedUploads, totalUploads);
                    });
        }
    }

    private void checkUploadStatus(ProgressDialog dialog, AtomicInteger success, AtomicInteger fail, int total) {
        if (success.get() + fail.get() == total) {
            dialog.dismiss();
            if (fail.get() == 0) {
                Toast.makeText(this, "Syllabus uploaded successfully", Toast.LENGTH_SHORT).show();
                syllabusInfoEdit.setText("");
                removeImage();
            } else {
                Toast.makeText(this, "Some uploads failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== HISTORY ====================

    private void loadHistory() {
        historyProgressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        historyContainer.removeAllViews();
        addedIds.clear();

        final int totalNodes = STD_STREAM_KEYS.length + 1; // +1 for TeacherSyllabus
        final AtomicInteger loadedNodes = new AtomicInteger(0);
        final Map<String, DataSnapshot> allEntries = new HashMap<>();
        final Map<String, String> idToStdStream = new HashMap<>();

        // 1. Check Student categories
        for (String stdStream : STD_STREAM_KEYS) {
            syllabusRef.child(stdStream).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    processSnapshot(snapshot, stdStream, allEntries, idToStdStream);
                    if (loadedNodes.incrementAndGet() >= totalNodes) {
                        historyProgressBar.setVisibility(View.GONE);
                        renderHistory(allEntries, idToStdStream);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (loadedNodes.incrementAndGet() >= totalNodes) {
                        historyProgressBar.setVisibility(View.GONE);
                        renderHistory(allEntries, idToStdStream);
                    }
                }
            });
        }

        // 2. Check TeacherSyllabus
        teacherSyllabusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processSnapshot(snapshot, "TeacherSyllabus", allEntries, idToStdStream);
                if (loadedNodes.incrementAndGet() >= totalNodes) {
                    historyProgressBar.setVisibility(View.GONE);
                    renderHistory(allEntries, idToStdStream);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadedNodes.incrementAndGet() >= totalNodes) {
                    historyProgressBar.setVisibility(View.GONE);
                    renderHistory(allEntries, idToStdStream);
                }
            }
        });
    }

    private void processSnapshot(DataSnapshot snapshot, String stdStream,
            Map<String, DataSnapshot> allEntries, Map<String, String> idToStdStream) {
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
            String uploaderId = child.child("teacherId").getValue(String.class);
            // ONLY show syllabus uploaded by THIS teacher
            if (uploaderId != null && uploaderId.equals(teacherId)) {
                String id = child.getKey();
                if (!allEntries.containsKey(id)) {
                    allEntries.put(id, child);
                    idToStdStream.put(id, stdStream);
                }
            }
        }
    }

    private void renderHistory(Map<String, DataSnapshot> allEntries, Map<String, String> idToStdStream) {
        historyContainer.removeAllViews();

        if (allEntries.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }
        tvNoHistory.setVisibility(View.GONE);

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

            String info = snap.child("syllabusInfo").getValue(String.class);
            String image = snap.child("image").getValue(String.class);
            Long timestamp = snap.child("timestamp").getValue(Long.class);

            String std = "", stream = "";
            if (stdStream != null && stdStream.contains("_")) {
                std = stdStream.split("_")[0];
                stream = stdStream.split("_")[1];
            } else if (stdStream != null && stdStream.equals("TeacherSyllabus")) {
                std = "Teachers";
                stream = "Only";
            }

            addHistoryCard(id, std, stream, info, image, timestamp, stdStream);
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
        tvExtraInfo.setVisibility(View.GONE);

        fetchSeenUnseenCounts(id, btnSeen, btnUnseen);

        btnSeen.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> showReadReceiptsDialog(id, "seen"), 150);
        });
        btnUnseen.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> showReadReceiptsDialog(id, "unseen"), 150);
        });

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

        btnDelete.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> showDeleteDialog(id, cardView), 150);
        });
        historyContainer.addView(cardView);
    }

    private void showDeleteDialog(String id, View cardView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Syllabus")
                .setMessage("Are you sure you want to delete this syllabus? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from all std streams
                    for (String s : STD_STREAM_KEYS) {
                        syllabusRef.child(s).child(id).removeValue();
                    }
                    teacherSyllabusRef.child(id).removeValue();
                    FirebaseDatabase.getInstance().getReference("SyllabusReads").child(id).removeValue();
                    historyContainer.removeView(cardView);
                    Toast.makeText(this, "Syllabus Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchSeenUnseenCounts(String syllabusId, Button btnSeen, Button btnUnseen) {
        DatabaseReference readsRef = FirebaseDatabase.getInstance().getReference("SyllabusReads").child(syllabusId);
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot readsSnap) {
                Set<String> readUids = new HashSet<>();
                for (DataSnapshot child : readsSnap.getChildren()) {
                    readUids.add(child.getKey());
                }

                findTargetStreamsForSyllabus(syllabusId, targetStreams -> {
                    boolean hasTeachers = targetStreams.contains("TeacherSyllabus");
                    Set<String> studentStreams = new HashSet<>(targetStreams);
                    studentStreams.remove("TeacherSyllabus");

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

    private interface TargetStreamsCallback {
        void onResult(Set<String> targetStreams);
    }

    private void findTargetStreamsForSyllabus(String syllabusId, TargetStreamsCallback callback) {
        Set<String> found = new HashSet<>();
        AtomicInteger checked = new AtomicInteger(0);
        int totalToCheck = STD_STREAM_KEYS.length + 1;

        for (String key : STD_STREAM_KEYS) {
            syllabusRef.child(key).child(syllabusId).addListenerForSingleValueEvent(new ValueEventListener() {
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
        teacherSyllabusRef.child(syllabusId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists())
                    found.add("TeacherSyllabus");
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

    private void showReadReceiptsDialog(String syllabusId, String initialTab) {
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

        FirebaseDatabase.getInstance().getReference("SyllabusReads").child(syllabusId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot readsSnap) {
                        Set<String> readUids = new HashSet<>();
                        for (DataSnapshot child : readsSnap.getChildren())
                            readUids.add(child.getKey());

                        findTargetStreamsForSyllabus(syllabusId, targetStreams -> {
                            boolean hasTeachers = targetStreams.remove("TeacherSyllabus");

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
                                                FirebaseDatabase.getInstance().getReference("Teachers")
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(
                                                                    @NonNull DataSnapshot teachersSnap) {
                                                                for (DataSnapshot t : teachersSnap.getChildren()) {
                                                                    String uid = t.getKey();
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

    private void setupDialogTabs(Button btnRead, Button btnUnread, LinearLayout container, TextView tvEmpty,
            List<String> readNames, List<String> unreadNames, String initialTab) {
        if ("unseen".equals(initialTab)) {
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, unreadNames, "All users have seen this.");
        } else {
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, readNames, "No one has seen this yet.");
        }

        btnRead.setOnClickListener(v -> {
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32));
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, readNames, "No one has seen this yet.");
        });

        btnUnread.setOnClickListener(v -> {
            btnUnread.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
            btnRead.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2E55));
            displayNamesInDialog(container, tvEmpty, unreadNames, "All users have seen this.");
        });
    }

    private void displayNamesInDialog(LinearLayout container, TextView tvEmpty, List<String> names,
            String emptyMessage) {
        container.removeAllViews();
        if (names == null || names.isEmpty()) {
            tvEmpty.setText(emptyMessage);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            for (String name : names) {
                TextView tv = new TextView(this);
                tv.setText("• " + name);
                tv.setTextColor(getResources().getColor(android.R.color.white));
                tv.setTextSize(16);
                tv.setPadding(0, 8, 0, 8);
                container.addView(tv);
            }
        }
    }

    // Navigation logic
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.teacher_home) {
            startActivity(new Intent(this, teachertab.class));
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            startActivity(new Intent(this, teacherhomework.class));
        } else if (id == R.id.teacher_syllabus) {
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_timetable) {
            startActivity(new Intent(this, teachertimetable.class));
        } else if (id == R.id.teacher_exam) {
            startActivity(new Intent(this, teacherexam.class));
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_feedback) {
            startActivity(new Intent(this, teacherfeedback.class));
        } else if (id == R.id.teacher_notices) {
            startActivity(new Intent(this, TeacherNoticesActivity.class));
        } else if (id == R.id.teacher_fees) {
            startActivity(new Intent(this, TeacherFeesActivity.class));
        } else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            caIntent.putExtra("senderUid", getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown"));
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
