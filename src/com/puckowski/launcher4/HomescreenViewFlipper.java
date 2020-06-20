package com.puckowski.launcher4;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ViewFlipper;

public class HomescreenViewFlipper extends ViewFlipper 
{
	final int LONG_PRESS_THRESHOLD = 300000000;
	final int MINIMUM_SWIPE_DISTANCE = 100;
	final int MAXIMUM_SWIPE_THRESHOLD = 16;
	
	private float mDownX;
	private float mDownY;
	
	private float mUpX;
	private int mDeltaX; 
	
	private long mTouchStartTime;
	private long mTouchEndTime;
	
	private boolean mLongPressInvalid;
	private boolean mTouchActionTaken;
	
	public HomescreenViewFlipper(Context context, AttributeSet attributeSet) 
	{
		super(context, attributeSet); 
		
		setCacheAttributes(context);
	}
	
	public void setCacheAttributes(Context context)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean drawWithCache = sharedPreferences.getBoolean("drawWithCache", true);
		
		setAnimationCacheEnabled(drawWithCache);
		setDrawingCacheEnabled(drawWithCache);
		
		setAlwaysDrawnWithCacheEnabled(drawWithCache);
		
		String drawingCacheQuality = sharedPreferences.getString("drawingCacheQuality", "HIGH");
		
		if(drawingCacheQuality.equals("HIGH"))
		{
			setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		}
		else if(drawingCacheQuality.equals("LOW"))
		{
			setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		}
		else if(drawingCacheQuality.equals("AUTO"))
		{
			setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
		}
	}
	
    private void onRightToLeftSwipe()
	{    	    	
    	((HomescreenActivity) getContext()).toNextHomescreen();
	}

	private void onLeftToRightSwipe()
	{	
		((HomescreenActivity) getContext()).toPreviousHomescreen();
	}
	
	private void onLongPress()
	{
		((HomescreenActivity) getContext()).onLongPress(true);
	}
	
	private void onAppWidgetRemove(int downX, int downY)
	{
		((HomescreenActivity) getContext()).removeWidget(downX, downY);
	}
	
	private void onAppWidgetExpand(int downX, int downY)
	{
		((HomescreenActivity) getContext()).expandWidget(downX, downY);
	}
	
	private void onAppWidgetCollapse(int downX, int downY)
	{
		((HomescreenActivity) getContext()).collapseWidget(downX, downY); 
	}
	
	private int densityIndependentSwipe(float swipePixelDistance)
	{
	    return (int) (swipePixelDistance / getResources().getDisplayMetrics().density);
	}

    public boolean onInterceptTouchEvent(MotionEvent motionEvent)
    {   
    	super.onInterceptTouchEvent(motionEvent);
    	
    	switch(motionEvent.getAction())
		{
			case MotionEvent.ACTION_DOWN: 
			{
				mLongPressInvalid = false;
				mTouchActionTaken = false;
				
				mTouchStartTime = System.nanoTime(); 
				LongPressTimer longPressTimer = new LongPressTimer(this);
				
				mDownX = motionEvent.getX();
				mDownY = motionEvent.getY();
							
				String actionType = ((HomescreenActivity) getContext()).getInterceptActionType();
				
				if(actionType.equals("NO_ACTION"))
				{
					return false;
				}
				else if(actionType.equals("REMOVE_APPWIDGET"))
				{
					onAppWidgetRemove((int) mDownX, (int) mDownY);
					
					mTouchActionTaken = true;
					
					return true;
				}
				else if(actionType.equals("EXPAND_APPWIDGET"))
				{
					onAppWidgetExpand((int) mDownX, (int) mDownY);
					
					mTouchActionTaken = true;
					
					return true;
				}
				else if(actionType.equals("COLLAPSE_APPWIDGET"))
				{
					onAppWidgetCollapse((int) mDownX, (int) mDownY);
					
					mTouchActionTaken = true;
					
					return true;
				}
			}
			case MotionEvent.ACTION_MOVE:
			{   
				mLongPressInvalid = true;
				
				float currentY = motionEvent.getY();
						
				if(Math.abs((mDownY - currentY)) < densityIndependentSwipe(MAXIMUM_SWIPE_THRESHOLD))
				{ 
					mTouchActionTaken = true;
					
					return true;	
				}
				else
				{
					return false;
				}
			}
			case MotionEvent.ACTION_UP:
			{   
				mLongPressInvalid = true;
			}
		}
		
		return false;
    }
    
    public boolean onTouchEvent(MotionEvent motionEvent) 
    {	   
    	if(mTouchStartTime == -1)
    	{
    		return true;
    	}
    	
    	super.onTouchEvent(motionEvent);
    	
    	switch(motionEvent.getAction())
		{
        	case MotionEvent.ACTION_UP: 
        	{	
        		mLongPressInvalid = true;
        		
        		mUpX = motionEvent.getX();
        		mDeltaX = (int) (mDownX - mUpX);
        		        		
        		if(Math.abs(mDeltaX) > densityIndependentSwipe(MINIMUM_SWIPE_DISTANCE))
        		{
        			if(mDeltaX < 0) 
        			{	
        				onLeftToRightSwipe();
        			}
        			if(mDeltaX > 0) 
        			{ 
        				onRightToLeftSwipe();
        			}
					
					mTouchActionTaken = true;
        		}
            }
		}
    	
    	if((mTouchEndTime - mTouchStartTime) > LONG_PRESS_THRESHOLD && mTouchActionTaken == false)
    	{
    		onLongPress();
    	}
    	
    	return true;
	}
    
    @Override
    public void onAnimationStart()
    {
    	super.onAnimationStart();
    }
    
    @Override 
    public void onAnimationEnd()
    {
    	super.onAnimationEnd();
    }
    
    private class LongPressTimer extends Thread
    {    	
    	public LongPressTimer(final HomescreenViewFlipper homescreenViewFlipper)
    	{
    		super(new Runnable() 
    		{
    			public void run() 
    			{
    				Looper.prepare();
   
    				long elapsedTime = 0;
    				
    				while(elapsedTime < LONG_PRESS_THRESHOLD && mLongPressInvalid == false)
    				{
    					elapsedTime = (System.nanoTime() - mTouchStartTime);
    				}
    				
    				mTouchEndTime = elapsedTime;
    				
    				if(elapsedTime > LONG_PRESS_THRESHOLD && mLongPressInvalid == false)
    				{	
    					post(new Runnable() 
    					{
    		                public void run() 
    		                {
    		                   homescreenViewFlipper.onLongPress();
    		                }
    		            });
    					    				    					
            			mTouchStartTime = -1;
    				}
       			}
    		});
    		
    		this.start();
       	}
    };
}