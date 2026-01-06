# OrangePlayer 橘子播放器

基于 [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer) 的增强视频播放器库，提供丰富的播放功能和自定义控制组件。

[![](https://jitpack.io/v/706412584/orangeplayer.svg)](https://jitpack.io/#706412584/orangeplayer)
[![Android CI](https://github.com/706412584/orangeplayer/actions/workflows/android.yml/badge.svg)](https://github.com/706412584/orangeplayer/actions/workflows/android.yml)

📱 [下载 Demo APK](https://github.com/706412584/orangeplayer/releases/tag/demo)

## 截图预览

<p align="center">
  <img src="docs/screenshot_player.jpg" width="30%" />
  <img src="docs/screenshot_settings.jpg" width="30%" />
  <img src="docs/screenshot_speed.jpg" width="30%" />
</p>
<p align="center">
  <img src="docs/screenshot_subtitle.jpg" width="30%" />
  <img src="docs/screenshot_danmaku.jpg" width="30%" />
  <img src="docs/screenshot_ocr.jpg" width="30%" />
</p>

## 功能特性

| 功能 | 说明 |
|------|------|
| 🎬 多播放内核 | 系统/ExoPlayer/IJK/阿里云，运行时切换 |
| 📝 字幕系统 | SRT/ASS/VTT 格式，大小可调 |
| 🔤 OCR 识别 | Tesseract 硬字幕识别 + ML Kit 翻译 |
| 💬 弹幕功能 | 大小/速度/透明度可调，支持发送 |
| 🎛️ 倍速播放 | 0.35x - 10x，长按倍速 |
| ⏰ 定时关闭 | 30/60/90/120 分钟 |
| ⏭️ 跳过片头尾 | 0-300 秒可调 |
| 📺 投屏 | DLNA 投屏支持 |
| 🖼️ 画中画 | PiP 小窗模式 |
| 📸 截图 | 视频截图保存 |

---

## 快速开始

### 1. 添加仓库

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        // 阿里云播放器仓库（如需使用阿里云内核）
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

### 2. 添加依赖

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.1'
    
    // GSY 基础依赖（必需）
    implementation 'io.github.carguo:gsyvideoplayer-java:11.3.0'
    
    // ExoPlayer 模式（推荐）
    implementation 'io.github.carguo:gsyvideoplayer-exo2:11.3.0'
    
    // 阿里云播放器模式（可选，需要 License）
    implementation 'io.github.carguo:gsyvideoplayer-aliplay:11.3.0'
    
    // IJK 播放器 so 库（根据需要选择 CPU 架构）
    implementation 'io.github.carguo:gsyvideoplayer-arm64:11.3.0'   // arm64-v8a
    implementation 'io.github.carguo:gsyvideoplayer-armv7a:11.3.0'  // armeabi-v7a
    // implementation 'io.github.carguo:gsyvideoplayer-armv5:11.3.0'   // armeabi
    // implementation 'io.github.carguo:gsyvideoplayer-x86:11.3.0'     // x86
    // implementation 'io.github.carguo:gsyvideoplayer-x64:11.3.0'     // x86_64
}
```

#### 更多格式支持（可选）

如需支持 MPEG 编码、RTSP、concat、crypto 协议等，添加扩展 so 库：

```gradle
dependencies {
    // 扩展编码支持（支持 mpeg 编码和更多协议，支持 16k Page Size）
    // 注意：会增加包体积
    implementation 'io.github.carguo:gsyvideoplayer-ex_so:11.3.0'
}
```

> **说明**：普通版本支持 H.263/H.264/H.265 等常见编码，对于 MPEG 编码可能出现有声音无画面的情况。`ex_so` 扩展库补充了 MPEG 编码和更多协议支持。

### 3. AndroidManifest.xml 配置

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">
    
    <!-- 允许使用 minSdk 24 的投屏库 -->
    <uses-sdk tools:overrideLibrary="com.uaoanlao.tv" />
    
    <!-- 网络权限（必需）-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- 投屏需要的 WiFi 权限（可选，投屏功能需要）-->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    
    <application
        android:usesCleartextTraffic="true"
        ... >
        
        <!-- Activity 配置（支持横竖屏切换和画中画）-->
        <activity
            android:name=".YourActivity"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"
            android:supportsPictureInPicture="true"
            android:resizeableActivity="true">
        </activity>
    </application>
</manifest>
```

**关键配置说明：**

| 配置项 | 说明 |
|--------|------|
| `usesCleartextTraffic="true"` | 允许 HTTP 明文流量（播放 HTTP 视频源需要）|
| `configChanges` | 防止横竖屏切换时 Activity 重建 |
| `supportsPictureInPicture` | 启用画中画模式 |
| `resizeableActivity` | 允许调整窗口大小 |

### 4. 基本使用

```xml
<!-- 布局文件 -->
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

```java
// Activity 中
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerConstants;

OrangevideoView videoView = findViewById(R.id.video_player);
videoView.setUp("https://example.com/video.mp4", true, "视频标题");
videoView.startPlayLogic();
```

---

## 播放内核切换

OrangePlayer 支持 4 种播放内核，可在运行时动态切换。

### 内核对比

| 内核 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|
| 系统 (MediaPlayer) | 无需额外依赖，兼容性好 | 格式支持有限 | 普通 MP4 播放 |
| ExoPlayer | Google 官方，格式支持全 | 包体积较大 | 推荐默认使用 |
| IJK | 格式支持最全，软解能力强 | 包体积大 | 特殊格式视频 |
| 阿里云 | 性能好，支持私有协议 | 需要 License | 商业项目 |

### 切换方法

```java
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerConstants;

// 切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);

// 切换到 ExoPlayer（推荐）
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 切换到 IJK 播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);

// 切换到阿里云播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
```

### IJK 内核集成

IJK 内核需要额外添加 so 库依赖：

```gradle
dependencies {
    // IJK 播放器 so 库（按需添加对应 CPU 架构）
    implementation 'io.github.carguo:gsyvideoplayer-arm64:11.3.0'   // arm64-v8a（推荐）
    implementation 'io.github.carguo:gsyvideoplayer-armv7a:11.3.0'  // armeabi-v7a
    implementation 'io.github.carguo:gsyvideoplayer-armv5:11.3.0'   // armeabi（旧设备）
    implementation 'io.github.carguo:gsyvideoplayer-x86:11.3.0'     // x86 模拟器
    implementation 'io.github.carguo:gsyvideoplayer-x64:11.3.0'     // x86_64 模拟器
    
    // 如需更多编码格式支持（mpeg、rtsp、concat、crypto 协议）
    implementation 'io.github.carguo:gsyvideoplayer-ex_so:11.3.0'
}
```

---

## ⚠️ 阿里云内核注意事项

### License 问题

阿里云播放器从 **5.4.0 版本开始需要 License 授权**，否则会出现：
- 播放黑屏
- 水印覆盖
- 功能受限

### 解决方案

**方案一：使用免授权版本（推荐测试使用）**

本库默认使用 5.4.7.1 版本，需要在阿里云控制台申请免费 License。

**方案二：申请 License**

1. 登录 [阿里云视频点播控制台](https://vod.console.aliyun.com/)
2. 创建应用获取 License
3. 在 Application 中初始化：

```java
import com.aliyun.player.AliPlayerFactory;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化阿里云播放器 License
        AliPlayerFactory.setLicenseKey("your_license_key");
    }
}
```

**方案三：使用旧版本（5.3.0 免授权）**

```gradle
// 排除默认的阿里云 SDK
implementation ('com.github.706412584:orangeplayer:v1.0.1') {
    exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
}

// 使用 5.3.0 免授权版本
implementation 'com.aliyun.sdk.android:AliyunPlayer:5.3.0-full'
```

### 阿里云仓库配置

```gradle
// settings.gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/releases' }
    maven { url 'https://maven.aliyun.com/repository/public' }
}
```

---

## 投屏功能集成

投屏功能使用 DLNA 协议，需要额外添加依赖。

### 添加依赖

```gradle
dependencies {
    // DLNA 投屏库
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    
    // 投屏库依赖
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okio:okio:3.6.0'
}
```

### 使用方法

```java
import com.orange.playerlibrary.cast.DLNACastManager;

// 检查投屏是否可用
if (DLNACastManager.isDLNAAvailable()) {
    // 开始投屏（会弹出设备选择界面）
    DLNACastManager.getInstance().startCast(
        activity,           // Activity
        videoUrl,           // 视频地址
        "视频标题"          // 标题
    );
}

// 监听投屏状态
DLNACastManager.getInstance().setOnCastStateListener(new DLNACastManager.OnCastStateListener() {
    @Override
    public void onCastStarted() {
        // 投屏开始
    }
    
    @Override
    public void onCastStopped() {
        // 投屏停止
    }
    
    @Override
    public void onCastError(String message) {
        // 投屏错误
    }
});
```

---

## OCR 字幕翻译

### 语言包

语言包文件位于 `tessdata_packs/` 目录，复制到 `assets/tessdata/` 或使用应用内下载。

| 语言 | 文件 | 大小 |
|------|------|------|
| 简体中文 | chi_sim.traineddata | 2.35 MB |
| 繁体中文 | chi_tra.traineddata | 2.26 MB |
| 英语 | eng.traineddata | 3.92 MB |
| 日语 | jpn.traineddata | 2.36 MB |
| 韩语 | kor.traineddata | 1.60 MB |

### 使用方法

```java
import com.orange.playerlibrary.ocr.LanguagePackManager;

// 检查语言包是否已安装
LanguagePackManager manager = new LanguagePackManager(context);
if (manager.isLanguageInstalled("chi_sim")) {
    // 已安装简体中文
}

// 下载语言包
manager.downloadLanguage("eng", new LanguagePackManager.DownloadCallback() {
    @Override
    public void onProgress(int progress, long downloaded, long total) {
        // 下载进度
    }
    
    @Override
    public void onSuccess() {
        // 下载成功
    }
    
    @Override
    public void onError(String error) {
        // 下载失败
    }
});
```

---

## 完整依赖配置示例

```gradle
// app/build.gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:1.0.3'
    
    // === GSY 基础依赖（必需）===
    implementation 'io.github.carguo:gsyvideoplayer-java:11.3.0'
    
    // === 播放内核（按需选择）===
    
    // ExoPlayer 模式（推荐）
    implementation 'io.github.carguo:gsyvideoplayer-exo2:11.3.0'
    
    // 阿里云播放器模式（需要 License）
    implementation 'io.github.carguo:gsyvideoplayer-aliplay:11.3.0'
    
    // IJK 播放器 so 库（根据目标设备选择）
    implementation 'io.github.carguo:gsyvideoplayer-arm64:11.3.0'
    implementation 'io.github.carguo:gsyvideoplayer-armv7a:11.3.0'
    
    // 扩展编码支持（mpeg、rtsp、concat、crypto 协议）
    // implementation 'io.github.carguo:gsyvideoplayer-ex_so:11.3.0'
    
    // === 可选功能 ===
    
    // DLNA 投屏
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

---

## API 文档

详细 API 请查看 [docs/API.md](docs/API.md)

## 项目结构

详细结构请查看 [docs/STRUCTURE.md](docs/STRUCTURE.md)

---

## 混淆配置

```proguard
# GSYVideoPlayer
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }

# OrangePlayer
-keep class com.orange.playerlibrary.** { *; }

# Tesseract OCR
-keep class com.googlecode.tesseract.android.** { *; }

# 阿里云播放器
-keep class com.aliyun.player.** { *; }
-keep class com.cicada.player.** { *; }

# DLNA 投屏
-keep class com.uaoanlao.tv.** { *; }
```

---

## License

Apache License 2.0

---

## 作者

**QQ: 706412584**

如有问题或建议，欢迎联系交流。

## 致谢

- [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer)
- [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android)
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
- [UaoanDLNA](https://github.com/AnyListen/UaoanDLNA)
