package com.example.compressdemo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

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
		
		JNILib nativelib = new JNILib();
	    nativelib.loadlib();	
	    
		tv1.setText(JNILib.whoamione());
		tv2.setText(JNILib.whoamitwo());
		tv3.setText(JNILib.whoamithree());			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.second, menu);
		return true;
	}

}
