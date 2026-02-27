# OrangePlayer 更新日志

## [1.2.0] - 2026-02-27

### ✨ 新功能

#### 控制器可见性控制
- 新增 `setControllerVisibilityEnabled(boolean)` API，支持临时禁用控制器UI显示
- 禁用后控制器功能保留，但UI不显示，适用于后台控制场景
- 启用/禁用时立即触发控制器显示/隐藏

#### M3U8下载增强
- M3U8下载合并输出为MP4格式
- 用纯Java实现TS合并，移除JeffVideoLib依赖
- 删除未使用的 `JeffVideoLib-extracted` 目录

### 🐛 Bug修复

#### 下载管理修复
- 修复 RecyclerView 并发崩溃问题
- 修复线程池终止崩溃问题
- 修复下载完成图标显示问题
- 修复重复 Toast 提示问题
- 移除 DialogX 依赖，减少第三方依赖

---

## [1.1.9] - 2025-02

### ✨ 新功能

#### 视频嗅探增强
- 添加嗅探自动播放第一个视频功能
- 完善嗅探自动播放功能
- 优化视频嗅探功能，减少 ANR 问题
- 修复视频嗅探时的 ANR 问题
- 最小化 WebView 控件并修复资源拦截逻辑

#### 播放功能
- 实现选集播放和播放模式功能
- 实现智能全屏功能，根据视频宽高比自动选择全屏模式
- 添加视频嗅探开源库对比分析报告

### 🐛 Bug修复

- 修复智能全屏和弹幕组件 ANR 问题
- 修复嗅探状态时准备视频控件的显示问题
- 优化嗅探状态时的 UI 显示
- 修复 ExoPlayer Activity 切换后状态保存问题
- 修复跳过片头片尾功能和临时设置持久化问题

---

## [1.1.3] - 2024-02

### ✨ 新功能

- 在 Maven Central 发布脚本中添加 gsyVideoPlayer-ex_so 模块
- 引入 gsyVideoPlayer-ex_so 模块支持 IJK 播放加密视频
- 在播放错误界面添加设置按钮并优化设置弹窗大小适配

### 🐛 Bug修复

- 修复 GSYVideoPlayer 依赖冲突
- 修复 IJK 播放器可用性检测不完整导致闪退
- 修复 IJK 播放器后台切换进度重置问题
- 修复阿里云播放器画面比例切换问题
- 修复 m3u8 seek 画面异常和后台自动恢复播放问题

---

## [1.1.1] - 2024-01

### 🎉 首次发布

#### 核心功能
- 基于 GSYVideoPlayer 的增强视频播放器
- 支持 Android 4.0+ (API 14+)
- 多播放器内核支持：系统播放器、ExoPlayer、IJK播放器、阿里云播放器

#### 特色功能
- **弹幕支持** - 集成 DanmakuFlameMaster
- **字幕支持** - 支持 SRT/ASS/VTT 格式
- **画面比例** - 支持多种画面比例切换
- **手势控制** - 音量、亮度、进度调节
- **倍速播放** - 支持 0.35x - 10x 倍速
- **投屏功能** - DLNA 投屏支持
- **画中画** - Android 8.0+ 画中画模式
- **视频嗅探** - 网页视频自动嗅探
- **下载功能** - 视频下载管理

#### 播放器内核
| 内核 | 说明 |
|------|------|
| 系统播放器 | MediaPlayer，无需额外依赖 |
| ExoPlayer | 推荐默认，格式支持全 |
| IJK播放器 | 格式支持最全 |
| 阿里云播放器 | 性能最好，需 License |

---

## 版本号说明

采用三段式版本号：`主版本.功能版本.修复版本`

- **主版本** - 重大架构变更或不兼容更新
- **功能版本** - 新增功能或较大改进
- **修复版本** - Bug修复或小改进

---

## 升级指南

### 从 1.1.x 升级到 1.2.0

```gradle
// 更新依赖版本
implementation 'io.github.706412584:orangeplayer:1.2.0'
```

**新API使用示例：**

```java
// 控制器可见性控制
videoView.setControllerVisibilityEnabled(false);  // 禁用控制器UI
videoView.setControllerVisibilityEnabled(true);   // 启用控制器UI

// 查询状态
boolean enabled = videoView.isControllerVisibilityEnabled();
```

### 从 1.0.x 升级到 1.1.x

```gradle
// 更新依赖版本
implementation 'io.github.706412584:orangeplayer:1.1.9'
```

**注意事项：**
- 1.1.x 版本移除了部分废弃的 API
- 建议查看 API 文档了解最新用法

---

## 贡献者

感谢所有为 OrangePlayer 做出贡献的开发者！

## 反馈与支持

- **GitHub Issues**: https://github.com/706412584/orangeplayer/issues
- **QQ**: 706412584
