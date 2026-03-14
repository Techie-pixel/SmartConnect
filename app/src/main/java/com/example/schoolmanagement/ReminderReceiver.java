package com.example.schoolmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        boolean forTeacher = intent.getBooleanExtra("forTeacher", false);
        boolean forStudent = intent.getBooleanExtra("forStudent", false);
        if (message == null || message.trim().isEmpty()) {
            message = "Reminder";
        }

        // Ensure notification channel exists (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Calendar reminders");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        // Tap on notification opens appropriate calendar activity
        Intent openIntent;
        if (forTeacher) {
            openIntent = new Intent(context, teachercalender.class);
        } else if (forStudent) {
            openIntent = new Intent(context, studentcalender.class);
        } else {
            openIntent = new Intent(context, PrincipalCalendarActivity.class);
        }
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("SmartConnect Reminder")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (int) (System.currentTimeMillis() & 0xFFFFFFF);
            manager.notify(notificationId, builder.build());
        }
    }
}
