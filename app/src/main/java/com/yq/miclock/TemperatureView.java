package com.yq.miclock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TemperatureView extends View {

    private int colorBegin, colorEnd, dotColor, distance;//distance 圆点和刻度间的距离
    private float tempBegin, tempEnd, currentTemp, deltaTemp;
    private float scaleRatio = 0.25f;//刻度线长度占控件宽度比例
    private Paint bgPaint, //渐变色背景画笔
            dotPaint, //圆点画笔
            linePaint, currentLinePaint, textPaint;
    private int centerX, centerY;
    private int ringWidth, dotStartX, padding, mWidth, mHeight;
    private SweepGradient bgGradient;
    private TemperatureListener temperatureListener;//滑动时候回调接口，返回当前温度值
    //手指位置，lastX,lastY用于判断是否是顺时针
    float currentX, lastX;
    float currentY, lastY;
    boolean handleTouch = true;//是否继续处理滑动事件
    private float angleToRotate;

    public TemperatureView(Context context) {
        this(context, null);
    }

    public TemperatureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TemperatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        init();
    }

    private void initAttrs(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TemperatureView);
        colorBegin = typedArray.getColor(R.styleable.TemperatureView_color_begin, Color.BLUE);
        colorEnd = typedArray.getColor(R.styleable.TemperatureView_color_end, Color.GREEN);
        dotColor = typedArray.getColor(R.styleable.TemperatureView_color_dot, Color.GRAY);
        tempBegin = typedArray.getFloat(R.styleable.TemperatureView_tempValue_start, 100);
        tempEnd = typedArray.getFloat(R.styleable.TemperatureView_tempValue_end, 200);
        currentTemp = typedArray.getFloat(R.styleable.TemperatureView_current_temp, tempBegin);
        scaleRatio = typedArray.getFloat(R.styleable.TemperatureView_scale_ratio, 0.25f);
        padding = typedArray.getDimensionPixelOffset(R.styleable.TemperatureView_padding, 200);
        distance = typedArray.getDimensionPixelOffset(R.styleable.TemperatureView_distance, 100);
        typedArray.recycle();
        deltaTemp = (tempEnd - tempBegin) / 270;
        angleToRotate = (currentTemp - tempBegin) / deltaTemp + 135;
        angleToRotate = angleToRotate > 270 ? angleToRotate - 270 : angleToRotate;
    }

    private void init() {

        setBackgroundColor(Color.WHITE);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgGradient = new SweepGradient(centerX, centerY, new int[]{colorBegin, colorEnd}, null);
        bgPaint.setShader(bgGradient);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(dotColor);
        dotPaint.setStrokeWidth(2);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);

        currentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentLinePaint.setShader(bgGradient);
        currentLinePaint.setStyle(Paint.Style.STROKE);
        currentLinePaint.setStrokeWidth(4);

        textPaint = new Paint(currentLinePaint);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(4);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        centerX = getMeasuredWidth() / 2;
        centerY = getMeasuredHeight() / 2;
        ringWidth = (int) (scaleRatio * centerX);

        bgPaint.setStrokeWidth(ringWidth);
        linePaint.setStrokeWidth(ringWidth + 4);
        dotStartX = getMeasuredWidth() - padding - ringWidth - distance;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentX = event.getX();
                currentY = event.getY();
                float curAngle = calcAngle(currentX, currentY);
                if (angleToRotate - 10 < 0)
                    handleTouch = curAngle >= angleToRotate - 10 + 360 || curAngle <= angleToRotate + 10;
                else
                    handleTouch = (curAngle <= angleToRotate + 10 && curAngle >= angleToRotate - 10);
                return handleTouch;
            case MotionEvent.ACTION_MOVE:
                currentX = event.getX();
                currentY = event.getY();
                float currentAngle = calcAngle(currentX, currentY);
                //滑动超过边界一定范围不再进行处理滑动事件，防止多圈滑动从最小跳到最大
                if ((isClockWise() && currentAngle > 60 && currentAngle <= 90) || (!isClockWise() && currentAngle >= 90 && currentAngle < 120)) {
                    handleTouch = false;
                }
                if ((currentAngle <= 45 || currentAngle >= 135) && handleTouch) {
                    calCurrentTemp(currentAngle);
                    angleToRotate = currentAngle;
                    lastX = currentX;
                    lastY = currentY;
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:

                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 是否是顺时针
     *
     * @return true:顺时针 false:逆时针
     */
    private boolean isClockWise() {
        return (currentY > centerY && currentX < lastX) || (currentY < centerY && currentX > lastX);
    }

    private float calcAngle(float currentX, float currentY) {
        float deltaX = currentX - centerX;
        float deltaY = currentY - centerY;
        double sin = deltaY / (Math.sqrt(deltaX * deltaX + deltaY * deltaY));
        double angle = Math.toDegrees(Math.asin(sin));//角度

        if (currentX >= centerX && currentY <= centerY)//第一象限
            angle += 360;
        else if (currentX <= centerX && currentY <= centerY)//第二象限
            angle = -angle + 180;
        else if (currentX < centerX && currentY > centerY)//第三象限
            angle = 180 - angle;
        System.out.println("angle = " + angle);
        return (float) angle;
    }

    /**
     * 计算当前温度值
     *
     * @param currentAngle
     */
    private void calCurrentTemp(float currentAngle) {
        float diffAngle = currentAngle - 135;
        if (diffAngle < 0) diffAngle += 360;
        currentTemp = Math.round(deltaTemp * diffAngle + tempBegin);

        if (null != temperatureListener)
            temperatureListener.onMove(currentTemp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
//        canvas.drawLine(0, centerY - 2, mWidth, centerY + 2, currentLinePaint);
//        canvas.drawLine(centerX - 2, 0, centerX + 2, mHeight, currentLinePaint);

    }

    //画刻度盘
    private void drawBackground(Canvas canvas) {
        RectF rectF = new RectF(0, 0, mWidth, mHeight);
        rectF.inset(ringWidth / 2 + padding, ringWidth / 2 + padding);
        canvas.save();
        canvas.rotate(-225, centerX, centerY);
        canvas.drawArc(rectF, -0.2f, 270.2f, false, bgPaint);
        for (int i = 0; i < 60; i++) {
            //画小圆点
            if (i <= 54)
                canvas.drawLine(dotStartX, centerY - 1, dotStartX + 1, centerY + 1, dotPaint);
            canvas.drawArc(rectF, 0.2f, 4.6f, false, linePaint);
            canvas.rotate(5f, centerX, centerY);
        }
        canvas.restore();
        drawCurrentScale(canvas);
    }

    /**
     * 画当前刻度
     *
     * @return
     */
    private void drawCurrentScale(Canvas canvas) {
        canvas.drawText(currentTemp + "", centerX, centerY, textPaint);
        canvas.save();
        System.out.println("angleToRotate draw = " + angleToRotate);
        canvas.rotate((float) angleToRotate, centerX, centerY);
        canvas.drawLine(mWidth - padding - ringWidth, centerY - 2, mWidth - padding / 2, centerY + 2, currentLinePaint);
//        RectF rectF = new RectF(padding + ringWidth / 2, padding + ringWidth / 2, mWidth - padding - ringWidth / 2, mHeight - padding - ringWidth / 2);
//        canvas.drawArc(rectF, -0.4f, 0.4f, false, currentLinePaint);
        canvas.restore();
    }

    /**
     * 设置当前温度
     *
     * @param value 目标值
     */
    @MainThread
    public void setCurrentTemp(int value) {
        if (value < tempBegin || value > tempEnd) return;
        currentTemp = value;
        invalidate();
    }

    public float getCurrentTemp() {
        return currentTemp;
    }

    public interface TemperatureListener {

        void onMove(float currentTemp);

    }

    /**
     * 设置回调监听接口
     *
     * @param temperatureListener
     */
    public void setTemperatureListener(TemperatureListener temperatureListener) {
        this.temperatureListener = temperatureListener;
    }
}
