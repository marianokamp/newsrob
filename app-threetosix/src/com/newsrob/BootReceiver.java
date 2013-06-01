package com.newsrob;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.newsrob.threetosix.R;

public class BootReceiver extends BroadcastReceiver {

    // private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context c, Intent intent) {

        Context applicationContext = c.getApplicationContext();

        EntryManager entryManager = EntryManager.getInstance(applicationContext);

        NewsRobScheduler scheduler = entryManager.getScheduler();
        PL.log("NewsRob BootReceiver: Scheduling background synchronization.", c);
        scheduler.adjustBackgroundSchedule(-1, false);

    }
}
