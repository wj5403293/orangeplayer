package com.orange.playerlibrary;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

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

/**
 * 橘子播放器视图
 * 继承 GSYBaseVideoPlayer，完全使用橘子组件 UI
 */
public class OrangevideoView extends GSYBaseVideoPlayer {

    private static final String TAG = "OrangevideoView";
    
    // ===== 状态常量 =====
    public static final int STATE_STARTSNIFFING = PlayerConstants.STATE_STARTSNIFFING;
    public static final int STATE_ENDSNIFFING = PlayerConstants.STATE_ENDSNIFFING;
    
    // ===== 全局 SQLite 存储 =====
    public static OrangeSharedSqlite sqlite;
    
    // ===== 成员变量 =====
    private String mVideoUrl;                     // 当前视频地址
    private Map<String, String> mVideoHeaders;    // 请求头
    private static float sSpeed = 1.0f;           // 当前倍速
    private static float sLongSpeed = 3.0f;       // 长按倍速
    private boolean mKeepVideoPlaying = false;    // 是否记忆播放位置
    private boolean mAutoThumbnailEnabled = true; // 是否启用自动缩略图
    private Object mDefaultThumbnail = null;      // 默认缩略图
    private boolean mIsLiveVideo = false;         // 是否直播
    private boolean mIsSniffing = false;          // 是否正在嗅探
    private boolean mAutoRotateOnFullscreen = true; // 全屏时是否自动旋转屏幕
    
    // ===== 跳过片头片尾管理器 =====
    private SkipManager mSkipManager;
    
    // ===== 视频比例管理器 =====
    private VideoScaleManager mVideoScaleManager;
    
    // ===== 播放状态管理器 =====
    private PlaybackStateManager mPlaybackStateManager;
    
    // ===== 组件状态管理器 =====
    private ComponentStateManager mComponentStateManager;
    
    // ===== 错误恢复管理器 =====
    private ErrorRecoveryManager mErrorRecoveryManager;
    
    // ===== 自定义全屏辅助类 =====
    private CustomFullscreenHelper mFullscreenHelper;
    
    // ===== ControlWrapper =====
    private com.orange.playerlibrary.interfaces.ControlWrapper mControlWrapper;
    
    // ===== 控制器 =====
    private OrangeVideoController mOrangeController;
    
    // ===== UI 组件 =====
    private com.orange.playerlibrary.component.PrepareView mPrepareView;
    private com.orange.playerlibrary.component.TitleView mTitleView;
    private com.orange.playerlibrary.component.VodControlView mVodControlView;
    private com.orange.playerlibrary.component.LiveControlView mLiveControlView;
    private com.orange.playerlibrary.component.CompleteView mCompleteView;
    private com.orange.playerlibrary.component.ErrorView mErrorView;
    private boolean mUseOrangeComponents = true; // 默认使用橘子组件
    
    // ===== 监听器 =====
    private final List<OnStateChangeListener> mStateChangeListeners = new ArrayList<>();
    private OnProgressListener mProgressListener;
    private OnPlayCompleteListener mPlayCompleteListener;
    
    // ===== 当前状态 =====
    private int mCurrentPlayState = PlayerConstants.STATE_IDLE;
    private int mCurrentPlayerState = PlayerConstants.PLAYER_NORMAL;
    
    // ===== 调试模式 =====
    private boolean mDebug = false;

    /**
     * 构造函数
     */
    public OrangevideoView(Context context) {
        super(context);
    }

    public OrangevideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrangevideoView(Context context, boolean fullFlag) {
        super(context, fullFlag);
    }

    /**
     * 初始化 - 重写父类方法
     */
    @Override
    protected void init(Context context) {
        super.init(context);
        initOrangePlayer();
    }

    /**
     * 橘子播放器初始化
     */
    private void initOrangePlayer() {
        // 强制启用橘子组件（解决初始化顺序问题）
        mUseOrangeComponents = true;
        
        // 初始化跳过管理器
        mSkipManager = new SkipManager();
        mSkipManager.attachVideoView(this);
        
        // 初始化视频比例管理器
        mVideoScaleManager = new VideoScaleManager(this, PlayerSettingsManager.getInstance(getContext()));
        android.util.Log.d(TAG, "initOrangePlayer: VideoScaleManager 初始化完成");
        
        // 初始化播放状态管理器
        mPlaybackStateManager = new PlaybackStateManager();
        android.util.Log.d(TAG, "initOrangePlayer: PlaybackStateManager 初始化完成");
        
        // 初始化组件状态管理器
        mComponentStateManager = new ComponentStateManager();
        android.util.Log.d(TAG, "initOrangePlayer: ComponentStateManager 初始化完成");
        
        // 初始化错误恢复管理器
        mErrorRecoveryManager = new ErrorRecoveryManager();
        mErrorRecoveryManager.attachVideoView(this);
        android.util.Log.d(TAG, "initOrangePlayer: ErrorRecoveryManager 初始化完成");
        
        // 初始化自定义全屏辅助类
        mFullscreenHelper = new CustomFullscreenHelper(this);
        android.util.Log.d(TAG, "initOrangePlayer: CustomFullscreenHelper 初始化完成");
        
        // 禁用旋转动画，直接切换
        setShowFullAnimation(false);
        setRotateViewAuto(false);
        setNeedLockFull(false);
        setLockLand(false);
        setRotateWithSystem(false);
        // 禁用全屏切换动画
        setNeedShowWifiTip(false);
        // 禁用 OrientationUtils，不旋转屏幕
        setNeedOrientationUtils(false);
        
        // 启用触摸手势（双击暂停/播放、滑动调节音量/亮度/进度）
        setIsTouchWiget(true);
        setIsTouchWigetFull(true);
        
        // 默认初始化橘子组件
        if (mUseOrangeComponents) {
            initOrangeComponents();
        }
        
        // 使用 ComponentStateManager 注册进度监听器
        // 这样可以确保监听器不重复注册，并在组件重建后自动重新注册
        if (mComponentStateManager != null) {
            mComponentStateManager.reregisterProgressListener(this);
            android.util.Log.d(TAG, "initOrangePlayer: 进度监听器已通过 ComponentStateManager 注册");
        }
        
        // 设置回调监听
        setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PREPARED);
                // 检查是否为直播
                if (getDuration() <= 0) {
                    mIsLiveVideo = true;
                }
                // 应用保存的视频比例设置
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                    android.util.Log.d(TAG, "onPrepared: 视频比例已应用");
                }
                
                // 检查是否需要恢复全屏切换时保存的播放位置
                if (mFullscreenHelper != null && mFullscreenHelper.getPendingSeekPosition() > 0) {
                    final long pendingPosition = mFullscreenHelper.getPendingSeekPosition();
                    final boolean pendingResume = mFullscreenHelper.isPendingResume();
                    android.util.Log.d(TAG, "onPrepared: 检测到待恢复位置=" + pendingPosition + ", pendingResume=" + pendingResume);
                    
                    // 清除待恢复状态
                    mFullscreenHelper.clearPendingSeekPosition();
                    
                    // 延迟执行 seekTo，确保播放器完全准备好
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            android.util.Log.d(TAG, "onPrepared: 恢复播放位置到 " + pendingPosition);
                            seekTo(pendingPosition);
                            
                            // 如果之前在播放，确保继续播放
                            if (pendingResume && !isPlaying()) {
                                android.util.Log.d(TAG, "onPrepared: 恢复播放状态");
                                resume();
                            }
                        }
                    }, 100);
                } else {
                    // 恢复播放进度（非全屏切换的情况）
                    if (mKeepVideoPlaying) {
                        restorePlaybackProgress();
                    }
                }
                // 执行跳过片头
                if (mSkipManager != null) {
                    mSkipManager.performSkipIntro();
                }
                // 准备完成后自动进入播放状态
                setOrangePlayState(PlayerConstants.STATE_PLAYING);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PLAYBACK_COMPLETED);
                // 播放完成，清除保存的进度
                if (mKeepVideoPlaying) {
                    clearSavedProgress();
                }
                // 重置跳过状态
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
                // 不旋转屏幕，保持竖屏
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
                // 不旋转屏幕，保持竖屏
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
     * 启用橘子组件 UI（替代 GSY 内置 UI）
     * 调用此方法后会添加 PrepareView、TitleView、VodControlView、CompleteView、ErrorView 等组件
     */
    // ===== 调试日志回调 =====
    private DebugLogCallback mDebugLogCallback;

    public interface DebugLogCallback {
        void onLog(String msg);
    }

    public void setDebugLogCallback(DebugLogCallback callback) {
        mDebugLogCallback = callback;
    }

    public void enableOrangeComponents() {
        if (mUseOrangeComponents) return;
        mUseOrangeComponents = true;
        initOrangeComponents();
    }

    /**
     * 初始化橘子组件
     */
    private void initOrangeComponents() {
        Context context = getContext();
        android.widget.RelativeLayout.LayoutParams matchParentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);

        debugLog("橘子组件初始化开始");

        // 创建 ControlWrapper 并保存到成员变量
        mControlWrapper = createControlWrapper();
        debugLog("ControlWrapper 创建完成");

        // 1. PrepareView - 准备/加载视图
        mPrepareView = new com.orange.playerlibrary.component.PrepareView(context);
        mPrepareView.attach(mControlWrapper);
        mPrepareView.setClickStart(); // 设置点击开始播放
        addView(mPrepareView, matchParentParams);
        debugLog("PrepareView 添加完成");

        // 2. CompleteView - 播放完成视图
        mCompleteView = new com.orange.playerlibrary.component.CompleteView(context);
        mCompleteView.attach(mControlWrapper);
        addView(mCompleteView, matchParentParams);

        // 3. ErrorView - 错误视图
        mErrorView = new com.orange.playerlibrary.component.ErrorView(context);
        mErrorView.attach(mControlWrapper);
        addView(mErrorView, matchParentParams);

        // 4. TitleView - 标题栏（全屏时显示）
        mTitleView = new com.orange.playerlibrary.component.TitleView(context);
        mTitleView.attach(mControlWrapper);
        addView(mTitleView, matchParentParams);

        // 5. VodControlView - 点播控制栏
        mVodControlView = new com.orange.playerlibrary.component.VodControlView(context);
        mVodControlView.attach(mControlWrapper);
        addView(mVodControlView, matchParentParams);
        debugLog("VodControlView 添加完成");

        // 初始状态设置为 IDLE，显示准备界面
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        debugLog("橘子组件初始化完成");
    }

    /**
     * 添加调试日志
     */
    public void debugLog(String msg) {
        if (mDebugLogCallback != null) {
            mDebugLogCallback.onLog(msg);
        }
        android.util.Log.d(TAG, msg);
    }

    /**
     * 创建 ControlWrapper 实现
     */
    private com.orange.playerlibrary.interfaces.ControlWrapper createControlWrapper() {
        final OrangevideoView videoView = this;
        return new com.orange.playerlibrary.interfaces.ControlWrapper() {
            @Override
            public void start() {
                videoView.startPlayLogic();
            }

            @Override
            public void pause() {
                // 调用橘子播放器的 pause() 方法，而不是直接调用 onVideoPause()
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
                // 使用橘子播放器的状态，而不是 GSY 的状态
                return videoView.isPlaying();
            }

            @Override
            public void togglePlay() {
                if (isPlaying()) {
                    pause();
                } else {
                    // 使用 resume() 方法而不是 onVideoResume()，确保状态正确更新
                    videoView.resume();
                }
            }

            @Override
            public void toggleFullScreen() {
                android.util.Log.d(TAG, "toggleFullScreen: isFullScreen=" + isFullScreen());
                if (isFullScreen()) {
                    // 退出全屏 - 使用自定义全屏辅助类
                    android.util.Log.d(TAG, "toggleFullScreen: 调用 CustomFullscreenHelper.exitFullscreen()");
                    Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.exitFullscreen(activity);
                    }
                } else {
                    // 进入全屏 - 使用自定义全屏辅助类
                    android.util.Log.d(TAG, "toggleFullScreen: 调用 CustomFullscreenHelper.enterFullscreen()");
                    Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.enterFullscreen(activity);
                    }
                }
            }

            @Override
            public void toggleLockState() {
                // GSY 不支持锁屏
            }

            @Override
            public boolean isFullScreen() {
                // 使用自定义全屏辅助类判断
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
            public void setMute(boolean isMute) {
                // 静音功能暂不实现
            }

            @Override
            public boolean isMute() {
                return false;
            }

            @Override
            public void setVolume(float volume) {
                // GSY 不直接支持
            }

            @Override
            public void replay(boolean resetPosition) {
                if (resetPosition) {
                    videoView.seekTo(0);
                }
                videoView.startPlayLogic();
            }

            @Override
            public void hide() {
                // 隐藏控制器
            }

            @Override
            public void show() {
                // 显示控制器
            }

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

    /**
     * 是否使用橘子组件
     */
    public boolean isUseOrangeComponents() {
        return mUseOrangeComponents;
    }

    /**
     * 获取 PrepareView
     */
    public com.orange.playerlibrary.component.PrepareView getPrepareView() {
        return mPrepareView;
    }

    /**
     * 获取 TitleView
     */
    public com.orange.playerlibrary.component.TitleView getTitleView() {
        return mTitleView;
    }

    /**
     * 获取 VodControlView
     */
    public com.orange.playerlibrary.component.VodControlView getVodControlView() {
        return mVodControlView;
    }

    /**
     * 获取 LiveControlView
     */
    public com.orange.playerlibrary.component.LiveControlView getLiveControlView() {
        return mLiveControlView;
    }

    /**
     * 获取 CompleteView
     */
    public com.orange.playerlibrary.component.CompleteView getCompleteView() {
        return mCompleteView;
    }

    /**
     * 获取 ErrorView
     */
    public com.orange.playerlibrary.component.ErrorView getErrorView() {
        return mErrorView;
    }

    /**
     * 获取 ControlWrapper
     * 用于外部调用全屏切换等功能
     */
    public com.orange.playerlibrary.interfaces.ControlWrapper getControlWrapper() {
        return mControlWrapper;
    }

    // ===== 视频地址设置方法 (Requirements: 1.2) =====
    
    /**
     * 设置视频地址
     * @param url 视频地址
     */
    public void setUrl(String url) {
        setUrl(url, null);
    }

    /**
     * 设置视频地址和请求头
     * @param url 视频地址
     * @param headers 请求头
     */
    public void setUrl(String url, Map<String, String> headers) {
        this.mVideoUrl = url;
        this.mVideoHeaders = headers;
        // 使用 GSYVideoPlayer 的 setUp 方法
        if (headers != null) {
            setUp(url, true, null, headers, "");
        } else {
            setUp(url, true, "");
        }
    }

    /**
     * 获取当前视频地址
     * @return 视频地址
     */
    public String getUrl() {
        return mVideoUrl;
    }

    // ===== 播放控制方法 (Requirements: 1.3, 1.4, 1.5, 1.6) =====

    /**
     * 开始播放
     */
    public void start() {
        mIsSniffing = false;
        mIsLiveVideo = false;
        // 重置跳过状态
        if (mSkipManager != null) {
            mSkipManager.reset();
        }
        // 启动错误恢复检测
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.startBlackScreenDetection();
            mErrorRecoveryManager.startStateConsistencyCheck();
        }
        setOrangePlayState(PlayerConstants.STATE_PREPARING);
        startPlayLogic();
    }

    /**
     * 暂停播放
     */
    public void pause() {
        android.util.Log.d(TAG, "pause() 被调用, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
        // 保存播放进度
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 停止检查片尾
        if (mSkipManager != null) {
            mSkipManager.stopOutroCheck();
        }
        // 调用 GSY 的暂停方法（会触发 onVideoPause，在那里更新状态）
        onVideoPause();
        android.util.Log.d(TAG, "pause() 完成, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
    }

    /**
     * 继续播放
     */
    public void resume() {
        android.util.Log.d(TAG, "resume() 被调用, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
        // 调用 GSY 的恢复播放方法（会触发 onVideoResume，在那里更新状态）
        onVideoResume();
        android.util.Log.d(TAG, "resume() 完成, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
        // 开始检查片尾
        if (mSkipManager != null) {
            mSkipManager.startOutroCheck();
        }
    }

    /**
     * 重写 GSY 的 onVideoPause 方法
     * 当 GSY 内部调用暂停时（如双击），确保更新橘子播放器状态
     * 如果当前已经是暂停状态，则恢复播放（修复双击恢复播放的问题）
     */
    @Override
    public void onVideoPause() {
        android.util.Log.d(TAG, "onVideoPause() 被调用, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
        
        // 如果当前已经是暂停状态，说明这是第二次双击，应该恢复播放
        if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            android.util.Log.d(TAG, "onVideoPause() 检测到已暂停，直接恢复播放");
            // 不调用 onVideoResume()，直接调用 super.onVideoResume() 并更新状态
            super.onVideoResume();
            android.util.Log.d(TAG, "onVideoPause() 调用 super.onVideoResume() 后, mCurrentState=" + mCurrentState);
            // 更新橘子播放器状态
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 直接通知组件，不使用 post，避免延迟导致状态混乱
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            android.util.Log.d(TAG, "onVideoPause() 已更新为播放状态");
            return;
        }
        
        // 只在播放状态时才暂停
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERED);
        super.onVideoPause();
        android.util.Log.d(TAG, "onVideoPause() super调用后, mCurrentState=" + mCurrentState);
        if (shouldUpdateState) {
            android.util.Log.d(TAG, "onVideoPause() 更新橘子状态为暂停");
            // 直接更新橘子播放器状态，不触发其他回调
            mCurrentPlayState = PlayerConstants.STATE_PAUSED;
            // 直接通知组件，不使用 post，避免延迟导致状态混乱
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PAUSED);
            // 确保 GSY 的 mCurrentState 保持为暂停状态
            if (mCurrentState != CURRENT_STATE_PAUSE) {
                android.util.Log.d(TAG, "onVideoPause() 强制设置 mCurrentState 为暂停");
                mCurrentState = CURRENT_STATE_PAUSE;
            }
        } else {
            android.util.Log.d(TAG, "onVideoPause() 跳过状态更新，当前已是暂停状态");
        }
    }

    /**
     * 重写 GSY 的 onVideoResume 方法
     * 当 GSY 内部调用恢复播放时（如双击），确保更新橘子播放器状态
     */
    @Override
    public void onVideoResume() {
        android.util.Log.d(TAG, "onVideoResume() 被调用, mCurrentPlayState=" + mCurrentPlayState + ", mCurrentState=" + mCurrentState);
        // 只在暂停状态时才更新为播放状态，避免重复设置
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PAUSED);
        super.onVideoResume();
        android.util.Log.d(TAG, "onVideoResume() super调用后, mCurrentState=" + mCurrentState);
        if (shouldUpdateState) {
            android.util.Log.d(TAG, "onVideoResume() 更新橘子状态为播放");
            // 直接更新橘子播放器状态，不触发其他回调
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 直接通知组件，不使用 post，避免延迟导致状态混乱
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            // 确保 GSY 的 mCurrentState 保持为播放状态
            if (mCurrentState != CURRENT_STATE_PLAYING) {
                android.util.Log.d(TAG, "onVideoResume() 强制设置 mCurrentState 为播放");
                mCurrentState = CURRENT_STATE_PLAYING;
            }
        } else {
            android.util.Log.d(TAG, "onVideoResume() 跳过状态更新，当前不是暂停状态: " + mCurrentPlayState);
        }
    }

    /**
     * 释放播放器资源
     */
    @Override
    public void release() {
        // 保存播放进度
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 清理跳过管理器
        if (mSkipManager != null) {
            mSkipManager.detachVideoView();
        }
        // 清理错误恢复管理器
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.detachVideoView();
        }
        super.release();
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        GSYVideoManager.releaseAllVideos();
    }

    // ===== 进度控制方法 (Requirements: 1.7, 1.8) =====

    /**
     * 获取当前播放位置（兼容原 API）
     * @return 当前位置（毫秒）
     */
    public long getCurrentPosition() {
        return getCurrentPositionWhenPlaying();
    }

    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    public void seekTo(int position) {
        seekTo((long) position);
    }

    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    public void seekTo(long position) {
        android.util.Log.d(TAG, "seekTo: position=" + position);
        
        // 首先尝试使用 GSYVideoManager
        if (GSYVideoManager.instance().getPlayer() != null) {
            android.util.Log.d(TAG, "seekTo: 使用 GSYVideoManager.seekTo");
            GSYVideoManager.instance().getPlayer().seekTo(position);
        } else {
            // 如果 GSYVideoManager 不可用，尝试使用父类方法
            android.util.Log.d(TAG, "seekTo: GSYVideoManager.getPlayer() 为 null，尝试使用 seekOnStart");
            setSeekOnStart(position);
        }
    }

    // ===== 倍速控制方法 (Requirements: 1.9) =====

    /**
     * 设置播放倍速
     * @param speed 倍速值 (0.5 - 3.0)
     */
    @Override
    public void setSpeed(float speed) {
        // 限制倍速范围
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
        super.setSpeed(speed);
    }

    /**
     * 获取当前倍速
     * @return 当前倍速
     */
    public static float getSpeeds() {
        return sSpeed;
    }

    /**
     * 设置倍速（静态方法）
     * @param speed 倍速值
     */
    public static void setSpeeds(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
    }

    /**
     * 获取长按倍速
     * @return 长按倍速
     */
    public static float getLongSpeeds() {
        return sLongSpeed;
    }

    /**
     * 设置长按倍速
     * @param speed 长按倍速
     */
    public static void setLongSpeeds(float speed) {
        sLongSpeed = speed;
    }


    // ===== 全屏控制方法 (Requirements: 1.10) =====

    /**
     * 进入全屏（兼容原 API）
     */
    public void startFullScreen() {
        android.util.Log.d(TAG, "startFullScreen: 使用自定义全屏辅助类");
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.enterFullscreen(activity);
        }
    }

    /**
     * 退出全屏
     */
    public void stopFullScreen() {
        android.util.Log.d(TAG, "stopFullScreen: 使用自定义全屏辅助类");
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.exitFullscreen(activity);
        }
    }

    /**
     * 是否全屏
     * @return true 全屏
     */
    public boolean isFullScreen() {
        return mFullscreenHelper != null && mFullscreenHelper.isFullscreen();
    }

    /**
     * 是否小窗模式
     * @return true 小窗
     */
    public boolean isTinyScreen() {
        return mCurrentPlayerState == PlayerConstants.PLAYER_TINY_SCREEN;
    }

    /**
     * 设置全屏时是否自动旋转屏幕
     * @param autoRotate true 自动旋转（默认）
     */
    public void setAutoRotateOnFullscreen(boolean autoRotate) {
        this.mAutoRotateOnFullscreen = autoRotate;
    }

    /**
     * 是否全屏时自动旋转屏幕
     * @return true 自动旋转
     */
    public boolean isAutoRotateOnFullscreen() {
        return mAutoRotateOnFullscreen;
    }

    // ===== 播放内核切换 (Requirements: 1.11) =====

    /**
     * 选择播放内核
     * @param engineType 内核类型 (ijk, exo, ali, default)
     */
    @SuppressWarnings("unchecked")
    public void selectPlayerFactory(String engineType) {
        if (engineType == null) {
            engineType = PlayerConstants.ENGINE_DEFAULT;
        }
        
        switch (engineType) {
            case PlayerConstants.ENGINE_IJK:
                // IJK 播放器
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
            case PlayerConstants.ENGINE_EXO:
                // ExoPlayer - 需要额外依赖
                try {
                    Class<?> exoClass = Class.forName("com.shuyu.gsyvideoplayer.player.Exo2PlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) exoClass);
                } catch (ClassNotFoundException e) {
                    // 回退到默认
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_ALI:
                // 阿里云播放器 - 需要额外依赖
                try {
                    Class<?> aliClass = Class.forName("com.shuyu.gsyvideoplayer.player.AliPlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) aliClass);
                } catch (ClassNotFoundException e) {
                    // 回退到默认
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                // 使用系统 MediaPlayer
                PlayerFactory.setPlayManager(SystemPlayerManager.class);
                break;
        }
    }

    // ===== 状态管理 =====

    /**
     * 设置播放状态
     * @param playState 播放状态
     */
    protected void setOrangePlayState(int playState) {
        mCurrentPlayState = playState;
        notifyPlayStateChanged(playState);
        
        // 使用 post 确保在组件状态更新后再显示/隐藏控制器
        post(new Runnable() {
            @Override
            public void run() {
                // 播放状态时显示控制器并启动自动隐藏定时器
                if (playState == PlayerConstants.STATE_PLAYING) {
                    showController();
                } else if (playState == PlayerConstants.STATE_PAUSED) {
                    // 暂停时显示控制器但不自动隐藏
                    showController();
                    cancelAutoHideTimer();
                } else {
                    cancelAutoHideTimer();
                }
            }
        });
    }

    /**
     * 设置播放器状态
     * @param playerState 播放器状态
     */
    protected void setOrangePlayerState(int playerState) {
        mCurrentPlayerState = playerState;
        notifyPlayerStateChanged(playerState);
    }

    /**
     * 获取当前播放状态
     * @return 播放状态
     */
    public int getPlayState() {
        return mCurrentPlayState;
    }

    /**
     * 获取当前播放器状态
     * @return 播放器状态
     */
    public int getPlayerState() {
        return mCurrentPlayerState;
    }

    // ===== 监听器管理 =====

    /**
     * 添加状态监听器
     * @param listener 监听器
     */
    public void addOnStateChangeListener(OnStateChangeListener listener) {
        if (listener != null && !mStateChangeListeners.contains(listener)) {
            mStateChangeListeners.add(listener);
        }
    }

    /**
     * 移除状态监听器
     * @param listener 监听器
     */
    public void removeOnStateChangeListener(OnStateChangeListener listener) {
        mStateChangeListeners.remove(listener);
    }

    /**
     * 清除所有状态监听器
     */
    public void clearOnStateChangeListeners() {
        mStateChangeListeners.clear();
    }

    /**
     * 通知播放状态改变
     */
    private void notifyPlayStateChanged(int playState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayStateChanged(playState);
            }
        }
        // 通知橘子组件
        if (mUseOrangeComponents) {
            notifyComponentsPlayStateChanged(playState);
        }
    }

    /**
     * 通知播放器状态改变
     */
    private void notifyPlayerStateChanged(int playerState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayerStateChanged(playerState);
            }
        }
        // 通知橘子组件
        if (mUseOrangeComponents) {
            notifyComponentsPlayerStateChanged(playerState);
        }
    }

    /**
     * 通知组件播放状态改变
     */
    private void notifyComponentsPlayStateChanged(int playState) {
        debugLog("PlayState=" + playState + " PrepareView=" + (mPrepareView != null ? mPrepareView.getVisibility() : "null"));
        if (mPrepareView != null) mPrepareView.onPlayStateChanged(playState);
        if (mCompleteView != null) mCompleteView.onPlayStateChanged(playState);
        if (mErrorView != null) mErrorView.onPlayStateChanged(playState);
        if (mTitleView != null) mTitleView.onPlayStateChanged(playState);
        if (mVodControlView != null) mVodControlView.onPlayStateChanged(playState);
        if (mLiveControlView != null) mLiveControlView.onPlayStateChanged(playState);
        debugLog("After: PrepareView=" + (mPrepareView != null ? mPrepareView.getVisibility() : "null") + " VodCtrl=" + (mVodControlView != null ? mVodControlView.getVisibility() : "null"));
    }

    /**
     * 通知组件播放器状态改变
     */
    private void notifyComponentsPlayerStateChanged(int playerState) {
        debugLog("PlayerState=" + playerState);
        if (mPrepareView != null) mPrepareView.onPlayerStateChanged(playerState);
        if (mCompleteView != null) mCompleteView.onPlayerStateChanged(playerState);
        if (mErrorView != null) mErrorView.onPlayerStateChanged(playerState);
        if (mTitleView != null) mTitleView.onPlayerStateChanged(playerState);
        if (mVodControlView != null) mVodControlView.onPlayerStateChanged(playerState);
        if (mLiveControlView != null) mLiveControlView.onPlayerStateChanged(playerState);
    }

    /**
     * 更新组件进度
     * Requirements: 3.2, 3.3, 6.4
     * 
     * 优化说明：
     * - 添加空指针检查，确保组件已初始化
     * - 确保在主线程更新 UI
     * - 处理组件未初始化的情况
     */
    public void updateComponentsProgress(int duration, int position) {
        // 空指针检查：确保组件已初始化
        if (mVodControlView == null && mLiveControlView == null) {
            android.util.Log.w(TAG, "updateComponentsProgress: 所有控制组件都未初始化");
            return;
        }
        
        // 确保在主线程更新 UI
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            // 如果不在主线程，post 到主线程执行
            final int finalDuration = duration;
            final int finalPosition = position;
            post(new Runnable() {
                @Override
                public void run() {
                    updateComponentsProgressInternal(finalDuration, finalPosition);
                }
            });
        } else {
            // 已在主线程，直接执行
            updateComponentsProgressInternal(duration, position);
        }
    }
    
    /**
     * 内部方法：更新组件进度（必须在主线程调用）
     */
    private void updateComponentsProgressInternal(int duration, int position) {
        // 更新点播控制组件
        if (mVodControlView != null) {
            try {
                mVodControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: VodControlView 更新失败", e);
            }
        }
        
        // 更新直播控制组件
        if (mLiveControlView != null) {
            try {
                mLiveControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: LiveControlView 更新失败", e);
            }
        }
    }

    /**
     * 设置进度监听器
     * @param listener 监听器
     */
    public void setOnProgressListener(OnProgressListener listener) {
        this.mProgressListener = listener;
    }

    /**
     * 设置播放完成监听器
     * @param listener 监听器
     */
    public void setOnPlayCompleteListener(OnPlayCompleteListener listener) {
        this.mPlayCompleteListener = listener;
    }


    // ===== 控制器管理 =====

    /**
     * 获取视频控制器
     * @return 控制器
     */
    public OrangeVideoController getVideoController() {
        return mOrangeController;
    }

    /**
     * 设置视频控制器
     * @param controller 控制器
     */
    public void setVideoController(OrangeVideoController controller) {
        this.mOrangeController = controller;
        
        // 通知控制器关联的播放器视图，以便初始化 VideoEventManager
        if (controller != null) {
            controller.setVideoView(this);
            
            // 绑定 TitleView 事件
            if (mTitleView != null) {
                mTitleView.setController(controller);
                debugLog("TitleView 事件绑定完成");
            }
        }
    }

    // ===== 特殊功能 =====

    /**
     * 设置是否启用自动获取缩略图功能
     * Requirements: 6.2 - THE OrangevideoView SHALL 支持自动获取视频缩略图功能
     * @param enabled true 启用，false 禁用
     */
    public void setAutoThumbnailEnabled(boolean enabled) {
        this.mAutoThumbnailEnabled = enabled;
    }

    /**
     * 是否启用自动缩略图
     * @return true 启用
     */
    public boolean isAutoThumbnailEnabled() {
        return mAutoThumbnailEnabled;
    }

    /**
     * 设置默认缩略图
     * @param thumbnail 缩略图
     */
    public void setDefaultThumbnail(Object thumbnail) {
        this.mDefaultThumbnail = thumbnail;
    }

    /**
     * 获取默认缩略图
     * @return 缩略图
     */
    public Object getDefaultThumbnail() {
        return mDefaultThumbnail;
    }

    /**
     * 异步获取视频第一帧作为缩略图
     * Requirements: 6.2 - THE OrangevideoView SHALL 支持自动获取视频缩略图功能
     * @param callback 回调
     */
    public void getVideoFirstFrameAsync(VideoThumbnailHelper.ThumbnailCallback callback) {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Video URL is empty");
            }
            return;
        }
        VideoThumbnailHelper.getVideoFirstFrameAsync(mVideoUrl, mVideoHeaders, callback);
    }

    /**
     * 异步获取指定时间的视频帧
     * @param timeUs 时间（微秒）
     * @param callback 回调
     */
    public void getFrameAtTimeAsync(long timeUs, VideoThumbnailHelper.ThumbnailCallback callback) {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Video URL is empty");
            }
            return;
        }
        VideoThumbnailHelper.getFrameAtTimeAsync(mVideoUrl, timeUs, mVideoHeaders, callback);
    }

    /**
     * 自动加载缩略图（如果启用）
     */
    private void autoLoadThumbnail() {
        if (!mAutoThumbnailEnabled || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        
        getVideoFirstFrameAsync(new VideoThumbnailHelper.ThumbnailCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap bitmap) {
                mDefaultThumbnail = bitmap;
                // 设置封面
                if (mThumbImageView != null && mThumbImageView instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) mThumbImageView).setImageBitmap(bitmap);
                }
                debug("Auto thumbnail loaded successfully");
            }

            @Override
            public void onError(String error) {
                debug("Auto thumbnail load failed: " + error);
            }
        });
    }

    /**
     * 设置是否记忆播放位置
     * Requirements: 6.3 - THE OrangevideoView SHALL 支持记忆播放位置功能 (setKeepVideoPlaying)
     * @param keep true 记忆
     */
    public void setKeepVideoPlaying(boolean keep) {
        this.mKeepVideoPlaying = keep;
    }

    /**
     * 是否记忆播放位置
     * @return true 记忆
     */
    public boolean isKeepVideoPlaying() {
        return mKeepVideoPlaying;
    }

    /**
     * 保存当前播放进度
     * Requirements: 6.3
     */
    public void savePlaybackProgress() {
        if (!mKeepVideoPlaying || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        
        long position = getCurrentPosition();
        long duration = getDuration();
        
        if (position > 0 && duration > 0) {
            PlaybackProgressManager.getInstance(getContext())
                    .saveProgress(mVideoUrl, position, duration);
            debug("Saved playback progress: " + position + "/" + duration);
        }
    }

    /**
     * 恢复播放进度
     * Requirements: 6.3
     * @return true 成功恢复
     */
    public boolean restorePlaybackProgress() {
        if (!mKeepVideoPlaying || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        
        PlaybackProgressManager manager = PlaybackProgressManager.getInstance(getContext());
        long resumePosition = manager.getResumePosition(mVideoUrl);
        
        if (resumePosition > 0) {
            seekTo(resumePosition);
            debug("Restored playback progress: " + resumePosition);
            return true;
        }
        return false;
    }

    /**
     * 获取保存的播放进度
     * @return 播放位置（毫秒）
     */
    public long getSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return 0;
        }
        return PlaybackProgressManager.getInstance(getContext()).getProgress(mVideoUrl);
    }

    /**
     * 检查是否有保存的进度
     * @return true 有保存的进度
     */
    public boolean hasSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        return PlaybackProgressManager.getInstance(getContext()).hasProgress(mVideoUrl);
    }

    /**
     * 清除当前视频的保存进度
     */
    public void clearSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        PlaybackProgressManager.getInstance(getContext()).removeProgress(mVideoUrl);
    }

    // ===== 跳过片头片尾功能 (Requirements: 6.4) =====

    /**
     * 设置跳过片头时长
     * Requirements: 6.4 - THE OrangevideoView SHALL 支持跳过片头片尾功能
     * @param timeMs 时长（毫秒）
     */
    public void setSkipIntroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroTime(timeMs);
        }
    }

    /**
     * 设置跳过片头时长（秒）
     * @param seconds 时长（秒）
     */
    public void setSkipIntroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroSeconds(seconds);
        }
    }

    /**
     * 获取跳过片头时长
     * @return 时长（毫秒）
     */
    public long getSkipIntroTime() {
        return mSkipManager != null ? mSkipManager.getSkipIntroTime() : 0;
    }

    /**
     * 设置是否启用跳过片头
     * @param enabled 是否启用
     */
    public void setSkipIntroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroEnabled(enabled);
        }
    }

    /**
     * 是否启用跳过片头
     * @return true 启用
     */
    public boolean isSkipIntroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipIntroEnabled();
    }

    /**
     * 设置跳过片尾时长
     * Requirements: 6.4 - THE OrangevideoView SHALL 支持跳过片头片尾功能
     * @param timeMs 时长（毫秒）
     */
    public void setSkipOutroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroTime(timeMs);
        }
    }

    /**
     * 设置跳过片尾时长（秒）
     * @param seconds 时长（秒）
     */
    public void setSkipOutroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroSeconds(seconds);
        }
    }

    /**
     * 获取跳过片尾时长
     * @return 时长（毫秒）
     */
    public long getSkipOutroTime() {
        return mSkipManager != null ? mSkipManager.getSkipOutroTime() : 0;
    }

    /**
     * 设置是否启用跳过片尾
     * @param enabled 是否启用
     */
    public void setSkipOutroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroEnabled(enabled);
        }
    }

    /**
     * 是否启用跳过片尾
     * @return true 启用
     */
    public boolean isSkipOutroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipOutroEnabled();
    }

    /**
     * 设置跳过监听器
     * @param listener 监听器
     */
    public void setOnSkipListener(SkipManager.OnSkipListener listener) {
        if (mSkipManager != null) {
            mSkipManager.setOnSkipListener(listener);
        }
    }

    /**
     * 获取跳过管理器
     * @return 跳过管理器
     */
    public SkipManager getSkipManager() {
        return mSkipManager;
    }

    /**
     * 获取视频比例管理器
     * @return 视频比例管理器
     */
    public VideoScaleManager getVideoScaleManager() {
        return mVideoScaleManager;
    }
    
    /**
     * 获取播放状态管理器
     * @return 播放状态管理器
     */
    public PlaybackStateManager getPlaybackStateManager() {
        return mPlaybackStateManager;
    }
    
    /**
     * 获取组件状态管理器
     * @return 组件状态管理器
     */
    public ComponentStateManager getComponentStateManager() {
        return mComponentStateManager;
    }
    
    /**
     * 获取错误恢复管理器
     * @return 错误恢复管理器
     */
    public ErrorRecoveryManager getErrorRecoveryManager() {
        return mErrorRecoveryManager;
    }

    /**
     * 刷新视频显示类型
     * 用于在改变视频比例后刷新显示
     */
    public void refreshVideoShowType() {
        changeTextureViewShowType();
    }

    /**
     * 是否为直播视频
     * @return true 直播
     */
    public boolean isLiveVideo() {
        return mIsLiveVideo;
    }

    /**
     * 设置是否为直播视频
     * @param isLive true 直播
     */
    public void setLiveVideo(boolean isLive) {
        this.mIsLiveVideo = isLive;
    }

    /**
     * 是否正在嗅探
     * @return true 正在嗅探
     */
    public boolean isSniffing() {
        return mIsSniffing;
    }

    /**
     * 开始视频嗅探
     * Requirements: 6.1 - THE OrangevideoView SHALL 支持视频嗅探功能 (startSniffing)
     */
    public void startSniffing() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            debug("startSniffing: url is empty");
            return;
        }
        startSniffing(mVideoUrl, null);
    }

    /**
     * 开始视频嗅探（带自定义请求头）
     * @param url 网页地址
     * @param headers 自定义请求头
     */
    public void startSniffing(String url, java.util.Map<String, String> headers) {
        mIsSniffing = true;
        setOrangePlayState(STATE_STARTSNIFFING);
        
        Context context = getContext();
        VideoSniffing.startSniffing(context, url, headers, new VideoSniffing.Call() {
            @Override
            public void received(String contentType, java.util.HashMap<String, String> respHeaders, 
                               String title, String videoUrl) {
                debug("Sniffing received: " + videoUrl);
                // 通知监听器
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
                debug("Sniffing finished: " + videoSize + " videos found");
                // 通知监听器
                for (OnStateChangeListener listener : mStateChangeListeners) {
                    if (listener instanceof OnSniffingListener) {
                        ((OnSniffingListener) listener).onSniffingFinish(videoList, videoSize);
                    }
                }
            }
        });
    }

    /**
     * 结束视频嗅探
     */
    public void stopSniffing() {
        mIsSniffing = false;
        VideoSniffing.stop(true);
        setOrangePlayState(STATE_ENDSNIFFING);
    }

    /**
     * 嗅探监听器接口
     */
    public interface OnSniffingListener {
        /**
         * 接收到视频资源
         */
        void onSniffingReceived(String contentType, java.util.HashMap<String, String> headers, 
                               String title, String url);
        
        /**
         * 嗅探完成
         */
        void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize);
    }

    // ===== 调试模式 =====

    /**
     * 设置调试模式
     * @param debug true 开启调试
     */
    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    /**
     * 是否调试模式
     * @return true 调试模式
     */
    public boolean isDebug() {
        return mDebug;
    }

    /**
     * 调试日志
     * @param message 日志信息
     */
    protected void debug(Object message) {
        if (mDebug) {
            android.util.Log.d(TAG, String.valueOf(message));
        }
    }

    // ===== 工具方法 =====

    /**
     * 获取 Activity
     * @return Activity
     */
    public Activity getActivity() {
        Context context = getContext();
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    /**
     * 是否正在播放
     * @return true 正在播放
     */
    public boolean isPlaying() {
        return mCurrentPlayState == PlayerConstants.STATE_PLAYING;
    }

    /**
     * 是否处于普通状态（非全屏、非小窗）
     * @return true 普通状态
     */
    public boolean isInNormalState() {
        return !isFullScreen() && !isTinyScreen();
    }

    // ===== 手势提示 GestureView =====
    private com.orange.playerlibrary.component.GestureView mGestureView;

    /**
     * 重写显示亮度对话框，使用 GestureView 替代 Dialog
     */
    @Override
    protected void showBrightnessDialog(float percent) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onBrightnessChange((int) (percent * 100));
        }
    }

    /**
     * 重写显示音量对话框，使用 GestureView 替代 Dialog
     */
    @Override
    protected void showVolumeDialog(float deltaY, int volumePercent) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onVolumeChange(volumePercent);
        }
    }

    /**
     * 重写显示进度对话框，使用 GestureView 替代 Dialog
     */
    @Override
    protected void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {
        ensureGestureView();
        if (mGestureView != null) {
            mGestureView.onStartSlide();
            mGestureView.onPositionChange((int) seekTimePosition, (int) getCurrentPosition(), (int) getDuration());
        }
    }

    /**
     * 重写隐藏亮度对话框
     */
    @Override
    protected void dismissBrightnessDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 重写隐藏音量对话框
     */
    @Override
    protected void dismissVolumeDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 重写隐藏进度对话框
     */
    @Override
    protected void dismissProgressDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 确保 GestureView 已初始化
     */
    private void ensureGestureView() {
        if (mGestureView == null) {
            mGestureView = new com.orange.playerlibrary.component.GestureView(getContext());
            // 设置布局参数，填充整个播放器
            android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
            addView(mGestureView, lp);
        }
    }

    /**
     * 获取 GestureView
     */
    public com.orange.playerlibrary.component.GestureView getGestureView() {
        ensureGestureView();
        return mGestureView;
    }

    /**
     * 设置播放状态（兼容原 API）
     * @param state 状态
     */
    public void setThisPlayState(int state) {
        setOrangePlayState(state);
    }

    /**
     * 设置播放器状态（兼容原 API）
     * @param state 状态
     */
    public void setThisPlayerState(int state) {
        setOrangePlayerState(state);
    }

    // ===== GSYBaseVideoPlayer 抽象方法实现 =====

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

    /**
     * 重写返回按钮获取方法，返回 null 让 GSY 不操作返回按钮
     * 返回按钮由 TitleView 组件控制
     */
    @Override
    public android.widget.ImageView getBackButton() {
        return null;
    }

    /**
     * 获取全屏播放器对象（橘子播放器版本）
     * @return OrangevideoView 如果没有则返回空
     */
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

    /**
     * 重写检查状态方法，避免调用父类的 getFullWindowPlayer 导致 ClassCastException
     * 因为 OrangevideoView 继承自 GSYBaseVideoPlayer 而不是 GSYVideoPlayer
     */
    @Override
    protected void checkoutState() {
        removeCallbacks(mOrangeCheckoutTask);
        mInnerHandler.postDelayed(mOrangeCheckoutTask, 500);
    }

    /**
     * 橘子播放器的状态检查任务
     */
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

    /**
     * 重写进入全屏方法，确保全屏播放器正确初始化橘子组件状态
     */
    @Override
    @SuppressWarnings({"ResourceType", "unchecked"})
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        debugLog("=== 进入全屏方法 ===");
        
        // 强制隐藏状态栏和导航栏（忽略传入的参数）
        hideStatusBarAndNavigation(context);
        
        // 如果启用了自动旋转，则旋转屏幕到横屏
        if (mAutoRotateOnFullscreen) {
            Activity activity = getActivity();
            if (activity != null) {
                debugLog("旋转屏幕到横屏");
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        
        // 调用父类方法创建全屏播放器（传入 true 让父类也处理）
        GSYBaseVideoPlayer fullPlayer = super.startWindowFullscreen(context, true, true);
        debugLog("父类全屏方法返回: " + (fullPlayer != null ? fullPlayer.getClass().getSimpleName() : "null"));
        
        // 如果是 OrangevideoView，同步状态
        if (fullPlayer instanceof OrangevideoView) {
            final OrangevideoView orangeFullPlayer = (OrangevideoView) fullPlayer;
            debugLog("全屏播放器是 OrangevideoView");
            
            // 立即设置全屏标志
            orangeFullPlayer.mIfCurrentIsFullscreen = true;
            
            // 延迟同步状态，确保布局完成后再显示控制器
            orangeFullPlayer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    debugLog("=== postDelayed 回调执行 ===");
                    
                    // 同步标题
                    if (mTitleView != null && orangeFullPlayer.mTitleView != null) {
                        String title = mTitleView.getTitle();
                        orangeFullPlayer.mTitleView.setTitle(title);
                        debugLog("同步标题: " + title);
                        
                        // 绑定全屏 TitleView 的事件
                        if (mOrangeController != null) {
                            orangeFullPlayer.mTitleView.setController(mOrangeController);
                            debugLog("全屏 TitleView 事件绑定完成");
                        }
                    }
                    
                    // 同步当前播放状态到全屏播放器的橘子组件
                    orangeFullPlayer.setOrangePlayState(mCurrentPlayState);
                    orangeFullPlayer.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
                    debugLog("同步播放状态: playState=" + mCurrentPlayState);
                    
                    // 重新注册全屏播放器的进度监听器，确保进度更新正常工作
                    if (orangeFullPlayer.mComponentStateManager != null) {
                        orangeFullPlayer.mComponentStateManager.reregisterProgressListener(orangeFullPlayer);
                        debugLog("全屏播放器进度监听器已重新注册");
                    }
                    
                    // 全屏时显示控制器（包括标题栏和弹幕区）
                    orangeFullPlayer.showController();
                    // 强制显示 TitleView
                    debugLog("TitleView=" + orangeFullPlayer.mTitleView + " VodControlView=" + orangeFullPlayer.mVodControlView);
                    if (orangeFullPlayer.mTitleView != null) {
                        orangeFullPlayer.mTitleView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mTitleView.bringToFront();
                        debugLog("强制显示 TitleView, visibility=" + orangeFullPlayer.mTitleView.getVisibility());
                    }
                    // 强制通知 VodControlView 进入全屏状态（显示弹幕区）
                    if (orangeFullPlayer.mVodControlView != null) {
                        orangeFullPlayer.mVodControlView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mVodControlView.bringToFront();
                        orangeFullPlayer.mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                        debugLog("通知 VodControlView 全屏状态, visibility=" + orangeFullPlayer.mVodControlView.getVisibility());
                    }
                    orangeFullPlayer.requestLayout();
                    
                    debugLog("全屏播放器初始化完成");
                }
            }, 300);
        } else {
            debugLog("全屏播放器不是 OrangevideoView: " + (fullPlayer != null ? fullPlayer.getClass().getName() : "null"));
        }
        
        // 通知当前播放器进入全屏状态
        setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        return fullPlayer;
    }
    
    /**
     * 隐藏状态栏和导航栏
     */
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
            
            // 隐藏 ActionBar
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

    /**
     * 重写退出全屏方法，避免 ClassCastException
     */
    @Override
    @SuppressWarnings("ResourceType")
    protected void clearFullscreenLayout() {
        android.util.Log.d(TAG, "clearFullscreenLayout: 开始退出全屏, mFullAnimEnd=" + mFullAnimEnd);
        
        if (!mFullAnimEnd) {
            android.util.Log.d(TAG, "clearFullscreenLayout: 动画未结束，跳过");
            return;
        }
        
        // 保存全屏播放器的状态
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        if (oldF != null && oldF instanceof OrangevideoView) {
            OrangevideoView orangeVideoPlayer = (OrangevideoView) oldF;
            android.util.Log.d(TAG, "clearFullscreenLayout: 保存全屏播放器状态");
            
            // 使用 PlaybackStateManager 保存状态
            if (mPlaybackStateManager != null) {
                mPlaybackStateManager.saveState(orangeVideoPlayer);
                android.util.Log.d(TAG, "clearFullscreenLayout: 状态已保存 position=" + orangeVideoPlayer.getCurrentPositionWhenPlaying());
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

        android.util.Log.d(TAG, "clearFullscreenLayout: 延迟 " + delay + "ms 后恢复正常");
        mInnerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                orangeBackToNormal();
            }
        }, delay);
    }

    /**
     * 橘子播放器回到正常效果
     */
    @SuppressWarnings("ResourceType")
    protected void orangeBackToNormal() {
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        final OrangevideoView orangeVideoPlayer;
        
        if (oldF != null && oldF instanceof OrangevideoView) {
            orangeVideoPlayer = (OrangevideoView) oldF;
            // 如果暂停了 - 跳过 pauseFullBackCoverLogic，因为它需要 GSYVideoPlayer 类型
            // 添加空检查，避免 NPE
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

    /**
     * 橘子播放器恢复正常显示
     */
    protected void orangeResolveNormalVideoShow(android.view.View oldF, android.view.ViewGroup vp, OrangevideoView orangeVideoPlayer) {
        android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 开始恢复正常显示");
        
        // 保存全屏播放器的播放位置（关键：在 cloneParams 之前保存）
        final long savedPosition = (orangeVideoPlayer != null) ? orangeVideoPlayer.getCurrentPositionWhenPlaying() : 0;
        final boolean wasPlaying = (orangeVideoPlayer != null) ? orangeVideoPlayer.isPlaying() : false;
        android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 保存全屏状态 position=" + savedPosition + ", wasPlaying=" + wasPlaying);
        
        if (oldF != null && oldF.getParent() != null) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
            android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 移除全屏视图");
        }
        
        mCurrentState = getGSYVideoManager().getLastState();
        android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 恢复状态 mCurrentState=" + mCurrentState);
        
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
        
        // 关键：重新添加 TextureView，确保 Surface 正确恢复
        android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 重新添加 TextureView");
        addTextureView();
        
        // 延迟恢复播放位置，确保 Surface 已经准备好
        postDelayed(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 延迟恢复播放位置");
                
                // 恢复播放位置（关键修复）
                if (savedPosition > 0) {
                    android.util.Log.d(TAG, "orangeResolveNormalVideoShow: seekTo " + savedPosition);
                    seekTo(savedPosition);
                    
                    // 如果之前在播放，继续播放
                    if (wasPlaying) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 恢复播放");
                                if (mCurrentState == CURRENT_STATE_PAUSE) {
                                    onVideoResume();
                                }
                            }
                        }, 200);
                    }
                }
                
                // 恢复组件状态
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                    android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 组件状态已恢复");
                }
                
                // 通知组件状态更新
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
        // 通知橘子组件状态变化
        setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
        
        android.util.Log.d(TAG, "orangeResolveNormalVideoShow: 完成");
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
        // 从全屏返回
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
        // 橘子组件处理移动网络提示
        if (mPrepareView != null) {
            setOrangePlayState(8); // 移动网络警告状态
        }
    }

    // UI 状态变化方法 - 空实现，由橘子组件处理
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
    
    // 自动隐藏控制器的定时器
    private static final int AUTO_HIDE_DELAY = 4000; // 4秒后自动隐藏
    private Runnable mAutoHideRunnable;

    @Override
    protected void onClickUiToggle(android.view.MotionEvent e) {
        // 只在播放或暂停状态时才切换控制器显示/隐藏
        if (mCurrentPlayState != PlayerConstants.STATE_PLAYING && 
            mCurrentPlayState != PlayerConstants.STATE_PAUSED &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERING &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERED) {
            return;
        }
        
        // 点击切换控制器显示/隐藏
        if (isControllerShowing()) {
            hideController();
        } else {
            showController();
        }
    }
    
    /**
     * 显示控制器
     */
    public void showController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.VISIBLE);
        }
        // 全屏时显示标题栏
        if (mTitleView != null && (mIfCurrentIsFullscreen || mCurrentPlayerState == PlayerConstants.PLAYER_FULL_SCREEN)) {
            mTitleView.setVisibility(android.view.View.VISIBLE);
        }
        // 启动自动隐藏定时器
        startAutoHideTimer();
    }
    
    /**
     * 隐藏控制器
     */
    public void hideController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.GONE);
        }
        if (mTitleView != null) {
            mTitleView.setVisibility(android.view.View.GONE);
        }
        // 取消自动隐藏定时器
        cancelAutoHideTimer();
    }
    
    /**
     * 控制器是否显示
     */
    public boolean isControllerShowing() {
        return mVodControlView != null && mVodControlView.getVisibility() == android.view.View.VISIBLE;
    }
    
    /**
     * 获取或创建自动隐藏 Runnable
     */
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
    
    /**
     * 启动自动隐藏定时器
     */
    private void startAutoHideTimer() {
        cancelAutoHideTimer();
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING && mInnerHandler != null) {
            mInnerHandler.postDelayed(getAutoHideRunnable(), AUTO_HIDE_DELAY);
        }
    }
    
    /**
     * 取消自动隐藏定时器
     */
    private void cancelAutoHideTimer() {
        if (mInnerHandler != null && mAutoHideRunnable != null) {
            mInnerHandler.removeCallbacks(mAutoHideRunnable);
        }
    }

    /**
     * 重写双击方法，完全使用橘子播放器的状态判断，不依赖 GSY 的 mCurrentState
     */
    protected void touchDoubleUp() {
        android.util.Log.d(TAG, "touchDoubleUp() 被调用, mCurrentState=" + mCurrentState + ", mCurrentPlayState=" + mCurrentPlayState);
        // 双击暂停/播放 - 完全使用橘子播放器的状态判断
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
            mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
            mCurrentPlayState == PlayerConstants.STATE_BUFFERED) {
            android.util.Log.d(TAG, "双击暂停");
            // 直接调用我们的 pause() 方法
            pause();
        } else if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
            android.util.Log.d(TAG, "双击恢复播放");
            // 直接调用我们的 resume() 方法
            resume();
        } else {
            android.util.Log.d(TAG, "双击无效，当前状态: " + mCurrentPlayState);
        }
    }

    @Override
    public void startPlayLogic() {
        prepareVideo();
    }

    /**
     * 重写 startAfterPrepared，确保 TextureView 正确添加
     */
    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
    }

    // ===== 配置改变处理 (Requirements: 2.3, 2.4, 5.1, 5.2) =====
    
    /**
     * 处理配置改变（如屏幕旋转）
     * 保存当前播放状态，避免黑屏和状态丢失
     * Requirements: 2.1, 2.2, 5.3, 5.4, 5.5
     * 
     * @param newConfig 新的配置
     */
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        android.util.Log.d(TAG, "onConfigurationChanged: orientation=" + newConfig.orientation 
                + ", playState=" + mCurrentPlayState 
                + ", playerState=" + mCurrentPlayerState);
        
        // 使用 PlaybackStateManager 保存当前状态
        if (mPlaybackStateManager != null) {
            mPlaybackStateManager.saveState(this);
        }
        
        // 使用 ComponentStateManager 保存组件状态
        if (mComponentStateManager != null) {
            mComponentStateManager.saveComponentState(
                (int) getDuration(), 
                (int) getCurrentPositionWhenPlaying()
            );
        }
        
        // 处理全屏/竖屏切换
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏 - 如果不是全屏状态，可能需要进入全屏
            android.util.Log.d(TAG, "横屏方向");
        } else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏 - 如果是全屏状态，可能需要退出全屏
            android.util.Log.d(TAG, "竖屏方向");
        }
        
        // 延迟恢复状态，确保布局完成
        postDelayed(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d(TAG, "开始恢复状态");
                
                // 使用 PlaybackStateManager 恢复状态
                if (mPlaybackStateManager != null) {
                    mPlaybackStateManager.restoreState(OrangevideoView.this);
                }
                
                // 使用 ComponentStateManager 恢复组件状态
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    // 重新注册进度监听器，确保组件重建后进度更新正常
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                    android.util.Log.d(TAG, "组件状态已恢复，进度监听器已重新注册");
                }
                
                // 重新应用视频比例
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                }
                
                // 通知组件状态更新
                notifyComponentsPlayStateChanged(mCurrentPlayState);
                notifyComponentsPlayerStateChanged(mCurrentPlayerState);
                
                android.util.Log.d(TAG, "配置改变处理完成");
            }
        }, 100);
    }

    /**
     * 重写 getLayoutParams 方法，确保全屏播放器使用正确的布局参数
     * 这是解决全屏黑屏问题的关键
     */
    @Override
    public android.view.ViewGroup.LayoutParams getLayoutParams() {
        android.view.ViewGroup.LayoutParams params = super.getLayoutParams();
        if (params == null) {
            // 如果没有布局参数，创建一个 MATCH_PARENT 的参数
            params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
        }
        return params;
    }
}
