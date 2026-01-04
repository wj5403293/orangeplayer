package com.orange.player;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.orange.playerlibrary.DanmakuControllerImpl;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PiPHelper;
import com.orange.playerlibrary.interfaces.IDanmakuController;
import com.shuyu.gsyvideoplayer.GSYVideoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 橘子播放器 Demo
 * 演示如何使用 OrangevideoView SDK
 */
public class MainActivity extends AppCompatActivity {

    private static final String VIDEO_URL = "http://player.alicdn.com/video/aliyunmedia.mp4";
    private static final String VIDEO_TITLE = "阿里云测试视频";

    private OrangevideoView mVideoView;
    private OrangeVideoController mController;
    private PiPHelper mPiPHelper;
    private DanmakuControllerImpl mDanmakuController;
    
    // Demo UI
    private TextView mTvInfo;
    private TextView mTvDebugLog;
    private StringBuilder mLogBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPlayer();
        initPiPHelper();
        setupBackPressedHandler();
    }

    private void initViews() {
        mVideoView = findViewById(R.id.video_view);
        mTvInfo = findViewById(R.id.tv_info);
        mTvDebugLog = findViewById(R.id.tv_debug_log);

        // 播放控制按钮
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnPause = findViewById(R.id.btn_pause);
        Button btnFullscreen = findViewById(R.id.btn_fullscreen);

        btnPlay.setOnClickListener(v -> {
            log("播放");
            mVideoView.startPlayLogic();
        });

        btnPause.setOnClickListener(v -> {
            if (mVideoView.isInPlayingState()) {
                mVideoView.onVideoPause();
                log("暂停");
            } else {
                mVideoView.onVideoResume();
                log("继续");
            }
        });

        btnFullscreen.setOnClickListener(v -> {
            if (mVideoView.getControlWrapper() != null) {
                mVideoView.getControlWrapper().toggleFullScreen();
            }
        });
        
        // 弹幕测试按钮
        Button btnBatchDanmaku = findViewById(R.id.btn_batch_danmaku);
        Button btnSendDanmaku = findViewById(R.id.btn_send_danmaku);
        Button btnToggleDanmaku = findViewById(R.id.btn_toggle_danmaku);
        
        btnBatchDanmaku.setOnClickListener(v -> loadBatchDanmaku());
        btnSendDanmaku.setOnClickListener(v -> sendDanmaku());
        btnToggleDanmaku.setOnClickListener(v -> toggleDanmaku(btnToggleDanmaku));

        updateInfo("橘子播放器 Demo\n视频: " + VIDEO_URL);
    }

    private void initPlayer() {
        // 创建控制器
        mController = new OrangeVideoController(this);
        mVideoView.setVideoController(mController);
        
        // 添加默认控制组件（内部会自动初始化弹幕）
        mController.addDefaultControlComponent(VIDEO_TITLE, false);
        
        // 获取弹幕控制器（用于加载测试数据）
        if (mController.isDanmakuAvailable()) {
            mDanmakuController = (DanmakuControllerImpl) mController.getDanmakuController();
            loadTestDanmaku();
            log("弹幕已启用");
        }
        
        // 设置测试视频列表
        setupVideoList();
        
        // 设置视频
        mVideoView.setUp(VIDEO_URL, false, VIDEO_TITLE);
        mVideoView.setLooping(false);
        mVideoView.setAutoRotateOnFullscreen(true);
        
        // 设置标题
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setTitle(VIDEO_TITLE);
        }
        
        log("播放器初始化完成");
    }
    
    private void initPiPHelper() {
        mPiPHelper = new PiPHelper(this, mVideoView);
        
        // 检查是否从 PiP 恢复
        long restorePosition = mPiPHelper.checkPiPRestore(VIDEO_URL);
        if (restorePosition > 0) {
            // 设置恢复回调
            mVideoView.setVideoAllCallBack(new com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack() {
                @Override
                public void onPrepared(String url, Object... objects) {
                    super.onPrepared(url, objects);
                    mVideoView.postDelayed(() -> {
                        mVideoView.seekTo(restorePosition);
                        mPiPHelper.clearPendingSeekPosition();
                    }, 200);
                }
            });
        }
    }
    
    private void setupVideoList() {
        ArrayList<HashMap<String, Object>> videoList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            HashMap<String, Object> video = new HashMap<>();
            video.put("name", "第" + i + "集");
            video.put("url", VIDEO_URL);
            videoList.add(video);
        }
        mController.setVideoList(videoList);
    }
    
    // ===== 弹幕测试方法（App 层提供测试数据）=====
    
    private void loadTestDanmaku() {
        if (mDanmakuController == null) return;
        
        List<IDanmakuController.DanmakuItem> danmakus = new ArrayList<>();
        String[] texts = {"测试弹幕1", "橘子播放器！", "666", "前方高能", "好看"};
        int[] colors = {Color.WHITE, Color.RED, Color.GREEN, Color.CYAN, Color.YELLOW};
        
        for (int i = 0; i < texts.length; i++) {
            danmakus.add(new IDanmakuController.DanmakuItem(
                texts[i], colors[i], (i + 1) * 3000, false));
        }
        mDanmakuController.setDanmakuData(danmakus);
    }
    
    private void loadBatchDanmaku() {
        if (mDanmakuController == null) return;
        
        List<IDanmakuController.DanmakuItem> danmakus = new ArrayList<>();
        int[] colors = {Color.WHITE, Color.RED, Color.GREEN, Color.CYAN, Color.YELLOW};
        long currentPos = mVideoView.getCurrentPositionWhenPlaying();
        
        for (int i = 0; i < 30; i++) {
            danmakus.add(new IDanmakuController.DanmakuItem(
                "弹幕" + (i + 1), colors[i % colors.length], currentPos + i * 500, false));
        }
        mDanmakuController.setDanmakuData(danmakus);
        log("加载 30 条弹幕");
    }
    
    private void sendDanmaku() {
        if (mDanmakuController != null) {
            mDanmakuController.sendDanmaku("用户弹幕 " + System.currentTimeMillis() % 1000, Color.YELLOW);
            log("发送弹幕");
        }
    }
    
    private void toggleDanmaku(Button btn) {
        if (mDanmakuController != null) {
            boolean enabled = !mDanmakuController.isDanmakuEnabled();
            mDanmakuController.setDanmakuEnabled(enabled);
            btn.setText(enabled ? "关闭弹幕" : "开启弹幕");
            log("弹幕: " + (enabled ? "开" : "关"));
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

    // ===== 生命周期 =====
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mPiPHelper != null && mPiPHelper.handleOnPause()) {
            return; // PiP 模式，跳过暂停
        }
        mVideoView.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPiPHelper != null && mPiPHelper.handleOnResume()) {
            return; // 从 PiP 退出，跳过恢复
        }
        mVideoView.onVideoResume();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mPiPHelper != null && mPiPHelper.handleOnStop()) {
            return; // PiP 模式，跳过处理
        }
    }
    
    @Override
    public void onPictureInPictureModeChanged(boolean isInPiP, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig);
        if (mPiPHelper != null) {
            mPiPHelper.onPictureInPictureModeChanged(isInPiP, VIDEO_URL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 使用 SDK 封装的方法释放弹幕资源
        if (mController != null) {
            mController.releaseDanmaku();
        }
        mVideoView.release();
    }

    // ===== Demo 辅助方法 =====
    
    private void log(String msg) {
        mLogBuilder.append(msg).append("\n");
        String[] lines = mLogBuilder.toString().split("\n");
        if (lines.length > 8) {
            mLogBuilder = new StringBuilder();
            for (int i = lines.length - 8; i < lines.length; i++) {
                mLogBuilder.append(lines[i]).append("\n");
            }
        }
        if (mTvDebugLog != null) {
            mTvDebugLog.setText(mLogBuilder.toString());
        }
    }

    private void updateInfo(String info) {
        if (mTvInfo != null) {
            mTvInfo.setText(info);
        }
    }
}
