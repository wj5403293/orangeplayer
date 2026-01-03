package com.orange.playerlibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

import java.util.ArrayList;

/**
 * 橘子播放器标准控制器基类
 * 提供基础控制功能：锁屏、UI显示控制、加载动画等
 * 
 * Requirements: 2.1, 2.3, 2.5
 */
public class OrangeStandardVideoController extends FrameLayout {

    private static final String TAG = "OrangeStandardController";

    // 控制组件容器
    protected FrameLayout mControlContainer;
    
    // 锁屏按钮
    protected ImageView mLockButton;
    
    // 加载动画容器
    protected ViewGroup mLoadingContainer;
    protected ProgressBar mLoadingProgress;
    protected TextView mNetSpeedText;
    
    // 控制组件列表
    protected final ArrayList<IControlComponent> mControlComponents = new ArrayList<>();
    
    // 控制包装器
    protected ControlWrapper mControlWrapper;
    
    // 锁屏状态
    protected boolean mIsLocked = false;
    
    // UI 显示状态
    protected boolean mIsShowing = false;
    
    // 显示/隐藏动画
    protected Animation mShowAnim;
    protected Animation mHideAnim;
    
    // 默认显示超时时间（毫秒）
    protected int mDefaultTimeout = 4000;
    
    // 隐藏 Runnable
    private final Runnable mFadeOut = this::hide;
    
    // 网速更新相关
    private android.os.Handler mSpeedHandler;
    private boolean mIsBuffering = false;
    private OrangevideoView mVideoViewRef;
    
    private final Runnable mSpeedUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateNetSpeed();
            if (mIsBuffering && mSpeedHandler != null) {
                mSpeedHandler.postDelayed(this, 1000);
            }
        }
    };

    public OrangeStandardVideoController(Context context) {
        super(context);
        init(context);
    }

    public OrangeStandardVideoController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OrangeStandardVideoController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    /**
     * 初始化
     */
    protected void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(getLayoutId(), this, true);
        
        // 初始化视图
        initViews();
        
        // 初始化动画
        initAnimations(context);
        
        // 设置点击事件
        setupClickListeners();
        
        // 初始化网速更新 Handler
        mSpeedHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }
    
    /**
     * 设置关联的播放器视图（用于获取网速）
     */
    public void setVideoViewRef(OrangevideoView videoView) {
        mVideoViewRef = videoView;
    }

    /**
     * 获取布局 ID
     * 子类可重写此方法返回自定义布局
     * 
     * @return 布局资源 ID
     */
    protected int getLayoutId() {
        return R.layout.layout_orange_standard_controller;
    }

    /**
     * 初始化视图
     */
    protected void initViews() {
        mControlContainer = findViewById(R.id.control_container);
        mLockButton = findViewById(R.id.iv_lock);
        mLoadingContainer = findViewById(R.id.loading_container);
        mLoadingProgress = findViewById(R.id.loading_progress);
        mNetSpeedText = findViewById(R.id.tv_net_speed);
    }

    /**
     * 初始化动画
     */
    protected void initAnimations(Context context) {
        mShowAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        mHideAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        mShowAnim.setDuration(300);
        mHideAnim.setDuration(300);
    }

    /**
     * 设置点击事件
     */
    protected void setupClickListeners() {
        if (mLockButton != null) {
            mLockButton.setOnClickListener(v -> toggleLockState());
        }
    }

    // ==================== 锁屏功能 ====================

    /**
     * 设置锁屏状态
     * Requirements: 2.5
     * 
     * @param locked 是否锁屏
     */
    public void setLocked(boolean locked) {
        mIsLocked = locked;
        updateLockButtonState();
        onLockStateChanged(locked);
    }

    /**
     * 是否锁屏
     * Requirements: 2.5
     * 
     * @return true 锁屏状态
     */
    public boolean isLocked() {
        return mIsLocked;
    }

    /**
     * 切换锁屏状态
     */
    public void toggleLockState() {
        setLocked(!mIsLocked);
    }

    /**
     * 锁屏状态改变回调
     * Requirements: 2.5
     * 
     * @param locked 是否锁屏
     */
    protected void onLockStateChanged(boolean locked) {
        // 通知所有控制组件
        for (IControlComponent component : mControlComponents) {
            component.onLockStateChanged(locked);
        }
        
        // 锁屏时隐藏其他控制组件
        if (locked) {
            hideAllControlComponents();
        } else {
            showAllControlComponents();
        }
    }

    /**
     * 更新锁屏按钮状态
     */
    protected void updateLockButtonState() {
        if (mLockButton != null) {
            mLockButton.setImageResource(mIsLocked 
                ? R.drawable.dkplayer_ic_action_lock_close 
                : R.drawable.dkplayer_ic_action_lock_open);
        }
    }

    // ==================== UI 显示控制 ====================

    /**
     * 显示控制器
     * Requirements: 2.3
     */
    public void show() {
        if (!mIsShowing) {
            mIsShowing = true;
            startShowAnimation();
            onVisibilityChanged(true, mShowAnim);
        }
        // 重置隐藏计时器
        removeCallbacks(mFadeOut);
        postDelayed(mFadeOut, mDefaultTimeout);
    }

    /**
     * 隐藏控制器
     * Requirements: 2.3
     */
    public void hide() {
        if (mIsShowing) {
            mIsShowing = false;
            startHideAnimation();
            onVisibilityChanged(false, mHideAnim);
        }
        removeCallbacks(mFadeOut);
    }

    /**
     * 是否显示
     * Requirements: 2.3
     * 
     * @return true 显示状态
     */
    public boolean isShowing() {
        return mIsShowing;
    }

    /**
     * 切换显示状态
     */
    public void toggleShowState() {
        if (mIsShowing) {
            hide();
        } else {
            show();
        }
    }

    /**
     * 可见性改变回调
     * Requirements: 2.3
     * 
     * @param isVisible 是否可见
     * @param anim 动画
     */
    protected void onVisibilityChanged(boolean isVisible, Animation anim) {
        // 通知所有控制组件
        for (IControlComponent component : mControlComponents) {
            component.onVisibilityChanged(isVisible, anim);
        }
        
        // 更新锁屏按钮可见性
        updateLockButtonVisibility(isVisible);
    }


    /**
     * 开始显示动画
     */
    protected void startShowAnimation() {
        setVisibility(VISIBLE);
        if (mShowAnim != null) {
            startAnimation(mShowAnim);
        }
    }

    /**
     * 开始隐藏动画
     */
    protected void startHideAnimation() {
        if (mHideAnim != null) {
            startAnimation(mHideAnim);
            mHideAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    setVisibility(GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        } else {
            setVisibility(GONE);
        }
    }

    /**
     * 更新锁屏按钮可见性
     */
    protected void updateLockButtonVisibility(boolean isVisible) {
        if (mLockButton != null) {
            // 全屏模式下才显示锁屏按钮
            if (isFullScreen()) {
                mLockButton.setVisibility(isVisible ? VISIBLE : GONE);
            } else {
                mLockButton.setVisibility(GONE);
            }
        }
    }

    /**
     * 隐藏所有控制组件
     */
    protected void hideAllControlComponents() {
        for (IControlComponent component : mControlComponents) {
            View view = component.getView();
            if (view != null && view != mLockButton) {
                view.setVisibility(GONE);
            }
        }
    }

    /**
     * 显示所有控制组件
     */
    protected void showAllControlComponents() {
        for (IControlComponent component : mControlComponents) {
            View view = component.getView();
            if (view != null) {
                view.setVisibility(VISIBLE);
            }
        }
    }

    // ==================== 加载动画控制 ====================

    /**
     * 显示加载动画
     */
    public void showLoading() {
        if (mLoadingContainer != null) {
            mLoadingContainer.setVisibility(VISIBLE);
        }
    }

    /**
     * 隐藏加载动画
     */
    public void hideLoading() {
        if (mLoadingContainer != null) {
            mLoadingContainer.setVisibility(GONE);
        }
    }

    /**
     * 设置网速文本
     * 
     * @param speed 网速文本
     */
    public void setNetSpeedText(String speed) {
        if (mNetSpeedText != null) {
            mNetSpeedText.setText(speed);
            mNetSpeedText.setVisibility(speed != null && !speed.isEmpty() ? VISIBLE : GONE);
        }
    }

    // ==================== 控制组件管理 ====================

    /**
     * 添加控制组件
     * 
     * @param components 控制组件
     */
    public void addControlComponent(IControlComponent... components) {
        for (IControlComponent component : components) {
            if (component != null && !mControlComponents.contains(component)) {
                mControlComponents.add(component);
                if (mControlWrapper != null) {
                    component.attach(mControlWrapper);
                }
                View view = component.getView();
                if (view != null && mControlContainer != null) {
                    mControlContainer.addView(view);
                }
            }
        }
    }
    
    /**
     * 添加控制组件（不添加视图到容器）
     * 用于弹幕等需要独立显示但仍需接收回调的组件
     * 
     * @param components 控制组件
     */
    public void addControlComponentWithoutView(IControlComponent... components) {
        for (IControlComponent component : components) {
            if (component != null && !mControlComponents.contains(component)) {
                mControlComponents.add(component);
                if (mControlWrapper != null) {
                    component.attach(mControlWrapper);
                }
                // 不添加视图到 mControlContainer
            }
        }
    }

    /**
     * 移除所有控制组件
     */
    public void removeAllControlComponent() {
        if (mControlContainer != null) {
            mControlContainer.removeAllViews();
        }
        mControlComponents.clear();
    }

    /**
     * 设置控制包装器
     * 
     * @param controlWrapper 控制包装器
     */
    public void setControlWrapper(ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
        // 通知所有已添加的组件
        for (IControlComponent component : mControlComponents) {
            component.attach(controlWrapper);
        }
    }

    /**
     * 获取控制包装器
     * 
     * @return 控制包装器
     */
    public ControlWrapper getControlWrapper() {
        return mControlWrapper;
    }

    // ==================== 状态回调 ====================

    /**
     * 播放状态改变
     * 
     * @param playState 播放状态
     */
    public void onPlayStateChanged(int playState) {
        // 根据播放状态控制加载动画和网速显示
        switch (playState) {
            case PlayerConstants.STATE_PREPARING:
            case PlayerConstants.STATE_BUFFERING:
                showLoading();
                startSpeedUpdate();
                break;
            case PlayerConstants.STATE_PREPARED:
            case PlayerConstants.STATE_PLAYING:
            case PlayerConstants.STATE_BUFFERED:
            case PlayerConstants.STATE_PAUSED:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
            case PlayerConstants.STATE_ERROR:
                hideLoading();
                stopSpeedUpdate();
                break;
        }
        
        // 通知所有控制组件
        for (IControlComponent component : mControlComponents) {
            component.onPlayStateChanged(playState);
        }
    }
    
    /**
     * 开始网速更新
     */
    protected void startSpeedUpdate() {
        if (!mIsBuffering && mSpeedHandler != null) {
            mIsBuffering = true;
            if (mNetSpeedText != null) {
                mNetSpeedText.setVisibility(VISIBLE);
            }
            mSpeedHandler.post(mSpeedUpdateRunnable);
        }
    }
    
    /**
     * 停止网速更新
     */
    protected void stopSpeedUpdate() {
        if (mIsBuffering && mSpeedHandler != null) {
            mIsBuffering = false;
            mSpeedHandler.removeCallbacks(mSpeedUpdateRunnable);
            if (mNetSpeedText != null) {
                mNetSpeedText.setVisibility(GONE);
            }
        }
    }
    
    /**
     * 更新网速显示
     */
    protected void updateNetSpeed() {
        if (mVideoViewRef != null && mNetSpeedText != null) {
            String speedText = mVideoViewRef.getNetSpeedText();
            mNetSpeedText.setText(speedText);
        }
    }

    /**
     * 播放器状态改变
     * 
     * @param playerState 播放器状态
     */
    public void onPlayerStateChanged(int playerState) {
        // 更新锁屏按钮可见性
        updateLockButtonVisibility(mIsShowing);
        
        // 通知所有控制组件
        for (IControlComponent component : mControlComponents) {
            component.onPlayerStateChanged(playerState);
        }
    }

    /**
     * 设置播放进度
     * 
     * @param duration 总时长
     * @param position 当前位置
     */
    public void setProgress(int duration, int position) {
        for (IControlComponent component : mControlComponents) {
            component.setProgress(duration, position);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 是否全屏
     * 
     * @return true 全屏状态
     */
    protected boolean isFullScreen() {
        if (mControlWrapper != null) {
            return mControlWrapper.isFullScreen();
        }
        return false;
    }

    /**
     * 设置默认显示超时时间
     * 
     * @param timeout 超时时间（毫秒）
     */
    public void setDefaultTimeout(int timeout) {
        mDefaultTimeout = timeout;
    }

    /**
     * 获取默认显示超时时间
     * 
     * @return 超时时间（毫秒）
     */
    public int getDefaultTimeout() {
        return mDefaultTimeout;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mFadeOut);
    }
}
