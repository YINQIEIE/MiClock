package com.yq.miclock;

import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * Created by Administrator on 2017/12/28.
 * 参考：http://blog.csdn.net/qq_31715429/article/details/54668668
 */

public class MiClockView extends View {

    private Paint linePaint,//线画笔
            numberPaint,
            scaleRingPaint, //刻度线圆环区域画笔
            secAndMinHandPaint, hourPaint,//时分秒针画笔
            bgColorPaint;//画刻度和画中心圆环使用
    private int mWidth, mHeight, padding = 20, space = 10;//space 数字和刻度以及刻度和秒针之间的间隔
    private Point centerPoint;//中心点

    private int mtextSize = 40;//文字大小
    private int numberRectWid;//文字显示区域边长
    private float deltaBaseLineY;//文字绘制基线

    private RectF ringRect;//数字所在圆环区域
    //数字两边圆环所形成的角度，默认 10
    private int startAngle = 10, sweepAngle = 90 - 2 * startAngle;
    private float mRadius;//半径，去掉 padding
    private float mScaleLength;//刻度线长度
    private SweepGradient sweepGradient;//刻度背景圆环渐变对象
    private int colorLight = 0x30FFFFFF, colorDark = 0xFFFFFFFF;//圆环渐变色范围

    //突出显示秒针扫过的区域
    private Matrix scaleLineMatrix = new Matrix();
    //时分秒的角度值
    private float mSecondDegree, mMinuteDegree, mHourDegree;
    //时分秒针的 path
    private Path secondHandPath, minuteHandPath, hourHandPath;
    //秒、分、时针顶端离控件顶部的距离
    private float secondOffset, minuteOffset, hourOffset;

    //用于设置 3D 旋转效果
    private Camera mCamera;
    private Matrix cameraMatrix;
    //旋转角度
    private float rotateX, rotateY;
    /* camera旋转的最大角度 */
    private float mMaxRotate = 10;

    //指针在 x 、y 轴的位移
    private float mCanvasTranslateX, mCanvasTranslateY;
    private float maxCanvasTranslate;

    //松手后的回弹动画
    private AnimatorSet animatorSet;
    private ValueAnimator mShakeAnim;
    private RectF scaleRectF;

    public MiClockView(Context context) {
        super(context);
    }

    public MiClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);

        numberPaint = new Paint(linePaint);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTextSize(mtextSize);

        //计算出文字的 baseLineY
        Paint.FontMetrics fontMetrics = numberPaint.getFontMetrics();
        deltaBaseLineY = (fontMetrics.ascent + fontMetrics.descent) / 2;

        //用来画中心圆环以及刻度线
        bgColorPaint = new Paint(linePaint);
        bgColorPaint.setColor(0xFF3F51B5);//背景色
        bgColorPaint.setStyle(Paint.Style.FILL);

        //圆环画笔，后面会设置 sweepGrident
        //先画出白色渐变圆环，上面再用背景色画笔分割出刻度线
        scaleRingPaint = new Paint(linePaint);

        //秒针画笔，和分针共用
        secAndMinHandPaint = new Paint();
        secAndMinHandPaint.setStyle(Paint.Style.FILL);
        secAndMinHandPaint.setColor(Color.WHITE);
        secAndMinHandPaint.setAntiAlias(true);

        hourPaint = new Paint(secAndMinHandPaint);
        hourPaint.setAlpha(0x80);

        centerPoint = new Point();

        Rect textRect = new Rect();
        ringRect = new RectF();
        numberPaint.getTextBounds("12", 0, 2, textRect);
        numberRectWid = Math.max(textRect.width(), textRect.height());//以 12 的宽高中较大的值为数字显示区域的边长

        secondHandPath = new Path();
        minuteHandPath = new Path();
        hourHandPath = new Path();

        mCamera = new Camera();
        cameraMatrix = new Matrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = getSize(widthMeasureSpec);
        mHeight = getSize(heightMeasureSpec);
        mWidth = mHeight = Math.min(mWidth, mHeight);
        setMeasuredDimension(mWidth, mHeight);

        centerPoint.x = mWidth / 2;
        centerPoint.y = mHeight / 2;

        mRadius = (mWidth - 2 * padding) / 2;
        padding += 0.08f * mRadius;
        mScaleLength = 0.10f * mRadius;
        //设置刻度间隔线宽度
        bgColorPaint.setStrokeWidth(0.012f * mRadius);
        //0.75 ~ 1.0 颜色变化的范围
        sweepGradient = new SweepGradient(centerPoint.x, centerPoint.y, new int[]{colorLight, colorDark}, new float[]{0.75f, 1.0f});
        scaleRingPaint.setShader(sweepGradient);
        scaleRingPaint.setStrokeWidth(mScaleLength);

        // 外围数字对应的圆环区域
        ringRect.set(padding + numberRectWid / 2,
                padding + numberRectWid / 2,
                mWidth - padding - numberRectWid / 2,
                mHeight - padding - numberRectWid / 2);

        scaleRectF = new RectF(padding + numberRectWid + 0.08f * mRadius,
                padding + numberRectWid + 0.08f * mRadius,
                mWidth - (padding + numberRectWid + 0.08f * mRadius),
                mHeight - (padding + numberRectWid + 0.08f * mRadius));

        scaleRectF.inset(0.5f * mScaleLength, 0.5f * mScaleLength);

        secondOffset = padding + space + numberRectWid + mScaleLength;
        secondOffset = scaleRectF.top + 0.5f * mScaleLength + 0.06f * mRadius;//最顶点位置
        //第一个 10，数字到刻度线的距离，第二个 10，刻度线到秒针的距离，第三个 10 ，保证分针到秒针的最小距离
        minuteOffset = secondOffset + 0.08f * mRadius;//秒针三角形的高度为 0.08f * mRadius
        hourOffset = minuteOffset + 0.04f * mRadius;//时针顶点离分针顶点的位置距离为 0.04f * mRadius
        maxCanvasTranslate = 0.02f * mRadius;
    }

    private int getSize(int measureSpec) {
        int result = 200;//默认200
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                result = Math.min(result, size);
                break;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setRotate(canvas);
        drawNumbers(canvas);
        getTimeDegree();
        drawScales(canvas);
        drawHands(canvas);
        invalidate();
    }

    private void setRotate(Canvas canvas) {
        cameraMatrix.reset();
        mCamera.save();
        mCamera.rotateX(rotateX);
        mCamera.rotateY(rotateY);
        mCamera.getMatrix(cameraMatrix);
        mCamera.restore();

        //camera在view左上角那个点，故旋转默认是以左上角为中心旋转
        //故在动作之前pre将matrix向左移动getWidth()/2长度，向上移动getHeight()/2长度
        cameraMatrix.preTranslate(-centerPoint.x, -centerPoint.y);
        cameraMatrix.postTranslate(centerPoint.x, centerPoint.y);

        //matrix与canvas相关联
        canvas.concat(cameraMatrix);

    }

    private void drawNumbers(Canvas canvas) {
        canvas.drawText("12", centerPoint.x, padding + numberRectWid / 2 - deltaBaseLineY, numberPaint);
        canvas.drawText("3", mWidth - padding - numberRectWid / 2, centerPoint.y - deltaBaseLineY, numberPaint);
        canvas.drawText("6", centerPoint.x, mHeight - padding - numberRectWid / 2 - deltaBaseLineY, numberPaint);
        canvas.drawText("9", padding + numberRectWid / 2, centerPoint.y - deltaBaseLineY, numberPaint);

        //几条边界线，方便查看画的内容位置对不对
//        canvas.drawLine(0, padding, mWidth, padding, linePaint);
//        canvas.drawLine(0, mHeight - padding, mWidth, mHeight - padding, linePaint);
//        canvas.drawLine(padding, 0, padding, mHeight, linePaint);
//        canvas.drawLine(mWidth - padding, 0, mWidth - padding, mHeight, linePaint);
//        canvas.drawLine(0, centerPoint.y, mWidth, centerPoint.y, linePaint);
        //画圆弧
        for (int i = 0; i < 4; i++) {
            canvas.drawArc(ringRect, startAngle + 90 * i, sweepAngle, false, linePaint);
        }

    }

    /**
     * 画刻度
     *
     * @param canvas
     */
    private void drawScales(Canvas canvas) {
        //画刻度线，以渐变白色圆环为背景，上面用背景色分割
        canvas.save();
        canvas.translate(mCanvasTranslateX, mCanvasTranslateY);

        canvas.drawArc(scaleRectF, 0, 360, false, scaleRingPaint);

        scaleLineMatrix.setRotate(mSecondDegree - 90, centerPoint.x, centerPoint.y);
        sweepGradient.setLocalMatrix(scaleLineMatrix);
        canvas.drawCircle(centerPoint.x, centerPoint.y, scaleRectF.width() / 2, scaleRingPaint);

        for (int i = 0; i < 200; i++) {
            canvas.drawLine(centerPoint.x,
                    scaleRectF.top - mScaleLength * 0.5f,
                    centerPoint.x,
                    scaleRectF.top + mScaleLength * 0.5f,
                    bgColorPaint);
            canvas.rotate(1.8f, centerPoint.x, centerPoint.y);
        }
        canvas.restore();
    }

    /**
     * 获取当前时分秒所对应的角度
     * 为了不让秒针走得像老式挂钟一样僵硬，需要精确到毫秒
     */
    private void getTimeDegree() {
        Calendar calendar = Calendar.getInstance();
        float milliSecond = calendar.get(Calendar.MILLISECOND);
        float second = calendar.get(Calendar.SECOND) + milliSecond / 1000;
        float minute = calendar.get(Calendar.MINUTE) + second / 60;
        float hour = calendar.get(Calendar.HOUR) + minute / 60;
        mSecondDegree = second / 60 * 360;
        mMinuteDegree = minute / 60 * 360;
        mHourDegree = hour / 12 * 360;
    }

    /**
     * 画指针
     *
     * @param canvas
     */
    private void drawHands(Canvas canvas) {
        drawSecondHand(canvas);
        drawMinuteHand(canvas);
        drawHourHand(canvas);
        //画中心圆环
        drawCircles(canvas);
    }

    /**
     * 画秒针
     *
     * @param canvas
     */
    private void drawSecondHand(Canvas canvas) {
        canvas.save();
        canvas.translate(mCanvasTranslateX, mCanvasTranslateY);
        canvas.rotate(mSecondDegree, centerPoint.x, centerPoint.y);
        secondHandPath.reset();
        //第一个 10 ，文字离刻度写死的距离，待替换；第二个 10 ，刻度离秒针的距离
        secondHandPath.moveTo(centerPoint.x, secondOffset);
        secondHandPath.lineTo(centerPoint.x - 0.05f * mRadius, secondOffset + 0.08f * mRadius);
        secondHandPath.lineTo(centerPoint.x + 0.05f * mRadius, secondOffset + 0.08f * mRadius);
        secondHandPath.close();

        canvas.drawPath(secondHandPath, secAndMinHandPaint);
        canvas.restore();
    }

    /**
     * 画分针
     *
     * @param canvas
     */
    private void drawMinuteHand(Canvas canvas) {
        canvas.save();
        canvas.translate(mCanvasTranslateX, mCanvasTranslateY);
        canvas.rotate(mMinuteDegree, centerPoint.x, centerPoint.y);
        minuteHandPath.reset();
        minuteHandPath.moveTo(centerPoint.x - 0.015f * mRadius, centerPoint.y);
        minuteHandPath.lineTo(centerPoint.x + 0.015f * mRadius, centerPoint.y);
        minuteHandPath.lineTo(centerPoint.x + 0.01f * mRadius, minuteOffset + 0.06f * mRadius);
        minuteHandPath.quadTo(centerPoint.x, minuteOffset + 0.03f * mRadius, centerPoint.x - 0.01f * mRadius, minuteOffset + 0.06f * mRadius);
        minuteHandPath.close();

        canvas.drawPath(minuteHandPath, secAndMinHandPaint);
        canvas.restore();
    }

    /**
     * 画时针
     *
     * @param canvas
     */
    private void drawHourHand(Canvas canvas) {
        canvas.save();
        canvas.translate(mCanvasTranslateX, mCanvasTranslateY);
        canvas.rotate(mHourDegree, centerPoint.x, centerPoint.y);
        hourHandPath.reset();
        hourHandPath.moveTo(centerPoint.x - 0.02f * mRadius, centerPoint.y);
        hourHandPath.lineTo(centerPoint.x + 0.02f * mRadius, centerPoint.y);
        hourHandPath.lineTo(centerPoint.x + 0.01f * mRadius, hourOffset + 0.09f * mRadius);
        hourHandPath.quadTo(centerPoint.x, hourOffset + 0.04f * mRadius, centerPoint.x - 0.01f * mRadius, hourOffset + 0.09f * mRadius);
        hourHandPath.close();

        canvas.drawPath(hourHandPath, hourPaint);
        canvas.restore();
    }

    private void drawCircles(Canvas canvas) {
        canvas.translate(mCanvasTranslateX, mCanvasTranslateY);
        canvas.drawCircle(centerPoint.x, centerPoint.y, 0.06f * mRadius, secAndMinHandPaint);
        canvas.drawCircle(centerPoint.x, centerPoint.y, 0.03f * mRadius, bgColorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                //set 动画稍微有点问题
//                if (animatorSet != null && animatorSet.isRunning())
//                    animatorSet.cancel();
                if (mShakeAnim != null && mShakeAnim.isRunning())
                    mShakeAnim.cancel();
                getRotate(event);
                getCanvasTranslate(event);
                break;

            case MotionEvent.ACTION_MOVE:
                getRotate(event);
                getCanvasTranslate(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startShakeAnim();
                break;

        }
        return true;
    }

    private void getRotate(MotionEvent event) {
        float deltaX = -(event.getY() - centerPoint.x);
        float deltaY = event.getX() - centerPoint.y;
        float[] percentArray = getPercentArray(deltaX, deltaY);
        rotateX = percentArray[0] * mMaxRotate;
        rotateY = percentArray[1] * mMaxRotate;
    }

    private float[] getPercentArray(float deltaX, float deltaY) {
        float[] result = new float[2];
        float x = deltaX / mRadius;
        float y = deltaY / mRadius;
        if (x > 1) x = 1;
        else if (x < -1) x = -1;
        if (y > 1) y = 1;
        else if (y < -1) y = -1;
        result[0] = x;
        result[1] = y;
        return result;
    }

    /**
     * 旋转的时候使时针也跟着变化
     *
     * @param event
     */
    private void getCanvasTranslate(MotionEvent event) {
        float deltaX = event.getX() - centerPoint.x;
        float deltaY = event.getY() - centerPoint.y;
        float[] percentArray = getPercentArray(deltaX, deltaY);
        mCanvasTranslateX = percentArray[0] * maxCanvasTranslate;
        mCanvasTranslateY = percentArray[1] * maxCanvasTranslate;
    }

    private void startShakeAnim() {
        final String cameraRotateXName = "cameraRotateX";
        final String cameraRotateYName = "cameraRotateY";
        final String canvasTranslateXName = "canvasTranslateX";
        final String canvasTranslateYName = "canvasTranslateY";
        PropertyValuesHolder cameraRotateXHolder =
                PropertyValuesHolder.ofFloat(cameraRotateXName, rotateX, 0);
        PropertyValuesHolder cameraRotateYHolder =
                PropertyValuesHolder.ofFloat(cameraRotateYName, rotateY, 0);
        PropertyValuesHolder canvasTranslateXHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateXName, mCanvasTranslateX, 0);
        PropertyValuesHolder canvasTranslateYHolder =
                PropertyValuesHolder.ofFloat(canvasTranslateYName, mCanvasTranslateY, 0);
        mShakeAnim = ValueAnimator.ofPropertyValuesHolder(cameraRotateXHolder,
                cameraRotateYHolder, canvasTranslateXHolder, canvasTranslateYHolder);
        mShakeAnim.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                //http://inloop.github.io/interpolator/
                float f = 0.571429f;
                return (float) (Math.pow(2, -2 * input) * Math.sin((input - f / 4) * (2 * Math.PI) / f) + 1);
            }
        });
        mShakeAnim.setDuration(1000);
        mShakeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotateX = (float) animation.getAnimatedValue(cameraRotateXName);
                rotateY = (float) animation.getAnimatedValue(cameraRotateYName);
                mCanvasTranslateX = (float) animation.getAnimatedValue(canvasTranslateXName);
                mCanvasTranslateY = (float) animation.getAnimatedValue(canvasTranslateYName);
            }
        });
        mShakeAnim.start();
    }

    /**
     * 这个动画稍微有点问题
     */
    private void startShakeAnimation() {
        ValueAnimator animationX = ValueAnimator.ofFloat(rotateX, 0);
        ValueAnimator animationY = ValueAnimator.ofFloat(rotateY, 0);
        ValueAnimator canvasAnimationX = ValueAnimator.ofFloat(mCanvasTranslateX, 0);
        ValueAnimator canvasAnimationY = ValueAnimator.ofFloat(mCanvasTranslateY, 0);
        animationX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotateX = (float) animation.getAnimatedValue();
            }
        });
        animationY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotateY = (float) animation.getAnimatedValue();
            }
        });
        canvasAnimationX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCanvasTranslateX = (float) animation.getAnimatedValue();
            }
        });
        canvasAnimationX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCanvasTranslateY = (float) animation.getAnimatedValue();
            }
        });
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(animationX, animationY, canvasAnimationX, canvasAnimationY);
        animatorSet.setInterpolator(new TimeInterpolator() {
            // 因子数值越小振动频率越高
            float f = 0.57f;

            @Override
            public float getInterpolation(float input) {
                return (float) (Math.pow(2, -2 * input) * Math.sin((input - f / 4) * (2 * Math.PI) / f) + 1);
            }
        });
        animatorSet.setDuration(1000);
        animatorSet.start();

    }


}
