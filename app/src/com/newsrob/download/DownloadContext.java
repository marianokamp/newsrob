package com.newsrob.download;

import java.util.HashSet;

import android.util.Log;

public class DownloadContext {

    // Tracking FAZ.net and the like to prevent accessing them a second
    // time.
    private HashSet<String> timedOutHosts = new HashSet<String>();

    public boolean containsTimedOutHost(String timedOutHost) {
        return timedOutHosts.contains(timedOutHost);
    }

    public void addTimedOutHost(String timedOutHost) {
        timedOutHosts.add(timedOutHost);
        Log.d(DownloadContext.class.getSimpleName(), "Added " + timedOutHost + " as a timed out host. Timed out hosts="
                + timedOutHosts);
    }

}
