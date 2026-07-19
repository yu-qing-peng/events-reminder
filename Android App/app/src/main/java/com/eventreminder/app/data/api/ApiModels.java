package com.eventreminder.app.data.api;

public class ApiModels {

    public static class AuthRequest {
        public String username;
        public String password;

        public AuthRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public boolean success;
        public int userId;
        public String error;
    }

    public static class LogoutRequest {
        public int userId;

        public LogoutRequest(int userId) {
            this.userId = userId;
        }
    }

    public static class CreateEventRequest {
        public int userId;
        public String title;
        public String description;
        public String eventDate;

        public CreateEventRequest(int userId, String title, String description, String eventDate) {
            this.userId = userId;
            this.title = title;
            this.description = description;
            this.eventDate = eventDate;
        }
    }

    public static class CreateEventResponse {
        public boolean success;
        public int id;
        public String error;
    }

    public static class DeleteResponse {
        public boolean success;
        public String error;
    }

    public static class HeartbeatResponse {
        public boolean success;
        public String error;
    }

    public static class ServerEvent {
        public int id;
        public int userId;
        public String title;
        public String description;
        public String eventDate;
        public String created_at;
    }
}
