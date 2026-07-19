const app = getApp();

Page({
  data: {
    currentTab: 'login',
    username: '',
    password: '',
    serverIP: '',
    serverStatus: '',
    serverStatusType: '',
    testing: false,
    loading: false,
    msg: '',
    msgType: ''
  },

  onLoad() {
    const saved = wx.getStorageSync('serverIP');
    if (saved) {
      this.setData({ serverIP: saved });
    }
    if (app.isLoggedIn()) {
      this.goToEvents();
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({
      currentTab: tab,
      msg: '',
      msgType: ''
    });
  },

  onInputUsername(e) {
    this.setData({ username: e.detail.value });
  },

  onInputPassword(e) {
    this.setData({ password: e.detail.value });
  },

  onInputServerIP(e) {
    this.setData({
      serverIP: e.detail.value,
      serverStatus: '',
      serverStatusType: ''
    });
  },

  getServerUrl() {
    const ip = this.data.serverIP.trim();
    if (!ip) return '';
    return `http://${ip}:3000`;
  },

  async testServer() {
    const serverUrl = this.getServerUrl();
    if (!serverUrl) {
      this.setData({
        serverStatus: 'Enter a server IP first',
        serverStatusType: 'err'
      });
      return;
    }

    this.setData({ testing: true, serverStatus: '', serverStatusType: '' });

    try {
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: serverUrl + '/',
          method: 'GET',
          timeout: 4000,
          success: resolve,
          fail: reject
        });
      });

      if (res.statusCode === 200) {
        this.setData({
          serverStatus: '✓ Server reachable',
          serverStatusType: 'ok'
        });
        app.setServerBase(serverUrl);
        wx.setStorageSync('serverIP', this.data.serverIP.trim());
      } else {
        this.setData({
          serverStatus: '✗ HTTP ' + res.statusCode,
          serverStatusType: 'err'
        });
      }
    } catch (e) {
      this.setData({
        serverStatus: '✗ Cannot reach server',
        serverStatusType: 'err'
      });
    } finally {
      this.setData({ testing: false });
    }
  },

  async submitAuth() {
    const { username, password, currentTab } = this.data;
    const serverUrl = this.getServerUrl();

    if (!serverUrl) {
      this.showMsg('Enter server IP first.', 'err');
      return;
    }

    if (!username || !password) {
      this.showMsg('Please fill in all fields.', 'err');
      return;
    }

    this.setData({ loading: true, msg: '' });

    try {
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: serverUrl + (currentTab === 'login' ? '/api/login' : '/api/register'),
          method: 'POST',
          header: { 'Content-Type': 'application/json' },
          data: { username, password },
          timeout: 6000,
          success: resolve,
          fail: reject
        });
      });

      if (res.statusCode === 200 && res.data.success) {
        if (currentTab === 'register') {
          this.showMsg('✓ Account created! Signing in...', 'ok');
          setTimeout(() => this.doLoginDirectly(username, password), 800);
        } else {
          this.doLogin(username, res.data.userId);
        }
      } else {
        this.showMsg(res.data.error || 'Something went wrong.', 'err');
      }
    } catch (e) {
      this.showMsg('✗ Cannot reach server. Check the IP.', 'err');
    } finally {
      this.setData({ loading: false });
    }
  },

  async doLoginDirectly(username, password) {
    try {
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: this.getServerUrl() + '/api/login',
          method: 'POST',
          header: { 'Content-Type': 'application/json' },
          data: { username, password },
          success: resolve,
          fail: reject
        });
      });
      if (res.statusCode === 200 && res.data.success) {
        this.doLogin(username, res.data.userId);
      }
    } catch (e) {}
  },

  doLogin(username, userId) {
    app.setServerBase(this.getServerUrl());
    wx.setStorageSync('serverIP', this.data.serverIP.trim());
    app.setUserInfo(userId, username);
    this.goToEvents();
  },

  goToEvents() {
    wx.redirectTo({
      url: '/pages/events/events'
    });
  },

  showMsg(text, type) {
    this.setData({
      msg: text,
      msgType: type
    });
  }
});