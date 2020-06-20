package com.puckowski.launcher4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SimpleLineGraph extends View
{
	private Paint mPaint;
	
	public SimpleLineGraph(Context context, AttributeSet attributeSet) 
	{
		super(context, attributeSet);
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	}
	
	public void createChart(int sampleDataPercentages[])
	{
		
	}
	
	@Override
    protected void onDraw(Canvas canvas) 
	{
        super.onDraw(canvas);
    }
}
