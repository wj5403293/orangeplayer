package com.orange.playerlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.component.VodControlView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 视频事件管理
 * 负责处理播放器UI的各种点击事件和功能
 */
public class VideoEventManager {
    
    private static final String TAG = "VideoEventManager";
    private static final int COLOR_HIGHLIGHT = Color.parseColor("#FFDDC333");
    private static final int COLOR_NORMAL = Color.parseColor("#FFACABAA");
    
    // 字幕文件选择请求码
    public static final int REQUEST_CODE_SUBTITLE_FILE = 10086;
    
    private final Context mContext;
    private final OrangevideoView mVideoView;
    private final Activity mActivity;
    private final OrangeVideoController mController;
    private final PlayerSettingsManager mSettingsManager;
    private final OrangeSharedSqlite mSqlite;
    
    // 组件引用
    private VodControlView mVodControlView;
    
    // 对话框引
    private AlertDialog mCurrentSetupDialog;
    
    // 长按倍速相
    private float mLongPressSpeed = 3.0f;
    private float mNormalSpeed = 1.0f;
    private boolean mIsLongPressing = false;
    
    // OCR 全屏切换相关
    private boolean mOcrPausedForFullscreen = false;
    private String mOcrSourceLang = "chi_sim";
    private String mOcrTargetLang = "en";
    
    public VideoEventManager(Context context, OrangevideoView videoView, OrangeVideoController controller) {
        mContext = context;
        mVideoView = videoView;
        mController = controller;
        mActivity = (Activity) context;
        mSettingsManager = PlayerSettingsManager.getInstance(context);
        mSqlite = OrangevideoView.sqlite;
        
        // 从设置中读取长按倍
        mLongPressSpeed = mSettingsManager.getLongPressSpeed();
        
        // 绑定基础事件
        bindEvents();
        
        // 注册播放器状态监听器（用于处理 OCR 全屏切换）
        registerPlayerStateListener();
    }
    
    /**
     * 注册播放器状态监听器
     * 用于在全屏切换时暂停/恢复 OCR
     */
    private void registerPlayerStateListener() {
        if (mVideoView != null) {
            mVideoView.addOnStateChangeListener(new com.orange.playerlibrary.interfaces.OnStateChangeListener() {
                @Override
                public void onPlayerStateChanged(int playerState) {
                    handlePlayerStateChangedForOcr(playerState);
                }
                
                @Override
                public void onPlayStateChanged(int playState) {
                    // 不需要处理
                }
            });
        }
    }
    
    /**
     * 处理播放器状态变化（用于 OCR 全屏切换）
     * 
     * 问题：TextureView 模式下全屏切换（屏幕旋转）会导致 MediaCodec 崩溃
     * 解决方案：
     * 1. 全屏切换前：停止 OCR，切换到 SurfaceView 模式（由 shouldInterceptFullscreenForOcr 处理）
     * 2. 全屏切换后：切换回 TextureView 模式，恢复 OCR（由本方法处理）
     */
    private void handlePlayerStateChangedForOcr(int playerState) {
        // 只有在 OCR 被暂停等待恢复时才处理
        if (!mOcrPausedForFullscreen) {
            return;
        }
        
        // 全屏切换完成后恢复 OCR
        // PLAYER_FULL_SCREEN = 11, PLAYER_NORMAL = 10
        if (playerState == PlayerConstants.PLAYER_FULL_SCREEN 
            || playerState == PlayerConstants.PLAYER_NORMAL) {
            // 延迟恢复 OCR，等待全屏切换动画和视频重新加载完成
            mMainHandler.postDelayed(() -> {
                resumeOcrAfterFullscreenSwitch();
            }, 2000);
        }
    }
    
    /**
     * 检查是否需要为 OCR 拦截全屏切换
     * 在全屏/竖屏切换前调用，如果返回 true，调用方应该先调用 pauseOcrForFullscreenSwitch
     * 
     * @return true 如果 OCR 正在运行且使用 EXO/系统内核（需要拦截）
     */
    public boolean shouldInterceptFullscreenForOcr() {
        // 检查 OCR 是否正在运行
        if (mOcrSubtitleManager == null || !mOcrSubtitleManager.isRunning()) {
            return false;
        }
        
        // 检查是否使用 EXO 或系统内核
        String currentEngine = mSettingsManager.getPlayerEngine();
        boolean isExoOrSystem = PlayerConstants.ENGINE_EXO.equals(currentEngine) 
            || PlayerConstants.ENGINE_DEFAULT.equals(currentEngine);
        
        // 检查是否 Android Q+（只有 Q+ 才使用 SurfaceControl.reparent）
        boolean isAndroidQ = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
        return isExoOrSystem && isAndroidQ;
    }
    
    /**
     * 在全屏切换前暂停 OCR 并切换到 SurfaceView
     * 调用方应该先调用 shouldInterceptFullscreenForOcr 检查是否需要拦截
     */
    public void pauseOcrForFullscreenSwitch() {
        if (mOcrSubtitleManager == null || !mOcrSubtitleManager.isRunning()) {
            return;
        }
        
        // 标记 OCR 已暂停，等待全屏切换完成后恢复
        mOcrPausedForFullscreen = true;
        
        // 1. 先暂停视频播放，避免切换过程中的画面撕裂
        boolean wasPlaying = false;
        long currentPosition = 0;
        String url = null;
        
        if (mVideoView != null) {
            wasPlaying = mVideoView.isPlaying();
            currentPosition = mVideoView.getCurrentPositionWhenPlaying();
            url = mVideoView.getUrl();
            
            if (wasPlaying) {
                mVideoView.pause();
            }
        }
        
        // 2. 停止 OCR
        if (mOcrSubtitleManager != null) {
            mOcrSubtitleManager.release();
            mOcrSubtitleManager = null;
        }
        
        // 3. 切换到 SurfaceView 模式并重新加载视频
        try {
            String currentEngine = mSettingsManager.getPlayerEngine();
            // 设置回 SurfaceView 模式
            if (PlayerConstants.ENGINE_EXO.equals(currentEngine)) {
                com.orange.playerlibrary.exo.OrangeExoPlayerManager.setForceTextureViewMode(false);
            } else if (PlayerConstants.ENGINE_DEFAULT.equals(currentEngine)) {
                com.orange.playerlibrary.player.OrangeSystemPlayerManager.setForceTextureViewMode(false);
            }
            
            // 设置 GSYVideoType 为 SurfaceView
            com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
            // 重新加载视频以应用新的渲染模式
            if (mVideoView != null && url != null && !url.isEmpty()) {
                final long seekPosition = currentPosition;
                final boolean shouldResume = wasPlaying;
                
                // 释放当前播放器
                mVideoView.release();
                com.shuyu.gsyvideoplayer.GSYVideoManager.releaseAllVideos();
                
                // 重新选择播放器工厂
                mVideoView.selectPlayerFactory(currentEngine);
                
                // 重新设置视频
                mVideoView.setUp(url, false, "");
                mVideoView.setSeekOnStart(seekPosition);
                
                // 暂停状态启动，让全屏切换更平滑
                // 全屏切换完成后会自动恢复 OCR 并继续播放
                mVideoView.startPlayLogic();
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * 在全屏切换后恢复 OCR
     */
    private void resumeOcrAfterFullscreenSwitch() {
        if (!mOcrPausedForFullscreen) {
            return;
        }
        mOcrPausedForFullscreen = false;
        
        // 重新启动 OCR
        // 注意：doStartOcrTranslate 会自动切换到 TextureView 模式并重新加载视频
        // 这里需要确保在正确的时机调用
        String currentEngine = mSettingsManager.getPlayerEngine();
        boolean needSwitchToTexture = PlayerConstants.ENGINE_EXO.equals(currentEngine) 
            || PlayerConstants.ENGINE_DEFAULT.equals(currentEngine);
        
        if (needSwitchToTexture && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // 对于 EXO/系统内核，需要先切换到 TextureView 模式
            // doStartOcrTranslate 会处理这个
            doStartOcrTranslate(mOcrSourceLang, mOcrTargetLang);
        } else {
            // 其他内核直接启动 OCR
            doStartOcrTranslateInternal(mOcrSourceLang, mOcrTargetLang);
        }
    }
    
    /**
     * 检查 OCR 是否正在运行
     */
    public boolean isOcrRunning() {
        return mOcrSubtitleManager != null && mOcrSubtitleManager.isRunning();
    }
    
    /**
     * 检查 OCR 是否被暂停等待恢复
     */
    public boolean isOcrPausedForFullscreen() {
        return mOcrPausedForFullscreen;
    }
    
    /**
     * 显示自定义Toast
     */
    private void showToast(String message) {
        if (mVideoView != null) {
            OrangeToast.show(mVideoView, message);
        }
    }
    
    /**
     * 绑定控制器组件
     */
    public void bindControllerComponents(VodControlView vodControlView) {
        mVodControlView = vodControlView;
        bindControllerEvents();
    }
    
    /**
     * 绑定 TitleView 组件
     */
    public void bindTitleView(com.orange.playerlibrary.component.TitleView titleView) {
        if (titleView != null) {
            // 绑定设置按钮点击事件
            titleView.setOnSettingsClickListener(v -> {
                showSetupDialog();
            });
            
            // 绑定投屏按钮点击事件
            titleView.setOnCastClickListener(v -> {
                showCastDialog();
            });
            
            // 绑定小窗按钮点击事件
            titleView.setOnWindowClickListener(v -> {
                onSmallWindowPlayClick();
            });
        }
    }
    
    /**
     * 绑定基础事件
     */
    private void bindEvents() {
        // 倍速按钮事
        // 注意：这里使用接口方式绑定，实际调用在bindControllerEvents
    }
    
    /**
     * 绑定控制器事
     */
    private void bindControllerEvents() {
                
        if (mVodControlView == null) {
                        return;
        }
        
        // 绑定倍速按钮点击事
                mVodControlView.setOnSpeedControlClickListener(v -> {
                        showSpeedDialog();
        });
        
        // 绑定选集按钮点击事件
                mVodControlView.setOnEpisodeSelectClickListener(v -> {
                        showPlaylistDialog();
        });
        
        // 绑定弹幕开关按钮点击事件
        mVodControlView.setOnDanmuToggleClickListener(v -> {
            toggleDanmaku(v);
        });
        
        // 绑定弹幕设置按钮点击事件
        mVodControlView.setOnDanmuSetClickListener(v -> {
            showDanmakuSettingsDialog();
        });
        
        // 绑定弹幕输入框点击事件
        mVodControlView.setOnDanmuInputClickListener(v -> {
            showDanmakuInputDialog();
        });
        
        // 绑定跳过片头片尾按钮点击事件
        mVodControlView.setOnSkipOpeningClickListener(v -> {
            showSkipDialog();
        });
        
        // 绑定下一集按钮点击事件
        mVodControlView.setOnPlayNextClickListener(v -> {
            playNextEpisode();
        });
        
        // 绑定字幕按钮点击事件
        mVodControlView.setOnSubtitleToggleClickListener(v -> {
            showSubtitleDialog(v);
        });
        
        // 绑定播放按钮长按事件（用于长按倍速）
        ImageView playButton = mVodControlView.getPlayButton();
        if (playButton != null) {
                        setupLongPressSpeed(playButton);
        } else {
                    }
        
            }
    
    /**
     * 显示倍速选择对话
     */
    private void showSpeedDialog() {
                mController.hide(); // 隐藏播放器UI
        
        try {
            // 创建对话框视图
            View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
                        
            // 始终显示在右侧
            final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                    DialogUtils.DialogPosition.RIGHT, null, null);
                        
            // 点击外部区域关闭对话框
            View layout = dialogView.findViewById(R.id.layout);
            if (layout != null) {
                layout.setOnClickListener(v -> dialog.dismiss());
                            }
            
            // 设置倍速选项
            setupSpeedOptions(dialogView, dialog);
        } catch (Exception e) {
        }
    }
    
    /**
     * 设置倍速选项
     */
    private void setupSpeedOptions(View dialogView, AlertDialog dialog) {
        // 倍速选项
        final String[] speeds = {"0.35x", "0.45x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", 
                                "2.5x", "3.0x", "3.5x", "4.0x", "4.5x", "5.0x", "6.0x", "7.0x", 
                                "8.0x", "9.0x", "10.0x"};
        
        // 创建数据列表
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        for (String speed : speeds) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", speed);
            arrayList.add(map);
        }
        
        // 使用 RecyclerView 显示倍速列
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView != null) {
            OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
            orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
            orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
                (holder, data, position) -> {
                    android.widget.TextView speedName = holder.itemView.findViewById(R.id.title);
                    String speedText = data.get(position).get("name").toString();
                    float speedValue = Float.parseFloat(speedText.replace("x", ""));
                    
                    // 高亮当前倍
                    float currentSpeed = mVideoView.getSpeed();
                    if (Math.abs(speedValue - currentSpeed) < 0.01f) {
                        speedName.setTextColor(COLOR_HIGHLIGHT);
                        speedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                    } else {
                        speedName.setTextColor(COLOR_NORMAL);
                        speedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                    }
                    
                    speedName.setText(speedText);
                    
                    // 倍速选择事件
                    speedName.setOnClickListener(v -> {
                        mVideoView.setSpeed(speedValue);
                        showToast("倍速 " + speedText);
                        dialog.dismiss();
                    });
                });
                    } else {
        }
    }
    
    /**
     * 设置长按倍速功
     * 长按视图时加速播放，松开恢复正常速度
     */
    private void setupLongPressSpeed(View view) {
        view.setOnLongClickListener(v -> {
            if (!mIsLongPressing && mVideoView.isPlaying()) {
                mIsLongPressing = true;
                mNormalSpeed = mVideoView.getSpeed();
                mVideoView.setSpeed(mLongPressSpeed);
                                return true;
            }
            return false;
        });
        
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                if (mIsLongPressing) {
                    mIsLongPressing = false;
                    mVideoView.setSpeed(mNormalSpeed);
                                    }
            }
            return false;
        });
    }
    
    /**
     * 设置长按倍
     */
    public void setLongPressSpeed(float speed) {
        mLongPressSpeed = speed;
        mSettingsManager.setLongPressSpeed(speed);
    }
    
    /**
     * 获取长按倍
     */
    public float getLongPressSpeed() {
        return mLongPressSpeed;
    }
    
    /**
     * 获取播放模式
     */
    private String getPlayMode() {
        return mSettingsManager.getPlayMode();
    }
    
    /**
     * 设置播放模式
     */
    private void setPlayMode(String mode) {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        mSettingsManager.setPlayMode(mode);
        showToast("播放模式: " + getPlayModeName(mode));
    }
    
    /**
     * 获取播放模式名称
     */
    private String getPlayModeName(String mode) {
        switch (mode) {
            case "sequential": return "顺序播放";
            case "single_loop": return "单集循环";
            case "play_pause": return "播放暂停";
            default: return "未知模式";
        }
    }
    
    // ==================== 设置对话====================
    
    /**
     * 显示投屏对话框
     */
    private void showCastDialog() {
        mController.hide(); // 隐藏播放器UI
        
        // 检查投屏库是否可用
        if (!com.orange.playerlibrary.cast.DLNACastManager.isDLNAAvailable()) {
            showToast("投屏功能未配置");
            return;
        }
        
        // 获取当前视频信息
        String videoUrl = mVideoView.getUrl();
        String title = mController.getVideoTitle();
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            showToast("无法获取视频地址");
            return;
        }
        
        try {
            // 启动投屏
            com.orange.playerlibrary.cast.DLNACastManager.getInstance().startCast(mActivity, videoUrl, title);
        } catch (Exception e) {
            showToast("投屏失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示设置对话框
     */
    public void showSetupDialog() {
        mController.hide(); // 隐藏播放器UI
        
        // 创建设置对话框视图
        View dialogView = View.inflate(mActivity, R.layout.setup_dialog, null);
        
        // 创建设置对话框 - 始终显示在右侧
        mCurrentSetupDialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 绑定所有设置项（使用 dialogView 而不是 mCurrentSetupDialog）
        bindSetupOptions(dialogView);
        
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> mCurrentSetupDialog.dismiss());
        }
    }
    
    /**
     * 绑定设置选项
     */
    private void bindSetupOptions(View dialogView) {
        // 获取所有设置项视图 - 从 dialogView 获取而不是从 dialog 获取
        android.widget.LinearLayout screenScaleButton = dialogView.findViewById(R.id.line1);
        android.widget.LinearLayout longPressSpeedButton = dialogView.findViewById(R.id.line2);
        android.widget.LinearLayout timerCloseButton = dialogView.findViewById(R.id.line3);
        android.widget.LinearLayout skipOpeningButton = dialogView.findViewById(R.id.line4);
        android.widget.LinearLayout skipEndingButton = dialogView.findViewById(R.id.line5);
        android.widget.LinearLayout smallWindowButton = dialogView.findViewById(R.id.line6);
        android.widget.LinearLayout progressBarButton = dialogView.findViewById(R.id.line7);
        android.widget.LinearLayout downloadButton = dialogView.findViewById(R.id.line10);
        android.widget.ImageView progressBarIcon = dialogView.findViewById(R.id.kgImage);
        
        // 播放核心按钮
        android.widget.TextView aliEngineBtn = dialogView.findViewById(R.id.alihx);
        android.widget.TextView exoEngineBtn = dialogView.findViewById(R.id.exohx);
        android.widget.TextView ijkEngineBtn = dialogView.findViewById(R.id.ijkhx);
        android.widget.TextView systemEngineBtn = dialogView.findViewById(R.id.systemhx);
        
        // 播放模式按钮
        android.widget.TextView sequentialPlayBtn = dialogView.findViewById(R.id.sxbf);
        android.widget.TextView singleLoopBtn = dialogView.findViewById(R.id.djxh);
        android.widget.TextView playPauseBtn = dialogView.findViewById(R.id.bwzt);
        
        // 获取音量控制组件
        android.widget.SeekBar volumeSeekBar = dialogView.findViewById(R.id.volumeSeek_bar);
        android.widget.TextView volumeText = dialogView.findViewById(R.id.volumeText);
        
        // 设置播放核心按钮
        setupEngineButtons(aliEngineBtn, exoEngineBtn, ijkEngineBtn, systemEngineBtn);
        
        // 设置播放模式按钮
        setupPlayModeButtons(sequentialPlayBtn, singleLoopBtn, playPauseBtn);
        
        // 设置进度条开关状态
        if (progressBarIcon != null) {
            boolean showProgress = mSettingsManager.isBottomProgressEnabled();
            progressBarIcon.setImageResource(showProgress ? R.mipmap.kg2 : R.mipmap.kg1);
        }
        
        // 绑定音量控制
        setupVolumeControl(volumeSeekBar, volumeText);
        
        // 绑定画面比例按钮点击事件
        if (screenScaleButton != null) {
            screenScaleButton.setOnClickListener(v -> showScreenScaleDialog());
        }
        
        // 绑定长按倍速按钮点击事件
        if (longPressSpeedButton != null) {
            longPressSpeedButton.setOnClickListener(v -> showLongPressSpeedDialog());
        }
        
        // 绑定定时关闭按钮点击事件
        if (timerCloseButton != null) {
            timerCloseButton.setOnClickListener(v -> showTimerCloseDialog());
        }
        
        // 绑定跳过片头按钮点击事件
        if (skipOpeningButton != null) {
            skipOpeningButton.setOnClickListener(v -> showSkipOpeningDialog());
        }
        
        // 绑定跳过片尾按钮点击事件
        if (skipEndingButton != null) {
            skipEndingButton.setOnClickListener(v -> showSkipEndingDialog());
        }
        
        // 绑定小窗播放按钮点击事件
        if (smallWindowButton != null) {
            smallWindowButton.setOnClickListener(v -> onSmallWindowPlayClick());
        }
        
        // 绑定进度条开关按钮点击事件
        if (progressBarButton != null) {
            progressBarButton.setOnClickListener(v -> onProgressBarClick(progressBarIcon));
        }
        
        // 绑定下载视频按钮点击事件
        if (downloadButton != null) {
            downloadButton.setOnClickListener(v -> onDownloadVideoClick());
        }
        
        // 绑定截图按钮点击事件
        android.widget.LinearLayout screenshotButton = dialogView.findViewById(R.id.line_screenshot);
        if (screenshotButton != null) {
            screenshotButton.setOnClickListener(v -> onScreenshotClick());
        }
    }
    
    /**
     * 设置播放核心按钮
     */
    private void setupEngineButtons(android.widget.TextView aliBtn, android.widget.TextView exoBtn, 
                                   android.widget.TextView ijkBtn, android.widget.TextView systemBtn) {
        // 检查核心是否可用
        boolean isAliPlayerAvailable = isClassPresent("com.aliyun.player.AliPlayer");
        boolean isIjkPlayerAvailable = isClassPresent("tv.danmaku.ijk.media.player.IjkMediaPlayer");
        // ExoPlayer 检测：GSY 11.x 使用 Media3，也检测旧版 ExoPlayer2
        boolean isExoPlayerAvailable = isClassPresent("com.shuyu.gsyvideoplayer.player.Exo2PlayerManager") ||
                                       isClassPresent("com.google.android.exoplayer2.ExoPlayer") ||
                                       isClassPresent("com.google.android.exoplayer2.Player") ||
                                       isClassPresent("androidx.media3.exoplayer.ExoPlayer");
        // 设置可见性
        if (aliBtn != null) aliBtn.setVisibility(isAliPlayerAvailable ? View.VISIBLE : View.GONE);
        if (ijkBtn != null) ijkBtn.setVisibility(isIjkPlayerAvailable ? View.VISIBLE : View.GONE);
        if (exoBtn != null) exoBtn.setVisibility(isExoPlayerAvailable ? View.VISIBLE : View.GONE);
        if (systemBtn != null) systemBtn.setVisibility(View.VISIBLE); // 系统核心始终可用
        
        // 获取当前引擎
        String currentEngine = mSettingsManager.getPlayerEngine();
        
        // 高亮当前引擎
        if (aliBtn != null) {
            aliBtn.setTextColor(PlayerConstants.ENGINE_ALI.equals(currentEngine) ? COLOR_HIGHLIGHT : COLOR_NORMAL);
            aliBtn.setOnClickListener(v -> selectEngine(PlayerConstants.ENGINE_ALI));
        }
        if (exoBtn != null) {
            exoBtn.setTextColor(PlayerConstants.ENGINE_EXO.equals(currentEngine) ? COLOR_HIGHLIGHT : COLOR_NORMAL);
            exoBtn.setOnClickListener(v -> selectEngine(PlayerConstants.ENGINE_EXO));
        }
        if (ijkBtn != null) {
            ijkBtn.setTextColor(PlayerConstants.ENGINE_IJK.equals(currentEngine) ? COLOR_HIGHLIGHT : COLOR_NORMAL);
            ijkBtn.setOnClickListener(v -> selectEngine(PlayerConstants.ENGINE_IJK));
        }
        if (systemBtn != null) {
            systemBtn.setTextColor(PlayerConstants.ENGINE_DEFAULT.equals(currentEngine) ? COLOR_HIGHLIGHT : COLOR_NORMAL);
            systemBtn.setOnClickListener(v -> selectEngine(PlayerConstants.ENGINE_DEFAULT));
        }
    }
    
    /**
     * 选择播放引擎
     */
    private void selectEngine(String engine) {
        String oldEngine = mSettingsManager.getPlayerEngine();
        // 保存播放核心设置
        mSettingsManager.setPlayerEngine(engine);
        
        // 关闭设置对话框
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        // 即时切换播放核心
        if (mVideoView != null) {
            // 记录当前播放位置和URL
            long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
            String currentUrl = mVideoView.getUrl();
            boolean wasPlaying = mVideoView.isPlaying();
            // 1. 先完全释放旧播放器（关键！）
            mVideoView.release();
            com.shuyu.gsyvideoplayer.GSYVideoManager.releaseAllVideos();
            
            // 2. 切换播放器工厂
            mVideoView.selectPlayerFactory(engine);
            // 3. 如果有正在播放的视频，重新加载
            if (currentUrl != null && !currentUrl.isEmpty()) {
                mVideoView.setUp(currentUrl, false, "");
                if (currentPosition > 0) {
                    mVideoView.setSeekOnStart(currentPosition);
                }
                if (wasPlaying) {
                    mVideoView.startPlayLogic();
                }
            }
        }
        
        // 提示用户
        showToast("播放核心已切换为 " + getEngineName(engine));
    }
    
    /**
     * 获取播放核心名称
     */
    private String getEngineName(String engine) {
        switch (engine) {
            case PlayerConstants.ENGINE_IJK:
                return "IJK";
            case PlayerConstants.ENGINE_EXO:
                return "EXO";
            case PlayerConstants.ENGINE_ALI:
                return "阿里云";
            case PlayerConstants.ENGINE_DEFAULT:
            default:
                return "系统核心";
        }
    }
    
    /**
     * 检查类是否存在
     */
    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 显示定时关闭对话框
     */
    private void showTimerCloseDialog() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        mController.hide();
        
        // 创建对话框
        View dialogView = View.inflate(mActivity, R.layout.timer_dialog, null);
        
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 设置定时关闭选项
        setupTimerOptions(dialogView, dialog);
    }
    
    /**
     * 设置定时关闭选项
     */
    private void setupTimerOptions(View dialogView, AlertDialog dialog) {
        android.widget.LinearLayout countdownLayout = dialogView.findViewById(R.id.line1);
        android.widget.LinearLayout optionsLayout = dialogView.findViewById(R.id.line2);
        android.widget.TextView timesText = dialogView.findViewById(R.id.times);
        android.widget.TextView cancelBtn = dialogView.findViewById(R.id.cancel);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        
        // 检查是否有正在运行的定时器
        if (mTimerRunning) {
            // 显示倒计时
            if (countdownLayout != null) countdownLayout.setVisibility(View.VISIBLE);
            if (optionsLayout != null) optionsLayout.setVisibility(View.GONE);
            
            // 更新倒计时显示
            if (timesText != null) {
                timesText.setText(formatTime(mRemainingTime));
            }
            
            // 取消按钮
            if (cancelBtn != null) {
                cancelBtn.setOnClickListener(v -> {
                    cancelTimer();
                    if (countdownLayout != null) countdownLayout.setVisibility(View.GONE);
                    if (optionsLayout != null) optionsLayout.setVisibility(View.VISIBLE);
                });
            }
        } else {
            // 显示选项列表
            if (countdownLayout != null) countdownLayout.setVisibility(View.GONE);
            if (optionsLayout != null) optionsLayout.setVisibility(View.VISIBLE);
            
            // 设置定时选项
            if (recyclerView != null) {
                ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
                String[] options = {"30分钟", "60分钟", "90分钟", "120分钟"};
                for (String option : options) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("name", option);
                    arrayList.add(map);
                }
                
                OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
                orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
                orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
                    (holder, data, position) -> {
                        android.widget.TextView title = holder.itemView.findViewById(R.id.title);
                        String optionText = data.get(position).get("name").toString();
                        title.setText(optionText);
                        title.setTextColor(COLOR_NORMAL);
                        
                        title.setOnClickListener(v -> {
                            int minutes = Integer.parseInt(optionText.replace("分钟", ""));
                            startTimer(minutes * 60 * 1000);
                            dialog.dismiss();
                            showToast("定时关闭: " + optionText);
                        });
                    });
            }
        }
    }
    
    // 定时器相关变量
    private boolean mTimerRunning = false;
    private long mRemainingTime = 0;
    private android.os.Handler mTimerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable mTimerRunnable;
    
    /**
     * 启动定时器
     */
    private void startTimer(long milliseconds) {
        mTimerRunning = true;
        mRemainingTime = milliseconds;
        
        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (mRemainingTime > 0) {
                    mRemainingTime -= 1000;
                    mTimerHandler.postDelayed(this, 1000);
                } else {
                    // 定时结束，关闭播放器
                    mTimerRunning = false;
                    if (mVideoView != null) {
                        mVideoView.pause();
                    }
                    if (mActivity != null) {
                        mActivity.finish();
                    }
                }
            }
        };
        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }
    
    /**
     * 取消定时器
     */
    private void cancelTimer() {
        mTimerRunning = false;
        mRemainingTime = 0;
        if (mTimerRunnable != null) {
            mTimerHandler.removeCallbacks(mTimerRunnable);
        }
        showToast("定时关闭已取消");
    }
    
    /**
     * 格式化时间显示
     */
    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * 小窗播放功能
     */
    private void onSmallWindowPlayClick() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                // 记录当前播放位置
                long currentPos = mVideoView.getCurrentPositionWhenPlaying();
                // 保存播放位置到 SharedPreferences（用于 Activity 重建后恢复）
                android.content.SharedPreferences prefs = mActivity.getSharedPreferences("pip_prefs", android.content.Context.MODE_PRIVATE);
                prefs.edit()
                    .putBoolean("pip_active", true)
                    .putString("pip_url", mVideoView.getUrl())
                    .putLong("pip_position", currentPos)
                    .apply();
                // 设置正在进入 PiP 模式的标志
                // 这样 onPause 中可以检测到并跳过暂停操作
                mVideoView.setEnteringPiPMode(true);
                // 进入小窗模式
                mActivity.enterPictureInPictureMode();
            } catch (Exception e) {
                mVideoView.setEnteringPiPMode(false);
                showToast("进入小窗模式失败");
            }
        } else {
            showToast("您的设备不支持小窗播放");
        }
    }
    
    /**
     * 进度条开关功能
     */
    private void onProgressBarClick(android.widget.ImageView progressBarIcon) {
        boolean currentState = mSettingsManager.isBottomProgressEnabled();
        boolean newState = !currentState;
        
        // 保存设置
        mSettingsManager.setBottomProgressEnabled(newState);
        
        // 更新 VodControlView 的进度条显示
        VodControlView.setBottomProgress(newState);
        
        // 更新图标
        if (progressBarIcon != null) {
            progressBarIcon.setImageResource(newState ? R.mipmap.kg2 : R.mipmap.kg1);
        }
        
        showToast(newState ? "底部进度条已开启" : "底部进度条已关闭");
    }
    
    /**
     * 下载视频功能
     */
    private void onDownloadVideoClick() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        // 获取当前视频URL和标题
        String url = mVideoView.getUrl();
        String title = mController.getVideoTitle();
        
        if (url == null || url.isEmpty()) {
            showToast("无法获取视频地址");
            return;
        }
        
        // 通知外部处理下载
        if (mOnDownloadClickListener != null) {
            mOnDownloadClickListener.onDownloadClick(url, title);
        } else {
            showToast("下载功能未配置");
        }
    }
    
    // 下载点击监听器
    private OnDownloadClickListener mOnDownloadClickListener;
    
    /**
     * 设置下载点击监听器
     */
    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        mOnDownloadClickListener = listener;
    }
    
    /**
     * 下载点击监听器接口
     */
    public interface OnDownloadClickListener {
        void onDownloadClick(String url, String title);
    }
    
    // ==================== 截图功能 ====================
    
    /**
     * 截图按钮点击事件
     */
    private void onScreenshotClick() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        // 使用 ScreenshotManager 进行截图
        com.orange.playerlibrary.screenshot.ScreenshotManager screenshotManager = 
            new com.orange.playerlibrary.screenshot.ScreenshotManager(mContext, mVideoView);
        
        // 截图并保存到相册
        screenshotManager.takeAndSave(true, new com.orange.playerlibrary.screenshot.ScreenshotManager.SaveCallback() {
            @Override
            public void onSuccess(String filePath) {
                showToast("截图已保存");
            }
            
            @Override
            public void onError(String message) {
                showToast(message);
            }
        });
    }
    
    /**
     * 设置音量控制
     */
    private void setupVolumeControl(android.widget.SeekBar volumeSeekBar, android.widget.TextView volumeText) {
        if (volumeSeekBar == null) return;
        
        // 获取系统音量管理器
        android.media.AudioManager audioManager = (android.media.AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        
        // 获取当前音量和最大音量
        int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        
        // 设置进度条最大值和当前值
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);
        
        // 更新音量文本
        if (volumeText != null) {
            int percent = (int) ((currentVolume * 100.0f) / maxVolume);
            volumeText.setText(percent + "%");
        }
        
        // 设置进度条监听器
        volumeSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // 设置系统音量
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, progress, 0);
                    
                    // 更新音量文本
                    if (volumeText != null) {
                        int percent = (int) ((progress * 100.0f) / maxVolume);
                        volumeText.setText(percent + "%");
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // 不需要处理
            }
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                // 不需要处理
            }
        });
    }
    
    /**
     * 设置播放模式选项
     */
    private void setupPlayModeButtons(android.widget.TextView sequentialBtn, 
                                     android.widget.TextView singleLoopBtn, 
                                     android.widget.TextView playPauseBtn) {
        if (sequentialBtn == null || singleLoopBtn == null || playPauseBtn == null) {
            return;
        }
        
        // 获取当前播放模式
        String currentMode = getPlayMode();
        if (currentMode == null || currentMode.isEmpty()) {
            currentMode = "sequential";
        }
        
        // 高亮当前模式
        sequentialBtn.setTextColor("sequential".equals(currentMode) ?
                COLOR_HIGHLIGHT : COLOR_NORMAL);
        singleLoopBtn.setTextColor("single_loop".equals(currentMode) ?
                COLOR_HIGHLIGHT : COLOR_NORMAL);
        playPauseBtn.setTextColor("play_pause".equals(currentMode) ?
                COLOR_HIGHLIGHT : COLOR_NORMAL);
        
        // 绑定点击事件
        sequentialBtn.setOnClickListener(v -> setPlayMode("sequential"));
        singleLoopBtn.setOnClickListener(v -> setPlayMode("single_loop"));
        playPauseBtn.setOnClickListener(v -> setPlayMode("play_pause"));
    }
    
    /**
     * 显示画面比例对话
     */
    private void showScreenScaleDialog() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        mController.hide();
        
        // 创建对话框
        View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
        
        // 始终显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 设置画面比例选项
        setupScreenScaleOptions(dialogView, dialog);
    }
    
    /**
     * 设置画面比例选项
     */
    private void setupScreenScaleOptions(View dialogView, AlertDialog dialog) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView == null) return;
        
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        String[] scales = {"默认", "16:9", "4:3", "全屏裁剪", "全屏拉伸"};
        for (String scale : scales) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", scale);
            arrayList.add(map);
        }
        
        // 当前选中的比
        final String currentScale = mSettingsManager.getVideoScale();
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
            (holder, data, position) -> {
                android.widget.TextView scaleName = holder.itemView.findViewById(R.id.title);
                String scaleText = data.get(position).get("name").toString();
                
                // 高亮当前比例
                if (scaleText.equals(currentScale)) {
                    scaleName.setTextColor(COLOR_HIGHLIGHT);
                    scaleName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                } else {
                    scaleName.setTextColor(COLOR_NORMAL);
                    scaleName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                }
                
                scaleName.setText(scaleText);
                
                // 比例选择事件
                scaleName.setOnClickListener(v -> {
                    mSettingsManager.setVideoScale(scaleText);
                    setScreenScaleType(scaleText);
                    showToast("画面比例: " + scaleText);
                    dialog.dismiss();
                });
            });
    }
    
    /**
     * 设置画面比例类型
     */
    private void setScreenScaleType(String scaleType) {
        // 保存设置并立即应用
        VideoScaleManager scaleManager = mVideoView.getVideoScaleManager();
        if (scaleManager != null) {
            scaleManager.setAndSaveScale(scaleType);
            showToast("画面比例: " + scaleType + " 已应用");
        } else {
            // 如果 VideoScaleManager 未初始化，只保存设置
            mSettingsManager.setVideoScale(scaleType);
            showToast("画面比例: " + scaleType + "\n将在下次播放时生效");
        }
    }
    
    /**
     * 显示长按倍速对话框
     */
    private void showLongPressSpeedDialog() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        mController.hide();
        
        // 创建对话框
        View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
        
        // 始终显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 设置长按倍速选项
        setupLongPressSpeedOptions(dialogView, dialog);
    }
    
    /**
     * 设置长按倍速选项
     */
    private void setupLongPressSpeedOptions(View dialogView, AlertDialog dialog) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView == null) return;
        
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        String[] speeds = {"2.0x", "3.0x", "3.5x", "4.0x", "4.5x", "5.0x", "5.5x", "6.0x"};
        for (String speed : speeds) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", speed);
            arrayList.add(map);
        }
        
        // 当前长按倍
        final float currentSpeed = mLongPressSpeed;
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
            (holder, data, position) -> {
                android.widget.TextView speedName = holder.itemView.findViewById(R.id.title);
                String speedText = data.get(position).get("name").toString();
                float speedValue = Float.parseFloat(speedText.replace("x", ""));
                
                // 高亮当前倍
                if (Math.abs(speedValue - currentSpeed) < 0.01f) {
                    speedName.setTextColor(COLOR_HIGHLIGHT);
                    speedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                } else {
                    speedName.setTextColor(COLOR_NORMAL);
                    speedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                }
                
                speedName.setText(speedText);
                
                // 倍速选择事件
                speedName.setOnClickListener(v -> {
                    setLongPressSpeed(speedValue);
                    showToast("长按倍 " + speedText);
                    dialog.dismiss();
                });
            });
    }
    
    /**
     * 显示播放模式对话
     */
    private void showPlayModeDialog() {
        final String[] modes = {"顺序播放", "单集循环", "播放暂停"};
        final String[] modeValues = {"sequential", "single_loop", "play_pause"};
        
        // 获取当前播放模式
        String currentMode = getPlayMode();
        int checkedItem = 0;
        for (int i = 0; i < modeValues.length; i++) {
            if (modeValues[i].equals(currentMode)) {
                checkedItem = i;
                break;
            }
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
        builder.setTitle("播放模式");
        builder.setSingleChoiceItems(modes, checkedItem, (dialog, which) -> {
            setPlayMode(modeValues[which]);
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示播放引擎对话框
     */
    private void showEngineDialog() {
        final String[] engines = {"系统播放器", "IJK播放器", "EXO播放器"};
        final String[] engineValues = {
            PlayerConstants.ENGINE_DEFAULT,
            PlayerConstants.ENGINE_IJK,
            PlayerConstants.ENGINE_EXO
        };
        
        // 获取当前引擎
        String currentEngine = mSettingsManager.getPlayerEngine();
        int checkedItem = 0;
        for (int i = 0; i < engineValues.length; i++) {
            if (engineValues[i].equals(currentEngine)) {
                checkedItem = i;
                break;
            }
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
        builder.setTitle("播放引擎");
        builder.setSingleChoiceItems(engines, checkedItem, (dialog, which) -> {
            mSettingsManager.setPlayerEngine(engineValues[which]);
            showToast("播放引擎: " + engines[which] + "\n重新播放生效");
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 显示跳过片头片尾弹窗（使用skip_dialog_full布局）
     */
    public void showSkipDialog() {
        mController.hide();
        
        // 创建对话框视图
        View dialogView = View.inflate(mActivity, R.layout.skip_dialog_full, null);
        
        // 创建对话框 - 显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 获取对话框的根视图（DecorView）并设置触摸监听
        View decorView = dialog.getWindow().getDecorView();
        View shik = dialogView.findViewById(R.id.shik);
        
        decorView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (shik != null) {
                    // 获取触摸位置
                    float x = event.getRawX();
                    float y = event.getRawY();
                    
                    // 获取内容区域的位置
                    int[] location = new int[2];
                    shik.getLocationOnScreen(location);
                    int left = location[0];
                    int top = location[1];
                    int right = left + shik.getWidth();
                    int bottom = top + shik.getHeight();
                    
                    // 如果点击在内容区域外，关闭对话框
                    if (x < left || x > right || y < top || y > bottom) {
                        dialog.dismiss();
                        return true;
                    }
                }
            }
            return false;
        });
        
        // 绑定跳过片头片尾的SeekBar
        bindSkipSeekBars(dialogView, dialog);
    }
    
    /**
     * 绑定跳过片头片尾的SeekBar
     */
    private void bindSkipSeekBars(View dialogView, AlertDialog dialog) {
        // 获取片头SeekBar和文本
        android.widget.SeekBar seekBarPt = dialogView.findViewById(R.id.seekBarpt);
        android.widget.TextView namePt = dialogView.findViewById(R.id.name);
        
        // 获取片尾SeekBar和文本
        android.widget.SeekBar seekBarPw = dialogView.findViewById(R.id.seekBarpw);
        android.widget.TextView namePw = dialogView.findViewById(R.id.wb2);
        
        // 获取当前设置值
        int skipOpening = mSettingsManager.getSkipOpening();
        int skipEnding = mSettingsManager.getSkipEnding();
        
        // 设置片头SeekBar
        if (seekBarPt != null) {
            // 最大值设为180秒（3分钟）
            seekBarPt.setMax(180000);
            seekBarPt.setProgress(skipOpening);
            
            if (namePt != null) {
                namePt.setText(formatSkipTime(skipOpening));
            }
            
            seekBarPt.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && namePt != null) {
                        namePt.setText(formatSkipTime(progress));
                    }
                }
                
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    mSettingsManager.setSkipOpening(progress);
                    showToast("跳过片头: " + formatSkipTime(progress));
                }
            });
        }
        
        // 设置片尾SeekBar
        if (seekBarPw != null) {
            // 最大值设为180秒（3分钟）
            seekBarPw.setMax(180000);
            seekBarPw.setProgress(skipEnding);
            
            if (namePw != null) {
                namePw.setText(formatSkipTime(skipEnding));
            }
            
            seekBarPw.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && namePw != null) {
                        namePw.setText(formatSkipTime(progress));
                    }
                }
                
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    mSettingsManager.setSkipEnding(progress);
                    showToast("跳过片尾: " + formatSkipTime(progress));
                }
            });
        }
    }
    
    /**
     * 格式化跳过时间显示
     */
    private String formatSkipTime(int milliseconds) {
        if (milliseconds <= 0) {
            return "不跳过";
        }
        int seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + "分钟";
            } else {
                return minutes + "分" + remainingSeconds + "秒";
            }
        }
    }
    
    /**
     * 显示跳过片头对话框
     */
    private void showSkipOpeningDialog() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        mController.hide();
        
        // 创建对话框
        View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
        
        // 始终显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 设置跳过片头选项
        setupSkipOpeningOptions(dialogView, dialog);
    }
    
    /**
     * 设置跳过片头选项
     */
    private void setupSkipOpeningOptions(View dialogView, AlertDialog dialog) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView == null) return;
        
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        String[] options = {"不跳过", "15秒", "30秒", "60秒", "90秒"};
        for (String option : options) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", option);
            arrayList.add(map);
        }
        
        // 当前设置
        final int currentValue = mSettingsManager.getSkipOpening();
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
            (holder, data, position) -> {
                android.widget.TextView title = holder.itemView.findViewById(R.id.title);
                String optionText = data.get(position).get("name").toString();
                
                // 计算秒数
                int seconds = 0;
                if (!optionText.equals("不跳过")) {
                    seconds = Integer.parseInt(optionText.replace("秒", "")) * 1000;
                }
                
                // 高亮当前选项
                if (seconds == currentValue) {
                    title.setTextColor(COLOR_HIGHLIGHT);
                    title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                } else {
                    title.setTextColor(COLOR_NORMAL);
                    title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                }
                
                title.setText(optionText);
                
                // 设置点击事件
                final int finalSeconds = seconds;
                title.setOnClickListener(v -> {
                    mSettingsManager.setSkipOpening(finalSeconds);
                    showToast("跳过片头: " + optionText);
                    dialog.dismiss();
                });
            });
    }
    
    /**
     * 显示跳过片尾对话框
     */
    private void showSkipEndingDialog() {
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        mController.hide();
        
        // 创建对话框
        View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
        
        // 始终显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 点击外部区域关闭对话框
        View layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> dialog.dismiss());
        }
        
        // 设置跳过片尾选项
        setupSkipEndingOptions(dialogView, dialog);
    }
    
    /**
     * 设置跳过片尾选项
     */
    private void setupSkipEndingOptions(View dialogView, AlertDialog dialog) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView == null) return;
        
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        String[] options = {"不跳过", "15秒", "30秒", "60秒", "90秒"};
        for (String option : options) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", option);
            arrayList.add(map);
        }
        
        // 当前设置
        final int currentValue = mSettingsManager.getSkipEnding();
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
            (holder, data, position) -> {
                android.widget.TextView title = holder.itemView.findViewById(R.id.title);
                String optionText = data.get(position).get("name").toString();
                
                // 计算秒数
                int seconds = 0;
                if (!optionText.equals("不跳过")) {
                    seconds = Integer.parseInt(optionText.replace("秒", "")) * 1000;
                }
                
                // 高亮当前选项
                if (seconds == currentValue) {
                    title.setTextColor(COLOR_HIGHLIGHT);
                    title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                } else {
                    title.setTextColor(COLOR_NORMAL);
                    title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                }
                
                title.setText(optionText);
                
                // 设置点击事件
                final int finalSeconds = seconds;
                title.setOnClickListener(v -> {
                    mSettingsManager.setSkipEnding(finalSeconds);
                    showToast("跳过片尾: " + optionText);
                    dialog.dismiss();
                });
            });
    }
    
    // ==================== 选集功能 ====================
    
    // 排序状态
    private boolean mIsSortAscending = true; // 默认正序
    private boolean mIsSmartMode = false; // 默认模式
    
    /**
     * 显示选集列表
     */
    public void showPlaylistDialog() {
        final ArrayList<HashMap<String, Object>> originalList = mController.getVideoList();
        if (originalList == null || originalList.isEmpty()) {
            showToast("暂无选集");
            return;
        }
        
        mController.hide();
        
        try {
            // 创建对话框视图
            View dialogView = View.inflate(mActivity, R.layout.playliset, null);
            
            final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView, 
                    DialogUtils.DialogPosition.RIGHT, null, null);
            
            // 点击外部区域关闭对话框
            View playListView = dialogView.findViewById(R.id.playLiset_v);
            if (playListView != null) {
                playListView.setOnClickListener(v -> dialog.dismiss());
            }
            
            // 获取排序和模式按钮
            TextView sortBtn = dialogView.findViewById(R.id.shorts);
            TextView modeBtn = dialogView.findViewById(R.id.mode);
            
            // 初始化按钮状态
            if (sortBtn != null) sortBtn.setText(mIsSortAscending ? "正序" : "倒序");
            if (modeBtn != null) modeBtn.setText(mIsSmartMode ? "智能" : "默认");
            
            // 创建用于显示的列表副本
            final ArrayList<HashMap<String, Object>> displayList = new ArrayList<>(originalList);
            
            // 模式按钮点击事件
            if (modeBtn != null) {
                final View finalDialogView = dialogView;
                modeBtn.setOnClickListener(v -> {
                    mIsSmartMode = !mIsSmartMode;
                    modeBtn.setText(mIsSmartMode ? "智能" : "默认");
                    
                    // 重置显示列表
                    displayList.clear();
                    displayList.addAll(originalList);
                    
                    if (mIsSmartMode && !mIsSortAscending) {
                        sortVideoList(displayList);
                    } else if (!mIsSmartMode && !mIsSortAscending) {
                        java.util.Collections.reverse(displayList);
                    }
                    
                    refreshPlaylistRecyclerView(finalDialogView, dialog, displayList);
                });
            }
            
            // 排序按钮点击事件
            if (sortBtn != null) {
                final View finalDialogView2 = dialogView;
                sortBtn.setOnClickListener(v -> {
                    mIsSortAscending = !mIsSortAscending;
                    sortBtn.setText(mIsSortAscending ? "正序" : "倒序");
                    
                    if (mIsSmartMode) {
                        sortVideoList(displayList);
                    } else {
                        displayList.clear();
                        displayList.addAll(originalList);
                        if (!mIsSortAscending) {
                            java.util.Collections.reverse(displayList);
                        }
                    }
                    
                    refreshPlaylistRecyclerView(finalDialogView2, dialog, displayList);
                });
            }
            
            // 初始显示
            if (mIsSmartMode) {
                sortVideoList(displayList);
            } else if (!mIsSortAscending) {
                java.util.Collections.reverse(displayList);
            }
            
            refreshPlaylistRecyclerView(dialogView, dialog, displayList);
            
        } catch (Exception e) {
        }
    }
    
    /**
     * 刷新选集列表
     */
    private void refreshPlaylistRecyclerView(View dialogView, AlertDialog dialog, ArrayList<HashMap<String, Object>> dataList) {
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView == null) return;
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.playliset_item, dataList,
            (holder, data, position) -> bindPlaylistItemView(holder, data, position, dialog));
    }
    
    /**
     * 绑定选集列表项
     */
    private void bindPlaylistItemView(OrangeRecyclerViewAdapter.ViewHolder holder,
                                      ArrayList<HashMap<String, Object>> dataList,
                                      int position, AlertDialog dialog) {
        HashMap<String, Object> itemData = dataList.get(position);
        android.widget.TextView titleTv = holder.itemView.findViewById(R.id.title);
        
        // 绑定标题
        String title = itemData.get("name") != null ? itemData.get("name").toString() : "第" + (position + 1) + "集";
        titleTv.setText(title);
        
        // 高亮当前播放集数
        String currentUrl = mVideoView.getUrl();
        String itemUrl = itemData.get("url") != null ? itemData.get("url").toString() : "";
        if (currentUrl != null && currentUrl.equals(itemUrl)) {
            titleTv.setTextColor(COLOR_HIGHLIGHT);
            titleTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        } else {
            titleTv.setTextColor(COLOR_NORMAL);
            titleTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        }
        
        // 点击事件
        titleTv.setOnClickListener(v -> {
            playEpisodeFromList(itemData);
            dialog.dismiss();
        });
    }
    
    /**
     * 从列表数据播放指定集数
     */
    private void playEpisodeFromList(HashMap<String, Object> item) {
        String url = item.get("url") != null ? item.get("url").toString() : "";
        String name = item.get("name") != null ? item.get("name").toString() : "";
        
        if (url.isEmpty()) {
            showToast("播放地址异常");
            return;
        }
        
        // 设置标题
        String isIndependent = item.get("dlsp") != null ? item.get("dlsp").toString() : "";
        if ("独立".equals(isIndependent)) {
            mController.setVideoTitle(name);
        } else {
            String currentTitle = mController.getVideoTitle();
            if (currentTitle != null && !currentTitle.isEmpty()) {
                mController.setTitle(currentTitle + " - " + name);
            } else {
                mController.setVideoTitle(name);
            }
        }
        
        // 播放视频
        @SuppressWarnings("unchecked")
        HashMap<String, String> headers = (HashMap<String, String>) item.get("headers");
        mVideoView.setUrl(url, headers);
        mVideoView.release();
        mVideoView.startPlayLogic();
    }
    
    /**
     * 视频列表排序（智能排序）
     */
    private void sortVideoList(ArrayList<HashMap<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) return;
        
        java.util.Collections.sort(dataList, (map1, map2) -> {
            String name1 = map1.get("name") != null ? map1.get("name").toString() : "";
            String name2 = map2.get("name") != null ? map2.get("name").toString() : "";
            
            Integer num1 = extractNumber(name1);
            Integer num2 = extractNumber(name2);
            
            if (num1 != null && num2 != null) {
                int numCompare = Integer.compare(num1, num2);
                return mIsSortAscending ? numCompare : -numCompare;
            } else if (num1 != null) {
                return -1;
            } else if (num2 != null) {
                return 1;
            } else {
                int strCompare = name1.compareTo(name2);
                return mIsSortAscending ? strCompare : -strCompare;
            }
        });
    }
    
    /**
     * 从字符串中提取数字
     */
    private Integer extractNumber(String str) {
        if (str == null || str.isEmpty()) return null;
        
        // 尝试匹配阿拉伯数字
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(str);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        return null;
    }
    
    /**
     * 播放指定集数
     */
    private void playEpisode(int index) {
        ArrayList<HashMap<String, Object>> videoList = mController.getVideoList();
        if (videoList == null || index < 0 || index >= videoList.size()) {
            return;
        }
        
        HashMap<String, Object> item = videoList.get(index);
        String url = item.get("url") != null ? item.get("url").toString() : "";
        String name = item.get("name") != null ? item.get("name").toString() : "";
        
        if (url.isEmpty()) {
            showToast("播放地址异常");
            return;
        }
        
        // 设置标题
        String isIndependent = item.get("dlsp") != null ? item.get("dlsp").toString() : "";
        if ("独立".equals(isIndependent)) {
            mController.setVideoTitle(name);
        } else {
            String currentTitle = mController.getVideoTitle();
            if (currentTitle != null && !currentTitle.isEmpty()) {
                mController.setTitle(currentTitle + " - " + name);
            } else {
                mController.setVideoTitle(name);
            }
        }
        
        // 播放视频
        @SuppressWarnings("unchecked")
        HashMap<String, String> headers = (HashMap<String, String>) item.get("headers");
        mVideoView.setUrl(url, headers);
        mVideoView.release();
        mVideoView.startPlayLogic();
    }
    
    /**
     * 播放下一集
     */
    public void playNextEpisode() {
        ArrayList<HashMap<String, Object>> videoList = mController.getVideoList();
        if (videoList == null || videoList.isEmpty()) {
            return;
        }
        
        String currentUrl = mVideoView.getUrl();
        int currentIndex = -1;
        
        for (int i = 0; i < videoList.size(); i++) {
            String url = videoList.get(i).get("url") != null ? videoList.get(i).get("url").toString() : "";
            if (url.equals(currentUrl)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex >= 0 && currentIndex < videoList.size() - 1) {
            playEpisode(currentIndex + 1);
        } else {
            showToast("已经是最后一集了");
        }
    }
    
    /**
     * 检查是否有下一集
     */
    public boolean hasNextEpisode() {
        ArrayList<HashMap<String, Object>> videoList = mController.getVideoList();
        if (videoList == null || videoList.isEmpty()) {
            return false;
        }
        
        String currentUrl = mVideoView.getUrl();
        int currentIndex = -1;
        
        for (int i = 0; i < videoList.size(); i++) {
            String url = videoList.get(i).get("url") != null ? videoList.get(i).get("url").toString() : "";
            if (url.equals(currentUrl)) {
                currentIndex = i;
                break;
            }
        }
        
        return currentIndex >= 0 && currentIndex < videoList.size() - 1;
    }
    
    // ==================== 播放完成处理 ====================
    
    /**
     * 处理播放完成事件
     */
    public void handlePlaybackCompleted() {
        String currentMode = getPlayMode();
        
        switch (currentMode) {
            case "sequential": // 顺序播放
                if (hasNextEpisode()) {
                    playNextEpisode();
                }
                break;
                
            case "single_loop": // 单集循环
                // 重新播放当前视频
                mVideoView.seekTo(0);
                mVideoView.startPlayLogic();
                break;
                
            case "play_pause": // 播放暂停
                // 不做任何操作
                break;
        }
    }
    
    // ==================== 弹幕功能 ====================
    
    /**
     * 切换弹幕开关
     * @param clickedView 被点击的View，用于找到正确的VodControlView
     */
    private void toggleDanmaku(View clickedView) {
        // 检查弹幕库是否可用
        if (!DanmakuHelper.isDanmakuLibraryAvailable()) {
            DanmakuHelper.showDanmakuNotAvailableToast(mContext);
            return;
        }
        boolean currentState = mSettingsManager.isDanmakuEnabled();
        boolean newState = !currentState;
        // 保存设置
        mSettingsManager.setDanmakuEnabled(newState);
        // 从点击的View向上找到VodControlView
        VodControlView actualVodControlView = findParentVodControlView(clickedView);
        if (actualVodControlView != null) {
            actualVodControlView.updateDanmakuToggleState(newState);
        } else {
        }
        
        // 通知外部监听器（如果有DanmaView组件）
        if (mOnDanmakuStateChangeListener != null) {
            mOnDanmakuStateChangeListener.onDanmakuStateChanged(newState);
        }
        
        // 通知弹幕控制器
        if (mController != null && mController.getDanmakuController() != null) {
            mController.getDanmakuController().setDanmakuEnabled(newState);
        }
        
        showToast(newState ? "弹幕已开启" : "弹幕已关闭");
    }
    
    /**
     * 从View向上遍历找到父VodControlView
     */
    private VodControlView findParentVodControlView(View view) {
        if (view == null) return null;
        
        android.view.ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof VodControlView) {
                return (VodControlView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * 获取当前实际显示的VodControlView
     * 如果是全屏模式，返回全屏播放器的VodControlView
     */
    private VodControlView getActualVodControlView() {
        // 首先尝试获取全屏播放器的VodControlView
        if (mActivity != null) {
            android.view.ViewGroup vp = (android.view.ViewGroup) mActivity.findViewById(android.view.Window.ID_ANDROID_CONTENT);
            if (vp != null) {
                android.view.View fullView = vp.findViewById(com.shuyu.gsyvideoplayer.GSYVideoManager.FULLSCREEN_ID);
                if (fullView instanceof OrangevideoView) {
                    OrangevideoView fullPlayer = (OrangevideoView) fullView;
                    VodControlView fullVodControlView = fullPlayer.getVodControlView();
                    if (fullVodControlView != null) {
                        return fullVodControlView;
                    }
                }
            }
        }
        
        // 如果没有全屏播放器，返回当前绑定的VodControlView
        return mVodControlView;
    }
    
    /**
     * 显示弹幕输入对话框
     */
    private void showDanmakuInputDialog() {
        // 检查弹幕库是否可用
        if (!DanmakuHelper.isDanmakuLibraryAvailable()) {
            DanmakuHelper.showDanmakuNotAvailableToast(mContext);
            return;
        }
        
        // 使用DanmuexitDialog显示弹幕发送界面
        com.orange.playerlibrary.tool.DanmuexitDialog danmuDialog = 
            new com.orange.playerlibrary.tool.DanmuexitDialog();
        
        // 设置发送监听器
        com.orange.playerlibrary.tool.DanmuexitDialog.setDanmuSendListener((text, color) -> {
            // 通知外部发送弹幕
            if (mOnDanmakuSendListener != null) {
                mOnDanmakuSendListener.onDanmakuSend(text, color);
            }
            
            // 通知弹幕控制器发送弹幕
            if (mController != null && mController.getDanmakuController() != null) {
                mController.getDanmakuController().sendDanmaku(text, color);
            }
            
            showToast("弹幕已发送");
        });
        
        // 显示对话框
        danmuDialog.show(mActivity);
    }
    
    /**
     * 显示弹幕设置对话框
     */
    private void showDanmakuSettingsDialog() {
        // 检查弹幕库是否可用
        if (!DanmakuHelper.isDanmakuLibraryAvailable()) {
            DanmakuHelper.showDanmakuNotAvailableToast(mContext);
            return;
        }
        
        mController.hide(); // 隐藏播放器UI
        
        // 创建对话框视图
        View dialogView = View.inflate(mActivity, R.layout.danmuset_dialog_full, null);
        
        // 创建对话框 - 显示在右侧
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 在 DecorView 上设置触摸监听，点击空白区域关闭对话框
        View decorView = dialog.getWindow().getDecorView();
        View shik = dialogView.findViewById(R.id.shik);
        
        decorView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (shik != null) {
                    float x = event.getRawX();
                    float y = event.getRawY();
                    
                    int[] location = new int[2];
                    shik.getLocationOnScreen(location);
                    int left = location[0];
                    int top = location[1];
                    int right = left + shik.getWidth();
                    int bottom = top + shik.getHeight();
                    
                    if (x < left || x > right || y < top || y > bottom) {
                        dialog.dismiss();
                        return true;
                    }
                }
            }
            return false;
        });
        
        // 绑定弹幕设置选项
        bindDanmakuSettings(dialogView, dialog);
    }
    
    /**
     * 绑定弹幕设置选项
     */
    private void bindDanmakuSettings(View dialogView, AlertDialog dialog) {
        // 获取SeekBar控件
        android.widget.SeekBar sizeBar = dialogView.findViewById(R.id.ai_sizebar);
        android.widget.SeekBar speedBar = dialogView.findViewById(R.id.ai_speedbar);
        android.widget.SeekBar alphaBar = dialogView.findViewById(R.id.ai_alphabar);
        
        android.widget.TextView sizeText = dialogView.findViewById(R.id.ai_size);
        android.widget.TextView speedText = dialogView.findViewById(R.id.ai_speed);
        android.widget.TextView alphaText = dialogView.findViewById(R.id.ai_alpha);
        
        // 获取当前设置
        float currentSize = mSettingsManager.getDanmakuTextSize();
        float currentSpeed = mSettingsManager.getDanmakuSpeed();
        float currentAlpha = mSettingsManager.getDanmakuAlpha();
        
        // 设置初始值（范围：文字大小10-30sp，速度0.5-3.0倍，透明度0-100%）
        if (sizeBar != null) {
            int progress = (int) ((currentSize - 10) / 20 * 100);
            sizeBar.setProgress(progress);
            if (sizeText != null) {
                sizeText.setText("弹幕文字大小: " + String.format("%.0f", currentSize) + "sp");
            }
            
            sizeBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float size = 10 + (progress / 100f) * 20; // 10-30sp
                        if (sizeText != null) {
                            sizeText.setText("弹幕文字大小: " + String.format("%.0f", size) + "sp");
                        }
                    }
                }
                
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    float size = 10 + (seekBar.getProgress() / 100f) * 20;
                    mSettingsManager.setDanmakuTextSize(size);
                    if (mOnDanmakuSettingsChangeListener != null) {
                        mOnDanmakuSettingsChangeListener.onTextSizeChanged(size);
                    }
                    // 通知弹幕控制器
                    if (mController != null && mController.getDanmakuController() != null) {
                        mController.getDanmakuController().setDanmakuTextSize(size);
                    }
                    showToast("弹幕文字大小: " + String.format("%.0f", size) + "sp");
                }
            });
        }
        
        if (speedBar != null) {
            int progress = (int) ((currentSpeed - 0.5f) / 2.5f * 100);
            speedBar.setProgress(progress);
            if (speedText != null) {
                speedText.setText("弹幕播放速度: " + String.format("%.1f", currentSpeed) + "x");
            }
            
            speedBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float speed = 0.5f + (progress / 100f) * 2.5f; // 0.5-3.0x
                        if (speedText != null) {
                            speedText.setText("弹幕播放速度: " + String.format("%.1f", speed) + "x");
                        }
                    }
                }
                
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    float speed = 0.5f + (seekBar.getProgress() / 100f) * 2.5f;
                    mSettingsManager.setDanmakuSpeed(speed);
                    if (mOnDanmakuSettingsChangeListener != null) {
                        mOnDanmakuSettingsChangeListener.onSpeedChanged(speed);
                    }
                    // 通知弹幕控制器
                    if (mController != null && mController.getDanmakuController() != null) {
                        mController.getDanmakuController().setDanmakuSpeed(speed);
                    }
                    showToast("弹幕速度: " + String.format("%.1f", speed) + "x");
                }
            });
        }
        
        if (alphaBar != null) {
            int progress = (int) (currentAlpha * 100);
            alphaBar.setProgress(progress);
            if (alphaText != null) {
                alphaText.setText("弹幕透明度: " + progress + "%");
            }
            
            alphaBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && alphaText != null) {
                        alphaText.setText("弹幕透明度: " + progress + "%");
                    }
                }
                
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    float alpha = seekBar.getProgress() / 100f;
                    mSettingsManager.setDanmakuAlpha(alpha);
                    if (mOnDanmakuSettingsChangeListener != null) {
                        mOnDanmakuSettingsChangeListener.onAlphaChanged(alpha);
                    }
                    // 通知弹幕控制器
                    if (mController != null && mController.getDanmakuController() != null) {
                        mController.getDanmakuController().setDanmakuAlpha(alpha);
                    }
                    showToast("弹幕透明度: " + seekBar.getProgress() + "%");
                }
            });
        }
    }
    
    // 弹幕状态变化监听器
    private OnDanmakuStateChangeListener mOnDanmakuStateChangeListener;
    private OnDanmakuSettingsChangeListener mOnDanmakuSettingsChangeListener;
    private OnDanmakuSendListener mOnDanmakuSendListener;
    
    /**
     * 设置弹幕状态变化监听器
     */
    public void setOnDanmakuStateChangeListener(OnDanmakuStateChangeListener listener) {
        mOnDanmakuStateChangeListener = listener;
    }
    
    /**
     * 设置弹幕设置变化监听器
     */
    public void setOnDanmakuSettingsChangeListener(OnDanmakuSettingsChangeListener listener) {
        mOnDanmakuSettingsChangeListener = listener;
    }
    
    /**
     * 设置弹幕发送监听器
     */
    public void setOnDanmakuSendListener(OnDanmakuSendListener listener) {
        mOnDanmakuSendListener = listener;
    }
    
    /**
     * 弹幕状态变化监听器接口
     */
    public interface OnDanmakuStateChangeListener {
        void onDanmakuStateChanged(boolean enabled);
    }
    
    /**
     * 弹幕设置变化监听器接口
     */
    public interface OnDanmakuSettingsChangeListener {
        void onTextSizeChanged(float size);
        void onSpeedChanged(float speed);
        void onAlphaChanged(float alpha);
    }
    
    /**
     * 弹幕发送监听器接口
     */
    public interface OnDanmakuSendListener {
        void onDanmakuSend(String text, int color);
    }
    
    // ===== 字幕功能 =====
    
    /**
     * 显示字幕对话框
     * 按照 steering rules，从点击的 View 向上遍历找到正确的父组件
     */
    private void showSubtitleDialog(View clickedView) {
        mController.hide();
        
        // 从点击的 View 找到实际的 VodControlView（全屏模式下需要）
        VodControlView actualVodControlView = findParentVodControlView(clickedView);
        
        try {
            View dialogView = View.inflate(mActivity, R.layout.subtitle_dialog, null);
            
            final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                    DialogUtils.DialogPosition.RIGHT, null, null);
            
            // 点击外部关闭
            View layout = dialogView.findViewById(R.id.layout);
            if (layout != null) {
                layout.setOnClickListener(v -> dialog.dismiss());
            }
            
            // 字幕开关
            android.widget.Switch subtitleSwitch = dialogView.findViewById(R.id.subtitle_switch);
            if (subtitleSwitch != null) {
                subtitleSwitch.setChecked(mController.isSubtitleEnabled());
                subtitleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        mController.getSubtitleManager().show();
                        mController.startSubtitle();
                    } else {
                        mController.getSubtitleManager().hide();
                        mController.stopSubtitle();
                        // 同时停止 OCR 翻译
                        stopOcrTranslate();
                    }
                    // 更新按钮状态
                    if (actualVodControlView != null) {
                        actualVodControlView.updateSubtitleToggleState(isChecked);
                    }
                });
            }
            
            // 加载本地字幕按钮
            View btnLoadLocal = dialogView.findViewById(R.id.btn_load_local);
            if (btnLoadLocal != null) {
                btnLoadLocal.setOnClickListener(v -> {
                    dialog.dismiss();
                    showSubtitleFilePicker();
                });
            }
            
            // 加载网络字幕按钮
            View btnLoadUrl = dialogView.findViewById(R.id.btn_load_url);
            if (btnLoadUrl != null) {
                btnLoadUrl.setOnClickListener(v -> {
                    dialog.dismiss();
                    showSubtitleUrlInput();
                });
            }
            
            // 字幕大小调节
            android.widget.SeekBar sizeBar = dialogView.findViewById(R.id.subtitle_size_bar);
            android.widget.TextView sizeText = dialogView.findViewById(R.id.subtitle_size_text);
            if (sizeBar != null) {
                // 从设置中读取已保存的字幕大小
                float savedSize = mSettingsManager.getSubtitleSize();
                int savedProgress = (int) ((savedSize - 12) / 24 * 100);
                sizeBar.setProgress(Math.max(0, Math.min(100, savedProgress)));
                if (sizeText != null) {
                    sizeText.setText("字幕大小: " + (int) savedSize + "sp");
                }
                
                sizeBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            float size = 12 + (progress / 100f) * 24; // 12-36sp
                            if (sizeText != null) {
                                sizeText.setText("字幕大小: " + (int) size + "sp");
                            }
                        }
                    }
                    
                    @Override
                    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                    
                    @Override
                    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                        float size = 12 + (seekBar.getProgress() / 100f) * 24;
                        mController.getSubtitleManager().setTextSize(size);
                        // 保存字幕大小设置
                        mSettingsManager.setSubtitleSize(size);
                        showToast("字幕大小: " + (int) size + "sp");
                    }
                });
            }
            
            // 显示当前字幕状态
            android.widget.TextView statusText = dialogView.findViewById(R.id.subtitle_status);
            if (statusText != null) {
                if (mController.isSubtitleLoaded()) {
                    int count = mController.getSubtitleManager().getSubtitleCount();
                    statusText.setText("已加载 " + count + " 条字幕");
                } else {
                    statusText.setText("未加载字幕");
                }
            }
            
            // OCR 翻译字幕按钮
            View btnOcrTranslate = dialogView.findViewById(R.id.btn_ocr_translate);
            android.widget.TextView ocrStatus = dialogView.findViewById(R.id.ocr_status);
            if (btnOcrTranslate != null) {
                // 检查 OCR 功能是否可用
                boolean ocrAvailable = com.orange.playerlibrary.ocr.OcrAvailabilityChecker.isTesseractAvailable();
                boolean translateAvailable = com.orange.playerlibrary.ocr.OcrAvailabilityChecker.isMlKitTranslateAvailable();
                
                if (!ocrAvailable || !translateAvailable) {
                    // 功能不可用，显示安装提示
                    if (ocrStatus != null) {
                        ocrStatus.setText("需要安装额外依赖");
                        ocrStatus.setTextColor(0xFFFF6B6B);
                    }
                    ((android.widget.Button) btnOcrTranslate).setText("查看安装说明");
                    btnOcrTranslate.setOnClickListener(v -> {
                        dialog.dismiss();
                        showOcrInstallGuide();
                    });
                } else {
                    // 功能可用
                    if (ocrStatus != null) {
                        ocrStatus.setText("识别视频画面中的硬字幕并翻译");
                    }
                    btnOcrTranslate.setOnClickListener(v -> {
                        dialog.dismiss();
                        showOcrTranslateSettings();
                    });
                }
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * 显示字幕文件选择器
     */
    private void showSubtitleFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            // 支持 .srt, .vtt, .ass, .ssa 字幕格式
            String[] mimeTypes = {
                "application/x-subrip",           // .srt
                "text/vtt",                        // .vtt
                "text/x-ssa",                      // .ssa/.ass
                "application/octet-stream",        // 通用二进制
                "text/plain"                       // 纯文本
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            
            mActivity.startActivityForResult(intent, REQUEST_CODE_SUBTITLE_FILE);
            Log.d(TAG, "启动字幕文件选择器");
        } catch (Exception e) {
            Log.e(TAG, "启动文件选择器失败", e);
            showToast("无法打开文件选择器");
        }
    }
    
    /**
     * 处理字幕文件选择结果
     * 需要在 Activity 的 onActivityResult 中调用此方法
     * 
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data Intent 数据
     * @return 是否处理了该结果
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SUBTITLE_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    Log.d(TAG, "选择的字幕文件: " + uri);
                    loadSubtitleFromUri(uri);
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * 从 Uri 加载字幕
     */
    private void loadSubtitleFromUri(Uri uri) {
        try {
            // 获取持久化读取权限
            mActivity.getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            // 某些 Uri 可能不支持持久化权限，忽略
            Log.w(TAG, "无法获取持久化权限: " + e.getMessage());
        }
        
        if (mController == null || mController.getSubtitleManager() == null) {
            showToast("字幕管理器未初始化");
            return;
        }
        
        showToast("正在加载字幕...");
        
        final String uriString = uri.toString();
        mController.getSubtitleManager().loadSubtitle(uri, new com.orange.playerlibrary.subtitle.SubtitleManager.OnSubtitleLoadListener() {
            @Override
            public void onLoadSuccess(int count) {
                mActivity.runOnUiThread(() -> {
                    showToast("字幕加载成功，共 " + count + " 条");
                    mController.startSubtitle();
                    // 保存本地字幕记忆
                    String videoUrl = mVideoView.getUrl();
                    if (videoUrl != null) {
                        mSettingsManager.setSubtitleLocalForVideo(videoUrl, uriString);
                        // 清除网络字幕记忆（本地优先）
                        mSettingsManager.setSubtitleUrlForVideo(videoUrl, null);
                        Log.d(TAG, "已保存本地字幕记忆: " + uriString);
                    }
                });
            }
            
            @Override
            public void onLoadFailed(String error) {
                mActivity.runOnUiThread(() -> {
                    showToast("字幕加载失败: " + error);
                });
            }
        });
    }
    
    /**
     * 显示字幕 URL 输入对话框（自定义风格）
     */
    private void showSubtitleUrlInput() {
        View dialogView = View.inflate(mActivity, R.layout.dialog_subtitle_url_input, null);
        
        android.widget.EditText etUrl = dialogView.findViewById(R.id.et_subtitle_url);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnLoad = dialogView.findViewById(R.id.btn_load);
        
        final AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setView(dialogView)
                .create();
        
        // 设置对话框背景透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnLoad.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                dialog.dismiss();
                loadSubtitleFromUrl(url);
            } else {
                showToast("请输入字幕 URL");
            }
        });
        
        dialog.show();
    }
    
    /**
     * 从 URL 加载字幕
     */
    private void loadSubtitleFromUrl(String url) {
        showToast("正在加载字幕...");
        mController.loadSubtitle(url, new com.orange.playerlibrary.subtitle.SubtitleManager.OnSubtitleLoadListener() {
            @Override
            public void onLoadSuccess(int count) {
                mActivity.runOnUiThread(() -> {
                    showToast("字幕加载成功，共 " + count + " 条");
                    mController.startSubtitle();
                    // 保存网络字幕记忆
                    String videoUrl = mVideoView.getUrl();
                    if (videoUrl != null) {
                        mSettingsManager.setSubtitleUrlForVideo(videoUrl, url);
                        // 清除本地字幕记忆（网络优先）
                        mSettingsManager.setSubtitleLocalForVideo(videoUrl, null);
                        Log.d(TAG, "已保存网络字幕记忆: " + url);
                    }
                });
            }
            
            @Override
            public void onLoadFailed(String error) {
                mActivity.runOnUiThread(() -> {
                    showToast("字幕加载失败: " + error);
                });
            }
        });
    }
    
    /**
     * 尝试自动加载已记忆的字幕
     * 应在视频开始播放时调用
     */
    public void tryLoadRememberedSubtitle() {
        String videoUrl = mVideoView.getUrl();
        if (videoUrl == null || mController == null || mController.getSubtitleManager() == null) {
            return;
        }
        
        // 应用已保存的字幕大小
        float savedSize = mSettingsManager.getSubtitleSize();
        mController.getSubtitleManager().setTextSize(savedSize);
        
        // 优先加载本地字幕
        String localUri = mSettingsManager.getSubtitleLocalForVideo(videoUrl);
        if (localUri != null && !localUri.isEmpty()) {
            Log.d(TAG, "自动加载本地字幕: " + localUri);
            try {
                Uri uri = Uri.parse(localUri);
                mController.getSubtitleManager().loadSubtitle(uri, new com.orange.playerlibrary.subtitle.SubtitleManager.OnSubtitleLoadListener() {
                    @Override
                    public void onLoadSuccess(int count) {
                        mActivity.runOnUiThread(() -> {
                            Log.d(TAG, "自动加载本地字幕成功，共 " + count + " 条");
                            mController.startSubtitle();
                        });
                    }
                    
                    @Override
                    public void onLoadFailed(String error) {
                        Log.w(TAG, "自动加载本地字幕失败: " + error);
                        // 本地字幕失败，尝试网络字幕
                        tryLoadRememberedUrlSubtitle(videoUrl);
                    }
                });
                return;
            } catch (Exception e) {
                Log.w(TAG, "解析本地字幕Uri失败", e);
            }
        }
        
        // 加载网络字幕
        tryLoadRememberedUrlSubtitle(videoUrl);
    }
    
    private void tryLoadRememberedUrlSubtitle(String videoUrl) {
        String subtitleUrl = mSettingsManager.getSubtitleUrlForVideo(videoUrl);
        if (subtitleUrl != null && !subtitleUrl.isEmpty()) {
            Log.d(TAG, "自动加载网络字幕: " + subtitleUrl);
            mController.loadSubtitle(subtitleUrl, new com.orange.playerlibrary.subtitle.SubtitleManager.OnSubtitleLoadListener() {
                @Override
                public void onLoadSuccess(int count) {
                    mActivity.runOnUiThread(() -> {
                        Log.d(TAG, "自动加载网络字幕成功，共 " + count + " 条");
                        mController.startSubtitle();
                    });
                }
                
                @Override
                public void onLoadFailed(String error) {
                    Log.w(TAG, "自动加载网络字幕失败: " + error);
                }
            });
        }
    }
    
    // ===== OCR 翻译字幕功能 =====
    
    /**
     * 显示 OCR 安装指南
     */
    private void showOcrInstallGuide() {
        String message = com.orange.playerlibrary.ocr.OcrAvailabilityChecker.getMissingDependenciesMessage();
        
        new AlertDialog.Builder(mActivity)
            .setTitle("安装 OCR 翻译功能")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show();
    }
    
    /**
     * 显示 OCR 翻译设置弹窗
     */
    private void showOcrTranslateSettings() {
        View dialogView = View.inflate(mActivity, R.layout.dialog_ocr_settings, null);
        
        final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.CENTER, null, null);
        
        // 源语言选择
        android.widget.Spinner spinnerSource = dialogView.findViewById(R.id.spinner_source_lang);
        android.widget.Spinner spinnerTarget = dialogView.findViewById(R.id.spinner_target_lang);
        
        String[] sourceLangs = {"中文", "英文", "日文", "韩文"};
        String[] sourceLangCodes = {"chi_sim", "eng", "jpn", "kor"};
        String[] targetLangs = {"中文", "英文", "日文", "韩文"};
        String[] targetLangCodes = {"zh", "en", "ja", "ko"};
        
        if (spinnerSource != null) {
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                mActivity, android.R.layout.simple_spinner_item, sourceLangs);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSource.setAdapter(adapter);
        }
        
        if (spinnerTarget != null) {
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                mActivity, android.R.layout.simple_spinner_item, targetLangs);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTarget.setAdapter(adapter);
            spinnerTarget.setSelection(1); // 默认英文
        }
        
        // 开始按钮
        View btnStart = dialogView.findViewById(R.id.btn_start_ocr);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                int sourceIndex = spinnerSource != null ? spinnerSource.getSelectedItemPosition() : 0;
                int targetIndex = spinnerTarget != null ? spinnerTarget.getSelectedItemPosition() : 1;
                
                String sourceLang = sourceLangCodes[sourceIndex];
                String targetLang = targetLangCodes[targetIndex];
                
                dialog.dismiss();
                startOcrTranslate(sourceLang, targetLang);
            });
        }
        
        // 取消按钮
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
    }
    
    /**
     * 开始 OCR 翻译
     */
    private void startOcrTranslate(String sourceLang, String targetLang) {
        // 保存语言设置，用于全屏切换后恢复
        mOcrSourceLang = sourceLang;
        mOcrTargetLang = targetLang;
        
        showToast("正在初始化 OCR...");
        doStartOcrTranslate(sourceLang, targetLang);
    }
    
    /**
     * 实际启动 OCR 翻译
     * 
     * 注意：Exo 和系统核心使用 SurfaceControl 模式时无法截图，
     * 需要切换到 TextureView 模式。
     * 横竖屏切换时通过 onSurfaceDestroyed 中先切换到 PlaceholderSurface 来避免崩溃。
     */
    private void doStartOcrTranslate(String sourceLang, String targetLang) {
        // 检查当前播放核心，如果是 Exo 或系统核心，需要切换到 TextureView 模式
        String currentEngine = mSettingsManager.getPlayerEngine();
        boolean needSwitchRenderer = PlayerConstants.ENGINE_EXO.equals(currentEngine) 
            || PlayerConstants.ENGINE_DEFAULT.equals(currentEngine);
        
        if (needSwitchRenderer) {
            // 记录当前播放状态
            long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
            String currentUrl = mVideoView.getUrl();
            boolean wasPlaying = mVideoView.isPlaying();
            // 设置强制 TextureView 模式
            if (PlayerConstants.ENGINE_EXO.equals(currentEngine)) {
                com.orange.playerlibrary.exo.OrangeExoPlayerManager.setForceTextureViewMode(true);
            } else {
                com.orange.playerlibrary.player.OrangeSystemPlayerManager.setForceTextureViewMode(true);
            }
            
            // 先设置 GSYVideoType 为 TextureView
            com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                com.shuyu.gsyvideoplayer.utils.GSYVideoType.TEXTURE);
            // 重新加载视频以应用新的渲染模式
            mVideoView.release();
            com.shuyu.gsyvideoplayer.GSYVideoManager.releaseAllVideos();
            mVideoView.selectPlayerFactory(currentEngine);
            
            if (currentUrl != null && !currentUrl.isEmpty()) {
                mVideoView.setUp(currentUrl, false, "");
                if (currentPosition > 0) {
                    mVideoView.setSeekOnStart(currentPosition);
                }
                if (wasPlaying) {
                    mVideoView.startPlayLogic();
                }
            }
            
            showToast("已切换到 TextureView 模式");
            
            // 延迟启动 OCR，等待视频重新加载
            mMainHandler.postDelayed(() -> {
                doStartOcrTranslateInternal(sourceLang, targetLang);
            }, 1000);
        } else {
            // 其他播放核心直接启动 OCR
            doStartOcrTranslateInternal(sourceLang, targetLang);
        }
    }
    
    private android.os.Handler mMainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    
    /**
     * 实际启动 OCR 翻译（内部方法）
     * 注意：调用此方法前，调用方应该已经确保切换到了 TextureView 模式（如果需要的话）
     */
    private void doStartOcrTranslateInternal(String sourceLang, String targetLang) {
        // 获取 OcrSubtitleManager
        com.orange.playerlibrary.ocr.OcrSubtitleManager ocrManager = 
            new com.orange.playerlibrary.ocr.OcrSubtitleManager(mActivity);
        
        // 初始化 OCR
        if (!ocrManager.initOcr(sourceLang)) {
            showToast("OCR 初始化失败，请检查语言包是否已安装");
            showOcrLanguagePackHint(sourceLang);
            return;
        }
        
        // 设置视频视图
        if (mVideoView != null) {
            android.view.View renderView = mVideoView.getRenderProxy() != null ? 
                mVideoView.getRenderProxy().getShowView() : null;
            if (renderView != null) {
                ocrManager.setVideoView(renderView);
            }
            // 设置 GSY 渲染视图引用，用于 taskShotPic 截图（支持 SurfaceView）
            if (mVideoView.getRenderProxy() != null) {
                ocrManager.setRenderProxy(mVideoView.getRenderProxy());
            }
        }
        
        // 设置回调
        ocrManager.setCallback(new com.orange.playerlibrary.ocr.OcrSubtitleManager.OcrSubtitleCallback() {
            @Override
            public void onSubtitleRecognized(String originalText, String translatedText) {
                // 打印OCR识别结果到日志
                Log.d(TAG, "=== OCR识别结果 ===");
                Log.d(TAG, "原文: " + originalText);
                if (translatedText != null) {
                    Log.d(TAG, "译文: " + translatedText);
                }
                
                // 显示字幕
                if (mController != null && mController.getSubtitleManager() != null) {
                    String displayText = translatedText != null ? 
                        originalText + "\n" + translatedText : originalText;
                    mController.getSubtitleManager().showText(displayText);
                }
            }
            
            @Override
            public void onError(String error) {
                mActivity.runOnUiThread(() -> showToast("OCR 错误: " + error));
            }
        });
        
        // 初始化翻译
        showToast("正在下载翻译模型...");
        ocrManager.initTranslation(targetLang, new com.orange.playerlibrary.ocr.TranslationEngine.ModelDownloadCallback() {
            @Override
            public void onProgress(int progress) {
                // 可以显示下载进度
            }
            
            @Override
            public void onSuccess() {
                mActivity.runOnUiThread(() -> {
                    showToast("OCR 翻译已启动");
                    ocrManager.start();
                    
                    // 保存引用以便后续停止
                    mOcrSubtitleManager = ocrManager;
                });
            }
            
            @Override
            public void onError(String error) {
                mActivity.runOnUiThread(() -> {
                    showToast("翻译模型下载失败: " + error);
                    // 即使翻译失败，也可以只显示 OCR 结果
                    ocrManager.start();
                    mOcrSubtitleManager = ocrManager;
                });
            }
        });
    }
    
    /**
     * 显示语言包安装提示
     */
    private void showOcrLanguagePackHint(String language) {
        String hint = com.orange.playerlibrary.ocr.TesseractOcrEngine.getTrainedDataDownloadHint(language);
        
        new AlertDialog.Builder(mActivity)
            .setTitle("需要下载语言包")
            .setMessage(hint)
            .setPositiveButton("知道了", null)
            .show();
    }
    
    // OCR 字幕管理器引用
    private com.orange.playerlibrary.ocr.OcrSubtitleManager mOcrSubtitleManager;
    
    /**
     * 停止 OCR 翻译
     */
    public void stopOcrTranslate() {
        if (mOcrSubtitleManager != null) {
            mOcrSubtitleManager.release();
            mOcrSubtitleManager = null;
        }
        
        // 切换回 SurfaceView 模式（Android Q+ 需要 SurfaceView 才能无缝切换全屏）
        try {
            String currentEngine = mSettingsManager.getPlayerEngine();
            boolean needSwitchBack = (PlayerConstants.ENGINE_EXO.equals(currentEngine) 
                && com.orange.playerlibrary.exo.OrangeExoPlayerManager.isForceTextureViewMode())
                || (PlayerConstants.ENGINE_DEFAULT.equals(currentEngine)
                && com.orange.playerlibrary.player.OrangeSystemPlayerManager.isForceTextureViewMode());
            
            if (needSwitchBack && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // 记录当前播放状态
                long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
                String currentUrl = mVideoView.getUrl();
                boolean wasPlaying = mVideoView.isPlaying();
                
                // 设置回 SurfaceView 模式
                if (PlayerConstants.ENGINE_EXO.equals(currentEngine)) {
                    com.orange.playerlibrary.exo.OrangeExoPlayerManager.setForceTextureViewMode(false);
                } else {
                    com.orange.playerlibrary.player.OrangeSystemPlayerManager.setForceTextureViewMode(false);
                }
                
                // 设置 GSYVideoType 为 SurfaceView
                com.shuyu.gsyvideoplayer.utils.GSYVideoType.setRenderType(
                    com.shuyu.gsyvideoplayer.utils.GSYVideoType.SURFACE);
                
                // 重新加载视频
                mVideoView.release();
                com.shuyu.gsyvideoplayer.GSYVideoManager.releaseAllVideos();
                mVideoView.selectPlayerFactory(currentEngine);
                
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    mVideoView.setUp(currentUrl, false, "");
                    if (currentPosition > 0) {
                        mVideoView.setSeekOnStart(currentPosition);
                    }
                    if (wasPlaying) {
                        mVideoView.startPlayLogic();
                    }
                }
                
                showToast("已切换回 SurfaceView 模式");
            } else {
                // 只设置标志，不重新加载
                if (PlayerConstants.ENGINE_EXO.equals(currentEngine)) {
                    com.orange.playerlibrary.exo.OrangeExoPlayerManager.setForceTextureViewMode(false);
                } else if (PlayerConstants.ENGINE_DEFAULT.equals(currentEngine)) {
                    com.orange.playerlibrary.player.OrangeSystemPlayerManager.setForceTextureViewMode(false);
                }
            }
        } catch (Exception e) {
        }
    }
}

