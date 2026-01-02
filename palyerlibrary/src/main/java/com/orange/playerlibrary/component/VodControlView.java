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
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

/**
 * 视频点播控制视图
 * 显示播放/暂停按钮、进度条、时间、全屏按钮、弹幕控制区
 * 
 * Requirements: 3.3
 */
public class VodControlView extends FrameLayout implements IControlComponent,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VodControlView";
    private static boolean sDebug = false;

    // 双击防抖：记录上次状态变化时间，避免双击后的单击事件干扰
    private long mLastStateChangeTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 500; // 500ms 内的单击事件忽略

    // 图标更新防抖：记录上次图标更新时间，避免更新过快
    private long mLastIconUpdateTime = 0;
    private static final long ICON_UPDATE_INTERVAL = 200; // 200ms 内的图标更新忽略

    // 长按倍速相关
    private float mLongPressSpeed = 3.0f;
    private float mNormalSpeed = 1.0f;
    private boolean mIsLongPressing = false;
    
    // 点击事件监听器
    private View.OnClickListener mOnSpeedControlClickListener;
    private View.OnClickListener mOnEpisodeSelectClickListener;
    private View.OnClickListener mOnSetupClickListener;

    // 控制包装器
    private ControlWrapper mControlWrapper;
    private OrangeVideoController mOrangeController;
    
    // 静态变量保存控制器引用，用于全屏时重新绑定
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
    private ImageView mDanmuToggle;
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

    /**
     * 初始化视图组件
     * Requirements: 1.5, 3.2, 3.4
     */
    private void init() {
        setVisibility(GONE);
        // 设置为不可点击，让触摸事件穿透到下层
        setClickable(false);
        LayoutInflater.from(getContext()).inflate(R.layout.orange_layout_vod_control_view, this, true);

        // 初始化主视图
        mBottomContainer = findViewById(R.id.bottom_container);
        mTopContainer = findViewById(R.id.container_main);
        mDanmuContainer = findViewById(R.id.danmu_container);

        // 播放按钮
        mPlayButton = findViewById(R.id.iv_play);
        if (mPlayButton != null) {
            mPlayButton.setOnClickListener(this);
            // 设置长按倍速功能
            setupLongPressSpeed(mPlayButton);
        } else {
            android.util.Log.w(TAG, "init: 播放按钮未找到");
        }

        // 全屏按钮
        mFullScreen = findViewById(R.id.fullscreen);
        if (mFullScreen != null) {
            mFullScreen.setOnClickListener(this);
        } else {
            android.util.Log.w(TAG, "init: 全屏按钮未找到");
        }

        // 进度条
        mVideoProgress = findViewById(R.id.seekBar);
        if (mVideoProgress != null) {
            mVideoProgress.setOnSeekBarChangeListener(this);
        } else {
            android.util.Log.w(TAG, "init: 进度条未找到");
        }

        // 时间显示
        mCurrTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);

        // 底部进度条
        mBottomProgress = findViewById(R.id.bottom_progress);

        // 弹幕控制
        mDanmuToggle = findViewById(R.id.danmu_toggle);
        mDanmuSet = findViewById(R.id.danmu_set);
        mDanmuInput = findViewById(R.id.danmu_input);

        // 功能按钮
        mSpeedControl = findViewById(R.id.speed_control);
        mEpisodeSelect = findViewById(R.id.episode_select);
        mSkipButton = findViewById(R.id.film_header_footer);
        mSourceSelect = findViewById(R.id.source_select);

        // 全屏时弹幕区的播放和全屏按钮
        mPlayButtonFullscreen = findViewById(R.id.iv_play_fullscreen);
        mFullScreenDanmu = findViewById(R.id.fullscreen_danmu);
        
        // 下一集按钮
        mPlayNext = findViewById(R.id.playnext);
        
        // 竖屏全屏按钮
        mShup = findViewById(R.id.shup);
        if (mShup != null) {
            mShup.setOnClickListener(this);
        }

        if (mSpeedControl != null) {
            mSpeedControl.setOnClickListener(this);
            android.util.Log.d(TAG, "init: 倍速按钮初始化完成");
        } else {
            android.util.Log.w(TAG, "init: 倍速按钮为 null");
        }
        if (mEpisodeSelect != null) {
            mEpisodeSelect.setVisibility(GONE);
            mEpisodeSelect.setOnClickListener(this);
            android.util.Log.d(TAG, "init: 选集按钮初始化完成");
        } else {
            android.util.Log.w(TAG, "init: 选集按钮为 null");
        }
        if (mPlayButtonFullscreen != null) {
            mPlayButtonFullscreen.setOnClickListener(this);
            // 全屏播放按钮也支持长按倍速
            setupLongPressSpeed(mPlayButtonFullscreen);
        } else {
            android.util.Log.w(TAG, "init: 全屏播放按钮为 null");
        }
        if (mFullScreenDanmu != null) {
            mFullScreenDanmu.setOnClickListener(this);
        } else {
            android.util.Log.w(TAG, "init: 全屏弹幕按钮为 null");
        }
    }

    public void setOrangeVideoController(OrangeVideoController controller) {
        mOrangeController = controller;
        sSharedController = controller; // 保存到静态变量
    }

    /**
     * 附加到控制包装器
     * Requirements: 3.5
     * 
     * @param controlWrapper 控制包装器实例
     */
    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        android.util.Log.d(TAG, "attach: 被调用, controlWrapper=" + controlWrapper.getClass().getName());
        mControlWrapper = controlWrapper;
        
        // 从静态变量获取控制器（用于全屏时重新绑定）
        if (mOrangeController == null && sSharedController != null) {
            android.util.Log.d(TAG, "attach: 从静态变量获取控制器");
            mOrangeController = sSharedController;
        }
        
        // 通知 OrangeVideoController 重新绑定事件
        if (mOrangeController != null) {
            android.util.Log.d(TAG, "attach: 通知控制器重新绑定事件");
            com.orange.playerlibrary.VideoEventManager eventManager = 
                    mOrangeController.getVideoEventManager();
            if (eventManager != null) {
                android.util.Log.d(TAG, "attach: 调用 bindControllerComponents");
                eventManager.bindControllerComponents(this);
            } else {
                android.util.Log.d(TAG, "attach: VideoEventManager 为 null");
            }
        } else {
            android.util.Log.d(TAG, "attach: mOrangeController 为 null，无法绑定事件");
        }
    }

    @Override
    public View getView() {
        return this;
    }

    /**
     * 处理点击事件
     * Requirements: 4.3
     * 
     * @param v 被点击的视图
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        android.util.Log.d(TAG, "onClick: 按钮被点击, id=" + id);
        
        if (id == R.id.fullscreen || id == R.id.fullscreen_danmu) {
            android.util.Log.d(TAG, "onClick: 全屏按钮");
            toggleFullScreen();
        } else if (id == R.id.iv_play || id == R.id.iv_play_fullscreen) {
            // 检查是否在双击后的短时间内，如果是则忽略此单击事件
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastStateChangeTime < DOUBLE_CLICK_INTERVAL) {
                android.util.Log.d(TAG, "onClick: 忽略双击后的单击事件, 时间间隔=" + (currentTime - mLastStateChangeTime) + "ms");
                return;
            }

            android.util.Log.d(TAG, "onClick: 播放按钮");
            if (mControlWrapper != null) {
                mControlWrapper.togglePlay();
            }
        } else if (id == R.id.speed_control) {
            android.util.Log.d(TAG, "onClick: 倍速按钮");
            // 倍速控制点击事件
            if (mOnSpeedControlClickListener != null) {
                android.util.Log.d(TAG, "onClick: 调用倍速监听器");
                mOnSpeedControlClickListener.onClick(v);
            } else {
                android.util.Log.d(TAG, "onClick: 倍速监听器为 null");
            }
        } else if (id == R.id.episode_select) {
            android.util.Log.d(TAG, "onClick: 选集按钮");
            // 选集点击事件
            if (mOnEpisodeSelectClickListener != null) {
                android.util.Log.d(TAG, "onClick: 调用选集监听器");
                mOnEpisodeSelectClickListener.onClick(v);
            } else {
                android.util.Log.d(TAG, "onClick: 选集监听器为 null");
            }
        } else if (id == R.id.shup) {
            android.util.Log.d(TAG, "onClick: 竖屏全屏按钮");
            // 竖屏全屏切换
            toggleFullScreen();
        }
    }

    /**
     * 切换全屏
     * Requirements: 4.3
     */
    private void toggleFullScreen() {
        android.util.Log.d(TAG, "toggleFullScreen: 被调用");
        if (mControlWrapper != null) {
            android.util.Log.d(TAG, "toggleFullScreen: 调用 mControlWrapper.toggleFullScreen()");
            mControlWrapper.toggleFullScreen();
        } else {
            android.util.Log.d(TAG, "toggleFullScreen: mControlWrapper 为 null");
        }
    }

    /**
     * 可见性变化回调
     * Requirements: 4.1, 4.4
     * 
     * @param isVisible 是否可见
     * @param anim 动画效果
     */
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

    /**
     * 播放状态变化回调
     * Requirements: 4.1, 4.2
     * 
     * @param playState 播放状态常量
     */
    @Override
    public void onPlayStateChanged(int playState) {

        // 记录状态变化时间，用于双击防抖
        mLastStateChangeTime = System.currentTimeMillis();

        switch (playState) {
            case PlayerConstants.STATE_ERROR:
            case PlayerConstants.STATE_PREPARING:
            case PlayerConstants.STATE_PREPARED:
            case 8: // 移动网络警告
                setVisibility(GONE);
                break;

            case PlayerConstants.STATE_IDLE:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                resetProgress();
                break;

            case PlayerConstants.STATE_PLAYING:

                // 直接根据 playState 判断，不依赖 mControlWrapper.isPlaying()
                updatePlayButtonState(true); // 播放中，显示暂停图标
                // 不自动设置可见性，由 OrangevideoView 控制
                updateBottomProgressVisibility();
                break;

            case PlayerConstants.STATE_PAUSED:
                // 直接根据 playState 判断，不依赖 mControlWrapper.isPlaying()
                updatePlayButtonState(false); // 暂停中，显示播放图标
                break;

            case PlayerConstants.STATE_BUFFERING:
                // 缓冲时保持当前图标不变
                if (mControlWrapper != null) {
                    updatePlayButtonState(mControlWrapper.isPlaying());
                }
                break;

            case PlayerConstants.STATE_BUFFERED:
                // 缓冲完成时保持当前图标不变
                if (mControlWrapper != null) {
                    updatePlayButtonState(mControlWrapper.isPlaying());
                }
                break;
        }
    }

    /**
     * 更新播放按钮状态
     * 使用 setSelected() 配合 selector drawable 实现图标切换
     * Requirements: 4.4
     * 
     * @param isPlaying true 表示播放中，false 表示暂停
     */
    private void updatePlayButtonState(final boolean isPlaying) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - mLastIconUpdateTime;
        // 记录本次更新时间
        mLastIconUpdateTime = currentTime;

        // 直接同步更新，不使用 post()
        if (mPlayButton != null) {
            // 使用 setSelected() 触发 selector 状态切换
            // isPlaying=true → selected=true → 显示暂停图标（双杠）
            // isPlaying=false → selected=false → 显示播放图标（三角形）
            mPlayButton.setSelected(isPlaying);
            // 强制刷新 drawable 状态
            mPlayButton.refreshDrawableState();
            mPlayButton.invalidate();
        }

        if (mPlayButtonFullscreen != null) {
            mPlayButtonFullscreen.setSelected(isPlaying);
            mPlayButtonFullscreen.refreshDrawableState();
            mPlayButtonFullscreen.invalidate();
        }
    }

    /**
     * 播放器状态变化回调（全屏/普通）
     * Requirements: 4.2
     * 
     * @param playerState 播放器状态常量
     */
    @Override
    public void onPlayerStateChanged(int playerState) {
        android.util.Log.d(TAG, "onPlayerStateChanged: playerState=" + playerState);
        
        if (playerState == PlayerConstants.PLAYER_FULL_SCREEN) {
            // 全屏模式
            if (mDanmuContainer != null) {
                mDanmuContainer.setVisibility(VISIBLE);
            }
            // 隐藏第一行的播放和全屏按钮
            if (mPlayButton != null) {
                mPlayButton.setVisibility(GONE);
            }
            if (mFullScreen != null) {
                mFullScreen.setVisibility(GONE);
            }
            // 显示弹幕区的播放和全屏按钮
            if (mPlayButtonFullscreen != null) {
                mPlayButtonFullscreen.setVisibility(VISIBLE);
            }
            if (mFullScreenDanmu != null) {
                mFullScreenDanmu.setVisibility(VISIBLE);
                mFullScreenDanmu.setSelected(true);
            }
            if (mSkipButton != null) {
                mSkipButton.setVisibility(VISIBLE);
            }
            // 全屏时显示选集按钮
            if (mEpisodeSelect != null) {
                mEpisodeSelect.setVisibility(VISIBLE);
                android.util.Log.d(TAG, "onPlayerStateChanged: 选集按钮显示");
            }
            // 全屏时显示倍速按钮
            if (mSpeedControl != null) {
                mSpeedControl.setVisibility(VISIBLE);
                android.util.Log.d(TAG, "onPlayerStateChanged: 倍速按钮显示");
            }
        } else if (playerState == PlayerConstants.PLAYER_NORMAL) {
            // 非全屏模式
            if (mDanmuContainer != null) {
                mDanmuContainer.setVisibility(GONE);
            }
            // 显示第一行的播放和全屏按钮
            if (mPlayButton != null) {
                mPlayButton.setVisibility(VISIBLE);
            }
            if (mFullScreen != null) {
                mFullScreen.setVisibility(VISIBLE);
                mFullScreen.setSelected(false);
            }
            // 隐藏弹幕区的播放和全屏按钮
            if (mPlayButtonFullscreen != null) {
                mPlayButtonFullscreen.setVisibility(GONE);
            }
            if (mFullScreenDanmu != null) {
                mFullScreenDanmu.setVisibility(GONE);
                mFullScreenDanmu.setSelected(false);
            }
            if (mSkipButton != null) {
                mSkipButton.setVisibility(GONE);
            }
            // 非全屏时隐藏选集按钮
            if (mEpisodeSelect != null) {
                mEpisodeSelect.setVisibility(GONE);
                android.util.Log.d(TAG, "onPlayerStateChanged: 选集按钮隐藏");
            }
        }
    }

    /**
     * 设置播放进度
     * Requirements: 4.4
     * 
     * @param duration 总时长（毫秒）
     * @param position 当前位置（毫秒）
     */
    @Override
    public void setProgress(int duration, int position) {
        if (mIsDragging)
            return;

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

            // 更新缓冲进度
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

        // 更新时间显示
        if (mTotalTime != null) {
            mTotalTime.setText(stringForTime(duration));
        }
        if (mCurrTime != null) {
            mCurrTime.setText(stringForTime(position));
        }
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        onVisibilityChanged(!isLocked, null);
    }

    // ===== SeekBar 监听 =====

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

    // ===== 辅助方法 =====

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

    /**
     * 格式化时间为可读字符串
     * Requirements: 4.4
     * 
     * @param timeMs 时间（毫秒）
     * @return 格式化后的时间字符串（HH:MM:SS 或 MM:SS）
     */
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

    // ===== 公共方法 =====

    public static void setBottomProgress(boolean show) {
        sShowBottomProgress = show;
    }

    public void showBottomProgress(boolean show) {
        mIsShowBottomProgress = show;
    }

    public boolean isFullScreen() {
        return mControlWrapper != null && mControlWrapper.isFullScreen();
    }

    // ===== 获取组件 =====

    public ImageView getPlayButton() {
        return mPlayButton;
    }

    public ImageView getFullScreenButton() {
        return mFullScreen;
    }

    public SeekBar getVideoProgress() {
        return mVideoProgress;
    }

    public ImageView getDanmuToggle() {
        return mDanmuToggle;
    }

    public ImageView getDanmuSet() {
        return mDanmuSet;
    }

    public TextView getSpeedControl() {
        return mSpeedControl;
    }

    public TextView getEpisodeSelect() {
        return mEpisodeSelect;
    }

    // ===== 调试相关 =====

    public void setDebug(boolean debug) {
        sDebug = debug;
    }

    private void debug(Object message) {
        if (sDebug && OrangeVideoController.isdebug()) {
            android.util.Log.d(TAG, String.valueOf(message));
        }
    }

    /**
     * 设置长按倍速功能
     * 长按播放按钮时加速播放，松开恢复正常速度
     * Requirements: 5.6
     */
    private void setupLongPressSpeed(View view) {
        if (view == null) {
            return;
        }
        
        view.setOnLongClickListener(v -> {
            if (mControlWrapper != null && mControlWrapper.isPlaying() && !mIsLongPressing) {
                mIsLongPressing = true;
                mNormalSpeed = mControlWrapper.getSpeed();
                mControlWrapper.setSpeed(mLongPressSpeed);
                android.util.Log.d(TAG, "长按加速: " + mLongPressSpeed + "x");
                // 显示提示
                String message = getContext().getString(R.string.orange_long_press_speed, mLongPressSpeed);
                android.widget.Toast.makeText(getContext(), message, 
                        android.widget.Toast.LENGTH_SHORT).show();
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
                    android.util.Log.d(TAG, "恢复正常速度: " + mNormalSpeed + "x");
                    // 显示提示
                    String message = getContext().getString(R.string.orange_restore_normal_speed, mNormalSpeed);
                    android.widget.Toast.makeText(getContext(), message, 
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            return false; // 返回false让其他事件继续处理
        });
    }

    /**
     * 设置长按倍速值
     */
    public void setLongPressSpeed(float speed) {
        mLongPressSpeed = speed;
    }

    /**
     * 获取长按倍速值
     */
    public float getLongPressSpeed() {
        return mLongPressSpeed;
    }
    
    /**
     * 设置倍速按钮点击监听器
     */
    public void setOnSpeedControlClickListener(View.OnClickListener listener) {
        mOnSpeedControlClickListener = listener;
    }
    
    /**
     * 设置选集按钮点击监听器
     */
    public void setOnEpisodeSelectClickListener(View.OnClickListener listener) {
        mOnEpisodeSelectClickListener = listener;
    }
    
    /**
     * 设置设置按钮点击监听器
     */
    public void setOnSetupClickListener(View.OnClickListener listener) {
        mOnSetupClickListener = listener;
    }
}
