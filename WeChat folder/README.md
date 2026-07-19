# Event Reminder - Source Files

## What was wrong with the EXE
The original `Event_Reminder.exe` was just the raw `electron.exe` binary (v28.3.3)
with no app code bundled inside — electron-builder failed to embed the ASAR.
This is why it wouldn't open.

## Project structure
```
event-reminder-src/
├── package.json          ← updated build config
├── server.js             ← your Express API (unchanged)
└── src/
    ├── main.js           ← Electron main process
    ├── preload.js        ← secure IPC bridge
    └── index.html        ← full UI (HTML/CSS/JS in one file)
```

## How the server connection works
- The app shows a **Server IP** field on the login screen.
- Enter the IP where `server.js` is running (e.g. `192.168.1.42`).
- Click **Test** to verify connectivity, then login or register.
- The IP is saved in localStorage so you only enter it once.
- All API calls go to `http://<ip>:3000/api/login` and `/api/register`.

## Build steps (on Windows)

1. Put all files into one folder.
2. Run `npm install` (needs node-gyp / Visual Studio build tools for `better-sqlite3`).
3. Run `npm run build` → produces `dist/Event Reminder.exe` (portable).

## Run without building (dev mode)
```
npm install
npm start
```

## Start the server
```
node server.js
```
The server prints the local IP to use in the app.
