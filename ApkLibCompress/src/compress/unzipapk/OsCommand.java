package compress.unzipapk;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

public class OsCommand {
	private static OsCommand mOsCommand=null;
	private String JarPath;
	private String JarPathParent;
	
	private OsCommand(String jarPath,String sys)
	{		
		JarPathParent = jarPath;
		JarPath = jarPath+sys;
	}
	
	protected static void newInstance(String jarPath) {
		if(mOsCommand!=null)
			return;
		
		String OS=System.getProperties().getProperty("os.name").toLowerCase(); //get the os name
		if(OS.indexOf("linux")>=0)
		{
			mOsCommand = new OsCommand(jarPath,"linux/");	
		}
		else if(OS.indexOf("windows")>=0)
		{
			mOsCommand = new OsCommand(jarPath,"windows/");	
		}
		else if(OS.indexOf("mac")>=0&&OS.indexOf("os")>0&&OS.indexOf("x")<0)
		{
			mOsCommand = new OsCommand(jarPath,"mac/");
		}
		else
		{
			mOsCommand = null;
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
        	JOptionPane.showMessageDialog(null,"Error: compress "+cmd);
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
        	JOptionPane.showMessageDialog(null,"Error: aapt "+cmd);
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
        	JOptionPane.showMessageDialog(null,"Error: JarSigner "+apkname);
        	return false; 
        }     			
		return true;
	}

	public boolean ApkZipAlign(String apkname) {
		// TODO Auto-generated method stub
        Runtime rn=Runtime.getRuntime();
        try{
        	String outname = apkname.substring(0, apkname.lastIndexOf("."))+"Align.apk";
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
            	JOptionPane.showMessageDialog(null,"Error: ZipAlign "+apkname);
            	return false; 
        	}
        	else
        	{
        		new File(apkname).delete();
        	}
        }catch(Exception e){
        	JOptionPane.showMessageDialog(null,"Error: ZipAlign "+apkname);
        	return false; 
        }     		
		return true;
	}		
	
}
