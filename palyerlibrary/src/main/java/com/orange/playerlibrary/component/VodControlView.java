package com.orange.playerlibrary.component;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

/**
 * 视频点播控制视图
 * 显示播放/暂停按钮、进度条、时间、全屏按钮、弹幕控制区
 */
public class VodControlView extends FrameLayout implements IControlComponent,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VodControlView";

    // 双击防抖：记录上次状态变化时间，避免双击后的单击事件干扰
    private long mLastStateChangeTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 500;

    // 图标更新防抖
    private long mLastIconUpdateTime = 0;

    // 长按倍速相关
    private float mLongPressSpeed = 3.0f;
    private float mNormalSpeed = 1.0f;
    private boolean mIsLongPressing = false;
    
    // 点击事件监听器
    private View.OnClickListener mOnSpeedControlClickListener;
    private View.OnClickListener mOnEpisodeSelectClickListener;
    private View.OnClickListener mOnSetupClickListener;
    private View.OnClickListener mOnDanmuToggleClickListener;
    private View.OnClickListener mOnDanmuSetClickListener;
    private View.OnClickListener mOnDanmuInputClickListener;
    private View.OnClickListener mOnSkipOpeningClickListener;
    private View.OnClickListener mOnSkipEndingClickListener;
    private View.OnClickListener mOnPlayNextClickListener;

    // 控制包装器
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    private static OrangeVideoController sSharedController;

    // UI 组件
    private LinearLayout mBottomContainer;
    private LinearLayout mTopContainer;
    private LinearLayout mDanmuContainer;
    private ImageView mPlayButton;
    private ImageView mFullScreen;
    private SeekBar mVideoProgress;
    private TextView mCurrTime;
    private TextView mTotalTime;
    private ProgressBar mBottomProgress;

    // 弹幕相关
    private ImageView mDanmuToggle;  // 保留用于兼容性
    private ImageView mDanmuToggleOn;  // 开启状态的图标
    private ImageView mDanmuToggleOff; // 关闭状态的图标
    private ImageView mDanmuSet;
    private EditText mDanmuInput;

    // 功能按钮
    private TextView mSpeedControl;
    private TextView mEpisodeSelect;
    private TextView mSkipButton;
    private TextView mSourceSelect;

    // 全屏时弹幕区的播放和全屏按钮
    private ImageView mPlayButtonFullscreen;
    private ImageView mFullScreenDanmu;
    
    // 下一集按钮
    private ImageView mPlayNext;
    
    // 竖屏全屏按钮
    private ImageView mShup;

    // 状态
    private boolean mIsDragging = false;
    private boolean mIsShowBottomProgress = true;
    private static boolean sShowBottomProgress = true;

    public VodControlView(@NonNull Context context) {
        super(context);
        init();
    }

    public VodControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VodControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
        setClickable(false);
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_vod_control_view, this, true);
        
        android.util.Log.d(TAG, "VodControlView init: this=" + this + ", hashCode=" + this.hashCode());

        mBottomContainer = findViewById(R.id.bottom_container);
        mTopContainer = findViewById(R.id.container_main);
        mDanmuContainer = findViewById(R.id.danmu_container);

        mPlayButton = findViewById(R.id.iv_play);
        if (mPlayButton != null) {
            mPlayButton.setOnClickListener(this);
            setupLongPressSpeed(mPlayButton);
        }

        mFullScreen = findViewById(R.id.fullscreen);
        if (mFullScreen != null) {
            mFullScreen.setOnClickListener(this);
        }

        mVideoProgress = findViewById(R.id.seekBar);
        if (mVideoProgress != null) {
            mVideoProgress.setOnSeekBarChangeListener(this);
        }

        mCurrTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);
        mBottomProgress = findViewById(R.id.bottom_progress);

        mDanmuToggleOn = findViewById(R.id.danmu_toggle_on);
        mDanmuToggleOff = findViewById(R.id.danmu_toggle_off);
        mDanmuToggle = mDanmuToggleOff; // 默认指向关闭状态，保持兼容性
        mDanmuSet = findViewById(R.id.danmu_set);
        mDanmuInput = findViewById(R.id.danmu_input);
        
        // 设置弹幕按钮点击事件 - 两个ImageView都要设置
        if (mDanmuToggleOn != null) {
            mDanmuToggleOn.setOnClickListener(this);
        }
        if (mDanmuToggleOff != null) {
            mDanmuToggleOff.setOnClickListener(this);
        }
        if (mDanmuSet != null) {
            mDanmuSet.setOnClickListener(this);
        }
        // 设置弹幕输入框点击事件
        if (mDanmuInput != null) {
            mDanmuInput.setOnClickListener(this);
        }

        mSpeedControl = findViewById(R.id.speed_control);
        mEpisodeSelect = findViewById(R.id.episode_select);
        mSkipButton = findViewById(R.id.film_header_footer);
        mSourceSelect = findViewById(R.id.source_select);
        
        // 设置跳过片头片尾按钮点击事件
        if (mSkipButton != null) {
            mSkipButton.setOnClickListener(this);
        }

        mPlayButtonFullscreen = findViewById(R.id.iv_play_fullscreen);
        mFullScreenDanmu = findViewById(R.id.fullscreen_danmu);
        mPlayNext = findViewById(R.id.playnext);
        if (mPlayNext != null) {
            mPlayNext.setOnClickListener(this);
        }
        
        mShup = findViewById(R.id.shup);
        if (mShup != null) {
            mShup.setOnClickListener(this);
        }

        if (mSpeedControl != null) {
            mSpeedControl.setOnClickListener(this);
        }
        if (mEpisodeSelect != null) {
            mEpisodeSelect.setVisibility(GONE);
            mEpisodeSelect.setOnClickListener(this);
        }
        if (mPlayButtonFullscreen != null) {
            mPlayButtonFullscreen.setOnClickListener(this);
            setupLongPressSpeed(mPlayButtonFullscreen);
        }
        if (mFullScreenDanmu != null) {
            mFullScreenDanmu.setOnClickListener(this);
        }
    }

    public void setOrangeVideoController(OrangeVideoController controller) {
        mOrangeController = controller;
        sSharedController = controller;
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
        
        if (mOrangeController == null && sSharedController != null) {
            mOrangeController = sSharedController;
        }
        
        if (mOrangeController != null) {
            com.orange.playerlibrary.VideoEventManager eventManager = 
                    mOrangeController.getVideoEventManager();
            if (eventManager != null) {
                eventManager.bindControllerComponents(this);
            }
        }
        
        // 初始化弹幕按钮状态
        initDanmakuButtonState();
    }
    
    /**
     * 初始化弹幕按钮状态
     */
    private void initDanmakuButtonState() {
        if (mDanmuToggleOn != null && mDanmuToggleOff != null && getContext() != null) {
            com.orange.playerlibrary.PlayerSettingsManager settingsManager = 
                com.orange.playerlibrary.PlayerSettingsManager.getInstance(getContext());
            boolean enabled = settingsManager.isDanmakuEnabled();
            
            android.util.Log.d(TAG, "initDanmakuButtonState: enabled=" + enabled);
            
            // 设置初始visibility
            if (enabled) {
                mDanmuToggleOn.setVisibility(VISIBLE);
                mDanmuToggleOff.setVisibility(GONE);
            } else {
                mDanmuToggleOn.setVisibility(GONE);
                mDanmuToggleOff.setVisibility(VISIBLE);
            }
            
            // 同时控制输入框和设置按钮的可见性
            if (mDanmuInput != null) {
                mDanmuInput.setVisibility(enabled ? VISIBLE : INVISIBLE);
            }
            if (mDanmuSet != null) {
                mDanmuSet.setVisibility(enabled ? VISIBLE : GONE);
            }
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        
        android.util.Log.d(TAG, "onClick: view id=" + id);
        
        if (id == R.id.fullscreen || id == R.id.fullscreen_danmu) {
            toggleFullScreen();
        } else if (id == R.id.iv_play || id == R.id.iv_play_fullscreen) {
            long currentTime = System.currentTimeMillis();
            
            // 检查是否在双击阻止时间内
            long timeSinceDoubleClick = currentTime - OrangevideoView.getLastDoubleClickTime();
            if (timeSinceDoubleClick < OrangevideoView.getDoubleClickBlockInterval()) {
                android.util.Log.d(TAG, "onClick iv_play: BLOCKED by double-click protection, timeSinceDoubleClick=" + timeSinceDoubleClick + "ms");
                return;
            }
            
            long timeSinceLastChange = currentTime - mLastStateChangeTime;
            android.util.Log.d(TAG, "onClick iv_play: timeSinceLastChange=" + timeSinceLastChange + "ms, DOUBLE_CLICK_INTERVAL=" + DOUBLE_CLICK_INTERVAL);
            if (timeSinceLastChange < DOUBLE_CLICK_INTERVAL) {
                android.util.Log.d(TAG, "onClick iv_play: BLOCKED by debounce");
                return;
            }
            android.util.Log.d(TAG, "onClick iv_play: calling togglePlay()");
            if (mControlWrapper != null) {
                mControlWrapper.togglePlay();
            }
        } else if (id == R.id.speed_control) {
            if (mOnSpeedControlClickListener != null) {
                mOnSpeedControlClickListener.onClick(v);
            }
        } else if (id == R.id.episode_select) {
            if (mOnEpisodeSelectClickListener != null) {
                mOnEpisodeSelectClickListener.onClick(v);
            }
        } else if (id == R.id.shup) {
            toggleFullScreen();
        } else if (id == R.id.danmu_toggle_on || id == R.id.danmu_toggle_off) {
            if (mOnDanmuToggleClickListener != null) {
                mOnDanmuToggleClickListener.onClick(v);
            }
        } else if (id == R.id.danmu_set) {
            if (mOnDanmuSetClickListener != null) {
                mOnDanmuSetClickListener.onClick(v);
            }
        } else if (id == R.id.danmu_input) {
            if (mOnDanmuInputClickListener != null) {
                mOnDanmuInputClickListener.onClick(v);
            }
        } else if (id == R.id.film_header_footer) {
            if (mOnSkipOpeningClickListener != null) {
                mOnSkipOpeningClickListener.onClick(v);
            }
        } else if (id == R.id.playnext) {
            if (mOnPlayNextClickListener != null) {
                mOnPlayNextClickListener.onClick(v);
            }
        }
    }

    private void toggleFullScreen() {
        if (mControlWrapper != null) {
            mControlWrapper.toggleFullScreen();
        }
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        if (isVisible) {
            if (mBottomContainer != null) {
                mBottomContainer.setVisibility(VISIBLE);
                if (anim != null) {
                    mBottomContainer.startAnimation(anim);
                }
            }
            if (mIsShowBottomProgress && mBottomProgress != null) {
                mBottomProgress.setVisibility(GONE);
            }
        } else {
            if (mBottomContainer != null) {
                mBottomContainer.setVisibility(GONE);
                if (anim != null) {
                    mBottomContainer.startAnimation(anim);
                }
            }
            if (sShowBottomProgress && mIsShowBottomProgress && mBottomProgress != null) {
                mBottomProgress.setVisibility(VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(300L);
                mBottomProgress.startAnimation(fadeIn);
            }
        }
    }

    @Override
    public void onPlayStateChanged(int playState) {
        // 如果当前实例没有附加到窗口，跳过UI更新（全屏模式下旧实例会收到回调但不应该更新）
        if (!isAttachedToWindow()) {
            android.util.Log.d(TAG, "onPlayStateChanged: skipping, not attached to window. playState=" + playState);
            return;
        }
        
        mLastStateChangeTime = System.currentTimeMillis();
        android.util.Log.d(TAG, "onPlayStateChanged: " + playState);

        switch (playState) {
            case PlayerConstants.STATE_ERROR:
            case PlayerConstants.STATE_PREPARING:
            case PlayerConstants.STATE_PREPARED:
            case 8:
                setVisibility(GONE);
                break;

            case PlayerConstants.STATE_IDLE:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                resetProgress();
                break;

            case PlayerConstants.STATE_PLAYING:
                setVisibility(VISIBLE);
                updatePlayButtonState(true);
                updateBottomProgressVisibility();
                break;

            case PlayerConstants.STATE_PAUSED:
                setVisibility(VISIBLE);
                updatePlayButtonState(false);
                break;

            case PlayerConstants.STATE_BUFFERING:
            case PlayerConstants.STATE_BUFFERED:
                setVisibility(VISIBLE);
                if (mControlWrapper != null) {
                    updatePlayButtonState(mControlWrapper.isPlaying());
                }
                break;
        }
    }

    private void updatePlayButtonState(final boolean isPlaying) {
        mLastIconUpdateTime = System.currentTimeMillis();
        android.util.Log.d(TAG, "updatePlayButtonState: isPlaying=" + isPlaying + ", this=" + this.hashCode() + ", isAttached=" + isAttachedToWindow());

        if (mPlayButton != null) {
            android.util.Log.d(TAG, "updatePlayButtonState: mPlayButton before selected=" + mPlayButton.isSelected());
            mPlayButton.setSelected(isPlaying);
            mPlayButton.refreshDrawableState();
            mPlayButton.invalidate();
            android.util.Log.d(TAG, "updatePlayButtonState: mPlayButton after selected=" + mPlayButton.isSelected());
        }

        if (mPlayButtonFullscreen != null) {
            mPlayButtonFullscreen.setSelected(isPlaying);
            mPlayButtonFullscreen.refreshDrawableState();
            mPlayButtonFullscreen.invalidate();
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        // 如果当前实例没有附加到窗口，跳过UI更新
        if (!isAttachedToWindow()) {
            android.util.Log.d(TAG, "onPlayerStateChanged: skipping, not attached to window. playerState=" + playerState);
            return;
        }
        
        android.util.Log.d(TAG, "onPlayerStateChanged: " + playerState);
        if (playerState == PlayerConstants.PLAYER_FULL_SCREEN) {
            if (mDanmuContainer != null) {
                mDanmuContainer.setVisibility(VISIBLE);
                android.util.Log.d(TAG, "Set danmu_container to VISIBLE, actual visibility=" + mDanmuContainer.getVisibility());
                android.util.Log.d(TAG, "danmu_container size: " + mDanmuContainer.getWidth() + "x" + mDanmuContainer.getHeight());
            }
            if (mPlayButton != null) mPlayButton.setVisibility(GONE);
            if (mFullScreen != null) mFullScreen.setVisibility(GONE);
            if (mPlayButtonFullscreen != null) mPlayButtonFullscreen.setVisibility(VISIBLE);
            if (mFullScreenDanmu != null) {
                mFullScreenDanmu.setVisibility(VISIBLE);
                mFullScreenDanmu.setSelected(true);
            }
            if (mSkipButton != null) mSkipButton.setVisibility(VISIBLE);
            if (mEpisodeSelect != null) mEpisodeSelect.setVisibility(VISIBLE);
            if (mSpeedControl != null) mSpeedControl.setVisibility(VISIBLE);
        } else if (playerState == PlayerConstants.PLAYER_NORMAL) {
            if (mDanmuContainer != null) {
                mDanmuContainer.setVisibility(GONE);
                android.util.Log.d(TAG, "Set danmu_container to GONE");
            }
            if (mPlayButton != null) mPlayButton.setVisibility(VISIBLE);
            if (mFullScreen != null) {
                mFullScreen.setVisibility(VISIBLE);
                mFullScreen.setSelected(false);
            }
            if (mPlayButtonFullscreen != null) mPlayButtonFullscreen.setVisibility(GONE);
            if (mFullScreenDanmu != null) {
                mFullScreenDanmu.setVisibility(GONE);
                mFullScreenDanmu.setSelected(false);
            }
            if (mSkipButton != null) mSkipButton.setVisibility(GONE);
            if (mEpisodeSelect != null) mEpisodeSelect.setVisibility(GONE);
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        if (mIsDragging) return;

        if (mVideoProgress != null) {
            if (duration > 0) {
                mVideoProgress.setEnabled(true);
                int progress = (int) ((position * 1.0 / duration) * mVideoProgress.getMax());
                mVideoProgress.setProgress(progress);
                if (mBottomProgress != null) {
                    mBottomProgress.setProgress(progress);
                }
            } else {
                mVideoProgress.setEnabled(false);
            }

            if (mControlWrapper != null) {
                int buffered = mControlWrapper.getBufferedPercentage();
                if (buffered >= 95) {
                    mVideoProgress.setSecondaryProgress(mVideoProgress.getMax());
                    if (mBottomProgress != null) {
                        mBottomProgress.setSecondaryProgress(mBottomProgress.getMax());
                    }
                } else {
                    mVideoProgress.setSecondaryProgress(buffered * 10);
                    if (mBottomProgress != null) {
                        mBottomProgress.setSecondaryProgress(buffered * 10);
                    }
                }
            }
        }

        if (mTotalTime != null) mTotalTime.setText(stringForTime(duration));
        if (mCurrTime != null) mCurrTime.setText(stringForTime(position));
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        onVisibilityChanged(!isLocked, null);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && mControlWrapper != null) {
            long duration = mControlWrapper.getDuration();
            long position = duration * progress / seekBar.getMax();
            if (mCurrTime != null) {
                mCurrTime.setText(stringForTime((int) position));
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsDragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mControlWrapper != null) {
            long duration = mControlWrapper.getDuration();
            long position = duration * seekBar.getProgress() / mVideoProgress.getMax();
            mControlWrapper.seekTo(position);
        }
        mIsDragging = false;
    }

    private void resetProgress() {
        if (mVideoProgress != null) {
            mVideoProgress.setProgress(0);
            mVideoProgress.setSecondaryProgress(0);
        }
        if (mBottomProgress != null) {
            mBottomProgress.setProgress(0);
            mBottomProgress.setSecondaryProgress(0);
        }
    }

    private void updateBottomProgressVisibility() {
        if (mIsShowBottomProgress && mBottomProgress != null) {
            if (mBottomContainer != null && mBottomContainer.getVisibility() == VISIBLE) {
                mBottomProgress.setVisibility(GONE);
            } else if (sShowBottomProgress) {
                mBottomProgress.setVisibility(VISIBLE);
            }
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static void setBottomProgress(boolean show) {
        sShowBottomProgress = show;
    }

    public void showBottomProgress(boolean show) {
        mIsShowBottomProgress = show;
    }

    public boolean isFullScreen() {
        return mControlWrapper != null && mControlWrapper.isFullScreen();
    }

    public ImageView getPlayButton() { return mPlayButton; }
    public ImageView getFullScreenButton() { return mFullScreen; }
    public SeekBar getVideoProgress() { return mVideoProgress; }
    public ImageView getDanmuToggle() { return mDanmuToggle; }
    public ImageView getDanmuSet() { return mDanmuSet; }
    public TextView getSpeedControl() { return mSpeedControl; }
    public TextView getEpisodeSelect() { return mEpisodeSelect; }

    private void setupLongPressSpeed(View view) {
        if (view == null) return;
        
        view.setOnLongClickListener(v -> {
            if (mControlWrapper != null && mControlWrapper.isPlaying() && !mIsLongPressing) {
                mIsLongPressing = true;
                mNormalSpeed = mControlWrapper.getSpeed();
                mControlWrapper.setSpeed(mLongPressSpeed);
                String message = getContext().getString(R.string.orange_long_press_speed, mLongPressSpeed);
                android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                if (mIsLongPressing && mControlWrapper != null) {
                    mIsLongPressing = false;
                    mControlWrapper.setSpeed(mNormalSpeed);
                    String message = getContext().getString(R.string.orange_restore_normal_speed, mNormalSpeed);
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });
    }

    public void setLongPressSpeed(float speed) { mLongPressSpeed = speed; }
    public float getLongPressSpeed() { return mLongPressSpeed; }
    public void setOnSpeedControlClickListener(View.OnClickListener listener) { mOnSpeedControlClickListener = listener; }
    public void setOnEpisodeSelectClickListener(View.OnClickListener listener) { mOnEpisodeSelectClickListener = listener; }
    public void setOnSetupClickListener(View.OnClickListener listener) { mOnSetupClickListener = listener; }
    public void setOnDanmuToggleClickListener(View.OnClickListener listener) { mOnDanmuToggleClickListener = listener; }
    public void setOnDanmuSetClickListener(View.OnClickListener listener) { mOnDanmuSetClickListener = listener; }
    public void setOnDanmuInputClickListener(View.OnClickListener listener) { mOnDanmuInputClickListener = listener; }
    public void setOnSkipOpeningClickListener(View.OnClickListener listener) { mOnSkipOpeningClickListener = listener; }
    public void setOnSkipEndingClickListener(View.OnClickListener listener) { mOnSkipEndingClickListener = listener; }
    public void setOnPlayNextClickListener(View.OnClickListener listener) { mOnPlayNextClickListener = listener; }
    
    /**
     * 更新弹幕开关按钮状态 - 使用两个ImageView切换visibility
     */
    public void updateDanmakuToggleState(boolean enabled) {
        android.util.Log.d(TAG, "updateDanmakuToggleState called: enabled=" + enabled + ", this=" + this + ", hashCode=" + this.hashCode());
        android.util.Log.d(TAG, "updateDanmakuToggleState: this.getWidth()=" + getWidth() + ", this.getHeight()=" + getHeight());
        android.util.Log.d(TAG, "updateDanmakuToggleState: isAttachedToWindow=" + isAttachedToWindow() + ", getVisibility=" + getVisibility());
        
        // 使用post确保在布局完成后执行
        final boolean finalEnabled = enabled;
        post(() -> {
            android.util.Log.d(TAG, "Post execution: enabled=" + finalEnabled);
            android.util.Log.d(TAG, "mDanmuToggleOn=" + mDanmuToggleOn + ", mDanmuToggleOff=" + mDanmuToggleOff);
            
            // 检查父容器
            if (mDanmuContainer != null) {
                android.util.Log.d(TAG, "danmu_container visibility=" + mDanmuContainer.getVisibility() + ", size=" + mDanmuContainer.getWidth() + "x" + mDanmuContainer.getHeight());
            }
            
            // 通过控制两个ImageView的visibility来切换图标
            if (mDanmuToggleOn != null && mDanmuToggleOff != null) {
                if (finalEnabled) {
                    android.util.Log.d(TAG, "Before: ON visibility=" + mDanmuToggleOn.getVisibility() + ", OFF visibility=" + mDanmuToggleOff.getVisibility());
                    android.util.Log.d(TAG, "ON size: " + mDanmuToggleOn.getWidth() + "x" + mDanmuToggleOn.getHeight() + ", OFF size: " + mDanmuToggleOff.getWidth() + "x" + mDanmuToggleOff.getHeight());
                    
                    mDanmuToggleOn.setVisibility(VISIBLE);
                    mDanmuToggleOff.setVisibility(GONE);
                    
                    android.util.Log.d(TAG, "After: ON visibility=" + mDanmuToggleOn.getVisibility() + ", OFF visibility=" + mDanmuToggleOff.getVisibility());
                    android.util.Log.d(TAG, "Showing ON icon, hiding OFF icon");
                } else {
                    android.util.Log.d(TAG, "Before: ON visibility=" + mDanmuToggleOn.getVisibility() + ", OFF visibility=" + mDanmuToggleOff.getVisibility());
                    android.util.Log.d(TAG, "ON size: " + mDanmuToggleOn.getWidth() + "x" + mDanmuToggleOn.getHeight() + ", OFF size: " + mDanmuToggleOff.getWidth() + "x" + mDanmuToggleOff.getHeight());
                    
                    mDanmuToggleOn.setVisibility(GONE);
                    mDanmuToggleOff.setVisibility(VISIBLE);
                    
                    android.util.Log.d(TAG, "After: ON visibility=" + mDanmuToggleOn.getVisibility() + ", OFF visibility=" + mDanmuToggleOff.getVisibility());
                    android.util.Log.d(TAG, "Showing OFF icon, hiding ON icon");
                }
            } else {
                android.util.Log.e(TAG, "mDanmuToggleOn or mDanmuToggleOff is NULL!");
            }
            
            // 同时控制输入框和设置按钮的可见性
            if (mDanmuInput != null) {
                mDanmuInput.setVisibility(finalEnabled ? VISIBLE : INVISIBLE);
            }
            if (mDanmuSet != null) {
                mDanmuSet.setVisibility(finalEnabled ? VISIBLE : GONE);
            }
        });
    }
}
