package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParentProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView changePhoto;
    private EditText editTextParentName, editTextParentEmail, editTextParentMobile, editTextStudentName, editTextStdStream;
    private Button btnLogout;
    private Toolbar toolbar;

    private DatabaseReference parentRef;
    private String parentKey;

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_profile);

        initializeViews();
        setupToolbar();
        applyAnimations();

        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        parentKey = parentPrefs.getString("parentKey", "");

        if (parentKey.isEmpty()) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentRef = FirebaseDatabase.getInstance().getReference("Parents").child(parentKey);

        loadParentProfile();

        profileImage.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            showImageDialog();
        });
        changePhoto.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            showImageDialog();
        });
        btnLogout.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> showLogoutDialog(), 150);
        });
    }

    private void applyAnimations() {
        UIAnimator.animateToolbar(toolbar, 100);
        UIAnimator.animateImageView(profileImage, 200);
        UIAnimator.animateTextView(changePhoto, 300);

        UIAnimator.animateEditText(editTextParentName, 400);
        UIAnimator.animateEditText(editTextParentEmail, 500);
        UIAnimator.animateEditText(editTextParentMobile, 600);
        UIAnimator.animateEditText(editTextStudentName, 700);
        UIAnimator.animateEditText(editTextStdStream, 800);
        UIAnimator.animateButton(btnLogout, 900);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImage = findViewById(R.id.profileImage);
        changePhoto = findViewById(R.id.changePhoto);
        editTextParentName = findViewById(R.id.editTextParentName);
        editTextParentEmail = findViewById(R.id.editTextParentEmail);
        editTextParentMobile = findViewById(R.id.editTextParentMobile);
        editTextStudentName = findViewById(R.id.editTextStudentName);
        editTextStdStream = findViewById(R.id.editTextStdStream);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Parent Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }
    }

    private void loadParentProfile() {
        parentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String mobile = snapshot.child("mobile").getValue(String.class);
                    String studentName = snapshot.child("studentName").getValue(String.class);
                    String std = snapshot.child("studentStandard").getValue(String.class);
                    String stream = snapshot.child("studentStream").getValue(String.class);
                    String base64Img = snapshot.child("profileImageBase64").getValue(String.class);

                    editTextParentName.setText(name != null ? name : "");
                    editTextParentEmail.setText(email != null ? email : "");
                    editTextParentMobile.setText(mobile != null ? mobile : "");
                    editTextStudentName.setText(studentName != null ? studentName : "");
                    editTextStdStream.setText((std != null ? std : "") + " " + (stream != null ? stream : ""));

                    if (base64Img != null && !base64Img.isEmpty()) {
                        byte[] imgBytes = Base64.decode(base64Img, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                        profileImage.setImageBitmap(bmp);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
                    parentPrefs.edit().clear().apply();
                    Intent intent = new Intent(ParentProfileActivity.this, loginchoice.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showImageDialog() {
        String[] options = {"Camera", "Gallery", "Remove Photo"};
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    } else if (which == 1) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryIntent.setType("image/*");
                        startActivityForResult(galleryIntent, GALLERY_REQUEST);
                    } else if (which == 2) {
                        removeProfileImage();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bitmap photo = null;
            if (requestCode == CAMERA_REQUEST) {
                photo = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == GALLERY_REQUEST) {
                Uri imageUri = data.getData();
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (photo != null) {
                uploadImageToDatabase(photo);
            }
        }
    }

    private void uploadImageToDatabase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        parentRef.child("profileImageBase64").setValue(base64String)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show());
    }

    private void removeProfileImage() {
        parentRef.child("profileImageBase64").removeValue()
                .addOnSuccessListener(aVoid -> {
                    profileImage.setImageResource(R.drawable.ic_default_profile);
                    Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show();
                });
    }
}
