package com.puckowski.launcher4;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SimpleBarGraph extends View
{
	private ArrayList<Double> mDataSampleArray;
	
	private int mGraphWidth;
	private int mGraphHeight;
	
	private String mGraphTitle;
	
	private Paint mPaint; 
	
	public SimpleBarGraph(Context context, AttributeSet attributeSet) 
	{
		super(context, attributeSet);
		
		onCreate();
	}
	
	private void onCreate()
	{
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		mPaint.setColor(Color.CYAN);
		mPaint.setTextSize(24);
	}
	
	public void setSampleData(ArrayList<Double> dataSampleArray)
	{
		mDataSampleArray = dataSampleArray;
		
		this.invalidate();
	}
	
	public ArrayList<Double> getSampleData()
	{
		return mDataSampleArray;
	}
	
	public void setGraphWidth(int graphWidth)
	{
		mGraphWidth = graphWidth;
		
		this.getLayoutParams().width = mGraphWidth;
		
		this.invalidate();
	}
	
	public int getGraphWidth()
	{
		return mGraphWidth;
	}
	
	public void setGraphHeight(int graphHeight)
	{
		mGraphHeight = graphHeight;
		
		this.getLayoutParams().height = mGraphHeight;
		
		this.invalidate();
	}
	
	public int getGraphHeight()
	{
		return mGraphHeight;
	}
	
	public void setGraphTitle(String graphTitle)
	{
		mGraphTitle = graphTitle;
		
		this.invalidate();
	}
	
	public String getGraphTitle()
	{
		return mGraphTitle;
	}
	
	private double getMinimumValue(ArrayList<Double> sampleArray)
	{
		double minimumValue = 100.0;
		
		for(int i = 0; i < sampleArray.size(); i++)
		{
			if(sampleArray.get(i) < minimumValue)
			{
				minimumValue = sampleArray.get(i);
			}
		}
		
		return minimumValue;
	}
	
	private double getMaximumValue(ArrayList<Double> sampleArray)
	{
		double maximumValue = 0.0;
		
		for(int i = 0; i < sampleArray.size(); i++)
		{
			if(sampleArray.get(i) > maximumValue)
			{
				maximumValue = sampleArray.get(i);
			}
		}
		
		return maximumValue;
	}
	
	@Override
    protected void onDraw(Canvas canvas) 
	{
        super.onDraw(canvas);
        
        double minimumSample = getMinimumValue(mDataSampleArray);
        String minimumSampleValue = null;
        
        if(minimumSample == 100.0)
        {
        	minimumSample = 0.0;
        	
        	minimumSampleValue = String.valueOf(minimumSample);
        }
        else
        {
        	minimumSampleValue = String.valueOf((minimumSample * 100.0));
        }
        
        String maximumSampleValue = String.valueOf(100.0); //(getMaximumValue(mDataSampleArray) * 100.0));
        
        if(minimumSampleValue.length() > 5)
        {
        	minimumSampleValue = minimumSampleValue.substring(0, 5);
        }
        
        if(maximumSampleValue.length() > 5)
        {
        	maximumSampleValue = maximumSampleValue.substring(0, 5);
        }
        
        int graphLeftStart = (int) Math.ceil((mPaint.measureText(maximumSampleValue, 0, maximumSampleValue.length()) * 1.2));
        int textHeight = (int) Math.ceil(mPaint.getTextSize());
        
        canvas.drawText(maximumSampleValue, 0, textHeight, mPaint);
        canvas.drawText(minimumSampleValue, 0, mGraphHeight, mPaint);
        
        canvas.drawLine(graphLeftStart, 0, mGraphWidth, 0, mPaint);
        
        int barSize = ((mGraphWidth - graphLeftStart) / 24);
        int currentX = graphLeftStart;
        
        for(int i = 0; i < mDataSampleArray.size(); i++)
        {
        	int currentBarHeight = (int) ((mDataSampleArray.get(i) - minimumSample) * (double) mGraphHeight);
        	
        	canvas.drawRect(currentX, (0 + (mGraphHeight - currentBarHeight)), (currentX + barSize), mGraphHeight, mPaint);
        
        	currentX = (currentX + barSize);
        }
        
        canvas.drawLine(graphLeftStart, mGraphHeight, mGraphWidth, mGraphHeight, mPaint);
    }
}
