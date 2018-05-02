package com.tg.test.gestureview;


import android.app.Service;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyp on 2018/4/24.
 * 手势解锁View
 */

public class GestureView extends View {

    private Context context;
    private int width, height;
    private float space;//背景圆圈的间隔
    private float circleRadius;//圆半径
    private Paint mPaint, linePaint;
    private float circleStrokeWidth;//背景圆圈的描边宽度
    private int circleColor;//背景圆圈颜色
    private int lineColor;//线的颜色
    private float lineStrokeWidth;//线的宽度
    private float centerCircleRadius;//解锁点的中心实体圆
    private int errorColor;//错误时连线跟中心圆的颜色
    private final int circleNumber = 3;//每行每列圆圈的个数
    private List<PointF> circlePoint;//用来存放背景圆圈的中心坐标
    private List<PointF> gesturePoint;//用来存放手势点击圆圈的中心坐标
    private PointF lineStartPoint, lineMovePoint;
    private Path path;
    private boolean showError;//手势验证错误
    private CountDownTimer countDownTimer;//错误时开启定时器销毁错误连线
    private static final long TIME = 1000;//定时器时长
    private static final long VIBRATE_TIME = 200;//震動的时长


    public GestureView(Context context) {
        this(context, null);
    }

    public GestureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GestureView);
        circleRadius = typedArray.getFloat(R.styleable.GestureView_circleRadius, 90);
        circleStrokeWidth = typedArray.getFloat(R.styleable.GestureView_circleStrokeWidth, 4);
        circleColor = typedArray.getColor(R.styleable.GestureView_circleColor, Color.WHITE);
        lineStrokeWidth = typedArray.getFloat(R.styleable.GestureView_lineStrokeWidth, 8);
        lineColor = typedArray.getColor(R.styleable.GestureView_moveLineColor, Color.WHITE);
        centerCircleRadius = typedArray.getFloat(R.styleable.GestureView_centerCircleRadius, 40);
        errorColor = typedArray.getColor(R.styleable.GestureView_moveLineColor, Color.RED);
        typedArray.recycle();

        if (circlePoint == null)
            circlePoint = new ArrayList<>();
        if (gesturePoint == null)
            gesturePoint = new ArrayList<>();

        if (lineStartPoint == null)
            lineStartPoint = new PointF();
        if (lineMovePoint == null)
            lineMovePoint = new PointF();


        initPaint();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(circleStrokeWidth);
        mPaint.setColor(circleColor);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineStrokeWidth);
        linePaint.setColor(lineColor);
    }

    //测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //宽度以屏幕为基准，计算出水平的间隔,width为wrap的情况下应该根据space来计算
        width = measureDimension(400, widthMeasureSpec);
        space = (width * 1.0f - 2 * circleNumber * (circleRadius + circleStrokeWidth)) / (circleNumber + 1);
        //因为是一个行列相等的，所以高度跟宽度一样
        int wrapHeight = (int) (circleNumber * 2 * (circleRadius + circleStrokeWidth) + (circleNumber - 1) * space);
        height = measureDimension(wrapHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureDimension(int defaultSize, int measureSpec) {
        int size = 0;
        int mode = MeasureSpec.getMode(measureSpec);
        int measureSize = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {//精确值
            size = measureSize;
        } else if (mode == MeasureSpec.AT_MOST) {//wrap_content,需要计算
            size = defaultSize;
        }
        return size;
    }


    //绘制
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //先绘制背景
        circlePoint.clear();
        drawBg(canvas);
        drawGestureLine(canvas);
        drawMoveLine(canvas);
        showError = false;
    }

    //绘制底部行列的圆圈
    private void drawBg(Canvas canvas) {
        for (int i = 0; i < circleNumber; i++) {
            for (int j = 0; j < circleNumber; j++) {
                //先画行，再画列
                float x = j * space + circleRadius + j * 2 * circleRadius + circleStrokeWidth + space;
                float y = i * space + circleRadius + i * 2 * circleRadius + circleStrokeWidth;
                PointF pointF = new PointF(x, y);
                //错误时画的背景圆圈
                if (showError && gesturePoint.contains(pointF))
                    mPaint.setColor(errorColor);
                else
                    mPaint.setColor(circleColor);
                circlePoint.add(pointF);
                canvas.drawCircle(x, y, circleRadius, mPaint);
            }
        }
    }

    //画之前连好的线(已经滑过的点),包括中心实体圆
    private void drawGestureLine(Canvas canvas) {
        if (showError)
            linePaint.setColor(errorColor);
        else
            linePaint.setColor(lineColor);
        //画之前保存下来的点连程序线，为了亮点连完线后的移动画线不受前面的影响，可以画折线
        for (int i = 0; i < gesturePoint.size(); i++) {
            if (path == null)
                path = new Path();
            PointF pointF = gesturePoint.get(i);
            if (i == 0)
                path.moveTo(pointF.x, pointF.y);
            else
                path.lineTo(pointF.x, pointF.y);

            //画实体圆
            linePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pointF.x, pointF.y, centerCircleRadius, linePaint);
            linePaint.setStyle(Paint.Style.STROKE);
        }
        if (path != null)
            canvas.drawPath(path, linePaint);
    }

    //画手指移动的线，不包括之前连好的点
    private void drawMoveLine(Canvas canvas) {
        if (lineStartPoint.x != 0 && lineStartPoint.y != 0)
            canvas.drawLine(lineStartPoint.x, lineStartPoint.y, lineMovePoint.x, lineMovePoint.y, linePaint);
        //找到除了起点之后的点
        if (checkOnCircle(lineMovePoint)) {
            lineStartPoint.set(gesturePoint.get(gesturePoint.size() - 1).x, gesturePoint.get(gesturePoint.size() - 1).y);
        }
    }


    private void startCountTimer() {
        if (countDownTimer == null) {
            countDownTimer = new CountDownTimer(TIME, TIME) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    releasePoint(true, true);
                }
            };


            countDownTimer.start();
        }
    }

    private void cancerCountTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    //处理滑动解锁事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //按下时把上一次处理错误显示的定时器销毁
                cancerCountTimer();
                releasePoint(true, false);
                //判断第一个按下的点是否在圆圈范围内
                lineStartPoint.x = event.getX();
                lineStartPoint.y = event.getY();
                if (!checkOnCircle(lineStartPoint))
                    lineStartPoint.set(0, 0);
                else//把第一个点设置为圆圈圆心
                    lineStartPoint.set(gesturePoint.get(0).x, gesturePoint.get(0).y);
                break;
            case MotionEvent.ACTION_MOVE:
                //如果按下的时候不在圆圈范围，一直移动找到第一个在圆圈范围的点
                if (lineStartPoint.x == 0 && lineStartPoint.y == 0) {
                    lineStartPoint.x = event.getX();
                    lineStartPoint.y = event.getY();
                    Log.d("xudaha", "startX:" + lineStartPoint.x + "---startY:" + lineStartPoint.y);
                    if (!checkOnCircle(lineStartPoint)) {
                        lineStartPoint.set(0, 0);
                        break;
                    } else//把第一个点设置为圆圈圆心
                        lineStartPoint.set(gesturePoint.get(0).x, gesturePoint.get(0).y);
                }

                lineMovePoint.x = event.getX();
                lineMovePoint.y = event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                //解决抬起时尾巴不在圆圈中心
                releasePoint(false, true);
                //手指抬起，验证密码是否正确，或者第一次设置密码保存数据库
                String password = pointToPassword();
                Log.d("xudahaPassword", password);
                if (this.gestureFinish != null && !TextUtils.isEmpty(password))
                    gestureFinish.onFinish(password, gesturePoint.size());
                break;
        }
        return true;
    }


    //判断是否点在某个圆圈内
    private boolean checkOnCircle(PointF point) {
        boolean onCircle = false;
        for (PointF pointF : circlePoint) {
            if (point.x <= pointF.x + circleRadius + circleStrokeWidth && point.x >= pointF.x - circleRadius - circleStrokeWidth
                    && point.y <= pointF.y + circleRadius + circleStrokeWidth && point.y >= pointF.y - circleRadius - circleStrokeWidth) {
                if (!gesturePoint.contains(pointF)) {
                    //不能有重复的点
                    onCircle = true;
                    gesturePoint.add(pointF);
                    //实现触摸震动
                    vibrate(VIBRATE_TIME / 4);
                    //实时告诉外部触摸的点位置，记住当初画背景圆圈的时候是从行开始画列的
                    if (this.gestureFinish != null)
                        this.gestureFinish.onMoveIndex(circlePoint.indexOf(pointF));
                }
                break;
            }
        }
        return onCircle;
    }


    public void releasePoint(boolean releaseGesturePoint, boolean invalidate) {
        lineStartPoint.set(0, 0);
        lineMovePoint.set(0, 0);
        if (releaseGesturePoint)
            gesturePoint.clear();
        if (path != null) {
            path.reset();
            path = null;
        }
        if (invalidate)
            invalidate();
    }


    private String pointToPassword() {
        StringBuilder password = new StringBuilder();
        for (PointF pointF : gesturePoint) {
            password.append(pointF.x).append(pointF.y);
        }
        return password.toString();
    }

    //震动
    private void vibrate(long time) {
        Vibrator vib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        if (vib != null)
            vib.vibrate(time);
    }

    public interface GestureFinish {
        void onFinish(String password, int pointNumber);

        //表示手指触摸的是哪个位置的点
        void onMoveIndex(int index);
    }

    private GestureFinish gestureFinish;

    public void setGestureFinish(GestureFinish gestureFinish) {
        this.gestureFinish = gestureFinish;
    }

    /**
     * 错误时显示,改变手势点背景圆圈的颜色，连线的颜色跟实体圆的颜色
     */
    public void showErrorUi() {
        showError = true;
        invalidate();
        //启动一个定时器自动把错误的连线清空
        startCountTimer();
        vibrate(VIBRATE_TIME);
    }


    /**
     * Activity/Fragment等生命周期結束时最好调用这个方法，销毁定时器
     */
    public void onDestory() {
        releasePoint(true, true);
        cancerCountTimer();
        this.gestureFinish = null;
    }
}
