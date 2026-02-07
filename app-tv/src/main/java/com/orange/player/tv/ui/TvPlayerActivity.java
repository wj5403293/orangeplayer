package com.orange.player.tv.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.orange.player.tv.R;
import com.orange.playerlibrary.OrangevideoView;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;

/**
 * TV 播放器 Activity
 * 
 * 特点：
 * 1. 支持遥控器导航
 * 2. 自动隐藏控制栏
 * 3. 焦点管理
 * 4. 大按钮设计
 */
public class TvPlayerActivity extends AppCompatActivity {
    
    private static final int CONTROL_BAR_HIDE_DELAY = 5000; // 5秒后隐藏控制栏
    
    // 播放器
    private OrangevideoView videoPlayer;
    
    // 控制栏
    private View controlBar;
    private ImageButton btnPlayPause;
    private ImageButton btnRewind;
    private ImageButton btnForward;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private TextView tvTitle;
    private ProgressBar tvLoading;
    
    // 状态
    private boolean isControlBarVisible = true;
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable = this::hideControlBar;
    
    // 视频信息
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
        setupControls();
        startPlayback();
    }
    
    private void initViews() {
        videoPlayer = findViewById(R.id.video_player);
        controlBar = findViewById(R.id.control_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);
        seekBar = findViewById(R.id.seek_bar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvDuration = findViewById(R.id.tv_duration);
        tvTitle = findViewById(R.id.tv_title);
        tvLoading = findViewById(R.id.tv_loading);
        
        // 设置标题
        if (videoTitle != null && !videoTitle.isEmpty()) {
            tvTitle.setText(videoTitle);
        } else {
            tvTitle.setText("视频播放");
        }
        
        // 默认焦点在播放按钮
        btnPlayPause.requestFocus();
    }
    
    private void setupPlayer() {
        videoPlayer.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                tvLoading.setVisibility(View.GONE);
                updateDuration();
                startProgressUpdate();
                Toast.makeText(TvPlayerActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                btnPlayPause.setImageResource(R.drawable.ic_replay);
            }
            
            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                tvLoading.setVisibility(View.GONE);
                Toast.makeText(TvPlayerActivity.this, "播放出错，请检查网络连接", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void setupControls() {
        // 播放/暂停按钮
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 快退按钮
        btnRewind.setOnClickListener(v -> seekBackward(10000));
        btnRewind.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 快进按钮
        btnForward.setOnClickListener(v -> seekForward(10000));
        btnForward.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                resetHideTimer();
            }
        });
        
        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdate();
                resetHideTimer();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoPlayer.seekTo(seekBar.getProgress());
                startProgressUpdate();
            }
        });
        
        seekBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                resetHideTimer();
            }
        });
    }
    
    private void startPlayback() {
        tvLoading.setVisibility(View.VISIBLE);
        videoPlayer.setUp(videoUrl, true, "");
        videoPlayer.startPlayLogic();
    }
    
    private void togglePlayPause() {
        if (videoPlayer.isPlaying()) {
            videoPlayer.onVideoPause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
            stopProgressUpdate();
        } else {
            videoPlayer.onVideoResume();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            startProgressUpdate();
        }
        resetHideTimer();
    }
    
    private void seekForward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long duration = videoPlayer.getDuration();
        long newPosition = Math.min(currentPosition + milliseconds, duration);
        videoPlayer.seekTo(newPosition);
        updateProgress();
        resetHideTimer();
    }
    
    private void seekBackward(long milliseconds) {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long newPosition = Math.max(currentPosition - milliseconds, 0);
        videoPlayer.seekTo(newPosition);
        updateProgress();
        resetHideTimer();
    }
    
    private void showControlBar() {
        if (!isControlBarVisible) {
            controlBar.setVisibility(View.VISIBLE);
            controlBar.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start();
            isControlBarVisible = true;
            
            // 恢复焦点到播放按钮
            btnPlayPause.requestFocus();
        }
        resetHideTimer();
    }
    
    private void hideControlBar() {
        if (isControlBarVisible && videoPlayer.isPlaying()) {
            controlBar.animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .withEndAction(() -> controlBar.setVisibility(View.GONE))
                    .start();
            isControlBarVisible = false;
        }
    }
    
    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable);
        if (videoPlayer.isPlaying()) {
            hideHandler.postDelayed(hideRunnable, CONTROL_BAR_HIDE_DELAY);
        }
    }
    
    private void animateFocus(View view, boolean hasFocus) {
        if (hasFocus) {
            view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start();
        } else {
            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        }
    }
    
    // 进度更新
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            progressHandler.postDelayed(this, 1000);
        }
    };
    
    private void startProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }
    
    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
    }
    
    private void updateProgress() {
        long currentPosition = videoPlayer.getCurrentPositionWhenPlaying();
        long duration = videoPlayer.getDuration();
        
        if (duration > 0) {
            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));
        }
    }
    
    private void updateDuration() {
        long duration = videoPlayer.getDuration();
        tvDuration.setText(formatTime(duration));
        seekBar.setMax((int) duration);
    }
    
    private String formatTime(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!isControlBarVisible) {
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!isControlBarVisible) {
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isControlBarVisible) {
                    hideControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!isControlBarVisible) {
                    seekBackward(10000);
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!isControlBarVisible) {
                    seekForward(10000);
                    showControlBar();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
                
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (!videoPlayer.isPlaying()) {
                    togglePlayPause();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (videoPlayer.isPlaying()) {
                    togglePlayPause();
                }
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekForward(30000);
                return true;
                
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekBackward(30000);
                return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
        stopProgressUpdate();
        hideHandler.removeCallbacks(hideRunnable);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
        startProgressUpdate();
        resetHideTimer();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.release();
        stopProgressUpdate();
        hideHandler.removeCallbacks(hideRunnable);
    }
}
