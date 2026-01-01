package com.orange.playerlibrary;

import android.content.Context;
import android.util.AttributeSet;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

/**
 * 自定义播放器 - 橘子播放器
 * 继承 StandardGSYVideoPlayer 实现自定义 UI
 */
public class OrangeVideoPlayer extends StandardGSYVideoPlayer {

    public OrangeVideoPlayer(Context context) {
        super(context);
    }

    public OrangeVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrangeVideoPlayer(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_orange_video_player;
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        // 可以在这里做额外的初始化
    }

    /**
     * 设置返回按钮点击事件
     */
    public void setBackClickListener(OnClickListener listener) {
        if (mBackButton != null) {
            mBackButton.setOnClickListener(listener);
        }
    }
}
