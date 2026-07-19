package com.eventreminder.app.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventreminder.app.R;
import com.eventreminder.app.data.api.ApiClient;
import com.eventreminder.app.data.api.ApiModels;
import com.eventreminder.app.data.api.ApiService;
import com.eventreminder.app.service.HeartbeatService;
import com.eventreminder.app.service.NotificationHelper;
import com.eventreminder.app.service.ReminderReceiver;
import com.eventreminder.app.ui.adapter.EventAdapter;
import com.eventreminder.app.util.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements EventAdapter.EventListener {

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private EditText etEventTitle, etEventNote, etEventDate;
    private Button btnAddEvent;
    private RecyclerView rvEvents;
    private View layoutEmptyState;
    private TextView tvUserAvatar, tvUserName;

    private ApiClient apiClient;
    private ApiService apiService;
    private PrefManager prefs;
    private NotificationHelper notificationHelper;

    private List<ApiModels.ServerEvent> events;
    private EventAdapter adapter;
    private Handler countdownHandler;
    private Runnable countdownRunnable;

    private String selectedDate = "";
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PrefManager(this);
        if (!prefs.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        apiClient = new ApiClient();
        apiClient.setServerIp(prefs.getServerIp());
        apiService = new ApiService(apiClient);
        notificationHelper = new NotificationHelper(this);

        events = new ArrayList<>();

        initViews();
        setupUserPill();
        loadEvents();
        startCountdownUpdates();
        requestNotificationPermission();

        // Start heartbeat service
        startHeartbeatService();
    }

    private void initViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etEventDate = findViewById(R.id.etEventDate);
        etEventNote = findViewById(R.id.etEventNote);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        rvEvents = findViewById(R.id.rvEvents);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvUserAvatar = findViewById(R.id.tvUserAvatar);
        tvUserName = findViewById(R.id.tvUserName);

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(events, this);
        rvEvents.setAdapter(adapter);

        etEventDate.setOnClickListener(v -> showDatePicker());
        etEventDate.setFocusable(false);

        btnAddEvent.setOnClickListener(v -> addEvent());

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        findViewById(R.id.btnMinimize).setOnClickListener(v -> moveTaskToBack(true));
        findViewById(R.id.btnClose).setOnClickListener(v -> finishAffinity());
    }

    private void setupUserPill() {
        String username = prefs.getUsername();
        tvUserAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        tvUserName.setText(username);
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    showTimePicker();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 3600000);
        datePicker.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    Calendar cal = Calendar.getInstance();
                    cal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(cal.getTime());

                    String display = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(cal.getTime());
                    etEventDate.setText(display);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true);
        timePicker.show();
    }

    private void addEvent() {
        String title = etEventTitle.getText().toString().trim();
        String note = etEventNote.getText().toString().trim();

        if (title.isEmpty() || selectedDate.isEmpty()) {
            return;
        }

        btnAddEvent.setEnabled(false);
        btnAddEvent.setText("\u2026");

        apiService.createEvent(prefs.getUserId(), title, note, selectedDate,
                new ApiService.Callback<ApiModels.CreateEventResponse>() {
                    @Override
                    public void onSuccess(ApiModels.CreateEventResponse result) {
                        btnAddEvent.setEnabled(true);
                        btnAddEvent.setText(R.string.add_event);
                        if (result.success) {
                            etEventTitle.setText("");
                            etEventDate.setText("");
                            etEventNote.setText("");
                            selectedDate = "";
                            loadEvents();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        btnAddEvent.setEnabled(true);
                        btnAddEvent.setText(R.string.add_event);
                    }
                });
    }

    private void loadEvents() {
        apiService.getEvents(prefs.getUserId(), new ApiService.Callback<List<ApiModels.ServerEvent>>() {
            @Override
            public void onSuccess(List<ApiModels.ServerEvent> result) {
                events.clear();
                events.addAll(result);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                scheduleReminders();
            }

            @Override
            public void onError(String error) {
                events.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (events.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    private void startCountdownUpdates() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                countdownHandler.postDelayed(this, 30000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void scheduleReminders() {
        long now = System.currentTimeMillis();
        for (ApiModels.ServerEvent ev : events) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                Date evDate = sdf.parse(ev.eventDate);
                if (evDate == null || evDate.getTime() <= now) continue;

                long diff = evDate.getTime() - now;
                if (diff <= 60 * 60 * 1000) {
                    // Within 1 hour - show notification immediately
                    String countdown = formatCountdownShort(diff);
                    notificationHelper.showReminderNotification(ev.title, countdown);
                } else {
                    // Schedule alarm 1 hour before
                    long alarmTime = evDate.getTime() - 60 * 60 * 1000;
                    if (alarmTime > now && alarmTime - now < 24 * 60 * 60 * 1000) {
                        scheduleAlarm(alarmTime, ev.id, ev.title, ev.eventDate);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void scheduleAlarm(long triggerAtMillis, int eventId, String title, String eventDate) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("event_title", title);
        intent.putExtra("event_date", eventDate);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                this, eventId, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private String formatCountdownShort(long diffMs) {
        long minutes = diffMs / 60000;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + "h " + mins + "m";
    }

    private void startHeartbeatService() {
        Intent serviceIntent = new Intent(this, HeartbeatService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopHeartbeatService() {
        stopService(new Intent(this, HeartbeatService.class));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void logout() {
        apiService.logout(prefs.getUserId(), null);
        stopHeartbeatService();
        prefs.clearSession();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteEvent(int eventId) {
        apiService.deleteEvent(eventId, new ApiService.Callback<ApiModels.DeleteResponse>() {
            @Override
            public void onSuccess(ApiModels.DeleteResponse result) {
                loadEvents();
            }

            @Override
            public void onError(String error) {}
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        super.onDestroy();
    }
}
