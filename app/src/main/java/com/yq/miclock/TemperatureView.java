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

    private int colorBegin, colorEnd, dotColor, distance, tempBegin, tempEnd, currentTemp;//distance 圆点和刻度间的距离
    private float deltaTemp;//两条刻度线之间的温度差值
    private float scaleRatio = 0.25f;//刻度线长度占控件宽度比例
    private Paint bgPaint, //渐变色背景画笔
            dotPaint, //圆点画笔
            linePaint, currentLinePaint, curPaint;
    private int centerX, centerY;
    private int ringWidth, dotStartX, padding, mWidth, mHeight;
    private SweepGradient bgGradient;
    private float downX = 0;//当前手指位置
    private TemperatureListener temperatureListener;//滑动时候回调接口，返回当前温度值
    private double angleToRotate, lastAngle;

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
        tempBegin = typedArray.getInt(R.styleable.TemperatureView_tempValue_start, 16);
        tempEnd = typedArray.getInt(R.styleable.TemperatureView_tempValue_end, 40);
        scaleRatio = typedArray.getFloat(R.styleable.TemperatureView_scale_ratio, 0.25f);
        padding = typedArray.getDimensionPixelOffset(R.styleable.TemperatureView_padding, 200);
        distance = typedArray.getDimensionPixelOffset(R.styleable.TemperatureView_distance, 100);
        typedArray.recycle();
        deltaTemp = (float) (tempEnd - tempBegin) / 270;//平分成60格，底部15格不显示刻度
        currentTemp = tempBegin;
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

        curPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curPaint.setColor(Color.RED);
        curPaint.setStyle(Paint.Style.STROKE);
        curPaint.setStrokeWidth(4);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        centerX = getMeasuredWidth() / 2;
        centerY = getMeasuredHeight() / 2;
        ringWidth = (int) (scaleRatio * centerX);
//        currentLinePaint.setStrokeWidth(ringWidth);

        bgPaint.setStrokeWidth(ringWidth);
        linePaint.setStrokeWidth(ringWidth + 4);
        dotStartX = getMeasuredWidth() - padding - ringWidth - distance;
    }


    float downY = 0;
    float currentX, lastX;
    float currentY, lastY;
    boolean continueDraw = true;//是否继续处理滑动事件

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                currentX = event.getX();
                currentY = event.getY();
                float curAngle = calcAngle(currentX, currentY);
                System.out.println("angle start = " + angleToRotate);
                if (angleToRotate - 10 < 0)
                    continueDraw = curAngle >= angleToRotate - 10 + 360 || curAngle <= angleToRotate + 10;
                else
                    continueDraw = (curAngle <= angleToRotate + 10 && curAngle >= angleToRotate - 10);
                return continueDraw;
            case MotionEvent.ACTION_MOVE:
                currentX = event.getX();
                currentY = event.getY();
                float currentAngle = calcAngle(currentX, currentY);
                if ((isClockWise() && currentAngle > 60 && currentAngle <= 90) || (!isClockWise() && currentAngle >= 90 && currentAngle < 120)) {
                    continueDraw = false;
                } else if (currentAngle > 45 && currentAngle < 135) {
//                    angleToRotate = lastAngle;
//                    continueDraw = false;
                } else {
                    if (continueDraw) {
                        angleToRotate = currentAngle;
//                        lastAngle = angleToRotate;
                        lastX = currentX;
                        lastY = currentY;
                        invalidate();
                    }
                }
//                float diffX = currentX - downX;
//                System.out.println("diffX =  " + diffX);
//                setCurrentTempByDelta(calcDeltaTemp(diffX));
//                downX = currentX;

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
     * 计算温度变化值
     *
     * @param diff 滑动距离
     * @return
     */
    private float calcDeltaTemp(float diff) {
        return diff / mWidth * (tempEnd - tempBegin) + 0.5f;
    }

    private void setCurrentTempByDelta(float deltaTemp) {
        System.out.println("deltaTemp =  " + deltaTemp);
        currentTemp += deltaTemp;
        if (currentTemp < tempBegin)
            currentTemp = tempBegin;
        else if (currentTemp > tempEnd)
            currentTemp = tempEnd;
        if (null != temperatureListener)
            temperatureListener.onMove(currentTemp);
        System.out.println(currentTemp);
        setCurrentTemp(currentTemp);
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
        currentLinePaint.setTextSize(40);
//        canvas.drawText(currentTemp + "", centerX, centerY, currentLinePaint);
        float currentTempDegree = (currentTemp - tempBegin) / deltaTemp;
//        if (currentTempDegree < -225)
//            currentTempDegree = -225;
//        else if (currentTempDegree > 45)
//            currentTempDegree = 45;
        canvas.save();
        System.out.println("angleToRotate draw = " + angleToRotate);
        canvas.rotate((float) angleToRotate, centerX, centerY);
        canvas.drawLine(mWidth - padding - ringWidth, centerY - 2, mWidth - padding / 2, centerY + 2, currentLinePaint);
//        RectF rectF = new RectF(padding + ringWidth / 2, padding + ringWidth / 2, mWidth - padding - ringWidth / 2, mHeight - padding - ringWidth / 2);
//        canvas.drawArc(rectF, -0.4f, 0.4f, false, currentLinePaint);
        canvas.restore();

//        double angle = angleToRotate;
//        if (lastAngle == 225 || lastAngle == 315)
//            angle = lastAngle;
//        canvas.save();
//        canvas.rotate((float) angle, centerX, centerY);
//        System.out.println("angleToRotate draw = " + angle);
//        canvas.drawLine(mWidth - padding - ringWidth, centerY - 2, mWidth, centerY + 2, curPaint);
//        canvas.restore();
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

    public int getCurrentTemp() {
        return currentTemp;
    }

    public interface TemperatureListener {

        void onMove(int currentTemp);

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
