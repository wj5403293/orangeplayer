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
    private static final String KEY_DANMAKU_ENABLED = "danmaku_enabled";
    private static final String KEY_DANMAKU_TEXT_SIZE = "danmaku_text_size";
    private static final String KEY_DANMAKU_SPEED = "danmaku_speed";
    private static final String KEY_DANMAKU_ALPHA = "danmaku_alpha";
    
    // 字幕设置
    private static final String KEY_SUBTITLE_SIZE = "subtitle_size";
    private static final String KEY_SUBTITLE_ENABLED = "subtitle_enabled";
    private static final String KEY_SUBTITLE_URL_PREFIX = "subtitle_url_";      // 按视频URL存储
    private static final String KEY_SUBTITLE_LOCAL_PREFIX = "subtitle_local_";  // 按视频URL存储本地字幕Uri
    
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
        return mPreferences.getString(KEY_PLAYER_ENGINE, PlayerConstants.ENGINE_IJK);
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
    
    // ===== 弹幕设置 =====
    
    public void setDanmakuEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(KEY_DANMAKU_ENABLED, enabled).apply();
    }
    
    public boolean isDanmakuEnabled() {
        return mPreferences.getBoolean(KEY_DANMAKU_ENABLED, true);
    }
    
    public void setDanmakuTextSize(float size) {
        mPreferences.edit().putFloat(KEY_DANMAKU_TEXT_SIZE, size).apply();
    }
    
    public float getDanmakuTextSize() {
        return mPreferences.getFloat(KEY_DANMAKU_TEXT_SIZE, 16.0f);
    }
    
    public void setDanmakuSpeed(float speed) {
        mPreferences.edit().putFloat(KEY_DANMAKU_SPEED, speed).apply();
    }
    
    public float getDanmakuSpeed() {
        return mPreferences.getFloat(KEY_DANMAKU_SPEED, 1.5f);
    }
    
    public void setDanmakuAlpha(float alpha) {
        mPreferences.edit().putFloat(KEY_DANMAKU_ALPHA, alpha).apply();
    }
    
    public float getDanmakuAlpha() {
        return mPreferences.getFloat(KEY_DANMAKU_ALPHA, 1.0f);
    }
    
    // ===== 字幕设置 =====
    
    public void setSubtitleSize(float size) {
        mPreferences.edit().putFloat(KEY_SUBTITLE_SIZE, size).apply();
    }
    
    public float getSubtitleSize() {
        return mPreferences.getFloat(KEY_SUBTITLE_SIZE, 18.0f); // 默认18sp
    }
    
    public void setSubtitleEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(KEY_SUBTITLE_ENABLED, enabled).apply();
    }
    
    public boolean isSubtitleEnabled() {
        return mPreferences.getBoolean(KEY_SUBTITLE_ENABLED, false);
    }
    
    /**
     * 保存视频对应的字幕URL
     * @param videoUrl 视频URL
     * @param subtitleUrl 字幕URL
     */
    public void setSubtitleUrlForVideo(String videoUrl, String subtitleUrl) {
        String key = KEY_SUBTITLE_URL_PREFIX + hashVideoUrl(videoUrl);
        mPreferences.edit().putString(key, subtitleUrl).apply();
    }
    
    /**
     * 获取视频对应的字幕URL
     */
    public String getSubtitleUrlForVideo(String videoUrl) {
        String key = KEY_SUBTITLE_URL_PREFIX + hashVideoUrl(videoUrl);
        return mPreferences.getString(key, null);
    }
    
    /**
     * 保存视频对应的本地字幕Uri
     * @param videoUrl 视频URL
     * @param subtitleUri 本地字幕Uri字符串
     */
    public void setSubtitleLocalForVideo(String videoUrl, String subtitleUri) {
        String key = KEY_SUBTITLE_LOCAL_PREFIX + hashVideoUrl(videoUrl);
        mPreferences.edit().putString(key, subtitleUri).apply();
    }
    
    /**
     * 获取视频对应的本地字幕Uri
     */
    public String getSubtitleLocalForVideo(String videoUrl) {
        String key = KEY_SUBTITLE_LOCAL_PREFIX + hashVideoUrl(videoUrl);
        return mPreferences.getString(key, null);
    }
    
    /**
     * 清除视频的字幕记忆
     */
    public void clearSubtitleForVideo(String videoUrl) {
        String hash = hashVideoUrl(videoUrl);
        mPreferences.edit()
            .remove(KEY_SUBTITLE_URL_PREFIX + hash)
            .remove(KEY_SUBTITLE_LOCAL_PREFIX + hash)
            .apply();
    }
    
    /**
     * 对视频URL进行哈希，避免key过长
     */
    private String hashVideoUrl(String url) {
        if (url == null) return "null";
        return String.valueOf(url.hashCode());
    }
}
