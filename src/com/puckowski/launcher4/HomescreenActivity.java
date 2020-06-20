package com.puckowski.launcher4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomescreenActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
	private final BroadcastReceiver mWallpaperIntentReceiver = new WallpaperIntentReceiver();
	
	private final String APPWIDGET_DATA_FILE  = "appwidget_data.txt";
	private final String HOMESCREEN_DATA_FILE = "homescreen_data.txt";
	private final String DIMENSIONS_DATA_FILE = "dimensions_data.txt";
	
	private HomescreenViewFlipper mViewFlipper;
	private AllHomescreensView mAllHomescreensView;
	private LauncherWallpaperManager mLauncherWallpaperManager;
	private GridView mApplicationGrid;
	
	private static ArrayList<ApplicationInfo> mApplications;
	
	private ArrayList<ViewGroup> mHomescreenArray;
	private ArrayList<Integer> mAppWidgetIdArray;
	private ArrayList<Integer> mAppWidgetIndexArray;
	private ArrayList<String> mAppWidgetDimensionsArray;
	
	private AppWidgetManager mAppWidgetManager;
	private AppWidgetHost mAppWidgetHost;
	
	private int mCurrentHomescreen;
	private int mNumberOfHomescreens;

	private String mInterceptActionType;
							
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_homescreen);

		mAppWidgetIdArray = new ArrayList<Integer>();
		mAppWidgetIndexArray = new ArrayList<Integer>();
		mAppWidgetDimensionsArray = new ArrayList<String>();
		mHomescreenArray = new ArrayList<ViewGroup>();
		
		registerIntentReceivers();
		
		loadApplications(true);
        bindApplications();
		
		mViewFlipper = (HomescreenViewFlipper) findViewById(R.id.viewFlipper);
		
		ViewGroup primaryHomescreen = (ViewGroup) findViewById(R.id.primaryHomescreen);
		mHomescreenArray.add(primaryHomescreen);
	
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		mNumberOfHomescreens = Integer.parseInt(sharedPreferences.getString("numberOfHomescreens", "1"));
		updateHomescreenList();
		
		mViewFlipper.setDisplayedChild(1);
		mCurrentHomescreen = 1;
		
		mAppWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetHost = new AppWidgetHost(this, R.id.APPWIDGET_HOST_ID);
		
		readWidgetData();
		reinstateSavedWidgets();
		measureOffscreenLayouts();
		
	    setWallpaperDrawable(null);
	    
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(sharedPreferences, null);
		
	    ImageButton appListButton = (ImageButton) findViewById(R.id.appListButton);
	    appListButton.setOnClickListener(new ApplicationButtonListener());
	    appListButton.setOnLongClickListener(new ApplicationButtonLongClickListener());
	    
	    mInterceptActionType = "NO_ACTION";
	}
	
	@Override
    protected void onRestoreInstanceState(Bundle state)
    {
        super.onRestoreInstanceState(state);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        
        if(hasFocus && mCurrentHomescreen == 0)
        {
        	hideHomescreenScrollbar(false);
        }
    }
    
    @Override
	protected void onStart()
	{
		super.onStart();
		
		mAppWidgetHost.startListening();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		saveWidgetData();
	}
	
	@Override
    protected void onResume()
    {
        super.onResume();
        
        loadApplications(false);
        bindApplications();
        resumeService();
        
        HomescreenDock homescreenDock = new HomescreenDock(this);
        homescreenDock.updateList();
        homescreenDock.getDockWidth(); 
    }
	
	@Override
	protected void onStop()
	{
		super.onStop();
		
		saveWidgetData(); 
		stopService();
		
		mAppWidgetHost.stopListening();
	}
	
	@Override
    public void onDestroy()
    {
        super.onDestroy();
        
        unregisterReceiver(mApplicationsReceiver);
        unregisterReceiver(mWallpaperIntentReceiver);
        
        Runtime.getRuntime().gc();
        System.gc();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.homescreen_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) 
	{
		switch(menuItem.getItemId()) 
		{
			case R.id.addWidget:
				if(mCurrentHomescreen == 0)
				{
					mViewFlipper.setDisplayedChild(1);
					
					mCurrentHomescreen = 1;
				}
				
				selectWidget();
				
				return true;
		
			case R.id.removeWidget:
				if(mCurrentHomescreen == 0)
				{
					mViewFlipper.setDisplayedChild(1);
					
					mCurrentHomescreen = 1;
				}
				
				removeWidgetMenuSelected();
				
				return true;
				
			case R.id.expandWidget:
				if(mCurrentHomescreen == 0)
				{
					mViewFlipper.setDisplayedChild(1);
					
					mCurrentHomescreen = 1;
				}
				
				expandWidgetMenuSelected();
				
				return true;
				
			case R.id.collapseWidget:
				if(mCurrentHomescreen == 0)
				{
					mViewFlipper.setDisplayedChild(1);
					
					mCurrentHomescreen = 1;
				}
				
				collapseWidgetMenuSelected();
				
				return true;
				
			case R.id.setWallpaper:
				startWallpaperActivity();
				
				return true;
				
			case R.id.launcherSettings:
				startPreferenceActivity();
				
				return true;
				
			case R.id.deviceSettings:
				startSettingsActivity();
				
				return true;
		}
		
		return super.onOptionsItemSelected(menuItem);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceArgument)
	{        	    		
	    mNumberOfHomescreens = Integer.parseInt(sharedPreferences.getString("numberOfHomescreens", "1"));
	    
	    updateHomescreenList();
	    updateDock();
	    updateScrollbarVisibility();
	    updateWallpaper();
	    updateService();
	    mViewFlipper.setCacheAttributes(this);
	    updateOrientation();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) 
	{
	    if(keyCode == KeyEvent.KEYCODE_BACK) 
	    {
	        keyEvent.startTracking();

	        return true;
	    }
	    
	    return super.onKeyDown(keyCode, keyEvent);
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent keyEvent)
	{
	    switch(keyCode) 
	    {
	    	case KeyEvent.KEYCODE_BACK: 
	    	{	  
	    		if(mCurrentHomescreen == 1 || mCurrentHomescreen == -1 || !mInterceptActionType.equals("NO_ACTION"))
	    		{
	    			flashCurrentViewGroup();
	    			
	    			return true;
	    		}
	    		else 
	    		{
	    			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
	    			boolean overrideHomeButton = sharedPreferences.getBoolean("overrideBackButton", false);
	    			
	    			if(overrideHomeButton == true)
	    			{
	    				Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
	    				vibrator.vibrate(25);
	    				
	    				mAllHomescreensView = new AllHomescreensView(this);
	    				mAllHomescreensView.populateView(mHomescreenArray);
	    				
	    				mViewFlipper.addView(mAllHomescreensView);
	    				
	    				mViewFlipper.setInAnimation(this, R.anim.all_homescreens_in);
	    				mViewFlipper.setOutAnimation(this, R.anim.all_homescreens_fade_out);
	    				
	    				mViewFlipper.setDisplayedChild((mViewFlipper.getChildCount() - 1));
	    				
	    				mCurrentHomescreen = -1;
	    				
	    				return true;
	    			}
	    		}
	    	}
	    }

	    return super.onKeyLongPress(keyCode, keyEvent);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) 
	{
	    return super.onKeyUp(keyCode, keyEvent);
	}
	
	private void registerIntentReceivers()
	{
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        
        registerReceiver(mApplicationsReceiver, filter);
        
        filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        
        registerReceiver(mWallpaperIntentReceiver, filter);
    }
	
	private void loadApplications(boolean isLaunching)
    {
        if(isLaunching && mApplications != null)
        {
            return;
        }

        PackageManager packageManager = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> applications = packageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(applications, new ResolveInfo.DisplayNameComparator(packageManager));

        if(applications != null)
        {
            final int applicationCount = applications.size();

            if(mApplications == null) 
            {
                mApplications = new ArrayList<ApplicationInfo>(applicationCount);
            }
            
            mApplications.clear();

            for(int i = 0; i < applicationCount; i++) 
            {
                ApplicationInfo application = new ApplicationInfo();
                ResolveInfo resolveInfo = applications.get(i);

                application.title = resolveInfo.loadLabel(packageManager);
                application.setActivity(new ComponentName(
                		resolveInfo.activityInfo.applicationInfo.packageName,
                		resolveInfo.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.icon = resolveInfo.activityInfo.loadIcon(packageManager);

                String applicationPackage = resolveInfo.activityInfo.applicationInfo.packageName;
                application.packageName = applicationPackage;
                
                mApplications.add(application);
            }
        }
    }
	
	private void bindApplications()
    {
        if(mApplicationGrid == null) 
        {
        	mApplicationGrid = (GridView) findViewById(R.id.applicationGrid);
        }
        
        mApplicationGrid.setAdapter(new ApplicationsAdapter(this, mApplications));
        mApplicationGrid.setSelection(0);
        
        mApplicationGrid.setOnItemClickListener(new ApplicationLauncher());
    }
	
	private void updateHomescreenList()
	{
		int screenCountModifier = (mNumberOfHomescreens - mHomescreenArray.size());
		
		for(int i = 0; i < screenCountModifier; i++)
		{
			LinearLayout homescreen = new LinearLayout(this);
		    homescreen.setOrientation(LinearLayout.VERTICAL);
		    homescreen.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			mViewFlipper.addView(homescreen);
			
			mHomescreenArray.add(homescreen);
		}
		
		boolean removeAllUnchecked = false;
		
		for(int i = screenCountModifier; i < 0; i++)
		{
			ViewGroup homescreen = mHomescreenArray.get((mHomescreenArray.size() - 1));
			
			if(homescreen.getChildCount() == 0 || removeAllUnchecked == true)
			{
				mHomescreenArray.remove(homescreen);
			
				for(int n = 0; n < homescreen.getChildCount(); n++)
				{
					View appWidgetHostView = (View) homescreen.getChildAt(n);
					
					if(appWidgetHostView instanceof AppWidgetHostView)
					{
						removeWidget(homescreen, (AppWidgetHostView) appWidgetHostView);
					}
				}
				
				int childCount = mViewFlipper.getChildCount();
				mViewFlipper.removeViewAt((childCount - 1));
			}
			else 
			{
				String title, message, positiveButton, negativeButton, neutralButton;
				
				title = "You are about to remove one or more homescreens that aren't empty";
				message = "Would you like to remove them anyway?";
				positiveButton = "Yes, remove them all";
				negativeButton = "No, keep them";
				neutralButton = null;
				
				SmartDialog smartDialog = new SmartDialog(HomescreenActivity.this, title, message,
						positiveButton, negativeButton, neutralButton);
				
				String dialogResult = smartDialog.showDialog();
				
				if(dialogResult.equals(positiveButton))
				{
					removeAllUnchecked = true;
					
					i--;
				}
				else
				{
					break;
				}
			}
		}
		
		if(screenCountModifier != 0)
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
			boolean scrollingWallpaperEnabled = sharedPreferences.getBoolean("scrollingWallpaperEnabled", false);
			
			if(scrollingWallpaperEnabled == true)
			{
				resizeScrollingWallpaper(); 
			}
		}
	}
	
	private void updateDock()
	{
		HomescreenDock homescreenDock = new HomescreenDock(this);
		
		homescreenDock.updateMargins();
	    homescreenDock.updateListStatus();
	    homescreenDock.updateDockWidth();
	   
	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean scrollbarMatchDockWidth = sharedPreferences.getBoolean("scrollbarMatchDockWidth", false);
		
		if(scrollbarMatchDockWidth == true)
		{
			this.getHomescreenScrollbarInstance().setScrollbarWidth(homescreenDock.getDockWidth());
		}
		else if(scrollbarMatchDockWidth == false)
		{
			this.getHomescreenScrollbarInstance().resetScrollbarWidth();
		}
	}
	
	private void updateScrollbarVisibility()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean isHidden = sharedPreferences.getBoolean("hideHomescreenScrollbar", false);
	    
		HomescreenScrollbar homescreenScrollbar = (HomescreenScrollbar) findViewById(R.id.homescreenScrollbar);
	    homescreenScrollbar.setVisibility(isHidden);
	}
	
	private void updateWallpaper()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean scrollingWallpaperEnabled = sharedPreferences.getBoolean("scrollingWallpaperEnabled", false);
		
		if(mLauncherWallpaperManager != null && scrollingWallpaperEnabled != true)
		{
			setWallpaperDrawable(null);
		}
		else if(mLauncherWallpaperManager == null && scrollingWallpaperEnabled == true)
		{
			setScrollingWallpaper();
		}
	}
	
	private void updateService()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean dataCollectionEnabled = sharedPreferences.getBoolean("enableDataCollection", false);
		
		if(dataCollectionEnabled == true)
		{
			startService(new Intent(this, DataCollectionService.class));
		}
		else if(dataCollectionEnabled == false)
		{
			stopService(new Intent(this, DataCollectionService.class));
		}
	}
	
	private void resumeService()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean dataCollectionEnabled = sharedPreferences.getBoolean("enableDataCollection", false);
		
		if(dataCollectionEnabled == true)
		{
			startService(new Intent(this, DataCollectionService.class));
		}
	}
	
	private void stopService()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean dataCollectionEnabled = sharedPreferences.getBoolean("enableDataCollection", false);
		
		if(dataCollectionEnabled == true)
		{
			stopService(new Intent(this, DataCollectionService.class));
		}
	}
	
	private void updateOrientation()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean landscapeModeEnabled = sharedPreferences.getBoolean("enableLandscapeMode", false);
		
		if(landscapeModeEnabled == false)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		else if(landscapeModeEnabled == true)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
	}
	
	private void startPreferenceActivity()
	{
		Intent preferenceActivity = new Intent(HomescreenActivity.this, LauncherPreferenceActivity.class);
	    
		startActivity(preferenceActivity); 
	}
	
	private void startSettingsActivity()
	{
		Intent settingsActivity = new Intent(android.provider.Settings.ACTION_SETTINGS);
	    
		startActivity(settingsActivity); 
	}
	
	private void startWallpaperActivity()
	{
		Intent wallpaperActivity = new Intent(HomescreenActivity.this, WallpaperActivity.class);
	    
		startActivity(wallpaperActivity); 
	}
	
	public void toNextHomescreen()
	{
		if(mCurrentHomescreen == -1)
		{			
			mCurrentHomescreen = 1;
			updateScrollingWallpaper();
			
			mViewFlipper.setInAnimation(this, R.anim.right_to_left);
			mViewFlipper.setOutAnimation(this, R.anim.all_homescreens_out);

			mViewFlipper.setDisplayedChild(1);
			
			mViewFlipper.removeViewAt((mViewFlipper.getChildCount() - 1));
			
			updateHomescreenScrollbar();
		}
		else if(mCurrentHomescreen < mHomescreenArray.size() && mInterceptActionType.equals("NO_ACTION"))
		{
			if(mCurrentHomescreen == 0)
			{
				resetHomescreenScrollbar();
			}
			
			mCurrentHomescreen++;
			updateScrollingWallpaper();
			
			mViewFlipper.setInAnimation(this, R.anim.right_to_left);
			mViewFlipper.setOutAnimation(this, R.anim.right_to_left_out);
			mViewFlipper.showNext();
			
			updateHomescreenScrollbar();
		}
		else
		{
			Animation overscrollRightAnimation = AnimationUtils.loadAnimation(this, R.anim.overscroll_right);
			mHomescreenArray.get((mCurrentHomescreen - 1)).startAnimation(overscrollRightAnimation);
			
			return;
		}
	}
	
	public void toPreviousHomescreen()
	{
		if(mCurrentHomescreen == -1)
		{
			return;
		}
		else if(mCurrentHomescreen == 0)
		{
			Animation currentViewFlashAnimation = AnimationUtils.loadAnimation(this, R.anim.current_view_flash);
			mViewFlipper.getChildAt(0).startAnimation(currentViewFlashAnimation);
		}
		else if(mCurrentHomescreen > 1 && mInterceptActionType.equals("NO_ACTION"))
		{
			mCurrentHomescreen--;
			updateScrollingWallpaper();
			
			mViewFlipper.setInAnimation(this, R.anim.left_to_right);
			mViewFlipper.setOutAnimation(this, R.anim.left_to_right_out);
			mViewFlipper.showPrevious();
			
			updateHomescreenScrollbar();
		}
		else if(mCurrentHomescreen == 1)
		{
			Animation overscrollLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.overscroll_left);
			mHomescreenArray.get((mCurrentHomescreen - 1)).startAnimation(overscrollLeftAnimation);
			
			return;
		}
	}
	
	public void setHomescreen(int homescreenIndex)
	{		
		mCurrentHomescreen = homescreenIndex;
		updateScrollingWallpaper();
		
		mViewFlipper.setInAnimation(this, R.anim.right_to_left);
		mViewFlipper.setOutAnimation(this, R.anim.all_homescreens_out);
		
		mViewFlipper.setDisplayedChild(homescreenIndex);
		
		mViewFlipper.removeViewAt((mViewFlipper.getChildCount() - 1));
		
		updateHomescreenScrollbar();
	}
	
	public void setScrollingWallpaper()
	{
		Drawable wallpaper = peekWallpaper();
		
		if(wallpaper == null)
		{
			return;
		}
		
		mLauncherWallpaperManager = new LauncherWallpaperManager();
		
		mLauncherWallpaperManager.resizeWallpaperDrawable(this, wallpaper, mHomescreenArray.size(), 
				getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		updateScrollingWallpaper();
	}
	
	public void updateScrollingWallpaper()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		boolean scrollingWallpaperEnabled = sharedPreferences.getBoolean("scrollingWallpaperEnabled", false);
		
		if(scrollingWallpaperEnabled == false)
		{
			mLauncherWallpaperManager = null;
			
			return;
		}
		
		Drawable homescreenWallpaper = mLauncherWallpaperManager.getDrawableForLayout(this, mCurrentHomescreen,
				getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		
		LinearLayout rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
		rootLayout.setBackgroundDrawable(homescreenWallpaper);
	}
	
	public void resizeScrollingWallpaper()
	{
		if(mLauncherWallpaperManager == null)
		{
			return;
		}
		
		Drawable wallpaper = peekWallpaper();
		
		if(wallpaper == null)
		{
			return;
		}
		
		mLauncherWallpaperManager.resizeWallpaperDrawable(this, wallpaper, mHomescreenArray.size(), 
				getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		
		Drawable homescreenWallpaper = mLauncherWallpaperManager.getDrawableForLayout(this, mCurrentHomescreen,
				getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		
		LinearLayout rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
		rootLayout.setBackgroundDrawable(homescreenWallpaper);
	}
	
	public void onLongPress(boolean needHapticFeedback)
	{
		if(mCurrentHomescreen == 0 || mCurrentHomescreen == -1)
		{
			return;
		}
		else if(needHapticFeedback == true)
		{
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(25);
		}
		
		showDialog(1);
	}
	
	@Override
	protected Dialog onCreateDialog(int dialogId) 
	{
		switch(dialogId) 
		{
			case 1:
				final Dialog longClickDialog = new Dialog(this);
				longClickDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				longClickDialog.setCancelable(true);
				
				longClickDialog.setContentView(R.layout.homescreen_dialog);
				
				int displayWidth = getResources().getDisplayMetrics().widthPixels;
		    	int dialogWidth = (int) (displayWidth * 0.70);
		    	
		    	ViewGroup rootLayout = (ViewGroup) longClickDialog.findViewById(R.id.rootLayout);
		    	rootLayout.setMinimumWidth(dialogWidth);
		    	
				Button addWidgetButton = (Button) longClickDialog.findViewById(R.id.addWidgetButton);
				Button removeWidgetButton = (Button) longClickDialog.findViewById(R.id.removeWidgetButton);
				Button expandWidgetButton = (Button) longClickDialog.findViewById(R.id.expandWidgetButton);
				Button collapseWidgetButton = (Button) longClickDialog.findViewById(R.id.collapseWidgetButton);
				Button setWallpaperButton = (Button) longClickDialog.findViewById(R.id.setWallpaperButton);
				 
				addWidgetButton.setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View view) 
					{
						Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
						vibrator.vibrate(25);
						
						if(mCurrentHomescreen == 0)
						{
							toNextHomescreen();
						}
						
						selectWidget();
						
						longClickDialog.dismiss();
					}
				});
				
				removeWidgetButton.setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View view) 
					{
						Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
						vibrator.vibrate(25);
						
						if(mCurrentHomescreen == 0)
						{
							toNextHomescreen();
						}
						
						removeWidgetMenuSelected();
						
						longClickDialog.dismiss();
					}
				});
				
				expandWidgetButton.setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View view) 
					{
						Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
						vibrator.vibrate(25);
						
						if(mCurrentHomescreen == 0)
						{
							toNextHomescreen();
						}
						
						expandWidgetMenuSelected();
						
						longClickDialog.dismiss();
					}
				});
				
				collapseWidgetButton.setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View view) 
					{
						Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
						vibrator.vibrate(25);
						
						if(mCurrentHomescreen == 0)
						{
							toNextHomescreen();
						}
						
						collapseWidgetMenuSelected();
						
						longClickDialog.dismiss();
					}
				});
				
				setWallpaperButton.setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View view) 
					{
						Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
						vibrator.vibrate(25);
						
						startWallpaperActivity();
						
						longClickDialog.dismiss();
					}
				});
				
				longClickDialog.show();
		}
		
		return super.onCreateDialog(dialogId);
	}
	
	public void setWallpaperDrawable(Drawable wallpaper) 
	{
		LinearLayout rootLayout = (LinearLayout) this.findViewById(R.id.rootLayout);
		
		if(wallpaper == null)
		{
			wallpaper = peekWallpaper();
		}
		
		if(wallpaper != null)
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
			boolean scrollingWallpaperEnabled = sharedPreferences.getBoolean("scrollingWallpaperEnabled", false);
			
			if(scrollingWallpaperEnabled == false)
			{
				rootLayout.setBackgroundDrawable(new CroppedDrawable(wallpaper));
			}
			else if(scrollingWallpaperEnabled == true)
			{
				setScrollingWallpaper();
			}
		}
		else
		{
			SharedPreferences sharedPreferences = getSharedPreferences("com.puckowski.launcher4.wallpaper_preference", MODE_PRIVATE);
			boolean askToSetWallpaper = sharedPreferences.getBoolean("askToSetWallpaper", true);
			
			if(askToSetWallpaper == true)
			{
				String title, message, positiveButton, negativeButton, neutralButton;
			
				title = "No wallpaper detected";
				message = "Would you like to set a wallpaper now?";
				positiveButton = "Yes, select an image now";
				negativeButton = "No, later";
				neutralButton = "No (and don't ask again)";
			
				SmartDialog smartDialog = new SmartDialog(HomescreenActivity.this, title, message,
						positiveButton, negativeButton, neutralButton);
			
				String dialogResult = smartDialog.showDialog();
			
				if(dialogResult == null)
				{
					return;
				}
				
				if(dialogResult.equals(positiveButton))
				{
					startWallpaperActivity();
				}
				else if(dialogResult.equals(neutralButton))
				{
					Editor editor = sharedPreferences.edit();

					editor.putBoolean("askToSetWallpaper", false);
					editor.commit();
				}
			}
		}
    }
	
	public String getInterceptActionType()
	{
		return mInterceptActionType;
	}
	
	private void selectWidget() 
	{
		int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
		
		Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		
		addEmptyData(intent);
		
		startActivityForResult(intent, R.id.REQUEST_PICK_APPWIDGET);
	}
	
	private void addEmptyData(Intent intent)
	{
		ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
		intent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		
		ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
		intent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
	}
	
	private void readWidgetData() 
	{
		try
		{
			FileInputStream idInputStream = openFileInput(APPWIDGET_DATA_FILE);
			FileInputStream indexInputStream = openFileInput(HOMESCREEN_DATA_FILE);
			FileInputStream dimensionsInputStream = openFileInput(DIMENSIONS_DATA_FILE);
			
	        try
	        {
	            BufferedReader idReader = new BufferedReader(new InputStreamReader(idInputStream));
	            BufferedReader indexReader = new BufferedReader(new InputStreamReader(indexInputStream));
	            BufferedReader dimensionsReader = new BufferedReader(new InputStreamReader(dimensionsInputStream));
	            
	            String line = null;
	            
	            while((line = idReader.readLine()) != null) 
	            {
	                mAppWidgetIdArray.add(Integer.parseInt(line));
	            }
	            
	            while((line = indexReader.readLine()) != null) 
	            {
	                mAppWidgetIndexArray.add(Integer.parseInt(line));
	            }
	            
	            while((line = dimensionsReader.readLine()) != null) 
	            {
	                mAppWidgetDimensionsArray.add(line);
	            }
	             
	            idInputStream.close();
	            indexInputStream.close();
	            dimensionsInputStream.close();
	        } 
	        catch(OutOfMemoryError outOfMemoryError)
	        {
	        } 
	        catch(Exception exception)
	        {
	        }
		}
		catch(Exception exception)
		{
		}
	}
	
	private boolean reinstateSavedWidgets() 
	{		
		boolean widgetsAdded = false;
		AppWidgetHostView appWidgetHostView = null;
		
		for(int i = 0; i < mAppWidgetIdArray.size(); i++)
		{
			try
			{
				int appWidgetId = mAppWidgetIdArray.get(i);
				 
				appWidgetHostView = loadSavedWidget(appWidgetId);
			}
			catch(Exception exception)
			{ 
				appWidgetHostView = null;
			}
			
			if(appWidgetHostView != null)
			{
				int[] appWidgetDimensions = parseAppWidgetDimensions(mAppWidgetDimensionsArray.get(i));
				
				mHomescreenArray.get(mAppWidgetIndexArray.get(i)).addView(appWidgetHostView, appWidgetDimensions[0], appWidgetDimensions[1]);
								
				widgetsAdded = true;
			}
		}
		  
		return widgetsAdded;
	}
	
	private AppWidgetHostView loadSavedWidget(int appWidgetId) 
	{   
		int newAppWidgetId = mAppWidgetHost.allocateAppWidgetId();
		newAppWidgetId = appWidgetId;
		
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(newAppWidgetId);
				
		AppWidgetHostView appWidgetHostView = mAppWidgetHost.createView(this, newAppWidgetId, appWidgetInfo);
		appWidgetHostView.setAppWidget(newAppWidgetId, appWidgetInfo);
		
		return appWidgetHostView;
	}
	
	private void saveWidgetData()
	{
		File file = new File(this.getFilesDir(), APPWIDGET_DATA_FILE);
		file = new File(this.getFilesDir(), HOMESCREEN_DATA_FILE);
		file = new File(this.getFilesDir(), DIMENSIONS_DATA_FILE);

		try
		{
			FileOutputStream idOutputStream = openFileOutput(APPWIDGET_DATA_FILE, Context.MODE_PRIVATE);
			FileOutputStream indexOutputStream = openFileOutput(HOMESCREEN_DATA_FILE, Context.MODE_PRIVATE);
			FileOutputStream dimensionsOutputStream = openFileOutput(DIMENSIONS_DATA_FILE, Context.MODE_PRIVATE);
			
			BufferedWriter idWriter = new BufferedWriter(new OutputStreamWriter(idOutputStream));
			BufferedWriter indexWriter = new BufferedWriter(new OutputStreamWriter(indexOutputStream));
			BufferedWriter dimensionsWriter = new BufferedWriter(new OutputStreamWriter(dimensionsOutputStream));
			
			for(int i = 0; i < mAppWidgetIdArray.size(); i++)
			{
				idWriter.write(mAppWidgetIdArray.get(i) + "\n");
				indexWriter.write(mAppWidgetIndexArray.get(i) + "\n");
				dimensionsWriter.write(mAppWidgetDimensionsArray.get(i) + "\n");
			}
			
			idWriter.close();
			indexWriter.close();
			dimensionsWriter.close();
		}
		catch(Exception exception)
		{ 
		}  
	}
	
	private void removeWidget(ViewGroup homescreen, AppWidgetHostView appWidgetHostView)
	{		
		int widgetId = appWidgetHostView.getAppWidgetId();
		
		mAppWidgetHost.deleteAppWidgetId(widgetId);
		
		for(int i = 0; i < mAppWidgetIdArray.size(); i++)
		{
			if(mAppWidgetIdArray.get(i) == widgetId)
			{
				mAppWidgetIdArray.remove(i);
				mAppWidgetIndexArray.remove(i);
				mAppWidgetDimensionsArray.remove(i);
			}
		}
		
		homescreen.removeView(appWidgetHostView);
	}
	
	public void removeWidget(int downX, int downY)
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		int homescreenWidgetCount = homescreen.getChildCount();
		
		for(int i = 0; i < homescreenWidgetCount; i++)
		{
			Rect appWidgetDisplayRect = new Rect();
			int[] appWidgetLocation = new int[2];
			
			homescreen.getChildAt(i).getDrawingRect(appWidgetDisplayRect);
			homescreen.getChildAt(i).getLocationOnScreen(appWidgetLocation);
			
			appWidgetDisplayRect.offset(appWidgetLocation[0], appWidgetLocation[1]);

			if(appWidgetDisplayRect.contains(downX, downY))
			{
				View view = homescreen.getChildAt(i);
				
				if (view instanceof AppWidgetHostView) 
				{
					removeWidget(homescreen, (AppWidgetHostView) view);
					
					for(int j = 0; j < homescreen.getChildCount(); j++)
					{
						homescreen.getChildAt(j).setBackgroundColor(0x00000000);
					}
					
					mInterceptActionType = "NO_ACTION";
					
					return;
				}
			}
		}
	}
	
	private void removeWidgetMenuSelected() 
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		if(homescreen.getChildCount() == 0)
		{
			return;
		}
		
		if(mInterceptActionType.equals("NO_ACTION"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x80EE0000);
			}
		
			mInterceptActionType = "REMOVE_APPWIDGET";
		}
		else if(mInterceptActionType.equals("REMOVE_APPWIDGET"))
		{
			for(int j = 0; j < homescreen.getChildCount(); j++)
			{
				homescreen.getChildAt(j).setBackgroundColor(0x00000000);
			}
			
			mInterceptActionType = "NO_ACTION";
		}
		else if(mInterceptActionType.equals("EXPAND_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x80EE0000);
			}
		
			mInterceptActionType = "REMOVE_APPWIDGET";
		}
		else if(mInterceptActionType.equals("COLLAPSE_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x80EE0000);
			}
		
			mInterceptActionType = "REMOVE_APPWIDGET";
		}
	}
	
	private void expandWidget(ViewGroup homescreen, AppWidgetHostView appWidgetHostView, int viewIndex)
	{
        int homescreenWidth = getResources().getDisplayMetrics().widthPixels;
        
        int appWidgetViewWidth = appWidgetHostView.getWidth();
        int appWidgetViewHeight = appWidgetHostView.getHeight();

        int newWidgetWidth = homescreenWidth;
        int newWidgetHeight = (int) (((double) appWidgetViewHeight / (double) appWidgetViewWidth) * newWidgetWidth);

        if(checkHomescreenForSpace((newWidgetHeight - appWidgetViewHeight)))
        {
        	homescreen.removeView(appWidgetHostView);
        	
        	for(int i = 0; i < mAppWidgetIdArray.size(); i++)
        	{
        		if(mAppWidgetIdArray.get(i) == appWidgetHostView.getAppWidgetId())
        		{
        			mAppWidgetDimensionsArray.set(i, (newWidgetWidth + "," + newWidgetHeight));
        		}
        	}
        	
        	AppWidgetHostView[] homescreenChildren = new AppWidgetHostView[homescreen.getChildCount()];
        	
        	for(int i = 0; i < homescreen.getChildCount(); i++)
        	{
        		AppWidgetHostView appWidgetView = (AppWidgetHostView) homescreen.getChildAt(i);
        		
        		homescreenChildren[i] = appWidgetView;
        	}
        	
        	homescreen.removeAllViews();

        	if(viewIndex == 0)
        	{
        		homescreen.addView(appWidgetHostView, newWidgetWidth, newWidgetHeight);
        	}
        	
        	for(int n = 0; n < homescreenChildren.length; n++)
        	{
        		if((n + 1) == viewIndex)
        		{
        			homescreen.addView(homescreenChildren[n]);
        			
        			homescreen.addView(appWidgetHostView, newWidgetWidth, newWidgetHeight);
        		}
        		else
        		{
        			homescreen.addView(homescreenChildren[n]);
        		}
        	}
        }
        else
        {
        	String title, message, positiveButton, negativeButton, neutralButton;
			
			title = "There is not enough space in the current homescreen";
			message = "Would you like to expand the widget anyway?";
			positiveButton = "Yes, expand";
			negativeButton = "No, keep the same size";
			neutralButton = null;
			
			SmartDialog smartDialog = new SmartDialog(HomescreenActivity.this, title, message,
					positiveButton, negativeButton, neutralButton);
			
			String dialogResult = smartDialog.showDialog();

			if(dialogResult.equals(positiveButton))
			{
				homescreen.removeView(appWidgetHostView);
	        	
	        	for(int i = 0; i < mAppWidgetIdArray.size(); i++)
	        	{
	        		if(mAppWidgetIdArray.get(i) == appWidgetHostView.getAppWidgetId())
	        		{
	        			mAppWidgetDimensionsArray.set(i, (newWidgetWidth + "," + newWidgetHeight));
	        		}
	        	}
	        	
	        	homescreen.addView(appWidgetHostView, newWidgetWidth, newWidgetHeight);
			}
        }
	}
	
	public void expandWidget(int downX, int downY)
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		int homescreenWidgetCount = homescreen.getChildCount();
		
		for(int i = 0; i < homescreenWidgetCount; i++)
		{
			Rect appWidgetDisplayRect = new Rect();
			int[] appWidgetLocation = new int[2];
			
			homescreen.getChildAt(i).getDrawingRect(appWidgetDisplayRect);
			homescreen.getChildAt(i).getLocationOnScreen(appWidgetLocation);
			
			appWidgetDisplayRect.offset(appWidgetLocation[0], appWidgetLocation[1]);

			if(appWidgetDisplayRect.contains(downX, downY))
			{
				View view = homescreen.getChildAt(i);
				
				if (view instanceof AppWidgetHostView) 
				{
					expandWidget(homescreen, (AppWidgetHostView) view, i);
					
					for(int j = 0; j < homescreen.getChildCount(); j++)
					{
						homescreen.getChildAt(j).setBackgroundColor(0x00000000);
					}
					
					mInterceptActionType = "NO_ACTION";
					
					return;
				}
			}
		}
	}
	
	private void expandWidgetMenuSelected()
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		if(homescreen.getChildCount() == 0)
		{
			return;
		}
		
		if(mInterceptActionType.equals("NO_ACTION"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000BFFF);
			}
		
			mInterceptActionType = "EXPAND_APPWIDGET";
		}
		else if(mInterceptActionType.equals("EXPAND_APPWIDGET"))
		{
			for(int j = 0; j < homescreen.getChildCount(); j++)
			{
				homescreen.getChildAt(j).setBackgroundColor(0x00000000);
			}
			
			mInterceptActionType = "NO_ACTION";
		}
		else if(mInterceptActionType.equals("REMOVE_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000BFFF);
			}
		
			mInterceptActionType = "EXPAND_APPWIDGET";
		}
		else if(mInterceptActionType.equals("COLLAPSE_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000BFFF);
			}
		
			mInterceptActionType = "EXPAND_APPWIDGET";
		}
	}
	
	private void collapseWidget(ViewGroup homescreen, AppWidgetHostView appWidgetHostView, int viewIndex)
	{
		int[] appWidgetDimensions = getAppWidgetDimensions(appWidgetHostView.getAppWidgetInfo().minWidth, 
				appWidgetHostView.getAppWidgetInfo().minHeight);
        
		int newWidgetWidth = appWidgetDimensions[0];
        int newWidgetHeight = appWidgetDimensions[1];

        homescreen.removeView(appWidgetHostView);
        	
        for(int i = 0; i < mAppWidgetIdArray.size(); i++)
        {
        	if(mAppWidgetIdArray.get(i) == appWidgetHostView.getAppWidgetId())
        	{
        		mAppWidgetDimensionsArray.set(i, (newWidgetWidth + "," + newWidgetHeight));
        	}
        }
        	
        AppWidgetHostView[] homescreenChildren = new AppWidgetHostView[homescreen.getChildCount()];
        	
        for(int i = 0; i < homescreen.getChildCount(); i++)
        {
        	AppWidgetHostView appWidgetView = (AppWidgetHostView) homescreen.getChildAt(i);
        		
        	homescreenChildren[i] = appWidgetView;
        }
        	
        homescreen.removeAllViews();

        if(viewIndex == 0)
    	{
    		homescreen.addView(appWidgetHostView, newWidgetWidth, newWidgetHeight);
    	}
        
        for(int n = 0; n < homescreenChildren.length; n++)
        {
        	if((n + 1) == viewIndex)
        	{
        		homescreen.addView(homescreenChildren[n]);
        			
        		homescreen.addView(appWidgetHostView, newWidgetWidth, newWidgetHeight);
        	}
        	else
        	{
        		homescreen.addView(homescreenChildren[n]);
        	}
        }
	}
	
	public void collapseWidget(int downX, int downY)
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		int homescreenWidgetCount = homescreen.getChildCount();
		
		for(int i = 0; i < homescreenWidgetCount; i++)
		{
			Rect appWidgetDisplayRect = new Rect();
			int[] appWidgetLocation = new int[2];
			
			homescreen.getChildAt(i).getDrawingRect(appWidgetDisplayRect);
			homescreen.getChildAt(i).getLocationOnScreen(appWidgetLocation);
			
			appWidgetDisplayRect.offset(appWidgetLocation[0], appWidgetLocation[1]);

			if(appWidgetDisplayRect.contains(downX, downY))
			{
				View view = homescreen.getChildAt(i);
				
				if (view instanceof AppWidgetHostView) 
				{
					collapseWidget(homescreen, (AppWidgetHostView) view, i);
					
					for(int j = 0; j < homescreen.getChildCount(); j++)
					{
						homescreen.getChildAt(j).setBackgroundColor(0x00000000);
					}
					
					mInterceptActionType = "NO_ACTION";
					
					return;
				}
			}
		}
	}
	
	private void collapseWidgetMenuSelected()
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		if(homescreen.getChildCount() == 0)
		{
			return;
		}
		
		if(mInterceptActionType.equals("NO_ACTION"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000688B);
			}
		
			mInterceptActionType = "COLLAPSE_APPWIDGET";
		}
		else if(mInterceptActionType.equals("COLLAPSE_APPWIDGET"))
		{
			for(int j = 0; j < homescreen.getChildCount(); j++)
			{
				homescreen.getChildAt(j).setBackgroundColor(0x00000000);
			}
			
			mInterceptActionType = "NO_ACTION";
		}
		else if(mInterceptActionType.equals("REMOVE_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000688B);
			}
		
			mInterceptActionType = "COLLAPSE_APPWIDGET";
		}
		else if(mInterceptActionType.equals("EXPAND_APPWIDGET"))
		{
			for(int i = 0; i < homescreen.getChildCount(); i++)
			{
				homescreen.getChildAt(i).setBackgroundColor(0x8000688B);
			}
		
			mInterceptActionType = "COLLAPSE_APPWIDGET";
		}
	}
	
	private void createWidget(Intent intent)
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		
		Bundle extras = intent.getExtras();
		
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		
		int[] appWidgetDimensions = getAppWidgetDimensions(appWidgetInfo.minWidth, appWidgetInfo.minHeight);
		
		AppWidgetHostView appWidgetHostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
		appWidgetHostView.setAppWidget(appWidgetId, appWidgetInfo);
			
		if(checkHomescreenForSpace(appWidgetDimensions[1]))
		{
			mAppWidgetIdArray.add(appWidgetId);
			mAppWidgetIndexArray.add((mCurrentHomescreen - 1));
			mAppWidgetDimensionsArray.add(appWidgetDimensions[0] + "," + appWidgetDimensions[1]);
	
			homescreen.addView(appWidgetHostView, appWidgetDimensions[0], appWidgetDimensions[1]);
		}
		else
		{
			String title, message, positiveButton, negativeButton, neutralButton;
			
			title = "There is not enough space in the current homescreen";
			message = "Would you like to add it anyway?";
			positiveButton = "Yes, go ahead";
			negativeButton = "No, don't add";
			neutralButton = null;
			
			int alternativeHomescreen = findHomescreenWithSpace(appWidgetDimensions[1]);
			
			if(alternativeHomescreen != -1)
			{
				neutralButton = "Or, add to homescreen number: " + String.valueOf((alternativeHomescreen + 1));
			}
			
			SmartDialog smartDialog = new SmartDialog(HomescreenActivity.this, title, message,
					positiveButton, negativeButton, neutralButton);
			
			String dialogResult = smartDialog.showDialog();

			if(dialogResult.equals(positiveButton))
			{
				mAppWidgetIdArray.add(appWidgetId);
				mAppWidgetIndexArray.add((mCurrentHomescreen - 1));
				mAppWidgetDimensionsArray.add(appWidgetDimensions[0] + "," + appWidgetDimensions[1]);
		      
				homescreen.addView(appWidgetHostView, appWidgetDimensions[0], appWidgetDimensions[1]);
			}
			else if(dialogResult.equals(neutralButton))
			{
				mAppWidgetIdArray.add(appWidgetId);
				mAppWidgetIndexArray.add(alternativeHomescreen);
				mAppWidgetDimensionsArray.add(appWidgetDimensions[0] + "," + appWidgetDimensions[1]);
		      
				mHomescreenArray.get(alternativeHomescreen).addView(appWidgetHostView, 
						appWidgetDimensions[0], appWidgetDimensions[1]);
				
				mViewFlipper.setDisplayedChild((alternativeHomescreen + 1));
				
				mCurrentHomescreen = (alternativeHomescreen + 1);
				
				updateHomescreenScrollbar();
			}
		}
	}
	
	private void configureWidget(Intent intent) 
	{
		Bundle extras = intent.getExtras();
		
		int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
		
		if (appWidgetInfo.configure != null)
		{
			intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			
			intent.setComponent(appWidgetInfo.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			
			startActivityForResult(intent, R.id.REQUEST_CREATE_APPWIDGET);
		} 
		else
		{
			createWidget(intent);
		}
	}
	
	private int[] parseAppWidgetDimensions(String appWidgetDimensions)
	{
		String width = appWidgetDimensions.substring(0, appWidgetDimensions.indexOf(","));
		String height = appWidgetDimensions.substring((appWidgetDimensions.indexOf(",") + 1), appWidgetDimensions.length());
		
		return new int[] { Integer.parseInt(width), Integer.parseInt(height) };
	}
	
	private int[] getAppWidgetDimensions(int width, int height) 
	{
		int appWidgetRows = (int) Math.ceil(((height + getPixelsFromDp(30)) / getPixelsFromDp(70)));
		int appWidgetColumns = (int) Math.ceil(((width + getPixelsFromDp(30)) / getPixelsFromDp(70)));

		//double appWidgetRows = ((height + getPixelsFromDp(30)) / getPixelsFromDp(70));
		//double appWidgetColumns = ((width + getPixelsFromDp(30)) / getPixelsFromDp(70));
		
		//Toast.makeText(this, String.valueOf(height) + "Rows: " + String.valueOf(((height + getPixelsFromDp(30)) / getPixelsFromDp(70))), Toast.LENGTH_LONG).show();
		//Toast.makeText(this, String.valueOf(width) + "Cols: " + String.valueOf(((width + getPixelsFromDp(30)) / getPixelsFromDp(70))), Toast.LENGTH_LONG).show();
		
		if(appWidgetRows < 1)
		{
			appWidgetRows = 1;
		}
		if(appWidgetColumns < 1)
		{
			appWidgetColumns = 1;
		}
		
		double homescreenHeight = getHomescreenHeight();
		double homescreenWidth = mHomescreenArray.get((mCurrentHomescreen - 1)).getWidth();
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);
		
		int appWidgetCellWidth = ((int) homescreenWidth / Integer.parseInt(String.valueOf(sharedPreferences.getString("homescreenColumns", "5"))));
		int appWidgetCellHeight = ((int) homescreenWidth / Integer.parseInt(String.valueOf(sharedPreferences.getString("homescreenRows", "5")))); //((int) homescreenHeight / Integer.parseInt(String.valueOf(sharedPreferences.getString("homescreenRows", "5"))));
	    	    
		int scaledAppWidgetHeight = (int) (appWidgetRows * appWidgetCellHeight);
		int scaledAppWidgetWidth = (int) (appWidgetColumns * appWidgetCellWidth);
		
		if(scaledAppWidgetWidth > homescreenWidth)
		{		
			scaledAppWidgetWidth = (int) homescreenWidth;
			
			//double heightResizeScale = (homescreenWidth / (double) scaledAppWidgetWidth);
			//scaledAppWidgetHeight = (int) (heightResizeScale * scaledAppWidgetHeight);
		}
						
		width = scaledAppWidgetWidth;
		height = scaledAppWidgetHeight;
		
        return new int[] { width, height };
    }
	
	private float getPixelsFromDp(float dp)
	{
	    return dp * getResources().getDisplayMetrics().density;
	}
	
	private void measureOffscreenLayouts()
	{
		Window contentWindow = this.getWindow();
		
		Rect statusBarRect = new Rect();
		contentWindow.getDecorView().getWindowVisibleDisplayFrame(statusBarRect);
		
		int homescreenHeight = getHomescreenHeight();
        int homescreenWidth = getResources().getDisplayMetrics().widthPixels;
        
		for(int i = 0; i < mHomescreenArray.size(); i++)
		{
			ViewGroup homescreen = mHomescreenArray.get(i);
		
			int measuredWidth = View.MeasureSpec.makeMeasureSpec(homescreenWidth, View.MeasureSpec.EXACTLY);
			int measuredHeight = View.MeasureSpec.makeMeasureSpec(homescreenHeight, View.MeasureSpec.EXACTLY);
			
			homescreen.measure(measuredWidth, measuredHeight);
			homescreen.layout(0, 0, homescreen.getMeasuredWidth(), homescreen.getMeasuredHeight());
		}
	}
	
	private boolean checkHomescreenForSpace(int appWidgetHeight)
	{
		ViewGroup homescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
		int usedSpace = 0;
		
		for(int i = 0; i < homescreen.getChildCount(); i++)
		{
			usedSpace = (usedSpace + homescreen.getChildAt(i).getHeight());
		}
		
		int homescreenHeight; 
				
		homescreenHeight = getHomescreenHeight(); 
		int spaceRemaining = (homescreenHeight - usedSpace);
				
		if(spaceRemaining >= appWidgetHeight)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private boolean checkHomescreenForSpace(int appWidgetHeight, int homescreenIndex)
	{		
		ViewGroup homescreen = mHomescreenArray.get(homescreenIndex);
		int usedSpace = 0;
		
		for(int i = 0; i < homescreen.getChildCount(); i++)
		{
			usedSpace = (usedSpace + homescreen.getChildAt(i).getHeight());
		}
		
		int homescreenHeight = getHomescreenHeight(); 
		
		int spaceRemaining = (homescreenHeight - usedSpace);
				
		if(spaceRemaining >= appWidgetHeight)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private int findHomescreenWithSpace(int appWidgetHeight)
	{
		int homescreenIndex = -1;
				
		for(int i = 0; i < mHomescreenArray.size(); i++)
		{			
			if(checkHomescreenForSpace(appWidgetHeight, i))
			{
				homescreenIndex = i;
				
				break;
			}
		}
		
		return homescreenIndex;
	}
	
	private void flashCurrentViewGroup()
	{
		if(mCurrentHomescreen == -1)
		{
			View currentView = mViewFlipper.getChildAt((mViewFlipper.getChildCount() - 1));
			
			Animation currentViewFlashAnimation = AnimationUtils.loadAnimation(this, R.anim.current_view_flash);
			currentView.startAnimation(currentViewFlashAnimation);
		}
		else
		{
			ViewGroup currentHomescreen = mHomescreenArray.get((mCurrentHomescreen - 1));
			
			Animation currentViewFlashAnimation = AnimationUtils.loadAnimation(this, R.anim.current_view_flash);
			currentHomescreen.startAnimation(currentViewFlashAnimation);
		}
	}
	
	public int getHomescreenHeight()
	{
		Window contentWindow = this.getWindow();
		
		Rect statusBarRect = new Rect();
		contentWindow.getDecorView().getWindowVisibleDisplayFrame(statusBarRect);
		
		int statusBarHeight = statusBarRect.top;
		
		int homescreenHeight = (int) (getResources().getDisplayMetrics().heightPixels - (statusBarHeight + getPixelsFromDp(72)));
		homescreenHeight = (homescreenHeight - getHomescreenScrollbarInstance().getScrollbarHeight());
		
		return homescreenHeight;
	}
	
	public int[] getHomescreenDimensions()
	{
		int homescreenDimensions[] = new int[2];
		
		homescreenDimensions[0] = getResources().getDisplayMetrics().widthPixels;
		homescreenDimensions[1] = getHomescreenHeight();
		
		return homescreenDimensions;
	}
	
	public HomescreenScrollbar getHomescreenScrollbarInstance()
	{
		return (HomescreenScrollbar) findViewById(R.id.homescreenScrollbar);
	}
	
	public void hideHomescreenScrollbar(boolean isDelayed)
	{
		HomescreenScrollbar homescreenScrollbar = (HomescreenScrollbar) findViewById(R.id.homescreenScrollbar);
		homescreenScrollbar.hide(isDelayed);
	}
	
	public void updateHomescreenScrollbar()
	{
		HomescreenScrollbar homescreenScrollbar = (HomescreenScrollbar) findViewById(R.id.homescreenScrollbar);
		homescreenScrollbar.repaint(mCurrentHomescreen, mHomescreenArray.size());
	}
	
	public void resetHomescreenScrollbar()
	{
		HomescreenScrollbar homescreenScrollbar = (HomescreenScrollbar) findViewById(R.id.homescreenScrollbar);
		homescreenScrollbar.reset();
	}
	
	public ArrayList<ApplicationInfo> getApplicationList()
	{
		return mApplications;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if(resultCode == RESULT_OK)
		{
			if(requestCode == R.id.REQUEST_PICK_APPWIDGET)
			{
				configureWidget(intent);
			} 
			else if(requestCode == R.id.REQUEST_CREATE_APPWIDGET)
			{
				createWidget(intent);
			}
		} 
		else if(resultCode == RESULT_CANCELED && intent != null) 
		{
			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			
			if(appWidgetId != -1)
			{
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}
	
	private class ApplicationsIntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            loadApplications(false);
            bindApplications();
        }
    }

    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo>
    {
        private Rect mIconBounds = new Rect();

        public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps)
        {
            super(context, 0, apps);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
        	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HomescreenActivity.this);	
    		boolean hideApplicationName = sharedPreferences.getBoolean("hideApplicationName", false);
    		
            final ApplicationInfo info = mApplications.get(position);
            
            if(convertView == null)
            {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_list_icon, parent, false);
            }

            final TextView textView = (TextView) convertView.findViewById(R.id.appIconView);  
            Drawable icon = info.icon;
            
            if(!info.filtered)
            {
                int width = (int) getPixelsFromDp(72); 
                
                if(hideApplicationName == false)
                {
                	width = (int) (width - (textView.getTextSize() * 2));
                }
                
                int height = width;

                final int iconWidth = icon.getIntrinsicWidth();
                final int iconHeight = icon.getIntrinsicHeight();

                final float ratio = (float) iconWidth / iconHeight;

                if(iconWidth > iconHeight)
                {
                    height = (int) (width / ratio);
                }
                else if(iconHeight > iconWidth)
                {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config bitmapConfig = (icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
                
                final Bitmap iconBitmap = Bitmap.createBitmap(width, height, bitmapConfig);
                final Canvas canvas = new Canvas(iconBitmap);

                canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, 0));

                mIconBounds.set(icon.getBounds());

                icon.setBounds(0, 0, width, height);
                icon.draw(canvas);
                icon.setBounds(mIconBounds);
                
                icon = info.icon = new BitmapDrawable(HomescreenActivity.this.getResources(), iconBitmap);
                info.filtered = true;
            }
            
            textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
    		
    		if(hideApplicationName == false)
    		{
    			textView.setText(info.title);
    		}

            return convertView;
        }
    }

    private class ApplicationLauncher implements AdapterView.OnItemClickListener
    {
        public void onItemClick(AdapterView adapterView, View view, int position, long id)
        {
            ApplicationInfo applicationInfo = (ApplicationInfo) adapterView.getItemAtPosition(position);
            
            startActivity(applicationInfo.intent);
        }
    }
    
    private class ApplicationButtonListener implements View.OnClickListener
	{
		@Override
		public void onClick(View view)
		{
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(25);
			
			if(!mInterceptActionType.equals("NO_ACTION"))
			{
				flashCurrentViewGroup();
			}
			else if(mCurrentHomescreen > 0)
			{
				mViewFlipper.setInAnimation(HomescreenActivity.this, R.anim.app_list_in);
				mViewFlipper.setOutAnimation(HomescreenActivity.this, R.anim.app_list_out);
				
				mViewFlipper.setDisplayedChild(0);
				
				hideHomescreenScrollbar(true);
			
				mCurrentHomescreen = 0;
			}
			else if(mCurrentHomescreen == -1)
			{
				mViewFlipper.setInAnimation(HomescreenActivity.this, R.anim.app_list_in);
				mViewFlipper.setOutAnimation(HomescreenActivity.this, R.anim.app_list_out);
				
				mViewFlipper.setDisplayedChild(0);
			
				hideHomescreenScrollbar(true);
				
				mCurrentHomescreen = 0;
			}
			else
			{			
				resetHomescreenScrollbar();
				
				mViewFlipper.setDisplayedChild(1);
				
				mCurrentHomescreen = 1;
				
				updateHomescreenScrollbar();
			}
		}	
	}
    
    private class ApplicationButtonLongClickListener implements View.OnLongClickListener
	{
		@Override
		public boolean onLongClick(View view)
		{
			if(mCurrentHomescreen == -1 || !mInterceptActionType.equals("NO_ACTION"))
			{
				flashCurrentViewGroup();
    			
				return true;
			}
			
			if(mCurrentHomescreen == 0)
			{
				resetHomescreenScrollbar();
			}
			
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(25);
			
			mAllHomescreensView = new AllHomescreensView(HomescreenActivity.this);
			mAllHomescreensView.populateView(mHomescreenArray);
			
			mViewFlipper.addView(mAllHomescreensView);
			
			mViewFlipper.setInAnimation(HomescreenActivity.this, R.anim.all_homescreens_in);
			mViewFlipper.setOutAnimation(HomescreenActivity.this, R.anim.all_homescreens_fade_out);
			
			mViewFlipper.setDisplayedChild((mViewFlipper.getChildCount() - 1));
			
			mCurrentHomescreen = -1;
			
			return true;
		}
	}
    
    private class WallpaperIntentReceiver extends BroadcastReceiver 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	setWallpaperDrawable(getWallpaper());
        }
    }
}
