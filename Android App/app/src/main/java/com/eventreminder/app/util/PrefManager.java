package com.eventreminder.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREFS_NAME = "event_reminder_prefs";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences prefs;

    public PrefManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setServerIp(String ip) {
        prefs.edit().putString(KEY_SERVER_IP, ip).apply();
    }

    public String getServerIp() {
        return prefs.getString(KEY_SERVER_IP, "");
    }

    public void setUserId(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public boolean isLoggedIn() {
        return getUserId() > 0 && !getUsername().isEmpty() && !getServerIp().isEmpty();
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
