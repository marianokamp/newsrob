package com.newsrob.storage;

import java.io.File;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.newsrob.PL;

public class SdCardStorageAdapter extends AbstractStorageAdapter {
    private static final String TAG = SdCardStorageAdapter.class.getName();
    private static final String BASE_EXTERNAL_OLD = Environment.getExternalStorageDirectory().getPath() + "/newsrob";
    private static File BASE_DIR = new File(BASE_EXTERNAL_OLD);

    private boolean readOnly;
    private boolean mounted;
    private boolean shared;

    BroadcastReceiver myBroadcastReceiver;

    public SdCardStorageAdapter(Context ctx) {
        this(ctx, true);
    }

    public SdCardStorageAdapter(Context ctx, boolean registerForMediaChanges) {
        super(ctx);

        // Checking for old style /mnt/sdcard/newsrob directory to be present
        // April 2012

        if (!BASE_DIR.exists())
            if (ctx.getExternalFilesDir(null) != null)
                BASE_DIR = ctx.getExternalFilesDir(null);

        setupMedium();

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentFilter.addDataScheme("file");

        if (registerForMediaChanges) {
            myBroadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "onReceiver called.");
                    setupMedium();
                }
            };

            getContext().registerReceiver(myBroadcastReceiver, intentFilter);
        }
    }

    private void setupMedium() {
        boolean oldMounted = mounted;
        boolean oldReadOnly = readOnly;
        boolean oldShared = shared;

        String status = Environment.getExternalStorageState();

        mounted = Environment.MEDIA_MOUNTED.equals(status) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status) ? true
                : false;

        readOnly = Environment.MEDIA_MOUNTED_READ_ONLY.equals(status) ? true : false;

        shared = Environment.MEDIA_MOUNTED_READ_ONLY.equals(status) ? true : false;

        Log.d(TAG, String.format("setupMedium status=%s mounted %s->%s, read-only %s->%s, shared %s -> %s.", status,
                oldMounted, mounted, oldReadOnly, readOnly, oldShared, shared));

        if (oldMounted != mounted || oldReadOnly != readOnly || oldShared != shared)
            if (canWrite())
                setupDirs();

    }

    public static boolean isAdvisable() {
        String status = Environment.getExternalStorageState();

        boolean mounted = Environment.MEDIA_MOUNTED.equals(status)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status) ? true : false;

        boolean readOnly = Environment.MEDIA_MOUNTED_READ_ONLY.equals(status) ? true : false;

        boolean shared = Environment.MEDIA_SHARED.equals(status) ? true : false;

        if (mounted && !readOnly && !shared) {
            try {
                StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
                return statFs.getAvailableBlocks() / 1024.0f / 1024.0f * statFs.getBlockSize() > 10;
            } catch (IllegalArgumentException iae) {
                Log.e(TAG, "IAE thrown when using statFS.", iae);
                return false;
            }
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        if (myBroadcastReceiver != null)
            getContext().unregisterReceiver(myBroadcastReceiver);
        super.finalize();
    }

    private void setupDirs() {
        // create Dir
        File f = BASE_DIR;

        if (f == null) {
            BASE_DIR = new File(BASE_EXTERNAL_OLD);
            f = BASE_DIR;
        }

        if (f == null) {
            PL.log("Problem getting storage directory.", getContext());
            return;
        }

        if (!f.exists())
            f.mkdirs();

        // create .nomedia
        File nomedia = new File(f, ".nomedia");
        if (!nomedia.exists())
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                PL.log("Problem when creating .nomedia.", e, getContext());
            }
    }

    public String getAbsolutePathForAsset(String fileName) {
        return new File(BASE_DIR, fileName).getPath();
    }

    public float megaBytesFree() {
        if (!mounted)
            return 0;
        StatFs statFs = new StatFs(BASE_DIR.getAbsolutePath());
        return statFs.getAvailableBlocks() / 1024.0f / 1024.0f * statFs.getBlockSize();
    }

    public boolean canWrite() {
        return mounted && !readOnly && !shared;
    }

    @Override
    File getBaseDir() {
        File f = BASE_DIR;
        if (!f.exists())
            f.mkdirs();
        return f;
    }

    @Override
    public int megaBytesThreshold() {
        return 100;
    }

}
