package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

public class ManageParentsActivity extends AppCompatActivity {

    private Spinner spinnerParentMode;
    private EditText etParentName, etParentEmail, etParentMobile, etStudentName;
    private Button btnCreateParent, btnDeleteParent;

    private DatabaseReference parentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_parents);

        spinnerParentMode = findViewById(R.id.spinnerParentMode);
        etParentName = findViewById(R.id.etParentName);
        etParentEmail = findViewById(R.id.etParentEmail);
        etParentMobile = findViewById(R.id.etParentMobile);
        etStudentName = findViewById(R.id.etStudentName);
        btnCreateParent = findViewById(R.id.btnCreateParent);
        btnDeleteParent = findViewById(R.id.btnDeleteParent);

        parentsRef = FirebaseDatabase.getInstance().getReference("Parents");

        setupParentModeSpinner();

        btnCreateParent.setOnClickListener(v -> createParent());
        btnDeleteParent.setOnClickListener(
                v -> startActivity(new Intent(ManageParentsActivity.this, DeleteParentActivity.class)));

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = {
                spinnerParentMode, etParentName, etParentEmail, etParentMobile, etStudentName,
                btnCreateParent, btnDeleteParent
        };

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

    private void setupParentModeSpinner() {
        String[] modes = { "Select Mode", "Father", "Mother" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                modes);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerParentMode.setAdapter(adapter);
    }

    private void createParent() {
        String mode = spinnerParentMode.getSelectedItem() != null
                ? spinnerParentMode.getSelectedItem().toString()
                : "";
        String parentName = etParentName.getText().toString().trim();
        String parentEmail = etParentEmail.getText().toString().trim();
        String parentMobile = etParentMobile.getText().toString().trim();
        String studentName = etStudentName.getText().toString().trim();

        if (TextUtils.isEmpty(mode) || "Select Mode".equals(mode)) {
            Toast.makeText(this, "Please select Parent Mode", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(parentName)) {
            etParentName.setError("Enter parent name");
            etParentName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(parentEmail)) {
            etParentEmail.setError("Enter parent email");
            etParentEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
            etParentEmail.setError("Enter valid email");
            etParentEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(parentMobile)) {
            etParentMobile.setError("Enter mobile number");
            etParentMobile.requestFocus();
            return;
        }
        if (parentMobile.length() != 10) {
            etParentMobile.setError("Mobile must be 10 digits");
            etParentMobile.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(studentName)) {
            etStudentName.setError("Enter student name");
            etStudentName.requestFocus();
            return;
        }

        // Look up the student in Firebase to get their UID, Standard, Stream
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String studentUID = "";
                String studentStandard = "";
                String studentStream = "";

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("Name").getValue(String.class);
                    if (name == null)
                        name = child.child("name").getValue(String.class);
                    if (name != null && name.equalsIgnoreCase(studentName)) {
                        studentUID = child.getKey();
                        String std = child.child("Standard").getValue(String.class);
                        String strm = child.child("Stream").getValue(String.class);
                        studentStandard = std != null ? std : "";
                        studentStream = strm != null ? strm : "";
                        break;
                    }
                }

                if (studentUID.isEmpty()) {
                    Toast.makeText(ManageParentsActivity.this,
                            "⚠ No student found with name \"" + studentName +
                                    "\". Parent will be created without student link.",
                            Toast.LENGTH_LONG).show();
                }

                saveParentToFirebase(mode, parentName, parentEmail, parentMobile,
                        studentName, studentUID, studentStandard, studentStream);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageParentsActivity.this,
                        "Error searching students: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveParentToFirebase(String mode, String parentName, String parentEmail,
            String parentMobile, String studentName,
            String studentUID, String studentStandard,
            String studentStream) {
        String key = parentsRef.push().getKey();
        if (key == null) {
            Toast.makeText(this, "Unable to create parent. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("id", key);
        map.put("mode", mode);
        map.put("name", parentName);
        map.put("email", parentEmail);
        map.put("mobile", parentMobile);
        map.put("studentName", studentName);
        map.put("studentUID", studentUID);
        map.put("studentStandard", studentStandard);
        map.put("studentStream", studentStream);

        parentsRef.child(key)
                .setValue(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ManageParentsActivity.this,
                                    "Parent created successfully.", Toast.LENGTH_SHORT).show();
                            sendParentEmail(parentEmail, parentName, parentMobile, mode, studentName);
                            clearForm();
                        } else {
                            Toast.makeText(ManageParentsActivity.this,
                                    "Failed to create parent.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void clearForm() {
        spinnerParentMode.setSelection(0);
        etParentName.setText("");
        etParentEmail.setText("");
        etParentMobile.setText("");
        etStudentName.setText("");
    }

    private void sendParentEmail(String email,
            String name,
            String mobile,
            String mode,
            String studentName) {
        new Thread(() -> {
            try {
                String subject = "SmartConnect Parent Account Created";
                String body = "Dear " + name + ",\n\n" +
                        "Your parent account has been created in SmartConnect as: " + mode + ".\n\n" +
                        "Registered Details:\n" +
                        "Name: " + name + "\n" +
                        "Email: " + email + "\n" +
                        "Mobile: " + mobile + "\n" +
                        "Student: " + studentName + "\n\n" +
                        "You can now log in using the 'Parent' option in the app and verify with OTP sent to this email.\n\n"
                        +
                        "Regards,\nSmartConnect Team";

                GmailSender.sendMailWithSubject(email, subject, body);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
