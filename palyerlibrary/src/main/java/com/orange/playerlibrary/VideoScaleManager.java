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
        
        android.util.Log.d(TAG, "========================================");
        android.util.Log.d(TAG, "applyScaleType: scaleType=" + scaleType);
        android.util.Log.d(TAG, "applyScaleType: mVideoView size=" + mVideoView.getWidth() + "x" + mVideoView.getHeight());
        android.util.Log.d(TAG, "applyScaleType: mVideoView isAttachedToWindow=" + (mVideoView.getWindowToken() != null));
        if (mVideoView.getRenderProxy() != null) {
            android.util.Log.d(TAG, "applyScaleType: RenderProxy size=" + 
                mVideoView.getRenderProxy().getWidth() + "x" + mVideoView.getRenderProxy().getHeight());
        }
        
        switch (scaleType) {
            case "默认":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
                break;
            case "16:9":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_16_9);
                break;
            case "4:3":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_4_3);
                break;
            case "全屏裁剪":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_FULL);
                break;
            case "全屏拉伸":
                GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL);
                break;
            default:
                GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
                break;
        }
        
        android.util.Log.d(TAG, "applyScaleType: GSYVideoType.getShowType()=" + GSYVideoType.getShowType());
        
        // 在主线程刷新视频显示
        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, "applyScaleType: 调用 refreshVideoShowType()");
                
                // 关键修复：先强制 RenderProxy 重新测量布局
                if (mVideoView.getRenderProxy() != null) {
                    mVideoView.getRenderProxy().requestLayout();
                }
                
                mVideoView.refreshVideoShowType();
                
                android.util.Log.d(TAG, "applyScaleType: refreshVideoShowType() 后 - mVideoView size=" + 
                    mVideoView.getWidth() + "x" + mVideoView.getHeight());
                if (mVideoView.getRenderProxy() != null) {
                    android.util.Log.d(TAG, "applyScaleType: refreshVideoShowType() 后 - RenderProxy size=" + 
                        mVideoView.getRenderProxy().getWidth() + "x" + mVideoView.getRenderProxy().getHeight());
                }
                
                // ExoPlayer 全屏时需要更新 SurfaceControl 尺寸
                // 延迟执行，等待布局完成
                mVideoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        android.util.Log.d(TAG, "applyScaleType: 调用 updateExoSurfaceControlIfNeeded()");
                        mVideoView.updateExoSurfaceControlIfNeeded();
                        
                        android.util.Log.d(TAG, "applyScaleType: updateExoSurfaceControlIfNeeded() 后 - mVideoView size=" + 
                            mVideoView.getWidth() + "x" + mVideoView.getHeight());
                        if (mVideoView.getRenderProxy() != null) {
                            android.util.Log.d(TAG, "applyScaleType: updateExoSurfaceControlIfNeeded() 后 - RenderProxy size=" + 
                                mVideoView.getRenderProxy().getWidth() + "x" + mVideoView.getRenderProxy().getHeight());
                        }
                        
                        // 再次强制刷新，确保尺寸正确
                        mVideoView.refreshVideoShowType();
                        
                        android.util.Log.d(TAG, "========================================");
                    }
                }, 200);  // 增加延迟到 200ms
            }
        });
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
        mSettingsManager.setVideoScale(scaleType);
        applyScaleType(scaleType);
    }
}
