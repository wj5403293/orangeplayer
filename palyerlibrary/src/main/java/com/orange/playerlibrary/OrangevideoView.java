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
 * 锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷??
 * 锟教筹拷 GSYBaseVideoPlayer锟斤拷锟斤拷全使锟斤拷锟斤拷锟斤拷锟斤拷??UI
 */
public class OrangevideoView extends GSYBaseVideoPlayer {

    private static final String TAG = "OrangevideoView";
    
    // ===== 状态锟斤拷??=====
    public static final int STATE_STARTSNIFFING = PlayerConstants.STATE_STARTSNIFFING;
    public static final int STATE_ENDSNIFFING = PlayerConstants.STATE_ENDSNIFFING;
    
    // ===== 全锟斤拷 SQLite 锟芥储 =====
    public static OrangeSharedSqlite sqlite;
    
    // ===== 锟斤拷员锟斤拷锟斤拷 =====
    private String mVideoUrl;                     // 锟斤拷前锟斤拷频锟斤拷址
    private Map<String, String> mVideoHeaders;    // 锟斤拷锟斤拷??
    private static float sSpeed = 1.0f;           // 锟斤拷前锟斤拷??
    private static float sLongSpeed = 3.0f;       // 锟斤拷锟斤拷锟斤拷??
    private boolean mKeepVideoPlaying = false;    // 锟角凤拷锟斤拷洳ワ拷锟轿伙拷锟?
    private boolean mAutoThumbnailEnabled = true; // 锟角凤拷锟斤拷锟斤拷锟皆讹拷锟斤拷锟斤拷??
    private Object mDefaultThumbnail = null;      // 默锟斤拷锟斤拷锟斤拷??
    private boolean mIsLiveVideo = false;         // 锟角凤拷直锟斤拷
    private boolean mIsSniffing = false;          // 锟角凤拷锟斤拷锟斤拷锟斤拷探
    private boolean mAutoRotateOnFullscreen = true; // 全锟斤拷时锟角凤拷锟皆讹拷锟斤拷转锟斤拷??
    
    // ===== 锟斤拷锟斤拷片头片尾锟斤拷锟斤拷??=====
    private SkipManager mSkipManager;
    
    // ===== 锟斤拷频锟斤拷锟斤拷锟斤拷锟斤拷??=====
    private VideoScaleManager mVideoScaleManager;
    
    // ===== 锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷 =====
    private PlaybackStateManager mPlaybackStateManager;
    
    // ===== 锟斤拷锟阶刺拷锟斤拷锟斤拷锟?=====
    private ComponentStateManager mComponentStateManager;
    
    // ===== 锟斤拷锟斤拷指锟斤拷锟斤拷锟??=====
    private ErrorRecoveryManager mErrorRecoveryManager;
    
    // ===== 锟皆讹拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷 =====
    private CustomFullscreenHelper mFullscreenHelper;
    
    // ===== ControlWrapper =====
    private com.orange.playerlibrary.interfaces.ControlWrapper mControlWrapper;
    
    // ===== 锟斤拷锟斤拷??=====
    private OrangeVideoController mOrangeController;
    
    // ===== UI 锟斤拷锟?=====
    private com.orange.playerlibrary.component.PrepareView mPrepareView;
    private com.orange.playerlibrary.component.TitleView mTitleView;
    private com.orange.playerlibrary.component.VodControlView mVodControlView;
    private com.orange.playerlibrary.component.LiveControlView mLiveControlView;
    private com.orange.playerlibrary.component.CompleteView mCompleteView;
    private com.orange.playerlibrary.component.ErrorView mErrorView;
    private boolean mUseOrangeComponents = true; // 默锟斤拷使锟斤拷锟斤拷锟斤拷锟斤拷锟?
    
    // ===== 锟斤拷锟斤拷??=====
    private final List<OnStateChangeListener> mStateChangeListeners = new ArrayList<>();
    private OnProgressListener mProgressListener;
    private OnPlayCompleteListener mPlayCompleteListener;
    
    // ===== 锟斤拷前状??=====
    private int mCurrentPlayState = PlayerConstants.STATE_IDLE;
    private int mCurrentPlayerState = PlayerConstants.PLAYER_NORMAL;
    
    // ===== 锟斤拷锟斤拷模式 =====
    private boolean mDebug = false;
    
    // ===== 画中画模式标志 =====
    private boolean mEnteringPiPMode = false;  // 是否正在进入画中画模式

    /**
     * 锟斤拷锟届函??
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
     * 锟斤拷始??- 锟斤拷写锟斤拷锟洁方锟斤拷
     */
    @Override
    protected void init(Context context) {
        super.init(context);
        initOrangePlayer();
    }

    /**
     * 锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷始锟斤拷
     */
    private void initOrangePlayer() {
        // 强锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷始锟斤拷顺锟斤拷锟斤拷锟斤拷??
        mUseOrangeComponents = true;
        
        // 锟斤拷始锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷
        mSkipManager = new SkipManager();
        mSkipManager.attachVideoView(this);
        
        // 锟斤拷始锟斤拷锟斤拷频锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷
        mVideoScaleManager = new VideoScaleManager(this, PlayerSettingsManager.getInstance(getContext()));
                
        // 锟斤拷始锟斤拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷
        mPlaybackStateManager = new PlaybackStateManager();
                
        // 锟斤拷始锟斤拷锟斤拷锟阶刺拷锟斤拷锟斤拷锟?
        mComponentStateManager = new ComponentStateManager();
                
        // 锟斤拷始锟斤拷锟斤拷锟斤拷指锟斤拷锟斤拷锟斤拷锟?
        mErrorRecoveryManager = new ErrorRecoveryManager();
        mErrorRecoveryManager.attachVideoView(this);
                
        // 锟斤拷始锟斤拷锟皆讹拷锟斤拷全锟斤拷锟斤拷锟斤拷??
        mFullscreenHelper = new CustomFullscreenHelper(this);
                
        // 锟斤拷锟斤拷锟斤拷转锟斤拷锟斤拷锟斤拷直锟斤拷锟斤拷??
        setShowFullAnimation(false);
        setRotateViewAuto(false);
        setNeedLockFull(false);
        setLockLand(false);
        setRotateWithSystem(false);
        // 锟斤拷锟斤拷全锟斤拷锟叫伙拷锟斤拷锟斤拷
        setNeedShowWifiTip(false);
        // 锟斤拷锟斤拷 OrientationUtils锟斤拷锟斤拷锟斤拷转锟斤拷幕
        setNeedOrientationUtils(false);
        
        // 锟斤拷锟矫达拷锟斤拷锟斤拷锟狡ｏ拷双锟斤拷锟斤拷??锟斤拷锟脚★拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??锟斤拷锟斤拷/锟斤拷锟斤拷??
        setIsTouchWiget(true);
        setIsTouchWigetFull(true);
        
        // 默锟较筹拷始锟斤拷锟斤拷锟斤拷锟斤拷??
        if (mUseOrangeComponents) {
            initOrangeComponents();
        }
        
        // 使锟斤拷 ComponentStateManager 注锟斤拷锟斤拷燃锟斤拷锟??
        // 锟斤拷锟斤拷锟斤拷锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟截革拷注锟结，锟斤拷锟斤拷锟斤拷锟斤拷亟锟斤拷锟斤拷远锟斤拷锟斤拷锟阶拷锟?
        if (mComponentStateManager != null) {
            mComponentStateManager.reregisterProgressListener(this);
                    }
        
        // 锟斤拷锟矫回碉拷锟斤拷锟斤拷
        setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PREPARED);
                // 锟斤拷锟斤拷欠锟轿憋拷锟?
                if (getDuration() <= 0) {
                    mIsLiveVideo = true;
                }
                // 应锟矫憋拷锟斤拷锟斤拷锟狡碉拷锟斤拷锟斤拷锟??
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                                    }
                
                // 锟斤拷锟斤拷欠锟斤拷锟揭拷指锟饺拷锟斤拷谢锟绞憋拷锟斤拷锟侥诧拷锟斤拷位??
                if (mFullscreenHelper != null && mFullscreenHelper.getPendingSeekPosition() > 0) {
                    final long pendingPosition = mFullscreenHelper.getPendingSeekPosition();
                    final boolean pendingResume = mFullscreenHelper.isPendingResume();
                                        
                    // 锟斤拷锟斤拷锟斤拷指锟阶??
                    mFullscreenHelper.clearPendingSeekPosition();
                    
                    // 锟接筹拷执锟斤拷 seekTo锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷全准锟斤拷??
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                                                        seekTo(pendingPosition);
                            
                            // 锟斤拷锟街帮拷诓锟斤拷牛锟饺凤拷锟斤拷锟斤拷锟斤拷锟斤拷锟?
                            if (pendingResume && !isPlaying()) {
                                                                resume();
                            }
                        }
                    }, 100);
                } else {
                    // 锟街革拷锟斤拷锟脚斤拷锟饺ｏ拷锟斤拷全锟斤拷锟叫伙拷锟斤拷锟斤拷锟斤拷锟?
                    if (mKeepVideoPlaying) {
                        restorePlaybackProgress();
                    }
                }
                // 执锟斤拷锟斤拷锟斤拷片头
                if (mSkipManager != null) {
                    mSkipManager.performSkipIntro();
                }
                // 准锟斤拷锟斤拷珊锟斤拷远锟斤拷锟斤拷氩ワ拷锟阶??
                setOrangePlayState(PlayerConstants.STATE_PLAYING);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PLAYBACK_COMPLETED);
                // 锟斤拷锟斤拷锟斤拷桑锟斤拷锟斤拷锟斤拷锟斤拷慕锟斤拷锟?
                if (mKeepVideoPlaying) {
                    clearSavedProgress();
                }
                // 锟斤拷锟斤拷锟斤拷锟斤拷状??
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
                // 锟斤拷锟斤拷转锟斤拷幕锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
                // 锟斤拷锟斤拷转锟斤拷幕锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷
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
     * 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟?UI锟斤拷锟斤拷??GSY 锟斤拷锟斤拷 UI??
     * 锟斤拷锟矫此凤拷锟斤拷锟斤拷锟斤拷锟??PrepareView锟斤拷TitleView锟斤拷VodControlView锟斤拷CompleteView锟斤拷ErrorView 锟斤拷锟斤拷??
     */
    // ===== 锟斤拷锟斤拷锟斤拷志锟截碉拷 =====
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
     * 锟斤拷始锟斤拷锟斤拷锟斤拷锟斤拷??
     */
    private void initOrangeComponents() {
        Context context = getContext();
        android.widget.RelativeLayout.LayoutParams matchParentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);

        
        // 锟斤拷锟斤拷 ControlWrapper 锟斤拷锟斤拷锟芥到锟斤拷员锟斤拷锟斤拷
        mControlWrapper = createControlWrapper();
        
        // 1. PrepareView - 准锟斤拷/锟斤拷锟斤拷锟斤拷图
        mPrepareView = new com.orange.playerlibrary.component.PrepareView(context);
        mPrepareView.attach(mControlWrapper);
        mPrepareView.setClickStart(); // 锟斤拷锟矫碉拷锟斤拷锟绞硷拷锟??
        addView(mPrepareView, matchParentParams);
        
        // 2. CompleteView - 锟斤拷锟斤拷锟斤拷锟斤拷锟酵?
        mCompleteView = new com.orange.playerlibrary.component.CompleteView(context);
        mCompleteView.attach(mControlWrapper);
        addView(mCompleteView, matchParentParams);

        // 3. ErrorView - 锟斤拷锟斤拷锟斤拷图
        mErrorView = new com.orange.playerlibrary.component.ErrorView(context);
        mErrorView.attach(mControlWrapper);
        addView(mErrorView, matchParentParams);

        // 4. TitleView - 锟斤拷锟斤拷锟斤拷锟斤拷全锟斤拷时锟斤拷示锟斤拷
        mTitleView = new com.orange.playerlibrary.component.TitleView(context);
        mTitleView.attach(mControlWrapper);
        addView(mTitleView, matchParentParams);

        // 5. VodControlView - 鐐规挱鎺у埗
        mVodControlView = new com.orange.playerlibrary.component.VodControlView(context);
        // 璁剧疆鎺у埗鍣ㄥ紩鐢紝纭繚浜嬩欢鑳藉琚粦瀹氾紙鍦?attach 涔嬪墠璁剧疆锛?
        if (mOrangeController != null) {
            mVodControlView.setOrangeVideoController(mOrangeController);
        }
        mVodControlView.attach(mControlWrapper);
        addView(mVodControlView, matchParentParams);
        
        // 鍒濆鐘舵€佽缃负 IDLE锛屾樉绀哄噯澶囪鍥?
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        
        // 纭繚浜嬩欢缁戝畾
        ensureEventBinding();
            }

    /**
     * 娣诲姞璋冭瘯鏃ュ織
     */
    public void debugLog(String msg) {
        if (mDebugLogCallback != null) {
            mDebugLogCallback.onLog(msg);
        }
            }

    /**
     * 纭繚鎵€鏈夋帶鍒剁粍浠剁殑浜嬩欢閮藉凡缁戝畾
     * 澶勭悊鍒濆鍖栭『搴忛棶棰?
     * Requirements: 1.2, 3.1, 3.2
     */
    private void ensureEventBinding() {
        if (mOrangeController == null) {
            android.util.Log.w(TAG, "ensureEventBinding: mOrangeController is null");
            return;
        }
        
        VideoEventManager eventManager = mOrangeController.getVideoEventManager();
        if (eventManager == null) {
            android.util.Log.w(TAG, "ensureEventBinding: VideoEventManager is null");
            return;
        }
        
        // 缁戝畾 VodControlView 浜嬩欢
        if (mVodControlView != null) {
            android.util.Log.d(TAG, "ensureEventBinding: binding VodControlView events");
            eventManager.bindControllerComponents(mVodControlView);
        }
        
        // 缁戝畾 TitleView 浜嬩欢
        if (mTitleView != null) {
            android.util.Log.d(TAG, "ensureEventBinding: binding TitleView events");
            eventManager.bindTitleView(mTitleView);
        }
        
        android.util.Log.d(TAG, "ensureEventBinding: completed");
    }

    /**
     * 锟斤拷锟斤拷 ControlWrapper 实锟斤拷
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
                // 锟斤拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷 pause() 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷直锟接碉拷??onVideoPause()
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
                // 使锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷??GSY 锟斤拷状??
                return videoView.isPlaying();
            }

            @Override
            public void togglePlay() {
                if (isPlaying()) {
                    pause();
                } else {
                    // 使锟斤拷 resume() 锟斤拷锟斤拷锟斤拷锟斤拷??onVideoResume()锟斤拷确锟斤拷状态锟斤拷确锟斤拷??
                    videoView.resume();
                }
            }

            @Override
            public void toggleFullScreen() {
                                if (isFullScreen()) {
                    // 锟剿筹拷全??- 使锟斤拷锟皆讹拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷
                                        Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.exitFullscreen(activity);
                    }
                } else {
                    // 锟斤拷锟斤拷全锟斤拷 - 使锟斤拷锟皆讹拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷
                                        Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.enterFullscreen(activity);
                    }
                }
            }

            @Override
            public void toggleLockState() {
                // GSY 锟斤拷支锟斤拷锟斤拷??
            }

            @Override
            public boolean isFullScreen() {
                // 使锟斤拷锟皆讹拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟叫讹拷
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
                // 锟斤拷锟斤拷锟斤拷锟斤拷锟捷诧拷实锟斤拷
            }

            @Override
            public boolean isMute() {
                return false;
            }

            @Override
            public void setVolume(float volume) {
                // GSY 锟斤拷直锟斤拷支??
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
                // 锟斤拷锟截匡拷锟斤拷??
            }

            @Override
            public void show() {
                // 锟斤拷示锟斤拷锟斤拷??
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
     * 锟角凤拷使锟斤拷锟斤拷锟斤拷锟斤拷锟?
     */
    public boolean isUseOrangeComponents() {
        return mUseOrangeComponents;
    }

    /**
     * 锟斤拷取 PrepareView
     */
    public com.orange.playerlibrary.component.PrepareView getPrepareView() {
        return mPrepareView;
    }

    /**
     * 锟斤拷取 TitleView
     */
    public com.orange.playerlibrary.component.TitleView getTitleView() {
        return mTitleView;
    }

    /**
     * 锟斤拷取 VodControlView
     */
    public com.orange.playerlibrary.component.VodControlView getVodControlView() {
        return mVodControlView;
    }

    /**
     * 锟斤拷取 LiveControlView
     */
    public com.orange.playerlibrary.component.LiveControlView getLiveControlView() {
        return mLiveControlView;
    }

    /**
     * 锟斤拷取 CompleteView
     */
    public com.orange.playerlibrary.component.CompleteView getCompleteView() {
        return mCompleteView;
    }

    /**
     * 锟斤拷取 ErrorView
     */
    public com.orange.playerlibrary.component.ErrorView getErrorView() {
        return mErrorView;
    }

    /**
     * 锟斤拷取 ControlWrapper
     * 锟斤拷锟斤拷锟解部锟斤拷锟斤拷全锟斤拷锟叫伙拷锟饺癸拷??
     */
    public com.orange.playerlibrary.interfaces.ControlWrapper getControlWrapper() {
        return mControlWrapper;
    }

    // ===== 锟斤拷频锟斤拷址锟斤拷锟矫凤拷锟斤拷 (Requirements: 1.2) =====
    
    /**
     * 锟斤拷锟斤拷锟斤拷频锟斤拷址
     * @param url 锟斤拷频锟斤拷址
     */
    public void setUrl(String url) {
        setUrl(url, null);
    }

    /**
     * 锟斤拷锟斤拷锟斤拷频锟斤拷址锟斤拷锟斤拷锟斤拷头
     * @param url 锟斤拷频锟斤拷址
     * @param headers 锟斤拷锟斤拷??
     */
    public void setUrl(String url, Map<String, String> headers) {
        this.mVideoUrl = url;
        this.mVideoHeaders = headers;
        // 使锟斤拷 GSYVideoPlayer ??setUp 锟斤拷锟斤拷
        if (headers != null) {
            setUp(url, true, null, headers, "");
        } else {
            setUp(url, true, "");
        }
    }

    /**
     * 锟斤拷取锟斤拷前锟斤拷频锟斤拷址
     * @return 锟斤拷频锟斤拷址
     */
    public String getUrl() {
        return mVideoUrl;
    }

    // ===== 锟斤拷锟脚匡拷锟狡凤拷锟斤拷 (Requirements: 1.3, 1.4, 1.5, 1.6) =====

    /**
     * 锟斤拷始锟斤拷??
     */
    public void start() {
        mIsSniffing = false;
        mIsLiveVideo = false;
        // 锟斤拷锟斤拷锟斤拷锟斤拷状??
        if (mSkipManager != null) {
            mSkipManager.reset();
        }
        // 锟斤拷锟斤拷锟斤拷锟斤拷指锟斤拷锟??
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.startBlackScreenDetection();
            mErrorRecoveryManager.startStateConsistencyCheck();
        }
        setOrangePlayState(PlayerConstants.STATE_PREPARING);
        startPlayLogic();
    }

    /**
     * 锟斤拷停锟斤拷锟斤拷
     */
    public void pause() {
                // 锟斤拷锟芥播锟脚斤拷锟斤拷
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 停止锟斤拷锟狡??
        if (mSkipManager != null) {
            mSkipManager.stopOutroCheck();
        }
        // 锟斤拷锟斤拷 GSY 锟斤拷锟斤拷停锟斤拷锟斤拷锟斤拷锟结触??onVideoPause锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟阶刺拷锟?
        onVideoPause();
            }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷
     */
    public void resume() {
                // 锟斤拷锟斤拷 GSY 锟侥恢革拷锟斤拷锟脚凤拷锟斤拷锟斤拷锟结触??onVideoResume锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟阶刺拷锟?
        onVideoResume();
                // 锟斤拷始锟斤拷锟狡??
        if (mSkipManager != null) {
            mSkipManager.startOutroCheck();
        }
    }

    /**
     * 锟斤拷写 GSY ??onVideoPause 锟斤拷锟斤拷
     * ??GSY 锟节诧拷锟斤拷锟斤拷锟斤拷停时锟斤拷锟斤拷双锟斤拷锟斤拷锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷状??
     * 锟斤拷锟斤拷锟角帮拷丫锟斤拷锟斤拷锟酵Ｗ刺拷锟斤拷锟街革拷锟斤拷锟脚ｏ拷锟睫革拷双锟斤拷锟街革拷锟斤拷锟脚碉拷锟斤拷锟解）
     */
    @Override
    public void onVideoPause() {
        // 检查是否正在进入画中画模式
        if (mEnteringPiPMode) {
            // 正在进入画中画模式，不做暂停操作
            return;
        }
        
        // 检查是否处于画中画模式，如果是则不暂停
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                // 当前处于画中画模式，不做暂停操作
                return;
            }
        }
                
        // 锟斤拷锟斤拷锟角帮拷丫锟斤拷锟斤拷锟酵Ｗ刺拷锟剿碉拷锟斤拷锟斤拷堑诙锟斤拷锟剿拷锟斤拷锟接︼拷没指锟斤拷锟斤拷锟?
        if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
                        // 锟斤拷锟斤拷??onVideoResume()锟斤拷直锟接碉拷??super.onVideoResume() 锟斤拷锟斤拷锟斤拷状??
            super.onVideoResume();
                        // 锟斤拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷状??
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 直锟斤拷通知锟斤拷锟斤拷锟斤拷锟绞癸拷锟?post锟斤拷锟斤拷锟斤拷锟接迟碉拷锟斤拷状态锟斤拷??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
                        return;
        }
        
        // 只锟节诧拷锟斤拷状态时锟斤拷锟斤拷??
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERED);
        super.onVideoPause();
                if (shouldUpdateState) {
                        // 直锟接革拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??
            mCurrentPlayState = PlayerConstants.STATE_PAUSED;
            // 直锟斤拷通知锟斤拷锟斤拷锟斤拷锟绞癸拷锟?post锟斤拷锟斤拷锟斤拷锟接迟碉拷锟斤拷状态锟斤拷??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PAUSED);
            // 确锟斤拷 GSY ??mCurrentState 锟斤拷锟斤拷为锟斤拷停状??
            if (mCurrentState != CURRENT_STATE_PAUSE) {
                                mCurrentState = CURRENT_STATE_PAUSE;
            }
        } else {
                    }
    }

    /**
     * 锟斤拷写 GSY ??onVideoResume 锟斤拷锟斤拷
     * ??GSY 锟节诧拷锟斤拷锟矫恢革拷锟斤拷锟斤拷时锟斤拷锟斤拷双锟斤拷锟斤拷锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷状??
     */
    @Override
    public void onVideoResume() {
        // 检查是否处于画中画模式，如果是则不需要恢复（视频一直在播放）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                // 当前处于画中画模式，不做恢复操作
                return;
            }
        }
                // 只锟斤拷锟斤拷停状态时锟脚革拷锟斤拷为锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷锟截革拷锟斤拷锟斤拷
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PAUSED);
        super.onVideoResume();
                if (shouldUpdateState) {
                        // 直锟接革拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 直锟斤拷通知锟斤拷锟斤拷锟斤拷锟绞癸拷锟?post锟斤拷锟斤拷锟斤拷锟接迟碉拷锟斤拷状态锟斤拷??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            // 确锟斤拷 GSY ??mCurrentState 锟斤拷锟斤拷为锟斤拷锟斤拷状??
            if (mCurrentState != CURRENT_STATE_PLAYING) {
                                mCurrentState = CURRENT_STATE_PLAYING;
            }
        } else {
                    }
    }

    /**
     * 设置是否正在进入画中画模式
     * @param entering true 表示正在进入画中画模式
     */
    public void setEnteringPiPMode(boolean entering) {
        this.mEnteringPiPMode = entering;
    }
    
    /**
     * 获取是否正在进入画中画模式
     * @return true 表示正在进入画中画模式
     */
    public boolean isEnteringPiPMode() {
        return mEnteringPiPMode;
    }

    /**
     * 锟酵放诧拷锟斤拷锟斤拷锟斤拷??
     */
    @Override
    public void release() {
        // 锟斤拷锟芥播锟脚斤拷锟斤拷
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??
        if (mSkipManager != null) {
            mSkipManager.detachVideoView();
        }
        // 锟斤拷锟斤拷锟斤拷锟斤拷指锟斤拷锟斤拷锟??
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.detachVideoView();
        }
        super.release();
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        GSYVideoManager.releaseAllVideos();
    }

    // ===== 锟斤拷锟饺匡拷锟狡凤拷锟斤拷 (Requirements: 1.7, 1.8) =====

    /**
     * 锟斤拷取锟斤拷前锟斤拷锟斤拷位锟矫ｏ拷锟斤拷锟斤拷原 API??
     * @return 锟斤拷前位锟矫ｏ拷锟斤拷锟诫）
     */
    public long getCurrentPosition() {
        return getCurrentPositionWhenPlaying();
    }

    /**
     * 锟斤拷转锟斤拷指锟斤拷位??
     * @param position 目锟斤拷位锟矫ｏ拷锟斤拷锟诫）
     */
    public void seekTo(int position) {
        seekTo((long) position);
    }

    /**
     * 锟斤拷转锟斤拷指锟斤拷位??
     * @param position 目锟斤拷位锟矫ｏ拷锟斤拷锟诫）
     */
    public void seekTo(long position) {
                
        // 锟斤拷锟饺筹拷锟斤拷使锟斤拷 GSYVideoManager
        if (GSYVideoManager.instance().getPlayer() != null) {
                        GSYVideoManager.instance().getPlayer().seekTo(position);
        } else {
            // 锟斤拷锟?GSYVideoManager 锟斤拷锟斤拷锟矫ｏ拷锟斤拷锟斤拷使锟矫革拷锟洁方锟斤拷
                        setSeekOnStart(position);
        }
    }

    // ===== 锟斤拷锟劫匡拷锟狡凤拷??(Requirements: 1.9) =====

    /**
     * 锟斤拷锟矫诧拷锟脚憋拷??
     * @param speed 锟斤拷锟斤拷??(0.5 - 3.0)
     */
    @Override
    public void setSpeed(float speed) {
        // 锟斤拷锟狡憋拷锟劫凤拷??
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
        super.setSpeed(speed);
    }

    /**
     * 锟斤拷取锟斤拷前锟斤拷??
     * @return 锟斤拷前锟斤拷??
     */
    public static float getSpeeds() {
        return sSpeed;
    }

    /**
     * 锟斤拷锟矫憋拷锟劫ｏ拷锟斤拷态锟斤拷锟斤拷锟斤拷
     * @param speed 锟斤拷锟斤拷??
     */
    public static void setSpeeds(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
    }

    /**
     * 锟斤拷取锟斤拷锟斤拷锟斤拷??
     * @return 锟斤拷锟斤拷锟斤拷??
     */
    public static float getLongSpeeds() {
        return sLongSpeed;
    }

    /**
     * 锟斤拷锟矫筹拷锟斤拷锟斤拷??
     * @param speed 锟斤拷锟斤拷锟斤拷??
     */
    public static void setLongSpeeds(float speed) {
        sLongSpeed = speed;
    }


    // ===== 全锟斤拷锟斤拷锟狡凤拷锟斤拷 =====

    /**
     * 锟斤拷锟斤拷全锟斤拷
     */
    public void startFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.enterFullscreen(activity);
        }
    }

    /**
     * 锟剿筹拷全锟斤拷
     */
    public void stopFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.exitFullscreen(activity);
        }
    }

    /**
     * 锟角凤拷全锟斤拷
     * @return true 全锟斤拷
     */
    public boolean isFullScreen() {
        return mFullscreenHelper != null && mFullscreenHelper.isFullscreen();
    }

    /**
     * 锟角凤拷小锟斤拷模式
     * @return true 小锟斤拷
     */
    public boolean isTinyScreen() {
        return mCurrentPlayerState == PlayerConstants.PLAYER_TINY_SCREEN;
    }

    /**
     * 锟斤拷锟斤拷全锟斤拷时锟角凤拷锟皆讹拷锟斤拷转锟斤拷??
     * @param autoRotate true 锟皆讹拷锟斤拷转锟斤拷默锟较ｏ拷
     */
    public void setAutoRotateOnFullscreen(boolean autoRotate) {
        this.mAutoRotateOnFullscreen = autoRotate;
    }

    /**
     * 锟角凤拷全锟斤拷时锟皆讹拷锟斤拷转锟斤拷??
     * @return true 锟皆讹拷锟斤拷转
     */
    public boolean isAutoRotateOnFullscreen() {
        return mAutoRotateOnFullscreen;
    }

    // ===== 锟斤拷锟斤拷锟节猴拷锟叫伙拷 (Requirements: 1.11) =====

    /**
     * 选锟今播凤拷锟节猴拷
     * @param engineType 锟节猴拷锟斤拷锟斤拷 (ijk, exo, ali, default)
     */
    @SuppressWarnings("unchecked")
    public void selectPlayerFactory(String engineType) {
        if (engineType == null) {
            engineType = PlayerConstants.ENGINE_DEFAULT;
        }
        
        switch (engineType) {
            case PlayerConstants.ENGINE_IJK:
                // IJK 锟斤拷锟斤拷??
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
            case PlayerConstants.ENGINE_EXO:
                // ExoPlayer - 锟斤拷要锟斤拷锟斤拷锟斤拷??
                try {
                    Class<?> exoClass = Class.forName("com.shuyu.gsyvideoplayer.player.Exo2PlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) exoClass);
                } catch (ClassNotFoundException e) {
                    // 锟斤拷锟剿碉拷默??
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_ALI:
                // 锟斤拷锟斤拷锟狡诧拷锟斤拷锟斤拷 - 锟斤拷要锟斤拷锟斤拷锟斤拷??
                try {
                    Class<?> aliClass = Class.forName("com.shuyu.gsyvideoplayer.player.AliPlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) aliClass);
                } catch (ClassNotFoundException e) {
                    // 锟斤拷锟剿碉拷默??
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                // 使锟斤拷系统 MediaPlayer
                PlayerFactory.setPlayManager(SystemPlayerManager.class);
                break;
        }
    }

    // ===== 状态锟斤拷??=====

    /**
     * 锟斤拷锟矫诧拷锟斤拷状??
     * @param playState 锟斤拷锟斤拷状??
     */
    protected void setOrangePlayState(int playState) {
        mCurrentPlayState = playState;
        notifyPlayStateChanged(playState);
        
        // 使锟斤拷 post 确锟斤拷锟斤拷锟斤拷锟阶刺拷锟斤拷潞锟斤拷锟斤拷锟??锟斤拷锟截匡拷锟斤拷??
        post(new Runnable() {
            @Override
            public void run() {
                // 锟斤拷锟斤拷状态时锟斤拷示锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟皆讹拷锟斤拷锟截讹拷时??
                if (playState == PlayerConstants.STATE_PLAYING) {
                    showController();
                } else if (playState == PlayerConstants.STATE_PAUSED) {
                    // 锟斤拷停时锟斤拷示锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟皆讹拷锟斤拷锟斤拷
                    showController();
                    cancelAutoHideTimer();
                } else {
                    cancelAutoHideTimer();
                }
            }
        });
    }

    /**
     * 锟斤拷锟矫诧拷锟斤拷锟斤拷状??
     * @param playerState 锟斤拷锟斤拷锟斤拷状??
     */
    protected void setOrangePlayerState(int playerState) {
        mCurrentPlayerState = playerState;
        notifyPlayerStateChanged(playerState);
    }

    /**
     * 锟斤拷取锟斤拷前锟斤拷锟斤拷状??
     * @return 锟斤拷锟斤拷状??
     */
    public int getPlayState() {
        return mCurrentPlayState;
    }

    /**
     * 锟斤拷取锟斤拷前锟斤拷锟斤拷锟斤拷状??
     * @return 锟斤拷锟斤拷锟斤拷状??
     */
    public int getPlayerState() {
        return mCurrentPlayerState;
    }

    // ===== 锟斤拷锟斤拷锟斤拷锟斤拷??=====

    /**
     * 锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷
     * @param listener 锟斤拷锟斤拷??
     */
    public void addOnStateChangeListener(OnStateChangeListener listener) {
        if (listener != null && !mStateChangeListeners.contains(listener)) {
            mStateChangeListeners.add(listener);
        }
    }

    /**
     * 锟狡筹拷状态锟斤拷锟斤拷锟斤拷
     * @param listener 锟斤拷锟斤拷??
     */
    public void removeOnStateChangeListener(OnStateChangeListener listener) {
        mStateChangeListeners.remove(listener);
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟阶刺拷锟斤拷锟斤拷锟?
     */
    public void clearOnStateChangeListeners() {
        mStateChangeListeners.clear();
    }

    /**
     * 通知锟斤拷锟斤拷状态锟斤拷??
     */
    private void notifyPlayStateChanged(int playState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayStateChanged(playState);
            }
        }
        // 通知锟斤拷锟斤拷锟斤拷锟?
        if (mUseOrangeComponents) {
            notifyComponentsPlayStateChanged(playState);
        }
    }

    /**
     * 通知锟斤拷锟斤拷锟斤拷状态锟斤拷??
     */
    private void notifyPlayerStateChanged(int playerState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayerStateChanged(playerState);
            }
        }
        // 通知锟斤拷锟斤拷锟斤拷锟?
        if (mUseOrangeComponents) {
            notifyComponentsPlayerStateChanged(playerState);
        }
    }

    /**
     * 通知锟斤拷锟斤拷锟斤拷锟阶刺拷锟??
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
     * 通知锟斤拷锟斤拷锟斤拷锟斤拷锟阶刺拷锟??
     */
    private void notifyComponentsPlayerStateChanged(int playerState) {
                if (mPrepareView != null) mPrepareView.onPlayerStateChanged(playerState);
        if (mCompleteView != null) mCompleteView.onPlayerStateChanged(playerState);
        if (mErrorView != null) mErrorView.onPlayerStateChanged(playerState);
        if (mTitleView != null) mTitleView.onPlayerStateChanged(playerState);
        if (mVodControlView != null) mVodControlView.onPlayerStateChanged(playerState);
        if (mLiveControlView != null) mLiveControlView.onPlayerStateChanged(playerState);
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟?
     * Requirements: 3.2, 3.3, 6.4
     * 
     * 锟脚伙拷说锟斤拷??
     * - 锟斤拷锟接匡拷指锟斤拷锟介，确锟斤拷锟斤拷锟斤拷殉锟绞硷拷锟?
     * - 确锟斤拷锟斤拷锟斤拷锟竭程革拷锟斤拷 UI
     * - 锟斤拷锟斤拷锟斤拷锟轿达拷锟绞硷拷锟斤拷锟斤拷锟??
     */
    public void updateComponentsProgress(int duration, int position) {
        // 空指针检查：确保组件已初始化
        if (mVodControlView == null && mLiveControlView == null) {
            android.util.Log.w(TAG, "updateComponentsProgress: 控制组件未初始化");
            return;
        }
        
        // 确锟斤拷锟斤拷锟斤拷锟竭程革拷锟斤拷 UI
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            // 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷叱蹋锟絧ost 锟斤拷锟斤拷锟竭筹拷执锟斤拷
            final int finalDuration = duration;
            final int finalPosition = position;
            post(new Runnable() {
                @Override
                public void run() {
                    updateComponentsProgressInternal(finalDuration, finalPosition);
                }
            });
        } else {
            // 锟斤拷锟斤拷锟斤拷锟竭程ｏ拷直锟斤拷执锟斤拷
            updateComponentsProgressInternal(duration, position);
        }
    }
    
    /**
     * 锟节诧拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷龋锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷叱痰锟斤拷锟??
     */
    private void updateComponentsProgressInternal(int duration, int position) {
        // 锟斤拷锟铰点播锟斤拷锟斤拷锟斤拷锟?
        if (mVodControlView != null) {
            try {
                mVodControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: VodControlView 锟斤拷锟斤拷失锟斤拷", e);
            }
        }
        
        // 锟斤拷锟斤拷直锟斤拷锟斤拷锟斤拷锟斤拷锟?
        if (mLiveControlView != null) {
            try {
                mLiveControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: LiveControlView 锟斤拷锟斤拷失锟斤拷", e);
            }
        }
    }

    /**
     * 锟斤拷锟矫斤拷锟饺硷拷锟斤拷??
     * @param listener 锟斤拷锟斤拷??
     */
    public void setOnProgressListener(OnProgressListener listener) {
        this.mProgressListener = listener;
    }

    /**
     * 锟斤拷锟矫诧拷锟斤拷锟斤拷杉锟斤拷锟??
     * @param listener 锟斤拷锟斤拷??
     */
    public void setOnPlayCompleteListener(OnPlayCompleteListener listener) {
        this.mPlayCompleteListener = listener;
    }


    // ===== 锟斤拷锟斤拷锟斤拷锟斤拷??=====

    /**
     * 锟斤拷取锟斤拷频锟斤拷锟斤拷??
     * @return 锟斤拷锟斤拷??
     */
    public OrangeVideoController getVideoController() {
        return mOrangeController;
    }

    /**
     * 锟斤拷锟斤拷锟斤拷频锟斤拷锟斤拷??
     * @param controller 锟斤拷锟斤拷??
     */
    public void setVideoController(OrangeVideoController controller) {
        this.mOrangeController = controller;

        // 通知控制器关联的播放器视图，以便初始化 VideoEventManager
        if (controller != null) {
            controller.setVideoView(this);

            // 绑定 TitleView 事件
            if (mTitleView != null) {
                mTitleView.setController(controller);
            }
            
            // 设置 VodControlView 的控制器引用
            if (mVodControlView != null) {
                mVodControlView.setOrangeVideoController(controller);
            }
            
            // 确保事件绑定（处理控制器在组件创建后设置的情况）
            ensureEventBinding();
        }
    }

    // ===== 锟斤拷锟解功锟斤拷 =====

    /**
     * 锟斤拷锟斤拷锟角凤拷锟斤拷锟斤拷锟皆讹拷锟斤拷取锟斤拷锟斤拷图锟斤拷??
     * Requirements: 6.2 - THE OrangevideoView SHALL 支锟斤拷锟皆讹拷锟斤拷取锟斤拷频锟斤拷锟斤拷图锟斤拷??
     * @param enabled true 锟斤拷锟矫ｏ拷false 锟斤拷锟斤拷
     */
    public void setAutoThumbnailEnabled(boolean enabled) {
        this.mAutoThumbnailEnabled = enabled;
    }

    /**
     * 锟角凤拷锟斤拷锟斤拷锟皆讹拷锟斤拷锟斤拷??
     * @return true 锟斤拷锟斤拷
     */
    public boolean isAutoThumbnailEnabled() {
        return mAutoThumbnailEnabled;
    }

    /**
     * 锟斤拷锟斤拷默锟斤拷锟斤拷锟斤拷??
     * @param thumbnail 锟斤拷锟斤拷??
     */
    public void setDefaultThumbnail(Object thumbnail) {
        this.mDefaultThumbnail = thumbnail;
    }

    /**
     * 锟斤拷取默锟斤拷锟斤拷锟斤拷??
     * @return 锟斤拷锟斤拷??
     */
    public Object getDefaultThumbnail() {
        return mDefaultThumbnail;
    }

    /**
     * 锟届步锟斤拷取锟斤拷频锟斤拷一帧锟斤拷为锟斤拷锟斤拷图
     * Requirements: 6.2 - THE OrangevideoView SHALL 支锟斤拷锟皆讹拷锟斤拷取锟斤拷频锟斤拷锟斤拷图锟斤拷??
     * @param callback 锟截碉拷
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
     * 锟届步锟斤拷取指锟斤拷时锟斤拷锟斤拷锟狡抵?
     * @param timeUs 时锟戒（微锟诫）
     * @param callback 锟截碉拷
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
     * 锟皆讹拷锟斤拷锟斤拷锟斤拷锟斤拷图锟斤拷锟斤拷锟斤拷锟斤拷锟??
     */
    private void autoLoadThumbnail() {
        if (!mAutoThumbnailEnabled || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        
        getVideoFirstFrameAsync(new VideoThumbnailHelper.ThumbnailCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap bitmap) {
                mDefaultThumbnail = bitmap;
                // 锟斤拷锟矫凤拷锟斤拷
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
     * 锟斤拷锟斤拷锟角凤拷锟斤拷洳ワ拷锟轿伙拷锟?
     * Requirements: 6.3 - THE OrangevideoView SHALL 支锟街硷拷锟戒播锟斤拷位锟矫癸拷锟斤拷 (setKeepVideoPlaying)
     * @param keep true 锟斤拷锟斤拷
     */
    public void setKeepVideoPlaying(boolean keep) {
        this.mKeepVideoPlaying = keep;
    }

    /**
     * 锟角凤拷锟斤拷洳ワ拷锟轿伙拷锟?
     * @return true 锟斤拷锟斤拷
     */
    public boolean isKeepVideoPlaying() {
        return mKeepVideoPlaying;
    }

    /**
     * 锟斤拷锟芥当前锟斤拷锟脚斤拷锟斤拷
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
     * 锟街革拷锟斤拷锟脚斤拷锟斤拷
     * Requirements: 6.3
     * @return true 锟缴癸拷锟街革拷
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
     * 锟斤拷取锟斤拷锟斤拷牟锟斤拷沤锟??
     * @return 锟斤拷锟斤拷位锟矫ｏ拷锟斤拷锟诫）
     */
    public long getSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return 0;
        }
        return PlaybackProgressManager.getInstance(getContext()).getProgress(mVideoUrl);
    }

    /**
     * 锟斤拷锟斤拷欠锟斤拷斜锟斤拷锟侥斤拷??
     * @return true 锟叫憋拷锟斤拷慕锟斤拷锟?
     */
    public boolean hasSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        return PlaybackProgressManager.getInstance(getContext()).hasProgress(mVideoUrl);
    }

    /**
     * 锟斤拷锟斤拷锟角帮拷锟狡碉拷谋锟斤拷锟斤拷??
     */
    public void clearSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        PlaybackProgressManager.getInstance(getContext()).removeProgress(mVideoUrl);
    }

    // ===== 锟斤拷锟斤拷片头片尾锟斤拷锟斤拷 (Requirements: 6.4) =====

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷片头时锟斤拷
     * Requirements: 6.4 - THE OrangevideoView SHALL 支锟斤拷锟斤拷锟斤拷片头片尾锟斤拷锟斤拷
     * @param timeMs 时锟斤拷锟斤拷锟斤拷锟诫）
     */
    public void setSkipIntroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroTime(timeMs);
        }
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷片头时锟斤拷锟斤拷锟斤拷??
     * @param seconds 时锟斤拷锟斤拷锟斤拷??
     */
    public void setSkipIntroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroSeconds(seconds);
        }
    }

    /**
     * 锟斤拷取锟斤拷锟斤拷片头时锟斤拷
     * @return 时锟斤拷锟斤拷锟斤拷锟诫）
     */
    public long getSkipIntroTime() {
        return mSkipManager != null ? mSkipManager.getSkipIntroTime() : 0;
    }

    /**
     * 锟斤拷锟斤拷锟角凤拷锟斤拷锟斤拷锟斤拷锟斤拷片头
     * @param enabled 锟角凤拷锟斤拷锟斤拷
     */
    public void setSkipIntroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroEnabled(enabled);
        }
    }

    /**
     * 锟角凤拷锟斤拷锟斤拷锟斤拷锟斤拷片头
     * @return true 锟斤拷锟斤拷
     */
    public boolean isSkipIntroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipIntroEnabled();
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷片尾时锟斤拷
     * Requirements: 6.4 - THE OrangevideoView SHALL 支锟斤拷锟斤拷锟斤拷片头片尾锟斤拷锟斤拷
     * @param timeMs 时锟斤拷锟斤拷锟斤拷锟诫）
     */
    public void setSkipOutroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroTime(timeMs);
        }
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷片尾时锟斤拷锟斤拷锟斤拷??
     * @param seconds 时锟斤拷锟斤拷锟斤拷??
     */
    public void setSkipOutroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroSeconds(seconds);
        }
    }

    /**
     * 锟斤拷取锟斤拷锟斤拷片尾时锟斤拷
     * @return 时锟斤拷锟斤拷锟斤拷锟诫）
     */
    public long getSkipOutroTime() {
        return mSkipManager != null ? mSkipManager.getSkipOutroTime() : 0;
    }

    /**
     * 锟斤拷锟斤拷锟角凤拷锟斤拷锟斤拷锟斤拷锟斤拷片尾
     * @param enabled 锟角凤拷锟斤拷锟斤拷
     */
    public void setSkipOutroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroEnabled(enabled);
        }
    }

    /**
     * 锟角凤拷锟斤拷锟斤拷锟斤拷锟斤拷片尾
     * @return true 锟斤拷锟斤拷
     */
    public boolean isSkipOutroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipOutroEnabled();
    }

    /**
     * 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??
     * @param listener 锟斤拷锟斤拷??
     */
    public void setOnSkipListener(SkipManager.OnSkipListener listener) {
        if (mSkipManager != null) {
            mSkipManager.setOnSkipListener(listener);
        }
    }

    /**
     * 锟斤拷取锟斤拷锟斤拷锟斤拷锟斤拷??
     * @return 锟斤拷锟斤拷锟斤拷锟斤拷??
     */
    public SkipManager getSkipManager() {
        return mSkipManager;
    }

    /**
     * 锟斤拷取锟斤拷频锟斤拷锟斤拷锟斤拷锟斤拷??
     * @return 锟斤拷频锟斤拷锟斤拷锟斤拷锟斤拷??
     */
    public VideoScaleManager getVideoScaleManager() {
        return mVideoScaleManager;
    }
    
    /**
     * 锟斤拷取锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷
     * @return 锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷
     */
    public PlaybackStateManager getPlaybackStateManager() {
        return mPlaybackStateManager;
    }
    
    /**
     * 锟斤拷取锟斤拷锟阶刺拷锟斤拷锟斤拷锟?
     * @return 锟斤拷锟阶刺拷锟斤拷锟斤拷锟?
     */
    public ComponentStateManager getComponentStateManager() {
        return mComponentStateManager;
    }
    
    /**
     * 锟斤拷取锟斤拷锟斤拷指锟斤拷锟斤拷锟??
     * @return 锟斤拷锟斤拷指锟斤拷锟斤拷锟??
     */
    public ErrorRecoveryManager getErrorRecoveryManager() {
        return mErrorRecoveryManager;
    }

    /**
     * 刷锟斤拷锟斤拷频锟斤拷示锟斤拷锟斤拷
     * 锟斤拷锟斤拷锟节改憋拷锟斤拷频锟斤拷锟斤拷锟斤拷刷锟斤拷锟斤拷示
     */
    public void refreshVideoShowType() {
        changeTextureViewShowType();
    }

    /**
     * 锟角凤拷为直锟斤拷锟斤拷??
     * @return true 直锟斤拷
     */
    public boolean isLiveVideo() {
        return mIsLiveVideo;
    }

    /**
     * 锟斤拷锟斤拷锟角凤拷为直锟斤拷锟斤拷??
     * @param isLive true 直锟斤拷
     */
    public void setLiveVideo(boolean isLive) {
        this.mIsLiveVideo = isLive;
    }

    /**
     * 锟角凤拷锟斤拷锟斤拷锟斤拷探
     * @return true 锟斤拷锟斤拷锟斤拷探
     */
    public boolean isSniffing() {
        return mIsSniffing;
    }

    /**
     * 锟斤拷始锟斤拷频锟斤拷??
     * Requirements: 6.1 - THE OrangevideoView SHALL 支锟斤拷锟斤拷频锟斤拷探锟斤拷锟斤拷 (startSniffing)
     */
    public void startSniffing() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            debug("startSniffing: url is empty");
            return;
        }
        startSniffing(mVideoUrl, null);
    }

    /**
     * 锟斤拷始锟斤拷频锟斤拷探锟斤拷锟斤拷锟皆讹拷锟斤拷锟斤拷锟斤拷头锟斤拷
     * @param url 锟斤拷页锟斤拷址
     * @param headers 锟皆讹拷锟斤拷锟斤拷锟斤拷头
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
                // 通知锟斤拷锟斤拷??
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
                // 通知锟斤拷锟斤拷??
                for (OnStateChangeListener listener : mStateChangeListeners) {
                    if (listener instanceof OnSniffingListener) {
                        ((OnSniffingListener) listener).onSniffingFinish(videoList, videoSize);
                    }
                }
            }
        });
    }

    /**
     * 锟斤拷锟斤拷锟斤拷频锟斤拷探
     */
    public void stopSniffing() {
        mIsSniffing = false;
        VideoSniffing.stop(true);
        setOrangePlayState(STATE_ENDSNIFFING);
    }

    /**
     * 锟斤拷探锟斤拷锟斤拷锟斤拷锟斤拷??
     */
    public interface OnSniffingListener {
        /**
         * 锟斤拷锟秸碉拷锟斤拷频锟斤拷??
         */
        void onSniffingReceived(String contentType, java.util.HashMap<String, String> headers, 
                               String title, String url);
        
        /**
         * 锟斤拷探锟斤拷锟?
         */
        void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize);
    }

    // ===== 锟斤拷锟斤拷模式 =====

    /**
     * 锟斤拷锟矫碉拷锟斤拷模式
     * @param debug true 锟斤拷锟斤拷锟斤拷??
     */
    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    /**
     * 锟角凤拷锟斤拷锟侥Ｊ?
     * @return true 锟斤拷锟斤拷模式
     */
    public boolean isDebug() {
        return mDebug;
    }

    /**
     * 锟斤拷锟斤拷锟斤拷志
     * @param message 锟斤拷志锟斤拷息
     */
    protected void debug(Object message) {
        if (mDebug) {
                    }
    }

    // ===== 锟斤拷锟竭凤拷锟斤拷 =====

    /**
     * 锟斤拷取 Activity
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
     * 锟角凤拷锟斤拷锟节诧拷锟斤拷
     * @return true 锟斤拷锟节诧拷锟斤拷
     */
    public boolean isPlaying() {
        return mCurrentPlayState == PlayerConstants.STATE_PLAYING;
    }

    /**
     * 锟角凤拷锟斤拷锟斤拷通状态锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷小锟斤拷??
     * @return true 锟斤拷通状??
     */
    public boolean isInNormalState() {
        return !isFullScreen() && !isTinyScreen();
    }

    // ===== 锟斤拷锟斤拷锟斤拷示 GestureView =====
    private com.orange.playerlibrary.component.GestureView mGestureView;

    /**
     * 锟斤拷写锟斤拷示锟斤拷锟饺对伙拷锟斤拷使锟斤拷 GestureView 锟斤拷锟?Dialog
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
     * 锟斤拷写锟斤拷示锟斤拷锟斤拷锟皆伙拷锟斤拷使锟斤拷 GestureView 锟斤拷锟?Dialog
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
     * 锟斤拷写锟斤拷示锟斤拷锟饺对伙拷锟斤拷使锟斤拷 GestureView 锟斤拷锟?Dialog
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
     * 锟斤拷写锟斤拷锟斤拷锟斤拷锟饺对伙拷??
     */
    @Override
    protected void dismissBrightnessDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 锟斤拷写锟斤拷锟斤拷锟斤拷锟斤拷锟皆伙拷??
     */
    @Override
    protected void dismissVolumeDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 锟斤拷写锟斤拷锟截斤拷锟饺对伙拷??
     */
    @Override
    protected void dismissProgressDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 确锟斤拷 GestureView 锟窖筹拷始锟斤拷
     */
    private void ensureGestureView() {
        if (mGestureView == null) {
            mGestureView = new com.orange.playerlibrary.component.GestureView(getContext());
            // 锟斤拷锟矫诧拷锟街诧拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟?
            android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
            addView(mGestureView, lp);
        }
    }

    /**
     * 锟斤拷取 GestureView
     */
    public com.orange.playerlibrary.component.GestureView getGestureView() {
        ensureGestureView();
        return mGestureView;
    }

    /**
     * 锟斤拷锟矫诧拷锟斤拷状态锟斤拷锟斤拷锟斤拷??API??
     * @param state 状??
     */
    public void setThisPlayState(int state) {
        setOrangePlayState(state);
    }

    /**
     * 锟斤拷锟矫诧拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷??API??
     * @param state 状??
     */
    public void setThisPlayerState(int state) {
        setOrangePlayerState(state);
    }

    // ===== GSYBaseVideoPlayer 锟斤拷锟襟方凤拷实锟斤拷 =====

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
     * 锟斤拷写锟斤拷锟截帮拷钮锟斤拷取锟斤拷锟斤拷锟斤拷锟斤拷??null ??GSY 锟斤拷锟斤拷锟斤拷锟斤拷锟截帮拷??
     * 锟斤拷锟截帮拷钮??TitleView 锟斤拷锟斤拷锟斤拷锟?
     */
    @Override
    public android.widget.ImageView getBackButton() {
        return null;
    }

    /**
     * 锟斤拷取全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷锟芥本锟斤拷
     * @return OrangevideoView 锟斤拷锟矫伙拷锟斤拷蚍祷乜锟?
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
     * 锟斤拷写锟斤拷锟阶刺拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟矫革拷锟斤拷??getFullWindowPlayer 锟斤拷锟斤拷 ClassCastException
     * 锟斤拷为 OrangevideoView 锟教筹拷??GSYBaseVideoPlayer 锟斤拷锟斤拷??GSYVideoPlayer
     */
    @Override
    protected void checkoutState() {
        removeCallbacks(mOrangeCheckoutTask);
        mInnerHandler.postDelayed(mOrangeCheckoutTask, 500);
    }

    /**
     * 锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷状态锟斤拷锟斤拷锟??
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
     * 锟斤拷写锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷确锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷确锟斤拷始锟斤拷锟斤拷锟斤拷锟斤拷锟阶??
     */
    @Override
    @SuppressWarnings({"ResourceType", "unchecked"})
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
                
        // 强锟斤拷锟斤拷锟斤拷状态锟斤拷锟酵碉拷锟斤拷锟斤拷锟斤拷锟斤拷锟皆达拷锟斤拷牟锟斤拷锟??
        hideStatusBarAndNavigation(context);
        
        // 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷远锟斤拷锟阶拷锟斤拷锟斤拷锟阶拷锟侥伙拷锟斤拷锟斤拷锟?
        if (mAutoRotateOnFullscreen) {
            Activity activity = getActivity();
            if (activity != null) {
                                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        
        // 锟斤拷锟矫革拷锟洁方锟斤拷锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷 true 锟矫革拷锟斤拷也锟斤拷锟斤拷??
        GSYBaseVideoPlayer fullPlayer = super.startWindowFullscreen(context, true, true);
        debugLog("锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷: " + (fullPlayer != null ? fullPlayer.getClass().getSimpleName() : "null"));
        
        // 锟斤拷锟??OrangevideoView锟斤拷同锟斤拷状??
        if (fullPlayer instanceof OrangevideoView) {
            final OrangevideoView orangeFullPlayer = (OrangevideoView) fullPlayer;
                        
            // 锟斤拷锟斤拷锟斤拷锟斤拷全锟斤拷锟斤拷志
            orangeFullPlayer.mIfCurrentIsFullscreen = true;
            
            // 锟接筹拷同锟斤拷状态锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷珊锟斤拷锟斤拷锟绞撅拷锟斤拷锟??
            orangeFullPlayer.postDelayed(new Runnable() {
                @Override
                public void run() {
                                        
                    // 同锟斤拷锟斤拷锟斤拷
                    if (mTitleView != null && orangeFullPlayer.mTitleView != null) {
                        String title = mTitleView.getTitle();
                        orangeFullPlayer.mTitleView.setTitle(title);
                                                
                        // 锟斤拷全锟斤拷 TitleView bindController
                        if (mOrangeController != null) {
                            orangeFullPlayer.mTitleView.setController(mOrangeController);
                        }
                    }
                    
                    // 缁戝畾鍏ㄥ睆鎾斁鍣ㄧ殑 VodControlView 鍒?VideoEventManager
                    if (mOrangeController != null && orangeFullPlayer.mVodControlView != null) {
                        com.orange.playerlibrary.VideoEventManager eventManager = 
                                mOrangeController.getVideoEventManager();
                        if (eventManager != null) {
                            eventManager.bindControllerComponents(orangeFullPlayer.mVodControlView);
                        }
                    }
                    
                    // 同锟斤拷锟斤拷前锟斤拷锟斤拷状态锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟?
                    orangeFullPlayer.setOrangePlayState(mCurrentPlayState);
                    orangeFullPlayer.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
                                        
                    // 锟斤拷锟斤拷注锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟侥斤拷锟饺硷拷锟斤拷锟斤拷锟斤拷确锟斤拷锟斤拷锟饺革拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷
                    if (orangeFullPlayer.mComponentStateManager != null) {
                        orangeFullPlayer.mComponentStateManager.reregisterProgressListener(orangeFullPlayer);
                    }
                    
                    // 全锟斤拷时锟斤拷示锟斤拷锟斤拷锟斤拷
                    orangeFullPlayer.showController();
                    // 强锟斤拷锟斤拷示 TitleView
                                        if (orangeFullPlayer.mTitleView != null) {
                        orangeFullPlayer.mTitleView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mTitleView.bringToFront();
                        debugLog("强锟斤拷锟斤拷示 TitleView, visibility=" + orangeFullPlayer.mTitleView.getVisibility());
                    }
                    // 强锟斤拷通知 VodControlView 锟斤拷锟斤拷全锟斤拷状态锟斤拷锟斤拷示锟斤拷幕锟斤拷锟斤拷
                    if (orangeFullPlayer.mVodControlView != null) {
                        orangeFullPlayer.mVodControlView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mVodControlView.bringToFront();
                        orangeFullPlayer.mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                        debugLog("通知 VodControlView 全锟斤拷状?? visibility=" + orangeFullPlayer.mVodControlView.getVisibility());
                    }
                    orangeFullPlayer.requestLayout();
                    
                                    }
            }, 300);
        } else {
            debugLog("全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷??OrangevideoView: " + (fullPlayer != null ? fullPlayer.getClass().getName() : "null"));
        }
        
        // 通知锟斤拷前锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷全锟斤拷状??
        setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        return fullPlayer;
    }
    
    /**
     * 锟斤拷锟斤拷状态锟斤拷锟酵碉拷锟斤拷锟斤拷
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
            
            // 锟斤拷锟斤拷 ActionBar
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
     * 锟斤拷写锟剿筹拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷 ClassCastException
     */
    @Override
    @SuppressWarnings("ResourceType")
    protected void clearFullscreenLayout() {
                
        if (!mFullAnimEnd) {
                        return;
        }
        
        // 锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷状??
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        if (oldF != null && oldF instanceof OrangevideoView) {
            OrangevideoView orangeVideoPlayer = (OrangevideoView) oldF;
                        
            // 使锟斤拷 PlaybackStateManager 锟斤拷锟斤拷状??
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

    /**
     * 锟斤拷锟接诧拷锟斤拷锟斤拷锟截碉拷锟斤拷锟斤拷效??
     */
    @SuppressWarnings("ResourceType")
    protected void orangeBackToNormal() {
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        final OrangevideoView orangeVideoPlayer;
        
        if (oldF != null && oldF instanceof OrangevideoView) {
            orangeVideoPlayer = (OrangevideoView) oldF;
            // 锟斤拷锟斤拷锟酵??- 锟斤拷锟斤拷 pauseFullBackCoverLogic锟斤拷锟斤拷为锟斤拷锟斤拷??GSYVideoPlayer 锟斤拷锟斤拷
            // 锟斤拷锟接空硷拷椋拷锟斤拷锟?NPE
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
     * 锟斤拷锟接诧拷锟斤拷锟斤拷锟街革拷锟斤拷锟斤拷锟斤拷??
     */
    protected void orangeResolveNormalVideoShow(android.view.View oldF, android.view.ViewGroup vp, OrangevideoView orangeVideoPlayer) {
                
        // 锟斤拷锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷锟侥诧拷锟斤拷位锟矫ｏ拷锟截硷拷锟斤拷??cloneParams 之前锟斤拷锟斤拷??
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
        
        // 锟截硷拷锟斤拷锟斤拷锟斤拷锟斤拷??TextureView锟斤拷确??Surface 锟斤拷确锟街革拷
                addTextureView();
        
        // 锟接迟恢革拷锟斤拷锟斤拷位锟矫ｏ拷确??Surface 锟窖撅拷准锟斤拷??
        postDelayed(new Runnable() {
            @Override
            public void run() {
                                
                // 锟街革拷锟斤拷锟斤拷位锟矫ｏ拷锟截硷拷锟睫革拷锟斤拷
                if (savedPosition > 0) {
                                        seekTo(savedPosition);
                    
                    // 锟斤拷锟街帮拷诓锟斤拷牛锟斤拷锟斤拷锟斤拷锟斤拷锟?
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
                
                // 锟街革拷锟斤拷锟阶??
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                                    }
                
                // 通知锟斤拷锟阶刺拷锟??
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
        // 通知锟斤拷锟斤拷锟斤拷锟阶刺拷锟??
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
        // 锟斤拷全锟斤拷锟斤拷??
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
        // 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷贫锟斤拷锟斤拷锟斤拷锟绞?
        if (mPrepareView != null) {
            setOrangePlayState(8); // 锟狡讹拷锟斤拷锟界警锟斤拷状??
        }
    }

    // UI 状态锟戒化锟斤拷??- 锟斤拷实锟街ｏ拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟??
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
    
    // 锟皆讹拷锟斤拷锟截匡拷锟斤拷锟斤拷锟侥讹拷时??
    private static final int AUTO_HIDE_DELAY = 4000; // 4锟斤拷锟斤拷远锟斤拷锟斤拷锟?
    private Runnable mAutoHideRunnable;

    @Override
    protected void onClickUiToggle(android.view.MotionEvent e) {
        // 只锟节诧拷锟脚伙拷锟斤拷停状态时锟斤拷锟叫伙拷锟斤拷锟斤拷锟斤拷锟斤拷示/锟斤拷锟斤拷
        if (mCurrentPlayState != PlayerConstants.STATE_PLAYING && 
            mCurrentPlayState != PlayerConstants.STATE_PAUSED &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERING &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERED) {
            return;
        }
        
        // 锟斤拷锟斤拷谢锟斤拷锟斤拷锟斤拷锟斤拷锟??锟斤拷锟斤拷
        if (isControllerShowing()) {
            hideController();
        } else {
            showController();
        }
    }
    
    /**
     * 锟斤拷示锟斤拷锟斤拷??
     */
    public void showController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.VISIBLE);
        }
        // 全锟斤拷时锟斤拷示锟斤拷锟斤拷锟斤拷
        if (mTitleView != null && (mIfCurrentIsFullscreen || mCurrentPlayerState == PlayerConstants.PLAYER_FULL_SCREEN)) {
            mTitleView.setVisibility(android.view.View.VISIBLE);
        }
        // 锟斤拷锟斤拷锟皆讹拷锟斤拷锟截讹拷时??
        startAutoHideTimer();
    }
    
    /**
     * 锟斤拷锟截匡拷锟斤拷??
     */
    public void hideController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.GONE);
        }
        if (mTitleView != null) {
            mTitleView.setVisibility(android.view.View.GONE);
        }
        // 取锟斤拷锟皆讹拷锟斤拷锟截讹拷时??
        cancelAutoHideTimer();
    }
    
    /**
     * 锟斤拷锟斤拷锟斤拷锟角凤拷锟斤拷??
     */
    public boolean isControllerShowing() {
        return mVodControlView != null && mVodControlView.getVisibility() == android.view.View.VISIBLE;
    }
    
    /**
     * 锟斤拷取锟津创斤拷锟皆讹拷锟斤拷??Runnable
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
     * 锟斤拷锟斤拷锟皆讹拷锟斤拷锟截讹拷时??
     */
    private void startAutoHideTimer() {
        cancelAutoHideTimer();
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING && mInnerHandler != null) {
            mInnerHandler.postDelayed(getAutoHideRunnable(), AUTO_HIDE_DELAY);
        }
    }
    
    /**
     * 取锟斤拷锟皆讹拷锟斤拷锟截讹拷时??
     */
    private void cancelAutoHideTimer() {
        if (mInnerHandler != null && mAutoHideRunnable != null) {
            mInnerHandler.removeCallbacks(mAutoHideRunnable);
        }
    }

    /**
     * 锟斤拷写双锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷全使锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷状态锟叫断ｏ拷锟斤拷锟斤拷??GSY ??mCurrentState
     */
    protected void touchDoubleUp() {
                // 双锟斤拷锟斤拷停/锟斤拷锟斤拷 - 锟斤拷全使锟斤拷锟斤拷锟接诧拷锟斤拷锟斤拷锟斤拷状态锟斤拷??
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
            mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
            mCurrentPlayState == PlayerConstants.STATE_BUFFERED) {
                        // 直锟接碉拷锟斤拷锟斤拷锟斤拷??pause() 锟斤拷锟斤拷
            pause();
        } else if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
                        // 直锟接碉拷锟斤拷锟斤拷锟斤拷??resume() 锟斤拷锟斤拷
            resume();
        } else {
                    }
    }

    @Override
    public void startPlayLogic() {
        prepareVideo();
    }

    /**
     * 锟斤拷写 startAfterPrepared锟斤拷确??TextureView 锟斤拷确锟斤拷锟斤拷
     */
    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
    }

    // ===== 锟斤拷锟矫改变处锟斤拷 (Requirements: 2.3, 2.4, 5.1, 5.2) =====
    
    /**
     * 锟斤拷锟斤拷锟斤拷锟矫改变（锟斤拷锟斤拷幕锟斤拷转??
     * 锟斤拷锟芥当前锟斤拷锟斤拷状态锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟阶刺拷锟??
     * Requirements: 2.1, 2.2, 5.3, 5.4, 5.5
     * 
     * @param newConfig 锟铰碉拷锟斤拷锟斤拷
     */
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                
        // 使锟斤拷 PlaybackStateManager 锟斤拷锟芥当前状??
        if (mPlaybackStateManager != null) {
            mPlaybackStateManager.saveState(this);
        }
        
        // 使锟斤拷 ComponentStateManager 锟斤拷锟斤拷锟斤拷锟阶??
        if (mComponentStateManager != null) {
            mComponentStateManager.saveComponentState(
                (int) getDuration(), 
                (int) getCurrentPositionWhenPlaying()
            );
        }
        
        // 锟斤拷锟斤拷全锟斤拷/锟斤拷锟斤拷锟叫伙拷
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 锟斤拷锟斤拷 - 锟斤拷锟斤拷锟斤拷锟饺拷锟阶刺拷锟斤拷锟斤拷锟斤拷锟揭拷锟斤拷锟饺??
                    } else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            // 锟斤拷锟斤拷 - 锟斤拷锟斤拷锟饺拷锟阶刺拷锟斤拷锟斤拷锟斤拷锟揭拷顺锟饺??
                    }
        
        // 锟接迟恢革拷状态锟斤拷确锟斤拷锟斤拷锟斤拷锟斤拷锟?
        postDelayed(new Runnable() {
            @Override
            public void run() {
                                
                // 使锟斤拷 PlaybackStateManager 锟街革拷状??
                if (mPlaybackStateManager != null) {
                    mPlaybackStateManager.restoreState(OrangevideoView.this);
                }
                
                // 使锟斤拷 ComponentStateManager 锟街革拷锟斤拷锟阶??
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    // 锟斤拷锟斤拷注锟斤拷锟斤拷燃锟斤拷锟斤拷锟斤拷锟饺凤拷锟斤拷锟斤拷锟截斤拷锟斤拷锟斤拷雀锟斤拷锟斤拷锟??
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                                    }
                
                // 锟斤拷锟斤拷应锟斤拷锟斤拷频锟斤拷锟斤拷
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                }
                
                // 通知锟斤拷锟阶刺拷锟??
                notifyComponentsPlayStateChanged(mCurrentPlayState);
                notifyComponentsPlayerStateChanged(mCurrentPlayerState);
                
                            }
        }, 100);
    }

    /**
     * 锟斤拷写 getLayoutParams 锟斤拷锟斤拷锟斤拷确锟斤拷全锟斤拷锟斤拷锟斤拷锟斤拷使锟斤拷锟斤拷确锟侥诧拷锟街诧拷锟斤拷
     * 锟斤拷锟角斤拷锟饺拷锟斤拷锟斤拷锟斤拷锟斤拷锟侥癸拷??
     */
    @Override
    public android.view.ViewGroup.LayoutParams getLayoutParams() {
        android.view.ViewGroup.LayoutParams params = super.getLayoutParams();
        if (params == null) {
            // 锟斤拷锟矫伙拷胁锟斤拷植锟斤拷锟斤拷锟斤拷锟斤拷锟揭??MATCH_PARENT 锟侥诧拷??
            params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
        }
        return params;
    }
}
