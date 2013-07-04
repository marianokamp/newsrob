package com.newsrob.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;

import com.newsrob.PL;

public abstract class AbstractStorageAdapter implements IStorageAdapter {
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

        if (f.exists()) {
            PL.log("Filename: " + f.getAbsolutePath() + " is being overwritten. This is probably not good.", ctx);
        }

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

    public static final String longAtomIdToShortAtomId(String longAtomId) {
        return md5(longAtomId);
    }

    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}