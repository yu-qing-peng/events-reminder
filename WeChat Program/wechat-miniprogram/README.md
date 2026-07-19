# Event Reminder - 微信小程序

基于原 Electron 桌面应用迁移的微信小程序版本。

## 功能特性

- 用户注册/登录（数据存储在微信云数据库）
- 事件管理（添加、删除、倒计时显示）
- 微信订阅消息提醒
- 暗黑主题界面

## 项目结构

```
wechat-miniprogram/
├── app.js              # 小程序入口
├── app.json            # 小程序配置
├── app.wxss            # 全局样式
├── pages/
│   ├── auth/           # 登录/注册页面
│   └── events/        # 事件列表页面
├── cloudfunctions/     # 云函数
│   ├── login/         # 登录云函数
│   ├── register/      # 注册云函数
│   ├── eventManager/  # 事件管理云函数
│   └── reminderTrigger/ # 提醒触发云函数
└── project.config.json # 项目配置
```

## 部署步骤

### 1. 开通微信云开发

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入小程序管理后台 → 开发 → 开发设置 → 云开发
3. 点击"开通"创建云开发环境
4. 记录环境 ID

### 2. 配置项目

修改以下文件中的占位符：

**project.config.json**
```json
{
  "appid": "YOUR_APPID"
}
```

**app.js**
```javascript
wx.cloud.init({
  env: 'YOUR_CLOUD_ENV_ID',  // 替换为你的云开发环境 ID
  traceUser: true,
});
```

**pages/events/events.js**
```javascript
const TEMPLATE_ID = 'YOUR_SUBSCRIBE_TEMPLATE_ID';  // 替换为订阅消息模板 ID
```

### 3. 创建云数据库集合

在云开发控制台 → 数据库创建以下集合：

1. `users` - 用户数据集合
2. `events` - 事件数据集合

### 4. 创建订阅消息模板

1. 进入微信公众平台 → 功能 → 订阅消息
2. 创建新模板，参考格式：
   - 标题：事件提醒
   - 字段1：事件名称 {{thing1}}
   - 字段2：提醒时间 {{thing2}}
   - 字段3：剩余时间 {{thing3}}
3. 获取模板 ID 并填入 `events.js` 和 `reminderTrigger/index.js`

### 5. 上传云函数

使用微信开发者工具：

1. 打开项目目录
2. 右键点击每个 cloudfunctions 文件夹
3. 选择"上传并部署：云端安装依赖"

或者使用命令行：
```bash
# 登录
wxcloud login

# 上传每个云函数
wxcloud upload login
wxcloud upload register
wxcloud upload eventManager
wxcloud upload reminderTrigger
```

### 6. 配置定时触发（可选）

要实现自动提醒，需要配置云函数的定时触发：

1. 进入云开发控制台 → 云函数
2. 选择 `reminderTrigger` 函数
3. 添加定时触发器：触发周期设为"每 5 分钟"

### 7. 运行小程序

1. 在微信开发者工具中点击"编译"
2. 使用手机扫码预览

## 使用说明

1. **首次使用**：注册新账户
2. **登录**：输入用户名和密码登录
3. **添加事件**：填写标题、日期、时间，点击"添加事件"
4. **删除事件**：点击事件右侧的 × 按钮
5. **开启提醒**：点击"启用提醒"按钮订阅消息
6. **登出**：点击右上角用户名旁的"× out"

## 注意事项

- 云函数免费额度：每月 1000 次调用
- 数据库免费容量：500MB
- 订阅消息需要在用户主动触发后调用
- 生产环境建议配置告警通知

## 目录结构说明

| 文件 | 说明 |
|------|------|
| `app.js` | 小程序全局逻辑，检查登录状态 |
| `app.json` | 页面路由和窗口配置 |
| `auth` 页面 | 登录/注册功能 |
| `events` 页面 | 事件列表和倒计时显示 |
| `login` 云函数 | 用户登录验证 |
| `register` 云函数 | 用户注册 |
| `eventManager` 云函数 | 事件的增删改查 |
| `reminderTrigger` 云函数 | 检查并标记待提醒事件 |