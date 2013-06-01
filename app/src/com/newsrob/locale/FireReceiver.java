package com.newsrob.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.newsrob.EntryManager;
import com.newsrob.PL;

public final class FireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        EntryManager entryManager = EntryManager.getInstance(context);
        PL.log("FireReceiver with intent action: " + intent.getAction(), context);
        if ("com.newsrob.CANCEL_SYNC".equals(intent.getAction())) {
            entryManager.cancel();
            PL.log("Externally triggered cancel.", context);
        } else if ("com.newsrob.UP_SYNC".equals(intent.getAction())) {
            entryManager.requestSynchronization(true);
            PL.log("Externally triggered refresh (up sync only).", context);
        } else {
            entryManager.requestSynchronization(false);
            PL.log("Externally triggered refresh (full).", context);

        }
    }

}