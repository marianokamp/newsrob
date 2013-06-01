package com.newsrob;

import java.io.File;
import java.io.FileInputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.util.Log;

import com.newsrob.activities.ShowMessageActivity;
import com.newsrob.threetosix.R;
import com.newsrob.util.NewsRobStrictMode;
import com.newsrob.util.SDKVersionUtil;
import com.newsrob.util.U;

public class NewsRob extends Application {

    private static final String TAG = NewsRob.class.getSimpleName();
    public static final boolean SHOW_CHANGED = true;
    private static final String DEBUG_PREFERENCES_FILE = "/sdcard/newsrob.debug";
    final static String HEAP_DUMP_FILE_NAME = "/sdcard/newsrob/newsrob.hprof";
    private static Properties debugProperties;
    private static Boolean debuggingEnabled;

    public static final String CONSTANT_MY_RECENTLY_STARRED = "my recently starred";
    public static final String CONSTANT_FRIENDS_RECENTLY_SHARED = "friends' recently shared";
    public static Activity lastActivity;
    public static boolean fireModelUpdateInProgress;

    private static final boolean STRICT_MODE_ENABLED = false;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "NewsRob.onCreate()");

        installNewsRobDefaultExceptionHandler(this);

        enableStrictMode();

        // remove old heap dump after one week
        final File f = new File(HEAP_DUMP_FILE_NAME);
        if (f.exists() && f.lastModified() < (System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)))
            f.delete();

        // setTheme(getEntryManager().getThemeResourceId());

        // re-schedule syncs after app has been (re-)installed
        final EntryManager entryManager = EntryManager.getInstance(this);
        entryManager.getScheduler().ensureSchedulingIsEnabled();
        entryManager.maintainBootReceiverState();
        entryManager.maintainPremiumDependencies();
        entryManager.maintainFirstInstalledVersion();
        entryManager.migrateSyncType();

    }

    private void enableStrictMode() {
        if (STRICT_MODE_ENABLED && SDKVersionUtil.getVersion() >= 9)
            NewsRobStrictMode.enableStrictMode();

    }

    public static void installNewsRobDefaultExceptionHandler(final NewsRob context) {
        final boolean isNRExceptionHandlerAlreadyInstalled = NewsRobDefaultExceptionHandler.class.getSimpleName()
                .equals(Thread.getDefaultUncaughtExceptionHandler().getClass().getSimpleName());
        Log.d(TAG, "NewsRob Default Exception handler installed already? " + isNRExceptionHandlerAlreadyInstalled);
        if (!isNRExceptionHandlerAlreadyInstalled)
            new NewsRobDefaultExceptionHandler(context);
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "NewsRob.onTerminate()");
        super.onTerminate();
    }

    public static Properties getDebugProperties(Context context) {
        if (debugProperties == null) {
            synchronized (NewsRob.class) {
                debugProperties = new Properties();
                try {
                    debugProperties.load(new FileInputStream(DEBUG_PREFERENCES_FILE));
                    Log.i(TAG, "Debug properties loaded: " + debugProperties.toString());
                } catch (final Exception e) {
                    Log.w(TAG, "No debug properties loaded.");
                }
            }
        }
        return debugProperties;
    }

    static class NewsRobDefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final UncaughtExceptionHandler oldDefaultExceptionHandler;
        private final NewsRob context;
        private static final String TAG = "NewsRobDefaultExceptionHandler";

        NewsRobDefaultExceptionHandler(final NewsRob context) {
            this.context = context;
            Log.d(TAG, "Default Exception Handler=" + Thread.getDefaultUncaughtExceptionHandler());
            oldDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler(this);
            Log.d(TAG, "Installed Exception Handler=" + Thread.getDefaultUncaughtExceptionHandler());

        }

        public void uncaughtException(final Thread t, final Throwable e) {

            Log.e("NewsRob", "Caught the following exception: ", e);

            final StringBuilder message = new StringBuilder(
                    "Sorry!\n\nNewsRob hit a wall. Please send this mail, so that the developer can analyze/fix the issue.\nIf it is not too much to ask, please add to this mail what you just did between the following lines:\n\n-------\n\n\n-------\n");

            SettingsRenderer.renderSettings(EntryManager.getInstance(context), message);

            if (lastActivity != null)
                message.append("-- LastActivity: " + lastActivity.getClass().getSimpleName() + ".\n");

            /*
             * StringBuilder sb = new StringBuilder("-- Networks: ("); try {
             * ConnectivityManager cm = (ConnectivityManager) context
             * .getSystemService(CONNECTIVITY_SERVICE);
             * 
             * for (NetworkInfo ni : cm.getAllNetworkInfo()) { sb.append("\n   "
             * + ni.getTypeName() + ": " + ni.getDetailedState() + " - " +
             * ni.getSubtypeName()); }
             * 
             * 
             * } finally { sb.append(")\n"); message.append(sb); }
             */

            U.renderStackTrace(e, message);

            final String messageBody = message.toString();

            // create heap dump if OOM is in stacktrace
            // boolean heapDumpCreated = createHeapDump(messageBody);

            if (Pattern.compile("OutOfMemoryError").matcher(messageBody).find()) {

                createHeapDump(messageBody);

                final Intent showMessageIntent = new Intent(context, ShowMessageActivity.class);
                showMessageIntent.putExtra("title", "Out Of Memory");
                showMessageIntent.putExtra("body", "" + context.getText(R.string.oom_message));
                showMessageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(showMessageIntent);
                if (oldDefaultExceptionHandler != null)
                    oldDefaultExceptionHandler.uncaughtException(t, e);
                return;
            }

            List<String> ignorePatterns = new ArrayList<String>(10);

            // Flash Player
            ignorePatterns.add("java.lang.UnsatisfiedLinkError: nativeIsOpenGLEnabled");

            // http://code.google.com/p/android/issues/detail?id=10041
            // Data Loader NPE
            ignorePatterns.add("WebViewCore.java:636");
            ignorePatterns.add("WebViewCore.java:629");
            ignorePatterns.add("WebViewCore.java:612");
            ignorePatterns.add("WebViewCore.java:611");
            ignorePatterns.add("WebViewCore.java:622");

            // Ads related exceptions
            ignorePatterns.add("com.google.ads");
            ignorePatterns.add("com.admob");

            // internal WebView issue
            ignorePatterns.add("android.webkit.WebViewDatabase.clearCache");

            // SSL certificate issue
            ignorePatterns.add("no more than 32 elements");

            // LG issue
            ignorePatterns.add("android.permission.WRITE_SETTINGS");

            // ignore a known issue:
            // http://code.google.com/p/android/issues/detail?id=2275
            ignorePatterns.add("CacheManager.java:391");

            // ignore bad token exceptions
            ignorePatterns.add("android.view.WindowManager\\$BadTokenException");

            // ignore WebView 1.6 bug
            ignorePatterns.add("Null or empty value for header");

            // WebView NPE
            ignorePatterns.add("SurfaceView.java:547");

            // Android HTTP Client
            ignorePatterns.add("Connection.java:231");

            // WebView NPE in handleMessage
            // http://code.google.com/p/android/issues/detail?id=9995
            ignorePatterns.add("WebView.java:7590");
            ignorePatterns.add("WebView.java:7577");
            ignorePatterns.add("WebView.java:6384");
            ignorePatterns.add("WebView.java:7779");
            ignorePatterns.add("WebView.java:7612");
            ignorePatterns.add("WebView.java:7850");
            ignorePatterns.add("WebView.java:7617");

            // WebView
            // http://code.google.com/p/android/issues/detail?id=11583
            ignorePatterns.add("BasicHttpRequest.java:57");

            // WebView COMMIT exception in CacheManager.endTransaction()
            ignorePatterns.add("CacheManager.java:276");

            // Android HTTP Client
            ignorePatterns.add("CharArrayBuffer.java:125");

            // 
            ignorePatterns.add("WebViewDatabase.java:736");

            // http://code.google.com/p/android/issues/detail?id=11977
            ignorePatterns.add("WebViewDatabase.java:737");

            // http://code.google.com/p/android/issues/detail?id=12236
            ignorePatterns.add("WebViewCore.java:1218");
            ignorePatterns.add("WebViewCore.java:691");
            ignorePatterns.add("WebViewCore.java:635");

            for (String ignorePattern : ignorePatterns)
                if (Pattern.compile(ignorePattern).matcher(messageBody).find()) {
                    Log.d("NewsRob ErrorHandler", "Ignoring: " + messageBody);
                    return;
                }

            // redirect to a website when permissions are broken
            if (Pattern.compile("process has android.permission.WAKE_LOCK").matcher(messageBody).find()
                    || Pattern.compile("process has android.permission.ACCESS_NETWORK_STATE").matcher(messageBody)
                            .find()
                    || Pattern.compile("lacks android.permission.USE_CREDENTIALS").matcher(messageBody).find()) {
                final Uri uri = Uri
                        .parse("http://newsrob.blogspot.com/2009/09/broken-permissions-due-to-rooting-your.html");
                context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return;
            }

            // Don't report bugs in Custom ROMs anymore
            if ("Stock Android".equals(SettingsRenderer.getCustomRomVersion())) {

                // Prepare Mail
                final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.setType("message/rfc822");
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bugs.newsrob@gmail.com" });
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "BugReport: " + e.getClass().getSimpleName() + ": "
                        + e.getMessage());
                sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);
                Log.d(TAG, "Message Body: " + messageBody);

                // if (heapDumpCreated)
                // sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"
                // + HEAP_DUMP_FILE_NAME));

                Log.e(TAG, "Exception handled. Email activity should be initiated now.");

                // Send Mail
                new Thread(new Runnable() {

                    public void run() {
                        context.startActivity(sendIntent);
                    }
                }).start();
                Log.e(TAG, "Exception handled. Email should be sent by now.");
            }
            // Use default exception mechanism
            if (oldDefaultExceptionHandler != null)
                oldDefaultExceptionHandler.uncaughtException(t, e);
        }

        private boolean createHeapDump(final String messageBody) {
            if (Pattern.compile("OutOfMemoryError").matcher(messageBody).find()) {
                try {
                    final Method m = Debug.class.getMethod("dumpHprofData", new Class[] { String.class });
                    m.invoke(null, HEAP_DUMP_FILE_NAME);
                    return true;
                } catch (final Throwable throwable) {
                    Log.d(TAG, "Error writing hprof dump.", throwable);
                }
            }
            return false;
        }
    }

    public static boolean isDebuggingEnabled(Context context) {
        if (debuggingEnabled == null)
            debuggingEnabled = "1".equals(getDebugProperties(context).getProperty("debug", "0"));
        return debuggingEnabled;
    }

    public static void sendLogFile(Context context) {
    }

}
