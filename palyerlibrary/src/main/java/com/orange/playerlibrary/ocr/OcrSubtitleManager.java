package com.orange.playerlibrary.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.shuyu.gsyvideoplayer.listener.GSYVideoShotListener;
import com.shuyu.gsyvideoplayer.render.GSYRenderView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OCR 字幕管理器
 * 负责截帧、OCR 识别、翻译和字幕显示
 */
public class OcrSubtitleManager {
    
    private static final String TAG = "OcrSubtitleManager";
    
    // 默认配置
    private static final int DEFAULT_FRAME_INTERVAL = 1000; // 1秒截取一帧
    private static final float DEFAULT_SUBTITLE_REGION_TOP = 0.75f; // 字幕区域从75%开始
    private static final float DEFAULT_SUBTITLE_REGION_BOTTOM = 0.95f; // 字幕区域到95%结束
    
    private Context mContext;
    private OcrEngine mOcrEngine;
    private TranslationEngine mTranslationEngine;
    
    private HandlerThread mOcrThread;
    private Handler mOcrHandler;
    private Handler mMainHandler;
    
    private boolean mIsRunning = false;
    private int mFrameInterval = DEFAULT_FRAME_INTERVAL;
    private float mSubtitleRegionTop = DEFAULT_SUBTITLE_REGION_TOP;
    private float mSubtitleRegionBottom = DEFAULT_SUBTITLE_REGION_BOTTOM;
    
    private String mSourceLanguage = "chi_sim"; // 默认中文
    private String mTargetLanguage = "en"; // 默认翻译成英文
    
    private String mLastRecognizedText = "";
    private OcrSubtitleCallback mCallback;
    
    private View mVideoView; // TextureView 或 SurfaceView
    private GSYRenderView mRenderProxy; // GSY 渲染视图引用，用于截图
    
    /**
     * OCR 字幕回调
     */
    public interface OcrSubtitleCallback {
        /**
         * 识别到字幕
         * @param originalText 原文
         * @param translatedText 译文（如果启用翻译）
         */
        void onSubtitleRecognized(String originalText, String translatedText);
        
        /**
         * 错误回调
         */
        void onError(String error);
    }
    
    public OcrSubtitleManager(Context context) {
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 检查 OCR 功能是否可用
     */
    public static boolean isAvailable() {
        return OcrAvailabilityChecker.isTesseractAvailable();
    }
    
    /**
     * 检查翻译功能是否可用
     */
    public static boolean isTranslationAvailable() {
        return OcrAvailabilityChecker.isMlKitTranslateAvailable();
    }
    
    /**
     * 获取缺失依赖提示
     */
    public static String getMissingDependenciesMessage() {
        return OcrAvailabilityChecker.getMissingDependenciesMessage();
    }
    
    /**
     * 初始化 OCR 引擎
     * @param sourceLanguage 源语言 (chi_sim, eng, jpn, kor)
     * @return 是否成功
     */
    public boolean initOcr(String sourceLanguage) {
        if (!isAvailable()) {
            Log.e(TAG, "OCR not available");
            return false;
        }
        
        mSourceLanguage = sourceLanguage;
        mOcrEngine = new TesseractOcrEngine();
        return mOcrEngine.init(mContext, sourceLanguage);
    }
    
    /**
     * 初始化翻译引擎
     * @param targetLanguage 目标语言
     * @param callback 模型下载回调
     */
    public void initTranslation(String targetLanguage, TranslationEngine.ModelDownloadCallback callback) {
        if (!isTranslationAvailable()) {
            Log.e(TAG, "Translation not available");
            if (callback != null) {
                callback.onError("Translation library not installed");
            }
            return;
        }
        
        mTargetLanguage = targetLanguage;
        mTranslationEngine = new MlKitTranslationEngine();
        mTranslationEngine.init(mContext, mSourceLanguage, targetLanguage);
        
        // 下载模型
        mTranslationEngine.downloadModel(callback);
    }
    
    /**
     * 设置视频视图（用于截帧）
     */
    public void setVideoView(View videoView) {
        mVideoView = videoView;
    }
    
    /**
     * 设置 GSY 渲染视图（用于 taskShotPic 截图）
     */
    public void setRenderProxy(GSYRenderView renderProxy) {
        mRenderProxy = renderProxy;
        Log.d(TAG, "setRenderProxy: " + renderProxy);
    }
    
    /**
     * 设置回调
     */
    public void setCallback(OcrSubtitleCallback callback) {
        mCallback = callback;
    }
    
    /**
     * 设置截帧间隔
     * @param intervalMs 间隔毫秒
     */
    public void setFrameInterval(int intervalMs) {
        mFrameInterval = intervalMs;
    }
    
    /**
     * 设置字幕区域
     * @param top 顶部位置 (0-1)
     * @param bottom 底部位置 (0-1)
     */
    public void setSubtitleRegion(float top, float bottom) {
        mSubtitleRegionTop = top;
        mSubtitleRegionBottom = bottom;
    }
    
    /**
     * 开始 OCR 识别
     */
    public void start() {
        Log.d(TAG, "start() called, mIsRunning=" + mIsRunning);
        
        if (mIsRunning) {
            Log.w(TAG, "Already running, skip");
            return;
        }
        
        if (mOcrEngine == null || !mOcrEngine.isInitialized()) {
            Log.e(TAG, "OCR engine not initialized, mOcrEngine=" + mOcrEngine);
            return;
        }
        
        Log.d(TAG, "mVideoView=" + mVideoView + ", mCallback=" + mCallback);
        
        mIsRunning = true;
        
        // 创建后台线程
        mOcrThread = new HandlerThread("OcrThread");
        mOcrThread.start();
        mOcrHandler = new Handler(mOcrThread.getLooper());
        
        // 开始定时截帧
        mOcrHandler.post(mOcrRunnable);
        
        Log.d(TAG, "OCR subtitle started, interval=" + mFrameInterval + "ms");
    }
    
    /**
     * 停止 OCR 识别
     */
    public void stop() {
        mIsRunning = false;
        
        if (mOcrHandler != null) {
            mOcrHandler.removeCallbacks(mOcrRunnable);
        }
        
        if (mOcrThread != null) {
            mOcrThread.quitSafely();
            mOcrThread = null;
        }
        
        mOcrHandler = null;
        
        Log.d(TAG, "OCR subtitle stopped");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        
        if (mOcrEngine != null) {
            mOcrEngine.release();
            mOcrEngine = null;
        }
        
        if (mTranslationEngine != null) {
            mTranslationEngine.release();
            mTranslationEngine = null;
        }
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return mIsRunning;
    }
    
    private int mFrameCount = 0;
    
    private final Runnable mOcrRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsRunning) {
                Log.d(TAG, "mOcrRunnable: not running, exit");
                return;
            }
            
            mFrameCount++;
            Log.d(TAG, "=== OCR Frame #" + mFrameCount + " ===");
            
            // 截取视频帧
            Bitmap frame = captureFrame();
            Log.d(TAG, "captureFrame result: " + (frame != null ? frame.getWidth() + "x" + frame.getHeight() : "null"));
            
            if (frame != null) {
                // 裁剪字幕区域
                Bitmap subtitleRegion = cropSubtitleRegion(frame);
                Log.d(TAG, "cropSubtitleRegion result: " + (subtitleRegion != null ? subtitleRegion.getWidth() + "x" + subtitleRegion.getHeight() : "null"));
                frame.recycle();
                
                if (subtitleRegion != null) {
                    // OCR 识别
                    long startTime = System.currentTimeMillis();
                    String text = mOcrEngine.recognize(subtitleRegion);
                    long ocrTime = System.currentTimeMillis() - startTime;
                    subtitleRegion.recycle();
                    
                    Log.d(TAG, "OCR recognize took " + ocrTime + "ms, result: [" + (text != null ? text.replace("\n", "\\n") : "null") + "]");
                    
                    if (text != null && !text.isEmpty() && !text.equals(mLastRecognizedText)) {
                        Log.d(TAG, "New text detected, lastText was: [" + mLastRecognizedText.replace("\n", "\\n") + "]");
                        mLastRecognizedText = text;
                        
                        // 翻译（如果启用）
                        if (mTranslationEngine != null && mTranslationEngine.isInitialized()) {
                            Log.d(TAG, "Translating text...");
                            mTranslationEngine.translate(text, new TranslationEngine.TranslationCallback() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    Log.d(TAG, "Translation success: [" + translatedText + "]");
                                    notifySubtitle(text, translatedText);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Translation error: " + error);
                                    // 翻译失败，只显示原文
                                    notifySubtitle(text, null);
                                }
                            });
                        } else {
                            Log.d(TAG, "No translation engine, showing original only");
                            // 不翻译，只显示原文
                            notifySubtitle(text, null);
                        }
                    } else {
                        if (text == null || text.isEmpty()) {
                            Log.d(TAG, "No text recognized");
                        } else {
                            Log.d(TAG, "Same text as before, skip");
                        }
                    }
                }
            } else {
                Log.w(TAG, "Failed to capture frame, mVideoView=" + mVideoView);
            }
            
            // 继续下一帧
            if (mIsRunning && mOcrHandler != null) {
                mOcrHandler.postDelayed(this, mFrameInterval);
            }
        }
    };
    
    /**
     * 截取视频帧
     */
    private Bitmap captureFrame() {
        // 优先使用 GSY 的 taskShotPic API（支持 SurfaceView）
        if (mRenderProxy != null) {
            return captureFromRenderProxy();
        }
        
        if (mVideoView == null) {
            Log.w(TAG, "captureFrame: mVideoView is null");
            return null;
        }
        
        Log.d(TAG, "captureFrame: mVideoView type=" + mVideoView.getClass().getSimpleName() 
            + ", size=" + mVideoView.getWidth() + "x" + mVideoView.getHeight()
            + ", isAttachedToWindow=" + mVideoView.isAttachedToWindow());
        
        try {
            if (mVideoView instanceof TextureView) {
                TextureView tv = (TextureView) mVideoView;
                Log.d(TAG, "TextureView isAvailable=" + tv.isAvailable());
                if (!tv.isAvailable()) {
                    Log.w(TAG, "TextureView not available");
                    return null;
                }
                Bitmap bitmap = tv.getBitmap();
                Log.d(TAG, "TextureView.getBitmap() = " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() : "null"));
                return bitmap;
            } else {
                Log.w(TAG, "Not a TextureView, type=" + mVideoView.getClass().getName());
                // SurfaceView 不能直接获取 Bitmap
                // 需要通过 PixelCopy API (Android O+)
                return captureFromSurfaceView();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture frame", e);
            return null;
        }
    }
    
    /**
     * 使用 GSY RenderProxy 的 taskShotPic 截图
     */
    private Bitmap captureFromRenderProxy() {
        if (mRenderProxy == null) {
            Log.w(TAG, "captureFromRenderProxy: mRenderProxy is null");
            return null;
        }
        
        final AtomicReference<Bitmap> bitmapRef = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(1);
        
        try {
            // 在主线程调用 taskShotPic
            mMainHandler.post(() -> {
                try {
                    mRenderProxy.taskShotPic(bitmap -> {
                        Log.d(TAG, "taskShotPic callback: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() + ", config=" + bitmap.getConfig() : "null"));
                        if (bitmap != null) {
                            // 复制 bitmap 为 ARGB_8888 格式，确保 Tesseract 能读取
                            Bitmap.Config config = bitmap.getConfig();
                            if (config == null || config == Bitmap.Config.HARDWARE) {
                                // HARDWARE 配置的 bitmap 需要转换为软件 bitmap
                                Bitmap softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                                bitmapRef.set(softwareBitmap);
                            } else {
                                bitmapRef.set(bitmap.copy(Bitmap.Config.ARGB_8888, false));
                            }
                        }
                        latch.countDown();
                    }, false);
                } catch (Exception e) {
                    Log.e(TAG, "taskShotPic exception", e);
                    latch.countDown();
                }
            });
            
            // 等待最多 500ms（缩短超时时间）
            boolean completed = latch.await(500, TimeUnit.MILLISECONDS);
            
            if (!completed) {
                Log.w(TAG, "taskShotPic timeout, trying fallback to PixelCopy");
                // 回退到直接使用 PixelCopy
                return captureFromSurfaceView();
            }
            
            Bitmap result = bitmapRef.get();
            Log.d(TAG, "captureFromRenderProxy result: " + (result != null ? result.getWidth() + "x" + result.getHeight() + ", config=" + result.getConfig() : "null"));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "captureFromRenderProxy exception", e);
            return null;
        }
    }
    
    /**
     * 从 SurfaceView 截取帧 (使用 PixelCopy API, Android O+)
     */
    private Bitmap captureFromSurfaceView() {
        if (!(mVideoView instanceof SurfaceView)) {
            Log.w(TAG, "captureFromSurfaceView: not a SurfaceView");
            return null;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "PixelCopy requires Android O (API 26) or higher");
            return null;
        }
        
        SurfaceView surfaceView = (SurfaceView) mVideoView;
        
        // 检查 Surface 是否有效
        if (surfaceView.getHolder() == null || surfaceView.getHolder().getSurface() == null 
            || !surfaceView.getHolder().getSurface().isValid()) {
            Log.w(TAG, "SurfaceView surface not valid");
            return null;
        }
        
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        
        if (width <= 0 || height <= 0) {
            Log.w(TAG, "SurfaceView size invalid: " + width + "x" + height);
            return null;
        }
        
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final AtomicReference<Integer> resultRef = new AtomicReference<>(-1);
        final CountDownLatch latch = new CountDownLatch(1);
        
        try {
            PixelCopy.request(
                surfaceView,
                bitmap,
                copyResult -> {
                    resultRef.set(copyResult);
                    latch.countDown();
                },
                mMainHandler
            );
            
            // 等待最多 500ms
            boolean completed = latch.await(500, TimeUnit.MILLISECONDS);
            
            if (!completed) {
                Log.w(TAG, "PixelCopy timeout");
                bitmap.recycle();
                return null;
            }
            
            int result = resultRef.get();
            if (result == PixelCopy.SUCCESS) {
                Log.d(TAG, "PixelCopy success: " + width + "x" + height);
                return bitmap;
            } else {
                Log.w(TAG, "PixelCopy failed with result: " + result);
                bitmap.recycle();
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "PixelCopy exception", e);
            bitmap.recycle();
            return null;
        }
    }
    
    /**
     * 裁剪字幕区域
     */
    private Bitmap cropSubtitleRegion(Bitmap frame) {
        if (frame == null) {
            return null;
        }
        
        int width = frame.getWidth();
        int height = frame.getHeight();
        
        int top = (int) (height * mSubtitleRegionTop);
        int bottom = (int) (height * mSubtitleRegionBottom);
        int cropHeight = bottom - top;
        
        if (cropHeight <= 0) {
            return null;
        }
        
        try {
            return Bitmap.createBitmap(frame, 0, top, width, cropHeight);
        } catch (Exception e) {
            Log.e(TAG, "Failed to crop subtitle region", e);
            return null;
        }
    }
    
    /**
     * 通知字幕识别结果
     */
    private void notifySubtitle(String originalText, String translatedText) {
        if (mCallback != null) {
            mMainHandler.post(() -> {
                mCallback.onSubtitleRecognized(originalText, translatedText);
            });
        }
    }
}
