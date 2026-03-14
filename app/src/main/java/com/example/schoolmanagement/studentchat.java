package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class studentchat extends AppCompatActivity {

    ListView chatListView;
    EditText messageBox;
    Button sendBtn;
    ImageButton imageBtn;

    String myUid, otherUid;
    boolean isBlocked = false;
    static final int PICK_IMAGE_MULTIPLE = 102;

    ArrayList<ChatMessage> messageList = new ArrayList<>();
    ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentchat);

        // Start chat notification service
        Intent serviceIntent = new Intent(this, ChatNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        chatListView = findViewById(R.id.chatListView);
        messageBox = findViewById(R.id.messageBox);
        sendBtn = findViewById(R.id.sendBtn);
        imageBtn = findViewById(R.id.imageBtn);

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Suppress notifications for this chat while it's open
        ChatNotificationService.activeChat = studentchat.makeChatKey(myUid,
                getIntent().getStringExtra("otherUid"));
        otherUid = getIntent().getStringExtra("otherUid");

        // FIX: otherUid passed to adapter
        chatAdapter = new ChatAdapter(this, messageList, myUid, otherUid);
        chatListView.setAdapter(chatAdapter);

        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(makeChatKey(myUid, otherUid))
                .child("messages");

        chatRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                messageList.clear();
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    ChatMessage msg = snap.getValue(ChatMessage.class);
                    if (msg != null) {
                        msg.key = snap.getKey();
                        messageList.add(msg);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                chatListView.setSelection(messageList.size() - 1);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
            }
        });

        sendBtn.setOnClickListener(v -> {
            if (isBlocked) {
                Toast.makeText(this, "Unblock user first.", Toast.LENGTH_SHORT).show();
                return;
            }
            String msg = messageBox.getText().toString().trim();
            if (msg.isEmpty())
                return;

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Chats")
                    .child(makeChatKey(myUid, otherUid))
                    .child("messages");

            String key = ref.push().getKey();
            ChatMessage chatMessage = new ChatMessage(myUid, msg, null, null, null, key);
            ref.child(key).setValue(chatMessage);

            messageBox.setText("");
        });

        imageBtn.setOnClickListener(v -> {
            if (isBlocked) {
                Toast.makeText(this, "Unblock first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            Intent chooser = Intent.createChooser(galleryIntent, "Select or Capture");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { cam });

            startActivityForResult(chooser, PICK_IMAGE_MULTIPLE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myUid != null && otherUid != null) {
            ChatNotificationService.activeChat = makeChatKey(myUid, otherUid);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ChatNotificationService.activeChat = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatNotificationService.activeChat = null;
    }

    public static String makeChatKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK) {

            // camera image
            if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                String base64Image = android.util.Base64.encodeToString(baos.toByteArray(),
                        android.util.Base64.DEFAULT);

                sendImageToFirebase(base64Image);
                return;
            }

            ArrayList<Uri> selectedUris = new ArrayList<>();

            if (data != null && data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 4);
                for (int i = 0; i < count; i++) {
                    selectedUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data != null && data.getData() != null) {
                selectedUris.add(data.getData());
            }

            showImagePreviewDialog(selectedUris);
        }
    }

    private void sendImageToFirebase(String base64Image) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(makeChatKey(myUid, otherUid))
                .child("messages");

        String key = ref.push().getKey();
        ChatMessage chatMessage = new ChatMessage(myUid, null, base64Image, null, null, key);

        ref.child(key).setValue(chatMessage);
    }

    private void showImagePreviewDialog(ArrayList<Uri> selectedUris) {
        if (selectedUris.isEmpty())
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Preview and Send");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        for (Uri uri : selectedUris) {
            ImageView img = new ImageView(this);
            img.setImageURI(uri);
            img.setLayoutParams(new LinearLayout.LayoutParams(220, 220));
            layout.addView(img);
        }

        builder.setView(layout);

        builder.setPositiveButton("Send", (d, w) -> {
            for (Uri uri : selectedUris) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);

                    String base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                    sendImageToFirebase(base64);

                } catch (Exception e) {
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public static class ChatMessage {
        public String senderUid, text, imageBase64, reaction, forwardFromUid, key;

        public ChatMessage() {
        }

        public ChatMessage(String senderUid, String text, String imageBase64, String reaction, String forwardFromUid,
                String key) {
            this.senderUid = senderUid;
            this.text = text;
            this.imageBase64 = imageBase64;
            this.reaction = reaction;
            this.forwardFromUid = forwardFromUid;
            this.key = key;
        }
    }
}
