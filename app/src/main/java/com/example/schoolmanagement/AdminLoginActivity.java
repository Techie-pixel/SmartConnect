package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminEmail, adminPassword;
    private Button adminLoginBtn;
    private TextView adminForgotPassword;
    private FirebaseAuth mAuth;
    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "your_admin_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        adminEmail = findViewById(R.id.adminEmail);
        adminPassword = findViewById(R.id.adminPassword);
        adminLoginBtn = findViewById(R.id.adminLoginBtn);
        adminForgotPassword = findViewById(R.id.adminForgotPassword);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainLayout.startAnimation(slideUp);
        }

        SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        if (adminPrefs.getBoolean("isAdminLoggedIn", false)) {
            startActivity(new Intent(this, admindashboard.class));
            finish();
            return;
        }

        adminLoginBtn.setOnClickListener(v -> loginAdmin());
        adminForgotPassword.setOnClickListener(v -> resetPassword());

        mAuth = FirebaseAuth.getInstance();

        setupPasswordVisibilityToggle();
        applyAnimations();
    }

    private void setupPasswordVisibilityToggle() {
        adminPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        adminPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (adminPassword.getCompoundDrawables()[DRAWABLE_END] != null &&
                        event.getRawX() >= (adminPassword.getRight()
                                - adminPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width()
                                - adminPassword.getPaddingRight())) {
                    boolean isVisible = adminPassword
                            .getTransformationMethod() instanceof HideReturnsTransformationMethod;
                    if (isVisible) {
                        adminPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        adminPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eyeoff_small, 0);
                    } else {
                        adminPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        adminPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eyeopen_small, 0);
                    }
                    adminPassword.setSelection(adminPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    private void applyAnimations() {
        android.view.View[] views = { adminEmail, adminPassword, adminLoginBtn };

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

    private void loginAdmin() {
        String email = adminEmail.getText().toString().trim();
        String password = adminPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            adminEmail.setError("Email is required");
            adminEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            adminEmail.setError("Enter a valid email");
            adminEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            adminPassword.setError("Password is required");
            adminPassword.requestFocus();
            return;
        }

        if (!email.equalsIgnoreCase(ADMIN_EMAIL) || !password.equals(ADMIN_PASSWORD)) {
            Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(adminLoginBtn, "Logging in...");

        // ✅ Login successful toast
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

        String otp = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        new Thread(() -> {
            try {
                GmailSender.sendMail(email, otp);
                runOnUiThread(() -> {
                    Intent intent = new Intent(AdminLoginActivity.this, loginotpverification.class);
                    intent.putExtra("loginType", "admin");
                    intent.putExtra("userEmail", email);
                    intent.putExtra("userName", "Admin");
                    intent.putExtra("realOtp", otp);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideLoading(adminLoginBtn);
                    Toast.makeText(AdminLoginActivity.this, "OTP send failed", Toast.LENGTH_SHORT).show();
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

    private void resetPassword() {
        String email = adminEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your admin email first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equalsIgnoreCase(ADMIN_EMAIL)) {
            Toast.makeText(this, "This email is not an admin account", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth == null)
            mAuth = FirebaseAuth.getInstance();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Reset link sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Failed to send reset email. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}