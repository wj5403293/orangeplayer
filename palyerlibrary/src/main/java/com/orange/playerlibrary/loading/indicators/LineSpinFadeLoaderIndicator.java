package com.orange.playerlibrary.loading.indicators;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.orange.playerlibrary.loading.Indicator;

import java.util.ArrayList;

/**
 * 线条旋转淡入淡出加载动画
 */
public class LineSpinFadeLoaderIndicator extends Indicator {

    public static final float SCALE = 1.0f;
    public static final int ALPHA = 255;

    float[] scaleYFloats = new float[]{SCALE, SCALE, SCALE, SCALE, SCALE, SCALE, SCALE, SCALE};
    int[] alphas = new int[]{ALPHA, ALPHA, ALPHA, ALPHA, ALPHA, ALPHA, ALPHA, ALPHA};

    @Override
    public void draw(Canvas canvas, Paint paint) {
        float radius = getWidth() / 10;
        for (int i = 0; i < 8; i++) {
            canvas.save();
            Point point = circleAt(getWidth(), getHeight(), getWidth() / 2.5f - radius, i * (Math.PI / 4));
            canvas.translate(point.x, point.y);
            canvas.rotate(i * 45);
            canvas.scale(1, scaleYFloats[i]);
            paint.setAlpha(alphas[i]);
            RectF rectF = new RectF(-radius, -radius / 1.5f, radius / 1.5f, radius / 1.5f);
            canvas.drawRoundRect(rectF, 5, 5, paint);
            canvas.restore();
        }
    }

    Point circleAt(int width, int height, float radius, double angle) {
        float x = (float) (width / 2 + radius * (Math.cos(angle)));
        float y = (float) (height / 2 + radius * (Math.sin(angle)));
        return new Point(x, y);
    }

    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        ArrayList<ValueAnimator> animators = new ArrayList<>();
        int[] delays = new int[]{0, 120, 240, 360, 480, 600, 720, 780, 840};
        for (int i = 0; i < 8; i++) {
            final int index = i;
            ValueAnimator scaleAnim = ValueAnimator.ofFloat(1, 0.4f, 1);
            scaleAnim.setDuration(1000);
            scaleAnim.setRepeatCount(-1);
            scaleAnim.setStartDelay(delays[i]);
            addUpdateListener(scaleAnim, new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    scaleYFloats[index] = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });

            ValueAnimator alphaAnim = ValueAnimator.ofInt(255, 77, 255);
            alphaAnim.setDuration(1000);
            alphaAnim.setRepeatCount(-1);
            alphaAnim.setStartDelay(delays[i]);
            addUpdateListener(alphaAnim, new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    alphas[index] = (int) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
            animators.add(scaleAnim);
            animators.add(alphaAnim);
        }
        return animators;
    }

    final class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
