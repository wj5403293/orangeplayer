# VideoDownloader 源码集成指南

## 1. 下载源码

从 GitHub 下载 VideoDownloader 源码：
```
https://github.com/JeffMony/VideoDownloader
```

## 2. 复制源码文件

将以下目录的 Java 文件复制到项目中：

### 源目录（VideoDownloader 项目）
```
VideoDownloader/videodownload/src/main/java/com/jeffmony/
```

### 目标目录（OrangePlayer 项目）
```
palyerlibrary/src/main/java/com/jeffmony/
```

### 需要复制的包结构
```
com/jeffmony/
├── downloader/          # 下载器核心
├── m3u8/                # M3U8 解析和下载
├── videocache/          # 视频缓存
├── database/            # 数据库管理
├── listener/            # 回调接口
├── model/               # 数据模型
└── utils/               # 工具类
```

## 3. 已添加的依赖

在 `palyerlibrary/build.gradle` 中已添加 VideoDownloader 需要的依赖：
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.9.3'
implementation 'com.google.code.gson:gson:2.10.1'
```

## 4. 初始化 VideoDownloader

在 Application 类中初始化（如果还没有 Application 类，需要创建）：

```java
import com.jeffmony.downloader.VideoDownloadConfig;
import com.jeffmony.downloader.VideoDownloadManager;

public class OrangePlayerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化 VideoDownloader
        VideoDownloadConfig config = new VideoDownloadConfig.Builder(this)
            .setCacheRoot(new File(getExternalFilesDir(null), "video"))  // 缓存目录
            .setUrlRedirect(true)                                         // 支持 URL 重定向
            .setTimeOut(30 * 1000, 30 * 1000, 30 * 1000)                 // 超时设置
            .setConcurrentCount(3)                                        // 并发下载数
            .setIgnoreCertErrors(true)                                    // 忽略证书错误
            .build();
            
        VideoDownloadManager.getInstance().initConfig(config);
    }
}
```

记得在 `AndroidManifest.xml` 中注册 Application：
```xml
<application
    android:name=".OrangePlayerApp"
    ...>
```

## 5. 集成到 SimpleDownloadManager

修改 `SimpleDownloadManager.java` 的 `startDownload()` 方法：

```java
public long startDownload(String url, String title, String description) {
    // 检测是否是 M3U8 格式
    if (url.toLowerCase().contains(".m3u8")) {
        // 使用 VideoDownloader 下载 M3U8
        downloadM3U8WithVideoDownloader(url, title);
        return -1;  // VideoDownloader 使用自己的任务 ID 系统
    }
    
    // 其他格式使用系统 DownloadManager
    // ... 原有代码 ...
}

private void downloadM3U8WithVideoDownloader(String url, String title) {
    File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS), "orangeplayer");
    if (!downloadDir.exists()) {
        downloadDir.mkdirs();
    }
    
    String fileName = (title != null && !title.isEmpty()) 
        ? title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4"
        : "video_" + System.currentTimeMillis() + ".mp4";
    
    File saveFile = new File(downloadDir, fileName);
    
    VideoDownloadManager.getInstance().startDownload(
        new VideoTaskItem(url, saveFile.getAbsolutePath()),
        new IDownloadListener() {
            @Override
            public void onDownloadPending(long itemId) {
                android.util.Log.d("SimpleDownloadManager", "M3U8 download pending: " + itemId);
            }
            
            @Override
            public void onDownloadPrepare(long itemId) {
                android.util.Log.d("SimpleDownloadManager", "M3U8 download prepare: " + itemId);
            }
            
            @Override
            public void onDownloadProgress(long itemId, long curSize, long totalSize, float percent) {
                android.util.Log.d("SimpleDownloadManager", 
                    "M3U8 download progress: " + percent + "% (" + curSize + "/" + totalSize + ")");
            }
            
            @Override
            public void onDownloadSpeed(long itemId, long speed) {
                android.util.Log.d("SimpleDownloadManager", "M3U8 download speed: " + speed + " bytes/s");
            }
            
            @Override
            public void onDownloadPause(long itemId) {
                android.util.Log.d("SimpleDownloadManager", "M3U8 download paused: " + itemId);
            }
            
            @Override
            public void onDownloadError(long itemId, int errorCode, String errorMsg) {
                android.util.Log.e("SimpleDownloadManager", 
                    "M3U8 download error: " + errorCode + " - " + errorMsg);
                Toast.makeText(mContext, "M3U8 下载失败: " + errorMsg, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onDownloadSuccess(long itemId, String filePath) {
                android.util.Log.d("SimpleDownloadManager", "M3U8 download success: " + filePath);
                Toast.makeText(mContext, "M3U8 视频下载完成\n保存位置: " + filePath, 
                    Toast.LENGTH_LONG).show();
            }
        }
    );
    
    Toast.makeText(mContext, "开始下载 M3U8 视频\n文件名: " + fileName, Toast.LENGTH_LONG).show();
}
```

## 6. 需要导入的类

```java
import com.jeffmony.downloader.VideoDownloadConfig;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.listener.IDownloadListener;
```

## 7. 权限要求

VideoDownloader 需要以下权限（已在 AndroidManifest.xml 中）：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 8. 功能特性

VideoDownloader 支持：
- ✅ M3U8 视频下载（自动解析和合并 TS 文件）
- ✅ MP4 视频下载
- ✅ 断点续传
- ✅ 下载队列管理
- ✅ 下载进度回调
- ✅ 下载速度监控
- ✅ 数据库持久化
- ✅ M3U8 转 MP4 合并

## 9. 测试步骤

1. 复制源码文件到项目
2. 编译项目（确保没有编译错误）
3. 创建 Application 类并初始化 VideoDownloader
4. 在 AndroidManifest.xml 中注册 Application
5. 修改 SimpleDownloadManager 集成 VideoDownloader API
6. 安装 APK 测试 M3U8 下载功能

## 10. 常见问题

### Q: 编译错误找不到类？
A: 确保已复制所有包目录，特别是 `com/jeffmony/` 下的所有子包。

### Q: 下载失败？
A: 检查网络权限、存储权限，查看 Logcat 日志中的错误信息。

### Q: M3U8 下载很慢？
A: 可以在 VideoDownloadConfig 中调整 `setConcurrentCount()` 增加并发数。

### Q: 下载的文件在哪里？
A: 默认保存在 `Downloads/orangeplayer/` 目录。

## 11. 下一步优化

- [ ] 添加下载列表界面（显示 M3U8 下载进度）
- [ ] 支持暂停/恢复 M3U8 下载
- [ ] 支持删除 M3U8 下载任务
- [ ] 添加下载完成通知
- [ ] 支持后台下载服务
