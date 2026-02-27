package com.orange.playerlibrary.download;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.listener.DownloadListener;
import com.jeffmony.downloader.listener.IDownloadInfosCallback;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.orange.playerlibrary.R;

import java.io.File;
import java.util.List;

/**
 * 下载列表对话框
 * 使用 VideoDownloadManager 管理下载任务
 */
public class DownloadListDialog extends Dialog {
    
    private static final String TAG = "DownloadListDialog";
    
    private Context mContext;
    private RecyclerView mRecyclerView;
    private DownloadListAdapter mAdapter;
    private LinearLayout mEmptyView;
    private ImageView mBtnClose;
    private TextView mBtnClear;
    private TextView mBtnStartAll;
    private TextView mBtnPauseAll;
    private TextView mTvTaskCount;
    
    private long mLastProgressTimeStamp = 0;
    
    public DownloadListDialog(@NonNull Context context) {
        super(context, R.style.OrangeDialog);
        mContext = context;
        init();
    }
    
    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.orange_dialog_download_list);
        
        // 设置对话框宽度
        Window window = getWindow();
        if (window != null) {
            // 全屏模式下不拉出状态栏和导航栏
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            );
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            // 清除 NOT_FOCUSABLE 标志，恢复焦点
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            // 设置在锁屏和全屏模式下正常显示
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        // 初始化视图
        mRecyclerView = findViewById(R.id.recycler_view);
        mEmptyView = findViewById(R.id.empty_view);
        mBtnClose = findViewById(R.id.btn_close);
        mBtnClear = findViewById(R.id.btn_clear);
        mBtnStartAll = findViewById(R.id.btn_start_all);
        mBtnPauseAll = findViewById(R.id.btn_pause_all);
        mTvTaskCount = findViewById(R.id.tv_task_count);
        
        // 设置 RecyclerView
        mAdapter = new DownloadListAdapter(mContext);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(mAdapter);
        
        // 设置监听器
        mAdapter.setOnItemClickListener(new DownloadListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(VideoTaskItem item) {
                // 已完成的任务，点击播放
                if (item.isCompleted()) {
                    String filePath = item.getFilePath();
                    if (filePath != null && new File(filePath).exists()) {
                        // 可以通过回调通知外部播放
                        android.widget.Toast.makeText(mContext, 
                            "视频已保存: " + filePath, 
                            android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            }
            
            @Override
            public void onDeleteClick(VideoTaskItem item) {
                showDeleteConfirmDialog(item);
            }
        });
        
        // 关闭按钮
        mBtnClose.setOnClickListener(v -> dismiss());
        
        // 全部开始按钮
        mBtnStartAll.setOnClickListener(v -> startAllDownloads());
        
        // 全部暂停按钮
        mBtnPauseAll.setOnClickListener(v -> pauseAllDownloads());
        
        // 全部删除按钮
        mBtnClear.setOnClickListener(v -> showClearAllConfirmDialog());
        
        // 设置下载监听器
        setupDownloadListener();
        
        // 获取已有任务
        fetchDownloadItems();
    }
    
    /**
     * 设置下载监听器
     */
    private void setupDownloadListener() {
        try {
            VideoDownloadManager manager = VideoDownloadManager.getInstance();
            if (manager == null) return;
            
            manager.setGlobalDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadDefault(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadPending(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadPrepare(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadStart(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadProgress(VideoTaskItem item) {
                    // 限制更新频率（每秒更新一次）
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mLastProgressTimeStamp > 1000) {
                        updateItem(item);
                        mLastProgressTimeStamp = currentTime;
                    }
                }
                
                @Override
                public void onDownloadSpeed(VideoTaskItem item) {
                    // 速度更新不需要单独处理，在 progress 中一起显示
                }
                
                @Override
                public void onDownloadPause(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadError(VideoTaskItem item) {
                    updateItem(item);
                }
                
                @Override
                public void onDownloadSuccess(VideoTaskItem item) {
                    updateItem(item);
                    // 显示完成提示
                    showToast("下载完成: " + item.getTitle());
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to setup download listener", e);
        }
    }
    
    /**
     * 获取下载任务列表
     */
    private void fetchDownloadItems() {
        try {
            VideoDownloadManager manager = VideoDownloadManager.getInstance();
            if (manager == null) {
                showEmpty(true);
                return;
            }
            
            manager.fetchDownloadItems(new IDownloadInfosCallback() {
                @Override
                public void onDownloadInfos(List<VideoTaskItem> items) {
                    if (items != null && !items.isEmpty()) {
                        mAdapter.setItems(items);
                        showEmpty(false);
                    } else {
                        showEmpty(true);
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to fetch download items", e);
            showEmpty(true);
        }
    }
    
    /**
     * 更新单个任务
     */
    private void updateItem(final VideoTaskItem item) {
        if (mRecyclerView != null && mRecyclerView.getHandler() != null) {
            mRecyclerView.post(() -> {
                mAdapter.updateItem(item);
                showEmpty(mAdapter.getItemCount() == 0);
            });
        }
    }
    
    /**
     * 显示/隐藏空状态
     */
    private void showEmpty(boolean show) {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        // 更新任务计数
        updateTaskCount();
    }
    
    /**
     * 更新任务计数
     */
    private void updateTaskCount() {
        if (mTvTaskCount != null) {
            int count = mAdapter.getItemCount();
            mTvTaskCount.setText("共 " + count + " 个任务");
        }
    }
    
    /**
     * 全部开始下载
     */
    private void startAllDownloads() {
        try {
            VideoDownloadManager manager = VideoDownloadManager.getInstance();
            if (manager == null) return;
            
            manager.fetchDownloadItems(new IDownloadInfosCallback() {
                @Override
                public void onDownloadInfos(List<VideoTaskItem> items) {
                    if (items == null) return;
                    int started = 0;
                    for (VideoTaskItem item : items) {
                        // 只开始暂停或错误的任务
                        if (item.isInterruptTask() || item.getTaskState() == VideoTaskState.ERROR) {
                            manager.resumeDownload(item.getUrl());
                            started++;
                        }
                    }
                    final int count = started;
                    if (mRecyclerView != null) {
                        mRecyclerView.post(() -> {
                            if (count > 0) {
                                showToast("已开始 " + count + " 个任务");
                            } else {
                                showToast("没有可开始的任务");
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to start all downloads", e);
        }
    }
    
    /**
     * 全部暂停下载
     */
    private void pauseAllDownloads() {
        try {
            VideoDownloadManager manager = VideoDownloadManager.getInstance();
            if (manager == null) return;
            
            manager.fetchDownloadItems(new IDownloadInfosCallback() {
                @Override
                public void onDownloadInfos(List<VideoTaskItem> items) {
                    if (items == null) return;
                    int paused = 0;
                    for (VideoTaskItem item : items) {
                        int state = item.getTaskState();
                        // 暂停所有进行中的任务（包括等待、准备、下载中）
                        if (state == VideoTaskState.PENDING || 
                            state == VideoTaskState.PREPARE || 
                            state == VideoTaskState.START || 
                            state == VideoTaskState.DOWNLOADING) {
                            manager.pauseDownloadTask(item.getUrl());
                            paused++;
                        }
                    }
                    final int count = paused;
                    if (mRecyclerView != null) {
                        mRecyclerView.post(() -> {
                            if (count > 0) {
                                showToast("已暂停 " + count + " 个任务");
                                // 刷新列表显示暂停状态
                                fetchDownloadItems();
                            } else {
                                showToast("没有可暂停的任务");
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to pause all downloads", e);
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(VideoTaskItem item) {
        try {
            Class<?> popTipClass = Class.forName("com.kongzue.dialogx.dialogs.PopTip");
            java.lang.reflect.Method showMethod = popTipClass.getMethod("show", 
                int.class, String.class, String.class);
            Object popTip = showMethod.invoke(null, 
                R.drawable.ic_delete, 
                "确定删除该下载任务？", 
                "删除");
            
            // 使用反射设置按钮点击监听
            Class<?> listenerClass = Class.forName("com.kongzue.dialogx.interfaces.OnDialogButtonClickListener");
            java.lang.reflect.Method setButtonMethod = popTipClass.getMethod("setButton", listenerClass);
            
            // 创建动态代理实现监听器接口
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("onClick")) {
                        // 删除任务
                        VideoDownloadManager.getInstance().deleteVideoTask(item.getUrl(), true);
                        mAdapter.removeItem(item);
                        showEmpty(mAdapter.getItemCount() == 0);
                        showToast("已删除");
                        return false;
                    }
                    return null;
                });
            setButtonMethod.invoke(popTip, listener);
            
            // 显示
            java.lang.reflect.Method showLongMethod = popTipClass.getMethod("showLong");
            showLongMethod.invoke(popTip);
            
        } catch (Exception e) {
            // DialogX 不可用，使用原生对话框
            showNativeDeleteConfirmDialog(item);
        }
    }
    
    /**
     * 原生删除确认对话框（DialogX 不可用时使用）
     */
    private void showNativeDeleteConfirmDialog(VideoTaskItem item) {
        new android.app.AlertDialog.Builder(mContext)
            .setTitle("删除下载任务")
            .setMessage("确定删除该下载任务？")
            .setPositiveButton("删除", (dialog, which) -> {
                VideoDownloadManager.getInstance().deleteVideoTask(item.getUrl(), true);
                mAdapter.removeItem(item);
                showEmpty(mAdapter.getItemCount() == 0);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 显示清空确认对话框
     */
    private void showClearAllConfirmDialog() {
        try {
            Class<?> popTipClass = Class.forName("com.kongzue.dialogx.dialogs.PopTip");
            java.lang.reflect.Method showMethod = popTipClass.getMethod("show", 
                int.class, String.class, String.class);
            Object popTip = showMethod.invoke(null, 
                R.drawable.ic_delete_all, 
                "确定清空所有下载任务？", 
                "清空");
            
            // 使用反射设置按钮点击监听
            Class<?> listenerClass = Class.forName("com.kongzue.dialogx.interfaces.OnDialogButtonClickListener");
            java.lang.reflect.Method setButtonMethod = popTipClass.getMethod("setButton", listenerClass);
            
            // 创建动态代理实现监听器接口
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("onClick")) {
                        VideoDownloadManager.getInstance().deleteAllVideoFiles();
                        mAdapter.setItems(null);
                        showEmpty(true);
                        showToast("已清空");
                        return false;
                    }
                    return null;
                });
            setButtonMethod.invoke(popTip, listener);
            
            java.lang.reflect.Method showLongMethod = popTipClass.getMethod("showLong");
            showLongMethod.invoke(popTip);
            
        } catch (Exception e) {
            // DialogX 不可用，使用原生对话框
            showNativeClearAllConfirmDialog();
        }
    }
    
    /**
     * 原生清空确认对话框
     */
    private void showNativeClearAllConfirmDialog() {
        new android.app.AlertDialog.Builder(mContext)
            .setTitle("清空下载任务")
            .setMessage("确定清空所有下载任务？")
            .setPositiveButton("清空", (dialog, which) -> {
                VideoDownloadManager.getInstance().deleteAllVideoFiles();
                mAdapter.setItems(null);
                showEmpty(true);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 显示 Toast
     */
    private void showToast(String message) {
        if (mRecyclerView != null && mRecyclerView.getHandler() != null) {
            mRecyclerView.post(() -> {
                android.widget.Toast.makeText(mContext, message, android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    @Override
    public void show() {
        super.show();
        // 每次显示时刷新列表
        fetchDownloadItems();
    }
    
    @Override
    public void dismiss() {
        // 清理适配器资源
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
        super.dismiss();
    }
}
