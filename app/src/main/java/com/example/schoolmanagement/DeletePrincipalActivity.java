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
import java.util.List;

public class DeletePrincipalActivity extends AppCompatActivity implements PrincipalDeleteAdapter.OnDeleteClick {

    ListView principalDeleteList;
    List<PrincipalRow> items = new ArrayList<>();
    PrincipalDeleteAdapter adapter;
    DatabaseReference principalsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_principal);

        principalDeleteList = findViewById(R.id.principalDeleteList);
        adapter = new PrincipalDeleteAdapter(this, items, this);
        principalDeleteList.setAdapter(adapter);

        principalsRef = FirebaseDatabase.getInstance().getReference("Principals");
        loadPrincipals();
    }

    private void loadPrincipals() {
        principalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getKey();
                    String name = ds.child("name").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);
                    String mobile = ds.child("mobile").getValue(String.class);
                    items.add(new PrincipalRow(id, name, email, mobile));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeletePrincipalActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(PrincipalRow item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Principal")
                .setMessage("Are you sure you want to delete " + item.name + " (" + item.id + ")?")
                .setPositiveButton("Delete", (d, w) -> {
                    principalsRef.child(item.id).removeValue()
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

class PrincipalRow {
    String id;
    String name;
    String email;
    String mobile;
    PrincipalRow(String id, String name, String email, String mobile) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
    }
}
