package com.orange.player;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.orange.playerlibrary.DanmakuControllerImpl;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PiPHelper;
import com.orange.playerlibrary.VideoSniffing;
import com.orange.playerlibrary.interfaces.IDanmakuController;
import com.orange.playerlibrary.interfaces.OnStateChangeListener;
import com.shuyu.gsyvideoplayer.GSYVideoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 橘子播放器 Demo
 * 演示如何使用 OrangevideoView SDK
 * 
 * 基于 GSYVideoPlayer 开源播放器框架
 * https://github.com/CarGuo/GSYVideoPlayer
 */
public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_VIDEO_URL = "http://player.alicdn.com/video/aliyunmedia.mp4";
    private static final String DEFAULT_VIDEO_TITLE = "阿里云测试视频";

    private OrangevideoView mVideoView;
    private OrangeVideoController mController;
    private PiPHelper mPiPHelper;
    private DanmakuControllerImpl mDanmakuController;
    
    // Demo UI
    private EditText mEtVideoUrl;
    private TextView mTvDebugLog;
    private ScrollView mScrollLog;
    private StringBuilder mLogBuilder = new StringBuilder();
    
    private String mCurrentUrl = DEFAULT_VIDEO_URL;
    private String mCurrentTitle = DEFAULT_VIDEO_TITLE;

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
        mEtVideoUrl = findViewById(R.id.et_video_url);
        mTvDebugLog = findViewById(R.id.tv_debug_log);
        mScrollLog = (ScrollView) mTvDebugLog.getParent();
        
        // 设置默认URL
        mEtVideoUrl.setText(DEFAULT_VIDEO_URL);

        // 视频链接播放按钮
        Button btnPlayUrl = findViewById(R.id.btn_play_url);
        Button btnSniffPlay = findViewById(R.id.btn_sniff_play);
        
        btnPlayUrl.setOnClickListener(v -> playInputUrl(false));
        btnSniffPlay.setOnClickListener(v -> playInputUrl(true));

        // 播放控制按钮
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnPause = findViewById(R.id.btn_pause);
        Button btnFullscreen = findViewById(R.id.btn_fullscreen);
        log("作者QQ706412584");
        btnPlay.setOnClickListener(v -> {
            log("▶ 播放");
            mVideoView.startPlayLogic();
        });

        btnPause.setOnClickListener(v -> {
            if (mVideoView.isInPlayingState()) {
                mVideoView.onVideoPause();
                log("⏸ 暂停");
            } else {
                mVideoView.clearUserPausedState();
                mVideoView.onVideoResume();
                log("▶ 继续");
            }
        });

        btnFullscreen.setOnClickListener(v -> {
            if (mVideoView.getControlWrapper() != null) {
                mVideoView.getControlWrapper().toggleFullScreen();
                log("⛶ 全屏切换");
            }
        });
        
        // 弹幕测试按钮
        Button btnBatchDanmaku = findViewById(R.id.btn_batch_danmaku);
        Button btnSendDanmaku = findViewById(R.id.btn_send_danmaku);
        Button btnToggleDanmaku = findViewById(R.id.btn_toggle_danmaku);
        
        btnBatchDanmaku.setOnClickListener(v -> loadBatchDanmaku());
        btnSendDanmaku.setOnClickListener(v -> sendDanmaku());
        btnToggleDanmaku.setOnClickListener(v -> toggleDanmaku(btnToggleDanmaku));

        log("🍊 橘子播放器 SDK Demo 启动");
        log("基于 GSYVideoPlayer 开源框架");
    }
    
    private void playInputUrl(boolean useSniff) {
        String url = mEtVideoUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            log("❌ 请输入视频链接");
            return;
        }
        
        mCurrentUrl = url;
        mCurrentTitle = "自定义视频";
        
        if (useSniff) {
            log("🔍 开始嗅探: " + getShortUrl(url));
            // 使用嗅探播放 - 先设置URL再启动嗅探
            mVideoView.setUrl(url);
            mVideoView.startSniffing();
        } else {
            log("▶ 直接播放: " + getShortUrl(url));
            mVideoView.setUp(url, false, mCurrentTitle);
            mVideoView.startPlayLogic();
        }
        
        // 更新标题
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setTitle(mCurrentTitle);
        }
    }
    
    private String getShortUrl(String url) {
        if (url.length() > 50) {
            return url.substring(0, 47) + "...";
        }
        return url;
    }

    private void initPlayer() {
        // 创建控制器
        mController = new OrangeVideoController(this);
        mVideoView.setVideoController(mController);
        
        // 设置加载动画（默认已是 LINE_SCALE_PULSE_OUT）
        mController.setLoading(OrangeVideoController.IndicatorType.LINE_SCALE_PULSE_OUT);
        
        // 添加默认控制组件（内部会自动初始化弹幕）
        mController.addDefaultControlComponent(mCurrentTitle, false);
        
        // 获取弹幕控制器（用于加载测试数据）
        if (mController.isDanmakuAvailable()) {
            mDanmakuController = (DanmakuControllerImpl) mController.getDanmakuController();
            loadTestDanmaku();
            log("✓ 弹幕功能已启用");
        }
        
        // 设置嗅探监听器
        setupSniffingListener();
        
        // 设置测试视频列表
        setupVideoList();
        
        // 设置视频
        mVideoView.setUp(mCurrentUrl, false, mCurrentTitle);
        mVideoView.setLooping(false);
        mVideoView.setAutoRotateOnFullscreen(true);
        
        // 设置标题
        if (mVideoView.getTitleView() != null) {
            mVideoView.getTitleView().setTitle(mCurrentTitle);
        }
        
        log("✓ 播放器初始化完成");
        log("✓ 加载动画: LINE_SCALE_PULSE_OUT");
    }
    
    /**
     * 设置嗅探监听器
     */
    private void setupSniffingListener() {
        mVideoView.addOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                // 处理嗅探状态
                if (playState == OrangevideoView.STATE_STARTSNIFFING) {
                    log("🔍 嗅探开始...");
                } else if (playState == OrangevideoView.STATE_ENDSNIFFING) {
                    log("✓ 嗅探结束");
                }
            }
            
            @Override
            public void onPlayerStateChanged(int playerState) {
                // 不处理
            }
        });
        
        // 添加嗅探结果监听器
        mVideoView.addOnStateChangeListener(new OrangevideoView.OnSniffingAdapter() {
            @Override
            public void onSniffingReceived(String contentType, HashMap<String, String> headers, 
                                          String title, String url) {
                runOnUiThread(() -> {
                    log("📹 发现视频: " + getShortUrl(url));
                    if (title != null && !title.isEmpty()) {
                        log("   标题: " + title);
                    }
                    if (contentType != null) {
                        log("   类型: " + contentType);
                    }
                });
            }
            
            @Override
            public void onSniffingFinish(java.util.List<VideoSniffing.VideoInfo> videoList, int videoSize) {
                runOnUiThread(() -> {
                    if (videoSize > 0) {
                        log("✓ 嗅探完成，共发现 " + videoSize + " 个视频");
                        // 自动播放第一个视频
                        if (videoList != null && !videoList.isEmpty()) {
                            VideoSniffing.VideoInfo firstVideo = videoList.get(0);
                            String videoUrl = firstVideo.url;
                            log("▶ 自动播放: " + getShortUrl(videoUrl));
                            mVideoView.setUp(videoUrl, false, mCurrentTitle);
                            mVideoView.startPlayLogic();
                        }
                    } else {
                        log("❌ 嗅探完成，未发现视频");
                    }
                });
            }
        });
    }
    
    private void initPiPHelper() {
        mPiPHelper = new PiPHelper(this, mVideoView);
        
        // 检查是否从 PiP 恢复
        long restorePosition = mPiPHelper.checkPiPRestore(mCurrentUrl);
        if (restorePosition > 0) {
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
        log("✓ 画中画功能已启用");
    }
    
    private void setupVideoList() {
        ArrayList<HashMap<String, Object>> videoList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            HashMap<String, Object> video = new HashMap<>();
            video.put("name", "第" + i + "集");
            video.put("url", mCurrentUrl);
            videoList.add(video);
        }
        mController.setVideoList(videoList);
    }
    
    // ===== 弹幕测试方法 =====
    
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
        if (mDanmakuController == null) {
            log("❌ 弹幕功能不可用");
            return;
        }
        
        List<IDanmakuController.DanmakuItem> danmakus = new ArrayList<>();
        int[] colors = {Color.WHITE, Color.RED, Color.GREEN, Color.CYAN, Color.YELLOW};
        long currentPos = mVideoView.getCurrentPositionWhenPlaying();
        
        for (int i = 0; i < 30; i++) {
            danmakus.add(new IDanmakuController.DanmakuItem(
                "弹幕" + (i + 1), colors[i % colors.length], currentPos + i * 500, false));
        }
        mDanmakuController.setDanmakuData(danmakus);
        log("✓ 加载 30 条弹幕");
    }
    
    private void sendDanmaku() {
        if (mDanmakuController != null) {
            mDanmakuController.sendDanmaku("用户弹幕 " + System.currentTimeMillis() % 1000, Color.YELLOW);
            log("✓ 发送弹幕");
        } else {
            log("❌ 弹幕功能不可用");
        }
    }
    
    private void toggleDanmaku(Button btn) {
        if (mDanmakuController != null) {
            boolean enabled = !mDanmakuController.isDanmakuEnabled();
            mDanmakuController.setDanmakuEnabled(enabled);
            btn.setText(enabled ? "关闭弹幕" : "开启弹幕");
            log("弹幕: " + (enabled ? "开启" : "关闭"));
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
            return;
        }
        mVideoView.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPiPHelper != null && mPiPHelper.handleOnResume()) {
            return;
        }
        // 不自动恢复播放，让用户手动控制
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mPiPHelper != null && mPiPHelper.handleOnStop()) {
            return;
        }
    }
    
    @Override
    public void onPictureInPictureModeChanged(boolean isInPiP, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig);
        if (mPiPHelper != null) {
            mPiPHelper.onPictureInPictureModeChanged(isInPiP, mCurrentUrl);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            mController.releaseDanmaku();
        }
        mVideoView.release();
    }

    // ===== Demo 辅助方法 =====
    
    private void log(String msg) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        mLogBuilder.append("[").append(timestamp).append("] ").append(msg).append("\n");
        
        // 限制日志行数
        String[] lines = mLogBuilder.toString().split("\n");
        if (lines.length > 50) {
            mLogBuilder = new StringBuilder();
            for (int i = lines.length - 50; i < lines.length; i++) {
                mLogBuilder.append(lines[i]).append("\n");
            }
        }
        
        if (mTvDebugLog != null) {
            mTvDebugLog.setText(mLogBuilder.toString());
            // 自动滚动到底部
            if (mScrollLog != null) {
                mScrollLog.post(() -> mScrollLog.fullScroll(ScrollView.FOCUS_DOWN));
            }
        }
    }
}
