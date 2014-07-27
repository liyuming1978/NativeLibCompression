/*M///////////////////////////////////////////////////////////////////////////////////////
//
//  IMPORTANT: READ BEFORE DOWNLOADING, COPYING, INSTALLING OR USING.
//
//  By downloading, copying, installing or using the software you agree to this license.
//  If you do not agree to this license, do not download, install,
//  copy or use the software.
//
//
//                           License Agreement
//                For Open Source Computer Vision Library
//
// Copyright (C) 2014
// All rights reserved.
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.

//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
//DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//M*/

package com.library.decrawso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;



public class DecRawso {
	private native int Decode(AssetManager  asset,String inpath, String outpath,String abi); //will unzip (*.7z) in the same folder with full path
	private native boolean IsArmMode(); //get unzip arm or x86 folder
	private native void SetFilter(String filter,String fix);
	//private native int GetCpufamily();
	
	//make sure outpath end with / : for ex, "/sdcard/test7z/"    "/sdcard/test7z" will make error
	private static DecRawso DecRawsoSingleton = null; 	
	private static String sFilter=null;
	private static String sFix=null;
	
	private String sPathName=null;
	private String sAppFilePath=null;
	private boolean bWorkat7z=false;

	private Thread mDec7zLibThread=null;
	private int localVersion=0;
	private long lasttime=0;
	private String abi=null;
	private Context mAppContext;
	private Handler mHdl;
	private ProgressDialog dProDlg;
	
	private UtilsFunc mUtils = new UtilsFunc();
	private CloudDownloader mCloudDlr = new CloudDownloader();

	private final static int HDL_MSGBASE = 54321;	
	public final static int  HDL_MSGDECEND = 1+HDL_MSGBASE;
	//public final static int  HDL_MSGDOWNLOADEND = 2+HDL_MSGBASE;
	
	//public final static int  SZ_ERROR_DATA = 1;
	public final static int  SZ_ERROR_MEM = 2;
	//public final static int  SZ_ERROR_CRC = 3;
	//public final static int  SZ_ERROR_UNSUPPORTED = 4;
	//public final static int  SZ_ERROR_PARAM = 5;
	//public final static int  SZ_ERROR_INPUT_EOF = 6;
	//public final static int  SZ_ERROR_OUTPUT_EOF = 7;
	//public final static int  SZ_ERROR_READ = 8;
	public final static int  SZ_ERROR_WRITE = 9;
	//public final static int  SZ_ERROR_PROGRESS = 10;
	//public final static int  SZ_ERROR_FAIL = 11;
	//public final static int  SZ_ERROR_THREAD = 12;
	//public final static int  SZ_ERROR_ARCHIVE = 16;
	//public final static int  SZ_ERROR_NO_ARCHIVE = 17;	
	
	
	private class CrashHandler implements UncaughtExceptionHandler {
		//系统默认的UncaughtException处理类 
		private Thread.UncaughtExceptionHandler mDefaultHandler;

		public CrashHandler() {
			//获取系统默认的UncaughtException处理器
			mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			//设置该CrashHandler为程序的默认处理器
			Thread.setDefaultUncaughtExceptionHandler(this);
		}

		/**
		 * 当UncaughtException发生时会转入该函数来处理
		 */
		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			if (!handleException(ex) && mDefaultHandler != null) {
				//如果用户没有处理则让系统默认的异常处理器来处理
				mDefaultHandler.uncaughtException(thread, ex);
			} else {
				mUtils.showToastInThread(mAppContext.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_ReStrat")),mAppContext);
				//退出程序
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(1);
			}
		}

		/**
		 * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
		 * 
		 * @param ex
		 * @return true:如果处理了该异常信息;否则返回false.
		 */
		private boolean handleException(Throwable ex) {
			if ((ex instanceof UnsatisfiedLinkError)) {
				if(abi.contains("x86"))
				{
					String[] exmsgs = ex.getMessage().split(" ");
					if(exmsgs[0].compareTo("Couldn't")==0 && exmsgs[1].compareTo("load")==0)
						GetPath(exmsgs[2]);  //check file is exist or not
				}
				else
				{
					if(waitdecoding())
						return true;
					else
						return false;
				}
				return true;
			}
			
			return false;
		}
	}	
	

	
	private void delteFilter()
	{
		waitdecoding();
		
		if(bWorkat7z)
		{
			boolean findfix = false;
			File filedir = new File(sAppFilePath+"/lib/");
			File[] allfiles = filedir.listFiles();
			for (File tmpfile : allfiles)
			{
				if(tmpfile.isFile())
				{
					if(tmpfile.getName().startsWith(sFilter)&&tmpfile.getName().compareTo(sFix)==0)
						findfix = true;
				}
			}   
			if(findfix)
			{
				for (File tmpfile : allfiles)
				{
					if(tmpfile.isFile())
					{
						if(tmpfile.getName().startsWith(sFilter)&&tmpfile.getName().compareTo(sFix)!=0)
							tmpfile.delete();
					}
				}  			
			}
		}
	}
	
	private void myreboot()
	{
		/*
		Intent i = mContext.getPackageManager() 
				.getLaunchIntentForPackage(mContext.getPackageName()); 
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		mContext.startActivity(i);		
		*/
		System.exit(0); //if not exit, will loadlib fail.
	}
	
	private void SendDecEndMsg(int res)
	{
		ProDlg_Dismiss();
		if(mHdl!=null)
        	mHdl.sendMessage(mHdl.obtainMessage(HDL_MSGDECEND, res, 0));	
	}
	
	private void UpdateHdl(Handler hdl)
	{
		mHdl = hdl;
	}
	
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private long GetLastTime(PackageInfo packageInfo)
	{
		return packageInfo.lastUpdateTime;
	}	
	
	private DecRawso()
	{		
	}

	private String getX86abi()   //someone say : build.prop abi can be changed
	{
		String x86abi = android.os.Build.CPU_ABI;
		
		if(x86abi.contains("x86")||x86abi.contains("x32"))
			return x86abi;
		else //if(x86abi.contains("armeabi-v7a")) //avoid any changes
		{
			Process process;
			try {
				process = Runtime.getRuntime().exec("getprop ro.product.cpu.abi");
	            InputStreamReader ir = new InputStreamReader(process.getInputStream());
	            BufferedReader input = new BufferedReader(ir);
	            String tmpabi = input.readLine();
	            input.close();
	            
	            if(tmpabi.contains("x86")||tmpabi.contains("x32"))
	            	return tmpabi;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		return null;
	}	
	
	private DecRawso(Context cont,Handler hdl,boolean showProgress)
	{	
		boolean bSendDecEnd = false;
		bWorkat7z = false;
		localVersion = 0;
		dProDlg = null;
		mDec7zLibThread = null;
		
		mHdl = hdl;
		mAppContext = cont.getApplicationContext();

		sAppFilePath = cont.getFilesDir().getAbsolutePath();
		sPathName  = sAppFilePath+"/../lib/";
		AssetFileDescriptor fd=null;
 
		abi = android.os.Build.CPU_ABI;
		if(!abi.contains("arm") && !abi.contains("x86") && !abi.contains("mips") && !abi.contains("x32"))
			abi="armeabi";	
		//todo:  x86 now will detect as armeabi-v7a
		String tmpx86abi = getX86abi();
		if(tmpx86abi!=null)
		{
			abi = tmpx86abi;
			if(abi.contains("x32"))
				abi = "x86";
		}
		
		//may error , so fd = null
		AssetManager am = mAppContext.getAssets();
		try {
			fd = am.openFd("rawso");
			fd.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	
		if(fd!=null){
	        try {  
	            PackageInfo packageInfo = cont.getApplicationContext()  
	                    .getPackageManager().getPackageInfo(cont.getPackageName(), 0);  
	            localVersion = packageInfo.versionCode;  
	            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
	            	lasttime = GetLastTime(packageInfo);
	        } catch (NameNotFoundException e) {  
	            e.printStackTrace();  
	        } 
	        
	        File filex = new File(sAppFilePath+"/lib/decdone_"+localVersion+"_"+lasttime);
	        File filedir = new File(sAppFilePath+"/lib/");
    		if(!filex.exists())
    		{
        		if(!filedir.exists()){  
        			filedir.mkdir();//empty so create dir
         		} 
        		else  //delete all sub files
        		{
        			File forcearm = new File(sAppFilePath+"/lib/_FORCEARM_.tmp");
        			if(forcearm.exists() && abi.contains("x86"))  //x86 lib miss, we can use arm lib
        				abi="armeabi-v7a";
        				
	    			File[] allfiles = filedir.listFiles();
	    			for (File tmpfile : allfiles)
	    			{
	    				tmpfile.delete();  //_FORCEARM_.tmp will be deleted
	    			}
        		}
        		
	    		sPathName  = sAppFilePath+"/lib/";
	    		bWorkat7z = true;
	    		Dec7zLib(showProgress,true,cont);
	    		bSendDecEnd = true;
    		}
    		else //if(!filex.exists())
    		{
    			sPathName  = sAppFilePath+"/lib/";
    			bWorkat7z = true;
        		
        		File filecloud = new File(sAppFilePath+"/lib/cloud.txt");
        		if(filecloud.exists())  //need download
        		{
        			mCloudDlr.RegisterCloudDownloader(mAppContext, sAppFilePath);
        		}
        		else
        		{
        			File filecloudraw = new File(sAppFilePath+"/lib/cloudrawso");
        			if(filecloudraw.exists())
        			{
    	    			File[] allfiles = filedir.listFiles();
    	    			for (File tmpfile : allfiles)
    	    			{
    	    				if(tmpfile.compareTo(filex)!=0 && tmpfile.compareTo(filecloudraw)!=0)
    	    					tmpfile.delete(); 
    	    			}        				
        				Dec7zLib(showProgress,false,cont);
        				bSendDecEnd = true;
        			}
        			filecloudraw = null;
        		}  
        		filecloud = null;
    		}
		}

		new CrashHandler();  
		                		
		if(!bSendDecEnd)  //send HDL_MSGDECEND for any
		{
			mUtils.HackLibPath(sPathName);   //only decoding finish then add library path, to avoid load a decoding file 
			SendDecEndMsg(0);
		}
	}
	
	private void ProDlg_Dismiss()
	{
		if(dProDlg!=null)
		{
			if(dProDlg.isShowing())
				dProDlg.dismiss();
			dProDlg = null;
		}
	}
	
	class Dec7zLibThread implements Runnable	
	{
		private boolean bLocalDec;
		public Dec7zLibThread(boolean LocalDec)
		{
			bLocalDec = LocalDec;
		}
		private int readRawso(String outname)
		{
			AssetManager am = mAppContext.getAssets();
			try {
				BufferedInputStream bin = new BufferedInputStream(am.open("rawso"));
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(outname))); 
				if(bin!=null && bout!=null)
				{
		            // cache 
		            byte[] b = new byte[1024 * 4];
		            int len;
		            while ((len = bin.read(b)) != -1) {
		            	bout.write(b, 0, len);
		            }
		            // refresh 
		            bout.flush(); 
		            bin.close();
		            bout.close();
				}
				else
					return 9;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 9;
			}
			return 0;
		}
		
		@Override
		public void run() {   
			int res;
			if(bLocalDec)
			{
				if(Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD) //decode on android2.2
				{
					res = readRawso(sAppFilePath+"/lib/rawso22");
					if(res==0)
						res = Decode(null,sAppFilePath+"/lib/rawso22",sPathName,abi);
				}
				else
					res = Decode(mAppContext.getAssets(),null,sPathName,abi);
				
	        	if(0==res)
	        	{
	        		File filex = new File(sAppFilePath+"/lib/decdone_"+localVersion+"_"+lasttime);
	        		try {
						filex.createNewFile();
						filex = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        	}
			}
			else
			{
				res = Decode(null,sAppFilePath+"/lib/cloudrawso",sAppFilePath+"/lib/",abi);
				if(0==res)
				{
					File fileraw = new File(sAppFilePath+"/lib/cloudrawso");
					fileraw.delete();	
					fileraw = null;
				}			
			}
			if(IsArmMode())
			{
				File file_armmode = new File(sAppFilePath+"/lib/armmode");
				try {
					file_armmode.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
    		File filecloud = new File(sAppFilePath+"/lib/cloud.txt");
    		if(filecloud.exists())  //need download
    		{
    			mCloudDlr.RegisterCloudDownloader(mAppContext, sAppFilePath);
    		}
    		
    		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD) //decode on android2.2
    		{
    			File fileraw22 = new File(sAppFilePath+"/lib/rawso22");
    			if(fileraw22.exists())
    				fileraw22.delete();
    			fileraw22 = null;
    		}
    		
    		mUtils.HackLibPath(sPathName); //only decoding finish then add library path, to avoid load a decoding file 
    		
        	if(mHdl!=null && !(!bLocalDec&&res!=0))
        	{
        		SendDecEndMsg(res);
        	}
        	else
        	{
        		if(res!=0)
        		{
        			mUtils.showToastInThread(geterror(res),mAppContext);
        			if(!bLocalDec)
        			{
						File forcearm = new File(sPathName+"_FORCEARM_.tmp");
						try {
							forcearm.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}       				
        				File filex = new File(sAppFilePath+"/lib/decdone_"+localVersion+"_"+lasttime);
        				filex.delete(); //if cloud dec error, re decode and use the default arm 
        			}
        			System.exit(0);
        		}
        	}
		}
	}
	
	private void Dec7zLib(boolean showProgress,boolean bLocalDec,Context cont)
	{
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD)
			System.loadLibrary("DecRawso22");
		else
			System.loadLibrary("DecRawso");		
		
		if(sFilter!=null)
			SetFilter(sFilter,sFix);

		if(showProgress)
		{
			try
			{
			dProDlg = ProgressDialog.show(cont, cont.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Initializing")), 
					cont.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Wait")));
			}catch(Exception e)
			{
				
			}
		}
		mDec7zLibThread = new Thread(new Dec7zLibThread(bLocalDec));
		mDec7zLibThread.start();
	}
	
	private String geterror(int errcode)
	{
		String errmsg;
		switch(errcode)
		{
		default:
			errmsg = mAppContext.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Unknown_Error"));
			break;
		case SZ_ERROR_MEM:
			errmsg = mAppContext.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Insufficient_Memory"));
			break;
		case SZ_ERROR_WRITE:
			errmsg = mAppContext.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Insufficient_Storage"));
			break;			
		}		
		return errmsg;
	}
	
	
//-------------------------------------------------------------------------------------------------------------
//####### do not change the public interface##################################################################
//-------------------------------------------------------------------------------------------------------------
	/*
	 * show the decode error code 
	 *generally error:  SZ_ERROR_WRITE(storage is full, can not write the file) 
	 */
	public boolean ShowError(Context cont,int errcode)
	{
		String errmsg;
		mHdl = null; //release reference
		if(errcode == 0) //no error
			return false;
		errmsg = geterror(errcode);
		// decode rawso error
		AlertDialog.Builder alert = new AlertDialog.Builder(cont);
		alert.setTitle(cont.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Initial_Error")))
				.setMessage(errmsg)
				.setPositiveButton(cont.getResources().getString(mUtils.getIdByName(mAppContext,"string","DecRawso_Quit")),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								System.exit(0);
							}
						});
		alert.create().show();
		return true;
	}
	
	/*
	 * change the system.loadlibrary to system.load
	 * system.loadlibrary also works, but system.load( GetPath(***)) is more safety
	 */
	public String GetPath(String libname)
	{
		waitdecoding();
		
		if(bWorkat7z)
		{
			//if work at 7z mode, but find the so is clear, reboot to decode again
			File filex = new File(sPathName+"lib"+libname+".so");
			if(!filex.exists())
			{
				filex = new File(sPathName+"decdone_"+localVersion+"_"+lasttime);
				if(!filex.exists())
				{
					//if so not exist, restart the application
					myreboot();		
				}
				else  //lib is not exist, but has decoded.
				{
					//if(abi=="x86")  //x86 lib miss, we can use arm lib
					if(abi.contains("x86"))
					{
						File file_armmode = new File(sAppFilePath+"/lib/armmode");
						if(!file_armmode.exists()) //not work on arm mode , so reboot and redecode
						{
							File forcearm = new File(sPathName+"_FORCEARM_.tmp");
							try {
								forcearm.createNewFile();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							filex.delete();
							myreboot();
						}
					}
					else
					{
						//arm lib miss, just pass
					}
				}
			}
		}
		return sPathName+"lib"+libname+".so";
	}
	
	/*
	 * use it to get the library path (if not compress, it's the default library path)
	 */
	public String GetStorePath()
	{
		return sPathName;
	}
	
	/*
	 * only decode fix name library, for ex:  libffmpegv6.so libffmpegneon.so with filter  libffmpeg
	 * if the fix is libffmpegv6.so, only libffmpegv6.so is decoded.  
	 * it's useful to save the installed size
	 */
	public static void ConfigureFilter(String filter,String fix)
	{
		sFilter = filter;
		sFix = fix;
		if(DecRawsoSingleton!=null)
		{
			DecRawsoSingleton.delteFilter();
		}
	}	
	
	/*
	 * call it if you do not wait the decoding end msg
	 * you must call it before you load the library (if you use system.load , it will call it)
	 * if system.loadlibrary is using and decoding is not finish, the exception will be catch and it will ok when you re-enter
	 */
	public boolean waitdecoding()
	{
		if(mDec7zLibThread!=null)
		{
			//when decoding , can not load so, need wait
			try {
				mDec7zLibThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mDec7zLibThread = null;		
			return true;
		}	
		else
			return false;
	}
	
	/*
	 * use it to check whether it's x86 cpu
	 * note! android.os.Build.CPU_ABI will return armeabi-v7a on some x86 devices
	 */
	public static boolean getX86Cpu()   //someone say : build.prop abi can be changed
	{
		boolean retc = false;
		String x86abi = android.os.Build.CPU_ABI;
		
		if(x86abi.contains("x86")||x86abi.contains("x32"))
			return true;
		else //if(x86abi.contains("armeabi-v7a")) //avoid any changes
		{
			Process process;
			try {
				process = Runtime.getRuntime().exec("getprop ro.product.cpu.abi");
	            InputStreamReader ir = new InputStreamReader(process.getInputStream());
	            BufferedReader input = new BufferedReader(ir);
	            String tmpabi = input.readLine();
	            input.close();
	            
	            if(tmpabi.contains("x86")||tmpabi.contains("x32"))
	            	retc =true;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
/*

			try {
				if(new File("/sys/devices/system/cpu/modalias").exists())
				{
					BufferedReader br = new BufferedReader(new FileReader("/sys/devices/system/cpu/modalias"));
					if(br.readLine().indexOf("x86cpu") > -1)
						retc = true;
					br.close();	
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
*/
		}
		return retc;
	}
	
	/*
	 * you must call it on the first , use Handler the get the decoding msg
	 * if you do not receive the decoding end msg. please make sure call waitdecoding or use system.load
	 */
	public static boolean NewInstance(Context cont,Handler hdl,boolean showProgress)
	{
		if(DecRawsoSingleton==null || DecRawsoSingleton.mCloudDlr.bReInit)
		{
			DecRawsoSingleton = new DecRawso(cont,hdl,showProgress);
			DecRawsoSingleton.mCloudDlr.bReInit = false;
			return true;
		}
		else 
		{
			DecRawsoSingleton.UpdateHdl(hdl);
			DecRawsoSingleton.SendDecEndMsg(0);
			return false;
		}
	}
	
	/*
	 * use it the get the singleton
	 */
	public static DecRawso GetInstance()
	{
		//must call NewInstance firstly
		return DecRawsoSingleton;
	}			
}
