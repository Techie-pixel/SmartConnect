package com.example.schoolmanagement;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class studentsyllabus extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private TextView usernameText;
    private ImageView userProfileImage;

    private RecyclerView rvSyllabus;
    private TextView tvEmpty;
    private SyllabusAdapter adapter;
    private List<SyllabusItem> allSyllabusList = new ArrayList<>();
    private Set<String> readSyllabusIds = new HashSet<>();

    private String studentId;
    private String studentStandard;
    private String studentStream;

    private LinearLayout filterContainer;
    private TextView chipAll;
    private TextView activeFilterChip;
    private String currentFilter = "All"; // "All", "Unread", "Read"

    private DatabaseReference syllabusRef;
    private DatabaseReference readsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studentsyllabus);

        loadStudentData();
        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupUserProfile();
        setupFilters();

        syllabusRef = FirebaseDatabase.getInstance().getReference("Syllabus");
        readsRef = FirebaseDatabase.getInstance().getReference("SyllabusReads");

        fetchSyllabus();

        LinearLayout mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mainContent.startAnimation(slideUp);
        }
        
        // Animate toolbar
        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView != null) {
            UIAnimator.animateToolbar(toolbarView, 200);
        }
        
        // Animate content after delay
        mainContent.postDelayed(() -> {
            View progressBar = findViewById(R.id.progressBar);
            if (progressBar != null) {
                UIAnimator.animateImageView(progressBar, 500);
            }
        }, 400);
    }

    private void loadStudentData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        studentId = prefs.getString("studentId", "");
        studentStandard = prefs.getString("Standard", "");
        studentStream = prefs.getString("Stream", "");
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);

        rvSyllabus = findViewById(R.id.rvSyllabus);
        tvEmpty = findViewById(R.id.tvEmpty);

        filterContainer = findViewById(R.id.filterContainer);
        chipAll = findViewById(R.id.chipAll);

        rvSyllabus.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SyllabusAdapter();
        rvSyllabus.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Syllabus");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupNavigationDrawer() {
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        if(toggle.getDrawerArrowDrawable() != null) {
            toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        }
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupUserProfile() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            usernameText = headerView.findViewById(R.id.username);
            userProfileImage = headerView.findViewById(R.id.userimage);
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String name = prefs.getString("studentName", "Student");
            if (usernameText != null) {
                usernameText.setText("Welcome, " + name);
            }
        }
    }

    private void setupFilters() {
        activeFilterChip = chipAll;
        chipAll.setOnClickListener(v -> setFilter("All", chipAll));

        addFilterChip("Unread");
        addFilterChip("Read");
    }

    private void addFilterChip(String filterName) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(8);
        chip.setLayoutParams(params);

        chip.setText(filterName);
        chip.setTextColor(Color.parseColor("#A3BFFA"));
        chip.setTextSize(13);
        chip.setBackgroundResource(R.drawable.chip_unselected);
        chip.setPadding(32, 16, 32, 16);

        chip.setOnClickListener(v -> setFilter(filterName, chip));

        filterContainer.addView(chip);
    }

    private void setFilter(String filter, TextView clickedChip) {
        if (activeFilterChip != null) {
            activeFilterChip.setBackgroundResource(R.drawable.chip_unselected);
            activeFilterChip.setTextColor(Color.parseColor("#A3BFFA"));
        }

        activeFilterChip = clickedChip;
        activeFilterChip.setBackgroundResource(R.drawable.chip_selected);
        activeFilterChip.setTextColor(Color.WHITE);

        currentFilter = filter;
        applyFilter();
    }

    private void fetchSyllabus() {
        if (studentStandard.isEmpty() || studentStream.isEmpty() || studentId.isEmpty()) {
            Toast.makeText(this, "Student profile data missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetNode = studentStandard + "_" + studentStream;

        syllabusRef.child(targetNode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSyllabusList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SyllabusItem item = new SyllabusItem();
                    item.id = child.getKey();
                    item.syllabusInfo = child.child("syllabusInfo").getValue(String.class);
                    item.image = child.child("image").getValue(String.class);
                    item.timestamp = child.child("timestamp").getValue(Long.class);
                    allSyllabusList.add(item);
                }

                // Sort descending
                allSyllabusList.sort((a, b) -> {
                    long tA = a.timestamp != null ? a.timestamp : 0L;
                    long tB = b.timestamp != null ? b.timestamp : 0L;
                    return Long.compare(tB, tA);
                });

                fetchReadReceipts(); // Fetch read states before showing
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(studentsyllabus.this, "Failed to load syllabus", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchReadReceipts() {
        readSyllabusIds.clear();
        final int[] callbacksReceived = { 0 };

        if (allSyllabusList.isEmpty()) {
            applyFilter();
            return;
        }

        for (SyllabusItem item : allSyllabusList) {
            readsRef.child(item.id).child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        readSyllabusIds.add(item.id);
                        item.isRead = true;
                    }
                    callbacksReceived[0]++;
                    if (callbacksReceived[0] == allSyllabusList.size()) {
                        applyFilter();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callbacksReceived[0]++;
                    if (callbacksReceived[0] == allSyllabusList.size()) {
                        applyFilter();
                    }
                }
            });
        }
    }

    private void applyFilter() {
        List<SyllabusItem> filteredList = new ArrayList<>();
        for (SyllabusItem item : allSyllabusList) {
            if (currentFilter.equals("All")) {
                filteredList.add(item);
            } else if (currentFilter.equals("Read") && item.isRead) {
                filteredList.add(item);
            } else if (currentFilter.equals("Unread") && !item.isRead) {
                filteredList.add(item);
            }
        }

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No " + currentFilter.toLowerCase() + " syllabus found.");
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        adapter.setItems(filteredList);
    }

    private void markAsRead(String syllabusId) {
        if (studentId == null || studentId.isEmpty())
            return;
        readsRef.child(syllabusId).child(studentId).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    for (SyllabusItem item : allSyllabusList) {
                        if (item.id.equals(syllabusId)) {
                            item.isRead = true;
                            readSyllabusIds.add(syllabusId);
                            break;
                        }
                    }
                    applyFilter();
                });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        try {
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            String filename = "Syllabus_" + System.currentTimeMillis() + ".jpg";
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartConnect");
            }

            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = resolver.openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                if (out != null)
                    out.close();
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreviewFallback(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxHeight(getResources().getDisplayMetrics().heightPixels - 200);

        Log.d("studentsyllabus", "Showing fallback dialog layout parameter");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(20, 20, 20, 20);
        layout.addView(imageView);

        Button downloadBtn = new Button(this);
        downloadBtn.setText("Download Image");
        downloadBtn.setBackgroundColor(Color.parseColor("#1565C0"));
        downloadBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 20, 0, 0);
        downloadBtn.setLayoutParams(btnParams);
        downloadBtn.setOnClickListener(v -> {
            saveImageToGallery(bitmap);
            dialog.dismiss();
        });
        layout.addView(downloadBtn);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    class SyllabusAdapter extends RecyclerView.Adapter<SyllabusAdapter.ViewHolder> {
        List<SyllabusItem> items = new ArrayList<>();

        public void setItems(List<SyllabusItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_syllabus, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SyllabusItem item = items.get(position);

            holder.tvSyllabusTarget.setText("Target: " + studentStandard + " " + studentStream);

            if (item.timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                holder.tvDate.setText(sdf.format(new Date(item.timestamp)));
            } else {
                holder.tvDate.setText("");
            }

            if (item.syllabusInfo != null && !item.syllabusInfo.isEmpty()) {
                holder.tvInfo.setVisibility(View.VISIBLE);
                holder.tvInfo.setText(item.syllabusInfo);
            } else {
                holder.tvInfo.setVisibility(View.GONE);
            }

            if (item.isRead) {
                holder.tvUnreadBadge.setVisibility(View.GONE);
                holder.btnMarkRead.setVisibility(View.GONE);
            } else {
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                holder.btnMarkRead.setVisibility(View.VISIBLE);
                holder.btnMarkRead.setOnClickListener(v -> markAsRead(item.id));
            }

            if (item.image != null && !item.image.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(item.image, Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) {
                        holder.ivImage.setImageBitmap(bmp);
                        holder.ivImage.setVisibility(View.VISIBLE);
                        holder.btnDownload.setVisibility(View.VISIBLE);

                        holder.btnDownload.setOnClickListener(v -> saveImageToGallery(bmp));
                        holder.ivImage.setOnClickListener(v -> {
                            Intent intent = new Intent(studentsyllabus.this, principle_full_image.class);
                            intent.putExtra("image_base64", item.image);
                            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
                        });
                    } else {
                        holder.ivImage.setVisibility(View.GONE);
                        holder.btnDownload.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    holder.ivImage.setVisibility(View.GONE);
                    holder.btnDownload.setVisibility(View.GONE);
                }
            } else {
                holder.ivImage.setVisibility(View.GONE);
                holder.btnDownload.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSyllabusTarget, tvDate, tvInfo, tvUnreadBadge;
            ImageView ivImage;
            Button btnMarkRead, btnDownload;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSyllabusTarget = itemView.findViewById(R.id.tvSyllabusTarget);
                tvDate = itemView.findViewById(R.id.tvSyllabusDate);
                tvInfo = itemView.findViewById(R.id.tvSyllabusInfo);
                tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
                ivImage = itemView.findViewById(R.id.ivSyllabusImage);
                btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
                btnDownload = itemView.findViewById(R.id.btnDownload);
            }
        }
    }

    static class SyllabusItem {
        String id;
        String syllabusInfo;
        String image;
        Long timestamp;
        boolean isRead = false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

        @Override
    public boolean onNavigationItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        android.content.Intent intent = null;

        if (id == R.id.homesection) {
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            if ("11".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, elevensciencehomepage.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, elevencommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, elevenartshome.class);
            } else if ("12".equals(std)) {
                if ("Science".equals(stream)) intent = new android.content.Intent(this, twelvesciencehome.class);
                else if ("Commerce".equals(stream)) intent = new android.content.Intent(this, twelvecommercehome.class);
                else if ("Arts".equals(stream)) intent = new android.content.Intent(this, twelveartshome.class);
            }
            if (intent == null) intent = new android.content.Intent(this, elevencommercehome.class);
        } else if (id == R.id.profile) {
            intent = new android.content.Intent(this, userprofile.class);
        } else if (id == R.id.feedback) {
            intent = new android.content.Intent(this, studentfeedback.class);
        } else if (id == R.id.gallery) {
            intent = new android.content.Intent(this, studentgallery.class);
        } else if (id == R.id.syllabus) {
            intent = new android.content.Intent(this, studentsyllabus.class);
        } else if (id == R.id.assignment) {
            intent = new android.content.Intent(this, studentassignment.class);
        } else if (id == R.id.exam) {
            intent = new android.content.Intent(this, studentexam.class);
        } else if (id == R.id.calender) {
            intent = new android.content.Intent(this, studentcalender.class);
        } else if (id == R.id.payment) {
            intent = new android.content.Intent(this, studentpayment.class);
        } else if (id == R.id.table) {
            intent = new android.content.Intent(this, studenttimetable.class);
        } else if (id == R.id.homework) {
            intent = new android.content.Intent(this, studenthomework.class);
            String std = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Standard", "");
            String stream = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("Stream", "");
            intent.putExtra("singleCategory", true);
            intent.putExtra("std", std);
            intent.putExtra("stream", stream);
        } else if (id == R.id.student) {
            intent = new android.content.Intent(this, studentslist.class);
        } else if (id == R.id.student_attendance) {
            intent = new android.content.Intent(this, studentattendance.class);
        } else if (id == R.id.contact_admin) {
            intent = new android.content.Intent(this, ContactAdminActivity.class);
            intent.putExtra("senderRole", "Student");
            String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("studentUid", "unknown");
            intent.putExtra("senderUid", uid);
        }

        // Avoid opening the exact same intent class
        if (intent != null && intent.getComponent() != null && this.getClass().getName().equals(intent.getComponent().getClassName())) {
            android.widget.Toast.makeText(this, "Already on this page", android.widget.Toast.LENGTH_SHORT).show();
            if (drawerLayout != null) drawerLayout.closeDrawers();
            return true;
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            if (!this.getClass().getSimpleName().contains("home") && !this.getClass().getSimpleName().contains("homepage")) {
                finish();
            }
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        return true;
    }
@Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}