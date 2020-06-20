package com.puckowski.launcher4;

import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SmartDialog extends Dialog
{	
	private Context mContext;
	
	private String mTitle;
	private String mMessage;
	private String mPositiveButtonMessage;
	private String mNegativeButtonMessage;
	private String mNeutralButtonMessage;
	
	private String mResult;
	
	private Handler mHandler;

    public SmartDialog(Activity context, String title, String message, 
    		String positiveButton, String negativeButton, String neutralButton)
    {
        super(context);

        mContext = context;
        
        mTitle = title;
        mMessage = message;
        mPositiveButtonMessage = positiveButton;
        mNegativeButtonMessage = negativeButton;
        mNeutralButtonMessage = neutralButton;
        
        setContentAndFeatures(); 
    }
    
    private void setContentAndFeatures()
    {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	this.setCancelable(false);
    	
    	setContentView(R.layout.smart_dialog);
    	
    	int displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
    	int dialogWidth = (int) (displayWidth * 0.70);
    	
    	ViewGroup rootLayout = (ViewGroup) findViewById(R.id.rootLayout);
    	rootLayout.setMinimumWidth(dialogWidth);
    }
    
    public void setTitle(String title)
    {
    	mTitle = title;
    }
    
    public void setMessage(String message)
    {
    	mMessage = message;
    }
    
    public void setPositiveButtonMessage(String positiveButtonMessage)
    {
    	mPositiveButtonMessage = positiveButtonMessage;
    }
    
    public void setNegativeButtonMessage(String negativeButtonMessage)
    {
    	mNegativeButtonMessage = negativeButtonMessage;
    }
    
    public void setNeutralButtonMessage(String neutralButtonMessage)
    {
    	mNeutralButtonMessage = neutralButtonMessage;
    }
    
    public String getTitle()
    {
    	return mTitle;
    }
    
    public String getMessage()
    {
    	return mMessage;
    }
    
    public String getPositiveButtonMessage()
    {
    	return mPositiveButtonMessage;
    }
    
    public String getNegativeButtonMessage()
    {
    	return mNegativeButtonMessage;
    }
    
    public String getNeutralButtonMessage()
    {
    	return mNeutralButtonMessage;
    }
    
    public String getResult()
    {
    	return mResult;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {    
        if(mTitle != null)
        {
        	TextView titleArea = (TextView) findViewById(R.id.titleArea);
        	
        	titleArea.setText(mTitle);
        }
        else
        {
        	ViewGroup rootLayout = (ViewGroup) findViewById(R.id.rootLayout);
        	TextView titleArea = (TextView) findViewById(R.id.titleArea);
        	
        	rootLayout.removeView(titleArea);
        }
        
        if(mMessage != null)
        {
        	TextView messageArea = (TextView) findViewById(R.id.messageArea);
        	
        	messageArea.setText(mMessage);
        }
        else
        {
        	ViewGroup rootLayout = (ViewGroup) findViewById(R.id.rootLayout);
        	TextView messageArea = (TextView) findViewById(R.id.messageArea);
        	
        	rootLayout.removeView(messageArea);
        }
        
        if(mPositiveButtonMessage != null)
        {
        	Button positiveButton = (Button) findViewById(R.id.positiveButton);
        	
        	positiveButton = buildDialogButton(positiveButton, mPositiveButtonMessage);
        }
        else
        {
        	LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
        	Button positiveButton = (Button) findViewById(R.id.positiveButton);
        	
        	buttonLayout.removeView(positiveButton);
        }
        
        if(mNegativeButtonMessage != null)
        {
        	Button negativeButton = (Button) findViewById(R.id.negativeButton);

        	negativeButton = buildDialogButton(negativeButton, mNegativeButtonMessage);
        }
        else
        {
        	LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
        	Button negativeButton = (Button) findViewById(R.id.negativeButton);
        	
        	buttonLayout.removeView(negativeButton);
        }
        
        if(mNeutralButtonMessage != null)
        {
        	Button neutralButton = (Button) findViewById(R.id.neutralButton);
        	
        	neutralButton = buildDialogButton(neutralButton, mNeutralButtonMessage);
        }
        else
        {
        	LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
        	Button neutralButton = (Button) findViewById(R.id.neutralButton);
        	
        	buttonLayout.removeView(neutralButton);
        }
    }
    
    private Button buildDialogButton(Button genericButton, final String buttonMessage)
    {
    	genericButton.setText(buttonMessage);
    	
    	genericButton.setOnClickListener(new android.view.View.OnClickListener() 
    	{
    		@Override
    		public void onClick(View paramView)
    		{
    			Vibrator vibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
				vibrator.vibrate(25);
				
    			mResult = buttonMessage;
    			
    			endDialog();
    		}
    	});
    	
    	return genericButton;
    }
    
    public void endDialog()
    {
        dismiss();
    
        Message message = mHandler.obtainMessage();
        mHandler.sendMessage(message);
    }
    
    public String showDialog()
    {    	
        mHandler = new Handler() 
        {
            @Override
            public void handleMessage(Message message) 
            {
                throw new RuntimeException();
            }
        };
        
        super.show();
        
        try 
        {
            Looper.getMainLooper().loop();
        }
        catch(Exception exception)
        {
        	dismiss();
        }
        
        return mResult;
    }
}