package com.orange.playerlibrary.download;

import android.content.Context;
import android.widget.Toast;

/**
 * 简单的下载管理器 - 使用 VideoDownloader 统一处理所有格式
 * 
 * 支持格式：
 * - M3U8 视频（自动解析和合并 TS 文件为 MP4）
 * - MP4 视频
 * - FLV 视频
 * - 其他常见视频格式
 * 
 * 优点：
 * 1. 统一的下载接口
 * 2. 自动识别视频格式
 * 3. 支持断点续传
 * 4. 支持下载队列管理
 * 5. M3U8 自动合并为 MP4
 * 6. 应用退出后继续下载
 */
public class SimpleDownloadManager {
    
    private Context mContext;
    private VideoDownloaderWrapper mVideoDownloader;
    
    public SimpleDownloadManager(Context context) {
        mContext = context.getApplicationContext();
        mVideoDownloader = VideoDownloaderWrapper.getInstance(mContext);
    }
    
    /**
     * 开始下载（支持所有格式：M3U8、MP4、FLV 等）
     * @param url 下载地址
     * @param title 视频标题
     * @param description 描述（暂未使用）
     * @return 下载任务 ID（-1 表示使用 VideoDownloader）
     */
    public long startDownload(String url, String title, String description) {
        android.util.Log.d("SimpleDownloadManager", "startDownload() called");
        android.util.Log.d("SimpleDownloadManager", "URL: " + url);
        android.util.Log.d("SimpleDownloadManager", "Title: " + title);
        
        if (url == null || url.isEmpty()) {
            android.util.Log.e("SimpleDownloadManager", "URL is null or empty");
            Toast.makeText(mContext, "下载地址为空", Toast.LENGTH_SHORT).show();
            return -1;
        }
        
        // 使用 VideoDownloader 统一处理所有格式
        // VideoDownloader 会自动识别视频类型（M3U8、MP4、FLV 等）
        mVideoDownloader.downloadM3U8(url, title, 
            new VideoDownloaderWrapper.DownloadCallback() {
                @Override
                public void onProgress(int progress, String message) {
                    android.util.Log.d("SimpleDownloadManager", "Download progress: " + message);
                    // 可以在这里更新 UI 或通知栏
                }
                
                @Override
                public void onSuccess(String filePath) {
                    android.util.Log.d("SimpleDownloadManager", "Download success: " + filePath);
                    Toast.makeText(mContext, "视频下载完成\n保存位置: " + filePath, 
                        Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("SimpleDownloadManager", "Download error: " + error);
                    Toast.makeText(mContext, "下载失败: " + error, Toast.LENGTH_LONG).show();
                }
            });
        
        return -1;  // VideoDownloader 使用自己的任务 ID 系统
    }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // VideoDownloader 的清理由 VideoDownloaderWrapper 管理
    }
}
