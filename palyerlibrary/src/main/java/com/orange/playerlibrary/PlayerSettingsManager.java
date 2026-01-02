package com.orange.playerlibrary;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 播放器设置管理器
 * 负责保存和读取播放器的各种设置
 */
public class PlayerSettingsManager {
    
    private static final String PREFERENCES_NAME = "orange_player_settings";
    private static final String KEY_PLAYER_ENGINE = "player_engine";
    private static final String KEY_LONG_PRESS_SPEED = "long_press_speed";
    private static final String KEY_PLAY_MODE = "play_mode";
    private static final String KEY_VIDEO_SCALE = "video_scale";
    private static final String KEY_SKIP_OPENING = "skip_opening";
    private static final String KEY_SKIP_ENDING = "skip_ending";
    private static final String KEY_BOTTOM_PROGRESS = "bottom_progress";
    
    private static PlayerSettingsManager sInstance;
    private final SharedPreferences mPreferences;
    
    private PlayerSettingsManager(Context context) {
        mPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
    
    public static PlayerSettingsManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PlayerSettingsManager.class) {
                if (sInstance == null) {
                    sInstance = new PlayerSettingsManager(context);
                }
            }
        }
        return sInstance;
    }
    
    // ===== 播放器引擎设置 =====
    
    public void setPlayerEngine(String engine) {
        mPreferences.edit().putString(KEY_PLAYER_ENGINE, engine).apply();
    }
    
    public String getPlayerEngine() {
        return mPreferences.getString(KEY_PLAYER_ENGINE, PlayerConstants.ENGINE_DEFAULT);
    }
    
    // ===== 长按倍速设置 =====
    
    public void setLongPressSpeed(float speed) {
        mPreferences.edit().putFloat(KEY_LONG_PRESS_SPEED, speed).apply();
    }
    
    public float getLongPressSpeed() {
        return mPreferences.getFloat(KEY_LONG_PRESS_SPEED, 3.0f);
    }
    
    // ===== 播放模式设置 =====
    
    public void setPlayMode(String mode) {
        mPreferences.edit().putString(KEY_PLAY_MODE, mode).apply();
    }
    
    public String getPlayMode() {
        return mPreferences.getString(KEY_PLAY_MODE, "sequential");
    }
    
    // ===== 画面比例设置 =====
    
    public void setVideoScale(String scale) {
        mPreferences.edit().putString(KEY_VIDEO_SCALE, scale).apply();
    }
    
    public String getVideoScale() {
        return mPreferences.getString(KEY_VIDEO_SCALE, "默认");
    }
    
    // ===== 跳过片头片尾设置 =====
    
    public void setSkipOpening(int milliseconds) {
        mPreferences.edit().putInt(KEY_SKIP_OPENING, milliseconds).apply();
    }
    
    public int getSkipOpening() {
        return mPreferences.getInt(KEY_SKIP_OPENING, 0);
    }
    
    public void setSkipEnding(int milliseconds) {
        mPreferences.edit().putInt(KEY_SKIP_ENDING, milliseconds).apply();
    }
    
    public int getSkipEnding() {
        return mPreferences.getInt(KEY_SKIP_ENDING, 0);
    }
    
    // ===== 底部进度条设置 =====
    
    public void setBottomProgressEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(KEY_BOTTOM_PROGRESS, enabled).apply();
    }
    
    public boolean isBottomProgressEnabled() {
        return mPreferences.getBoolean(KEY_BOTTOM_PROGRESS, true);
    }
}
