package com.puckowski.launcher4;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

public class CroppedDrawable extends Drawable 
{
    private final Drawable mWallpaper;

    public CroppedDrawable(Drawable wallpaper) 
    {
        mWallpaper = wallpaper;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) 
    {
        super.setBounds(left, top, right, bottom);

        mWallpaper.setBounds(left, top, left + mWallpaper.getIntrinsicWidth(),
                top + mWallpaper.getIntrinsicHeight());
    }

    public void draw(Canvas canvas) 
    {
        mWallpaper.draw(canvas);
    }

    public void setAlpha(int alpha) 
    {
        mWallpaper.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter colorFilter) 
    {
        mWallpaper.setColorFilter(colorFilter);
    }

    public int getOpacity() 
    {
        return mWallpaper.getOpacity();
    }
}