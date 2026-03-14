package com.example.schoolmanagement;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttachmentAdapter extends RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder> {

    private List<Uri> attachmentUris;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onRemoveClick(int position);
    }

    public AttachmentAdapter(List<Uri> attachmentUris, OnItemClickListener listener) {
        this.attachmentUris = attachmentUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        Uri uri = attachmentUris.get(position);

        String fileName = getFileNameFromUri(holder.itemView.getContext().getContentResolver(), uri);
        holder.tvFileName.setText(fileName);

        String mimeType = holder.itemView.getContext().getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                holder.ivFileIcon.setImageURI(uri);
                holder.ivFileIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else if ("application/pdf".equals(mimeType)) {
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_upload);
                holder.ivFileIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            } else {
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
                holder.ivFileIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        } else {
            holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.ivFileIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(pos);
                }
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onRemoveClick(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return attachmentUris.size();
    }

    private String getFileNameFromUri(ContentResolver contentResolver, Uri uri) {
        String fileName = "Unknown File";

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if ("Unknown File".equals(fileName)) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < path.length() - 1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }

        if ("Unknown File".equals(fileName)) {
            fileName = "File " + (attachmentUris.indexOf(uri) + 1);
        }

        return fileName;
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName;
        ImageButton btnRemove;

        public AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
