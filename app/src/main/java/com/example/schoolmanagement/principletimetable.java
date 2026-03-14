package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

public class principletimetable extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private RadioGroup standardRadioGroup, shareTeacherRadioGroup;
    private RadioButton radio11, radio12, radioAll, radioYes, radioNo;
    private CheckBox checkScience, checkCommerce, checkArts, checkAllStreams;
    private Button cameraBtn, galleryBtn, submitBtn, removeImageBtn;
    private ImageView previewImage;
    private LinearLayout imagePreviewContainer;
    private LinearLayout historyContainer;
    private ProgressBar historyProgressBar;
    private TextView tvNoHistory;

    private DatabaseReference timetableReference, teacherTimetableReference;
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
        setContentView(R.layout.activity_principletimetable);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            setTitle("Time Table");
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

        shareTeacherRadioGroup = findViewById(R.id.shareTeacherRadioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);

        checkScience = findViewById(R.id.checkScience);
        checkCommerce = findViewById(R.id.checkCommerce);
        checkArts = findViewById(R.id.checkArts);
        checkAllStreams = findViewById(R.id.checkAllStreams);

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
        timetableReference = FirebaseDatabase.getInstance().getReference("Timetable");
        teacherTimetableReference = FirebaseDatabase.getInstance().getReference("Timetable").child("TeacherTimetable");
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
        submitBtn.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> submitTimetable(), 150); });

        // Apply animations
        UIAnimator.animateButton(cameraBtn, 200);
        UIAnimator.animateButton(galleryBtn, 350);
        UIAnimator.animateButton(submitBtn, 500);
    }

    // ==================== HISTORY ====================
    // Structure: Timetable/{std_stream}/{id} -> {timetableInfo, image, timestamp}

    private void loadHistory() {
        historyProgressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        historyContainer.removeAllViews();
        addedIds.clear();

        // All possible std_stream combinations
        String[] stdStreamKeys = {
                "11_Science", "11_Commerce", "11_Arts",
                "12_Science", "12_Commerce", "12_Arts"
        };

        final int totalNodes = stdStreamKeys.length;
        final AtomicInteger loadedNodes = new AtomicInteger(0);
        // Collect all entries then display
        // Map: id -> {stdStream, snapshot}
        final Map<String, DataSnapshot> allEntries = new HashMap<>();
        final Map<String, String> idToStdStream = new HashMap<>();

        for (String stdStream : stdStreamKeys) {
            timetableReference.child(stdStream)
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
                                // Skip the TeacherTimetable sub-node
                                if ("TeacherTimetable".equals(id))
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

            String timetableInfo = snap.child("timetableInfo").getValue(String.class);
            String image = snap.child("image").getValue(String.class);
            Long timestamp = snap.child("timestamp").getValue(Long.class);

            // Parse std and stream from key e.g. "11_Science"
            String std = "", stream = "";
            if (stdStream != null && stdStream.contains("_")) {
                std = stdStream.split("_")[0];
                stream = stdStream.split("_")[1];
            }

            addHistoryCard(id, std, stream, image, timestamp, stdStream);
        }
    }

    private void addHistoryCard(String id, String std, String stream,
            String image, Long timestamp, String stdStream) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_history_card, historyContainer, false);

        TextView tvStandard = cardView.findViewById(R.id.tvStandard);
        TextView tvExtraInfo = cardView.findViewById(R.id.tvExtraInfo);
        TextView tvInfo = cardView.findViewById(R.id.tvInfo);
        TextView tvTimestamp = cardView.findViewById(R.id.tvTimestamp);
        ImageView ivHistoryImage = cardView.findViewById(R.id.ivHistoryImage);
        ImageButton btnDelete = cardView.findViewById(R.id.btnDelete);

        tvStandard.setText("Standard: " + std + "  |  Stream: " + stream);
        tvExtraInfo.setVisibility(View.GONE); // No exam type for timetable
        tvInfo.setVisibility(View.GONE); // No text info for timetable

        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(timestamp)));
        }

        if (image != null && !image.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(image, Base64.DEFAULT);
                final Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bmp != null) {
                    ivHistoryImage.setImageBitmap(bmp);
                    ivHistoryImage.setVisibility(View.VISIBLE);

                    ivHistoryImage.setOnClickListener(v -> {
                        Intent intent = new Intent(principletimetable.this, ImagePreviewActivity.class);
                        intent.putExtra("image_base64", image);
                        startActivity(intent);
                    });
                }
            } catch (Exception e) {
                ivHistoryImage.setVisibility(View.GONE);
            }
        }

        btnDelete.setOnClickListener(v -> showDeleteDialog(id, stdStream, cardView));

        // Wire up Seen / Unseen buttons
        Button btnSeen = cardView.findViewById(R.id.btnSeen);
        Button btnUnseen = cardView.findViewById(R.id.btnUnseen);
        fetchTimetableSeenUnseenCounts(id, stdStream, btnSeen, btnUnseen);

        historyContainer.addView(cardView);
    }

    private void showDeleteDialog(String timetableId, String stdStream, View cardView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Timetable")
                .setMessage("Are you sure you want to delete this timetable? It will be removed for everyone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTimetable(timetableId, stdStream, cardView))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTimetable(String timetableId, String stdStream, View cardView) {
        // Delete from ALL 6 std_stream nodes — because "All Streams" or multiple
        // streams upload the same ID to multiple paths. Firebase ignores missing paths
        // silently.
        String[] allKeys = { "11_Science", "11_Commerce", "11_Arts", "12_Science", "12_Commerce", "12_Arts" };
        for (String key : allKeys) {
            timetableReference.child(key).child(timetableId).removeValue();
        }
        // Also delete nested teacher copies under each std_stream
        for (String key : allKeys) {
            timetableReference.child(key).child("TeacherTimetable").child(timetableId).removeValue();
        }
        historyContainer.removeView(cardView);
        if (historyContainer.getChildCount() == 0) {
            tvNoHistory.setVisibility(View.VISIBLE);
        }
        Toast.makeText(principletimetable.this,
                "Timetable deleted successfully!", Toast.LENGTH_SHORT).show();
    }

    // ==================== SEEN / UNSEEN TRACKING ====================

    private void fetchTimetableSeenUnseenCounts(String timetableId, String stdStream, Button btnSeen,
            Button btnUnseen) {
        DatabaseReference readsRef = FirebaseDatabase.getInstance().getReference("TimetableReads").child(timetableId);
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot readsSnap) {
                Set<String> readUids = new HashSet<>();
                for (DataSnapshot child : readsSnap.getChildren()) {
                    readUids.add(child.getKey());
                }

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
                                btnSeen.setOnClickListener(
                                        v -> showSeenUnseenDialog("Seen", readUids, snap, stdStream));
                                btnUnseen.setOnClickListener(
                                        v -> showSeenUnseenDialog("Unseen", readUids, snap, stdStream));
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
        new android.app.AlertDialog.Builder(this)
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

    private void submitTimetable() {
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

        if (base64Image == null || base64Image.isEmpty()) {
            Toast.makeText(this, "Please upload a timetable image", Toast.LENGTH_SHORT).show();
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
        saveTimetableToFirebase(selectedStandard, selectedStreams, shareWithTeachers);
    }

    private void saveTimetableToFirebase(String standard, List<String> streams,
            boolean shareWithTeachers) {
        String timetableId = timetableReference.push().getKey();
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
        studentData.put("image", base64Image);
        studentData.put("timestamp", timestamp);

        // Save student nodes
        for (String std : standards) {
            for (String stream : streamsList) {
                timetableReference.child(std + "_" + stream).child(timetableId)
                        .setValue(studentData);
            }
        }

        // Save teacher data nested under Timetable/{std_stream}/TeacherTimetable/{id}
        if (shareWithTeachers) {
            for (String std : standards) {
                for (String stream : streamsList) {
                    Map<String, Object> teacherData = new HashMap<>();
                    teacherData.put("image", base64Image);
                    teacherData.put("timestamp", timestamp);
                    teacherData.put("standard", std);
                    teacherData.put("stream", stream);
                    timetableReference.child(std + "_" + stream)
                            .child("TeacherTimetable")
                            .child(timetableId)
                            .setValue(teacherData);
                }
            }
        }

        // Send email notifications to target students
        sendTimetableNotifications(standards, streamsList);

        // Show success after a short delay to allow Firebase writes to propagate
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(principletimetable.this,
                    "Timetable uploaded successfully!", Toast.LENGTH_SHORT).show();
            clearForm();
            submitBtn.setEnabled(true);
        }, 1000);
    }

    // Notifications are now handled by ChatNotificationService (in-app push)
    private void sendTimetableNotifications(List<String> standards, List<String> streams) {
        // No-op: ChatNotificationService listens to Timetable nodes and sends
        // in-app push notifications to students, teachers, and parents automatically.
    }

    private void checkCompletion(AtomicInteger completed, int total) {
        if (completed.incrementAndGet() == total) {
            runOnUiThread(() -> {
                Toast.makeText(principletimetable.this,
                        "Timetable uploaded successfully!", Toast.LENGTH_SHORT).show();
                clearForm();
                submitBtn.setEnabled(true);
            });
        }
    }

    private void clearForm() {
        standardRadioGroup.clearCheck();
        checkScience.setChecked(false);
        checkCommerce.setChecked(false);
        checkArts.setChecked(false);
        checkAllStreams.setChecked(false);
        shareTeacherRadioGroup.clearCheck();
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
        else if (id == R.id.principal_timetable) {
            /* current */ } else if (id == R.id.principal_exam)
            startActivity(new Intent(this, PrincipalExamActivity.class));
        else if (id == R.id.principal_notices)
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
