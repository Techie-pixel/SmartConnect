package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

public class ManageStudentsActivity extends AppCompatActivity {
    EditText studentSearchBar;
    ListView studentListView;
    StudentManageAdapter adapter;
    List<StudentRow> items = new ArrayList<>();
    DatabaseReference studentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        studentSearchBar = findViewById(R.id.studentSearchBar);
        studentListView = findViewById(R.id.studentListView);

        adapter = new StudentManageAdapter(this, items, item -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Student")
                    .setMessage("Delete " + (item.name != null ? item.name : "student") + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        studentsRef.child(item.id).removeValue()
                                .addOnSuccessListener(a -> {
                                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                    loadStudents();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        studentListView.setAdapter(adapter);

        studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        loadStudents();

        studentSearchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = { studentSearchBar, studentListView };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(android.view.View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                views[i].startAnimation(anim);
            }
        }
    }

    private void loadStudents() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("Name").getValue(String.class);
                    String email = child.child("Email").getValue(String.class);
                    String standard = child.child("Standard").getValue(String.class);
                    String stream = child.child("Stream").getValue(String.class);
                    items.add(new StudentRow(id, name, email, standard, stream));
                }
                adapter.updateData(items);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ManageStudentsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
