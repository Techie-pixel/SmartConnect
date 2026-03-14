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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyContactMessagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MyContactMessageAdapter adapter;
    private List<MyContactMessageAdapter.MyMessageItem> messageList = new ArrayList<>();
    private DatabaseReference contactRef;
    private String senderUid;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_contact_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Messages");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        senderUid = getIntent().getStringExtra("senderUid");
        if (senderUid == null)
            senderUid = "unknown";

        contactRef = FirebaseDatabase.getInstance().getReference("ContactAdmin");

        adapter = new MyContactMessageAdapter(this, messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadMessages();
    }

    private void loadMessages() {
        listener = contactRef.orderByChild("senderUid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String key = child.getKey();
                            String msg = child.child("message").getValue(String.class);
                            Long ts = child.child("timestamp").getValue(Long.class);

                            // Check if there are unread admin replies
                            boolean hasUnread = false;
                            if (child.hasChild("replies")) {
                                for (DataSnapshot reply : child.child("replies").getChildren()) {
                                    String sender = reply.child("sender").getValue(String.class);
                                    Boolean read = reply.child("read").getValue(Boolean.class);
                                    if ("Admin".equals(sender) && (read == null || !read)) {
                                        hasUnread = true;
                                        break;
                                    }
                                }
                            }

                            messageList.add(new MyContactMessageAdapter.MyMessageItem(
                                    key, msg, ts != null ? ts : 0, hasUnread));
                        }

                        // Sort newest first
                        Collections.sort(messageList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(messageList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyContactMessagesActivity.this, "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            contactRef.removeEventListener(listener);
        }
    }
}
