# API 文档

## OrangevideoView

主播放器视图类。

### 基础方法

```java
// 设置播放地址
boolean setUp(String url, boolean cacheWithPlay, String title)

// 开始播放
void startPlayLogic()

// 暂停
void pause()

// 恢复播放
void resume()

// 跳转到指定位置（毫秒）
void seekTo(long position)

// 释放播放器
void release()
```

### 播放状态

```java
// 是否正在播放
boolean isPlaying()

// 获取当前播放位置（毫秒）
long getCurrentPositionWhenPlaying()

// 获取视频总时长（毫秒）
long getDuration()

// 获取缓冲进度
int getBuffterPoint()
```

### 播放控制

```java
// 设置倍速 (0.35 - 10.0)
void setSpeed(float speed)

// 获取当前倍速
float getSpeed()

// 设置播放地址
void setUrl(String url)
void setUrl(String url, Map<String, String> headers)

// 获取播放地址
String getUrl()
```

### 播放内核

```java
// 切换播放内核
void selectPlayerFactory(String engine)

// 可选值：
// PlayerConstants.ENGINE_DEFAULT  - 系统播放器
// PlayerConstants.ENGINE_EXO      - ExoPlayer
// PlayerConstants.ENGINE_IJK      - IJK 播放器
// PlayerConstants.ENGINE_ALI      - 阿里云播放器
```

### 全屏控制

```java
// 进入全屏
void startWindowFullscreen(Context context, boolean actionBar, boolean statusBar)

// 退出全屏
void exitFullScreen()

// 是否全屏
boolean isFullScreen()
```

### 组件控制

```java
// 启用 Orange 组件
void enableOrangeComponents()

// 获取控制器
OrangeVideoController getController()

// 设置调试日志回调
void setDebugLogCallback(DebugLogCallback callback)
```

---

## OrangeVideoController

播放器控制器。

### 字幕控制

```java
// 加载字幕
void loadSubtitle(String url, SubtitleManager.OnSubtitleLoadListener listener)

// 获取字幕管理器
SubtitleManager getSubtitleManager()

// 是否已加载字幕
boolean isSubtitleLoaded()

// 开始字幕显示
void startSubtitle()

// 停止字幕显示
void stopSubtitle()
```

### 弹幕控制

```java
// 获取弹幕控制器
IDanmakuController getDanmakuController()

// 发送弹幕
void sendDanmaku(String text)

// 显示/隐藏弹幕
void showDanmaku()
void hideDanmaku()
```

### 播放列表

```java
// 设置播放列表
void setVideoList(ArrayList<HashMap<String, Object>> list)

// 获取播放列表
ArrayList<HashMap<String, Object>> getVideoList()

// 播放指定索引
void playIndex(int index)

// 播放下一个
void playNext()
```

### UI 控制

```java
// 显示/隐藏控制器
void show()
void hide()

// 是否正在显示
boolean isShowing()

// 设置标题
void setTitle(String title)
```

---

## SubtitleManager

字幕管理器。

```java
// 从 URL 加载字幕
void loadSubtitle(String url, OnSubtitleLoadListener listener)

// 从 Uri 加载字幕
void loadSubtitle(Uri uri, OnSubtitleLoadListener listener)

// 设置字幕大小 (12-36 sp)
void setTextSize(float size)

// 获取字幕数量
int getSubtitleCount()

// 开始/停止字幕
void start()
void stop()

// 回调接口
interface OnSubtitleLoadListener {
    void onLoadSuccess(int count);
    void onLoadFailed(String error);
}
```

---

## LanguagePackManager

OCR 语言包管理器。

```java
// 获取可用语言列表
List<LanguagePack> getAvailableLanguages()

// 获取已安装语言列表
List<String> getInstalledLanguages()

// 检查语言是否已安装
boolean isLanguageInstalled(String languageCode)

// 下载语言包
void downloadLanguage(String languageCode, DownloadCallback callback)

// 删除语言包
boolean deleteLanguage(String languageCode)

// 获取语言显示名称
static String getLanguageDisplayName(String langCode)

// 下载回调
interface DownloadCallback {
    void onProgress(int progress, long downloaded, long total);
    void onSuccess();
    void onError(String error);
}
```

---

## PlayerSettingsManager

设置管理器。

```java
// 获取实例
static PlayerSettingsManager getInstance(Context context)

// 播放内核
void setPlayerEngine(String engine)
String getPlayerEngine()

// 播放模式
void setPlayMode(String mode)  // "sequential", "single_loop", "play_pause"
String getPlayMode()

// 倍速设置
void setLongPressSpeed(float speed)
float getLongPressSpeed()

// 跳过片头片尾
void setSkipOpening(int seconds)
int getSkipOpening()
void setSkipEnding(int seconds)
int getSkipEnding()

// 弹幕设置
void setDanmakuTextSize(float size)
void setDanmakuSpeed(float speed)
void setDanmakuAlpha(float alpha)

// 底部进度条
void setBottomProgressEnabled(boolean enabled)
boolean isBottomProgressEnabled()
```

---

## PlayerConstants

常量定义。

```java
// 播放内核
String ENGINE_DEFAULT = "system"
String ENGINE_EXO = "exo"
String ENGINE_IJK = "ijk"
String ENGINE_ALI = "ali"

// 播放状态
int STATE_IDLE = 0
int STATE_PREPARING = 1
int STATE_PREPARED = 2
int STATE_PLAYING = 3
int STATE_PAUSED = 4
int STATE_PLAYBACK_COMPLETED = 5
int STATE_ERROR = 6

// 播放器状态
int PLAYER_NORMAL = 10
int PLAYER_FULL_SCREEN = 11
```

---

## 监听器

### OnStateChangeListener

```java
interface OnStateChangeListener {
    void onPlayerStateChanged(int playerState);
    void onPlayStateChanged(int playState);
}

// 使用
videoView.addOnStateChangeListener(new OnStateChangeListener() {
    @Override
    public void onPlayerStateChanged(int playerState) {
        // PLAYER_NORMAL, PLAYER_FULL_SCREEN
    }
    
    @Override
    public void onPlayStateChanged(int playState) {
        // STATE_IDLE, STATE_PLAYING, STATE_PAUSED, etc.
    }
});
```

### OnPlayCompleteListener

```java
interface OnPlayCompleteListener {
    void onPlayComplete();
}
```

### OnProgressListener

```java
interface OnProgressListener {
    void onProgress(long currentPosition, long duration);
}
```
