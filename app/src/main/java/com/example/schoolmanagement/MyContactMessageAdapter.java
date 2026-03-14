package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyContactMessageAdapter extends RecyclerView.Adapter<MyContactMessageAdapter.ViewHolder> {

    public static class MyMessageItem {
        public String key, message;
        public long timestamp;
        public boolean hasUnreadReply;

        public MyMessageItem(String key, String message, long timestamp, boolean hasUnreadReply) {
            this.key = key;
            this.message = message;
            this.timestamp = timestamp;
            this.hasUnreadReply = hasUnreadReply;
        }
    }

    private final Context context;
    private final List<MyMessageItem> items;

    public MyContactMessageAdapter(Context context, List<MyMessageItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_contact_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMessageItem item = items.get(position);

        holder.tvMessagePreview.setText(item.message != null ? item.message : "");

        String timeStr = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(new Date(item.timestamp));
        holder.tvTimestamp.setText(timeStr);

        holder.tvUnreadBadge.setVisibility(item.hasUnreadReply ? View.VISIBLE : View.GONE);

        // Tap to open detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ContactMessageDetailActivity.class);
            intent.putExtra("messageId", item.key);
            intent.putExtra("viewerRole", "User");
            context.startActivity(intent);
        });

        // Long press to delete
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Message")
                    .setMessage("Delete this message?")
                    .setPositiveButton("Delete", (d, w) -> {
                        FirebaseDatabase.getInstance().getReference("ContactAdmin")
                                .child(item.key).removeValue()
                                .addOnSuccessListener(
                                        unused -> Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessagePreview, tvTimestamp, tvUnreadBadge;

        ViewHolder(@NonNull View v) {
            super(v);
            tvMessagePreview = v.findViewById(R.id.tvMessagePreview);
            tvTimestamp = v.findViewById(R.id.tvTimestamp);
            tvUnreadBadge = v.findViewById(R.id.tvUnreadBadge);
        }
    }
}
