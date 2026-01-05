# OrangePlayer 橘子播放器

基于 [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer) 的增强视频播放器库，提供丰富的播放功能和自定义控制组件。

[![](https://jitpack.io/v/706412584/orangeplayer.svg)](https://jitpack.io/#706412584/orangeplayer)
[![Android CI](https://github.com/706412584/orangeplayer/actions/workflows/android.yml/badge.svg)](https://github.com/706412584/orangeplayer/actions/workflows/android.yml)

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
| 📋 播放列表 | 选集/历史/进度记忆 |

## 快速开始

### 1. 添加仓库

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

### 2. 添加依赖

```gradle
dependencies {
    implementation 'com.github.706412584:orangeplayer:1.0.0'
}
```

### 3. 使用播放器

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

```java
OrangevideoView videoView = findViewById(R.id.video_player);
videoView.setUp("https://example.com/video.mp4", true, "视频标题");
videoView.startPlayLogic();
```

## API 文档

详细 API 请查看 [API.md](docs/API.md)

## 项目结构

详细结构请查看 [STRUCTURE.md](docs/STRUCTURE.md)

## OCR 语言包

语言包文件位于 `tessdata_packs/` 目录：

| 语言 | 文件 | 大小 |
|------|------|------|
| 简体中文 | chi_sim.traineddata | 2.35 MB |
| 繁体中文 | chi_tra.traineddata | 2.26 MB |
| 英语 | eng.traineddata | 3.92 MB |
| 日语 | jpn.traineddata | 2.36 MB |
| 韩语 | kor.traineddata | 1.60 MB |

将语言包复制到 `assets/tessdata/` 或使用应用内下载功能。

## 依赖

| 库 | 版本 |
|------|------|
| GSYVideoPlayer | 11.3.0 |
| Tesseract4Android | 4.7.0 |
| ML Kit Translate | 17.0.2 |
| DanmakuFlameMaster | 0.9.25 |
| AliyunPlayer | 5.4.7.1 |

## 混淆配置

```proguard
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }
-keep class com.orange.playerlibrary.** { *; }
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.aliyun.player.** { *; }
```

## License

Apache License 2.0

## 致谢

- [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer)
- [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android)
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
