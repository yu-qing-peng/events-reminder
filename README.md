# Event Reminder

A cross-platform event reminder app with a desktop Electron client, a WeChat Mini Program, and an Android app — all backed by a shared Express.js + SQLite server.

## Structure

```
WeChat folder/     Desktop (Electron) app + WeChat Mini Program
Android App/       Native Android app (Java)
```

## Features

- **User authentication** — Register/login with bcrypt-hashed passwords, session heartbeat
- **Event management** — Create, view, and delete events with title, date/time, and notes
- **Countdown display** — Live countdown for each event
- **Notifications** — Native desktop notifications (Electron), foreground service countdown (Android)
- **Multi-platform** — Windows (Electron), Android (Java), WeChat Mini Program
- **Server sync** — Auto-syncs events every 15 seconds via heartbeat

## Getting Started

### Server (required for all clients)

```bash
cd "WeChat folder"
npm install
node server.js
```

Prints the local IP address — enter this in the app's Server IP field.

### Desktop App

```bash
cd "WeChat folder"
npm install
npm start
```

### Android App

Open `Android App/` in Android Studio, sync Gradle, and run on a device on the same network as the server.
