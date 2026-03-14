package com.example.schoolmanagement;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class manageteachers extends AppCompatActivity {

    EditText etTeacherId, etTeacherName, etTeacherEmail;
    Spinner spinnerSubject, spinnerStandard, spinnerStream;
    Button btnCreateTeacher, btnAddSubject, btnDeleteTeacher;

    String selectedSubject = "";

    DatabaseReference root = FirebaseDatabase.getInstance().getReference();

    ArrayList<String> subjectList = new ArrayList<>();
    ArrayList<String> allSubjectsDisplay = new ArrayList<>();
    ArrayAdapter<String> subjectAdapter;
    Map<String, List<String>> subjectCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manageteachers);

        initViews();
        initSubjectSpinner();

        applyAnimations();

        // Load all subjects on start
        loadAllSubjects();
    }

    private void applyAnimations() {
        View[] views = {
                etTeacherId, etTeacherName, etTeacherEmail,
                spinnerStandard, spinnerStream, spinnerSubject,
                btnCreateTeacher, btnAddSubject, btnDeleteTeacher
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all subjects when coming back to activity
        loadAllSubjects();
    }

    // ---------------------------------------------------------
    private void initViews() {

        etTeacherId = findViewById(R.id.etTeacherId);
        etTeacherName = findViewById(R.id.etTeacherName);
        etTeacherEmail = findViewById(R.id.etTeacherEmail);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        spinnerStandard = findViewById(R.id.spinnerStandard);
        spinnerStream = findViewById(R.id.spinnerStream);

        btnCreateTeacher = findViewById(R.id.btnCreateTeacher);
        btnAddSubject = findViewById(R.id.AddnewSubject);
        btnDeleteTeacher = findViewById(R.id.btnDeleteTeacher);

        btnAddSubject.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            openAddSubjectDialog();
        });
        btnCreateTeacher.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            createTeacher();
        });
        btnDeleteTeacher.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new android.content.Intent(this, DeleteTeacherActivity.class)), 100);
        });
    }

    // ---------------------------------------------------------
    private void initSubjectSpinner() {
        subjectList.add("Select Subject");

        subjectAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                subjectList
        );
        subjectAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinnerSubject.setAdapter(subjectAdapter);

        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    selectedSubject = "";
                } else {
                    selectedSubject = subjectList.get(pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSubject = "";
            }
        });

        ArrayAdapter<String> stdAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item,
                new String[]{"Select Standard", "11", "12"}
        );
        stdAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStandard.setAdapter(stdAdapter);
        spinnerStandard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySubjectFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        ArrayAdapter<String> streamAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_item,
                new String[]{"Select Stream", "Science", "Commerce", "Arts"}
        );
        streamAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStream.setAdapter(streamAdapter);
        spinnerStream.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySubjectFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void deleteTeacher() {

        String id = etTeacherId.getText().toString().trim();

        if (id.isEmpty()) {
            Toast.makeText(this, "Enter Teacher ID to delete", Toast.LENGTH_SHORT).show();
            etTeacherId.requestFocus();
            return;
        }

        if (selectedSubject == null || selectedSubject.isEmpty()) {
            root.child("Teachers").child(id).removeValue()
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Teacher deleted successfully", Toast.LENGTH_SHORT).show();
                        clearFields();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        root.child("Teachers").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(manageteachers.this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean found = false;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String dbSubject = child.child("subject").getValue(String.class);
                            if (selectedSubject.equals(dbSubject)) {
                                child.getRef().removeValue()
                                        .addOnSuccessListener(a -> {
                                            Toast.makeText(manageteachers.this,
                                                    "Subject assignment deleted", Toast.LENGTH_SHORT).show();
                                            clearFields();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(manageteachers.this,
                                                        "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            Toast.makeText(manageteachers.this,
                                    "Subject assignment not found for this teacher", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(manageteachers.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------------------------
    // Load ALL subjects from ALL streams and classes with format: SubjectName (Standard StreamShort)
    private void loadAllSubjects() {
        applySubjectFilter();
    }

    private void applySubjectFilter() {
        String stdSel = spinnerStandard.getSelectedItem() != null
                ? spinnerStandard.getSelectedItem().toString() : "Select Standard";
        String streamSel = spinnerStream.getSelectedItem() != null
                ? spinnerStream.getSelectedItem().toString() : "Select Stream";

        if (!"Select Standard".equals(stdSel) && !"Select Stream".equals(streamSel)) {
            loadSubjectsFor(streamSel, stdSel);
        } else {
            subjectList.clear();
            subjectList.add("Select Subject");
            subjectAdapter.notifyDataSetChanged();
            if (subjectList.size() == 1) selectedSubject = "";
        }
    }

    private void loadSubjectsFor(String stream, String standard) {
        String key = stream + "|" + standard;
        if (subjectCache.containsKey(key)) {
            subjectList.clear();
            subjectList.add("Select Subject");
            subjectList.addAll(subjectCache.get(key));
            subjectAdapter.notifyDataSetChanged();
            return;
        }

        // Merge subjects from both "Subjects" and "subjects" for compatibility
        HashSet<String> merged = new HashSet<>();
        DatabaseReference refUpper = root.child("Subjects").child(stream).child(standard);
        DatabaseReference refLower = root.child("subjects").child(stream).child(standard);

        refUpper.get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DataSnapshot ds : task1.getResult().getChildren()) {
                    String subjectName = ds.getValue(String.class);
                    if (subjectName != null && !subjectName.isEmpty()) {
                        merged.add(subjectName);
                    }
                }
            }
            refLower.get().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful() && task2.getResult() != null) {
                    for (DataSnapshot ds : task2.getResult().getChildren()) {
                        String subjectName = ds.getValue(String.class);
                        if (subjectName != null && !subjectName.isEmpty()) {
                            merged.add(subjectName);
                        }
                    }
                }

                List<String> fetched = new ArrayList<>();
                for (String name : merged) {
                    fetched.add(name + " (" + standard + " " + getStreamShort(stream) + ")");
                }
                Collections.sort(fetched, String::compareToIgnoreCase);

                subjectCache.put(key, fetched);
                subjectList.clear();
                subjectList.add("Select Subject");
                subjectList.addAll(fetched);
                subjectAdapter.notifyDataSetChanged();

                if (fetched.isEmpty()) {
                    Toast.makeText(this, "No subjects found for " + stream + " " + standard, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    // Helper method to get short form of stream name
    private String getStreamShort(String stream) {
        if (stream == null) return "";

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

    // ---------------------------------------------------------
    private void createTeacher() {

        String id = etTeacherId.getText().toString().trim();
        String name = etTeacherName.getText().toString().trim();
        String email = etTeacherEmail.getText().toString().trim();

        if (id.isEmpty() || name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all teacher details", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSubject.isEmpty()) {
            Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract stream and class from selected subject
        // Format: "SubjectName (11 Sci)" -> extract "11" and "Science"
        String[] parts = selectedSubject.split("\\(");
        if (parts.length < 2) {
            Toast.makeText(this, "Invalid subject format", Toast.LENGTH_SHORT).show();
            return;
        }

        String subjectDetails = parts[1].replace(")", "").trim(); // "11 Sci"
        String[] details = subjectDetails.split(" ");

        if (details.length < 2) {
            Toast.makeText(this, "Invalid subject format", Toast.LENGTH_SHORT).show();
            return;
        }

        String standard = details[0]; // "11"
        String streamShort = details[1]; // "Sci"
        String stream = getFullStreamName(streamShort); // "Science"

        // Check if this subject is already assigned to ANY teacher
        checkIfSubjectAlreadyAssigned(id, name, email, stream, standard, selectedSubject);
    }

    // Helper method to convert short stream name to full name
    private String getFullStreamName(String streamShort) {
        switch (streamShort) {
            case "Sci":
                return "Science";
            case "Com":
                return "Commerce";
            case "Arts":
                return "Arts";
            default:
                return streamShort;
        }
    }

    // ---------------------------------------------------------
    // Check if subject is already assigned to ANY teacher in the entire Teachers node
    private void checkIfSubjectAlreadyAssigned(String teacherId, String teacherName, String teacherEmail,
                                               String stream, String standard, String subject) {

        root.child("Teachers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean subjectAlreadyAssigned = false;
                String assignedTeacherName = "";
                String assignedTeacherId = "";

                // Loop through ALL teachers
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String currentTeacherId = teacherSnapshot.getKey();

                    // Loop through all subjects of this teacher
                    for (DataSnapshot subjectSnapshot : teacherSnapshot.getChildren()) {
                        Object value = subjectSnapshot.getValue();

                        String existingSubject = null;
                        String existingTeacherName = null;

                        if (value instanceof HashMap) {
                            HashMap<String, Object> teacherData = (HashMap<String, Object>) value;
                            existingSubject = (String) teacherData.get("subject");
                            existingTeacherName = (String) teacherData.get("name");
                        }

                        // Check if this subject is already assigned
                        if (existingSubject != null && existingSubject.equals(subject)) {
                            subjectAlreadyAssigned = true;
                            assignedTeacherName = existingTeacherName != null ? existingTeacherName : "Unknown";
                            assignedTeacherId = currentTeacherId;
                            break;
                        }
                    }

                    if (subjectAlreadyAssigned) {
                        break;
                    }
                }

                if (subjectAlreadyAssigned) {
                    // Subject is already assigned to another teacher
                    Toast.makeText(manageteachers.this,
                            subject + " is already assigned to " + assignedTeacherName +
                                    " (ID: " + assignedTeacherId + ")!",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Subject is available, save teacher
                    saveTeacherWithSubject(teacherId, teacherName, teacherEmail, stream, standard, subject);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(manageteachers.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------
    // Save teacher data with unique subject ID
    private void saveTeacherWithSubject(String id, String name, String email,
                                        String stream, String standard, String subject) {

        String subjectKey = root.child("Teachers").child(id).push().getKey();

        if (subjectKey == null) {
            Toast.makeText(this, "Error generating subject key", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> teacherData = new HashMap<>();
        teacherData.put("teacherId", id);
        teacherData.put("name", name);
        teacherData.put("email", email);
        teacherData.put("stream", stream);
        teacherData.put("class", standard);
        teacherData.put("subject", subject);

        // Save under Teachers/teacherId/uniqueSubjectKey
        root.child("Teachers").child(id).child(subjectKey).setValue(teacherData)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Teacher assigned to " + subject + " successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Send email with teacher details
                    sendTeacherDetailsEmail(id, name, email, stream, standard, subject);

                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------------------------------------------------
    // Send teacher details via email
    private void sendTeacherDetailsEmail(String teacherId, String teacherName, String teacherEmail,
                                         String stream, String standard, String subject) {

        // Create email body with all teacher details
        String emailBody = "Dear " + teacherName + ",\n\n" +
                "Welcome to Smartconnect!\n\n" +
                "You have been assigned a new subject. Here are your login credentials and details:\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "TEACHER LOGIN CREDENTIALS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "📌 Teacher ID: " + teacherId + "\n" +
                "📌 Name: " + teacherName + "\n" +
                "📌 Email: " + teacherEmail + "\n" +
                "📌 Standard: " + standard + "\n" +
                "📌 Stream: " + stream + "\n" +
                "📌 Subject: " + subject + "\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "IMPORTANT INSTRUCTIONS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "⚠️ You can teach multiple subjects with the same Teacher ID.\n" +
                "⚠️ Please keep these credentials safe and confidential.\n" +
                "⚠️ You will need to enter ALL these details during login.\n" +
                "⚠️ Make sure to select the correct Standard, Stream, and Subject while logging in.\n\n" +
                "To log in to the system:\n" +
                "1. Open the School Management App\n" +
                "2. Click on 'Teacher Login'\n" +
                "3. Enter your Teacher ID: " + teacherId + "\n" +
                "4. Enter your Email: " + teacherEmail + "\n" +
                "5. Enter your Name: " + teacherName + "\n" +
                "6. Select Standard: " + standard + "\n" +
                "7. Select Stream: " + stream + "\n" +
                "8. Select Subject: " + subject + "\n" +
                "9. Click 'Get OTP' to receive verification code\n\n" +
                "If you have been assigned multiple subjects, you can login with the same credentials " +
                "by selecting the appropriate subject each time.\n\n" +
                "If you face any issues during login, please contact the school administration.\n\n" +
                "Best Regards,\n" +
                "Smartconnect\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "This is an automated email. Please do not reply to this message.";

        // Send email in background thread
        new Thread(() -> {
            try {
                GmailSender.sendMailWithSubject(
                        teacherEmail,
                        "New Subject Assignment - " + subject,
                        emailBody
                );

                runOnUiThread(() -> {
                    Toast.makeText(manageteachers.this,
                            "Subject assignment details sent to teacher's email!",
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(manageteachers.this,
                            "Teacher created but email failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void clearFields() {
        etTeacherId.setText("");
        etTeacherName.setText("");
        etTeacherEmail.setText("");
        spinnerSubject.setSelection(0);
    }

    // ---------------------------------------------------------
    private void openAddSubjectDialog() {

        Dialog d = new Dialog(this);
        d.setContentView(R.layout.dialog_add_subject);
        d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etName = d.findViewById(R.id.etSubjectName);
        Spinner spStream = d.findViewById(R.id.dialogSpinnerStream);
        Spinner spClass = d.findViewById(R.id.dialogSpinnerClass);
        Button btnSave = d.findViewById(R.id.btnSaveSubject);
        Button btnDelete = d.findViewById(R.id.btnDeleteSubject);

        final String[] dlgStream = {""};
        final String[] dlgClass = {""};

        // DIALOG STREAM SPINNER - Hardcoded array
        ArrayList<String> dialogStreamsList = new ArrayList<>(Arrays.asList(
                "Select Stream", "Science", "Commerce", "Arts"
        ));

        ArrayAdapter<String> dialogStreamAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                dialogStreamsList
        );
        dialogStreamAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spStream.setAdapter(dialogStreamAdapter);

        spStream.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                dlgStream[0] = pos == 0 ? "" : parent.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                dlgStream[0] = "";
            }
        });

        // DIALOG CLASS SPINNER - Hardcoded array
        ArrayList<String> dialogClassList = new ArrayList<>(Arrays.asList(
                "Select Standard", "11", "12"
        ));

        ArrayAdapter<String> dialogClassAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                dialogClassList
        );
        dialogClassAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spClass.setAdapter(dialogClassAdapter);

        spClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                dlgClass[0] = pos == 0 ? "" : parent.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                dlgClass[0] = "";
            }
        });

        // SAVE SUBJECT
        btnSave.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            String sub = etName.getText().toString().trim();

            if (sub.isEmpty()) {
                Toast.makeText(this, "Please enter subject name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dlgStream[0].isEmpty() || dlgClass[0].isEmpty()) {
                Toast.makeText(this, "Please select stream and class", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ref = root.child("Subjects")
                    .child(dlgStream[0])
                    .child(dlgClass[0]);

            // Check if subject already exists in this stream/class
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean exists = false;

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if (ds.getValue(String.class).equalsIgnoreCase(sub)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        Toast.makeText(manageteachers.this,
                                "Subject already exists in " + dlgStream[0] + " - " + dlgClass[0] + "!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        String key = ref.push().getKey();
                        ref.child(key).setValue(sub)
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(manageteachers.this,
                                            "Subject Added Successfully!",
                                            Toast.LENGTH_SHORT).show();

                                    subjectCache.remove(dlgStream[0] + "|" + dlgClass[0]);
                                    if (spinnerStream.getSelectedItem() != null
                                            && spinnerStandard.getSelectedItem() != null
                                            && dlgStream[0].equals(spinnerStream.getSelectedItem().toString())
                                            && dlgClass[0].equals(spinnerStandard.getSelectedItem().toString())) {
                                        loadSubjectsFor(dlgStream[0], dlgClass[0]);
                                    }
                                    d.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(manageteachers.this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(manageteachers.this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        // DELETE SUBJECT
        btnDelete.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            String sub = etName.getText().toString().trim();

            if (sub.isEmpty()) {
                Toast.makeText(this, "Please enter subject name to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dlgStream[0].isEmpty() || dlgClass[0].isEmpty()) {
                Toast.makeText(this, "Please select stream and class", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ref = root.child("Subjects")
                    .child(dlgStream[0])
                    .child(dlgClass[0]);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    boolean found = false;

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if (ds.getValue(String.class).equalsIgnoreCase(sub)) {
                            ds.getRef().removeValue()
                                    .addOnSuccessListener(a -> {
                                        Toast.makeText(manageteachers.this,
                                                "Subject Deleted Successfully!",
                                                Toast.LENGTH_SHORT).show();

                                        subjectCache.remove(dlgStream[0] + "|" + dlgClass[0]);
                                        if (spinnerStream.getSelectedItem() != null
                                                && spinnerStandard.getSelectedItem() != null
                                                && dlgStream[0].equals(spinnerStream.getSelectedItem().toString())
                                                && dlgClass[0].equals(spinnerStandard.getSelectedItem().toString())) {
                                            loadSubjectsFor(dlgStream[0], dlgClass[0]);
                                        }
                                        d.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(manageteachers.this,
                                                "Failed to delete: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Toast.makeText(manageteachers.this,
                                "Subject Not Found in " + dlgStream[0] + " - " + dlgClass[0] + "!",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(manageteachers.this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        d.show();
    }
}
