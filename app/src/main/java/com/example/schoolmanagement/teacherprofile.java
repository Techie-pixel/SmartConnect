package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class teacherprofile extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    CircleImageView profileImage;
    TextView changePhoto;
    EditText tName, tEmail, tId, tStream, tSubject;
    Button btnLogout;

    DatabaseReference teacherRef;
    String teacherId; // jo bhi teacher login hai uski id
    String teacherSubject; // which subject the teacher logged in with

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherprofile);

        profileImage = findViewById(R.id.teacherProfileImage);
        changePhoto = findViewById(R.id.teacherChangePhoto);
        tName = findViewById(R.id.tName);
        tEmail = findViewById(R.id.tEmail);
        tId = findViewById(R.id.tId);
        tStream = findViewById(R.id.tStream);
        tSubject = findViewById(R.id.tSubject);
        btnLogout = findViewById(R.id.btnTeacherLogout);

        // teachertab se aayi hui id
        teacherId = getIntent().getStringExtra("teacherId");
        if (teacherId == null || teacherId.isEmpty()) {
            Toast.makeText(this, "Teacher ID missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Read which subject the teacher logged in with
        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        teacherSubject = tPrefs.getString("teacherSubject", "");

        teacherRef = FirebaseDatabase.getInstance()
                .getReference("Teachers")
                .child(teacherId); // hamesha current teacher wali node [web:11][web:14]

        try {
            teacherRef.keepSynced(true);
        } catch (Exception ignored) {
        }

        // Cache is scoped per teacher to avoid showing old teacher's data
        SharedPreferences cache = getSharedPreferences("TeacherCache_" + teacherId, MODE_PRIVATE);
        if (cache.contains("name") || cache.contains("email") ||
                cache.contains("stream") || cache.contains("subject")) {
            tName.setText("Name: " + cache.getString("name", ""));
            tEmail.setText("Email: " + cache.getString("email", ""));
            tStream.setText("Stream: " + cache.getString("stream", ""));
            tSubject.setText("Subject: " + cache.getString("subject", ""));
        }
        tId.setText("ID: " + teacherId);

        loadTeacherProfile();

        profileImage.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showImageDialog, 150);
        });
        changePhoto.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showImageDialog, 150);
        });

        btnLogout.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showLogoutDialog, 150);
        });
    }

    private void loadTeacherProfile() {
        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(teacherprofile.this,
                            "Profile not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Find the correct push-key child matching the logged-in subject
                DataSnapshot dataSnap = snapshot;
                String name = snapshot.child("name").getValue(String.class);
                if (name == null) {
                    DataSnapshot firstPushChild = null;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey() != null && child.getKey().startsWith("-")) {
                            if (firstPushChild == null) {
                                firstPushChild = child; // remember first as fallback
                            }
                            // Match by subject selected at login
                            String childSubject = child.child("subject").getValue(String.class);
                            if (teacherSubject != null && !teacherSubject.isEmpty()
                                    && teacherSubject.equals(childSubject)) {
                                dataSnap = child;
                                firstPushChild = null; // no need for fallback
                                break;
                            }
                        }
                    }
                    // If no exact match found, fall back to first push child
                    if (firstPushChild != null) {
                        dataSnap = firstPushChild;
                    }
                }

                name = dataSnap.child("name").getValue(String.class);
                String email = dataSnap.child("email").getValue(String.class);
                String stream = dataSnap.child("stream").getValue(String.class);
                String subject = dataSnap.child("subject").getValue(String.class);
                String base64Img = snapshot.child("ProfileImageBase64").getValue(String.class);
                if (base64Img == null) {
                    base64Img = dataSnap.child("ProfileImageBase64").getValue(String.class);
                }

                // ALWAYS apply latest from DB for current teacherId
                tName.setText("Name: " + (name != null ? name : ""));
                tEmail.setText("Email: " + (email != null ? email : ""));
                tId.setText("ID: " + teacherId);
                tStream.setText("Stream: " + (stream != null ? stream : ""));
                tSubject.setText("Subject: " + (subject != null ? subject : ""));

                if (base64Img != null && !base64Img.isEmpty()) {
                    byte[] imgBytes = android.util.Base64.decode(
                            base64Img, android.util.Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    profileImage.setImageBitmap(bmp);
                } else {
                    profileImage.setImageResource(R.drawable.ic_default_profile);
                }

                // Update cache for faster next open
                SharedPreferences cache = getSharedPreferences("TeacherCache_" + teacherId, MODE_PRIVATE);
                cache.edit()
                        .putString("name", name != null ? name : "")
                        .putString("email", email != null ? email : "")
                        .putString("stream", stream != null ? stream : "")
                        .putString("subject", subject != null ? subject : "")
                        .apply();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(teacherprofile.this,
                        "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Removed conditional setters: we now always show latest for current teacher

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Yes", (dialog, which) -> {

            SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Toast.makeText(teacherprofile.this,
                    "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(teacherprofile.this, loginchoice.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showImageDialog() {
        String[] options = { "Camera", "Gallery", "Delete" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            } else if (which == 2) {
                deleteProfileImage();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap photo = null;
            if (requestCode == CAMERA_REQUEST) {
                photo = (Bitmap) data.getExtras().get("data");
            }
            if (requestCode == GALLERY_REQUEST) {
                Uri imageUri = data.getData();
                try {
                    photo = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (photo != null) {
                profileImage.setImageBitmap(photo);
                uploadImageToDatabase(photo);
            }
        }
    }

    private void uploadImageToDatabase(Bitmap bitmap) {
        if (teacherId == null || teacherId.isEmpty())
            return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = android.util.Base64.encodeToString(
                imageBytes, android.util.Base64.DEFAULT);

        teacherRef.child("ProfileImageBase64").setValue(base64String)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile image updated!",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed",
                        Toast.LENGTH_SHORT).show());
    }

    private void deleteProfileImage() {
        if (teacherId == null || teacherId.isEmpty())
            return;

        // pehle check karo ki koi image stored hai ya nahi
        teacherRef.child("ProfileImageBase64")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists() || snapshot.getValue() == null) {
                            // kabhi image set hi nahi hui
                            Toast.makeText(teacherprofile.this,
                                    "No profile photo to delete",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // image hai, ab delete karo
                            teacherRef.child("ProfileImageBase64").removeValue();
                            profileImage.setImageResource(R.drawable.ic_default_profile);
                            Toast.makeText(teacherprofile.this,
                                    "Profile image removed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(teacherprofile.this,
                                "Failed to delete image",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
