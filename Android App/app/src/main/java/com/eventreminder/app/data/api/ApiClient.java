package com.eventreminder.app.data.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 6;

    private final OkHttpClient client;
    private final Gson gson;
    private String baseUrl;

    public ApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void setServerIp(String ip) {
        this.baseUrl = "http://" + ip + ":3000";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean hasServerIp() {
        return baseUrl != null && !baseUrl.isEmpty();
    }

    public boolean testServer() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/")
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public ApiModels.AuthResponse login(String username, String password) throws IOException {
        ApiModels.AuthRequest body = new ApiModels.AuthRequest(username, password);
        String json = gson.toJson(body);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/login")
                .post(RequestBody.create(json, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.AuthResponse.class);
        }
    }

    public ApiModels.AuthResponse register(String username, String password) throws IOException {
        ApiModels.AuthRequest body = new ApiModels.AuthRequest(username, password);
        String json = gson.toJson(body);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/register")
                .post(RequestBody.create(json, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.AuthResponse.class);
        }
    }

    public ApiModels.DeleteResponse logout(int userId) throws IOException {
        ApiModels.LogoutRequest body = new ApiModels.LogoutRequest(userId);
        String json = gson.toJson(body);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/logout")
                .post(RequestBody.create(json, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.DeleteResponse.class);
        }
    }

    public ApiModels.HeartbeatResponse heartbeat(int userId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/heartbeat?userId=" + userId)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.HeartbeatResponse.class);
        }
    }

    public List<ApiModels.ServerEvent> getEvents(int userId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/events?userId=" + userId)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "[]";
            Type listType = new TypeToken<List<ApiModels.ServerEvent>>() {}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }

    public ApiModels.CreateEventResponse createEvent(int userId, String title, String description, String eventDate) throws IOException {
        ApiModels.CreateEventRequest body = new ApiModels.CreateEventRequest(userId, title, description, eventDate);
        String json = gson.toJson(body);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/events")
                .post(RequestBody.create(json, JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.CreateEventResponse.class);
        }
    }

    public ApiModels.DeleteResponse deleteEvent(int eventId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/events/" + eventId)
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return gson.fromJson(responseBody, ApiModels.DeleteResponse.class);
        }
    }
}
