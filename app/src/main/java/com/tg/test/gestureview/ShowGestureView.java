package com.tg.test.gestureview;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyp on 2018/4/27.
 * 用于展示手势滑动点击哪个点位置
 */

public class ShowGestureView extends View {

    private Paint paint;
    private float circleRadius;//半径
    private int circleColor;//颜色
    private float strokeWidth;//圆圈描边宽度
    private int circleNumber;//每行每列个数
    private float space;//间隔
    private int width, height;
    private List<Integer> indexS;

    public ShowGestureView(Context context) {
        this(context, null);
    }

    public ShowGestureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShowGestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShowGestureView);
        circleRadius = typedArray.getFloat(R.styleable.ShowGestureView_show_circleRadius, 16);
        strokeWidth = typedArray.getFloat(R.styleable.ShowGestureView_show_strokeWidth, 1);
        circleColor = typedArray.getColor(R.styleable.ShowGestureView_show_color, Color.WHITE);
        circleNumber = typedArray.getInteger(R.styleable.ShowGestureView_show_number, 3);
        space = typedArray.getFloat(R.styleable.ShowGestureView_show_space, 20);
        typedArray.recycle();

        init();
    }

    private void init() {
        indexS = new ArrayList<>();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(circleColor);
    }

    //测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wrapWidth = (int) (space * (circleNumber - 1) + (circleRadius + strokeWidth) * 2 * circleNumber);
        //宽度以屏幕为基准，计算出水平的间隔,width为wrap的情况下应该根据space来计算，这里就没有实现了
        width = measureDimension(wrapWidth, widthMeasureSpec);
        //因为是一个行列相等的，所以高度跟宽度一样
        height = measureDimension(width, heightMeasureSpec);
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBg(canvas);
    }


    //绘制底部行列的圆圈
    private void drawBg(Canvas canvas) {
        int index = -1;
        for (int i = 0; i < circleNumber; i++) {
            for (int j = 0; j < circleNumber; j++) {
                index++;
                //先画行，再画列
                float x = j * space + circleRadius + j * 2 * circleRadius + strokeWidth;
                float y = i * space + circleRadius + i * 2 * circleRadius + strokeWidth;
                if (indexS.contains(index))
                    paint.setStyle(Paint.Style.FILL);
                else
                    paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(x, y, circleRadius, paint);
            }
        }
    }


    /**
     * -1为清空
     *
     * @param index
     */
    public void showGesture(int index) {
        if (index == -1)
            this.indexS.clear();
        else
            this.indexS.add(index);
        invalidate();
    }
}
