package com.eventreminder.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.eventreminder.app.R;
import com.eventreminder.app.ui.MainActivity;
import com.eventreminder.app.util.CountdownFormatter;

import java.util.List;

public class NotificationHelper {

    public static final String CHANNEL_COUNTDOWN = "event_countdown";
    public static final String CHANNEL_REMINDER = "event_reminders";
    public static final int NOTIFICATION_COUNTDOWN_ID = 1001;
    public static final int NOTIFICATION_REMINDER_BASE = 2000;

    private final Context context;
    private final NotificationManager manager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        NotificationChannel countdownChannel = new NotificationChannel(
                CHANNEL_COUNTDOWN,
                context.getString(R.string.notification_channel_countdown),
                NotificationManager.IMPORTANCE_LOW
        );
        countdownChannel.setDescription(context.getString(R.string.heartbeat_channel_desc));
        countdownChannel.setShowBadge(false);

        NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.notification_channel_reminder),
                NotificationManager.IMPORTANCE_HIGH
        );
        reminderChannel.setDescription(context.getString(R.string.reminder_channel_desc));

        manager.createNotificationChannel(countdownChannel);
        manager.createNotificationChannel(reminderChannel);
    }

    public Notification buildCountdownNotification(String title, String countdown, String dateTime) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(countdown)
                .setSubText(dateTime)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    public Notification buildNoEventNotification() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_no_events))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    public void showReminderNotification(String title, String countdown) {
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_REMINDER)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("\u23F0 " + title)
                .setContentText("Happening in " + countdown + "!")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        int notificationId = NOTIFICATION_REMINDER_BASE + title.hashCode();
        manager.notify(notificationId, notification);
    }

    public void updateCountdownNotification(String title, String countdown, String dateTime) {
        Notification notification = buildCountdownNotification(title, countdown, dateTime);
        manager.notify(NOTIFICATION_COUNTDOWN_ID, notification);
    }

    public void updateNoEventNotification() {
        manager.notify(NOTIFICATION_COUNTDOWN_ID, buildNoEventNotification());
    }

    public void cancelCountdownNotification() {
        manager.cancel(NOTIFICATION_COUNTDOWN_ID);
    }
}
