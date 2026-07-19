const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
  showNotification: (title, body) => ipcRenderer.send('show-notification', { title, body }),
  updateMiniEvents: (eventData) => ipcRenderer.send('update-mini-events', eventData),
  onMiniEventsUpdated: (callback) => {
    ipcRenderer.on('mini-events-updated', (e, eventData) => callback(eventData));
  },
  getUpcomingEvent: () => ipcRenderer.invoke('get-upcoming-event'),
  logout: (userId) => ipcRenderer.send('user-logout', userId),
  heartbeat: (userId) => ipcRenderer.send('user-heartbeat', userId),
});

ipcRenderer.on('user-logout', (e, userId) => {
  const ip = localStorage.getItem('serverIP');
  if (ip) {
    fetch(`http://${ip}:3000/api/logout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId })
    }).catch(() => {});
  }
});

ipcRenderer.on('user-heartbeat', (e, userId) => {
  const ip = localStorage.getItem('serverIP');
  if (ip) {
    fetch(`http://${ip}:3000/api/heartbeat?userId=${userId}`).catch(() => {});
  }
});
