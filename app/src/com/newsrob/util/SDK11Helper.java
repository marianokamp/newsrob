package com.newsrob.util;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;

import com.newsrob.PL;

public class SDK11Helper {

    // private static final Method sApplyMethod = findApplyMethod();

    public static void enableHWAccelerationForActivity(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {

            final Class cls = WindowManager.LayoutParams.class;
            Field f = null;
            try {
                f = cls.getField("FLAG_HARDWARE_ACCELERATED");
                // PL.log("f=" + f, activity);
                if (f == null)
                    return;
                final int flag = f.getInt(WindowManager.class);
                // PL.log("flag=" + flag, activity);
                Window w = activity.getWindow();
                w.setFlags(flag, flag);
                PL.log("Added hw acceleration to window of activity " + activity + ".", activity);

            }

            catch (Exception e) {
                PL.log("No hw acceleration.", e, activity.getApplicationContext());
                return;
            }

        }
    }

    /*
     * public static void checkIfActivityIsHWAccelerated(Activity activity) {
     * 
     * Class viewClass = View.class; Method m =
     * viewClass.getMethod("isHardwareAccelerated", null);
     * 
     * boolean isHardwareAccelerated = false; final String toastMessage =
     * String.format("This screen is %shw accelerated.", (isHardwareAccelerated
     * ? "" : "not ")); Toast.makeText(activity, toastMessage,
     * Toast.LENGTH_LONG).show(); return isHardwareAccelerated; }
     */
}
