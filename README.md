# OrangePlayer 橘子播放器

> **For Developers**: Professional Android video player library for building video applications.  
> Open source under Apache 2.0 License.

<p align="center">
  <img src="docs/logo.png" width="200" alt="OrangePlayer Logo" />
</p>

<p align="center">
  <strong>功能完整的 Android 视频播放器 SDK</strong>
</p>

<p align="center">
  基于 <a href="https://github.com/CarGuo/GSYVideoPlayer">GSYVideoPlayer</a> 的增强视频播放器库，为 Android 开发者提供完整的视频播放解决方案
</p>

<p align="center">
  <a href="https://jitpack.io/#706412584/orangeplayer"><img src="https://jitpack.io/v/706412584/orangeplayer.svg" alt="JitPack"></a>
  <a href="https://github.com/706412584/orangeplayer/actions/workflows/android.yml"><img src="https://github.com/706412584/orangeplayer/actions/workflows/android.yml/badge.svg" alt="Android CI"></a>
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

- **开发文档**
  - [API 文档](docs/API.md) - 完整的 API 参考
  - [项目结构](docs/STRUCTURE.md) - 代码组织说明

- **其他**
  - [常见问题](docs/FAQ.md) - 问题排查和解决方案
  - [更新日志](CHANGELOG.md) - 版本更新记录

---

## 快速开始

### 1. 添加仓库

**使用 Maven Central（推荐）**

无需额外配置，Maven Central 已默认包含在 Gradle 中。

**使用 JitPack**

在项目根目录的 `settings.gradle` 中添加 JitPack 仓库：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 添加 JitPack 仓库
    }
}
```

或者在 `build.gradle` (Project) 中添加：

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 添加 JitPack 仓库
    }
}
```

### 2. 添加依赖

在 `app/build.gradle` 中添加：

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.7'  // JitPack
    // 或
    implementation 'io.github.706412584:orangeplayer:1.0.7'    // Maven Central
    
    // GSY 基础依赖（必需）
    implementation 'io.github.carguo:gsyvideoplayer-java:11.3.0'
    
    // ExoPlayer 播放内核（推荐）
    implementation 'io.github.carguo:gsyvideoplayer-exo2:11.3.0'
}
```

> 💡 **提示**：
> - JitPack 版本：`com.github.706412584:orangeplayer:v1.0.7`（需要添加 JitPack 仓库）
> - 完整的依赖配置请查看 [安装指南](docs/INSTALLATION.md)

### 3. 布局文件

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

### 4. 基本使用

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

OrangePlayer 支持 4 种播放内核，可在运行时动态切换。

```java
// 切换到 ExoPlayer（推荐）
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 切换到 IJK 播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);

// 切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);

// 切换到阿里云播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);
```

详细对比和配置请查看 [播放内核指南](docs/PLAYER_ENGINES.md)。

---

## 可选功能

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

### DLNA 投屏

```gradle
dependencies {
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
```

详细配置请查看 [投屏功能指南](docs/CAST_GUIDE.md)。

---

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
