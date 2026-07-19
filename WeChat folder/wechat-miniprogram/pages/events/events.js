const app = getApp();

Page({
  data: {
    username: '',
    avatar: '?',
    events: [],
    newTitle: '',
    newDate: '',
    newTime: '',
    newNote: '',
    subscribed: false,
    countdownTimer: null,
    syncTimer: null
  },

  onLoad() {
    const userInfo = app.globalData.userInfo;
    const userId = app.globalData.userId;

    if (!userId || !app.getServerBase()) {
      this.goToAuth();
      return;
    }

    this.setData({
      username: userInfo.nickName,
      avatar: userInfo.nickName[0].toUpperCase()
    });

    this.loadEvents();
    this.checkSubscription();
    this.startCountdown();
    this.startSync();
  },

  onUnload() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    if (this.data.syncTimer) {
      clearInterval(this.data.syncTimer);
    }
  },

  goToAuth() {
    wx.redirectTo({ url: '/pages/auth/auth' });
  },

  request(method, path, data) {
    const base = app.getServerBase();
    return new Promise((resolve, reject) => {
      wx.request({
        url: base + path,
        method,
        header: { 'Content-Type': 'application/json' },
        data,
        timeout: 8000,
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res.data);
          } else {
            reject(new Error(res.data.error || 'Request failed'));
          }
        },
        fail: reject
      });
    });
  },

  async loadEvents() {
    try {
      const data = await this.request('GET', '/api/events', {
        userId: app.globalData.userId
      });

      const events = Array.isArray(data) ? data : [];
      this.setData({ events });
      this.renderEvents();
    } catch (err) {
      console.error('Load events error:', err);
      wx.showToast({ title: 'Failed to load events', icon: 'none' });
    }
  },

  renderEvents() {
    const events = this.data.events.map(ev => {
      const now = new Date();
      const evDate = new Date(ev.eventDate);
      const diff = evDate - now;
      const past = diff < 0;
      const soon = diff > 0 && diff < 60 * 60 * 1000;

      let dotClass = '';
      let cdClass = '';
      let countdown = '';

      if (past) {
        dotClass = 'past';
        cdClass = 'past';
        countdown = 'Passed';
      } else if (soon) {
        dotClass = 'soon';
        cdClass = 'soon';
        countdown = `In ${Math.round(diff / 60000)}m`;
      } else {
        countdown = this.formatCountdown(diff);
      }

      const timeLabel = this.formatDate(evDate);

      return {
        ...ev,
        dotClass,
        cdClass,
        countdown,
        timeLabel,
        note: ev.description
      };
    });

    this.setData({ events });
  },

  formatCountdown(ms) {
    const days = Math.floor(ms / 86400000);
    const hrs = Math.floor((ms % 86400000) / 3600000);
    const mins = Math.floor((ms % 3600000) / 60000);
    if (days > 0) return `${days}d ${hrs}h`;
    if (hrs > 0) return `${hrs}h ${mins}m`;
    return `${mins}m`;
  },

  formatDate(date) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const month = months[date.getMonth()];
    const day = date.getDate();
    const weekday = weekdays[date.getDay()];
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');
    return `${weekday}, ${month} ${day} ${hour}:${minute}`;
  },

  startCountdown() {
    const timer = setInterval(() => {
      this.renderEvents();
    }, 30000);
    this.setData({ countdownTimer: timer });
  },

  startSync() {
    const timer = setInterval(() => {
      this.loadEvents();
    }, 15000);
    this.setData({ syncTimer: timer });
  },

  onInputTitle(e) {
    this.setData({ newTitle: e.detail.value });
  },

  onDateChange(e) {
    this.setData({ newDate: e.detail.value });
  },

  onTimeChange(e) {
    this.setData({ newTime: e.detail.value });
  },

  onInputNote(e) {
    this.setData({ newNote: e.detail.value });
  },

  async addEvent() {
    const { newTitle, newDate, newTime, newNote } = this.data;

    if (!newTitle || !newDate || !newTime) {
      wx.showToast({ title: 'Please fill all fields', icon: 'none' });
      return;
    }

    const eventDate = `${newDate} ${newTime}`;

    try {
      const result = await this.request('POST', '/api/events', {
        userId: app.globalData.userId,
        title: newTitle,
        description: newNote,
        eventDate: eventDate
      });

      if (result.success) {
        this.setData({
          newTitle: '',
          newDate: '',
          newTime: '',
          newNote: ''
        });
        this.loadEvents();
        wx.showToast({ title: 'Event added!', icon: 'success' });
      } else {
        wx.showToast({ title: 'Failed to add event', icon: 'none' });
      }
    } catch (err) {
      console.error('Add event error:', err);
      wx.showToast({ title: 'Error', icon: 'none' });
    }
  },

  async deleteEvent(e) {
    const id = e.currentTarget.dataset.id;

    wx.showModal({
      title: 'Delete Event',
      content: 'Are you sure?',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await this.request('DELETE', `/api/events/${id}`);

            if (result.success) {
              this.loadEvents();
              wx.showToast({ title: 'Deleted', icon: 'success' });
            }
          } catch (err) {
            console.error('Delete error:', err);
          }
        }
      }
    });
  },

  async requestSubscribe() {
    try {
      const result = await wx.requestSubscribeMessage({
        tmplIds: ['YOUR_TEMPLATE_ID_HERE']
      });

      if (result.errMsg === 'requestSubscribeMessage:ok') {
        this.setData({ subscribed: true });
        wx.showToast({ title: 'Reminders enabled!', icon: 'success' });
      }
    } catch (err) {
      console.error('Subscribe error:', err);
    }
  },

  checkSubscription() {
    const subscribed = wx.getStorageSync('subscribed');
    this.setData({ subscribed: !!subscribed });
  },

  logout() {
    wx.showModal({
      title: 'Logout',
      content: 'Are you sure you want to logout?',
      success: (res) => {
        if (res.confirm) {
          app.clearUserInfo();
          this.goToAuth();
        }
      }
    });
  }
});