package com.puckowski.launcher4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class DataCollectionService extends Service 
{
	private final String VMHEAP_DATA_FILE = "vmheap_data.txt";
	
	private ArrayList<Integer> mDataSampleArray;
	private int mSampleCapacity;
	private long mSampleSleepInterval;
	
	private final Handler mHandler = new Handler();
	private Runnable mRunnable;
	
	@Override
    public void onCreate() 
	{
        super.onCreate();
        
        mRunnable = new Runnable() 
    	{
    	    @Override public void run() 
    	    {
    	    	collectDataSample();
    	    	
    	        queueRunnable();
    	    }
    	};
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    	mSampleCapacity = Integer.parseInt(sharedPreferences.getString("numberOfSamples", "24"));
		mSampleSleepInterval = Long.parseLong(sharedPreferences.getString("dataCollectionInterval", "5000"));
		
    	queueRunnable();
    }
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}
	
	private void queueRunnable() 
	{
	    mHandler.postDelayed(mRunnable, mSampleSleepInterval);
	}
	
	private void collectDataSample()
	{	
		mDataSampleArray = new ArrayList<Integer>();
	        
		readDataSamples();
		
		File file = new File(this.getFilesDir(), VMHEAP_DATA_FILE);

		try
		{
			FileOutputStream heapOutputStream = openFileOutput(VMHEAP_DATA_FILE, Context.MODE_PRIVATE);
			BufferedWriter heapWriter = new BufferedWriter(new OutputStreamWriter(heapOutputStream));

			for(int i = 0; i < mDataSampleArray.size(); i++)
			{
				heapWriter.write(String.valueOf(mDataSampleArray.get(i)) + "\n");
			}
			
			double vmHeapSize = Runtime.getRuntime().totalMemory();
		
			heapWriter.write(String.valueOf(vmHeapSize) + "\n");
	
			heapWriter.close();
		}
		catch(Exception exception)
		{ 
		} 
		
		mDataSampleArray = null;
	}
	
	private void readDataSamples()
	{
		FileInputStream heapInputStream = null;
			
		try
		{
			heapInputStream = openFileInput(VMHEAP_DATA_FILE);
		}
		catch(Exception exception)
		{
		}
			
	    try
	    {
	        BufferedReader idReader = new BufferedReader(new InputStreamReader(heapInputStream));

	        String line = null;

	        while((line = idReader.readLine()) != null) 
	        {
	         	int dataSample = (int) Double.parseDouble(line);
	           	
	           	if(mDataSampleArray.size() == (mSampleCapacity - 1))
	           	{
	           		mDataSampleArray.remove(0);
	           	}
	            
            	mDataSampleArray.add(dataSample);
            }
	             
	        heapInputStream.close();
	    } 
	    catch(Exception exception)
	    {
	    }
	}
}