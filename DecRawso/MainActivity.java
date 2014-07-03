package com.intel.only7zdec;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private Button m_ButtonStart;
	private Context cont;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		cont = this.getApplicationContext();
		
		m_ButtonStart = (Button)findViewById(R.id.button1);
		m_ButtonStart.setOnClickListener(new Button.OnClickListener()
        {
        	public void onClick(View v)
        	{         		
        		//JNILib.Decode("/sdcard/test7z/test.7z",temp);
        		JNILib tt = new JNILib(cont);
        	}
        }
        ); 				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
