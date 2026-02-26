package com.jeffmony.downloader.task;

import android.os.Handler;
import android.os.HandlerThread;

import com.jeffmony.downloader.common.DownloadConstants;
import com.jeffmony.downloader.listener.IVideoCacheListener;
import com.jeffmony.downloader.model.MultiRangeInfo;
import com.jeffmony.downloader.model.VideoRange;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.utils.LogUtils;
import com.jeffmony.downloader.utils.VideoDownloadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiSegVideoDownloadTask extends VideoDownloadTask {

    private HandlerThread mMultiMsgThread;
    private Handler mMultiMsgHandler;
    private List<VideoRange> mRangeList;

    private final long mTotalLength;

    private final int mThreadCount;

    public MultiSegVideoDownloadTask(VideoTaskItem taskItem, Map<String, String> headers) {
        super(taskItem, headers);
        LogUtils.i(DownloadConstants.TAG, "MultiSegVideoDownloadTask constructor start");
        LogUtils.i(DownloadConstants.TAG, "  URL: " + taskItem.getUrl());
        LogUtils.i(DownloadConstants.TAG, "  FinalUrl: " + taskItem.getFinalUrl());
        LogUtils.i(DownloadConstants.TAG, "  TotalSize: " + taskItem.getTotalSize());
        LogUtils.i(DownloadConstants.TAG, "  MimeType: " + taskItem.getMimeType());
        LogUtils.i(DownloadConstants.TAG, "  SaveDir: " + mSaveDir.getAbsolutePath());
        LogUtils.i(DownloadConstants.TAG, "  SaveDir exists: " + mSaveDir.exists());
        
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mTotalLength = taskItem.getTotalSize();
        mThreadCount = VideoDownloadUtils.getDownloadConfig().getConcurrentCount();

        mRangeList = new ArrayList<>();

        mMultiMsgThread = new HandlerThread("Multi-thread download");
        mMultiMsgThread.start();
        mMultiMsgHandler = new Handler(mMultiMsgThread.getLooper());
        
        LogUtils.i(DownloadConstants.TAG, "MultiSegVideoDownloadTask constructor end");
    }

    @Override
    public void startDownload() {
        mDownloadTaskListener.onTaskStart(mTaskItem.getUrl());
        startDownloadVideo();
    }

    private void startDownloadVideo() {
        LogUtils.i(DownloadConstants.TAG, "startDownloadVideo() called");
        try {
            if (mTaskItem.isCompleted()) {
                LogUtils.i(DownloadConstants.TAG, "BaseVideoDownloadTask local file.");
                notifyDownloadFinish();
                return;
            }
            
            LogUtils.i(DownloadConstants.TAG, "Creating ThreadPoolExecutor with " + (mThreadCount + 1) + " threads");
            mDownloadExecutor = new ThreadPoolExecutor(
                    mThreadCount + 1, mThreadCount + 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            LogUtils.i(DownloadConstants.TAG, "ThreadPoolExecutor created successfully");
        } catch (Exception e) {
            LogUtils.e(DownloadConstants.TAG, "Error in startDownloadVideo: " + e.getMessage() + "\n" + android.util.Log.getStackTraceString(e));
            notifyDownloadError(e);
            return;
        }


        long requestSegment;
        mRangeList.clear();

        List<Long> startList = new ArrayList<>();

        LogUtils.i(DownloadConstants.TAG, "Reading range info from: " + mSaveDir.getAbsolutePath());
        MultiRangeInfo rangeInfo = VideoDownloadUtils.readRangeInfo(mSaveDir);
        LogUtils.i(DownloadConstants.TAG, "Range info result: " + (rangeInfo == null ? "null (new download)" : "exists (resume)"));
        
        if (rangeInfo == null) {
            requestSegment = mThreadCount;
            long segSize = mTotalLength / requestSegment;
            LogUtils.i(DownloadConstants.TAG, "Creating " + requestSegment + " segments, each " + segSize + " bytes");
            /// 三个线程，每个线程下载segSize大小

            for (int i = 0; i < requestSegment; i++) {
                long requestStart = segSize * i;
                long requestEnd = segSize * (i + 1) - 1;
                if (i == requestSegment - 1) {
                    requestEnd = mTotalLength;
                }

                VideoRange range = new VideoRange(requestStart, requestEnd);
                LogUtils.i(DownloadConstants.TAG, "  Segment " + i + ": " + requestStart + "-" + requestEnd);

                mRangeList.add(range);
                startList.add(0L);
            }
        } else {
            List<Integer> ids = rangeInfo.getIds();
            requestSegment = ids.size();
            List<Long> ends = rangeInfo.getEnds();
            List<Long> sizes = rangeInfo.getSizes();
            for (int i = 0; i < requestSegment; i++) {
                if (i == 0) {
                    VideoRange range = new VideoRange(sizes.get(i), ends.get(i));
                    mRangeList.add(range);
                } else {
                    VideoRange range = new VideoRange(ends.get(i - 1) + sizes.get(i), ends.get(i));
                    mRangeList.add(range);
                }

            }
            startList.addAll(sizes);
        }


        Map<Integer, Long> cachedMap = new HashMap<>();
        Map<Integer, Boolean> completedMap = new HashMap<>();

        LogUtils.i(DownloadConstants.TAG, "Starting " + requestSegment + " download threads");
        for (int i = 0; i < requestSegment; i++) {
            cachedMap.put(i, 0L);
            completedMap.put(i, false);
            
            try {
                LogUtils.i(DownloadConstants.TAG, "Creating thread " + i + " for range: " + mRangeList.get(i));
                SingleVideoCacheThread thread = new SingleVideoCacheThread(mFinalUrl, mHeaders, mRangeList.get(i), mTotalLength, mSaveDir.getAbsolutePath());

                thread.setHandler(mMultiMsgHandler);

                thread.setId(i);

            thread.setCacheListener(new IVideoCacheListener() {

                @Override
                public void onFailed(VideoRange range, int id, Exception e) {
                    notifyDownloadError(e);
                }

                @Override
                public void onProgress(VideoRange range, int id, long cachedSize) {
                    long size = startList.get(id) +  cachedSize;
                    LogUtils.i(DownloadConstants.TAG, "onProgress ID="+id+", size=" + size);
                    cachedMap.put(id, size);
                    notifyOnProgress(cachedMap);
                }

                @Override
                public void onRangeCompleted(VideoRange range, int id) {
                    LogUtils.i(DownloadConstants.TAG, "onRangeCompleted Range=" + range +", completeMap size=" + completedMap.size());
                    completedMap.put(id, true);

                    boolean completed = true;
                    for (boolean tag : completedMap.values()) {
                        LogUtils.i(DownloadConstants.TAG, "onRangeCompleted tag = " + tag);
                        if (!tag) {
                            completed = false;
                            break;
                        }
                    }

                    if (completed) {
                        LogUtils.i(DownloadConstants.TAG, "TotalSize=" + mTotalLength);
                        notifyDownloadFinish();
                    }
                }

                @Override
                public void onCompleted(VideoRange range, int id) {

                }
            });

                LogUtils.i(DownloadConstants.TAG, "Submitting thread " + i + " to executor");
                mDownloadExecutor.execute(thread);
                LogUtils.i(DownloadConstants.TAG, "Thread " + i + " submitted successfully");
            } catch (Exception e) {
                LogUtils.e(DownloadConstants.TAG, "Error creating/submitting thread " + i + ": " + e.getMessage() + "\n" + android.util.Log.getStackTraceString(e));
                notifyDownloadError(e);
                return;
            }
        }
        LogUtils.i(DownloadConstants.TAG, "All download threads started successfully");
    }

    @Override
    public void pauseDownload() {
        if (mDownloadExecutor != null && !mDownloadExecutor.isShutdown()) {
            mDownloadExecutor.shutdownNow();
            notifyOnTaskPaused();
        }
    }

    @Override
    public void resumeDownload() {
        startDownloadVideo();
    }

    private void saveCacheInfo(Map<Integer, Long> cacheMap) {
        int size = cacheMap.size();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ids.add(i);
        }
        List<Long> starts = new ArrayList<>();
        List<Long> ends = new ArrayList<>();
        for(VideoRange range : mRangeList) {
            starts.add(range.getStart());
            ends.add(range.getEnd());
        }

        List<Long> cachedSizes = new ArrayList<>();
        for (Map.Entry entry : cacheMap.entrySet()) {
            int id = (int) entry.getKey();
            long cachedSize = (long) entry.getValue();
            cachedSizes.add(id, cachedSize);
            LogUtils.i(DownloadConstants.TAG, "saveCacheInfo id="+id+", cachedSize=" + cachedSize);
        }

        MultiRangeInfo rangeInfo = new MultiRangeInfo();

        rangeInfo.setIds(ids);
        rangeInfo.setStarts(starts);
        rangeInfo.setEnds(ends);
        rangeInfo.setSizes(cachedSizes);

        VideoDownloadUtils.saveRangeInfo(rangeInfo, mSaveDir);

    }

    private void notifyOnProgress(Map<Integer, Long> cachedMap) {
        long currentSize = 0;
        for (long size : cachedMap.values()) {
            currentSize += size;
        }
        mCurrentCachedSize = currentSize;
        if (mCurrentCachedSize >= mTotalLength ) {
            mDownloadTaskListener.onTaskProgress(100, mTotalLength, mTotalLength, mSpeed);
            mPercent = 100.0f;
            notifyDownloadFinish();
        } else {
            float percent = mCurrentCachedSize * 1.0f * 100 / mTotalLength;
            if (!VideoDownloadUtils.isFloatEqual(percent, mPercent)) {
                long nowTime = System.currentTimeMillis();
                if (mCurrentCachedSize > mLastCachedSize && nowTime > mLastInvokeTime) {
                    mSpeed = (mCurrentCachedSize - mLastCachedSize) * 1000 * 1.0f / (nowTime - mLastInvokeTime);
                }
                mDownloadTaskListener.onTaskProgress(percent, mCurrentCachedSize, mTotalLength, mSpeed);
                mPercent = percent;
                mLastInvokeTime = nowTime;
                mLastCachedSize = mCurrentCachedSize;

                saveCacheInfo(cachedMap);
            }
        }
    }

    private void notifyDownloadError(Exception e) {
        notifyOnTaskFailed(e);
    }

    private void notifyDownloadFinish() {
        synchronized (mDownloadLock) {
            if (!mDownloadFinished) {
                mDownloadTaskListener.onTaskFinished(mTotalLength);
                mDownloadFinished = true;
            }
        }
    }
}
