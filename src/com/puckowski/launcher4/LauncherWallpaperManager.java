package com.puckowski.launcher4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LauncherWallpaperManager 
{
	private Drawable mWallpaperDrawable;

	private int mHorizontalStep;
	private int mWallpaperHeight;
	
    public LauncherWallpaperManager() 
    { 	
    }
    
    public void resizeWallpaperDrawable(Context context, Drawable systemWallpaper, int numberOfHomescreens, 
    		int displayWidth, int displayHeight)
    { 
    	Bitmap bitmapToScale = ((BitmapDrawable) systemWallpaper).getBitmap(); 
        Bitmap resizedBitmap = null;
        
        int bitmapHeight = displayHeight;
        int bitmapWidth = (int) ((double) displayHeight / (double) bitmapHeight) * bitmapToScale.getWidth();
        	
        mHorizontalStep = (int) ((double) displayWidth / (double) numberOfHomescreens);
        mWallpaperHeight = bitmapHeight;
        
    	System.gc();
        Runtime.getRuntime().gc();  
        	
        try
        {
        	resizedBitmap = Bitmap.createScaledBitmap(bitmapToScale, bitmapWidth,
        			bitmapHeight, false);
        }
        catch(Exception exception)
        {
        }
         
        if(resizedBitmap != null)
        {
        	mWallpaperDrawable = new BitmapDrawable(context.getResources(), resizedBitmap);
        }
    }
    
    public Drawable getDrawableForLayout(Context context, int homescreenIndex, int displayWidth, int displayHeight)
    {
    	homescreenIndex--;

    	Bitmap homescreenBitmap = ((BitmapDrawable) mWallpaperDrawable).getBitmap();
    	Bitmap scaledHomescreenBitmap = null;
    	
    	System.gc();
        Runtime.getRuntime().gc();  
        
    	try
    	{
    		scaledHomescreenBitmap = Bitmap.createBitmap(homescreenBitmap, (mHorizontalStep * homescreenIndex), 0, 
    	    			displayWidth, mWallpaperHeight);
    	}
    	catch(Exception exception)
    	{
    	}
    	 
    	if(scaledHomescreenBitmap != null)
    	{
    		return new BitmapDrawable(context.getResources(), scaledHomescreenBitmap);
    	}
    	else
    	{
    		return mWallpaperDrawable;
    	} 		
    }
}
