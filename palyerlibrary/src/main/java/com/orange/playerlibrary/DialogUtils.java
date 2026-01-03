package com.orange.playerlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;

public class DialogUtils {
    
    // 位置枚举
    public enum DialogPosition {
        LEFT, RIGHT, TOP, BOTTOM, CENTER
    }
    
    // 位置默认值变量
    private static float DEFAULT_RIGHT_DIALOG_WIDTH_PORTRAIT = 0.5f; // 竖屏默认50%
    private static float DEFAULT_RIGHT_DIALOG_WIDTH_LANDSCAPE = 0.3f; // 横屏默认30%
    
    /**
     * 使用布局资源ID创建弹窗
     */
    public static AlertDialog showAlertDialog(Activity context, int layoutResId, DialogPosition position) {
        return createDialog(context, 
                           context.getLayoutInflater().inflate(layoutResId, null, false), 
                           position, 
                           null, // 宽度比例 - 使用智能默认值
                           null); // 高度比例 - 使用位置默认值
    }
    
    /**
     * 使用自定义View创建弹窗
     */
    public static AlertDialog showDialogFromView(Activity context,
                                                View view,
                                                DialogPosition position) {
        return createDialog(context, 
                           view, 
                           position, 
                           null, // 宽度比例 - 使用智能默认值
                           null); // 高度比例 - 使用位置默认值
    }
    
    /**
     * 完整参数创建弹窗（基于视图对象）
     */
    public static AlertDialog showCustomDialog(Activity context,
                                              View view,
                                              DialogPosition position,
                                              Float widthRatio,
                                              Float heightRatio) {
        return createDialog(context, view, position, widthRatio, heightRatio);
    }
    
    /**
     * 完整参数创建弹窗（基于布局ID）
     */
    public static AlertDialog showCustomDialog(Activity context,
                                              int layoutResId,
                                              DialogPosition position,
                                              Float widthRatio,
                                              Float heightRatio) {
        View view = context.getLayoutInflater().inflate(layoutResId, null, false);
        return createDialog(context, view, position, widthRatio, heightRatio);
    }
    
    /**
     * 设置右弹窗默认宽度比例
     */
    public static void setDefaultRightDialogWidth(float portraitRatio, float landscapeRatio) {
        DEFAULT_RIGHT_DIALOG_WIDTH_PORTRAIT = portraitRatio;
        DEFAULT_RIGHT_DIALOG_WIDTH_LANDSCAPE = landscapeRatio;
    }
    
    /**
     * 内部创建对话框的通用方法
     */
    private static AlertDialog createDialog(Activity context,
                                          View view,
                                          DialogPosition position,
                                          Float widthRatio,
                                          Float heightRatio) {
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(view)
            .create();
        
        // 在 show() 之前设置这些属性
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        dialog.show();
        setupWindow(dialog.getWindow(), position, widthRatio, heightRatio, context);
        
        dialog.setOnDismissListener(d -> removeBlurEffect(context));
        
        return dialog;
    }
    
    /**
     * 配置窗口属性
     */
    private static void setupWindow(Window window, DialogPosition position,
                                  Float widthRatio, Float heightRatio, Context context) {
        if (window == null) return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // 弹窗窗口始终占满全屏，点击外部区域由布局内部处理
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setWindowAnimations(getAnimationStyle(position));
        
        setImmersiveMode(window);
    }
    
    /**
     * 设置弹窗边距
     */
    private static void setDialogMargins(Window window, DialogPosition position, Context context) {
        if (window == null) return;
        
        // 暂时不设置边距，避免点击区域偏移问题
        // 边距通过布局文件的 padding 来实现
    }
    
    /**
     * 计算对话框尺寸
     */
    private static int calculateDialogDimension(boolean isWidth,
                                             DisplayMetrics metrics,
                                             DialogPosition position,
                                             Float ratio,
                                             Context context) {
        // 对于左右位置的弹窗，高度默认使用全屏
        if (!isWidth && (position == DialogPosition.LEFT || position == DialogPosition.RIGHT)) {
            if (ratio == null || ratio <= 0) {
                return ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        
        // 对于上下位置的弹窗，宽度默认使用全屏
        if (isWidth && (position == DialogPosition.TOP || position == DialogPosition.BOTTOM)) {
            if (ratio == null || ratio <= 0) {
                return ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        
        // 如果ratio为null，表示使用布局默认尺寸
        if (ratio == null) {
            return isWidth ? ViewGroup.LayoutParams.WRAP_CONTENT : 
                    ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        
        // 如果ratio为-1，表示使用MATCH_PARENT
        if (ratio < 0) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        
        // 特殊处理右弹窗的横竖屏自适应
        if (isWidth && (position == DialogPosition.LEFT || position == DialogPosition.RIGHT)) {
            boolean isPortrait = context.getResources().getConfiguration().orientation == 
                                Configuration.ORIENTATION_PORTRAIT;
            float effectiveRatio = isPortrait ? DEFAULT_RIGHT_DIALOG_WIDTH_PORTRAIT :
                                             DEFAULT_RIGHT_DIALOG_WIDTH_LANDSCAPE;
            return (int) (metrics.widthPixels * effectiveRatio);
        }
        
        // 位置默认值
        float effectiveRatio = ratio != null && ratio > 0 ? ratio :
                getDefaultRatio(isWidth, position);
        
        // 计算尺寸
        return (int) (isWidth ? 
                     metrics.widthPixels * effectiveRatio : 
                     metrics.heightPixels * effectiveRatio);
    }
    
    /**
     * 获取位置默认比例
     */
    private static float getDefaultRatio(boolean isWidth, DialogPosition position) {
        if (isWidth) {
            switch (position) {
                case LEFT:
                case RIGHT:
                    return 0.4f; // 左右位置默认40%
                case CENTER:
                    return 0.8f; // 居中默认80%
                default: // TOP/BOTTOM
                    return 1.0f; // 上下位置全宽
            }
        } else {
            switch (position) {
                case TOP:
                case BOTTOM:
                    return 0.3f; // 上下位置30%
                case CENTER:
                    return 0.0f; // 居中高度使用WRAP_CONTENT
                default: // LEFT/RIGHT
                    return 1.0f; // 左右位置全高
            }
        }
    }
    
    private static int getGravity(DialogPosition position) {
        switch (position) {
            case LEFT: return Gravity.LEFT | Gravity.CENTER_VERTICAL;
            case RIGHT: return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            case TOP: return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            case BOTTOM: return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            default: return Gravity.CENTER;
        }
    }
    
    private static int getAnimationStyle(DialogPosition position) {
        return android.R.style.Animation_Dialog;
    }
    
    private static void setImmersiveMode(Window window) {
        if (window != null) {
            // 设置沉浸式模式，但不使用 FLAG_NOT_FOCUSABLE，以确保能接收触摸事件
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    
    public static void showBlurEffect(Activity activity, String str, String str2) {
        if (Build.VERSION.SDK_INT >= 31) {
            int parseInt = Integer.parseInt(str);
            int parseInt2 = Integer.parseInt(str2);
            activity.getWindow().getDecorView().setRenderEffect(
                RenderEffect.createBlurEffect(parseInt, parseInt2, Shader.TileMode.CLAMP));
        }
    }
    
    public static void removeBlurEffect(Activity activity) {
        if (Build.VERSION.SDK_INT >= 31) {
            activity.getWindow().getDecorView().setRenderEffect(null);
        }
    }
}
