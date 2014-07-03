package compress.unzipapk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

public class ComPressApk {
	private static String ApkFullname;
	private static String[] KeyName;
	private static String x86Link;
	private static String x64Link;
	private static String mips64Link;
	private static String armv7Link;
	private static String arm64Link;
	
	private static String ApkPathname;
	private static String ApkFilename;
	private static String ApkCompressname;
	
	
	private static File makeCloud(String Link,String abi,String ApkZiptmpname,File cloudso,File libfolder) 
	{
		if(Link==null)
			return null;
		if(!cloudso.exists())
			cloudso.mkdirs();
		if(!OsCommand.getInstance().CompressWithLzma(cloudso.getAbsolutePath()+"/cloudrawso_"+abi+". "+libfolder.getAbsolutePath()))
			return null;				
		File tmp = new File(ApkZiptmpname+"/lib/"+abi+"tmp");
		libfolder.renameTo(tmp);
		libfolder.mkdirs();
		try {
			OutputStreamWriter fCloudOut = new OutputStreamWriter(new FileOutputStream(ApkZiptmpname+"/lib/"+abi+"/cloud.txt"));
			fCloudOut.write(Link+"cloudrawso_"+abi);
			fCloudOut.close();
			//TODO add http auto upload
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tmp;
	}
	
	private static void removeLib(File libfolder,String abi,String ApkZiptmpname)
	{
		File[] files;
		if(libfolder.exists())
		{
			files = libfolder.listFiles();
			for(File file:files)
			{
				if(!file.isDirectory())
				{
					if(!OsCommand.getInstance().AndroidAapt("r "+ApkCompressname+" lib/"+abi+"/"+file.getName(),ApkZiptmpname))
						return;					
				}
			}
		}			
	}
	
	public static void main(String[] args) {
		int i;
		String JarPath;
		boolean bOnly = false;
		
		if(args.length<=1 || args[0].compareToIgnoreCase("-h")==0)
		{
			System.out.print("-a [ApkFullname]\n-k [key storepass keypass alias] --if not use it,use testkey\n-x86 [http link]\n" +
					"-arm64 [http link]\n-x86_64 [http link]\n-mips64 [http link]\n note: http link do not include the filename\n" +
					"ex: ComPressApk.jar -a C:/my/test.apk -k c:/key *** ### alias [name] -x86 http://www.test.com");
			JOptionPane.showMessageDialog(null,"-a [ApkFullname]\n-k [key storepass keypass alias] --if not use it,use testkey\n-x86 [http link]\n" +
					"-arm64 [http link]\n-x86_64 [http link]\n-mips64 [http link]\n note: http link do not include the filename\n" +
					"ex: ComPressApk.jar -a C:/my/test.apk -k c:/key *** ### alias [name] -x86 http://www.test.com");
			return;
		}
		KeyName = new String[5];
		ApkFullname  = KeyName[0]= x86Link = x64Link = mips64Link = armv7Link = arm64Link = null;
		
		for(i=0;i<args.length;i++)
		{
			if(args[i].compareToIgnoreCase("-a")==0)
			{
				if(++i<args.length)
					ApkFullname = args[i];
			}	
			else if(args[i].compareToIgnoreCase("-s")==0)
			{
				if(++i<args.length)
					ApkFullname = args[i];
				bOnly = true;
			}	
			else if(args[i].compareToIgnoreCase("-k")==0)
			{
				int c=-1;
				while(++i<args.length && ++c<4)
				{
					KeyName[c] = args[i];
					if(KeyName[c].startsWith("-"))
					{
						JOptionPane.showMessageDialog(null,"key must have at least 4 paramater: key storepass keypass alias [name]");
						return;
					}
				}	
				if(c<3)
				{
					JOptionPane.showMessageDialog(null,"key must have at least 4 paramater: key storepass keypass alias [name]");
					return;				
				}
				
				if(i<args.length && !args[i].startsWith("-"))  //not use default CERT
					KeyName[4] = args[i];
				else
					KeyName[4] = "CERT";
			}	
			else if(args[i].compareToIgnoreCase("-x86")==0)
			{
				if(++i<args.length)
				{
					x86Link = args[i];
					if(!x86Link.endsWith("/"))
						x86Link+="/";
					if(!x86Link.startsWith("http"))
					{
						JOptionPane.showMessageDialog(null,x86Link+" must start with http");
						return;
					}
				}
			}
			else if(args[i].compareToIgnoreCase("-x86_64")==0)
			{
				if(++i<args.length)
				{
					x64Link = args[i];
					if(!x64Link.endsWith("/"))
						x64Link+="/";
					if(!x64Link.startsWith("http"))
					{
						JOptionPane.showMessageDialog(null,x64Link+" must start with http");
						return;
					}
				}
			}			
			else if(args[i].compareToIgnoreCase("-mips64")==0)
			{
				if(++i<args.length)
				{
					mips64Link = args[i];
					if(!mips64Link.endsWith("/"))
						mips64Link+="/";	
					if(!mips64Link.startsWith("http"))
					{
						JOptionPane.showMessageDialog(null,mips64Link+" must start with http");
						return;
					}					
				}
			}
			else if(args[i].compareToIgnoreCase("-arm64")==0)
			{
				if(++i<args.length)
				{
					arm64Link = args[i];
					if(!arm64Link.endsWith("/"))
						arm64Link+="/";	
					if(!arm64Link.startsWith("http"))
					{
						JOptionPane.showMessageDialog(null,arm64Link+" must start with http");
						return;
					}						
				}
			}				
		}
		
		if(ApkFullname==null || !ApkFullname.contains(".apk"))
		{
			JOptionPane.showMessageDialog(null,"Invalid pathname");
			return;
		}
		if(KeyName[0]==null)
		{
			KeyName[0] = System.getProperties().getProperty("user.home")+"/.android/debug.keystore";
			KeyName[1] = KeyName[2] = "android";
			KeyName[3] = "androiddebugkey";
			KeyName[4] = "CERT";
			File testKey = new File(KeyName[0]);
			if(!testKey.exists())
			{
				JOptionPane.showMessageDialog(null,"No testKey in "+KeyName[0]+"\nUse -k in command line or install eclipse");
				return;
			}
		}
		
//--------------------------------------------------------------------------------------------------------------        			
        try {
        	JarPath = URLDecoder.decode(ComPressApk.class.getProtectionDomain()
			        .getCodeSource().getLocation().getFile(), "UTF-8");
        	File jarfile = new File(JarPath);
        	if(!jarfile.isDirectory())
        		JarPath = jarfile.getParent();
        	if(!(JarPath.endsWith("/") ||JarPath.endsWith("\\")))
        		JarPath+="/";
        	//JOptionPane.showMessageDialog(null,JarPath);
        	OsCommand.newInstance(JarPath);
        	if(OsCommand.getInstance()==null)
        	{
        		JOptionPane.showMessageDialog(null,"Error: unsupport system, only for windows,linux and mac");
        		return;
        	}
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			//JOptionPane.showMessageDialog(null,"err jar");
			e1.printStackTrace();
		}		
	
//--------------------------------------------------------------------------------------------------------------        
		int last=ApkFullname.lastIndexOf("/");
		int last1=ApkFullname.lastIndexOf("\\");
		last = last>last1?last:last1;
		last++;
		
		ApkPathname = ApkFullname.substring(0, last);
		ApkFilename = ApkFullname.substring(last, ApkFullname.length());
		if(bOnly)
		{
			//sign the apk
			if(!OsCommand.getInstance().ApkJarSigner(ApkFullname, KeyName))
				return;
			//zipalign
			if(!OsCommand.getInstance().ApkZipAlign(ApkFullname))
				return;		
			JOptionPane.showMessageDialog(null,"Done");
			return;
		}
		
		if(!( ApkPathname.startsWith("/")||ApkPathname.startsWith("~")||ApkPathname.startsWith(":", 1) ))
		{
			ApkPathname = new File("").getAbsolutePath()+"/"+ApkPathname;
		}
		
		try {
			unzip(ApkFullname,ApkPathname);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,"Error: apk unzip error");
			return;
		}
		
		//---------do not change !! danger! dir delete without any alert-----
		String ApkZiptmpname = ApkPathname+ getSuffixName(ApkFilename);  //do not change!
		//---------do not change !! danger! dir delete without any alert-----
		File rawso,cloudso;
		File libx86,x86tmp = null;
		File libx64,x64tmp = null;
		File libarm;
		File libarmv7,armv7tmp = null;
		File libarm64,arm64tmp = null;
		File libmips,mipstmp = null;
		File libmips64,mips64tmp = null;
		//OutputStreamWriter fCloudOut;
		
		if((rawso = new File(ApkZiptmpname+"/assets/rawso.")).exists())
		{
			JOptionPane.showMessageDialog(null,"Error: rawso is exist in assets");
			deleteDir(new File(ApkZiptmpname));
			return;
		}
		
		cloudso = new File(ApkZiptmpname+"_Cloud");
		
		String cmdfolder = "";
		if((libx86=new File(ApkZiptmpname+"/lib/x86")).exists())
		{
			cmdfolder+=" "+libx86.getAbsolutePath();
			x86tmp=makeCloud(x86Link, "x86", ApkZiptmpname, cloudso, libx86);
		}
		if((libx64=new File(ApkZiptmpname+"/lib/x86_64")).exists())
		{
			cmdfolder+=" "+libx64.getAbsolutePath();
			x64tmp=makeCloud(x64Link, "x86_64", ApkZiptmpname, cloudso, libx64);
		}		
		if((libarm=new File(ApkZiptmpname+"/lib/armeabi")).exists())
		{
			cmdfolder+=" "+libarm.getAbsolutePath();
		}
		if((libarmv7=new File(ApkZiptmpname+"/lib/armeabi-v7a")).exists())
		{
			cmdfolder+=" "+libarmv7.getAbsolutePath();
			armv7tmp=makeCloud(armv7Link, "armeabi-v7a", ApkZiptmpname, cloudso, libarmv7);		
		}
		if((libarm64=new File(ApkZiptmpname+"/lib/arm64")).exists())
		{
			cmdfolder+=" "+libarm64.getAbsolutePath();
			arm64tmp=makeCloud(arm64Link, "arm64", ApkZiptmpname, cloudso, libarm64);		
		}		
		if((libmips=new File(ApkZiptmpname+"/lib/mips")).exists())
		{
			cmdfolder+=" "+libmips.getAbsolutePath();
		}
		if((libmips64=new File(ApkZiptmpname+"/lib/mips64")).exists())
		{
			cmdfolder+=" "+libmips64.getAbsolutePath();
			mips64tmp=makeCloud(mips64Link, "mips64", ApkZiptmpname, cloudso, libmips64);			
		}
		
		//LZMA compress
		if(!OsCommand.getInstance().CompressWithLzma(rawso.getAbsolutePath()+cmdfolder))
			return;

		//after compress, restore the tmp
		if(x86Link!=null && x86tmp!=null)
		{
			deleteDir(libx86);
			x86tmp.renameTo(libx86);
		}
		if(x64Link!=null && x64tmp!=null)
		{
			deleteDir(libx64);
			x64tmp.renameTo(libx64);
		}		
		if(armv7Link!=null && armv7tmp!=null)
		{
			deleteDir(libarmv7);
			armv7tmp.renameTo(libarmv7);
		}
		if(arm64Link!=null && arm64tmp!=null)
		{
			deleteDir(libarm64);
			arm64tmp.renameTo(libarm64);
		}		
		if(mips64Link!=null && mips64tmp!=null)
		{
			deleteDir(libmips);
			mips64tmp.renameTo(libmips);
		}	
		
		ApkCompressname = ApkPathname+getSuffixName(ApkFilename)+"_Compress.apk";	
		fileChannelCopy(ApkFullname,ApkCompressname);
		
		//AAPT:add assets/rawso and delete .so in libs
		if(!OsCommand.getInstance().AndroidAapt("a "+ApkCompressname+" assets/rawso",ApkZiptmpname))
			return;
		
		removeLib(libx86,"x86",ApkZiptmpname);
		removeLib(libx64,"x86_64",ApkZiptmpname);
		removeLib(libarm,"armeabi",ApkZiptmpname);
		removeLib(libarmv7,"armeabi-v7a",ApkZiptmpname);
		removeLib(libarm64,"arm64",ApkZiptmpname);
		removeLib(libmips,"mips",ApkZiptmpname);
		removeLib(libmips64,"mips64",ApkZiptmpname);
		
		File MetaDir = new File(ApkZiptmpname+"/META-INF");
		if(MetaDir.exists())
		{
			File[] files = MetaDir.listFiles();
			for(File file:files)
			{
				//AAPT:del META-INF
				if(!OsCommand.getInstance().AndroidAapt("r "+ApkCompressname+" META-INF/"+file.getName(),ApkZiptmpname))
					return;		
			}
		}
		
		//sign the apk
		if(!OsCommand.getInstance().ApkJarSigner(ApkCompressname, KeyName))
			return;
		//zipalign
		if(!OsCommand.getInstance().ApkZipAlign(ApkCompressname))
			return;		
		//--delete tmp
		deleteDir(new File(ApkZiptmpname));
		JOptionPane.showMessageDialog(null,"Done");
	}

	private static String getSuffixName(final String name) {
		return name.substring(0, name.lastIndexOf("."));
	}
	
    private static boolean deleteDir(File dir) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so now it can be smoked
        return dir.delete();
    }	

	public static void unzip(final String zipFilePath,
			final String unzipDirectory) throws Exception {
		// 
		final File file = new File(zipFilePath);
		// 
		final ZipFile zipFile = new ZipFile(file);
		// 
		final File unzipFile = new File(unzipDirectory + getSuffixName(file.getName()));
		if (unzipFile.exists())
			deleteDir(unzipFile);
		unzipFile.mkdir();
		
		// 
		final Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
		// 
		InputStream input = null;
		OutputStream output = null;
		// 
		//System.out
		//		.println("name\t\t\tsize\t\t\tcompressedSize\t\t\tisDirectory");
		while (zipEnum.hasMoreElements()) {
			// 
			final ZipEntry entry = (ZipEntry) zipEnum.nextElement();
			final String entryName = new String(entry.getName().getBytes(
					"ISO8859_1"));
			//System.out.println(entryName + "\t\t\t" + entry.getSize()
			//		+ "\t\t\t" + entry.getCompressedSize() + "\t\t\t\t\t\t\t"
			//		+ entry.isDirectory());
			
			if(!(entryName.startsWith("lib")|| entryName.startsWith("META-INF") || entryName.compareTo("assets/rawso")==0))
				continue;
			if(entryName.contains("libDecRawso.so")||entryName.contains("libDecRawso22.so"))
				continue;


			File Fout= new File(unzipFile.getAbsolutePath() + "/" + entryName);
			if(!Fout.exists()){  
				(new File(Fout.getParent())).mkdirs();  
            }  

			if(!entry.isDirectory())  //liyuming mark: here if dir , can not copy
			{ //
				input = zipFile.getInputStream(entry);
				output = new FileOutputStream(Fout);
				final byte[] buffer = new byte[1024 * 8];
				int readLen = 0;
				while ((readLen = input.read(buffer, 0, 1024 * 8)) != -1)
					output.write(buffer, 0, readLen);
				input.close();
				output.flush();
				output.close();
			}
		}
		zipFile.close();
	}
	
	public static void fileChannelCopy(String s, String t) {
		FileInputStream fi = null;
		FileOutputStream fo = null;
		FileChannel in = null;
		FileChannel out = null;
		try {
			fi = new FileInputStream(s);
			fo = new FileOutputStream(t);
			in = fi.getChannel();//
			out = fo.getChannel();//
			in.transferTo(0, in.size(), out);//
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				fi.close();
				in.close();
				fo.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	
}
