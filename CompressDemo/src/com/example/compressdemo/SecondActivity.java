package com.example.compressdemo;

import com.example.compressdemo.DemoService.LocalBinder;

import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.widget.TextView;
import android.os.IBinder;

public class SecondActivity extends Activity {

	private TextView  tv1;
	private TextView  tv2;
	private TextView  tv3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_second);
        
		tv1 = (TextView)findViewById(R.id.textView1);
		tv2 = (TextView)findViewById(R.id.textView2);
		tv3 = (TextView)findViewById(R.id.TextView3);
		
		ServiceConnection mSc;  
		mSc = new ServiceConnection(){  
            @Override  
            public void onServiceConnected(ComponentName name, IBinder service) {  
                //Log.d(TAG, "service connected");  
            	DemoService ss = ((LocalBinder)service).getService();  
                //ss.sayHelloWorld();  
        		tv3.setText("Service: "+ss.getLibStr());			
            }  
  
            @Override  
            public void onServiceDisconnected(ComponentName name) {  
                //Log.d(TAG, "service disconnected");  
            }  
        }; 
        
        Intent service = new Intent(this.getApplicationContext(),DemoService.class);  
        this.bindService(service, mSc, Context.BIND_AUTO_CREATE); 
		
		JNILib nativelib = new JNILib();
	    nativelib.loadlib();	
	    
		tv1.setText(JNILib.whoamione());
		tv2.setText(JNILib.whoamitwo());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.second, menu);
		return true;
	}

}
