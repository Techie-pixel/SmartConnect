package com.example.schoolmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class loginchoice extends AppCompatActivity {

    Button btnStudentRegister, btnStudentLogin;
    Button btnAdminLogin, btnTeacherLogin, btnParentsLogin, btnPrincipleLogin;
    CardView cardStudent, cardAdmin, cardTeacher, cardParents, cardPrinciple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginchoice);

        // Buttons
        btnStudentRegister = findViewById(R.id.btnStudentRegister);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
        btnTeacherLogin = findViewById(R.id.btnTeacherLogin);
        btnParentsLogin = findViewById(R.id.btnParentsLogin);
        btnPrincipleLogin = findViewById(R.id.btnPrincipleLogin);

        // Cards
        cardStudent = findViewById(R.id.cardStudent);
        cardAdmin = findViewById(R.id.cardAdmin);
        cardTeacher = findViewById(R.id.cardTeacher);
        cardParents = findViewById(R.id.cardParents);
        cardPrinciple = findViewById(R.id.cardPrinciple);

        // Apply animations
        applyAnimations();

        // Student — Register
        btnStudentRegister.setOnClickListener(v -> {
            startActivity(new Intent(loginchoice.this, studentsignin.class));
        });

        // Student — Login
        btnStudentLogin.setOnClickListener(v -> {
            startActivity(new Intent(loginchoice.this, studentlogin.class));
        });

        // Admin — Login
        btnAdminLogin.setOnClickListener(v -> {
            startActivity(new Intent(loginchoice.this, AdminLoginActivity.class));
        });

        // Teacher — Login
        btnTeacherLogin.setOnClickListener(v -> {
            Intent intent = new Intent(loginchoice.this, loginteacher.class);
            intent.putExtra("loginRole", "teacher");
            startActivity(intent);
        });

        // Parents — Login
        btnParentsLogin.setOnClickListener(v -> {
            startActivity(new Intent(loginchoice.this, ParentLoginActivity.class));
        });

        // Principle — Login
        btnPrincipleLogin.setOnClickListener(v -> {
            Intent intent = new Intent(loginchoice.this, loginteacher.class);
            intent.putExtra("loginRole", "principal");
            startActivity(intent);
        });
    }

    private void applyAnimations() {
        CardView[] cards = { cardStudent, cardAdmin, cardTeacher, cardParents, cardPrinciple };
        Button[][] buttonGroups = {
                { btnStudentRegister, btnStudentLogin },
                { btnAdminLogin },
                { btnTeacherLogin },
                { btnParentsLogin },
                { btnPrincipleLogin }
        };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                // Initial visibility to invisible to prevent showing before animation
                cards[i].setVisibility(View.INVISIBLE);

                Animation cardAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                cardAnim.setStartOffset(i * 100L); // Stagger by 100ms

                int finalI = i;
                cardAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        cards[finalI].setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                cards[i].startAnimation(cardAnim);
            }

            for (Button btn : buttonGroups[i]) {
                if (btn != null) {
                    btn.setVisibility(View.INVISIBLE);

                    Animation btnAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                    btnAnim.setStartOffset((i * 100L) + 300L); // Button animates slightly after card

                    btnAnim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            btn.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });

                    btn.startAnimation(btnAnim);
                    addTouchAnimation(btn);
                }
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
            return false; // Let click listener fire
        });
    }
}