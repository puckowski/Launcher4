package com.puckowski.launcher4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class HomescreenScrollbar extends SurfaceView
{	
	private final int HOMESCREEN_SCROLLBAR_HEIGHT = 2;
	
	private SurfaceHolder mSurfaceHolder;
	private Paint mPaint;
	
	private Context mContext;
	
	private boolean mScrollbarHidden;

	public HomescreenScrollbar(Context context, AttributeSet attributeSet) 
	{
		super(context, attributeSet);
		
		mContext = context; 
		
		onCreate();
	}
	
	private void onCreate()
	{
		mSurfaceHolder = this.getHolder();
		
		mPaint = new Paint();
		
		this.setZOrderOnTop(true);  
		mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
	}
	
	public void setVisibility(boolean isHidden)
	{
		if(isHidden == false)
		{
			this.getLayoutParams().height = ((int) getPixelsFromDp(HOMESCREEN_SCROLLBAR_HEIGHT));
			
			mScrollbarHidden = false;
		}
		else if(isHidden == true)
		{
			this.getLayoutParams().height = ((int) getPixelsFromDp(0));
			
			mScrollbarHidden = true;
		}
	}
	
	public boolean isHidden()
	{
		return mScrollbarHidden;
	}
	
	public int getScrollbarHeight()
	{
		if(isHidden())
		{
			return 0;
		}
		else
		{
			return (int) getPixelsFromDp(HOMESCREEN_SCROLLBAR_HEIGHT);
		}
	}
	
	public void setScrollbarWidth(int newWidth)
	{
		if(newWidth == 0)
		{
			return;
		}
		
		if(this.getLayoutParams().width != newWidth)
		{
			int displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
			
			if(newWidth > displayWidth)
			{
				return;
			}
			
			this.getLayoutParams().width = newWidth;
			
			int totalMarginNeeded = (displayWidth - newWidth);
			
			((android.widget.LinearLayout.LayoutParams) this.getLayoutParams()).setMargins((totalMarginNeeded / 2), 0, (totalMarginNeeded / 2), 0);
		}
	}
	
	public void resetScrollbarWidth()
	{
		int displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
		
		if(this.getLayoutParams().width != displayWidth)
		{
			this.getLayoutParams().width = displayWidth;
			
			((android.widget.LinearLayout.LayoutParams) this.getLayoutParams()).setMargins(0, 0, 0, 0);
		}
	}
	
	private float getPixelsFromDp(float dp)
	{
	    return (dp * mContext.getResources().getDisplayMetrics().density);
	}
	
	public void repaint(int currentHomescreen, int numberOfHomescreens)
	{
		if(this.getLayoutParams().height == 0)
		{
			return;
		}
		
		Canvas canvas = null;
		  
		try 
		{
		    canvas = mSurfaceHolder.lockCanvas();
		
		    int scrollbarSize = (this.getWidth() / numberOfHomescreens);
		    int scrollbarOffset = ((currentHomescreen - 1) * scrollbarSize);
		    
		    int darkGoldenrod = getResources().getColor(R.color.color_dark_goldenrod);
		    mPaint.setColor(darkGoldenrod);
		    
		    canvas.drawRect(scrollbarOffset, 0, (scrollbarOffset + scrollbarSize), this.getHeight(), mPaint);
		}
		finally 
		{
		    if(canvas != null) 
		    {
		    	mSurfaceHolder.unlockCanvasAndPost(canvas);
		    }
		}
		
		Handler handler = new Handler();
		
	    handler.postDelayed(new Runnable() 
	    { 
	         public void run() 
	         { 
	        	 reset();
	         } 
	    }, 300);
	}
	
	public void hide(boolean isDelayed)
	{
		if(this.getLayoutParams().height == 0)
		{
			return;
		}
		
		Handler handler = new Handler();
		
		int drawDelayTime;
		
		if(isDelayed == true)
		{
			drawDelayTime = 500;
		}
		else 
		{
			drawDelayTime = 0;
		}
		
	    handler.postDelayed(new Runnable() 
	    { 
	         public void run() 
	         { 
	        	 delayedHide();
	         } 
	    }, drawDelayTime);
	}
	
	private void delayedHide()
	{
		Canvas canvas = null;
		  
		try 
		{
		    canvas = mSurfaceHolder.lockCanvas();
		
		    mPaint.setColor(Color.BLACK);
		    
		    canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), mPaint);
		}
		finally 
		{
		    if(canvas != null) 
		    {
		    	mSurfaceHolder.unlockCanvasAndPost(canvas);
		    }
		}
	}
	
	public void reset()
	{
		if(this.getLayoutParams().height == 0)
		{
			return;
		}
		
		Canvas canvas = null;
		  
		try 
		{
		    canvas = mSurfaceHolder.lockCanvas();

		    Paint clearPaint = new Paint(); 
		    clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		    canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), clearPaint);
		}
		finally 
		{
		    if(canvas != null) 
		    {
		    	mSurfaceHolder.unlockCanvasAndPost(canvas);
		    }
		}
	}
}
