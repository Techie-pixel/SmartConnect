package com.example.schoolmanagement;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class principleevents extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private RadioGroup standardRadioGroup, shareTeacherRadioGroup;
    private RadioButton radio11, radio12, radioAll, radioYes, radioNo;
    private CheckBox checkScience, checkCommerce, checkArts, checkAllStreams;
    private EditText eventInfoEdit;
    private Button cameraBtn, galleryBtn, submitBtn, removeImageBtn;
    private ImageView previewImage;
    private LinearLayout imagePreviewContainer;

    private DatabaseReference eventsReference, teacherEventsReference;
    private String base64Image = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principleevents);

        initializeViews();
        setupFirebase();
        setupListeners();
    }

    private void initializeViews() {
        standardRadioGroup = findViewById(R.id.standardRadioGroup);
        radio11 = findViewById(R.id.radio11);
        radio12 = findViewById(R.id.radio12);
        radioAll = findViewById(R.id.radioAll);

        shareTeacherRadioGroup = findViewById(R.id.shareTeacherRadioGroup);
        radioYes = findViewById(R.id.radioYes);
        radioNo = findViewById(R.id.radioNo);

        checkScience = findViewById(R.id.checkScience);
        checkCommerce = findViewById(R.id.checkCommerce);
        checkArts = findViewById(R.id.checkArts);
        checkAllStreams = findViewById(R.id.checkAllStreams);

        eventInfoEdit = findViewById(R.id.eventInfoEdit);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        submitBtn = findViewById(R.id.submitBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);
        previewImage = findViewById(R.id.previewImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
    }

    private void setupFirebase() {
        eventsReference = FirebaseDatabase.getInstance().getReference("Events");
        teacherEventsReference = FirebaseDatabase.getInstance().getReference("TeacherEvents");
    }

    private void setupListeners() {
        // All Streams checkbox logic
        checkAllStreams.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkScience.setChecked(false);
                    checkCommerce.setChecked(false);
                    checkArts.setChecked(false);
                    checkScience.setEnabled(false);
                    checkCommerce.setEnabled(false);
                    checkArts.setEnabled(false);
                } else {
                    checkScience.setEnabled(true);
                    checkCommerce.setEnabled(true);
                    checkArts.setEnabled(true);
                }
            }
        });

        // Disable All Streams when individual stream is selected
        CompoundButton.OnCheckedChangeListener individualStreamListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && checkAllStreams.isChecked()) {
                    checkAllStreams.setChecked(false);
                }
            }
        };

        checkScience.setOnCheckedChangeListener(individualStreamListener);
        checkCommerce.setOnCheckedChangeListener(individualStreamListener);
        checkArts.setOnCheckedChangeListener(individualStreamListener);

        // Camera button
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        // Gallery button
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Remove image button
        removeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImage();
            }
        });

        // Submit button
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitEvent();
            }
        });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    private void removeImage() {
        base64Image = null;
        previewImage.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap bitmap = null;

            try {
                if (requestCode == CAMERA_REQUEST) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST) {
                    Uri imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                }

                if (bitmap != null) {
                    // Compress bitmap to reduce size
                    Bitmap compressedBitmap = compressBitmap(bitmap, 800, 800);
                    base64Image = encodeImageToBase64(compressedBitmap);
                    previewImage.setImageBitmap(compressedBitmap);
                    imagePreviewContainer.setVisibility(View.VISIBLE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void submitEvent() {
        // Validate standard selection
        int selectedStandardId = standardRadioGroup.getCheckedRadioButtonId();
        if (selectedStandardId == -1) {
            Toast.makeText(this, "Please select a standard", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate stream selection
        if (!checkScience.isChecked() && !checkCommerce.isChecked() &&
                !checkArts.isChecked() && !checkAllStreams.isChecked()) {
            Toast.makeText(this, "Please select at least one stream", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate share with teachers selection
        int shareTeacherSelectedId = shareTeacherRadioGroup.getCheckedRadioButtonId();
        if (shareTeacherSelectedId == -1) {
            Toast.makeText(this, "Please select Yes or No for sharing with teachers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate that at least image or text is provided
        String eventInfo = eventInfoEdit.getText().toString().trim();
        if (eventInfo.isEmpty() && base64Image == null) {
            Toast.makeText(this, "Please enter event information or add an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected standard
        String selectedStandard = "";
        if (selectedStandardId == R.id.radio11) {
            selectedStandard = "11";
        } else if (selectedStandardId == R.id.radio12) {
            selectedStandard = "12";
        } else if (selectedStandardId == R.id.radioAll) {
            selectedStandard = "All";
        }

        // Get selected streams
        List<String> selectedStreams = new ArrayList<>();
        if (checkAllStreams.isChecked()) {
            selectedStreams.add("All");
        } else {
            if (checkScience.isChecked()) selectedStreams.add("Science");
            if (checkCommerce.isChecked()) selectedStreams.add("Commerce");
            if (checkArts.isChecked()) selectedStreams.add("Arts");
        }

        // Check if should share with teachers
        boolean shareWithTeachers = radioYes.isChecked();

        // Disable submit button to prevent multiple clicks
        submitBtn.setEnabled(false);

        // Save to Firebase
        saveEventToFirebase(selectedStandard, selectedStreams, eventInfo, shareWithTeachers);
    }

    private void saveEventToFirebase(String standard, List<String> streams, String eventInfo, boolean shareWithTeachers) {
        String eventId = eventsReference.push().getKey();

        // Calculate total operations
        List<String> standards = new ArrayList<>();
        if (standard.equals("All")) {
            standards.add("11");
            standards.add("12");
        } else {
            standards.add(standard);
        }

        List<String> streamsList = new ArrayList<>();
        if (streams.contains("All")) {
            streamsList.add("Science");
            streamsList.add("Commerce");
            streamsList.add("Arts");
        } else {
            streamsList.addAll(streams);
        }

        // Student event data
        Map<String, Object> studentEventData = new HashMap<>();
        studentEventData.put("eventInfo", eventInfo);
        studentEventData.put("image", base64Image != null ? base64Image : "");
        studentEventData.put("timestamp", System.currentTimeMillis());

        // Teacher event data with additional info
        Map<String, Object> teacherEventData = new HashMap<>();
        teacherEventData.put("eventInfo", eventInfo);
        teacherEventData.put("image", base64Image != null ? base64Image : "");
        teacherEventData.put("timestamp", System.currentTimeMillis());
        teacherEventData.put("standard", standard);
        teacherEventData.put("streams", streamsList);

        // Calculate total operations (Events + TeacherEvents if sharing)
        int eventsOperations = standards.size() * streamsList.size();
        int teacherOperations = shareWithTeachers ? 1 : 0; // Only 1 operation for teacher
        final int totalOperations = eventsOperations + teacherOperations;
        final AtomicInteger completedOperations = new AtomicInteger(0);

        // Save to Events (for students) - with standard_stream structure
        for (String std : standards) {
            for (String stream : streamsList) {
                eventsReference.child(std + "_" + stream).child(eventId).setValue(studentEventData)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                checkCompletion(completedOperations, totalOperations);
                            }
                        });
            }
        }

        // Save to TeacherEvents if Yes is selected - single entry with all info
        if (shareWithTeachers) {
            teacherEventsReference.child(eventId).setValue(teacherEventData)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            checkCompletion(completedOperations, totalOperations);
                        }
                    });
        }
    }

    private void checkCompletion(AtomicInteger completedOperations, int totalOperations) {
        if (completedOperations.incrementAndGet() == totalOperations) {
            // All operations completed
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(principleevents.this,
                            "Event submitted successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    submitBtn.setEnabled(true);
                }
            });
        }
    }

    private void clearForm() {
        standardRadioGroup.clearCheck();
        checkScience.setChecked(false);
        checkCommerce.setChecked(false);
        checkArts.setChecked(false);
        checkAllStreams.setChecked(false);
        shareTeacherRadioGroup.clearCheck();
        eventInfoEdit.setText("");
        base64Image = null;
        previewImage.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}