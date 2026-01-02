package com.orange.playerlibrary;

import com.shuyu.gsyvideoplayer.utils.GSYVideoType;

/**
 * 视频比例管理器
 * 负责视频比例的应用和管理
 * 
 * Requirements: 1.1, 1.2
 */
public class VideoScaleManager {
    
    private static final String TAG = "VideoScaleManager";
    
    private final OrangevideoView mVideoView;
    private final PlayerSettingsManager mSettingsManager;
    
    /**
     * 构造函数
     * 
     * @param videoView 视频播放器视图
     * @param settingsManager 设置管理器
     */
    public VideoScaleManager(OrangevideoView videoView, PlayerSettingsManager settingsManager) {
        mVideoView = videoView;
        mSettingsManager = settingsManager;
    }
    
    /**
     * 应用保存的视频比例设置
     * 从 PlayerSettingsManager 读取保存的视频比例，并应用到播放器
     * 
     * Requirements: 1.1, 1.2
     */
    public void applyVideoScale() {
        String scale = mSettingsManager.getVideoScale();
        android.util.Log.d(TAG, "applyVideoScale: 应用视频比例 = " + scale);
        applyScaleType(scale);
    }
    
    /**
     * 根据比例类型设置视频显示模式
     * 
     * @param scaleType 比例类型（默认、16:9、4:3、全屏裁剪、全屏拉伸）
     * 
     * Requirements: 1.2, 1.3
     */
    public void applyScaleType(String scaleType) {
        if (scaleType == null || scaleType.isEmpty()) {
            scaleType = "默认";
        }
        
        android.util.Log.d(TAG, "applyScaleType: 设置视频比例类型 = " + scaleType);
        
        switch (scaleType) {
            case "默认":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
                android.util.Log.d(TAG, "applyScaleType: 应用默认比例");
                break;
            case "16:9":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_16_9);
                android.util.Log.d(TAG, "applyScaleType: 应用 16:9 比例");
                break;
            case "4:3":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_4_3);
                android.util.Log.d(TAG, "applyScaleType: 应用 4:3 比例");
                break;
            case "全屏裁剪":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_FULL);
                android.util.Log.d(TAG, "applyScaleType: 应用全屏裁剪");
                break;
            case "全屏拉伸":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL);
                android.util.Log.d(TAG, "applyScaleType: 应用全屏拉伸");
                break;
            default:
                android.util.Log.w(TAG, "applyScaleType: 未知的比例类型 = " + scaleType + ", 使用默认比例");
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
                break;
        }
        
        // 刷新视频显示
        mVideoView.refreshVideoShowType();
    }
    
    /**
     * 获取当前视频比例设置
     * 
     * @return 当前视频比例
     */
    public String getCurrentScale() {
        return mSettingsManager.getVideoScale();
    }
    
    /**
     * 设置并保存视频比例
     * 
     * @param scaleType 比例类型
     * 
     * Requirements: 1.1
     */
    public void setAndSaveScale(String scaleType) {
        android.util.Log.d(TAG, "setAndSaveScale: 保存并应用视频比例 = " + scaleType);
        mSettingsManager.setVideoScale(scaleType);
        applyScaleType(scaleType);
    }
}
