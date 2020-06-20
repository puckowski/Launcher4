package com.puckowski.launcher4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

public class LauncherPreferenceActivity extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener 
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		this.addPreferencesFromResource(R.xml.homescreen_preferences);
		
		Preference preference = findPreference("numberOfHomescreens");
		preference.setOnPreferenceChangeListener(mNumberInputListener);
		
		preference = findPreference("launcherInfo");
		preference.setOnPreferenceClickListener(mLauncherInfoListener);
		
		preference = findPreference("launcherSupport");
		preference.setOnPreferenceClickListener(mLauncherSupportListener);
		
		preference = findPreference("launcherReport");
		preference.setOnPreferenceClickListener(mLauncherReportListener);
		
		preference = findPreference("launcherMemory");
		preference.setOnPreferenceClickListener(mLauncherHeapListener);
		
		preference = findPreference("recentOrRunningTasks");
		preference.setOnPreferenceClickListener(mRecentOrRunningListener);
	}

	Preference.OnPreferenceClickListener mLauncherInfoListener = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference) 
		{
			Intent intent = new Intent(LauncherPreferenceActivity.this, LauncherInfoActivity.class);
			startActivity(intent);
			
			return false;
		}	
	};
	
	Preference.OnPreferenceClickListener mLauncherSupportListener = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference) 
		{
			Intent intent = new Intent(LauncherPreferenceActivity.this, UserSupportActivity.class);
			startActivity(intent);
			
			return false;
		}	
	};
	
	Preference.OnPreferenceClickListener mLauncherReportListener = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference) 
		{
			Intent intent = new Intent(LauncherPreferenceActivity.this, BugReportActivity.class);
			startActivity(intent);
			
			return false;
		}	
	};
	
	Preference.OnPreferenceClickListener mLauncherHeapListener = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference) 
		{
			Intent intent = new Intent(LauncherPreferenceActivity.this, MemoryUsageActivity.class);
			startActivity(intent);
			
			return false;
		}	
	};
	
	Preference.OnPreferenceChangeListener mNumberInputListener = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object object)
		{
			if(object != null && object.toString().length() > 0 && object.toString().matches("\\d*"))
			{
				if(Integer.parseInt(object.toString()) < 1)
				{
					Toast.makeText(LauncherPreferenceActivity.this, "Invalid input, must be at least 1.", Toast.LENGTH_SHORT).show();
					
					return false;
				}
				else
				{
					return true;
				}
			}

			Toast.makeText(LauncherPreferenceActivity.this, "Invalid input, numbers only.", Toast.LENGTH_SHORT).show();
			
			return false;
		}
	};
	
	Preference.OnPreferenceClickListener mRecentOrRunningListener = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference) 
		{
			int deviceApiCheck = Integer.valueOf(android.os.Build.VERSION.SDK);
			
			if(deviceApiCheck >= 11)
			{
				return true;
			}
			
			SharedPreferences sharedPreferences = LauncherPreferenceActivity.this.getSharedPreferences("com.puckowski.launcher4.homescreen_preferences", MODE_PRIVATE);
			Editor editor = sharedPreferences.edit(); 
			
			editor.putBoolean("recentOrRunningTasks", false);
			editor.commit();
			
			Toast.makeText(LauncherPreferenceActivity.this, "Your device does not support this feature.", Toast.LENGTH_SHORT).show();
			
			return false;
		}
	};
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String argument)
	{
		return;
	}
}
