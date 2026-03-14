package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class DeleteParentAdapter extends BaseAdapter {

    public interface OnDeleteClick {
        void onDeleteClick(ParentBrief item);
    }

    Context context;
    List<ParentBrief> data;
    OnDeleteClick callback;

    public DeleteParentAdapter(Context context, List<ParentBrief> data, OnDeleteClick callback) {
        this.context = context;
        this.data = data;
        this.callback = callback;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_parent_delete, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.tvParentName);
            holder.email = convertView.findViewById(R.id.tvParentEmail);
            holder.details = convertView.findViewById(R.id.tvParentDetails);
            holder.delete = convertView.findViewById(R.id.btnDeleteParent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ParentBrief item = data.get(position);
        holder.name.setText(item.name);
        holder.email.setText(item.email != null ? item.email : "");
        String modePart = item.mode != null && !item.mode.isEmpty() ? item.mode : "-";
        String studentPart = item.studentName != null && !item.studentName.isEmpty() ? item.studentName : "-";
        String mobilePart = item.mobile != null && !item.mobile.isEmpty() ? item.mobile : "-";
        holder.details.setText(modePart + " • " + studentPart + " • " + mobilePart);

        holder.delete.setOnClickListener(v -> {
            if (callback != null) callback.onDeleteClick(item);
        });

        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView details;
        ImageButton delete;
    }
}

