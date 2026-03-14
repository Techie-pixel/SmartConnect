package com.example.schoolmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherFeesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private RecyclerView recyclerStudents;
    private TextView tvClassHeader, tvSummary;

    private String teacherStandard = "";
    private String teacherStream = "";

    private List<Map<String, String>> studentFeeList = new ArrayList<>();
    private StudentFeeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_fees);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Student Fees");
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        tvClassHeader = findViewById(R.id.tvClassHeader);
        tvSummary = findViewById(R.id.tvSummary);
        recyclerStudents = findViewById(R.id.recyclerStudents);
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentFeeAdapter();
        recyclerStudents.setAdapter(adapter);

        SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        teacherStandard = tPrefs.getString("teacherStandard", "");
        teacherStream = tPrefs.getString("teacherStream", "");

        tvClassHeader.setText("Fee Status - Class " + teacherStandard + " " + teacherStream);
        tvSummary.setText("Loading...");

        // Apply animations
        android.view.View ivLogo = findViewById(R.id.ivLogo);
        if (ivLogo != null) {
            UIAnimator.animateImageView(ivLogo, 100);
        }
        UIAnimator.animateTextView(tvClassHeader, 200);
        UIAnimator.animateTextView(tvSummary, 300);

        loadStudentsAndFees();
    }

    private void loadStudentsAndFees() {
        // First get all students matching teacher's class
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> classStudentNames = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String std = child.child("Standard").getValue(String.class);
                    String strm = child.child("Stream").getValue(String.class);
                    if (teacherStandard.equals(std) && teacherStream.equals(strm)) {
                        String name = child.child("Name").getValue(String.class);
                        if (name == null)
                            name = child.child("name").getValue(String.class);
                        if (name != null)
                            classStudentNames.add(name);
                    }
                }

                if (classStudentNames.isEmpty()) {
                    tvSummary.setText("No students found in your class.");
                    return;
                }

                // Now check payments
                loadPaymentsForStudents(classStudentNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvSummary.setText("Error loading students.");
            }
        });
    }

    private void loadPaymentsForStudents(List<String> studentNames) {
        DatabaseReference paymentsRef = FirebaseDatabase.getInstance()
                .getReference("Fees").child("Payments");

        paymentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentFeeList.clear();
                int paidCount = 0;
                int unpaidCount = 0;

                // Build payment totals per student
                Map<String, Long> paidMap = new HashMap<>();
                for (DataSnapshot parentSnap : snapshot.getChildren()) {
                    for (DataSnapshot payment : parentSnap.getChildren()) {
                        String sName = payment.child("studentName").getValue(String.class);
                        String sClass = payment.child("studentClass").getValue(String.class);
                        String sStream = payment.child("studentStream").getValue(String.class);
                        if (sName != null && teacherStandard.equals(sClass)
                                && teacherStream.equals(sStream)) {
                            Long amt = payment.child("amountPaid").getValue(Long.class);
                            long prev = paidMap.containsKey(sName) ? paidMap.get(sName) : 0;
                            paidMap.put(sName, prev + (amt != null ? amt : 0));
                        }
                    }
                }

                // Load fee structure to determine status
                String feeKey = teacherStandard + "_" + teacherStream;
                FirebaseDatabase.getInstance().getReference("Fees").child("FeeStructure").child(feeKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot feeSnap) {
                                Long totalAmount = feeSnap.child("amount").getValue(Long.class);
                                long total = totalAmount != null ? totalAmount : 0;

                                int paid = 0, unpaid = 0;
                                for (String name : studentNames) {
                                    Map<String, String> item = new HashMap<>();
                                    item.put("name", name);
                                    item.put("class", teacherStandard + " " + teacherStream);

                                    long paidAmt = paidMap.containsKey(name) ? paidMap.get(name) : 0;
                                    if (paidAmt >= total && total > 0) {
                                        item.put("status", "Paid");
                                        paid++;
                                    } else if (paidAmt > 0) {
                                        item.put("status", "Partial");
                                        unpaid++;
                                    } else {
                                        item.put("status", "Unpaid");
                                        unpaid++;
                                    }
                                    studentFeeList.add(item);
                                }
                                adapter.notifyDataSetChanged();
                                tvSummary.setText("Total: " + studentNames.size() +
                                        "  |  Paid: " + paid + "  |  Pending: " + unpaid);
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

    // ── RecyclerView Adapter ──
    private class StudentFeeAdapter extends RecyclerView.Adapter<StudentFeeAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_teacher_fee_student, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, String> item = studentFeeList.get(position);
            holder.tvStudentName.setText(item.get("name"));
            holder.tvClassName.setText("Class " + item.get("class"));

            String status = item.get("status");
            holder.tvStatus.setText(status);
            if ("Paid".equals(status)) {
                holder.tvStatus.setBackgroundColor(0xFF388E3C);
            } else if ("Partial".equals(status)) {
                holder.tvStatus.setBackgroundColor(0xFFFF8F00);
            } else {
                holder.tvStatus.setBackgroundColor(0xFFD32F2F);
            }
        }

        @Override
        public int getItemCount() {
            return studentFeeList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvClassName, tvStatus;

            VH(@NonNull View v) {
                super(v);
                tvStudentName = v.findViewById(R.id.tvStudentName);
                tvClassName = v.findViewById(R.id.tvClassName);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }

    // ── Navigation ──
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
            startActivity(new Intent(this, teacherexam.class));
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_feedback) {
            startActivity(new Intent(this, teacherfeedback.class));
        } else if (id == R.id.teacher_notices) {
            startActivity(new Intent(this, TeacherNoticesActivity.class));
        } else if (id == R.id.teacher_fees) {
            drawerLayout.closeDrawers();
            return true;
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
