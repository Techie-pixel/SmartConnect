package com.example.schoolmanagement;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TeacherAdapter extends ArrayAdapter<TeacherModel> {

    private List<TeacherModel> fullList;

    public TeacherAdapter(@NonNull Context context, @NonNull List<TeacherModel> objects) {
        super(context, 0, objects);
        fullList = new ArrayList<>(objects);
    }

    public void updateFullList() {
        fullList = new ArrayList<>(this.getCount());
        for (int i = 0; i < this.getCount(); i++) {
            fullList.add(getItem(i));
        }
    }

    public void filterText(String query) {
        query = query.toLowerCase();
        clear();
        for (TeacherModel t : fullList) {
            if (t.getName() != null && t.getName().toLowerCase().contains(query)
                    || t.getSubject() != null && t.getSubject().toLowerCase().contains(query)) {
                add(t);
            }
        }
        notifyDataSetChanged();
    }

    public void filterCategory(String category) {
        category = category.toLowerCase();
        clear();
        for (TeacherModel t : fullList) {
            boolean match = false;
            if (category.equals("all")) match = true;
            else if (category.equals("science") && "Science".equalsIgnoreCase(t.getStream())) match = true;
            else if (category.equals("commerce") && "Commerce".equalsIgnoreCase(t.getStream())) match = true;
            else if (category.equals("arts") && "Arts".equalsIgnoreCase(t.getStream())) match = true;
            else if (category.equals("11") && "11".equals(t.getStandard())) match = true;
            else if (category.equals("12") && "12".equals(t.getStandard())) match = true;

            if (match) add(t);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_teacher, parent, false);
        }

        TeacherModel t = getItem(position);

        ImageView img = convertView.findViewById(R.id.imgTeacher);
        TextView tvName = convertView.findViewById(R.id.tvTeacherName);
        TextView tvSub = convertView.findViewById(R.id.tvTeacherSubject);
        TextView tvClsStr = convertView.findViewById(R.id.tvTeacherClassStream);

        if (t != null) {
            // Handle null values with defaults
            String name = t.getName();
            String subject = t.getSubject();
            String standard = t.getStandard();
            String stream = t.getStream();
            
            tvName.setText(name != null ? name : "Unknown");
            tvSub.setText(subject != null ? subject : "No Subject");
            
            String classStreamText = "";
            if (standard != null && !standard.isEmpty()) {
                classStreamText += "Class " + standard;
            }
            if (stream != null && !stream.isEmpty()) {
                if (!classStreamText.isEmpty()) classStreamText += " • ";
                classStreamText += stream;
            }
            if (classStreamText.isEmpty()) {
                classStreamText = "No Class Info";
            }
            tvClsStr.setText(classStreamText);

            if (t.getProfileBase64() != null && !t.getProfileBase64().isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(t.getProfileBase64(), Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    img.setImageBitmap(bmp);
                } catch (Exception e) {
                    img.setImageResource(R.drawable.ic_default_profile);
                }
            } else {
                img.setImageResource(R.drawable.ic_default_profile);
            }
        }

        return convertView;
    }
}
