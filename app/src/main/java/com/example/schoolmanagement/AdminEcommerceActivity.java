package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import android.view.animation.Animation;

public class AdminEcommerceActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 601;
    private static final int GALLERY_REQUEST = 602;
    private static final int CAMERA_PERMISSION_CODE = 700;

    private ListView listView;
    private Button btnAddItem;
    private final ArrayList<EcomItem> items = new ArrayList<>();
    private EcomAdapter adapter;
    private DatabaseReference ecomRef;

    // Dialog references kept alive across onActivityResult
    private AlertDialog currentDialog;
    private ImageView dialogImageView;
    private String tempImageBase64 = null;

    // Track pending action after permission grant
    private int pendingAction = 0; // 0=none, 1=camera, 2=gallery

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ecommerce);

        listView = findViewById(R.id.ecommerceListView);
        btnAddItem = findViewById(R.id.btnAddItem);

        ecomRef = FirebaseDatabase.getInstance().getReference("Ecommerce");

        adapter = new EcomAdapter();
        listView.setAdapter(adapter);

        btnAddItem.setOnClickListener(v -> openAddItemDialog());
        btnAddItem.setOnClickListener(v -> openAddItemDialog());
        loadItems();

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = { listView, btnAddItem };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(android.view.View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(android.view.View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                views[i].startAnimation(anim);
            }
        }
    }

    private void loadItems() {
        ecomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                items.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    int price = 0;
                    Long priceLong = child.child("price").getValue(Long.class);
                    if (priceLong != null)
                        price = priceLong.intValue();
                    String image = child.child("imageBase64").getValue(String.class);
                    items.add(new EcomItem(key, name, price, image));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminEcommerceActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAddItemDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_add_item, null, false);
        EditText etName = view.findViewById(R.id.etDialogName);
        EditText etPrice = view.findViewById(R.id.etDialogPrice);
        dialogImageView = view.findViewById(R.id.ivDialogImage);
        Button btnPick = view.findViewById(R.id.btnDialogPickImage);
        Button btnAdd = view.findViewById(R.id.btnDialogAdd);

        tempImageBase64 = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setCancelable(true);
        currentDialog = builder.create();
        if (currentDialog.getWindow() != null) {
            currentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        currentDialog.show();

        // Image picker with runtime permission check
        btnPick.setOnClickListener(v -> {
            String[] options = { "Camera", "Gallery" };
            new AlertDialog.Builder(this)
                    .setTitle("Select Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openCamera();
                        } else {
                            openGallery();
                        }
                    }).show();
        });

        // Add button
        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Enter name and price", Toast.LENGTH_SHORT).show();
                return;
            }
            int price;
            try {
                price = Integer.parseInt(priceStr);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("imageBase64", tempImageBase64 != null ? tempImageBase64 : "");

            ecomRef.push().setValue(data)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Item added!", Toast.LENGTH_SHORT).show();
                        currentDialog.dismiss();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // ── Camera / Gallery with runtime permissions ──
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = 1;
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        } else {
            launchCamera();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAction == 1)
                    launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
            pendingAction = 0;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        Bitmap photo = null;

        try {
            if (requestCode == CAMERA_REQUEST) {
                if (data != null && data.getExtras() != null) {
                    photo = (Bitmap) data.getExtras().get("data");
                }
            } else if (requestCode == GALLERY_REQUEST && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream != null) {
                        photo = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photo != null) {
            photo = scaleBitmap(photo, 300);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            tempImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            if (dialogImageView != null) {
                dialogImageView.setImageBitmap(photo);
            }
            Toast.makeText(this, "Image selected ✅", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleBitmap(Bitmap original, int maxDim) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= maxDim && h <= maxDim)
            return original;
        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
        return Bitmap.createScaledBitmap(original, (int) (w * ratio), (int) (h * ratio), true);
    }

    private void deleteItem(EcomItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"" + item.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    ecomRef.child(item.key).removeValue()
                            .addOnSuccessListener(a -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Adapter ──
    private class EcomAdapter extends BaseAdapter {
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
                convertView = LayoutInflater.from(AdminEcommerceActivity.this)
                        .inflate(R.layout.item_admin_ecommerce, parent, false);
            }
            EcomItem item = items.get(position);
            ImageView iv = convertView.findViewById(R.id.ivItemImage);
            TextView tvName = convertView.findViewById(R.id.tvItemName);
            TextView tvPrice = convertView.findViewById(R.id.tvItemPrice);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteItem);

            tvName.setText(item.name != null ? item.name : "");
            tvPrice.setText("₹" + item.price);

            if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(item.imageBase64, Base64.DEFAULT);
                    iv.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                } catch (Exception e) {
                    iv.setImageResource(R.drawable.ic_default_profile);
                }
            } else {
                iv.setImageResource(R.drawable.ic_default_profile);
            }

            btnDelete.setOnClickListener(v -> deleteItem(item));
            return convertView;
        }
    }

    // ── Model ──
    static class EcomItem {
        String key, name, imageBase64;
        int price;

        EcomItem(String key, String name, int price, String imageBase64) {
            this.key = key;
            this.name = name;
            this.price = price;
            this.imageBase64 = imageBase64;
        }
    }
}
