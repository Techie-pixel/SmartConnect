package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PrincipalPaymentActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private TextView tvTotalCollected, tvPendingAmount, tvPendingCount;
    private LinearLayout layoutClassWise, layoutMonthly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_payment);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Fee Reports");
        }

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        tvTotalCollected = findViewById(R.id.tvTotalCollected);
        tvPendingAmount = findViewById(R.id.tvPendingAmount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        layoutClassWise = findViewById(R.id.layoutClassWise);
        layoutMonthly = findViewById(R.id.layoutMonthly);

        // Apply animations
        UIAnimator.animateToolbar(toolbar, 100);
        ImageView logoImg = findViewById(R.id.logoImg);
        if (logoImg != null) UIAnimator.animateImageView(logoImg, 200);
        UIAnimator.animateTextView(tvTotalCollected, 350);
        UIAnimator.animateTextView(tvPendingAmount, 500);
        UIAnimator.animateTextView(tvPendingCount, 650);
        UIAnimator.animateLayout(layoutClassWise, 800);
        UIAnimator.animateLayout(layoutMonthly, 950);

        loadReports();
    }

    private void loadReports() {
        DatabaseReference paymentsRef = FirebaseDatabase.getInstance()
                .getReference("Fees").child("Payments");
        DatabaseReference feeStructureRef = FirebaseDatabase.getInstance()
                .getReference("Fees").child("FeeStructure");

        feeStructureRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot feeSnap) {
                // Build fee structure map
                Map<String, Long> feeStructure = new HashMap<>();
                for (DataSnapshot child : feeSnap.getChildren()) {
                    Long amount = child.child("amount").getValue(Long.class);
                    feeStructure.put(child.getKey(), amount != null ? amount : 0);
                }

                // Now load payments
                paymentsRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalCollected = 0;
                        Map<String, Long> classWiseCollected = new TreeMap<>();
                        Map<String, Long> monthlyIncome = new TreeMap<>();
                        // Track per-student totals for pending calculation
                        Map<String, Long> studentPaidMap = new HashMap<>();
                        Map<String, String> studentClassMap = new HashMap<>();

                        for (DataSnapshot parentSnap : snapshot.getChildren()) {
                            for (DataSnapshot payment : parentSnap.getChildren()) {
                                Long amt = payment.child("amountPaid").getValue(Long.class);
                                long amount = amt != null ? amt : 0;
                                totalCollected += amount;

                                String sClass = payment.child("studentClass").getValue(String.class);
                                String sStream = payment.child("studentStream").getValue(String.class);
                                String sName = payment.child("studentName").getValue(String.class);
                                String date = payment.child("date").getValue(String.class);

                                // Class-wise
                                String classKey = (sClass != null ? sClass : "?") + " " +
                                        (sStream != null ? sStream : "?");
                                long prev = classWiseCollected.containsKey(classKey) ? classWiseCollected.get(classKey)
                                        : 0;
                                classWiseCollected.put(classKey, prev + amount);

                                // Monthly (extract month-year from date dd-MM-yyyy)
                                if (date != null && date.length() >= 10) {
                                    String monthYear = date.substring(3, 10); // MM-yyyy
                                    long mp = monthlyIncome.containsKey(monthYear) ? monthlyIncome.get(monthYear) : 0;
                                    monthlyIncome.put(monthYear, mp + amount);
                                }

                                // Per student total
                                if (sName != null) {
                                    long sp = studentPaidMap.containsKey(sName) ? studentPaidMap.get(sName) : 0;
                                    studentPaidMap.put(sName, sp + amount);
                                    String feeKey = (sClass != null ? sClass : "") + "_" +
                                            (sStream != null ? sStream : "");
                                    studentClassMap.put(sName, feeKey);
                                }
                            }
                        }

                        tvTotalCollected.setText("₹ " + totalCollected);

                        // Calculate pending
                        long pendingAmount = 0;
                        int pendingCount = 0;
                        for (Map.Entry<String, Long> entry : studentPaidMap.entrySet()) {
                            String feeKey = studentClassMap.get(entry.getKey());
                            long totalFee = feeStructure.containsKey(feeKey) ? feeStructure.get(feeKey) : 0;
                            long remaining = totalFee - entry.getValue();
                            if (remaining > 0) {
                                pendingAmount += remaining;
                                pendingCount++;
                            }
                        }
                        tvPendingAmount.setText("₹ " + pendingAmount);
                        tvPendingCount.setText(pendingCount + " students with pending fees");

                        // Class-wise layout
                        layoutClassWise.removeAllViews();
                        if (classWiseCollected.isEmpty()) {
                            addTextTo(layoutClassWise, "No data yet.", 0xFF888888);
                        }
                        for (Map.Entry<String, Long> e : classWiseCollected.entrySet()) {
                            addClassWiseItem(e.getKey(), e.getValue());
                        }

                        // Monthly layout
                        layoutMonthly.removeAllViews();
                        if (monthlyIncome.isEmpty()) {
                            addTextTo(layoutMonthly, "No data yet.", 0xFF888888);
                        }
                        for (Map.Entry<String, Long> e : monthlyIncome.entrySet()) {
                            addMonthlyItem(e.getKey(), e.getValue());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addClassWiseItem(String className, long amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView tvName = new TextView(this);
        tvName.setText("Class " + className);
        tvName.setTextColor(0xFFCCCCCC);
        tvName.setTextSize(14);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvAmt = new TextView(this);
        tvAmt.setText("₹ " + amount);
        tvAmt.setTextColor(0xFF66BB6A);
        tvAmt.setTextSize(14);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvName);
        row.addView(tvAmt);
        layoutClassWise.addView(row);
    }

    private void addMonthlyItem(String monthYear, long amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView tvMonth = new TextView(this);
        tvMonth.setText(monthYear);
        tvMonth.setTextColor(0xFFCCCCCC);
        tvMonth.setTextSize(14);
        tvMonth.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvAmt = new TextView(this);
        tvAmt.setText("₹ " + amount);
        tvAmt.setTextColor(0xFFFFAB40);
        tvAmt.setTextSize(14);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvMonth);
        row.addView(tvAmt);
        layoutMonthly.addView(row);
    }

    private void addTextTo(LinearLayout layout, String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(13);
        layout.addView(tv);
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
        if (id == R.id.principal_home) {
            startActivity(new Intent(this, principaltab.class));
        } else if (id == R.id.principal_timetable) {
            startActivity(new Intent(this, principletimetable.class));
        } else if (id == R.id.principal_exam) {
            startActivity(new Intent(this, PrincipalExamActivity.class));
        } else if (id == R.id.principal_notices) {
            startActivity(new Intent(this, PrincipalNoticesActivity.class));
        } else if (id == R.id.principal_calender) {
            startActivity(new Intent(this, PrincipalCalendarActivity.class));
        } else if (id == R.id.principal_payment) {
            // current
        } else if (id == R.id.principal_feedback) {
            startActivity(new Intent(this, principalfeedback.class));
        } else if (id == R.id.principal_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Principal");
            String principalId = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE).getString("principalId", "unknown");
            caIntent.putExtra("senderUid", principalId);
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
