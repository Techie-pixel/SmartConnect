package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class admindashboard extends AppCompatActivity {

    TextView adminWelcomeText;
    CardView cardManageTeachers, cardManagePrincipal, cardManageParents, cardManageStudents, cardProfile, cardFeedback,
            cardEcommerce, cardFees, cardContactMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindashboard);

        // Start ChatNotificationService for feedback notifications
        Intent serviceIntent = new Intent(this, ChatNotificationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Views
        adminWelcomeText = findViewById(R.id.adminWelcomeText);
        cardManageTeachers = findViewById(R.id.cardManageTeachers);
        cardManagePrincipal = findViewById(R.id.cardManagePrincipal);
        cardManageParents = findViewById(R.id.cardManageParents);
        cardManageStudents = findViewById(R.id.cardManageStudents);
        cardProfile = findViewById(R.id.cardProfile);
        cardFeedback = findViewById(R.id.cardFeedback);
        cardEcommerce = findViewById(R.id.cardEcommerce);
        cardFees = findViewById(R.id.cardFees);
        cardContactMessages = findViewById(R.id.cardContactMessages);

        applyAnimations();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }

        ImageView mainLogo = findViewById(R.id.adminMainLogo);
        if (mainLogo != null) {
            UIAnimator.animateImageView(mainLogo, 100);
            UIAnimator.startPulseAnimation(mainLogo);
        }

        if (adminWelcomeText != null) {
            UIAnimator.animateTextView(adminWelcomeText, 200);
        }

        // --- Card Click Listeners ---
        cardManageTeachers.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, manageteachers.class)), 150);
        });

        cardManagePrincipal.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, ManagePrincipalActivity.class)), 150);
        });

        cardManageParents.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, ManageParentsActivity.class)), 150);
        });

        cardManageStudents.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, ManageStudentsActivity.class)), 150);
        });

        cardProfile.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, AdminProfileActivity.class)), 150);
        });

        cardFeedback.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, AdminFeedbackActivity.class)), 150);
        });

        cardEcommerce.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, AdminEcommerceActivity.class)), 150);
        });

        cardFees.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, AdminFeesActivity.class)), 150);
        });

        cardContactMessages.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> startActivity(new Intent(this, AdminContactMessagesActivity.class)), 150);
        });
    }

    private void applyAnimations() {
        CardView[] cards = {
                cardManageTeachers, cardManagePrincipal,
                cardManageParents, cardManageStudents,
                cardProfile, cardFeedback,
                cardEcommerce, cardFees,
                cardContactMessages
        };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].setVisibility(android.view.View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        cards[finalI].setVisibility(android.view.View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                cards[i].startAnimation(anim);
            }
        }
    }
}
