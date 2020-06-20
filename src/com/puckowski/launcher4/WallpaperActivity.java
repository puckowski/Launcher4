package com.puckowski.launcher4;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class WallpaperActivity extends Activity 
{
	private Bitmap mWallpaperBitmap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		onCreate(); 
	}
	
	private void onCreate()
	{
		setContentView(R.layout.activity_wallpaper);
		setOnClickListeners();

		System.gc();
        Runtime.getRuntime().gc();  
        
		Intent pickImage = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(pickImage, R.id.REQUEST_PICK_IMAGE);
	}
	
	private void setOnClickListeners() 
	{ 
		final Button selectImageButton = (Button) findViewById(R.id.selectImage),
				cancelActivityButton = (Button) findViewById(R.id.cancelActivity);
		
		View.OnClickListener buttonListener = new View.OnClickListener() 
		{
		    public void onClick(View view) 
		    {
		    	if(selectImageButton.getId() == ((Button) view).getId())
		    	{
		    		WallpaperManager wallpaperManager = WallpaperManager.getInstance(WallpaperActivity.this);
		    		
		    		if(mWallpaperBitmap != null)
		    		{
		    			try 
		    			{
		    				wallpaperManager.setBitmap(mWallpaperBitmap);
		    			}
		    			catch(Exception exception)
		    			{
		    			}
		    		}
		        }
		        
		    	finish();
		    }
		};
 
		selectImageButton.setOnClickListener(buttonListener);
	    cancelActivityButton.setOnClickListener(buttonListener);
	}
	
	private String getImagePath(Uri imageUri) 
	{
		Cursor cursor = null;
		 
		String[] filePathColumn = 
		{ 
		    MediaStore.Images.Media.DATA 
		};
		
		try 
		{ 
		    cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
		    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    
		    cursor.moveToFirst();
		    
		    return cursor.getString(columnIndex);
		} 
		finally 
		{
		    if(cursor != null) 
		    {
		        cursor.close();
		    }
        }
    }
	
	private int calculateSampleSize(BitmapFactory.Options bitmapFactoryOptions, int preferredWidth, int preferredHeight) 
	{
		final int height = bitmapFactoryOptions.outHeight;
		final int width = bitmapFactoryOptions.outWidth;
		
        int inSampleSize = 1;

        if(height > preferredHeight || width > preferredWidth) 
        {
        	final int halfHeight = (height / 2);
        	final int halfWidth = (width / 2);

        	while((halfHeight / inSampleSize) > preferredHeight
                && (halfWidth / inSampleSize) > preferredWidth)
        	{
        		inSampleSize = (inSampleSize * 2);
        	}
        }

        return inSampleSize;
	}
	
	private int[] parseWallpaperResolution(String targetResolutionPreference)
    {
    	int wallpaperResolution[] = new int[2];
    	
    	wallpaperResolution[0] = Integer.parseInt(targetResolutionPreference.substring(0, 
    			targetResolutionPreference.indexOf("x")));
    	
    	wallpaperResolution[1] = Integer.parseInt(targetResolutionPreference.substring((targetResolutionPreference.indexOf("x") + 1),
    			targetResolutionPreference.length()));
    	
    	return wallpaperResolution;
    }
	
	private void getImageBitmap(Intent data)
	{
		Uri selectedImage = data.getData();
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String targetResolutionPreference = sharedPreferences.getString("wallpaperImageQuality", "1280x720");
			
		int targetResolution[] = parseWallpaperResolution(targetResolutionPreference);
		
		System.gc();
        Runtime.getRuntime().gc();
		
		BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
		bitmapFactoryOptions.inJustDecodeBounds = true;
			
		BitmapFactory.decodeFile(getImagePath(selectedImage), bitmapFactoryOptions);
				
		bitmapFactoryOptions.inSampleSize = calculateSampleSize(bitmapFactoryOptions, targetResolution[0], targetResolution[1]);
		bitmapFactoryOptions.inJustDecodeBounds = false;
		
		boolean tryDitherPreference = sharedPreferences.getBoolean("ditherWallpaper", false);
		boolean disablePurgeablePreference = sharedPreferences.getBoolean("disableInPurgeable", false);
		
		bitmapFactoryOptions.inDither = tryDitherPreference;
		bitmapFactoryOptions.inPurgeable = (! disablePurgeablePreference);
		bitmapFactoryOptions.inInputShareable = (! disablePurgeablePreference);
		
		String bitmapConfigPreference = sharedPreferences.getString("wallpaperBitmapConfig", "RGB_565");
		
		if(bitmapConfigPreference.equals("RGB_565"))
		{
			bitmapFactoryOptions.inPreferredConfig = Config.RGB_565; 
		}
		else if(bitmapConfigPreference.equals("ARGB_4444"))
		{
			bitmapFactoryOptions.inPreferredConfig = Config.ARGB_4444; 
		}
		else if(bitmapConfigPreference.equals("ARGB_8888"))
		{
			bitmapFactoryOptions.inPreferredConfig = Config.ARGB_8888; 
		}
		
		mWallpaperBitmap = BitmapFactory.decodeFile(getImagePath(selectedImage), bitmapFactoryOptions);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{		
		if(resultCode == RESULT_OK)
		{		
			if(requestCode == R.id.REQUEST_PICK_IMAGE)
			{	
				getImageBitmap(data); 
				 
				if(mWallpaperBitmap != null)
				{			
					((ImageView) findViewById(R.id.wallpaperPreview)).setImageBitmap(mWallpaperBitmap);
				}
				else
				{
					finish();
				}
			}
		} 
		else if(resultCode == RESULT_CANCELED) 
		{
			finish();
		}
	}
}
