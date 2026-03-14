package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DeleteTeacherActivity extends AppCompatActivity implements DeleteTeacherAdapter.OnDeleteClick {

    ListView teacherDeleteList;
    ArrayList<TeacherBrief> items = new ArrayList<>();
    DeleteTeacherAdapter adapter;
    DatabaseReference teachersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_teacher);

        teacherDeleteList = findViewById(R.id.teacherDeleteList);
        adapter = new DeleteTeacherAdapter(this, items, this);
        teacherDeleteList.setAdapter(adapter);

        teachersRef = FirebaseDatabase.getInstance().getReference("Teachers");
        loadTeachers();
    }

    private void loadTeachers() {
        teachersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot teacherNode : snapshot.getChildren()) {
                    String teacherId = teacherNode.getKey();
                    String name = null;
                    String email = null;
                    java.util.HashSet<String> streams = new java.util.HashSet<>();
                    java.util.HashSet<String> subjects = new java.util.HashSet<>();

                    for (DataSnapshot subj : teacherNode.getChildren()) {
                        if (name == null) name = subj.child("name").getValue(String.class);
                        if (email == null) email = subj.child("email").getValue(String.class);
                        String st = subj.child("stream").getValue(String.class);
                        String sb = subj.child("subject").getValue(String.class);
                        if (st != null && !st.isEmpty()) streams.add(st);
                        if (sb != null && !sb.isEmpty()) subjects.add(sb);
                    }
                    if (teacherId != null) {
                        String nameVal = name != null ? name : "Teacher " + teacherId;
                        String emailVal = email != null ? email : "";
                        String streamsVal = String.join(", ", streams);
                        String subjectsVal = String.join(", ", subjects);
                        items.add(new TeacherBrief(teacherId, nameVal, emailVal, streamsVal, subjectsVal));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeleteTeacherActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(TeacherBrief item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Teacher")
                .setMessage("Are you sure you want to delete " + item.name + " (" + item.id + ")?")
                .setPositiveButton("Delete", (d, w) -> {
                    teachersRef.child(item.id).removeValue()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                items.remove(item);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

class TeacherBrief {
    String id;
    String name;
    String email;
    String streams;
    String subjects;
    TeacherBrief(String id, String name, String email, String streams, String subjects) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.streams = streams;
        this.subjects = subjects;
    }
}
