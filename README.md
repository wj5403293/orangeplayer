# OrangePlayer 橘子播放器

<p align="center">
  <strong>功能完整的 Android 视频播放器 SDK</strong>
</p>

<p align="center">
  基于 <a href="https://github.com/CarGuo/GSYVideoPlayer">GSYVideoPlayer</a> 的增强视频播放器库
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.706412584/orangeplayer"><img src="https://img.shields.io/maven-central/v/io.github.706412584/orangeplayer.svg" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://android-arsenal.com/api?level=14"><img src="https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat" alt="API"></a>
</p>

## 功能特性

- 🎬 **多播放内核**：系统/ExoPlayer/IJK/阿里云，运行时切换
- 📝 **字幕系统**：SRT/ASS/VTT 格式支持
- 🔍 **视频嗅探**：自动检测网页中的视频资源
- 🔤 **OCR 识别**：硬字幕识别 + ML Kit 翻译
- 🎤 **语音识别**：Vosk 离线语音识别，实时字幕生成
- 💬 **弹幕功能**：大小/速度/透明度可调
- 🎛️ **倍速播放**：0.35x - 10x，长按倍速
- 📺 **投屏支持**：DLNA 投屏
- 🖼️ **画中画**：PiP 小窗模式
- � **多平台**：手机、平板、Android TV 全平台支持

---

## 快速开始

### 系统要求

- **Android 4.0+ (API 14+)** - 从 v1.1.0+ 开始支持 Android 4.0 及以上版本
- **Android 5.0+ (API 21+)** - 推荐使用，支持所有功能（包括 ExoPlayer 和 AI 功能）

### 添加依赖

**⚠️ 重要更新：我们已从 JitPack 迁移到 Maven Central**

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    // OrangePlayer 核心库（Maven Central）- 请使用最新版本
    implementation 'io.github.706412584:orangeplayer:+'

    //以下均为可选功能
    // 播放器内核（默认ijk,需要引入ijk架构支持gsyVideoPlayer-ex_so，全架构或者指定架构）
    implementation 'io.github.706412584:gsyVideoPlayer-java:+'    // IJK 播放器
    implementation 'io.github.706412584:gsyVideoPlayer-ex_so:+'
    //EXOPlayer播放器支持
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:+' // ExoPlayer
    // 阿里云播放器支持（排除内置版本，避免授权问题）- 请使用最新版本
    implementation('io.github.706412584:gsyVideoPlayer-aliplay:+') {
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    // 使用 5.4.7.1 版本（无需授权）
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
    // 可选依赖（按需添加）
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'  // 弹幕功能
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'  // OCR 识别
    implementation 'com.google.mlkit:translate:17.0.2'  // ML Kit 翻译
    implementation 'com.alphacephei:vosk-android:0.3.47'  // 语音识别
    implementation 'androidx.media3:media3-decoder-ffmpeg:1.5.0'  // FFmpeg 解码器(可选)
}
```

> 📦 **可用的播放器内核组件**（所有组件请使用最新版本）：
> - `gsyVideoPlayer-java` - IJK 播放器（推荐，支持更多格式）
> - `gsyVideoPlayer-exo_player2` - ExoPlayer（性能好，RTSP 支持完整）
> - `gsyVideoPlayer-aliplay` - 阿里云播放器（商业级，RTMP 延迟低）
> - `gsyVideoPlayer-base` - 播放器基础库
> - `gsyVideoPlayer-proxy_cache` - 代理缓存支持
> - `gsyVideoPlayer-armv7a` - ARMv7a 架构 so 库
> - `gsyVideoPlayer-armv64` - ARM64 架构 so 库
> - `gsyVideoPlayer-x86` - x86 架构 so 库
> - `gsyVideoPlayer-x86_64` - x86_64 架构 so 库
> - `gsyVideoPlayer-ex_so` - IJK 加密支持 so 库（全架构，支持 HLS AES-128 等加密视频）
>
> ⚠️ **重要提示**：
> - `gsyVideoPlayer-ex_so` 和标准 so 库（armv7a/armv64/x86/x86_64）**不能同时使用**，会导致 SO 库冲突
> - 如需播放加密视频，使用 `gsyVideoPlayer-ex_so` 替代标准 so 库
> - `ex_so` 包含所有架构，体积较大（约增加 20-30MB）
>
> 💡 **版本说明**：
> - 使用 `+` 可自动获取最新版本
> - 或访问 [Maven Central](https://central.sonatype.com/artifact/io.github.706412584/orangeplayer) 查看最新版本号
> - 当前最新版本：[![Maven Central](https://img.shields.io/maven-central/v/io.github.706412584/orangeplayer.svg)](https://central.sonatype.com/artifact/io.github.706412584/orangeplayer)


> - **可选功能**：弹幕、OCR、语音识别、FFmpeg 解码器等按需添加
> - 完整的依赖配置请查看 [安装指南](docs/INSTALLATION.md)

###

### 基本使用

#### 1. 配置 AndroidManifest.xml

在使用播放器的 Activity 中添加以下配置（**必需**）：

```xml
<activity
    android:name=".YourActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"
    android:screenOrientation="portrait"
    android:supportsPictureInPicture="true"
    android:resizeableActivity="true">
</activity>
```

**配置说明：**
- `configChanges` - 防止横竖屏切换时 Activity 重建（**必需**）
- `screenOrientation` - 设置屏幕方向（portrait/landscape/unspecified）
- `supportsPictureInPicture` - 启用画中画模式（可选）
- `resizeableActivity` - 允许调整窗口大小（可选）

#### 2. 布局文件

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

#### 3. Activity 代码

```java
OrangevideoView videoView = findViewById(R.id.video_player);
videoView.setUp("https://example.com/video.mp4", true, "视频标题");
videoView.startPlayLogic();
```

### 智能全屏功能

点击全屏按钮时，播放器会根据视频宽高比自动选择最佳全屏模式：
- **横屏视频**（宽 > 高）→ 自动进入横屏全屏
- **竖屏视频**（高 > 宽）→ 自动进入竖屏全屏

```java
// 智能全屏默认开启，无需额外配置
// 用户点击全屏按钮时会自动根据视频比例选择全屏模式

// 如需手动控制：
videoView.setSmartFullscreenEnabled(true);   // 启用智能全屏（默认）
videoView.setSmartFullscreenEnabled(false);  // 禁用智能全屏（使用传统横屏全屏）

// 查询状态
boolean isEnabled = videoView.isSmartFullscreenEnabled();
```

**智能全屏特性：**
- ✅ 点击全屏按钮时自动检测视频宽高比
- ✅ 默认开启，可手动关闭
- ✅ 禁用后使用传统横屏全屏
- ✅ 支持横屏/竖屏两种全屏模式
- ✅ 视频尺寸无效时自动降级到横屏全屏

### 视频嗅探功能

自动检测网页中的视频资源，支持 MP4、M3U8、FLV 等格式。

```java
import com.orange.playerlibrary.VideoSniffing;

// 开始嗅探
VideoSniffing.startSniffing(context, "https://example.com/page", new VideoSniffing.Call() {
    @Override
    public void received(String contentType, HashMap<String, String> headers, 
                        String title, String url) {
        // 发现视频资源
        Log.d(TAG, "发现视频: " + url);
        Log.d(TAG, "类型: " + contentType);
        Log.d(TAG, "标题: " + title);
    }
    
    @Override
    public void onFinish(List<VideoSniffing.VideoInfo> videoList, int videoSize) {
        // 嗅探完成
        Log.d(TAG, "共发现 " + videoSize + " 个视频");
        
        // 播放第一个视频
        if (videoSize > 0) {
            VideoSniffing.VideoInfo video = videoList.get(0);
            videoView.setUp(video.url, true, video.title);
            videoView.startPlayLogic();
        }
    }
});

// 停止嗅探
VideoSniffing.stop(true);
```

**嗅探功能特性：**
- ✅ 自动检测视频资源（MP4、M3U8、FLV、AVI、MOV、MKV）
- ✅ 支持自定义请求头
- ✅ 智能过滤非视频资源（图片、CSS、JS、广告等）
- ✅ 并发控制，避免 ANR
- ✅ URL 去重，避免重复检测
- ✅ 调试模式，方便排查问题
- ✅ 自动播放第一个视频（可选）

**嗅探自动播放设置：**

```java
// 启用嗅探自动播放（默认关闭）
PlayerSettingsManager.getInstance(context).setSniffingAutoPlayEnabled(true);

// 启用后，嗅探完成时会自动播放第一个视频并隐藏嗅探组件
videoView.startSniffing();

// 查询状态
boolean autoPlay = PlayerSettingsManager.getInstance(context).isSniffingAutoPlayEnabled();
```

**带自定义请求头的嗅探：**

```java
// 添加自定义请求头（如 User-Agent、Referer 等）
Map<String, String> headers = new HashMap<>();
headers.put("User-Agent", "Mozilla/5.0 ...");
headers.put("Referer", "https://example.com");

VideoSniffing.startSniffing(context, url, headers, callback);
```

**调试模式：**

```java
// 启用调试日志（查看嗅探过程）
VideoSniffing.isDebug = true;

// 查看 logcat 日志（标签：VideoSniffing）
// 会输出：检查 URL、响应码、Content-Type、视频发现等信息
```

---

## 更多功能

查看完整文档了解更多功能：

- [更新日志](docs/CHANGELOG.md) - 版本更新历史
- [播放内核切换](docs/PLAYER_ENGINES.md) - 系统/ExoPlayer/IJK/阿里云
- [OCR 字幕翻译](docs/OCR_GUIDE.md) - 硬字幕识别与翻译
- [语音识别字幕](docs/SPEECH_RECOGNITION.md) - 实时语音转字幕
- [投屏功能](docs/CAST_GUIDE.md) - DLNA 投屏配置
- [Android TV 适配](docs/TV_QUICK_START.md) - TV 平台快速集成
- [API 文档](docs/API.md) - 完整的 API 参考
- [常见问题](docs/FAQ.md) - 问题排查和解决方案

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

# ML Kit Translation
-keep class com.google.mlkit.** { *; }

# Vosk 语音识别
-keep class org.vosk.** { *; }

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
