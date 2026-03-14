package com.example.schoolmanagement;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the reply text from RemoteInput
        Bundle remoteInputBundle = RemoteInput.getResultsFromIntent(intent);
        if (remoteInputBundle == null)
            return;

        CharSequence replyText = remoteInputBundle.getCharSequence(ChatNotificationService.KEY_REPLY);
        if (replyText == null || replyText.toString().trim().isEmpty())
            return;

        String chatKey = intent.getStringExtra("chatKey");
        String myUid = intent.getStringExtra("myUid");
        int notifId = intent.getIntExtra("notifId", 0);

        if (chatKey == null || myUid == null)
            return;

        // Send the reply message to Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(chatKey)
                .child("messages");

        String key = ref.push().getKey();
        if (key == null)
            return;

        studentchat.ChatMessage chatMessage = new studentchat.ChatMessage(
                myUid, replyText.toString().trim(), null, null, null, key);
        ref.child(key).setValue(chatMessage);

        // Update the notification to show "Reply sent"
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "chat_notifications")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Reply sent")
                .setContentText(replyText.toString().trim())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }
}
