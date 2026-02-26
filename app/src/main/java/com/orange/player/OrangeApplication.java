package com.orange.player;

import android.app.Application;
import android.util.Log;

import com.orange.playerlibrary.OrangePlayerConfig;
import com.orange.playerlibrary.download.VideoDownloaderWrapper;
import com.orange.playerlibrary.utils.TvUtils;

/**
 * 应用程序类
 * 负责全局初始化
 */
public class OrangeApplication extends Application {
    
    private static final String TAG = "OrangeApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 自动检测 TV 模式
        boolean isTvDevice = TvUtils.isTvDevice(this);
        OrangePlayerConfig.setTvMode(isTvDevice);
        
        if (isTvDevice) {
            Log.d(TAG, "TV device detected, TV mode enabled");
        } else {
            Log.d(TAG, "Mobile/Tablet device detected, standard mode enabled");
        }
        
        // 初始化 VideoDownloader（用于 M3U8、MP4 等视频下载）
        try {
            VideoDownloaderWrapper.getInstance(this).init();
            Log.d(TAG, "VideoDownloader initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize VideoDownloader", e);
        }
        
        // 注意：播放器核心的初始化不在这里进行
        // 因为 GSYVideoManager 需要 Activity Context
        // 播放核心会在 OrangevideoView 第一次初始化时设置
    }
}

