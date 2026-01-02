package com.orange.playerlibrary;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 视频缩略图帮助类
 * 用于异步获取视频第一帧作为缩略图
 * 
 * Requirements: 6.2 - THE OrangevideoView SHALL 支持自动获取视频缩略图功能
 */
public class VideoThumbnailHelper {

    private static final String TAG = "VideoThumbnailHelper";
    
    /** 线程池 */
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /** 主线程 Handler */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /** 缩略图缓存 */
    private static final Map<String, Bitmap> thumbnailCache = new HashMap<>();
    
    /** 最大缓存数量 */
    private static final int MAX_CACHE_SIZE = 50;

    /**
     * 缩略图回调接口
     */
    public interface ThumbnailCallback {
        /**
         * 获取成功
         * @param bitmap 缩略图
         */
        void onSuccess(Bitmap bitmap);
        
        /**
         * 获取失败
         * @param error 错误信息
         */
        void onError(String error);
    }

    /**
     * 异步获取视频第一帧
     * @param videoUrl 视频地址
     * @param callback 回调
     */
    public static void getVideoFirstFrameAsync(String videoUrl, ThumbnailCallback callback) {
        getVideoFirstFrameAsync(videoUrl, null, callback);
    }

    /**
     * 异步获取视频第一帧（带请求头）
     * @param videoUrl 视频地址
     * @param headers 请求头
     * @param callback 回调
     */
    public static void getVideoFirstFrameAsync(String videoUrl, Map<String, String> headers, 
                                               ThumbnailCallback callback) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Video URL is empty"));
            }
            return;
        }

        // 检查缓存
        Bitmap cached = thumbnailCache.get(videoUrl);
        if (cached != null && !cached.isRecycled()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onSuccess(cached));
            }
            return;
        }

        // 异步获取
        executor.execute(() -> {
            Bitmap bitmap = getVideoFirstFrame(videoUrl, headers);
            mainHandler.post(() -> {
                if (bitmap != null) {
                    // 添加到缓存
                    addToCache(videoUrl, bitmap);
                    if (callback != null) {
                        callback.onSuccess(bitmap);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to get video thumbnail");
                    }
                }
            });
        });
    }

    /**
     * 同步获取视频第一帧
     * @param videoUrl 视频地址
     * @return 缩略图
     */
    public static Bitmap getVideoFirstFrame(String videoUrl) {
        return getVideoFirstFrame(videoUrl, null);
    }

    /**
     * 同步获取视频第一帧（带请求头）
     * @param videoUrl 视频地址
     * @param headers 请求头
     * @return 缩略图
     */
    public static Bitmap getVideoFirstFrame(String videoUrl, Map<String, String> headers) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                if (headers != null && !headers.isEmpty()) {
                    retriever.setDataSource(videoUrl, headers);
                } else {
                    retriever.setDataSource(videoUrl, new HashMap<>());
                }
            } else {
                retriever.setDataSource(videoUrl);
            }
            
            // 获取第一帧
            return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取指定时间的帧
     * @param videoUrl 视频地址
     * @param timeUs 时间（微秒）
     * @param callback 回调
     */
    public static void getFrameAtTimeAsync(String videoUrl, long timeUs, ThumbnailCallback callback) {
        getFrameAtTimeAsync(videoUrl, timeUs, null, callback);
    }

    /**
     * 获取指定时间的帧（带请求头）
     * @param videoUrl 视频地址
     * @param timeUs 时间（微秒）
     * @param headers 请求头
     * @param callback 回调
     */
    public static void getFrameAtTimeAsync(String videoUrl, long timeUs, Map<String, String> headers,
                                           ThumbnailCallback callback) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Video URL is empty"));
            }
            return;
        }

        executor.execute(() -> {
            Bitmap bitmap = getFrameAtTime(videoUrl, timeUs, headers);
            mainHandler.post(() -> {
                if (bitmap != null) {
                    if (callback != null) {
                        callback.onSuccess(bitmap);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to get frame at time");
                    }
                }
            });
        });
    }

    /**
     * 同步获取指定时间的帧
     * @param videoUrl 视频地址
     * @param timeUs 时间（微秒）
     * @param headers 请求头
     * @return 帧图片
     */
    public static Bitmap getFrameAtTime(String videoUrl, long timeUs, Map<String, String> headers) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                if (headers != null && !headers.isEmpty()) {
                    retriever.setDataSource(videoUrl, headers);
                } else {
                    retriever.setDataSource(videoUrl, new HashMap<>());
                }
            } else {
                retriever.setDataSource(videoUrl);
            }
            
            return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加到缓存
     */
    private static synchronized void addToCache(String url, Bitmap bitmap) {
        // 清理过多的缓存
        if (thumbnailCache.size() >= MAX_CACHE_SIZE) {
            // 移除第一个
            String firstKey = thumbnailCache.keySet().iterator().next();
            Bitmap removed = thumbnailCache.remove(firstKey);
            if (removed != null && !removed.isRecycled()) {
                removed.recycle();
            }
        }
        thumbnailCache.put(url, bitmap);
    }

    /**
     * 清除缓存
     */
    public static synchronized void clearCache() {
        for (Bitmap bitmap : thumbnailCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        thumbnailCache.clear();
    }

    /**
     * 从缓存获取
     * @param url 视频地址
     * @return 缩略图
     */
    public static synchronized Bitmap getFromCache(String url) {
        return thumbnailCache.get(url);
    }

    /**
     * 检查缓存是否存在
     * @param url 视频地址
     * @return true 存在
     */
    public static synchronized boolean hasCache(String url) {
        Bitmap bitmap = thumbnailCache.get(url);
        return bitmap != null && !bitmap.isRecycled();
    }
}
