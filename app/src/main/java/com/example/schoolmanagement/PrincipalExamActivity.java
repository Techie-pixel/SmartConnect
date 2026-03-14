package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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
import android.widget.Spinner;
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

public class PrincipalExamActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private RadioGroup standardRadioGroup, examTypeRadioGroup, shareTeacherRadioGroup;
    private RadioButton radio11, radio12, radioAll;
    private RadioButton radioUnitTest, radioMidTerm, radioFinalExam, radioPractical;
    private RadioButton radioYes, radioNo;
    private CheckBox checkScience, checkCommerce, checkArts, checkAllStreams;
    private EditText examInfoEdit;
    private Spinner teacherSpinner;
    private Button cameraBtn, galleryBtn, submitBtn, removeImageBtn;
    private ImageView previewImage;
    private LinearLayout imagePreviewContainer;
    private LinearLayout historyContainer;
    private ProgressBar historyProgressBar;
    private TextView tvNoHistory;

    private DatabaseReference examReference, teacherExamReference;
    private String base64Image = null;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private List<String> teacherNames = new ArrayList<>();
    private List<String> teacherIds = new ArrayList<>();
    private ArrayAdapter<String> teacherAdapter;

    // Exam types as stored in Firebase (spaces removed)
    private static final String[] EXAM_TYPE_KEYS = { "UnitTest", "MidTerm", "FinalExam", "Practical" };
    private static final String[] STD_STREAM_KEYS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_exam);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setTitle("Exam");

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        // Apply toolbar and logo animations
        UIAnimator.animateToolbar(toolbar, 100);
        android.widget.ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 200);

        initializeViews();
        
        // Staggered animation for all controls
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

        examTypeRadioGroup = findViewById(R.id.examTypeRadioGroup);
        radioUnitTest = findViewById(R.id.radioUnitTest);
        radioMidTerm = findViewById(R.id.radioMidTerm);
        radioFinalExam = findViewById(R.id.radioFinalExam);
        radioPractical = findViewById(R.id.radioPractical);

        shareTeacherRadioGroup = findViewById(R.id.shareTeacherRadioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);

        checkScience = findViewById(R.id.checkScience);
        checkCommerce = findViewById(R.id.checkCommerce);
        checkArts = findViewById(R.id.checkArts);
        checkAllStreams = findViewById(R.id.checkAllStreams);

        examInfoEdit = findViewById(R.id.examInfoEdit);
        teacherSpinner = findViewById(R.id.teacherSpinner);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        submitBtn = findViewById(R.id.submitBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);
        previewImage = findViewById(R.id.previewImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        historyContainer = findViewById(R.id.historyContainer);
        historyProgressBar = findViewById(R.id.historyProgressBar);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        teacherNames.add("Select Teacher (Optional)");
        teacherIds.add("none");
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teacherNames);
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teacherSpinner.setAdapter(teacherAdapter);
    }

    private void setupFirebase() {
        examReference = FirebaseDatabase.getInstance().getReference("Exam");
        teacherExamReference = FirebaseDatabase.getInstance().getReference("Exam").child("TeacherExam");
        fetchTeachers();
    }

    private void fetchTeachers() {
        FirebaseDatabase.getInstance().getReference("Teachers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot teacherSnap : snapshot.getChildren()) {
                            String name = teacherSnap.child("Name").getValue(String.class);
                            if (name == null)
                                name = teacherSnap.child("name").getValue(String.class);
                            String id = teacherSnap.getKey();
                            if (name != null && id != null) {
                                teacherNames.add(name);
                                teacherIds.add(id);
                            }
                        }
                        teacherAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
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
        submitBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> submitExam(), 150); });

        // Apply animations
        UIAnimator.animateEditText(examInfoEdit, 200);
        UIAnimator.animateButton(cameraBtn, 350);
        UIAnimator.animateButton(galleryBtn, 500);
        UIAnimator.animateButton(submitBtn, 650);
    }

    // ==================== HISTORY ====================
    // Structure: Exam/{ExamTypeKey}/{std_stream}/{id} -> {examInfo, examType,
    // image, timestamp}

    private void loadHistory() {
        historyProgressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        // historyContainer.removeAllViews(); // Don't clear here, clear in renderHistory

        // Total nodes to read: 4 examTypes x 6 stdStream = 24
        final int totalNodes = EXAM_TYPE_KEYS.length * STD_STREAM_KEYS.length;
        final AtomicInteger loadedNodes = new AtomicInteger(0);

        // id -> {snapshot, examTypeKey, stdStream}
        final Map<String, DataSnapshot> allEntries = new HashMap<>();
        final Map<String, String> idToExamType = new HashMap<>();
        final Map<String, String> idToStdStream = new HashMap<>();

        for (String examTypeKey : EXAM_TYPE_KEYS) {
            for (String stdStream : STD_STREAM_KEYS) {
                examReference.child(examTypeKey).child(stdStream)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    String id = child.getKey();
                                    if (id != null) {
                                        allEntries.put(id, child);
                                        idToExamType.put(id, examTypeKey);
                                        idToStdStream.put(id, stdStream);
                                    }
                                }

                                if (loadedNodes.incrementAndGet() >= totalNodes) {
                                    historyProgressBar.setVisibility(View.GONE);
                                    renderHistory(allEntries, idToExamType, idToStdStream);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (loadedNodes.incrementAndGet() >= totalNodes) {
                                    historyProgressBar.setVisibility(View.GONE);
                                    renderHistory(allEntries, idToExamType, idToStdStream);
                                }
                            }
                        });
            }
        }
    }

    private void renderHistory(Map<String, DataSnapshot> allEntries,
            Map<String, String> idToExamType,
            Map<String, String> idToStdStream) {
        historyContainer.removeAllViews();

        if (allEntries.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }
        tvNoHistory.setVisibility(View.GONE);

        // Sort by timestamp descending
        List<Map.Entry<String, DataSnapshot>> sortedList = new ArrayList<>(allEntries.entrySet());
        sortedList.sort((a, b) -> {
            Long tsA = a.getValue().child("timestamp").getValue(Long.class);
            Long tsB = b.getValue().child("timestamp").getValue(Long.class);
            if (tsA == null)
                tsA = 0L;
            if (tsB == null)
                tsB = 0L;
            return Long.compare(tsB, tsA);
        });

        for (Map.Entry<String, DataSnapshot> entry : sortedList) {
            String id = entry.getKey();
            DataSnapshot snap = entry.getValue();
            String examTypeKey = idToExamType.get(id);
            String stdStream = idToStdStream.get(id);

            String examInfo = snap.child("examInfo").getValue(String.class);
            String examType = snap.child("examType").getValue(String.class);
            String image = snap.child("image").getValue(String.class);
            Long timestamp = snap.child("timestamp").getValue(Long.class);
            String assignedTeacher = snap.child("assignedTeacherName").getValue(String.class);

            // Parse std and stream from key e.g. "11_Commerce"
            String std = "", stream = "";
            if (stdStream != null && stdStream.contains("_")) {
                std = stdStream.split("_")[0];
                stream = stdStream.split("_")[1];
            }

            addHistoryCard(id, std, stream, examType, examInfo, image, timestamp, examTypeKey, stdStream, assignedTeacher);
        }
    }

    private void addHistoryCard(String id, String std, String stream,
            String examType, String info, String image,
            Long timestamp, String examTypeKey, String stdStream, String assignedTeacher) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_history_card, historyContainer, false);

        TextView tvStandard = cardView.findViewById(R.id.tvStandard);
        TextView tvExtraInfo = cardView.findViewById(R.id.tvExtraInfo);
        TextView tvInfo = cardView.findViewById(R.id.tvInfo);
        TextView tvTimestamp = cardView.findViewById(R.id.tvTimestamp);
        ImageView ivHistoryImage = cardView.findViewById(R.id.ivHistoryImage);
        ImageButton btnDelete = cardView.findViewById(R.id.btnDelete);

        tvStandard.setText("Standard: " + std + "  |  Stream: " + stream);

        // Show exam type and assigned teacher
        String extra = "Type: " + (examType != null ? examType : "Unknown");
        if (assignedTeacher != null && !assignedTeacher.equals("None")) {
            extra += "  |  Teacher: " + assignedTeacher;
        }
        tvExtraInfo.setVisibility(View.VISIBLE);
        tvExtraInfo.setText(extra);

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

                    ivHistoryImage.setOnClickListener(v -> {
                        Intent intent = new Intent(PrincipalExamActivity.this, ImagePreviewActivity.class);
                        intent.putExtra("image_base64", image);
                        startActivity(intent);
                    });
                }
            } catch (Exception e) {
                ivHistoryImage.setVisibility(View.GONE);
            }
        }

        btnDelete.setOnClickListener(v -> showDeleteDialog(id, examTypeKey, stdStream, cardView));

        // Wire up Seen / Unseen buttons
        Button btnSeen = cardView.findViewById(R.id.btnSeen);
        Button btnUnseen = cardView.findViewById(R.id.btnUnseen);
        fetchExamSeenUnseenCounts(id, stdStream, btnSeen, btnUnseen);

        historyContainer.addView(cardView);
    }

    private void showDeleteDialog(String examId, String examTypeKey, String stdStream, View cardView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Exam Schedule")
                .setMessage("Are you sure you want to delete this exam? It will be removed for everyone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteExam(examId, examTypeKey, stdStream, cardView))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteExam(String examId, String examTypeKey, String stdStream, View cardView) {
        // Delete from all possible Exam/{examTypeKey}/{stdStream}/{id} paths
        for (String etKey : EXAM_TYPE_KEYS) {
            for (String ssKey : STD_STREAM_KEYS) {
                examReference.child(etKey).child(ssKey).child(examId).removeValue();
            }
        }
        // Also delete from TeacherExam (flat node inside Exam folder)
        teacherExamReference.child(examId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        historyContainer.removeView(cardView);
                        if (historyContainer.getChildCount() == 0) {
                            tvNoHistory.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(PrincipalExamActivity.this,
                                "Exam deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PrincipalExamActivity.this,
                                "Delete failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================== SEEN / UNSEEN TRACKING ====================

    private void fetchExamSeenUnseenCounts(String examId, String stdStream, Button btnSeen, Button btnUnseen) {
        DatabaseReference readsRef = FirebaseDatabase.getInstance().getReference("ExamReads").child(examId);
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot readsSnap) {
                Set<String> readUids = new HashSet<>();
                for (DataSnapshot child : readsSnap.getChildren()) {
                    readUids.add(child.getKey());
                }

                // Count target students for this stdStream only
                FirebaseDatabase.getInstance().getReference("Students")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snap) {
                                int totalStudents = 0;
                                for (DataSnapshot s : snap.getChildren()) {
                                    String st = s.child("Standard").getValue(String.class);
                                    String sr = s.child("Stream").getValue(String.class);
                                    String key = st + "_" + sr;
                                    if (stdStream != null && stdStream.equals(key)) {
                                        totalStudents++;
                                    }
                                }
                                int seenCount = readUids.size();
                                int unseenCount = Math.max(0, totalStudents - seenCount);
                                btnSeen.setText("\uD83D\uDC41 Seen (" + seenCount + ")");
                                btnUnseen.setText("\uD83D\uDEAB Unseen (" + unseenCount + ")");

                                // Set click listeners for detailed dialog
                                btnSeen.setOnClickListener(v -> showSeenUnseenDialog("Seen", readUids, snap, stdStream));
                                btnUnseen.setOnClickListener(v -> showSeenUnseenDialog("Unseen", readUids, snap, stdStream));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError e) {
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showSeenUnseenDialog(String title, Set<String> readUids, DataSnapshot studentsSnap,
            String targetStdStream) {
        List<String> names = new ArrayList<>();
        for (DataSnapshot s : studentsSnap.getChildren()) {
            String st = s.child("Standard").getValue(String.class);
            String sr = s.child("Stream").getValue(String.class);
            String key = st + "_" + sr;
            if (targetStdStream != null && targetStdStream.equals(key)) {
                String uid = s.getKey();
                boolean hasSeen = readUids.contains(uid);

                if (title.equals("Seen") && hasSeen) {
                    String name = s.child("name").getValue(String.class);
                    if (name == null)
                        name = s.child("Name").getValue(String.class);
                    names.add(name != null ? name : "Unknown Student");
                } else if (title.equals("Unseen") && !hasSeen) {
                    String name = s.child("name").getValue(String.class);
                    if (name == null)
                        name = s.child("Name").getValue(String.class);
                    names.add(name != null ? name : "Unknown Student");
                }
            }
        }

        if (names.isEmpty()) {
            names.add("No students found.");
        }

        String[] namesArray = names.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(title + " Students")
                .setItems(namesArray, null)
                .setPositiveButton("Close", null)
                .show();
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
        startActivityForResult(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                GALLERY_REQUEST);
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
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                }
                if (bitmap != null) {
                    Bitmap compressed = compressBitmap(bitmap, 800, 800);
                    base64Image = encodeImageToBase64(compressed);
                    previewImage.setImageBitmap(compressed);
                    imagePreviewContainer.setVisibility(View.VISIBLE);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxW, int maxH) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        float ratio = (float) w / h;
        if (ratio > 1) {
            w = maxW;
            h = (int) (w / ratio);
        } else {
            h = maxH;
            w = (int) (h * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, w, h, true);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, bos);
        return Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
    }

    private void submitExam() {
        int stdId = standardRadioGroup.getCheckedRadioButtonId();
        if (stdId == -1) {
            Toast.makeText(this, "Please select a standard", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkScience.isChecked() && !checkCommerce.isChecked() &&
                !checkArts.isChecked() && !checkAllStreams.isChecked()) {
            Toast.makeText(this, "Please select at least one stream", Toast.LENGTH_SHORT).show();
            return;
        }

        int examTypeId = examTypeRadioGroup.getCheckedRadioButtonId();
        if (examTypeId == -1) {
            Toast.makeText(this, "Please select an exam type", Toast.LENGTH_SHORT).show();
            return;
        }

        int shareId = shareTeacherRadioGroup.getCheckedRadioButtonId();
        if (shareId == -1) {
            Toast.makeText(this, "Please select Yes or No for sharing with teachers", Toast.LENGTH_SHORT).show();
            return;
        }

        String examInfo = examInfoEdit.getText().toString().trim();
        if (examInfo.isEmpty() && base64Image == null) {
            Toast.makeText(this, "Please enter exam information or add an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String standard = stdId == R.id.radio11 ? "11" : stdId == R.id.radio12 ? "12" : "All";

        // examType display label and Firebase key
        String examTypeLabel, examTypeKey;
        if (examTypeId == R.id.radioUnitTest) {
            examTypeLabel = "Unit Test";
            examTypeKey = "UnitTest";
        } else if (examTypeId == R.id.radioMidTerm) {
            examTypeLabel = "Mid Term";
            examTypeKey = "MidTerm";
        } else if (examTypeId == R.id.radioFinalExam) {
            examTypeLabel = "Final Exam";
            examTypeKey = "FinalExam";
        } else {
            examTypeLabel = "Practical";
            examTypeKey = "Practical";
        }

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

        int teacherPos = teacherSpinner.getSelectedItemPosition();
        String selectedTeacherId = "none";
        String selectedTeacherName = "None";
        if (teacherPos > 0) {
            selectedTeacherId = teacherIds.get(teacherPos);
            selectedTeacherName = teacherNames.get(teacherPos);
        }

        submitBtn.setEnabled(false);
        saveExamToFirebase(standard, selectedStreams, examTypeLabel, examTypeKey, examInfo, radioYes.isChecked(), selectedTeacherId, selectedTeacherName);
    }

    private void saveExamToFirebase(String standard, List<String> streams,
            String examTypeLabel, String examTypeKey,
            String examInfo, boolean shareWithTeachers, String teacherId, String teacherName) {
        String examId = examReference.push().getKey();
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
        studentData.put("examInfo", examInfo);
        studentData.put("examType", examTypeLabel); // "Mid Term" readable label
        studentData.put("image", base64Image != null ? base64Image : "");
        studentData.put("timestamp", timestamp);
        studentData.put("assignedTeacherId", teacherId);
        studentData.put("assignedTeacherName", teacherName);

        // Save student nodes independently
        for (String std : standards) {
            for (String stream : streamsList) {
                examReference.child(examTypeKey).child(std + "_" + stream).child(examId)
                        .setValue(studentData);
            }
        }

        // Save teacher data flat under Exam/TeacherExam/{id}
        if (shareWithTeachers) {
            Map<String, Object> teacherData = new HashMap<>();
            teacherData.put("examInfo", examInfo);
            teacherData.put("examType", examTypeLabel);
            teacherData.put("image", base64Image != null ? base64Image : "");
            teacherData.put("timestamp", timestamp);
            teacherData.put("standard", standard);
            teacherData.put("streams", streams.contains("All") ? "All" : String.join(", ", streams));
            teacherData.put("assignedTeacherId", teacherId);
            teacherData.put("assignedTeacherName", teacherName);
            teacherExamReference.child(examId)
                    .setValue(teacherData);
        }

        // Send email notifications to target students
        sendExamNotifications(standards, streamsList, examTypeLabel, examInfo);
        // Also send notification to teacher if assigned
        if (!teacherId.equals("none")) {
            sendTeacherExamNotification(teacherId, examTypeLabel, examInfo);
        }

        // Show success after short delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(PrincipalExamActivity.this,
                    "Exam uploaded successfully!", Toast.LENGTH_SHORT).show();
            clearForm();
            submitBtn.setEnabled(true);
            loadHistory(); // Refresh history after upload
        }, 1000);
    }

    private void sendTeacherExamNotification(String teacherId, String examType, String examInfo) {
        FirebaseDatabase.getInstance().getReference("Teachers").child(teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String email = snapshot.child("email").getValue(String.class);
                        if (email == null) email = snapshot.child("Email").getValue(String.class);
                        String name = snapshot.child("name").getValue(String.class);
                        if (name == null) name = snapshot.child("Name").getValue(String.class);

                        if (email != null && !email.isEmpty()) {
                            final String finalEmail = email;
                            final String finalName = name != null ? name : "Teacher";
                            new Thread(() -> {
                                try {
                                    String subject = "Assigned Exam Duty: " + examType;
                                    String body = "Dear " + finalName + ",\n\n" +
                                            "You have been assigned an exam duty for " + examType + ".\n\n" +
                                            (examInfo != null && !examInfo.isEmpty()
                                                    ? "Details: " + examInfo + "\n\n"
                                                    : "") +
                                            "Please check your SmartConnect app for details.\n\n" +
                                            "SmartConnect Team";
                                    GmailSender.sendMailWithSubject(finalEmail, subject, body);
                                } catch (Exception e) {}
                            }).start();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendExamNotifications(List<String> standards, List<String> streams,
            String examType, String examInfo) {
        // 1. Notify Students
        FirebaseDatabase.getInstance().getReference("Students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot student : snapshot.getChildren()) {
                            String st = student.child("Standard").getValue(String.class);
                            String sr = student.child("Stream").getValue(String.class);
                            if (st != null && sr != null &&
                                    standards.contains(st) && streams.contains(sr)) {
                                String email = student.child("email").getValue(String.class);
                                if (email == null)
                                    email = student.child("Email").getValue(String.class);
                                String name = student.child("name").getValue(String.class);
                                if (name == null)
                                    name = student.child("Name").getValue(String.class);
                                if (email != null && !email.isEmpty()) {
                                    sendEmail(email, name != null ? name : "Student", examType, examInfo, "Student");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        // 2. Notify Parents
        FirebaseDatabase.getInstance().getReference("Parents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot parent : snapshot.getChildren()) {
                            String st = parent.child("studentStandard").getValue(String.class);
                            String sr = parent.child("studentStream").getValue(String.class);
                            if (st != null && sr != null &&
                                    standards.contains(st) && streams.contains(sr)) {
                                String email = parent.child("email").getValue(String.class);
                                String name = parent.child("name").getValue(String.class);
                                if (email != null && !email.isEmpty()) {
                                    sendEmail(email, name != null ? name : "Parent", examType, examInfo, "Parent");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        // 3. Notify All Teachers (if shared)
        if (radioYes.isChecked()) {
            FirebaseDatabase.getInstance().getReference("Teachers")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot teacher : snapshot.getChildren()) {
                                String email = teacher.child("email").getValue(String.class);
                                if (email == null)
                                    email = teacher.child("Email").getValue(String.class);
                                String name = teacher.child("name").getValue(String.class);
                                if (name == null)
                                    name = teacher.child("Name").getValue(String.class);
                                if (email != null && !email.isEmpty()) {
                                    sendEmail(email, name != null ? name : "Teacher", examType, examInfo, "Teacher");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }
    }

    private void sendEmail(String email, String name, String examType, String examInfo, String role) {
        new Thread(() -> {
            try {
                String subject = "New Exam Update: " + examType;
                String body = "Dear " + name + ",\n\n" +
                        "A new " + examType + " has been posted for your " + (role.equals("Parent") ? "child's " : "")
                        + "class.\n\n" +
                        (examInfo != null && !examInfo.isEmpty()
                                ? "Details: " + examInfo + "\n\n"
                                : "")
                        +
                        "Please check your SmartConnect app for details.\n\n" +
                        "SmartConnect Team";
                GmailSender.sendMailWithSubject(email, subject, body);
            } catch (Exception e) {
                // Silent fail for notifications
            }
        }).start();
    }

    private void clearForm() {
        standardRadioGroup.clearCheck();
        examTypeRadioGroup.clearCheck();
        checkScience.setChecked(false);
        checkCommerce.setChecked(false);
        checkArts.setChecked(false);
        checkAllStreams.setChecked(false);
        shareTeacherRadioGroup.clearCheck();
        teacherSpinner.setSelection(0);
        examInfoEdit.setText("");
        base64Image = null;
        previewImage.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
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
        if (id == R.id.principal_home)
            startActivity(new Intent(this, principaltab.class));
        else if (id == R.id.principal_timetable)
            startActivity(new Intent(this, principletimetable.class));
        else if (id == R.id.principal_exam) {
            /* current */ } else if (id == R.id.principal_notices)
            startActivity(new Intent(this, PrincipalNoticesActivity.class));
        else if (id == R.id.principal_calender)
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
