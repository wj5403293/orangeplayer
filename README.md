# OrangePlayer 橘子播放器

基于 [GSYVideoPlayer](https://github.com/CarGuo/GSYVideoPlayer) 的增强视频播放器库，提供丰富的播放功能和自定义控制组件。

[![](https://jitpack.io/v/user/orangeplayer.svg)](https://jitpack.io/#user/orangeplayer)

## 功能特性

- 🎬 多播放内核支持（系统、ExoPlayer、IJK、阿里云）
- 📝 字幕支持（SRT/ASS/VTT 格式）
- 🔤 OCR 硬字幕识别与翻译
- 💬 弹幕功能
- 🎛️ 倍速播放（0.35x - 10x）
- ⏰ 定时关闭
- 📺 投屏支持
- 🖼️ 画中画模式
- 📸 视频截图
- ⏭️ 跳过片头片尾

## 集成方式

### Step 1. 添加 JitPack 仓库

在项目根目录的 `settings.gradle` 中添加：

```gradle
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
        maven { url 'https://maven.aliyun.com/repository/releases' }
    }
}
```

### Step 2. 添加依赖

在 app 模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.github.user:orangeplayer:1.0.0'
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

## OCR 字幕翻译

OCR 功能需要下载语言包。语言包文件位于 `tessdata_packs/` 目录：

| 语言 | 文件名 | 大小 |
|------|--------|------|
| 简体中文 | chi_sim.traineddata | 2.35 MB |
| 繁体中文 | chi_tra.traineddata | 2.26 MB |
| 英语 | eng.traineddata | 3.92 MB |
| 日语 | jpn.traineddata | 2.36 MB |
| 韩语 | kor.traineddata | 1.60 MB |

### 使用预置语言包

将语言包文件复制到 `assets/tessdata/` 目录，应用启动时会自动识别。

### 在线下载

用户也可以在应用内通过"管理语言包"功能在线下载所需语言包。

## 依赖说明

本库依赖以下组件：

- GSYVideoPlayer 11.3.0
- Tesseract4Android 4.7.0
- ML Kit Translate 17.0.2
- DanmakuFlameMaster 0.9.25
- Glide 4.16.0

## 混淆配置

如果开启了代码混淆，请添加以下规则：

```proguard
# GSYVideoPlayer
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keep class tv.danmaku.ijk.** { *; }

# OrangePlayer
-keep class com.orange.playerlibrary.** { *; }

# Tesseract
-keep class com.googlecode.tesseract.android.** { *; }
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
