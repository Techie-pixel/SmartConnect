package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Image_detail extends AppCompatActivity {

    private ImageView fullImage;
    private TextView fullDescription;
    private LinearLayout descriptionLayout;
    private ImageButton closeButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        // Initialize views
        fullImage = findViewById(R.id.fullImage);
        fullDescription = findViewById(R.id.fullDescription);
        descriptionLayout = findViewById(R.id.descriptionLayout);
        closeButton = findViewById(R.id.closeButton);

        // Get data from intent
        String imageBase64 = getIntent().getStringExtra("image");
        String description = getIntent().getStringExtra("description");

        // Display image
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                fullImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Display description if available
        if (description != null && !description.trim().isEmpty()) {
            fullDescription.setText(description);
            descriptionLayout.setVisibility(View.VISIBLE);
        } else {
            descriptionLayout.setVisibility(View.GONE);
        }

        // Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Click anywhere on image to close
        fullImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}