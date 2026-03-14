package com.example.schoolmanagement;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ImagePreviewActivity extends AppCompatActivity {

    private Bitmap displayedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root layout: black background, image + download button
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Full-screen ImageView
        ImageView imgView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        imgView.setLayoutParams(imgParams);
        imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Try loading from base64 first (preferred — supports download)
        String base64 = getIntent().getStringExtra("image_base64");
        if (base64 != null && !base64.isEmpty()) {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            displayedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (displayedBitmap != null) {
                imgView.setImageBitmap(displayedBitmap);
            }
        } else {
            // Fallback: load from file path (legacy callers)
            String path = getIntent().getStringExtra("imagePath");
            if (path != null && !path.isEmpty()) {
                File imgFile = new File(path);
                if (imgFile.exists()) {
                    displayedBitmap = BitmapFactory.decodeFile(path);
                    imgView.setImageBitmap(displayedBitmap);
                }
            }
        }

        root.addView(imgView);

        // Download button at the bottom
        Button btnDownload = new Button(this);
        btnDownload.setText("⬇  Download Image");
        btnDownload.setTextColor(0xFFFFFFFF);
        btnDownload.setBackgroundColor(0xFF0288D1);
        btnDownload.setAllCaps(false);
        btnDownload.setTextSize(15);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(32, 16, 32, 32);
        btnDownload.setLayoutParams(btnParams);

        btnDownload.setOnClickListener(v -> {
            if (displayedBitmap != null) {
                saveImageToGallery(displayedBitmap);
            } else {
                Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show();
            }
        });

        root.addView(btnDownload);

        setContentView(root);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "SmartConnect_" + System.currentTimeMillis() + ".jpg";
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SmartConnect");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                } else {
                    throw new Exception("Failed to create media record");
                }
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File imageFile = new File(imagesDir, fileName);
                fos = new FileOutputStream(imageFile);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            Toast.makeText(this, "Image saved to Gallery!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
