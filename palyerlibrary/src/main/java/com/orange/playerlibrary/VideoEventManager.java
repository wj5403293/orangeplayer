package com.orange.playerlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
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
            
            // 绑定投屏按钮点击事件（暂时只显示提示
            titleView.setOnCastClickListener(v -> {
                                Toast.makeText(mContext, "投屏功能开发中", Toast.LENGTH_SHORT).show();
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
                                                Toast.makeText(mContext, "倍 " + speedText, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(mActivity, "播放模式: " + getPlayModeName(mode), Toast.LENGTH_SHORT).show();
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
    }
    
    /**
     * 设置播放核心按钮
     */
    private void setupEngineButtons(android.widget.TextView aliBtn, android.widget.TextView exoBtn, 
                                   android.widget.TextView ijkBtn, android.widget.TextView systemBtn) {
        // 检查核心是否可用
        boolean isAliPlayerAvailable = isClassPresent("com.aliyun.player.AliPlayer");
        boolean isIjkPlayerAvailable = isClassPresent("tv.danmaku.ijk.media.player.IjkMediaPlayer");
        boolean isExoPlayerAvailable = isClassPresent("com.google.android.exoplayer2.ExoPlayer") ||
                                       isClassPresent("com.google.android.exoplayer2.Player");
        
        // 设置可见性
        if (aliBtn != null) aliBtn.setVisibility(isAliPlayerAvailable ? View.VISIBLE : View.GONE);
        if (ijkBtn != null) ijkBtn.setVisibility(isIjkPlayerAvailable ? View.VISIBLE : View.GONE);
        if (exoBtn != null) exoBtn.setVisibility(isExoPlayerAvailable ? View.VISIBLE : View.GONE);
        
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
        android.util.Log.d("VideoEventManager", "selectEngine: 切换播放核心 from " + oldEngine + " to " + engine);
        
        // 保存播放核心设置
        mSettingsManager.setPlayerEngine(engine);
        
        // 关闭设置对话框
        if (mCurrentSetupDialog != null) {
            mCurrentSetupDialog.dismiss();
        }
        
        // 提示用户需要重启应用
        android.widget.Toast.makeText(mVideoView.getContext(), 
            "播放核心已切换为 " + getEngineName(engine) + "\n请重启应用后生效", 
            android.widget.Toast.LENGTH_LONG).show();
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
                            Toast.makeText(mContext, "定时关闭: " + optionText, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(mContext, "定时关闭已取消", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(mActivity, "进入小窗模式失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mActivity, "您的设备不支持小窗播放", Toast.LENGTH_SHORT).show();
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
        
        Toast.makeText(mContext, newState ? "底部进度条已开启" : "底部进度条已关闭", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "无法获取视频地址", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 通知外部处理下载
        if (mOnDownloadClickListener != null) {
            mOnDownloadClickListener.onDownloadClick(url, title);
        } else {
            Toast.makeText(mContext, "下载功能未配置", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "画面比例: " + scaleText, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "画面比例: " + scaleType + " 已应用", Toast.LENGTH_SHORT).show();
        } else {
            // 如果 VideoScaleManager 未初始化，只保存设置
            mSettingsManager.setVideoScale(scaleType);
            Toast.makeText(mContext, "画面比例: " + scaleType + "\n将在下次播放时生效", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "长按倍 " + speedText, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "播放引擎: " + engines[which] + "\n重新播放生效", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "跳过片头: " + formatSkipTime(progress), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "跳过片尾: " + formatSkipTime(progress), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "跳过片头: " + optionText, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "跳过片尾: " + optionText, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mActivity, "暂无选集", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mActivity, "播放地址异常", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mActivity, "播放地址异常", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mActivity, "已经是最后一集了", Toast.LENGTH_SHORT).show();
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
        
        Toast.makeText(mContext, newState ? "弹幕已开启" : "弹幕已关闭", Toast.LENGTH_SHORT).show();
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
            
            Toast.makeText(mContext, "弹幕已发送", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "弹幕文字大小: " + String.format("%.0f", size) + "sp", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "弹幕速度: " + String.format("%.1f", speed) + "x", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mContext, "弹幕透明度: " + seekBar.getProgress() + "%", Toast.LENGTH_SHORT).show();
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
}

