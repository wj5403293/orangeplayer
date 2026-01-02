package com.orange.player;

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

    private OrangevideoView mVideoView;
    private TextView mTvInfo;
    private TextView mTvDebugLog;
    private StringBuilder mLogBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPlayer();
        setupBackPressedHandler();
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
        
        // 调用 OrangevideoView 的配置改变处理方法
        if (mVideoView != null) {
            mVideoView.onConfigurationChanged(newConfig);
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onVideoResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }
}
