package com.orange.player;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.orange.playerlibrary.OrangevideoView;
import com.shuyu.gsyvideoplayer.GSYVideoManager;

/**
 * 橘子播放器 Demo
 */
public class MainActivity extends AppCompatActivity {

    private static final String VIDEO_URL = "http://player.alicdn.com/video/aliyunmedia.mp4";
    private static final String PREFS_NAME = "pip_prefs";
    private static final String KEY_PIP_POSITION = "pip_position";
    private static final String KEY_PIP_URL = "pip_url";
    private static final String KEY_PIP_ACTIVE = "pip_active";

    private OrangevideoView mVideoView;
    private TextView mTvInfo;
    private TextView mTvDebugLog;
    private StringBuilder mLogBuilder = new StringBuilder();
    
    // PiP 恢复相关
    private long mPendingSeekPosition = -1;
    private boolean mRestoringFromPiP = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 检查是否从 PiP 模式恢复
        checkPiPRestore();

        initViews();
        initPlayer();
        setupBackPressedHandler();
    }
    
    /**
     * 检查是否从 PiP 模式恢复，如果是则读取保存的播放位置
     */
    private void checkPiPRestore() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean pipActive = prefs.getBoolean(KEY_PIP_ACTIVE, false);
        String pipUrl = prefs.getString(KEY_PIP_URL, "");
        long pipPosition = prefs.getLong(KEY_PIP_POSITION, -1);
        
        android.util.Log.d("PiP_DEBUG", "=== checkPiPRestore ===");
        android.util.Log.d("PiP_DEBUG", "pipActive: " + pipActive);
        android.util.Log.d("PiP_DEBUG", "pipUrl: " + pipUrl);
        android.util.Log.d("PiP_DEBUG", "pipPosition: " + pipPosition);
        
        if (pipActive && pipPosition > 0 && VIDEO_URL.equals(pipUrl)) {
            mPendingSeekPosition = pipPosition;
            mRestoringFromPiP = true;
            android.util.Log.d("PiP_DEBUG", "Will restore to position: " + pipPosition);
        }
        
        // 清除 PiP 状态
        prefs.edit()
            .putBoolean(KEY_PIP_ACTIVE, false)
            .putLong(KEY_PIP_POSITION, -1)
            .apply();
    }
    
    /**
     * 保存 PiP 播放位置
     */
    private void savePiPPosition(long position) {
        android.util.Log.d("PiP_DEBUG", "=== savePiPPosition: " + position + " ===");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_PIP_ACTIVE, true)
            .putString(KEY_PIP_URL, VIDEO_URL)
            .putLong(KEY_PIP_POSITION, position)
            .apply();
    }

    private void initViews() {
        mVideoView = findViewById(R.id.video_view);
        mTvInfo = findViewById(R.id.tv_info);
        mTvDebugLog = findViewById(R.id.tv_debug_log);

        Button btnPlay = findViewById(R.id.btn_play);
        Button btnPause = findViewById(R.id.btn_pause);
        Button btnFullscreen = findViewById(R.id.btn_fullscreen);

        btnPlay.setOnClickListener(v -> {
            log("点击播放按钮");
            mVideoView.startPlayLogic();
            updateInfo("播放中...");
        });

        btnPause.setOnClickListener(v -> {
            if (mVideoView.isInPlayingState()) {
                mVideoView.onVideoPause();
                updateInfo("已暂停");
                log("暂停");
            } else {
                mVideoView.onVideoResume();
                updateInfo("继续播放");
                log("继续播放");
            }
        });

        btnFullscreen.setOnClickListener(v -> {
            log("点击全屏按钮");
            // 使用 ControlWrapper 的 toggleFullScreen，它会调用 CustomFullscreenHelper
            // 这样不会创建新的播放器实例，播放进度得以保持
            if (mVideoView.getControlWrapper() != null) {
                mVideoView.getControlWrapper().toggleFullScreen();
            }
        });
    }

    private void initPlayer() {
        log("initPlayer 开始");
        
        // 创建并设置控制器
        com.orange.playerlibrary.OrangeVideoController controller = 
                new com.orange.playerlibrary.OrangeVideoController(this);
        mVideoView.setVideoController(controller);
        log("控制器创建完成");
        
        // 添加默认控制组件
        controller.addDefaultControlComponent("阿里云测试视频", false);
        log("控制组件添加完成");
        
        // 设置测试视频列表（用于测试选集功能）
        java.util.ArrayList<java.util.HashMap<String, Object>> videoList = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            java.util.HashMap<String, Object> video = new java.util.HashMap<>();
            video.put("name", "第" + i + "集");
            video.put("url", VIDEO_URL); // 使用相同的测试视频
            videoList.add(video);
        }
        controller.setVideoList(videoList);
        log("视频列表设置完成: " + videoList.size() + " 集");
        
        // 设置外部日志回调
        mVideoView.setDebugLogCallback(msg -> runOnUiThread(() -> log(msg)));
        
        // 设置视频地址和标题（禁用缓存避免 NullPointerException）
        mVideoView.setUp(VIDEO_URL, false, "阿里云测试视频");
        log("setUp 完成");
        
        // 设置是否循环播放
        mVideoView.setLooping(false);
        
        // 设置播放速度
        mVideoView.setSpeed(1.0f);
        
        // 启用全屏自动旋转（默认已启用）
        mVideoView.setAutoRotateOnFullscreen(true);
        
        // 设置 TitleView 的标题
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setTitle("阿里云测试视频");
        }
        
        // 如果从 PiP 恢复，设置 onPrepared 回调来恢复播放位置
        if (mRestoringFromPiP && mPendingSeekPosition > 0) {
            android.util.Log.d("PiP_DEBUG", "Setting up PiP restore callback, position: " + mPendingSeekPosition);
            final long seekPosition = mPendingSeekPosition;
            mVideoView.setVideoAllCallBack(new com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack() {
                @Override
                public void onPrepared(String url, Object... objects) {
                    super.onPrepared(url, objects);
                    android.util.Log.d("PiP_DEBUG", "=== onPrepared (PiP restore) ===");
                    android.util.Log.d("PiP_DEBUG", "Seeking to: " + seekPosition);
                    // 延迟执行 seek，确保播放器完全准备好
                    mVideoView.postDelayed(() -> {
                        mVideoView.seekTo(seekPosition);
                        android.util.Log.d("PiP_DEBUG", "Seek completed to: " + seekPosition);
                        // 清除恢复状态
                        mRestoringFromPiP = false;
                        mPendingSeekPosition = -1;
                    }, 200);
                }
            });
        }

        updateInfo("橘子播放器 Demo\n视频地址: " + VIDEO_URL);
        log("initPlayer 完成");
    }

    private void log(String msg) {
        mLogBuilder.append(msg).append("\n");
        // 只保留最后10行
        String[] lines = mLogBuilder.toString().split("\n");
        if (lines.length > 10) {
            mLogBuilder = new StringBuilder();
            for (int i = lines.length - 10; i < lines.length; i++) {
                mLogBuilder.append(lines[i]).append("\n");
            }
        }
        if (mTvDebugLog != null) {
            mTvDebugLog.setText(mLogBuilder.toString());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        log("配置改变: orientation=" + newConfig.orientation);
        // CustomFullscreenHelper 会处理全屏切换，不需要额外处理
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (GSYVideoManager.isFullState(MainActivity.this)) {
                    GSYVideoManager.backFromWindowFull(MainActivity.this);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void updateInfo(String info) {
        if (mTvInfo != null) {
            mTvInfo.setText(info);
        }
    }

    // 记录是否从小窗模式退出
    private boolean mExitingPiP = false;
    // 记录是否正在进入小窗模式
    private boolean mEnteringPiP = false;

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("PiP_DEBUG", "=== MainActivity.onPause ===");
        android.util.Log.d("PiP_DEBUG", "mEnteringPiP: " + mEnteringPiP);
        android.util.Log.d("PiP_DEBUG", "mVideoView.isEnteringPiPMode: " + mVideoView.isEnteringPiPMode());
        
        // 检查是否处于画中画模式或正在进入画中画模式
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            boolean isInPiP = isInPictureInPictureMode();
            android.util.Log.d("PiP_DEBUG", "isInPictureInPictureMode: " + isInPiP);
            if (isInPiP || mEnteringPiP || mVideoView.isEnteringPiPMode()) {
                android.util.Log.d("PiP_DEBUG", "onPause: SKIP pause - PiP mode");
                mEnteringPiP = false;
                return; // 小窗模式下不暂停
            }
        }
        android.util.Log.d("PiP_DEBUG", "onPause: calling mVideoView.onVideoPause()");
        mVideoView.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("PiP_DEBUG", "=== MainActivity.onResume ===");
        android.util.Log.d("PiP_DEBUG", "mExitingPiP: " + mExitingPiP);
        
        // 如果是从小窗模式退出，不需要调用 onVideoResume，因为视频一直在播放
        if (mExitingPiP) {
            android.util.Log.d("PiP_DEBUG", "onResume: SKIP - exiting PiP");
            mExitingPiP = false;
            return;
        }
        // 如果当前处于画中画模式，不需要恢复
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (isInPictureInPictureMode()) {
                android.util.Log.d("PiP_DEBUG", "onResume: SKIP - in PiP mode");
                return;
            }
        }
        android.util.Log.d("PiP_DEBUG", "onResume: calling mVideoView.onVideoResume()");
        mVideoView.onVideoResume();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // 如果处于画中画模式，不做任何操作，让视频继续播放
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (isInPictureInPictureMode()) {
                return;
            }
        }
    }
    
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        // 用户按 Home 键或进入小窗时会调用此方法
        // 可以在这里标记正在进入小窗模式
    }
    
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        android.util.Log.d("PiP_DEBUG", "=== onPictureInPictureModeChanged ===");
        android.util.Log.d("PiP_DEBUG", "isInPictureInPictureMode: " + isInPictureInPictureMode);
        long currentPosition = mVideoView.getCurrentPositionWhenPlaying();
        android.util.Log.d("PiP_DEBUG", "current position: " + currentPosition);
        
        if (isInPictureInPictureMode) {
            // 进入小窗模式，保存当前播放位置并清除进入标志
            android.util.Log.d("PiP_DEBUG", "Entered PiP mode - saving position and clearing flags");
            savePiPPosition(currentPosition);
            mVideoView.setEnteringPiPMode(false);
            mEnteringPiP = false;
            // 隐藏控制器
            if (mVideoView.getVideoController() != null) {
                mVideoView.getVideoController().hide();
            }
        } else {
            // 退出小窗模式，再次保存位置（以防万一），标记状态
            android.util.Log.d("PiP_DEBUG", "Exited PiP mode - saving position, setting mExitingPiP = true");
            savePiPPosition(currentPosition);
            mExitingPiP = true;
            // 显示控制器
            if (mVideoView.getVideoController() != null) {
                mVideoView.getVideoController().show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }
}
