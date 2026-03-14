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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

public class AdminFeedbackActivity extends AppCompatActivity implements FeedbackAdapter.OnDeleteClick {

    ListView feedbackListView;
    ArrayList<FeedbackItem> items = new ArrayList<>();
    FeedbackAdapter adapter;
    DatabaseReference feedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_feedback);

        feedbackListView = findViewById(R.id.feedbackListView);
        adapter = new FeedbackAdapter(this, items, this);
        feedbackListView.setAdapter(adapter);

        feedbackRef = FirebaseDatabase.getInstance().getReference("Feedback");
        loadFeedback();

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = { feedbackListView };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(android.view.View.INVISIBLE);
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

    @Override
    protected void onResume() {
        super.onResume();
        ChatNotificationService.activeFeedbackScreen = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatNotificationService.activeFeedbackScreen = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatNotificationService.activeFeedbackScreen = false;
    }

    private void loadFeedback() {
        feedbackRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String name = child.child("Name").getValue(String.class);
                    String email = child.child("Email").getValue(String.class);
                    String contact = child.child("Contact").getValue(String.class);
                    String feedback = child.child("Feedback").getValue(String.class);
                    items.add(new FeedbackItem(key, name, email, contact, feedback));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminFeedbackActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(FeedbackItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Feedback")
                .setMessage("Are you sure you want to delete this feedback from " + item.name + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    feedbackRef.child(item.key).removeValue()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
