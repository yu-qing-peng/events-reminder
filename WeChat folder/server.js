const express = require('express');
const Database = require('better-sqlite3');
const bcrypt = require('bcryptjs');
const cors = require('cors');
const os = require('os');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const db = new Database('server.db');

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  )
`);

db.exec(`
  CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    eventDate TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES users(id)
  )
`);

const loggedInUsers = new Map();

const SESSION_TIMEOUT = 30 * 60 * 1000;

function cleanupSessions() {
  const now = Date.now();
  for (const [userId, data] of loggedInUsers) {
    if (now - data.lastActivity > SESSION_TIMEOUT) {
      loggedInUsers.delete(userId);
    }
  }
}

setInterval(cleanupSessions, 5 * 60 * 1000);

function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

app.post('/api/register', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing username or password' });
  }
  try {
    const hashed = await bcrypt.hash(password, 10);
    const stmt = db.prepare('INSERT INTO users (username, password) VALUES (?, ?)');
    stmt.run(username, hashed);
    res.json({ success: true });
  } catch (err) {
    if (err.code === 'SQLITE_CONSTRAINT_UNIQUE') {
      res.status(409).json({ error: 'Username already exists' });
    } else {
      res.status(500).json({ error: 'Server error' });
    }
  }
});

app.post('/api/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Missing username or password' });
  }
  const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
  if (!user) {
    return res.status(401).json({ error: 'Invalid credentials' });
  }
  bcrypt.compare(password, user.password).then(valid => {
    if (!valid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    loggedInUsers.set(user.id, {
      username: user.username,
      loginTime: Date.now(),
      lastActivity: Date.now()
    });
    res.json({ success: true, userId: user.id });
  });
});

app.post('/api/logout', (req, res) => {
  const { userId } = req.body;
  if (userId) {
    loggedInUsers.delete(parseInt(userId));
  }
  res.json({ success: true });
});

app.get('/api/heartbeat', (req, res) => {
  const { userId } = req.query;
  if (userId && loggedInUsers.has(parseInt(userId))) {
    loggedInUsers.get(parseInt(userId)).lastActivity = Date.now();
    res.json({ success: true });
  } else {
    res.status(401).json({ error: 'Not logged in' });
  }
});

app.get('/api/users', (req, res) => {
  cleanupSessions();
  const users = db.prepare('SELECT id, username, created_at FROM users ORDER BY created_at DESC').all();
  const result = users.map(u => ({
    ...u,
    isOnline: loggedInUsers.has(u.id)
  }));
  res.json(result);
});

app.get('/api/logged-in-users', (req, res) => {
  cleanupSessions();
  const result = [];
  for (const [userId, data] of loggedInUsers) {
    result.push({
      userId,
      username: data.username,
      loginTime: data.loginTime,
      lastActivity: data.lastActivity
    });
  }
  res.json(result);
});

app.delete('/api/users/:id', (req, res) => {
  const { id } = req.params;
  try {
    db.prepare('DELETE FROM events WHERE userId = ?').run(id);
    db.prepare('DELETE FROM users WHERE id = ?').run(id);
    loggedInUsers.delete(parseInt(id));
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete user' });
  }
});

app.post('/api/events', (req, res) => {
  const { userId, title, description, eventDate } = req.body;
  if (!userId || !title || !eventDate) {
    return res.status(400).json({ error: 'Missing required fields' });
  }
  try {
    const stmt = db.prepare('INSERT INTO events (userId, title, description, eventDate) VALUES (?, ?, ?, ?)');
    const result = stmt.run(userId, title, description || '', eventDate);
    res.json({ success: true, id: result.lastInsertRowid });
  } catch (err) {
    res.status(500).json({ error: 'Failed to create event' });
  }
});

app.get('/api/events', (req, res) => {
  const userId = req.query.userId;
  if (!userId) {
    return res.status(400).json({ error: 'Missing userId' });
  }
  try {
    const events = db.prepare('SELECT * FROM events WHERE userId = ? ORDER BY eventDate ASC').all(userId);
    res.json(events);
  } catch (err) {
    res.status(500).json({ error: 'Failed to fetch events' });
  }
});

app.delete('/api/events/:id', (req, res) => {
  const { id } = req.params;
  try {
    db.prepare('DELETE FROM events WHERE id = ?').run(id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete event' });
  }
});

const server = app.listen(PORT, '0.0.0.0', () => {
  const ip = getLocalIP();
  console.log(`Server running at http://${ip}:${PORT}`);
  console.log(`Use this IP address for desktop client connection`);
});

const ip = getLocalIP();

app.get('/', (req, res) => {
  res.type('html').send(`
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Server Dashboard</title>
  <link href="https://fonts.googleapis.com/css2?family=DM+Mono:wght@300;400;500&family=Syne:wght@400;600;700;800&display=swap" rel="stylesheet" />
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root {
      --bg: #0c0c0e;
      --surface: #141418;
      --border: #232328;
      --border-bright: #3a3a44;
      --accent: #c8ff00;
      --text: #e8e8f0;
      --text-muted: #6b6b7a;
      --danger: #ff4d6d;
      --success: #00e5a0;
      --font-head: 'Syne', sans-serif;
      --font-mono: 'DM Mono', monospace;
      --radius: 10px;
    }
    html, body { background: var(--bg); color: var(--text); font-family: var(--font-mono); min-height: 100vh; }
    .container { max-width: 800px; margin: 0 auto; padding: 40px 24px; }
    .header { margin-bottom: 40px; }
    .header h1 { font-family: var(--font-head); font-size: 24px; font-weight: 800; color: var(--accent); margin-bottom: 8px; }
    .server-ip { font-size: 13px; color: var(--text-muted); }
    .server-ip span { color: var(--text); }
    .section { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px; margin-bottom: 24px; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .section-header h2 { font-family: var(--font-head); font-size: 14px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--text-muted); }
    .count { font-size: 11px; color: var(--text-muted); background: var(--bg); padding: 4px 10px; border-radius: 20px; }
    .count.online { color: var(--success); }
    .refresh-btn { font-size: 10px; padding: 6px 12px; background: var(--bg); border: 1px solid var(--border); border-radius: var(--radius); color: var(--text-muted); cursor: pointer; font-family: var(--font-mono); transition: all 0.15s; }
    .refresh-btn:hover { border-color: var(--border-bright); color: var(--text); }
    .refresh-btn.loading { opacity: 0.5; }
    table { width: 100%; border-collapse: collapse; }
    th { font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-muted); text-align: left; padding: 8px 12px; border-bottom: 1px solid var(--border); }
    td { font-size: 12px; padding: 10px 12px; border-bottom: 1px solid var(--border); }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: rgba(200, 255, 0, 0.02); }
    .status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 8px; }
    .status-dot.online { background: var(--success); }
    .status-dot.offline { background: var(--text-muted); }
    .del-btn { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 14px; padding: 4px 8px; transition: color 0.15s; opacity: 0.5; }
    .del-btn:hover { color: var(--danger); opacity: 1; }
    .empty-state { text-align: center; padding: 24px; color: var(--text-muted); font-size: 12px; }
    .online-users-list { display: flex; flex-direction: column; gap: 8px; }
    .online-user { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; background: var(--bg); border-radius: var(--radius); }
    .online-user-name { font-size: 12px; }
    .online-user-meta { font-size: 10px; color: var(--text-muted); margin-top: 2px; }
    .auto-refresh { display: flex; align-items: center; gap: 8px; font-size: 10px; color: var(--text-muted); }
    .auto-refresh input { accent-color: var(--accent); }
    .toast { position: fixed; bottom: 20px; right: 20px; background: var(--success); color: #000; padding: 10px 16px; border-radius: var(--radius); font-size: 12px; opacity: 0; transition: opacity 0.2s; }
    .toast.show { opacity: 1; }
    .toast.error { background: var(--danger); color: var(--text); }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Server Dashboard</h1>
      <div class="server-ip">Running at <span>http://${ip}:${PORT}</span></div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2>Online Now</h2>
        <div class="auto-refresh">
          <input type="checkbox" id="autoRefresh" checked />
          <label for="autoRefresh">Auto-refresh</label>
        </div>
      </div>
      <div id="onlineUsers" class="online-users-list">
        <div class="empty-state">Loading...</div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2>All Users</h2>
        <div style="display:flex;gap:8px;align-items:center;">
          <span id="userCount" class="count">0</span>
          <button class="refresh-btn" id="refreshBtn" onclick="loadData()">Refresh</button>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>User</th>
            <th>Joined</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody id="usersTable">
          <tr><td colspan="4" class="empty-state">Loading...</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <div id="toast" class="toast"></div>

  <script>
    let autoRefreshEnabled = true;
    let refreshInterval = null;

    const autoRefreshCheckbox = document.getElementById('autoRefresh');
    const refreshBtn = document.getElementById('refreshBtn');

    autoRefreshCheckbox.addEventListener('change', (e) => {
      autoRefreshEnabled = e.target.checked;
      if (autoRefreshEnabled) startAutoRefresh();
      else stopAutoRefresh();
    });

    function startAutoRefresh() {
      if (refreshInterval) clearInterval(refreshInterval);
      refreshInterval = setInterval(loadData, 5000);
    }

    function stopAutoRefresh() {
      if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
      }
    }

    async function loadData() {
      const btn = document.getElementById('refreshBtn');
      btn.classList.add('loading');
      btn.textContent = '...';

      try {
        const [usersRes, onlineRes] = await Promise.all([
          fetch('/api/users'),
          fetch('/api/logged-in-users')
        ]);

        const users = await usersRes.json();
        const onlineUsers = await onlineRes.json();

        renderOnlineUsers(onlineUsers);
        renderUsersTable(users);
        document.getElementById('userCount').textContent = users.length;
        document.getElementById('userCount').className = 'count' + (users.some(u => u.isOnline) ? ' online' : '');
      } catch (e) {
        showToast('Failed to load data', true);
      }

      btn.classList.remove('loading');
      btn.textContent = 'Refresh';
    }

    function renderOnlineUsers(users) {
      const container = document.getElementById('onlineUsers');
      if (users.length === 0) {
        container.innerHTML = '<div class="empty-state">No users online</div>';
        return;
      }
      container.innerHTML = users.map(u => {
        const loginTime = new Date(u.loginTime).toLocaleTimeString();
        return '<div class="online-user"><div><div class="online-user-name">' + u.username + '</div><div class="online-user-meta">Since ' + loginTime + '</div></div><span class="status-dot online"></span></div>';
      }).join('');
    }

    function renderUsersTable(users) {
      const tbody = document.getElementById('usersTable');
      if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No users registered</td></tr>';
        return;
      }
      tbody.innerHTML = users.map(u => {
        const joined = new Date(u.created_at).toLocaleDateString();
        return '<tr><td>' + u.username + '</td><td style="color:var(--text-muted)">' + joined + '</td><td><span class="status-dot ' + (u.isOnline ? 'online' : 'offline') + '"></span>' + (u.isOnline ? 'Online' : 'Offline') + '</td><td><button class="del-btn" onclick="deleteUser(' + u.id + ')">×</button></td></tr>';
      }).join('');
    }

    async function deleteUser(id) {
      if (!confirm('Delete this user and all their events?')) return;
      try {
        const res = await fetch('/api/users/' + id, { method: 'DELETE' });
        if (res.ok) {
          showToast('User deleted');
          loadData();
        } else {
          showToast('Failed to delete', true);
        }
      } catch (e) {
        showToast('Failed to delete', true);
      }
    }

    function showToast(msg, isError = false) {
      const toast = document.getElementById('toast');
      toast.textContent = msg;
      toast.className = 'toast' + (isError ? ' error' : '') + ' show';
      setTimeout(() => toast.classList.remove('show'), 2000);
    }

    loadData();
    startAutoRefresh();
  </script>
</body>
</html>
  `);
});
