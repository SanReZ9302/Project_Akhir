package com.example.aplikasishalat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_PRAYER_NAME = "extra_prayer_name";
    private static final String CHANNEL_ID = "PRAYER_ALARM_CHANNEL";
    private static final String CHANNEL_NAME = "Prayer Alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME);
        if (prayerName == null) {
            prayerName = "Waktu Sholat";
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_compass)
                .setContentTitle("Waktunya Sholat " + prayerName)
                .setContentText("Segera laksanakan sholat.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}