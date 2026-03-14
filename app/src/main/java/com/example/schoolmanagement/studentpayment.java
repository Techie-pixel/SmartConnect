package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class studentpayment extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private TextView tvTotalFees, tvPaidAmount, tvRemainingAmount, tvFeeStatus;
    private LinearLayout layoutPaymentHistory;

    private String studentName = "";
    private String studentUID = "";
    private String standard = "";
    private String stream = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentpayment);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        studentName = prefs.getString("studentName", "");
        studentUID = prefs.getString("studentId", "");
        standard = prefs.getString("Standard", "");
        stream = prefs.getString("Stream", "");

        // Setup nav header
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView usernameText = headerView.findViewById(R.id.username);
            if (usernameText != null) {
                usernameText.setText("Welcome, " + studentName);
            }
        }

        loadFeeStatus();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Animate toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            UIAnimator.animateToolbar(toolbarView, 200);
        }
        
        // Animate content after delay
        mainContent.postDelayed(() -> {
            if (tvTotalFees != null) {
                UIAnimator.animateTextView(tvTotalFees, 500);
            }
            if (tvPaidAmount != null) {
                UIAnimator.animateTextView(tvPaidAmount, 600);
            }
            if (tvRemainingAmount != null) {
                UIAnimator.animateTextView(tvRemainingAmount, 700);
            }
        }, 400);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        tvTotalFees = findViewById(R.id.tvTotalFees);
        tvPaidAmount = findViewById(R.id.tvPaidAmount);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        tvFeeStatus = findViewById(R.id.tvFeeStatus);
        layoutPaymentHistory = findViewById(R.id.layoutPaymentHistory);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Fee Status");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupNavigationDrawer() {
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if(toggle.getDrawerArrowDrawable() != null) {
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        }
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void loadFeeStatus() {
        if (studentName.isEmpty()) {
            Toast.makeText(this, "Student name not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Also load fee structure for total
        String key = standard + "_" + stream;
        FirebaseDatabase.getInstance().getReference("Fees").child("FeeStructure").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long totalAmount = snapshot.child("amount").getValue(Long.class);
                        long total = totalAmount != null ? totalAmount : 0;
                        tvTotalFees.setText("₹ " + total);

                        // Find the parent linked to this student, then load their payments
                        findParentAndLoadPayments(total);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void findParentAndLoadPayments(long totalFees) {
        DatabaseReference parentsRef = FirebaseDatabase.getInstance().getReference("Parents");
        parentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String linkedParentKey = "";
                for (DataSnapshot parent : snapshot.getChildren()) {
                    // Match by studentUID first
                    String pStudentUID = parent.child("studentUID").getValue(String.class);
                    if (pStudentUID != null && !pStudentUID.isEmpty()
                            && studentUID != null && !studentUID.isEmpty()
                            && pStudentUID.equals(studentUID)) {
                        linkedParentKey = parent.getKey();
                        break;
                    }
                    // Fallback: match by studentName
                    String pStudentName = parent.child("studentName").getValue(String.class);
                    if (pStudentName != null && studentName != null
                            && pStudentName.trim().equalsIgnoreCase(studentName.trim())) {
                        linkedParentKey = parent.getKey();
                        // Don't break; keep looking for a UID match which is more accurate
                    }
                }

                if (!linkedParentKey.isEmpty()) {
                    loadPaymentsForParent(linkedParentKey, totalFees);
                } else {
                    // No parent found for this student – show unpaid
                    tvPaidAmount.setText("₹ 0");
                    tvRemainingAmount.setText("₹ " + totalFees);
                    tvFeeStatus.setText("❌ Unpaid");
                    tvFeeStatus.setBackgroundColor(0xFFD32F2F);

                    TextView tv = new TextView(studentpayment.this);
                    tv.setText("No payment records found.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(13);
                    layoutPaymentHistory.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadPaymentsForParent(String parentKey, long totalFees) {
        DatabaseReference paymentsRef = FirebaseDatabase.getInstance()
                .getReference("Fees").child("Payments").child(parentKey);

        paymentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long paid = 0;
                layoutPaymentHistory.removeAllViews();
                boolean found = false;

                for (DataSnapshot payment : snapshot.getChildren()) {
                    Long amt = payment.child("amountPaid").getValue(Long.class);
                    if (amt != null)
                        paid += amt;
                    found = true;
                    addPaymentHistoryItem(payment);
                }

                long remaining = totalFees - paid;
                if (remaining < 0)
                    remaining = 0;

                tvPaidAmount.setText("₹ " + paid);
                tvRemainingAmount.setText("₹ " + remaining);

                if (remaining == 0 && totalFees > 0) {
                    tvFeeStatus.setText("✅ Paid");
                    tvFeeStatus.setBackgroundColor(0xFF388E3C);
                } else if (paid > 0) {
                    tvFeeStatus.setText("⏳ Partial");
                    tvFeeStatus.setBackgroundColor(0xFFFF8F00);
                } else {
                    tvFeeStatus.setText("❌ Unpaid");
                    tvFeeStatus.setBackgroundColor(0xFFD32F2F);
                }

                if (!found) {
                    TextView tv = new TextView(studentpayment.this);
                    tv.setText("No payment records found.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(13);
                    layoutPaymentHistory.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addPaymentHistoryItem(DataSnapshot payment) {
        String date = payment.child("date").getValue(String.class);
        Long amt = payment.child("amountPaid").getValue(Long.class);
        String type = payment.child("paymentType").getValue(String.class);

        TextView tv = new TextView(this);
        tv.setText("• " + (date != null ? date : "N/A") + "  |  ₹" +
                (amt != null ? amt : 0) + "  |  " + (type != null ? type : ""));
        tv.setTextColor(0xFFCCCCCC);
        tv.setTextSize(13);
        tv.setPadding(0, 6, 0, 6);
        layoutPaymentHistory.addView(tv);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

        @Override
    public boolean onNavigationItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        android.content.Intent intent = null;

        if (id == R.id.homesection) {
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            if ("11".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, elevensciencehomepage.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, elevencommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, elevenartshome.class);
            } else if ("12".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, twelvesciencehome.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, twelvecommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, twelveartshome.class);
            }
            if (intent == null) intent = new android.content.Intent(this, elevencommercehome.class);
        } else if (id == R.id.profile) {
            intent = new android.content.Intent(this, userprofile.class);
        } else if (id == R.id.feedback) {
            intent = new android.content.Intent(this, studentfeedback.class);
        } else if (id == R.id.gallery) {
            intent = new android.content.Intent(this, studentgallery.class);
        } else if (id == R.id.syllabus) {
            intent = new android.content.Intent(this, studentsyllabus.class);
        } else if (id == R.id.assignment) {
            intent = new android.content.Intent(this, studentassignment.class);
        } else if (id == R.id.exam) {
            intent = new android.content.Intent(this, studentexam.class);
        } else if (id == R.id.calender) {
            intent = new android.content.Intent(this, studentcalender.class);
        } else if (id == R.id.payment) {
            intent = new android.content.Intent(this, studentpayment.class);
        } else if (id == R.id.table) {
            intent = new android.content.Intent(this, studenttimetable.class);
        } else if (id == R.id.homework) {
            intent = new android.content.Intent(this, studenthomework.class);
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            intent.putExtra("singleCategory", true);
            intent.putExtra("std", std);
            intent.putExtra("stream", stream);
        } else if (id == R.id.student) {
            intent = new android.content.Intent(this, studentslist.class);
        } else if (id == R.id.student_attendance) {
            intent = new android.content.Intent(this, studentattendance.class);
        } else if (id == R.id.contact_admin) {
            intent = new android.content.Intent(this, ContactAdminActivity.class);
            intent.putExtra("senderRole", "Student");
            String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentUid", "unknown");
            intent.putExtra("senderUid", uid);
        }

        // Avoid opening the exact same intent class
        if (intent != null && intent.getComponent() != null && this.getClass().getName().equals(intent.getComponent().getClassName())) {
            android.widget.Toast.makeText(this, "Already on this page", android.widget.Toast.LENGTH_SHORT).show();
            if (drawerLayout != null) drawerLayout.closeDrawers();
            return true;
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            if (!this.getClass().getSimpleName().contains("home") && !this.getClass().getSimpleName().contains("homepage")) {
                finish();
            }
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        return true;
    }
@Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}