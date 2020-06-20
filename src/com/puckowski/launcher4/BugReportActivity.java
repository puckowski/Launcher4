package com.puckowski.launcher4;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class BugReportActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
		
		onCreate();
	}
	
	private void onCreate()
	{
		setContentView(R.layout.activity_bug_report);
		
		Button sendEmailButton = (Button) findViewById(R.id.sendEmailButton);
		final EditText editTextMessage = (EditText) findViewById(R.id.editTextMessage);
 
		sendEmailButton.setOnClickListener(new OnClickListener() 
		{
			@SuppressLint("NewApi")
			@Override
			public void onClick(View view) 
			{
			    String emailRecipient = "puckowski.d@gmail.com";
			    String emailSubject = "Launcher4 Issue/Bug Report";
			    String emailMessage = editTextMessage.getText().toString();
			    
			    if(emailMessage.length() == 0)
			    {
			    	return;
			    }
			    
			    String title, message, positiveButton, negativeButton, neutralButton;
				
				title = "Transparency notice";
				message = "In addition to your message, Launcher4 would like to send" + 
						" additional information about your Android device. This information" +
						" may include things such as: Android version, display resolution" +
						" and density, and visibility of status bar or navigation bar." +
						" No personal information will be sent.";
				positiveButton = "Yes, send the additional information";
				negativeButton = "No, just send my message please";
				neutralButton = null;
				
				SmartDialog smartDialog = new SmartDialog(BugReportActivity.this, title, message,
						positiveButton, negativeButton, neutralButton);
				
				String dialogResult = smartDialog.showDialog();
				String deviceInformation = "";
				
				if(dialogResult.equals(positiveButton))
				{
					int deviceApiCheck = Integer.valueOf(android.os.Build.VERSION.SDK);
					
					deviceInformation = (deviceInformation + "\nAndroid SDK version: " + deviceApiCheck);
					deviceInformation = (deviceInformation + "\nDisplay density: " + getResources().getDisplayMetrics().density);
					deviceInformation = (deviceInformation + "\nDisplay width (pixels): " + getResources().getDisplayMetrics().widthPixels);
					deviceInformation = (deviceInformation + "\nDisplay height (pixels): " + getResources().getDisplayMetrics().heightPixels);
					
					View decorView = getWindow().getDecorView();
					
					if(deviceApiCheck >= 11)
					{
						int isSystemUiVisible = decorView.getSystemUiVisibility();
						deviceInformation = (deviceInformation + "\nSystem UI visibility: " + isSystemUiVisible);
					}
					
					deviceInformation = (deviceInformation + "\nVM heap size: " + Runtime.getRuntime().totalMemory());
					deviceInformation = (deviceInformation + "\nAllocated VM memory: " + (Runtime.getRuntime().totalMemory()
						- Runtime.getRuntime().freeMemory()));
					deviceInformation = (deviceInformation + "\nVM heap size limit: " + Runtime.getRuntime().maxMemory());
					deviceInformation = (deviceInformation + "\nNative allocated memory: " + Debug.getNativeHeapAllocatedSize());
				}
 
			    Intent emailIntent = new Intent(Intent.ACTION_SEND);
			  
			    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ emailRecipient });
			    emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
			    emailIntent.putExtra(Intent.EXTRA_TEXT, (emailMessage + deviceInformation));
 
			    emailIntent.setType("message/rfc822");
 
			    startActivity(Intent.createChooser(emailIntent, "Choose an Email client :"));
			    finish();
			}
		});
	}
}
