# Event Reminder - Android App

Native Android app (Java) for Event Reminder — connects to the Express.js + SQLite server over LAN.

## Structure

```
app/
└── src/main/
    ├── java/com/eventreminder/app/
    │   ├── ui/             LoginActivity, MainActivity, EventAdapter
    │   │   └── adapter/
    │   ├── data/api/       ApiClient (OkHttp), ApiService, ApiModels
    │   ├── service/        HeartbeatService, NotificationHelper, ReminderReceiver
    │   └── util/           PrefManager, CountdownFormatter
    └── res/                Layouts, drawables, fonts (Syne + DM Mono)
```

## Features

- **User authentication** — Register/login with bcrypt-hashed passwords
- **Event management** — Create, view, and delete events with title, date/time, notes
- **Countdown display** — Live countdown per event (Xd Yh, Xh Ym, Xm)
- **Notifications** — Foreground service countdown while in background, alarm 1hr before events
- **Server sync** — Auto-syncs events every 15 seconds via heartbeat

## Getting Started

1. Start the server from the `WeChat folder` at repo root:
   ```bash
   cd WeChat folder
   npm install
   node server.js
   ```
2. Open this folder in Android Studio, sync Gradle, run on a device on the same network as the server.
3. Enter the server's local IP in the app and log in.
