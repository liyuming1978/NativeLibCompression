/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.content;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Slog;

import java.io.File;

/**
 * Native libraries helper.
 *
 * @hide
 */
public class NativeLibraryHelper {
    private static final String TAG = "NativeHelper";

    private static final boolean DEBUG_NATIVE = false;
    private static final boolean ENABLE_HOUDINI = Build.CPU_ABI.equals("x86") && (Build.CPU_ABI2.length()!=0);

    private static native long nativeSumNativeBinaries(String file, String cpuAbi, String cpuAbi2);

    /**
     * Sums the size of native binaries in an APK.
     *
     * @param apkFile APK file to scan for native libraries
     * @return size of all native binary files in bytes
     */
    public static long sumNativeBinariesLI(File apkFile) {
        final String cpuAbi = Build.CPU_ABI;
        final String cpuAbi2 = Build.CPU_ABI2;

        if (ENABLE_HOUDINI) {
            long result = nativeSumNativeBinaries(apkFile.getPath(), cpuAbi, cpuAbi2);
            if (result == 0) {
                final String abiUpgrade = SystemProperties.get("ro.product.cpu.upgradeabi", "armeabi");
                result = nativeSumNativeBinaries(apkFile.getPath(), cpuAbi, abiUpgrade);
            }
            return result;
        } else {
            return nativeSumNativeBinaries(apkFile.getPath(), cpuAbi, cpuAbi2);
        }
    }

    private static native int nativeListNativeBinaries(String file, String cpuAbi, String cpuAbi2);

    /**
     * List the native binaries info in an APK.
     *
     * @param apkFile APK file to scan for native libraries
     * @return {@link PackageManager#INSTALL_SUCCEEDED} or {@link PackageManager#INSTALL_ABI2_SUCCEEDED}
     *         or another error code from that class if not
     */
    public static int listNativeBinariesLI(File apkFile) {
        final String cpuAbi = Build.CPU_ABI;
        final String cpuAbi2 = Build.CPU_ABI2;

        if (ENABLE_HOUDINI) {
            int result = nativeListNativeBinaries(apkFile.getPath(), cpuAbi, cpuAbi2);
            if ((result != PackageManager.INSTALL_SUCCEEDED) && (result != PackageManager.INSTALL_ABI2_SUCCEEDED)) {
                final String abiUpgrade = SystemProperties.get("ro.product.cpu.upgradeabi", "armeabi");
                result = nativeListNativeBinaries(apkFile.getPath(), cpuAbi, abiUpgrade);
            }
            return result;
        } else {
            return PackageManager.INSTALL_SUCCEEDED;
        }
    }


    private native static int nativeCopyNativeBinaries(String filePath, String sharedLibraryPath,
            String cpuAbi, String cpuAbi2);

    /**
     * Copies native binaries to a shared library directory.
     *
     * @param apkFile APK file to scan for native libraries
     * @param sharedLibraryDir directory for libraries to be copied to
     * @return {@link PackageManager#INSTALL_SUCCEEDED} or {@link PackageManager#INSTALL_ABI2_SUCCEEDED}
     *         if successful or another error code from that class if not
     */
    public static int copyNativeBinariesIfNeededLI(File apkFile, File sharedLibraryDir) {
        final String cpuAbi = Build.CPU_ABI;
        final String cpuAbi2 = Build.CPU_ABI2;

        if (ENABLE_HOUDINI) {
            int result = nativeCopyNativeBinaries(apkFile.getPath(), sharedLibraryDir.getPath(), cpuAbi, cpuAbi2);
            if ((result != PackageManager.INSTALL_SUCCEEDED) && (result != PackageManager.INSTALL_ABI2_SUCCEEDED)) {
                final String abiUpgrade = SystemProperties.get("ro.product.cpu.upgradeabi", "armeabi");
                result = nativeCopyNativeBinaries(apkFile.getPath(), sharedLibraryDir.getPath(), cpuAbi, abiUpgrade);
            }
            return result;
        } else {
            return nativeCopyNativeBinaries(apkFile.getPath(), sharedLibraryDir.getPath(), cpuAbi, cpuAbi2);
        }
    }

    // Convenience method to call removeNativeBinariesFromDirLI(File)
    public static boolean removeNativeBinariesLI(String nativeLibraryPath) {
        return removeNativeBinariesFromDirLI(new File(nativeLibraryPath));
    }

    // Remove the native binaries of a given package. This simply
    // gets rid of the files in the 'lib' sub-directory.
    public static boolean removeNativeBinariesFromDirLI(File nativeLibraryDir) {
        if (DEBUG_NATIVE) {
            Slog.w(TAG, "Deleting native binaries from: " + nativeLibraryDir.getPath());
        }

        boolean deletedFiles = false;

        /*
         * Just remove any file in the directory. Since the directory is owned
         * by the 'system' UID, the application is not supposed to have written
         * anything there.
         */
        if (nativeLibraryDir.exists()) {
            final File[] binaries = nativeLibraryDir.listFiles();
            if (binaries != null) {
                for (int nn = 0; nn < binaries.length; nn++) {
                    if (DEBUG_NATIVE) {
                        Slog.d(TAG, "    Deleting " + binaries[nn].getName());
                    }

                    if (!binaries[nn].delete()) {
                        Slog.w(TAG, "Could not delete native binary: " + binaries[nn].getPath());
                    } else {
                        deletedFiles = true;
                    }
                }
            }
            // Do not delete 'lib' directory itself, or this will prevent
            // installation of future updates.
        }

        return deletedFiles;
    }
}
