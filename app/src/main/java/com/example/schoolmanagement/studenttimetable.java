package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
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

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class studenttimetable extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private LinearLayout timetableContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private String studentStdStream;
    private String studentUserId;

    private final List<TimetableData> allTimetables = new ArrayList<>();
    private final Set<String> readTimetableIds = new HashSet<>();
    private DatabaseReference timetableRef, readsRef;

    private static class TimetableData {
        String id;
        String image;
        long timestamp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studenttimetable);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String std = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        studentUserId = prefs.getString("studentId", "");
        studentStdStream = std + "_" + stream;

        initializeViews();
        setupToolbar(std, stream);
        setupNavigationDrawer();
        setupUserProfile();

        timetableRef = FirebaseDatabase.getInstance().getReference("Timetable").child(studentStdStream);
        readsRef = FirebaseDatabase.getInstance().getReference("TimetableReads");

        loadTimetables();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Animate toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            UIAnimator.animateToolbar(toolbarView, 200);
        }
        
        // Animate content after delay
        mainContent.postDelayed(() -> {
            if (progressBar != null) {
                UIAnimator.animateImageView(progressBar, 500);
            }
        }, 400);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        timetableContainer = findViewById(R.id.timetableContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupToolbar(String std, String stream) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Timetable - " + std + " " + stream);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupNavigationDrawer() {
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if(toggle.getDrawerArrowDrawable() != null) {
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        }
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView usernameText = headerView.findViewById(R.id.username);
            if (usernameText != null) {
                android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                String studentName = prefs.getString("studentName", "");
                usernameText.setText("Welcome, " + studentName);
            }
        }
    }

    private void loadTimetables() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        timetableRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTimetables.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    // Skip the TeacherTimetable sub-node
                    if ("TeacherTimetable".equals(child.getKey()))
                        continue;

                    TimetableData td = new TimetableData();
                    td.id = child.getKey();
                    td.image = child.child("image").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    td.timestamp = ts != null ? ts : 0;
                    allTimetables.add(td);
                }
                allTimetables.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                loadReadStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(studenttimetable.this, "Error loading timetables", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadStatus() {
        if (studentUserId == null || studentUserId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            renderTimetables();
            return;
        }

        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readTimetableIds.clear();
                for (TimetableData td : allTimetables) {
                    if (snapshot.child(td.id).child(studentUserId).exists()) {
                        readTimetableIds.add(td.id);
                    }
                }
                progressBar.setVisibility(View.GONE);
                renderTimetables();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                renderTimetables();
            }
        });
    }

    private void renderTimetables() {
        timetableContainer.removeAllViews();

        if (allTimetables.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (TimetableData td : allTimetables) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_timetable_card, timetableContainer, false);

            TextView tvDate = card.findViewById(R.id.tvTimetableDate);
            ImageView ivImage = card.findViewById(R.id.ivTimetableImage);
            TextView tvUnreadBadge = card.findViewById(R.id.tvUnreadBadge);
            Button btnMarkRead = card.findViewById(R.id.btnMarkRead);

            tvDate.setText(sdf.format(new Date(td.timestamp)));

            if (td.image != null && !td.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(td.image, Base64.DEFAULT);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivImage.setImageBitmap(bmp);
                        ivImage.setVisibility(View.VISIBLE);

                        ivImage.setOnClickListener(v -> {
                            Intent intent = new Intent(studenttimetable.this, ImagePreviewActivity.class);
                            intent.putExtra("image_base64", td.image);
                            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
                        });
                    }
                } catch (Exception e) {
                    ivImage.setVisibility(View.GONE);
                }
            }

            Button btnDownloadImage = card.findViewById(R.id.btnDownloadImage);
            if (btnDownloadImage != null) {
                btnDownloadImage.setOnClickListener(v -> {
                    if (td.image != null && !td.image.isEmpty()) {
                        byte[] decoded = Base64.decode(td.image, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bmp != null) {
                            saveImageToGallery(bmp, td.id);
                        } else {
                            Toast.makeText(studenttimetable.this, "Failed to decode image for download",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(studenttimetable.this, "No image to download", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            boolean isRead = readTimetableIds.contains(td.id);
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
                final String timetableId = td.id;
                btnMarkRead.setOnClickListener(v -> markAsRead(timetableId));
            }

            timetableContainer.addView(card);
        }
    }

    private void saveImageToGallery(Bitmap bitmap, String timetableId) {
        String fileName = "Timetable_" + timetableId + ".jpg";
        try {
            OutputStream fos;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentResolver resolver = getContentResolver();
                android.content.ContentValues contentValues = new android.content.ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/SmartConnect");
                android.net.Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                } else {
                    throw new IOException("Failed to create new MediaStore record.");
                }
            } else {
                File imagesDir = android.os.Environment
                        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                File imageFile = new File(imagesDir, fileName);
                fos = new FileOutputStream(imageFile);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            Toast.makeText(this, "Timetable saved to Downloads/Gallery", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsRead(String timetableId) {
        if (studentUserId == null || studentUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        readsRef.child(timetableId).child(studentUserId).setValue(true)
                .addOnSuccessListener(v -> Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark", Toast.LENGTH_SHORT).show());
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
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}