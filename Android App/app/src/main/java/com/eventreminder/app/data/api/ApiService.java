package com.eventreminder.app.data.api;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ApiService {

    private final ApiClient client;
    private final Executor executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public ApiService(ApiClient client) {
        this.client = client;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void testServer(Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean result = client.testServer();
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Cannot reach server");
            }
        });
    }

    public void login(String username, String password, Callback<ApiModels.AuthResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.AuthResponse result = client.login(username, password);
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Cannot reach server. Check the IP.");
            }
        });
    }

    public void register(String username, String password, Callback<ApiModels.AuthResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.AuthResponse result = client.register(username, password);
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Cannot reach server. Check the IP.");
            }
        });
    }

    public void logout(int userId, Callback<ApiModels.DeleteResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.DeleteResponse result = client.logout(userId);
                postResult(callback, result);
            } catch (IOException ignored) {}
        });
    }

    public void heartbeat(int userId, Callback<ApiModels.HeartbeatResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.HeartbeatResponse result = client.heartbeat(userId);
                if (callback != null) postResult(callback, result);
            } catch (IOException ignored) {}
        });
    }

    public void getEvents(int userId, Callback<java.util.List<ApiModels.ServerEvent>> callback) {
        executor.execute(() -> {
            try {
                java.util.List<ApiModels.ServerEvent> result = client.getEvents(userId);
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Failed to load events");
            }
        });
    }

    public void createEvent(int userId, String title, String description, String eventDate, Callback<ApiModels.CreateEventResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.CreateEventResponse result = client.createEvent(userId, title, description, eventDate);
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Failed to create event");
            }
        });
    }

    public void deleteEvent(int eventId, Callback<ApiModels.DeleteResponse> callback) {
        executor.execute(() -> {
            try {
                ApiModels.DeleteResponse result = client.deleteEvent(eventId);
                postResult(callback, result);
            } catch (IOException e) {
                postError(callback, "Failed to delete event");
            }
        });
    }

    private <T> void postResult(Callback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(Callback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
