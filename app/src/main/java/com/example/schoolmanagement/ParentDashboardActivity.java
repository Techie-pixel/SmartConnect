package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ParentDashboardActivity extends AppCompatActivity {

        ViewFlipper viewFlipper;
        MaterialCardView cardAttendance, cardFees, cardExam, cardTimetable, cardProfile, cardFeedback, cardNotice,
                        cardSyllabus, cardContactAdmin;
        TextView tvWelcomeParent;
        ImageView logoImg;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_parent_dashboard);

                logoImg = findViewById(R.id.logoImg);
                viewFlipper = findViewById(R.id.viewFlipper);
                cardAttendance = findViewById(R.id.cardAttendance);
                cardFees = findViewById(R.id.cardFees);
                cardExam = findViewById(R.id.cardExam);
                cardTimetable = findViewById(R.id.cardTimetable);
                cardProfile = findViewById(R.id.cardProfile);
                cardFeedback = findViewById(R.id.cardFeedback);
                cardNotice = findViewById(R.id.cardNotice);
                cardSyllabus = findViewById(R.id.cardSyllabus);
                cardContactAdmin = findViewById(R.id.cardContactAdmin);
                tvWelcomeParent = findViewById(R.id.tvWelcomeParent);

                applyAnimations();

                viewFlipper.startFlipping();

                // Show parent name
                SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
                String parentName = parentPrefs.getString("parentName", "Parent");
                if (tvWelcomeParent != null)
                        tvWelcomeParent.setText("Welcome, " + parentName + "!");

                cardAttendance.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(new Intent(ParentDashboardActivity.this,
                                                        ParentAttendanceActivity.class)), 150);
                                });

                cardFees.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(
                                                        new Intent(ParentDashboardActivity.this, ParentFeesActivity.class)),
                                                        150);
                                });

                cardExam.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(
                                                        new Intent(ParentDashboardActivity.this, ParentExamActivity.class)),
                                                        150);
                                });

                cardTimetable.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(new Intent(ParentDashboardActivity.this,
                                                        ParentTimetableActivity.class)), 150);
                                });

                cardFeedback.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(new Intent(ParentDashboardActivity.this,
                                                        ParentFeedbackActivity.class)), 150);
                                });

                cardNotice.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(
                                                        new Intent(ParentDashboardActivity.this,
                                                                        ParentNoticesActivity.class)),
                                                        150);
                                });

                cardSyllabus.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(new Intent(ParentDashboardActivity.this,
                                                        ParentSyllabusActivity.class)), 150);
                                });

                cardProfile.setOnClickListener(
                                v -> {
                                        UIAnimator.animateClick(v);
                                        v.postDelayed(() -> startActivity(
                                                        new Intent(ParentDashboardActivity.this,
                                                                        ParentProfileActivity.class)),
                                                        150);
                                });

                cardContactAdmin.setOnClickListener(v -> {
                        UIAnimator.animateClick(v);
                        v.postDelayed(() -> {
                                Intent caIntent = new Intent(ParentDashboardActivity.this, ContactAdminActivity.class);
                                caIntent.putExtra("senderRole", "Parent");
                                String parentId = parentPrefs.getString("parentKey", "unknown");
                                caIntent.putExtra("senderUid", parentId);
                                startActivity(caIntent);
                        }, 150);
                });
        }

        private void applyAnimations() {
                if (logoImg != null)
                        UIAnimator.animateImageView(logoImg, 100);
                if (tvWelcomeParent != null)
                        UIAnimator.animateTextView(tvWelcomeParent, 200);

                MaterialCardView[] cards = {
                                cardAttendance, cardFees, cardExam, cardTimetable,
                                cardProfile, cardFeedback, cardNotice, cardSyllabus, cardContactAdmin
                };

                for (int i = 0; i < cards.length; i++) {
                        if (cards[i] != null) {
                                UIAnimator.animateCard(cards[i], 300 + (i * 100));
                        }
                }
        }
}