package com.orange.playerlibrary;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;

import com.orange.playerlibrary.interfaces.OnPlayCompleteListener;
import com.orange.playerlibrary.interfaces.OnProgressListener;
import com.orange.playerlibrary.interfaces.OnStateChangeListener;
import com.orange.playerlibrary.history.PlayHistoryManager;
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
    private static final String SURFACE_CONTROL_NAME = "OrangeExoSurface";
    
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
    
    // 首帧加载状态
    private boolean mIsLoadingThumbnail = false;
    
    // 用户主动暂停标记（用于修复后台自动恢复播放bug）
    private boolean mUserPaused = false;
    
    private SkipManager mSkipManager;
    private VideoScaleManager mVideoScaleManager;
    private PlaybackStateManager mPlaybackStateManager;
    private ComponentStateManager mComponentStateManager;
    private ErrorRecoveryManager mErrorRecoveryManager;
    private CustomFullscreenHelper mFullscreenHelper;
    
    // ExoPlayer Surface 切换相关 (Android Q+)
    private SurfaceControl mExoSurfaceControl;
    private Surface mExoVideoSurface;
    private boolean mUseExoSurfaceControl = false;
    
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
    
    // 网速显示相关
    private android.widget.TextView mLoadingSpeedText;
    private android.os.Handler mSpeedHandler;
    private boolean mIsShowingLoading = false;
    // 网速计算相关
    private long mLastRxBytes = 0;
    private long mLastSpeedTime = 0;
    // 播放器核心是否已初始化
    private boolean mPlayerFactoryInitialized = false;
    private final Runnable mSpeedUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateLoadingSpeed();
            if (mIsShowingLoading && mSpeedHandler != null) {
                mSpeedHandler.postDelayed(this, 1000);
            }
        }
    };
    
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
        
        // 初始化播放核心（必须在这里初始化，因为需要 Context）
        initPlayerFactory();
        
        mSkipManager = new SkipManager();
        mSkipManager.attachVideoView(this);
        
        mVideoScaleManager = new VideoScaleManager(this, PlayerSettingsManager.getInstance(getContext()));
        mPlaybackStateManager = new PlaybackStateManager();
        mComponentStateManager = new ComponentStateManager();
        
        mErrorRecoveryManager = new ErrorRecoveryManager();
        mErrorRecoveryManager.attachVideoView(this);
        
        mFullscreenHelper = new CustomFullscreenHelper(this);
        
        // 初始化网速更新 Handler
        mSpeedHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
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
                
                // 启动播放历史自动保存
                startPlayHistoryAutoSave();
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PLAYBACK_COMPLETED);
                
                // 停止播放历史自动保存
                stopPlayHistoryAutoSave();
                
                // 播放完成，删除历史进度记录
                PlayHistoryManager.getInstance(getContext()).deleteHistory(url);
                
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
    
    /**
     * 初始化播放器核心
     * 根据用户设置选择合适的播放引擎
     */
    private void initPlayerFactory() {
        if (mPlayerFactoryInitialized) {
            return; // 已经初始化过了
        }
        
        PlayerSettingsManager settingsManager = PlayerSettingsManager.getInstance(getContext());
        String engine = settingsManager.getPlayerEngine();
        // 根据设置切换播放器核心
        switch (engine) {
            case PlayerConstants.ENGINE_IJK:
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
                
            case PlayerConstants.ENGINE_EXO:
                // 使用自定义的 OrangeExoPlayerManager，支持 SurfaceControl 无缝切换
                // 解决横竖屏切换时 MediaCodec IllegalStateException 问题
                try {
                    PlayerFactory.setPlayManager(com.orange.playerlibrary.exo.OrangeExoPlayerManager.class);
                    // ExoPlayer 需要使用 SurfaceView 才能使用 SurfaceControl.reparent()
                    // 设置渲染类型为 SurfaceView (Android Q+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                            com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
                    }
                } catch (Exception e) {
                    // 回退到 GSY 原生 Exo2PlayerManager
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends IPlayerManager> exoClass = 
                            (Class<? extends IPlayerManager>) Class.forName("tv.danmaku.ijk.media.exo2.Exo2PlayerManager");
                        PlayerFactory.setPlayManager(exoClass);
                    } catch (ClassNotFoundException ex) {
                        PlayerFactory.setPlayManager(IjkPlayerManager.class);
                    }
                }
                break;
                
            case PlayerConstants.ENGINE_ALI:
                // GSY AliPlayer 类名: com.shuyu.aliplay.AliPlayerManager
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends IPlayerManager> aliClass = 
                        (Class<? extends IPlayerManager>) Class.forName("com.shuyu.aliplay.AliPlayerManager");
                    PlayerFactory.setPlayManager(aliClass);
                } catch (ClassNotFoundException e) {
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
                
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                // 使用自定义的 OrangeSystemPlayerManager，统一网速计算和 SurfaceControl 支持
                PlayerFactory.setPlayManager(com.orange.playerlibrary.player.OrangeSystemPlayerManager.class);
                // 系统播放器也需要使用 SurfaceView 才能使用 SurfaceControl.reparent()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                        com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
                }
                break;
        }
        
        mPlayerFactoryInitialized = true;
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
            public void hide() {
                if (mOrangeController != null) {
                    mOrangeController.hide();
                }
            }

            @Override
            public void show() {
                if (mOrangeController != null) {
                    mOrangeController.show();
                }
            }

            @Override
            public boolean isShowing() {
                return mOrangeController != null && mOrangeController.isShowing();
            }

            @Override
            public void stopProgress() {
                // 停止进度更新 - 由控制器处理
                if (mOrangeController != null) {
                    mOrangeController.stopProgress();
                }
            }

            @Override
            public void startProgress() {
                // 开始进度更新 - 由控制器处理
                if (mOrangeController != null) {
                    mOrangeController.startProgress();
                }
            }

            @Override
            public void stopFadeOut() {
                // 停止自动隐藏 - 由控制器处理
                if (mOrangeController != null) {
                    mOrangeController.stopFadeOut();
                }
            }

            @Override
            public void startFadeOut() {
                // 开始自动隐藏倒计时 - 由控制器处理
                if (mOrangeController != null) {
                    mOrangeController.startFadeOut();
                }
            }

            @Override
            public boolean hasCutout() {
                return false;
            }

            @Override
            public int getCutoutHeight() {
                return 0;
            }
            
            @Override
            public int getVideoWidth() {
                return videoView.getCurrentVideoWidth();
            }
            
            @Override
            public int getVideoHeight() {
                return videoView.getCurrentVideoHeight();
            }
            
            @Override
            public String getVideoUrl() {
                return videoView.getVideoUrl();
            }
            
            @Override
            public String getVideoTitle() {
                // GSY基类使用 mTitle 存储标题
                return mTitle;
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

    // ==================== 重写 setUp 方法以支持预览功能 ====================
    
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, String title) {
        // 保存视频URL
        this.mVideoUrl = url;
        // 设置视频URL给VodControlView用于预览功能
        com.orange.playerlibrary.component.VodControlView.setVideoUrl(url);
        // 异步获取视频首帧作为封面
        getVideoFirstFrameAsync(url);
        return super.setUp(url, cacheWithPlay, title);
    }
    
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, java.io.File cachePath, String title) {
        // 保存视频URL
        this.mVideoUrl = url;
        com.orange.playerlibrary.component.VodControlView.setVideoUrl(url);
        return super.setUp(url, cacheWithPlay, cachePath, title);
    }
    
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, java.io.File cachePath, Map<String, String> mapHeadData, String title) {
        // 保存视频URL
        this.mVideoUrl = url;
        com.orange.playerlibrary.component.VodControlView.setVideoUrl(url);
        return super.setUp(url, cacheWithPlay, cachePath, mapHeadData, title);
    }

    public void setUrl(String url) {
        setUrl(url, null);
    }

    public void setUrl(String url, Map<String, String> headers) {
        this.mVideoUrl = url;
        this.mVideoHeaders = headers;
        
        // 设置视频URL给VodControlView用于预览功能
        com.orange.playerlibrary.component.VodControlView.setVideoUrl(url);
        
        if (headers != null) {
            setUp(url, true, null, headers, "");
        } else {
            setUp(url, true, "");
        }
    }

    public String getUrl() {
        return mOriginUrl != null ? mOriginUrl : mVideoUrl;
    }

    public String getVideoUrl() {
        return mOriginUrl != null ? mOriginUrl : mVideoUrl;
    }

    public Map<String, String> getVideoHeaders() {
        return mVideoHeaders;
    }

    public void start() {
        mUserPaused = false;  // 清除用户暂停标记
        mIsSniffing = false;
        mIsLiveVideo = false;
        mIsLoadingThumbnail = false;  // 重置首帧加载状态
        if (mSkipManager != null) {
            mSkipManager.reset();
        }
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.startBlackScreenDetection();
            mErrorRecoveryManager.startStateConsistencyCheck();
        }
        // 清除旧的缩略图
        if (mOrangeController != null) {
            mOrangeController.setThumbnail(null);
        }
        setOrangePlayState(PlayerConstants.STATE_PREPARING);
        startPlayLogic();
    }

    public void pause() {
        mUserPaused = true;  // 标记用户主动暂停
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        if (mSkipManager != null) {
            mSkipManager.stopOutroCheck();
        }
        onVideoPause();
    }

    public void resume() {
        mUserPaused = false;  // 清除用户暂停标记
        onVideoResume();
        if (mSkipManager != null) {
            mSkipManager.startOutroCheck();
        }
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
        // 全屏切换时跳过
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            return true;
        }

        // 画中画模式时跳过
        if (mEnteringPiPMode) {
            return true;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                return true;
            }
        }
        return super.onSurfaceDestroyed(surface);
    }
    
    /**
     * 重写 setDisplay - 关键方法
     * 
     * ExoPlayer 全屏切换问题解决方案：
     * 使用 SurfaceControl.reparent() (Android Q+) 来无缝切换 Surface，
     * 避免 MediaCodec 在 Surface 切换时被释放导致的 IllegalStateException
     */
    @Override
    protected void setDisplay(Surface surface) {
        // 检查是否使用 ExoPlayer 或系统播放器
        String currentEngine = PlayerSettingsManager.getInstance(getContext()).getPlayerEngine();
        boolean isExoPlayer = PlayerConstants.ENGINE_EXO.equals(currentEngine);
        boolean isSystemPlayer = PlayerConstants.ENGINE_DEFAULT.equals(currentEngine);
        
        // Android Q+ 使用 SurfaceControl.reparent 方式处理 Surface 切换
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isExoPlayer) {
                setDisplayForExo(surface);
                return;
            } else if (isSystemPlayer) {
                setDisplayForSystem(surface);
                return;
            }
        }
        
        // 其他播放器或低版本 Android，使用原有逻辑
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            if (surface != null) {
                super.setDisplay(surface);
            }
            // 跳过 setDisplay(null)，保持播放状态
            return;
        }
        super.setDisplay(surface);
    }
    
    /**
     * 系统播放器专用的 Surface 切换方法
     * 使用 OrangeSystemPlayerManager 的 setDisplayNew 方法实现无缝切换
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void setDisplayForSystem(Surface surface) {
        com.shuyu.gsyvideoplayer.player.IPlayerManager playerManager = GSYVideoManager.instance().getPlayer();
        
        boolean isSurfaceView = mTextureView != null && mTextureView.getShowView() instanceof SurfaceView;
        
        if (playerManager instanceof com.orange.playerlibrary.player.OrangeSystemPlayerManager) {
            com.orange.playerlibrary.player.OrangeSystemPlayerManager systemManager = 
                (com.orange.playerlibrary.player.OrangeSystemPlayerManager) playerManager;
            
            if (surface != null && isSurfaceView) {
                SurfaceView surfaceView = (SurfaceView) mTextureView.getShowView();
                systemManager.setDisplayNew(surfaceView);
            } else if (surface != null) {
                GSYVideoManager.instance().setDisplay(surface);
            } else {
                systemManager.setDisplayNew(null);
            }
        } else {
            if (surface != null) {
                GSYVideoManager.instance().setDisplay(surface);
            }
        }
    }
    
    /**
     * ExoPlayer 专用的 Surface 切换方法
     * 完全按照 GSY 官方 GSYExo2PlayerView 的实现方式
     * 使用 OrangeExoPlayerManager 的 setDisplayNew 方法实现无缝切换
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void setDisplayForExo(Surface surface) {
        // 获取当前的 PlayerManager
        com.shuyu.gsyvideoplayer.player.IPlayerManager playerManager = GSYVideoManager.instance().getPlayer();
        
        // 添加调试日志
        boolean isSurfaceView = mTextureView != null && mTextureView.getShowView() instanceof SurfaceView;
        // 检查是否是 OrangeExoPlayerManager
        if (playerManager instanceof com.orange.playerlibrary.exo.OrangeExoPlayerManager) {
            com.orange.playerlibrary.exo.OrangeExoPlayerManager exoManager = 
                (com.orange.playerlibrary.exo.OrangeExoPlayerManager) playerManager;
            
            // 完全按照 GSY 官方的逻辑
            if (surface != null && isSurfaceView) {
                // 使用 SurfaceView 进行 reparent
                SurfaceView surfaceView = (SurfaceView) mTextureView.getShowView();
                exoManager.setDisplayNew(surfaceView);
            } else if (surface != null) {
                // 非 SurfaceView，使用普通方式（这种情况不应该发生）
                GSYVideoManager.instance().setDisplay(surface);
            } else {
                // surface 为 null，也要通过 setDisplayNew 处理
                exoManager.setDisplayNew(null);
            }
        } else {
            // 不是 OrangeExoPlayerManager，使用传统方式
            if (surface != null) {
                GSYVideoManager.instance().setDisplay(surface);
            }
        }
    }
    
    /**
     * 使用 SurfaceControl.reparent 切换 Surface
     * 这是 GSY 官方 ExoPlayer 示例的核心方法
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void reparentExoSurface(SurfaceView surfaceView) {
        // 确保 SurfaceControl 已初始化
        if (mExoSurfaceControl == null) {
            // 如果 SurfaceControl 未初始化，回退到普通方式
            if (surfaceView != null) {
                super.setDisplay(surfaceView.getHolder().getSurface());
            }
            return;
        }
        
        try {
            if (surfaceView == null) {
                // reparent 到空，隐藏视频
                new SurfaceControl.Transaction()
                    .reparent(mExoSurfaceControl, null)
                    .setBufferSize(mExoSurfaceControl, 0, 0)
                    .setVisibility(mExoSurfaceControl, false)
                    .apply();
            } else {
                // reparent 到新的 SurfaceView
                SurfaceControl newParentSurfaceControl = surfaceView.getSurfaceControl();
                if (newParentSurfaceControl != null && newParentSurfaceControl.isValid()) {
                    new SurfaceControl.Transaction()
                        .reparent(mExoSurfaceControl, newParentSurfaceControl)
                        .setBufferSize(mExoSurfaceControl, surfaceView.getWidth(), surfaceView.getHeight())
                        .setVisibility(mExoSurfaceControl, true)
                        .apply();
                } else {
                    // SurfaceControl 无效，回退到普通方式
                    super.setDisplay(surfaceView.getHolder().getSurface());
                }
            }
        } catch (Exception e) {
            // 出错时回退到普通方式
            if (surfaceView != null) {
                super.setDisplay(surfaceView.getHolder().getSurface());
            }
        }
    }
    
    /**
     * 初始化 ExoPlayer 的 SurfaceControl
     * 在播放开始时调用
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.Q)
    private void initExoSurfaceControl() {
        if (mExoSurfaceControl != null) {
            return; // 已初始化
        }
        
        try {
            mExoSurfaceControl = new SurfaceControl.Builder()
                .setName(SURFACE_CONTROL_NAME)
                .setBufferSize(0, 0)
                .build();
            mExoVideoSurface = new Surface(mExoSurfaceControl);
            mUseExoSurfaceControl = true;
        } catch (Exception e) {
            mUseExoSurfaceControl = false;
        }
    }
    
    /**
     * 释放 ExoPlayer 的 SurfaceControl
     */
    private void releaseExoSurfaceControl() {
        if (mExoVideoSurface != null) {
            mExoVideoSurface.release();
            mExoVideoSurface = null;
        }
        if (mExoSurfaceControl != null) {
            mExoSurfaceControl.release();
            mExoSurfaceControl = null;
        }
        mUseExoSurfaceControl = false;
    }
    
    /**
     * 重写 releaseSurface - 关键方法
     * 
     * 在全屏切换时跳过 Surface 释放
     * ExoPlayer 使用 SurfaceControl 时不需要释放 Surface
     */
    @Override
    protected void releaseSurface(Surface surface) {
        // ExoPlayer 使用 SurfaceControl 时，不释放 Surface
        if (mUseExoSurfaceControl) {
            return;
        }
        
        if (mFullscreenHelper != null && mFullscreenHelper.isFullscreenTransitioning()) {
            return;
        }
        if (mEnteringPiPMode) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                return;
            }
        }
        super.releaseSurface(surface);
    }

    @Override
    public void onVideoPause() {
        long startTime = System.currentTimeMillis();
        android.util.Log.d(TAG, "onVideoPause: START at " + startTime 
            + ", enteringPiP=" + mEnteringPiPMode 
            + ", currentState=" + mCurrentPlayState
            + ", isPlaying=" + isPlaying());
        
        if (mEnteringPiPMode) {
            android.util.Log.d(TAG, "onVideoPause: skipped - entering PiP mode");
            return;
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                android.util.Log.d(TAG, "onVideoPause: skipped - in PiP mode");
                return;
            }
        }
        
        // 如果已经是暂停状态，不需要再次暂停
        if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            android.util.Log.d(TAG, "onVideoPause: skipped - already paused");
            return;
        }
        
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERED);
        
        // 直接调用 GSYVideoManager 暂停，确保立即生效
        android.util.Log.d(TAG, "onVideoPause: pausing GSYVideoManager");
        try {
            getGSYVideoManager().getPlayer().pause();
        } catch (Exception e) {
            android.util.Log.e(TAG, "onVideoPause: direct pause failed", e);
        }
        
        android.util.Log.d(TAG, "onVideoPause: calling super.onVideoPause");
        super.onVideoPause();
        
        if (shouldUpdateState) {
            mCurrentPlayState = PlayerConstants.STATE_PAUSED;
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PAUSED);
            if (mCurrentState != CURRENT_STATE_PAUSE) {
                mCurrentState = CURRENT_STATE_PAUSE;
            }
        }
        
        long endTime = System.currentTimeMillis();
        android.util.Log.d(TAG, "onVideoPause: END, took " + (endTime - startTime) + "ms, isPlaying=" + isPlaying());
    }

    @Override
    public void onVideoResume() {
        // 如果用户主动暂停了，不自动恢复播放
        if (mUserPaused) {
            android.util.Log.d(TAG, "onVideoResume: skipped because user paused");
            return;
        }
        
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
        // 停止播放历史自动保存
        stopPlayHistoryAutoSave();
        
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        if (mSkipManager != null) {
            mSkipManager.detachVideoView();
        }
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.detachVideoView();
        }
        // 释放 ExoPlayer 的 SurfaceControl
        releaseExoSurfaceControl();
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
        // 1. 先释放当前播放器
        GSYVideoManager.releaseAllVideos();
        
        // 2. 设置新的播放器工厂
        switch (engineType) {
            case PlayerConstants.ENGINE_IJK:
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
            case PlayerConstants.ENGINE_EXO:
                // 使用自定义的 OrangeExoPlayerManager，支持 SurfaceControl 无缝切换
                try {
                    PlayerFactory.setPlayManager(com.orange.playerlibrary.exo.OrangeExoPlayerManager.class);
                    // ExoPlayer 需要使用 SurfaceView 才能使用 SurfaceControl.reparent()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                            com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
                    }
                } catch (Exception e) {
                    // 回退到 GSY 原生 Exo2PlayerManager
                    try {
                        Class<?> exoClass = Class.forName("tv.danmaku.ijk.media.exo2.Exo2PlayerManager");
                        PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) exoClass);
                    } catch (ClassNotFoundException ex) {
                        PlayerFactory.setPlayManager(IjkPlayerManager.class);
                    }
                }
                break;
            case PlayerConstants.ENGINE_ALI:
                // GSY AliPlayer 类名: com.shuyu.aliplay.AliPlayerManager
                try {
                    Class<?> aliClass = Class.forName("com.shuyu.aliplay.AliPlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) aliClass);
                } catch (ClassNotFoundException e) {
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                // 使用自定义的 OrangeSystemPlayerManager，统一网速计算和 SurfaceControl 支持
                PlayerFactory.setPlayManager(com.orange.playerlibrary.player.OrangeSystemPlayerManager.class);
                // 系统播放器也需要使用 SurfaceView 才能使用 SurfaceControl.reparent()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                        com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
                }
                break;
        }
        
        // 3. 重置播放器初始化标志，确保下次播放时使用新的工厂
        mPlayerFactoryInitialized = true;
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
                } else if (playState == STATE_STARTSNIFFING) {
                    // 嗅探开始 - 显示加载动画
                    changeUiToSniffingShow();
                } else if (playState == STATE_ENDSNIFFING) {
                    // 嗅探结束 - 隐藏加载动画
                    changeUiToSniffingEnd();
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
        // 通过控制器分发进度更新给所有控制组件（包括弹幕）
        if (mOrangeController != null) {
            try {
                mOrangeController.setProgress(duration, position);
            } catch (Exception e) {
            }
        }
        
        // 保留直接调用以兼容旧代码
        if (mVodControlView != null) {
            try {
                mVodControlView.setProgress(duration, position);
            } catch (Exception e) {
            }
        }
        
        if (mLiveControlView != null) {
            try {
                mLiveControlView.setProgress(duration, position);
            } catch (Exception e) {
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
    
    // ==================== 播放历史功能 ====================
    
    /**
     * 启动播放历史自动保存
     */
    private void startPlayHistoryAutoSave() {
        android.util.Log.d(TAG, "startPlayHistoryAutoSave: mVideoUrl=" + mVideoUrl);
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            android.util.Log.d(TAG, "startPlayHistoryAutoSave: url is empty, skip");
            return;
        }
        
        String title = "";
        if (mOrangeController != null) {
            title = mOrangeController.getVideoTitle();
        }
        android.util.Log.d(TAG, "startPlayHistoryAutoSave: title=" + title);
        
        final OrangevideoView self = this;
        PlayHistoryManager.getInstance(getContext()).startAutoSave(
            mVideoUrl, 
            title,
            new PlayHistoryManager.ProgressProvider() {
                @Override
                public long getCurrentPosition() {
                    return self.getCurrentPositionWhenPlaying();
                }
                
                @Override
                public long getDuration() {
                    return self.getDuration();
                }
                
                @Override
                public View getVideoView() {
                    return self;
                }
            }
        );
    }
    
    /**
     * 停止播放历史自动保存
     */
    private void stopPlayHistoryAutoSave() {
        PlayHistoryManager.getInstance(getContext()).stopAutoSave();
    }
    
    /**
     * 获取播放历史中保存的进度
     */
    public long getHistoryProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return 0;
        }
        return PlayHistoryManager.getInstance(getContext()).getProgress(mVideoUrl);
    }
    
    /**
     * 检查是否有播放历史
     */
    public boolean hasPlayHistory() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        return PlayHistoryManager.getInstance(getContext()).getProgress(mVideoUrl) > 0;
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
    
    /**
     * 更新 SurfaceControl 尺寸（如果需要）
     * 用于视频比例切换后更新画面位置
     * 支持 ExoPlayer 和系统播放器
     */
    public void updateSurfaceControlIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            return;
        }
        
        try {
            String currentEngine = PlayerSettingsManager.getInstance(getContext()).getPlayerEngine();
            boolean isExoPlayer = PlayerConstants.ENGINE_EXO.equals(currentEngine);
            boolean isSystemPlayer = PlayerConstants.ENGINE_DEFAULT.equals(currentEngine);
            
            if (!isExoPlayer && !isSystemPlayer) {
                return;
            }
            
            com.shuyu.gsyvideoplayer.player.IPlayerManager playerManager = 
                GSYVideoManager.instance().getPlayer();
            
            if (mTextureView != null && mTextureView.getShowView() instanceof android.view.SurfaceView) {
                android.view.SurfaceView surfaceView = (android.view.SurfaceView) mTextureView.getShowView();
                
                if (isExoPlayer && playerManager instanceof com.orange.playerlibrary.exo.OrangeExoPlayerManager) {
                    com.orange.playerlibrary.exo.OrangeExoPlayerManager exoManager = 
                        (com.orange.playerlibrary.exo.OrangeExoPlayerManager) playerManager;
                    exoManager.updateSurfaceControlSize(surfaceView);
                } else if (isSystemPlayer && playerManager instanceof com.orange.playerlibrary.player.OrangeSystemPlayerManager) {
                    com.orange.playerlibrary.player.OrangeSystemPlayerManager systemManager = 
                        (com.orange.playerlibrary.player.OrangeSystemPlayerManager) playerManager;
                    systemManager.updateSurfaceControlSize(surfaceView);
                }
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * 更新 ExoPlayer 的 SurfaceControl 尺寸（如果需要）
     * @deprecated 使用 updateSurfaceControlIfNeeded() 代替
     */
    @Deprecated
    public void updateExoSurfaceControlIfNeeded() {
        updateSurfaceControlIfNeeded();
    }

    public boolean isLiveVideo() {
        return mIsLiveVideo;
    }

    public void setLiveVideo(boolean isLive) {
        this.mIsLiveVideo = isLive;
    }
    
    /**
     * 获取网络速度（字节/秒）
     * 使用 Android 系统 API 计算实时网速，因为 GSY 的 getNetSpeed 在某些播放器返回 0
     * @return 网络速度
     */
    public long getNetSpeed() {
        // 先尝试 GSY 的方法
        long gsySpeed = GSYVideoManager.instance().getNetSpeed();
        if (gsySpeed > 0) {
            return gsySpeed;
        }
        
        // GSY 返回 0，使用系统 API 计算（当前应用的 UID）
        long currentRxBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid());
        long currentTime = System.currentTimeMillis();
        
        // 处理不支持的情况
        if (currentRxBytes == android.net.TrafficStats.UNSUPPORTED) {
            return 0;
        }
        
        if (mLastRxBytes == 0 || mLastSpeedTime == 0) {
            mLastRxBytes = currentRxBytes;
            mLastSpeedTime = currentTime;
            return 0;
        }
        
        long timeDiff = currentTime - mLastSpeedTime;
        if (timeDiff <= 0) {
            mLastSpeedTime = currentTime;
            return 0;
        }
        
        long bytesDiff = currentRxBytes - mLastRxBytes;
        long speed = (bytesDiff * 1000) / timeDiff; // 字节/秒
        
        mLastRxBytes = currentRxBytes;
        mLastSpeedTime = currentTime;
        
        return Math.max(0, speed);
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

    /**
     * 嗅探监听器接口
     * 注意：这是一个独立接口，不继承 OnStateChangeListener
     * 通过 addOnStateChangeListener 添加时，会在内部检查是否实现此接口
     */
    public interface OnSniffingListener extends OnStateChangeListener {
        void onSniffingReceived(String contentType, java.util.HashMap<String, String> headers, 
                               String title, String url);
        void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize);
    }
    
    /**
     * 嗅探监听器适配器（提供默认空实现）
     */
    public static abstract class OnSniffingAdapter implements OnSniffingListener {
        @Override
        public void onPlayStateChanged(int playState) {}
        
        @Override
        public void onPlayerStateChanged(int playerState) {}
    }

    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    public boolean isDebug() {
        return mDebug;
    }

    protected void debug(Object message) {
        if (mDebug) {
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
    protected void touchSurfaceMove(float deltaX, float deltaY, float y) {
        super.touchSurfaceMove(deltaX, deltaY, y);
    }

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
                    
                    // 先隐藏原始播放器的控制器
                    hideController();
                    
                    // 强制刷新全屏播放器的控制器：先隐藏再显示
                    orangeFullPlayer.hideController();
                    orangeFullPlayer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            orangeFullPlayer.showController();
                            if (orangeFullPlayer.mTitleView != null) {
                                orangeFullPlayer.mTitleView.setVisibility(android.view.View.VISIBLE);
                                orangeFullPlayer.mTitleView.bringToFront();
                            }
                            if (orangeFullPlayer.mVodControlView != null) {
                                orangeFullPlayer.mVodControlView.setVisibility(android.view.View.VISIBLE);
                                orangeFullPlayer.mVodControlView.bringToFront();
                                orangeFullPlayer.mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                                // 强制刷新进度
                                if (mComponentStateManager != null) {
                                    int duration = (int) getDuration();
                                    int position = (int) getCurrentPositionWhenPlaying();
                                    orangeFullPlayer.mVodControlView.setProgress(duration, position);
                                }
                            }
                            orangeFullPlayer.requestLayout();
                        }
                    }, 100);
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
        // 移除全屏 View
        if (oldF != null && oldF.getParent() != null) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        
        // 恢复状态（与 GSY 基类保持一致）
        mCurrentState = getGSYVideoManager().getLastState();
        
        if (orangeVideoPlayer != null) {
            cloneParams(orangeVideoPlayer, this);
        }
        
        if (mCurrentState != CURRENT_STATE_NORMAL
            || mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
            createNetWorkState();
        }
        
        // 切换监听器（关键：让原始播放器接管）
        getGSYVideoManager().setListener(getGSYVideoManager().lastListener());
        getGSYVideoManager().setLastListener(null);
        setStateAndUi(mCurrentState);
        
        // 重新添加 TextureView（GSY 基类的标准做法）
        addTextureView();
        
        // 延迟恢复组件状态（不做 seekTo，避免 ExoPlayer 状态混乱）
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                }
                
                notifyComponentsPlayStateChanged(mCurrentPlayState);
                notifyComponentsPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
                
                // 强制刷新控制器：先隐藏再显示，确保使用正确的实例
                hideController();
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showController();
                        if (mVodControlView != null) {
                            mVodControlView.setVisibility(android.view.View.VISIBLE);
                            mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_NORMAL);
                            // 强制刷新进度
                            int duration = (int) getDuration();
                            int position = (int) getCurrentPositionWhenPlaying();
                            mVodControlView.setProgress(duration, position);
                        }
                        requestLayout();
                    }
                }, 100);
            }
        }, 300);
        
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
        GSYVideoManager.instance().initContext(getContext().getApplicationContext());
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

    @Override
    protected void changeUiToNormal() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    @Override
    protected void changeUiToPreparingShow() {
        // 视频加载时显示加载动画
        setViewShowState(mLoadingProgressBar, VISIBLE);
        startSpeedUpdate();
    }
    
    @Override
    protected void changeUiToPlayingShow() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        // 不停止网速更新，让它持续运行，updateLoadingSpeed 会根据 loading 可见性决定是否显示
    }
    
    @Override
    protected void changeUiToPlayingBufferingShow() {
        setViewShowState(mLoadingProgressBar, VISIBLE);
        startSpeedUpdate();
    }
    
    @Override
    protected void changeUiToPauseShow() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    @Override
    protected void changeUiToError() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    @Override
    protected void changeUiToCompleteShow() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    protected void changeUiToPrepareingClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    protected void changeUiToPlayingClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    protected void changeUiToPlayingBufferingClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    protected void changeUiToPauseClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    protected void changeUiToCompleteClear() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    /**
     * 嗅探开始 - 显示加载动画
     */
    protected void changeUiToSniffingShow() {
        setViewShowState(mLoadingProgressBar, VISIBLE);
        startSpeedUpdate();
    }
    
    /**
     * 嗅探结束 - 隐藏加载动画
     */
    protected void changeUiToSniffingEnd() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    @Override
    protected void hideAllWidget() {
        setViewShowState(mLoadingProgressBar, INVISIBLE);
        stopSpeedUpdate();
    }
    
    /**
     * 开始网速更新
     */
    private void startSpeedUpdate() {
        if (!mIsShowingLoading && mSpeedHandler != null) {
            mIsShowingLoading = true;
            // 重置网速计算初始值（使用当前应用的 UID）
            mLastRxBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid());
            if (mLastRxBytes == android.net.TrafficStats.UNSUPPORTED) {
                mLastRxBytes = 0;
            }
            mLastSpeedTime = System.currentTimeMillis();
            // 查找网速文本视图
            if (mLoadingSpeedText == null && mLoadingProgressBar != null) {
                if (mLoadingProgressBar instanceof android.view.ViewGroup) {
                    mLoadingSpeedText = ((android.view.ViewGroup) mLoadingProgressBar).findViewById(R.id.tv_loading_speed);
                } else {
                    android.view.ViewParent parent = mLoadingProgressBar.getParent();
                    if (parent instanceof android.view.ViewGroup) {
                        mLoadingSpeedText = ((android.view.ViewGroup) parent).findViewById(R.id.tv_loading_speed);
                    }
                }
            }
            if (mLoadingSpeedText != null) {
                mLoadingSpeedText.setVisibility(VISIBLE);
            }
            mSpeedHandler.post(mSpeedUpdateRunnable);
        }
    }
    
    /**
     * 停止网速更新
     */
    private void stopSpeedUpdate() {
        if (mIsShowingLoading && mSpeedHandler != null) {
            mIsShowingLoading = false;
            mSpeedHandler.removeCallbacks(mSpeedUpdateRunnable);
            if (mLoadingSpeedText != null) {
                mLoadingSpeedText.setVisibility(GONE);
            }
        }
    }
    
    /**
     * 更新网速显示
     * 只在网速大于 1 KB/s 且正在缓冲时显示
     */
    private void updateLoadingSpeed() {
        if (mLoadingSpeedText != null && mIsShowingLoading) {
            long speed = getNetSpeed();
            
            // 限制最大显示速度为 100 MB/s，避免异常值
            if (speed > 100 * 1024 * 1024) {
                speed = 100 * 1024 * 1024;
            }
            
            // 只在网速大于 1 KB/s (1024 字节) 时显示
            if (speed > 1024) {
                String speedText = formatSpeed(speed);
                mLoadingSpeedText.setText(speedText);
                mLoadingSpeedText.setVisibility(VISIBLE);
            } else {
                mLoadingSpeedText.setText("");
                mLoadingSpeedText.setVisibility(GONE);
            }
        }
    }
    
    /**
     * 设置加载动画指示器
     * 
     * @param indicator 指示器
     */
    public void setLoadingIndicator(com.orange.playerlibrary.loading.Indicator indicator) {
        if (mLoadingProgressBar != null) {
            com.orange.playerlibrary.loading.AVLoadingIndicatorView loadingView = null;
            if (mLoadingProgressBar instanceof android.view.ViewGroup) {
                loadingView = ((android.view.ViewGroup) mLoadingProgressBar).findViewById(R.id.loading_indicator);
            } else if (mLoadingProgressBar.getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) mLoadingProgressBar.getParent();
                loadingView = parent.findViewById(R.id.loading_indicator);
            }
            if (loadingView != null) {
                loadingView.setIndicator(indicator);
            }
        }
    }
    
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
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
            mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
            mCurrentPlayState == PlayerConstants.STATE_BUFFERED) {
            pause();
        } else if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            resume();
        } else {
        }
    }

    @Override
    public void startPlayLogic() {
        // 添加日志显示当前播放核心
        String currentEngine = PlayerSettingsManager.getInstance(getContext()).getPlayerEngine();
        com.shuyu.gsyvideoplayer.player.IPlayerManager playerManager = getGSYVideoManager().getPlayer();
        String playerClass = playerManager != null ? playerManager.getClass().getSimpleName() : "null";
        // ExoPlayer 使用 SurfaceControl 处理全屏切换 (Android Q+)
        if (PlayerConstants.ENGINE_EXO.equals(currentEngine) && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initExoSurfaceControl();
        } else {
            // 非 ExoPlayer，确保释放之前的 SurfaceControl
            releaseExoSurfaceControl();
        }
        
        prepareVideo();
    }

    @Override
    protected void prepareVideo() {
        // 添加日志
        String currentEngine = PlayerSettingsManager.getInstance(getContext()).getPlayerEngine();
        super.prepareVideo();
    }

    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
        setSpeed(sSpeed);
    }
    
    // ==================== 首帧预览功能 ====================
    
    /**
     * 异步获取视频首帧作为封面
     * 使用 MediaMetadataRetriever（IJK/系统播放器自带支持）
     */
    private void getVideoFirstFrameAsync(final String videoUrl) {
        // 检查是否启用自动缩略图
        if (!mAutoThumbnailEnabled) {
            return;
        }
        
        // 如果已设置默认缩略图，直接使用
        if (mDefaultThumbnail != null) {
            if (mOrangeController != null) {
                mOrangeController.setThumbnail(mDefaultThumbnail);
            }
            return;
        }
        
        // 避免重复加载
        if (mIsLoadingThumbnail) {
            return;
        }
        mIsLoadingThumbnail = true;
        
        // 使用AsyncTask异步获取首帧
        new android.os.AsyncTask<Void, Void, android.graphics.Bitmap>() {
            @Override
            protected android.graphics.Bitmap doInBackground(Void... voids) {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                try {
                    if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                        retriever.setDataSource(videoUrl, new java.util.HashMap<>());
                    } else {
                        retriever.setDataSource(videoUrl);
                    }
                    // 获取1秒处的帧（避免黑屏）
                    android.graphics.Bitmap bitmap = retriever.getFrameAtTime(
                            1000000, // 1秒 = 1000000微秒
                            android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    retriever.release();
                    return bitmap;
                } catch (Exception e) {
                    android.util.Log.e(TAG, "获取视频首帧失败: " + e.getMessage());
                    try {
                        retriever.release();
                    } catch (Exception ignored) {}
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(android.graphics.Bitmap bitmap) {
                mIsLoadingThumbnail = false;
                if (bitmap != null && mOrangeController != null) {
                    mOrangeController.setThumbnail(bitmap);
                    android.util.Log.d(TAG, "视频首帧获取成功");
                } else if (mDefaultThumbnail != null && mOrangeController != null) {
                    // 首帧获取失败，使用默认缩略图
                    mOrangeController.setThumbnail(mDefaultThumbnail);
                }
            }
        }.execute();
    }
    
    /**
     * 检查用户是否主动暂停
     */
    public boolean isUserPaused() {
        return mUserPaused;
    }
    
    /**
     * 清除用户暂停状态（用于外部控制恢复播放）
     */
    public void clearUserPausedState() {
        mUserPaused = false;
    }
}
