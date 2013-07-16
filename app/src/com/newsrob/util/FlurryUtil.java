package com.newsrob.util;

import android.app.Activity;
import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.newsrob.EntryManager;

public class FlurryUtil {

    public static final String FLURRY_KEY = "KV4YCWYG3HW34D652GSC";
    public static final String FLURRY_KEY_PRO = "KV4YCWYG3HW34D652GSC";

    private static EntryManager entryManager;

    public static void onStart(Activity owningActivity) {
        if (getEntryManager(owningActivity).isUsageDataCollectionPermitted()) {
            try {
                FlurryAgent.setContinueSessionMillis(10 * 60 * 1000);
                String flurryKey = getEntryManager(owningActivity).isProVersion() ? FLURRY_KEY_PRO : FLURRY_KEY;
                FlurryAgent.onStartSession(owningActivity, flurryKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void onStop(Activity owningActivity) {

        if (getEntryManager(owningActivity).isUsageDataCollectionPermitted()) {
            try {
                FlurryAgent.onEndSession(owningActivity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final EntryManager getEntryManager(Context ctx) {
        synchronized (FlurryUtil.class) {
            if (entryManager == null)
                entryManager = EntryManager.getInstance(ctx);
        }
        return entryManager;
    }
}
