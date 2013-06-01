package com.newsrob;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.newsrob.storage.IStorageAdapter;

// LATER ... Make the add here too?
public class AssetContentProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.newsrob.assets.threetosix");
    private static final String TAG = AssetContentProvider.class.getName();
    private IStorageAdapter fileContextAdapter;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "---------------- Delete called.");
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, "-------------- getType called.");
        return "text/html";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "---------------- insert called.");
        return null;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate()");
        fileContextAdapter = EntryManager.getInstance(getContext()).getStorageAdapter();
        return true;
    }

    @Override
    public Cursor query(final Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "-------------- query called.");

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "-------------- update called.");
        return 0;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String path = fileContextAdapter.getAbsolutePathForAsset(uri.getPath());
        File f = new File(path + "nr");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

}
