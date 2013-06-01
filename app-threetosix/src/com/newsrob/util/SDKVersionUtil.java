package com.newsrob.util;

import java.lang.reflect.Field;

public class SDKVersionUtil {
    private static int version = -1;

    static {
        version = 3;
        Field f;
        try {
            f = android.os.Build.VERSION.class.getField("SDK_INT");
            version = f.getInt(f);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static final int getVersion() {
        return version;
    }

}
