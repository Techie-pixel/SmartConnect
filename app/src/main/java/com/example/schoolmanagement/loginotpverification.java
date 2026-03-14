package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class loginotpverification extends AppCompatActivity {

    private EditText otpInput;
    private Button verifyBtn, resendBtn;
    private TextView tvResendTimer;

    // Data from previous activity
    private String realOtp, loginType, userEmail, userName,
            userStandard, userStream, userSubject, teacherId, userMobile, userId;

    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginotpverification);

        otpInput = findViewById(R.id.otpInput);
        verifyBtn = findViewById(R.id.verifyBtn);
        resendBtn = findViewById(R.id.resendBtn);
        tvResendTimer = findViewById(R.id.tvResendTimer);

        // Receive all data from previous activity
        realOtp = getIntent().getStringExtra("realOtp");
        loginType = getIntent().getStringExtra("loginType"); // "principal","teacher","student","parent"
        userEmail = getIntent().getStringExtra("userEmail");
        userName = getIntent().getStringExtra("userName");
        userStandard = getIntent().getStringExtra("userStandard");
        userStream = getIntent().getStringExtra("userStream");
        userSubject = getIntent().getStringExtra("userSubject");
        teacherId = getIntent().getStringExtra("teacherId");
        userMobile = getIntent().getStringExtra("userMobile");
        userId = getIntent().getStringExtra("userId");

        verifyBtn.setOnClickListener(v -> verifyOTP());
        resendBtn.setOnClickListener(v -> resendOtp());

        applyAnimations();
        startCooldown(); // start initial cooldown to prevent immediate spam
    }

    private void resendOtp() {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Email not available for resend", Toast.LENGTH_SHORT).show();
            return;
        }

        String newOtp = String.format("%06d", new java.util.Random().nextInt(1000000));
        realOtp = newOtp; // update current expected OTP

        // If student flow, also store in Firebase with timestamp
        if ("student".equalsIgnoreCase(loginType) && userId != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("Students").child(userId);
            dbRef.child("OTP").setValue(newOtp);
            dbRef.child("OTPTimestamp").setValue(System.currentTimeMillis());
        }

        String subject = "SmartConnect OTP";
        String message = "Dear " + (userName != null ? userName : "User") + ",\n\n" +
                "Your new OTP is: " + newOtp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "SmartConnect Team";

        new Thread(() -> {
            try {
                GmailSender.sendMailWithSubject(userEmail, subject, message);
                runOnUiThread(() -> {
                    Toast.makeText(this, "OTP resent to " + userEmail, Toast.LENGTH_SHORT).show();
                    startCooldown();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Failed to resend OTP: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startCooldown() {
        resendBtn.setEnabled(false);
        new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                if (tvResendTimer != null) {
                    tvResendTimer.setText("Resend in 00:" + (sec < 10 ? "0" + sec : sec));
                }
            }

            @Override
            public void onFinish() {
                resendBtn.setEnabled(true);
                if (tvResendTimer != null)
                    tvResendTimer.setText("Didn’t receive OTP?");
            }
        }.start();
    }

    private void verifyOTP() {
        String enteredOtp = otpInput.getText().toString().trim();

        if (TextUtils.isEmpty(enteredOtp)) {
            otpInput.setError("Please enter OTP");
            return;
        }
        if (enteredOtp.length() != 6) {
            otpInput.setError("OTP must be 6 digits");
            return;
        }

        if (!enteredOtp.equals(realOtp)) {
            otpInput.setError("Invalid OTP. Try again.");
            otpInput.setText("");
            return;
        }

        showLoading(verifyBtn, "Verifying...");

        // OTP is correct — handle by role
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

        if ("principal".equalsIgnoreCase(loginType)) {
            handlePrincipalLogin();
        } else if ("teacher".equalsIgnoreCase(loginType)) {
            handleTeacherLogin();
        } else if ("student".equalsIgnoreCase(loginType)) {
            handleStudentLogin();
        } else if ("admin".equalsIgnoreCase(loginType)) {
            handleAdminLogin();
        } else if ("parent".equalsIgnoreCase(loginType)) {
            handleParentLogin();
        } else {
            // Fallback
            startActivity(new Intent(this, loginchoice.class));
            finish();
        }
    }

    // ─── PRINCIPAL ────────────────────────────────────────────────
    private void handlePrincipalLogin() {
        SharedPreferences principalPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
        principalPrefs.edit()
                .putBoolean("isPrincipalLoggedIn", true)
                .putString("principalId", teacherId != null ? teacherId : "951890")
                .putString("principalEmail", userEmail != null ? userEmail : "")
                .putString("principalName", userName != null ? userName : "Principal")
                .apply();

        Intent intent = new Intent(this, principaltab.class);
        intent.putExtra("loginType", "principal");
        startActivity(intent);
        finish();
    }

    // ─── ADMIN ─────────────────────────────────────────────────────
    private void handleAdminLogin() {
        SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        adminPrefs.edit()
                .putBoolean("isAdminLoggedIn", true)
                .putString("adminEmail", userEmail != null ? userEmail : "")
                .apply();

        Toast.makeText(this, "Admin OTP verified! Redirecting...", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(this, admindashboard.class);
            startActivity(intent);
            finish();
        }, 800);
    }

    // ─── TEACHER ──────────────────────────────────────────────────
    private void handleTeacherLogin() {
        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        String standard = getIntent().getStringExtra("userStandard");
        tPrefs.edit()
                .putBoolean("isTeacherLoggedIn", true)
                .putString("teacherId", teacherId)
                .putString("teacherStandard", standard != null ? standard : "")
                .putString("teacherSubject", userSubject != null ? userSubject : "")
                .putString("teacherStream", userStream != null ? userStream : "")
                .putString("teacherName", userName != null ? userName : "")
                .putString("teacherEmail", userEmail != null ? userEmail : "")
                .apply();

        Intent intent = new Intent(this, teachertab.class);
        intent.putExtra("teacherId", teacherId);
        startActivity(intent);
        finish();
    }

    // ─── STUDENT ──────────────────────────────────────────────────
    private void handleStudentLogin() {
        // Clear OTP from Firebase
        if (userId != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("Students").child(userId);
            dbRef.child("OTP").removeValue();
            dbRef.child("OTPTimestamp").removeValue();
        }

        // Save student session
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("Standard", userStandard)
                .putString("Stream", userStream)
                .putString("studentEmail", userEmail != null ? userEmail : "")
                .putString("studentName", userName != null ? userName : "Student")
                .putString("studentId", userId != null ? userId : "")
                .apply();

        // Redirect to appropriate student dashboard
        redirectStudentToDashboard(userStandard, userStream);
    }

    // ─── PARENT ───────────────────────────────────────────────────
    private void handleParentLogin() {
        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        parentPrefs.edit()
                .putBoolean("isParentLoggedIn", true)
                .putString("parentName", userName)
                .putString("parentEmail", userEmail)
                .putString("parentMobile", userMobile)
                .apply();

        // Fetch studentUID, studentName, class info from Parents node
        com.google.firebase.database.DatabaseReference parentsRef = FirebaseDatabase.getInstance()
                .getReference("Parents");
        parentsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            private boolean found = false;

            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    String email = child.child("email").getValue(String.class);
                    if (userEmail != null && userEmail.equalsIgnoreCase(email)) {
                        String sUID = child.child("studentUID").getValue(String.class);
                        String sName = child.child("studentName").getValue(String.class);
                        String sStd = child.child("studentStandard").getValue(String.class);
                        String sStream = child.child("studentStream").getValue(String.class);
                        String pKey = child.getKey();

                        parentPrefs.edit()
                                .putString("parentKey", pKey != null ? pKey : "")
                                .putString("studentUID", sUID != null ? sUID : "")
                                .putString("studentName", sName != null ? sName : "")
                                .putString("studentStandard", sStd != null ? sStd : "")
                                .putString("studentStream", sStream != null ? sStream : "")
                                .apply();
                        found = true;
                        break;
                    }
                }

                if (found) {
                    Toast.makeText(loginotpverification.this, "Welcome, " + userName + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(loginotpverification.this, ParentDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    hideLoading(verifyBtn);
                    Toast.makeText(loginotpverification.this, "Error: Parent details not found in database.", Toast.LENGTH_LONG).show();
                    // Optional: redirect to login
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                hideLoading(verifyBtn);
                Toast.makeText(loginotpverification.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── STUDENT REDIRECT ─────────────────────────────────────────
    private void redirectStudentToDashboard(String standard, String stream) {
        Intent intent;
        if ("11".equals(standard) && "Science".equals(stream)) {
            intent = new Intent(this, elevensciencehomepage.class);
        } else if ("11".equals(standard) && "Commerce".equals(stream)) {
            intent = new Intent(this, elevencommercehome.class);
        } else if ("11".equals(standard) && "Arts".equals(stream)) {
            intent = new Intent(this, elevenartshome.class);
        } else if ("12".equals(standard) && "Science".equals(stream)) {
            intent = new Intent(this, twelvesciencehome.class);
        } else if ("12".equals(standard) && "Commerce".equals(stream)) {
            intent = new Intent(this, twelvecommercehome.class);
        } else if ("12".equals(standard) && "Arts".equals(stream)) {
            intent = new Intent(this, twelveartshome.class);
        } else {
            intent = new Intent(this, studentlogin.class);
        }
        startActivity(intent);
        finish();
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
        View[] editTexts = { otpInput };
        View[] otherViews = { verifyBtn, resendBtn };

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
                        if (otherViews[finalI] == resendBtn) {
                            startPulseAnimation(resendBtn);
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

    @android.annotation.SuppressLint("ClickableViewAccessibility")
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
