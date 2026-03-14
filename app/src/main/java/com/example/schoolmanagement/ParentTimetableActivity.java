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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ParentTimetableActivity extends AppCompatActivity {

    private LinearLayout timetableContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTitle;
    private ImageView logoImg;

    private String childStdStream;
    private String parentId;

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
        setContentView(R.layout.activity_parent_timetable);

        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        String std = parentPrefs.getString("studentStandard", "");
        String stream = parentPrefs.getString("studentStream", "");
        parentId = parentPrefs.getString("parentKey", "");
        childStdStream = std + "_" + stream;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Timetable - " + std + " " + stream);
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
        timetableContainer = findViewById(R.id.timetableContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        timetableRef = FirebaseDatabase.getInstance().getReference("Timetable").child(childStdStream);
        readsRef = FirebaseDatabase.getInstance().getReference("TimetableReads");

        applyAnimations(toolbar);
        loadTimetables();
    }

    private void applyAnimations(Toolbar toolbar) {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);
        if (tvTitle != null)
            UIAnimator.animateTextView(tvTitle, 300);
    }

    private void loadTimetables() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        timetableRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTimetables.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
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
                Toast.makeText(ParentTimetableActivity.this, "Error loading", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadStatus() {
        if (parentId == null || parentId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            renderTimetables();
            return;
        }

        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readTimetableIds.clear();
                for (TimetableData td : allTimetables) {
                    if (snapshot.child(td.id).child(parentId).exists()) {
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
                            Intent intent = new Intent(ParentTimetableActivity.this, ImagePreviewActivity.class);
                            intent.putExtra("image_base64", td.image);
                            startActivity(intent);
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
                            Toast.makeText(ParentTimetableActivity.this, "Failed to decode image for download",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ParentTimetableActivity.this, "No image to download", Toast.LENGTH_SHORT).show();
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
        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        readsRef.child(timetableId).child(parentId).setValue(true)
                .addOnSuccessListener(v -> Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark", Toast.LENGTH_SHORT).show());
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
