package com.example.compressdemo;

import com.library.decrawso.DecRawso;

public class JNILib {
    public native static String whoamione();
    public native static String whoamitwo();
    public native static String whoamithree();
    
    public void loadlib()
    {
    	System.load(DecRawso.GetInstance().GetPath("helloone"));
    	//System.loadLibrary("helloone");  //now, you can just use the orignal code
    	System.loadLibrary("hellotwo"); 
    	//System.loadLibrary("hellothree");  //move to service
    }
}