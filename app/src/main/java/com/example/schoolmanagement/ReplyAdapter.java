package com.example.schoolmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    public static class ReplyItem {
        public String sender; // "Admin" or "User"
        public String message;
        public long timestamp;
        public boolean read;
        public String key;

        public ReplyItem(String key, String sender, String message, long timestamp, boolean read) {
            this.key = key;
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }
    }

    private final List<ReplyItem> replies;
    private final String viewerRole; // "Admin" or "User"

    public ReplyAdapter(List<ReplyItem> replies, String viewerRole) {
        this.replies = replies;
        this.viewerRole = viewerRole;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        ReplyItem item = replies.get(position);
        String timeStr = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(new Date(item.timestamp));

        if ("Admin".equals(item.sender)) {
            holder.adminBubble.setVisibility(View.VISIBLE);
            holder.userBubble.setVisibility(View.GONE);
            holder.tvAdminMessage.setText(item.message);
            holder.tvAdminTime.setText(timeStr);
            // If viewer is admin, show "You" label, else "Admin"
            holder.tvAdminLabel.setText("Admin".equals(viewerRole) ? "You" : "Admin");
        } else {
            holder.userBubble.setVisibility(View.VISIBLE);
            holder.adminBubble.setVisibility(View.GONE);
            holder.tvUserMessage.setText(item.message);
            holder.tvUserTime.setText(timeStr);
            // If viewer is user, show "You", else show sender role
            holder.tvUserLabel.setText("Admin".equals(viewerRole) ? "User" : "You");
        }
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout adminBubble, userBubble;
        TextView tvAdminLabel, tvAdminMessage, tvAdminTime;
        TextView tvUserLabel, tvUserMessage, tvUserTime;

        ReplyViewHolder(@NonNull View v) {
            super(v);
            adminBubble = v.findViewById(R.id.adminBubble);
            userBubble = v.findViewById(R.id.userBubble);
            tvAdminLabel = v.findViewById(R.id.tvAdminLabel);
            tvAdminMessage = v.findViewById(R.id.tvAdminMessage);
            tvAdminTime = v.findViewById(R.id.tvAdminTime);
            tvUserLabel = v.findViewById(R.id.tvUserLabel);
            tvUserMessage = v.findViewById(R.id.tvUserMessage);
            tvUserTime = v.findViewById(R.id.tvUserTime);
        }
    }
}
