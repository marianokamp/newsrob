package com.newsrob.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.TextSize;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.newsrob.AssetContentProvider;
import com.newsrob.DBQuery;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Feed;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.NewsRob;
import com.newsrob.PL;
import com.newsrob.ReadState;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.threetosix.R;
import com.newsrob.util.FlurryUtil;
import com.newsrob.util.GoogleAdsUtil;
import com.newsrob.util.MessageHelper;
import com.newsrob.util.SDKVersionUtil;
import com.newsrob.util.Timing;
import com.newsrob.util.U;
import com.newsrob.util.WebViewHelper6;
import com.newsrob.util.WebViewHelper8;
import com.newsrob.widget.RelativeLayout;

public class ShowArticleActivity extends Activity implements IEntryModelUpdateListener, View.OnClickListener {

    static final int VIEW_MODE_FEED = 0;
    static final int VIEW_MODE_ALTERNATE = 1;
    static final int VIEW_MODE_DEFAULT = VIEW_MODE_FEED;

    private static final int MENU_ITEM_ID_ZOOM_IN = 911;
    private static final int MENU_ITEM_ID_ZOOM_OUT = 912;
    static final int MENU_ITEM_TOGGLE_VIEW_MODE = 913;

    private WebView webView;

    private int viewMode;

    private Handler handler = new Handler();

    private Cursor contentCursor;
    private int savedContentCursorPosition = -1;
    private Long savedContentCursoCurrentId = null;

    private Entry selectedEntry;

    private Button nextButton;
    private Button prevButton;
    private Runnable refreshUIRunnable;
    private RelativeLayout container;
    private TextView viewModeTextView;
    private String atomIdOfCurrentlyShowingArticle;
    private UIHelper uiHelper;

    private boolean leavingThisActivity;
    private boolean articleWasAlreadyRead;

    private EmbeddedWebViewClient embeddedWebClient;

    private View lastLongClickTarget;
    private float defaultScale;
    private float currentScale;

    private boolean inFullScreenMode;

    private EntryManager entryManager;

    private int backgroundColorLight;
    private int backgroundColorDark;

    private String TAG = ShowArticleActivity.class.getSimpleName();
    private int currentTheme;
    private Timing switchTiming;
    private Runnable hideTitlePreviewRunnable;
    private ScaleAnimation hideAnimation;
    private View titlePreviewContainer;
    private ScaleAnimation showAnimation;
    private MotionEvent lastDownEvent;

    private GoogleAdsUtil googleAdsUtil;
    private boolean debug;
    private int latestOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ("1".equals(NewsRob.getDebugProperties(this).getProperty("traceArticleDetailViewLaunch", "0")))
            Debug.startMethodTracing("traceArticleDetialViewLaunch");

        setTheme(getEntryManager().getCurrentThemeResourceId());
        // setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        // gui
        debug = "1".equals(NewsRob.getDebugProperties(this).getProperty("articleDetailViewVerbose", "0"));

        savedContentCursorPosition = -1;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (getIntent().getExtras() == null) {
            finish();
            return;
        }

        setContentView(com.newsrob.threetosix.R.layout.show_article);

        googleAdsUtil = new GoogleAdsUtil(getEntryManager());

        uiHelper = new UIHelper(getEntryManager());

        RelativeLayout container = getContainer();
        registerForContextMenu(container);

        container.setOnClickListener(this);
        container.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                lastLongClickTarget = v;
                return false;
            }
        });

        embeddedWebClient = new EmbeddedWebViewClient();

        createWebView(container);
        createOnScreenControls(container);

        // end-gui

        // runnables

        refreshUIRunnable = new Runnable() {
            public void run() {
                refreshUI();
            }
        };

        hideTitlePreviewRunnable = new Runnable() {
            public void run() {
                hideTitlePreview();
            }
        };

        titlePreviewContainer = findViewById(R.id.title_preview);

        hideAnimation = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_PARENT, 0.5f,
                Animation.RELATIVE_TO_PARENT, 0.5f);
        hideAnimation.setDuration(300);
        hideAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titlePreviewContainer.setVisibility(View.INVISIBLE);
            }
        });

        showAnimation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_PARENT, 0.5f,
                Animation.RELATIVE_TO_PARENT, 0.5f);
        showAnimation.setDuration(300);
        showAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                titlePreviewContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });

        // end-runnables

        initialize(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initialize(intent);
    }

    private void initialize(Intent i) {

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(R.attr.webViewBackgroundColorLight, tv, true);

        backgroundColorLight = Color.parseColor(tv.coerceToString().toString());
        backgroundColorDark = Color.argb(255, 11, 11, 11);

        int position = i.getExtras().getInt(UIHelper.EXTRA_KEY_POSITION);
        if ("com.newsrob.VIEW".equals(i.getAction())) {
            contentCursor = getEntryManager().getArticleAsCursor(i.getDataString());
        } else if (Intent.ACTION_SEARCH.equals(i.getAction()) && i.hasExtra("atomId")) {
            contentCursor = getEntryManager().getArticleAsCursor(i.getExtras().getString("atomId"));
        } else {
            DBQuery dbQuery = UIHelper.createDBQueryFromIntentExtras(getEntryManager(), i);
            dbQuery.setLimit(getEntryManager().getMaxArticlesInArticleList());
            contentCursor = getEntryManager().getContentCursor(dbQuery);
        }

        startManagingCursor(contentCursor);

        if (!contentCursor.moveToPosition(position)) {
            PL.log("MoveToPosition failed. position=" + position, this);
            finish();
            return;
        }
        newPosition();
        getEntryManager().updateLastUsed();
        currentTheme = getEntryManager().getCurrentThemeResourceId();

    }

    final RelativeLayout getContainer() {
        if (container == null)
            container = (RelativeLayout) findViewById(R.id.show_entry_container);
        return container;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // AdUtil.onConfigurationChanged(this, newConfig);
        googleAdsUtil.showAds(this);
        if (viewMode != VIEW_MODE_ALTERNATE)
            viewFeedContent();

    }

    private void toggleFullScreenMode() {

        final View v = findViewById(R.id.outter_container);

        if (inFullScreenMode) {

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (false) {
                Animation animation = new TranslateAnimation(0f, 0f, 0f, v.getHeight());
                animation.setDuration(500);
                v.startAnimation(animation);
            }
            v.setVisibility(View.VISIBLE);
            hideTitlePreview();

            inFullScreenMode = false;
        } else {

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if (false) {
                Animation animation = new TranslateAnimation(0f, 0f, 0f, -v.getHeight());
                animation.setDuration(500);
                v.startAnimation(animation);
            }

            v.setVisibility(View.GONE);
            Toast.makeText(this, "- Fullscreen Mode -\n  (Tap twice to exit)", Toast.LENGTH_SHORT).show();

            inFullScreenMode = true;
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (entryManager == null)
            entryManager = getEntryManager();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) { // &&
            // entryManager.isProVersion()

            Rect r = new Rect();
            if (findViewById(R.id.content_web_view).getGlobalVisibleRect(r)
                    && (r.contains((int) ev.getRawX(), (int) ev.getRawY()))) {

                if (lastDownEvent != null) {

                    long timeMsSinceLastDown = ev.getDownTime() - lastDownEvent.getEventTime();

                    float travelX = Math.abs(ev.getRawX() - lastDownEvent.getRawX());
                    float travelY = Math.abs(ev.getRawY() - lastDownEvent.getRawY());
                    float totalTravel = travelX + travelY;

                    if (false)
                        System.out.println("time(ms)=" + timeMsSinceLastDown + " travel(px)=" + totalTravel
                                + " travel(px_translated)=" + (totalTravel * getDisplayMetrics().density)
                                + " lastDown=" + lastDownEvent);

                    final float MAX_TRAVEL = 28 * getDisplayMetrics().density;
                    final float MAX_TIME_MS = 200;

                    // PL.log("ShowArticle: travel=" + totalTravel + " time=" +
                    // timeMsSinceLastDown, this);

                    if (timeMsSinceLastDown < MAX_TIME_MS && totalTravel < MAX_TRAVEL) {
                        toggleFullScreenMode();
                        lastDownEvent = null;
                    }

                }
                lastDownEvent = MotionEvent.obtain(ev);
            }

        }
        return super.dispatchTouchEvent(ev);
    }

    public void onClick(View v) {
        try {
            if (NewsRob.isDebuggingEnabled(this)) {
                switchTiming = new Timing("Click to load", this);
                if ("1".equals(NewsRob.getDebugProperties(this).getProperty("dumpClickToLoad", "0"))) {
                    new File(Environment.getExternalStorageDirectory(), "t").mkdirs();
                    Debug.startMethodTracing("t/click-to-load_"
                            + new SimpleDateFormat("yyMMdd-hh:mm:ss").format(new Date()).replace(':', '-'));
                }
            }

            try {
                if (v == nextButton) {

                    if (!contentCursor.moveToNext()) {
                        PL.log(TAG + ": MoveToNext failed.", this);
                        finish();
                        return;
                    }
                    newPosition();
                } else if (v == prevButton) {
                    if (!contentCursor.moveToPrevious()) {
                        PL.log(TAG + ": MoveToPrevious failed.", this);
                        finish();
                        return;
                    }
                    newPosition();
                } else {
                    toggleViewMode(false);
                    view();
                }
            } catch (IllegalStateException ise) {
                // ignore
            } catch (CursorIndexOutOfBoundsException cioobe) {
                // ignore -- moveToNext
            }

        } finally {
            if (switchTiming != null) {
                switchTiming.stop();
                try {
                    if ("1".equals(NewsRob.getDebugProperties(this).getProperty("dumpClickToLoad", "0")))
                        Debug.stopMethodTracing();
                } catch (Throwable t) {
                }
            }
        }
    }

    private void createWebView(View parent) {
        WebView wv = getWebView();
        wv.setWebViewClient(embeddedWebClient);

        try {

            Method m = wv.getClass().getMethod("setScrollbarFadingEnabled", Boolean.class);
            m.invoke(wv, true);
            // webView.setScrollbarFadingEnabled(true); // no longer the default
            // for
            // Froyo
        } catch (Exception e) {
            // never mind
        }

        wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        defaultScale = wv.getScale(); // saved for later use

        wv.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                lastLongClickTarget = v;
                return false;
            }
        });

        WebSettings webSettings = wv.getSettings();
        webSettings.setSavePassword(false);

        // Log.d("ArticleDetailView", "User Agent= " +
        // webView.getSettings().getUserAgentString());

        Method setBuiltInZoomControlsMethod = null;
        try {
            setBuiltInZoomControlsMethod = webSettings.getClass().getMethod("setBuiltInZoomControls",
                    new Class[] { boolean.class });
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }

        if (setBuiltInZoomControlsMethod != null) {
            try {
                setBuiltInZoomControlsMethod.invoke(webSettings, new Object[] { getEntryManager()
                        .areHoveringZoomControlsEnabled() });
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        } else {
            FrameLayout zoomControlContainer = (FrameLayout) findViewById(R.id.browse_zoom);
            zoomControlContainer.addView(wv.getZoomControls());
            wv.getZoomControls().setVisibility(View.GONE);
        }

        if (SDKVersionUtil.getVersion() >= 8)
            WebViewHelper8.setupWebView(entryManager, wv);
        else
            webSettings.setPluginsEnabled(true); // Enables YouTube
        webSettings.setTextSize(TextSize.valueOf(getEntryManager().getDefaultTextSize()));

        // set Overview Mode
        Method setLoadWithOverviewModeMethod = null;
        try {
            setLoadWithOverviewModeMethod = webSettings.getClass().getMethod("setLoadWithOverviewMode",
                    new Class[] { boolean.class });
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }

        if (setLoadWithOverviewModeMethod != null) {
            try {
                setLoadWithOverviewModeMethod.invoke(webSettings, new Object[] { false });
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }

        // webSettings.setLoadWithOverviewMode(true);

        // LATER webSettings.setLightTouchEnabled(true);

    }

    private WebView getWebView() {
        if (webView == null)
            webView = (WebView) findViewById(R.id.content_web_view);
        return webView;
    }

    private void createOnScreenControls(RelativeLayout container) {
        nextButton = (Button) findViewById(R.id.next);
        prevButton = (Button) findViewById(R.id.prev);

        nextButton.setOnClickListener(this);
        prevButton.setOnClickListener(this);

        container.setNextButton(nextButton);
        container.setPrevButton(prevButton);
    }

    protected void toggleViewMode(boolean triggeredByAMenu) {

        if (viewMode == VIEW_MODE_ALTERNATE)
            viewMode = VIEW_MODE_FEED;
        else
            viewMode = VIEW_MODE_ALTERNATE;

        String mode = (viewMode == VIEW_MODE_ALTERNATE ? "web" : "feed");
        String message = "Switching to \'" + mode + "\' mode.";
        if (triggeredByAMenu)
            message += "\n\nYou can also tap on the article header to switch.";

        final int duration = triggeredByAMenu ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, message, duration).show();

        webView.clearHistory();
        if (webView.getScrollX() != 0 || webView.getScrollY() != 0) {
            webView.scrollTo(0, 0);
        }
        updateViewModeTextView();
        // webView.loadData("", "", "");
        // webView.clearView();
        atomIdOfCurrentlyShowingArticle = null; // NOPMD
    }

    private void newPosition() {

        Timing t = new Timing("ShowArticleActivity.newPosition()", this);
        try {
            // webView.clearView();
            atomIdOfCurrentlyShowingArticle = null; // NOPMD
            latestOrientation = -99;

            selectedEntry = null; // NOPMD
            // Mark Item as Read
            selectedEntry = getSelectedEntry();
            if (selectedEntry == null) {
                finish();
                return;
            }

            // googleAdsUtil.showAds(this);
            // AdUtil.refreshAd(this);

            webView.clearHistory();
            if (webView.getScrollX() != 0 || webView.getScrollY() != 0) {
                webView.scrollTo(0, 0);
            }

            webView.getSettings().setJavaScriptEnabled(selectedEntry.isJavaScriptEnabled());

            getEntryManager().setMostRecentArticleAtomId(selectedEntry.getAtomId());
            adjustScale(selectedEntry); // TODO I tested it quickly, this is not
            // needed, let's remove it in the next
            // release

            // set view mode
            int vMode = selectedEntry.getResolvedDisplayPref(getEntryManager());

            viewMode = vMode == Feed.DISPLAY_PREF_WEBPAGE ? VIEW_MODE_ALTERNATE : VIEW_MODE_FEED;
            updateViewModeTextView();

            // end set view mode

            getContainer().updateState(!contentCursor.isLast(), !contentCursor.isFirst());

            articleWasAlreadyRead = selectedEntry.getReadState() == ReadState.READ;

            if (selectedEntry.getReadState() == ReadState.UNREAD) {
                setArticleReadStateAsynchronously(selectedEntry, ReadState.READ);
            }
            view();
            refreshUI();
            TextView tv = (TextView) findViewById(R.id.title_preview_text);
            tv.setText(selectedEntry.getTitle());
            showTitlePreview();
        } finally {
            t.stop();
        }

    }

    private void hideTitlePreview() {
        handler.removeCallbacks(hideTitlePreviewRunnable);

        if (titlePreviewContainer.getVisibility() != View.INVISIBLE)
            titlePreviewContainer.startAnimation(hideAnimation);
    }

    private void showTitlePreview() {

        if (!inFullScreenMode)
            return;

        if (titlePreviewContainer.getVisibility() != View.VISIBLE)
            titlePreviewContainer.startAnimation(showAnimation);

        handler.removeCallbacks(hideTitlePreviewRunnable);
        handler.postDelayed(hideTitlePreviewRunnable, 5000);
    }

    private void setArticleReadStateAsynchronously(final Entry selectedEntry, final ReadState newReadState) {

        final Timing t = new Timing("setArticleReadStateAsynchronously(" + U.getCallingMethod() + ")", this);
        final Timing t3 = new Timing("setArticleReadStateAsync waiting for post", this);

        new Thread(new Runnable() {

            @Override
            public void run() {
                t3.stop();
                Timing t2 = new Timing("getEntryManager.updateReadState", ShowArticleActivity.this);
                getEntryManager().updateReadState(selectedEntry, newReadState);
                t2.stop();
                handler.post(new Runnable() {
                    public void run() {
                        refreshUI();
                        t.stop();
                    }
                });

            }
        }).start();

    }

    private void adjustScale(Entry selectedEntry) {
        if (selectedEntry == null)
            return;

        Feed f = getEntryManager().findFeedById(selectedEntry.getFeedId());
        if (f != null) {
            float targetScale = (viewMode == VIEW_MODE_ALTERNATE ? f.getWebScale() : f.getFeedScale());

            // not set yet? LATER move it to Feed?
            if (Math.abs(targetScale + 1.0f) < 0.02f)
                targetScale = defaultScale;
            currentScale = targetScale;
            webView.setInitialScale((int) (100 * targetScale));

        }
    }

    private void updateViewModeTextView() {
        getViewModeTextView().setText(viewMode == VIEW_MODE_ALTERNATE ? "web" : "feed");

    }

    private TextView getViewModeTextView() {
        if (viewModeTextView == null) {
            viewModeTextView = (TextView) findViewById(R.id.view_mode);
            viewModeTextView.setVisibility(View.VISIBLE);
        }
        return viewModeTextView;
    }

    private Entry getSelectedEntry() {
        if (selectedEntry != null)
            return selectedEntry;

        if (contentCursor.isBeforeFirst())
            return null;

        String entryAtomId = null;
        try {
            entryAtomId = contentCursor.getString(1);
        } catch (CursorIndexOutOfBoundsException cioobe) {
            Log.e("SAA", "Ooops. CIOOBE", cioobe);
            cioobe.printStackTrace();
        }

        if (entryAtomId == null)
            return null;

        // TODO PERF Instead use the cursor directly

        return getEntryManager().findEntryByAtomId(entryAtomId);

    }

    private void viewFeedContent() {

        if (debug)
            PL.log("ADV: viewFeedContent " + selectedEntry.getTitle(), this);

        setFeedViewsBackgroundColor();

        Entry selectedEntry = getSelectedEntry();

        if (shouldSkipReloading(selectedEntry))
            return;

        webView.loadData("", "", "");
        webView.getSettings().setUseWideViewPort(false);
        this.atomIdOfCurrentlyShowingArticle = selectedEntry.getAtomId();

        boolean feedContentAvailableLocal = false;
        String localUrl = AssetContentProvider.CONTENT_URI + "/a" + getSelectedEntry().getShortAtomId() + "/a"
                + getSelectedEntry().getShortAtomId() + "_s.html";
        try {
            feedContentAvailableLocal = (selectedEntry.getDownloaded() == Entry.STATE_DOWNLOADED_FEED_CONTENT || selectedEntry
                    .getDownloaded() == Entry.STATE_DOWNLOADED_FULL_PAGE)
                    && getContentResolver().openFileDescriptor(Uri.parse(localUrl), null) != null;
        } catch (FileNotFoundException e) {
        }

        if (debug)
            PL.log("ADV: viewFeedContent localContentAvailable " + selectedEntry.getTitle() + " "
                    + feedContentAvailableLocal, this);

        if (feedContentAvailableLocal) {
            // webView.loadUrl(localUrl);
            final String baseUrl = selectedEntry.getBaseUrl(getEntryManager());
            final String content = getTopDecoration(selectedEntry) + loadContent(localUrl)
                    + ShowArticleActivity.getBodyBottomDecoration();
            final String contentType = "text/" + selectedEntry.getContentType();

            if (debug)
                PL.log("ADV: viewFeedContent (locally) " + selectedEntry.getTitle() + " \n  baseUrl=" + baseUrl
                        + "\n  contentType=" + contentType + "\n  content=" + content, this);

            webView.loadDataWithBaseURL(baseUrl, content, contentType, "utf-8", null);

        } else if (selectedEntry.getContent() != null) {
            final String content = (selectedEntry.getContent() != null ? selectedEntry.getContent() : "");
            final String html = getTopDecoration(selectedEntry) + content
                    + ShowArticleActivity.getBodyBottomDecoration();
            final String contentType = "text/" + selectedEntry.getContentType();

            if (debug)
                PL.log("ADV: viewFeedContent (inline) " + selectedEntry.getTitle() + "\n  contentType=" + contentType
                        + "\n  content=" + content + "\n  html=" + html, this);

            webView.loadDataWithBaseURL(selectedEntry.getBaseUrl(getEntryManager()), html, contentType, "utf-8", null);
        } else if (selectedEntry.getContentURL() != null) {
            webView.loadUrl(selectedEntry.getContentURL());
        } else {

            if (debug)
                PL.log("ADV: viewFeedContent (title) " + selectedEntry.getTitle(), this);

            final String html = getTopDecoration(selectedEntry) + "<p>"
                    + (UIHelper.linkize(selectedEntry.getAlternateHRef(), selectedEntry.getTitle())) + "</p>"
                    + ShowArticleActivity.getBodyBottomDecoration();
            webView.loadDataWithBaseURL(selectedEntry.getBaseUrl(getEntryManager()), html, "text/html", "UTF-8", null);
        }

    }

    private boolean shouldSkipReloading(Entry newArticle) {
        // skip reloading the article
        // when the article is the same
        // and the orientation remained
        // or the orientation changed, but the user
        // already scrolled.

        int currentOrientation = getResources().getConfiguration().orientation;
        int scrolled = webView.getScrollX() + webView.getScrollY();
        boolean shouldSkipReloading = (newArticle.getAtomId().equals(atomIdOfCurrentlyShowingArticle) && (latestOrientation == currentOrientation || scrolled != 0));

        latestOrientation = currentOrientation;
        return shouldSkipReloading;
    }

    private void setFeedViewsBackgroundColor() {
        webView.setBackgroundColor(getEntryManager().isLightColorSchemeSelected() ? backgroundColorLight
                : backgroundColorDark);
    }

    private String getTopDecoration(Entry selectedEntry) {
        return getDateAuthorHtml(selectedEntry) + getNoteSubmittedHtml(selectedEntry);
    }

    private void viewAlternateContent() {

        if (debug)
            PL.log("ADV: viewAlternativeContent " + selectedEntry.getTitle(), this);

        if (getSelectedEntry().getAtomId().equals(atomIdOfCurrentlyShowingArticle))
            return;

        atomIdOfCurrentlyShowingArticle = getSelectedEntry().getAtomId();
        // webView.getSettings().setUseWideViewPort(showFullWebPage); LATER

        if (isFullPageDownloaded()) {

            String localUrl = AssetContentProvider.CONTENT_URI + "/a" + getSelectedEntry().getShortAtomId() + "/a"
                    + getSelectedEntry().getShortAtomId() + "_x.html";
            String content = loadContent(localUrl);
            if (isShowingSimplifiedPage(selectedEntry)) {

                // add style that possibly darkens the background
                content = content.replaceFirst("</head>", renderInlineStylesheet(selectedEntry) + "</head>");
            }
            webView.loadDataWithBaseURL(selectedEntry.getBaseUrl(getEntryManager()), content, "text/"
                    + selectedEntry.getContentType(), "utf-8", null);

        } else {
            webView.loadUrl(selectedEntry.getBaseUrl(getEntryManager()));
        }
        setWebModeBackgroundColor(true);
    }

    private boolean isFullPageDownloaded() {
        Entry entry = getSelectedEntry();
        if (entry == null)
            return false;
        return entry.getDownloaded() == Entry.STATE_DOWNLOADED_FULL_PAGE;
    }

    private void setWebModeBackgroundColor(boolean showingInitialPage) {

        boolean downloadedFullPage = isFullPageDownloaded();
        boolean showSimplifiedPage = isShowingSimplifiedPage(selectedEntry);

        int bgColor = backgroundColorLight;

        if (showingInitialPage && showSimplifiedPage && downloadedFullPage)
            bgColor = getEntryManager().isLightColorSchemeSelected() ? backgroundColorLight : backgroundColorDark;

        webView.setBackgroundColor(bgColor);
    }

    private boolean isShowingSimplifiedPage(Entry selectedEntry) {
        if (selectedEntry == null)
            return false;
        return selectedEntry.getResolvedDownloadPref(getEntryManager()) == Feed.DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE;
    }

    private String loadContent(String localUrl) {
        StringBuilder sb = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(
                    Uri.parse(localUrl))), 8192);
            sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();

        } catch (IOException e) {
            sb = new StringBuilder(e.getClass() + ":" + e.getMessage());
            e.printStackTrace();
        }
        String result = sb.toString();

        // Pattern.compile("<\\s*body/?", Pattern.DOTALL |
        // Pattern.CASE_INSENSITIVE |
        // Pattern.MULTILINE).matcher(sql).replaceAll("");

        return result;
    }

    @Override
    protected void onPause() {

        if (!leavingThisActivity && !articleWasAlreadyRead) {
            Entry selectedEntry = getSelectedEntry();
            if (selectedEntry != null && selectedEntry.getReadState() == ReadState.READ)
                setArticleReadStateAsynchronously(selectedEntry, ReadState.UNREAD);
        }

        getEntryManager().removeListener(this);
        webView.stopLoading();
        webView.clearCache(true);
        // AdUtil.unpublishAd(this);
        googleAdsUtil.hideAds(this);

        if (getSelectedEntry() != null) {
            try {
                savedContentCursorPosition = contentCursor.getPosition();
                savedContentCursoCurrentId = contentCursor.getLong(0);
            } catch (CursorIndexOutOfBoundsException cioobe) {
                savedContentCursorPosition = -1;
                savedContentCursoCurrentId = null;
            }
        }

        UIHelper.pauseWebViews(this);
        // webView.pauseTimers();

        super.onPause();
    }

    @Override
    protected void onResume() {
        NewsRob.lastActivity = this;

        super.onResume();
        UIHelper.resumeWebViews(this);

        if (currentTheme != getEntryManager().getCurrentThemeResourceId()) {
            finish();
            startActivity(getIntent());
        } else {

            if (getWebView() != null) {
                PL.log("WebView timer resumed.", this);
                webView.resumeTimers();
            } else
                PL.log("WebView timer not resumed.", this);

            // trying to forward the restoredCursor to the last known position
            // staying there when the position's article has the same id as the
            // one
            // that was stored, otherwise rewind.
            try {
                if (savedContentCursorPosition != -1 && savedContentCursoCurrentId != null) {
                    contentCursor.moveToPosition(savedContentCursorPosition);
                    if (!savedContentCursoCurrentId.equals(contentCursor.getLong(0)))
                        contentCursor.moveToPosition(-1);
                }
            } catch (CursorIndexOutOfBoundsException e) {
                contentCursor.moveToPosition(-1);
            }

            // AdUtil.publishAd(this);
            googleAdsUtil.showAds(this);
            leavingThisActivity = false;
            getEntryManager().addListener(this);

            Entry selectedEntry = getSelectedEntry();
            if (selectedEntry != null) {
                articleWasAlreadyRead = selectedEntry.getReadState() == ReadState.READ;
                if (selectedEntry.getReadState() == ReadState.UNREAD)
                    setArticleReadStateAsynchronously(selectedEntry, ReadState.READ);
            }
            view();
            refreshUI();
            savedContentCursorPosition = -1;
            savedContentCursoCurrentId = null;
        }

        if (entryManager.isProVersion() && entryManager.getDaysInstalled() > 0)
            MessageHelper.showMessage(this, R.string.explain_fullscreen_toggle_title,
                    R.string.explain_fullscreen_toggle_message, "explain_fullscreen_toggle");

        if ("1".equals(NewsRob.getDebugProperties(this).getProperty("traceArticleDetailViewLaunch", "0")))
            Debug.stopMethodTracing();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (KeyEvent.KEYCODE_HOME == keyCode || KeyEvent.KEYCODE_BACK == keyCode)
            leavingThisActivity = true;

        if ((KeyEvent.KEYCODE_VOLUME_DOWN == keyCode || KeyEvent.KEYCODE_VOLUME_UP == keyCode)
                && getEntryManager().isVolumeControlNavigationEnabled()) {
            if (KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
                if (nextButton.isEnabled())
                    nextButton.performClick();
                else
                    vibrate();

                return true;
            } else if (KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
                if (prevButton.isEnabled())
                    prevButton.performClick();
                else
                    vibrate();
                return true;
            }
        }
        if (KeyEvent.KEYCODE_CAMERA == keyCode && getEntryManager().isCameraButtonControllingReadStateEnabled()) {
            if (selectedEntry != null) {
                ReadState newReadState = selectedEntry.getReadState() == ReadState.READ ? ReadState.UNREAD
                        : ReadState.READ;
                setArticleReadStateAsynchronously(selectedEntry, newReadState);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void vibrate() {
        if (!getEntryManager().shouldVibrateOnFirstLast())
            return;

        final long vibrateDuration = 28;
        U.vibrate(this, vibrateDuration);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if ((KeyEvent.KEYCODE_VOLUME_DOWN == keyCode || KeyEvent.KEYCODE_VOLUME_UP == keyCode)
                && getEntryManager().isVolumeControlNavigationEnabled())
            return true;
        if (KeyEvent.KEYCODE_CAMERA == keyCode && getEntryManager().isCameraButtonControllingReadStateEnabled())
            return true;

        return super.onKeyUp(keyCode, event);
    }

    private EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(getApplication());
        return entryManager;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        ArticleViewHelper.createArticleMenu(menu, this, selectedEntry);
        if (!getEntryManager().areHoveringZoomControlsEnabled()) {
            menu.add(0, MENU_ITEM_ID_ZOOM_IN, 8, R.string.zoom_in).setIcon(R.drawable.gen_zoom_in)
                    .setAlphabeticShortcut('i');
            menu.add(0, MENU_ITEM_ID_ZOOM_OUT, 8, R.string.zoom_out).setIcon(R.drawable.gen_zoom_out)
                    .setAlphabeticShortcut('o');
        }
        menu.add(0, MENU_ITEM_TOGGLE_VIEW_MODE, 10, "Toggle View Mode");
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (isContextMenuLaunchedOnLink()) {
            Uri uri = Uri.parse(webView.getHitTestResult().getExtra());

            menu.add(0, ArticleViewHelper.MENU_ITEM_SHOW_IN_BROWSER_ID, 0, R.string.menu_show_in_browser)
                    .setTitleCondensed(U.t(this, R.string.menu_show_in_browser_condensed)).setIntent(
                            new Intent(Intent.ACTION_VIEW, uri)).setIcon(android.R.drawable.ic_menu_view);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            // sendIntent.setType("message/rfc822");
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, String.valueOf(uri));
            sendIntent.putExtra(Intent.EXTRA_TEXT, String.valueOf(uri));

            menu.add(0, ArticleViewHelper.MENU_ITEM_SHARE_LINK_ID, 0, R.string.menu_item_share_link).setIntent(
                    Intent.createChooser(sendIntent, null)).setIcon(android.R.drawable.ic_menu_share);

        } else
            ArticleViewHelper.createArticleMenu(menu, this, getSelectedEntry());

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private String readInputStreamIntoString(InputStream is) {

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;
        try {
            while ((line = br.readLine()) != null)
                sb.append(line);
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return sb.toString();
    }

    private String getNoteSubmittedHtml(Entry entry) {

        if (entry.getNote() == null)
            return "";

        int ressource = entry.isNoteSubmitted() ? R.raw.note_submitted_but_not_yet_removed
                : R.raw.note_entered_but_not_submitted;

        String template = readInputStreamIntoString(getResources().openRawResource(ressource));

        final String note = entry.getNote() != null && entry.getNote().length() > 0 ? "&#147;" + entry.getNote()
                + "&#148;" : "<< No Annotation >>";
        String html = Pattern.compile("##NOTE##", Pattern.DOTALL).matcher(template).replaceAll(
                note.replaceAll("\n", "<br/>"));
        final String notShared = entry.shouldNoteBeShared() ? "" : "not";
        html = Pattern.compile("##NOT_SHARED##", Pattern.DOTALL).matcher(html).replaceAll(notShared);
        return html;
    }

    private boolean isContextMenuLaunchedOnLink() {
        return lastLongClickTarget != null && WebView.class.equals(lastLongClickTarget.getClass())
                && webView.getHitTestResult().getExtra() != null
                && webView.getHitTestResult().getExtra().startsWith("http");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (isContextMenuLaunchedOnLink())
            return false; // let the intents be resolved.

        if (ArticleViewHelper.articleActionSelected(this, item, getEntryManager(), getSelectedEntry()))
            return true;
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean consumed = ArticleViewHelper.articleActionSelected(this, item, getEntryManager(), selectedEntry);
        if (!consumed) {
            if (item.getItemId() == MENU_ITEM_ID_ZOOM_IN) {
                webView.zoomIn();
                return true;
            } else if (item.getItemId() == MENU_ITEM_ID_ZOOM_OUT) {
                webView.zoomOut();
                return true;
            } else if (item.getItemId() == MENU_ITEM_TOGGLE_VIEW_MODE) {
                toggleViewMode(true);
                view();
                return true;
            }

        }
        return false;
    }

    protected void refreshUI() {

        runOnUiThread(new Runnable() {

            public void run() {

                Timing t = new Timing("ShowArticleActivity.refreshUI()", ShowArticleActivity.this);
                try {
                    Entry selectedEntry = getSelectedEntry();
                    if (selectedEntry == null) {
                        finish();
                        return;
                    }

                    ArticleViewHelper.populateEntryView(getContainer(), selectedEntry, getEntryManager(), uiHelper);

                    if (false) {
                        webView.refreshDrawableState();
                        webView.forceLayout();
                        webView.invalidate();
                        webView.postInvalidate();
                    }

                    getContainer().updateState(!contentCursor.isLast() && !contentCursor.isAfterLast(),
                            !contentCursor.isFirst() && !contentCursor.isBeforeFirst());

                    // LATER Necessary?
                    // closeOptionsMenu();
                } finally {
                    t.stop();
                }
            }
        });
    }

    private void dumpHistory() {
        if (false) {
            WebBackForwardList history = webView.copyBackForwardList();
            for (int i = 0, lim = history.getSize(); i < lim; i++) {

                WebHistoryItem hi = history.getItemAtIndex(i);

                System.out.println("XXX" + i + "    url = " + hi.getUrl() + " originalUrl=" + hi.getOriginalUrl());
            }
            System.out.println("XXX current url=" + selectedEntry.getAlternateHRef());
        }
    }

    private void view() {

        if (debug)
            PL.log("ADV: view " + selectedEntry.getTitle(), this);

        // webView.clearHistory();

        adjustScale(selectedEntry);

        if (false)
            webView.getSettings().setLayoutAlgorithm(
                    selectedEntry.isFitToWidthEnabled() ? LayoutAlgorithm.SINGLE_COLUMN
                            : LayoutAlgorithm.NARROW_COLUMNS);

        switch (viewMode) {
        case VIEW_MODE_ALTERNATE:
            // LATER Add file exists to this clause
            viewAlternateContent();
            break;

        case VIEW_MODE_FEED:
        default:
            viewFeedContent();
        }

    }

    public void modelUpdateFinished(ModelUpdateResult result) {
    }

    public void modelUpdateStarted(boolean fastSyncOnly) {
    }

    public void modelUpdated() {
        handler.post(refreshUIRunnable);
    }

    /** Manages the progress bar while the content is loaded */
    private class EmbeddedWebViewClient extends android.webkit.WebViewClient {

        private ProgressBar progressBar;

        public EmbeddedWebViewClient() {
            progressBar = (ProgressBar) findViewById(R.id.progress);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {

            super.onScaleChanged(view, oldScale, newScale);
            lastDownEvent = null; // reset lastDownEvent so that tapping +/- on
            // the
            // zoom
            // doesn't toggle the full screen mode

            if (selectedEntry != null) {
                Feed feed = getEntryManager().findFeedById(selectedEntry.getFeedId());
                if (feed != null) {
                    if (ShowArticleActivity.this.viewMode == VIEW_MODE_ALTERNATE)
                        feed.setWebScale(newScale);
                    else
                        feed.setFeedScale(newScale);
                    getEntryManager().updateFeed(feed);
                }
            }

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            try {
                progressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
                if (webView.getScrollX() != 0 || webView.getScrollY() != 0) {
                    webView.scrollTo(0, 0);
                }

                if (false) {
                    System.out.println("XXXonPageStarted url=" + url);
                    System.out.println("webView.getUrl=" + webView.getUrl());
                    System.out.println("webView.getOriginalUrl=" + webView.getOriginalUrl());
                    System.out.println("selectedEntry.getAlternateHref=" + selectedEntry.getAlternateHRef());
                    System.out.println("selectedEntry.getBaseURL=" + selectedEntry.getBaseUrl(getEntryManager()));
                }

                boolean showingInitialPage = (url.equals("data:;,") || url.equals("about:blank") || (selectedEntry != null && url
                        .equals(selectedEntry.getBaseUrl(getEntryManager()))));
                setWebModeBackgroundColor(showingInitialPage);
                // dumpHistory();
            } catch (android.database.StaleDataException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.INVISIBLE);
            super.onPageFinished(view, url);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            super.onUnhandledKeyEvent(view, event);

            if (event.getKeyCode() == KeyEvent.KEYCODE_SPACE) {
                if (event.isShiftPressed())
                    webView.pageUp(false);
                else
                    webView.pageDown(false);
            }

        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            if ("click".equals(uri.getScheme())) {
                String fragment = uri.getFragment();
                if ("remove".equals(fragment)) {
                    if (selectedEntry != null) {
                        selectedEntry.setNote(null);
                        selectedEntry.setShouldNoteBeShared(false);
                        selectedEntry.setNoteSubmitted(false);
                        getEntryManager().update(selectedEntry);
                    }
                }
                return true;
            }

            // Fallback

            if (url.indexOf("vnd.youtube") >= 0 || url.startsWith("mailto:")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addCategory(Intent.CATEGORY_BROWSABLE);

                try {
                    ShowArticleActivity.this.startActivity(i);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    // If no application can handle the URL, assume that the
                    // browser can handle it.
                }
            }

            if (url.startsWith("http:") || url.startsWith("https:"))
                return false;

            boolean methodExists = false;
            for (Method m : Intent.class.getMethods()) {
                if (m.getName().equals("parseUri")) {
                    methodExists = true;
                    break;
                }
            }

            if (methodExists) {
                boolean result = WebViewHelper6.handleUrl(url, ShowArticleActivity.this);
                if (result == true)
                    return true;
                else
                    return super.shouldOverrideUrlLoading(view, url);
            }

            return super.shouldOverrideUrlLoading(view, url);
        }

    }

    public void statusUpdated() {
    }

    public static String getBodyBottomDecoration() {
        return "&nbsp;<br/>&nbsp;<br/>&nbsp;</br></div></body></html>";
    }

    private String getDateAuthorHtml(Entry entry) {
        String authorName = entry.getAuthor();
        Date date = entry.getUpdated();

        String styleBody = renderInlineStylesheet(entry);

        StringBuilder topDeco = new StringBuilder(
                "<html><head>"
                        + styleBody
                        + "</head><body><div style=\"color: #777; font-size: 1em; margin-top: -12px; margin-bottom: -12px; margin-right: "
                        + (16 * 1) + "px;\">");

        if (authorName != null)
            topDeco.append("<p style=\"float: left; font-size: 0.6em;\">By " + authorName + "</p>");

        try {
            topDeco.append("<p style=\"float: right; font-size: 0.6em;\">" + U.getDateFormat().format(date) + "</p>");
        } catch (Exception e) {
            // ignore
        }
        topDeco.append("</div>\n");
        if (getEntryManager().isLightColorSchemeSelected())
            topDeco.append("<div style=\"clear: both; color: black;\">");
        else
            topDeco.append("<div style=\"clear: both; color: #ccc;\">");

        return topDeco.toString();
    }

    private String renderInlineStylesheet(Entry entry) {

        String styleBody = "\n<style>\n";
        if (true && entry.isFitToWidthEnabled()) {

            // maxWidth = (int) (maxWidth / dm.density);
            int maxWidth = getWidth() - (int) (25 * getDisplayMetrics().density);
            maxWidth /= currentScale;

            styleBody += "img {max-width:" + maxWidth + "px; height:auto;}\n";
        }
        if (!getEntryManager().isLightColorSchemeSelected())
            styleBody += "\na:link {color:#ffffff}\na:visited {color:#bbbbbb}";
        styleBody += getEntryManager().isLightColorSchemeSelected() ? "" : "\nbody {background: #222; color: #ccc;}\n";
        styleBody += "</style>";
        return styleBody;
    }

    public void modelUpdated(String atomId) {
        if (selectedEntry != null && selectedEntry.getAtomId().equals(atomId)) {
            selectedEntry = null; // NOPMD
            selectedEntry = getSelectedEntry();
            atomIdOfCurrentlyShowingArticle = null; // NOPMD
            view();
            refreshUI();
        }
    }

    private int getWidth() {
        return getWindowManager().getDefaultDisplay().getWidth();
    }

    private DisplayMetrics getDisplayMetrics() {
        Display d = getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics(dm);
        return dm;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryUtil.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryUtil.onStop(this);
    }
}