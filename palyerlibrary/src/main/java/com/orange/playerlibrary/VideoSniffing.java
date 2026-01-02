package com.orange.playerlibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 视频嗅探工具类
 * 通过 WebView 拦截网络请求，识别视频资源
 * 
 * Requirements: 6.1 - THE OrangevideoView SHALL 支持视频嗅探功能 (startSniffing)
 */
public class VideoSniffing {
    
    private static final String TAG = "VideoSniffing";
    
    /** 调试模式 */
    public static boolean isDebug = false;
    
    @SuppressLint("StaticFieldLeak")
    private static WebView webView;
    
    /** 视频信息集合（去重存储）*/
    private static Set<VideoInfo> videoInfoSet = new HashSet<>();
    
    /** 主线程 Handler */
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /** 结束延迟 Handler */
    private static Handler finishHandler = new Handler(Looper.getMainLooper());
    
    /** 结束延迟判断时间（毫秒）*/
    private static final long FINISH_DELAY = 3000;
    
    /** 存储正在进行的网络连接，用于 stop 时取消 */
    private static Set<HttpURLConnection> activeConnections = new HashSet<>();
    
    /** 当前回调 */
    private static Call currentCall;

    /**
     * 嗅探回调接口
     */
    public interface Call {
        /**
         * 接收到视频资源
         * @param contentType 内容类型
         * @param headers 响应头
         * @param title 视频标题
         * @param url 视频地址
         */
        void received(String contentType, HashMap<String, String> headers, String title, String url);
        
        /**
         * 嗅探完成
         * @param videoList 视频列表
         * @param videoSize 视频数量
         */
        void onFinish(List<VideoInfo> videoList, int videoSize);
    }

    /**
     * 创建空资源响应
     */
    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }

    /**
     * 初始化 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private static void initWebView(WebView webView2) {
        webView2.setLayoutParams(isDebug ?
                new ViewGroup.LayoutParams(300, 300) :
                new ViewGroup.LayoutParams(0, 0));

        WebSettings settings = webView2.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setBlockNetworkImage(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        if (isDebug && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }


    /**
     * 判断是否为视频资源
     * @param str 内容类型或 URL
     * @return true 是视频资源
     */
    public static Boolean isVideoSource(String str) {
        if (str == null) return false;
        if (str.contains("text/plain; charset=utf-8") || str.contains("text/html")
                || str.contains("application/json") || str.contains("application/javascript")) {
            return false;
        }
        return str.contains("video/")
                || str.contains("application/vnd.apple.mpegurl")
                || str.contains("application/x-mpegurl")
                || str.contains("application/octet-stream")
                || str.contains(".mp4") || str.contains(".m3u8")
                || str.contains(".flv") || str.contains(".avi")
                || str.contains(".mov") || str.contains(".mkv");
    }

    /**
     * 开始视频嗅探（带自定义请求头）
     * @param context 上下文
     * @param url 网页地址
     * @param customHeaders 自定义请求头
     * @param call 回调
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void startSniffing(Context context, String url, Map<String, String> customHeaders, Call call) {
        // 初始化：清空历史数据、取消未完成任务
        stop(false);
        videoInfoSet.clear();
        finishHandler.removeCallbacksAndMessages(null);
        activeConnections.clear();
        currentCall = call;

        if (!(context instanceof Activity)) {
            if (call != null) {
                call.onFinish(new ArrayList<>(), 0);
            }
            return;
        }
        Activity activity = (Activity) context;

        WebView webView2 = new WebView(activity);
        webView = webView2;
        initWebView(webView2);

        activity.addContentView(webView, isDebug ?
                new LinearLayout.LayoutParams(300, 300) :
                new LinearLayout.LayoutParams(0, 0));

        // 加载 URL 时携带自定义请求头
        webView.setWebViewClient(new VideoWebViewClient(activity, call, customHeaders));
        if (customHeaders != null && !customHeaders.isEmpty()) {
            webView.loadUrl(url, customHeaders);
        } else {
            webView.loadUrl(url);
        }
    }

    /**
     * 开始视频嗅探
     * @param context 上下文
     * @param url 网页地址
     * @param call 回调
     */
    public static void startSniffing(Context context, String url, Call call) {
        startSniffing(context, url, null, call);
    }

    /**
     * 停止嗅探
     * @param z 是否完成
     */
    public static void stop(boolean z) {
        // 1. 取消所有未完成的网络请求
        for (HttpURLConnection conn : activeConnections) {
            if (conn != null) {
                conn.disconnect();
            }
        }
        activeConnections.clear();

        // 2. 清理延迟任务和回调引用
        finishHandler.removeCallbacksAndMessages(null);
        currentCall = null;

        // 3. 销毁 WebView
        WebView webView2 = webView;
        if (webView2 != null) {
            webView2.stopLoading();
            webView2.loadUrl("about:blank");
            ViewGroup parent = (ViewGroup) webView2.getParent();
            if (parent != null) {
                parent.removeView(webView2);
            }
            webView2.destroy();
            webView = null;
        }
    }

    /**
     * 触发完成回调
     */
    private static void triggerFinishCallback() {
        if (currentCall == null || webView == null) {
            return;
        }
        List<VideoInfo> videoList = new ArrayList<>(videoInfoSet);
        currentCall.onFinish(videoList, videoList.size());
        stop(true);
    }


    /**
     * 视频 WebView 客户端
     */
    private static class VideoWebViewClient extends WebViewClient {
        private final Activity activity;
        private final Call call;
        private String currentTitle;
        private final Map<String, String> customHeaders;

        public VideoWebViewClient(Activity activity, Call call, Map<String, String> customHeaders) {
            this.activity = activity;
            this.call = call;
            this.customHeaders = customHeaders;
            mainHandler.post(() -> {
                if (webView != null) {
                    currentTitle = webView.getTitle();
                }
            });
        }

        private void handleResponse(String contentType, HashMap<String, String> headers,
                                    String title, String url) {
            VideoInfo videoInfo = new VideoInfo(url, contentType, title, headers);
            videoInfoSet.add(videoInfo);
            if (call != null) {
                call.received(contentType, headers, title, url);
            }
            resetFinishDelay();
        }

        private void resetFinishDelay() {
            finishHandler.removeCallbacksAndMessages(null);
            finishHandler.postDelayed(VideoSniffing::triggerFinishCallback, FINISH_DELAY);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mainHandler.post(() -> {
                if (view == webView) {
                    currentTitle = view.getTitle();
                }
            });
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mainHandler.post(() -> {
                if (view != webView) {
                    return;
                }
                currentTitle = view.getTitle();
                resetFinishDelay();
            });
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            String reqContentType = request.getRequestHeaders().get("content-type");

            // 过滤非必要资源
            if (reqContentType != null && (reqContentType.contains("text/css")
                    || reqContentType.contains("image/")
                    || reqContentType.contains("font/"))
                    || url.contains(".css") || url.contains(".gif")
                    || url.contains(".ttf") || url.contains(".jpg")
                    || url.contains(".jpeg") || url.contains(".svg")
                    || url.contains(".ico") || url.contains(".png")) {
                return createEmptyResource();
            }

            // 过滤 JS、JSON 等非视频资源
            if (url.contains(".js") || !"*/*".equals(request.getRequestHeaders().get("Accept"))
                    || "application/json".equals(reqContentType)) {
                return super.shouldInterceptRequest(view, request);
            }

            HttpURLConnection connection = null;
            try {
                URL requestUrl = new URL(url);
                connection = (HttpURLConnection) requestUrl.openConnection();
                activeConnections.add(connection);

                connection.setRequestMethod(request.getMethod());
                if (customHeaders != null) {
                    for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                        connection.setRequestProperty(header.getKey(), header.getValue());
                    }
                }
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return super.shouldInterceptRequest(view, request);
                }

                String respContentType = connection.getHeaderField("Content-Type");
                HashMap<String, String> headers = new HashMap<>();
                for (int i = 0; ; i++) {
                    String key = connection.getHeaderFieldKey(i);
                    String value = connection.getHeaderField(i);
                    if (key == null && value == null) break;
                    if (key != null) {
                        headers.put(key, value);
                    }
                }

                HttpURLConnection finalConnection = connection;
                mainHandler.post(() -> {
                    if (webView == null || view != webView) {
                        return;
                    }
                    if (isVideoSource(respContentType) || isVideoSource(url)) {
                        handleResponse(respContentType, headers, currentTitle, url);
                    }
                });

                return isVideoSource(respContentType) ? createEmptyResource() :
                        new WebResourceResponse(respContentType, "utf-8", finalConnection.getInputStream());

            } catch (IOException e) {
                e.printStackTrace();
                return super.shouldInterceptRequest(view, request);
            } finally {
                if (connection != null) {
                    activeConnections.remove(connection);
                }
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            return !(url.startsWith("http://") || url.startsWith("https://"));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !(url.startsWith("http://") || url.startsWith("https://"));
        }
    }


    /**
     * 视频信息类
     */
    public static class VideoInfo {
        /** 视频地址 */
        public String url;
        /** 内容类型 */
        public String contentType;
        /** 视频标题 */
        public String title;
        /** 响应头信息 */
        public HashMap<String, String> headers;

        public VideoInfo(String url, String contentType, String title, HashMap<String, String> headers) {
            this.url = url;
            this.contentType = contentType;
            this.title = title;
            this.headers = headers;
        }

        /**
         * 清洗 URL（去重关键：忽略查询参数）
         */
        private String cleanUrl() {
            if (url == null) return "";
            int queryIndex = url.indexOf("?");
            return queryIndex != -1 ? url.substring(0, queryIndex) : url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VideoInfo videoInfo = (VideoInfo) o;
            return cleanUrl().equals(videoInfo.cleanUrl());
        }

        @Override
        public int hashCode() {
            return cleanUrl().hashCode();
        }
    }
}
