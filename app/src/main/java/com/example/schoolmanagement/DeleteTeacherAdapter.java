package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class DeleteTeacherAdapter extends BaseAdapter {

    public interface OnDeleteClick {
        void onDeleteClick(TeacherBrief item);
    }

    Context context;
    List<TeacherBrief> data;
    OnDeleteClick callback;

    public DeleteTeacherAdapter(Context context, List<TeacherBrief> data, OnDeleteClick callback) {
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_teacher_delete, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.tvTeacherName);
            holder.email = convertView.findViewById(R.id.tvTeacherEmail);
            holder.idText = convertView.findViewById(R.id.tvTeacherIdText);
            holder.streamSub = convertView.findViewById(R.id.tvTeacherStreamSub);
            holder.delete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TeacherBrief item = data.get(position);
        holder.name.setText(item.name);
        holder.email.setText(item.email != null ? item.email : "");
        holder.idText.setText("ID: " + item.id);
        String streamPart = item.streams != null && !item.streams.isEmpty() ? item.streams : "-";
        String subPart = item.subjects != null && !item.subjects.isEmpty() ? item.subjects : "-";
        holder.streamSub.setText(streamPart + " • " + subPart);
        holder.delete.setOnClickListener(v -> {
            if (callback != null) callback.onDeleteClick(item);
        });
        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView idText;
        TextView streamSub;
        ImageButton delete;
    }
}
