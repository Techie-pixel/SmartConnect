package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ParentLoginActivity extends AppCompatActivity {

    private EditText parentName, parentEmail, parentMobile;
    private Button parentLoginBtn;
    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_login);

        parentName = findViewById(R.id.parentName);
        parentEmail = findViewById(R.id.parentEmail);
        parentMobile = findViewById(R.id.parentMobile);
        parentLoginBtn = findViewById(R.id.parentLoginBtn);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainLayout.startAnimation(slideUp);
        }

        applyAnimations();

        parentLoginBtn.setOnClickListener(v -> validateAndLogin());
    }

    private void applyAnimations() {
        android.view.View[] views = { parentName, parentEmail, parentMobile, parentLoginBtn };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(android.view.View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(android.view.View.VISIBLE);
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

    private void validateAndLogin() {
        String name = parentName.getText().toString().trim();
        String email = parentEmail.getText().toString().trim();
        String mobile = parentMobile.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            parentName.setError("Name is required");
            parentName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            parentEmail.setError("Email is required");
            parentEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            parentEmail.setError("Enter a valid email");
            parentEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mobile)) {
            parentMobile.setError("Mobile number is required");
            parentMobile.requestFocus();
            return;
        }
        if (mobile.length() != 10) {
            parentMobile.setError("Mobile number must be 10 digits");
            parentMobile.requestFocus();
            return;
        }

        showLoading(parentLoginBtn, "Verifying...");

        // Verify if parent exists in Firebase before sending OTP
        com.google.firebase.database.DatabaseReference parentsRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Parents");
        parentsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                boolean found = false;
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    String dbEmail = child.child("email").getValue(String.class);
                    if (email.equalsIgnoreCase(dbEmail)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    hideLoading(parentLoginBtn);
                    Toast.makeText(ParentLoginActivity.this, "This email is not registered as a parent.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Parent found, proceed to send OTP
                sendOTP(name, email, mobile);
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                hideLoading(parentLoginBtn);
                Toast.makeText(ParentLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOTP(String name, String email, String mobile) {
        parentLoginBtn.setText("Sending OTP...");
        // Generate OTP and send via email
        String otp = String.format("%06d", new Random().nextInt(1000000));

        new Thread(() -> {
            try {
                String subject = "SmartConnect Parents OTP";
                String message = "Dear " + name + ",\n\n" +
                        "Your OTP for SmartConnect Parents Login is: " + otp + "\n\n" +
                        "This OTP is valid for 10 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Thank you,\nSmartConnect Team";
                GmailSender.sendMailWithSubject(email, subject, message);
                runOnUiThread(() -> {
                    Toast.makeText(this, "OTP sent to " + email, Toast.LENGTH_LONG).show();
                    // Navigate to unified OTP screen
                    Intent intent = new Intent(ParentLoginActivity.this, loginotpverification.class);
                    intent.putExtra("loginType", "parent");
                    intent.putExtra("realOtp", otp);
                    intent.putExtra("userEmail", email);
                    intent.putExtra("userName", name);
                    intent.putExtra("userMobile", mobile);
                    startActivity(intent);
                    finish(); // Optional: Close login after starting OTP verification
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading(parentLoginBtn);
                    Toast.makeText(this, "Failed to send OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLoading(Button button, String loadingText) {
        originalButtonText = button.getText().toString();
        button.setText(loadingText);
        button.setEnabled(false);

        buttonAnimator = ObjectAnimator.ofPropertyValuesHolder(
                button,
                PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.6f));
        buttonAnimator.setDuration(600);
        buttonAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        buttonAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        buttonAnimator.start();
    }

    private void hideLoading(Button button) {
        if (buttonAnimator != null) {
            buttonAnimator.cancel();
        }
        button.setAlpha(1.0f);
        button.setText(originalButtonText);
        button.setEnabled(true);
    }
}
