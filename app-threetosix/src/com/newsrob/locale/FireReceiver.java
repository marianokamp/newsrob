package com.newsrob.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.newsrob.EntryManager;
import com.newsrob.NewsRob;
import com.newsrob.PL;

public final class FireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        EntryManager entryManager = EntryManager.getInstance(context);
        if ("com.newsrob.CANCEL_SYNC".equals(intent.getAction())) {
            entryManager.cancel();
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("External triggered cancel.", context);
        } else {
            entryManager.requestSynchronization(false);
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("External triggered refresh.", context);

        }
    }

}