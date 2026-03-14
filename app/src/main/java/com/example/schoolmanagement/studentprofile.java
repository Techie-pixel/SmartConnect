package com.example.schoolmanagement;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import android.view.View;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

public class studentprofile extends AppCompatActivity {

    CircleImageView profileImage;
    TextView profileName, profileStd, profileStream;
    EditText profileMobile;
    Button blockBtn;
    boolean isBlocked = false;

    String targetUid;
    String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentprofile);

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileStd = findViewById(R.id.profileStd);
        profileStream = findViewById(R.id.profileStream);
        profileMobile = findViewById(R.id.profileMobile);
        blockBtn = findViewById(R.id.blockBtn);

        targetUid = getIntent().getStringExtra("uid");
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (targetUid == null || targetUid.isEmpty()) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Students").child(targetUid);

        // Fetch and display profile data
        ref.get().addOnSuccessListener(snapshot -> {
            String name = snapshot.child("Name").getValue(String.class);
            String std = snapshot.child("Standard").getValue(String.class);
            String stream = snapshot.child("Stream").getValue(String.class);
            String mobile = snapshot.child("Mobile").getValue(String.class);
            String base64Img = snapshot.child("ProfileImageBase64").getValue(String.class);

            profileName.setText("Name: " + (name != null ? name : ""));
            profileStd.setText("Standard: " + (std != null ? std : ""));
            profileStream.setText("Stream: " + (stream != null ? stream : ""));
            profileMobile.setText("Mobile: " + (mobile != null ? mobile : ""));

            if (base64Img != null && !base64Img.isEmpty()) {
                try {
                    byte[] imgBytes = android.util.Base64.decode(base64Img, android.util.Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    profileImage.setImageBitmap(bmp);
                } catch (Exception e) {
                    profileImage.setImageResource(R.drawable.ic_default_profile);
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_default_profile);
            }
        });

        // Check block/unblock status (simple demo: currentUser/Blocked/targetUid = true/false)
        DatabaseReference blockRef = FirebaseDatabase.getInstance().getReference("Blocked").child(myUid).child(targetUid);
        blockRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class)) {
                isBlocked = true;
                blockBtn.setText("Unblock");
            } else {
                isBlocked = false;
                blockBtn.setText("Block");
            }
        });

        blockBtn.setOnClickListener(v -> {
            isBlocked = !isBlocked;
            blockRef.setValue(isBlocked).addOnCompleteListener(task -> {
                if (isBlocked) {
                    blockBtn.setText("Unblock");
                    Toast.makeText(this, "User blocked. You can't message.", Toast.LENGTH_SHORT).show();
                } else {
                    blockBtn.setText("Block");
                    Toast.makeText(this, "User unblocked. You can message.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        // Add animations to UI elements
        animateUIElements();
    }
    
    private void animateUIElements() {
        // Animate toolbar
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            UIAnimator.animateToolbar(toolbar, 200);
        }
        
        // Animate profile image
        if (profileImage != null) {
            UIAnimator.animateImageView(profileImage, 400);
        }
        
        // Animate text views
        if (profileName != null) {
            UIAnimator.animateTextView(profileName, 500);
        }
        if (profileStd != null) {
            UIAnimator.animateTextView(profileStd, 600);
        }
        if (profileStream != null) {
            UIAnimator.animateTextView(profileStream, 700);
        }
        
        // Animate edit text
        if (profileMobile != null) {
            UIAnimator.animateEditText(profileMobile, 800);
        }
        
        // Animate button
        if (blockBtn != null) {
            UIAnimator.animateButton(blockBtn, 900);
        }
    }
}
