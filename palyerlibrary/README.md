# Orange Player Library

一个功能强大的 Android 视频播放器库，支持多内核切换、无缝全屏切换、智能内核选择等特性。

## 目录

- [播放器架构图](#播放器架构图)
- [内核类型常量](#内核类型常量)
- [公开 API 列表](#公开-api-列表)
  - [播放器内核设置与获取](#播放器内核设置与获取)
  - [PlayerSettingsManager 设置管理器](#playersettingsmanager-设置管理器)
  - [播放器管理器类](#播放器管理器类)
  - [播放器内核对象类型](#播放器内核对象类型)
- [使用示例](#使用示例)
  - [获取播放器内核对象](#1-获取播放器内核对象)
  - [获取播放器管理器](#2-获取播放器管理器)
  - [切换播放器内核](#3-切换播放器内核)
  - [检查内核可用性](#4-检查内核可用性)
  - [监听内核变更](#5-监听内核变更)
- [默认内核选择逻辑](#默认内核选择逻辑)
- [内核检测机制](#内核检测机制)
- [依赖配置](#依赖配置)
- [文件结构](#文件结构)

## 播放器架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OrangevideoView (主入口)                           │
│                     d:\android\projecet_iade\orangeplayer\                   │
│                   palyerlibrary\src\main\java\com\orange\                    │
│                          playerlibrary\OrangevideoView.java                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  PlayerSettingsManager │  │  VideoEventManager   │  │ OrangeVideoController│
│   (设置管理器)          │  │   (事件管理器)        │  │    (控制器)           │
│                      │  │                      │  │                      │
│ - getPlayerEngine()  │  │ - selectEngine()     │  │ - UI控制              │
│ - setPlayerEngine()  │  │ - setupEngineButtons()│  │ - 手势处理            │
│ - isAutoSelectEngine()│  │ - updateEngineButtonsUI│  │ - 进度条              │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PlayerFactory (播放器工厂)                          │
│                        GSYVideoPlayer 框架提供                                │
└─────────────────────────────────────────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┬───────────────┐
    │               │               │               │
    ▼               ▼               ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│ExoPlayer   │ │IJKPlayer   │ │系统播放器   │ │阿里云播放器 │
│Manager     │ │Manager     │ │Manager     │ │Manager     │
│            │ │            │ │            │ │            │
│OrangeExo   │ │OrangeIjk   │ │OrangeSystem│ │AliPlayer   │
│PlayerManager│ │PlayerManager│ │PlayerManager│ │Manager    │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
    │               │               │               │
    ▼               ▼               ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│IjkExo2     │ │IjkMedia    │ │MediaPlayer │ │AliPlayer   │
│MediaPlayer │ │Player      │ │(Android)   │ │(阿里云SDK) │
│(ExoPlayer) │ │(IJK SDK)   │ │            │ │            │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
```

## 内核类型常量

| 常量 | 值 | 说明 |
|------|-----|------|
| `PlayerConstants.ENGINE_EXO` | `"exo"` | ExoPlayer 内核 |
| `PlayerConstants.ENGINE_IJK` | `"ijk"` | IJKPlayer 内核 |
| `PlayerConstants.ENGINE_DEFAULT` | `"default"` | 系统播放器内核 |
| `PlayerConstants.ENGINE_ALI` | `"ali"` | 阿里云播放器内核 |

## 公开 API 列表

### 播放器内核设置与获取

| 方法 | 参数 | 返回值 | 说明 | 源码位置 |
|------|------|--------|------|----------|
| `getPlayerManager(Class<T>)` | 管理器类型 | T | 泛型获取播放器管理器 | OrangevideoView.java:1912 |
| `getPlayerManager()` | - | IPlayerManager | 获取原始管理器接口 | OrangevideoView.java:1930 |
| `getMediaPlayer(Class<T>)` | 播放器类型 | T | 泛型获取内核对象 | OrangevideoView.java:1959 |
| `getMediaPlayer()` | - | IMediaPlayer | 获取原始内核接口 | OrangevideoView.java:1979 |
| `getCurrentEngineType()` | - | String | 获取当前内核类型 | OrangevideoView.java:1996 |
| `isExoPlayerEngine()` | - | boolean | 是否 ExoPlayer 内核 | OrangevideoView.java:2003 |
| `isIjkPlayerEngine()` | - | boolean | 是否 IJK 内核 | OrangevideoView.java:2010 |
| `isSystemPlayerEngine()` | - | boolean | 是否系统播放器内核 | OrangevideoView.java:2017 |
| `isAliPlayerEngine()` | - | boolean | 是否阿里云内核 | OrangevideoView.java:2024 |
| `isEngineAvailable(String)` | 内核类型 | boolean | 检查内核是否可用 | OrangevideoView.java:2034 |
| `selectPlayerFactory(String)` | 内核类型 | void | 切换播放器内核 | OrangevideoView.java:1793 |

### PlayerSettingsManager 设置管理器

| 方法 | 参数 | 返回值 | 说明 | 源码位置 |
|------|------|--------|------|----------|
| `setPlayerEngine(String)` | 内核类型 | void | 设置播放器内核 | PlayerSettingsManager.java:86 |
| `getPlayerEngine()` | - | String | 获取当前内核设置 | PlayerSettingsManager.java:107 |
| `hasUserSetEngine()` | - | boolean | 用户是否手动设置过 | PlayerSettingsManager.java:132 |
| `setEngineChangeListener(EngineChangeListener)` | 监听器 | void | 设置变更监听 | PlayerSettingsManager.java:64 |
| `setAutoSelectEngine(boolean)` | 是否启用 | void | 设置自动选择内核 | PlayerSettingsManager.java:334 |
| `isAutoSelectEngine()` | - | boolean | 是否启用自动选择 | PlayerSettingsManager.java:343 |

### 播放器管理器类

| 类名 | 内核类型 | 源码位置 |
|------|----------|----------|
| `OrangeExoPlayerManager` | ExoPlayer | palyerlibrary/src/main/java/com/orange/playerlibrary/exo/OrangeExoPlayerManager.java |
| `OrangeIjkPlayerManager` | IJK | palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeIjkPlayerManager.java |
| `OrangeSystemPlayerManager` | 系统 | palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeSystemPlayerManager.java |

### 播放器内核对象类型

| 内核类型 | 管理器类 | 内核对象类 |
|----------|----------|------------|
| ExoPlayer | `OrangeExoPlayerManager` | `tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer` |
| IJK | `OrangeIjkPlayerManager` | `tv.danmaku.ijk.media.player.IjkMediaPlayer` |
| 系统 | `OrangeSystemPlayerManager` | `android.media.MediaPlayer` |
| 阿里云 | `AliPlayerManager` | `com.aliyun.player.AliPlayer` |

## 使用示例

### 1. 获取播放器内核对象

```java
// 获取 ExoPlayer 内核对象
tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer exoPlayer = 
    videoView.getMediaPlayer(tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer.class);
if (exoPlayer != null) {
    // 使用 ExoPlayer 特有功能
}

// 获取 IJK 内核对象
tv.danmaku.ijk.media.player.IjkMediaPlayer ijkPlayer = 
    videoView.getMediaPlayer(tv.danmaku.ijk.media.player.IjkMediaPlayer.class);
if (ijkPlayer != null) {
    // 使用 IJK 特有功能
}

// 获取系统播放器内核对象
android.media.MediaPlayer systemPlayer = 
    videoView.getMediaPlayer(android.media.MediaPlayer.class);
if (systemPlayer != null) {
    // 使用系统播放器特有功能
}
```

### 2. 获取播放器管理器

```java
// 获取 ExoPlayer 管理器
OrangeExoPlayerManager exoManager = 
    videoView.getPlayerManager(OrangeExoPlayerManager.class);
if (exoManager != null) {
    // 使用 ExoPlayer 管理器特有功能
}

// 获取 IJK 管理器
OrangeIjkPlayerManager ijkManager = 
    videoView.getPlayerManager(OrangeIjkPlayerManager.class);
```

### 3. 切换播放器内核

```java
// 方式1：通过设置管理器（推荐，会持久化）
PlayerSettingsManager.getInstance(context).setPlayerEngine(PlayerConstants.ENGINE_EXO);

// 方式2：直接切换（不持久化）
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
```

### 4. 检查内核可用性

```java
// 检查 ExoPlayer 是否可用
if (videoView.isEngineAvailable(PlayerConstants.ENGINE_EXO)) {
    // ExoPlayer 可用
}

// 检查 IJK 是否可用（包括 SO 库）
if (videoView.isEngineAvailable(PlayerConstants.ENGINE_IJK)) {
    // IJK 可用
}
```

### 5. 监听内核变更

```java
PlayerSettingsManager.getInstance(context).setEngineChangeListener(newEngine -> {
    // 内核已变更，更新 UI
    updateEngineUI(newEngine);
});
```

## 默认内核选择逻辑

当用户未手动设置内核时，系统会智能选择默认内核：

```
优先级：
1. 用户手动设置的内核（最高优先级）
2. 智能检测可用内核：
   - ExoPlayer 可用 → 使用 ExoPlayer
   - ExoPlayer 不可用 → 使用系统播放器
```

**注意**：默认不再使用 IJK，因为很多项目可能不包含 IJK 依赖。

## 内核检测机制

| 内核 | 检测方式 |
|------|----------|
| ExoPlayer | 检测 `tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer` 或 `androidx.media3.exoplayer.ExoPlayer` |
| IJK | 检测 Java 类 + SO 库加载状态 |
| 系统 | 始终可用 |
| 阿里云 | 检测 `com.aliyun.player.AliPlayer` |

## 依赖配置

```gradle
// app/build.gradle
dependencies {
    // 播放器核心库
    implementation project(':palyerlibrary')
    
    // ExoPlayer 内核（可选）
    implementation project(':gsyVideoPlayer-exo_player2')
    
    // IJK 内核（可选，需要 SO 库）
    implementation project(':gsyVideoPlayer-armv64')  // ARM64
    implementation project(':gsyVideoPlayer-armv7a') // ARMv7
    implementation project(':gsyVideoPlayer-x86')    // x86
    implementation project(':gsyVideoPlayer-x86_64') // x86_64
}
```

## 文件结构

```
palyerlibrary/
├── src/main/java/com/orange/playerlibrary/
│   ├── OrangevideoView.java          # 主入口，播放器视图
│   ├── PlayerSettingsManager.java    # 设置管理器
│   ├── VideoEventManager.java        # 事件管理器
│   ├── OrangeVideoController.java    # 控制器
│   ├── PlayerConstants.java          # 常量定义
│   ├── exo/
│   │   └── OrangeExoPlayerManager.java  # ExoPlayer 管理器
│   ├── player/
│   │   ├── OrangeIjkPlayerManager.java  # IJK 管理器
│   │   └── OrangeSystemPlayerManager.java # 系统播放器管理器
│   └── utils/
│       └── PlayerEngineSelector.java    # 内核智能选择器
└── src/main/res/
    └── layout/
        └── setup_dialog.xml          # 设置对话框布局
```
