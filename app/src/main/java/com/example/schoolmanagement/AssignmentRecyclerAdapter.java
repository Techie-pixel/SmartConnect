package com.example.schoolmanagement;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentRecyclerAdapter extends RecyclerView.Adapter<AssignmentRecyclerAdapter.VH> {

    public interface OnActionListener {
        void onViewImages(AssignmentListFragment.AssignmentItem item);

        void onSubmit(AssignmentListFragment.AssignmentItem item);
    }

    private final List<AssignmentListFragment.AssignmentItem> items;
    private final OnActionListener listener;
    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public AssignmentRecyclerAdapter(List<AssignmentListFragment.AssignmentItem> items, OnActionListener l) {
        this.items = items;
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AssignmentListFragment.AssignmentItem it = items.get(position);
        h.title.setText(it.title);
        h.teacher.setText(it.teacherName);

        if (it.dueTimestamp > 0) {
            h.due.setText("Due " + df.format(new Date(it.dueTimestamp)));
        } else {
            h.due.setText("");
        }

        // Rejection banner
        if (it.rejectionReason != null && !it.rejectionReason.isEmpty()) {
            h.rejectionBanner.setVisibility(View.VISIBLE);
            h.status.setText("Rejected: " + it.rejectionReason);
            // Change submit button to "Resubmit"
            h.submitBtn.setText("Resubmit");
            h.submitBtn.setEnabled(true);
        } else if (it.submitted) {
            h.rejectionBanner.setVisibility(View.GONE);
            h.submitBtn.setText("Submitted");
            h.submitBtn.setEnabled(false);
            h.submitBtn.setAlpha(0.5f);
        } else {
            h.rejectionBanner.setVisibility(View.GONE);
            h.submitBtn.setText("Submit");
            h.submitBtn.setEnabled(true);
            h.submitBtn.setAlpha(1f);
        }

        // Thumbnail
        if (!it.imageBase64List.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(it.imageBase64List.get(0), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                h.thumb.setImageBitmap(bmp);
            } catch (Exception ignored) {
                h.thumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            h.thumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        h.viewBtn.setOnClickListener(v -> listener.onViewImages(it));
        h.submitBtn.setOnClickListener(v -> listener.onSubmit(it));

        addTouchAnimation(h.viewBtn);
        addTouchAnimation(h.submitBtn);
    }

    private void addTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, teacher, due, status;
        Button viewBtn, submitBtn;
        LinearLayout rejectionBanner;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.ivThumb);
            title = itemView.findViewById(R.id.tvTitle);
            teacher = itemView.findViewById(R.id.tvTeacher);
            due = itemView.findViewById(R.id.tvDue);
            status = itemView.findViewById(R.id.tvStatus);
            viewBtn = itemView.findViewById(R.id.btnView);
            submitBtn = itemView.findViewById(R.id.btnSubmit);
            rejectionBanner = itemView.findViewById(R.id.rejectionBanner);
        }
    }
}
