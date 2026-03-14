package com.example.schoolmanagement;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class teacherhomework extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "TeacherHomework";
    private static final int MAX_FILES = 4;
    private static final int REQ_CAMERA = 200;
    private static final int REQ_MEDIA_PERMISSION = 501;

    private EditText etTitle, etDescription, etTeacherName;
    private Spinner spinnerStandard, spinnerStream;
    private TextView tvFilesLabel, tvDueSummary;
    private Button btnGallery, btnPhoto, btnUpload, btnPickDate, btnPickTime;
    private RecyclerView rvAttachments;
    private ProgressDialog progressDialog;

    private final List<Uri> attachmentUris = new ArrayList<>();
    private AttachmentAdapter adapter;

    private String teacherName = "";
    private String selectedStandard = "";
    private String selectedStream = "";
    private String teacherId = "";
    private final Calendar dueCalendar = Calendar.getInstance();
    private long dueTimestamp = 0L;

    private DatabaseReference homeworkRef, teachersRef;

    private Uri cameraUri;

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherhomework);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Homework");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        if (drawerLayout != null && navigationView != null) {
            toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
            navigationView.setNavigationItemSelectedListener(this);
        }

        initializeViews();
        setupFirebase();
        loadTeacherName();
        setupSpinners();
        setupRecyclerView();
        setupPickerLaunchers();
        setupClickListeners();
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
        if (id == R.id.teacher_home) {
            startActivity(new Intent(this, teachertab.class));
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            if (drawerLayout != null) drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_timetable) {
            startActivity(new Intent(this, teachertimetable.class));
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_syllabus) {
            startActivity(new Intent(this, teachersyllabus.class));
        } else if (id == R.id.teacher_exam) {
            startActivity(new Intent(this, teacherexam.class));
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
        if (drawerLayout != null)
            drawerLayout.closeDrawers();
        return true;
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etTeacherName = findViewById(R.id.etTeacherName);
        spinnerStandard = findViewById(R.id.spinnerStandard);
        spinnerStream = findViewById(R.id.spinnerStream);
        tvFilesLabel = findViewById(R.id.tvFilesLabel);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        tvDueSummary = findViewById(R.id.tvDueSummary);
        btnGallery = findViewById(R.id.btnGallery);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnUpload = findViewById(R.id.btnUpload);
        rvAttachments = findViewById(R.id.rvAttachments);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Uploading homework...");
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        try {
            homeworkRef = FirebaseDatabase.getInstance().getReference("Homework");
            teachersRef = FirebaseDatabase.getInstance().getReference("Teachers");
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Firebase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadTeacherName() {
        String tId = getIntent().getStringExtra("teacherId");
        if (tId == null || tId.isEmpty()) {
            android.content.SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            tId = tPrefs.getString("teacherId", null);
        }
        teacherId = tId != null ? tId : "";
        if (teacherId.isEmpty()) {
            teacherName = "";
            return;
        }

        teachersRef.child(teacherId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.getKey() != null && child.getKey().startsWith("-")) {
                                name = child.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty())
                                    break;
                            }
                        }
                    }
                    if (name != null && !name.isEmpty()) {
                        teacherName = name;
                        etTeacherName.setText(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load teacher name", error.toException());
            }
        });
    }

    private void setupSpinners() {
        String[] standards = { "Select Standard", "11", "12" };
        ArrayAdapter<String> standardAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_dropdown_item, standards);
        standardAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStandard.setAdapter(standardAdapter);

        spinnerStandard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStandard = position > 0 ? standards[position] : "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStandard = "";
            }
        });

        String[] streams = { "Select Stream", "Science", "Commerce", "Arts" };
        ArrayAdapter<String> streamAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, streams);
        streamAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerStream.setAdapter(streamAdapter);

        spinnerStream.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStream = position > 0 ? streams[position] : "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStream = "";
            }
        });

        // Lock spinners to teacher's assigned standard and stream
        android.content.SharedPreferences tp = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String assignedStd = tp.getString("teacherStandard", "");
        String assignedStream = tp.getString("teacherStream", "");

        if (assignedStd != null && !assignedStd.isEmpty()) {
            for (int i = 0; i < standards.length; i++) {
                if (standards[i].equals(assignedStd)) {
                    spinnerStandard.setSelection(i);
                    break;
                }
            }
            spinnerStandard.setEnabled(false);
            selectedStandard = assignedStd;
        }

        if (assignedStream != null && !assignedStream.isEmpty()) {
            for (int i = 0; i < streams.length; i++) {
                if (streams[i].equalsIgnoreCase(assignedStream)) {
                    spinnerStream.setSelection(i);
                    break;
                }
            }
            spinnerStream.setEnabled(false);
            selectedStream = assignedStream;
        }
    }

    private void setupRecyclerView() {
        adapter = new AttachmentAdapter(attachmentUris, new AttachmentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (position < 0 || position >= attachmentUris.size())
                    return;

                Uri uri = attachmentUris.get(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, getContentResolver().getType(uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(teacherhomework.this,
                            "Cannot open file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRemoveClick(int position) {
                if (position < 0 || position >= attachmentUris.size())
                    return;

                attachmentUris.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, attachmentUris.size());
                updateFilesVisibility();
                Toast.makeText(teacherhomework.this,
                        "Image removed", Toast.LENGTH_SHORT).show();
            }
        });

        rvAttachments.setLayoutManager(new LinearLayoutManager(this));
        rvAttachments.setAdapter(adapter);
    }

    private void setupPickerLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty())
                        return;

                    int addedCount = 0;
                    for (Uri uri : uris) {
                        if (attachmentUris.size() >= MAX_FILES) {
                            Toast.makeText(this,
                                    "Maximum 4 images allowed", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not take persistable permission", e);
                            }
                        }

                        attachmentUris.add(uri);
                        addedCount++;
                        Log.d(TAG, "Image added: " + uri.toString());
                    }

                    adapter.notifyDataSetChanged();
                    updateFilesVisibility();

                    if (addedCount > 0) {
                        Toast.makeText(this,
                                addedCount + " image(s) added", Toast.LENGTH_SHORT).show();
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && cameraUri != null) {
                        if (attachmentUris.size() < MAX_FILES) {
                            attachmentUris.add(cameraUri);
                            adapter.notifyDataSetChanged();
                            updateFilesVisibility();
                            Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "Maximum 4 images allowed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupClickListeners() {
        btnGallery.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::pickFromGallery, 150);
        });
        btnPhoto.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::pickFromCamera, 150);
        });
        btnUpload.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::uploadHomework, 150);
        });
        btnPickDate.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showDatePicker, 150);
        });
        btnPickTime.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showTimePicker, 150);
        });
        Button btnManage = findViewById(R.id.btnManageSubmissions);
        btnManage.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, TeacherHomeworkSubmissionsActivity.class)), 150);
        });
    }

    private void showDatePicker() {
        final Calendar now = Calendar.getInstance();
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    dueCalendar.set(Calendar.YEAR, year);
                    dueCalendar.set(Calendar.MONTH, month);
                    dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDueSummary();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void showTimePicker() {
        final Calendar now = Calendar.getInstance();
        android.app.TimePickerDialog tp = new android.app.TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    dueCalendar.set(Calendar.MINUTE, minute);
                    dueCalendar.set(Calendar.SECOND, 0);
                    dueCalendar.set(Calendar.MILLISECOND, 0);
                    updateDueSummary();
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false);
        tp.show();
    }

    private void updateDueSummary() {
        dueTimestamp = dueCalendar.getTimeInMillis();
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String txt = "Due: " + df.format(new Date(dueTimestamp));
        tvDueSummary.setText(txt);
        tvDueSummary.setVisibility(View.VISIBLE);
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
        if (attachmentUris.size() >= MAX_FILES) {
            Toast.makeText(this,
                    "Maximum 4 images already selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasMediaPermission()) {
            requestMediaPermission();
            return;
        }

        filePickerLauncher.launch(new String[] { "image/*" });
    }

    private void pickFromCamera() {
        if (attachmentUris.size() >= MAX_FILES) {
            Toast.makeText(this,
                    "Maximum 4 images already selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.CAMERA },
                    REQ_CAMERA);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Homework_Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Homework Photo");

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickFromCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_MEDIA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickFromGallery();
            } else {
                Toast.makeText(this,
                        "Storage permission needed to pick images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFilesVisibility() {
        if (attachmentUris.isEmpty()) {
            tvFilesLabel.setVisibility(View.GONE);
            rvAttachments.setVisibility(View.GONE);
        } else {
            tvFilesLabel.setVisibility(View.VISIBLE);
            rvAttachments.setVisibility(View.VISIBLE);
            tvFilesLabel.setText("Selected Images: (" + attachmentUris.size() + "/" + MAX_FILES + ")");
        }
    }

    private void uploadHomework() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String tNameInput = etTeacherName.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Subject name required");
            etTitle.requestFocus();
            return;
        }

        if (selectedStandard.isEmpty()) {
            Toast.makeText(this, "Please select a standard", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedStream.isEmpty()) {
            Toast.makeText(this, "Please select a stream", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tNameInput.isEmpty() && (teacherName == null || teacherName.isEmpty())) {
            etTeacherName.setError("Teacher name required");
            etTeacherName.requestFocus();
            return;
        }

        if (!tNameInput.isEmpty()) {
            teacherName = tNameInput;
        }

        boolean hasText = !description.isEmpty();
        boolean hasImages = !attachmentUris.isEmpty();

        if (!hasText && !hasImages) {
            Toast.makeText(this,
                    "Add homework instructions or at least one image",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueTimestamp <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please set a valid future submission deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "=== STARTING UPLOAD ===");
        btnUpload.setEnabled(false);
        btnUpload.setText("Uploading...");
        progressDialog.setMessage("Processing images...");
        progressDialog.show();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String uploadedDate = dateFormat.format(new Date());

        if (!hasImages) {
            saveHomeworkData(title, description, selectedStandard, selectedStream,
                    teacherName, uploadedDate, new ArrayList<>());
            return;
        }

        new Thread(() -> {
            List<String> base64Images = new ArrayList<>();

            for (int i = 0; i < attachmentUris.size(); i++) {
                Uri imageUri = attachmentUris.get(i);
                final int index = i + 1;

                runOnUiThread(
                        () -> progressDialog.setMessage("Processing image " + index + "/" + attachmentUris.size()));

                String base64 = uriToBase64(imageUri);
                if (base64 != null && !base64.isEmpty()) {
                    base64Images.add(base64);
                } else {
                    Log.e(TAG, "Failed to convert image " + index);
                }
            }

            final List<String> finalBase64Images = base64Images;

            runOnUiThread(() -> {
                if (attachmentUris.size() > 0 && finalBase64Images.isEmpty()) {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to process images", Toast.LENGTH_SHORT).show();
                    btnUpload.setEnabled(true);
                    btnUpload.setText("Upload Homework");
                    return;
                }

                progressDialog.setMessage("Uploading please wait");

                saveHomeworkData(title, description, selectedStandard, selectedStream,
                        teacherName, uploadedDate, finalBase64Images);
            });
        }).start();
    }

    private void saveHomeworkData(String title, String description,
            String standard, String stream, String teacher,
            String date, List<String> base64Images) {

        String categoryPath = standard + "_" + stream;

        String homeworkKey = homeworkRef.child(categoryPath).push().getKey();

        if (homeworkKey == null) {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Error generating homework ID", Toast.LENGTH_SHORT).show();
                btnUpload.setEnabled(true);
                btnUpload.setText("Upload Homework");
            });
            return;
        }

        Map<String, Object> homeworkData = new HashMap<>();
        homeworkData.put("title", title);
        homeworkData.put("description", description);
        homeworkData.put("standard", standard);
        homeworkData.put("stream", stream);
        homeworkData.put("teacherName", teacher);
        homeworkData.put("teacherId", teacherId);
        homeworkData.put("uploadedDate", date);
        homeworkData.put("imageCount", base64Images.size());
        homeworkData.put("dueTimestamp", dueTimestamp);
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        homeworkData.put("dueReadable", df.format(new Date(dueTimestamp)));

        DatabaseReference homeworkRefNode = homeworkRef.child(categoryPath).child(homeworkKey);

        homeworkRefNode.setValue(homeworkData)
                .addOnSuccessListener(unused -> {
                    if (!base64Images.isEmpty()) {
                        saveImagesOneByOne(homeworkRefNode, base64Images, 0);
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Homework Uploaded Successfully!", Toast.LENGTH_LONG).show();
                            clearFields();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnUpload.setEnabled(true);
                        btnUpload.setText("Upload Homework");
                    });
                });
    }

    private void saveImagesOneByOne(DatabaseReference homeworkRefNode, List<String> base64Images, int index) {
        if (index >= base64Images.size()) {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Homework Uploaded Successfully!", Toast.LENGTH_LONG).show();
                clearFields();
            });
            return;
        }

        final int currentIndex = index + 1;
        runOnUiThread(() -> progressDialog.setMessage("Uploading image " + currentIndex + "/" + base64Images.size()));

        String imageKey = "image" + currentIndex;
        String base64Image = base64Images.get(index);

        homeworkRefNode.child("images").child(imageKey).setValue(base64Image)
                .addOnSuccessListener(unused -> saveImagesOneByOne(homeworkRefNode, base64Images, index + 1))
                .addOnFailureListener(e -> saveImagesOneByOne(homeworkRefNode, base64Images, index + 1));
    }

    private void clearFields() {
        etTitle.setText("");
        etDescription.setText("");
        etTeacherName.setText("");
        spinnerStandard.setSelection(0);
        spinnerStream.setSelection(0);
        attachmentUris.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateFilesVisibility();
        tvDueSummary.setVisibility(View.GONE);
        dueTimestamp = 0L;
        btnUpload.setEnabled(true);
        btnUpload.setText("Upload Homework");
    }

    private String uriToBase64(Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null)
                return null;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null)
                return null;

            int maxSize = 600;
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float ratio = Math.min(
                        (float) maxSize / bitmap.getWidth(),
                        (float) maxSize / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();

            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            baos.close();
            inputStream.close();

            return base64String;

        } catch (Exception e) {
            Log.e(TAG, "Error converting to Base64", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
