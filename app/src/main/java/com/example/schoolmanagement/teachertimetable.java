package com.example.schoolmanagement;

import android.content.Intent;
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

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class teachertimetable extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private LinearLayout timetableContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private static final String[] STD_STREAM_KEYS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    private static class TimetableData {
        String id;
        String standard;
        String stream;
        String image;
        long timestamp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachertimetable);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Time Table");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        timetableContainer = findViewById(R.id.timetableContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        loadTimetables();
    }

    private void loadTimetables() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        timetableContainer.removeAllViews();

        DatabaseReference timetableRef = FirebaseDatabase.getInstance().getReference("Timetable");
        final int totalNodes = STD_STREAM_KEYS.length;
        final AtomicInteger loaded = new AtomicInteger(0);
        final Map<String, TimetableData> allEntries = new HashMap<>();

        for (String stdStream : STD_STREAM_KEYS) {
            timetableRef.child(stdStream).child("TeacherTimetable")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (!allEntries.containsKey(child.getKey())) {
                                    TimetableData td = new TimetableData();
                                    td.id = child.getKey();
                                    td.standard = child.child("standard").getValue(String.class);
                                    td.stream = child.child("stream").getValue(String.class);
                                    td.image = child.child("image").getValue(String.class);
                                    Long ts = child.child("timestamp").getValue(Long.class);
                                    td.timestamp = ts != null ? ts : 0;
                                    allEntries.put(td.id, td);
                                }
                            }

                            if (loaded.incrementAndGet() >= totalNodes) {
                                progressBar.setVisibility(View.GONE);
                                List<TimetableData> list = new ArrayList<>(allEntries.values());
                                list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                                renderTimetables(list);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (loaded.incrementAndGet() >= totalNodes) {
                                progressBar.setVisibility(View.GONE);
                                List<TimetableData> list = new ArrayList<>(allEntries.values());
                                list.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                                renderTimetables(list);
                            }
                        }
                    });
        }
    }

    private void renderTimetables(List<TimetableData> timetables) {
        timetableContainer.removeAllViews();

        if (timetables.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (TimetableData td : timetables) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_timetable_card, timetableContainer, false);

            TextView tvLabel = card.findViewById(R.id.tvTimetableLabel);
            TextView tvDate = card.findViewById(R.id.tvTimetableDate);
            ImageView ivImage = card.findViewById(R.id.ivTimetableImage);
            // Hide mark-as-read for teachers
            LinearLayout llReadAction = card.findViewById(R.id.llReadAction);
            Button btnMarkRead = card.findViewById(R.id.btnMarkRead);
            if (btnMarkRead != null)
                btnMarkRead.setVisibility(View.GONE);
            TextView tvUnreadBadge = card.findViewById(R.id.tvUnreadBadge);
            if (tvUnreadBadge != null)
                tvUnreadBadge.setVisibility(View.GONE);

            String label = "📋 Timetable";
            if (td.standard != null && !td.standard.isEmpty()) {
                label += " • Std " + td.standard;
            }
            if (td.stream != null && !td.stream.isEmpty()) {
                label += " " + td.stream;
            }
            tvLabel.setText(label);
            tvDate.setText(sdf.format(new Date(td.timestamp)));

            if (td.image != null && !td.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(td.image, Base64.DEFAULT);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivImage.setImageBitmap(bmp);
                        ivImage.setVisibility(View.VISIBLE);

                        ivImage.setOnClickListener(v -> {
                            UIAnimator.animateClick(v);
                            v.postDelayed(() -> {
                                Intent intent = new Intent(teachertimetable.this, ImagePreviewActivity.class);
                                intent.putExtra("image_base64", td.image);
                                startActivity(intent);
                            }, 150);
                        });
                    }
                } catch (Exception e) {
                    ivImage.setVisibility(View.GONE);
                }
            }

            Button btnDownloadImage = card.findViewById(R.id.btnDownloadImage);
            if (btnDownloadImage != null) {
                btnDownloadImage.setOnClickListener(v -> {
                    UIAnimator.animateClick(v);
                    v.postDelayed(() -> {
                        if (td.image != null && !td.image.isEmpty()) {
                            byte[] decoded = Base64.decode(td.image, Base64.DEFAULT);
                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            if (bmp != null) {
                                saveImageToGallery(bmp, td.id);
                            } else {
                                Toast.makeText(teachertimetable.this, "Failed to decode image for download",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(teachertimetable.this, "No image to download", Toast.LENGTH_SHORT).show();
                        }
                    }, 150);
                });
            }

            timetableContainer.addView(card);
        }
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
        if (id == R.id.teacher_home) {
            startActivity(new Intent(this, teachertab.class));
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            startActivity(new Intent(this, teacherhomework.class));
        } else if (id == R.id.teacher_syllabus) {
            startActivity(new Intent(this, teachersyllabus.class));
        } else if (id == R.id.teacher_timetable) {
            drawerLayout.closeDrawers();
            return true;
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
}
