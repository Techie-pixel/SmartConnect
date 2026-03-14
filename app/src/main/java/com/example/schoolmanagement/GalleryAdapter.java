package com.example.schoolmanagement;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class GalleryAdapter extends BaseAdapter {
    private Context context;
    private List<GalleryItem> galleryItems;
    private LayoutInflater inflater;

    public GalleryAdapter(Context context, List<GalleryItem> galleryItems) {
        this.context = context;
        this.galleryItems = galleryItems;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return galleryItems.size();
    }

    @Override
    public Object getItem(int position) {
        return galleryItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.gallery_item, parent, false);
            holder = new ViewHolder();
            holder.galleryImage = convertView.findViewById(R.id.galleryImage);
            holder.galleryDescription = convertView.findViewById(R.id.galleryDescription);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        GalleryItem item = galleryItems.get(position);

        // Set Image from Base64
        if (item.getImage() != null && !item.getImage().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(item.getImage(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.galleryImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                holder.galleryImage.setImageResource(R.drawable.projectlogo); // fallback
            }
        }

        // Set Description
        if (item.hasDescription()) {
            holder.galleryDescription.setText(item.getDescription());
            holder.galleryDescription.setVisibility(View.VISIBLE);
        } else {
            holder.galleryDescription.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView galleryImage;
        TextView galleryDescription;
    }
}