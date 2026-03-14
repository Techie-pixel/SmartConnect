package com.example.schoolmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AdminContactMessagesActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ContactMessageAdapter adapter;
    private List<ContactMessageAdapter.MessageItem> allMessages = new ArrayList<>();
    private List<ContactMessageAdapter.MessageItem> filteredMessages = new ArrayList<>();
    private DatabaseReference contactRef;
    private ValueEventListener listener;
    private int currentTab = 0; // 0=All, 1=Unread, 2=Read

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_contact_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Contact Messages");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        contactRef = FirebaseDatabase.getInstance().getReference("ContactAdmin");

        adapter = new ContactMessageAdapter(this, filteredMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        loadMessages();

        applyAnimations();
    }

    private void applyAnimations() {
        ImageView ivLogo = findViewById(R.id.ivLogo);
        View[] views = { ivLogo, tabLayout, recyclerView };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(View.VISIBLE);
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

    private void loadMessages() {
        listener = contactRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMessages.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String name = child.child("senderName").getValue(String.class);
                    String email = child.child("senderEmail").getValue(String.class);
                    String mobile = child.child("senderMobile").getValue(String.class);
                    String role = child.child("senderRole").getValue(String.class);
                    String uid = child.child("senderUid").getValue(String.class);
                    String msg = child.child("message").getValue(String.class);
                    String std = child.child("std").getValue(String.class);
                    String stream = child.child("stream").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    Boolean read = child.child("read").getValue(Boolean.class);

                    allMessages.add(new ContactMessageAdapter.MessageItem(
                            key, name, email, mobile, role, uid, msg, std, stream,
                            ts != null ? ts : 0, read != null ? read : false));
                }

                // Sort newest first
                Collections.sort(allMessages, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminContactMessagesActivity.this, "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        filteredMessages.clear();
        for (ContactMessageAdapter.MessageItem item : allMessages) {
            if (currentTab == 0) {
                filteredMessages.add(item);
            } else if (currentTab == 1 && !item.read) {
                filteredMessages.add(item);
            } else if (currentTab == 2 && item.read) {
                filteredMessages.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredMessages.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredMessages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            contactRef.removeEventListener(listener);
        }
    }
}
