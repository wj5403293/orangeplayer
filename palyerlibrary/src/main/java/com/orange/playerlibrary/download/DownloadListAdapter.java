package com.orange.playerlibrary.download;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jeffmony.downloader.VideoDownloadManager;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import com.orange.playerlibrary.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载任务列表适配器
 */
public class DownloadListAdapter extends RecyclerView.Adapter<DownloadListAdapter.ViewHolder> {
    
    private static final String TAG = "DownloadListAdapter";
    
    private Context mContext;
    private List<VideoTaskItem> mItems = new ArrayList<>();
    private OnItemClickListener mItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(VideoTaskItem item);
        void onDeleteClick(VideoTaskItem item);
    }
    
    public DownloadListAdapter(Context context) {
        mContext = context;
    }
    
    public void setItems(List<VideoTaskItem> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }
    
    public void updateItem(VideoTaskItem item) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getUrl().equals(item.getUrl())) {
                mItems.set(i, item);
                notifyItemChanged(i);
                return;
            }
        }
        // 新任务，添加到列表
        mItems.add(0, item);
        notifyItemInserted(0);
    }
    
    public void removeItem(VideoTaskItem item) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getUrl().equals(item.getUrl())) {
                mItems.remove(i);
                notifyItemRemoved(i);
                notifyItemRangeChanged(i, mItems.size());
                return;
            }
        }
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.orange_item_download_task, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoTaskItem item = mItems.get(position);
        
        // 标题
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "未知视频");
        
        // 缩略图
        loadThumbnail(holder, item);
        
        // 状态
        setStateText(holder.tvStatus, item);
        
        // 进度
        setProgressInfo(holder, item);
        
        // 操作按钮
        setActionButton(holder.btnAction, item);
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            handleActionClick(item);
        });
        
        holder.btnAction.setOnClickListener(v -> {
            handleActionClick(item);
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onDeleteClick(item);
            }
        });
    }
    
    private void handleActionClick(VideoTaskItem item) {
        if (item.isInitialTask()) {
            // 初始状态 → 开始下载
            VideoDownloadManager.getInstance().startDownload(item);
        } else if (item.isRunningTask()) {
            // 下载中 → 暂停
            VideoDownloadManager.getInstance().pauseDownloadTask(item.getUrl());
        } else if (item.isInterruptTask()) {
            // 中断 → 恢复下载
            VideoDownloadManager.getInstance().resumeDownload(item.getUrl());
        } else if (item.isCompleted()) {
            // 已完成 → 回调播放
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(item);
            }
        }
    }
    
    private void setStateText(TextView tvStatus, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
            case VideoTaskState.PREPARE:
                tvStatus.setText("等待中");
                tvStatus.setTextColor(0xFFAAAAAA);
                break;
            case VideoTaskState.START:
            case VideoTaskState.DOWNLOADING:
                tvStatus.setText("下载中");
                tvStatus.setTextColor(0xFF0082EC);
                break;
            case VideoTaskState.PAUSE:
                tvStatus.setText("已暂停");
                tvStatus.setTextColor(0xFFFF9800);
                break;
            case VideoTaskState.SUCCESS:
                tvStatus.setText("已完成");
                tvStatus.setTextColor(0xFF4CAF50);
                break;
            case VideoTaskState.ERROR:
                tvStatus.setText("下载失败");
                tvStatus.setTextColor(0xFFF44336);
                break;
            default:
                tvStatus.setText("未下载");
                tvStatus.setTextColor(0xFFAAAAAA);
                break;
        }
    }
    
    private void setProgressInfo(ViewHolder holder, VideoTaskItem item) {
        int progress = (int) item.getPercent();
        holder.progressBar.setProgress(progress);
        
        String sizeInfo = formatSize(item.getDownloadSize()) + " / " + formatSize(item.getTotalSize());
        String percentInfo = String.format("%.1f%%", item.getPercent());
        holder.tvProgress.setText(sizeInfo + " (" + percentInfo + ")");
        
        // 下载中显示速度
        if (item.getTaskState() == VideoTaskState.DOWNLOADING && item.getSpeed() > 0) {
            holder.tvProgress.setText(sizeInfo + " (" + percentInfo + ") - " + formatSpeed(item.getSpeed()));
        }
    }
    
    private void setActionButton(ImageView btnAction, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
            case VideoTaskState.PREPARE:
            case VideoTaskState.START:
            case VideoTaskState.DOWNLOADING:
                btnAction.setImageResource(android.R.drawable.ic_media_pause);
                break;
            case VideoTaskState.PAUSE:
            case VideoTaskState.ERROR:
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                break;
            case VideoTaskState.SUCCESS:
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                break;
            default:
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                break;
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    private String formatSpeed(float speed) {
        if (speed < 1024) {
            return String.format("%.0f B/s", speed);
        } else if (speed < 1024 * 1024) {
            return String.format("%.1f KB/s", speed / 1024.0);
        } else {
            return String.format("%.1f MB/s", speed / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public int getItemCount() {
        return mItems.size();
    }
    
    // 线程池用于提取视频帧
    private ExecutorService mExecutor = Executors.newFixedThreadPool(2);
    
    /**
     * 加载缩略图
     * 优先级：封面URL > 视频URL(Glide提取帧) > 本地视频第一帧 > 默认图标
     * Glide 支持直接从视频URL提取帧，包括m3u8
     */
    private void loadThumbnail(ViewHolder holder, VideoTaskItem item) {
        String coverUrl = item.getCoverUrl();
        String videoUrl = item.getUrl();
        String filePath = item.getFilePath();
        
        // 1. 优先使用封面 URL
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(mContext)
                .load(coverUrl)
                .placeholder(R.drawable.ic_download)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.ivThumbnail);
            return;
        }
        
        // 2. 使用 Glide 从视频 URL 提取帧（支持 m3u8）
        if (videoUrl != null && !videoUrl.isEmpty()) {
            Glide.with(mContext)
                .asBitmap()
                .load(videoUrl)
                .placeholder(R.drawable.ic_download)
                .error(R.drawable.ic_download)
                .centerCrop()
                .timeout(5000)  // 5秒超时
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@androidx.annotation.NonNull Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            holder.ivThumbnail.setImageBitmap(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                        holder.ivThumbnail.setImageResource(R.drawable.ic_download);
                    }
                });
            return;
        }
        
        // 3. 已完成的本地视频，提取第一帧
        if (item.isCompleted() && filePath != null && new File(filePath).exists()) {
            // 使用 Glide 加载本地视频帧
            Glide.with(mContext)
                .asBitmap()
                .load(new File(filePath))
                .placeholder(R.drawable.ic_download)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@androidx.annotation.NonNull Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            holder.ivThumbnail.setImageBitmap(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {
                        holder.ivThumbnail.setImageResource(R.drawable.ic_download);
                    }
                });
            return;
        }
        
        // 4. 默认图标
        holder.ivThumbnail.setImageResource(R.drawable.ic_download);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvStatus;
        ProgressBar progressBar;
        TextView tvProgress;
        ImageView btnAction;
        ImageView btnDelete;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
