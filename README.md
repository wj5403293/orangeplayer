# OrangePlayer 橘子播放器

基于 [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer) 的增强视频播放器库，提供丰富的播放功能和自定义控制组件。

[![](https://jitpack.io/v/706412584/orangeplayer.svg)](https://jitpack.io/#706412584/orangeplayer)

## 功能特性

### 🎬 多播放内核
- 系统播放器（MediaPlayer）
- ExoPlayer
- IJK 播放器
- 阿里云播放器
- 运行时动态切换内核

### 📝 字幕系统
- 支持 SRT/ASS/VTT 格式
- 本地字幕文件加载
- 网络字幕 URL 加载
- 字幕大小调节（12-36sp）
- 字幕显示/隐藏切换

### 🔤 OCR 硬字幕识别
- Tesseract OCR 引擎
- 支持多语言识别（中/英/日/韩等）
- ML Kit 实时翻译
- 语言包在线下载管理
- 支持 assets 预置语言包

### 💬 弹幕功能
- 基于 DanmakuFlameMaster
- 弹幕开关控制
- 弹幕大小调节（10-30sp）
- 弹幕速度调节（0.5x-3x）
- 弹幕透明度调节
- 弹幕发送功能

### 🎛️ 播放控制
- 倍速播放（0.35x - 10x）
- 长按倍速（可自定义）
- 播放模式（顺序/单集循环/播完暂停）
- 手势控制（亮度/音量/进度）
- 双击暂停/播放

### 📺 画面控制
- 多种画面比例（16:9/4:3/填充/原始等）
- 全屏/小窗切换
- 画中画模式（PiP）
- 底部进度条显示/隐藏

### ⏰ 定时功能
- 定时关闭（30/60/90/120分钟）
- 跳过片头（0-300秒可调）
- 跳过片尾（0-300秒可调）

### 📋 播放列表
- 选集功能
- 自动播放下一集
- 播放历史记录
- 播放进度记忆

### 🔧 其他功能
- 视频截图保存
- 视频下载
- DLNA 投屏
- 音量调节
- 自定义 Loading 动画
- 错误自动恢复
- 视频嗅探

## 集成方式

### Step 1. 添加 JitPack 仓库

在项目根目录的 `settings.gradle` 中添加：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

### Step 2. 添加依赖

在 app 模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.github.706412584:orangeplayer:1.0.0'
}
```

## 基本使用

### 布局文件

```xml
<com.orange.playerlibrary.OrangevideoView
    android:id="@+id/video_player"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

### 代码中使用

```java
OrangevideoView videoView = findViewById(R.id.video_player);

// 设置播放地址
videoView.setUp("https://example.com/video.mp4", true, "视频标题");

// 开始播放
videoView.startPlayLogic();
```

### 切换播放内核

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);

// 切换到 IJK
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);

// 切换到阿里云
videoView.selectPlayerFactory(PlayerConstants.ENGINE_ALI);

// 切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);
```

### 加载字幕

```java
// 从 URL 加载
videoView.getController().loadSubtitle("https://example.com/subtitle.srt", listener);

// 从本地文件加载
videoView.getController().getSubtitleManager().loadSubtitle(uri, listener);
```

### 播放列表

```java
ArrayList<HashMap<String, Object>> videoList = new ArrayList<>();
HashMap<String, Object> video1 = new HashMap<>();
video1.put("title", "第1集");
video1.put("url", "https://example.com/ep1.mp4");
videoList.add(video1);

videoView.getController().setVideoList(videoList);
```

## OCR 字幕翻译

OCR 功能需要 Tesseract 语言包。

### 预置语言包

将语言包文件复制到 `assets/tessdata/` 目录：

| 语言 | 文件名 | 大小 |
|------|--------|------|
| 简体中文 | chi_sim.traineddata | 2.35 MB |
| 繁体中文 | chi_tra.traineddata | 2.26 MB |
| 英语 | eng.traineddata | 3.92 MB |
| 日语 | jpn.traineddata | 2.36 MB |
| 韩语 | kor.traineddata | 1.60 MB |

语言包文件可从 `tessdata_packs/` 目录获取，或从 [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) 下载。

### 在线下载

用户可在应用内通过"管理语言包"功能在线下载，支持国内镜像加速。

## 组件说明

| 组件 | 说明 |
|------|------|
| `OrangevideoView` | 主播放器视图 |
| `OrangeVideoController` | 播放器控制器 |
| `VodControlView` | 点播控制组件 |
| `LiveControlView` | 直播控制组件 |
| `TitleView` | 标题栏组件 |
| `GestureView` | 手势控制组件 |
| `SubtitleView` | 字幕显示组件 |
| `DanmaView` | 弹幕显示组件 |

## 依赖说明

| 依赖 | 版本 | 说明 |
|------|------|------|
| GSYVideoPlayer | 11.3.0 | 播放器核心 |
| Tesseract4Android | 4.7.0 | OCR 引擎 |
| ML Kit Translate | 17.0.2 | 翻译引擎 |
| DanmakuFlameMaster | 0.9.25 | 弹幕引擎 |
| Glide | 4.16.0 | 图片加载 |
| AliyunPlayer | 5.4.7.1 | 阿里云播放器 |

## 混淆配置

```proguard
# GSYVideoPlayer
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }

# OrangePlayer
-keep class com.orange.playerlibrary.** { *; }

# Tesseract
-keep class com.googlecode.tesseract.android.** { *; }

# 阿里云播放器
-keep class com.aliyun.player.** { *; }
-keep class com.cicada.player.** { *; }
```

## 许可证

```
Copyright 2024 OrangePlayer

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 致谢

- [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer)
- [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android)
- [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
- [ML Kit](https://developers.google.com/ml-kit)
