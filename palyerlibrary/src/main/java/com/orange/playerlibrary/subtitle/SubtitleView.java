package com.orange.playerlibrary.subtitle;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * 字幕显示视图
 */
public class SubtitleView extends AppCompatTextView {

    private static final float DEFAULT_TEXT_SIZE = 16f; // sp
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_SHADOW_COLOR = Color.BLACK;
    private static final float DEFAULT_SHADOW_RADIUS = 4f;
    private static final int DEFAULT_PADDING = 8; // dp

    public SubtitleView(@NonNull Context context) {
        super(context);
        init();
    }

    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 默认样式
        setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE);
        setTextColor(DEFAULT_TEXT_COLOR);
        setGravity(Gravity.CENTER);
        setTypeface(Typeface.DEFAULT_BOLD);
        
        // 文字阴影，增强可读性
        setShadowLayer(DEFAULT_SHADOW_RADIUS, 2, 2, DEFAULT_SHADOW_COLOR);
        
        // 内边距
        int padding = dpToPx(DEFAULT_PADDING);
        setPadding(padding * 2, padding, padding * 2, padding);
        
        // 半透明背景
        setBackgroundColor(0x80000000);
        
        // 默认隐藏
        setVisibility(GONE);
    }

    /**
     * 设置字幕文字大小
     */
    public void setTextSize(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    /**
     * 设置字幕背景颜色
     */
    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
    }

    /**
     * 设置字幕阴影
     */
    public void setShadow(float radius, int color) {
        setShadowLayer(radius, 2, 2, color);
    }

    /**
     * 设置字幕样式
     */
    public void setStyle(float textSizeSp, int textColor, int bgColor) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        setTextColor(textColor);
        setBackgroundColor(bgColor);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
