package com.newsrob.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;

import com.newsrob.PL;

public class SingleValueStore {

    private Context ctx;
    private Map<String, File> files = new HashMap<String, File>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Map<String, String> data = new HashMap<String, String>();

    public SingleValueStore(Context context) {
        this.ctx = context.getApplicationContext() != null ? context.getApplicationContext() : context;
    }

    public String getString(final String key) {

        String result = data.get(key);

        if (result != null)
            return result;

        File f = getFile(key);

        if (!f.exists())
            return null;

        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(f, "r");
            result = raf.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (raf != null)
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return result;

    }

    public void putString(final String key, final String value) {

        data.put(key, value);
        Runnable saveRunnable = new Runnable() {

            @Override
            public void run() {
                File f = getFile(key);

                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(f, "rw");
                    raf.writeBytes(value + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (raf != null)
                        try {
                            raf.close();
                            data.remove(key);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }

        };

        try {
            executorService.execute(saveRunnable);
        } catch (RejectedExecutionException e) {
            PL
                    .log(
                            "SingleValueStore: Oops cannot execute because of a RejectedExecutionException. Running it on the sending thread instead.",
                            ctx);
            saveRunnable.run();
        }

    }

    public void putLong(final String key, final Long value) {
        putString(key, String.valueOf(value));
    }

    public Long getLong(final String key) {
        return getLong(key, null);
    }

    public Long getLong(final String key, final Long def) {
        String s = getString(key);
        if (s == null)
            return def;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    private File getFile(String key) {
        if (!files.containsKey(key))
            files.put(key, new File(ctx.getFilesDir(), "svs_" + key));
        return files.get(key);
    }

}
