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

/**
 * 閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
 * 閿熸暀绛规嫹 GSYBaseVideoPlayer閿熸枻鎷烽敓鏂ゆ嫹鍏ㄤ娇閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??UI
 */
public class OrangevideoView extends GSYBaseVideoPlayer {

    private static final String TAG = "OrangevideoView";
    
    // ===== 鐘舵€侀敓鏂ゆ嫹??=====
    public static final int STATE_STARTSNIFFING = PlayerConstants.STATE_STARTSNIFFING;
    public static final int STATE_ENDSNIFFING = PlayerConstants.STATE_ENDSNIFFING;
    
    // ===== 鍏ㄩ敓鏂ゆ嫹 SQLite 閿熻姤鍌?=====
    public static OrangeSharedSqlite sqlite;
    
    // ===== 閿熸枻鎷峰憳閿熸枻鎷烽敓鏂ゆ嫹 =====
    private String mVideoUrl;                     // 閿熸枻鎷峰墠閿熸枻鎷烽閿熸枻鎷峰潃
    private Map<String, String> mVideoHeaders;    // 閿熸枻鎷烽敓鏂ゆ嫹??
    private static float sSpeed = 1.0f;           // 閿熸枻鎷峰墠閿熸枻鎷??
    private static float sLongSpeed = 3.0f;       // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
    private boolean mKeepVideoPlaying = false;    // 閿熻鍑ゆ嫹閿熸枻鎷锋闯銉嫹閿熻娇浼欐嫹閿?
    private boolean mAutoThumbnailEnabled = true; // 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹??
    private Object mDefaultThumbnail = null;      // 榛橀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
    private boolean mIsLiveVideo = false;         // 閿熻鍑ゆ嫹鐩撮敓鏂ゆ嫹
    private boolean mIsSniffing = false;          // 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋帰
    private boolean mAutoRotateOnFullscreen = true; // 鍏ㄩ敓鏂ゆ嫹鏃堕敓瑙掑嚖鎷烽敓鐨嗚鎷烽敓鏂ゆ嫹杞敓鏂ゆ嫹??
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鐗囧熬閿熸枻鎷烽敓鏂ゆ嫹??=====
    private SkipManager mSkipManager;
    
    // ===== 閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??=====
    private VideoScaleManager mVideoScaleManager;
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹 =====
    private PlaybackStateManager mPlaybackStateManager;
    
    // ===== 閿熸枻鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿?=====
    private ComponentStateManager mComponentStateManager;
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹鎸囬敓鏂ゆ嫹閿熸枻鎷烽敓??=====
    private ErrorRecoveryManager mErrorRecoveryManager;
    
    // ===== 閿熺殕璁规嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹 =====
    private CustomFullscreenHelper mFullscreenHelper;
    
    // ===== ControlWrapper =====
    private com.orange.playerlibrary.interfaces.ControlWrapper mControlWrapper;
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹??=====
    private OrangeVideoController mOrangeController;
    
    // ===== UI 閿熸枻鎷烽敓?=====
    private com.orange.playerlibrary.component.PrepareView mPrepareView;
    private com.orange.playerlibrary.component.TitleView mTitleView;
    private com.orange.playerlibrary.component.VodControlView mVodControlView;
    private com.orange.playerlibrary.component.LiveControlView mLiveControlView;
    private com.orange.playerlibrary.component.CompleteView mCompleteView;
    private com.orange.playerlibrary.component.ErrorView mErrorView;
    private boolean mUseOrangeComponents = true; // 榛橀敓鏂ゆ嫹浣块敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹??=====
    private final List<OnStateChangeListener> mStateChangeListeners = new ArrayList<>();
    private OnProgressListener mProgressListener;
    private OnPlayCompleteListener mPlayCompleteListener;
    
    // ===== 閿熸枻鎷峰墠鐘??=====
    private int mCurrentPlayState = PlayerConstants.STATE_IDLE;
    private int mCurrentPlayerState = PlayerConstants.PLAYER_NORMAL;
    
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹妯″紡 =====
    private boolean mDebug = false;
    
    // ===== 鐢讳腑鐢绘ā寮忔爣蹇?=====
    private boolean mEnteringPiPMode = false;  // 鏄惁姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮?

    /**
     * 閿熸枻鎷烽敓灞婂嚱??
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
     * 閿熸枻鎷峰??- 閿熸枻鎷峰啓閿熸枻鎷烽敓娲佹柟閿熸枻鎷?
     */
    @Override
    protected void init(Context context) {
        super.init(context);
        initOrangePlayer();
    }

    /**
     * 閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹濮嬮敓鏂ゆ嫹
     */
    private void initOrangePlayer() {
        // 寮洪敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰閿熸枻鎷烽『閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
        mUseOrangeComponents = true;
        
        // 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
        mSkipManager = new SkipManager();
        mSkipManager.attachVideoView(this);
        
        // 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹棰戦敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
        mVideoScaleManager = new VideoScaleManager(this, PlayerSettingsManager.getInstance(getContext()));
                
        // 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
        mPlaybackStateManager = new PlaybackStateManager();
                
        // 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熼樁鍒侯剨鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        mComponentStateManager = new ComponentStateManager();
                
        // 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋寚閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        mErrorRecoveryManager = new ErrorRecoveryManager();
        mErrorRecoveryManager.attachVideoView(this);
                
        // 閿熸枻鎷峰閿熸枻鎷烽敓鐨嗚鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
        mFullscreenHelper = new CustomFullscreenHelper(this);
                
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷疯浆閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风洿閿熸枻鎷烽敓鏂ゆ嫹??
        setShowFullAnimation(false);
        setRotateViewAuto(false);
        setNeedLockFull(false);
        setLockLand(false);
        setRotateWithSystem(false);
        // 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熷彨浼欐嫹閿熸枻鎷烽敓鏂ゆ嫹
        setNeedShowWifiTip(false);
        // 閿熸枻鎷烽敓鏂ゆ嫹 OrientationUtils閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷疯浆閿熸枻鎷峰箷
        setNeedOrientationUtils(false);
        
        // 閿熸枻鎷烽敓鐭揪鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鐙★綇鎷峰弻閿熸枻鎷烽敓鏂ゆ嫹??閿熸枻鎷烽敓鑴氣槄鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??閿熸枻鎷烽敓鏂ゆ嫹/閿熸枻鎷烽敓鏂ゆ嫹??
        setIsTouchWiget(true);
        setIsTouchWigetFull(true);
        
        // 榛橀敓杈冪鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
        if (mUseOrangeComponents) {
            initOrangeComponents();
        }
        
        // 浣块敓鏂ゆ嫹 ComponentStateManager 娉ㄩ敓鏂ゆ嫹閿熸枻鎷风噧閿熸枻鎷烽敓??
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸埅闈╂嫹娉ㄩ敓缁擄紝閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹浜熼敓鏂ゆ嫹閿熸枻鎷疯繙閿熸枻鎷烽敓鏂ゆ嫹閿熼樁顫嫹閿?
        if (mComponentStateManager != null) {
            mComponentStateManager.reregisterProgressListener(this);
                    }
        
        // 閿熸枻鎷烽敓鐭洖纰夋嫹閿熸枻鎷烽敓鏂ゆ嫹
        setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                android.util.Log.d(TAG, "=== onPrepared callback ===");
                android.util.Log.d(TAG, "mEnteringPiPMode: " + mEnteringPiPMode);
                setOrangePlayState(PlayerConstants.STATE_PREPARED);
                // 閿熸枻鎷烽敓鏂ゆ嫹娆犻敓杞款€垫唻鎷烽敓?
                if (getDuration() <= 0) {
                    mIsLiveVideo = true;
                }
                // 搴旈敓鐭唻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鐙＄鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                                    }
                
                // 閿熸枻鎷烽敓鏂ゆ嫹娆犻敓鏂ゆ嫹閿熸彮顏庢嫹鎸囬敓楗侯偓鎷烽敓鏂ゆ嫹璋㈤敓缁炴唻鎷烽敓鏂ゆ嫹閿熶茎璇ф嫹閿熸枻鎷蜂綅??
                if (mFullscreenHelper != null && mFullscreenHelper.getPendingSeekPosition() > 0) {
                    final long pendingPosition = mFullscreenHelper.getPendingSeekPosition();
                    final boolean pendingResume = mFullscreenHelper.isPendingResume();
                                        
                    // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋寚閿熼樁??
                    mFullscreenHelper.clearPendingSeekPosition();
                    
                    // 閿熸帴绛规嫹鎵ч敓鏂ゆ嫹 seekTo閿熸枻鎷风‘閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰叏鍑嗛敓鏂ゆ嫹??
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                                                        seekTo(pendingPosition);
                            
                            // 閿熸枻鎷烽敓琛楊啚甯嫹璇撻敓鏂ゆ嫹鐗涢敓楗哄嚖鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
                            if (pendingResume && !isPlaying()) {
                                                                resume();
                            }
                        }
                    }, 100);
                } else {
                    // 閿熻闈╂嫹閿熸枻鎷烽敓鑴氭枻鎷烽敓楗猴綇鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熷彨浼欐嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
                    if (mKeepVideoPlaying) {
                        restorePlaybackProgress();
                    }
                }
                // 鎵ч敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご
                if (mSkipManager != null) {
                    mSkipManager.performSkipIntro();
                }
                // 鍑嗛敓鏂ゆ嫹閿熸枻鎷风強閿熸枻鎷疯繙閿熸枻鎷烽敓鏂ゆ嫹姘┿儻鎷烽敓闃??
                setOrangePlayState(PlayerConstants.STATE_PLAYING);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                setOrangePlayState(PlayerConstants.STATE_PLAYBACK_COMPLETED);
                // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鎱曢敓鏂ゆ嫹閿?
                if (mKeepVideoPlaying) {
                    clearSavedProgress();
                }
                // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘??
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
                // 閿熸枻鎷烽敓鏂ゆ嫹杞敓鏂ゆ嫹骞曢敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                setOrangePlayerState(PlayerConstants.PLAYER_NORMAL);
                // 閿熸枻鎷烽敓鏂ゆ嫹杞敓鏂ゆ嫹骞曢敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
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
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?UI閿熸枻鎷烽敓鏂ゆ嫹??GSY 閿熸枻鎷烽敓鏂ゆ嫹 UI??
     * 閿熸枻鎷烽敓鐭鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??PrepareView閿熸枻鎷稵itleView閿熸枻鎷稸odControlView閿熸枻鎷稢ompleteView閿熸枻鎷稥rrorView 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    // ===== 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰織閿熸埅纰夋嫹 =====
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
     * 閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    private void initOrangeComponents() {
        Context context = getContext();
        android.widget.RelativeLayout.LayoutParams matchParentParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);

        
        // 閿熸枻鎷烽敓鏂ゆ嫹 ControlWrapper 閿熸枻鎷烽敓鏂ゆ嫹閿熻姤鍒伴敓鏂ゆ嫹鍛橀敓鏂ゆ嫹閿熸枻鎷?
        mControlWrapper = createControlWrapper();
        
        // 1. PrepareView - 鍑嗛敓鏂ゆ嫹/閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰浘
        mPrepareView = new com.orange.playerlibrary.component.PrepareView(context);
        mPrepareView.attach(mControlWrapper);
        mPrepareView.setClickStart(); // 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹閿熺粸纭锋嫹閿??
        addView(mPrepareView, matchParentParams);
        
        // 2. CompleteView - 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熼叺?
        mCompleteView = new com.orange.playerlibrary.component.CompleteView(context);
        mCompleteView.attach(mControlWrapper);
        addView(mCompleteView, matchParentParams);

        // 3. ErrorView - 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰浘
        mErrorView = new com.orange.playerlibrary.component.ErrorView(context);
        mErrorView.attach(mControlWrapper);
        addView(mErrorView, matchParentParams);

        // 4. TitleView - 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹鏃堕敓鏂ゆ嫹绀洪敓鏂ゆ嫹
        mTitleView = new com.orange.playerlibrary.component.TitleView(context);
        mTitleView.attach(mControlWrapper);
        addView(mTitleView, matchParentParams);

        // 5. VodControlView - 閻愯鎸遍幒褍鍩?
        mVodControlView = new com.orange.playerlibrary.component.VodControlView(context);
        // 鐠佸墽鐤嗛幒褍鍩楅崳銊ョ穿閻㈩煉绱濈涵顔荤箽娴滃娆㈤懗钘夘檮鐞氼偆绮︾€规熬绱欓崷?attach 娑斿澧犵拋鍓х枂閿?
        if (mOrangeController != null) {
            mVodControlView.setOrangeVideoController(mOrangeController);
        }
        mVodControlView.attach(mControlWrapper);
        addView(mVodControlView, matchParentParams);
        
        // 閸掓繂顫愰悩鑸碘偓浣筋啎缂冾喕璐?IDLE閿涘本妯夌粈鍝勫櫙婢跺洩顫嬮崶?
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        
        // 绾喕绻氭禍瀣╂缂佹垵鐣?
        ensureEventBinding();
            }

    /**
     * 濞ｈ濮炵拫鍐槸閺冦儱绻?
     */
    public void debugLog(String msg) {
        if (mDebugLogCallback != null) {
            mDebugLogCallback.onLog(msg);
        }
            }

    /**
     * 绾喕绻氶幍鈧張澶嬪付閸掑墎绮嶆禒鍓佹畱娴滃娆㈤柈钘夊嚒缂佹垵鐣?
     * 婢跺嫮鎮婇崚婵嗩潗閸栨牠銆庢惔蹇涙６妫?
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
        
        // 缂佹垵鐣?VodControlView 娴滃娆?
        if (mVodControlView != null) {
            android.util.Log.d(TAG, "ensureEventBinding: binding VodControlView events");
            eventManager.bindControllerComponents(mVodControlView);
        }
        
        // 缂佹垵鐣?TitleView 娴滃娆?
        if (mTitleView != null) {
            android.util.Log.d(TAG, "ensureEventBinding: binding TitleView events");
            eventManager.bindTitleView(mTitleView);
        }
        
        android.util.Log.d(TAG, "ensureEventBinding: completed");
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹 ControlWrapper 瀹為敓鏂ゆ嫹
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
                // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹 pause() 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐩撮敓鎺ョ鎷??onVideoPause()
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
                // 浣块敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??GSY 閿熸枻鎷风姸??
                return videoView.isPlaying();
            }

            @Override
            public void togglePlay() {
                if (isPlaying()) {
                    pause();
                } else {
                    // 浣块敓鏂ゆ嫹 resume() 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??onVideoResume()閿熸枻鎷风‘閿熸枻鎷风姸鎬侀敓鏂ゆ嫹纭敓鏂ゆ嫹??
                    videoView.resume();
                }
            }

            @Override
            public void toggleFullScreen() {
                                if (isFullScreen()) {
                    // 閿熷壙绛规嫹鍏??- 浣块敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
                                        Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.exitFullscreen(activity);
                    }
                } else {
                    // 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹 - 浣块敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
                                        Activity activity = videoView.getActivity();
                    if (activity != null && mFullscreenHelper != null) {
                        mFullscreenHelper.enterFullscreen(activity);
                    }
                }
            }

            @Override
            public void toggleLockState() {
                // GSY 閿熸枻鎷锋敮閿熸枻鎷烽敓鏂ゆ嫹??
            }

            @Override
            public boolean isFullScreen() {
                // 浣块敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熷彨璁规嫹
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
                // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸嵎璇ф嫹瀹為敓鏂ゆ嫹
            }

            @Override
            public boolean isMute() {
                return false;
            }

            @Override
            public void setVolume(float volume) {
                // GSY 閿熸枻鎷风洿閿熸枻鎷锋敮??
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
                // 閿熸枻鎷烽敓鎴尅鎷烽敓鏂ゆ嫹??
            }

            @Override
            public void show() {
                // 閿熸枻鎷风ず閿熸枻鎷烽敓鏂ゆ嫹??
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
     * 閿熻鍑ゆ嫹浣块敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
     */
    public boolean isUseOrangeComponents() {
        return mUseOrangeComponents;
    }

    /**
     * 閿熸枻鎷峰彇 PrepareView
     */
    public com.orange.playerlibrary.component.PrepareView getPrepareView() {
        return mPrepareView;
    }

    /**
     * 閿熸枻鎷峰彇 TitleView
     */
    public com.orange.playerlibrary.component.TitleView getTitleView() {
        return mTitleView;
    }

    /**
     * 閿熸枻鎷峰彇 VodControlView
     */
    public com.orange.playerlibrary.component.VodControlView getVodControlView() {
        return mVodControlView;
    }

    /**
     * 閿熸枻鎷峰彇 LiveControlView
     */
    public com.orange.playerlibrary.component.LiveControlView getLiveControlView() {
        return mLiveControlView;
    }

    /**
     * 閿熸枻鎷峰彇 CompleteView
     */
    public com.orange.playerlibrary.component.CompleteView getCompleteView() {
        return mCompleteView;
    }

    /**
     * 閿熸枻鎷峰彇 ErrorView
     */
    public com.orange.playerlibrary.component.ErrorView getErrorView() {
        return mErrorView;
    }

    /**
     * 閿熸枻鎷峰彇 ControlWrapper
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻В閮ㄩ敓鏂ゆ嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鍙紮鎷烽敓楗虹櫢鎷??
     */
    public com.orange.playerlibrary.interfaces.ControlWrapper getControlWrapper() {
        return mControlWrapper;
    }

    // ===== 閿熸枻鎷烽閿熸枻鎷峰潃閿熸枻鎷烽敓鐭嚖鎷烽敓鏂ゆ嫹 (Requirements: 1.2) =====
    
    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷峰潃
     * @param url 閿熸枻鎷烽閿熸枻鎷峰潃
     */
    public void setUrl(String url) {
        setUrl(url, null);
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷峰潃閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰ご
     * @param url 閿熸枻鎷烽閿熸枻鎷峰潃
     * @param headers 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setUrl(String url, Map<String, String> headers) {
        this.mVideoUrl = url;
        this.mVideoHeaders = headers;
        // 浣块敓鏂ゆ嫹 GSYVideoPlayer ??setUp 閿熸枻鎷烽敓鏂ゆ嫹
        if (headers != null) {
            setUp(url, true, null, headers, "");
        } else {
            setUp(url, true, "");
        }
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷峰墠閿熸枻鎷烽閿熸枻鎷峰潃
     * @return 閿熸枻鎷烽閿熸枻鎷峰潃
     */
    public String getUrl() {
        return mVideoUrl;
    }

    // ===== 閿熸枻鎷烽敓鑴氬尅鎷烽敓鐙″嚖鎷烽敓鏂ゆ嫹 (Requirements: 1.3, 1.4, 1.5, 1.6) =====

    /**
     * 閿熸枻鎷峰閿熸枻鎷??
     */
    public void start() {
        mIsSniffing = false;
        mIsLiveVideo = false;
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘??
        if (mSkipManager != null) {
            mSkipManager.reset();
        }
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鎸囬敓鏂ゆ嫹閿??
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.startBlackScreenDetection();
            mErrorRecoveryManager.startStateConsistencyCheck();
        }
        setOrangePlayState(PlayerConstants.STATE_PREPARING);
        startPlayLogic();
    }

    /**
     * 閿熸枻鎷峰仠閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void pause() {
                // 閿熸枻鎷烽敓鑺ユ挱閿熻剼鏂ゆ嫹閿熸枻鎷?
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 鍋滄閿熸枻鎷烽敓鐙??
        if (mSkipManager != null) {
            mSkipManager.stopOutroCheck();
        }
        // 閿熸枻鎷烽敓鏂ゆ嫹 GSY 閿熸枻鎷烽敓鏂ゆ嫹鍋滈敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺粨瑙??onVideoPause閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃跺埡顒婃嫹閿?
        onVideoPause();
            }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void resume() {
                // 閿熸枻鎷烽敓鏂ゆ嫹 GSY 閿熶茎鎭㈤潻鎷烽敓鏂ゆ嫹閿熻剼鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺粨瑙??onVideoResume閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃跺埡顒婃嫹閿?
        onVideoResume();
                // 閿熸枻鎷峰閿熸枻鎷烽敓鐙??
        if (mSkipManager != null) {
            mSkipManager.startOutroCheck();
        }
    }

    /**
     * 閿熸枻鎷峰啓 GSY ??onVideoPause 閿熸枻鎷烽敓鏂ゆ嫹
     * ??GSY 閿熻妭璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰仠鏃堕敓鏂ゆ嫹閿熸枻鎷峰弻閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风‘閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸帴璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹鐘??
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻甯嫹涓敓鏂ゆ嫹閿熸枻鎷烽敓閰碉挤鍒侯剨鎷烽敓鏂ゆ嫹閿熻闈╂嫹閿熸枻鎷烽敓鑴氾綇鎷烽敓鐫潻鎷峰弻閿熸枻鎷烽敓琛楅潻鎷烽敓鏂ゆ嫹閿熻剼纰夋嫹閿熸枻鎷烽敓瑙ｏ級
     */
    /**
     * 閲嶅啓 onSurfaceDestroyed 鏂规硶
     * 鍦ㄧ敾涓敾妯″紡涓嬶紝涓嶉噴鏀?Surface锛岄伩鍏嶈棰戦噸鏂版挱鏀?
     * 鍙傝€?GSY 鐨?SmartPickVideo 鍜?MediaCodecVideo 瀹炵幇
     */
    @Override
    public boolean onSurfaceDestroyed(Surface surface) {
        android.util.Log.d(TAG, "=== onSurfaceDestroyed called ===");
        android.util.Log.d(TAG, "mEnteringPiPMode: " + mEnteringPiPMode);
        android.util.Log.d(TAG, "current position: " + getCurrentPositionWhenPlaying());
        
        // 妫€鏌ユ槸鍚︽鍦ㄨ繘鍏ョ敾涓敾妯″紡鎴栧凡缁忓浜庣敾涓敾妯″紡
        if (mEnteringPiPMode) {
            // 姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮忥紝涓嶉噴鏀?Surface
            android.util.Log.d(TAG, "onSurfaceDestroyed: SKIP - entering PiP mode");
            return true;
        }
        
        // 妫€鏌ユ槸鍚﹀浜庣敾涓敾妯″紡
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            boolean isInPiP = activity != null && activity.isInPictureInPictureMode();
            android.util.Log.d(TAG, "isInPictureInPictureMode: " + isInPiP);
            if (isInPiP) {
                // 褰撳墠澶勪簬鐢讳腑鐢绘ā寮忥紝涓嶉噴鏀?Surface
                android.util.Log.d(TAG, "onSurfaceDestroyed: SKIP - in PiP mode");
                return true;
            }
        }
        
        // 姝ｅ父鎯呭喌涓嬶紝璋冪敤鐖剁被鏂规硶閲婃斁 Surface
        android.util.Log.d(TAG, "onSurfaceDestroyed: calling super");
        return super.onSurfaceDestroyed(surface);
    }

    @Override
    public void onVideoPause() {
        android.util.Log.d(TAG, "=== onVideoPause called ===");
        android.util.Log.d(TAG, "mEnteringPiPMode: " + mEnteringPiPMode);
        android.util.Log.d(TAG, "current position: " + getCurrentPositionWhenPlaying());
        
        // 妫€鏌ユ槸鍚︽鍦ㄨ繘鍏ョ敾涓敾妯″紡
        if (mEnteringPiPMode) {
            // 姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮忥紝涓嶅仛鏆傚仠鎿嶄綔
            android.util.Log.d(TAG, "onVideoPause: SKIP - entering PiP mode");
            return;
        }
        
        // 妫€鏌ユ槸鍚﹀浜庣敾涓敾妯″紡锛屽鏋滄槸鍒欎笉鏆傚仠
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            boolean isInPiP = activity != null && activity.isInPictureInPictureMode();
            android.util.Log.d(TAG, "isInPictureInPictureMode: " + isInPiP);
            if (isInPiP) {
                // 褰撳墠澶勪簬鐢讳腑鐢绘ā寮忥紝涓嶅仛鏆傚仠鎿嶄綔
                android.util.Log.d(TAG, "onVideoPause: SKIP - in PiP mode");
                return;
            }
        }
                
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熻甯嫹涓敓鏂ゆ嫹閿熸枻鎷烽敓閰碉挤鍒侯剨鎷烽敓鍓跨鎷烽敓鏂ゆ嫹閿熸枻鎷峰爲璇欓敓鏂ゆ嫹閿熷壙顐嫹閿熸枻鎷烽敓鎺ワ讣鎷锋病鎸囬敓鏂ゆ嫹閿熸枻鎷烽敓?
        if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
                        // 閿熸枻鎷烽敓鏂ゆ嫹??onVideoResume()閿熸枻鎷风洿閿熸帴纰夋嫹??super.onVideoResume() 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
            super.onVideoResume();
                        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 鐩撮敓鏂ゆ嫹閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓缁炵櫢鎷烽敓?post閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ繜纰夋嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
                        return;
        }
        
        // 鍙敓鑺傝鎷烽敓鏂ゆ嫹鐘舵€佹椂閿熸枻鎷烽敓鏂ゆ嫹??
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
                                     mCurrentPlayState == PlayerConstants.STATE_BUFFERED);
        super.onVideoPause();
                if (shouldUpdateState) {
                        // 鐩撮敓鎺ラ潻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
            mCurrentPlayState = PlayerConstants.STATE_PAUSED;
            // 鐩撮敓鏂ゆ嫹閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓缁炵櫢鎷烽敓?post閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ繜纰夋嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PAUSED);
            // 纭敓鏂ゆ嫹 GSY ??mCurrentState 閿熸枻鎷烽敓鏂ゆ嫹涓洪敓鏂ゆ嫹鍋滅姸??
            if (mCurrentState != CURRENT_STATE_PAUSE) {
                                mCurrentState = CURRENT_STATE_PAUSE;
            }
        } else {
                    }
    }

    /**
     * 閿熸枻鎷峰啓 GSY ??onVideoResume 閿熸枻鎷烽敓鏂ゆ嫹
     * ??GSY 閿熻妭璇ф嫹閿熸枻鎷烽敓鐭仮闈╂嫹閿熸枻鎷烽敓鏂ゆ嫹鏃堕敓鏂ゆ嫹閿熸枻鎷峰弻閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风‘閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸帴璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹鐘??
     */
    @Override
    public void onVideoResume() {
        // 妫€鏌ユ槸鍚﹀浜庣敾涓敾妯″紡锛屽鏋滄槸鍒欎笉闇€瑕佹仮澶嶏紙瑙嗛涓€鐩村湪鎾斁锛?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Activity activity = getActivity();
            if (activity != null && activity.isInPictureInPictureMode()) {
                // 褰撳墠澶勪簬鐢讳腑鐢绘ā寮忥紝涓嶅仛鎭㈠鎿嶄綔
                return;
            }
        }
                // 鍙敓鏂ゆ嫹閿熸枻鎷峰仠鐘舵€佹椂閿熻剼闈╂嫹閿熸枻鎷蜂负閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸埅闈╂嫹閿熸枻鎷烽敓鏂ゆ嫹
        boolean shouldUpdateState = (mCurrentPlayState == PlayerConstants.STATE_PAUSED);
        super.onVideoResume();
                if (shouldUpdateState) {
                        // 鐩撮敓鎺ラ潻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
            mCurrentPlayState = PlayerConstants.STATE_PLAYING;
            // 鐩撮敓鏂ゆ嫹閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓缁炵櫢鎷烽敓?post閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ繜纰夋嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹??
            notifyComponentsPlayStateChanged(PlayerConstants.STATE_PLAYING);
            // 纭敓鏂ゆ嫹 GSY ??mCurrentState 閿熸枻鎷烽敓鏂ゆ嫹涓洪敓鏂ゆ嫹閿熸枻鎷风姸??
            if (mCurrentState != CURRENT_STATE_PLAYING) {
                                mCurrentState = CURRENT_STATE_PLAYING;
            }
        } else {
                    }
    }

    /**
     * 璁剧疆鏄惁姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮?
     * @param entering true 琛ㄧず姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮?
     */
    public void setEnteringPiPMode(boolean entering) {
        this.mEnteringPiPMode = entering;
    }
    
    /**
     * 鑾峰彇鏄惁姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮?
     * @return true 琛ㄧず姝ｅ湪杩涘叆鐢讳腑鐢绘ā寮?
     */
    public boolean isEnteringPiPMode() {
        return mEnteringPiPMode;
    }

    /**
     * 閿熼叺鏀捐鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    @Override
    public void release() {
        // 閿熸枻鎷烽敓鑺ユ挱閿熻剼鏂ゆ嫹閿熸枻鎷?
        if (mKeepVideoPlaying) {
            savePlaybackProgress();
        }
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
        if (mSkipManager != null) {
            mSkipManager.detachVideoView();
        }
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鎸囬敓鏂ゆ嫹閿熸枻鎷烽敓??
        if (mErrorRecoveryManager != null) {
            mErrorRecoveryManager.detachVideoView();
        }
        super.release();
        setOrangePlayState(PlayerConstants.STATE_IDLE);
        GSYVideoManager.releaseAllVideos();
    }

    // ===== 閿熸枻鎷烽敓楗哄尅鎷烽敓鐙″嚖鎷烽敓鏂ゆ嫹 (Requirements: 1.7, 1.8) =====

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷峰墠閿熸枻鎷烽敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鏂ゆ嫹閿熸枻鎷峰師 API??
     * @return 閿熸枻鎷峰墠浣嶉敓鐭綇鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public long getCurrentPosition() {
        return getCurrentPositionWhenPlaying();
    }

    /**
     * 閿熸枻鎷疯浆閿熸枻鎷锋寚閿熸枻鎷蜂綅??
     * @param position 鐩敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public void seekTo(int position) {
        seekTo((long) position);
    }

    /**
     * 閿熸枻鎷疯浆閿熸枻鎷锋寚閿熸枻鎷蜂綅??
     * @param position 鐩敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public void seekTo(long position) {
                
        // 閿熸枻鎷烽敓楗虹鎷烽敓鏂ゆ嫹浣块敓鏂ゆ嫹 GSYVideoManager
        if (GSYVideoManager.instance().getPlayer() != null) {
                        GSYVideoManager.instance().getPlayer().seekTo(position);
        } else {
            // 閿熸枻鎷烽敓?GSYVideoManager 閿熸枻鎷烽敓鏂ゆ嫹閿熺煫锝忔嫹閿熸枻鎷烽敓鏂ゆ嫹浣块敓鐭潻鎷烽敓娲佹柟閿熸枻鎷?
                        setSeekOnStart(position);
        }
    }

    // ===== 閿熸枻鎷烽敓鍔尅鎷烽敓鐙″嚖鎷??(Requirements: 1.9) =====

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鑴氭唻鎷??
     * @param speed 閿熸枻鎷烽敓鏂ゆ嫹??(0.5 - 3.0)
     */
    @Override
    public void setSpeed(float speed) {
        // 閿熸枻鎷烽敓鐙℃唻鎷烽敓鍔嚖鎷??
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
        super.setSpeed(speed);
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷峰墠閿熸枻鎷??
     * @return 閿熸枻鎷峰墠閿熸枻鎷??
     */
    public static float getSpeeds() {
        return sSpeed;
    }

    /**
     * 閿熸枻鎷烽敓鐭唻鎷烽敓鍔綇鎷烽敓鏂ゆ嫹鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     * @param speed 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public static void setSpeeds(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 3.0f) speed = 3.0f;
        sSpeed = speed;
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
     */
    public static float getLongSpeeds() {
        return sLongSpeed;
    }

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹閿熸枻鎷??
     * @param speed 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
     */
    public static void setLongSpeeds(float speed) {
        sLongSpeed = speed;
    }


    // ===== 鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鐙″嚖鎷烽敓鏂ゆ嫹 =====

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹
     */
    public void startFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.enterFullscreen(activity);
        }
    }

    /**
     * 閿熷壙绛规嫹鍏ㄩ敓鏂ゆ嫹
     */
    public void stopFullScreen() {
        Activity activity = getActivity();
        if (activity != null && mFullscreenHelper != null) {
            mFullscreenHelper.exitFullscreen(activity);
        }
    }

    /**
     * 閿熻鍑ゆ嫹鍏ㄩ敓鏂ゆ嫹
     * @return true 鍏ㄩ敓鏂ゆ嫹
     */
    public boolean isFullScreen() {
        return mFullscreenHelper != null && mFullscreenHelper.isFullscreen();
    }

    /**
     * 閿熻鍑ゆ嫹灏忛敓鏂ゆ嫹妯″紡
     * @return true 灏忛敓鏂ゆ嫹
     */
    public boolean isTinyScreen() {
        return mCurrentPlayerState == PlayerConstants.PLAYER_TINY_SCREEN;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹鏃堕敓瑙掑嚖鎷烽敓鐨嗚鎷烽敓鏂ゆ嫹杞敓鏂ゆ嫹??
     * @param autoRotate true 閿熺殕璁规嫹閿熸枻鎷疯浆閿熸枻鎷烽粯閿熻緝锝忔嫹
     */
    public void setAutoRotateOnFullscreen(boolean autoRotate) {
        this.mAutoRotateOnFullscreen = autoRotate;
    }

    /**
     * 閿熻鍑ゆ嫹鍏ㄩ敓鏂ゆ嫹鏃堕敓鐨嗚鎷烽敓鏂ゆ嫹杞敓鏂ゆ嫹??
     * @return true 閿熺殕璁规嫹閿熸枻鎷疯浆
     */
    public boolean isAutoRotateOnFullscreen() {
        return mAutoRotateOnFullscreen;
    }

    // ===== 閿熸枻鎷烽敓鏂ゆ嫹閿熻妭鐚存嫹閿熷彨浼欐嫹 (Requirements: 1.11) =====

    /**
     * 閫夐敓浠婃挱鍑ゆ嫹閿熻妭鐚存嫹
     * @param engineType 閿熻妭鐚存嫹閿熸枻鎷烽敓鏂ゆ嫹 (ijk, exo, ali, default)
     */
    @SuppressWarnings("unchecked")
    public void selectPlayerFactory(String engineType) {
        if (engineType == null) {
            engineType = PlayerConstants.ENGINE_DEFAULT;
        }
        
        switch (engineType) {
            case PlayerConstants.ENGINE_IJK:
                // IJK 閿熸枻鎷烽敓鏂ゆ嫹??
                PlayerFactory.setPlayManager(IjkPlayerManager.class);
                break;
            case PlayerConstants.ENGINE_EXO:
                // ExoPlayer - 閿熸枻鎷疯閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
                try {
                    Class<?> exoClass = Class.forName("com.shuyu.gsyvideoplayer.player.Exo2PlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) exoClass);
                } catch (ClassNotFoundException e) {
                    // 閿熸枻鎷烽敓鍓跨鎷烽粯??
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_ALI:
                // 閿熸枻鎷烽敓鏂ゆ嫹閿熺嫛璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹 - 閿熸枻鎷疯閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
                try {
                    Class<?> aliClass = Class.forName("com.shuyu.gsyvideoplayer.player.AliPlayerManager");
                    PlayerFactory.setPlayManager((Class<? extends IPlayerManager>) aliClass);
                } catch (ClassNotFoundException e) {
                    // 閿熸枻鎷烽敓鍓跨鎷烽粯??
                    PlayerFactory.setPlayManager(IjkPlayerManager.class);
                }
                break;
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                // 浣块敓鏂ゆ嫹绯荤粺 MediaPlayer
                PlayerFactory.setPlayManager(SystemPlayerManager.class);
                break;
        }
    }

    // ===== 鐘舵€侀敓鏂ゆ嫹??=====

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹鐘??
     * @param playState 閿熸枻鎷烽敓鏂ゆ嫹鐘??
     */
    protected void setOrangePlayState(int playState) {
        mCurrentPlayState = playState;
        notifyPlayStateChanged(playState);
        
        // 浣块敓鏂ゆ嫹 post 纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熼樁鍒侯剨鎷烽敓鏂ゆ嫹娼為敓鏂ゆ嫹閿熸枻鎷烽敓??閿熸枻鎷烽敓鎴尅鎷烽敓鏂ゆ嫹??
        post(new Runnable() {
            @Override
            public void run() {
                // 閿熸枻鎷烽敓鏂ゆ嫹鐘舵€佹椂閿熸枻鎷风ず閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鎴鎷锋椂??
                if (playState == PlayerConstants.STATE_PLAYING) {
                    showController();
                } else if (playState == PlayerConstants.STATE_PAUSED) {
                    // 閿熸枻鎷峰仠鏃堕敓鏂ゆ嫹绀洪敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹
                    showController();
                    cancelAutoHideTimer();
                } else {
                    cancelAutoHideTimer();
                }
            }
        });
    }

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
     * @param playerState 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
     */
    protected void setOrangePlayerState(int playerState) {
        mCurrentPlayerState = playerState;
        notifyPlayerStateChanged(playerState);
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷峰墠閿熸枻鎷烽敓鏂ゆ嫹鐘??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹鐘??
     */
    public int getPlayState() {
        return mCurrentPlayState;
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷峰墠閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸??
     */
    public int getPlayerState() {
        return mCurrentPlayerState;
    }

    // ===== 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??=====

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     * @param listener 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void addOnStateChangeListener(OnStateChangeListener listener) {
        if (listener != null && !mStateChangeListeners.contains(listener)) {
            mStateChangeListeners.add(listener);
        }
    }

    /**
     * 閿熺嫛绛规嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     * @param listener 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void removeOnStateChangeListener(OnStateChangeListener listener) {
        mStateChangeListeners.remove(listener);
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿?
     */
    public void clearOnStateChangeListeners() {
        mStateChangeListeners.clear();
    }

    /**
     * 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹??
     */
    private void notifyPlayStateChanged(int playState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayStateChanged(playState);
            }
        }
        // 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        if (mUseOrangeComponents) {
            notifyComponentsPlayStateChanged(playState);
        }
    }

    /**
     * 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹??
     */
    private void notifyPlayerStateChanged(int playerState) {
        if (mStateChangeListeners != null) {
            for (OnStateChangeListener listener : mStateChangeListeners) {
                listener.onPlayerStateChanged(playerState);
            }
        }
        // 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        if (mUseOrangeComponents) {
            notifyComponentsPlayerStateChanged(playerState);
        }
    }

    /**
     * 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃跺埡顒婃嫹閿??
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
     * 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熼樁鍒侯剨鎷烽敓??
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
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
     * Requirements: 3.2, 3.3, 6.4
     * 
     * 閿熻剼浼欐嫹璇撮敓鏂ゆ嫹??
     * - 閿熸枻鎷烽敓鎺ュ尅鎷锋寚閿熸枻鎷烽敓浠嬶紝纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹娈夐敓缁炵》鎷烽敓?
     * - 纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺绋嬮潻鎷烽敓鏂ゆ嫹 UI
     * - 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓杞胯揪鎷烽敓缁炵》鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??
     */
    public void updateComponentsProgress(int duration, int position) {
        // 绌烘寚閽堟鏌ワ細纭繚缁勪欢宸插垵濮嬪寲
        if (mVodControlView == null && mLiveControlView == null) {
            android.util.Log.w(TAG, "updateComponentsProgress: 鎺у埗缁勪欢鏈垵濮嬪寲");
            return;
        }
        
        // 纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺绋嬮潻鎷烽敓鏂ゆ嫹 UI
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰彵韫嬮敓绲st 閿熸枻鎷烽敓鏂ゆ嫹閿熺绛规嫹鎵ч敓鏂ゆ嫹
            final int finalDuration = duration;
            final int finalPosition = position;
            post(new Runnable() {
                @Override
                public void run() {
                    updateComponentsProgressInternal(finalDuration, finalPosition);
                }
            });
        } else {
            // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓绔▼锝忔嫹鐩撮敓鏂ゆ嫹鎵ч敓鏂ゆ嫹
            updateComponentsProgressInternal(duration, position);
        }
    }
    
    /**
     * 閿熻妭璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹榫嬮敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍙辩棸閿熸枻鎷烽敓??
     */
    private void updateComponentsProgressInternal(int duration, int position) {
        // 閿熸枻鎷烽敓閾扮偣鎾敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿?
        if (mVodControlView != null) {
            try {
                mVodControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: VodControlView 閿熸枻鎷烽敓鏂ゆ嫹澶遍敓鏂ゆ嫹", e);
            }
        }
        
        // 閿熸枻鎷烽敓鏂ゆ嫹鐩撮敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        if (mLiveControlView != null) {
            try {
                mLiveControlView.setProgress(duration, position);
            } catch (Exception e) {
                android.util.Log.e(TAG, "updateComponentsProgress: LiveControlView 閿熸枻鎷烽敓鏂ゆ嫹澶遍敓鏂ゆ嫹", e);
            }
        }
    }

    /**
     * 閿熸枻鎷烽敓鐭枻鎷烽敓楗虹》鎷烽敓鏂ゆ嫹??
     * @param listener 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setOnProgressListener(OnProgressListener listener) {
        this.mProgressListener = listener;
    }

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹閿熸枻鎷锋潐閿熸枻鎷烽敓??
     * @param listener 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setOnPlayCompleteListener(OnPlayCompleteListener listener) {
        this.mPlayCompleteListener = listener;
    }


    // ===== 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??=====

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public OrangeVideoController getVideoController() {
        return mOrangeController;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹??
     * @param controller 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setVideoController(OrangeVideoController controller) {
        this.mOrangeController = controller;

        // 閫氱煡鎺у埗鍣ㄥ叧鑱旂殑鎾斁鍣ㄨ鍥撅紝浠ヤ究鍒濆鍖?VideoEventManager
        if (controller != null) {
            controller.setVideoView(this);

            // 缁戝畾 TitleView 浜嬩欢
            if (mTitleView != null) {
                mTitleView.setController(controller);
            }
            
            // 璁剧疆 VodControlView 鐨勬帶鍒跺櫒寮曠敤
            if (mVodControlView != null) {
                mVodControlView.setOrangeVideoController(controller);
            }
            
            // 纭繚浜嬩欢缁戝畾锛堝鐞嗘帶鍒跺櫒鍦ㄧ粍浠跺垱寤哄悗璁剧疆鐨勬儏鍐碉級
            ensureEventBinding();
        }
    }

    // ===== 閿熸枻鎷烽敓瑙ｅ姛閿熸枻鎷?=====

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鍥鹃敓鏂ゆ嫹??
     * Requirements: 6.2 - THE OrangevideoView SHALL 鏀敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰彇閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹鍥鹃敓鏂ゆ嫹??
     * @param enabled true 閿熸枻鎷烽敓鐭綇鎷穎alse 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void setAutoThumbnailEnabled(boolean enabled) {
        this.mAutoThumbnailEnabled = enabled;
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public boolean isAutoThumbnailEnabled() {
        return mAutoThumbnailEnabled;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹榛橀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @param thumbnail 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setDefaultThumbnail(Object thumbnail) {
        this.mDefaultThumbnail = thumbnail;
    }

    /**
     * 閿熸枻鎷峰彇榛橀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public Object getDefaultThumbnail() {
        return mDefaultThumbnail;
    }

    /**
     * 閿熷眾姝ラ敓鏂ゆ嫹鍙栭敓鏂ゆ嫹棰戦敓鏂ゆ嫹涓€甯ч敓鏂ゆ嫹涓洪敓鏂ゆ嫹閿熸枻鎷峰浘
     * Requirements: 6.2 - THE OrangevideoView SHALL 鏀敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷峰彇閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹鍥鹃敓鏂ゆ嫹??
     * @param callback 閿熸埅纰夋嫹
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
     * 閿熷眾姝ラ敓鏂ゆ嫹鍙栨寚閿熸枻鎷锋椂閿熸枻鎷烽敓鏂ゆ嫹閿熺嫛鎶?
     * @param timeUs 鏃堕敓鎴掞紙寰敓璇級
     * @param callback 閿熸埅纰夋嫹
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
     * 閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍥鹃敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??
     */
    private void autoLoadThumbnail() {
        if (!mAutoThumbnailEnabled || mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        
        getVideoFirstFrameAsync(new VideoThumbnailHelper.ThumbnailCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap bitmap) {
                mDefaultThumbnail = bitmap;
                // 閿熸枻鎷烽敓鐭嚖鎷烽敓鏂ゆ嫹
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
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻鍑ゆ嫹閿熸枻鎷锋闯銉嫹閿熻娇浼欐嫹閿?
     * Requirements: 6.3 - THE OrangevideoView SHALL 鏀敓琛楃》鎷烽敓鎴掓挱閿熸枻鎷蜂綅閿熺煫鐧告嫹閿熸枻鎷?(setKeepVideoPlaying)
     * @param keep true 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void setKeepVideoPlaying(boolean keep) {
        this.mKeepVideoPlaying = keep;
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷锋闯銉嫹閿熻娇浼欐嫹閿?
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public boolean isKeepVideoPlaying() {
        return mKeepVideoPlaying;
    }

    /**
     * 閿熸枻鎷烽敓鑺ュ綋鍓嶉敓鏂ゆ嫹閿熻剼鏂ゆ嫹閿熸枻鎷?
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
     * 閿熻闈╂嫹閿熸枻鎷烽敓鑴氭枻鎷烽敓鏂ゆ嫹
     * Requirements: 6.3
     * @return true 閿熺即鐧告嫹閿熻闈╂嫹
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
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鐗熼敓鏂ゆ嫹娌ら敓??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public long getSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return 0;
        }
        return PlaybackProgressManager.getInstance(getContext()).getProgress(mVideoUrl);
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹娆犻敓鏂ゆ嫹鏂滈敓鏂ゆ嫹閿熶茎鏂ゆ嫹??
     * @return true 閿熷彨鎲嬫嫹閿熸枻鎷锋厱閿熸枻鎷烽敓?
     */
    public boolean hasSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return false;
        }
        return PlaybackProgressManager.getInstance(getContext()).hasProgress(mVideoUrl);
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻甯嫹閿熺嫛纰夋嫹璋嬮敓鏂ゆ嫹閿熸枻鎷??
     */
    public void clearSavedProgress() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            return;
        }
        PlaybackProgressManager.getInstance(getContext()).removeProgress(mVideoUrl);
    }

    // ===== 閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鐗囧熬閿熸枻鎷烽敓鏂ゆ嫹 (Requirements: 6.4) =====

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鏃堕敓鏂ゆ嫹
     * Requirements: 6.4 - THE OrangevideoView SHALL 鏀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鐗囧熬閿熸枻鎷烽敓鏂ゆ嫹
     * @param timeMs 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public void setSkipIntroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroTime(timeMs);
        }
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @param seconds 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setSkipIntroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroSeconds(seconds);
        }
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鏃堕敓鏂ゆ嫹
     * @return 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public long getSkipIntroTime() {
        return mSkipManager != null ? mSkipManager.getSkipIntroTime() : 0;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご
     * @param enabled 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void setSkipIntroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipIntroEnabled(enabled);
        }
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public boolean isSkipIntroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipIntroEnabled();
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧熬鏃堕敓鏂ゆ嫹
     * Requirements: 6.4 - THE OrangevideoView SHALL 鏀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧ご鐗囧熬閿熸枻鎷烽敓鏂ゆ嫹
     * @param timeMs 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public void setSkipOutroTime(long timeMs) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroTime(timeMs);
        }
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧熬鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @param seconds 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setSkipOutroSeconds(int seconds) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroSeconds(seconds);
        }
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鐗囧熬鏃堕敓鏂ゆ嫹
     * @return 鏃堕敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熻锛?
     */
    public long getSkipOutroTime() {
        return mSkipManager != null ? mSkipManager.getSkipOutroTime() : 0;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧熬
     * @param enabled 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void setSkipOutroEnabled(boolean enabled) {
        if (mSkipManager != null) {
            mSkipManager.setSkipOutroEnabled(enabled);
        }
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐗囧熬
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹
     */
    public boolean isSkipOutroEnabled() {
        return mSkipManager != null && mSkipManager.isSkipOutroEnabled();
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @param listener 閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void setOnSkipListener(SkipManager.OnSkipListener listener) {
        if (mSkipManager != null) {
            mSkipManager.setOnSkipListener(listener);
        }
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public SkipManager getSkipManager() {
        return mSkipManager;
    }

    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     * @return 閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public VideoScaleManager getVideoScaleManager() {
        return mVideoScaleManager;
    }
    
    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     * @return 閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
     */
    public PlaybackStateManager getPlaybackStateManager() {
        return mPlaybackStateManager;
    }
    
    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿?
     * @return 閿熸枻鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿?
     */
    public ComponentStateManager getComponentStateManager() {
        return mComponentStateManager;
    }
    
    /**
     * 閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹鎸囬敓鏂ゆ嫹閿熸枻鎷烽敓??
     * @return 閿熸枻鎷烽敓鏂ゆ嫹鎸囬敓鏂ゆ嫹閿熸枻鎷烽敓??
     */
    public ErrorRecoveryManager getErrorRecoveryManager() {
        return mErrorRecoveryManager;
    }

    /**
     * 鍒烽敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷风ず閿熸枻鎷烽敓鏂ゆ嫹
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻妭鏀规唻鎷烽敓鏂ゆ嫹棰戦敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍒烽敓鏂ゆ嫹閿熸枻鎷风ず
     */
    public void refreshVideoShowType() {
        changeTextureViewShowType();
    }

    /**
     * 閿熻鍑ゆ嫹涓虹洿閿熸枻鎷烽敓鏂ゆ嫹??
     * @return true 鐩撮敓鏂ゆ嫹
     */
    public boolean isLiveVideo() {
        return mIsLiveVideo;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熻鍑ゆ嫹涓虹洿閿熸枻鎷烽敓鏂ゆ嫹??
     * @param isLive true 鐩撮敓鏂ゆ嫹
     */
    public void setLiveVideo(boolean isLive) {
        this.mIsLiveVideo = isLive;
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋帰
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷锋帰
     */
    public boolean isSniffing() {
        return mIsSniffing;
    }

    /**
     * 閿熸枻鎷峰閿熸枻鎷烽閿熸枻鎷??
     * Requirements: 6.1 - THE OrangevideoView SHALL 鏀敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷锋帰閿熸枻鎷烽敓鏂ゆ嫹 (startSniffing)
     */
    public void startSniffing() {
        if (mVideoUrl == null || mVideoUrl.isEmpty()) {
            debug("startSniffing: url is empty");
            return;
        }
        startSniffing(mVideoUrl, null);
    }

    /**
     * 閿熸枻鎷峰閿熸枻鎷烽閿熸枻鎷锋帰閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰ご閿熸枻鎷?
     * @param url 閿熸枻鎷烽〉閿熸枻鎷峰潃
     * @param headers 閿熺殕璁规嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰ご
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
                // 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹??
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
                // 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹??
                for (OnStateChangeListener listener : mStateChangeListeners) {
                    if (listener instanceof OnSniffingListener) {
                        ((OnSniffingListener) listener).onSniffingFinish(videoList, videoSize);
                    }
                }
            }
        });
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷锋帰
     */
    public void stopSniffing() {
        mIsSniffing = false;
        VideoSniffing.stop(true);
        setOrangePlayState(STATE_ENDSNIFFING);
    }

    /**
     * 閿熸枻鎷锋帰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public interface OnSniffingListener {
        /**
         * 閿熸枻鎷烽敓绉哥鎷烽敓鏂ゆ嫹棰戦敓鏂ゆ嫹??
         */
        void onSniffingReceived(String contentType, java.util.HashMap<String, String> headers, 
                               String title, String url);
        
        /**
         * 閿熸枻鎷锋帰閿熸枻鎷烽敓?
         */
        void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize);
    }

    // ===== 閿熸枻鎷烽敓鏂ゆ嫹妯″紡 =====

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹妯″紡
     * @param debug true 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷??
     */
    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓渚ワ吉?
     * @return true 閿熸枻鎷烽敓鏂ゆ嫹妯″紡
     */
    public boolean isDebug() {
        return mDebug;
    }

    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰織
     * @param message 閿熸枻鎷峰織閿熸枻鎷锋伅
     */
    protected void debug(Object message) {
        if (mDebug) {
                    }
    }

    // ===== 閿熸枻鎷烽敓绔嚖鎷烽敓鏂ゆ嫹 =====

    /**
     * 閿熸枻鎷峰彇 Activity
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
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鑺傝鎷烽敓鏂ゆ嫹
     * @return true 閿熸枻鎷烽敓鑺傝鎷烽敓鏂ゆ嫹
     */
    public boolean isPlaying() {
        return mCurrentPlayState == PlayerConstants.STATE_PLAYING;
    }

    /**
     * 閿熻鍑ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閫氱姸鎬侀敓鏂ゆ嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰皬閿熸枻鎷??
     * @return true 閿熸枻鎷烽€氱姸??
     */
    public boolean isInNormalState() {
        return !isFullScreen() && !isTinyScreen();
    }

    // ===== 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风ず GestureView =====
    private com.orange.playerlibrary.component.GestureView mGestureView;

    /**
     * 閿熸枻鎷峰啓閿熸枻鎷风ず閿熸枻鎷烽敓楗哄浼欐嫹閿熸枻鎷蜂娇閿熸枻鎷?GestureView 閿熸枻鎷烽敓?Dialog
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
     * 閿熸枻鎷峰啓閿熸枻鎷风ず閿熸枻鎷烽敓鏂ゆ嫹閿熺殕浼欐嫹閿熸枻鎷蜂娇閿熸枻鎷?GestureView 閿熸枻鎷烽敓?Dialog
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
     * 閿熸枻鎷峰啓閿熸枻鎷风ず閿熸枻鎷烽敓楗哄浼欐嫹閿熸枻鎷蜂娇閿熸枻鎷?GestureView 閿熸枻鎷烽敓?Dialog
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
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓楗哄浼欐嫹??
     */
    @Override
    protected void dismissBrightnessDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕浼欐嫹??
     */
    @Override
    protected void dismissVolumeDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓鎴枻鎷烽敓楗哄浼欐嫹??
     */
    @Override
    protected void dismissProgressDialog() {
        if (mGestureView != null) {
            mGestureView.onStopSlide();
        }
    }

    /**
     * 纭敓鏂ゆ嫹 GestureView 閿熺獤绛规嫹濮嬮敓鏂ゆ嫹
     */
    private void ensureGestureView() {
        if (mGestureView == null) {
            mGestureView = new com.orange.playerlibrary.component.GestureView(getContext());
            // 閿熸枻鎷烽敓鐭鎷烽敓琛楄鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
            android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
            addView(mGestureView, lp);
        }
    }

    /**
     * 閿熸枻鎷峰彇 GestureView
     */
    public com.orange.playerlibrary.component.GestureView getGestureView() {
        ensureGestureView();
        return mGestureView;
    }

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??API??
     * @param state 鐘??
     */
    public void setThisPlayState(int state) {
        setOrangePlayState(state);
    }

    /**
     * 閿熸枻鎷烽敓鐭鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??API??
     * @param state 鐘??
     */
    public void setThisPlayerState(int state) {
        setOrangePlayerState(state);
    }

    // ===== GSYBaseVideoPlayer 閿熸枻鎷烽敓瑗熸柟鍑ゆ嫹瀹為敓鏂ゆ嫹 =====

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
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓鎴府鎷烽挳閿熸枻鎷峰彇閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??null ??GSY 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸埅甯嫹??
     * 閿熸枻鎷烽敓鎴府鎷烽挳??TitleView 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
     */
    @Override
    public android.widget.ImageView getBackButton() {
        return null;
    }

    /**
     * 閿熸枻鎷峰彇鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸帴璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹閿熻姤鏈敓鏂ゆ嫹
     * @return OrangevideoView 閿熸枻鎷烽敓鐭紮鎷烽敓鏂ゆ嫹铓嶇シ涔滈敓?
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
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鐭潻鎷烽敓鏂ゆ嫹??getFullWindowPlayer 閿熸枻鎷烽敓鏂ゆ嫹 ClassCastException
     * 閿熸枻鎷蜂负 OrangevideoView 閿熸暀绛规嫹??GSYBaseVideoPlayer 閿熸枻鎷烽敓鏂ゆ嫹??GSYVideoPlayer
     */
    @Override
    protected void checkoutState() {
        removeCallbacks(mOrangeCheckoutTask);
        mInnerHandler.postDelayed(mOrangeCheckoutTask, 500);
    }

    /**
     * 閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷烽敓??
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
     * 閿熸枻鎷峰啓閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风‘閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风‘閿熸枻鎷峰閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熼樁??
     */
    @Override
    @SuppressWarnings({"ResourceType", "unchecked"})
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
                
        // 寮洪敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熼叺纰夋嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺殕杈炬嫹閿熸枻鎷风墴閿熸枻鎷烽敓??
        hideStatusBarAndNavigation(context);
        
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷疯繙閿熸枻鎷烽敓闃额亷鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃额亷鎷烽敓渚ヤ紮鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        if (mAutoRotateOnFullscreen) {
            Activity activity = getActivity();
            if (activity != null) {
                                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        
        // 閿熸枻鎷烽敓鐭潻鎷烽敓娲佹柟閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷?true 閿熺煫闈╂嫹閿熸枻鎷蜂篃閿熸枻鎷烽敓鏂ゆ嫹??
        GSYBaseVideoPlayer fullPlayer = super.startWindowFullscreen(context, true, true);
        debugLog("閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹: " + (fullPlayer != null ? fullPlayer.getClass().getSimpleName() : "null"));
        
        // 閿熸枻鎷烽敓??OrangevideoView閿熸枻鎷峰悓閿熸枻鎷风姸??
        if (fullPlayer instanceof OrangevideoView) {
            final OrangevideoView orangeFullPlayer = (OrangevideoView) fullPlayer;
                        
            // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷峰織
            orangeFullPlayer.mIfCurrentIsFullscreen = true;
            
            // 閿熸帴绛规嫹鍚岄敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风強閿熸枻鎷烽敓鏂ゆ嫹閿熺粸鎾呮嫹閿熸枻鎷烽敓??
            orangeFullPlayer.postDelayed(new Runnable() {
                @Override
                public void run() {
                                        
                    // 鍚岄敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
                    if (mTitleView != null && orangeFullPlayer.mTitleView != null) {
                        String title = mTitleView.getTitle();
                        orangeFullPlayer.mTitleView.setTitle(title);
                                                
                        // 閿熸枻鎷峰叏閿熸枻鎷?TitleView bindController
                        if (mOrangeController != null) {
                            orangeFullPlayer.mTitleView.setController(mOrangeController);
                        }
                    }
                    
                    // 缂佹垵鐣鹃崗銊ョ潌閹绢厽鏂侀崳銊ф畱 VodControlView 閸?VideoEventManager
                    if (mOrangeController != null && orangeFullPlayer.mVodControlView != null) {
                        com.orange.playerlibrary.VideoEventManager eventManager = 
                                mOrangeController.getVideoEventManager();
                        if (eventManager != null) {
                            eventManager.bindControllerComponents(orangeFullPlayer.mVodControlView);
                        }
                    }
                    
                    // 鍚岄敓鏂ゆ嫹閿熸枻鎷峰墠閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
                    orangeFullPlayer.setOrangePlayState(mCurrentPlayState);
                    orangeFullPlayer.setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
                                        
                    // 閿熸枻鎷烽敓鏂ゆ嫹娉ㄩ敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓渚ユ枻鎷烽敓楗虹》鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹纭敓鏂ゆ嫹閿熸枻鎷烽敓楗洪潻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
                    if (orangeFullPlayer.mComponentStateManager != null) {
                        orangeFullPlayer.mComponentStateManager.reregisterProgressListener(orangeFullPlayer);
                    }
                    
                    // 鍏ㄩ敓鏂ゆ嫹鏃堕敓鏂ゆ嫹绀洪敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
                    orangeFullPlayer.showController();
                    // 寮洪敓鏂ゆ嫹閿熸枻鎷风ず TitleView
                                        if (orangeFullPlayer.mTitleView != null) {
                        orangeFullPlayer.mTitleView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mTitleView.bringToFront();
                        debugLog("寮洪敓鏂ゆ嫹閿熸枻鎷风ず TitleView, visibility=" + orangeFullPlayer.mTitleView.getVisibility());
                    }
                    // 寮洪敓鏂ゆ嫹閫氱煡 VodControlView 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熸枻鎷风ず閿熸枻鎷峰箷閿熸枻鎷烽敓鏂ゆ嫹
                    if (orangeFullPlayer.mVodControlView != null) {
                        orangeFullPlayer.mVodControlView.setVisibility(android.view.View.VISIBLE);
                        orangeFullPlayer.mVodControlView.bringToFront();
                        orangeFullPlayer.mVodControlView.onPlayerStateChanged(PlayerConstants.PLAYER_FULL_SCREEN);
                        debugLog("閫氱煡 VodControlView 鍏ㄩ敓鏂ゆ嫹鐘?? visibility=" + orangeFullPlayer.mVodControlView.getVisibility());
                    }
                    orangeFullPlayer.requestLayout();
                    
                                    }
            }, 300);
        } else {
            debugLog("鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??OrangevideoView: " + (fullPlayer != null ? fullPlayer.getClass().getName() : "null"));
        }
        
        // 閫氱煡閿熸枻鎷峰墠閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷峰叏閿熸枻鎷风姸??
        setOrangePlayerState(PlayerConstants.PLAYER_FULL_SCREEN);
        
        return fullPlayer;
    }
    
    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹閿熼叺纰夋嫹閿熸枻鎷烽敓鏂ゆ嫹
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
            
            // 閿熸枻鎷烽敓鏂ゆ嫹 ActionBar
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
     * 閿熸枻鎷峰啓閿熷壙绛规嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷?ClassCastException
     */
    @Override
    @SuppressWarnings("ResourceType")
    protected void clearFullscreenLayout() {
                
        if (!mFullAnimEnd) {
                        return;
        }
        
        // 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘??
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        if (oldF != null && oldF instanceof OrangevideoView) {
            OrangevideoView orangeVideoPlayer = (OrangevideoView) oldF;
                        
            // 浣块敓鏂ゆ嫹 PlaybackStateManager 閿熸枻鎷烽敓鏂ゆ嫹鐘??
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
     * 閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎴鎷烽敓鏂ゆ嫹閿熸枻鎷锋晥??
     */
    @SuppressWarnings("ResourceType")
    protected void orangeBackToNormal() {
        final android.view.ViewGroup vp = getViewGroup();
        final android.view.View oldF = vp.findViewById(getFullId());
        final OrangevideoView orangeVideoPlayer;
        
        if (oldF != null && oldF instanceof OrangevideoView) {
            orangeVideoPlayer = (OrangevideoView) oldF;
            // 閿熸枻鎷烽敓鏂ゆ嫹閿熼叺??- 閿熸枻鎷烽敓鏂ゆ嫹 pauseFullBackCoverLogic閿熸枻鎷烽敓鏂ゆ嫹涓洪敓鏂ゆ嫹閿熸枻鎷??GSYVideoPlayer 閿熸枻鎷烽敓鏂ゆ嫹
            // 閿熸枻鎷烽敓鎺ョ┖纭锋嫹妞嬵剨鎷烽敓鏂ゆ嫹閿?NPE
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
     * 閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓琛楅潻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??
     */
    protected void orangeResolveNormalVideoShow(android.view.View oldF, android.view.ViewGroup vp, OrangevideoView orangeVideoPlayer) {
                
        // 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓渚ヨ鎷烽敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鎴》鎷烽敓鏂ゆ嫹??cloneParams 涔嬪墠閿熸枻鎷烽敓鏂ゆ嫹??
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
        
        // 閿熸埅纭锋嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??TextureView閿熸枻鎷风‘??Surface 閿熸枻鎷风‘閿熻闈╂嫹
                addTextureView();
        
        // 閿熸帴杩熸仮闈╂嫹閿熸枻鎷烽敓鏂ゆ嫹浣嶉敓鐭綇鎷风‘??Surface 閿熺獤鎾呮嫹鍑嗛敓鏂ゆ嫹??
        postDelayed(new Runnable() {
            @Override
            public void run() {
                                
                // 閿熻闈╂嫹閿熸枻鎷烽敓鏂ゆ嫹浣嶉敓鐭綇鎷烽敓鎴》鎷烽敓鐫潻鎷烽敓鏂ゆ嫹
                if (savedPosition > 0) {
                                        seekTo(savedPosition);
                    
                    // 閿熸枻鎷烽敓琛楊啚甯嫹璇撻敓鏂ゆ嫹鐗涢敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
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
                
                // 閿熻闈╂嫹閿熸枻鎷烽敓闃??
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                                    }
                
                // 閫氱煡閿熸枻鎷烽敓闃跺埡顒婃嫹閿??
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
        // 閫氱煡閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃跺埡顒婃嫹閿??
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
        // 閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹??
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
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹璐敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熺粸?
        if (mPrepareView != null) {
            setOrangePlayState(8); // 閿熺嫛璁规嫹閿熸枻鎷烽敓鐣岃閿熸枻鎷风姸??
        }
    }

    // UI 鐘舵€侀敓鎴掑寲閿熸枻鎷??- 閿熸枻鎷峰疄閿熻锝忔嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??
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
    
    // 閿熺殕璁规嫹閿熸枻鎷烽敓鎴尅鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓渚ヨ鎷锋椂??
    private static final int AUTO_HIDE_DELAY = 4000; // 4閿熸枻鎷烽敓鏂ゆ嫹杩滈敓鏂ゆ嫹閿熸枻鎷烽敓?
    private Runnable mAutoHideRunnable;

    @Override
    protected void onClickUiToggle(android.view.MotionEvent e) {
        // 鍙敓鑺傝鎷烽敓鑴氫紮鎷烽敓鏂ゆ嫹鍋滅姸鎬佹椂閿熸枻鎷烽敓鍙紮鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风ず/閿熸枻鎷烽敓鏂ゆ嫹
        if (mCurrentPlayState != PlayerConstants.STATE_PLAYING && 
            mCurrentPlayState != PlayerConstants.STATE_PAUSED &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERING &&
            mCurrentPlayState != PlayerConstants.STATE_BUFFERED) {
            return;
        }
        
        // 閿熸枻鎷烽敓鏂ゆ嫹璋㈤敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓??閿熸枻鎷烽敓鏂ゆ嫹
        if (isControllerShowing()) {
            hideController();
        } else {
            showController();
        }
    }
    
    /**
     * 閿熸枻鎷风ず閿熸枻鎷烽敓鏂ゆ嫹??
     */
    public void showController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.VISIBLE);
        }
        // 鍏ㄩ敓鏂ゆ嫹鏃堕敓鏂ゆ嫹绀洪敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹
        if (mTitleView != null && (mIfCurrentIsFullscreen || mCurrentPlayerState == PlayerConstants.PLAYER_FULL_SCREEN)) {
            mTitleView.setVisibility(android.view.View.VISIBLE);
        }
        // 閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鎴鎷锋椂??
        startAutoHideTimer();
    }
    
    /**
     * 閿熸枻鎷烽敓鎴尅鎷烽敓鏂ゆ嫹??
     */
    public void hideController() {
        if (mVodControlView != null) {
            mVodControlView.setVisibility(android.view.View.GONE);
        }
        if (mTitleView != null) {
            mTitleView.setVisibility(android.view.View.GONE);
        }
        // 鍙栭敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鎴鎷锋椂??
        cancelAutoHideTimer();
    }
    
    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓瑙掑嚖鎷烽敓鏂ゆ嫹??
     */
    public boolean isControllerShowing() {
        return mVodControlView != null && mVodControlView.getVisibility() == android.view.View.VISIBLE;
    }
    
    /**
     * 閿熸枻鎷峰彇閿熸触鍒涙枻鎷烽敓鐨嗚鎷烽敓鏂ゆ嫹??Runnable
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
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鎴鎷锋椂??
     */
    private void startAutoHideTimer() {
        cancelAutoHideTimer();
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING && mInnerHandler != null) {
            mInnerHandler.postDelayed(getAutoHideRunnable(), AUTO_HIDE_DELAY);
        }
    }
    
    /**
     * 鍙栭敓鏂ゆ嫹閿熺殕璁规嫹閿熸枻鎷烽敓鎴鎷锋椂??
     */
    private void cancelAutoHideTimer() {
        if (mInnerHandler != null && mAutoHideRunnable != null) {
            mInnerHandler.removeCallbacks(mAutoHideRunnable);
        }
    }

    /**
     * 閿熸枻鎷峰啓鍙岄敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鍏ㄤ娇閿熸枻鎷烽敓鏂ゆ嫹閿熸帴璇ф嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鍙柇锝忔嫹閿熸枻鎷烽敓鏂ゆ嫹??GSY ??mCurrentState
     */
    protected void touchDoubleUp() {
                // 鍙岄敓鏂ゆ嫹閿熸枻鎷峰仠/閿熸枻鎷烽敓鏂ゆ嫹 - 閿熸枻鎷峰叏浣块敓鏂ゆ嫹閿熸枻鎷烽敓鎺ヨ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹鐘舵€侀敓鏂ゆ嫹??
        if (mCurrentPlayState == PlayerConstants.STATE_PLAYING || 
            mCurrentPlayState == PlayerConstants.STATE_BUFFERING ||
            mCurrentPlayState == PlayerConstants.STATE_BUFFERED) {
                        // 鐩撮敓鎺ョ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??pause() 閿熸枻鎷烽敓鏂ゆ嫹
            pause();
        } else if (mCurrentPlayState == PlayerConstants.STATE_PAUSED) {
                        // 鐩撮敓鎺ョ鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹??resume() 閿熸枻鎷烽敓鏂ゆ嫹
            resume();
        } else {
                    }
    }

    @Override
    public void startPlayLogic() {
        prepareVideo();
    }

    /**
     * 閿熸枻鎷峰啓 startAfterPrepared閿熸枻鎷风‘??TextureView 閿熸枻鎷风‘閿熸枻鎷烽敓鏂ゆ嫹
     */
    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
    }

    // ===== 閿熸枻鎷烽敓鐭敼鍙樺閿熸枻鎷?(Requirements: 2.3, 2.4, 5.1, 5.2) =====
    
    /**
     * 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鐭敼鍙橈紙閿熸枻鎷烽敓鏂ゆ嫹骞曢敓鏂ゆ嫹杞??
     * 閿熸枻鎷烽敓鑺ュ綋鍓嶉敓鏂ゆ嫹閿熸枻鎷风姸鎬侀敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熼樁鍒侯剨鎷烽敓??
     * Requirements: 2.1, 2.2, 5.3, 5.4, 5.5
     * 
     * @param newConfig 閿熼摪纰夋嫹閿熸枻鎷烽敓鏂ゆ嫹
     */
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                
        // 浣块敓鏂ゆ嫹 PlaybackStateManager 閿熸枻鎷烽敓鑺ュ綋鍓嶇姸??
        if (mPlaybackStateManager != null) {
            mPlaybackStateManager.saveState(this);
        }
        
        // 浣块敓鏂ゆ嫹 ComponentStateManager 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓闃??
        if (mComponentStateManager != null) {
            mComponentStateManager.saveComponentState(
                (int) getDuration(), 
                (int) getCurrentPositionWhenPlaying()
            );
        }
        
        // 閿熸枻鎷烽敓鏂ゆ嫹鍏ㄩ敓鏂ゆ嫹/閿熸枻鎷烽敓鏂ゆ嫹閿熷彨浼欐嫹
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 閿熸枻鎷烽敓鏂ゆ嫹 - 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓楗侯偓鎷烽敓闃跺埡顒婃嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎻亷鎷烽敓鏂ゆ嫹閿熼ズ??
                    } else if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            // 閿熸枻鎷烽敓鏂ゆ嫹 - 閿熸枻鎷烽敓鏂ゆ嫹閿熼ズ顐嫹閿熼樁鍒侯剨鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸彮顏庢嫹椤洪敓楗??
                    }
        
        // 閿熸帴杩熸仮闈╂嫹鐘舵€侀敓鏂ゆ嫹纭敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓?
        postDelayed(new Runnable() {
            @Override
            public void run() {
                                
                // 浣块敓鏂ゆ嫹 PlaybackStateManager 閿熻闈╂嫹鐘??
                if (mPlaybackStateManager != null) {
                    mPlaybackStateManager.restoreState(OrangevideoView.this);
                }
                
                // 浣块敓鏂ゆ嫹 ComponentStateManager 閿熻闈╂嫹閿熸枻鎷烽敓闃??
                if (mComponentStateManager != null) {
                    mComponentStateManager.restoreComponentState(OrangevideoView.this);
                    // 閿熸枻鎷烽敓鏂ゆ嫹娉ㄩ敓鏂ゆ嫹閿熸枻鎷风噧閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓楗哄嚖鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鎴枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽泙閿熸枻鎷烽敓鏂ゆ嫹閿??
                    mComponentStateManager.reregisterProgressListener(OrangevideoView.this);
                                    }
                
                // 閿熸枻鎷烽敓鏂ゆ嫹搴旈敓鏂ゆ嫹閿熸枻鎷烽閿熸枻鎷烽敓鏂ゆ嫹
                if (mVideoScaleManager != null) {
                    mVideoScaleManager.applyVideoScale();
                }
                
                // 閫氱煡閿熸枻鎷烽敓闃跺埡顒婃嫹閿??
                notifyComponentsPlayStateChanged(mCurrentPlayState);
                notifyComponentsPlayerStateChanged(mCurrentPlayerState);
                
                            }
        }, 100);
    }

    /**
     * 閿熸枻鎷峰啓 getLayoutParams 閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷风‘閿熸枻鎷峰叏閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹浣块敓鏂ゆ嫹閿熸枻鎷风‘閿熶茎璇ф嫹閿熻璇ф嫹閿熸枻鎷?
     * 閿熸枻鎷烽敓瑙掓枻鎷烽敓楗侯偓鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓渚ョ櫢鎷??
     */
    @Override
    public android.view.ViewGroup.LayoutParams getLayoutParams() {
        android.view.ViewGroup.LayoutParams params = super.getLayoutParams();
        if (params == null) {
            // 閿熸枻鎷烽敓鐭紮鎷疯儊閿熸枻鎷锋閿熸枻鎷烽敓鏂ゆ嫹閿熸枻鎷烽敓鏂ゆ嫹閿熸彮??MATCH_PARENT 閿熶茎璇ф嫹??
            params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
        }
        return params;
    }
}
