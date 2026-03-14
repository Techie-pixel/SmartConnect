package com.example.schoolmanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ParentFeesActivity extends AppCompatActivity {

    private TextView tvStudentName, tvStudentClass, tvTotalFees, tvPaidAmount,
            tvRemainingAmount, tvFeeStatus, tvNoHistory;
    private RadioGroup radioGroupPayment;
    private RadioButton radioInstallment, radioFull;
    private EditText etPaymentAmount, etAdminQuery;
    private MaterialButton btnPayNow, btnContactAdmin;
    private MaterialCardView cardPaymentOptions;
    private LinearLayout layoutPaymentHistory;
    private LinearLayout layoutQueryHistory;
    private ImageView logoImg;

    private DatabaseReference feesRef, parentsRef;
    private String parentName, parentEmail, parentMobile;
    private String parentKey = "";
    private String studentName = "";
    private String studentClass = "";
    private String studentStream = "";
    private long totalFees = 0, paidAmount = 0, remainingAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_fees);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Fees Payment");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        }

        initViews();
        applyAnimations(toolbar);

        feesRef = FirebaseDatabase.getInstance().getReference("Fees");
        parentsRef = FirebaseDatabase.getInstance().getReference("Parents");

        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        parentName = parentPrefs.getString("parentName", "");
        parentEmail = parentPrefs.getString("parentEmail", "");
        parentMobile = parentPrefs.getString("parentMobile", "");

        // Try UID-based lookup first (new flow), fallback to name-matching (old flow)
        String savedKey = parentPrefs.getString("parentKey", "");
        String savedStudentUID = parentPrefs.getString("studentUID", "");
        String savedStudentName = parentPrefs.getString("studentName", "");
        String savedStudentStd = parentPrefs.getString("studentStandard", "");
        String savedStudentStream = parentPrefs.getString("studentStream", "");

        if (!savedKey.isEmpty() && !savedStudentName.isEmpty()) {
            // Fast path: all data is in prefs already
            parentKey = savedKey;
            studentName = savedStudentName;
            studentClass = savedStudentStd;
            studentStream = savedStudentStream;
            tvStudentName.setText("Student: " + studentName);
            tvStudentClass.setText("Class: " + studentClass + " " + studentStream);
            loadFeeStructure();
        } else {
            // Fallback: find parent by email matching (backward compatibility)
            findParentRecord();
        }

        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioFull) {
                etPaymentAmount.setText(String.valueOf(remainingAmount));
                etPaymentAmount.setEnabled(false);
            } else {
                etPaymentAmount.setText("");
                etPaymentAmount.setEnabled(true);
            }
        });

        btnPayNow.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            processPayment();
        });
        btnContactAdmin.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            sendQueryToAdmin();
        });

        // Load query history (uses parentEmail to filter)
        loadQueryHistory();
    }

    private void applyAnimations(Toolbar toolbar) {
        UIAnimator.animateToolbar(toolbar, 100);
        if (logoImg != null)
            UIAnimator.animateImageView(logoImg, 200);

        View[] views = {
                findViewById(R.id.cardStudentInfo), // Add these IDs in XML if missing or use references
                findViewById(R.id.cardFeeDetails),
                cardPaymentOptions,
                btnPayNow, etAdminQuery, btnContactAdmin
        };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                if (views[i] instanceof MaterialCardView) {
                    UIAnimator.animateCard(views[i], 300 + (i * 100));
                } else if (views[i] instanceof MaterialButton) {
                    UIAnimator.animateButton((MaterialButton) views[i], 300 + (i * 100));
                } else if (views[i] instanceof EditText) {
                    UIAnimator.animateEditText((EditText) views[i], 300 + (i * 100));
                }
            }
        }
    }

    private void initViews() {
        logoImg = findViewById(R.id.logoImg);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentClass = findViewById(R.id.tvStudentClass);
        tvTotalFees = findViewById(R.id.tvTotalFees);
        tvPaidAmount = findViewById(R.id.tvPaidAmount);
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount);
        tvFeeStatus = findViewById(R.id.tvFeeStatus);
        tvNoHistory = findViewById(R.id.tvNoHistory);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        radioInstallment = findViewById(R.id.radioInstallment);
        radioFull = findViewById(R.id.radioFull);
        etPaymentAmount = findViewById(R.id.etPaymentAmount);
        etAdminQuery = findViewById(R.id.etAdminQuery);
        btnPayNow = findViewById(R.id.btnPayNow);
        btnContactAdmin = findViewById(R.id.btnContactAdmin);
        cardPaymentOptions = findViewById(R.id.cardPaymentOptions);
        layoutPaymentHistory = findViewById(R.id.layoutPaymentHistory);
        layoutQueryHistory = findViewById(R.id.layoutQueryHistory);
    }

    private void findParentRecord() {
        parentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String email = child.child("email").getValue(String.class);
                    if (parentEmail.equalsIgnoreCase(email)) {
                        parentKey = child.getKey();
                        studentName = child.child("studentName").getValue(String.class);
                        if (studentName == null)
                            studentName = "";
                        tvStudentName.setText("Student: " + studentName);

                        // Check for new UID-based fields
                        String sStd = child.child("studentStandard").getValue(String.class);
                        String sStrm = child.child("studentStream").getValue(String.class);
                        if (sStd != null && !sStd.isEmpty()) {
                            studentClass = sStd;
                            studentStream = sStrm != null ? sStrm : "";
                            tvStudentClass.setText("Class: " + studentClass + " " + studentStream);
                            loadFeeStructure();
                        } else {
                            // Old data: look up student class by name
                            findStudentClassInfo();
                        }
                        return;
                    }
                }
                tvStudentName.setText("Student: Not linked");
                Toast.makeText(ParentFeesActivity.this,
                        "Parent record not found in database.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentFeesActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findStudentClassInfo() {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance().getReference("Students");
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("Name").getValue(String.class);
                    if (name == null)
                        name = child.child("name").getValue(String.class);
                    if (name != null && name.equalsIgnoreCase(studentName)) {
                        studentClass = child.child("Standard").getValue(String.class);
                        studentStream = child.child("Stream").getValue(String.class);
                        if (studentClass == null)
                            studentClass = "";
                        if (studentStream == null)
                            studentStream = "";
                        tvStudentClass.setText("Class: " + studentClass + " " + studentStream);
                        loadFeeStructure();
                        return;
                    }
                }
                tvStudentClass.setText("Class: Unknown");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadFeeStructure() {
        String key = studentClass + "_" + studentStream;
        feesRef.child("FeeStructure").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long amount = snapshot.child("amount").getValue(Long.class);
                        totalFees = amount != null ? amount : 0;
                        tvTotalFees.setText("₹ " + totalFees);
                        loadPaymentData();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void loadPaymentData() {
        if (parentKey.isEmpty())
            return;
        feesRef.child("Payments").child(parentKey)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        paidAmount = 0;
                        layoutPaymentHistory.removeAllViews();
                        boolean hasHistory = false;

                        // First pass: calculate total paid so far
                        long runningTotal = 0;
                        for (DataSnapshot payment : snapshot.getChildren()) {
                            Long amt = payment.child("amountPaid").getValue(Long.class);
                            if (amt != null)
                                runningTotal += amt;
                        }
                        paidAmount = runningTotal;

                        // Second pass: build history items with running total
                        long paidSoFar = 0;
                        for (DataSnapshot payment : snapshot.getChildren()) {
                            Long amt = payment.child("amountPaid").getValue(Long.class);
                            if (amt != null)
                                paidSoFar += amt;
                            hasHistory = true;
                            addPaymentHistoryCard(payment, paidSoFar);
                        }

                        remainingAmount = totalFees - paidAmount;
                        if (remainingAmount < 0)
                            remainingAmount = 0;

                        tvPaidAmount.setText("₹ " + paidAmount);
                        tvRemainingAmount.setText("₹ " + remainingAmount);

                        if (remainingAmount == 0 && totalFees > 0) {
                            tvFeeStatus.setText("Status: Paid ✅");
                            tvFeeStatus.setTextColor(0xFF66BB6A);
                            cardPaymentOptions.setVisibility(View.GONE);
                        } else if (paidAmount > 0) {
                            tvFeeStatus.setText("Status: Partial ⏳");
                            tvFeeStatus.setTextColor(0xFFFFAB40);
                            cardPaymentOptions.setVisibility(View.VISIBLE);
                        } else {
                            tvFeeStatus.setText("Status: Unpaid ❌");
                            tvFeeStatus.setTextColor(0xFFEF5350);
                            cardPaymentOptions.setVisibility(View.VISIBLE);
                        }

                        if (!hasHistory) {
                            TextView noHist = new TextView(ParentFeesActivity.this);
                            noHist.setText("No payments yet.");
                            noHist.setTextColor(0xFF888888);
                            noHist.setTextSize(13);
                            layoutPaymentHistory.addView(noHist);
                        }

                        // Update amount field if "Pay Full" is selected
                        if (radioFull.isChecked()) {
                            etPaymentAmount.setText(String.valueOf(remainingAmount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void addPaymentHistoryCard(DataSnapshot payment, long paidSoFar) {
        String date = payment.child("date").getValue(String.class);
        Long amt = payment.child("amountPaid").getValue(Long.class);
        String type = payment.child("paymentType").getValue(String.class);
        String paymentId = payment.getKey();

        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_payment_history, layoutPaymentHistory, false);

        TextView tvPayDate = itemView.findViewById(R.id.tvPayDate);
        TextView tvPayType = itemView.findViewById(R.id.tvPayType);
        TextView tvPayAmount = itemView.findViewById(R.id.tvPayAmount);
        MaterialButton btnDownload = itemView.findViewById(R.id.btnDownloadItemReceipt);

        tvPayDate.setText(date != null ? date : "N/A");
        tvPayType.setText(type != null ? type : "—");
        tvPayAmount.setText("₹ " + (amt != null ? amt : 0));

        final long finalAmt = amt != null ? amt : 0;
        final String finalDate = date != null ? date : "N/A";
        final String finalType = type != null ? type : "";
        final String finalId = paymentId != null ? paymentId : "";
        final long finalPaidSoFar = paidSoFar;

        btnDownload.setOnClickListener(v -> {
            String savedFileName = ReceiptGenerator.generateReceipt(
                    ParentFeesActivity.this,
                    finalId,
                    finalDate,
                    studentName,
                    studentClass + " " + studentStream,
                    parentName,
                    parentEmail,
                    totalFees,
                    finalAmt,
                    finalPaidSoFar,
                    finalType);

            if (savedFileName != null) {
                Toast.makeText(this, "Receipt saved to Gallery/Downloads: " + savedFileName,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to save receipt",
                        Toast.LENGTH_SHORT).show();
            }
        });

        layoutPaymentHistory.addView(itemView);
    }

    private void processPayment() {
        if (parentKey.isEmpty()) {
            Toast.makeText(this, "Parent record not found!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (radioGroupPayment.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select payment type", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etPaymentAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Enter payment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount > remainingAmount) {
            Toast.makeText(this, "Amount exceeds remaining fee (₹" + remainingAmount + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentType = radioInstallment.isChecked() ? "Installment" : "Full";
        String dateStr = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        String paymentId = feesRef.child("Payments").child(parentKey).push().getKey();
        if (paymentId == null)
            return;

        long newPaid = paidAmount + amount;
        long newRemaining = totalFees - newPaid;
        if (newRemaining < 0)
            newRemaining = 0;
        String status;
        if (newRemaining == 0)
            status = "Paid";
        else if (newPaid > 0)
            status = "Partial";
        else
            status = "Unpaid";

        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("parentName", parentName);
        paymentData.put("parentEmail", parentEmail);
        paymentData.put("studentName", studentName);

        SharedPreferences parentPrefs = getSharedPreferences("ParentPrefs", MODE_PRIVATE);
        String savedStudentUID = parentPrefs.getString("studentUID", "");
        if (!savedStudentUID.isEmpty()) {
            paymentData.put("studentUID", savedStudentUID);
        } else {
            paymentData.put("studentUID", parentKey); // Fallback if missing
        }

        paymentData.put("studentClass", studentClass);
        paymentData.put("studentStream", studentStream);
        paymentData.put("totalFees", totalFees);
        paymentData.put("amountPaid", amount);
        paymentData.put("remainingAmount", newRemaining);
        paymentData.put("paymentType", paymentType);
        paymentData.put("status", status);
        paymentData.put("date", dateStr);
        paymentData.put("timestamp", System.currentTimeMillis());

        feesRef.child("Payments").child(parentKey).child(paymentId)
                .setValue(paymentData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Payment successful! You can download the receipt from Payment History below.",
                                Toast.LENGTH_LONG).show();
                        radioGroupPayment.clearCheck();
                        etPaymentAmount.setText("");
                        etPaymentAmount.setEnabled(true);

                        // Send email notification to Admin
                        new Thread(() -> {
                            try {
                                String adminEmail = "admin@example.com";
                                String subject = "New Fee Payment Received - SmartConnect";
                                String body = "Hello Admin,\n\n" +
                                        "A new fee payment has been made.\n\n" +
                                        "Parent Name: " + parentName + "\n" +
                                        "Student Name: " + studentName + " (" + studentClass + " " + studentStream
                                        + ")\n" +
                                        "Amount Paid: ₹" + amount + "\n" +
                                        "Date: " + dateStr + "\n\n" +
                                        "Regards,\nSmartConnect System";
                                GmailSender.sendMailWithSubject(adminEmail, subject, body);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } else {
                        Toast.makeText(this, "Payment failed. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendQueryToAdmin() {
        String query = etAdminQuery.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please enter your query", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        HashMap<String, Object> queryData = new HashMap<>();
        queryData.put("parentName", parentName);
        queryData.put("parentEmail", parentEmail);
        queryData.put("message", query);
        queryData.put("date", dateStr);
        queryData.put("status", "Pending");
        queryData.put("parentKey", parentKey);

        feesRef.child("ParentQueries").push().setValue(queryData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Query sent to Admin successfully!",
                                Toast.LENGTH_SHORT).show();
                        etAdminQuery.setText("");

                        // Send email notification to Admin
                        new Thread(() -> {
                            try {
                                String adminEmail = "admin@example.com";
                                String subject = "New Parent Query - SmartConnect";
                                String body = "Hello Admin,\n\n" +
                                        "You have received a new fee-related query.\n\n" +
                                        "From: " + parentName + " (" + parentEmail + ")\n" +
                                        "Message: " + query + "\n" +
                                        "Date: " + dateStr + "\n\n" +
                                        "Please log in to the app to reply.\n\n" +
                                        "Regards,\nSmartConnect System";
                                GmailSender.sendMailWithSubject(adminEmail, subject, body);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } else {
                        Toast.makeText(this, "Failed to send query. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadQueryHistory() {
        if (parentEmail == null || parentEmail.isEmpty())
            return;

        feesRef.child("ParentQueries").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutQueryHistory.removeAllViews();
                boolean filterByEmail = true;

                for (DataSnapshot querySnap : snapshot.getChildren()) {
                    String qEmail = querySnap.child("parentEmail").getValue(String.class);
                    if (qEmail != null && qEmail.equalsIgnoreCase(parentEmail)) {
                        addQueryHistoryItem(querySnap);
                    }
                }

                if (layoutQueryHistory.getChildCount() == 0) {
                    TextView tv = new TextView(ParentFeesActivity.this);
                    tv.setText("No past queries found.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(13);
                    layoutQueryHistory.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addQueryHistoryItem(DataSnapshot querySnap) {
        String date = querySnap.child("date").getValue(String.class);
        String message = querySnap.child("message").getValue(String.class);
        String status = querySnap.child("status").getValue(String.class);
        String adminReply = querySnap.child("adminReply").getValue(String.class);

        View queryView = LayoutInflater.from(this).inflate(R.layout.item_parent_query, layoutQueryHistory, false);

        TextView tvDate = queryView.findViewById(R.id.tvQueryDateParent);
        TextView tvMessage = queryView.findViewById(R.id.tvQueryMessageParent);
        TextView tvStatus = queryView.findViewById(R.id.tvQueryStatusParent);
        LinearLayout layoutReply = queryView.findViewById(R.id.layoutAdminReply);
        TextView tvReplyText = queryView.findViewById(R.id.tvAdminReplyText);

        tvDate.setText(date != null ? date : "N/A");
        tvMessage.setText(message != null ? message : "");
        tvStatus.setText(status != null ? status : "Pending");

        if ("Replied".equals(status) && adminReply != null && !adminReply.isEmpty()) {
            tvStatus.setBackgroundColor(0xFF388E3C); // Green
            layoutReply.setVisibility(View.VISIBLE);
            tvReplyText.setText(adminReply);
        } else {
            tvStatus.setBackgroundColor(0xFFFF8F00); // Orange
            layoutReply.setVisibility(View.GONE);
        }

        layoutQueryHistory.addView(queryView);
    }
}
