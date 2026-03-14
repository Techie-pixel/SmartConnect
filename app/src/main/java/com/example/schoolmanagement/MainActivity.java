package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView schoolLogo = findViewById(R.id.school);
        Animation scaleUpAndFade = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        schoolLogo.startAnimation(scaleUpAndFade);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // 0) ADMIN SESSION CHECK
            SharedPreferences adminPrefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
            boolean isAdminLoggedIn = adminPrefs.getBoolean("isAdminLoggedIn", false);
            if (isAdminLoggedIn) {
                startActivity(new Intent(MainActivity.this, admindashboard.class));
                finish();
                return;
            }

            // 1) PRINCIPAL SESSION CHECK
            SharedPreferences principalPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
            boolean isPrincipalLoggedIn = principalPrefs.getBoolean("isPrincipalLoggedIn", false);
            String principalId = principalPrefs.getString("principalId", null);

            if (isPrincipalLoggedIn && principalId != null && !principalId.isEmpty()) {
                Intent pIntent = new Intent(MainActivity.this, principaltab.class);
                pIntent.putExtra("principalId", principalId);
                startActivity(pIntent);
                finish();
                return;
            }

            // 2) TEACHER SESSION CHECK
            SharedPreferences teacherPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
            boolean isTeacherLoggedIn = teacherPrefs.getBoolean("isTeacherLoggedIn", false);
            String savedTeacherId = teacherPrefs.getString("teacherId", null);

            if (isTeacherLoggedIn && savedTeacherId != null && !savedTeacherId.isEmpty()) {
                String savedStd = teacherPrefs.getString("teacherStandard", null);
                Intent tIntent = new Intent(MainActivity.this, teachertab.class);
                tIntent.putExtra("teacherId", savedTeacherId);
                startActivity(tIntent);
                finish();
                return;
            }

            // 3) STUDENT SESSION CHECK
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

            if (isLoggedIn) {
                String standard = prefs.getString("Standard", null);
                String stream = prefs.getString("Stream", null);
                if (standard != null && stream != null) {
                    redirectUser(standard, stream);
                    return;
                }
            }

            // 4) PARENT SESSION CHECK
            SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
            boolean isParentLoggedIn = parentPrefs.getBoolean("isParentLoggedIn", false);
            String parentKey = parentPrefs.getString("parentKey", "");
            if (isParentLoggedIn && !parentKey.isEmpty()) {
                Intent parentIntent = new Intent(MainActivity.this, ParentDashboardActivity.class);
                startActivity(parentIntent);
                finish();
                return;
            } else if (isParentLoggedIn) {
                // Session exists but is broken (missing parentKey), clear it
                parentPrefs.edit().clear().apply();
            }

            // 5) Firebase Auth student check
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Students");
                dbRef.child(currentUser.getUid()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DataSnapshot snapshot = task.getResult();
                        String standard = snapshot.child("Standard").getValue(String.class);
                        String stream = snapshot.child("Stream").getValue(String.class);

                        prefs.edit()
                                .putString("Standard", standard)
                                .putString("Stream", stream)
                                .putBoolean("isLoggedIn", true)
                                .apply();

                        redirectUser(standard, stream);
                    } else {
                        startActivity(new Intent(MainActivity.this, loginchoice.class));
                        finish();
                    }
                });
            } else {
                startActivity(new Intent(MainActivity.this, loginchoice.class));
                finish();
            }

        }, 2000); // 2 sec splash
    }

    private void redirectUser(String standard, String stream) {
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
            intent = new Intent(this, loginchoice.class);
        }
        startActivity(intent);
        finish();
    }
}
