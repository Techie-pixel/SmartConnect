package com.example.schoolmanagement;

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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminProfileActivity extends AppCompatActivity {

    CircleImageView adminProfileImage;
    TextView adminChangePhoto;
    EditText adminName, adminEmail, adminMobile;
    Button adminLogout;

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        adminProfileImage = findViewById(R.id.adminProfileImage);
        adminChangePhoto = findViewById(R.id.adminChangePhoto);
        adminName = findViewById(R.id.adminName);
        adminEmail = findViewById(R.id.adminEmail);
        adminMobile = findViewById(R.id.adminMobile);
        adminLogout = findViewById(R.id.adminLogout);

        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        String email = prefs.getString("adminEmail", "");
        String name = prefs.getString("adminName", "Admin");
        String mobile = prefs.getString("adminMobile", "");
        String img64 = prefs.getString("adminProfileImageBase64", "");

        if (!prefs.contains("adminName")) {
            prefs.edit().putString("adminName", name).apply();
        }
        String desiredMobile = "9518906814";
        if (!desiredMobile.equals(mobile)) {
            prefs.edit().putString("adminMobile", desiredMobile).apply();
            mobile = desiredMobile;
        }

        adminName.setText("Name: " + name);
        adminEmail.setText("Email: " + email);
        adminMobile.setText("Mobile: " + mobile);

        if (img64 != null && !img64.isEmpty()) {
            try {
                byte[] imgBytes = android.util.Base64.decode(img64, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                adminProfileImage.setImageBitmap(bmp);
            } catch (Exception ignored) {
                adminProfileImage.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            adminProfileImage.setImageResource(R.drawable.ic_default_profile);
        }

        adminProfileImage.setOnClickListener(v -> showImageDialog());
        adminChangePhoto.setOnClickListener(v -> showImageDialog());

        adminLogout.setOnClickListener(v -> showLogoutDialog());

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = { adminProfileImage, adminChangePhoto, adminName, adminEmail, adminMobile, adminLogout };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                views[i].startAnimation(anim);
            }
        }
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
                adminProfileImage.setImageBitmap(photo);
                saveImageToPrefs(photo);
            }
        }
    }

    private void saveImageToPrefs(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        prefs.edit().putString("adminProfileImageBase64", base64String).apply();
    }

    private void deleteProfileImage() {
        adminProfileImage.setImageResource(R.drawable.ic_default_profile);
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        prefs.edit().remove("adminProfileImageBase64").apply();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
            String img64 = prefs.getString("adminProfileImageBase64", "");
            prefs.edit().clear().apply();
            if (img64 != null && !img64.isEmpty()) {
                prefs.edit().putString("adminProfileImageBase64", img64).apply();
            }
            Intent i = new Intent(this, loginchoice.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
