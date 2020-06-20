package com.puckowski.launcher4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class UserSupportActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
		
		onCreate();
	}
	
	private void onCreate()
	{
		setContentView(R.layout.activity_user_support);
		
		Button sendEmailButton = (Button) findViewById(R.id.sendEmailButton);
		final EditText editTextMessage = (EditText) findViewById(R.id.editTextMessage);
 
		sendEmailButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View view) 
			{
			    String emailRecipient = "puckowski.d@gmail.com";
			    String emailSubject = "Launcher4 Support";
			    String emailMessage = editTextMessage.getText().toString();
			    
			    if(emailMessage.length() == 0)
			    {
			    	return;
			    }
 
			    Intent emailIntent = new Intent(Intent.ACTION_SEND);
			  
			    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ emailRecipient });
			    emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
			    emailIntent.putExtra(Intent.EXTRA_TEXT, emailMessage);
 
			    emailIntent.setType("message/rfc822");
 
			    startActivity(Intent.createChooser(emailIntent, "Choose an Email client :"));
			    finish();
			}
		});
	}
}
