package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class FeedbackAdapter extends BaseAdapter {

    public interface OnDeleteClick {
        void onDeleteClick(FeedbackItem item);
    }

    private Context context;
    private ArrayList<FeedbackItem> items;
    private OnDeleteClick listener;

    public FeedbackAdapter(Context context, ArrayList<FeedbackItem> items, OnDeleteClick listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_feedback, parent, false);
        }

        FeedbackItem item = items.get(position);

        TextView tvName = convertView.findViewById(R.id.tvFeedbackName);
        TextView tvEmail = convertView.findViewById(R.id.tvFeedbackEmail);
        TextView tvContact = convertView.findViewById(R.id.tvFeedbackContact);
        TextView tvFeedback = convertView.findViewById(R.id.tvFeedbackText);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteFeedback);

        tvName.setText(item.name != null ? item.name : "");
        tvEmail.setText(item.email != null ? item.email : "");
        tvContact.setText(item.contact != null ? item.contact : "");
        tvFeedback.setText(item.feedback != null ? item.feedback : "");

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });

        return convertView;
    }
}

class FeedbackItem {
    String key;
    String name;
    String email;
    String contact;
    String feedback;

    FeedbackItem(String key, String name, String email, String contact, String feedback) {
        this.key = key;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.feedback = feedback;
    }
}
