package com.newsrob.activities;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.newsrob.DBQuery;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Feed;
import com.newsrob.NewsRob;
import com.newsrob.PL;

/**
 * @SuppressWarnings("PMD.NullAssignments")
 */

public class UIHelper {

    public static final String EXTRA_KEY_POSITION = "POSITION";
    public static final String EXTRA_KEY_FILTER_LABEL = "FILTER_LABEL";
    public static final String EXTRA_KEY_FILTER_FEED = "FILTER_FEED";
    public static final String EXTRA_KEY_START_DATE = "START_DATE";
    public static final String EXTRA_KEY_TITLE = "TITLE";
    public static final String EXTRA_KEY_SORT_ORDER_ASCENDING = "SORT_ASC";
    private static final String EXTRA_KEY_HIDE_READ_ARTICLES = "HIDE_READ_ARTICLES";

    static Pattern PATTERN_HTTP_LINK = Pattern.compile("(https?://[a-z0-9_:\\%\\+\\-\\/?~=.,&#!]*[^.!? ()\"“”])",
            Pattern.CASE_INSENSITIVE);
    static Pattern PATTERN_TWITTER_USER = Pattern.compile("@([a-z0-9_\\-~]*[^.!? ()])", Pattern.CASE_INSENSITIVE);
    static Pattern PATTERN_TWITTER_HASH = Pattern.compile("#([a-z0-9_\\-]+[^.!? ()])", Pattern.CASE_INSENSITIVE);

    private EntryManager entryManager;
    private int[] resourceIdCache = new int[4 * 5 + 5];

    UIHelper(EntryManager entryManager) {
        this.entryManager = entryManager;
    }

    public static DBQuery createDBQueryFromIntentExtras(EntryManager entryManager, Intent intent) {
        String filterLabel = null;
        Long filterFeedId = null;
        long startDate = 0;
        Bundle extras = intent.getExtras();
        boolean sortOrderAscending = !entryManager.shouldShowNewestArticlesFirst();

        if (extras != null) {
            filterLabel = extras.getString(UIHelper.EXTRA_KEY_FILTER_LABEL);
            filterFeedId = extras.getLong(UIHelper.EXTRA_KEY_FILTER_FEED, -1l);
            if (filterFeedId == -1l)
                filterFeedId = null; // NOPMD by mkamp on 1/18/10 12:36 PM

            startDate = extras.getLong(UIHelper.EXTRA_KEY_START_DATE, 0);
            if (extras.containsKey(UIHelper.EXTRA_KEY_SORT_ORDER_ASCENDING))
                sortOrderAscending = extras.getBoolean(UIHelper.EXTRA_KEY_SORT_ORDER_ASCENDING, sortOrderAscending);

        }

        DBQuery dbq = new DBQuery(entryManager, filterLabel, filterFeedId);
        dbq.setStartDate(startDate);
        dbq.setSortOrderAscending(sortOrderAscending);
        PL.log("dbq=" + dbq, entryManager.getContext()); // REMOVE TODO

        return dbq;
    }

    public static void addExtrasFromDBQuery(Intent i, DBQuery dbq) {
        URLHelper url = new URLHelper("newsrob://act" + "?activity=" + Uri.encode(i.getComponent().getClassName()));

        if (dbq.getFilterLabel() != null) {
            i.putExtra(UIHelper.EXTRA_KEY_FILTER_LABEL, dbq.getFilterLabel());
            url.a(UIHelper.EXTRA_KEY_FILTER_LABEL, dbq.getFilterLabel());
        }
        if (dbq.getFilterFeedId() != null && dbq.getFilterFeedId() != -1l) {
            i.putExtra(UIHelper.EXTRA_KEY_FILTER_FEED, dbq.getFilterFeedId());
            url.a(UIHelper.EXTRA_KEY_FILTER_FEED, String.valueOf(dbq.getFilterFeedId()));

        }
        if (dbq.getStartDate() > 0l) {
            i.putExtra(UIHelper.EXTRA_KEY_START_DATE, dbq.getStartDate());
            url.a(UIHelper.EXTRA_KEY_START_DATE, String.valueOf(dbq.getStartDate()));
        }
        url.a(EXTRA_KEY_HIDE_READ_ARTICLES, String.valueOf(dbq.shouldHideReadItems()));

        i.putExtra(UIHelper.EXTRA_KEY_SORT_ORDER_ASCENDING, dbq.isSortOrderAscending());
        url.a(EXTRA_KEY_SORT_ORDER_ASCENDING, String.valueOf(dbq.isSortOrderAscending()));

        if (i.hasExtra(UIHelper.EXTRA_KEY_TITLE))
            url.a(UIHelper.EXTRA_KEY_TITLE, i.getStringExtra(UIHelper.EXTRA_KEY_TITLE));

        // uri = new
        // Uri.Builder().authority("a").scheme("n").path(i.getComponent().getClassName()).build();
        i.setData(Uri.parse(url.toString()));

        i.setFlags(i.getFlags() | Intent.FLAG_DEBUG_LOG_RESOLUTION);

        // encodedQuery("time=" + System.currentTimeMillis()

    }

    int getArticleDownloadIndicatorDrawable(int downloaded, int downloadPref, Resources resources) {

        int cacheKey = downloaded + (downloadPref * 5);
        if (resourceIdCache[cacheKey] != 0)
            return resourceIdCache[cacheKey];

        String resourceName = "gen_m_downloaded_";
        switch (downloaded) {
        case Entry.STATE_DOWNLOAD_ERROR:
            resourceName += "error_";
            break;
        case Entry.STATE_DOWNLOADED_FEED_CONTENT:
        case Entry.STATE_DOWNLOADED_FULL_PAGE:
            resourceName += "yes_";
            break;
        default:
            resourceName += "no_";
        }
        int downloadPrefResolved = (downloadPref == Feed.DOWNLOAD_PREF_DEFAULT ? entryManager.getDefaultDownloadPref()
                : downloadPref);
        switch (downloadPrefResolved) {
        case Feed.DOWNLOAD_HEADERS_ONLY:
            resourceName += "headers";
            break;
        case Feed.DOWNLOAD_PREF_FEED_ONLY:
            resourceName += "feed";
            break;
        case Feed.DOWNLOAD_PREF_FEED_AND_WEBPAGE:
        case Feed.DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE:
            resourceName += "web";
            break;
        }

        int backgroundResource = resources.getIdentifier(resourceName, "drawable", "com.newsrob");
        resourceIdCache[cacheKey] = backgroundResource;
        return backgroundResource;
    }

    public static String linkize(String url, String s) {
        try {
            if (s == null || s.length() < 5)
                return s;

            s = PATTERN_HTTP_LINK.matcher(s).replaceAll("<a href=\"$1\">$1</a>");
            if (url.indexOf("twitter.com") >= 0 || url.indexOf("facebook.com") >= 0) {
                s = PATTERN_TWITTER_USER.matcher(s).replaceAll("<a href=\"http://twitter.com/$1\">@$1</a>");
                s = PATTERN_TWITTER_HASH.matcher(s).replaceAll(
                        "<a href=\"http://search.twitter.com/search?q=$1\">#$1</a>");
            }
        } catch (Exception e) {
            // ignored!
            e.printStackTrace();
        }
        return s;
    }

    public static void pauseWebViews(final Activity activity) {
        manageWebViews(activity, true);

    }

    public static void resumeWebViews(final Activity activity) {
        manageWebViews(activity, false);
    }

    private static void manageWebViews(final Activity activity, final boolean pause) {
        ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        manageWebViews(root, pause);
    }

    private static void manageWebViews(final ViewGroup parent, final boolean pause) {
        WebView wv = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof WebView) {
                wv = (WebView) child;
                manageWebView(wv, pause);
            } else if (child instanceof ViewGroup) {
                manageWebViews((ViewGroup) child, pause);
            }
        }
        if (false && wv != null) {
            if (pause)
                wv.pauseTimers();
            else
                wv.resumeTimers();
        }
    }

    private static void manageWebView(WebView webView, boolean pause) {

        final String methodName = pause ? "onPause" : "onResume";
        final boolean enabled = "1".equals(NewsRob.getDebugProperties(webView.getContext()).getProperty(
                "pauseWebViews", "1"));
        PL.log("UIHelper.manageWebView called with action: " + methodName + " on webView=" + webView + " enabled="
                + enabled, webView.getContext());
        try {
            PL.log("Found webview" + webView + " " + methodName, webView.getContext());
            Method m = webView.getClass().getMethod(methodName, null);
            m.invoke(webView, new Object[] {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class URLHelper {
    private StringBuilder buffer;

    URLHelper(String baseUrl) {
        buffer = new StringBuilder(baseUrl);
    }

    URLHelper a(String parameterName, String value) {
        buffer.append("&" + parameterName + "=" + Uri.encode(value));
        return this;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
