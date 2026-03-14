package com.example.schoolmanagement;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ParentSyllabusActivity extends AppCompatActivity {
    private RecyclerView rvSyllabus;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvTitle;
    private Toolbar toolbar;
    private LinearLayout filterContainer;
    private TextView chipAll;
    private ImageView logoImg;

    private DatabaseReference syllabusRef, readsRef;
    private String studentStdStream;
    private String parentId;

    private List<SyllabusItem> allSyllabusList = new ArrayList<>();
    private Set<String> readSyllabusIds = new HashSet<>();
    private String currentFilterSubject = "All";
    private SyllabusAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_syllabus);

        SharedPreferences prefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        parentId = prefs.getString("parentKey", "");
        String std = prefs.getString("studentStandard", "");
        String stream = prefs.getString("studentStream", "");
        studentStdStream = std + "_" + stream;

        initializeViews();
        setupToolbar();
        applyAnimations();

        syllabusRef = FirebaseDatabase.getInstance().getReference("Syllabus").child(studentStdStream);
        readsRef = FirebaseDatabase.getInstance().getReference("SyllabusReads");

        fetchSyllabus();

        chipAll.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            filterBySubject("All", chipAll);
        });
    }

    private void applyAnimations() {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);
        if (tvTitle != null)
            UIAnimator.animateTextView(tvTitle, 300);
        if (chipAll != null)
            UIAnimator.animateTextView(chipAll, 400);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        logoImg = findViewById(R.id.logoImg);
        tvTitle = findViewById(R.id.tvTitle);
        rvSyllabus = findViewById(R.id.rvSyllabus);
        progressBar = findViewById(R.id.progressBar);
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
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }
    }

    private void fetchSyllabus() {
        progressBar.setVisibility(View.VISIBLE);
        syllabusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSyllabusList.clear();
                Set<String> subjects = new HashSet<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SyllabusItem item = child.getValue(SyllabusItem.class);
                    if (item != null) {
                        item.id = child.getKey();
                        allSyllabusList.add(item);
                        if (item.subject != null) subjects.add(item.subject);
                    }
                }
                allSyllabusList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                updateSubjectFilters(subjects);
                fetchReadStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ParentSyllabusActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchReadStatus() {
        readsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                readSyllabusIds.clear();
                for (DataSnapshot syllabusSnap : snapshot.getChildren()) {
                    if (syllabusSnap.child(parentId).exists()) {
                        readSyllabusIds.add(syllabusSnap.getKey());
                    }
                }
                progressBar.setVisibility(View.GONE);
                filterBySubject(currentFilterSubject, chipAll);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateSubjectFilters(Set<String> subjects) {
        filterContainer.removeAllViews();
        filterContainer.addView(chipAll);
        for (String sub : subjects) {
            TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_subject_chip, filterContainer, false);
            chip.setText(sub);
            chip.setOnClickListener(v -> filterBySubject(sub, chip));
            filterContainer.addView(chip);
        }
    }

    private void filterBySubject(String subject, TextView chip) {
        currentFilterSubject = subject;
        List<SyllabusItem> filtered = new ArrayList<>();
        for (SyllabusItem item : allSyllabusList) {
            if (subject.equals("All") || subject.equals(item.subject)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        // Reset all chips
        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            TextView c = (TextView) filterContainer.getChildAt(i);
            c.setBackgroundResource(R.drawable.chip_unselected);
            c.setTextColor(0xFFFFFFFF);
        }
        chip.setBackgroundResource(R.drawable.chip_selected);
        chip.setTextColor(0xFFFFFFFF);
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
            holder.tvInfo.setText(item.syllabusInfo);
            holder.tvTarget.setText(item.subject != null ? item.subject : "Syllabus");
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(item.timestamp)));

            boolean isRead = readSyllabusIds.contains(item.id);
            holder.tvBadge.setVisibility(isRead ? View.GONE : View.VISIBLE);
            holder.btnMark.setVisibility(isRead ? View.GONE : View.VISIBLE);

            if (item.image != null && !item.image.isEmpty()) {
                byte[] decoded = Base64.decode(item.image, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                holder.ivImage.setImageBitmap(bmp);
                holder.ivImage.setVisibility(View.VISIBLE);
            } else {
                holder.ivImage.setVisibility(View.GONE);
            }

            holder.btnMark.setOnClickListener(v -> readsRef.child(item.id).child(parentId).setValue(true));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTarget, tvDate, tvInfo, tvBadge;
            ImageView ivImage;
            Button btnMark;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTarget = itemView.findViewById(R.id.tvSyllabusTarget);
                tvDate = itemView.findViewById(R.id.tvSyllabusDate);
                tvInfo = itemView.findViewById(R.id.tvSyllabusInfo);
                tvBadge = itemView.findViewById(R.id.tvUnreadBadge);
                ivImage = itemView.findViewById(R.id.ivSyllabusImage);
                btnMark = itemView.findViewById(R.id.btnMarkRead);
            }
        }
    }

    public static class SyllabusItem {
        public String id;
        public String syllabusInfo;
        public String image;
        public Long timestamp;
        public String subject;
    }
}
