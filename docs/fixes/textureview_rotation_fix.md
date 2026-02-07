# TextureView 横竖屏切换修复

## 问题描述

ExoPlayer 和系统播放器在使用 TextureView 渲染模式时，横竖屏切换会导致以下问题：

1. **SurfaceTexture 被销毁和重建**：系统默认会在 `onSurfaceTextureDestroyed()` 时销毁 SurfaceTexture
2. **MediaCodec 崩溃**：MediaCodec 正在渲染到已销毁的 Surface，导致 `IllegalStateException`
3. **播放中断**：直播流（RTSP/RTMP）在 Surface 切换时连接中断

## 根本原因

GSYVideoPlayer 的 `GSYTextureView.onSurfaceTextureDestroyed()` 方法：

```java
@Override
public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    if (mIGSYSurfaceListener != null) {
        mIGSYSurfaceListener.onSurfaceDestroyed(mSurface);
    }
    if (GSYVideoType.isMediaCodecTexture()) {
        return (mSaveTexture == null);  // 返回 false，保留 SurfaceTexture
    } else {
        return true;  // 返回 true，销毁 SurfaceTexture
    }
}
```

- **返回 `true`**：系统会销毁 SurfaceTexture，横竖屏切换时重新创建
- **返回 `false`**：保留 SurfaceTexture，横竖屏切换时复用

## 解决方案

### 方案 1：启用 MediaCodecTexture（推荐）✅

在 `OrangevideoView.applyDecodeMode()` 中，**无论硬解还是软解，都启用 MediaCodecTexture**：

```java
private void applyDecodeMode(PlayerSettingsManager settingsManager) {
    boolean useHardware = settingsManager.isHardwareDecode();
    
    if (useHardware) {
        // 硬件解码
        com.shuyu.gsyvideoplayer.utils.GSYVideoType.enableMediaCodec();
    } else {
        // 软件解码
        com.shuyu.gsyvideoplayer.utils.GSYVideoType.disableMediaCodec();
    }
    
    // 关键修复：无论硬解还是软解，都启用 MediaCodecTexture
    // 这样 TextureView 在横竖屏切换时会保留 SurfaceTexture，不会重新创建
    com.shuyu.gsyvideoplayer.utils.GSYVideoType.enableMediaCodecTexture();
}
```

**优点：**
- ✅ 简单有效，一行代码解决问题
- ✅ 适用于所有播放器内核（ExoPlayer、系统播放器、IJK）
- ✅ 适用于所有 Android 版本
- ✅ 不影响性能

**原理：**
- `enableMediaCodecTexture()` 会让 `onSurfaceTextureDestroyed()` 返回 `false`
- 系统不会销毁 SurfaceTexture，横竖屏切换时复用
- MediaCodec 继续渲染到同一个 Surface，不会崩溃

### 方案 2：使用 SurfaceControl（Android Q+）

在 `OrangeExoPlayerManager` 和 `OrangeSystemPlayerManager` 中使用 `SurfaceControl.reparent()` 实现无缝切换：

```java
@RequiresApi(api = Build.VERSION_CODES.Q)
private void reparent(@Nullable SurfaceView surfaceView) {
    if (surfaceControl == null) {
        return;
    }
    
    if (surfaceView == null) {
        // 隐藏视频
        new SurfaceControl.Transaction()
            .reparent(surfaceControl, null)
            .setBufferSize(surfaceControl, 0, 0)
            .setVisibility(surfaceControl, false)
            .apply();
    } else {
        // reparent 到新的 SurfaceView
        SurfaceControl newParentSurfaceControl = surfaceView.getSurfaceControl();
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        
        new SurfaceControl.Transaction()
            .reparent(surfaceControl, newParentSurfaceControl)
            .setBufferSize(surfaceControl, width, height)
            .setVisibility(surfaceControl, true)
            .apply();
    }
}
```

**优点：**
- ✅ 完全无缝切换，画面不闪烁
- ✅ 不需要重新设置 Surface
- ✅ MediaCodec 不会被释放

**缺点：**
- ❌ 只支持 Android Q+ (API 29+)
- ❌ 需要使用 SurfaceView 渲染模式
- ❌ 实现复杂

### 方案 3：使用 PlaceholderSurface 作为中转

在 Surface 切换时，先切换到 PlaceholderSurface，避免 MediaCodec 渲染到 null：

```java
@Override
public void showDisplay(final Message msg) {
    if (msg.obj == null) {
        // Surface 为 null，切换到 PlaceholderSurface
        if (dummySurface != null && dummySurface.isValid()) {
            mediaPlayer.setSurface(dummySurface);
        }
    } else if (msg.obj instanceof Surface) {
        // 设置新的 Surface
        Surface holder = (Surface) msg.obj;
        if (holder != null && holder.isValid()) {
            mediaPlayer.setSurface(holder);
        }
    }
}
```

**优点：**
- ✅ 避免 MediaCodec 渲染到 null 导致崩溃
- ✅ 适用于所有 Android 版本

**缺点：**
- ❌ 仍然会重新创建 Surface
- ❌ 画面会闪烁
- ❌ 直播流可能中断

## 最终方案

**组合使用方案 1 + 方案 2 + 方案 3**：

1. **默认启用 MediaCodecTexture**（方案 1）：适用于所有版本和内核
2. **Android Q+ 使用 SurfaceControl**（方案 2）：提供最佳体验
3. **PlaceholderSurface 作为兜底**（方案 3）：避免极端情况崩溃

```java
// 1. 启用 MediaCodecTexture（所有版本）
GSYVideoType.enableMediaCodecTexture();

// 2. Android Q+ 使用 SurfaceControl（ExoPlayer 和系统播放器）
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    GSYVideoType.setRenderType(GSYVideoType.SURFACE);
    OrangeExoPlayerManager.setForceTextureViewMode(false);
} else {
    GSYVideoType.setRenderType(GSYVideoType.TEXTURE);
    OrangeExoPlayerManager.setForceTextureViewMode(true);
}

// 3. PlaceholderSurface 作为兜底
dummySurface = PlaceholderSurface.newInstanceV17(context, false);
```

## 测试结果

### 修复前

- ❌ ExoPlayer + TextureView：横竖屏切换崩溃
- ❌ 系统播放器 + TextureView：横竖屏切换崩溃
- ❌ RTSP 直播流：横竖屏切换连接中断

### 修复后

- ✅ ExoPlayer + TextureView：横竖屏切换正常
- ✅ 系统播放器 + TextureView：横竖屏切换正常
- ✅ RTSP 直播流：横竖屏切换不中断
- ✅ Android Q+ SurfaceView：完全无缝切换

## 相关文件

- `palyerlibrary/src/main/java/com/orange/playerlibrary/OrangevideoView.java` - 启用 MediaCodecTexture
- `palyerlibrary/src/main/java/com/orange/playerlibrary/exo/OrangeExoPlayerManager.java` - SurfaceControl 实现
- `palyerlibrary/src/main/java/com/orange/playerlibrary/player/OrangeSystemPlayerManager.java` - 系统播放器 SurfaceControl 实现
- `GSYVideoPlayer-source/gsyVideoPlayer-java/src/main/java/com/shuyu/gsyvideoplayer/render/view/GSYTextureView.java` - GSY 原始实现

## 参考资料

- [GSYVideoPlayer 官方文档](https://github.com/CarGuo/GSYVideoPlayer)
- [Android SurfaceControl 文档](https://developer.android.com/reference/android/view/SurfaceControl)
- [MediaCodec 文档](https://developer.android.com/reference/android/media/MediaCodec)
