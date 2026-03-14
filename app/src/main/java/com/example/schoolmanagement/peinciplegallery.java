package com.example.schoolmanagement;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class peinciplegallery extends AppCompatActivity {

    private static final String TAG = "PrincipleGallery";
    private static final int REQ_GALLERY = 1001;
    private static final int REQ_CAMERA = 1002;
    private static final int REQ_CAMERA_PERMISSION = 2001;
    private static final int REQ_STORAGE_PERMISSION = 2002;

    Button btnGallery, btnCamera, btnUpload, btnRemove;
    EditText etDescription;
    ImageView imgPreview;
    LinearLayout historyContainer;
    ProgressBar historyProgressBar;
    TextView tvNoHistory;

    Bitmap selectedBitmap = null;
    DatabaseReference galleryRef;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_peinciplegallery);

        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);
        btnUpload = findViewById(R.id.btnUpload);
        btnRemove = findViewById(R.id.btnRemove);
        etDescription = findViewById(R.id.etDescription);
        imgPreview = findViewById(R.id.imgPreview);
        historyContainer = findViewById(R.id.historyContainer);
        historyProgressBar = findViewById(R.id.historyProgressBar);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        try {
            galleryRef = FirebaseDatabase.getInstance().getReference("principlephotos");
        } catch (Exception e) {
            Toast.makeText(this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);

        btnGallery.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> checkStoragePermissionAndOpenGallery(), 150); });
        btnCamera.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> checkCameraPermission(), 150); });
        btnUpload.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> uploadImageToFirebase(), 150); });
        btnRemove.setOnClickListener(v -> { UIAnimator.animateClick(v); v.postDelayed(() -> removeSelectedImage(), 150); });
        imgPreview.setOnClickListener(v -> openFullScreenPreview());

        // Apply animations
        android.widget.ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 100);
        UIAnimator.animateEditText(etDescription, 200);
        UIAnimator.animateButton(btnGallery, 350);
        UIAnimator.animateButton(btnCamera, 500);
        UIAnimator.animateButton(btnUpload, 650);
        UIAnimator.animateImageView(imgPreview, 400);

        updateUIState();
        loadHistory();
    }

    // ==================== HISTORY ====================
    // Structure: principlephotos/{id} -> {image, description}

    private void loadHistory() {
        historyProgressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);
        historyContainer.removeAllViews();

        galleryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyProgressBar.setVisibility(View.GONE);
                historyContainer.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    tvNoHistory.setVisibility(View.VISIBLE);
                    return;
                }
                tvNoHistory.setVisibility(View.GONE);

                // Newest first — collect and reverse
                List<DataSnapshot> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    items.add(0, child);
                }

                for (DataSnapshot item : items) {
                    String id = item.getKey();
                    String image = item.child("image").getValue(String.class);
                    String description = item.child("description").getValue(String.class);
                    addHistoryCard(id, image, description);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                historyProgressBar.setVisibility(View.GONE);
                Toast.makeText(peinciplegallery.this, "Failed to load gallery", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addHistoryCard(String id, String image, String description) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_history_card, historyContainer, false);

        TextView tvStandard = cardView.findViewById(R.id.tvStandard);
        TextView tvExtraInfo = cardView.findViewById(R.id.tvExtraInfo);
        TextView tvInfo = cardView.findViewById(R.id.tvInfo);
        TextView tvTimestamp = cardView.findViewById(R.id.tvTimestamp);
        ImageView ivHistoryImage = cardView.findViewById(R.id.ivHistoryImage);
        ImageButton btnDelete = cardView.findViewById(R.id.btnDelete);

        // Gallery mein standard/stream nahi hota — description dikhao title mein
        if (description != null && !description.isEmpty()) {
            tvStandard.setText(description);
        } else {
            tvStandard.setText("No description");
        }

        // Hide unused fields
        tvExtraInfo.setVisibility(View.GONE);
        tvInfo.setVisibility(View.GONE);
        tvTimestamp.setVisibility(View.GONE);

        // Show image
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

        // Click on image to open full screen
        ivHistoryImage.setOnClickListener(v -> {
            if (image != null && !image.isEmpty()) {
                Intent intent = new Intent(this, principle_full_image.class);
                intent.putExtra("image_base64", image);
                startActivity(intent);
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteDialog(id, cardView));
        historyContainer.addView(cardView);
    }

    private void showDeleteDialog(String photoId, View cardView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo? It will be removed for everyone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePhoto(photoId, cardView))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePhoto(String photoId, View cardView) {
        galleryRef.child(photoId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        historyContainer.removeView(cardView);
                        if (historyContainer.getChildCount() == 0) {
                            tvNoHistory.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(peinciplegallery.this,
                                "Photo deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(peinciplegallery.this,
                                "Delete failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================== UPLOAD ====================

    private void checkStoragePermissionAndOpenGallery() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            openGallery();
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_STORAGE_PERMISSION);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQ_CAMERA);
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera();
            else
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openGallery();
            else
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        try {
            if (requestCode == REQ_GALLERY && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    if (bitmap != null) {
                        selectedBitmap = getResizedBitmap(bitmap, 1200);
                        imgPreview.setImageBitmap(selectedBitmap);
                        updateUIState();
                        Toast.makeText(this, "Image selected successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQ_CAMERA && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap != null) {
                        selectedBitmap = bitmap;
                        imgPreview.setImageBitmap(selectedBitmap);
                        updateUIState();
                        Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int)(width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int)(height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void uploadImageToFirebase() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (galleryRef == null) {
            Toast.makeText(this, "Firebase not initialized!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        btnUpload.setEnabled(false);

        try {
            String description = etDescription.getText().toString().trim();
            String base64Image = convertBitmapToBase64(selectedBitmap);
            String imageId = galleryRef.push().getKey();

            if (imageId == null) {
                Toast.makeText(this, "Error generating image ID", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                btnUpload.setEnabled(true);
                return;
            }

            PrinciplePhoto photo = new PrinciplePhoto(base64Image, description);

            galleryRef.child(imageId).setValue(photo)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(peinciplegallery.this,
                                "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        resetFormAfterUpload();
                        // History auto-refreshes via ValueEventListener
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        btnUpload.setEnabled(true);
                        Toast.makeText(peinciplegallery.this,
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            btnUpload.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void removeSelectedImage() {
        selectedBitmap = null;
        imgPreview.setImageDrawable(null);
        etDescription.setText("");
        updateUIState();
        Toast.makeText(this, "Image removed from preview", Toast.LENGTH_SHORT).show();
    }

    private void resetFormAfterUpload() {
        selectedBitmap = null;
        imgPreview.setImageDrawable(null);
        etDescription.setText("");
        updateUIState();
    }

    private void updateUIState() {
        if (selectedBitmap != null) {
            btnRemove.setVisibility(View.VISIBLE);
            btnUpload.setEnabled(true);
        } else {
            btnRemove.setVisibility(View.GONE);
            btnUpload.setEnabled(false);
        }
    }

    private void openFullScreenPreview() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "No image to preview", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String base64Image = convertBitmapToBase64(selectedBitmap);
            Intent intent = new Intent(this, principle_full_image.class);
            intent.putExtra("image_base64", base64Image);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening preview", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}