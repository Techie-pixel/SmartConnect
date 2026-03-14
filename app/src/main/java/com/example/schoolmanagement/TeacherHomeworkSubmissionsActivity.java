package com.example.schoolmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherHomeworkSubmissionsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView chipAll, chipUnchecked, chipChecked, tvEmpty;
    private android.widget.Button btnDelete;
    private SubmissionAdapter adapter;
    private final List<SubmissionAdapter.SubmissionItem> items = new ArrayList<>();
    private final List<SubmissionAdapter.SubmissionItem> visible = new ArrayList<>();
    private final Set<String> myHomeworkKeys = new HashSet<>();
    private DatabaseReference homeworkRef, submissionsRef;
    private String myTeacherId = "";

    private enum Filter {
        ALL, UNCHECKED, CHECKED
    }

    private Filter current = Filter.ALL;
    private final java.util.Map<String, String> keyToCategory = new java.util.HashMap<>();
    private final java.util.Map<String, String> keyToTitle = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_homework_submissions);

        android.widget.ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::finish, 150);
        });

        rv = findViewById(R.id.rvSubmissions);
        tvEmpty = findViewById(R.id.tvEmpty);
        chipAll = findViewById(R.id.chipAll);
        chipUnchecked = findViewById(R.id.chipUnchecked);
        chipChecked = findViewById(R.id.chipChecked);
        btnDelete = findViewById(R.id.btnDeleteHomework);

        adapter = new SubmissionAdapter(visible, new SubmissionAdapter.OnMarkListener() {
            @Override
            public void onMarkChecked(SubmissionAdapter.SubmissionItem item) {
                markChecked(item, true);
            }

            @Override
            public void onOpen(SubmissionAdapter.SubmissionItem item) {
                openSubmission(item);
            }

            @Override
            public void onReject(SubmissionAdapter.SubmissionItem item) {
                rejectSubmission(item);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        chipAll.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> apply(Filter.ALL), 150);
        });
        chipUnchecked.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> apply(Filter.UNCHECKED), 150);
        });
        chipChecked.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> apply(Filter.CHECKED), 150);
        });
        btnDelete.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showDeleteHomeworkDialog, 150);
        });

        myTeacherId = getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "");
        homeworkRef = FirebaseDatabase.getInstance().getReference("Homework");
        submissionsRef = FirebaseDatabase.getInstance().getReference("HomeworkSubmissions");
        loadMyHomeworkKeys();
    }

    private void loadMyHomeworkKeys() {
        homeworkRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myHomeworkKeys.clear();
                keyToCategory.clear();
                keyToTitle.clear();
                for (DataSnapshot cat : snapshot.getChildren()) {
                    for (DataSnapshot a : cat.getChildren()) {
                        String tId = a.child("teacherId").getValue(String.class);
                        if (tId != null && tId.equals(myTeacherId)) {
                            if (a.getKey() != null) {
                                myHomeworkKeys.add(a.getKey());
                                keyToCategory.put(a.getKey(), cat.getKey());
                                String title = a.child("title").getValue(String.class);
                                keyToTitle.put(a.getKey(), title != null ? title : a.getKey());
                            }
                        }
                    }
                }
                loadSubmissions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadSubmissions() {
        submissionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot a : snapshot.getChildren()) {
                    String key = a.getKey();
                    if (key == null || !myHomeworkKeys.contains(key))
                        continue;
                    for (DataSnapshot s : a.getChildren()) {
                        String studentName = s.child("studentName").getValue(String.class);
                        String status = s.child("status").getValue(String.class);
                        Long ts = s.child("submittedAt").getValue(Long.class);
                        String text = s.child("text").getValue(String.class);
                        String image = s.child("image").getValue(String.class);
                        SubmissionAdapter.SubmissionItem it = new SubmissionAdapter.SubmissionItem();
                        it.assignmentKey = key;
                        it.assignmentTitle = keyToTitle.containsKey(key) ? keyToTitle.get(key) : "";
                        it.studentUid = s.getKey();
                        it.studentName = studentName != null ? studentName : "Student";
                        it.status = status != null ? status : "unchecked";
                        it.submittedAt = ts != null ? ts : 0L;
                        it.itText = text;
                        it.itImage = image;
                        items.add(it);
                    }
                }
                apply(current);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void apply(Filter f) {
        current = f;
        chipAll.setBackgroundResource(f == Filter.ALL ? R.drawable.chip_selected : R.drawable.chip_unselected);
        chipUnchecked
                .setBackgroundResource(f == Filter.UNCHECKED ? R.drawable.chip_selected : R.drawable.chip_unselected);
        chipChecked.setBackgroundResource(f == Filter.CHECKED ? R.drawable.chip_selected : R.drawable.chip_unselected);
        visible.clear();
        for (SubmissionAdapter.SubmissionItem it : items) {
            if (f == Filter.ALL)
                visible.add(it);
            else if (f == Filter.UNCHECKED && !"checked".equals(it.status))
                visible.add(it);
            else if (f == Filter.CHECKED && "checked".equals(it.status))
                visible.add(it);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openSubmission(SubmissionAdapter.SubmissionItem item) {
        String msg = item.studentName + "\n" +
                new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date(item.submittedAt)) +
                "\n\n" + (item.itText != null ? item.itText : "");
        androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
        b.setTitle("Submission");
        b.setMessage(msg);
        if (item.itImage != null && !item.itImage.isEmpty()) {
            b.setPositiveButton("View Image", (d, w) -> {
                Intent intent = new Intent(this, Image_detail.class);
                intent.putExtra("image", item.itImage);
                intent.putExtra("description", item.itText != null ? item.itText : "");
                startActivity(intent);
            });
            b.setNegativeButton("Close", null);
        } else {
            b.setPositiveButton("Close", null);
        }
        b.show();
    }

    private void showDeleteHomeworkDialog() {
        if (myHomeworkKeys.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setMessage("No homework to delete")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        java.util.List<String> keys = new java.util.ArrayList<>(myHomeworkKeys);
        String[] titles = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            titles[i] = keyToTitle.getOrDefault(keys.get(i), keys.get(i));
        }
        final int[] sel = { -1 };
        androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
        b.setTitle("Delete Homework");
        b.setSingleChoiceItems(titles, -1, (d, which) -> sel[0] = which);
        b.setNegativeButton("Cancel", null);
        b.setPositiveButton("Delete", (d, w) -> {
            if (sel[0] >= 0) {
                String key = keys.get(sel[0]);
                String category = keyToCategory.get(key);
                if (category != null) {
                    FirebaseDatabase.getInstance().getReference("Homework")
                            .child(category).child(key).removeValue();
                    FirebaseDatabase.getInstance().getReference("HomeworkSubmissions")
                            .child(key).removeValue();
                    loadMyHomeworkKeys();
                }
            }
        });
        b.show();
    }

    private void markChecked(SubmissionAdapter.SubmissionItem item, boolean checked) {
        if ("rejected".equals(item.status)) {
            android.widget.Toast
                    .makeText(this, "Cannot mark a rejected submission as checked", android.widget.Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String newStatus = checked ? "checked" : "unchecked";
        FirebaseDatabase.getInstance().getReference("HomeworkSubmissions")
                .child(item.assignmentKey).child(item.studentUid)
                .child("status").setValue(newStatus)
                .addOnSuccessListener(unused -> {
                    item.status = newStatus;
                    adapter.notifyDataSetChanged();
                });
    }

    private void rejectSubmission(SubmissionAdapter.SubmissionItem item) {
        if ("checked".equals(item.status)) {
            android.widget.Toast.makeText(this, "Already checked — cannot reject", android.widget.Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason for rejection");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reject Submission")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty())
                        reason = "Please correct and submit again";
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("HomeworkSubmissions")
                            .child(item.assignmentKey).child(item.studentUid);
                    java.util.Map<String, Object> upd = new java.util.HashMap<>();
                    upd.put("status", "rejected");
                    upd.put("rejectionReason", reason);
                    ref.updateChildren(upd).addOnSuccessListener(u -> {
                        item.status = "rejected";
                        item.itText = item.itText != null ? item.itText : "";
                        adapter.notifyDataSetChanged();
                    });
                })
                .show();
    }

}
