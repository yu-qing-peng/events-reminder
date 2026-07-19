# Event Reminder

Desktop event reminder app with AI assistant, built on Electron.

## Project structure

```
├── main.js          Electron main process (local HTTP server + window mgmt)
├── preload.js       Secure IPC bridge
├── index.html       Full UI (HTML/CSS/JS in one file)
├── mini.html        Compact mini window shown on minimize
├── server.js        Express + SQLite backend (auth, events, dashboard)
├── package.json
```

## How it works

- A **server.js** backend runs on your network (stores users/events in SQLite).
- The Electron app asks for the server's IP on the login screen, persists it in localStorage.
- All API calls go to `http://<ip>:3000/api/*`.
- AI queries go through a remote relay API (`gpt-5.4-mini` model).
- **Voice input** uses MediaRecorder + Deepgram API — paste your free Deepgram key in the AI card.

## Getting started

### 1. Start the server

```
node server.js
```

The server prints its local IP — use this in the Electron app.

### 2. Run the desktop app

```
npm install
npm start
```

### 3. Build a portable EXE

```
npm run build
```

Output goes to `dist/Event Reminder.exe`.

## Features

- **Auth**: register/login with bcrypt, 24h session persistence, heartbeat tracking
- **Events**: create, list, delete with automatic countdown
- **AI Assistant**: ask questions about your events, create events conversationally via tool calls with user confirmation
- **Voice input**: record via mic, transcribe via Deepgram API
- **Mini window**: on minimize, shows next upcoming event with live countdown
- **Server dashboard**: at `http://<server-ip>:3000/` — view/manage users and online status
