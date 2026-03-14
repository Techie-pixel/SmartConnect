package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class studentsignin extends AppCompatActivity {

    TextView signin;
    EditText username, useremail, userpassword, usermobile;
    Button userbutton;
    Spinner standardSpinner, streamSpinner;
    private FirebaseAuth mauth;
    private DatabaseReference databaseReference;
    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentsignin);

        standardSpinner = findViewById(R.id.spinnerStandard);
        ArrayAdapter<String> standardAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[] { "Select Standard", "11", "12" });
        standardAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        standardSpinner.setAdapter(standardAdapter);

        streamSpinner = findViewById(R.id.spinnerStream);
        ArrayAdapter<String> streamAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[] { "Select Stream", "Science", "Commerce", "Arts" });
        streamAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        streamSpinner.setAdapter(streamAdapter);

        signin = findViewById(R.id.signin);
        username = findViewById(R.id.username);
        useremail = findViewById(R.id.useremail);
        usermobile = findViewById(R.id.usermobile);
        userpassword = findViewById(R.id.userpassword);
        userbutton = findViewById(R.id.userbutton);

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainLayout.startAnimation(slideUp);
        }

        applyAnimations();

        mauth = FirebaseAuth.getInstance();

        setupPasswordToggle();

        databaseReference = FirebaseDatabase.getInstance().getReference("Students");

        signin.setOnClickListener(view -> {
            Intent intent = new Intent(studentsignin.this, studentlogin.class);
            startActivity(intent);
        });

        userbutton.setOnClickListener(view -> registeruser());
    }

    private void registeruser() {
        String name = username.getText().toString().trim();
        String email = useremail.getText().toString().trim();
        String password = userpassword.getText().toString().trim();
        String mobile = usermobile.getText().toString().trim();
        String standard = standardSpinner.getSelectedItem().toString();
        String stream = streamSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            username.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            useremail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            userpassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            userpassword.setError("Password length should be 6 or above!");
            return;
        }
        if (TextUtils.isEmpty(mobile)) {
            usermobile.setError("Mobile number is required");
            return;
        }
        if (mobile.length() != 10) {
            usermobile.setError("Mobile number should be 10 digits");
            return;
        }
        if (standard.equals("Select Standard")) {
            Toast.makeText(this, "Please select Standard", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stream.equals("Select Stream")) {
            Toast.makeText(this, "Please select Stream", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(userbutton, "Registering...");

        mauth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mauth.getCurrentUser();
                        if (user != null) {
                            String userid = user.getUid();

                            String folder = (standard + stream).toLowerCase();

                            HashMap<String, String> userdata = new HashMap<>();
                            userdata.put("Name", name);
                            userdata.put("Email", email);
                            userdata.put("Mobile", mobile);
                            userdata.put("Password", password);
                            userdata.put("Standard", standard);
                            userdata.put("Stream", stream);

                            databaseReference.child(userid).setValue(userdata)
                                    .addOnSuccessListener(unused -> {

                                        DatabaseReference classRef = FirebaseDatabase.getInstance()
                                                .getReference("ClassGroups")
                                                .child(folder);

                                        classRef.child(userid).setValue(userdata);

                                        Toast.makeText(getApplicationContext(),
                                                "Registered Successfully! Sending OTP...", Toast.LENGTH_LONG).show();

                                        // Generate and send OTP
                                        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
                                        final String studentEmail = email;
                                        final String studentId = userid;
                                        new Thread(() -> {
                                            try {
                                                GmailSender.sendMailWithSubject(studentEmail,
                                                        "SmartConnect Registration OTP",
                                                        "Dear Student,\n\nYour OTP is: " + otp +
                                                                "\n\nValid for 10 minutes.\n\nSmartConnect Team");
                                            } catch (Exception ignored) {
                                            }
                                        }).start();

                                        // Redirect to unified OTP screen
                                        Intent intent = new Intent(studentsignin.this, loginotpverification.class);
                                        intent.putExtra("loginType", "student");
                                        intent.putExtra("realOtp", otp);
                                        intent.putExtra("userEmail", email);
                                        intent.putExtra("userId", userid);
                                        intent.putExtra("userStandard", standard);
                                        intent.putExtra("userStream", stream);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        hideLoading(userbutton);
                                        Toast.makeText(getApplicationContext(), "Failed to save data",
                                                Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        hideLoading(userbutton);
                        Toast.makeText(getApplicationContext(),
                                "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupPasswordToggle() {

        Drawable eyeOff = getResources().getDrawable(R.drawable.eyeoff);
        Drawable eyeOn = getResources().getDrawable(R.drawable.eyeopen);

        int size = (int) (22 * getResources().getDisplayMetrics().density);

        eyeOff.setBounds(0, 0, size, size);
        eyeOn.setBounds(0, 0, size, size);

        userpassword.setCompoundDrawables(null, null, eyeOff, null);

        userpassword.setOnTouchListener((v, event) -> {

            if (event.getAction() == MotionEvent.ACTION_UP) {

                int drawableRight = 2;

                if (event.getX() >= (userpassword.getWidth()
                        - size
                        - userpassword.getPaddingRight())) {

                    if (userpassword.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

                        userpassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        userpassword.setCompoundDrawables(null, null, eyeOn, null);

                    } else {

                        userpassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        userpassword.setCompoundDrawables(null, null, eyeOff, null);
                    }

                    userpassword.setSelection(userpassword.getText().length());
                    return true;
                }
            }

            return false;
        });
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
        View[] editTexts = { username, useremail, usermobile, userpassword };
        View[] spinners = { standardSpinner, streamSpinner };
        View[] otherViews = { userbutton, signin };

        int offsetIndex = 0;

        for (View v : editTexts) {
            if (v != null) {
                v.setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(offsetIndex * 80L);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        v.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                v.startAnimation(anim);
                addFocusAnimation(v);
                offsetIndex++;
            }
        }

        for (View v : spinners) {
            if (v != null) {
                v.setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(offsetIndex * 80L);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        v.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                v.startAnimation(anim);
                offsetIndex++;
            }
        }

        for (View v : otherViews) {
            if (v != null) {
                v.setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                anim.setStartOffset(offsetIndex * 80L);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        v.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (v == signin) {
                            startPulseAnimation(signin);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                v.startAnimation(anim);
                if (v instanceof Button) {
                    addTouchAnimation(v);
                }
                offsetIndex++;
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