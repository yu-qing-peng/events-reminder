package com.eventreminder.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eventreminder.app.data.api.ApiClient;
import com.eventreminder.app.data.api.ApiModels;
import com.eventreminder.app.util.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int eventId = intent.getIntExtra("event_id", -1);
        String eventTitle = intent.getStringExtra("event_title");
        String eventDate = intent.getStringExtra("event_date");

        if (eventTitle != null && eventDate != null) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                Date evDate = sdf.parse(eventDate);
                if (evDate != null) {
                    long diff = evDate.getTime() - System.currentTimeMillis();
                    String countdown = formatReminderCountdown(diff);
                    notificationHelper.showReminderNotification(eventTitle, countdown);
                }
            } catch (Exception ignored) {}
        }
    }

    private String formatReminderCountdown(long diffMs) {
        if (diffMs <= 0) return "now";
        long minutes = diffMs / 60000;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "h " + mins + "m";
    }
}
