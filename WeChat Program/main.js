const { app, BrowserWindow, ipcMain, Notification, screen } = require('electron');
const path = require('path');

let mainWindow = null;
let miniWindow = null;

function createMainWindow() {
  const { width } = screen.getPrimaryDisplay().workAreaSize;

  mainWindow = new BrowserWindow({
    width: 420,
    height: 620,
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
    },
    icon: path.join(__dirname, 'icon.png'),
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));

  mainWindow.on('minimize', () => {
    if (miniWindow) return;
    createMiniWindow();
  });

  mainWindow.on('close', () => {
    if (miniWindow && !miniWindow.isDestroyed()) {
      miniWindow.destroy();
    }
  });
}

function createMiniWindow() {
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
    },
  });

  miniWindow.loadFile(path.join(__dirname, 'mini.html'));

  miniWindow.once('ready-to-show', () => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.executeJavaScript('updateMiniWindow()').catch(() => {});
    }
  });

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

ipcMain.handle('get-upcoming-event', async () => {
  if (!mainWindow || mainWindow.isDestroyed()) return null;
  try {
    return await mainWindow.webContents.executeJavaScript(`
      (() => {
        const now = new Date();
        const upcoming = (window.events || [])
          .filter(ev => new Date(ev.eventDate) > now)
          .sort((a, b) => new Date(a.eventDate) - new Date(b.eventDate));
        return upcoming[0] || null;
      })()
    `);
  } catch {
    return null;
  }
});

app.whenReady().then(() => {
  createMainWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
