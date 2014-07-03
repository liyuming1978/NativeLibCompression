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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;


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
	private boolean b7zDecoding;
	private Thread mCloudThread=null;
	private Thread mDec7zLibThread=null;
	private HttpURLConnection httpURLConnection=null;
	private int localVersion=0;
	private long lasttime=0;
	private String abi=null;
	private Context mAppContext;
	private Handler mHdl;
	private ProgressDialog dProDlg;
	private NetworkConnectChangedReceiver mNetworkStateReceiver;

	private final static int TIMEOUT = 10 * 1000;//
	
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
	
	public static void ConfigureFilter(String filter,String fix)
	{
		sFilter = filter;
		sFix = fix;
		if(DecRawsoSingleton!=null)
		{
			DecRawsoSingleton.delteFilter();
		}
	}
	
	private void delteFilter()
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
		}	
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
	
	public boolean IsDecoding()
	{
		return b7zDecoding;
	}
	
	public static boolean NewInstance(Context cont,Handler hdl,boolean showProgress)
	{
		if(DecRawsoSingleton==null)
		{
			DecRawsoSingleton = new DecRawso(cont,hdl,showProgress);
			return true;
		}
		else 
		{
			DecRawsoSingleton.UpdateHdl(hdl);
			DecRawsoSingleton.SendDecEndMsg(0);
			return false;
		}
	}
	
	public static DecRawso GetInstance()
	{
		//must call NewInstance firstly
		return DecRawsoSingleton;
	}	
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private long GetLastTime(PackageInfo packageInfo)
	{
		return packageInfo.lastUpdateTime;
	}	
	
	private DecRawso()
	{		
	}
	

	public static boolean getX86Cpu()   //someone say : build.prop abi can be changed
	{
		boolean retc = false;
		String x86abi = android.os.Build.CPU_ABI;
		
		if(x86abi.startsWith("x86"))
			return true;
		else //if(x86abi.startsWith("armeabi-v7a")) //avoid any changes
		{
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
		}
		return retc;
	}
	
	private DecRawso(Context cont,Handler hdl,boolean showProgress)
	{	
		boolean bSendDecEnd = false;
		bWorkat7z = false;
		b7zDecoding = false;
		localVersion = 0;
		dProDlg = null;
		mDec7zLibThread = null;
		
		mHdl = hdl;
		mAppContext = cont.getApplicationContext();

		sAppFilePath = cont.getFilesDir().getAbsolutePath();
		sPathName  = sAppFilePath+"/../lib/";
		AssetFileDescriptor fd=null;
 
		abi = android.os.Build.CPU_ABI;
		if(!abi.startsWith("arm") && !abi.startsWith("x86") && !abi.startsWith("mips"))
			abi="armeabi";	
		//todo:  x86 now will detect as armeabi-v7a
		if(getX86Cpu())
		{
			if(!abi.startsWith("x86"))
			{
				if(abi.startsWith("arm64"))
					abi="x86_64";
				else
					abi="x86";
			}
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
	            if(Build.VERSION.SDK_INT>=9)
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
        			if(forcearm.exists() && abi.startsWith("x86"))  //x86 lib miss, we can use arm lib
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
        			 IntentFilter filter = new IntentFilter();
        			 filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        			 mNetworkStateReceiver = new NetworkConnectChangedReceiver();
        			 mAppContext.registerReceiver(mNetworkStateReceiver, filter);
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
		if(!bSendDecEnd)  //send HDL_MSGDECEND for any
			SendDecEndMsg(0);
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
				if(Build.VERSION.SDK_INT<9) //decode on android2.2
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
    			 IntentFilter filter = new IntentFilter();
    			 filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    			 mNetworkStateReceiver = new NetworkConnectChangedReceiver();
    			 mAppContext.registerReceiver(mNetworkStateReceiver, filter);
    		}
    		
    		if(Build.VERSION.SDK_INT<9) //decode on android2.2
    		{
    			File fileraw22 = new File(sAppFilePath+"/lib/rawso22");
    			if(fileraw22.exists())
    				fileraw22.delete();
    			fileraw22 = null;
    		}
    		
        	if(mHdl!=null && !(!bLocalDec&&res!=0))
        	{
        		SendDecEndMsg(res);
        	}
        	else
        	{
        		if(res!=0)
        		{
        			showToastInThread(geterror(res));
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
        	b7zDecoding = false;
		}
	}
	
	private void Dec7zLib(boolean showProgress,boolean bLocalDec,Context cont)
	{
		if(Build.VERSION.SDK_INT<9)
			System.loadLibrary("DecRawso22");
		else
			System.loadLibrary("DecRawso");		
		
		if(sFilter!=null)
			SetFilter(sFilter,sFix);

		b7zDecoding = true;
		if(showProgress)
		{
			dProDlg = ProgressDialog.show(cont, cont.getResources().getString(R.string.Initializing), 
					cont.getResources().getString(R.string.Wait));
		}
		mDec7zLibThread = new Thread(new Dec7zLibThread(bLocalDec));
		mDec7zLibThread.start();
	}
	
	private boolean downloadCloudFile(String down_url, String file)
			throws Exception {
		int down_step = 5;// step
		long totalSize;// totalsize
		long downloadCount = 0;
		int updateCount = 0;
		InputStream inputStream;
		OutputStream outputStream;

		URL url = new URL(down_url);
		httpURLConnection = (HttpURLConnection) url
				.openConnection();
		httpURLConnection.setConnectTimeout(TIMEOUT);
		//httpURLConnection.setReadTimeout(TIMEOUT);
		//httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
		//httpURLConnection.setRequestMethod("GET");
		//httpURLConnection.setRequestProperty("Connection", "Keep-Alive");    
		//httpURLConnection.connect();
		// 
		totalSize = httpURLConnection.getContentLength();
		if (httpURLConnection.getResponseCode() == 404 || totalSize<=0) {
			throw new Exception("fail!");
		}
		if (httpURLConnection != null) {
			httpURLConnection.disconnect();
		}
		
		File downloadis = new File(file);
		if(downloadis.exists())
			downloadCount = downloadis.length();
		
		if(downloadCount<totalSize)
		{
			outputStream = new FileOutputStream(file, true);// if file is exist , than pending at end
			//
			httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setConnectTimeout(TIMEOUT);
			//httpURLConnection.setReadTimeout(TIMEOUT);
			//httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive");    		
			httpURLConnection.setRequestProperty("Range", "bytes="+downloadCount+ "-" + (totalSize-1));
			inputStream = httpURLConnection.getInputStream();
	
			byte buffer[] = new byte[1024];
			int readsize = 0;
			while ((readsize = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, readsize);
				//outputStream.flush();
				downloadCount += readsize;//
				/**
				 * 5% each time
				 */
				if (updateCount == 0
						|| (downloadCount * 100 / totalSize - down_step) >= updateCount) {
					updateCount += down_step;
					// 
					// notification.setLatestEventInfo(this, "downloading...", updateCount
					// + "%" + "", pendingIntent);
				}
			}			
			inputStream.close();
			outputStream.close();
			
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
				httpURLConnection = null;
			}
		}

		return downloadCount>=totalSize;
	}	
	
	private String geterror(int errcode)
	{
		String errmsg;
		switch(errcode)
		{
		default:
			errmsg = mAppContext.getResources().getString(R.string.Unknown_Error);
			break;
		case SZ_ERROR_MEM:
			errmsg = mAppContext.getResources().getString(R.string.Insufficient_Memory);
			break;
		case SZ_ERROR_WRITE:
			errmsg = mAppContext.getResources().getString(R.string.Insufficient_Storage);
			break;			
		}		
		return errmsg;
	}
	
	public boolean ShowError(Context cont,int errcode)
	{
		String errmsg;
		mHdl = null; //release reference
		if(errcode == 0) //no error
			return false;
		errmsg = geterror(errcode);
		// decode rawso error
		AlertDialog.Builder alert = new AlertDialog.Builder(cont);
		alert.setTitle(cont.getResources().getString(R.string.Initial_Error))
				.setMessage(errmsg)
				.setPositiveButton(cont.getResources().getString(R.string.Quit),
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
	public void showalert()
	{
		// network download finish.
		AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
		alert.setTitle(mContext.getResources().getString(R.string.Optimization))
				.setMessage(mContext.getResources().getString(R.string.TackEffect_Restart))
				.setPositiveButton(mContext.getResources().getString(R.string.ReStrat),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								myreboot();
							}
						})
				.setNegativeButton(mContext.getResources().getString(R.string.Continue),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		alert.create().show();		
	}
	*/
	
	class ToastThread implements Runnable	
	{
		private String msgstr;
		private Looper lp;
		public ToastThread(String _msgstr)
		{
			msgstr = _msgstr;
		}
		public Looper getItLooper()
		{
			return lp;
		}
		@Override
		public void run() {   
			Looper.prepare();  
			lp = Looper.myLooper();
	        Toast.makeText(mAppContext,msgstr,Toast.LENGTH_LONG).show();  
	        Looper.loop(); 
		}
	}
	private boolean showToastInThread(String msgstr)
	{
		ToastThread runToast = new ToastThread(msgstr);
		Thread tmpToastThread = new Thread(runToast);
		tmpToastThread.start();
		try {
			tmpToastThread.join(5000);
			runToast.getItLooper().quit();
			tmpToastThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;
	}
	
	class NetworkConnectChangedReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
	            Parcelable parcelableExtra = intent
	                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	            if (null != parcelableExtra) {
	                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
	                if (networkInfo.getState() == android.net.NetworkInfo.State.CONNECTED) {
	                	checkCloud();
	                } 
	            }
	        }
	    }
	}
	
	private void checkCloud()
	{	
		if(mCloudThread!=null)
		{
			try {
				if (httpURLConnection != null) {
					httpURLConnection.disconnect();
					httpURLConnection = null;
				}				
				mCloudThread.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		mCloudThread = new Thread(new Runnable() {
			@Override
			public void run() {
				File filex = new File(sAppFilePath+"/lib/cloud.txt");
				if(filex.exists())  //need download
				{
					try {
						BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( filex ) ), 1000);
						//String cloudtype = reader.readLine();	
						String cloudURL = reader.readLine();
						//Long cloudCRC  = Long.valueOf(reader.readLine()).longValue();
						reader.close();
						
						//if(cloudtype == "LINK")
						if(downloadCloudFile(cloudURL,sAppFilePath+"/lib/cloudrawso"))
						{
							mAppContext.unregisterReceiver(mNetworkStateReceiver); 
							filex.delete();
							showToastInThread(mAppContext.getResources().getString(R.string.TackEffect_Restart));
		        			System.exit(0);
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		mCloudThread.start();
	}
	
	public String GetPath(String libname)
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
		}
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
					if(abi.startsWith("x86"))
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
	
	public String GetStorePath()
	{
		return sPathName;
	}
}
