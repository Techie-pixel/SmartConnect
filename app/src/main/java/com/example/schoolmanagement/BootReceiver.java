package com.example.schoolmanagement;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Restore Principal reminders
        restoreReminders(context, "PrincipalReminderPrefs", "PrincipalReminders", false);
        // Restore Teacher reminders
        restoreReminders(context, "TeacherReminderPrefs", "TeacherReminders", true);
    }

    private void restoreReminders(Context context, String prefsName, String key, boolean forTeacher) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        String s = prefs.getString(key, "[]");
        try {
            JSONArray arr = new JSONArray(s);
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long triggerTime = o.getLong("triggerTime");
                if (triggerTime <= now) continue;
                String message = o.optString("message", "Reminder");
                int requestCode = o.getInt("requestCode");
                Intent rIntent = new Intent(context, ReminderReceiver.class);
                rIntent.putExtra("message", message);
                rIntent.putExtra("forTeacher", forTeacher);
                PendingIntent pi = PendingIntent.getBroadcast(
                        context, requestCode, rIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
