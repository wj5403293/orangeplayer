package com.orange.playerlibrary;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;

import com.orange.playerlibrary.interfaces.OnPlayCompleteListener;
import com.orange.playerlibrary.interfaces.OnProgressListener;
import com.orange.playerlibrary.interfaces.OnStateChangeListener;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener;
import com.shuyu.gsyvideoplayer.player.IPlayerManager;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.player.SystemPlayerManager;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrangevideoView extends GSYBaseVideoPlayer {

    private static final String TAG = "OrangevideoView";
    
    public static final int STATE_STARTSNIFFING = PlayerConstants.STATE_STARTSNIFFING;
    public static final int STATE_ENDSNIFFING = PlayerConstants.STATE_ENDSNIFFING;
    
    public static OrangeSharedSqlite sqlite;
    
    private String mVideoUrl;
    private Map<String, String> mVideoHeaders;
    private static float sSpeed = 1.0f;
    private static float sLongSpeed = 3.0f;
    private boolean mKeepVideoPlaying = false;
    private boolean mAutoThumbnailEnabled = true;
    private Object mDefaultThumbnail = null;
    private boolean mIsLiveVideo = false;
    private boolean mIsSniffing = false;
    private boolean mAutoRotateOnFullscreen = true;
    
    private SkipManager mSkipManager;
    private VideoScaleManager mVideoScaleManager;
    private PlaybackStateManager mPlaybackStateManager;
    private ComponentStateManager mComponentStateManager;
    private ErrorRecoveryManager mErrorRecoveryManager;
    private CustomFullscreenHelper mFullscreenHelper;
    
    private com.orange.playerlibrary.interfaces.ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    
    private com.orange.playerlibrary.component.PrepareView mPrepareView;
    private com.orange.playerlibrary.component.TitleView mTitleView;
    private com.orange.playerlibrary.component.VodControlView mVodControlView;
    private com.orange.playerlibrary.component.LiveControlView mLiveControlView;
    private com.orange.playerlibrary.component.CompleteView mCompleteView;
    private com.orange.playerlibrary.component.ErrorView mErrorView;
    private boolean mUseOrangeComponents = true;
    
    private final List<OnStateChangeListener> mStateChangeListeners = new ArrayList<>();
    private OnProgressListener mProgressListener;
    private OnPlayCompleteListener mPlayCompleteListener;
    
    private int mCurrentPlayState = PlayerConstants.STATE_IDLE;
    private int mCurrentPlayerState = PlayerConstants.PLAYER_NORMAL;
    
    private boolean mDebug = false;
    private boolean mEnteringPiPMode = false;
    
    private DebugLogCallback mDebugLogCallback;

    public interface DebugLogCallback {
        void onLog(String msg);
    }

    public OrangevideoView(Context context) {
        super(context);
    }

    public OrangevideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrangevideoView(Context context, boolean fullFlag) {
        super(context, fullFlag);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        initOrangePlayer();
    }

    private void initOrangePlayer() {
        mUseOrangeComponents = true;
        
        mSkipManager = new SkipManager();
        mSkipManager.attachVideoView(this);
        
        mVideoScaleManager = new VideoScaleManager(this, PlayerSettingsManager.getInstance(getContext()));
        mPlaybackStateManager = new PlaybackStateManager();
        mComponentStateManager = new ComponentStateManager();
        
        mErrorRecoveryManager = new ErrorRecoveryManager();
        mErrorRecoveryManager.attachVideoView(this);
        
        mFullscreenHelper = new CustomFullscreenHelper(this);
        
        setShowFullAnimation(false);
        setRotateViewAuto(false);
        setNeedLockFull(false);
        setLockLand(false);
        setRotateWithSystem(false);
        setNeedShowWifiTip(false);
        setNeedOrientationUtils(false);
        setIsTouchWiget(true);
        setIsTouchWigetFull(true);
        
        if (mUseOrangeComponents) {
            initOrangeComponents();
        }
        
        if (mComponentStateManager != null) {
            mComponentStateManager.reregisterProgressListener(this);
        }
        
        setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                android.util.Log.d(TAG, "=== onPrepared callback ===");
                setOrangePlayState(PlayerConstants.STATE_PREPARED);
                if (getDuration() <= 0) {
                    mIsLiveVideo = true;
                }
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                }
                
                if (mFullscreenHelper != null && mFullscreenHelper.getPendingSeekPosition() > 0) {
                    final long pendingPosition = mFullscreenHelper.getPendingSeekPosition();
                    final boolean pendingResume = mFullscreenHelper.isPendingResume();
                    mFullscreenHelper.clearPendingSeekPosition();
                    
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            seekTo(pendingPosition);
                            if (pendingResume && !isPlaying()) {
                                resume();
                            }
                        }
                    }, 100);
                } else {
                    if (mKeepVideoPlaying) {
                        restorePlaybackProgress();
                    }
                }
                if (mSkipManager != null) {
                    mSkipManager.performSkipIntro();
                }
                setOrangePlayState(PlayerConstants.STATE_PLAYING);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PLAYBACK_COMPLETED);
                if (mKeepVideoPlaying) {
                    clearSavedProgress();
                }
                if (mSkipManager != null) {
                    mSkipManager.reset();
                }
                if (mPlayCompleteListener != null) {
                    mPlayCompleteListener.onPlayComplete();
                }
            }

            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                setOrangePlayState(PlayerConstants.STATE_ERROR);
            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {
                super.onEnterFullscreen(url, objects);
                setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
                if (mAutoRotateOnFullscreen) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                }
            }
        });
    }

    public void setDebugLogCallback(DebugLogCallback callback) {
        mDebugLogCallback = callback;
    }

    public void enableOrangeComponents() {
        if (mUseOrangeComponents) return;
        mUseOrangeComponents = true;
        initOrangeComponents();
    }

    private void initOrangeComponents() {
        Context context = getContext();
        android.widget.RelativeLayout.LayoutParams matchParentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);

        mControlWrapper = createControlWrapper();
        
        mPrepareView = new com.orange.playerlibrary.component.PrepareView(context);
        mPrepareView.attach(mControlWrapper);
        mPrepareView.setClickStart();
        addView(mPrepareView, matchParentParams);
        
        mCompleteView = new com.orange.playerlibrary.component.CompleteView(context);
        mCompleteView.attach(mControlWrapper);
        addView(mCompleteView, matchParentParams);

        mErrorView = new com.orange.playerlibrary.component.ErrorView(context);
        mErrorView.attach(mControlWrapper);
        addView(mErrorView, matchParentParams);

        mTitleView = new com.orange.playerlibrary.component.TitleView(context);
        mTitleView.attach(mControlWrapper);
        addView(mTitleView, matchParentParams);

        mVodControlView = new com.orange.playerlibrary.component.VodControlView(context);
        if (mOrangeController != null) {
            mVodControlView.setOrangeVideoController(mOrangeController);
        }
        mVodControlView.attach(mControlWrapper);
        addView(mVodControlView, matchParentParams);
        
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        ensureEventBinding();
    }

    public void debugLog(String msg) {
        if (mDebugLogCallback != null) {
            mDebugLogCallback.onLog(msg);
        }
    }

    private void ensureEventBinding() {
        if (mOrangeController == null) {
            return;
        }
        
        VideoEventManager eventManager = mOrangeController.getVideoEventManager();
        if (eventManager == null) {
            return;
        }
        
        if (mVodControlView != null) {
            eventManager.bindControllerComponents(mVodControlView);
        }
        
        if (mTitleView != null) {
            eventManager.bindTitleView(mTitleView);
        }
    }

    private com.orange.playerlibrary.interfaces.ControlWrapper createControlWrapper() {
        final OrangevideoView videoView = this;
        return new com.orange.playerlibrary.interfaces.ControlWrapper() {
            @Override
            public void start() {
                // 调用 start() 方法，它会设置 STATE_PREPARING 状态
                videoView.start();
            }

            @Override
            public void pause() {
                videoView.pause();
            }

            @Override
            public void seekTo(long position) {
                videoView.seekTo(position);
            }

            @Override
            public long getDuration() {
                return videoView.getDuration();
            }

            @Override
            public long getCurrentPosition() {
                return videoView.getCurrentPositionWhenPlaying();
            }

            @Override
            public boolean isPlaying() {
                return videoView.isPlaying();
            }

            @Override
            public void togglePlay() {
                if (isPlaying()) {
                    pause();
                } else {
                    videoView.resume();
                }
            }

            @Override
            public void toggleFullScreen() {
                if (isFullScreen()) {
                    Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.exitFullscreen(activity);
                    }
                } else {
                    Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.enterFullscreen(activity);
                    }
                }
            }

            @Override
            public void toggleLockState() {}

            @Override
            public boolean isFullScreen() {
                return mFullscreenHelper != null && mFullscreenHelper.isFullscreen();
            }

            @Override
            public boolean isLocked() {
                return false;
            }

            @Override
            public void setSpeed(float speed) {
                videoView.setSpeed(speed);
            }

            @Override
            public float getSpeed() {
                return videoView.getSpeed();
            }

            @Override
            public int getBufferedPercentage() {
                return videoView.getBuffterPoint();
            }

            @Override
            public void setMute(boolean isMute) {}

            @Override
            public boolean isMute() {
                return false;
            }

            @Override
            public void setVolume(float volume) {}

            @Override
            public void replay(boolean resetPosition) {
                if (resetPosition) {
                    videoView.seekTo(0);
                }
                videoView.startPlayLogic();
            }

            @Override
            public void hide() {}

            @Override
            public void show() {}

            @Override
            public boolean hasCutout() {
                return false;
            }

            @Override
            public int getCutoutHeight() {
                return 0;
            }
        };
    }

    public boolean isUseOrangeComponents() {
        return mUseOrangeComponents;
    }

    public com.orange.playerlibrary.component.PrepareView getPrepareView() {
        return mPrepareView;
    }

    public com.orange.playerlibrary.component.TitleView getTitleView() {
        return mTitleView;
    }

    public com.orange.playerlibrary.component.VodControlView getVodControlView() {
        return mVodControlView;
    }

    public com.orange.playerlibrary.component.LiveControlView getLiveControlView() {
        return mLiveControlView;
    }

    public com.orange.playerlibrary.component.CompleteView getCompleteView() {
        return mCompleteView;
    }

    public com.orange.playerlibrary.component.ErrorView getErrorView() {
        return mErrorView;
    }

    public com.orange.playerlibrary.interfaces.ControlWrapper getControlWrapper() {
        return mControlWrapper;
    }

    public void setUrl(String url) {
        setUrl(url, null);
    }

    public void setUrl(String url, Map<String, String> headers) {
        this.mVideoUrl = url;
        this.mVideoHeaders = headers;
        if (headers != null) {
            setUp(url, true, null, headers, "");
        } else {
            setUp(url, true, "");
        }
    }

    public String getUrl() {
        return mVideoUrl;
    }

    public void start() {
        android.util.Log.d(TAG, "=== start() called ===");
        mIsSniffing = false;
        mIsLiveVideo = false;
        if (mSkipManager != null) {
            mSkipManager.reset();
        }
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.startBlackScreenDetection();
            mErrorRecoveryManager.startStateConsistencyCheck();
        }
        android.util.Log.d(TAG, "start(): setting STATE_PREPARING");
        setOrangePlayState(PlayerConstants.STATE_PREPARING);
        android.util.Log.d(TAG, "start(): calling startPlayLogic");
        startPlayLogic();
    }

    public void pause() {
        android.util.Log.d(TAG, "pause() called");
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        if (mSkipManager != null) {
            mSkipManager.stopOutroCheck();
        }
        onVideoPause();
        android.util.Log.d(TAG, "pause() completed");
    }

    public void resume() {
        android.util.Log.d(TAG, "resume() called");
        onVideoResume();
        if (mSkipManager != null) {
            mSkipManager.startOutroCheck();
        }
        android.util.Log.d(TAG, "resume() completed");
    }


    /**
     * 重写 onSurfaceDestroyed - 关键方法
     * 
     * 问题分析：
     * 当屏幕旋转时，TextureView 会被销毁并重建，触发 onSurfaceTextureDestroyed
     * GSY 的默认实现会调用 setDisplay(null) 和 releaseSurface()，导致播放器重置
     * 
     * 解决方案：
     * 在全屏切换期间，跳过 Surface 释放，保持播放器状态
     */
    @Override
    public boolean onSurfaceDestroyed(Surface surface) {
        android.util.Log.d(TAG, "=== onSurfaceDestroyed ===");
        android.util.Log.d(TAG, "position: " + getCurrentPositionWhenPlaying());
        android.util.Log.d(TAG, "isFullscreenTransitioning: " + (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()));
        android.util.Log.d(TAG, "mEnteringPiPMode: " + mEnteringPiPMode);

        // 全屏切换时跳过
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            android.util.Log.d(TAG, "onSurfaceDestroyed: SKIP - fullscreen transitioning");
            return true;
        }

        // 画中画模式时跳过
        if (mEnteringPiPMode) {
            android.util.Log.d(TAG, "onSurfaceDestroyed: SKIP - entering PiP mode");
            return true;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                android.util.Log.d(TAG, "onSurfaceDestroyed: SKIP - in PiP mode");
                return true;
            }
        }

        android.util.Log.d(TAG, "onSurfaceDestroyed: calling super");
        return super.onSurfaceDestroyed(surface);
    }
    
    /**
     * 重写 setDisplay - 关键方法
     * 
     * 在全屏切换时，跳过 setDisplay(null)，避免播放器重置
     */
    @Override
    protected void setDisplay(Surface surface) {
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            android.util.Log.d(TAG, "setDisplay: transitioning, surface=" + surface);
            if (surface != null) {
                super.setDisplay(surface);
            } else {
                android.util.Log.d(TAG, "setDisplay: SKIP null - transitioning");
            }
            return;
        }
        super.setDisplay(surface);
    }
    
    /**
     * 重写 releaseSurface - 关键方法
     * 
     * 在全屏切换时跳过 Surface 释放
     */
    @Override
    protected void releaseSurface(Surface surface) {
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            android.util.Log.d(TAG, "releaseSurface: SKIP - transitioning");
            return;
        }
        if (mEnteringPiPMode) {
            android.util.Log.d(TAG, "releaseSurface: SKIP - PiP mode");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                android.util.Log.d(TAG, "releaseSurface: SKIP - in PiP mode");
                return;
            }
        }
        super.releaseSurface(surface);
    }

    @Override
    public void onVideoPause() {
        android.util.Log.d(TAG, "=== onVideoPause ===");
        android.util.Log.d(TAG, "position: " + getCurrentPositionWhenPlaying());
        
        if (mEnteringPiPMode) {
            android.util.Log.d(TAG, "onVideoPause: SKIP - PiP mode");
            return;
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                android.util.Log.d(TAG, "onVideoPause: SKIP - in PiP mode");
                return;
            }
        }
                
        if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            super.onVideoResume();
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            return;
        }
        
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERED);
        super.onVideoPause();
        if (shouldUpdateState) {
            mCurrentPlayState = PlayerConstants.STATE_PAUSED;
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PAUSED);
            if (mCurrentState != CURRENT_STATE_PAUSE) {
                mCurrentState = CURRENT_STATE_PAUSE;
            }
        }
    }

    @Override
    public void onVideoResume() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                return;
            }
        }
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PAUSED);
        super.onVideoResume();
        if (shouldUpdateState) {
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            if (mCurrentState != CURRENT_STATE_PLAYING) {
                mCurrentState = CURRENT_STATE_PLAYING;
            }
        }
    }

    public void setEnteringPiPMode(boolean entering) {
        this.mEnteringPiPMode = entering;
    }
    
    public boolean isEnteringPiPMode() {
        return mEnteringPiPMode;
    }

    @Override
    public void release() {
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        if (mSkipManager != null) {
            mSkipManager.detachVideoView();
        }
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.detachVideoView();
        }
        super.release();
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        GSYVideoManager.releaseAllVideos();
    }

    public long getCurrentPosition() {
        return getCurrentPositionWhenPlaying();
    }

    public void seekTo(int position) {
        seekTo((long) position);
    }

    public void seekTo(long position) {
        if (GSYVideoManager.instance().getPlayer() != null) {
            GSYVideoManager.instance().getPlayer().seekTo(position);
        } else {
            setSeekOnStart(position);
        }
    }

    @Override
    public void setSpeed(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
        super.setSpeed(speed);
    }

    public static float getSpeeds() {
        return sSpeed;
    }

    public static void setSpeeds(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
    }

    public static float getLongSpeeds() {
        return sLongSpeed;
    }

    public static void setLongSpeeds(float speed) {
        sLongSpeed = speed;
    }

    public void startFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.enterFullscreen(activity);
        }
    }

    public void stopFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.exitFullscreen(activity);
        }
    }
    
    /**
     * 进入竖屏全屏模式
     * 不旋转屏幕，只是将播放器移动到全屏显示
     */
    public void startPortraitFullScreen() {
        if (mFullscreenHelper != null) {
            mFullscreenHelper.startPortraitFullScreen();
        }
    }
    
    /**
     * 退出竖屏全屏模式
     */
    public void stopPortraitFullScreen() {
        if (mFullscreenHelper != null) {
            mFullscreenHelper.stopPortraitFullScreen();
        }
    }
    
    /**
     * 是否竖屏全屏
     */
    public boolean isPortraitFullScreen() {
        return mFullscreenHelper != null && mFullscreenHelper.isPortraitFullscreen();
    }

    public boolean isFullScreen() {
        return mFullscreenHelper != null && mFullscreenHelper.isFullscreen();
    }

    public boolean isTinyScreen() {
        return mCurrentPlayerState == PlayerConstants.PLAYER_TINY_SCREEN;
    }

    public void setAutoRotateOnFullscreen(boolean autoRotate) {
        this.mAutoRotateOnFullscreen = autoRotate;
    }

    public boolean isAutoRotateOnFullscreen() {
        return mAutoRotateOnFullscreen;
    }

    @SuppressWarnings("unchecked")
    public void selectPlayerFactory(String engineType) {
        if (engineType == null) {
            engineType = PlayerConstants.ENGINE_DEFAULT;
        }
        
        switch (engineType) {
            case PlayerConstants.ENGINE_IJK:
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
            case PlayerConstants.ENGINE_EXO:
                try {
                    Class<?> exoClass = Class.forName("com.shuyu.gsyvideoplayer.player.Exo2PlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) exoClass);
                } catch (ClassNotFoundException e) {
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_ALI:
                try {
                    Class<?> aliClass = Class.forName("com.shuyu.gsyvideoplayer.player.AliPlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) aliClass);
                } catch (ClassNotFoundException e) {
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                PlayerFactory.setPlayManager(SystemPlayerManager.class);
                break;
        }
    }

    protected void setOrangePlayState(int playState) {
        mCurrentPlayState = playState;
        notifyPlayStateChanged(playState);
        
        post(new Runnable() {
            @Override
            public void run() {
                if (playState == PlayerConstants.STATE_PLAYING) {
                    showController();
                } else if (playState == PlayerConstants.STATE_PAUSED) {
                    showController();
                    cancelAutoHideTimer();
                } else {
                    cancelAutoHideTimer();
                }
            }
        });
    }

    protected void setOrangePlayerState(int playerState) {
        mCurrentPlayerState = playerState;
        notifyPlayerStateChanged(playerState);
    }

    public int getPlayState() {
        return mCurrentPlayState;
    }

    public int getPlayerState() {
        return mCurrentPlayerState;
    }

    public void addOnStateChangeListener(OnStateChangeListener listener) {
        if (listener != null && !mStateChangeListeners.contains(listener)) {
            mStateChangeListeners.add(listener);
        }
    }

    public void removeOnStateChangeListener(OnStateChangeListener listener) {
        mStateChangeListeners.remove(listener);
    }

    public void clearOnStateChangeListeners() {
        mStateChangeListeners.clear();
    }

    private void notifyPlayStateChanged(int playState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayStateChanged(playState);
            }
        }
        if (mUseOrangeComponents) {
            notifyComponentsPlayStateChanged(playState);
        }
        // 通知控制器更新加载动画和网速显示
        if (mOrangeController != null) {
            mOrangeController.onPlayStateChanged(playState);
        }
    }

    private void notifyPlayerStateChanged(int playerState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayerStateChanged(playerState);
            }
        }
        if (mUseOrangeComponents) {
            notifyComponentsPlayerStateChanged(playerState);
        }
        // 通知控制器更新状态
        if (mOrangeController != null) {
            mOrangeController.onPlayerStateChanged(playerState);
        }
    }

    private void notifyComponentsPlayStateChanged(int playState) {
        if (mPrepareView != null) mPrepareView.onPlayStateChanged(playState);
        if (mCompleteView != null) mCompleteView.onPlayStateChanged(playState);
        if (mErrorView != null) mErrorView.onPlayStateChanged(playState);
        if (mTitleView != null) mTitleView.onPlayStateChanged(playState);
        if (mVodControlView != null) mVodControlView.onPlayStateChanged(playState);
        if (mLiveControlView != null) mLiveControlView.onPlayStateChanged(playState);
    }

    private void notifyComponentsPlayerStateChanged(int playerState) {
        if (mPrepareView != null) mPrepareView.onPlayerStateChanged(playerState);
        if (mCompleteView != null) mCompleteView.onPlayerStateChanged(playerState);
        if (mErrorView != null) mErrorView.onPlayerStateChanged(playerState);
        if (mTitleView != null) mTitleView.onPlayerStateChanged(playerState);
        if (mVodControlView != null) mVodControlView.onPlayerStateChanged(playerState);
        if (mLiveControlView != null) mLiveControlView.onPlayerStateChanged(playerState);
    }


    public void updateComponentsProgress(int duration, int position) {
        if (mVodControlView == null && mLiveControlView == null) {
            return;
        }
        
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            final int finalDuration = duration;
            final int finalPosition = position;
            post(new Runnable() {
                @Override
                public void run() {
                    updateComponentsProgressInternal(finalDuration, finalPosition);
                }
            });
        } else {
            updateComponentsProgressInternal(duration, position);
        }
    }
    
    private void updateComponentsProgressInternal(int duration, int position) {
        if (mVodControlView != null) {
            try {
                mVodControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress error", e);
            }
        }
        
        if (mLiveControlView != null) {
            try {
                mLiveControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress error", e);
            }
        }
    }

    public void setOnProgressListener(OnProgressListener listener) {
        this.mProgressListener = listener;
    }

    public void setOnPlayCompleteListener(OnPlayCompleteListener listener) {
        this.mPlayCompleteListener = listener;
    }

    public OrangeVideoController getVideoController() {
        return mOrangeController;
    }

    public void setVideoController(OrangeVideoController controller) {
        this.mOrangeController = controller;

        if (controller != null) {
            controller.setVideoView(this);
            // 设置播放器引用，用于获取网速
            controller.setVideoViewRef(this);

            if (mTitleView != null) {
                mTitleView.setController(controller);
            }
            
            if (mVodControlView != null) {
                mVodControlView.setOrangeVideoController(controller);
            }
            
            ensureEventBinding();
        }
    }

    public void setAutoThumbnailEnabled(boolean enabled) {
        this.mAutoThumbnailEnabled = enabled;
    }

    public boolean isAutoThumbnailEnabled() {
        return mAutoThumbnailEnabled;
    }

    public void setDefaultThumbnail(Object thumbnail) {
        this.mDefaultThumbnail = thumbnail;
    }

    public Object getDefaultThumbnail() {
        return mDefaultThumbnail;
    }

    public void getVideoFirstFrameAsync(VideoThumbnailHelper.ThumbnailCallback callback) {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Video URL is empty");
            }
            return;
        }
        VideoThumbnailHelper.getVideoFirstFrameAsync(mVideoUrl, mVideoHeaders, callback);
    }

    public void getFrameAtTimeAsync(long timeUs, VideoThumbnailHelper.ThumbnailCallback callback) {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Video URL is empty");
            }
            return;
        }
        VideoThumbnailHelper.getFrameAtTimeAsync(mVideoUrl, timeUs, mVideoHeaders, callback);
    }

    public void setKeepVideoPlaying(boolean keep) {
        this.mKeepVideoPlaying = keep;
    }

    public boolean isKeepVideoPlaying() {
        return mKeepVideoPlaying;
    }

    public void savePlaybackProgress() {
        if (!mKeepVideoPlaying || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        
        long position = getCurrentPosition();
        long duration = getDuration();
        
        if (position > 0 && duration > 0) {
            PlaybackProgressManager.getInstance(getContext())
                    .saveProgress(mVideoUrl, position, duration);
        }
    }

    public boolean restorePlaybackProgress() {
        if (!mKeepVideoPlaying || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        
        PlaybackProgressManager manager = PlaybackProgressManager.getInstance(getContext());
        long resumePosition = manager.getResumePosition(mVideoUrl);
        
        if (resumePosition > 0) {
            seekTo(resumePosition);
            return true;
        }
        return false;
    }

    public long getSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return 0;
        }
        return PlaybackProgressManager.getInstance(getContext()).getProgress(mVideoUrl);
    }

    public boolean hasSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        return PlaybackProgressManager.getInstance(getContext()).hasProgress(mVideoUrl);
    }

    public void clearSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        PlaybackProgressManager.getInstance(getContext()).removeProgress(mVideoUrl);
    }

    public void setSkipIntroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroTime(timeMs);
        }
    }

    public void setSkipIntroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroSeconds(seconds);
        }
    }

    public long getSkipIntroTime() {
        return mSkipManager != null ? mSkipManager.getSkipIntroTime() : 0;
    }

    public void setSkipIntroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroEnabled(enabled);
        }
    }

    public boolean isSkipIntroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipIntroEnabled();
    }

    public void setSkipOutroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroTime(timeMs);
        }
    }

    public void setSkipOutroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroSeconds(seconds);
        }
    }

    public long getSkipOutroTime() {
        return mSkipManager != null ? mSkipManager.getSkipOutroTime() : 0;
    }

    public void setSkipOutroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroEnabled(enabled);
        }
    }

    public boolean isSkipOutroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipOutroEnabled();
    }

    public void setOnSkipListener(SkipManager.OnSkipListener listener) {
        if (mSkipManager != null) {
            mSkipManager.setOnSkipListener(listener);
        }
    }

    public SkipManager getSkipManager() {
        return mSkipManager;
    }

    public VideoScaleManager getVideoScaleManager() {
        return mVideoScaleManager;
    }
    
    public PlaybackStateManager getPlaybackStateManager() {
        return mPlaybackStateManager;
    }
    
    public ComponentStateManager getComponentStateManager() {
        return mComponentStateManager;
    }
    
    public ErrorRecoveryManager getErrorRecoveryManager() {
        return mErrorRecoveryManager;
    }

    public void refreshVideoShowType() {
        changeTextureViewShowType();
    }

    public boolean isLiveVideo() {
        return mIsLiveVideo;
    }

    public void setLiveVideo(boolean isLive) {
        this.mIsLiveVideo = isLive;
    }
    
    /**
     * 获取网络速度（字节/秒）
     * @return 网络速度
     */
    public long getNetSpeed() {
        return GSYVideoManager.instance().getNetSpeed();
    }
    
    /**
     * 获取网络速度（兼容旧 API）
     * @return 网络速度
     */
    public long getTcpSpeed() {
        return getNetSpeed();
    }
    
    /**
     * 获取格式化的网速文本
     * @return 网速文本，如 "1.5 MB/s"
     */
    public String getNetSpeedText() {
        long speed = getNetSpeed();
        return formatSpeed(speed);
    }
    
    /**
     * 格式化网速
     */
    private String formatSpeed(long speed) {
        if (speed <= 0) return "0 KB/s";
        
        final long KB = 1024;
        final long MB = KB * 1024;
        
        if (speed < KB) {
            return speed + " B/s";
        } else if (speed < MB) {
            float speedKB = speed / (float) KB;
            return String.format(speedKB >= 100 ? "%.0f KB/s" : "%.1f KB/s", speedKB);
        } else {
            float speedMB = speed / (float) MB;
            return speedMB >= 10 ?
                    String.format("%.1f MB/s", speedMB) :
                    String.format("%.2f MB/s", speedMB);
        }
    }

    public boolean isSniffing() {
        return mIsSniffing;
    }

    public void startSniffing() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        startSniffing(mVideoUrl, null);
    }

    public void startSniffing(String url, java.util.Map<String, String> headers) {
        mIsSniffing = true;
        setOrangePlayState(STATE_STARTSNIFFING);
        
        Context context = getContext();
        VideoSniffing.startSniffing(context, url, headers, new VideoSniffing.Call() {
            @Override
            public void received(String contentType, java.util.HashMap<String, String> respHeaders, 
                               String title, String videoUrl) {
                for (OnStateChangeListener listener : mStateChangeListeners) {
                    if (listener instanceof OnSniffingListener) {
                        ((OnSniffingListener) listener).onSniffingReceived(contentType, respHeaders, title, videoUrl);
                    }
                }
            }

            @Override
            public void onFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize) {
                mIsSniffing = false;
                setOrangePlayState(STATE_ENDSNIFFING);
                for (OnStateChangeListener listener : mStateChangeListeners) {
                    if (listener instanceof OnSniffingListener) {
                        ((OnSniffingListener) listener).onSniffingFinish(videoList, videoSize);
                    }
                }
            }
        });
    }

    public void stopSniffing() {
        mIsSniffing = false;
        VideoSniffing.stop(true);
        setOrangePlayState(STATE_ENDSNIFFING);
    }

    public interface OnSniffingListener {
        void onSniffingReceived(String contentType, java.util.HashMap<String, String> headers, 
                               String title, String url);
        void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize);
    }

    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    public boolean isDebug() {
        return mDebug;
    }

    protected void debug(Object message) {
        if (mDebug) {
            android.util.Log.d(TAG, String.valueOf(message));
        }
    }

    public Activity getActivity() {
        Context context = getContext();
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    public boolean isPlaying() {
        return mCurrentPlayState == PlayerConstants.STATE_PLAYING;
    }

    public boolean isInNormalState() {
        return !isFullScreen() && !isTinyScreen();
    }

    private com.orange.playerlibrary.component.GestureView mGestureView;

    @Override
    protected void showBrightnessDialog(float percent) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onBrightnessChange((int) (percent * 100));
        }
    }

    @Override
    protected void showVolumeDialog(float deltaY, int volumePercent) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onVolumeChange(volumePercent);
        }
    }

    @Override
    protected void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onPositionChange((int) seekTimePosition, (int) getCurrentPosition(), (int) getDuration());
        }
    }

    @Override
    protected void dismissBrightnessDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    @Override
    protected void dismissVolumeDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    @Override
    protected void dismissProgressDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    private void ensureGestureView() {
        if (mGestureView == null) {
            mGestureView = new com.orange.playerlibrary.component.GestureView(getContext());
            android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
            addView(mGestureView, lp);
        }
    }

    public com.orange.playerlibrary.component.GestureView getGestureView() {
        ensureGestureView();
        return mGestureView;
    }

    public void setThisPlayState(int state) {
        setOrangePlayState(state);
    }

    public void setThisPlayerState(int state) {
        setOrangePlayerState(state);
    }


    @Override
    public int getLayoutId() {
        return R.layout.layout_orange_base_player;
    }

    @Override
    public int getSmallId() {
        return 0;
    }

    @Override
    public int getFullId() {
        return GSYVideoManager.FULLSCREEN_ID;
    }

    @Override
    public android.widget.ImageView getBackButton() {
        return null;
    }

    @SuppressWarnings("ResourceType")
    public OrangevideoView getOrangeFullWindowPlayer() {
        Activity activity = com.shuyu.gsyvideoplayer.utils.CommonUtil.scanForActivity(getContext());
        if (activity == null) {
            return null;
        }
        android.view.ViewGroup vp = (android.view.ViewGroup) activity.findViewById(android.view.Window.ID_ANDROID_CONTENT);
        final android.view.View full = vp.findViewById(getFullId());
        OrangevideoView orangeVideoView = null;
        if (full != null && full instanceof OrangevideoView) {
            orangeVideoView = (OrangevideoView) full;
        }
        return orangeVideoView;
    }

    @Override
    protected void checkoutState() {
        removeCallbacks(mOrangeCheckoutTask);
        mInnerHandler.postDelayed(mOrangeCheckoutTask, 500);
    }

    private Runnable mOrangeCheckoutTask = new Runnable() {
        @Override
        public void run() {
            OrangevideoView fullPlayer = getOrangeFullWindowPlayer();
            if (fullPlayer != null && fullPlayer.mCurrentState != mCurrentState) {
                if (fullPlayer.mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START
                    && mCurrentState != CURRENT_STATE_PREPAREING) {
                    fullPlayer.setStateAndUi(mCurrentState);
                }
            }
        }
    };

    @Override
    @SuppressWarnings({"ResourceType", "unchecked"})
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        hideStatusBarAndNavigation(context);
        
        if (mAutoRotateOnFullscreen) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        
        GSYBaseVideoPlayer fullPlayer = super.startWindowFullscreen(context, true, true);
        
        if (fullPlayer instanceof OrangevideoView) {
            final OrangevideoView orangeFullPlayer = (OrangevideoView) fullPlayer;
            orangeFullPlayer.mIfCurrentIsFullscreen = true;
            
            orangeFullPlayer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mTitleView != null && orangeFullPlayer.mTitleView != null) {
                        String title = mTitleView.getTitle();
                        orangeFullPlayer.mTitleView.setTitle(title);
                        if (mOrangeController != null) {
                            orangeFullPlayer.mTitleView.setController(mOrangeController);
                        }
                    }
                    
                    if (mOrangeController != null && orangeFullPlayer.mVodControlView != null) {
                        com.orange.playerlibrary.VideoEventManager eventManager = 
                                mOrangeController.getVideoEventManager();
                        if (eventManager != null) {
                            eventManager.bindControllerComponents(orangeFullPlayer.mVodControlView);
                        }
                    }
                    
                    orangeFullPlayer.setOrangePlayState(mCurrentPlayState);
                    orangeFullPlayer.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
                    
                    if (orangeFullPlayer.mComponentStateManager != null) {
                        orangeFullPlayer.mComponentStateManager.reregisterProgressListener(orangeFullPlayer);
                    }
                    
                    orangeFullPlayer.showController();
                    if (orangeFullPlayer.mTitleView != null) {
                        orangeFullPlayer.mTitleView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mTitleView.bringToFront();
                    }
                    if (orangeFullPlayer.mVodControlView != null) {
                        orangeFullPlayer.mVodControlView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mVodControlView.bringToFront();
                        orangeFullPlayer.mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                    }
                    orangeFullPlayer.requestLayout();
                }
            }, 300);
        }
        
        setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        return fullPlayer;
    }
    
    private void hideStatusBarAndNavigation(Context context) {
        Activity activity = com.shuyu.gsyvideoplayer.utils.CommonUtil.scanForActivity(context);
        if (activity != null) {
            android.view.View decorView = activity.getWindow().getDecorView();
            int uiOptions = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(uiOptions);
            
            if (activity.getActionBar() != null) {
                activity.getActionBar().hide();
            }
            if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.ActionBar supportActionBar = ((androidx.appcompat.app.AppCompatActivity) activity).getSupportActionBar();
                if (supportActionBar != null) {
                    supportActionBar.hide();
                }
            }
        }
    }

    @Override
    @SuppressWarnings("ResourceType")
    protected void clearFullscreenLayout() {
        if (!mFullAnimEnd) {
            return;
        }
        
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        if (oldF != null && oldF instanceof OrangevideoView) {
            OrangevideoView orangeVideoPlayer = (OrangevideoView) oldF;
            if (mPlaybackStateManager != null) {
                mPlaybackStateManager.saveState(orangeVideoPlayer);
            }
            orangeVideoPlayer.mIfCurrentIsFullscreen = false;
        }
        
        mIfCurrentIsFullscreen = false;
        int delay = 0;
        if (mOrientationUtils != null) {
            delay = mOrientationUtils.backToProtVideo();
            mOrientationUtils.setEnable(false);
            if (mOrientationUtils != null) {
                mOrientationUtils.releaseListener();
                mOrientationUtils = null;
            }
        }

        if (!mShowFullAnimation) {
            delay = 0;
        }

        mInnerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                orangeBackToNormal();
            }
        }, delay);
    }

    @SuppressWarnings("ResourceType")
    protected void orangeBackToNormal() {
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        final OrangevideoView orangeVideoPlayer;
        
        if (oldF != null && oldF instanceof OrangevideoView) {
            orangeVideoPlayer = (OrangevideoView) oldF;
            if (mShowFullAnimation && mListItemRect != null && mListItemSize != null) {
                android.transition.TransitionManager.beginDelayedTransition(vp);
                android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) orangeVideoPlayer.getLayoutParams();
                lp.setMargins(mListItemRect[0], mListItemRect[1], 0, 0);
                lp.width = mListItemSize[0];
                lp.height = mListItemSize[1];
                lp.gravity = android.view.Gravity.NO_GRAVITY;
                orangeVideoPlayer.setLayoutParams(lp);
                mInnerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        orangeResolveNormalVideoShow(oldF, vp, orangeVideoPlayer);
                    }
                }, 400);
            } else {
                orangeResolveNormalVideoShow(oldF, vp, orangeVideoPlayer);
            }
        } else {
            orangeResolveNormalVideoShow(null, vp, null);
        }
    }

    protected void orangeResolveNormalVideoShow(android.view.View oldF, android.view.ViewGroup vp, OrangevideoView orangeVideoPlayer) {
        final long savedPosition = (orangeVideoPlayer != null) ? orangeVideoPlayer.getCurrentPositionWhenPlaying() : 0;
        final boolean wasPlaying = (orangeVideoPlayer != null) ? orangeVideoPlayer.isPlaying() : false;
        
        if (oldF != null && oldF.getParent() != null) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        
        mCurrentState = getGSYVideoManager().getLastState();
        
        if (orangeVideoPlayer != null) {
            cloneParams(orangeVideoPlayer, this);
        }
        
        if (mCurrentState != CURRENT_STATE_NORMAL
            || mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
            createNetWorkState();
        }
        
        getGSYVideoManager().setListener(getGSYVideoManager().lastListener());
        getGSYVideoManager().setLastListener(null);
        setStateAndUi(mCurrentState);
        
        addTextureView();
        
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (savedPosition > 0) {
                    seekTo(savedPosition);
                    if (wasPlaying) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mCurrentState == CURRENT_STATE_PAUSE) {
                                    onVideoResume();
                                }
                            }
                        }, 200);
                    }
                }
                
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                }
                
                notifyComponentsPlayStateChanged(mCurrentPlayState);
                notifyComponentsPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
            }
        }, 500);
        
        mSaveChangeViewTIme = System.currentTimeMillis();
        if (mVideoAllCallBack != null) {
            mVideoAllCallBack.onQuitFullscreen(mOriginUrl, mTitle, this);
        }
        mIfCurrentIsFullscreen = false;
        if (mHideKey) {
            com.shuyu.gsyvideoplayer.utils.CommonUtil.showNavKey(mContext, mSystemUiVisibility);
        }
        com.shuyu.gsyvideoplayer.utils.CommonUtil.showSupportActionBar(mContext, mActionBar, mStatusBar);
        if (getFullscreenButton() != null) {
            getFullscreenButton().setImageResource(getEnlargeImageRes());
        }
        setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
    }

    @Override
    public GSYVideoManager getGSYVideoManager() {
        return GSYVideoManager.instance();
    }

    @Override
    public void releaseVideos() {
        GSYVideoManager.releaseAllVideos();
    }

    @Override
    public boolean backFromFull(Context context) {
        if (mIfCurrentIsFullscreen) {
            mIfCurrentIsFullscreen = false;
            setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
            if (context instanceof Activity) {
                ((Activity) context).setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void showWifiDialog() {
        if (mPrepareView != null) {
            setOrangePlayState(8);
        }
    }

    protected void changeUiToNormal() {}
    protected void changeUiToPreparingShow() {}
    protected void changeUiToPlayingShow() {}
    protected void changeUiToPlayingBufferingShow() {}
    protected void changeUiToPauseShow() {}
    protected void changeUiToError() {}
    protected void changeUiToCompleteShow() {}
    protected void changeUiToPrepareingClear() {}
    protected void changeUiToPlayingClear() {}
    protected void changeUiToPlayingBufferingClear() {}
    protected void changeUiToPauseClear() {}
    protected void changeUiToCompleteClear() {}
    protected void hideAllWidget() {}
    
    private static final int AUTO_HIDE_DELAY = 4000;
    private Runnable mAutoHideRunnable;

    @Override
    protected void onClickUiToggle(android.view.MotionEvent e) {
        if (mCurrentPlayState != PlayerConstants.STATE_PLAYING && 
            mCurrentPlayState != PlayerConstants.STATE_PAUSED &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERING &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERED) {
            return;
        }
        
        if (isControllerShowing()) {
            hideController();
        } else {
            showController();
        }
    }
    
    public void showController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.VISIBLE);
        }
        if (mTitleView != null && (mIfCurrentIsFullscreen || mCurrentPlayerState == PlayerConstants.PLAYER_FULL_SCREEN)) {
            mTitleView.setVisibility(android.view.View.VISIBLE);
        }
        startAutoHideTimer();
    }
    
    public void hideController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.GONE);
        }
        if (mTitleView != null) {
            mTitleView.setVisibility(android.view.View.GONE);
        }
        cancelAutoHideTimer();
    }
    
    public boolean isControllerShowing() {
        return mVodControlView != null && mVodControlView.getVisibility() == android.view.View.VISIBLE;
    }
    
    private Runnable getAutoHideRunnable() {
        if (mAutoHideRunnable == null) {
            mAutoHideRunnable = new Runnable() {
                @Override
                public void run() {
                    hideController();
                }
            };
        }
        return mAutoHideRunnable;
    }
    
    private void startAutoHideTimer() {
        cancelAutoHideTimer();
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING && mInnerHandler != null) {
            mInnerHandler.postDelayed(getAutoHideRunnable(), AUTO_HIDE_DELAY);
        }
    }
    
    private void cancelAutoHideTimer() {
        if (mInnerHandler != null && mAutoHideRunnable != null) {
            mInnerHandler.removeCallbacks(mAutoHideRunnable);
        }
    }

    // 双击事件时间戳，用于防止双击后的单击事件干扰
    private static long sLastDoubleClickTime = 0;
    private static final long DOUBLE_CLICK_BLOCK_INTERVAL = 600; // 双击后600ms内阻止单击
    
    public static long getLastDoubleClickTime() {
        return sLastDoubleClickTime;
    }
    
    public static long getDoubleClickBlockInterval() {
        return DOUBLE_CLICK_BLOCK_INTERVAL;
    }
    
    @Override
    protected void touchDoubleUp(android.view.MotionEvent e) {
        sLastDoubleClickTime = System.currentTimeMillis();
        android.util.Log.d(TAG, "touchDoubleUp: mCurrentPlayState=" + mCurrentPlayState + ", timestamp=" + sLastDoubleClickTime);
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
            mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
            mCurrentPlayState == PlayerConstants.STATE_BUFFERED) {
            android.util.Log.d(TAG, "touchDoubleUp: calling pause()");
            pause();
        } else if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            android.util.Log.d(TAG, "touchDoubleUp: calling resume()");
            resume();
        } else {
            android.util.Log.d(TAG, "touchDoubleUp: no action for state " + mCurrentPlayState);
        }
    }

    @Override
    public void startPlayLogic() {
        prepareVideo();
    }

    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
        setSpeed(sSpeed);
    }
}
