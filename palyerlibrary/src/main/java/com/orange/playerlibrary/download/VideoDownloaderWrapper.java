package com.orange.playerlibrary.download;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.jeffmony.downloader.VideoDownloadConfig;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.listener.DownloadListener;
import com.jeffmony.downloader.model.VideoTaskItem;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VideoDownloader 包装类
 * 用于下载 M3U8、MP4、FLV 等视频并自动合并
 */
public class VideoDownloaderWrapper {
    
    private Context mContext;
    private static VideoDownloaderWrapper sInstance;
    private Map<String, DownloadCallback> mCallbackMap = new ConcurrentHashMap<>();
    private boolean mInitialized = false;
    
    private VideoDownloaderWrapper(Context context) {
        mContext = context.getApplicationContext();
    }
    
    public static VideoDownloaderWrapper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (VideoDownloaderWrapper.class) {
                if (sInstance == null) {
                    sInstance = new VideoDownloaderWrapper(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 初始化 VideoDownloader
     * 在 Application.onCreate() 中调用
     */
    public void init() {
        android.util.Log.d("VideoDownloaderWrapper", "========== init() START ==========");
        
        // 使用公共 Download 目录，不需要权限
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File cacheDir = new File(downloadDir, "orangeplayer");
        
        android.util.Log.d("VideoDownloaderWrapper", "Download directory: " + downloadDir.getAbsolutePath());
        android.util.Log.d("VideoDownloaderWrapper", "Cache directory: " + cacheDir.getAbsolutePath());
        android.util.Log.d("VideoDownloaderWrapper", "Download dir exists: " + downloadDir.exists());
        
        // 确保目录存在
        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            android.util.Log.d("VideoDownloaderWrapper", "Cache directory created: " + created);
            if (!created) {
                android.util.Log.e("VideoDownloaderWrapper", "FAILED to create cache directory!");
            }
        } else {
            android.util.Log.d("VideoDownloaderWrapper", "Cache directory already exists");
        }
        
        // 验证目录权限
        android.util.Log.d("VideoDownloaderWrapper", "Cache dir exists: " + cacheDir.exists());
        android.util.Log.d("VideoDownloaderWrapper", "Cache dir canWrite: " + cacheDir.canWrite());
        android.util.Log.d("VideoDownloaderWrapper", "Cache dir canRead: " + cacheDir.canRead());
        
        try {
            VideoDownloadConfig config = new VideoDownloadManager.Build(mContext)
                .setCacheRoot(cacheDir.getAbsolutePath())                    // 缓存目录：/storage/emulated/0/Download/orangeplayer
                .setTimeOut(30 * 1000, 30 * 1000)                           // 超时设置
                .setConcurrentCount(3)                                       // 并发下载数
                .setIgnoreCertErrors(true)                                   // 忽略证书错误
                .setShouldM3U8Merged(true)                                   // M3U8 自动合并为 MP4
                .buildConfig();
                
            android.util.Log.d("VideoDownloaderWrapper", "VideoDownloadConfig created");
            
            VideoDownloadManager manager = VideoDownloadManager.getInstance();
            android.util.Log.d("VideoDownloaderWrapper", "VideoDownloadManager instance: " + manager);
            
            manager.initConfig(config);
            android.util.Log.d("VideoDownloaderWrapper", "VideoDownloadManager.initConfig() called");
            
            // 验证配置
            VideoDownloadConfig verifyConfig = manager.downloadConfig();
            android.util.Log.d("VideoDownloaderWrapper", "Config verification:");
            android.util.Log.d("VideoDownloaderWrapper", "  CacheRoot: " + (verifyConfig != null ? verifyConfig.getCacheRoot() : "null"));
            android.util.Log.d("VideoDownloaderWrapper", "  ConcurrentCount: " + (verifyConfig != null ? verifyConfig.getConcurrentCount() : "null"));
            
            // 设置全局下载监听器
            manager.setGlobalDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadDefault(VideoTaskItem item) {
                    android.util.Log.d("VideoDownloaderWrapper", "Download default: " + item.getUrl());
                }
                
                @Override
                public void onDownloadPending(VideoTaskItem item) {
                    android.util.Log.d("VideoDownloaderWrapper", "Download pending: " + item.getUrl());
                    notifyCallback(item, 0, "等待下载...");
                }
                
                @Override
                public void onDownloadPrepare(VideoTaskItem item) {
                    android.util.Log.d("VideoDownloaderWrapper", "Download prepare: " + item.getUrl());
                    notifyCallback(item, 0, "准备下载...");
                }
                
                @Override
                public void onDownloadStart(VideoTaskItem item) {
                    android.util.Log.d("VideoDownloaderWrapper", "Download start: " + item.getUrl());
                    notifyCallback(item, 0, "开始下载...");
                }
                
                @Override
                public void onDownloadProgress(VideoTaskItem item) {
                    int progress = (int) item.getPercent();
                    String message = String.format("下载中: %.1f%% (%s/%s)", 
                        item.getPercent(),
                        SimpleDownloadManager.formatFileSize(item.getDownloadSize()),
                        SimpleDownloadManager.formatFileSize(item.getTotalSize()));
                    notifyCallback(item, progress, message);
                }
                
                @Override
                public void onDownloadSpeed(VideoTaskItem item) {
                    // 可选：显示下载速度
                }
                
                @Override
                public void onDownloadPause(VideoTaskItem item) {
                    android.util.Log.d("VideoDownloaderWrapper", "Download paused: " + item.getUrl());
                    notifyCallback(item, -1, "下载已暂停");
                }
                
                @Override
                public void onDownloadError(VideoTaskItem item) {
                    String errorMsg = "错误码: " + item.getErrorCode();
                    android.util.Log.e("VideoDownloaderWrapper", "========== Download ERROR ==========");
                    android.util.Log.e("VideoDownloaderWrapper", "Error: " + errorMsg);
                    android.util.Log.e("VideoDownloaderWrapper", "URL: " + item.getUrl());
                    android.util.Log.e("VideoDownloaderWrapper", "FinalUrl: " + item.getFinalUrl());
                    android.util.Log.e("VideoDownloaderWrapper", "FilePath: " + item.getFilePath());
                    android.util.Log.e("VideoDownloaderWrapper", "FileName: " + item.getFileName());
                    android.util.Log.e("VideoDownloaderWrapper", "SaveDir: " + item.getSaveDir());
                    android.util.Log.e("VideoDownloaderWrapper", "TotalSize: " + item.getTotalSize());
                    android.util.Log.e("VideoDownloaderWrapper", "DownloadSize: " + item.getDownloadSize());
                    android.util.Log.e("VideoDownloaderWrapper", "MimeType: " + item.getMimeType());
                    android.util.Log.e("VideoDownloaderWrapper", "VideoType: " + item.getVideoType());
                    
                    // 尝试直接访问 URL 测试连接
                    testUrlConnection(item.getUrl());
                    
                    notifyCallbackError(item, "下载失败: " + errorMsg);
                }
                
                @Override
                public void onDownloadSuccess(VideoTaskItem item) {
                    String filePath = item.getFilePath();
                    android.util.Log.d("VideoDownloaderWrapper", "========== Download SUCCESS ==========");
                    android.util.Log.d("VideoDownloaderWrapper", "FilePath: " + filePath);
                    notifyCallbackSuccess(item, filePath);
                }
            });
            
            mInitialized = true;
            android.util.Log.d("VideoDownloaderWrapper", "VideoDownloader initialized successfully");
            android.util.Log.d("VideoDownloaderWrapper", "========== init() END ==========");
        } catch (Exception e) {
            android.util.Log.e("VideoDownloaderWrapper", "Failed to initialize VideoDownloader", e);
            mInitialized = false;
        }
    }
    
    /**
     * 检查 VideoDownloader 是否可用
     */
    public boolean isAvailable() {
        try {
            Class.forName("com.jeffmony.downloader.VideoDownloadManager");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 下载视频（支持 M3U8、MP4、FLV 等所有格式）
     * 
     * @param url 视频 URL
     * @param title 视频标题
     * @param callback 下载回调
     */
    public void downloadM3U8(String url, String title, DownloadCallback callback) {
        android.util.Log.d("VideoDownloaderWrapper", "========== downloadM3U8() START ==========");
        android.util.Log.d("VideoDownloaderWrapper", "URL: " + url);
        android.util.Log.d("VideoDownloaderWrapper", "Title: " + title);
        android.util.Log.d("VideoDownloaderWrapper", "Initialized: " + mInitialized);
        
        if (!isAvailable()) {
            android.util.Log.e("VideoDownloaderWrapper", "VideoDownloader not available (class not found)");
            if (callback != null) {
                callback.onError("VideoDownloader 未初始化");
            }
            return;
        }
        
        if (!mInitialized) {
            android.util.Log.e("VideoDownloaderWrapper", "VideoDownloader not initialized! Calling init()...");
            init();
            if (!mInitialized) {
                android.util.Log.e("VideoDownloaderWrapper", "Failed to initialize VideoDownloader");
                if (callback != null) {
                    callback.onError("VideoDownloader 初始化失败");
                }
                return;
            }
        }
        
        // 检查 VideoDownloadManager 实例
        VideoDownloadManager manager = VideoDownloadManager.getInstance();
        if (manager == null) {
            android.util.Log.e("VideoDownloaderWrapper", "VideoDownloadManager instance is null!");
            if (callback != null) {
                callback.onError("VideoDownloadManager 未初始化");
            }
            return;
        }
        android.util.Log.d("VideoDownloaderWrapper", "VideoDownloadManager instance OK");
        
        String fileName = (title != null && !title.isEmpty()) 
            ? title.replaceAll("[\\\\/:*?\"<>|]", "_")
            : "video_" + System.currentTimeMillis();
        
        android.util.Log.d("VideoDownloaderWrapper", "FileName: " + fileName);
        
        // 创建下载任务
        // 构造函数参数：(url, coverUrl, title, groupName)
        VideoTaskItem taskItem = new VideoTaskItem(url, "", title, "orangeplayer");
        // 单独设置文件名
        taskItem.setFileName(fileName);
        
        android.util.Log.d("VideoDownloaderWrapper", "VideoTaskItem created:");
        android.util.Log.d("VideoDownloaderWrapper", "  URL: " + taskItem.getUrl());
        android.util.Log.d("VideoDownloaderWrapper", "  Title: " + taskItem.getTitle());
        android.util.Log.d("VideoDownloaderWrapper", "  FileName: " + taskItem.getFileName());
        android.util.Log.d("VideoDownloaderWrapper", "  GroupName: " + taskItem.getGroupName());
        android.util.Log.d("VideoDownloaderWrapper", "  SaveDir: " + taskItem.getSaveDir());
        
        // 保存回调
        if (callback != null) {
            mCallbackMap.put(url, callback);
            android.util.Log.d("VideoDownloaderWrapper", "Callback registered for URL");
        }
        
        // 开始下载
        try {
            android.util.Log.d("VideoDownloaderWrapper", "Calling VideoDownloadManager.startDownload()...");
            manager.startDownload(taskItem);
            android.util.Log.d("VideoDownloaderWrapper", "startDownload() called successfully");
            android.util.Log.d("VideoDownloaderWrapper", "========== downloadM3U8() END ==========");
            
            Toast.makeText(mContext, "开始下载视频\n文件名: " + fileName, 
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("VideoDownloaderWrapper", "Exception in startDownload()", e);
            if (callback != null) {
                callback.onError("启动下载失败: " + e.getMessage());
            }
        }
    }
    
    private void notifyCallback(VideoTaskItem item, int progress, String message) {
        DownloadCallback callback = mCallbackMap.get(item.getUrl());
        if (callback != null) {
            callback.onProgress(progress, message);
        }
    }
    
    private void notifyCallbackError(VideoTaskItem item, String error) {
        DownloadCallback callback = mCallbackMap.get(item.getUrl());
        if (callback != null) {
            callback.onError(error);
            mCallbackMap.remove(item.getUrl());
        }
    }
    
    private void notifyCallbackSuccess(VideoTaskItem item, String filePath) {
        DownloadCallback callback = mCallbackMap.get(item.getUrl());
        if (callback != null) {
            callback.onSuccess(filePath);
            mCallbackMap.remove(item.getUrl());
        }
    }
    
    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onProgress(int progress, String message);
        void onSuccess(String filePath);
        void onError(String error);
    }
    
    /**
     * 测试 URL 连接（用于诊断）
     */
    private void testUrlConnection(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.net.URL testUrl = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) testUrl.openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    
                    int responseCode = conn.getResponseCode();
                    long contentLength = conn.getContentLengthLong();
                    String contentType = conn.getContentType();
                    
                    android.util.Log.d("VideoDownloaderWrapper", "========== URL Test Result ==========");
                    android.util.Log.d("VideoDownloaderWrapper", "Response code: " + responseCode);
                    android.util.Log.d("VideoDownloaderWrapper", "Content-Length: " + contentLength);
                    android.util.Log.d("VideoDownloaderWrapper", "Content-Type: " + contentType);
                    
                    conn.disconnect();
                } catch (Exception e) {
                    android.util.Log.e("VideoDownloaderWrapper", "URL test failed", e);
                }
            }
        }).start();
    }
}
