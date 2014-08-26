package com.example.compressdemo;

import com.library.decrawso.DecRawso;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


public class DemoService  extends Service  {

	private native static String whoamithree();
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return new LocalBinder();  
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		DecRawso.NewInstanceInService(this);  //must initial DecRawso in different process
		System.loadLibrary("hellothree");   //must load after NewInstanceInService
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

		super.onDestroy();
	}
	
	public class LocalBinder extends Binder {  
		DemoService getService() {  
            // Return this instance of LocalService so clients can call public methods  
            return DemoService.this;  
        }     
    } 
	
	public String getLibStr()
	{
		return whoamithree();
	}
}
