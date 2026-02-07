package com.orange.player.tv.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.orange.player.tv.R;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.component.TvControlView;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

/**
 * TV 播放器 Activity
 * 
 * 使用 OrangevideoView 和 TvControlView 实现
 * 自动适配 TV 模式
 */
public class TvPlayerActivity extends AppCompatActivity {
    
    private OrangevideoView videoPlayer;
    private TvControlView tvControl;
    
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
        tvControl = findViewById(R.id.tv_control);
        
        // 绑定播放器
        if (tvControl != null) {
            tvControl.bindVideoPlayer(videoPlayer);
            tvControl.setTitle(videoTitle != null ? videoTitle : "视频播放");
        }
    }
    
    private void setupPlayer() {
        videoPlayer.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                if (tvControl != null) {
                    tvControl.hideLoading();
                    tvControl.updateProgress(0, videoPlayer.getDuration());
                    tvControl.showControlBar();
                }
                Toast.makeText(TvPlayerActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                if (tvControl != null) {
                    tvControl.updatePlayState(false);
                }
            }
            
            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                if (tvControl != null) {
                    tvControl.hideLoading();
                }
                Toast.makeText(TvPlayerActivity.this, "播放出错，请检查网络连接", Toast.LENGTH_LONG).show();
            }
        });
        
        // 进度监听
        videoPlayer.setGSYVideoProgressListener((progress, secProgress, currentPosition, duration) -> {
            if (tvControl != null) {
                tvControl.updateProgress(currentPosition, duration);
            }
        });
    }
    
    private void startPlayback() {
        if (tvControl != null) {
            tvControl.showLoading();
        }
        videoPlayer.setUp(videoUrl, true, videoTitle);
        videoPlayer.startPlayLogic();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (tvControl != null) {
                    tvControl.toggleControlBar();
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_BACK:
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
            if (tvControl != null) {
                tvControl.updatePlayState(false);
            }
        } else {
            videoPlayer.onVideoResume();
            if (tvControl != null) {
                tvControl.updatePlayState(true);
            }
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
