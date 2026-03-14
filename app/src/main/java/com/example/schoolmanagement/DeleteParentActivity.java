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

public class DeleteParentActivity extends AppCompatActivity implements DeleteParentAdapter.OnDeleteClick {

    ListView parentDeleteList;
    ArrayList<ParentBrief> items = new ArrayList<>();
    DeleteParentAdapter adapter;
    DatabaseReference parentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_parent);

        parentDeleteList = findViewById(R.id.parentDeleteList);
        adapter = new DeleteParentAdapter(this, items, this);
        parentDeleteList.setAdapter(adapter);

        parentsRef = FirebaseDatabase.getInstance().getReference("Parents");
        loadParents();
    }

    private void loadParents() {
        parentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot parentNode : snapshot.getChildren()) {
                    String id = parentNode.getKey();
                    String name = parentNode.child("name").getValue(String.class);
                    String email = parentNode.child("email").getValue(String.class);
                    String mobile = parentNode.child("mobile").getValue(String.class);
                    String mode = parentNode.child("mode").getValue(String.class);
                    String studentName = parentNode.child("studentName").getValue(String.class);

                    if (id != null) {
                        String nameVal = name != null ? name : "Parent";
                        String emailVal = email != null ? email : "";
                        String mobileVal = mobile != null ? mobile : "";
                        String modeVal = mode != null ? mode : "";
                        String studentVal = studentName != null ? studentName : "";
                        items.add(new ParentBrief(id, nameVal, emailVal, mobileVal, modeVal, studentVal));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeleteParentActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(ParentBrief item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Parent")
                .setMessage("Are you sure you want to delete " + item.name + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    parentsRef.child(item.id).removeValue()
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

class ParentBrief {
    String id;
    String name;
    String email;
    String mobile;
    String mode;
    String studentName;

    ParentBrief(String id, String name, String email, String mobile, String mode, String studentName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.mode = mode;
        this.studentName = studentName;
    }
}

