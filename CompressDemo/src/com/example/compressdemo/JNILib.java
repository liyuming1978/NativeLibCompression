package com.example.compressdemo;

import com.library.decrawso.DecRawso;

public class JNILib {
    public native static String whoamione();
    public native static String whoamitwo();
    public native static String whoamithree();
    
    public void loadlib()
    {
    	System.load(DecRawso.GetInstance().GetPath("hellotwo"));
    	//System.load(DecRawso.GetInstance().GetPath("helloone"));
    	System.loadLibrary("helloone");  //now, you can just use the orignal code
    	System.load(DecRawso.GetInstance().GetPath("hellothree"));
    }
}