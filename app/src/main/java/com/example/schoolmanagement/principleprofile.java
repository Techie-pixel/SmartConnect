package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class principleprofile extends AppCompatActivity {

    private CircleImageView principalProfileImage;
    private TextView principalChangePhoto, principalEmail, principalId;

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principleprofile);

        principalProfileImage = findViewById(R.id.principalProfileImage);
        principalChangePhoto = findViewById(R.id.principalChangePhoto);
        principalEmail = findViewById(R.id.principalEmail);
        principalId = findViewById(R.id.principalId);

        SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        String email = prefs.getString("principalEmail", "principal@example.com");
        String pid = prefs.getString("principalId", "-");
        String img64 = prefs.getString("principalProfileImageBase64", "");

        principalEmail.setText("Email: " + email);
        principalId.setText("Principal ID: " + pid);

        if (img64 != null && !img64.isEmpty()) {
            try {
                byte[] imgBytes = android.util.Base64.decode(img64, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                principalProfileImage.setImageBitmap(bmp);
            } catch (Exception ignored) {
                principalProfileImage.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            principalProfileImage.setImageResource(R.drawable.ic_default_profile);
        }

        principalProfileImage.setOnClickListener(v -> showImageDialog());
        principalChangePhoto.setOnClickListener(v -> showImageDialog());

        Button logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> showLogoutDialog(), 150);
        });

        // Apply animations
        ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 100);
        UIAnimator.animateImageView(principalProfileImage, 250);
        UIAnimator.animateTextView(principalChangePhoto, 350);
        UIAnimator.animateTextView(principalEmail, 500);
        UIAnimator.animateTextView(principalId, 650);
        UIAnimator.animateButton(logoutBtn, 800);
    }

    private void showImageDialog() {
        String[] options = {"Camera", "Gallery", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else if (which == 1) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (photo != null) {
                principalProfileImage.setImageBitmap(photo);
                saveImageToPrefs(photo);
            }
        }
    }

    private void saveImageToPrefs(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
        SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        prefs.edit().putString("principalProfileImageBase64", base64String).apply();
    }

    private void deleteProfileImage() {
        principalProfileImage.setImageResource(R.drawable.ic_default_profile);
        SharedPreferences prefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        prefs.edit().remove("principalProfileImageBase64").apply();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences principalPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
                    principalPrefs.edit()
                            .putBoolean("isPrincipalLoggedIn", false)
                            .putString("principalId", null)
                            .apply();

                    Intent intent = new Intent(principleprofile.this, loginchoice.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
