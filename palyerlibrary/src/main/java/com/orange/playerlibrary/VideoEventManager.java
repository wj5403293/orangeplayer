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
 * 视频事件管理器
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
    
    // 对话框引用
    private AlertDialog mCurrentSetupDialog;
    
    // 长按倍速相关
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
        
        // 从设置中读取长按倍速
        mLongPressSpeed = mSettingsManager.getLongPressSpeed();
        
        // 绑定基础事件
        bindEvents();
    }
    
    /**
     * 绑定控制器组件
     */
    public void bindControllerComponents(VodControlView vodControlView) {
        android.util.Log.d(TAG, "bindControllerComponents: 被调用, vodControlView=" + vodControlView);
        mVodControlView = vodControlView;
        bindControllerEvents();
    }
    
    /**
     * 绑定 TitleView 组件
     */
    public void bindTitleView(com.orange.playerlibrary.component.TitleView titleView) {
        android.util.Log.d(TAG, "bindTitleView: 被调用");
        if (titleView != null) {
            // 绑定设置按钮点击事件
            titleView.setOnSettingsClickListener(v -> {
                android.util.Log.d(TAG, "设置按钮被点击（来自 TitleView）");
                showSetupDialog();
            });
            
            // 绑定投屏按钮点击事件（暂时只显示提示）
            titleView.setOnCastClickListener(v -> {
                android.util.Log.d(TAG, "投屏按钮被点击");
                Toast.makeText(mContext, "投屏功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * 绑定基础事件
     */
    private void bindEvents() {
        // 倍速按钮事件
        // 注意：这里使用接口方式绑定，实际调用在bindControllerEvents中
    }
    
    /**
     * 绑定控制器事件
     */
    private void bindControllerEvents() {
        android.util.Log.d(TAG, "bindControllerEvents: 开始绑定, mVodControlView=" + mVodControlView);
        
        if (mVodControlView == null) {
            android.util.Log.d(TAG, "bindControllerEvents: mVodControlView 为 null，无法绑定");
            return;
        }
        
        // 绑定倍速按钮点击事件
        android.util.Log.d(TAG, "bindControllerEvents: 设置倍速监听器");
        mVodControlView.setOnSpeedControlClickListener(v -> {
            android.util.Log.d(TAG, "倍速按钮被点击");
            showSpeedDialog();
        });
        
        // 绑定选集按钮点击事件
        android.util.Log.d(TAG, "bindControllerEvents: 设置选集监听器");
        mVodControlView.setOnEpisodeSelectClickListener(v -> {
            android.util.Log.d(TAG, "选集按钮被点击");
            showPlaylistDialog();
        });
        
        // 绑定播放按钮长按事件（用于长按倍速）
        ImageView playButton = mVodControlView.getPlayButton();
        if (playButton != null) {
            android.util.Log.d(TAG, "bindControllerEvents: 设置长按倍速");
            setupLongPressSpeed(playButton);
        } else {
            android.util.Log.d(TAG, "bindControllerEvents: playButton 为 null");
        }
        
        android.util.Log.d(TAG, "控制器事件绑定完成");
    }
    
    /**
     * 显示倍速选择对话框
     */
    private void showSpeedDialog() {
        android.util.Log.d(TAG, "showSpeedDialog: 开始显示倍速对话框");
        mController.hide(); // 隐藏播放器UI
        
        try {
            // 创建对话框视图
            View dialogView = View.inflate(mActivity, R.layout.speed_dialog, null);
            android.util.Log.d(TAG, "showSpeedDialog: 布局加载成功");
            
            // 始终显示在右侧
            final AlertDialog dialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                    DialogUtils.DialogPosition.RIGHT, null, null);
            android.util.Log.d(TAG, "showSpeedDialog: 对话框创建成功");
            
            // 点击外部区域关闭对话框
            android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
            if (layout != null) {
                layout.setOnClickListener(v -> dialog.dismiss());
                android.util.Log.d(TAG, "showSpeedDialog: 外部点击事件绑定成功");
            }
            
            // 设置倍速选项
            setupSpeedOptions(dialogView, dialog);
        } catch (Exception e) {
            android.util.Log.e(TAG, "showSpeedDialog: 错误", e);
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
        
        // 使用 RecyclerView 显示倍速列表
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler);
        if (recyclerView != null) {
            OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
            orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
            orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
                (holder, data, position) -> {
                    android.widget.TextView speedName = holder.itemView.findViewById(R.id.title);
                    String speedText = data.get(position).get("name").toString();
                    float speedValue = Float.parseFloat(speedText.replace("x", ""));
                    
                    // 高亮当前倍速
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
                        android.util.Log.d(TAG, "设置倍速: " + speedValue + "x");
                        Toast.makeText(mContext, "倍速: " + speedText, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                });
            android.util.Log.d(TAG, "setupSpeedOptions: RecyclerView 设置完成");
        } else {
            android.util.Log.e(TAG, "setupSpeedOptions: RecyclerView 为 null");
        }
    }
    
    /**
     * 设置长按倍速功能
     * 长按视图时加速播放，松开恢复正常速度
     */
    private void setupLongPressSpeed(View view) {
        view.setOnLongClickListener(v -> {
            if (!mIsLongPressing && mVideoView.isPlaying()) {
                mIsLongPressing = true;
                mNormalSpeed = mVideoView.getSpeed();
                mVideoView.setSpeed(mLongPressSpeed);
                android.util.Log.d(TAG, "长按加速: " + mLongPressSpeed + "x");
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
                    android.util.Log.d(TAG, "恢复正常速度: " + mNormalSpeed + "x");
                }
            }
            return false;
        });
    }
    
    /**
     * 设置长按倍速
     */
    public void setLongPressSpeed(float speed) {
        mLongPressSpeed = speed;
        mSettingsManager.setLongPressSpeed(speed);
    }
    
    /**
     * 获取长按倍速
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
        Toast.makeText(mActivity, "播放模式：" + getPlayModeName(mode), Toast.LENGTH_SHORT).show();
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
    
    // ==================== 设置对话框 ====================
    
    /**
     * 显示设置对话框
     */
    public void showSetupDialog() {
        android.util.Log.d(TAG, "showSetupDialog: 开始显示设置对话框");
        mController.hide(); // 隐藏播放器UI
        
        // 创建设置对话框视图
        View dialogView = View.inflate(mActivity, R.layout.setup_dialog, null);
        
        // 创建设置对话框 - 始终显示在右侧
        mCurrentSetupDialog = DialogUtils.showCustomDialog(mActivity, dialogView,
                DialogUtils.DialogPosition.RIGHT, null, null);
        
        // 点击空白区域关闭对话框
        android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
        if (layout != null) {
            layout.setOnClickListener(v -> mCurrentSetupDialog.dismiss());
        }
        
        android.util.Log.d(TAG, "showSetupDialog: 对话框创建完成");
        
        // 绑定所有设置项
        bindSetupOptions();
    }
    
    /**
     * 绑定设置选项
     */
    private void bindSetupOptions() {
        // 获取所有设置项视图
        android.widget.LinearLayout screenScaleButton = mCurrentSetupDialog.findViewById(R.id.line1);
        android.widget.LinearLayout longPressSpeedButton = mCurrentSetupDialog.findViewById(R.id.line2);
        android.widget.LinearLayout skipOpeningButton = mCurrentSetupDialog.findViewById(R.id.line4);
        android.widget.LinearLayout skipEndingButton = mCurrentSetupDialog.findViewById(R.id.line5);
        android.widget.TextView sequentialPlayBtn = mCurrentSetupDialog.findViewById(R.id.sxbf);
        android.widget.TextView singleLoopBtn = mCurrentSetupDialog.findViewById(R.id.djxh);
        android.widget.TextView playPauseBtn = mCurrentSetupDialog.findViewById(R.id.bwzt);
        
        // 设置播放模式按钮
        setupPlayModeButtons(sequentialPlayBtn, singleLoopBtn, playPauseBtn);
        
        // 绑定设置项点击事件
        if (screenScaleButton != null) {
            screenScaleButton.setOnClickListener(v -> showScreenScaleDialog());
        }
        if (longPressSpeedButton != null) {
            longPressSpeedButton.setOnClickListener(v -> showLongPressSpeedDialog());
        }
        if (skipOpeningButton != null) {
            skipOpeningButton.setOnClickListener(v -> showSkipOpeningDialog());
        }
        if (skipEndingButton != null) {
            skipEndingButton.setOnClickListener(v -> showSkipEndingDialog());
        }
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
     * 显示画面比例对话框
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
        android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
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
        
        // 当前选中的比例
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
            android.util.Log.d(TAG, "setScreenScaleType: 画面比例已设置并应用 = " + scaleType);
        } else {
            // 如果 VideoScaleManager 未初始化，只保存设置
            mSettingsManager.setVideoScale(scaleType);
            Toast.makeText(mContext, "画面比例: " + scaleType + "\n将在下次播放时生效", Toast.LENGTH_SHORT).show();
            android.util.Log.w(TAG, "setScreenScaleType: VideoScaleManager 未初始化，只保存设置");
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
        android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
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
        
        // 当前长按倍速
        final float currentSpeed = mLongPressSpeed;
        
        OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
        orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
        orangeRecyclerView.setAdapter(recyclerView, R.layout.speed_dialog_item, arrayList,
            (holder, data, position) -> {
                android.widget.TextView speedName = holder.itemView.findViewById(R.id.title);
                String speedText = data.get(position).get("name").toString();
                float speedValue = Float.parseFloat(speedText.replace("x", ""));
                
                // 高亮当前倍速
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
                    Toast.makeText(mContext, "长按倍速: " + speedText, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
    }
    
    /**
     * 显示播放模式对话框
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
        android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
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
        android.widget.LinearLayout layout = dialogView.findViewById(R.id.layout);
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
    
    /**
     * 显示选集列表
     */
    public void showPlaylistDialog() {
        android.util.Log.d(TAG, "showPlaylistDialog: 开始显示选集对话框");
        ArrayList<HashMap<String, Object>> videoList = mController.getVideoList();
        if (videoList == null || videoList.isEmpty()) {
            Toast.makeText(mActivity, "暂无选集", Toast.LENGTH_SHORT).show();
            return;
        }
        
        mController.hide();
        
        try {
            // 始终显示在右侧
            final AlertDialog dialog = DialogUtils.showAlertDialog(mActivity, R.layout.playliset, 
                    DialogUtils.DialogPosition.RIGHT);
            android.util.Log.d(TAG, "showPlaylistDialog: 对话框创建成功");
            
            // 点击外部区域关闭对话框
            View playListView = dialog.findViewById(R.id.playLiset_v);
            if (playListView != null) {
                playListView.setOnClickListener(v -> dialog.dismiss());
                android.util.Log.d(TAG, "showPlaylistDialog: 外部点击事件绑定成功");
            }
            
            // 设置 RecyclerView 显示选集列表
            RecyclerView recyclerView = dialog.findViewById(R.id.recycler);
            if (recyclerView != null) {
                OrangeRecyclerView orangeRecyclerView = new OrangeRecyclerView();
                orangeRecyclerView.setLinearLayoutManager(recyclerView, mActivity);
                orangeRecyclerView.setAdapter(recyclerView, R.layout.playliset_item, videoList,
                    (holder, data, position1) -> {
                        android.widget.TextView titleTv = holder.itemView.findViewById(R.id.title);
                        HashMap<String, Object> itemData = data.get(position1);
                        
                        // 绑定标题文字
                        String title = itemData.get("name") != null ? itemData.get("name").toString() : "第" + (position1 + 1) + "集";
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
                        
                        // 列表项点击事件
                        titleTv.setOnClickListener(v -> {
                            playEpisode(position1);
                            dialog.dismiss();
                        });
                    });
                android.util.Log.d(TAG, "showPlaylistDialog: RecyclerView 设置完成");
            } else {
                android.util.Log.e(TAG, "showPlaylistDialog: RecyclerView 为 null");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "showPlaylistDialog: 错误", e);
        }
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
}
