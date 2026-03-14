package com.example.schoolmanagement;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ChatAdapter extends BaseAdapter {

    Context context;
    List<studentchat.ChatMessage> messageList;
    String myUid;
    String otherUid;

    public ChatAdapter(Context context, List<studentchat.ChatMessage> messageList,
                       String myUid, String otherUid) {
        this.context = context;
        this.messageList = messageList;
        this.myUid = myUid;
        this.otherUid = otherUid;
    }

    @Override
    public int getCount() { return messageList.size(); }

    @Override
    public Object getItem(int position) { return messageList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        studentchat.ChatMessage msg = messageList.get(position);

        if (convertView == null)
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.chat_item, parent, false);

        TextView msgText = convertView.findViewById(R.id.msgText);
        ImageView msgImage = convertView.findViewById(R.id.msgImage);

        // RESET LISTENERS
        msgText.setOnLongClickListener(null);
        msgImage.setOnLongClickListener(null);
        convertView.setOnLongClickListener(null);

        // TEXT MESSAGE
        if (msg.text != null) {

            msgText.setVisibility(View.VISIBLE);
            msgImage.setVisibility(View.GONE);
            msgText.setText(msg.text);

            msgText.setOnLongClickListener(v -> {
                showUnsendDialog(position, msg);
                return true;
            });

        }
        // IMAGE MESSAGE
        else if (msg.imageBase64 != null) {

            byte[] bytes = Base64.decode(msg.imageBase64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            msgImage.setVisibility(View.VISIBLE);
            msgText.setVisibility(View.GONE);
            msgImage.setImageBitmap(bmp);

            // PREVIEW
            msgImage.setOnClickListener(v -> {
                try {
                    File cacheFile = new File(context.getCacheDir(),
                            "preview_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();

                    Intent intent = new Intent(context, ImagePreviewActivity.class);
                    intent.putExtra("imagePath", cacheFile.getAbsolutePath());
                    context.startActivity(intent);

                } catch (Exception ignored) {}
            });

            // IMAGE LONG PRESS (UNSEND)
            msgImage.setOnLongClickListener(v -> {
                showUnsendDialog(position, msg);
                return true;
            });

        }

        // BUBBLE STYLE
        if (msg.senderUid.equals(myUid))
            convertView.setBackgroundResource(R.drawable.bg_msg_sent);
        else
            convertView.setBackgroundResource(R.drawable.bg_msg_received);

        return convertView;
    }

    private void showUnsendDialog(int position, studentchat.ChatMessage msg) {

        new AlertDialog.Builder(context)
                .setTitle("Unsend Message?")
                .setItems(new String[]{"Unsend"}, (dialog, which) -> {

                    String chatKey = studentchat.makeChatKey(myUid, otherUid);

                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("Chats")
                            .child(chatKey)
                            .child("messages")
                            .child(msg.key);

                    ref.removeValue();

                    messageList.remove(position);
                    notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
