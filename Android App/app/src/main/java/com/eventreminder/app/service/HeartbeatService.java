package com.eventreminder.app.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.eventreminder.app.data.api.ApiClient;
import com.eventreminder.app.data.api.ApiModels;
import com.eventreminder.app.util.CountdownFormatter;
import com.eventreminder.app.util.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HeartbeatService extends Service {

    private static final long HEARTBEAT_INTERVAL = 15000;
    private static final long COUNTDOWN_UPDATE_INTERVAL = 5000;

    private Handler handler;
    private boolean running = false;
    private ApiClient apiClient;
    private PrefManager prefs;
    private NotificationHelper notificationHelper;
    private Executor executor;
    private List<ApiModels.ServerEvent> cachedEvents;
    private Runnable heartbeatRunnable;
    private Runnable countdownRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        prefs = new PrefManager(this);
        String serverIp = prefs.getServerIp();
        apiClient = new ApiClient();
        if (!serverIp.isEmpty()) {
            apiClient.setServerIp(serverIp);
        }
        notificationHelper = new NotificationHelper(this);

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                doHeartbeat();
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        };

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdownNotification();
                handler.postDelayed(this, COUNTDOWN_UPDATE_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NotificationHelper.NOTIFICATION_COUNTDOWN_ID,
                        notificationHelper.buildCountdownNotification("Starting...", "", ""),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NotificationHelper.NOTIFICATION_COUNTDOWN_ID,
                        notificationHelper.buildCountdownNotification("Starting...", "", ""));
            }
            handler.post(heartbeatRunnable);
            handler.post(countdownRunnable);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(heartbeatRunnable);
        handler.removeCallbacks(countdownRunnable);
        notificationHelper.cancelCountdownNotification();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doHeartbeat() {
        int userId = prefs.getUserId();
        if (userId <= 0 || !apiClient.hasServerIp()) return;

        executor.execute(() -> {
            try {
                apiClient.heartbeat(userId);
            } catch (Exception ignored) {}

            try {
                List<ApiModels.ServerEvent> events = apiClient.getEvents(userId);
                cachedEvents = events;
            } catch (Exception ignored) {}
        });
    }

    private void updateCountdownNotification() {
        if (cachedEvents == null || cachedEvents.isEmpty()) {
            notificationHelper.updateNoEventNotification();
            return;
        }

        long now = System.currentTimeMillis();
        ApiModels.ServerEvent next = null;

        for (ApiModels.ServerEvent ev : cachedEvents) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                Date evDate = sdf.parse(ev.eventDate);
                if (evDate != null && evDate.getTime() > now) {
                    if (next == null || evDate.getTime() < sdf.parse(next.eventDate).getTime()) {
                        next = ev;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (next == null) {
            notificationHelper.updateNoEventNotification();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            Date evDate = sdf.parse(next.eventDate);
            if (evDate != null) {
                long diff = evDate.getTime() - System.currentTimeMillis();
                String countdown = CountdownFormatter.formatNotification(diff);

                SimpleDateFormat displayFmt = new SimpleDateFormat("EEE, MMM d HH:mm", Locale.US);
                String dateTime = displayFmt.format(evDate);

                notificationHelper.updateCountdownNotification(next.title, countdown, dateTime);
            }
        } catch (Exception ignored) {}
    }
}
