package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class teacherslist extends AppCompatActivity {

    ListView teacherListView;
    EditText teacherSearchBar;

    ArrayList<TeacherModel> teacherList;
    TeacherAdapter teacherAdapter;

    ArrayList<RecentChatModel> recentChats;
    RecentChatAdapter recentChatAdapter;

    Button tAllBtn, tSciBtn, tComBtn, tArtBtn, tEleventhBtn, tTwelfthBtn, tRecentChatsBtn;

    String myUid; // current logged-in teacher ka uid

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacherslist);

        teacherSearchBar = findViewById(R.id.teacherSearchBar);
        teacherListView = findViewById(R.id.teacherListView);

        tAllBtn = findViewById(R.id.tAllBtn);
        tSciBtn = findViewById(R.id.tSciBtn);
        tComBtn = findViewById(R.id.tComBtn);
        tArtBtn = findViewById(R.id.tArtBtn);
        tEleventhBtn = findViewById(R.id.tEleventhBtn);
        tTwelfthBtn = findViewById(R.id.tTwelfthBtn);
        tRecentChatsBtn = findViewById(R.id.tRecentChatsBtn);

        // Check TeacherPrefs first, then PrincipalPrefs
        android.content.SharedPreferences tPrefs = getSharedPreferences("TeacherPrefs", MODE_PRIVATE);
        myUid = tPrefs.getString("teacherId", null);
        if (myUid == null || myUid.isEmpty()) {
            android.content.SharedPreferences pPrefs = getSharedPreferences("PrincipalPrefs", MODE_PRIVATE);
            myUid = pPrefs.getString("principalId", null);
        }

        teacherList = new ArrayList<>();
        teacherAdapter = new TeacherAdapter(this, teacherList);

        recentChats = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(this, recentChats);

        teacherListView.setAdapter(teacherAdapter);

        loadAllTeachers();

        // Search by name / subject
        teacherSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                teacherAdapter.filterText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Filter buttons — switch back to teacher adapter first
        tAllBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("all");
            }, 100);
        });
        tSciBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("Science");
            }, 100);
        });
        tComBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("Commerce");
            }, 100);
        });
        tArtBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("Arts");
            }, 100);
        });
        tEleventhBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("11");
            }, 100);
        });
        tTwelfthBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(teacherAdapter);
                teacherAdapter.filterCategory("12");
            }, 100);
        });

        // Recent Chats button
        tRecentChatsBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                teacherListView.setAdapter(recentChatAdapter);
                loadRecentChats();
            }, 100);
        });

        // Item click → Show options dialog (View Profile / Chat) or open recent chat
        teacherListView.setOnItemClickListener((parent, view, position, id) -> {

            // If showing recent chats, open chat directly
            if (teacherListView.getAdapter() == recentChatAdapter) {
                RecentChatModel rc = recentChats.get(position);
                Intent intent = new Intent(teacherslist.this, teacherchat.class);
                intent.putExtra("otherUid", rc.getUid());
                intent.putExtra("otherName", rc.getName());
                startActivity(intent);
                return;
            }

            // Normal teacher list item
            TeacherModel t = teacherAdapter.getItem(position);
            if (t == null)
                return;

            String otherId = t.getId();

            if (myUid != null && myUid.equals(otherId)) {
                Toast.makeText(this, "This is your own profile", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose Action")
                    .setItems(new String[] { "View Profile", "Chat" }, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(this, teacherprofileview.class);
                            intent.putExtra("teacherId", otherId);
                            intent.putExtra("teacherName", t.getName());
                            intent.putExtra("teacherSubject", t.getSubject());
                            intent.putExtra("teacherClass", t.getStandard());
                            intent.putExtra("teacherStream", t.getStream());
                            // profileBase64 removed — loaded from Firebase in teacherprofileview
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(this, teacherchat.class);
                            intent.putExtra("otherUid", otherId);
                            intent.putExtra("otherName", t.getName());
                            startActivity(intent);
                        }
                    }).show();
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Prevent Android from saving the huge adapter state (Base64 images)
        // which causes TransactionTooLargeException
        super.onSaveInstanceState(new Bundle());
    }

    private void loadAllTeachers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Teachers");

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                teacherList.clear();

                for (DataSnapshot teacherSnap : task.getResult().getChildren()) {
                    String id = teacherSnap.getKey();
                    String profileBase64 = teacherSnap.child("ProfileImageBase64").getValue(String.class);

                    // Teachers data is stored under push-key children:
                    // Teachers/{teacherId}/{pushKey} → {name, email, stream, class, subject}
                    String name = null, std = null, stream = null, subject = null;

                    // First try direct child fields (flat structure)
                    name = teacherSnap.child("name").getValue(String.class);
                    if (name == null)
                        name = teacherSnap.child("Name").getValue(String.class);

                    // If no direct name found, look inside push-key children
                    if (name == null) {
                        StringBuilder subjectBuilder = new StringBuilder();
                        boolean first = true;

                        for (DataSnapshot pushChild : teacherSnap.getChildren()) {
                            String key = pushChild.getKey();
                            if (key != null && key.startsWith("-")) {
                                // This is a push-key child
                                if (name == null) {
                                    name = pushChild.child("name").getValue(String.class);
                                    std = pushChild.child("class").getValue(String.class);
                                    if (std == null)
                                        std = pushChild.child("standard").getValue(String.class);
                                    stream = pushChild.child("stream").getValue(String.class);
                                }
                                // Collect all subjects
                                String sub = pushChild.child("subject").getValue(String.class);
                                if (sub != null && !sub.isEmpty()) {
                                    if (!first)
                                        subjectBuilder.append(", ");
                                    subjectBuilder.append(sub);
                                    first = false;
                                }
                                if (profileBase64 == null) {
                                    profileBase64 = pushChild.child("ProfileImageBase64").getValue(String.class);
                                }
                            }
                        }
                        subject = subjectBuilder.length() > 0 ? subjectBuilder.toString() : null;
                    } else {
                        // Flat structure — read directly
                        std = teacherSnap.child("class").getValue(String.class);
                        if (std == null)
                            std = teacherSnap.child("standard").getValue(String.class);
                        if (std == null)
                            std = teacherSnap.child("Standard").getValue(String.class);
                        stream = teacherSnap.child("stream").getValue(String.class);
                        if (stream == null)
                            stream = teacherSnap.child("Stream").getValue(String.class);
                        subject = teacherSnap.child("subject").getValue(String.class);
                        if (subject == null)
                            subject = teacherSnap.child("Subject").getValue(String.class);
                    }

                    if (profileBase64 == null)
                        profileBase64 = teacherSnap.child("profileImageBase64").getValue(String.class);

                    teacherList.add(new TeacherModel(id, name, std, stream, subject, profileBase64));
                }

                // After loading teachers, also load principals
                loadAllPrincipals();
            }
        });
    }

    private void loadAllPrincipals() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Principals");

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    String id = snap.getKey();
                    String name = snap.child("name").getValue(String.class);
                    if (name == null)
                        name = snap.child("Name").getValue(String.class);

                    String profileBase64 = snap.child("ProfileImageBase64").getValue(String.class);

                    // Add principal as a TeacherModel with "Principal" as subject
                    teacherList.add(new TeacherModel(id, name, null, null, "Principal", profileBase64));
                }

                teacherAdapter.updateFullList();
                teacherAdapter.notifyDataSetChanged();
            }
        });
    }

    // Recent chats loader — matches studentslist pattern
    private void loadRecentChats() {
        if (myUid == null)
            return;
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("Chats");
        recentChats.clear();
        chatsRef.get().addOnSuccessListener(snapshot -> {
            for (DataSnapshot chatSnap : snapshot.getChildren()) {
                String chatKey = chatSnap.getKey();
                if (chatKey != null && chatKey.contains(myUid) && chatKey.split("_").length == 2) {
                    final String[] split = chatKey.split("_");
                    final String otherUid = split[0].equals(myUid) ? split[1] : split[0];

                    final String[] lastMsgArr = { "" };
                    DataSnapshot messagesSnap = chatSnap.child("messages");
                    if (messagesSnap.getChildrenCount() > 0) {
                        for (DataSnapshot msgSnap : messagesSnap.getChildren()) {
                            String text = msgSnap.child("text").getValue(String.class);
                            if (text != null && !text.isEmpty())
                                lastMsgArr[0] = text;
                        }
                    }

                    // Try Teachers node first, fallback to Principal
                    DatabaseReference teacherRef = FirebaseDatabase.getInstance().getReference("Teachers")
                            .child(otherUid);
                    teacherRef.get().addOnSuccessListener(tSnap -> {
                        String name = tSnap.child("name").getValue(String.class);
                        if (name == null)
                            name = tSnap.child("Name").getValue(String.class);
                        if (name == null)
                            name = tSnap.child("teacherName").getValue(String.class);

                        if (name != null) {
                            recentChats.add(new RecentChatModel(otherUid, name, lastMsgArr[0]));
                            recentChatAdapter.notifyDataSetChanged();
                        } else {
                            // Check Principal node as fallback
                            DatabaseReference principalRef = FirebaseDatabase.getInstance().getReference("Principal")
                                    .child(otherUid);
                            principalRef.get().addOnSuccessListener(pSnap -> {
                                String pName = pSnap.child("name").getValue(String.class);
                                if (pName == null)
                                    pName = pSnap.child("Name").getValue(String.class);
                                recentChats.add(new RecentChatModel(otherUid, pName != null ? pName : "Unknown",
                                        lastMsgArr[0]));
                                recentChatAdapter.notifyDataSetChanged();
                            });
                        }
                    });
                }
            }
        });
    }
}
