package com.orange.playerlibrary;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * 自定义全屏辅助类 - 无旋转方案
 * 
 * 核心思路：
 * 1. 不调用 setRequestedOrientation，避免触发配置变化
 * 2. 将 OrangevideoView 移动到 DecorView，实现全屏效果
 * 3. 通过隐藏系统 UI 实现沉浸式全屏
 * 
 * 注意：这种方案下视频不会自动旋转，需要用户手动旋转设备
 * 或者使用 OrientationEventListener 监听设备方向
 */
public class CustomFullscreenHelper {
    
    private static final String TAG = "CustomFullscreenHelper";
    
    private OrangevideoView mVideoView;
    private boolean mIsFullscreen = false;
    private int mOriginalSystemUiVisibility = 0;
    
    // 保存原始布局参数和父容器
    private ViewGroup.LayoutParams mOriginalLayoutParams;
    private ViewGroup mOriginalParent;
    private int mOriginalIndex;
    
    // 全屏背景遮罩
    private View mFullscreenBackground;
    
    // 兼容旧接口
    private long mPendingSeekPosition = 0;
    private boolean mPendingResume = false;
    
    // 全屏切换中标志
    private boolean mFullscreenTransitioning = false;
    
    public CustomFullscreenHelper(OrangevideoView videoView) {
        this.mVideoView = videoView;
    }
    
    // 兼容旧接口的方法
    public long getPendingSeekPosition() {
        return mPendingSeekPosition;
    }
    
    public void clearPendingSeekPosition() {
        mPendingSeekPosition = 0;
        mPendingResume = false;
    }
    
    public boolean isPendingResume() {
        return mPendingResume;
    }
    
    /**
     * 进入全屏模式
     * 
     * 关键改进：先移动 View 到 DecorView，再旋转屏幕
     * 这样 View 已经在 DecorView 中，旋转时不会触发 Surface 销毁
     * 
     * SystemPlayerManager 特殊处理：暂停播放，切换后恢复
     */
    public void startFullScreen() {
        if (mIsFullscreen || mVideoView == null) {
            return;
        }
        
        Activity activity = mVideoView.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) {
            return;
        }
        
        // 检查是否使用 SystemPlayerManager
        final boolean isSystemPlayer = isUsingSystemPlayer();
        final boolean wasPlaying = mVideoView.isPlaying();
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        
        // SystemPlayerManager: 先暂停播放，避免 Surface 切换时出错
        if (isSystemPlayer && wasPlaying) {
            android.util.Log.d(TAG, "startFullScreen: SystemPlayerManager 检测到，先暂停播放");
            mVideoView.pause();
            mPendingSeekPosition = currentPosition;
            mPendingResume = true;
        }
        
        mIsFullscreen = true;
        mFullscreenTransitioning = true;
        mOriginalSystemUiVisibility = decorView.getSystemUiVisibility();
        
        // 延迟执行全屏切换，等待暂停完成
        final int delay = (isSystemPlayer && wasPlaying) ? 200 : 0;
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 1. 先保存原始父容器和布局参数
                mOriginalParent = (ViewGroup) mVideoView.getParent();
                if (mOriginalParent != null) {
                    mOriginalIndex = mOriginalParent.indexOfChild(mVideoView);
                    mOriginalLayoutParams = mVideoView.getLayoutParams();
                    // 从原始父容器移除
                    mOriginalParent.removeView(mVideoView);
                }
                
                // 2. 添加全屏黑色背景到 DecorView
                mFullscreenBackground = new View(activity);
                mFullscreenBackground.setBackgroundColor(Color.BLACK);
                FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                decorView.addView(mFullscreenBackground, bgParams);
                
                // 3. 将 OrangevideoView 添加到 DecorView
                FrameLayout.LayoutParams fullParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                decorView.addView(mVideoView, fullParams);
                
                // 4. 隐藏系统 UI
                hideSysBar(decorView, activity);
                
                // 5. 最后设置横屏 - View 已经在 DecorView 中，旋转不会影响它
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                
                // 6. 通知状态变化
                mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
                
                if (mVideoView.getTitleView() != null) {
                    mVideoView.getTitleView().setVisibility(View.VISIBLE);
                }
                if (mVideoView.getVodControlView() != null) {
                    mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                }
                
                // 7. 延迟重置标志，等待旋转完成
                mVideoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFullscreenTransitioning = false;
                        
                        // SystemPlayerManager: 恢复播放
                        if (isSystemPlayer && mPendingResume) {
                            android.util.Log.d(TAG, "startFullScreen: SystemPlayerManager 恢复播放, position=" + mPendingSeekPosition);
                            if (mPendingSeekPosition > 0) {
                                mVideoView.seekTo(mPendingSeekPosition);
                            }
                            mVideoView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mVideoView.resume();
                                    clearPendingSeekPosition();
                                }
                            }, 200);
                        }
                    }
                }, 800);
            }
        }, delay);
    }
    
    /**
     * 将播放器移动到全屏位置 - 已废弃，合并到 startFullScreen
     */
    private void moveToFullscreen(Activity activity, ViewGroup decorView) {
        // 已合并到 startFullScreen
    }
    
    /**
     * 退出全屏模式
     * 
     * 关键改进：先设置竖屏，等待旋转完成后再移动 View
     * SystemPlayerManager 特殊处理：暂停播放，切换后恢复
     */
    public void stopFullScreen() {
        if (!mIsFullscreen || mVideoView == null) {
            return;
        }
        
        Activity activity = mVideoView.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) {
            return;
        }
        
        // 检查是否使用 SystemPlayerManager
        final boolean isSystemPlayer = isUsingSystemPlayer();
        final boolean wasPlaying = mVideoView.isPlaying();
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        
        // SystemPlayerManager: 先暂停播放，避免 Surface 切换时出错
        if (isSystemPlayer && wasPlaying) {
            android.util.Log.d(TAG, "stopFullScreen: SystemPlayerManager 检测到，先暂停播放");
            mVideoView.pause();
            mPendingSeekPosition = currentPosition;
            mPendingResume = true;
        }
        
        mIsFullscreen = false;
        mFullscreenTransitioning = true;
        
        // 延迟执行退出全屏，等待暂停完成
        final int delay = (isSystemPlayer && wasPlaying) ? 200 : 0;
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 1. 显示系统 UI
                showSysBar(decorView, activity);
                
                // 2. 先设置竖屏
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                
                // 3. 从 DecorView 移除 OrangevideoView
                decorView.removeView(mVideoView);
                
                // 4. 移除全屏背景
                if (mFullscreenBackground != null) {
                    decorView.removeView(mFullscreenBackground);
                    mFullscreenBackground = null;
                }
                
                // 5. 恢复到原始父容器
                if (mOriginalParent != null && mOriginalLayoutParams != null) {
                    mOriginalParent.addView(mVideoView, mOriginalIndex, mOriginalLayoutParams);
                }
                
                // 6. 通知状态变化
                mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
                
                if (mVideoView.getTitleView() != null) {
                    mVideoView.getTitleView().setVisibility(View.GONE);
                }
                if (mVideoView.getVodControlView() != null) {
                    mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
                }
                
                // 7. 延迟重置标志
                mVideoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFullscreenTransitioning = false;
                        
                        // SystemPlayerManager: 恢复播放
                        if (isSystemPlayer && mPendingResume) {
                            android.util.Log.d(TAG, "stopFullScreen: SystemPlayerManager 恢复播放, position=" + mPendingSeekPosition);
                            if (mPendingSeekPosition > 0) {
                                mVideoView.seekTo(mPendingSeekPosition);
                            }
                            mVideoView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mVideoView.resume();
                                    clearPendingSeekPosition();
                                }
                            }, 200);
                        }
                    }
                }, 800);
            }
        }, delay);
    }
    
    public void enterFullscreen(Activity activity) {
        startFullScreen();
    }
    
    public void exitFullscreen(Activity activity) {
        stopFullScreen();
    }
    
    private void hideSysBar(ViewGroup decorView, Activity activity) {
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        
        decorView.setSystemUiVisibility(uiOptions);
        activity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
    }
    
    private void showSysBar(ViewGroup decorView, Activity activity) {
        decorView.setSystemUiVisibility(mOriginalSystemUiVisibility);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    
    public boolean isFullscreen() {
        return mIsFullscreen;
    }
    
    /**
     * 是否正在进行全屏切换
     */
    public boolean isFullscreenTransitioning() {
        return mFullscreenTransitioning;
    }
    
    /**
     * 是否竖屏全屏
     */
    public boolean isPortraitFullscreen() {
        return mIsPortraitFullscreen;
    }
    
    // 竖屏全屏标志
    private boolean mIsPortraitFullscreen = false;
    
    /**
     * 进入竖屏全屏模式
     * 不旋转屏幕，只是将播放器移动到 DecorView 并全屏显示
     */
    public void startPortraitFullScreen() {
        if (mIsFullscreen || mVideoView == null) {
            return;
        }
        
        Activity activity = mVideoView.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) {
            return;
        }
        
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        mIsFullscreen = true;
        mIsPortraitFullscreen = true;
        mFullscreenTransitioning = true;
        mOriginalSystemUiVisibility = decorView.getSystemUiVisibility();
        
        // 1. 保存原始父容器和布局参数
        mOriginalParent = (ViewGroup) mVideoView.getParent();
        if (mOriginalParent != null) {
            mOriginalIndex = mOriginalParent.indexOfChild(mVideoView);
            mOriginalLayoutParams = mVideoView.getLayoutParams();
            mOriginalParent.removeView(mVideoView);
        }
        
        // 2. 添加全屏黑色背景
        mFullscreenBackground = new View(activity);
        mFullscreenBackground.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        decorView.addView(mFullscreenBackground, bgParams);
        
        // 3. 将播放器添加到 DecorView
        FrameLayout.LayoutParams fullParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        decorView.addView(mVideoView, fullParams);
        
        // 4. 隐藏系统 UI
        hideSysBar(decorView, activity);
        
        // 5. 锁定竖屏方向（不旋转）
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // 6. 通知状态变化
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.VISIBLE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
        }
        
        // 7. 延迟重置标志
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFullscreenTransitioning = false;
            }
        }, 500);
    }
    
    /**
     * 退出竖屏全屏模式
     */
    public void stopPortraitFullScreen() {
        if (!mIsFullscreen || !mIsPortraitFullscreen || mVideoView == null) {
            return;
        }
        
        Activity activity = mVideoView.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) {
            return;
        }
        
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        mIsFullscreen = false;
        mIsPortraitFullscreen = false;
        mFullscreenTransitioning = true;
        
        // 1. 显示系统 UI
        showSysBar(decorView, activity);
        
        // 2. 从 DecorView 移除播放器
        decorView.removeView(mVideoView);
        
        // 3. 移除全屏背景
        if (mFullscreenBackground != null) {
            decorView.removeView(mFullscreenBackground);
            mFullscreenBackground = null;
        }
        
        // 4. 恢复到原始父容器
        if (mOriginalParent != null && mOriginalLayoutParams != null) {
            mOriginalParent.addView(mVideoView, mOriginalIndex, mOriginalLayoutParams);
        }
        
        // 5. 通知状态变化
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
        
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.GONE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
        }
        
        // 6. 延迟重置标志
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFullscreenTransitioning = false;
            }
        }, 500);
    }
    
    /**
     * 检查是否使用 SystemPlayerManager
     */
    private boolean isUsingSystemPlayer() {
        try {
            com.shuyu.gsyvideoplayer.player.IPlayerManager playerManager = 
                com.shuyu.gsyvideoplayer.GSYVideoManager.instance().getPlayer();
            if (playerManager != null) {
                String className = playerManager.getClass().getSimpleName();
                return "SystemPlayerManager".equals(className);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "isUsingSystemPlayer: 检查失败", e);
        }
        return false;
    }
    
    public void toggleFullScreen() {
        if (mIsFullscreen) {
            stopFullScreen();
        } else {
            startFullScreen();
        }
    }
    
    // 兼容旧接口
    public void initPlayerContainer() {
    }
    
    public FrameLayout getPlayerContainer() {
        return null;
    }
    
    public boolean isContainerInitialized() {
        return true;
    }
}
