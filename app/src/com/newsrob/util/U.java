package com.newsrob.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.Process;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Display;

import com.newsrob.PL;
import com.newsrob.download.HtmlEntitiesDecoder;

public class U {

    private static Pattern PATTERN_HTML_TAGS = Pattern.compile("</?[a-zA-Z-_]*?.*?/?\\s*?>");
    private static Pattern PATTERN_BLANKS = Pattern.compile("\\s+", Pattern.MULTILINE);

    // private static final String TAG = U.class.getName();
    private static DateFormat dateFormat;
    private static Vibrator vibrator;
    private static PowerManager pm;
    protected static int batteryStatusInPercent = -1;
    private static BroadcastReceiver batteryChangedBroadcastReceiver;

    private static final int SCREENLAYOUT_SIZE_SMALL = 0x00000001;
    private static final int SCREENLAYOUT_SIZE_LARGE = 0x00000003;

    public static Map<String, String> parseKeyValuePairsFromString(String s) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(s), 8 * 1024);
        Map<String, String> results = new HashMap<String, String>();
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;

            int separatorIndex = line.indexOf('=');
            if (separatorIndex >= 0) {
                results.put(line.substring(0, separatorIndex), line.substring(separatorIndex + 1));
            }
        }
        br.close();
        return results;
    }

    public static int getScreenSize(final Context ctx) {
        int size = 0;

        try {
            Configuration c = ctx.getResources().getConfiguration();

            if (c.getClass().getField("screenLayout") != null) {
                Field f = c.getClass().getField("screenLayout");
                int screenSize = f.getInt(c) & Configuration.SCREENLAYOUT_SIZE_MASK;
                if (screenSize == SCREENLAYOUT_SIZE_LARGE)
                    size = 1;
                else if (screenSize == SCREENLAYOUT_SIZE_SMALL)
                    size = -1;

            }
        } catch (Exception e) {
        }

        return size;

    }

    float getScreenSizeFactor(final Context ctx) {
        float screenSizeFactor = 1.0f;
        try {

            Configuration c = ctx.getResources().getConfiguration();

            if (c.getClass().getField("screenLayout") != null) {
                Field f = c.getClass().getField("screenLayout");
                int screenSize = f.getInt(c) & Configuration.SCREENLAYOUT_SIZE_MASK;

                if (screenSize == SCREENLAYOUT_SIZE_LARGE)
                    return 1.0f;
                if (screenSize == SCREENLAYOUT_SIZE_SMALL)
                    return 0.75f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return screenSizeFactor;
    }

    public static void setLowPrio() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    }

    static String join(Collection<? extends Object> collection, String separator, String prefix) {
        if (collection == null || collection.size() == 0)
            return "";

        if (separator == null)
            separator = ", ";

        StringBuilder result = new StringBuilder();

        for (Object object : collection) {
            result.append((prefix != null ? prefix : "") + object.toString() + separator);
        }
        // return result, but remove the trailing separator first
        return result.substring(0, result.length() - (separator.length()));

    }

    public static String htmlToText(String html) {
        if (html == null)
            return "";

        String returnValue = PATTERN_HTML_TAGS.matcher(html).replaceAll("");
        returnValue = PATTERN_BLANKS.matcher(returnValue).replaceAll(" ");
        return HtmlEntitiesDecoder.decodeString(returnValue).trim();
    }

    public static String join(Collection<? extends Object> collection, String separator) {
        return join(collection, separator, null);
    }

    public static String t(Context context, int id) {
        return context.getResources().getString(id);
    }

    public static DateFormat getDateFormat() {
        if (dateFormat == null)
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return dateFormat;
    }

    public static final String longToHex(long l) {
        String s = "0000000000000000" + Long.toHexString(l);
        int beginIndex = s.length() - 16;
        return s.substring(beginIndex);
    }

    public static String pluralize(int number, String wordSingular) {
        if (number == 1)
            return wordSingular;

        /*
         * String[] numbers = { "no", "one", "two", "three", "four", "five",
         * "six", "seven", "eight", "nine", "ten", "eleven", "twelve" }; if
         * (number > numbers.length - 1) return number + " " + wordSingular +
         * "s";
         * 
         * return numbers[number] + " " + wordSingular + "s";
         */
        return wordSingular + "s";
    }

    public static void vibrate(Context context, long milliseconds) {
        if (vibrator == null) {
            vibrator = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        vibrator.vibrate(milliseconds);
    }

    public static String getCallingMethod() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }

    public static float getDensity(Activity activity) {

        Display d = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics(dm);
        return dm.density;

    }

    public static void renderStackTrace(final Throwable e, final StringBuilder message) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        final StringBuffer stackTrace = sw.getBuffer();
        message.append("-- Stacktrace:(" + stackTrace.length() + ")\n");
        message.append(sw.getBuffer());
    }

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        return packageManager.queryIntentActivities(intent, 0).size() > 0;
    }

    public static boolean isBroadcastAvailable(Context ctx, String action) {
        final PackageManager packageManager = ctx.getPackageManager();
        final Intent intent = new Intent(action);
        return packageManager.queryBroadcastReceivers(intent, 0).size() > 0;
    }

    public static final boolean isScreenOn(Context ctx) {

        return false;

        /*
         * if (pm == null) pm = (PowerManager)
         * ctx.getSystemService(Context.POWER_SERVICE); return pm.isScreenOn();
         */

    }

    public static int getBatteryChargedPercent(final Context ctx) {
        if (batteryChangedBroadcastReceiver == null) {
            batteryChangedBroadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    int max = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                    if (max == -1 || current == -1)
                        U.batteryStatusInPercent = -1;
                    else
                        U.batteryStatusInPercent = (int) ((current * 1.0 / max) * 100.0);

                    PL.log("Battery: " + U.batteryStatusInPercent + "%", ctx);
                }
            };
            ctx.registerReceiver(batteryChangedBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
        return U.batteryStatusInPercent;
    }
}