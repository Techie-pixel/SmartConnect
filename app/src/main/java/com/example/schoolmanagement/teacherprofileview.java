package com.example.schoolmanagement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class teacherprofileview extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView tvName;
    private EditText tvSubject, tvClass, tvStream;
    private Button btnChat, btnBlock;

    private String teacherId, teacherName;
    private String myUid; // current logged-in teacher

    private DatabaseReference blockRef;
    private boolean isBlocked = false;
    private boolean amIBlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherprofileview);

        // Get current logged-in user ID (teacher or principal)
        android.content.SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        myUid = tPrefs.getString("teacherId", null);
        if (myUid == null || myUid.isEmpty()) {
            android.content.SharedPreferences pPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
            myUid = pPrefs.getString("principalId", null);
        }

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvName);
        tvSubject = findViewById(R.id.tvSubject);
        tvClass = findViewById(R.id.tvClass);
        tvStream = findViewById(R.id.tvStream);
        btnChat = findViewById(R.id.btnChat);
        btnBlock = findViewById(R.id.btnBlock);

        // Get teacherId from intent
        Intent intent = getIntent();
        teacherId = intent.getStringExtra("teacherId");
        teacherName = intent.getStringExtra("teacherName");

        // Set initial values
        tvName.setText(teacherName != null ? teacherName : "Loading...");
        tvSubject.setText("Subject: -");
        tvClass.setText("Class: -");
        tvStream.setText("Stream: -");
        profileImage.setImageResource(R.drawable.ic_default_profile);

        // Load ALL data from Firebase
        if (teacherId != null && !teacherId.isEmpty()) {
            loadTeacherData(teacherId);
        }

        // Setup block functionality
        if (myUid != null && teacherId != null) {
            String chatKey = studentchat.makeChatKey(myUid, teacherId);
            blockRef = FirebaseDatabase.getInstance()
                    .getReference("Blocks").child(chatKey);
            checkBlockStatus();
        }

        // Chat button click
        btnChat.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                if (amIBlocked) {
                    Toast.makeText(this, "You are blocked by this user", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isBlocked) {
                    Toast.makeText(this, "Unblock this user first to chat", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent chatIntent = new Intent(this, teacherchat.class);
                chatIntent.putExtra("otherUid", teacherId);
                chatIntent.putExtra("otherName", teacherName);
                startActivity(chatIntent);
            }, 150);
        });

        // Block/Unblock button
        btnBlock.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::toggleBlock, 150);
        });
    }

    private void checkBlockStatus() {
        blockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String blockedBy = snapshot.child("blockedBy").getValue(String.class);
                    if (blockedBy != null) {
                        if (blockedBy.equals(myUid)) {
                            isBlocked = true;
                            amIBlocked = false;
                            btnBlock.setText("Unblock");
                            btnBlock.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFF2E7D32)); // green for unblock
                        } else {
                            amIBlocked = true;
                            isBlocked = false;
                            btnBlock.setText("Blocked by user");
                            btnBlock.setEnabled(false);
                        }
                    }
                } else {
                    isBlocked = false;
                    amIBlocked = false;
                    btnBlock.setText("Block");
                    btnBlock.setEnabled(true);
                    btnBlock.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0A2C56)); // dark blue
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void toggleBlock() {
        if (blockRef == null)
            return;
        if (isBlocked) {
            // Unblock
            blockRef.removeValue();
            isBlocked = false;
            Toast.makeText(this, "Unblocked", Toast.LENGTH_SHORT).show();
        } else {
            // Block
            blockRef.child("blockedBy").setValue(myUid);
            isBlocked = true;
            Toast.makeText(this, "Blocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTeacherData(String id) {
        // Try Teachers node first
        DatabaseReference teacherRef = FirebaseDatabase.getInstance()
                .getReference("Teachers").child(id);
        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    applyTeacherSnapshot(snapshot);
                } else {
                    // Fallback: check Principals node
                    DatabaseReference principalRef = FirebaseDatabase.getInstance()
                            .getReference("Principals").child(id);
                    principalRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot pSnap) {
                            if (pSnap.exists()) {
                                applyPrincipalSnapshot(pSnap);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void applyTeacherSnapshot(DataSnapshot snapshot) {
        String name = null, subject = null, standard = null, stream = null, base64Img = null;

        name = snapshot.child("name").getValue(String.class);
        if (name == null)
            name = snapshot.child("Name").getValue(String.class);

        if (name != null) {
            // Flat structure
            subject = snapshot.child("subject").getValue(String.class);
            if (subject == null)
                subject = snapshot.child("Subject").getValue(String.class);
            standard = snapshot.child("class").getValue(String.class);
            if (standard == null)
                standard = snapshot.child("standard").getValue(String.class);
            if (standard == null)
                standard = snapshot.child("Standard").getValue(String.class);
            stream = snapshot.child("stream").getValue(String.class);
            if (stream == null)
                stream = snapshot.child("Stream").getValue(String.class);
        } else {
            // Push-key children structure
            StringBuilder subjectBuilder = new StringBuilder();
            boolean first = true;
            for (DataSnapshot child : snapshot.getChildren()) {
                if (child.getKey() != null && child.getKey().startsWith("-")) {
                    if (name == null) {
                        name = child.child("name").getValue(String.class);
                        standard = child.child("class").getValue(String.class);
                        if (standard == null)
                            standard = child.child("standard").getValue(String.class);
                        stream = child.child("stream").getValue(String.class);
                    }
                    String sub = child.child("subject").getValue(String.class);
                    if (sub != null && !sub.isEmpty()) {
                        if (!first)
                            subjectBuilder.append(", ");
                        subjectBuilder.append(sub);
                        first = false;
                    }
                }
            }
            subject = subjectBuilder.length() > 0 ? subjectBuilder.toString() : null;
        }

        base64Img = snapshot.child("ProfileImageBase64").getValue(String.class);
        if (base64Img == null)
            base64Img = snapshot.child("profileImageBase64").getValue(String.class);

        teacherName = name;
        tvName.setText(name != null ? name : "Unknown");
        tvSubject.setText(subject != null ? "Subject: " + subject : "Subject: -");
        tvClass.setText(standard != null ? "Class: " + standard : "Class: -");
        tvStream.setText(stream != null ? "Stream: " + stream : "Stream: -");

        if (base64Img != null && !base64Img.isEmpty()) {
            setProfileBitmap(base64Img);
        }
    }

    private void applyPrincipalSnapshot(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        if (name == null)
            name = snapshot.child("Name").getValue(String.class);
        String base64Img = snapshot.child("ProfileImageBase64").getValue(String.class);

        teacherName = name;
        tvName.setText(name != null ? name : "Unknown");
        tvSubject.setText("Subject: Principal");
        tvClass.setText("Class: -");
        tvStream.setText("Stream: -");

        if (base64Img != null && !base64Img.isEmpty()) {
            setProfileBitmap(base64Img);
        }
    }

    private void setProfileBitmap(String base64) {
        try {
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            profileImage.setImageBitmap(bmp);
        } catch (Exception e) {
            profileImage.setImageResource(R.drawable.ic_default_profile);
        }
    }
}
