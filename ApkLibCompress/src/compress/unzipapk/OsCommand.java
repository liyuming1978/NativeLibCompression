package compress.unzipapk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class OsCommand {
	private static OsCommand mOsCommand=null;
	private String JarPath;
	private String JarPathParent;
	private static boolean mbSlience=false;
	
	private OsCommand(String jarPath,String sys)
	{		
		JarPathParent = jarPath;
		JarPath = jarPath+sys;
	}
	
	protected static void newInstance(String jarPath,boolean bSlience) {
		if(mOsCommand!=null)
			return;
		
		mbSlience = bSlience;
		String OS=System.getProperties().getProperty("os.name").toLowerCase(); //get the os name
		if(OS.indexOf("linux")>=0)
		{
			mOsCommand = new OsCommand(jarPath,"../exefile/linux/");	
			try {
				Runtime.getRuntime().exec("chmod +x "+jarPath+"../exefile/linux/*");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(OS.indexOf("windows")>=0)
		{
			mOsCommand = new OsCommand(jarPath,"../exefile/windows/");	
		}
		else if(OS.indexOf("mac")>=0&&OS.indexOf("os")>0&&OS.indexOf("x")<0)
		{
			mOsCommand = new OsCommand(jarPath,"../exefile/mac/");
			try {
				Runtime.getRuntime().exec("chmod +x "+jarPath+"../exefile/mac/*");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		else
		{
			mOsCommand = null;
		}		
	}
	
	private void outputstr(String msg)
	{
		System.out.print(msg);
		if(!mbSlience)
			JOptionPane.showMessageDialog(null,msg);
		else
		{
			try {
				BufferedWriter porterr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(JarPathParent+"oserror.log",true)));
				porterr.write(msg);
				porterr.newLine();
				porterr.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
	
	protected static OsCommand getInstance() {
		return mOsCommand;
	}
	
	class WatchThread extends Thread
	{
		Process p; 
		boolean over; 
		public WatchThread(Process p)
		{
			this.p = p; 
			over = false; 
		}
		
		public void run() {
			try { 
				if (p == null) return; 
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())); 
				while (true) { 
					if (p==null || over) { 
						break; 
					} 
					while(br.readLine()!=null); 
					Thread.sleep(50);
				} 
				br.close();
			} catch (Exception e) { 
				e.printStackTrace(); 
			} 
		} 
		public void setOver(boolean over) { 
			this.over = over; 
		} 
	}

	public boolean CompressWithLzma(String cmd) {
		// TODO Auto-generated method stub
        Runtime rn=Runtime.getRuntime();
        try{
        	System.out.println("compress, it will take some time");
        	Process process = rn.exec(JarPath+"7za a -t7z "+cmd);
        	WatchThread wt = new WatchThread(process); 
        	wt.start(); 
        	process.waitFor();
        	wt.setOver(true); 
        	wt.join();
        }catch(Exception e){
        	outputstr("Error: compress "+cmd);
        	return false; 
        }     		
		return true;
	}

	public boolean AndroidAapt(String cmd,String workpath) {
		// TODO Auto-generated method stub
        Runtime rn=Runtime.getRuntime();
        try{
        	//System.out.println("aapt "+cmd);
        	Process process = rn.exec(JarPath+"aapt "+cmd,null,new File(workpath));
        	process.waitFor();
        }catch(Exception e){
        	outputstr("Error: aapt "+cmd);
        	return false; 
        }     			
		return true;
	}

	public boolean ApkJarSigner(String apkname, String[] keyname) {
		// TODO Auto-generated method stub
        Runtime rn=Runtime.getRuntime();
        try{
        	System.out.println("JarSigner "+apkname);
        	Process process = rn.exec("java -classpath "+JarPathParent+"tools.jar sun.security.tools.JarSigner -keystore "+
        			keyname[0]+" -storepass " +keyname[1]+" -keypass "+keyname[2]+" -sigfile "+keyname[4]+" "+apkname+" "+keyname[3]);
        	process.waitFor();
        }catch(Exception e){
        	outputstr("Error: JarSigner "+apkname);
        	return false; 
        }     			
		return true;
	}

	public String ApkZipAlign(String apkname) {
		// TODO Auto-generated method stub
		String outname = null;
        Runtime rn=Runtime.getRuntime();
        try{
        	outname = apkname.substring(0, apkname.lastIndexOf("."))+"Align.apk";
        	System.out.println("ZipAlign "+outname);      
        	File zipalignf = new File(outname);
        	if(zipalignf.exists())
        		zipalignf.delete();
        	
        	Process process = rn.exec(JarPath+"zipalign -v 4 "+apkname+" "+outname);     	      	
        	WatchThread wt = new WatchThread(process); 
        	wt.start(); 
        	process.waitFor();
        	wt.setOver(true); 
        	wt.join();
        	
        	if(!new File(outname).exists())
        	{
            	outputstr("Error: ZipAlign "+apkname);
            	return null; 
        	}
        	else
        	{
        		new File(apkname).delete();
        	}
        }catch(Exception e){
        	outputstr("Error: ZipAlign "+apkname);
        	return null; 
        }     		
		return outname;
	}		
	
	public ArrayList<String> GetShareLibrary(String libname)
	{
		boolean IsX86Lib=false;
		String LineStr=null;
		ArrayList<String> libarrary = new ArrayList<String>();
		Runtime rn=Runtime.getRuntime();
		
		if(!libname.endsWith(".so"))
			return libarrary;
		
        try{
        	Process process = rn.exec(JarPath+"readelf -h "+libname);
        	if(process != null)
        	{
	        	BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())); 
				while((LineStr=br.readLine())==null) //wait output,if the output is just once, use it ok, otherwise must use thread
					Thread.sleep(50);
				while(LineStr!=null)
				{
					if(LineStr.indexOf("Intel")>-1)
					{
						IsX86Lib = true;
						break;
					}						
					LineStr = br.readLine();
				}
				if(LineStr!=null)
					while(br.readLine()!=null);
				
				br.close();
				process.waitFor();
        	}
        	if(IsX86Lib) //only detect x86 lib
        	{
        		process = rn.exec(JarPath+"readelf -d "+libname);
        		if(process != null)
            	{
    	        	BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())); 
    	        	while((LineStr=br.readLine())==null) //wait output,if the output is just once, use it ok, otherwise must use thread
    					Thread.sleep(50);
    				while(LineStr!=null)
    				{
    	  				if(LineStr.indexOf("Shared library")>-1)
    	  				{
    	  					String[] getnames = LineStr.split("\\[");
    	  					if(getnames.length>1 && getnames[1].startsWith("lib"))
    	  					{
    	  						libarrary.add(getnames[1].substring(0, getnames[1].length()-1));
    	  					}
    	  				}
    					LineStr = br.readLine();
    				}
    				br.close();
    				process.waitFor();
            	}
        	}
        }catch(Exception e){
        	outputstr("Error: GetShareLibrary "+libname);
        	return libarrary; 
        }     					
		return libarrary;
	}
	
	public boolean IsArmShareLibrary(String libname)
	{
		boolean IsArmLib=false;
		String LineStr=null;
		Runtime rn=Runtime.getRuntime();
		
		if(!libname.endsWith(".so"))
			return false;
		
        try{
        	Process process = rn.exec(JarPath+"readelf -h "+libname);
        	if(process != null)
        	{
	        	BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())); 
				while((LineStr=br.readLine())==null) //wait output,if the output is just once, use it ok, otherwise must use thread
					Thread.sleep(50);
				while(LineStr!=null)
				{
					if(LineStr.indexOf("ARM")>-1)
					{
						IsArmLib = true;
						break;
					}						
					LineStr = br.readLine();
				}
				if(LineStr!=null)
					while(br.readLine()!=null);
				
				br.close();
				process.waitFor();
        	}

        }catch(Exception e){
        	outputstr("Error: IsArmShareLibrary "+libname);
        	return false; 
        }     					
		return IsArmLib;
	}	
	
}
