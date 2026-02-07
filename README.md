# OrangePlayer 橘子播放器

> **For Developers**: Professional Android video player library for building video applications.  
> Open source under Apache 2.0 License.


<p align="center">
  <strong>功能完整的 Android 视频播放器 SDK</strong>
</p>

<p align="center">
  基于 <a href="https://github.com/CarGuo/GSYVideoPlayer">GSYVideoPlayer</a> 的增强视频播放器库，为 Android 开发者提供完整的视频播放解决方案
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.706412584/orangeplayer"><img src="https://img.shields.io/maven-central/v/io.github.706412584/orangeplayer.svg" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://android-arsenal.com/api?level=21"><img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat" alt="API"></a>
  <a href="https://github.com/706412584/orangeplayer/stargazers"><img src="https://img.shields.io/github/stars/706412584/orangeplayer.svg?style=social&label=Star" alt="GitHub stars"></a>
</p>

<p align="center">
  📱 <a href="https://github.com/706412584/orangeplayer/releases/tag/demo">下载 Demo APK</a> | 
  📖 <a href="docs/INSTALLATION.md">快速开始</a> | 
  📚 <a href="docs/API.md">API 文档</a> | 
  ❓ <a href="docs/FAQ.md">常见问题</a>
</p>

---

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
| 🎤 语音识别 | Vosk 离线语音识别，实时字幕生成 |
| 💬 弹幕功能 | 大小/速度/透明度可调，支持发送 |
| 🎛️ 倍速播放 | 0.35x - 10x，长按倍速 |
| ⏰ 定时关闭 | 30/60/90/120 分钟 |
| ⏭️ 跳过片头尾 | 0-300 秒可调 |
| 📺 投屏 | DLNA 投屏支持 |
| 🖼️ 画中画 | PiP 小窗模式 |
| 📸 截图 | 视频截图保存 |
| 📺 Android TV | 完整的 TV 适配，遥控器支持 |

---

## 📚 文档导航

- **快速开始**
  - [安装指南](docs/INSTALLATION.md) - 完整的依赖配置和环境设置
  - [基本使用](#基本使用) - 最简单的集成示例
  - [完整示例](#完整使用示例) - 15 个实用代码示例

- **功能指南**
  - [播放内核切换](docs/PLAYER_ENGINES.md) - 系统/ExoPlayer/IJK/阿里云
  - [OCR 字幕翻译](docs/OCR_GUIDE.md) - 硬字幕识别与翻译
  - [语音识别字幕](docs/SPEECH_RECOGNITION.md) - 实时语音转字幕
  - [投屏功能](docs/CAST_GUIDE.md) - DLNA 投屏配置
  - [Android TV 适配](docs/TV_QUICK_START.md) - TV 平台快速集成

- **开发文档**
  - [API 文档](docs/API.md) - 完整的 API 参考
  - [项目结构](docs/STRUCTURE.md) - 代码组织说明

- **其他**
  - [常见问题](docs/FAQ.md) - 问题排查和解决方案
  - [更新日志](CHANGELOG.md) - 版本更新记录

---

## 快速开始

### 系统要求

- **Android 4.0+ (API 14+)** - 从 v1.1.0 开始支持 Android 4.0 及以上版本
- **Android 5.0+ (API 21+)** - 推荐使用，支持所有功能（包括 ExoPlayer 和 AI 功能）

### 1. 添加依赖

**⚠️ 重要更新：我们已从 JitPack 迁移到 Maven Central**

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    // OrangePlayer 核心库（Maven Central）
    implementation 'io.github.706412584:orangeplayer:1.1.0'
    
    // 必需依赖
    implementation 'com.github.bumptech.glide:glide:4.16.0'  // 图片加载
    
    // 播放器内核（至少选择一个）
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'      // IJK 播放器（推荐）
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0' // ExoPlayer
    
    // 可选依赖（按需添加）
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'  // 弹幕功能
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'  // OCR 识别
    implementation 'com.google.mlkit:translate:17.0.2'  // ML Kit 翻译
    implementation 'com.alphacephei:vosk-android:0.3.47'  // 语音识别
    implementation 'androidx.media3:media3-decoder-ffmpeg:1.5.0'  // FFmpeg 解码器(可选)
}
```

**阿里云播放器（可选）：**

```gradle
dependencies {
    // 阿里云播放器支持（排除内置版本，避免授权问题）
    implementation('io.github.706412584:gsyVideoPlayer-aliplay:1.1.0') {
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    
    // 使用 5.4.7.1 版本（无需授权）
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

需要在项目根目录的 `build.gradle` 中添加阿里云 Maven 仓库：

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

> 💡 **依赖说明**：
> - **必需**：`orangeplayer` + `glide` + 至少一个播放器内核
> - **播放器内核**：IJK（推荐，支持更多格式）、ExoPlayer（性能好）、系统播放器（无需额外依赖）、阿里云（商业级）
> - **可选功能**：弹幕、OCR、语音识别、FFmpeg 解码器等按需添加
> - 完整的依赖配置请查看 [安装指南](docs/INSTALLATION.md)

### 2. 布局文件

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

### 3. 基本使用

```java
import com.orange.playerlibrary.OrangevideoView;

public class MainActivity extends AppCompatActivity {
    private OrangevideoView mVideoView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mVideoView = findViewById(R.id.video_player);
        
        // 设置视频地址和标题
        mVideoView.setUp("https://example.com/video.mp4", true, "示例视频");
        
        // 开始播放
        mVideoView.startPlayLogic();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.onVideoPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onVideoResume();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }
    
    @Override
    public void onBackPressed() {
        if (mVideoView.isFullScreen()) {
            mVideoView.exitFullScreen();
            return;
        }
        super.onBackPressed();
    }
}
```

就这么简单！OrangePlayer 会自动创建和配置所有 UI 组件。

---

## 完整使用示例

查看 [完整示例代码](docs/EXAMPLES.md)，包含 15 个实用场景：

1. [基础播放器](docs/EXAMPLES.md#示例-1基础播放器) - 最简单的实现
2. [带自定义请求头](docs/EXAMPLES.md#示例-2带自定义请求头的播放)
3. [播放状态监听](docs/EXAMPLES.md#示例-3播放状态监听)
4. [播放进度监听](docs/EXAMPLES.md#示例-4播放进度监听)
5. [播放完成监听](docs/EXAMPLES.md#示例-5播放完成监听)
6. [倍速播放](docs/EXAMPLES.md#示例-6倍速播放)
7. [字幕加载](docs/EXAMPLES.md#示例-7字幕加载)
8. [弹幕功能](docs/EXAMPLES.md#示例-8弹幕功能)
9. [播放列表](docs/EXAMPLES.md#示例-9播放列表)
10. [画中画模式](docs/EXAMPLES.md#示例-10画中画模式)
11. [投屏功能](docs/EXAMPLES.md#示例-11投屏功能)
12. [OCR 字幕识别](docs/EXAMPLES.md#示例-12ocr-字幕识别)
13. [语音识别字幕](docs/EXAMPLES.md#示例-13语音识别字幕)
14. [播放器设置](docs/EXAMPLES.md#示例-14播放器设置)
15. [错误处理](docs/EXAMPLES.md#示例-15错误处理)

---

## 播放内核切换

OrangePlayer 支持 4 种播放内核，并提供**智能自动选择**功能。

### 自动内核选择（可选）⭐

**从 v1.0.8 开始，OrangePlayer 提供智能内核选择功能，可根据视频 URL 协议自动选择最合适的播放器内核。**

**启用自动选择：**

```java
// 在 Application 或 Activity 中启用
PlayerSettingsManager.getInstance(context).setAutoSelectEngine(true);

// 之后直接使用 setUp，播放器会自动选择最合适的内核
videoView.setUp("rtsp://192.168.1.6:8554/live", true, "RTSP 直播");
videoView.startPlayLogic();

// RTSP → 自动使用 ExoPlayer
// RTMP → 自动使用阿里云（延迟低）
// HLS  → 自动使用阿里云（性能好）
// HTTP → 自动使用 ExoPlayer
```

**自动选择规则：**
- **RTSP 协议** → ExoPlayer（阿里云不支持）
- **RTMP 协议** → 阿里云（延迟极低 1-3秒）⭐
- **HLS (m3u8)** → 阿里云（商业级优化）
- **HTTP/HTTPS** → ExoPlayer（性能好）
- **本地文件** → ExoPlayer

**智能特性：**
- ✅ 自动检测依赖是否已导入
- ✅ 只在需要时才切换内核（避免不必要的切换）
- ✅ 如果推荐的内核不可用，继续使用当前内核
- ✅ 默认禁用，需要手动启用

**禁用自动选择：**

```java
// 禁用后恢复手动选择模式
PlayerSettingsManager.getInstance(context).setAutoSelectEngine(false);
```

### 手动切换内核（默认方式）

如果不启用自动选择，或需要手动指定内核，可以在 `setUp` 之前调用：

```java
// 手动切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 手动切换到 IJK 播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);

// 手动切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);

// 手动切换到阿里云播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
```

### 使用智能选择器工具类（高级用法）

如果需要在 `setUp` 之外使用智能选择功能：

```java
import com.orange.playerlibrary.utils.PlayerEngineSelector;

// 获取推荐的内核
String url = "rtsp://192.168.1.6:8554/live";
int engine = PlayerEngineSelector.selectEngine(url);
videoView.selectPlayerFactory(engine);

// 打印内核支持情况（调试用）
PlayerEngineSelector.printEngineSupportInfo(url);

// 检查某个内核是否支持该 URL
boolean supported = PlayerEngineSelector.isEngineSupported(url, PlayerConstants.ENGINE_ALI);
```

详细对比和配置请查看 [播放内核指南](docs/PLAYER_ENGINES.md)。

---

## 可选功能依赖

OrangePlayer 采用模块化设计，所有高级功能都是可选的，按需添加即可。

### 弹幕功能

```gradle
dependencies {
    implementation 'com.github.bilibili:DanmakuFlameMaster:0.9.25'
}
```

使用方法：

```java
// 启用弹幕
videoView.setDanmakuEnabled(true);

// 发送弹幕
videoView.sendDanmaku("弹幕内容", textSize, textColor);
```

### OCR 字幕翻译

识别视频画面中的硬字幕并翻译。

```gradle
dependencies {
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

详细配置请查看 [OCR 功能指南](docs/OCR_GUIDE.md)。

### 语音识别字幕

实时识别视频音频并生成字幕（需要 Android 10+）。

```gradle
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

详细配置请查看 [语音识别指南](docs/SPEECH_RECOGNITION.md)。

### FFmpeg 解码器（可选）

增强的音视频解码支持，处理更多编码格式。

```gradle
dependencies {
    // 方案 1：Google 官方 Media3 FFmpeg 解码器（推荐）
    implementation 'androidx.media3:media3-decoder-ffmpeg:1.5.0'
    
    // 方案 2：Jellyfin 定制版 FFmpeg 解码器
    implementation 'org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1'
}
```

> ⚠️ **重要说明**：两个方案**二选一**即可，不要同时添加。

**FFmpeg 解码器的作用：**

`media3-decoder-ffmpeg` 和 `media3-ffmpeg-decoder` 都是**音视频解码器**（Decoder），不是协议处理器或解封装器。

**它能做什么：**
- ✅ **解码音视频编码格式**：
  - 视频：H.264, H.265/HEVC, VP8, VP9, AV1, MPEG-2, MPEG-4 等
  - 音频：AAC, MP3, Opus, Vorbis, FLAC, AC-3, DTS 等
- ✅ **增强格式支持**：处理 Android 原生不支持的编码格式
- ✅ **提高兼容性**：在不同设备上提供一致的解码能力
- ✅ **软件解码**：在硬件解码失败时提供备选方案

**它不能做什么：**
- ❌ **不提供网络协议支持**（RTSP, RTMP, HLS 等协议由播放器内核处理）
- ❌ **不提供解封装能力**（MP4, MKV, FLV 等容器格式由播放器内核处理）
- ❌ **不能解决阿里云播放器不支持 RTSP 的问题**（这是协议层的限制，需要切换内核）

**播放器架构说明：**

```
完整的视频播放流程：

网络协议层 (Protocol)  ← 处理 RTSP/RTMP/HLS 等协议（播放器内核负责）
    ↓
解封装层 (Demuxer)     ← 处理 MP4/MKV/FLV 等容器（播放器内核负责）
    ↓
解码层 (Decoder)       ← FFmpeg 解码器在这里工作 ⭐
    ↓
渲染层 (Renderer)      ← 显示画面和播放声音
```

**使用场景：**
- ✅ 播放包含特殊编码格式的视频（如 VP9, AV1, HEVC）
- ✅ 在低端设备上提供软件解码支持
- ✅ 确保在所有设备上的解码一致性
- ✅ 播放包含 AC-3, DTS 等音频编码的视频

**不适用场景：**
- ❌ 解决协议不支持问题（如阿里云不支持 RTSP）→ 应该切换播放器内核
- ❌ 添加新的网络协议支持 → 由播放器内核决定
- ❌ 处理容器格式问题 → 由播放器内核决定

**有无 FFmpeg 解码器的区别：**

| 场景 | 无 FFmpeg 解码器 | 有 FFmpeg 解码器 |
|------|----------------|----------------|
| H.264 视频 | ✅ 硬件解码 | ✅ 硬件解码（优先）+ 软件解码（备选） |
| H.265/HEVC | ✅ 硬件解码（部分设备） | ✅ 硬件解码 + 软件解码（兼容性更好） |
| VP9 视频 | ⚠️ 部分设备不支持 | ✅ 软件解码（所有设备支持） |
| AV1 视频 | ❌ 大部分设备不支持 | ✅ 软件解码（所有设备支持） |
| AC-3/DTS 音频 | ❌ 不支持 | ✅ 软件解码 |
| RTSP 协议 | 取决于播放器内核 | 取决于播放器内核（解码器不影响） |

**推荐配置：**

```gradle
dependencies {
    // 播放器内核（必需）
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
    
    // FFmpeg 解码器（可选，增强编码格式支持）
    implementation 'androidx.media3:media3-decoder-ffmpeg:1.5.0'
}
```

> 💡 **提示**：
> - 如果只播放常见格式（H.264 + AAC），可以不添加 FFmpeg 解码器
> - 如果需要播放 VP9, AV1, HEVC 或 AC-3/DTS 音频，建议添加
> - 如果遇到协议不支持的问题（如阿里云无法播放 RTSP），应该切换播放器内核，而不是添加 FFmpeg 解码器
> - 详见 [阿里云 FFmpeg 分析](docs/ALIYUN_FFMPEG_ANALYSIS.md)

### ExoPlayer 内核

Google 官方播放器，性能优秀。

```gradle
dependencies {
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
}
```

切换到 ExoPlayer：

```java
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
```

### 阿里云播放器

商业级播放器，支持加密视频、更好的直播支持。

```gradle
// 在项目根目录 build.gradle 添加阿里云仓库
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}

// 在 app/build.gradle 添加依赖
dependencies {
    // 排除内置的阿里云播放器，避免授权问题
    implementation('io.github.706412584:gsyVideoPlayer-aliplay:1.1.0') {
        exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
        exclude group: 'com.alivc.conan', module: 'AlivcConan'
    }
    
    // 使用指定版本的阿里云播放器（5.4.7.1 版本无需授权）
    implementation 'com.aliyun.sdk.android:AliyunPlayer:5.4.7.1-full'
}
```

> ⚠️ **重要提示**：
> - `gsyvideoplayer-aliplay` 内置的阿里云播放器版本较新，需要授权才能播放
> - 使用 `5.4.7.1-full` 版本可以免授权使用
> - 如需使用最新版本，请到阿里云官网申请授权

切换到阿里云播放器：

```java
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
```

### DLNA 投屏

```gradle
dependencies {
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

详细配置请查看 [投屏功能指南](docs/CAST_GUIDE.md)。

---

## 播放内核对比

| 内核 | 优点 | 缺点 | 推荐场景 | 支持协议 |
|------|------|------|----------|----------|
| **IJK** | 格式支持最全，开源免费 | 包体积较大（~10MB） | 通用场景，格式复杂 | RTSP, RTMP, HLS, HTTP, FLV |
| **ExoPlayer** | 性能好，Google 官方，RTSP 支持完整 | 部分格式支持有限 | 性能要求高，RTSP 直播 | RTSP, HLS, DASH, HTTP |
| **系统播放器** | 无额外依赖，包体积小 | 功能有限，兼容性差 | 简单场景 | 取决于设备 |
| **阿里云** | **RTMP 延迟极低（1-3秒）**，商业级优化 | 收费，包体积大，**不支持 RTSP** | **RTMP 直播**，HLS 直播 | HLS, RTMP, FLV, HTTP |

> ⚠️ **直播流播放建议**：
> - **RTSP 直播**：使用 ExoPlayer 或 IJK（阿里云不支持）
> - **RTMP 直播**：⭐ **强烈推荐阿里云**（延迟极低 1-3秒，商业级优化）
>   - 阿里云 RTMP：1-3 秒延迟
>   - IJK RTMP：3-5 秒延迟
>   - HLS：10-30 秒延迟
> - **HLS 直播 (m3u8)**：阿里云、ExoPlayer、IJK 均可
> - **系统播放器**：不推荐用于直播（兼容性差）

> 💡 **协议选择建议**：
> - **超低延迟直播（1-3秒）**：RTMP + 阿里云 ⭐
> - **RTSP 直播**：ExoPlayer 或 IJK
> - **HLS 直播 (m3u8)**：阿里云、ExoPlayer、IJK 均可
> - **点播视频 (MP4/HTTP)**：所有内核均支持

> 🚀 **自动内核选择**：
> 从 v1.0.8 开始，OrangePlayer 提供智能内核选择功能，可根据 URL 协议自动选择最合适的内核。
> 
> **启用方式：**
> ```java
> PlayerSettingsManager.getInstance(context).setAutoSelectEngine(true);
> ```
> 
> **特性：**
> - ✅ 自动检测依赖是否已导入
> - ✅ 只在需要时才切换内核
> - ✅ 默认禁用，需要手动启用

---

## 可选功能

## 常见问题

遇到问题？查看 [常见问题解答](docs/FAQ.md)：

- [NoClassDefFoundError: BasePlayerManager](docs/FAQ.md#q1-noclassdeffounderror-baseplayermanager)
- [播放黑屏或无声音](docs/FAQ.md#q4-播放黑屏或无声音)
- [阿里云播放器黑屏/水印](docs/FAQ.md#q5-阿里云播放器黑屏水印)
- [字幕不显示](docs/FAQ.md#q8-字幕不显示)
- [语音识别无法启动](docs/FAQ.md#q10-语音识别无法启动)
- [OCR 识别不准确](docs/FAQ.md#q11-ocr-识别不准确)

更多问题请查看完整的 [FAQ 文档](docs/FAQ.md)。

---

## API 文档

完整的 API 参考请查看 [API 文档](docs/API.md)。

### 主要类

- [OrangevideoView](docs/API.md#orangevideoview) - 主播放器视图
- [OrangeVideoController](docs/API.md#orangevideocontroller) - 播放器控制器
- [SubtitleManager](docs/API.md#subtitlemanager) - 字幕管理器
- [LanguagePackManager](docs/API.md#languagepackmanager) - OCR 语言包管理
- [PlayerSettingsManager](docs/API.md#playersettingsmanager) - 设置管理器
- [PlayerConstants](docs/API.md#playerconstants) - 常量定义

---

## 项目结构

详细结构请查看 [STRUCTURE.md](docs/STRUCTURE.md)

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
