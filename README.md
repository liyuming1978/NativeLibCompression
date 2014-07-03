NativeLibCompression
====================

There are lots of NDK apps on Google software market. To reduce package size, some ISV will only release Separate APK. A native library compression sdk is given to solve the apk size problem. It is easy to integrate and will get max 50% size decreasing. Beside sdk, a Java tool for package is provided to convert normal apk to compressed apk.

HOW TO USE IT:

1.Include DecRawso into your project

2.Call DecRawso.NewInstance before native library loading

3.Replace all system.loadlibrary(***) to system.load(DecRawso . GetInstance ().GetPath(***))

--- build your apk as usual, and run your apk as usual when in your development, the apk is not compressed.


HOW TO COMPRESS THE APK: -- Use compress tool : ApkLibCompress	

1.You can use it as:  ComPressApk.jar -a C:/my/test.apk -k c:/key storepass keypass alias [your keyname] -x86 http://www.test.com

2.if “-k” is missing, eclipse default test key will be used to sign this apk. 

3.[you keyname] is optional, if not have it. the defalt CERT will be used

4.If -x86 with link is used, then x86 library will be stored on http://www.test.com/cloudrawso_x86,   you must store the lib on the network bu manuanlly.

5.you can put arm lib on x86 folder to avoid library miss on x86 devices


