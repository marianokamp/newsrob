package com.newsrob.util;

import java.net.URISyntaxException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class WebViewHelper6 {

    public static boolean handleUrl(String url, Activity owningActivity) {
        Intent intent;

        // perform generic parsing of the URI to turn it into an Intent.
        try {

            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException ex) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
            return false;
        }

        // check whether the intent can be resolved. If not, we will see
        // whether we can download it from the Market.
        if (owningActivity.getPackageManager().resolveActivity(intent, 0) == null) {
            String packagename = intent.getPackage();
            if (packagename != null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + packagename));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                owningActivity.startActivity(intent);
                return true;
            } else {
                return false;
            }
        }

        // sanitize the Intent, ensuring web pages can not bypass
        // browser
        // security (only access to BROWSABLE activities).
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setComponent(null);
        try {
            if (owningActivity.startActivityIfNeeded(intent, -1)) {
                return true;
            }
        } catch (ActivityNotFoundException ex) {
            // ignore the error. If no application can handle the URL,
            // eg about:blank, assume the browser can handle it.
        }

        return false;

    }

}
