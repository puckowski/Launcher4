package com.puckowski.launcher4;

import java.util.ArrayList;
import java.util.List;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.Service;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class HomescreenDock 
{
	private final int APPLICATION_BUTTON_SIZE = 72;
	private final int APPLICATION_BUTTON_PADDING = 8;
	
	private HomescreenActivity mHomescreenActivity;
		
	public HomescreenDock(HomescreenActivity homescreenActivity)
	{
		mHomescreenActivity = homescreenActivity; 
	}
	
	private boolean checkActivity()
	{
		if(mHomescreenActivity == null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private float getPixelsFromDp(float dp)
	{
	    return (dp * mHomescreenActivity.getResources().getDisplayMetrics().density);
	}
	
	public int getDockWidth()
	{		
		int dockWidth = mHomescreenActivity.getResources().getDisplayMetrics().widthPixels;
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		int dockMargin = (int) Integer.parseInt(String.valueOf(sharedPreferences.getString("dockMargins", "0")));
		
		dockWidth = (int) (dockWidth - (getPixelsFromDp(dockMargin) * 2));
			
		return dockWidth;
	}
	
	public void updateDockWidth()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		boolean isDockWidthFixed = sharedPreferences.getBoolean("fixedDockWidth", false);
		
		LinearLayout dockLayoutWrapper = (LinearLayout) mHomescreenActivity.findViewById(R.id.dockLayoutWrapper);
		LinearLayout dockLayout = (LinearLayout) mHomescreenActivity.findViewById(R.id.dockLayout);
		
		if(isDockWidthFixed == true)
		{
			dockLayoutWrapper.setBackgroundResource(R.color.color_transparent);
			
			for(int i = 0; i < dockLayout.getChildCount(); i++)
			{
				dockLayout.getChildAt(i).setBackgroundResource(R.drawable.custom_view_fully_transparent_selector);
			}
			
			dockLayoutWrapper.setGravity(Gravity.LEFT);
		}
		else if(isDockWidthFixed == false)
		{
			dockLayoutWrapper.setBackgroundResource(R.color.color_fully_transparent);
			
			for(int i = 0; i < dockLayout.getChildCount(); i++)
			{
				dockLayout.getChildAt(i).setBackgroundResource(R.drawable.custom_view_transparent_selector);
			}
			
			dockLayoutWrapper.setGravity(Gravity.CENTER);
		}
	}
	
	public void updateListStatus()
	{
		if(checkActivity())
		{
			return;
		}
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		
		boolean disableRecentApps = sharedPreferences.getBoolean("disableRecentApps", false);

		if(disableRecentApps == true)
		{			
			LinearLayout dockLayout = (LinearLayout) mHomescreenActivity.findViewById(R.id.dockLayout);
			dockLayout.removeAllViews();
		}
	}
	
	public void updateList()
	{
		if(checkActivity())
		{
			return;
		}
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		boolean disableRecentApps = sharedPreferences.getBoolean("disableRecentApps", false);
		
		if(disableRecentApps == true)
		{
			return;
		}
		
		LinearLayout dockLayout = (LinearLayout) mHomescreenActivity.findViewById(R.id.dockLayout);
		dockLayout.removeAllViews();
		
		int numberOfRecentApps = (int) Integer.parseInt(String.valueOf(sharedPreferences.getString("numberOfRecentApps", "8")));
		boolean recentOrRunningTasks = sharedPreferences.getBoolean("recentOrRunningTasks", true);
		
		ActivityManager activityManager = (ActivityManager) mHomescreenActivity.getSystemService(Context.ACTIVITY_SERVICE);
		
		List<RunningTaskInfo> runningTaskInfoList = null;
		List<RecentTaskInfo> recentTaskInfoList = null;
		
		if(recentOrRunningTasks == true)
		{
			recentTaskInfoList = activityManager.getRecentTasks(numberOfRecentApps, ActivityManager.RECENT_IGNORE_UNAVAILABLE); //RECENT_WITH_EXCLUDED);
		
			buildRecentAppsDock(dockLayout, recentTaskInfoList);
		}
		else if(recentOrRunningTasks == false)
		{
			runningTaskInfoList = activityManager.getRunningTasks(numberOfRecentApps);
			
			buildRunningAppsDock(dockLayout, runningTaskInfoList);
		}
		
		final HorizontalScrollView dockScrollView = (HorizontalScrollView) mHomescreenActivity.findViewById(R.id.dockScrollView);
		
		dockScrollView.postDelayed(new Runnable() 
		{
		    public void run() 
		    {
		    	dockScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
		    }
		}, 0L);
	}
	
	private void buildRecentAppsDock(LinearLayout dockLayout, List<RecentTaskInfo> recentTaskInfoList)
	{
		RecentTaskInfo recentTaskInfo;
		
		for(int i = (recentTaskInfoList.size() - 1); i > 0; i--)
		{
			recentTaskInfo = recentTaskInfoList.get(i);
			String packageName = null;
			
			Intent intent = new Intent(recentTaskInfo.baseIntent);
			
            if(recentTaskInfo.origActivity != null) 
            {
                intent.setComponent(recentTaskInfo.origActivity);
            }

            final PackageManager packageManager = mHomescreenActivity.getPackageManager();
            final ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
            
            if(resolveInfo == null)
            {
            	continue;
            }
           
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            
            packageName = activityInfo.packageName;
            
			if(packageName == null || packageName.contains("puckowski.launcher4"))
			{
				continue;
			}
			
			ArrayList<ApplicationInfo> mApplications = mHomescreenActivity.getApplicationList();
			
			for(int n = 0; n < mApplications.size(); n++)
			{
				if(mApplications.get(n).packageName.equals(packageName))
				{
					ImageButton iconButton;
					
					ApplicationInfo applicationInfo = mApplications.get(n);
					
					iconButton = buildApplicationButton(applicationInfo, applicationInfo.icon);
					dockLayout.addView(iconButton, 0);
				}
			}
		}
	}
	
	private void buildRunningAppsDock(LinearLayout dockLayout, List<RunningTaskInfo> runningTaskInfoList)
	{
		RunningTaskInfo runningTaskInfo;
		
		for(int i = runningTaskInfoList.size() - 1; i > 0; i--)
		{
			runningTaskInfo = runningTaskInfoList.get(i);
			String packageName = runningTaskInfo.baseActivity.getPackageName();
			
			if(packageName.contains("puckowski.launcher4"))
			{
				continue;
			}
			
			ArrayList<ApplicationInfo> mApplications = mHomescreenActivity.getApplicationList();
			
			for(int n = 0; n < mApplications.size(); n++)
			{
				if(mApplications.get(n).packageName.equals(packageName))
				{
					ImageButton iconButton;
					
					ApplicationInfo applicationInfo = mApplications.get(n);
					
					iconButton = buildApplicationButton(applicationInfo, applicationInfo.icon);
					dockLayout.addView(iconButton, 0);
				}
			}
		}
	}
	
	private ImageButton buildApplicationButton(final ApplicationInfo applicationInfo, Drawable icon)
	{
		icon.setBounds(0, 0, (int) getPixelsFromDp(APPLICATION_BUTTON_SIZE), (int) getPixelsFromDp(APPLICATION_BUTTON_SIZE));
		
		ImageButton iconButton = new ImageButton(mHomescreenActivity);
		
		iconButton.setMinimumWidth((int) getPixelsFromDp(APPLICATION_BUTTON_SIZE));
		iconButton.setMinimumHeight((int) getPixelsFromDp(APPLICATION_BUTTON_SIZE));
		
		iconButton.setPadding((int) getPixelsFromDp(APPLICATION_BUTTON_PADDING), (int) getPixelsFromDp(APPLICATION_BUTTON_PADDING), 
				(int) getPixelsFromDp(APPLICATION_BUTTON_PADDING), (int) getPixelsFromDp(APPLICATION_BUTTON_PADDING));
		
		iconButton.setImageDrawable(icon); 
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		boolean isDockWidthFixed = sharedPreferences.getBoolean("fixedDockWidth", false);
		
		if(isDockWidthFixed == true)
		{
			iconButton.setBackgroundResource(R.drawable.custom_view_fully_transparent_selector);
		}
		else
		{
			iconButton.setBackgroundResource(R.drawable.custom_view_transparent_selector);
		}
		
		iconButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View view) 
			{
				Vibrator vibrator = (Vibrator) mHomescreenActivity.getSystemService(Service.VIBRATOR_SERVICE);
				vibrator.vibrate(25);
				
				Intent activityIntent = applicationInfo.intent;
				mHomescreenActivity.startActivity(activityIntent);
			}
		});
		
		return iconButton;
	}
	
	public void updateMargins()
	{
		if(checkActivity())
		{
			return;
		}
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mHomescreenActivity);
		int dockMargin = (int) Integer.parseInt(String.valueOf(sharedPreferences.getString("dockMargins", "0")));

		LinearLayout dockLayoutWrapper = (LinearLayout) mHomescreenActivity.findViewById(R.id.dockLayoutWrapper);
		LinearLayout.LayoutParams dockLayoutParams = (LinearLayout.LayoutParams) dockLayoutWrapper.getLayoutParams();

		dockLayoutParams.setMargins((int) getPixelsFromDp(dockMargin), 0, (int) getPixelsFromDp(dockMargin), 0);
	}
}
