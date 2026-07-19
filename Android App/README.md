# Event Reminder

A cross-platform event reminder app with a desktop Electron client, an Android app, and a WeChat Mini Program — all backed by a shared Express.js + SQLite server.

## Structure

```
Android App/
├── WeChat folder/          Desktop (Electron) app + WeChat Mini Program
│   ├── server.js           Express API + SQLite database
│   ├── package.json        Electron app config
│   ├── main.js             Electron main process
│   ├── preload.js          Secure IPC bridge
│   ├── index.html          Full UI (HTML/CSS/JS)
│   ├── mini.html           Compact mini-window UI
│   └── wechat-miniprogram/ WeChat Mini Program
│       ├── pages/auth/     Login/Register
│       └── pages/events/   Event management
│
├── app/                    Native Android app (Java)
│   └── src/main/
│       ├── java/com/eventreminder/app/
│       │   ├── ui/         Activities + adapter
│       │   ├── data/api/   HTTP client + models
│       │   ├── service/    Foreground service + notifications
│       │   └── util/       Prefs + countdown formatter
│       └── res/            Layouts, drawables, fonts
├── build.gradle            Android build config
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/
├── .gitignore
└── README.md
```

## Features

- **User authentication** — Register/login with bcrypt-hashed passwords, session heartbeat
- **Event management** — Create, view, and delete events with title, date/time, and notes
- **Countdown display** — Live countdown for each event (Xd Yh, Xh Ym, Xm)
- **Notifications** — Native desktop notifications (Electron), foreground service countdown (Android), WeChat template messages
- **Multi-platform** — Windows desktop (Electron), Android (Java), WeChat Mini Program
- **Server sync** — Auto-syncs events every 15 seconds via heartbeat

## Getting Started

### Server (required for all clients)

```bash
cd "Android App/WeChat folder"
npm install
node server.js
```

Prints the local IP address — enter this in the app's Server IP field.

### Desktop App

```bash
cd "Android App/WeChat folder"
npm install
npm start
```

### Android App

Open `Android App/` in Android Studio, sync Gradle, and run on a device on the same network as the server.
