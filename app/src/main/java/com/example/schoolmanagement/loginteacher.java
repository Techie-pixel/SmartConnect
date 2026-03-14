package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class loginteacher extends AppCompatActivity {

    EditText idInput, emailInput, nameInput;
    Spinner standardSpinner, streamSpinner, subjectSpinner;
    Button loginBtn;
    private ObjectAnimator buttonAnimator;
    private String originalButtonText;

    private static final String PRINCIPAL_ID = "YOUR_PRINCIPAL_ID";
    private static final String PRINCIPAL_EMAIL = "principal@example.com";
    private static final String PRINCIPAL_NAME = "Principal Name";

    String selectedStandard = "";
    String selectedStream = "";
    String selectedSubject = "";

    String loginRole = "teacher";

    DatabaseReference root = FirebaseDatabase.getInstance().getReference();

    ArrayList<String> subjectList = new ArrayList<>();
    ArrayAdapter<String> subjectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getIntent() != null && getIntent().hasExtra("loginRole")) {
            loginRole = getIntent().getStringExtra("loginRole");
        }
        if ("principal".equals(loginRole)) {
            setContentView(R.layout.activity_principallogin);
        } else {
            setContentView(R.layout.activity_teacherlogin);
        }

        LinearLayout mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainLayout.startAnimation(slideUp);
        }

        idInput = findViewById(R.id.idInput);
        emailInput = findViewById(R.id.emailInput);
        nameInput = findViewById(R.id.nameInput);

        standardSpinner = findViewById(R.id.standardSpinner);
        streamSpinner = findViewById(R.id.streamSpinner);
        subjectSpinner = findViewById(R.id.subjectSpinner);
        loginBtn = findViewById(R.id.loginBtn);

        TextView tvStandardLabel = findViewById(R.id.tvStandardLabel);
        TextView tvStreamLabel = findViewById(R.id.tvStreamLabel);
        TextView tvSubjectLabel = findViewById(R.id.tvSubjectLabel);

        if ("principal".equals(loginRole)) {
            if (standardSpinner != null)
                standardSpinner.setVisibility(View.GONE);
            if (streamSpinner != null)
                streamSpinner.setVisibility(View.GONE);
            if (subjectSpinner != null)
                subjectSpinner.setVisibility(View.GONE);
            if (tvStandardLabel != null)
                tvStandardLabel.setVisibility(View.GONE);
            if (tvStreamLabel != null)
                tvStreamLabel.setVisibility(View.GONE);
            if (tvSubjectLabel != null)
                tvSubjectLabel.setVisibility(View.GONE);
        }

        if (!"principal".equals(loginRole)) {
            initSubjectSpinner();
            setupMainSpinners();
            loadAllSubjects();
        }

        applyAnimations();

        loginBtn.setOnClickListener(v -> validateAndLogin());
    }

    private void applyAnimations() {
        View[] views;
        if ("principal".equals(loginRole)) {
            views = new View[] { idInput, emailInput, nameInput, loginBtn };
        } else {
            views = new View[] { idInput, emailInput, nameInput, standardSpinner, streamSpinner, subjectSpinner, loginBtn };
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        if (!"principal".equals(loginRole)) {
            loadAllSubjects();
        }
        clearForm(); // back aane par sab clear
    }

    private void clearForm() {
        idInput.setText("");
        emailInput.setText("");
        nameInput.setText("");

        if (standardSpinner != null && standardSpinner.getAdapter() != null) {
            standardSpinner.setSelection(0);
        }
        if (streamSpinner != null && streamSpinner.getAdapter() != null) {
            streamSpinner.setSelection(0);
        }
        if (subjectSpinner != null && subjectSpinner.getAdapter() != null) {
            subjectSpinner.setSelection(0);
        }

        selectedStandard = "";
        selectedStream = "";
        selectedSubject = "";
    }

    private void initSubjectSpinner() {
        subjectList.add("Select Subject");

        subjectAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                subjectList);
        subjectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedSubject = (pos == 0) ? "" : subjectList.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSubject = "";
            }
        });
    }

    private void setupMainSpinners() {

        String[] standards = { "Select Standard", "11", "12" };
        ArrayAdapter<String> standardAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                standards);
        standardAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        standardSpinner.setAdapter(standardAdapter);

        standardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedStandard = (pos == 0) ? "" : standards[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStandard = "";
            }
        });

        String[] streams = { "Select Stream", "Science", "Commerce", "Arts" };
        ArrayAdapter<String> streamAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                streams);
        streamAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        streamSpinner.setAdapter(streamAdapter);

        streamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedStream = (pos == 0) ? "" : streams[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStream = "";
            }
        });
    }

    private void loadAllSubjects() {

        root.child("Subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                HashSet<String> uniqueSubjects = new HashSet<>();

                for (DataSnapshot streamSnapshot : snapshot.getChildren()) {
                    String streamName = streamSnapshot.getKey();
                    String streamShort = getStreamShort(streamName);

                    for (DataSnapshot classSnapshot : streamSnapshot.getChildren()) {
                        String className = classSnapshot.getKey();

                        for (DataSnapshot subjectSnapshot : classSnapshot.getChildren()) {
                            String subjectName = subjectSnapshot.getValue(String.class);

                            if (subjectName != null && !subjectName.isEmpty()) {
                                String display = subjectName + " (" + className + " " + streamShort + ")";
                                uniqueSubjects.add(display);
                            }
                        }
                    }
                }

                subjectList.clear();
                subjectList.add("Select Subject");
                subjectList.addAll(uniqueSubjects);

                subjectAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(loginteacher.this,
                        "Error loading subjects: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStreamShort(String stream) {
        if (stream == null)
            return "";
        switch (stream) {
            case "Science":
                return "Sci";
            case "Commerce":
                return "Com";
            case "Arts":
                return "Arts";
            default:
                return stream.substring(0, Math.min(3, stream.length()));
        }
    }

    private void validateAndLogin() {

        String id = idInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();

        if (id.isEmpty()) {
            idInput.setError("Enter ID");
            idInput.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Enter email");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid email");
            emailInput.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            nameInput.setError("Enter name");
            nameInput.requestFocus();
            return;
        }

        // PRINCIPAL LOGIN
        if ("principal".equals(loginRole)) {
            checkPrincipalInFirebase(id, email, name);
            return;
        }

        // TEACHER LOGIN
        if (selectedStandard.isEmpty()) {
            Toast.makeText(this, "Please select standard", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedStream.isEmpty()) {
            Toast.makeText(this, "Please select stream", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSubject.isEmpty()) {
            Toast.makeText(this, "Please select subject", Toast.LENGTH_SHORT).show();
            return;
        }

        checkTeacherInFirebase(id, email, name,
                selectedStandard, selectedStream, selectedSubject);
    }

    private void checkPrincipalInFirebase(String id, String email, String name) {
        root.child("Principals").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(loginteacher.this,
                                    "Principal ID not found!",
                                    Toast.LENGTH_LONG).show();
                            idInput.setError("ID not found");
                            idInput.requestFocus();
                            return;
                        }

                        String dbEmail = snapshot.child("email").getValue(String.class);
                        String dbName = snapshot.child("name").getValue(String.class);

                        if (email.equalsIgnoreCase(dbEmail) && name.equalsIgnoreCase(dbName)) {
                            sendOTPAndProceed("principal", email, name,
                                    "N/A", "N/A", "N/A", id);
                            clearForm();
                        } else {
                            Toast.makeText(loginteacher.this,
                                    "Wrong credentials!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(loginteacher.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkTeacherInFirebase(String id, String email, String name,
            String standard, String stream, String subject) {

        root.child("Teachers").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(loginteacher.this,
                                    "Teacher ID not found!",
                                    Toast.LENGTH_LONG).show();
                            idInput.setError("ID not found");
                            idInput.requestFocus();
                            return;
                        }

                        // Check all subject records for this teacher
                        boolean credentialsMatch = false;
                        String matchedEmail = "";
                        String matchedName = "";
                        String matchedClass = "";
                        String matchedStream = "";
                        String matchedSubject = "";

                        for (DataSnapshot subjectSnapshot : snapshot.getChildren()) {
                            String dbEmail = subjectSnapshot.child("email").getValue(String.class);
                            String dbName = subjectSnapshot.child("name").getValue(String.class);
                            String dbClass = subjectSnapshot.child("class").getValue(String.class);
                            String dbStream = subjectSnapshot.child("stream").getValue(String.class);
                            String dbSubject = subjectSnapshot.child("subject").getValue(String.class);

                            if (email.equalsIgnoreCase(dbEmail) &&
                                    name.equalsIgnoreCase(dbName) &&
                                    standard.equals(dbClass) &&
                                    stream.equals(dbStream) &&
                                    subject.equals(dbSubject)) {
                                credentialsMatch = true;
                                matchedEmail = dbEmail;
                                matchedName = dbName;
                                matchedClass = dbClass;
                                matchedStream = dbStream;
                                matchedSubject = dbSubject;
                                break;
                            }
                        }

                        if (credentialsMatch) {
                            sendOTPAndProceed("teacher",
                                    matchedEmail, matchedName,
                                    matchedClass, matchedStream, matchedSubject,
                                    id);

                            clearForm();
                        } else {
                            Toast.makeText(loginteacher.this,
                                    "Wrong credentials!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(loginteacher.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendOTPAndProceed(String loginType,
            String email,
            String name,
            String standard,
            String stream,
            String subject,
            String teacherId) {

        int otp = 100000 + new Random().nextInt(900000);
        String otpValue = String.valueOf(otp);

        showLoading(loginBtn, "Sending OTP...");

        new Thread(() -> {
            try {
                GmailSender.sendMail(email, otpValue);

                // Email sent successfully → navigate to OTP screen
                runOnUiThread(() -> {
                    Toast.makeText(loginteacher.this,
                            "OTP sent to " + email, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(loginteacher.this, loginotpverification.class);
                    intent.putExtra("loginType", loginType);
                    intent.putExtra("userEmail", email);
                    intent.putExtra("userName", name);
                    intent.putExtra("userStandard", standard);
                    intent.putExtra("userStream", stream);
                    intent.putExtra("userSubject", subject);
                    intent.putExtra("teacherId", teacherId);
                    intent.putExtra("realOtp", otpValue);

                    startActivity(intent);
                    hideLoading(loginBtn);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(loginteacher.this,
                            "Failed to send OTP: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    hideLoading(loginBtn);
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
