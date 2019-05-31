package com.yq.miclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class TemperatureView extends View {

    private Paint bgPaint, dashPaint, linePaint, currentLinePaint;
    private int centerX, centerY;
    private int ringWidth, dotStartX, padding, mWidth, mHeight;

    public TemperatureView(Context context) {
        this(context, null);
    }

    public TemperatureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TemperatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);

        dashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashPaint.setColor(Color.GRAY);
        dashPaint.setStrokeWidth(2);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);

        currentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentLinePaint.setColor(Color.YELLOW);
        currentLinePaint.setStyle(Paint.Style.STROKE);
        currentLinePaint.setStrokeWidth(4);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        centerX = getMeasuredWidth() / 2;
        centerY = getMeasuredHeight() / 2;
        ringWidth = (int) (0.25 * centerX);
        padding = ringWidth / 2 + 20;
        SweepGradient bgGradient = new SweepGradient(centerX, centerY, new int[]{Color.BLUE, Color.GREEN}, null);
        bgPaint.setShader(bgGradient);
        bgPaint.setStrokeWidth(ringWidth);
        linePaint.setStrokeWidth(ringWidth + 4);
//        currentLinePaint.setStrokeWidth(ringWidth + 20);
        dotStartX = getMeasuredWidth() - 40 - ringWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
    }

    private void drawBackground(Canvas canvas) {
        RectF rectF = new RectF(0, 0, mWidth, mHeight);
        rectF.inset(padding, padding);
//        RectF rectF = new RectF(ringWidth / 2 + padding, ringWidth / 2 + padding, getWidth() - ringWidth / 2 - padding, getHeight() - ringWidth / 2 - padding);
        canvas.save();
        canvas.rotate(135, centerX, centerY);
        canvas.drawArc(rectF, 0, 270, false, bgPaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(-90, centerX, centerY);
        for (int i = 0; i < 60; i++) {

            if (i < 23 || i > 37)
                canvas.drawLine(dotStartX, centerY - 1, dotStartX + 1, centerY + 1, dashPaint);

            canvas.drawArc(rectF, 0.2f, 5.6f, false, linePaint);

            if (i == 0) {
                canvas.drawLine(mWidth - 20 - ringWidth, centerY - 2, mWidth, centerY + 2, currentLinePaint);
//                canvas.drawArc(rectF, -0.2f, 0.2f, false, currentLinePaint);
            }
            canvas.rotate(6f, centerX, centerY);
        }
        canvas.restore();
    }

}
