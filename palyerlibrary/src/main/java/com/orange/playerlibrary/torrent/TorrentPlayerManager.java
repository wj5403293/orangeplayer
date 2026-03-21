package com.orange.playerlibrary.torrent;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import org.libtorrent4j.AlertListener;
import org.libtorrent4j.LibTorrent;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentHandle;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.Sha1Hash;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.PieceFinishedAlert;
import org.libtorrent4j.alerts.TorrentFinishedAlert;

import java.io.File;

public class TorrentPlayerManager implements AlertListener {

    private static final String TAG = "TorrentPlayerManager";

    private static TorrentPlayerManager sInstance;

    private final Context mContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private SessionManager mSession;
    private TorrentHandle mCurrentTorrent;
    private LocalHttpProxy mHttpProxy;
    private PowerManager.WakeLock mWakeLock;

    private boolean mInited;

    private TorrentCallback mCallback;

    private volatile String mPendingFileName;
    private volatile long mPendingFileSize;
    private volatile boolean mReadyNotified;

    public interface TorrentCallback {
        void onReady(String proxyUrl, String fileName, long fileSize);

        void onBufferProgress(int bufferedPieces, int totalPieces, long bufferedBytes);

        void onDownloadProgress(int progress, long downloadSpeed, long uploadSpeed);

        void onError(String error);
        
        /**
         * 磁力链接元数据解析进度回调
         * @param elapsedSeconds 已经过的秒数
         * @param totalSeconds 总超时秒数
         */
        default void onMagnetResolving(int elapsedSeconds, int totalSeconds) {
            // 默认空实现，子类可选择实现
        }
    }

    private TorrentPlayerManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized TorrentPlayerManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TorrentPlayerManager(context);
        }
        return sInstance;
    }

    public synchronized boolean isAvailable() {
        return TorrentSupport.isJlibtorrentClassAvailable() && TorrentSupport.isJlibtorrentNativeAvailable();
    }

    public synchronized void initIfNeeded() {
        if (mInited) return;
        
        // 配置 DHT 引导节点
        Log.d(TAG, "init: configuring DHT bootstrap nodes...");
        try {
            org.libtorrent4j.SettingsPack sp = new org.libtorrent4j.SettingsPack();
            
            // 设置 DHT 引导节点（包含国内和国际节点）
            String dhtBootstrapNodes = 
                    // 国际节点
                    "dht.libtorrent.org:25401," +
                    "router.bittorrent.com:6881," +
                    "router.utorrent.com:6881," +
                    "dht.transmissionbt.com:6881," +
                    "dht.aelitis.com:6881," +
                    // 国内节点（部分可能不稳定，但可以尝试）
                    "dht.anacrolix.link:42069," +
                    "bt.byr.cn:6969," +
                    "tracker.gbitt.info:80";
            sp.setString(org.libtorrent4j.swig.settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes);
            
            // 启用 DHT
            sp.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_dht.swigValue(), true);
            
            // 启用 LSD (Local Service Discovery) - 局域网发现
            sp.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_lsd.swigValue(), true);
            
            // 启用 UPnP 和 NAT-PMP - 自动端口映射
            sp.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_upnp.swigValue(), true);
            sp.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_natpmp.swigValue(), true);
            
            Log.d(TAG, "init: DHT bootstrap nodes configured: " + dhtBootstrapNodes);
            
            // 使用 SessionParams 包装 SettingsPack
            org.libtorrent4j.SessionParams params = new org.libtorrent4j.SessionParams(sp);
            
            mSession = new SessionManager(false);
            mSession.addListener(this);
            mSession.start(params);
            Log.d(TAG, "init: SessionManager started with params, isRunning=" + mSession.isRunning());
        } catch (Throwable t) {
            Log.w(TAG, "init: failed to configure DHT, using default settings", t);
            // 降级到默认配置
            mSession = new SessionManager();
            mSession.addListener(this);
            mSession.start();
            Log.d(TAG, "init: SessionManager started with defaults, isRunning=" + mSession.isRunning());
        }
        
        mInited = true;
        Log.d(TAG, "init: libtorrent=" + LibTorrent.version());
    }

    public void loadTorrent(File torrentFile, File saveDir, TorrentCallback callback) {
        Log.d(TAG, "loadTorrent() called with torrentFile=" + torrentFile + ", exists=" + torrentFile.exists());
        mCallback = callback;

        if (!TorrentSupport.isJlibtorrentClassAvailable()) {
            Log.e(TAG, "loadTorrent: jlibtorrent class not available");
            notifyError(TorrentSupport.getJlibtorrentMissingReason());
            return;
        }

        Log.d(TAG, "loadTorrent: jlibtorrent class available, calling initIfNeeded()");
        try {
            initIfNeeded();
        } catch (Throwable t) {
            Log.e(TAG, "loadTorrent: initIfNeeded() failed", t);
            notifyError("init jlibtorrent failed: " + t.getMessage());
            return;
        }

        Log.d(TAG, "loadTorrent: initIfNeeded() success, creating thread...");
        Log.d(TAG, "loadTorrent: initIfNeeded() success, creating thread...");
        new Thread(() -> {
            try {
                Log.d(TAG, "loadTorrent: thread started");
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                    Log.d(TAG, "loadTorrent: created saveDir=" + saveDir);
                }
                
                Log.d(TAG, "loadTorrent: creating TorrentInfo from file...");
                TorrentInfo torrentInfo = new TorrentInfo(torrentFile);
                Log.d(TAG, "loadTorrent: TorrentInfo created, name=" + torrentInfo.name() + ", numFiles=" + torrentInfo.numFiles());

                int fileIndex = getLargestFileIndex(torrentInfo);
                String filePath = torrentInfo.files().filePath(fileIndex);  // 使用 filePath 获取完整路径（如 BDMV/STREAM/00001.m2ts）
                String fileName = torrentInfo.files().fileName(fileIndex);  // 文件名（如 00001.m2ts）
                long fileSize = torrentInfo.files().fileSize(fileIndex);
                Log.d(TAG, "loadTorrent: largest file index=" + fileIndex + ", path=" + filePath + ", name=" + fileName + ", size=" + fileSize);

                mPendingFileName = filePath;  // 使用完整路径而不是文件名
                mPendingFileSize = fileSize;

                // jlibtorrent 1.2.x: download() returns void; TorrentHandle is delivered via AddTorrentAlert
                Log.d(TAG, "loadTorrent: calling mSession.download()...");
                mSession.download(torrentInfo, saveDir);
                Log.d(TAG, "loadTorrent: mSession.download() returned");
                
                // 检查 session 状态
                Log.d(TAG, "loadTorrent: session isRunning=" + mSession.isRunning() + ", isPaused=" + mSession.isPaused());
                
                // 尝试手动查找 torrent handle（libtorrent4j 2.x 可能不触发 AddTorrentAlert）
                try {
                    Thread.sleep(500); // 等待 torrent 被添加
                    
                    // 使用 info hash 查找 torrent
                    org.libtorrent4j.Sha1Hash infoHash = torrentInfo.infoHash();
                    Log.d(TAG, "loadTorrent: searching for torrent with infoHash=" + infoHash);
                    
                    org.libtorrent4j.TorrentHandle handle = mSession.find(infoHash);
                    if (handle != null && handle.isValid()) {
                        Log.d(TAG, "loadTorrent: found torrent handle via find(), processing...");
                        mCurrentTorrent = handle;
                        
                        // 设置顺序下载模式
                        handle.setFlags(org.libtorrent4j.TorrentFlags.SEQUENTIAL_DOWNLOAD);
                        Log.d(TAG, "loadTorrent: enabled sequential download mode");
                        
                        // 设置文件优先级（只下载最大的文件）
                        int numFiles = torrentInfo.numFiles();
                        org.libtorrent4j.Priority[] priorities = new org.libtorrent4j.Priority[numFiles];
                        for (int i = 0; i < numFiles; i++) {
                            if (i == fileIndex) {
                                priorities[i] = org.libtorrent4j.Priority.TOP_PRIORITY;
                            } else {
                                priorities[i] = org.libtorrent4j.Priority.IGNORE;
                            }
                        }
                        handle.prioritizeFiles(priorities);
                        Log.d(TAG, "loadTorrent: set file priorities, downloading file " + fileIndex + " only");
                        
                        // 启动 HTTP 代理并通知准备就绪
                        startHttpProxy(filePath);  // 使用完整路径而不是文件名
                        String proxyUrl = mHttpProxy.getUrl(filePath);  // 使用完整路径构造代理 URL
                        mReadyNotified = true;
                        Log.d(TAG, "loadTorrent: notifying ready, proxyUrl=" + proxyUrl);
                        notifyReady(proxyUrl, fileName, fileSize);
                    } else {
                        Log.w(TAG, "loadTorrent: torrent handle not found via find(), waiting for AddTorrentAlert...");
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "loadTorrent: sleep interrupted", e);
                }

                // Wait for AddTorrentAlert to start proxy & notifyReady (fallback)

            } catch (Throwable t) {
                Log.e(TAG, "loadTorrent failed", t);
                notifyError("loadTorrent failed: " + t.getMessage());
            }
        }).start();
        Log.d(TAG, "loadTorrent: thread.start() called");
    }

    public void loadMagnet(String magnetUri, File saveDir, TorrentCallback callback) {
        Log.d(TAG, "loadMagnet: uri=" + magnetUri + ", saveDir=" + saveDir);
        mCallback = callback;

        if (!TorrentSupport.isJlibtorrentClassAvailable()) {
            Log.e(TAG, "loadMagnet: jlibtorrent classes not available");
            notifyError(TorrentSupport.getJlibtorrentMissingReason());
            return;
        }

        try {
            initIfNeeded();
            Log.d(TAG, "loadMagnet: initIfNeeded done");
        } catch (Throwable t) {
            Log.e(TAG, "loadMagnet: init failed", t);
            notifyError("init jlibtorrent failed: " + t.getMessage());
            return;
        }

        new Thread(() -> {
            // 获取 WakeLock 防止系统杀死进程
            acquireWakeLock();
            
            try {
                Log.d(TAG, "loadMagnet: starting fetchMagnet thread");
                if (!saveDir.exists()) saveDir.mkdirs();

                // 添加公共 tracker 服务器到磁力链接（包含国内和国际 tracker）
                // 这些 tracker 可以帮助快速找到 peers
                String enhancedMagnetUri = magnetUri;
                if (!magnetUri.contains("&tr=")) {
                    // 添加多个公共 tracker（国际 + 国内）
                    enhancedMagnetUri = magnetUri +
                            // 国际 tracker（稳定性好）
                            "&tr=udp://tracker.opentrackr.org:1337/announce" +
                            "&tr=udp://open.tracker.cl:1337/announce" +
                            "&tr=udp://tracker.openbittorrent.com:6969/announce" +
                            "&tr=udp://tracker.torrent.eu.org:451/announce" +
                            "&tr=udp://open.stealth.si:80/announce" +
                            "&tr=udp://tracker.tiny-vps.com:6969/announce" +
                            "&tr=udp://tracker.moeking.me:6969/announce" +
                            "&tr=udp://explodie.org:6969/announce" +
                            "&tr=udp://tracker1.bt.moack.co.kr:80/announce" +
                            "&tr=udp://tracker.theoks.net:6969/announce" +
                            "&tr=http://tracker.openbittorrent.com:80/announce" +
                            "&tr=udp://opentracker.i2p.rocks:6969/announce" +
                            // 国内 tracker（可能被墙，但国内用户连接快）
                            "&tr=http://tracker.gbitt.info:80/announce" +
                            "&tr=http://tracker.bt4g.com:2095/announce" +
                            "&tr=http://t.nyaatracker.com:80/announce" +
                            "&tr=http://open.acgnxtracker.com:80/announce" +
                            "&tr=udp://tracker.btzoo.eu:80/announce" +
                            "&tr=http://share.camoe.cn:8080/announce";
                    Log.d(TAG, "loadMagnet: added public trackers (including CN trackers) to magnet URI");
                }

                // 启动进度更新线程
                final int TIMEOUT_SECONDS = 120;
                final long startTime = System.currentTimeMillis();
                final Thread progressThread = new Thread(() -> {
                    try {
                        while (true) {
                            Thread.sleep(1000); // 每秒更新一次
                            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                            if (elapsed >= TIMEOUT_SECONDS) {
                                break;
                            }
                            // 通知进度
                            final int elapsedSeconds = (int) elapsed;
                            mMainHandler.post(() -> {
                                if (mCallback != null) {
                                    mCallback.onMagnetResolving(elapsedSeconds, TIMEOUT_SECONDS);
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        // 线程被中断，正常退出
                    }
                });
                progressThread.start();

                // jlibtorrent 1.2.0.x signature: fetchMagnet(String uri, int timeout, File saveDir)
                // 增加超时时间到 120 秒，给 DHT 和 tracker 更多时间响应
                Log.d(TAG, "loadMagnet: calling fetchMagnet (will block up to 120s)...");
                byte[] data = mSession.fetchMagnet(enhancedMagnetUri, TIMEOUT_SECONDS, saveDir);
                
                // 停止进度更新线程
                progressThread.interrupt();
                
                Log.d(TAG, "loadMagnet: fetchMagnet returned, data=" + (data != null ? data.length + " bytes" : "null"));
                
                if (data == null) {
                    Log.e(TAG, "loadMagnet: magnet metadata fetch timeout or failed");
                    notifyError("magnet metadata fetch timeout");
                    return;
                }
                
                TorrentInfo torrentInfo = new TorrentInfo(data);
                Log.d(TAG, "loadMagnet: torrentInfo created, name=" + torrentInfo.name());

                int fileIndex = getLargestFileIndex(torrentInfo);
                String filePath = torrentInfo.files().filePath(fileIndex);  // 使用 filePath 获取完整路径（如 BDMV/STREAM/00001.m2ts）
                String fileName = torrentInfo.files().fileName(fileIndex);  // 文件名（如 00001.m2ts）
                long fileSize = torrentInfo.files().fileSize(fileIndex);

                mPendingFileName = filePath;  // 使用完整路径而不是文件名
                mPendingFileSize = fileSize;
                Log.d(TAG, "loadMagnet: largest file index=" + fileIndex + ", path=" + filePath + ", name=" + fileName + ", size=" + fileSize);

                // jlibtorrent 1.2.x: download() returns void; TorrentHandle is delivered via AddTorrentAlert
                mSession.download(torrentInfo, saveDir);
                Log.d(TAG, "loadMagnet: download() called, waiting for AddTorrentAlert...");

                // Wait for AddTorrentAlert to start proxy & notifyReady

            } catch (Throwable t) {
                Log.e(TAG, "loadMagnet failed", t);
                notifyError("loadMagnet failed: " + t.getMessage());
            } finally {
                // 释放 WakeLock
                releaseWakeLock();
            }
        }).start();
    }

    public synchronized void stop() {
        if (mHttpProxy != null) {
            mHttpProxy.stop();
            mHttpProxy = null;
        }
        mCurrentTorrent = null;
        mReadyNotified = false;
    }

    private void startHttpProxy(String fileName) {
        if (mHttpProxy != null) {
            mHttpProxy.stop();
        }
        mHttpProxy = new LocalHttpProxy(mCurrentTorrent);
        mHttpProxy.start();
        Log.d(TAG, "http proxy started: " + mHttpProxy.getPort());
    }

    private int getLargestFileIndex(TorrentInfo torrentInfo) {
        int largestIndex = 0;
        long largestSize = 0;
        int numFiles = torrentInfo.files().numFiles();
        for (int i = 0; i < numFiles; i++) {
            long size = torrentInfo.files().fileSize(i);
            if (size > largestSize) {
                largestSize = size;
                largestIndex = i;
            }
        }
        return largestIndex;
    }

    @Override
    public int[] types() {
        // Listen to all alert types by returning empty array
        // jlibtorrent 1.2.0.x will send all alerts
        Log.d(TAG, "types() called - listening to all alert types");
        return new int[0];
    }

    @Override
    public void alert(Alert<?> alert) {
        String alertType = alert.getClass().getSimpleName();
        // Log all alerts for debugging
        if (!(alertType.equals("StateUpdateAlert") || alertType.equals("StatsAlert"))) {
            Log.d(TAG, "alert: " + alertType + " - " + alert.message());
        }

        if (alert instanceof AddTorrentAlert) {
            AddTorrentAlert a = (AddTorrentAlert) alert;
            try {
                Log.d(TAG, "alert: received AddTorrentAlert");
                if (mReadyNotified) {
                    return;
                }

                // libtorrent4j: use handle().infoHash() directly
                TorrentHandle handle = a.handle();
                if (handle == null || !handle.isValid()) {
                    Log.e(TAG, "alert: handle is null or invalid");
                    return;
                }

                mCurrentTorrent = handle;

                // Get file info from torrent handle
                TorrentInfo torrentInfo = handle.torrentFile();
                if (torrentInfo != null) {
                    int fileIndex = getLargestFileIndex(torrentInfo);
                    String filePath = torrentInfo.files().filePath(fileIndex);  // 使用 filePath 获取完整路径（如 BDMV/STREAM/00001.m2ts）
                    String fileName = torrentInfo.files().fileName(fileIndex);  // 文件名（如 00001.m2ts）
                    long fileSize = torrentInfo.files().fileSize(fileIndex);
                    mPendingFileName = filePath;  // 使用完整路径而不是文件名
                    mPendingFileSize = fileSize;
                    Log.d(TAG, "alert: largest file index=" + fileIndex + ", path=" + filePath + ", name=" + fileName + ", size=" + fileSize);
                    
                    // 关键修复：设置顺序下载模式（边下边播必需）
                    handle.setFlags(org.libtorrent4j.TorrentFlags.SEQUENTIAL_DOWNLOAD);
                    Log.d(TAG, "alert: enabled sequential download mode");
                    
                    // 关键修复：设置文件优先级（只下载最大的文件）
                    int numFiles = torrentInfo.numFiles();
                    org.libtorrent4j.Priority[] priorities = new org.libtorrent4j.Priority[numFiles];
                    for (int i = 0; i < numFiles; i++) {
                        if (i == fileIndex) {
                            // 最大文件设置为最高优先级
                            priorities[i] = org.libtorrent4j.Priority.TOP_PRIORITY;
                        } else {
                            // 其他文件设置为忽略（不下载）
                            priorities[i] = org.libtorrent4j.Priority.IGNORE;
                        }
                    }
                    handle.prioritizeFiles(priorities);
                    Log.d(TAG, "alert: set file priorities, downloading file " + fileIndex + " only");
                }

                String filePath = mPendingFileName;  // mPendingFileName 现在存储的是完整路径
                long fileSize = mPendingFileSize;
                if (filePath != null) {
                    startHttpProxy(filePath);  // 使用完整路径
                    String proxyUrl = mHttpProxy.getUrl(filePath);  // 使用完整路径构造代理 URL
                    mReadyNotified = true;
                    // 从完整路径中提取文件名用于显示
                    String displayName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath;
                    Log.d(TAG, "alert: notifying ready, proxyUrl=" + proxyUrl + ", displayName=" + displayName);
                    notifyReady(proxyUrl, displayName, fileSize);
                }
            } catch (Throwable t) {
                Log.e(TAG, "alert: AddTorrentAlert handling failed", t);
                notifyError("AddTorrentAlert handling failed: " + t.getMessage());
            }
            return;
        }

        if (alert instanceof PieceFinishedAlert) {
            // TODO: buffer progress callback
            return;
        }

        if (alert instanceof TorrentFinishedAlert) {
            // TODO: finished callback
            return;
        }
    }

    private void notifyReady(String proxyUrl, String fileName, long fileSize) {
        mMainHandler.post(() -> {
            if (mCallback != null) mCallback.onReady(proxyUrl, fileName, fileSize);
        });
    }

    private void notifyError(String error) {
        mMainHandler.post(() -> {
            if (mCallback != null) mCallback.onError(error);
        });
    }

    private synchronized void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OrangePlayer:TorrentFetch");
                mWakeLock.setReferenceCounted(false);
            }
        }
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            mWakeLock.acquire(150000); // 150 秒超时，比 fetchMagnet 的 120 秒多一点
            Log.d(TAG, "acquireWakeLock: WakeLock acquired");
        }
    }

    private synchronized void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "releaseWakeLock: WakeLock released");
        }
    }
}
