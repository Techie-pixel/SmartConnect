package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class studentlogin extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton;
    TextView clickhere, forgetPassword;

    private FirebaseAuth mauth;
    private DatabaseReference databaseReference;
    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentlogin);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        clickhere = findViewById(R.id.clickhere);
        forgetPassword = findViewById(R.id.forgetPassword);

        LinearLayout mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainLayout.startAnimation(slideUp);
        }

        applyAnimations();

        mauth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Students");

        clickhere.setOnClickListener(v -> {
            startActivity(new Intent(studentlogin.this, studentsignin.class));
        });

        loginButton.setOnClickListener(v -> loginUser());

        forgetPassword.setOnClickListener(v -> resetPassword());

        setupPasswordToggle();
    }

    private void setupPasswordToggle() {

        Drawable eyeOff = getResources().getDrawable(R.drawable.eyeoff);
        Drawable eyeOn = getResources().getDrawable(R.drawable.eyeopen);

        int size = (int) (20 * getResources().getDisplayMetrics().density);

        eyeOff.setBounds(0, 0, size, size);
        eyeOn.setBounds(0, 0, size, size);

        loginPassword.setCompoundDrawables(null, null, eyeOff, null);

        loginPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == event.ACTION_UP) {

                if (event.getRawX() >= (loginPassword.getRight() - loginPassword.getPaddingRight() - 50)) {

                    if (loginPassword.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

                        loginPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        loginPassword.setCompoundDrawables(null, null, eyeOn, null);

                    } else {

                        loginPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        loginPassword.setCompoundDrawables(null, null, eyeOff, null);
                    }

                    loginPassword.setSelection(loginPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    private void loginUser() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            loginEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password is required");
            return;
        }

        showLoading(loginButton, "Logging in...");

        mauth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mauth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            databaseReference.child(userId).get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful() && task1.getResult().exists()) {
                                    DataSnapshot snapshot = task1.getResult();
                                    String standard = snapshot.child("Standard").getValue(String.class);
                                    String stream = snapshot.child("Stream").getValue(String.class);

                                    if (standard != null && stream != null) {
                                        Toast.makeText(this, "Login successful! Sending OTP...", Toast.LENGTH_SHORT)
                                                .show();

                                        // Redirect to unified OTP screen
                                        Intent intent = new Intent(studentlogin.this, loginotpverification.class);
                                        intent.putExtra("loginType", "student");
                                        intent.putExtra("userEmail", email);
                                        intent.putExtra("userId", userId);
                                        intent.putExtra("userStandard", standard);
                                        intent.putExtra("userStream", stream);
                                        intent.putExtra("realOtp", generateAndSendOtp(email, userId));
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        hideLoading(loginButton);
                                        Toast.makeText(this, "User data missing!", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    hideLoading(loginButton);
                                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        hideLoading(loginButton);
                        Toast.makeText(this, "Login Failed! Check your email and password.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetPassword() {
        String email = loginEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
            return;
        }

        mauth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String generateAndSendOtp(String email, String uid) {
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

        // Store OTP in Firebase
        if (uid != null) {
            databaseReference.child(uid).child("OTP").setValue(otp);
            databaseReference.child(uid).child("OTPTimestamp").setValue(System.currentTimeMillis());
        }

        // Send OTP via email in background
        new Thread(() -> {
            try {
                String subject = "SmartConnect Student OTP";
                String message = "Dear Student,\n\nYour OTP for login is: " + otp +
                        "\n\nValid for 10 minutes.\n\nThank you,\nSmartConnect Team";
                GmailSender.sendMailWithSubject(email, subject, message);
                runOnUiThread(() -> Toast.makeText(this, "OTP sent to " + email, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(
                        () -> Toast.makeText(this, "OTP send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();

        return otp;
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

    private void applyAnimations() {
        View[] editTexts = { loginEmail, loginPassword };
        View[] otherViews = { loginButton, forgetPassword, clickhere };

        for (int i = 0; i < editTexts.length; i++) {
            if (editTexts[i] != null) {
                editTexts[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        editTexts[finalI].setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                editTexts[i].startAnimation(anim);
                addFocusAnimation(editTexts[i]);
            }
        }

        for (int i = 0; i < otherViews.length; i++) {
            if (otherViews[i] != null) {
                otherViews[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                anim.setStartOffset((editTexts.length * 100L) + (i * 100L));
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        otherViews[finalI].setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (otherViews[finalI] == clickhere) {
                            startPulseAnimation(clickhere);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                otherViews[i].startAnimation(anim);
                if (otherViews[i] instanceof Button) {
                    addTouchAnimation(otherViews[i]);
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void addFocusAnimation(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }
        });
    }

    private void startPulseAnimation(View view) {
        ObjectAnimator scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat("scaleX", 1.03f),
                PropertyValuesHolder.ofFloat("scaleY", 1.03f));
        scaleAnim.setDuration(800);
        scaleAnim.setRepeatCount(ObjectAnimator.INFINITE);
        scaleAnim.setRepeatMode(ObjectAnimator.REVERSE);
        scaleAnim.start();
    }
}