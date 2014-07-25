package com.library.decrawso;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

public class UtilsFunc {
	private Context mUtilsFuncContext;

	class ToastThread implements Runnable	
	{
		private String msgstr;
		private Looper lp;
		public ToastThread(String _msgstr,Context mc)
		{
			msgstr = _msgstr;
			mUtilsFuncContext = mc;
		}
		public Looper getItLooper()
		{
			return lp;
		}
		@Override
		public void run() {   
			Looper.prepare();  
			lp = Looper.myLooper();
	        Toast.makeText(mUtilsFuncContext,msgstr,Toast.LENGTH_LONG).show();  
	        Looper.loop(); 
		}
	}
	public boolean showToastInThread(String msgstr,Context mc)
	{
		ToastThread runToast = new ToastThread(msgstr,mc);
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
	
	@SuppressLint("NewApi")
	public void HackSystemICS(String pname)
	{
		try{
			Field fieldSysPath = BaseDexClassLoader.class.getDeclaredField("pathList");  
	        fieldSysPath.setAccessible(true);
	        Object paths = (Object)fieldSysPath.get(this.getClass().getClassLoader());  
	        Class c = paths.getClass();
	        Field Libpaths = c.getDeclaredField("nativeLibraryDirectories");
	        Libpaths.setAccessible(true);
	        
	        File[] nativepaths = (File[])Libpaths.get(paths);        
	        File[] tmp = new File[nativepaths.length+1];     
	        System.arraycopy(nativepaths,0,tmp,0,nativepaths.length);     
	        tmp[nativepaths.length] = new File(pname);     
	        Libpaths.set(paths, tmp);

		}catch (Exception e) {
			e.printStackTrace();
		}				
	}
	
	public boolean HackSystemLow3(String pname) //even older
	{
		boolean bret = true;
		Field fieldSysPath;
		
		try{
			fieldSysPath = DexClassLoader.class.getDeclaredField("mLibPaths");  
	        fieldSysPath.setAccessible(true);
	        
	        String[] paths = (String[])fieldSysPath.get(this.getClass().getClassLoader());  
	        String[] tmp= new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);     
	        tmp[paths.length] = pname;
	        fieldSysPath.set(this.getClass().getClassLoader(), tmp);

		}catch (Exception e) {
			e.printStackTrace();
			bret = false;
		}
		return bret;
	}	
	
	public boolean HackSystemLow2(String pname)  //for 2.2
	{
		boolean bret = true;
		try{
			Field fieldSysPath = PathClassLoader.class.getDeclaredField("mLibPaths");  
	        fieldSysPath.setAccessible(true);
	        
	        String[] paths = (String[])fieldSysPath.get(this.getClass().getClassLoader());  
	        String[] tmp= new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);     
	        tmp[paths.length] = pname;
	        fieldSysPath.set(this.getClass().getClassLoader(), tmp);

		}catch (Exception e) {
			e.printStackTrace();
			bret = false;
		}
		return bret;
	}	
	
	public boolean HackSystemLow1(String pname)  //for 2.3
	{
		boolean bret = true;
		try{
			Field fieldSysPath = PathClassLoader.class.getDeclaredField("libraryPathElements");  
	        fieldSysPath.setAccessible(true);
	        
	        List<String> paths = (List<String>)fieldSysPath.get(this.getClass().getClassLoader());  
	        paths.add(pname);
	        //fieldSysPath.set(paths, paths);

		}catch (Exception e) {
			e.printStackTrace();
			bret = false;
		}
		return bret;
	}	

	public int getIdByName(Context context, String className, String name) {
		String packageName = context.getPackageName();
		Class r = null;
		int id = 0;
		try {
			r = Class.forName(packageName + ".R");

			Class[] classes = r.getClasses();
			Class desireClass = null;

			for (int i = 0; i < classes.length; ++i) {
				if (classes[i].getName().split("\\$")[1].equals(className)) {
					desireClass = classes[i];
					break;
				}
			}

			if (desireClass != null)
				id = desireClass.getField(name).getInt(desireClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}

		return id;
	}
}
