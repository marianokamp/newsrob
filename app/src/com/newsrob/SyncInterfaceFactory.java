package com.newsrob;

import java.lang.reflect.Constructor;

import android.content.Context;
import android.util.Log;

public class SyncInterfaceFactory {
    private static final String TAG = SyncInterfaceFactory.class.getName();

    public static SyncInterface getSyncInterface(Context context) {
        String className = NewsRob.getDebugProperties(context).getProperty("syncClassName",
                "com.newsrob.EntriesRetriever");

        Log.d(TAG, "Attempting to load sync class: " + className);

        try {
            Class<?> syncClass = Class.forName(className);
            Log.d(TAG, "Class loaded: " + syncClass.toString());
            Constructor<Context> constructor = (Constructor<Context>) syncClass.getConstructor(Context.class);
            Log.d(TAG, "Constructor loaded: " + constructor.toString());
            SyncInterface obj = (SyncInterface) constructor.newInstance(context);
            Log.d(TAG, "SyncInterface loaded: " + obj.toString());
            return obj;
        } catch (Exception e) {
            return new EntriesRetriever(context);
        }
    }
}
