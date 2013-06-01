package com.newsrob.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

import android.content.Context;

abstract class AbstractStorageAdapter implements IStorageAdapter {
    private Context ctx;

    AbstractStorageAdapter(Context ctx) {
        this.ctx = ctx;
    }

    Context getContext() {
        return ctx;
    }

    abstract File getBaseDir();

    private File[] getAllAssetFiles(String longAtomId) {
        final String atomId = longAtomIdToShortAtomId(longAtomId);

        return getBaseDir().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.startsWith(atomId);
            }
        });
    }

    public void clear() {
        File[] filesToClear = getBaseDir().listFiles();

        if (filesToClear == null)
            return;

        for (File file : filesToClear) {
            if (!file.getName().endsWith(".settings"))
                deleteFile(file);
        }
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                deleteFile(f);
        }
        file.delete();
    }

    public OutputStream openFileOutput(String fileName) throws FileNotFoundException {
        File f = new File(getBaseDir(), fileName);
        File parent = f.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        return new FileOutputStream(f);
    }

    public int removeAllAssets(String atomId) {
        int noOfDeletedAssets = 0;
        File[] allFiles2delete = getAllAssetFiles(atomId);
        if (allFiles2delete == null)
            return 0;

        for (File file : allFiles2delete) {
            deleteFile(file);
            noOfDeletedAssets++;
        }

        return noOfDeletedAssets;
    }

    private static final String longAtomIdToShortAtomId(String longAtomId) {
        return "a" + longAtomId.substring(longAtomId.lastIndexOf("/") + 1).replace('/', '_');
    }

}