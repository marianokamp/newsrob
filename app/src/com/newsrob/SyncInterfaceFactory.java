package com.newsrob;

import android.content.Context;

public class SyncInterfaceFactory {
    public static SyncInterface getSyncInterface(Context context) {
        return new EntriesRetriever(context);
    }
}
