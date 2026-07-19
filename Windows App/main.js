const { app, BrowserWindow, ipcMain, Notification, screen, session } = require('electron');
const path = require('path');
const http = require('http');
const fs = require('fs');

app.commandLine.appendSwitch('enable-features', 'WebSpeech,SpeechRecognition');
app.commandLine.appendSwitch('enable-speech-recognition');

let mainWindow = null;
let miniWindow = null;
let server = null;

const MIME = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
};

function startServer() {
  return new Promise((resolve) => {
    server = http.createServer((req, res) => {
      let filePath = req.url === '/' ? '/index.html' : req.url;
      filePath = path.join(__dirname, filePath);
      fs.readFile(filePath, (err, data) => {
        if (err) {
          res.writeHead(404);
          res.end();
          return;
        }
        const ext = path.extname(filePath);
        res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
        res.end(data);
      });
    });
    server.listen(0, 'localhost', () => {
      resolve(server.address().port);
    });
  });
}

async function createMainWindow() {
  const port = await startServer();
  const { width } = screen.getPrimaryDisplay().workAreaSize;

  session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
    if (permission === 'media' || permission === 'speech') callback(true);
    else callback(false);
  });

  mainWindow = new BrowserWindow({
    width: 420,
    height: 660,
    x: width - 430,
    y: 20,
    alwaysOnTop: true,
    resizable: false,
    frame: false,
    transparent: false,
    backgroundColor: '#0c0c0e',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: false,
    },
    icon: path.join(__dirname, 'icon.png'),
  });

  mainWindow.loadURL(`http://localhost:${port}/`);

  mainWindow.on('minimize', () => {
    if (miniWindow) return;
    createMiniWindow(port);
  });

  mainWindow.on('close', () => {
    if (miniWindow && !miniWindow.isDestroyed()) {
      miniWindow.destroy();
    }
  });
}

function createMiniWindow(port) {
  const { width } = screen.getPrimaryDisplay().workAreaSize;

  miniWindow = new BrowserWindow({
    width: 300,
    height: 160,
    x: width - 310,
    y: 20,
    alwaysOnTop: true,
    resizable: false,
    frame: false,
    transparent: false,
    skipTaskbar: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: false,
    },
  });

  miniWindow.loadURL(`http://localhost:${port}/mini.html`);

  miniWindow.on('closed', () => {
    miniWindow = null;
  });
}

ipcMain.on('window-minimize', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.minimize();
  }
});

ipcMain.on('window-maximize', () => {
  if (miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.close();
  }
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.restore();
    mainWindow.show();
    mainWindow.focus();
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.close();
  }
});

ipcMain.on('update-mini-events', (e, eventData) => {
  if (miniWindow && !miniWindow.isDestroyed()) {
    miniWindow.webContents.send('mini-events-updated', eventData);
  }
});

ipcMain.on('show-mini-window', () => {
  if (!miniWindow || miniWindow.isDestroyed()) {
    createMiniWindow();
  }
});

ipcMain.on('show-notification', (e, { title, body }) => {
  if (Notification.isSupported()) {
    new Notification({ title, body }).show();
  }
});

ipcMain.handle('get-upcoming-event', () => {
  return null;
});

app.whenReady().then(() => {
  createMainWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('will-quit', () => {
  if (server) server.close();
});
