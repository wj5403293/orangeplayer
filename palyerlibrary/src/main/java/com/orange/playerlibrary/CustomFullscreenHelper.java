package com.orange.playerlibrary;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 自定义全屏辅助类
 * 不使用 GSYVideoPlayer 的全屏机制，避免创建新的播放器实例
 */
public class CustomFullscreenHelper {
    
    private static final String TAG = "CustomFullscreenHelper";
    
    private OrangevideoView mVideoView;
    private ViewGroup mOriginalParent;
    private int mOriginalIndex;
    private FrameLayout.LayoutParams mOriginalLayoutParams;
    private ViewGroup mFullscreenContainer;
    
    // 保存播放状态
    private long mSavedPosition = 0;
    private boolean mWasPlaying = false;
    
    // 独立的全屏状态标志（不依赖 mOriginalParent）
    private boolean mIsFullscreen = false;
    
    // 需要在 onPrepared 后恢复的位置（用于处理 GSY 重新准备视频的情况）
    private long mPendingSeekPosition = 0;
    private boolean mPendingResume = false;
    
    public CustomFullscreenHelper(OrangevideoView videoView) {
        this.mVideoView = videoView;
    }
    
    /**
     * 获取待恢复的播放位置
     * 在 onPrepared 后调用此方法恢复位置
     */
    public long getPendingSeekPosition() {
        return mPendingSeekPosition;
    }
    
    /**
     * 清除待恢复的播放位置
     */
    public void clearPendingSeekPosition() {
        mPendingSeekPosition = 0;
        mPendingResume = false;
    }
    
    /**
     * 是否需要在 onPrepared 后恢复播放
     */
    public boolean isPendingResume() {
        return mPendingResume;
    }
    
    /**
     * 进入全屏
     */
    public void enterFullscreen(Activity activity) {
        if (activity == null || mVideoView == null) {
            android.util.Log.e(TAG, "enterFullscreen: activity or videoView is null");
            return;
        }
        
        // 如果已经是全屏状态，直接返回
        if (mIsFullscreen) {
            android.util.Log.d(TAG, "enterFullscreen: 已经是全屏状态，跳过");
            return;
        }
        
        android.util.Log.d(TAG, "enterFullscreen: 开始进入全屏");
        
        // 保存当前播放位置和状态
        mSavedPosition = mVideoView.getCurrentPositionWhenPlaying();
        mWasPlaying = mVideoView.isPlaying();
        android.util.Log.d(TAG, "enterFullscreen: 保存播放状态 position=" + mSavedPosition + ", wasPlaying=" + mWasPlaying);
        
        // 设置待恢复的位置（用于 onPrepared 后恢复）
        mPendingSeekPosition = mSavedPosition;
        mPendingResume = mWasPlaying;
        
        // 保存原始父容器和位置
        mOriginalParent = (ViewGroup) mVideoView.getParent();
        if (mOriginalParent != null) {
            mOriginalIndex = mOriginalParent.indexOfChild(mVideoView);
            // 保存原始布局参数（不强制转换类型）
            ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                mOriginalLayoutParams = (FrameLayout.LayoutParams) params;
            } else {
                // 如果不是 FrameLayout.LayoutParams，创建一个新的
                mOriginalLayoutParams = new FrameLayout.LayoutParams(
                    params.width,
                    params.height
                );
            }
            android.util.Log.d(TAG, "enterFullscreen: 保存原始位置 index=" + mOriginalIndex + ", layoutParams=" + params.getClass().getSimpleName());
        }
        
        // 获取 DecorView 作为全屏容器
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        mFullscreenContainer = decorView.findViewById(android.R.id.content);
        
        // 从原始父容器中移除
        if (mOriginalParent != null) {
            mOriginalParent.removeView(mVideoView);
            android.util.Log.d(TAG, "enterFullscreen: 从原始父容器移除");
        }
        
        // 添加到全屏容器
        FrameLayout.LayoutParams fullscreenParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        mFullscreenContainer.addView(mVideoView, fullscreenParams);
        android.util.Log.d(TAG, "enterFullscreen: 添加到全屏容器");
        
        // 设置全屏状态标志
        mIsFullscreen = true;
        
        // 旋转屏幕到横屏
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // 隐藏状态栏和导航栏
        hideSystemUI(activity);
        
        // 通知播放器进入全屏状态
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        // 立即恢复播放位置（不使用延迟，因为播放器实例没有改变）
        final long positionToRestore = mSavedPosition;
        final boolean shouldResume = mWasPlaying;
        
        // 使用 post 确保 View 已经添加到新容器
        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, "enterFullscreen: 恢复播放状态 position=" + positionToRestore + ", shouldResume=" + shouldResume);
                
                // 恢复播放位置
                if (positionToRestore > 0) {
                    android.util.Log.d(TAG, "enterFullscreen: 调用 seekTo(" + positionToRestore + ")");
                    mVideoView.seekTo(positionToRestore);
                    
                    // 再次确认 seek 是否成功
                    mVideoView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            long currentPos = mVideoView.getCurrentPositionWhenPlaying();
                            android.util.Log.d(TAG, "enterFullscreen: seek 后当前位置=" + currentPos);
                        }
                    }, 500);
                }
                
                // 如果之前在播放，确保继续播放
                if (shouldResume && !mVideoView.isPlaying()) {
                    android.util.Log.d(TAG, "enterFullscreen: 恢复播放");
                    mVideoView.resume();
                }
            }
        });
        
        // 显示全屏 UI 组件
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.VISIBLE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
        }
        
        android.util.Log.d(TAG, "enterFullscreen: 完成");
    }
    
    /**
     * 退出全屏
     */
    public void exitFullscreen(Activity activity) {
        if (activity == null || mVideoView == null) {
            android.util.Log.e(TAG, "exitFullscreen: activity or videoView is null");
            return;
        }
        
        // 如果不是全屏状态，直接返回
        if (!mIsFullscreen) {
            android.util.Log.d(TAG, "exitFullscreen: 不是全屏状态，跳过");
            return;
        }
        
        if (mOriginalParent == null) {
            android.util.Log.e(TAG, "exitFullscreen: originalParent is null");
            return;
        }
        
        android.util.Log.d(TAG, "exitFullscreen: 开始退出全屏");
        
        // 保存当前播放位置和状态（退出全屏前）
        mSavedPosition = mVideoView.getCurrentPositionWhenPlaying();
        mWasPlaying = mVideoView.isPlaying();
        android.util.Log.d(TAG, "exitFullscreen: 保存播放状态 position=" + mSavedPosition + ", wasPlaying=" + mWasPlaying);
        
        // 设置待恢复的位置（用于 onPrepared 后恢复）
        mPendingSeekPosition = mSavedPosition;
        mPendingResume = mWasPlaying;
        
        // 从全屏容器中移除
        if (mFullscreenContainer != null) {
            mFullscreenContainer.removeView(mVideoView);
            android.util.Log.d(TAG, "exitFullscreen: 从全屏容器移除");
        }
        
        // 恢复到原始父容器
        // 需要创建与原始父容器匹配的 LayoutParams
        ViewGroup.LayoutParams restoreParams;
        if (mOriginalLayoutParams != null) {
            // 尝试使用保存的参数
            restoreParams = mOriginalLayoutParams;
        } else {
            // 如果没有保存的参数，创建一个 MATCH_PARENT 的参数
            restoreParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
        
        // 根据原始父容器的类型创建正确的 LayoutParams
        if (mOriginalParent instanceof FrameLayout) {
            if (!(restoreParams instanceof FrameLayout.LayoutParams)) {
                FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                    restoreParams.width,
                    restoreParams.height
                );
                restoreParams = frameParams;
            }
        } else if (mOriginalParent instanceof android.widget.LinearLayout) {
            if (!(restoreParams instanceof android.widget.LinearLayout.LayoutParams)) {
                android.widget.LinearLayout.LayoutParams linearParams = new android.widget.LinearLayout.LayoutParams(
                    restoreParams.width,
                    restoreParams.height
                );
                restoreParams = linearParams;
            }
        } else if (mOriginalParent instanceof android.widget.RelativeLayout) {
            if (!(restoreParams instanceof android.widget.RelativeLayout.LayoutParams)) {
                android.widget.RelativeLayout.LayoutParams relativeParams = new android.widget.RelativeLayout.LayoutParams(
                    restoreParams.width,
                    restoreParams.height
                );
                restoreParams = relativeParams;
            }
        }
        
        mOriginalParent.addView(mVideoView, mOriginalIndex, restoreParams);
        android.util.Log.d(TAG, "exitFullscreen: 恢复到原始位置 index=" + mOriginalIndex + ", parentType=" + mOriginalParent.getClass().getSimpleName());
        
        // 清除全屏状态标志
        mIsFullscreen = false;
        
        // 旋转屏幕到竖屏
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // 显示状态栏和导航栏
        showSystemUI(activity);
        
        // 通知播放器退出全屏状态
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
        
        // 立即恢复播放位置（不使用延迟，因为播放器实例没有改变）
        final long positionToRestore = mSavedPosition;
        final boolean shouldResume = mWasPlaying;
        
        // 使用 post 确保 View 已经添加到原始容器
        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, "exitFullscreen: 恢复播放状态 position=" + positionToRestore + ", shouldResume=" + shouldResume);
                
                // 恢复播放位置
                if (positionToRestore > 0) {
                    android.util.Log.d(TAG, "exitFullscreen: 调用 seekTo(" + positionToRestore + ")");
                    mVideoView.seekTo(positionToRestore);
                    
                    // 再次确认 seek 是否成功
                    mVideoView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            long currentPos = mVideoView.getCurrentPositionWhenPlaying();
                            android.util.Log.d(TAG, "exitFullscreen: seek 后当前位置=" + currentPos);
                        }
                    }, 500);
                }
                
                // 如果之前在播放，确保继续播放
                if (shouldResume && !mVideoView.isPlaying()) {
                    android.util.Log.d(TAG, "exitFullscreen: 恢复播放");
                    mVideoView.resume();
                }
            }
        });
        
        // 隐藏全屏 UI 组件
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.GONE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
        }
        
        // 清理引用（但保留 mOriginalParent 以便下次使用）
        mOriginalParent = null;
        mOriginalLayoutParams = null;
        mFullscreenContainer = null;
        
        android.util.Log.d(TAG, "exitFullscreen: 完成");
    }
    
    /**
     * 隐藏系统 UI
     */
    private void hideSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        
        // 隐藏 ActionBar
        if (activity.getActionBar() != null) {
            activity.getActionBar().hide();
        }
        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar supportActionBar = 
                ((androidx.appcompat.app.AppCompatActivity) activity).getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.hide();
            }
        }
    }
    
    /**
     * 显示系统 UI
     */
    private void showSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        
        // 显示 ActionBar
        if (activity.getActionBar() != null) {
            activity.getActionBar().show();
        }
        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar supportActionBar = 
                ((androidx.appcompat.app.AppCompatActivity) activity).getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.show();
            }
        }
    }
    
    /**
     * 是否全屏
     */
    public boolean isFullscreen() {
        return mIsFullscreen;
    }
}
