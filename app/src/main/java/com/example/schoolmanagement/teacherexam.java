package com.example.schoolmanagement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class teacherexam extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private LinearLayout examContainer;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private DatabaseReference teacherExamRef;

    private static class ExamData {
        String id;
        String examInfo;
        String examType;
        String standard;
        String streams;
        String image;
        long timestamp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherexam);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Exam");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        examContainer = findViewById(R.id.examContainer);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        teacherExamRef = FirebaseDatabase.getInstance().getReference("Exam").child("TeacherExam");
        loadExams();
    }

    private void loadExams() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        teacherExamRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ExamData> exams = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ExamData ed = new ExamData();
                    ed.id = child.getKey();
                    ed.examInfo = child.child("examInfo").getValue(String.class);
                    ed.examType = child.child("examType").getValue(String.class);
                    ed.standard = child.child("standard").getValue(String.class);
                    ed.streams = child.child("streams").getValue(String.class);
                    ed.image = child.child("image").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    ed.timestamp = ts != null ? ts : 0;
                    exams.add(ed);
                }
                exams.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                progressBar.setVisibility(View.GONE);
                renderExams(exams);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(teacherexam.this, "Error loading exams", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderExams(List<ExamData> exams) {
        examContainer.removeAllViews();

        if (exams.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (ExamData ed : exams) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_exam_card, examContainer, false);

            TextView tvExamType = card.findViewById(R.id.tvExamType);
            TextView tvExamDate = card.findViewById(R.id.tvExamDate);
            TextView tvExamInfo = card.findViewById(R.id.tvExamInfo);
            ImageView ivExamImage = card.findViewById(R.id.ivExamImage);
            Button btnDownload = card.findViewById(R.id.btnDownload);
            // Hide mark-as-read for teachers
            LinearLayout llReadAction = card.findViewById(R.id.llReadAction);
            llReadAction.setVisibility(View.GONE);

            String typeText = (ed.examType != null ? ed.examType : "Exam");
            if (ed.standard != null && !ed.standard.isEmpty()) {
                typeText += " • Std " + ed.standard;
            }
            if (ed.streams != null && !ed.streams.isEmpty()) {
                typeText += " • " + ed.streams;
            }
            tvExamType.setText(typeText);
            tvExamDate.setText(sdf.format(new Date(ed.timestamp)));

            if (ed.examInfo != null && !ed.examInfo.isEmpty()) {
                tvExamInfo.setText(ed.examInfo);
                tvExamInfo.setVisibility(View.VISIBLE);
            }

            if (ed.image != null && !ed.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(ed.image, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        ivExamImage.setImageBitmap(bmp);
                        ivExamImage.setVisibility(View.VISIBLE);
                        btnDownload.setVisibility(View.VISIBLE);
                        btnDownload.setOnClickListener(v -> {
                            UIAnimator.animateClick(v);
                            v.postDelayed(() -> saveExamImage(ed.image), 150);
                        });
                    }
                } catch (Exception e) {
                    ivExamImage.setVisibility(View.GONE);
                    btnDownload.setVisibility(View.GONE);
                }
            } else {
                ivExamImage.setVisibility(View.GONE);
                btnDownload.setVisibility(View.GONE);
            }

            examContainer.addView(card);
        }
    }

    private void saveExamImage(String base64Image) {
        try {
            byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bitmap != null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "Exam_" + timeStamp + ".jpg";
                File storageDir = new File(getExternalFilesDir(null), "Exams");
                if (!storageDir.exists())
                    storageDir.mkdirs();
                File imageFile = new File(storageDir, imageFileName);
                OutputStream fOut = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
                Toast.makeText(this, "Image saved to: " + imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.teacher_home) {
            startActivity(new Intent(this, teachertab.class));
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            startActivity(new Intent(this, teacherhomework.class));
        } else if (id == R.id.teacher_syllabus) {
            startActivity(new Intent(this, teachersyllabus.class));
        } else if (id == R.id.teacher_timetable) {
            startActivity(new Intent(this, teachertimetable.class));
        } else if (id == R.id.teacher_exam) {
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_feedback) {
            startActivity(new Intent(this, teacherfeedback.class));
        } else if (id == R.id.teacher_notices) {
            startActivity(new Intent(this, TeacherNoticesActivity.class));
        } else if (id == R.id.teacher_fees) {
            startActivity(new Intent(this, TeacherFeesActivity.class));
        } else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            caIntent.putExtra("senderUid", getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown"));
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
