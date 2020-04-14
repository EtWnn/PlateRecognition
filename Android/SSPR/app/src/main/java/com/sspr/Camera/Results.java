package com.sspr.Camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

public class Results extends View {
    private static final float TEXT_SIZE_DIP = 16;
    private final float textSizePx;
    private Paint mRectanglePaint = new Paint();
    private Paint mTextPaint = new Paint();
    private RectF mRectangle;
    private int mCustomRed = Color.argb(255, 255, 0 , 0);
    private int mCustomBlack = Color.argb(255, 0, 0 , 0);
    private String mResults = null;
    private int mRectangleStrokeWidth = 8;
    private int mTextStrokeWidth = 2;
    private int mRoundAngle = 20;

    public Results(Context context) {
        super(context);


        //init rect to 0
        reset();

        mRectanglePaint.setColor(mCustomRed);
        mRectanglePaint.setStrokeWidth(mRectangleStrokeWidth);

        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mCustomBlack);
        mTextPaint.setStrokeWidth(mTextStrokeWidth);
        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        mTextPaint.setTextSize(textSizePx);
    }


    public void setRectangle(RectF rectangle){
        mRectangle = new RectF(rectangle);
    }

    public void setResult(String s){
        mResults = s;
    }

    public void pushChange(){
        invalidate();
    }

    public void reset(){
        mResults = null;
        mRectangle = new RectF(0,0,0,0);
    }


    @Override
    public void onDraw(Canvas canvas) {
        if (mResults != null) {
            mRectanglePaint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(mRectangle, mRoundAngle,mRoundAngle, mRectanglePaint);

            mRectanglePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            int paintTextWidth = (int)mTextPaint.measureText(mResults);
            RectF textRectangle = new RectF(mRectangle.left, mRectangle.bottom, mRectangle.left + paintTextWidth, mRectangle.bottom + (int)(mTextPaint.getTextSize() * 1.5f));
            canvas.drawRoundRect(textRectangle, mRoundAngle,mRoundAngle, mRectanglePaint);

            canvas.drawText(mResults, mRectangle.left, mRectangle.bottom + (int)(mTextPaint.getTextSize() * 1.1f), mTextPaint);
        }

        super.onDraw(canvas);
    }
}
