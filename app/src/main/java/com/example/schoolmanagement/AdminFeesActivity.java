package com.example.schoolmanagement;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AdminFeesActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private FrameLayout contentFrame;
    private DatabaseReference feesRef, parentsRef, studentsRef;

    private static final String[] CLASS_STREAM_OPTIONS = {
            "11_Science", "11_Commerce", "11_Arts",
            "12_Science", "12_Commerce", "12_Arts"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_fees);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Fees");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        tabLayout = findViewById(R.id.tabLayout);
        contentFrame = findViewById(R.id.contentFrame);

        feesRef = FirebaseDatabase.getInstance().getReference("Fees");
        parentsRef = FirebaseDatabase.getInstance().getReference("Parents");
        studentsRef = FirebaseDatabase.getInstance().getReference("Students");

        tabLayout.addTab(tabLayout.newTab().setText("Fee Structure"));
        tabLayout.addTab(tabLayout.newTab().setText("Payments"));
        tabLayout.addTab(tabLayout.newTab().setText("Reminders"));
        tabLayout.addTab(tabLayout.newTab().setText("Delete Account"));
        tabLayout.addTab(tabLayout.newTab().setText("Queries"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showFeeStructureTab();
                        break;
                    case 1:
                        showPaymentsTab();
                        break;
                    case 2:
                        showRemindersTab();
                        break;
                    case 3:
                        showDeleteAccountTab();
                        break;
                    case 4:
                        showQueriesTab();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Show first tab
        showFeeStructureTab();

        applyAnimations();
    }

    private void applyAnimations() {
        View[] views = { tabLayout, contentFrame };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setVisibility(View.INVISIBLE);
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                anim.setStartOffset(i * 100L);
                int finalI = i;
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        views[finalI].setVisibility(View.VISIBLE);
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

    // ─── TAB 1: FEE STRUCTURE ─────────────────────────────────────
    private void showFeeStructureTab() {
        contentFrame.removeAllViews();
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 48, 40, 48);

        // Title
        TextView title = new TextView(this);
        title.setText("Set Fee Structure");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);

        addSpacer(layout, 24);

        // Spinner for class+stream
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, CLASS_STREAM_OPTIONS);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setBackgroundResource(R.drawable.gradient2);
        spinner.setPadding(32, 32, 32, 32);
        layout.addView(spinner);

        addSpacer(layout, 20);

        // Amount input
        EditText etAmount = new EditText(this);
        etAmount.setHint("Enter fee amount (₹)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etAmount.setTextColor(0xFF000000);
        etAmount.setTextSize(18);
        etAmount.setHintTextColor(0xFF757575);
        etAmount.setBackgroundResource(R.drawable.gradient2);
        etAmount.setPadding(40, 40, 40, 40);
        layout.addView(etAmount);

        addSpacer(layout, 20);

        // Description input
        EditText etDesc = new EditText(this);
        etDesc.setHint("Description (e.g. Class 11 Science Fees)");
        etDesc.setTextColor(0xFF000000);
        etDesc.setTextSize(18);
        etDesc.setHintTextColor(0xFF757575);
        etDesc.setBackgroundResource(R.drawable.gradient2);
        etDesc.setPadding(40, 40, 40, 40);
        layout.addView(etDesc);

        addSpacer(layout, 32);

        // Buttons Layout
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Save button
        MaterialButton btnSave = new MaterialButton(this);
        btnSave.setText("Save");
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setTextSize(16);
        btnSave.setBackgroundColor(0xFF0A2C56); // Better contrasting button color matching theme
        btnSave.setCornerRadius(32);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        saveParams.setMargins(0, 0, 16, 0);
        btnSave.setLayoutParams(saveParams);
        btnSave.setPadding(16, 24, 16, 24);
        btnLayout.addView(btnSave);

        // Delete button
        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("Delete");
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTextSize(16);
        btnDelete.setBackgroundColor(0xFFEF5350);
        btnDelete.setCornerRadius(32);
        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        delParams.setMargins(16, 0, 0, 0);
        btnDelete.setLayoutParams(delParams);
        btnDelete.setPadding(16, 24, 16, 24);
        btnLayout.addView(btnDelete);

        layout.addView(btnLayout);

        // Load current value
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                String key = CLASS_STREAM_OPTIONS[pos];
                feesRef.child("FeeStructure").child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Long amt = snapshot.child("amount").getValue(Long.class);
                                String desc = snapshot.child("description").getValue(String.class);
                                etAmount.setText(amt != null ? String.valueOf(amt) : "");
                                etDesc.setText(desc != null ? desc : "");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnSave.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            String key = spinner.getSelectedItem().toString();
            String amtStr = etAmount.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (TextUtils.isEmpty(amtStr)) {
                Toast.makeText(this, "Enter fee amount", Toast.LENGTH_SHORT).show();
                return;
            }
            long amount;
            try {
                amount = Long.parseLong(amtStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> data = new HashMap<>();
            data.put("amount", amount);
            data.put("description", desc.isEmpty() ? key.replace("_", " ") + " Fees" : desc);

            feesRef.child("FeeStructure").child(key).setValue(data)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Fee structure saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnDelete.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            String key = spinner.getSelectedItem().toString();
            new AlertDialog.Builder(this)
                    .setTitle("Delete Fee Structure")
                    .setMessage("Are you sure you want to delete the fee structure for " + key.replace("_", " ") + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        feesRef.child("FeeStructure").child(key).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminFeesActivity.this, "Fee structure deleted", Toast.LENGTH_SHORT).show();
                                    etAmount.setText("");
                                    etDesc.setText("");
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Animations for tab 1
        View[] animViews = { title, spinner, etAmount, etDesc, btnLayout };
        for (int i = 0; i < animViews.length; i++) {
            animViews[i].setVisibility(View.INVISIBLE);
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
            anim.setStartOffset(i * 100L);
            int finalI = i;
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) { animViews[finalI].setVisibility(View.VISIBLE); }
                @Override public void onAnimationEnd(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            animViews[i].startAnimation(anim);
        }

        addSpacer(layout, 32);

        // Current fee structure overview
        TextView tvCurrent = new TextView(this);
        tvCurrent.setText("Current Fee Structure Overview:");
        tvCurrent.setTextColor(0xFFFFFFFF);
        tvCurrent.setTextSize(20);
        tvCurrent.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvCurrent);

        addSpacer(layout, 16);

        LinearLayout currentList = new LinearLayout(this);
        currentList.setOrientation(LinearLayout.VERTICAL);
        layout.addView(currentList);

        feesRef.child("FeeStructure").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentList.removeAllViews();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Long amt = child.child("amount").getValue(Long.class);
                    String desc = child.child("description").getValue(String.class);
                    TextView tv = new TextView(AdminFeesActivity.this);
                    tv.setText("• " + child.getKey().replace("_", " ") +
                            " → ₹" + (amt != null ? amt : 0));
                    tv.setTextColor(0xFFE0E0E0);
                    tv.setTextSize(16);
                    tv.setPadding(0, 8, 0, 8);
                    currentList.addView(tv);
                }
                if (!snapshot.hasChildren()) {
                    TextView tv = new TextView(AdminFeesActivity.this);
                    tv.setText("No fee structure set yet.");
                    tv.setTextColor(0xFFAAAAAA);
                    tv.setTextSize(15);
                    currentList.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        scrollView.addView(layout);
        contentFrame.addView(scrollView);
    }

    // ─── TAB 2: PAYMENTS OVERVIEW ──────────────────────────────────
    private void showPaymentsTab() {
        contentFrame.removeAllViews();
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 16);

        TextView title = new TextView(this);
        title.setText("All Parents Payment Status");
        title.setTextColor(0xFF4FC3F7);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(title);

        addSpacer(container, 12);

        RecyclerView recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        container.addView(recycler);

        contentFrame.addView(container);

        // Load parents and their payment data
        loadParentPayments(recycler);
    }

    private void loadParentPayments(RecyclerView recycler) {
        parentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot parentSnapshot) {
                List<Map<String, String>> parentList = new ArrayList<>();

                for (DataSnapshot parent : parentSnapshot.getChildren()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("key", parent.getKey());
                    item.put("name", getStr(parent, "name"));
                    item.put("email", getStr(parent, "email"));
                    item.put("studentName", getStr(parent, "studentName"));
                    parentList.add(item);
                }

                // Now load fee data
                feesRef.child("Payments").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot paySnap) {
                        // Load fee structure
                        feesRef.child("FeeStructure").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot feeSnap) {
                                Map<String, Long> feeMap = new HashMap<>();
                                for (DataSnapshot f : feeSnap.getChildren()) {
                                    Long a = f.child("amount").getValue(Long.class);
                                    feeMap.put(f.getKey(), a != null ? a : 0);
                                }

                                // Calculate paid amounts per parent
                                for (Map<String, String> p : parentList) {
                                    String pKey = p.get("key");
                                    long paid = 0;
                                    String sClass = "", sStream = "";
                                    if (paySnap.hasChild(pKey)) {
                                        for (DataSnapshot pay : paySnap.child(pKey).getChildren()) {
                                            Long amt = pay.child("amountPaid").getValue(Long.class);
                                            paid += (amt != null ? amt : 0);
                                            if (sClass.isEmpty()) {
                                                sClass = getStr(pay, "studentClass");
                                                sStream = getStr(pay, "studentStream");
                                            }
                                        }
                                    }
                                    String feeKey = sClass + "_" + sStream;
                                    long total = feeMap.containsKey(feeKey) ? feeMap.get(feeKey) : 0;
                                    p.put("paid", String.valueOf(paid));
                                    p.put("total", String.valueOf(total));
                                    p.put("class", sClass);
                                    p.put("stream", sStream);
                                    if (total > 0 && paid >= total)
                                        p.put("status", "Paid");
                                    else if (paid > 0)
                                        p.put("status", "Partial");
                                    else
                                        p.put("status", "Unpaid");
                                }

                                recycler.setAdapter(new AdminParentAdapter(parentList, false));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ─── TAB 3: SEND REMINDERS ─────────────────────────────────────
    private void showRemindersTab() {
        contentFrame.removeAllViews();
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Send Fee Reminder");
        title.setTextColor(0xFFFFAB40);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        addSpacer(layout, 12);

        TextView info = new TextView(this);
        info.setText("Select a parent from the list below to send a fee reminder via email.");
        info.setTextColor(0xFFCCCCCC);
        info.setTextSize(13);
        layout.addView(info);

        addSpacer(layout, 16);

        RecyclerView recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        layout.addView(recycler);

        // Load parents with actual payment data
        loadParentPaymentsForTab(recycler, true);

        scrollView.addView(layout);
        contentFrame.addView(scrollView);
    }

    private void sendReminder(String parentEmail, String parentName) {
        EditText input = new EditText(this);
        input.setHint("Enter reminder message");
        input.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle("Send Reminder to " + parentName)
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Send email
                    new Thread(() -> {
                        try {
                            String subject = "Fee Payment Reminder - SmartConnect";
                            String body = "Dear " + parentName + ",\n\n" +
                                    message + "\n\n" +
                                    "Please pay the pending fees at the earliest.\n\n" +
                                    "Regards,\nSmartConnect Admin";
                            GmailSender.sendMailWithSubject(parentEmail, subject, body);
                            runOnUiThread(() -> Toast.makeText(this,
                                    "Reminder sent to " + parentEmail, Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this,
                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();

                    // Save reminder to Firebase
                    String dateStr = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                            .format(new Date());
                    HashMap<String, Object> reminder = new HashMap<>();
                    reminder.put("parentEmail", parentEmail);
                    reminder.put("parentName", parentName);
                    reminder.put("message", message);
                    reminder.put("date", dateStr);
                    feesRef.child("Reminders").push().setValue(reminder);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── TAB 4: DELETE ACCOUNT ─────────────────────────────────────
    private void showDeleteAccountTab() {
        contentFrame.removeAllViews();
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Delete Parent & Student Account");
        title.setTextColor(0xFFEF5350);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        addSpacer(layout, 8);

        TextView warning = new TextView(this);
        warning.setText(
                "⚠️ Warning: Deleting a parent account will also remove the associated student account. An email will be sent to the parent.");
        warning.setTextColor(0xFFFFAB40);
        warning.setTextSize(13);
        layout.addView(warning);

        addSpacer(layout, 16);

        RecyclerView recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        layout.addView(recycler);

        // Load parents with actual payment data
        loadParentPaymentsForTab(recycler, true);

        scrollView.addView(layout);
        contentFrame.addView(scrollView);
    }

    private void deleteParentAndStudent(String parentKey, String parentName, String parentEmail, String studentName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete parent \"" + parentName +
                        "\" and student \"" + studentName + "\"?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete parent
                    parentsRef.child(parentKey).removeValue();

                    // Delete payment data
                    feesRef.child("Payments").child(parentKey).removeValue();

                    // Find and delete student
                    studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String name = child.child("Name").getValue(String.class);
                                if (name == null)
                                    name = child.child("name").getValue(String.class);
                                if (name != null && name.equalsIgnoreCase(studentName)) {
                                    studentsRef.child(child.getKey()).removeValue();
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });

                    // Send deletion email
                    new Thread(() -> {
                        try {
                            String subject = "Account Deleted - SmartConnect";
                            String body = "Dear " + parentName + ",\n\n" +
                                    "Your parent account and the associated student account (\"" +
                                    studentName + "\") have been deleted from SmartConnect " +
                                    "due to non-payment of fees.\n\n" +
                                    "If you believe this was done in error, please contact the school administration.\n\n"
                                    +
                                    "Regards,\nSmartConnect Admin";
                            GmailSender.sendMailWithSubject(parentEmail, subject, body);
                            runOnUiThread(() -> Toast.makeText(AdminFeesActivity.this,
                                    "Account deleted & notification sent.", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(AdminFeesActivity.this,
                                    "Account deleted. Email failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── TAB 5: QUERIES ───────────────────────────────────────────
    private void showQueriesTab() {
        contentFrame.removeAllViews();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Parent Queries");
        title.setTextColor(0xFF4FC3F7);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        addSpacer(layout, 16);

        RecyclerView recycler = new RecyclerView(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        layout.addView(recycler);
        contentFrame.addView(layout);

        feesRef.child("ParentQueries").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, String>> queries = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Map<String, String> q = new HashMap<>();
                    q.put("key", snap.getKey());
                    q.put("parentName", getStr(snap, "parentName"));
                    q.put("parentEmail", getStr(snap, "parentEmail"));
                    q.put("parentKey", getStr(snap, "parentKey"));
                    q.put("message", getStr(snap, "message"));
                    q.put("date", getStr(snap, "date"));
                    q.put("status", getStr(snap, "status"));
                    queries.add(q);
                }
                // Sort by new first (simplified approximation using insertion order initially)
                java.util.Collections.reverse(queries);
                recycler.setAdapter(new AdminQueryAdapter(queries));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private class AdminQueryAdapter extends RecyclerView.Adapter<AdminQueryAdapter.QVH> {
        List<Map<String, String>> data;

        AdminQueryAdapter(List<Map<String, String>> d) {
            this.data = d;
        }

        @NonNull
        @Override
        public QVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_query, parent, false);
            return new QVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull QVH holder, int position) {
            Map<String, String> item = data.get(position);
            holder.tvParentName.setText(item.get("parentName") + " (" + item.get("parentEmail") + ")");
            holder.tvDate.setText(item.get("date"));
            // For now, student info is omitted or can be fetched via parentKey later,
            // setting to N/A
            holder.tvStudent.setVisibility(View.GONE);
            holder.tvMessage.setText(item.get("message"));

            String status = item.get("status");
            holder.tvStatus.setText(status);

            if ("Replied".equals(status)) {
                holder.tvStatus.setBackgroundColor(0xFF388E3C); // Green
                holder.btnReply.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setBackgroundColor(0xFFFF8F00); // Orange
                holder.btnReply.setVisibility(View.VISIBLE);
                holder.btnReply.setOnClickListener(
                        v -> {
                            UIAnimator.animateClick(v);
                            showReplyDialog(item.get("key"), item.get("parentName"), item.get("parentEmail"));
                        });
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class QVH extends RecyclerView.ViewHolder {
            TextView tvParentName, tvDate, tvStudent, tvMessage, tvStatus;
            MaterialButton btnReply;

            QVH(View v) {
                super(v);
                tvParentName = v.findViewById(R.id.tvParentNameTitle);
                tvDate = v.findViewById(R.id.tvQueryDate);
                tvStudent = v.findViewById(R.id.tvStudentInfo);
                tvMessage = v.findViewById(R.id.tvQueryMessage);
                tvStatus = v.findViewById(R.id.tvQueryStatus);
                btnReply = v.findViewById(R.id.btnReply);
            }
        }
    }

    private void showReplyDialog(String queryKey, String parentName, String parentEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reply to " + parentName);

        final EditText input = new EditText(this);
        input.setHint("Type your reply here...");
        input.setPadding(32, 32, 32, 32);
        input.setMinLines(3);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String replyText = input.getText().toString().trim();
            if (TextUtils.isEmpty(replyText)) {
                Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> update = new HashMap<>();
            update.put("status", "Replied");
            update.put("adminReply", replyText);

            feesRef.child("ParentQueries").child(queryKey).updateChildren(update)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminFeesActivity.this, "Reply sent!", Toast.LENGTH_SHORT).show();

                        // Send email notification to Parent
                        new Thread(() -> {
                            try {
                                String subject = "Admin Reply to Your Query - SmartConnect";
                                String body = "Dear " + parentName + ",\n\n" +
                                        "The Admin has replied to your fee-related query.\n\n" +
                                        "Admin's Reply:\n" + replyText + "\n\n" +
                                        "You can check your query history in the app for more details.\n\n" +
                                        "Regards,\nSmartConnect System";
                                GmailSender.sendMailWithSubject(parentEmail, subject, body);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ─── Shared payment loader for Reminders & Delete tabs ────────
    private void loadParentPaymentsForTab(RecyclerView recycler, boolean showActions) {
        parentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot parentSnapshot) {
                List<Map<String, String>> parentList = new ArrayList<>();
                for (DataSnapshot parent : parentSnapshot.getChildren()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("key", parent.getKey());
                    item.put("name", getStr(parent, "name"));
                    item.put("email", getStr(parent, "email"));
                    item.put("studentName", getStr(parent, "studentName"));

                    // Also get class/stream from parent record (new fields)
                    String sStd = getStr(parent, "studentStandard");
                    String sStrm = getStr(parent, "studentStream");
                    item.put("class", sStd);
                    item.put("stream", sStrm);
                    item.put("paid", "0");
                    item.put("total", "0");
                    item.put("status", "");
                    parentList.add(item);
                }

                // Now load payments & fee structure
                feesRef.child("Payments").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot paySnap) {
                        feesRef.child("FeeStructure").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot feeSnap) {
                                Map<String, Long> feeMap = new HashMap<>();
                                for (DataSnapshot f : feeSnap.getChildren()) {
                                    Long a = f.child("amount").getValue(Long.class);
                                    feeMap.put(f.getKey(), a != null ? a : 0);
                                }

                                for (Map<String, String> p : parentList) {
                                    String pKey = p.get("key");
                                    long paid = 0;
                                    String sClass = p.get("class");
                                    String sStream = p.get("stream");
                                    if (paySnap.hasChild(pKey)) {
                                        for (DataSnapshot pay : paySnap.child(pKey).getChildren()) {
                                            Long amt = pay.child("amountPaid").getValue(Long.class);
                                            paid += (amt != null ? amt : 0);
                                            // Fallback: get class/stream from payment if not in parent record
                                            if (sClass == null || sClass.isEmpty()) {
                                                sClass = getStr(pay, "studentClass");
                                                sStream = getStr(pay, "studentStream");
                                            }
                                        }
                                    }
                                    String feeKey = sClass + "_" + sStream;
                                    long total = feeMap.containsKey(feeKey) ? feeMap.get(feeKey) : 0;
                                    p.put("paid", String.valueOf(paid));
                                    p.put("total", String.valueOf(total));
                                    p.put("class", sClass != null ? sClass : "");
                                    p.put("stream", sStream != null ? sStream : "");

                                    if (total > 0 && paid >= total)
                                        p.put("status", "Paid");
                                    else if (paid > 0)
                                        p.put("status", "Partial");
                                    else
                                        p.put("status", "Unpaid");
                                }

                                recycler.setAdapter(new AdminParentAdapter(parentList, showActions));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ─── Adapter ───────────────────────────────────────────────────
    private class AdminParentAdapter extends RecyclerView.Adapter<AdminParentAdapter.VH> {
        private List<Map<String, String>> data;
        private boolean showActions;

        AdminParentAdapter(List<Map<String, String>> data, boolean showActions) {
            this.data = data;
            this.showActions = showActions;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_fee_parent, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, String> item = data.get(position);
            holder.tvParentName.setText(item.get("name"));
            holder.tvStudentInfo.setText("Student: " + item.get("studentName"));

            String status = item.get("status");
            String paid = item.get("paid");
            String total = item.get("total");

            if (paid != null && total != null && !paid.isEmpty() && !total.isEmpty()) {
                holder.tvPaymentInfo.setText("Paid: ₹" + paid + " / ₹" + total);
            } else {
                holder.tvPaymentInfo.setText("");
            }

            if ("Paid".equals(status)) {
                holder.tvStatus.setText("Paid");
                holder.tvStatus.setBackgroundColor(0xFF388E3C);
            } else if ("Partial".equals(status)) {
                holder.tvStatus.setText("Partial");
                holder.tvStatus.setBackgroundColor(0xFFFF8F00);
            } else {
                holder.tvStatus.setText("Unpaid");
                holder.tvStatus.setBackgroundColor(0xFFD32F2F);
            }

            if (showActions) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                // Hide Remind button if fees are fully paid
                if ("Paid".equals(status)) {
                    holder.btnRemind.setVisibility(View.GONE);
                } else {
                    holder.btnRemind.setVisibility(View.VISIBLE);
                    holder.btnRemind.setOnClickListener(v -> {
                        UIAnimator.animateClick(v);
                        sendReminder(item.get("email"), item.get("name"));
                    });
                }
                holder.btnDelete.setOnClickListener(v -> {
                    UIAnimator.animateClick(v);
                    deleteParentAndStudent(item.get("key"), item.get("name"),
                            item.get("email"), item.get("studentName"));
                });
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvParentName, tvStudentInfo, tvPaymentInfo, tvStatus;
            LinearLayout layoutActions;
            MaterialButton btnRemind, btnDelete;

            VH(@NonNull View v) {
                super(v);
                tvParentName = v.findViewById(R.id.tvParentName);
                tvStudentInfo = v.findViewById(R.id.tvStudentInfo);
                tvPaymentInfo = v.findViewById(R.id.tvPaymentInfo);
                tvStatus = v.findViewById(R.id.tvStatus);
                layoutActions = v.findViewById(R.id.layoutActions);
                btnRemind = v.findViewById(R.id.btnRemind);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────
    private String getStr(DataSnapshot snap, String key) {
        String val = snap.child(key).getValue(String.class);
        return val != null ? val : "";
    }

    private void addSpacer(LinearLayout layout, int dpHeight) {
        View spacer = new View(this);
        int px = (int) (dpHeight * getResources().getDisplayMetrics().density);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, px));
        layout.addView(spacer);
    }
}
