package com.orange.playerlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;

import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 橘子播放器完整控制器
 * 继承 OrangeStandardVideoController，管理 UI 组件和手势交互
 * 
 * Requirements: 2.1, 2.2, 2.6, 2.8, 2.9
 */
public class OrangeVideoController extends OrangeStandardVideoController {

    private static final String TAG = "OrangeVideoController";
    
    // ===== 调试模式 =====
    private static boolean sDebug = false;
    
    // ===== 视频标题 =====
    private String mVideoTitle = "";
    
    // ===== 视频列表（集数管理）=====
    private ArrayList<HashMap<String, Object>> mVideoList;
    
    // ===== 缩略图 =====
    private Object mThumbnail;
    
    // ===== 关联的播放器视图 =====
    private OrangevideoView mVideoView;
    
    // ===== 事件管理器 =====
    private VideoEventManager mVideoEventManager;
    
    // ===== 竖屏全屏相关 =====
    private boolean mIsPortraitFullScreen = false;
    private int mOriginalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    
    // ===== 弹幕相关 =====
    private boolean mIsAddDanmu = false;
    private com.orange.playerlibrary.interfaces.IDanmakuController mDanmakuController;
    
    // ===== 预览功能 =====
    private boolean mPreViewEnabled = false;
    
    // ===== 加载动画类型 =====
    private IndicatorType mCurrentIndicatorType = IndicatorType.BALL_PULSE;

    // ===== 构造函数 (Requirements: 2.1) =====

    public OrangeVideoController(Context context) {
        super(context);
        initController(context);
    }

    public OrangeVideoController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initController(context);
    }

    public OrangeVideoController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initController(context);
    }

    /**
     * 初始化控制器
     * Requirements: 2.1
     */
    private void initController(Context context) {
        mVideoList = new ArrayList<>();
    }
    
    /**
     * 设置关联的播放器视图并初始化事件管理器
     * 
     * @param videoView 播放器视图
     */
    public void setVideoView(OrangevideoView videoView) {
        mVideoView = videoView;
        
        // 设置播放器视图引用（用于获取网速）
        setVideoViewRef(videoView);
        
        // 初始化 VideoEventManager
        if (mVideoEventManager == null && videoView != null) {
            mVideoEventManager = new VideoEventManager(getContext(), videoView, this);
            debug("VideoEventManager initialized");
        }
    }
    
    /**
     * 获取事件管理器
     * 
     * @return 事件管理器
     */
    public VideoEventManager getVideoEventManager() {
        return mVideoEventManager;
    }


    // ===== 控制组件管理 (Requirements: 2.2) =====

    /**
     * 添加控制组件
     * Requirements: 2.2
     * 
     * @param components 控制组件
     */
    @Override
    public void addControlComponent(IControlComponent... components) {
        super.addControlComponent(components);
    }

    /**
     * 移除所有控制组件
     * Requirements: 2.2
     */
    @Override
    public void removeAllControlComponent() {
        super.removeAllControlComponent();
    }

    /**
     * 添加默认控制组件
     * Requirements: 2.2
     * 
     * @param title 视频标题
     * @param isLive 是否直播模式
     */
    public void addDefaultControlComponent(String title, boolean isLive) {
        android.util.Log.d(TAG, "addDefaultControlComponent: 开始, title=" + title + ", isLive=" + isLive);
        
        // 清除现有组件
        removeAllControlComponent();
        
        // 设置标题
        mVideoTitle = title;
        
        // 创建 VodControlView
        com.orange.playerlibrary.component.VodControlView vodControlView = 
                new com.orange.playerlibrary.component.VodControlView(getContext());
        vodControlView.setOrangeVideoController(this);
        addControlComponent(vodControlView);
        
        android.util.Log.d(TAG, "addDefaultControlComponent: VodControlView 创建完成");
        
        // 如果 VideoEventManager 已初始化，绑定控制器组件
        if (mVideoEventManager != null) {
            android.util.Log.d(TAG, "addDefaultControlComponent: 调用 bindControllerComponents");
            mVideoEventManager.bindControllerComponents(vodControlView);
            
            // 绑定 TitleView（如果存在）
            if (mVideoView != null) {
                com.orange.playerlibrary.component.TitleView titleView = mVideoView.getTitleView();
                if (titleView != null) {
                    android.util.Log.d(TAG, "addDefaultControlComponent: 调用 bindTitleView");
                    mVideoEventManager.bindTitleView(titleView);
                }
            }
            
            debug("VideoEventManager bound to VodControlView");
        } else {
            android.util.Log.d(TAG, "addDefaultControlComponent: mVideoEventManager 为 null");
        }
        
        debug("addDefaultControlComponent: title=" + title + ", isLive=" + isLive);
    }

    // ===== 视频集数管理 (Requirements: 2.6) =====

    /**
     * 添加视频到列表
     * Requirements: 2.6
     * 
     * @param name 视频名称
     * @param url 视频地址
     */
    public synchronized void addVideo(String name, String url) {
        addVideo(name, url, null);
    }

    /**
     * 添加视频到列表（带请求头）
     * Requirements: 2.6
     * 
     * @param name 视频名称
     * @param url 视频地址
     * @param headers 请求头
     */
    public synchronized void addVideo(String name, String url, HashMap<String, String> headers) {
        if (mVideoList == null) {
            mVideoList = new ArrayList<>();
        }
        
        HashMap<String, Object> video = new HashMap<>();
        video.put("name", name);
        video.put("url", url);
        video.put("headers", headers != null ? headers : new HashMap<>());
        mVideoList.add(video);
        
        debug("addVideo: name=" + name + ", url=" + url);
    }

    /**
     * 添加独立视频到列表
     * 
     * @param name 视频名称
     * @param url 视频地址
     * @param isIndependent 是否独立视频
     */
    public synchronized void addVideo(String name, String url, boolean isIndependent) {
        addVideo(name, url, isIndependent, null);
    }

    /**
     * 添加独立视频到列表（带请求头）
     * 
     * @param name 视频名称
     * @param url 视频地址
     * @param isIndependent 是否独立视频
     * @param headers 请求头
     */
    public synchronized void addVideo(String name, String url, boolean isIndependent, HashMap<String, String> headers) {
        if (mVideoList == null) {
            mVideoList = new ArrayList<>();
        }
        
        HashMap<String, Object> video = new HashMap<>();
        video.put("name", name);
        video.put("url", url);
        video.put("headers", headers != null ? headers : new HashMap<>());
        if (isIndependent) {
            video.put("dlsp", "独立");
        }
        mVideoList.add(video);
    }

    /**
     * 获取视频列表
     * Requirements: 2.6
     * 
     * @return 视频列表
     */
    public synchronized ArrayList<HashMap<String, Object>> getVideoList() {
        return mVideoList;
    }

    /**
     * 设置视频列表
     * 
     * @param list 视频列表
     */
    public synchronized void setVideoList(ArrayList<HashMap<String, Object>> list) {
        if (list != null) {
            // 确保每个视频都有 headers 字段
            for (HashMap<String, Object> item : list) {
                if (!item.containsKey("headers")) {
                    item.put("headers", new HashMap<>());
                }
            }
        }
        mVideoList = list;
    }

    /**
     * 清空视频列表
     * Requirements: 2.6
     */
    public synchronized void removeVideoList() {
        if (mVideoList != null) {
            mVideoList.clear();
        }
        mVideoList = null;
    }

    /**
     * 获取指定位置视频的请求头
     * 
     * @param position 位置索引
     * @return 请求头
     */
    @SuppressWarnings("unchecked")
    public synchronized HashMap<String, String> getVideoHeaders(int position) {
        if (mVideoList == null || position < 0 || position >= mVideoList.size()) {
            return new HashMap<>();
        }
        
        HashMap<String, Object> item = mVideoList.get(position);
        Object headers = item.get("headers");
        return headers instanceof HashMap ? (HashMap<String, String>) headers : new HashMap<>();
    }


    // ===== 全屏控制 (Requirements: 2.8) =====

    /**
     * 进入全屏
     * Requirements: 2.8
     * 
     * @return true 成功
     */
    public boolean startFullScreen() {
        if (mVideoView != null) {
            mVideoView.startFullScreen();
            return true;
        }
        return false;
    }

    /**
     * 退出全屏
     * Requirements: 2.8
     * 
     * @return true 成功
     */
    public boolean stopFullScreen() {
        if (mIsPortraitFullScreen) {
            exitPortraitFullScreen();
            return true;
        }
        
        if (mVideoView != null) {
            mVideoView.stopFullScreen();
            return true;
        }
        return false;
    }

    /**
     * 进入竖屏全屏
     * Requirements: 2.8
     */
    public void startPortraitFullScreen() {
        Activity activity = getActivity();
        if (activity == null) return;
        
        // 保存原始方向
        mOriginalOrientation = activity.getRequestedOrientation();
        
        // 设置竖屏
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // 进入全屏
        if (mVideoView != null) {
            mVideoView.startFullScreen();
        }
        
        mIsPortraitFullScreen = true;
        
        // 锁定竖屏方向
        lockPortraitOrientation(activity);
        
        debug("startPortraitFullScreen");
    }

    /**
     * 退出竖屏全屏
     * Requirements: 2.8
     */
    public void exitPortraitFullScreen() {
        Activity activity = getActivity();
        if (activity == null || !mIsPortraitFullScreen) return;
        
        mIsPortraitFullScreen = false;
        
        // 退出全屏
        if (mVideoView != null) {
            mVideoView.stopFullScreen();
        }
        
        // 恢复原始方向
        activity.setRequestedOrientation(mOriginalOrientation);
        
        debug("exitPortraitFullScreen");
    }

    /**
     * 是否处于竖屏全屏状态
     * 
     * @return true 竖屏全屏
     */
    public boolean isPortraitFullScreen() {
        return mIsPortraitFullScreen;
    }

    /**
     * 锁定竖屏方向
     */
    private void lockPortraitOrientation(Activity activity) {
        if (activity == null) return;
        
        // Activity 层面锁定
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Window 层面锁定
        Window window = activity.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            window.setAttributes(params);
            window.setWindowAnimations(0);
        }
    }

    // ===== 状态回调通知 (Requirements: 2.9) =====

    /**
     * 播放状态改变回调
     * Requirements: 2.9
     * 
     * @param playState 播放状态
     */
    @Override
    public void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        
        // 处理特殊状态
        switch (playState) {
            case PlayerConstants.STATE_PLAYING:
                // 播放开始，可以加载弹幕
                if (!mIsAddDanmu) {
                    mIsAddDanmu = true;
                    debug("onPlayStateChanged: STATE_PLAYING, danmu ready");
                }
                break;
            case PlayerConstants.STATE_STARTSNIFFING:
                // 开始嗅探
                showLoading();
                setNetSpeedText("视频资源嗅探中…");
                break;
            case PlayerConstants.STATE_ENDSNIFFING:
                // 结束嗅探
                hideLoading();
                setNetSpeedText("");
                break;
        }
        
        debug("onPlayStateChanged: " + playState);
    }

    /**
     * 播放器状态改变回调
     * Requirements: 2.9
     * 
     * @param playerState 播放器状态
     */
    @Override
    public void onPlayerStateChanged(int playerState) {
        super.onPlayerStateChanged(playerState);
        
        debug("onPlayerStateChanged: " + playerState);
    }


    // ===== 视频标题管理 =====

    /**
     * 设置视频标题
     * 
     * @param title 标题
     */
    public void setVideoTitle(String title) {
        mVideoTitle = title;
    }

    /**
     * 获取视频标题
     * 
     * @return 标题
     */
    public String getVideoTitle() {
        return mVideoTitle;
    }

    /**
     * 设置标题（兼容原 API）
     * 
     * @param title 标题
     */
    public void setTitle(String title) {
        if (mVideoTitle != null && !mVideoTitle.isEmpty()) {
            mVideoTitle = mVideoTitle + "|" + title;
        } else {
            mVideoTitle = title;
        }
    }

    // ===== 缩略图管理 =====

    /**
     * 设置缩略图
     * 
     * @param thumbnail 缩略图
     */
    public void setThumbnail(Object thumbnail) {
        mThumbnail = thumbnail;
    }

    /**
     * 获取缩略图
     * 
     * @return 缩略图
     */
    public Object getThumbnail() {
        return mThumbnail;
    }

    // ===== 播放器视图关联 =====

    /**
     * 获取关联的播放器视图
     * 
     * @return 播放器视图
     */
    public OrangevideoView getVideoView() {
        return mVideoView;
    }

    // ===== 弹幕相关 =====

    /**
     * 设置是否添加弹幕
     * 
     * @param add 是否添加
     */
    public void isaddDanmu(boolean add) {
        mIsAddDanmu = add;
    }

    /**
     * 是否已添加弹幕
     * 
     * @return true 已添加
     */
    public boolean isAddDanmu() {
        return mIsAddDanmu;
    }
    
    /**
     * 设置弹幕控制器
     * App 层实现 IDanmakuController 接口后，通过此方法设置
     * 
     * @param controller 弹幕控制器
     */
    public void setDanmakuController(com.orange.playerlibrary.interfaces.IDanmakuController controller) {
        mDanmakuController = controller;
        debug("setDanmakuController: " + controller);
    }
    
    /**
     * 获取弹幕控制器
     * 
     * @return 弹幕控制器，如果未设置返回 null
     */
    public com.orange.playerlibrary.interfaces.IDanmakuController getDanmakuController() {
        return mDanmakuController;
    }
    
    /**
     * 检查弹幕功能是否可用
     * 
     * @return true 如果弹幕库已导入且控制器已设置
     */
    public boolean isDanmakuAvailable() {
        return DanmakuHelper.isDanmakuLibraryAvailable() && mDanmakuController != null;
    }

    // ===== 预览功能 =====

    /**
     * 设置是否启用预览
     * 
     * @param enabled 是否启用
     */
    public void setPreViewEnabled(boolean enabled) {
        mPreViewEnabled = enabled;
    }

    /**
     * 是否启用预览
     * 
     * @return true 启用
     */
    public boolean isPreViewEnabled() {
        return mPreViewEnabled;
    }

    // ===== 加载动画类型 =====

    /**
     * 加载动画类型枚举
     */
    public enum IndicatorType {
        BALL_BEAT(1, "BallBeatIndicator"),
        BALL_CLIP_ROTATE(2, "BallClipRotateIndicator"),
        BALL_CLIP_ROTATE_MULTIPLE(3, "BallClipRotateMultipleIndicator"),
        BALL_CLIP_ROTATE_PULSE(4, "BallClipRotatePulseIndicator"),
        BALL_GRID_BEAT(5, "BallGridBeatIndicator"),
        BALL_GRID_PULSE(6, "BallGridPulseIndicator"),
        BALL_PULSE(7, "BallPulseIndicator"),
        BALL_PULSE_RISE(8, "BallPulseRiseIndicator"),
        BALL_PULSE_SYNC(9, "BallPulseSyncIndicator"),
        BALL_ROTATE(10, "BallRotateIndicator"),
        BALL_SCALE(11, "BallScaleIndicator"),
        BALL_SCALE_MULTIPLE(12, "BallScaleMultipleIndicator"),
        BALL_SCALE_RIPPLE(13, "BallScaleRippleIndicator"),
        BALL_SCALE_RIPPLE_MULTIPLE(14, "BallScaleRippleMultipleIndicator"),
        BALL_SPIN_FADE_LOADER(15, "BallSpinFadeLoaderIndicator"),
        BALL_TRIANGLE_PATH(16, "BallTrianglePathIndicator"),
        BALL_ZIG_ZAG_DEFLECT(17, "BallZigZagDeflectIndicator"),
        BALL_ZIG_ZAG(18, "BallZigZagIndicator"),
        CUBE_TRANSITION(19, "CubeTransitionIndicator"),
        LINE_SCALE(20, "LineScaleIndicator"),
        LINE_SCALE_PARTY(21, "LineScalePartyIndicator"),
        LINE_SCALE_PULSE_OUT(22, "LineScalePulseOutIndicator"),
        LINE_SCALE_PULSE_OUT_RAPID(23, "LineScalePulseOutRapidIndicator"),
        LINE_SPIN_FADE_LOADER(24, "LineSpinFadeLoaderIndicator"),
        PACMAN(25, "PacmanIndicator"),
        SEMI_CIRCLE_SPIN(26, "SemiCircleSpinIndicator"),
        SQUARE_SPIN(27, "SquareSpinIndicator"),
        TRIANGLE_SKEW_SPIN(28, "TriangleSkewSpinIndicator");

        private final int id;
        private final String indicatorName;

        IndicatorType(int id, String indicatorName) {
            this.id = id;
            this.indicatorName = indicatorName;
        }

        public int getId() {
            return id;
        }

        public String getIndicatorName() {
            return indicatorName;
        }

        public static IndicatorType findById(int id) {
            for (IndicatorType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 设置加载动画类型
     * Requirements: 6.5 - THE OrangeVideoController SHALL 支持多种加载动画样式 (setLoading)
     * 
     * @param type 动画类型
     */
    public void setLoading(IndicatorType type) {
        if (type == null) {
            debug("setLoading: type is null, using default");
            type = IndicatorType.BALL_PULSE;
        }
        mCurrentIndicatorType = type;
        
        // 创建并设置指示器
        com.orange.playerlibrary.loading.Indicator indicator = 
                com.orange.playerlibrary.loading.IndicatorFactory.createIndicator(type);
        
        // 如果有 AVLoadingIndicatorView，设置指示器
        if (mLoadingContainer != null) {
            View loadingView = mLoadingContainer.findViewWithTag("loading_indicator");
            if (loadingView instanceof com.orange.playerlibrary.loading.AVLoadingIndicatorView) {
                ((com.orange.playerlibrary.loading.AVLoadingIndicatorView) loadingView).setIndicator(indicator);
            }
        }
        
        debug("setLoading: " + type.getIndicatorName());
    }

    /**
     * 设置加载动画类型（通过 ID）
     * 
     * @param typeId 动画类型 ID
     */
    public void setLoading(int typeId) {
        IndicatorType type = IndicatorType.findById(typeId);
        setLoading(type);
    }

    /**
     * 获取当前加载动画类型
     * @return 加载动画类型
     */
    public IndicatorType getCurrentIndicatorType() {
        return mCurrentIndicatorType;
    }


    // ===== 播放列表可见性 =====

    /**
     * 设置播放列表可见性
     * 
     * @param visible 是否可见
     */
    public void setplaylistVisibility(boolean visible) {
        // TODO: 实现播放列表可见性控制
        debug("setplaylistVisibility: " + visible);
    }

    /**
     * 设置加载速度文本
     * 
     * @param text 文本
     */
    public void setLoadingSpeedText(String text) {
        setNetSpeedText(text);
    }

    // ===== 辅助方法 =====

    /**
     * 获取 Activity
     * 
     * @return Activity
     */
    public Activity getActivity() {
        Context context = getContext();
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    /**
     * 是否直播模式
     * 
     * @return true 直播
     */
    public boolean isLiveVideoModel() {
        if (mVideoView != null) {
            return mVideoView.isLiveVideo();
        }
        return false;
    }

    // ===== 调试相关 =====

    /**
     * 设置调试模式
     * 
     * @param debug 是否调试
     */
    public static void setdebug(boolean debug) {
        sDebug = debug;
    }

    /**
     * 设置调试模式（兼容原 API）
     * 
     * @param debug 是否调试
     */
    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    /**
     * 是否调试模式
     * 
     * @return true 调试模式
     */
    public static boolean isdebug() {
        return sDebug;
    }

    /**
     * 调试日志
     * 
     * @param message 日志信息
     */
    protected void debug(Object message) {
        if (sDebug && sDebugLogger != null) {
            sDebugLogger.log(TAG, message);
        }
    }

    /**
     * 调试日志接口
     */
    public interface DebugLogger {
        void log(String tag, Object message);
    }

    /**
     * 默认调试日志实现
     */
    public static DebugLogger sDebugLogger = new DebugLogger() {
        @Override
        public void log(String tag, Object message) {
            if (sDebug) {
                android.util.Log.d(tag, String.valueOf(message));
            }
        }
    };

    /**
     * 设置调试日志实现
     * 
     * @param logger 日志实现
     */
    public static void setDebugLogger(DebugLogger logger) {
        if (logger != null) {
            sDebugLogger = logger;
        }
    }
}
