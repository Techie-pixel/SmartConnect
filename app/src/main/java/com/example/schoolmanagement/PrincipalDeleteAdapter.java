package com.example.schoolmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class PrincipalDeleteAdapter extends BaseAdapter {

    public interface OnDeleteClick {
        void onDeleteClick(PrincipalRow item);
    }

    Context context;
    List<PrincipalRow> data;
    OnDeleteClick callback;

    public PrincipalDeleteAdapter(Context context, List<PrincipalRow> data, OnDeleteClick callback) {
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
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_principal_delete, parent, false);
            h = new ViewHolder();
            h.name = convertView.findViewById(R.id.tvPrincipalName);
            h.email = convertView.findViewById(R.id.tvPrincipalEmail);
            h.idMobile = convertView.findViewById(R.id.tvPrincipalIdMobile);
            h.delete = convertView.findViewById(R.id.btnDeletePrincipal);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        PrincipalRow item = data.get(position);
        h.name.setText(item.name != null ? item.name : "");
        h.email.setText(item.email != null ? item.email : "");
        h.idMobile.setText("ID: " + item.id + " • " + (item.mobile != null ? item.mobile : ""));

        h.delete.setOnClickListener(v -> {
            if (callback != null) callback.onDeleteClick(item);
        });
        return convertView;
    }

    static class ViewHolder {
        TextView name;
        TextView email;
        TextView idMobile;
        ImageButton delete;
    }
}
