package com.example.schoolmanagement;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ParentAttendanceActivity extends AppCompatActivity {

    private EditText etStudentId;
    private Spinner spinnerStandard, spinnerStream;
    private MaterialCardView cardSummary, cardSearch;
    private TextView tvStudentNameResult, tvSumPresent, tvSumAbsent, tvSumLate, tvSumPct, tvRecordsHeader;
    private RecyclerView rvDateRecords;
    private DateRecordAdapter adapter;
    private final List<DateRecordAdapter.DateRecord> records = new ArrayList<>();
    private ImageView logoImg;

    private String selectedStandard = "", selectedStream = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_attendance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Attendance");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        logoImg = findViewById(R.id.logoImg);
        etStudentId = findViewById(R.id.etStudentId);
        spinnerStandard = findViewById(R.id.spinnerStandard);
        spinnerStream = findViewById(R.id.spinnerStream);
        cardSummary = findViewById(R.id.cardSummary);
        cardSearch = findViewById(R.id.cardSearch); // Ensure this ID is added in XML or use layout reference
        tvStudentNameResult = findViewById(R.id.tvStudentNameResult);
        tvSumPresent = findViewById(R.id.tvSumPresent);
        tvSumAbsent = findViewById(R.id.tvSumAbsent);
        tvSumLate = findViewById(R.id.tvSumLate);
        tvSumPct = findViewById(R.id.tvSumPct);
        tvRecordsHeader = findViewById(R.id.tvRecordsHeader);
        rvDateRecords = findViewById(R.id.rvDateRecords);

        rvDateRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DateRecordAdapter(records, this);
        rvDateRecords.setAdapter(adapter);

        setupSpinners();

        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            searchAttendance();
        });

        applyAnimations(toolbar, btnSearch);
    }

    private void applyAnimations(Toolbar toolbar, MaterialButton btnSearch) {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);

        View[] textViews = { tvRecordsHeader, tvStudentNameResult };
        View[] imageViews = { logoImg };
        // We'll use animateActivity but customized if needed, or just manual
        UIAnimator.animateEditText(etStudentId, 300);
        UIAnimator.animateImageView(spinnerStandard, 400);
        UIAnimator.animateImageView(spinnerStream, 500);
        UIAnimator.animateButton(btnSearch, 600);
    }

    private void setupSpinners() {
        String[] standards = { "Select Standard", "11", "12" };
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, standards);
        sa.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStandard.setAdapter(sa);
        spinnerStandard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedStandard = pos > 0 ? standards[pos] : "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
                selectedStandard = "";
            }
        });

        String[] streams = { "Select Stream", "Science", "Commerce", "Arts" };
        ArrayAdapter<String> st = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, streams);
        st.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerStream.setAdapter(st);
        spinnerStream.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedStream = pos > 0 ? streams[pos] : "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
                selectedStream = "";
            }
        });
    }

    private void searchAttendance() {
        String query = etStudentId.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter student name or ID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStandard.isEmpty() || selectedStream.isEmpty()) {
            Toast.makeText(this, "Select standard and stream", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = selectedStandard + "_" + selectedStream;

        // First try direct ID lookup, then fallback to name search
        FirebaseDatabase.getInstance().getReference("Students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String foundId = null, foundName = null;
                        for (DataSnapshot stSnap : snapshot.getChildren()) {
                            String std = stSnap.child("Standard").getValue(String.class);
                            String stream = stSnap.child("Stream").getValue(String.class);

                            if (selectedStandard.equalsIgnoreCase(std) && selectedStream.equalsIgnoreCase(stream)) {
                                String sid = stSnap.getKey();
                                String name = stSnap.child("name").getValue(String.class);
                                if (name == null)
                                    name = stSnap.child("Name").getValue(String.class);
                                // Match by ID or by name (case-insensitive)
                                if (query.equalsIgnoreCase(sid) ||
                                        (name != null && name.toLowerCase().contains(query.toLowerCase()))) {
                                    foundId = sid;
                                    foundName = name != null ? name : sid;
                                    break;
                                }
                            }
                        }
                        if (foundId == null) {
                            Toast.makeText(ParentAttendanceActivity.this,
                                    "Student not found in this class", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        loadStudentAttendance(path, foundId, foundName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentAttendanceActivity.this, "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStudentAttendance(String path, String studentId, String studentName) {
        FirebaseDatabase.getInstance().getReference("Attendance").child(path)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        records.clear();
                        int p = 0, a = 0, l = 0;
                        for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                            String subjectName = subjectSnap.getKey();
                            for (DataSnapshot dateSnap : subjectSnap.getChildren()) {
                                DataSnapshot stSnap = dateSnap.child(studentId);
                                if (!stSnap.exists())
                                    continue;
                                String status = stSnap.child("status").getValue(String.class);
                                String recordedSubject = stSnap.child("subject").getValue(String.class);
                                if (recordedSubject == null) {
                                    recordedSubject = subjectName;
                                }
                                if (status == null)
                                    status = "Present";
                                if ("Present".equalsIgnoreCase(status))
                                    p++;
                                else if ("Absent".equalsIgnoreCase(status))
                                    a++;
                                else if ("Late".equalsIgnoreCase(status))
                                    l++;
                                DateRecordAdapter.DateRecord dr = new DateRecordAdapter.DateRecord();
                                dr.date = dateSnap.getKey() != null ? dateSnap.getKey().replace("_", " ") : "?";
                                dr.status = status;
                                dr.subject = recordedSubject != null ? recordedSubject : "";
                                // Try to sort newest first
                                records.add(0, dr);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        int total = p + a + l;
                        int pct = total == 0 ? 0 : (int) ((p * 100.0) / total);

                        tvStudentNameResult
                                .setText(studentName + " (" + selectedStandard + " - " + selectedStream + ")");
                        tvSumPresent.setText(String.valueOf(p));
                        tvSumAbsent.setText(String.valueOf(a));
                        tvSumLate.setText(String.valueOf(l));
                        tvSumPct.setText(pct + "%");
                        tvSumPct.setTextColor(pct >= 75 ? 0xFF4CAF50 : pct >= 50 ? 0xFFFFA726 : 0xFFF44336);

                        cardSummary.setVisibility(View.VISIBLE);
                        tvRecordsHeader.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);

                        if (records.isEmpty()) {
                            Toast.makeText(ParentAttendanceActivity.this,
                                    "No attendance records found for this student", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentAttendanceActivity.this, "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ──── Adapter ──────────────────────────────────────────────────
    static class DateRecordAdapter extends RecyclerView.Adapter<DateRecordAdapter.VH> {
        static class DateRecord {
            String date, status, subject;
        }

        private final List<DateRecord> list;
        private final Context ctx;

        DateRecordAdapter(List<DateRecord> list, Context ctx) {
            this.list = list;
            this.ctx = ctx;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_attendance_record, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DateRecord r = list.get(pos);
            h.tvName.setText(r.date + (r.subject.isEmpty() ? "" : "  (" + r.subject + ")"));
            h.tvDate.setText("");
            h.tvStatus.setText(r.status);
            if ("Present".equalsIgnoreCase(r.status)) {
                h.tvStatus.setBackgroundColor(0xFF1B5E20);
                h.tvStatus.setTextColor(0xFFA5D6A7);
            } else if ("Absent".equalsIgnoreCase(r.status)) {
                h.tvStatus.setBackgroundColor(0xFFB71C1C);
                h.tvStatus.setTextColor(0xFFFFCDD2);
            } else {
                h.tvStatus.setBackgroundColor(0xFFE65100);
                h.tvStatus.setTextColor(0xFFFFE0B2);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvStatus;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvDate = v.findViewById(R.id.tvAttendanceDate);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }
}
