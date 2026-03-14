package com.example.schoolmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentAdapter extends BaseAdapter {

    Context context;
    List<StudentModel> studentList;
    List<StudentModel> fullList;

    public StudentAdapter(Context context, List<StudentModel> studentList) {
        this.context = context;
        this.studentList = studentList;
        this.fullList = new ArrayList<>(studentList);
    }

    public void updateFullList() {
        fullList = new ArrayList<>(studentList);
    }

    @Override
    public int getCount() { return studentList.size(); }

    @Override
    public Object getItem(int position) { return studentList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.student_item, parent, false);

        StudentModel s = studentList.get(position);

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvStd = convertView.findViewById(R.id.tvStd);
        TextView tvStream = convertView.findViewById(R.id.tvStream);
        CircleImageView profileImage = convertView.findViewById(R.id.profileImage);

        tvName.setText("Name: " + s.getName());
        tvStd.setText("Standard: " + s.getStandard());
        tvStream.setText("Stream: " + s.getStream());

        String base64 = s.getProfileImageBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                profileImage.setImageBitmap(bmp);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_default_profile);
        }

        // ✅ Disable own profile click
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!s.getUid().equals(currentUid)) {
            convertView.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choose Action")
                        .setItems(new String[]{"View Profile", "Chat"}, (dialog, which) -> {
                            if (which == 0) {
                                Intent intent = new Intent(context, studentprofile.class);
                                intent.putExtra("uid", s.getUid());
                                context.startActivity(intent);
                            } else {
                                Intent intent = new Intent(context, studentchat.class);
                                intent.putExtra("otherUid", s.getUid());
                                intent.putExtra("otherName", s.getName());
                                context.startActivity(intent);
                            }
                        }).show();
            });
        } else {
            convertView.setOnClickListener(null); // disabled
        }

        return convertView;
    }

    // ✅ Search Filter and Category Filter
    public void filter(String text) {
        studentList.clear();

        if(text.isEmpty()) {
            studentList.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for(StudentModel s : fullList) {
                if(s.getName().toLowerCase().contains(text)) {
                    studentList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterCategory(String category) {
        studentList.clear();

        if(category.equals("all")) {
            studentList.addAll(fullList);
        }
        else if(category.equals("11") || category.equals("12")) {
            for(StudentModel s : fullList) {
                if(s.getStandard().equalsIgnoreCase(category)) {
                    studentList.add(s);
                }
            }
        }
        else {
            for(StudentModel s : fullList) {
                if(s.getStream().equalsIgnoreCase(category)) {
                    studentList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }
}
