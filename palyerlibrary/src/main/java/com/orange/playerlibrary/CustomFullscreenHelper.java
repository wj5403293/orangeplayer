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
    
    private OrangevideoView mVideoView;
    private ViewGroup mOriginalParent;
    private int mOriginalIndex;
    private FrameLayout.LayoutParams mOriginalLayoutParams;
    private ViewGroup mFullscreenContainer;
    
    private long mSavedPosition = 0;
    private boolean mWasPlaying = false;
    private boolean mIsFullscreen = false;
    
    // 需要在 onPrepared 后恢复的位置
    private long mPendingSeekPosition = 0;
    private boolean mPendingResume = false;
    
    public CustomFullscreenHelper(OrangevideoView videoView) {
        this.mVideoView = videoView;
    }
    
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
    
    public void enterFullscreen(Activity activity) {
        if (activity == null || mVideoView == null || mIsFullscreen) {
            return;
        }
        
        // 保存当前播放位置和状态
        mSavedPosition = mVideoView.getCurrentPositionWhenPlaying();
        mWasPlaying = mVideoView.isPlaying();
        mPendingSeekPosition = mSavedPosition;
        mPendingResume = mWasPlaying;
        
        // 保存原始父容器和位置
        mOriginalParent = (ViewGroup) mVideoView.getParent();
        if (mOriginalParent != null) {
            mOriginalIndex = mOriginalParent.indexOfChild(mVideoView);
            ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                mOriginalLayoutParams = (FrameLayout.LayoutParams) params;
            } else {
                mOriginalLayoutParams = new FrameLayout.LayoutParams(params.width, params.height);
            }
        }
        
        // 获取全屏容器
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        mFullscreenContainer = decorView.findViewById(android.R.id.content);
        
        // 从原始父容器中移除
        if (mOriginalParent != null) {
            mOriginalParent.removeView(mVideoView);
        }
        
        // 添加到全屏容器
        FrameLayout.LayoutParams fullscreenParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        mFullscreenContainer.addView(mVideoView, fullscreenParams);
        
        mIsFullscreen = true;
        
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hideSystemUI(activity);
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        // 恢复播放位置
        final long positionToRestore = mSavedPosition;
        final boolean shouldResume = mWasPlaying;
        
        mVideoView.post(() -> {
            if (positionToRestore > 0) {
                mVideoView.seekTo(positionToRestore);
            }
            if (shouldResume && !mVideoView.isPlaying()) {
                mVideoView.resume();
            }
        });
        
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.VISIBLE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
        }
        
        // 确保全屏模式下事件绑定正常 (Requirements: 2.1, 2.2, 2.3)
        ensureFullscreenEventBinding();
    }
    
    /**
     * 确保全屏模式下的事件绑定
     * Requirements: 2.1, 2.2, 2.3
     */
    private void ensureFullscreenEventBinding() {
        if (mVideoView == null) {
            return;
        }
        
        OrangeVideoController controller = mVideoView.getVideoController();
        if (controller == null) {
            android.util.Log.w("CustomFullscreenHelper", "ensureFullscreenEventBinding: controller is null");
            return;
        }
        
        VideoEventManager eventManager = controller.getVideoEventManager();
        if (eventManager == null) {
            android.util.Log.w("CustomFullscreenHelper", "ensureFullscreenEventBinding: eventManager is null");
            return;
        }
        
        // 重新绑定 VodControlView 事件，确保全屏模式下按钮可用
        com.orange.playerlibrary.component.VodControlView vodControlView = mVideoView.getVodControlView();
        if (vodControlView != null) {
            android.util.Log.d("CustomFullscreenHelper", "ensureFullscreenEventBinding: binding VodControlView events for fullscreen");
            eventManager.bindControllerComponents(vodControlView);
        }
        
        // 重新绑定 TitleView 事件
        com.orange.playerlibrary.component.TitleView titleView = mVideoView.getTitleView();
        if (titleView != null) {
            android.util.Log.d("CustomFullscreenHelper", "ensureFullscreenEventBinding: binding TitleView events for fullscreen");
            eventManager.bindTitleView(titleView);
        }
    }
    
    public void exitFullscreen(Activity activity) {
        if (activity == null || mVideoView == null || !mIsFullscreen || mOriginalParent == null) {
            return;
        }
        
        // 保存当前播放位置和状态
        mSavedPosition = mVideoView.getCurrentPositionWhenPlaying();
        mWasPlaying = mVideoView.isPlaying();
        mPendingSeekPosition = mSavedPosition;
        mPendingResume = mWasPlaying;
        
        // 从全屏容器中移除
        if (mFullscreenContainer != null) {
            mFullscreenContainer.removeView(mVideoView);
        }
        
        // 恢复到原始父容器
        ViewGroup.LayoutParams restoreParams = mOriginalLayoutParams != null ? 
            mOriginalLayoutParams : new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
        
        // 根据原始父容器的类型创建正确的 LayoutParams
        if (mOriginalParent instanceof FrameLayout && !(restoreParams instanceof FrameLayout.LayoutParams)) {
            restoreParams = new FrameLayout.LayoutParams(restoreParams.width, restoreParams.height);
        } else if (mOriginalParent instanceof android.widget.LinearLayout && 
                   !(restoreParams instanceof android.widget.LinearLayout.LayoutParams)) {
            restoreParams = new android.widget.LinearLayout.LayoutParams(restoreParams.width, restoreParams.height);
        } else if (mOriginalParent instanceof android.widget.RelativeLayout && 
                   !(restoreParams instanceof android.widget.RelativeLayout.LayoutParams)) {
            restoreParams = new android.widget.RelativeLayout.LayoutParams(restoreParams.width, restoreParams.height);
        }
        
        mOriginalParent.addView(mVideoView, mOriginalIndex, restoreParams);
        
        mIsFullscreen = false;
        
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        showSystemUI(activity);
        mVideoView.setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
        
        // 恢复播放位置
        final long positionToRestore = mSavedPosition;
        final boolean shouldResume = mWasPlaying;
        
        mVideoView.post(() -> {
            if (positionToRestore > 0) {
                mVideoView.seekTo(positionToRestore);
            }
            if (shouldResume && !mVideoView.isPlaying()) {
                mVideoView.resume();
            }
        });
        
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setVisibility(View.GONE);
        }
        if (mVideoView.getVodControlView() != null) {
            mVideoView.getVodControlView().onPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
        }
        
        // 确保退出全屏后原播放器事件正常 (Requirements: 2.4)
        ensureNormalModeEventBinding();
        
        mOriginalParent = null;
        mOriginalLayoutParams = null;
        mFullscreenContainer = null;
    }
    
    /**
     * 确保退出全屏后原播放器的事件绑定正常
     * Requirements: 2.4
     */
    private void ensureNormalModeEventBinding() {
        if (mVideoView == null) {
            return;
        }
        
        OrangeVideoController controller = mVideoView.getVideoController();
        if (controller == null) {
            android.util.Log.w("CustomFullscreenHelper", "ensureNormalModeEventBinding: controller is null");
            return;
        }
        
        VideoEventManager eventManager = controller.getVideoEventManager();
        if (eventManager == null) {
            android.util.Log.w("CustomFullscreenHelper", "ensureNormalModeEventBinding: eventManager is null");
            return;
        }
        
        // 重新绑定 VodControlView 事件，确保退出全屏后按钮可用
        com.orange.playerlibrary.component.VodControlView vodControlView = mVideoView.getVodControlView();
        if (vodControlView != null) {
            android.util.Log.d("CustomFullscreenHelper", "ensureNormalModeEventBinding: binding VodControlView events after exit fullscreen");
            eventManager.bindControllerComponents(vodControlView);
        }
        
        // 重新绑定 TitleView 事件
        com.orange.playerlibrary.component.TitleView titleView = mVideoView.getTitleView();
        if (titleView != null) {
            android.util.Log.d("CustomFullscreenHelper", "ensureNormalModeEventBinding: binding TitleView events after exit fullscreen");
            eventManager.bindTitleView(titleView);
        }
    }
    
    private void hideSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        
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
    
    private void showSystemUI(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        
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
    
    public boolean isFullscreen() {
        return mIsFullscreen;
    }
}
