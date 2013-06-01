package com.newsrob.util;

import android.content.Context;

import com.newsrob.NewsRob;
import com.newsrob.PL;

public class Timing {
    private String title;
    private long started;
    private static final String TAG = Timing.class.getSimpleName();
    private Context context;

    public Timing(String title, Context context) {
        this.title = title;
        this.context = context.getApplicationContext();
        this.started = System.currentTimeMillis();
    }

    public void stop() {
        stop(null);
    }

    public void stop(String message) {
        if (message == null)
            message = title;
        long elapsed = System.currentTimeMillis() - started;
        if (NewsRob.isDebuggingEnabled(context) || elapsed > 500)
            PL.log(TAG + ": " + message + " took " + elapsed + " ms.", context);
    }
}