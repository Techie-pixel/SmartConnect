package com.example.schoolmanagement;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class teachercalender extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private CalendarView calendarView;
    private TextView selectedDateText;
    private TextClock textClock;
    private LinearLayout remindersContainer;

    private int reminderYear, reminderMonth, reminderDay, reminderHour, reminderMinute;
    private static final String CHANNEL_ID = "teacher_reminder_channel";
    private final List<ReminderItem> reminderList = new ArrayList<>();
    private static final String PREFS_KEY = "TeacherReminders";
    private static final String PREFS_NAME = "TeacherReminderPrefs";

    private static class ReminderItem {
        String message;
        int year, month, day, hour, minute;
        long triggerTime;
        int requestCode;

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("message", message);
            o.put("year", year);
            o.put("month", month);
            o.put("day", day);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("triggerTime", triggerTime);
            o.put("requestCode", requestCode);
            return o;
        }

        static ReminderItem fromJson(JSONObject o) throws JSONException {
            ReminderItem r = new ReminderItem();
            r.message = o.optString("message", "Reminder");
            r.year = o.getInt("year");
            r.month = o.getInt("month");
            r.day = o.getInt("day");
            r.hour = o.getInt("hour");
            r.minute = o.getInt("minute");
            r.triggerTime = o.getLong("triggerTime");
            r.requestCode = o.getInt("requestCode");
            return r;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachercalender);

        createNotificationChannel();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setTitle("Teacher Calendar");

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigationview);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        navigationView.setNavigationItemSelectedListener(this);

        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        textClock = findViewById(R.id.textClock);
        remindersContainer = findViewById(R.id.remindersContainer);
        MaterialButton addReminderBtn = findViewById(R.id.addReminderBtn);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        selectedDateText.setText(sdf.format(new Date(calendarView.getDate())));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDateText.setText(sdf.format(cal.getTime()));
        });

        addReminderBtn.setOnClickListener(v -> {
            UIAnimator.animateClick(v);
            v.postDelayed(this::showAddReminderDialog, 150);
        });
        loadReminders();
        ensureNotificationPermission();
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Reminder");

        final EditText input = new EditText(this);
        input.setHint("Enter reminder message...");
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);

        builder.setPositiveButton("Pick Date & Time", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            Calendar now = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        reminderYear = year;
                        reminderMonth = month;
                        reminderDay = day;

                        TimePickerDialog timePicker = new TimePickerDialog(this,
                                (tView, hour, minute) -> {
                                    reminderHour = hour;
                                    reminderMinute = minute;
                                    scheduleReminder(message, year, month, day, hour, minute);
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE), false);
                        timePicker.show();
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void scheduleReminder(String message, int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long triggerTime = cal.getTimeInMillis();

        if (triggerTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please pick a future time!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("message", message);
        intent.putExtra("forTeacher", true);
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }

        ReminderItem item = new ReminderItem();
        item.message = message;
        item.year = year;
        item.month = month;
        item.day = day;
        item.hour = hour;
        item.minute = minute;
        item.triggerTime = triggerTime;
        item.requestCode = requestCode;
        reminderList.add(item);
        saveReminders();
        addReminderCard(item);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a", Locale.getDefault());
        cal.set(year, month, day, hour, minute);
        Toast.makeText(this, "Reminder set for " + sdf.format(cal.getTime()), Toast.LENGTH_LONG).show();
    }

    private void addReminderCard(ReminderItem item) {
        Calendar cal = Calendar.getInstance();
        cal.set(item.year, item.month, item.day, item.hour, item.minute);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a", Locale.getDefault());

        View card = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView title = card.findViewById(android.R.id.text1);
        TextView subtitle = card.findViewById(android.R.id.text2);

        title.setText("📌 " + item.message);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(15);
        subtitle.setText("🕐 " + sdf.format(cal.getTime()));
        subtitle.setTextColor(0xFFA3BFFA);

        card.setBackgroundColor(0xFF1A1A2E);
        card.setPadding(20, 16, 20, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        card.setOnLongClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Delete Reminder");
            b.setMessage("Delete this reminder?");
            b.setPositiveButton("Delete", (d, w) -> {
                cancelReminder(item);
                remindersContainer.removeView(card);
                Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
            });
            b.setNegativeButton("Cancel", null);
            b.show();
            return true;
        });

        remindersContainer.addView(card);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Teacher Reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Teacher calendar reminders");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            int granted = ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 2002);
            }
        }
    }

    private SharedPreferences getReminderPrefs() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void saveReminders() {
        JSONArray arr = new JSONArray();
        for (ReminderItem r : reminderList) {
            try {
                arr.put(r.toJson());
            } catch (JSONException ignored) {
            }
        }
        getReminderPrefs().edit().putString(PREFS_KEY, arr.toString()).apply();
    }

    private void loadReminders() {
        String s = getReminderPrefs().getString(PREFS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(s);
            reminderList.clear();
            for (int i = 0; i < arr.length(); i++) {
                ReminderItem r = ReminderItem.fromJson(arr.getJSONObject(i));
                reminderList.add(r);
                addReminderCard(r);
            }
        } catch (JSONException ignored) {
        }
    }

    private void cancelReminder(ReminderItem item) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("message", item.message);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, item.requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null)
            am.cancel(pi);
        for (int i = 0; i < reminderList.size(); i++) {
            ReminderItem r = reminderList.get(i);
            if (r.requestCode == item.requestCode) {
                reminderList.remove(i);
                break;
            }
        }
        saveReminders();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.teacher_home) {
            startActivity(new Intent(this, teachertab.class));
        } else if (id == R.id.teacher_assignments) {
            startActivity(new Intent(this, teacherassignments.class));
        } else if (id == R.id.teacher_homework) {
            startActivity(new Intent(this, teacherhomework.class));
        } else if (id == R.id.teacher_syllabus) {
            startActivity(new Intent(this, teachersyllabus.class));
        } else if (id == R.id.teacher_timetable) {
            startActivity(new Intent(this, teachertimetable.class));
        } else if (id == R.id.teacher_exam) {
            startActivity(new Intent(this, teacherexam.class));
        } else if (id == R.id.teacher_calendar) {
            drawerLayout.closeDrawers();
            return true;
        } else if (id == R.id.teacher_attendance) {
            startActivity(new Intent(this, teacherattendance.class));
        } else if (id == R.id.teacher_feedback) {
            startActivity(new Intent(this, teacherfeedback.class));
        } else if (id == R.id.teacher_notices) {
            startActivity(new Intent(this, TeacherNoticesActivity.class));
        } else if (id == R.id.teacher_fees) {
            startActivity(new Intent(this, TeacherFeesActivity.class));
        } else if (id == R.id.teacher_contact_admin) {
            Intent caIntent = new Intent(this, ContactAdminActivity.class);
            caIntent.putExtra("senderRole", "Teacher");
            caIntent.putExtra("senderUid", getSharedPreferences("TeacherPrefs", MODE_PRIVATE).getString("teacherId", "unknown"));
            startActivity(caIntent);
        }
        drawerLayout.closeDrawers();
        return true;
    }
}
