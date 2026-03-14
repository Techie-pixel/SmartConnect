package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

public class ManagePrincipalActivity extends AppCompatActivity {
    EditText etPrincipalId, etPrincipalName, etPrincipalEmail, etPrincipalMobile;
    Button btnCreatePrincipal, btnDeletePrincipal;
    DatabaseReference principalsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_principal);

        etPrincipalId = findViewById(R.id.etPrincipalId);
        etPrincipalName = findViewById(R.id.etPrincipalName);
        etPrincipalEmail = findViewById(R.id.etPrincipalEmail);
        etPrincipalMobile = findViewById(R.id.etPrincipalMobile);
        btnCreatePrincipal = findViewById(R.id.btnCreatePrincipal);
        btnDeletePrincipal = findViewById(R.id.btnDeletePrincipal);

        principalsRef = FirebaseDatabase.getInstance().getReference("Principals");

        btnCreatePrincipal.setOnClickListener(v -> createPrincipal());
        btnDeletePrincipal.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, DeletePrincipalActivity.class)));

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = {
                etPrincipalId, etPrincipalName, etPrincipalEmail, etPrincipalMobile,
                btnCreatePrincipal, btnDeletePrincipal
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

    private void createPrincipal() {
        String id = etPrincipalId.getText().toString().trim();
        String name = etPrincipalName.getText().toString().trim();
        String email = etPrincipalEmail.getText().toString().trim();
        String mobile = etPrincipalMobile.getText().toString().trim();

        if (id.length() < 6) {
            etPrincipalId.setError("Min 6 characters");
            etPrincipalId.requestFocus();
            return;
        }
        if (name.isEmpty()) {
            etPrincipalName.setError("Enter name");
            etPrincipalName.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etPrincipalEmail.setError("Enter valid email");
            etPrincipalEmail.requestFocus();
            return;
        }
        if (mobile.length() < 10) {
            etPrincipalMobile.setError("Enter valid mobile");
            etPrincipalMobile.requestFocus();
            return;
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("principalId", id);
        data.put("name", name);
        data.put("email", email);
        data.put("mobile", mobile);

        principalsRef.child(id).setValue(data)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Principal created", Toast.LENGTH_SHORT).show();
                    sendPrincipalEmail(id, name, email, mobile);
                    clear();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clear() {
        etPrincipalId.setText("");
        etPrincipalName.setText("");
        etPrincipalEmail.setText("");
        etPrincipalMobile.setText("");
    }

    private void sendPrincipalEmail(String id, String name, String email, String mobile) {
        String subject = "Principal Account Created";
        String body = "Dear " + name + ",\n\n" +
                "Welcome to Smartconnect!\n\n" +
                "Your principal account has been created with the following details:\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "PRINCIPAL DETAILS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "ID: " + id + "\n" +
                "Name: " + name + "\n" +
                "Email: " + email + "\n" +
                "Mobile: " + mobile + "\n\n" +
                "Keep these details safe. If you have any issues, contact the admin.\n\n" +
                "Best Regards,\n" +
                "Smartconnect";

        new Thread(() -> {
            try {
                GmailSender.sendMailWithSubject(email, subject, body);
                runOnUiThread(() -> Toast.makeText(this, "Confirmation email sent", Toast.LENGTH_SHORT).show());
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this, "Email failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
