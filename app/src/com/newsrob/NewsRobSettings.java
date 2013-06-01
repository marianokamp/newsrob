package com.newsrob;

import android.content.SharedPreferences;

public class NewsRobSettings {

    private EntryManager entryManager;
    private SharedPreferences sharedPreferences;

    public NewsRobSettings(EntryManager entryManager, SharedPreferences sharedPreferences) {
        this.entryManager = entryManager;
        this.sharedPreferences = sharedPreferences;
    }

    public boolean shouldSlowDownDownloadsWhenScreenIsOn() {
        return true;
    }

    public int getStorageCapacity() {
        return Integer.parseInt(entryManager.sharedPreferences.getString(EntryManager.SETTINGS_ENTRY_MANAGER_CAPACITY, "100"));
    
    }

}
