package com.example.schoolmanagement;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ContactMessageDetailActivity extends AppCompatActivity {

    private TextView tvSenderInfo, tvContactInfo, tvOriginalMessage, tvMessageTime;
    private RecyclerView recyclerReplies;
    private TextInputEditText etReply;
    private ImageButton btnSendReply;

    private DatabaseReference messageRef;
    private ReplyAdapter replyAdapter;
    private List<ReplyAdapter.ReplyItem> replyList = new ArrayList<>();
    private String messageId, viewerRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_message_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Message Detail");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSenderInfo = findViewById(R.id.tvSenderInfo);
        tvContactInfo = findViewById(R.id.tvContactInfo);
        tvOriginalMessage = findViewById(R.id.tvOriginalMessage);
        tvMessageTime = findViewById(R.id.tvMessageTime);
        recyclerReplies = findViewById(R.id.recyclerReplies);
        etReply = findViewById(R.id.etReply);
        btnSendReply = findViewById(R.id.btnSendReply);

        messageId = getIntent().getStringExtra("messageId");
        viewerRole = getIntent().getStringExtra("viewerRole");
        if (viewerRole == null)
            viewerRole = "User";

        messageRef = FirebaseDatabase.getInstance().getReference("ContactAdmin").child(messageId);

        replyAdapter = new ReplyAdapter(replyList, viewerRole);
        recyclerReplies.setLayoutManager(new LinearLayoutManager(this));
        recyclerReplies.setAdapter(replyAdapter);

        loadMessageDetails();
        loadReplies();

        btnSendReply.setOnClickListener(v -> sendReply());
    }

    private void loadMessageDetails() {
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("senderName").getValue(String.class);
                String role = snapshot.child("senderRole").getValue(String.class);
                String email = snapshot.child("senderEmail").getValue(String.class);
                String mobile = snapshot.child("senderMobile").getValue(String.class);
                String msg = snapshot.child("message").getValue(String.class);
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                String std = snapshot.child("std").getValue(String.class);
                String stream = snapshot.child("stream").getValue(String.class);

                tvSenderInfo.setText((name != null ? name : "Unknown") + " (" + (role != null ? role : "") + ")");

                StringBuilder contact = new StringBuilder();
                if (email != null && !email.isEmpty())
                    contact.append("Email: ").append(email);
                if (mobile != null && !mobile.isEmpty()) {
                    if (contact.length() > 0)
                        contact.append(" | ");
                    contact.append("Mobile: ").append(mobile);
                }
                if (std != null && !std.isEmpty()) {
                    if (contact.length() > 0)
                        contact.append(" | ");
                    contact.append("Std: ").append(std);
                }
                if (stream != null && !stream.isEmpty()) {
                    contact.append(", ").append(stream);
                }
                tvContactInfo.setText(contact.toString());

                tvOriginalMessage.setText(msg != null ? msg : "");
                if (ts != null) {
                    tvMessageTime.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(new Date(ts)));
                }

                // If admin is viewing, mark as read
                if ("Admin".equals(viewerRole)) {
                    messageRef.child("read").setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactMessageDetailActivity.this, "Error loading message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReplies() {
        messageRef.child("replies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                replyList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String sender = child.child("sender").getValue(String.class);
                    String msg = child.child("message").getValue(String.class);
                    Long ts = child.child("timestamp").getValue(Long.class);
                    Boolean read = child.child("read").getValue(Boolean.class);

                    replyList.add(new ReplyAdapter.ReplyItem(
                            key,
                            sender != null ? sender : "Unknown",
                            msg != null ? msg : "",
                            ts != null ? ts : 0,
                            read != null ? read : false));

                    // Mark reply as read if it's from the other party
                    if ("Admin".equals(viewerRole) && "User".equals(sender) && (read == null || !read)) {
                        child.getRef().child("read").setValue(true);
                    } else if ("User".equals(viewerRole) && "Admin".equals(sender) && (read == null || !read)) {
                        child.getRef().child("read").setValue(true);
                    }
                }
                replyAdapter.notifyDataSetChanged();
                if (!replyList.isEmpty()) {
                    recyclerReplies.scrollToPosition(replyList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendReply() {
        String replyText = etReply.getText() != null ? etReply.getText().toString().trim() : "";
        if (replyText.isEmpty()) {
            Toast.makeText(this, "Please type a reply", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> replyData = new HashMap<>();
        replyData.put("sender", viewerRole); // "Admin" or "User"
        replyData.put("message", replyText);
        replyData.put("timestamp", System.currentTimeMillis());
        replyData.put("read", false);

        messageRef.child("replies").push().setValue(replyData)
                .addOnSuccessListener(unused -> {
                    etReply.setText("");
                    Toast.makeText(this, "Reply sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
