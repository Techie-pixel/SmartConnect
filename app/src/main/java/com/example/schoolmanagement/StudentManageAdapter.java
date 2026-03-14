package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StudentManageAdapter extends BaseAdapter {

    public interface OnDeleteClick {
        void onDeleteStudent(StudentRow item);
    }

    Context context;
    List<StudentRow> full;
    List<StudentRow> filtered;
    OnDeleteClick callback;

    public StudentManageAdapter(Context context, List<StudentRow> data, OnDeleteClick callback) {
        this.context = context;
        this.full = new ArrayList<>(data);
        this.filtered = new ArrayList<>(data);
        this.callback = callback;
    }

    public void updateData(List<StudentRow> data) {
        this.full.clear();
        this.full.addAll(data);
        filterText("");
    }

    public void filterText(String q) {
        String query = q == null ? "" : q.trim().toLowerCase();
        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(full);
        } else {
            for (StudentRow s : full) {
                if ((s.name != null && s.name.toLowerCase().contains(query)) ||
                        (s.email != null && s.email.toLowerCase().contains(query)) ||
                        (s.stream != null && s.stream.toLowerCase().contains(query)) ||
                        (s.standard != null && s.standard.toLowerCase().contains(query))) {
                    filtered.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filtered.size();
    }

    @Override
    public Object getItem(int position) {
        return filtered.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_student_manage, parent, false);
            h = new ViewHolder();
            h.name = convertView.findViewById(R.id.tvStudentName);
            h.email = convertView.findViewById(R.id.tvStudentEmail);
            h.stdStream = convertView.findViewById(R.id.tvStudentStdStream);
            h.delete = convertView.findViewById(R.id.btnDeleteStudent);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        StudentRow item = filtered.get(position);
        h.name.setText(item.name != null ? item.name : "");
        h.email.setText(item.email != null ? item.email : "");
        String info = (item.standard != null ? item.standard : "") + " • " + (item.stream != null ? item.stream : "");
        h.stdStream.setText(info.trim());
        h.delete.setOnClickListener(v -> {
            if (callback != null) callback.onDeleteStudent(item);
        });
        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView stdStream;
        ImageButton delete;
    }
}

class StudentRow {
    String id;
    String name;
    String email;
    String standard;
    String stream;
    StudentRow(String id, String name, String email, String standard, String stream) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.standard = standard;
        this.stream = stream;
    }
}
