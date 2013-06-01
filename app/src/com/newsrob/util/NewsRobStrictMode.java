package com.newsrob.util;

import android.os.StrictMode;

public class NewsRobStrictMode {

    public static void enableStrictMode() {

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectNetwork(). // or

                detectDiskWrites().penaltyLog().build());
        if (false)
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable
                    // problems
                    .penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().build());

    }
}
