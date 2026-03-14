package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class teacherattendance extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // Shared teacher info
    public static String teacherStandard = "";
    public static String teacherStream = "";
    public static String teacherSubject = "";
    public static String teacherId = "";

    private static String[] tabTitles = { "Take", "Edit", "History", "My Class", "Summary" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherattendance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Attendance");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        // Read teacher prefs
        SharedPreferences prefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        teacherStandard = prefs.getString("teacherStandard", "");
        teacherStream = prefs.getString("teacherStream", "");
        teacherSubject = prefs.getString("teacherSubject", "");
        teacherId = prefs.getString("teacherId", "");

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new AttendancePagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, pos) -> tab.setText(tabTitles[pos])).attach();
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
            startActivity(new Intent(this, teacherexam.class));
        } else if (id == R.id.teacher_calendar) {
            startActivity(new Intent(this, teachercalender.class));
        } else if (id == R.id.teacher_attendance) {
            drawerLayout.closeDrawers();
            return true;
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

    // ─── ViewPager2 Adapter ───────────────────────────────────────────
    private static class AttendancePagerAdapter extends FragmentStateAdapter {
        AttendancePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new TakeAttendanceFragment();
                case 1:
                    return new EditAttendanceFragment();
                case 2:
                    return new HistoryFragment();
                case 3:
                    return new ClassAttendanceFragment();
                case 4:
                    return new MySummaryFragment();
                default:
                    return new TakeAttendanceFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 1 — Take Attendance
    // ═══════════════════════════════════════════════════════════════════
    public static class TakeAttendanceFragment extends Fragment {

        private TextView tvSelectedDate, tvClassInfo, tvSubjectInfo, tvStudentCount;
        private LinearLayout studentsContainer;
        private MaterialButton btnLoad, btnSubmit;
        private String selectedDate = "";
        private final List<StudentEntry> studentList = new ArrayList<>();
        private DatabaseReference studentsRef, attendanceRef;

        private static class StudentEntry {
            String studentId, name, status = "Present";
            RadioGroup radioGroup;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_take_attendance, container, false);

            tvSelectedDate = v.findViewById(R.id.tvSelectedDate);
            tvClassInfo = v.findViewById(R.id.tvClassInfo);
            tvSubjectInfo = v.findViewById(R.id.tvSubjectInfo);
            tvStudentCount = v.findViewById(R.id.tvStudentCount);
            studentsContainer = v.findViewById(R.id.studentsContainer);
            btnLoad = v.findViewById(R.id.btnLoadStudents);
            btnSubmit = v.findViewById(R.id.btnSubmitAttendance);

            studentsRef = FirebaseDatabase.getInstance().getReference("Students");
            attendanceRef = FirebaseDatabase.getInstance().getReference("Attendance");

            // Show class info
            tvClassInfo.setText("Class: " + teacherStandard + " - " + teacherStream);
            tvSubjectInfo.setText("Subject: " + teacherSubject);

            // Default date = today
            SimpleDateFormat sdf = new SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault());
            selectedDate = sdf.format(Calendar.getInstance().getTime());
            SimpleDateFormat displaySdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvSelectedDate.setText(displaySdf.format(Calendar.getInstance().getTime()));

            tvSelectedDate.setOnClickListener(vv -> {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day);
                    selectedDate = sdf.format(sel.getTime());
                    tvSelectedDate.setText(displaySdf.format(sel.getTime()));
                    // Clear loaded students when date changes
                    studentsContainer.removeAllViews();
                    studentList.clear();
                    btnSubmit.setVisibility(View.GONE);
                    tvStudentCount.setVisibility(View.GONE);
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

            btnLoad.setOnClickListener(vv -> {
                UIAnimator.animateClick(vv);
                vv.postDelayed(this::loadStudents, 150);
            });
            btnSubmit.setOnClickListener(vv -> {
                UIAnimator.animateClick(vv);
                vv.postDelayed(this::submitAttendance, 150);
            });

            return v;
        }

        private void loadStudents() {
            if (teacherStandard.isEmpty() || teacherStream.isEmpty()) {
                Toast.makeText(getContext(), "Teacher class info not found", Toast.LENGTH_SHORT).show();
                return;
            }
            studentsContainer.removeAllViews();
            String path = teacherStandard + "_" + teacherStream;

            // Check if attendance is already taken today for this subject
            attendanceRef.child(path).child(teacherSubject).child(selectedDate)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            if (snap.exists() && snap.hasChildren()) {
                                Toast.makeText(getContext(),
                                        "Attendance already taken for this subject today! Use Edit tab.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            fetchStudentsList();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }

        private void fetchStudentsList() {

            studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        Toast.makeText(getContext(), "No students found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (DataSnapshot s : snapshot.getChildren()) {
                        String std = s.child("Standard").getValue(String.class);
                        String stream = s.child("Stream").getValue(String.class);

                        if (teacherStandard.equalsIgnoreCase(std) && teacherStream.equalsIgnoreCase(stream)) {
                            String sid = s.getKey();
                            String name = s.child("name").getValue(String.class);
                            if (name == null)
                                name = s.child("Name").getValue(String.class);
                            if (name == null)
                                name = "Student";
                            StudentEntry se = new StudentEntry();
                            se.studentId = sid;
                            se.name = name;
                            studentList.add(se);
                            addStudentRow(se);
                        }
                    }
                    if (studentList.isEmpty()) {
                        Toast.makeText(getContext(), "No students found in " + teacherStandard + " " + teacherStream,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        tvStudentCount.setText(studentList.size() + " students loaded");
                        tvStudentCount.setVisibility(View.VISIBLE);
                        btnSubmit.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void addStudentRow(StudentEntry se) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.attendance_student_item, studentsContainer,
                    false);
            TextView tvName = row.findViewById(R.id.tvStudentName);
            RadioGroup rg = row.findViewById(R.id.radioGroupAttendance);
            RadioButton rbPresent = row.findViewById(R.id.rbPresent);
            rbPresent.setChecked(true);
            tvName.setText(se.name);
            rg.setOnCheckedChangeListener((g, id) -> {
                if (id == R.id.rbPresent)
                    se.status = "Present";
                else if (id == R.id.rbAbsent)
                    se.status = "Absent";
                else if (id == R.id.rbLate)
                    se.status = "Late";
            });
            se.radioGroup = rg;
            studentsContainer.addView(row);
        }

        private void submitAttendance() {
            if (studentList.isEmpty()) {
                Toast.makeText(getContext(), "No students loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            String path = teacherStandard + "_" + teacherStream;
            long timestamp = System.currentTimeMillis();
            Map<String, Object> batch = new HashMap<>();
            for (StudentEntry se : studentList) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", se.name);
                entry.put("status", se.status);
                entry.put("markedBy", teacherId);
                entry.put("subject", teacherSubject);
                entry.put("timestamp", timestamp);
                batch.put(se.studentId, entry);
            }
            attendanceRef.child(path).child(teacherSubject).child(selectedDate).setValue(batch)
                    .addOnSuccessListener(u -> {
                        Toast.makeText(getContext(), "✅ Attendance submitted!", Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(false);
                        // Trigger student FCM-like notification via Firebase flag
                        notifyStudents(path, teacherSubject, selectedDate, timestamp);
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        /** Write a notification trigger node that ChatNotificationService picks up. */
        private void notifyStudents(String classPath, String subject, String dateKey, long timestamp) {
            DatabaseReference notifRef = FirebaseDatabase.getInstance()
                    .getReference("AttendanceNotifications").child(classPath).child(subject).child(dateKey);
            Map<String, Object> notif = new HashMap<>();
            notif.put("markedBy", teacherId);
            notif.put("subject", teacherSubject);
            notif.put("timestamp", timestamp);
            notifRef.setValue(notif);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 2 — Edit Attendance
    // ═══════════════════════════════════════════════════════════════════
    public static class EditAttendanceFragment extends Fragment {

        private TextView tvSelectedDate, tvClassInfo, tvSubjectInfo, tvStudentCount;
        private LinearLayout studentsContainer;
        private MaterialButton btnLoad, btnUpdate;
        private String selectedDate = "";
        private final List<TakeAttendanceFragment.StudentEntry> studentList = new ArrayList<>();
        private DatabaseReference attendanceRef;
        private long originalTimestamp = 0;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_edit_attendance, container, false);

            tvSelectedDate = v.findViewById(R.id.tvSelectedDateEdit);
            tvClassInfo = v.findViewById(R.id.tvClassInfoEdit);
            tvSubjectInfo = v.findViewById(R.id.tvSubjectInfoEdit);
            tvStudentCount = v.findViewById(R.id.tvStudentCountEdit);
            studentsContainer = v.findViewById(R.id.studentsContainerEdit);
            btnLoad = v.findViewById(R.id.btnLoadAttendanceEdit);
            btnUpdate = v.findViewById(R.id.btnUpdateAttendance);

            attendanceRef = FirebaseDatabase.getInstance().getReference("Attendance");

            tvClassInfo.setText("Class: " + teacherStandard + " - " + teacherStream);
            tvSubjectInfo.setText("Subject: " + teacherSubject);

            SimpleDateFormat sdf = new SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault());
            selectedDate = sdf.format(Calendar.getInstance().getTime());
            SimpleDateFormat displaySdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvSelectedDate.setText(displaySdf.format(Calendar.getInstance().getTime()));

            tvSelectedDate.setOnClickListener(vv -> {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, day);
                    selectedDate = sdf.format(sel.getTime());
                    tvSelectedDate.setText(displaySdf.format(sel.getTime()));
                    studentsContainer.removeAllViews();
                    studentList.clear();
                    btnUpdate.setVisibility(View.GONE);
                    tvStudentCount.setVisibility(View.GONE);
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });

            btnLoad.setOnClickListener(vv -> {
                UIAnimator.animateClick(vv);
                vv.postDelayed(this::loadExistingAttendance, 150);
            });
            btnUpdate.setOnClickListener(vv -> {
                UIAnimator.animateClick(vv);
                vv.postDelayed(this::updateAttendance, 150);
            });

            return v;
        }

        private void loadExistingAttendance() {
            if (teacherStandard.isEmpty() || teacherStream.isEmpty())
                return;
            studentsContainer.removeAllViews();
            studentList.clear();
            btnUpdate.setVisibility(View.GONE);

            String path = teacherStandard + "_" + teacherStream;
            attendanceRef.child(path).child(teacherSubject).child(selectedDate)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists() || !snapshot.hasChildren()) {
                                Toast.makeText(getContext(), "No attendance found for this date/subject.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Verify 24 hr edit window
                            for (DataSnapshot st : snapshot.getChildren()) {
                                Long ts = st.child("timestamp").getValue(Long.class);
                                if (ts != null && ts > 0) {
                                    originalTimestamp = ts;
                                    long now = System.currentTimeMillis();
                                    if ((now - originalTimestamp) > 24 * 60 * 60 * 1000L) {
                                        Toast.makeText(getContext(), "Edit window (24 hrs) has expired.",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    break;
                                }
                            }

                            for (DataSnapshot s : snapshot.getChildren()) {
                                String sid = s.getKey();
                                String name = s.child("name").getValue(String.class);
                                String status = s.child("status").getValue(String.class);

                                TakeAttendanceFragment.StudentEntry se = new TakeAttendanceFragment.StudentEntry();
                                se.studentId = sid;
                                se.name = name;
                                se.status = status != null ? status : "Present";
                                studentList.add(se);
                                addStudentRow(se);
                            }

                            tvStudentCount.setText(studentList.size() + " records loaded for editing");
                            tvStudentCount.setVisibility(View.VISIBLE);
                            btnUpdate.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        private void addStudentRow(TakeAttendanceFragment.StudentEntry se) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.attendance_student_item, studentsContainer,
                    false);
            TextView tvName = row.findViewById(R.id.tvStudentName);
            RadioGroup rg = row.findViewById(R.id.radioGroupAttendance);
            RadioButton rbPresent = row.findViewById(R.id.rbPresent);
            RadioButton rbAbsent = row.findViewById(R.id.rbAbsent);
            RadioButton rbLate = row.findViewById(R.id.rbLate);

            tvName.setText(se.name);
            if ("Absent".equalsIgnoreCase(se.status))
                rbAbsent.setChecked(true);
            else if ("Late".equalsIgnoreCase(se.status))
                rbLate.setChecked(true);
            else
                rbPresent.setChecked(true);

            rg.setOnCheckedChangeListener((g, id) -> {
                if (id == R.id.rbPresent)
                    se.status = "Present";
                else if (id == R.id.rbAbsent)
                    se.status = "Absent";
                else if (id == R.id.rbLate)
                    se.status = "Late";
            });

            se.radioGroup = rg;
            studentsContainer.addView(row);
        }

        private void updateAttendance() {
            if (studentList.isEmpty())
                return;
            String path = teacherStandard + "_" + teacherStream;

            Map<String, Object> batch = new HashMap<>();
            for (TakeAttendanceFragment.StudentEntry se : studentList) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", se.name);
                entry.put("status", se.status);
                entry.put("markedBy", teacherId);
                entry.put("subject", teacherSubject);
                entry.put("timestamp", originalTimestamp > 0 ? originalTimestamp : System.currentTimeMillis());
                batch.put(se.studentId, entry);
            }

            attendanceRef.child(path).child(teacherSubject).child(selectedDate).setValue(batch)
                    .addOnSuccessListener(u -> {
                        Toast.makeText(getContext(), "✅ Attendance updated successfully!", Toast.LENGTH_LONG).show();
                        btnUpdate.setEnabled(false);
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 3 — History
    // ═══════════════════════════════════════════════════════════════════
    public static class HistoryFragment extends Fragment {

        private RecyclerView rv;
        private TextView tvEmpty;
        private HistoryAdapter adapter;
        private final List<HistoryItem> historyList = new ArrayList<>();

        static class HistoryItem {
            String dateKey, displayDate;
            int presentCount, absentCount, lateCount;
            long timestamp; // earliest timestamp found
            Map<String, Map<String, String>> studentStatuses = new HashMap<>();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_attendance_history, container, false);
            rv = v.findViewById(R.id.rvHistory);
            tvEmpty = v.findViewById(R.id.tvHistoryEmpty);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new HistoryAdapter(historyList, getContext(), this);
            rv.setAdapter(adapter);
            loadHistory();
            return v;
        }

        void loadHistory() {
            String path = teacherStandard + "_" + teacherStream;
            FirebaseDatabase.getInstance().getReference("Attendance").child(path).child(teacherSubject)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            historyList.clear();
                            for (DataSnapshot dateSnap : snapshot.getChildren()) {
                                HistoryItem item = new HistoryItem();
                                item.dateKey = dateSnap.getKey();
                                if (item.dateKey == null)
                                    continue;
                                // Convert date key back to display format
                                item.displayDate = item.dateKey.replace("_", " ");
                                int p = 0, a = 0, l = 0;
                                long ts = 0;
                                for (DataSnapshot stSnap : dateSnap.getChildren()) {
                                    String status = stSnap.child("status").getValue(String.class);
                                    if (status == null)
                                        status = "Present";
                                    Long tsnap = stSnap.child("timestamp").getValue(Long.class);
                                    if (tsnap != null && tsnap > ts)
                                        ts = tsnap;
                                    if ("Present".equalsIgnoreCase(status))
                                        p++;
                                    else if ("Absent".equalsIgnoreCase(status))
                                        a++;
                                    else if ("Late".equalsIgnoreCase(status))
                                        l++;
                                    // Store for edit dialog
                                    Map<String, String> sdata = new HashMap<>();
                                    sdata.put("name", stSnap.child("name").getValue(String.class) != null
                                            ? stSnap.child("name").getValue(String.class)
                                            : "");
                                    sdata.put("status", status);
                                    item.studentStatuses.put(stSnap.getKey(), sdata);
                                }
                                item.presentCount = p;
                                item.absentCount = a;
                                item.lateCount = l;
                                item.timestamp = ts;
                                historyList.add(0, item); // newest first
                            }
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }
    }

    // ─── History Adapter ────────────────────────────────────────────
    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<HistoryFragment.HistoryItem> list;
        private final Context ctx;
        private final HistoryFragment fragment;
        private static final long EDIT_WINDOW_MS = 24 * 60 * 60 * 1000L;

        HistoryAdapter(List<HistoryFragment.HistoryItem> list, Context ctx, HistoryFragment fragment) {
            this.list = list;
            this.ctx = ctx;
            this.fragment = fragment;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_attendance_history, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HistoryFragment.HistoryItem item = list.get(pos);
            h.tvDate.setText(item.displayDate);
            h.tvClass.setText(teacherStandard + " - " + teacherStream);
            h.tvSubject.setText("Subject: " + teacherSubject);
            h.tvPresent.setText("P: " + item.presentCount);
            h.tvAbsent.setText("A: " + item.absentCount);
            h.tvLate.setText("L: " + item.lateCount);

            boolean canEdit = (System.currentTimeMillis() - item.timestamp) < EDIT_WINDOW_MS;
            h.tvExpired.setVisibility(canEdit ? View.GONE : View.VISIBLE);
            // We disable the inline Edit button as we now have a dedicated Edit tab
            h.btnEdit.setVisibility(View.GONE);
            h.btnDelete.setVisibility(canEdit ? View.VISIBLE : View.GONE);

            h.btnDelete.setOnClickListener(v -> {
                UIAnimator.animateClick(v);
                v.postDelayed(() -> {
                    new AlertDialog.Builder(ctx)
                            .setTitle("Delete Attendance")
                            .setMessage("Delete attendance for " + item.displayDate + "?")
                            .setPositiveButton("Delete", (d, w) -> {
                                String path = teacherStandard + "_" + teacherStream;
                                FirebaseDatabase.getInstance().getReference("Attendance")
                                        .child(path).child(teacherSubject).child(item.dateKey).removeValue()
                                        .addOnSuccessListener(u -> {
                                            list.remove(pos);
                                            notifyDataSetChanged();
                                            Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }, 150);
            });

            h.btnEdit.setOnClickListener(v -> {
                UIAnimator.animateClick(v);
                v.postDelayed(() -> showEditDialog(item), 150);
            });
        }

        private void showEditDialog(HistoryFragment.HistoryItem item) {
            // Build a scrollable dialog listing each student with a spinner/radio
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle("Edit Attendance — " + item.displayDate);

            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 16, 32, 16);
            layout.setBackgroundColor(0xFF1A2744);

            // Map studentId -> new status (mutable)
            Map<String, String> updatedStatuses = new HashMap<>(item.studentStatuses.size());
            for (Map.Entry<String, Map<String, String>> e : item.studentStatuses.entrySet()) {
                updatedStatuses.put(e.getKey(), e.getValue().get("status"));
            }
            Map<String, RadioGroup> rgMap = new HashMap<>();

            for (Map.Entry<String, Map<String, String>> e : item.studentStatuses.entrySet()) {
                String sid = e.getKey();
                String sname = e.getValue().get("name");
                String sstatus = e.getValue().get("status");

                // Student name label
                TextView tv = new TextView(ctx);
                tv.setText(sname);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(14f);
                tv.setPadding(0, 16, 0, 4);
                layout.addView(tv);

                RadioGroup rg = new RadioGroup(ctx);
                rg.setOrientation(RadioGroup.HORIZONTAL);

                RadioButton rbP = new RadioButton(ctx);
                rbP.setText("Present");
                rbP.setTextColor(0xFFA5D6A7);
                rbP.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFA5D6A7));
                RadioButton rbA = new RadioButton(ctx);
                rbA.setText("Absent");
                rbA.setTextColor(0xFFEF9A9A);
                rbA.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFEF9A9A));
                RadioButton rbL = new RadioButton(ctx);
                rbL.setText("Late");
                rbL.setTextColor(0xFFFFCC80);
                rbL.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFFCC80));

                rbP.setId(View.generateViewId());
                rbA.setId(View.generateViewId());
                rbL.setId(View.generateViewId());

                rg.addView(rbP);
                rg.addView(rbA);
                rg.addView(rbL);
                if ("Absent".equalsIgnoreCase(sstatus))
                    rbA.setChecked(true);
                else if ("Late".equalsIgnoreCase(sstatus))
                    rbL.setChecked(true);
                else
                    rbP.setChecked(true);

                final String fSid = sid;
                rg.setOnCheckedChangeListener((g, checkedId) -> {
                    if (checkedId == rbP.getId())
                        updatedStatuses.put(fSid, "Present");
                    else if (checkedId == rbA.getId())
                        updatedStatuses.put(fSid, "Absent");
                    else if (checkedId == rbL.getId())
                        updatedStatuses.put(fSid, "Late");
                });

                rgMap.put(sid, rg);
                layout.addView(rg);
            }

            android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
            sv.addView(layout);
            builder.setView(sv);

            builder.setPositiveButton("Save", (d, w) -> {
                String path = teacherStandard + "_" + teacherStream;
                DatabaseReference dateRef = FirebaseDatabase.getInstance()
                        .getReference("Attendance").child(path).child(teacherSubject).child(item.dateKey);
                for (Map.Entry<String, String> entry : updatedStatuses.entrySet()) {
                    dateRef.child(entry.getKey()).child("status").setValue(entry.getValue());
                }
                Toast.makeText(ctx, "Attendance updated", Toast.LENGTH_SHORT).show();
                // Refresh the list
                fragment.loadHistory();
            });
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dialog.show();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvClass, tvSubject, tvPresent, tvAbsent, tvLate, tvExpired;
            MaterialButton btnEdit, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvClass = itemView.findViewById(R.id.tvHistoryClass);
                tvSubject = itemView.findViewById(R.id.tvHistorySubject);
                tvPresent = itemView.findViewById(R.id.tvPresentCount);
                tvAbsent = itemView.findViewById(R.id.tvAbsentCount);
                tvLate = itemView.findViewById(R.id.tvLateCount);
                tvExpired = itemView.findViewById(R.id.tvEditExpired);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 3 — Class Attendance (all students summary)
    // ═══════════════════════════════════════════════════════════════════
    public static class ClassAttendanceFragment extends Fragment {
        private RecyclerView rv;
        private TextView tvEmpty;
        private ClassAttendanceAdapter adapter;
        private final List<ClassAttendanceAdapter.StudentSummary> summaryList = new ArrayList<>();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_class_attendance, container, false);
            rv = v.findViewById(R.id.rvClassAttendance);
            tvEmpty = v.findViewById(R.id.tvClassEmpty);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ClassAttendanceAdapter(summaryList, getContext());
            rv.setAdapter(adapter);
            loadClassAttendance();
            return v;
        }

        private void loadClassAttendance() {
            String path = teacherStandard + "_" + teacherStream;
            FirebaseDatabase.getInstance().getReference("Attendance").child(path).child(teacherSubject)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // studentId -> summary
                            Map<String, ClassAttendanceAdapter.StudentSummary> map = new HashMap<>();
                            for (DataSnapshot dateSnap : snapshot.getChildren()) {
                                for (DataSnapshot stSnap : dateSnap.getChildren()) {
                                    String sid = stSnap.getKey();
                                    if (sid == null)
                                        continue;
                                    String name = stSnap.child("name").getValue(String.class);
                                    String status = stSnap.child("status").getValue(String.class);
                                    if (!map.containsKey(sid)) {
                                        ClassAttendanceAdapter.StudentSummary ss = new ClassAttendanceAdapter.StudentSummary();
                                        ss.studentId = sid;
                                        ss.name = name != null ? name : "Student";
                                        map.put(sid, ss);
                                    }
                                    ClassAttendanceAdapter.StudentSummary ss = map.get(sid);
                                    if ("Present".equalsIgnoreCase(status))
                                        ss.present++;
                                    else if ("Absent".equalsIgnoreCase(status))
                                        ss.absent++;
                                    else if ("Late".equalsIgnoreCase(status))
                                        ss.late++;
                                }
                            }
                            summaryList.clear();
                            summaryList.addAll(map.values());
                            // Sort by name
                            summaryList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(summaryList.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }
    }

    static class ClassAttendanceAdapter extends RecyclerView.Adapter<ClassAttendanceAdapter.VH> {
        static class StudentSummary {
            String studentId, name;
            int present, absent, late;
        }

        private final List<StudentSummary> list;
        private final Context ctx;

        ClassAttendanceAdapter(List<StudentSummary> list, Context ctx) {
            this.list = list;
            this.ctx = ctx;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_class_attendance, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            StudentSummary ss = list.get(pos);
            String firstLetter = ss.name.isEmpty() ? "?" : ss.name.substring(0, 1).toUpperCase();
            h.tvAvatar.setText(firstLetter);
            h.tvName.setText(ss.name);
            h.tvPresent.setText("P: " + ss.present);
            h.tvAbsent.setText("A: " + ss.absent);
            h.tvLate.setText("L: " + ss.late);
            int total = ss.present + ss.absent + ss.late;
            int pct = total == 0 ? 0 : (int) ((ss.present * 100.0) / total);
            h.tvPct.setText(pct + "%");
            h.tvPct.setTextColor(pct >= 75 ? 0xFF4CAF50 : pct >= 50 ? 0xFFFFA726 : 0xFFF44336);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvPresent, tvAbsent, tvLate, tvPct;

            VH(@NonNull View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvAvatar);
                tvName = v.findViewById(R.id.tvClassName);
                tvPresent = v.findViewById(R.id.tvTotalPresent);
                tvAbsent = v.findViewById(R.id.tvTotalAbsent);
                tvLate = v.findViewById(R.id.tvTotalLate);
                tvPct = v.findViewById(R.id.tvAttendancePct);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 4 — My Summary (how many classes teacher has taken)
    // ═══════════════════════════════════════════════════════════════════
    public static class MySummaryFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            // Build programmatically — simple stats card
            android.widget.ScrollView sv = new android.widget.ScrollView(getContext());
            LinearLayout root = new LinearLayout(getContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(32, 32, 32, 32);

            // Info card
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(
                    getContext());
            android.widget.FrameLayout.LayoutParams cardParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 32);
            card.setLayoutParams(cardParams);
            card.setCardBackgroundColor(0xFF1A2744);
            card.setRadius(24f);
            card.setCardElevation(8f);

            LinearLayout cardInner = new LinearLayout(getContext());
            cardInner.setOrientation(LinearLayout.VERTICAL);
            cardInner.setPadding(40, 40, 40, 40);

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText("My Attendance Summary");
            tvTitle.setTextColor(0xFF4FC3F7);
            tvTitle.setTextSize(18f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            cardInner.addView(tvTitle);

            TextView tvClass = new TextView(getContext());
            tvClass.setText("Class: " + teacherStandard + " - " + teacherStream);
            tvClass.setTextColor(0xFFCCDDFF);
            tvClass.setTextSize(14f);
            tvClass.setPadding(0, 16, 0, 0);
            cardInner.addView(tvClass);

            TextView tvSubj = new TextView(getContext());
            tvSubj.setText("Subject: " + teacherSubject);
            tvSubj.setTextColor(0xFFCCDDFF);
            tvSubj.setTextSize(14f);
            tvSubj.setPadding(0, 8, 0, 0);
            cardInner.addView(tvSubj);

            TextView tvCount = new TextView(getContext());
            tvCount.setText("Total classes taken: Loading...");
            tvCount.setTextColor(0xFF90CAF9);
            tvCount.setTextSize(16f);
            tvCount.setPadding(0, 16, 0, 0);
            tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
            cardInner.addView(tvCount);

            card.addView(cardInner);
            root.addView(card);
            sv.addView(root);

            // Load count
            String path = teacherStandard + "_" + teacherStream;
            FirebaseDatabase.getInstance().getReference("Attendance").child(path).child(teacherSubject)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Count date nodes where markedBy == teacherId
                            int count = 0;
                            for (DataSnapshot dateSnap : snapshot.getChildren()) {
                                boolean hasTeacherMarker = false;
                                for (DataSnapshot stSnap : dateSnap.getChildren()) {
                                    String mb = stSnap.child("markedBy").getValue(String.class);
                                    if (mb != null && mb.equals(teacherId)) {
                                        hasTeacherMarker = true;
                                    }
                                }
                                if (hasTeacherMarker)
                                    count++;
                            }
                            tvCount.setText("Total classes taken: " + count);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            tvCount.setText("Error loading data");
                        }
                    });

            return sv;
        }
    }
}
