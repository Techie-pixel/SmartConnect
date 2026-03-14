package com.example.schoolmanagement;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.VH> {

    public static class SubmissionItem {
        public String assignmentKey;
        public String assignmentTitle;
        public String studentUid;
        public String studentName;
        public long submittedAt;
        public String status; // checked/unchecked/rejected
        public String itText;
        public String itImage;
    }

    public interface OnMarkListener {
        void onMarkChecked(SubmissionItem item);

        void onOpen(SubmissionItem item);

        void onReject(SubmissionItem item);
    }

    private final List<SubmissionItem> items;
    private final OnMarkListener listener;
    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public SubmissionAdapter(List<SubmissionItem> items, OnMarkListener l) {
        this.items = items;
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SubmissionItem it = items.get(position);

        // Assignment title
        if (it.assignmentTitle != null && !it.assignmentTitle.isEmpty()) {
            h.assignmentTitle.setText(it.assignmentTitle);
            h.assignmentTitle.setVisibility(View.VISIBLE);
        } else {
            h.assignmentTitle.setVisibility(View.GONE);
        }

        h.name.setText(it.studentName);
        h.time.setText(df.format(new Date(it.submittedAt)));

        // Status text with dynamic color
        String st = it.status != null ? it.status : "unchecked";
        String displayStatus = st.substring(0, 1).toUpperCase(Locale.getDefault()) + st.substring(1);
        h.status.setText(displayStatus);

        if ("checked".equals(st)) {
            h.status.setTextColor(Color.parseColor("#2E7D32")); // Green
        } else if ("rejected".equals(st)) {
            h.status.setTextColor(Color.parseColor("#C62828")); // Red
        } else {
            h.status.setTextColor(Color.parseColor("#E65100")); // Orange for unchecked
        }

        // Mark Checked: disabled if already checked OR rejected
        boolean canMark = !"checked".equals(st) && !"rejected".equals(st);
        h.mark.setEnabled(canMark);
        h.mark.setAlpha(canMark ? 1f : 0.4f);

        // Reject: disabled if already checked OR rejected
        boolean canReject = !"checked".equals(st) && !"rejected".equals(st);
        h.reject.setEnabled(canReject);
        h.reject.setAlpha(canReject ? 1f : 0.4f);

        h.mark.setOnClickListener(v -> listener.onMarkChecked(it));
        h.view.setOnClickListener(v -> listener.onOpen(it));
        h.reject.setOnClickListener(v -> listener.onReject(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView assignmentTitle, name, time, status;
        Button mark, view, reject;

        VH(@NonNull View itemView) {
            super(itemView);
            assignmentTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            name = itemView.findViewById(R.id.tvStudentName);
            time = itemView.findViewById(R.id.tvSubmittedAt);
            status = itemView.findViewById(R.id.tvStatus);
            mark = itemView.findViewById(R.id.btnMarkChecked);
            view = itemView.findViewById(R.id.btnView);
            reject = itemView.findViewById(R.id.btnReject);
        }
    }
}
