package com.puckowski.launcher4;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MemoryUsageActivity extends Activity
{
	private final String VMHEAP_DATA_FILE = "vmheap_data.txt";
	
	ArrayList<Double> mDataSampleArray; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState); 
		
		onCreate();
	}
	
	private void onCreate()
	{
		setContentView(R.layout.activity_memory_usage);
		
		mDataSampleArray = new ArrayList<Double>();
		
		SimpleBarGraph heapBarGraph = (SimpleBarGraph) findViewById(R.id.heapBarGraph);
		
		int displayWidth = getResources().getDisplayMetrics().widthPixels;
		int displayHeight = getResources().getDisplayMetrics().heightPixels;
		
		readDataSamples();
		
		heapBarGraph.setGraphWidth(displayWidth);
		heapBarGraph.setGraphHeight(400);
		heapBarGraph.setSampleData(mDataSampleArray);
		
		Button clearSamplesButton = (Button) findViewById(R.id.clearSamplesButton);
		
		clearSamplesButton.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View view) 
			{
				clearSampleData();
			}
		});
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
	         	 
	           	double heapPercentage = dataSample;
	           	double heapMaximum = Runtime.getRuntime().maxMemory();
	           	
	           	heapPercentage = (heapPercentage / heapMaximum);
	           
            	mDataSampleArray.add(heapPercentage);
            }
	             
	        heapInputStream.close();
	    } 
	    catch(Exception exception)
	    {
	    }
	}
	
	private void clearSampleData()
	{
		deleteFile(VMHEAP_DATA_FILE);
		
		SimpleBarGraph heapBarGraph = (SimpleBarGraph) findViewById(R.id.heapBarGraph);
		heapBarGraph.invalidate();
	}
}