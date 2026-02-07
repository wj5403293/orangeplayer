package com.orange.player.tv.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.orange.player.tv.R;
import com.orange.playerlibrary.OrangevideoView;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

/**
 * TV 播放器 Activity
 * 
 * 使用标准的 OrangevideoView（包含 VodControlView）
 * TV 模式下自动隐藏不适合的 UI（投屏、小窗、弹幕区）
 */
public class TvPlayerActivity extends AppCompatActivity {
    
    private OrangevideoView videoPlayer;
    
    private String videoUrl;
    private String videoTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);
        
        // 获取视频信息
        videoUrl = getIntent().getStringExtra("video_url");
        videoTitle = getIntent().getStringExtra("video_title");
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "视频地址为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupPlayer();
        startPlayback();
    }
    
    private void initViews() {
        videoPlayer = findViewById(R.id.video_player);
        
        // 创建并设置控制器
        com.orange.playerlibrary.OrangeVideoController controller = 
            new com.orange.playerlibrary.OrangeVideoController(this);
        videoPlayer.setVideoController(controller);
        
        // 添加默认控制组件
        controller.addDefaultControlComponent(videoTitle != null ? videoTitle : "视频播放", false);
    }
    
    private void setupPlayer() {
        videoPlayer.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                android.util.Log.d("TvPlayerActivity", "onPrepared: 视频准备完成");
                Toast.makeText(TvPlayerActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                android.util.Log.e("TvPlayerActivity", "onPlayError: 播放出错");
                Toast.makeText(TvPlayerActivity.this, "播放出错，请检查网络连接", Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                android.util.Log.d("TvPlayerActivity", "onAutoComplete: 播放完成");
            }
        });
        
        // 添加状态监听，用于调试
        videoPlayer.addOnStateChangeListener(new com.orange.playerlibrary.interfaces.OnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                android.util.Log.d("TvPlayerActivity", "播放状态变化: " + playState);
            }
            
            @Override
            public void onPlayerStateChanged(int playerState) {
                android.util.Log.d("TvPlayerActivity", "播放器状态变化: " + playerState + 
                    " (0=NORMAL, 1=FULLSCREEN)");
            }
        });
    }
    
    private void startPlayback() {
        videoPlayer.setUp(videoUrl, true, videoTitle);
        videoPlayer.startPlayLogic();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (videoPlayer.isIfCurrentIsFullscreen()) {
                    videoPlayer.onBackFullscreen();
                    return true;
                }
                finish();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (videoPlayer.getCurrentState() != GSYVideoPlayer.CURRENT_STATE_PLAYING) {
                    videoPlayer.onVideoResume();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (videoPlayer.getCurrentState() == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
                    videoPlayer.onVideoPause();
                }
                return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private void togglePlayPause() {
        if (videoPlayer.getCurrentState() == GSYVideoPlayer.CURRENT_STATE_PLAYING) {
            videoPlayer.onVideoPause();
        } else {
            videoPlayer.onVideoResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.release();
    }
}
