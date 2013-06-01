package com.newsrob.auth;

import android.content.Context;
import android.os.Build;

public class AccountManagementUtils {

    private static IAccountManagementUtils instance;
    private static boolean initialized;

    public static IAccountManagementUtils getAccountManagementUtils(Context context) {
        if (!initialized) {
            try {
                boolean sdk7Plus = false;
                try {
                    int sdkLevel = Build.VERSION.class.getDeclaredField("SDK_INT").getInt(Build.VERSION.class);
                    sdk7Plus = sdkLevel >= 7;
                } catch (Exception e) {
                    // 
                }

                if (sdk7Plus) {
                    Class.forName("android.accounts.Account"); // API there?
                    Class c = Class.forName("com.newsrob.auth.AccountManagementUtilsImplementation");

                    if (c != null) {
                        IAccountManagementUtils amu = (IAccountManagementUtils) c.newInstance();
                        if (amu.supportsGoogleAuth(context))
                            instance = amu;
                    }
                }
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (IllegalAccessException e) {
                // ignore
            } catch (InstantiationException e) {
                // ignore
            } finally {
                initialized = true;
            }
        }
        return instance;
    }
}
