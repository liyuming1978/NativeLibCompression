package com.example.compressdemo;

import com.library.decrawso.DecRawso;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	private Context mContext;
	private int sync_async = 0;
	
	private void switchto()
	{
		Intent intent = new Intent();
        intent.setClass(MainActivity.this, SecondActivity.class);
        startActivity(intent);
	}
		
	private Handler mDecRawsoHdl = new Handler()
	{
		public void handleMessage(android.os.Message msg) 
		{
			switch(msg.what)
			{
			case DecRawso.HDL_MSGDECEND:	
				if(!DecRawso.GetInstance().ShowError(mContext,msg.arg1))  //if no error, go on
				{				
					switchto();
					MainActivity.this.finish();	
				}
				//if you don't use ProgressDialog, you must stop the application by your self, and until this message.
				break;
			}
		}
	};	
	
	protected void onDestroy(){   
        super.onDestroy();  
    }  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		
		//test for x86
		//DecRawso.ConfigureFilter("libffmpeg", "libffmpeg_x86.so");

		if(sync_async==0)
		{
			DecRawso.NewInstance(mContext,mDecRawsoHdl,true);
		}
		else
		{
			//you can also use sync call
			DecRawso.NewInstance(mContext,null,false);
			// you can also add here, but if decoding use too much time, it will make application no response
			// if you loadlib just after initial, it will change to sync call (not recommend)
			switchto();
			this.finish();			
		}
		//test for x86
		//DecRawso.ConfigureFilter("libffmpeg", "libffmpeg_x86xx.so");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
