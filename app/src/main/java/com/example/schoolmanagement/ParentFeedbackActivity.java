package com.example.schoolmanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ParentFeedbackActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editName, editEmail, editContact, textfeedback;
    private Button btnSubmit;
    private TextView tvFeedbackTitle;
    private ImageView logoImg;
    private DatabaseReference feedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_feedback);

        initializeViews();
        setupToolbar();
        applyAnimations();

        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback");

        // Pre-fill fields from SharedPreferences
        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        editName.setText(parentPrefs.getString("parentName", ""));
        editEmail.setText(parentPrefs.getString("parentEmail", ""));
        editContact.setText(parentPrefs.getString("parentMobile", ""));

        btnSubmit.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            submitFeedback();
        });
    }

    private void applyAnimations() {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);
        if (tvFeedbackTitle != null)
            UIAnimator.animateTextView(tvFeedbackTitle, 300);

        UIAnimator.animateEditText(editName, 400);
        UIAnimator.animateEditText(editEmail, 500);
        UIAnimator.animateEditText(editContact, 600);
        UIAnimator.animateEditText(textfeedback, 700);
        UIAnimator.animateButton(btnSubmit, 800);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        logoImg = findViewById(R.id.logoImg);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackTitle);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editContact = findViewById(R.id.editContact);
        textfeedback = findViewById(R.id.textfeedback);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Parent Feedback");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void submitFeedback() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String contact = editContact.getText().toString().trim();
        String feedback = textfeedback.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || contact.isEmpty() || feedback.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, String> feedbackData = new HashMap<>();
        feedbackData.put("Name", name);
        feedbackData.put("Email", email);
        feedbackData.put("Contact", contact);
        feedbackData.put("Feedback", feedback);
        feedbackData.put("Role", "Parent"); // Identifying the role

        feedbackRef.push().setValue(feedbackData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                    textfeedback.setText(""); // Only clear feedback text, keep personal info
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
