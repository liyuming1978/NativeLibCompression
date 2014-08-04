<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">NativeLibCompression<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">====================<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">There are lots of NDK apps on Google software market. To
reduce package size, some ISV will only release Separate APK. A native library
compression sdk is given to solve the apk size problem. It is easy to integrate
and will get max 50% size decreasing. Beside sdk, a Java tool for package is
provided to convert normal apk to compressed apk.<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">&nbsp;</span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><b><span lang="EN-US" style="font-family: Arial, sans-serif;">HOW TO USE IT:</span></b><span lang="EN-US" style="font-family: Arial, sans-serif;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">1.Include DecRawso into your project (if you use ant,
please copy&nbsp;<b>DecRawso_Jar&nbsp;</b>to your project , and add the
Decrawso.jar, do not use the jar in the sdk bin folder)<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">2.Call DecRawso.NewInstance&nbsp;<b>before any native
library loading!!!</b><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">3.Replace all system.loadlibrary(***) to
system.load(DecRawso . GetInstance ().GetPath(***))<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; now, it is
recommend to change to system.load, but system.loadlibrary also work.<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">--- build your apk as usual, and run your apk as usual
when in your development, the apk is not compressed.<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">&nbsp;</span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><b><span lang="EN-US" style="font-family: Arial, sans-serif;">HOW TO COMPRESS THE APK: -- Use compress tool :
ApkLibCompress/bin/ ComPressApk.jar</span></b><span lang="EN-US" style="font-family: Arial, sans-serif;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">1.You can use it as:&nbsp;<b>&nbsp;ComPressApk.jar -a
C:/my/test.apk -k c:/key storepass keypass alias [your keyname] -x86
http://www.test.com</b><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">2.if “-k” is missing, eclipse default test key will be
used to sign this apk.&nbsp;<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">3.[you keyname] is optional, if not have it. the
defalt&nbsp;<b>CERT&nbsp;</b>will be used<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">4.If -x86 with link is used, then x86 library will be
stored on http://www.test.com/cloudrawso_x86, &nbsp; you must store the lib on
the network bu manuanlly.<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span lang="EN-US" style="font-family: Arial, sans-serif;">5.you can put&nbsp;<b>arm lib on x86 folder&nbsp;</b>to
avoid library miss on x86 devices, use -<b>nox86check&nbsp;</b>to forbidden the
check (x86 directly cal arm lib is&nbsp;<b>unsafed</b>)<o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">6.you can copy all of "<b>DecRawso_Jar</b>"
into your project if you use "ant" to package your project</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">7.<strong>new flag</strong>:&nbsp;</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">&nbsp; -o outputfilename&nbsp;&nbsp;&nbsp;&nbsp; define
the finaly output file name</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">&nbsp;&nbsp;&nbsp; -slience &nbsp; &nbsp; &nbsp; &nbsp;
&nbsp; &nbsp; no popup window, that is suitable for ant package</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">&nbsp;&nbsp;&nbsp; -nosign &nbsp; &nbsp; &nbsp; &nbsp;
&nbsp; &nbsp; do not sign the apk, that is suitable for ant package , due to
the ant will sign apk</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="text-indent: 14.25pt; line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">-nox86check&nbsp;&nbsp; &nbsp; &nbsp;do not check x86 library missing and mix use of arm
issue (x86 directly call arm library is forbidden default)</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="text-indent: 14.25pt; line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">-noarm &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; do
not compress arm lib. just put x86 lib on the cloud (with –x86)</span><span style="font-family: Arial, sans-serif; line-height: 170%;"><o:p></o:p></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;"><strong>8.how to know the result (when you use ant)</strong></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="line-height: 170%;"><span style="line-height: 100%; font-family: Arial, sans-serif;">&nbsp; &nbsp; now will create 3 files in the
ApkLibCompress.jar folder</span><span style="line-height: 100%; font-family: Arial, sans-serif;"><o:p></o:p></span></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="line-height: 170%;"><span style="line-height: 100%; font-family: Arial, sans-serif;">&nbsp;&nbsp;&nbsp; :Done.flag &nbsp;&nbsp;&nbsp;&nbsp; you can check whether the file is exist , if
exist , then&nbsp;<b>compression is ok</b></span><span style="line-height: 100%; font-family: Arial, sans-serif;"><o:p></o:p></span></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="line-height: 170%;"><span style="line-height: 100%; font-family: Arial, sans-serif;">&nbsp;&nbsp;&nbsp; :error.log &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; if generation fail, the log will has the
reason</span><span style="line-height: 100%; font-family: Arial, sans-serif;"><o:p></o:p></span></span></p>

<p class="MsoNormal" align="left" style="line-height: 17.85pt;"><span style="font-family: Arial, sans-serif; line-height: 170%;">&nbsp;&nbsp;&nbsp; :porting.log&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; it will show the x86/arm mix using or x86
lib missing issue</span></p>