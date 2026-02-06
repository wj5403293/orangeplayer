package com.orange.player;

import android.app.Application;

/**
 * 应用程序类
 * 负责全局初始化
 */
public class OrangeApplication extends Application {
    
    private static final String TAG = "OrangeApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 注意：播放器核心的初始化不在这里进行
        // 因为 GSYVideoManager 需要 Activity Context
        // 播放核心会在 OrangevideoView 第一次初始化时设置
    }
}
