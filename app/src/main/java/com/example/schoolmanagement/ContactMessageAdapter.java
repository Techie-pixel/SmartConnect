package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactMessageAdapter extends RecyclerView.Adapter<ContactMessageAdapter.ViewHolder> {

    public static class MessageItem {
        public String key, senderName, senderEmail, senderMobile, senderRole, senderUid, message, std, stream;
        public long timestamp;
        public boolean read;

        public MessageItem(String key, String senderName, String senderEmail, String senderMobile,
                String senderRole, String senderUid, String message, String std, String stream,
                long timestamp, boolean read) {
            this.key = key;
            this.senderName = senderName;
            this.senderEmail = senderEmail;
            this.senderMobile = senderMobile;
            this.senderRole = senderRole;
            this.senderUid = senderUid;
            this.message = message;
            this.std = std;
            this.stream = stream;
            this.timestamp = timestamp;
            this.read = read;
        }
    }

    private final Context context;
    private final List<MessageItem> items;

    public ContactMessageAdapter(Context context, List<MessageItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageItem item = items.get(position);

        holder.tvSenderName.setText(item.senderName != null ? item.senderName : "Unknown");
        holder.tvRoleBadge.setText(item.senderRole != null ? item.senderRole : "");
        holder.tvMessagePreview.setText(item.message != null ? item.message : "");

        String timeStr = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(new Date(item.timestamp));
        holder.tvTimestamp.setText(timeStr);

        // Unread dot
        if (item.read) {
            holder.unreadDot.setVisibility(View.GONE);
            holder.tvSenderName.setAlpha(0.7f);
        } else {
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.tvSenderName.setAlpha(1.0f);
        }

        // Toggle read/unread
        holder.btnToggleRead.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            FirebaseDatabase.getInstance().getReference("ContactAdmin")
                    .child(item.key).child("read").setValue(!item.read)
                    .addOnSuccessListener(
                            unused -> Toast.makeText(context, item.read ? "Marked as unread" : "Marked as read",
                                    Toast.LENGTH_SHORT).show());
        });

        // Delete
        holder.btnDelete.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            new AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setMessage("Delete this message from " + item.senderName + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        FirebaseDatabase.getInstance().getReference("ContactAdmin")
                                .child(item.key).removeValue()
                                .addOnSuccessListener(
                                        unused -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(
                                        e -> Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Click to open detail
        holder.itemView.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(() -> {
                Intent intent = new Intent(context, ContactMessageDetailActivity.class);
                intent.putExtra("messageId", item.key);
                intent.putExtra("viewerRole", "Admin");
                context.startActivity(intent);
            }, 100);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View unreadDot;
        TextView tvSenderName, tvRoleBadge, tvMessagePreview, tvTimestamp;
        ImageButton btnToggleRead, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            unreadDot = v.findViewById(R.id.unreadDot);
            tvSenderName = v.findViewById(R.id.tvSenderName);
            tvRoleBadge = v.findViewById(R.id.tvRoleBadge);
            tvMessagePreview = v.findViewById(R.id.tvMessagePreview);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
            btnToggleRead = v.findViewById(R.id.btnToggleRead);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
