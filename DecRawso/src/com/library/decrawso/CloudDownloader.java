package com.library.decrawso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;

public class CloudDownloader {
	private Thread mCloudThread=null;
	private HttpURLConnection httpURLConnection=null;
	private NetworkConnectChangedReceiver mNetworkStateReceiver;
	private final static int TIMEOUT = 10 * 1000;//	
	private String sAppFilePathClound;
	private Context mAppContextClound;
	private UtilsFunc mUtils = new UtilsFunc();
	public boolean bReInit = false;
	
	public void RegisterCloudDownloader(Context mc, String spath)
	{
		mAppContextClound = mc;
		sAppFilePathClound = spath;
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mNetworkStateReceiver = new NetworkConnectChangedReceiver();
		mAppContextClound.registerReceiver(mNetworkStateReceiver, filter);		
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
				File filex = new File(sAppFilePathClound+"/lib/cloud.txt");
				if(filex.exists())  //need download
				{
					try {
						BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( filex ) ), 1000);
						//String cloudtype = reader.readLine();	
						String cloudURL = reader.readLine();
						//Long cloudCRC  = Long.valueOf(reader.readLine()).longValue();
						reader.close();
						
						//if(cloudtype == "LINK")
						if(downloadCloudFile(cloudURL,sAppFilePathClound+"/lib/cloudrawso"))
						{
							mAppContextClound.unregisterReceiver(mNetworkStateReceiver); 
							filex.delete();
							mUtils.showToastInThread(mAppContextClound.getResources().getString(mUtils.getIdByName(mAppContextClound,"string","DecRawso_TackEffect_Restart")),mAppContextClound);
							bReInit = true;
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
}
