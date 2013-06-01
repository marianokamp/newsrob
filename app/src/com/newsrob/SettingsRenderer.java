package com.newsrob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.view.Display;
import android.view.WindowManager;

import com.newsrob.jobs.Job;
import com.newsrob.storage.IStorageAdapter;
import com.newsrob.util.Timing;

public class SettingsRenderer {

    public static StringBuilder renderSettings(EntryManager entryManager, StringBuilder sb) {

        // final String customROM = getCustomRomVersion();

        sb.append("-- Time: " + new Date() + "\n");
        sb.append(String.format("-- Android Version: sdk=%s, release=%s, inc=%s\n", Build.VERSION.SDK,
                Build.VERSION.RELEASE, Build.VERSION.INCREMENTAL));
        final Runtime rt = Runtime.getRuntime();
        sb.append(String.format("-- Memory free: %4.2fMB total: %4.2fMB max: %4.2fMB\n",
                rt.freeMemory() / 1024 / 1024.0, rt.totalMemory() / 1024 / 1024.0, rt.maxMemory() / 1024 / 1024.0));
        // sb.append("-- Custom ROM: " + customROM + "\n");
        sb.append("-- BuildD: " + Build.DISPLAY + " \n");
        sb.append("-- Manufacturer: " + Build.MANUFACTURER + " \n");

        sb.append("-- Device: " + Build.DEVICE + "\n");
        sb.append("-- Model: " + Build.MODEL + "\n");
        // message.append("-- Manufacturer: " + Build.MANUFACTURER +
        // "\n");

        sb.append(String.format("-- NewsRob Version: %s/%s\n", getMyVersionName(entryManager.getContext()),
                getMyVersionCode(entryManager.getContext())));

        final Job j = entryManager.getCurrentRunningJob();
        if (j != null)
            sb.append(String.format("-- Job: %s\n", j.getJobDescription()));

        if (entryManager.runningThread != null)
            sb.append(String.format("-- Running Thread: %s\n", entryManager.runningThread));

        sb.append("-- Configured Capacity: " + entryManager.getNewsRobSettings().getStorageCapacity() + "\n");
        sb.append("--        Keep Starred: " + entryManager.getNoOfStarredArticlesToKeep() + "\n");
        sb.append("--        Keep Shared: " + entryManager.getNoOfSharedArticlesToKeep() + "\n");
        sb.append("--        Keep Notes: " + entryManager.getNoOfNotesToKeep() + "\n");

        sb.append("-- Current Article Count: " + entryManager.getArticleCount() + "\n");
        if (NewsRob.isDebuggingEnabled(entryManager.getContext())) {
            Timing t = new Timing("Comprehensive article counting", entryManager.getContext());
            try {
                sb.append("--   Article Count:" + entryManager.getArticleCount() + "\n");
                sb.append("--   Article Unread Count:" + entryManager.getUnreadArticleCountExcludingPinned() + "\n");
                sb.append("--   Article Read Count:" + entryManager.getReadArticleCount() + "\n");
                sb.append("--   Article Pinned Count:" + entryManager.getPinnedArticleCount() + "\n");
                sb.append("--   Article Starred Count:" + entryManager.getStarredArticleCount() + "\n");
                sb.append("--   Article Shared Count:" + entryManager.getSharedArticleCount() + "\n");
                sb.append("--   Article Notes Count:" + entryManager.getNotesCount() + "\n");
                sb.append("--   Article Changed Count:" + entryManager.getChangedArticleCount() + "\n");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                t.stop();
            }

        }
        sb.append("-- Action Bar Location: " + entryManager.getActionBarLocation() + "\n");

        sb.append("-- Days installed: " + entryManager.getDaysInstalled() + "\n");
        sb.append("-- Orientation: " + getOrientation(entryManager.getContext()) + "\n");

        try {
            IStorageAdapter storageAdapter = entryManager.getStorageAdapter();
            sb.append("-- StorageAdapter: " + storageAdapter.getClass().getSimpleName() + "\n");
            sb.append("--   MB available: " + Math.round(storageAdapter.megaBytesFree()) + "\n");

            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            float mbFree = Math.round(statFs.getAvailableBlocks() / 1024.0f / 1024.0f * statFs.getBlockSize());
            sb.append("-- Phone Memory MB available: " + mbFree + "\n");

        } catch (RuntimeException re) {
            // ignored
        }

        sb.append("-- Sync Auto enabled: " + entryManager.isAutoSyncEnabled() + "\n");
        sb.append("--      Unread Only: " + entryManager.shouldOnlyUnreadArticlesBeDownloaded() + "\n");
        sb.append("--      Delete Read: " + entryManager.shouldReadItemsBeDeleted() + "\n");
        sb.append("--      NewsRob only: " + entryManager.isNewsRobOnlySyncingEnabled() + "\n");
        sb.append("--      Offline use: "
                + entryManager.getSharedPreferences().getString(EntryManager.SETTINGS_STORAGE_ASSET_DOWNLOAD, "unset")
                + "\n");
        sb.append("--      Offline Def: " + entryManager.getDefaultDownloadPref() + "\n");
        sb.append("-- FireModelUpdate in progress: " + NewsRob.fireModelUpdateInProgress + "\n");

        return sb;

    }

    private static String getOrientation(Context ctx) {
        try {
            Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            return "" + display.getOrientation();
        } catch (Throwable t) {
            return "";
        }

    }

    private static String getProperty(final String name) {
        final StringBuffer rv = new StringBuffer();
        Process process = null;
        BufferedReader br = null;
        try {
            process = Runtime.getRuntime().exec("getprop " + name);
            br = new BufferedReader(new InputStreamReader(process.getInputStream()), 512);
            String line;
            while ((line = br.readLine()) != null)
                rv.append(line);
            br.close();
            process.destroy();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                }
            if (process != null)
                process.destroy();
        }
        final String s = rv.toString().trim();
        if (s.length() > 0)
            return s;
        return "Stock Android";
    }

    public static int getMyVersionCode(Context ctx) {
        PackageInfo packageInfo;
        try {
            packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            throw new IllegalStateException(ctx.getPackageName() + " was not found when quering the Package Manager.",
                    e);
        }
        return packageInfo.versionCode;

    }

    public static String getMyVersionName(Context ctx) {
        PackageInfo packageInfo;
        try {
            packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            throw new IllegalStateException(ctx.getPackageName() + " was not found when quering the Package Manager.",
                    e);
        }
        final int checkSignature = ctx.getPackageManager()
                .checkSignatures("com.newsrob", EntryManager.PRO_PACKAGE_NAME);
        boolean proVersion = checkSignature == PackageManager.SIGNATURE_MATCH
                || checkSignature == PackageManager.SIGNATURE_NEITHER_SIGNED;
        return packageInfo.versionName + (proVersion ? " Pro" : "");
    }

    public static String getCustomRomVersion() {
        return getProperty("ro.modversion");
    }

}
