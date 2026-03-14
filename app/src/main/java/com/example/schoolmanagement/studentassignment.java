package com.example.schoolmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class studentassignment extends AppCompatActivity {

    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private boolean singleCategory = false;
    private String fixedStd = "";
    private String fixedStream = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_studentassignment);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
        }

        Intent i = getIntent();
        if (i != null && i.getBooleanExtra("singleCategory", false)) {
            singleCategory = true;
            fixedStd = i.getStringExtra("std") != null ? i.getStringExtra("std") : "";
            fixedStream = i.getStringExtra("stream") != null ? i.getStringExtra("stream") : "";
        } else {
            // Fallback to UserPrefs
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            fixedStd = prefs.getString("Standard", "");
            fixedStream = prefs.getString("Stream", "");
            singleCategory = !fixedStd.isEmpty() && !fixedStream.isEmpty();
        }

        setupTabs();
    }

    @Override
    public void onBackPressed() {
        // Go back to home page like notices and e-commerce do
        Intent intent = new Intent(this, elevensciencehomepage.class);
        
        // Check prefs for current standard and stream
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String standard = prefs.getString("Standard", "");
        String stream = prefs.getString("Stream", "");
        
        if ("11".equals(standard)) {
            if ("Science".equals(stream)) {
                intent = new Intent(this, elevensciencehomepage.class);
            } else if ("Commerce".equals(stream)) {
                intent = new Intent(this, elevencommercehome.class);
            } else if ("Arts".equals(stream)) {
                intent = new Intent(this, elevenartshome.class);
            }
        } else if ("12".equals(standard)) {
            if ("Science".equals(stream)) {
                intent = new Intent(this, twelvesciencehome.class);
            } else if ("Commerce".equals(stream)) {
                intent = new Intent(this, twelvecommercehome.class);
            } else if ("Arts".equals(stream)) {
                intent = new Intent(this, twelveartshome.class);
            }
        }
        
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void setupTabs() {
        if (singleCategory && !fixedStd.isEmpty() && !fixedStream.isEmpty()) {
            viewPager.setAdapter(new SinglePagerAdapter(this, fixedStd, fixedStream));
            tabLayout.setVisibility(View.GONE);
        } else {
            viewPager.setAdapter(new AssignmentPagerAdapter(this));
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("11 Science");
                        break;
                    case 1:
                        tab.setText("11 Commerce");
                        break;
                    case 2:
                        tab.setText("11 Arts");
                        break;
                    case 3:
                        tab.setText("12 Science");
                        break;
                    case 4:
                        tab.setText("12 Commerce");
                        break;
                    case 5:
                        tab.setText("12 Arts");
                        break;
                }
            }).attach();
        }
    }

    static class AssignmentPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public AssignmentPagerAdapter(@NonNull AppCompatActivity act) {
            super(act);
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return AssignmentListFragment.newInstance("11", "Science");
                case 1:
                    return AssignmentListFragment.newInstance("11", "Commerce");
                case 2:
                    return AssignmentListFragment.newInstance("11", "Arts");
                case 3:
                    return AssignmentListFragment.newInstance("12", "Science");
                case 4:
                    return AssignmentListFragment.newInstance("12", "Commerce");
                default:
                    return AssignmentListFragment.newInstance("12", "Arts");
            }
        }

        @Override
        public int getItemCount() {
            return 6;
        }
    }

    static class SinglePagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        private final String std;
        private final String stream;

        public SinglePagerAdapter(@NonNull AppCompatActivity act, String std, String stream) {
            super(act);
            this.std = std;
            this.stream = stream;
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return AssignmentListFragment.newInstance(std, stream);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }
}
