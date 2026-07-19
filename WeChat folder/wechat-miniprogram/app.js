const DEFAULT_PORT = 3000;

App({
  globalData: {
    userInfo: null,
    userId: null,
    serverBase: ''
  },

  onLaunch() {
    this.checkLoginStatus();
    this.restoreServerBase();
  },

  checkLoginStatus() {
    const userId = wx.getStorageSync('userId');
    const username = wx.getStorageSync('username');
    if (userId && username) {
      this.globalData.userId = userId;
      this.globalData.userInfo = { nickName: username };
    }
  },

  restoreServerBase() {
    const serverBase = wx.getStorageSync('serverBase');
    if (serverBase) {
      this.globalData.serverBase = serverBase;
    }
  },

  setServerBase(url) {
    this.globalData.serverBase = url;
    wx.setStorageSync('serverBase', url);
  },

  getServerBase() {
    return this.globalData.serverBase || wx.getStorageSync('serverBase') || '';
  },

  setUserInfo(userId, username) {
    this.globalData.userId = userId;
    this.globalData.userInfo = { nickName: username };
    wx.setStorageSync('userId', userId);
    wx.setStorageSync('username', username);
  },

  clearUserInfo() {
    this.globalData.userId = null;
    this.globalData.userInfo = null;
    wx.removeStorageSync('userId');
    wx.removeStorageSync('username');
  },

  isLoggedIn() {
    return !!this.globalData.userId;
  }
});