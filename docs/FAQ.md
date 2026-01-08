# 常见问题

OrangePlayer 常见问题解答。

## 目录

- [依赖问题](#依赖问题)
- [播放问题](#播放问题)
- [功能问题](#功能问题)
- [性能问题](#性能问题)
- [其他问题](#其他问题)

---

## 依赖问题

### Q1: NoClassDefFoundError: BasePlayerManager

**错误信息：**
```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/shuyu/gsyvideoplayer/player/BasePlayerManager
```

**原因：** 缺少 GSYVideoPlayer 基础依赖或子依赖

**解决方案：**

**方案一：仅使用系统播放器（最小依赖）**

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 最小依赖（系统播放器必需）
    implementation 'io.github.carguo:gsyvideoplayer-java:11.3.0'
    implementation 'io.github.carguo:gsyvideoplayer-base:11.3.0'
    implementation 'io.github.carguo:gsyvideoplayer-androidvideocache:11.3.0'
    implementation 'io.github.carguo:gsyijkjava:1.0.0'
}
```

**方案二：使用 ExoPlayer（推荐）**

```gradle
dependencies {
    // OrangePlayer 核心库
    implementation 'com.github.706412584:orangeplayer:v1.0.3'
    
    // GSY 基础依赖（必需！）
    implementation 'io.github.carguo:gsyvideoplayer-java:11.3.0'
    
    // GSY 子依赖（如果构建工具不自动解析传递依赖）
    implementation 'io.github.carguo:gsyvideoplayer-androidvideocache:11.3.0'
    implementation 'io.github.carguo:gsyvideoplayer-base:11.3.0'
    implementation 'io.github.carguo:gsyijkjava:1.0.0'
    
    // ExoPlayer 播放内核
    implementation 'io.github.carguo:gsyvideoplayer-exo2:11.3.0'
    
    // ExoPlayer 依赖（Media3）
    implementation 'androidx.media3:media3-exoplayer:1.8.0'
    implementation 'androidx.media3:media3-ui:1.8.0'
}
```

详见 [安装指南](INSTALLATION.md)。

### Q2: 投屏功能不可用

**原因：** 缺少 DLNA 投屏库

**解决方案：**

```gradle
dependencies {
    implementation 'com.github.AnyListen:UaoanDLNA:1.0.1'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okio:okio:3.6.0'
}
```

### Q3: OCR/语音识别按钮显示"查看安装说明"

**原因：** 缺少对应的 SDK 依赖

**解决方案：**

**OCR 功能：**
```gradle
dependencies {
    implementation 'cz.adaptech.tesseract4android:tesseract4android:4.7.0'
    implementation 'com.google.mlkit:translate:17.0.2'
}
```

详见 [OCR 功能指南](OCR_GUIDE.md)。

**语音识别：**
```gradle
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

详见 [语音识别指南](SPEECH_RECOGNITION.md)。

---

## 播放问题

### Q4: 播放黑屏或无声音

**可能原因：**
1. 视频编码格式不支持
2. 播放内核不兼容
3. 视频文件损坏
4. 网络连接问题

**解决方案：**

**1. 尝试切换播放内核**

```java
// 切换到 ExoPlayer
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
videoView.startPlayLogic();

// 切换到 IJK
videoView.selectPlayerFactory(PlayerConstants.ENGINE_IJK);
videoView.startPlayLogic();

// 切换到系统播放器
videoView.selectPlayerFactory(PlayerConstants.ENGINE_DEFAULT);
videoView.startPlayLogic();
```

**2. 添加扩展编码支持（MPEG 编码）**

如果是 MPEG 编码视频，添加扩展 so 库：

```gradle
dependencies {
    implementation 'io.github.carguo:gsyvideoplayer-ex_so:11.3.0'
}
```

**3. 检查视频格式**

支持的格式：
- 容器：MP4、MKV、AVI、FLV、TS、M3U8
- 视频编码：H.264、H.265、VP8、VP9
- 音频编码：AAC、MP3、Opus

**4. 检查网络连接**

```java
// 添加网络权限
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

// 允许 HTTP 明文流量
<application android:usesCleartextTraffic="true" ... >
```

### Q5: 阿里云播放器黑屏/水印

**原因：** 阿里云播放器 5.4.0+ 需要 License

**解决方案：**

**方案一：申请 License（推荐）**

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

**方案二：使用旧版本（5.3.0 免授权）**

```gradle
// 排除默认的阿里云 SDK
implementation ('com.github.706412584:orangeplayer:v1.0.3') {
    exclude group: 'com.aliyun.sdk.android', module: 'AliyunPlayer'
}

// 使用 5.3.0 免授权版本
implementation 'com.aliyun.sdk.android:AliyunPlayer:5.3.0-full'
```

详见 [阿里云播放器配置](ALIYUN_PLAYER.md)。

### Q6: 视频卡顿或缓冲慢

**可能原因：**
1. 网络速度慢
2. 视频码率过高
3. 设备性能不足
4. 缓存设置不当

**解决方案：**

**1. 降低视频分辨率**

选择较低分辨率的视频源。

**2. 启用硬件解码**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setHardwareDecode(true);
```

**3. 调整缓冲设置**

```java
// 增加缓冲大小
GSYVideoManager.instance().setBufferSize(512 * 1024);  // 512KB
```

**4. 使用 ExoPlayer**

ExoPlayer 的缓冲策略更优：

```java
videoView.selectPlayerFactory(PlayerConstants.ENGINE_EXO);
```

### Q7: 横竖屏切换后播放异常

**可能原因：**
1. Activity 配置不正确
2. 生命周期处理不当

**解决方案：**

**1. 配置 AndroidManifest.xml**

```xml
<activity
    android:name=".YourActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden"
    android:supportsPictureInPicture="true"
    android:resizeableActivity="true">
</activity>
```

**2. 处理配置变化**

```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    videoView.onConfigurationChanged(this, newConfig, 
        mOrientationUtils, true, true);
}
```

**3. 处理返回键**

```java
@Override
public void onBackPressed() {
    if (videoView.isFullScreen()) {
        videoView.exitFullScreen();
        return;
    }
    super.onBackPressed();
}
```

---

## 功能问题

### Q8: 字幕不显示

**可能原因：**
1. 字幕文件格式不支持
2. 字幕编码问题
3. 字幕加载失败

**解决方案：**

**1. 检查字幕格式**

支持的格式：SRT、ASS/SSA、VTT

**2. 检查字幕编码**

字幕文件应使用 UTF-8 编码。

**3. 检查加载结果**

```java
SubtitleManager manager = videoView.getVideoController().getSubtitleManager();
manager.loadSubtitle(url, new SubtitleManager.OnSubtitleLoadListener() {
    @Override
    public void onLoadSuccess(int count) {
        Log.d(TAG, "字幕加载成功，共 " + count + " 条");
        manager.start();
    }
    
    @Override
    public void onLoadFailed(String error) {
        Log.e(TAG, "字幕加载失败：" + error);
    }
});
```

**4. 调整字幕大小**

```java
manager.setTextSize(18f);  // 18sp
```

### Q9: 弹幕不显示

**可能原因：**
1. 弹幕被隐藏
2. 弹幕透明度设置为 0
3. 弹幕速度过快

**解决方案：**

**1. 显示弹幕**

```java
IDanmakuController danmaku = videoView.getVideoController().getDanmakuController();
danmaku.show();
```

**2. 调整弹幕设置**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setDanmakuTextSize(16f);      // 字体大小
settings.setDanmakuSpeed(1.2f);        // 滚动速度
settings.setDanmakuAlpha(0.8f);        // 透明度（0.0-1.0）
```

**3. 发送测试弹幕**

```java
danmaku.sendDanmaku("测试弹幕", 0xFFFFFFFF);
```

### Q10: 语音识别无法启动

**可能原因：**
1. Android 版本低于 10
2. 缺少 Vosk SDK
3. 模型文件未正确放置
4. 权限未授予

**解决方案：**

**1. 检查 Android 版本**

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // Android 10+，可以使用语音识别
} else {
    Toast.makeText(this, "语音识别需要 Android 10 或更高版本", 
        Toast.LENGTH_SHORT).show();
}
```

**2. 添加依赖**

```gradle
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.47'
}
```

**3. 下载并放置模型文件**

下载模型：https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip

放置位置：
```
app/src/main/assets/
└── vosk-model-small-cn/
    ├── am/
    ├── conf/
    ├── graph/
    └── ...
```

**4. 授予权限**

首次使用时会弹出系统权限对话框，选择"此应用"并允许。

详见 [语音识别指南](SPEECH_RECOGNITION.md)。

### Q11: OCR 识别不准确

**可能原因：**
1. 字幕太小或太模糊
2. 识别区域设置不正确
3. 选择的语言不正确
4. 背景干扰

**解决方案：**

**1. 调整识别区域**

只包含字幕部分，排除背景干扰。

**2. 选择正确的源语言**

确保选择的语言与字幕语言一致。

**3. 使用高清视频源**

清晰的字幕识别率更高。

**4. 暂停视频进行识别**

暂停后识别准确率更高。

详见 [OCR 功能指南](OCR_GUIDE.md)。

---

## 性能问题

### Q12: 应用内存占用过高

**可能原因：**
1. 视频分辨率过高
2. 缓存设置过大
3. 语言包/模型占用内存
4. 内存泄漏

**解决方案：**

**1. 降低视频分辨率**

选择较低分辨率的视频源。

**2. 及时释放资源**

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    videoView.release();
}
```

**3. 清理缓存**

```java
// 清理视频缓存
GSYVideoManager.instance().clearAllDefaultCache(context);
```

**4. 卸载不用的语言包**

```java
LanguagePackManager manager = new LanguagePackManager(context);
manager.deleteLanguage("unused_lang");
```

### Q13: 应用启动慢

**可能原因：**
1. 语言包/模型加载慢
2. 初始化操作过多
3. 主线程阻塞

**解决方案：**

**1. 延迟加载语言包**

不要在启动时加载所有语言包，按需加载。

**2. 异步初始化**

```java
new Thread(() -> {
    // 初始化操作
    initPlayer();
}).start();
```

**3. 使用懒加载**

只在需要时才初始化功能模块。

### Q14: 播放时 CPU 占用高

**可能原因：**
1. 使用软件解码
2. 视频码率过高
3. OCR/语音识别占用 CPU
4. 弹幕渲染占用 CPU

**解决方案：**

**1. 启用硬件解码**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setHardwareDecode(true);
```

**2. 关闭不需要的功能**

```java
// 关闭 OCR
eventManager.stopOcrTranslate();

// 关闭语音识别
eventManager.stopSpeechTranslate();

// 隐藏弹幕
danmaku.hide();
```

**3. 降低视频分辨率**

选择较低分辨率的视频源。

---

## 其他问题

### Q15: 如何自定义 UI

**方案一：修改主题颜色**

在 `styles.xml` 中定义主题颜色：

```xml
<style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>
```

**方案二：自定义控制器**

```java
OrangeVideoController controller = new OrangeVideoController(context);
// 自定义控制器...
videoView.setVideoController(controller);
```

**方案三：自定义组件**

继承现有组件并重写方法：

```java
public class MyVodControlView extends VodControlView {
    public MyVodControlView(Context context) {
        super(context);
    }
    
    @Override
    protected void initView() {
        super.initView();
        // 自定义 UI
    }
}
```

### Q16: 如何保存播放进度

OrangePlayer 自动保存播放进度，无需手动处理。

**查看播放历史：**

```java
import com.orange.playerlibrary.history.PlayHistoryManager;

PlayHistoryManager manager = PlayHistoryManager.getInstance(context);
long position = manager.getHistory(videoUrl);
if (position > 0) {
    videoView.seekTo(position);
}
```

**清除播放历史：**

```java
manager.deleteHistory(videoUrl);
```

### Q17: 如何实现倍速播放

**设置倍速：**

```java
videoView.setSpeed(1.5f);  // 1.5 倍速
```

**倍速限制：**
- IJK 内核：最高 2.0x
- 其他内核：最高 5.0x

**长按倍速：**

```java
PlayerSettingsManager settings = PlayerSettingsManager.getInstance(context);
settings.setLongPressSpeed(2.0f);  // 长按 2 倍速
```

### Q18: 如何实现画中画

**进入画中画：**

```java
import android.app.PictureInPictureParams;
import android.util.Rational;

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Rational aspectRatio = new Rational(16, 9);
    PictureInPictureParams params = new PictureInPictureParams.Builder()
        .setAspectRatio(aspectRatio)
        .build();
    enterPictureInPictureMode(params);
}
```

**处理画中画状态变化：**

```java
@Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, 
                                          Configuration newConfig) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
    
    if (isInPictureInPictureMode) {
        videoView.getVideoController().hide();
    } else {
        videoView.getVideoController().show();
    }
}
```

**配置 AndroidManifest.xml：**

```xml
<activity
    android:name=".YourActivity"
    android:supportsPictureInPicture="true"
    android:resizeableActivity="true">
</activity>
```

---

## 获取帮助

如果以上方案无法解决你的问题，可以通过以下方式获取帮助：

1. **查看文档**
   - [安装指南](INSTALLATION.md)
   - [API 文档](API.md)
   - [OCR 功能指南](OCR_GUIDE.md)
   - [语音识别指南](SPEECH_RECOGNITION.md)

2. **提交 Issue**
   - GitHub Issues: https://github.com/706412584/orangeplayer/issues
   - 请提供详细的错误信息和复现步骤

3. **联系作者**
   - QQ: 706412584

4. **查看示例代码**
   - Demo 应用：https://github.com/706412584/orangeplayer/tree/main/app
