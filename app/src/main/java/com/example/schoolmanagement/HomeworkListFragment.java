package com.example.schoolmanagement;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class HomeworkListFragment extends Fragment {

    private static final String ARG_STD = "arg_std";
    private static final String ARG_STREAM = "arg_stream";
    private static final int REQ_MEDIA_PERMISSION = 902;
    private Runnable afterPickUiUpdater = null;
    private android.widget.ImageView submitPreviewImage = null;

    private String std;
    private String stream;

    private RecyclerView rv;
    private TextView tvEmpty;
    private Button chipAll, chipPending, chipSubmitted;
    private HomeworkRecyclerAdapter adapter;
    private final List<HomeworkItem> allHomework = new ArrayList<>();
    private final List<HomeworkItem> visibleHomework = new ArrayList<>();
    private DatabaseReference homeworkRef, submissionsRef;
    private String myUid;
    private String myName = "Student";
    private String myStd = "";
    private String myStream = "";

    private final List<Uri> pickedImage = new ArrayList<>(1);
    private ActivityResultLauncher<String[]> imagePickerLauncher;

    public static HomeworkListFragment newInstance(String std, String stream) {
        HomeworkListFragment f = new HomeworkListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STD, std);
        b.putString(ARG_STREAM, stream);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            std = args.getString(ARG_STD, "");
            stream = args.getString(ARG_STREAM, "");
        }
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        homeworkRef = FirebaseDatabase.getInstance().getReference("Homework");
        submissionsRef = FirebaseDatabase.getInstance().getReference("HomeworkSubmissions");
        resolveStudentProfile();
        setupPickerLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_assignment_list, container, false);
        rv = v.findViewById(R.id.rvAssignments);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        chipAll = v.findViewById(R.id.btnAll);
        chipPending = v.findViewById(R.id.btnPending);
        chipSubmitted = v.findViewById(R.id.btnSubmitted);

        adapter = new HomeworkRecyclerAdapter(visibleHomework, new HomeworkRecyclerAdapter.OnActionListener() {
            @Override
            public void onViewImages(HomeworkItem item) {
                if (item.imageBase64List.isEmpty())
                    return;
                Intent intent = new Intent(getContext(), Image_detail.class);
                intent.putExtra("image", item.imageBase64List.get(0));
                intent.putExtra("description", item.title);
                startActivity(intent);
            }

            @Override
            public void onSubmit(HomeworkItem item) {
                showSubmitDialog(item);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        chipAll.setOnClickListener(v1 -> applyFilter(Filter.ALL));
        chipPending.setOnClickListener(v12 -> applyFilter(Filter.PENDING));
        chipSubmitted.setOnClickListener(v13 -> applyFilter(Filter.SUBMITTED));

        addTouchAnimation(chipAll);
        addTouchAnimation(chipPending);
        addTouchAnimation(chipSubmitted);

        android.view.animation.Animation btnAnim = android.view.animation.AnimationUtils.loadAnimation(getContext(),
                R.anim.fade_in);
        chipAll.startAnimation(btnAnim);

        android.view.animation.Animation pendingAnim = android.view.animation.AnimationUtils.loadAnimation(getContext(),
                R.anim.fade_in);
        pendingAnim.setStartOffset(100);
        chipPending.startAnimation(pendingAnim);

        android.view.animation.Animation submittedAnim = android.view.animation.AnimationUtils
                .loadAnimation(getContext(), R.anim.fade_in);
        submittedAnim.setStartOffset(200);
        chipSubmitted.startAnimation(submittedAnim);

        loadHomework();
        return v;
    }

    private void addTouchAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void resolveStudentProfile() {
        Context ctx = getContext();
        if (ctx == null)
            return;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        myStd = prefs.getString("Standard", "");
        myStream = prefs.getString("Stream", "");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Students").child(myUid);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String n = snapshot.child("Name").getValue(String.class);
                if (n != null && !n.isEmpty())
                    myName = n;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupPickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    pickedImage.clear();
                    if (uri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            try {
                                requireActivity().getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {
                            }
                        }
                        pickedImage.add(uri);
                        Toast.makeText(getContext(), "Image selected", Toast.LENGTH_SHORT).show();
                        if (afterPickUiUpdater != null) {
                            afterPickUiUpdater.run();
                        }
                    }
                });
    }

    private enum Filter {
        ALL, PENDING, SUBMITTED
    }

    private Filter currentFilter = Filter.ALL;

    private void applyFilter(Filter f) {
        currentFilter = f;
        chipAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                f == Filter.ALL ? 0xFF3B5BDB : 0xFF1A2E55));
        chipPending.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                f == Filter.PENDING ? 0xFF3B5BDB : 0xFF1A2E55));
        chipSubmitted.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                f == Filter.SUBMITTED ? 0xFF3B5BDB : 0xFF1A2E55));

        visibleHomework.clear();
        long now = System.currentTimeMillis();
        for (HomeworkItem it : allHomework) {
            if (it.dueTimestamp > 0 && it.dueTimestamp < now)
                continue;
            if (f == Filter.ALL) {
                visibleHomework.add(it);
            } else if (f == Filter.PENDING && !it.submitted) {
                visibleHomework.add(it);
            } else if (f == Filter.SUBMITTED && it.submitted) {
                visibleHomework.add(it);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(visibleHomework.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadHomework() {
        String cat = std + "_" + stream;
        homeworkRef.child(cat).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allHomework.clear();
                long now = System.currentTimeMillis();
                for (DataSnapshot a : snapshot.getChildren()) {
                    String key = a.getKey();
                    String title = a.child("title").getValue(String.class);
                    String desc = a.child("description").getValue(String.class);
                    String teacher = a.child("teacherName").getValue(String.class);
                    Long dueTs = a.child("dueTimestamp").getValue(Long.class);
                    long due = dueTs != null ? dueTs : 0L;
                    if (due > 0 && due < now)
                        continue;
                    List<String> images = new ArrayList<>();
                    DataSnapshot imgs = a.child("images");
                    for (DataSnapshot img : imgs.getChildren()) {
                        String b64 = img.getValue(String.class);
                        if (b64 != null)
                            images.add(b64);
                    }
                    HomeworkItem item = new HomeworkItem();
                    item.key = key;
                    item.title = title != null ? title : "Homework";
                    item.description = desc != null ? desc : "";
                    item.teacherName = teacher != null ? teacher : "";
                    item.dueTimestamp = due;
                    item.imageBase64List = images;
                    item.std = std;
                    item.stream = stream;
                    allHomework.add(item);
                }
                markSubmissionsAndApply();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void markSubmissionsAndApply() {
        final List<HomeworkItem> copy = new ArrayList<>(allHomework);
        visibleHomework.clear();
        if (copy.isEmpty()) {
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        final int[] remaining = { copy.size() };
        for (HomeworkItem it : copy) {
            submissionsRef.child(it.key).child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    it.submitted = snapshot.exists();
                    if (snapshot.exists()) {
                        String status = snapshot.child("status").getValue(String.class);
                        String reason = snapshot.child("rejectionReason").getValue(String.class);
                        if ("rejected".equals(status)) {
                            it.submitted = false;
                            it.rejectionReason = reason != null ? reason : "Rejected";
                        } else {
                            it.rejectionReason = null;
                        }
                    }
                    visibleHomework.add(it);
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        applyFilter(currentFilter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        applyFilter(currentFilter);
                    }
                }
            });
        }
    }

    private void showSubmitDialog(HomeworkItem item) {
        Context ctx = getContext();
        if (ctx == null)
            return;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        final android.widget.EditText input = new android.widget.EditText(ctx);
        input.setHint("Write your answer (optional)");
        layout.addView(input);
        final android.widget.Button pickBtn = new android.widget.Button(ctx);
        pickBtn.setText("Pick Image");
        layout.addView(pickBtn);
        submitPreviewImage = new android.widget.ImageView(ctx);
        submitPreviewImage.setAdjustViewBounds(true);
        submitPreviewImage.setMaxHeight((int) (200 * ctx.getResources().getDisplayMetrics().density));
        submitPreviewImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        layout.addView(submitPreviewImage);
        afterPickUiUpdater = () -> {
            if (submitPreviewImage != null && !pickedImage.isEmpty()) {
                submitPreviewImage.setImageURI(pickedImage.get(0));
            }
        };

        AlertDialog d = new AlertDialog.Builder(ctx)
                .setTitle("Submit Homework")
                .setView(layout)
                .setNegativeButton("Cancel", (dialog, which) -> {
                })
                .setPositiveButton("Submit", null)
                .create();
        pickBtn.setOnClickListener(v1 -> {
            if (!hasMediaPermission()) {
                requestMediaPermission();
                return;
            }
            imagePickerLauncher.launch(new String[] { "image/*" });
        });
        d.setOnDismissListener(di -> pickedImage.clear());
        d.setOnShowListener(di -> {
            android.widget.Button submit = d.getButton(AlertDialog.BUTTON_POSITIVE);
            submit.setOnClickListener(v12 -> {
                String answerText = input.getText().toString().trim();
                String imgB64 = "";
                if (!pickedImage.isEmpty()) {
                    imgB64 = uriToBase64(pickedImage.get(0));
                }
                if (answerText.isEmpty() && imgB64.isEmpty()) {
                    Toast.makeText(getContext(), "Add text or image", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadSubmission(item, answerText, imgB64);
                d.dismiss();
            });
        });
        d.show();
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int img = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES);
            return img == PackageManager.PERMISSION_GRANTED;
        } else {
            int read = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[] { Manifest.permission.READ_MEDIA_IMAGES }, REQ_MEDIA_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQ_MEDIA_PERMISSION);
        }
    }

    private void uploadSubmission(HomeworkItem item, String text, String imgBase64) {
        long now = System.currentTimeMillis();
        if (item.dueTimestamp > 0 && now > item.dueTimestamp) {
            Toast.makeText(getContext(), "Deadline over", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("studentUid", myUid);
        data.put("studentName", myName);
        data.put("standard", myStd);
        data.put("stream", myStream);
        data.put("homeworkKey", item.key);
        data.put("submittedAt", now);
        data.put("text", text);
        data.put("image", imgBase64);
        data.put("status", "unchecked");
        submissionsRef.child(item.key).child(myUid).setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Submitted", Toast.LENGTH_SHORT).show();
                    item.submitted = true;
                    applyFilter(currentFilter);
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String uriToBase64(Uri uri) {
        try {
            ContentResolver cr = requireContext().getContentResolver();
            InputStream inputStream = cr.openInputStream(uri);
            if (inputStream == null)
                return "";
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            int maxSize = 600;
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float ratio = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            baos.close();
            inputStream.close();
            return b64;
        } catch (Exception e) {
            return "";
        }
    }

    public static class HomeworkItem {
        public String key;
        public String title;
        public String description;
        public String teacherName;
        public long dueTimestamp;
        public String std;
        public String stream;
        public boolean submitted = false;
        public List<String> imageBase64List = new ArrayList<>();
        public String rejectionReason = null;
    }
}
