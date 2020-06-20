package com.puckowski.launcher4;

import java.util.ArrayList;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Vibrator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class AllHomescreensView extends ScrollView
{
	private Context mContext;

	private Paint mPaint;
	
	public AllHomescreensView(Context context) 
	{
		super(context);   
		
		mContext = context;
		
		onCreate();
	}

	private void onCreate()
	{
		setVerticalScrollBarEnabled(false);
		
		LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.all_homescreens, this);

        mPaint = new Paint();
		
		int darkGoldenrod = getResources().getColor(R.color.color_dark_goldenrod);
		mPaint.setColor(darkGoldenrod); 
	}
	
	public void populateView(ArrayList<ViewGroup> homescreenArray)
	{
		if(mContext == null)
		{
			return;
		}
		
		for(int i = 0; i < homescreenArray.size(); i++)
		{
			ViewGroup homescreen = homescreenArray.get(i);
			
			ImageButton homescreenButton = buildHomescreenButton(homescreen, i);
			
			if(homescreenButton == null)
			{
				continue;
			}
			
			((LinearLayout) findViewById(R.id.allHomescreensLayout)).addView(homescreenButton);
		}
	}
	
	private int[] getHomescreenDimensions()
	{	
		int homescreenWidth = getResources().getDisplayMetrics().widthPixels;
		int homescreenHeight = ((HomescreenActivity) mContext).getHomescreenHeight();
        
        return new int[] { homescreenWidth, homescreenHeight };
	}
	
	private ImageButton buildHomescreenButton(ViewGroup homescreen, final int currentIndex)
	{
		int[] homescreenDimensions = getHomescreenDimensions();
		
		int homescreenWidth = homescreenDimensions[0];
		int homescreenHeight = homescreenDimensions[1];
		
		ImageButton homescreenButton = new ImageButton(mContext);
		
		int viewHeight = (int) ((homescreenHeight / 3) + getPixelsFromDp(3));
		homescreenButton.setMaxHeight((int) getPixelsFromDp(viewHeight));
		
		double widthToHeightAspectRatio = ((double) homescreenWidth / (double) homescreenHeight);
		
		int viewWidth = (int) (viewHeight * widthToHeightAspectRatio);
		homescreenButton.setMaxWidth((int) getPixelsFromDp(viewWidth));
		
		Bitmap homescreenBitmap = loadBitmapFromViewGroup(homescreen, homescreenWidth, homescreenHeight, viewWidth, viewHeight);
		
		if(homescreenBitmap == null)
		{
			return null;
		}
		
		homescreenButton.setPadding((int) getPixelsFromDp(16), (int) getPixelsFromDp(16), 
				(int) getPixelsFromDp(16), (int) getPixelsFromDp(16));
		
		homescreenButton.setImageBitmap(homescreenBitmap);
		
		homescreenButton.setBackgroundResource(R.drawable.custom_view_transparent_selector);
		homescreenButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View view) 
			{
				Vibrator vibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
				vibrator.vibrate(25);
				
				((HomescreenActivity) mContext).setHomescreen((currentIndex + 1));
			}
		});
		
		return homescreenButton;
	}
	
	private float getPixelsFromDp(float dp)
	{
	    return (dp * getResources().getDisplayMetrics().density);
	}
	
	private Bitmap loadBitmapFromViewGroup(ViewGroup viewGroup, int homescreenWidth, int homescreenHeight, int viewWidth, int viewHeight) 
	{  
        Bitmap bitmap = Bitmap.createBitmap(homescreenWidth, (int) (homescreenHeight + getPixelsFromDp(3)), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
                 
        viewGroup.draw(canvas);
        drawSeparatorOnCanvas(canvas, homescreenWidth, homescreenHeight);
        
        bitmap = Bitmap.createScaledBitmap(bitmap, viewWidth, viewHeight, true);
        
        return bitmap;
	}
	
	private void drawSeparatorOnCanvas(Canvas canvas, int homescreenWidth, int homescreenHeight)
	{
		canvas.drawRect(0, homescreenHeight, homescreenWidth, (int) (homescreenHeight + getPixelsFromDp(3)), mPaint);
	}
}
