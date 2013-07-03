package com.newsrob.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.newsrob.DBQuery;
import com.newsrob.DashboardListActivity;
import com.newsrob.EntryManager;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.NewsRob;
import com.newsrob.PL;
import com.newsrob.R;
import com.newsrob.jobs.ClearModelSucceeded;
import com.newsrob.jobs.EntryUpdateSucceeded;
import com.newsrob.jobs.Job;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.jobs.SynchronizeModelSucceeded;
import com.newsrob.storage.SdCardStorageAdapter;
import com.newsrob.util.FlurryUtil;
import com.newsrob.util.GoogleAdsUtil;
import com.newsrob.util.SDK11Helper;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public abstract class AbstractNewsRobListActivity extends ListActivity implements IEntryModelUpdateListener,
        View.OnLongClickListener {
    private static final String TAG = AbstractNewsRobListActivity.class.getSimpleName();

    private static final int MENU_ITEM_REFRESH_ID = 1;
    private static final int MENU_ITEM_SETTINGS_ID = 2;
    private static final int MENU_ITEM_CLEAR_CACHE_ID = 3;

    private static final int MENU_ITEM_LOGOUT_ID = 4;
    private static final int MENU_ITEM_HIDE_ID = 5;

    private static final int MENU_ITEM_CANCEL_ID = 6;

    protected static final int MENU_ITEM_MARK_ALL_READ_ID = 7;
    protected static final int MENU_ITEM_MANAGE_FEED_ID = 8;
    protected static final int MENU_ITEM_SUBSCRIBE_FEED_ID = 15;

    protected static final int MENU_ITEM_SEARCH_ID = 9;

    protected static final int MENU_ITEM_FAQ_ID = 10;
    protected static final int MENU_ITEM_TOGGLE_THEME_ID = 11;
    protected static final int MENU_ITEM_SORT_ID = 17;

    protected static final int MENU_ITEM_SHOW_FILTER_ID = 18;

    private static final int DIALOG_SD_CARD_ADVISABLE_ID = 0;
    private static final int DIALOG_CLEAR_CACHE = 1;
    private static final int DIALOG_SHOW_FILTER_INFO_ID = 2;

    Handler handler = new Handler();

    protected EntryManager entryManager;

    private Runnable refreshUIRunnable;

    private int positionOfSelectedItemOnLongPress;

    private ImageButton refreshButton;
    private ImageButton showHideButton;
    private ImageButton markAllReadButton;
    private ImageButton toggleOrderButton;

    private int currentTheme;
    private String currentActionBarLocation;

    private View progressIndicator;

    private ProgressBar progressBar;
    private TextView progressDescription;
    private LinearLayout progressContainer;

    private GoogleAdsUtil googleAdsUtil;

    protected SharedPreferences getSharedPreferences() {
        return getEntryManager().getSharedPreferences();
    }

    protected EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(this);
        return entryManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getEntryManager();

        boolean isHwAccelerationEnabled = EntryManager.getInstance(this).isHardwareAccelerationListsEnabled();
        PL.log("User set hw accel to enabled=" + isHwAccelerationEnabled + " (activity: " + this + ").", this);

        if (isHwAccelerationEnabled)
            SDK11Helper.enableHWAccelerationForActivity(this);

        setTheme(getEntryManager().getCurrentThemeResourceId());
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        googleAdsUtil = new GoogleAdsUtil(entryManager);

        /*
         * WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
         * lp.copyFrom(getWindow().getAttributes()); lp.format =
         * PixelFormat.RGBA_8888; getWindow().setAttributes(lp);
         */

        // setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        super.onCreate(savedInstanceState);

        /*
         * if ("com.newsrob.VIEW".equals(getIntent().getAction())) { Intent i =
         * new Intent(this, ShowArticleActivity.class); i.putExtra("atomId",
         * getIntent().getDataString()); startActivity(i); finish(); } else {
         */

        refreshUIRunnable = new Runnable() {
            public void run() {
                refreshUI();
            }
        };
        getEntryManager().updateLastUsed();
        if (NewsRob.isDebuggingEnabled(this))
            PL.log("onCreate called on " + getClass().getSimpleName(), this);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (!shouldActionBarBeHidden()) {
            findViewById(R.id.control_panel_stub).setVisibility(View.VISIBLE);
            setupButtons();
        }

        // getLayoutInflater().inflate(R.layout.control_panel, parent);

        final ViewGroup parent = (ViewGroup) findViewById(R.id.ad_parent);
        final View controlPanel = findViewById(R.id.control_panel);
        final View statusBar = findViewById(R.id.status_bar);

        if (EntryManager.ACTION_BAR_BOTTOM.equals(getEntryManager().getActionBarLocation())) {

            // put the toolbar at the bottom
            parent.removeView(controlPanel);
            parent.addView(controlPanel);

            // show the status bar
            statusBar.setVisibility(View.VISIBLE);
        } else if (shouldActionBarBeHidden()) {
            // show the status bar
            statusBar.setVisibility(View.VISIBLE);
            if (controlPanel != null)
                controlPanel.setVisibility(View.GONE);
        }

        // RelativeLayout.LayoutParams lp = new
        // RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
        // LayoutParams.WRAP_CONTENT);
        // lp.addRule(RelativeLayout.BELOW, R.id.action_bar)

        if (!shouldActionBarBeHidden()) {
            progressIndicator = findViewById(R.id.background_progress);

            progressIndicator.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    toggleProgressBarVisibility();

                    progressIndicator.postInvalidateDelayed(150);

                }

            });
            progressBar = (ProgressBar) findViewById(R.id.progress_bar);
            progressDescription = (TextView) findViewById(R.id.status_text);

            progressContainer = (LinearLayout) findViewById(R.id.progress_container);

            boolean isLightTheme = getEntryManager().isLightColorSchemeSelected();
            findViewById(com.newsrob.R.id.control_panel).setBackgroundResource(
                    isLightTheme ? R.drawable.list_header_background : R.drawable.list_header_background_dark);
            findViewById(R.id.action_bar).setBackgroundResource(
                    isLightTheme ? R.drawable.label_background : R.drawable.label_background_dark);
            findViewById(R.id.status_bar).setBackgroundResource(
                    isLightTheme ? R.drawable.label_background : R.drawable.label_background_dark);

        }

        getListView().setOnCreateContextMenuListener(this);

        signalBackgroundDataIsTurnedOffOrInAirplaneMode();
        getEntryManager().showReleaseNotes();
        currentTheme = getEntryManager().getCurrentThemeResourceId();
        currentActionBarLocation = getEntryManager().getActionBarLocation();

        Drawable d = getResources().getDrawable(R.drawable.progress_small_white);
        ((ProgressBar) findViewById(R.id.progress_status_bar)).setIndeterminateDrawable(d);

        TextView controlPanelText = (TextView) findViewById(R.id.control_panel_text);
        if (controlPanelText != null) {
            controlPanelText.setText(getDefaultControlPanelTitle());
            controlPanelText.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Toast.makeText(AbstractNewsRobListActivity.this, getToastMessage(), Toast.LENGTH_LONG).show();
                }

            });
        }

        View newsRobLogo = findViewById(R.id.newsrob_logo);
        if (newsRobLogo != null)
            newsRobLogo.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Toast.makeText(AbstractNewsRobListActivity.this, "-> Home", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(AbstractNewsRobListActivity.this, DashboardListActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra("skip", false);
                    finish();
                    startActivity(i);
                }
            });

        googleAdsUtil.showAds(this);

        if (getIntent().hasExtra("showProgress") && getIntent().getBooleanExtra("showProgress", false))
            showProgressBar();

    }

    protected abstract CharSequence getToastMessage();

    protected void toggleProgressBarVisibility() {
        if (progressContainer.getVisibility() == View.VISIBLE)
            hideProgressBar();
        else
            showProgressBar();
    }

    protected void showProgressBar() {
        if (shouldActionBarBeHidden())
            return;

        getEntryManager().updateProgressReportBeenOpened();

        Animation inAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_in);

        progressContainer.setVisibility(View.VISIBLE);
        progressContainer.startAnimation(inAnimation);
    }

    protected void hideProgressBar() {

        if (shouldActionBarBeHidden())
            return;

        PL.log("AbstractNewsRobList.hideProgressBar(" + progressContainer + ")", this);

        if (progressContainer == null)
            return;

        PL.log("AbstractNewsRobList.hideProgressBar2(" + progressContainer + ") visibility="
                + progressContainer.getVisibility(), this);

        if (progressContainer.getVisibility() == View.GONE)
            return;

        Animation outAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_out);
        progressContainer.startAnimation(outAnimation);
        progressContainer.setVisibility(View.GONE);

        PL.log("AbstractNewsRobList.hideProgressBar3(" + progressContainer + ") visibility="
                + progressContainer.getVisibility(), this);

    }

    /**
     * Signal to the user that background data is turned off, if s/he has
     * auto-sync enabled and this is the entry point (task root) Also signal if
     * Airplane mode is on.
     */
    private void signalBackgroundDataIsTurnedOffOrInAirplaneMode() {
        if (getEntryManager().isAutoSyncEnabled()) {
            long minutesSinceLastSignalling = (System.currentTimeMillis() - getEntryManager()
                    .getLastShownThatSyncIsNotPossible()) / 1000 / 60;
            if (minutesSinceLastSignalling > 60) {
                String message = null;

                if (getEntryManager().isInAirplaneMode())
                    message = "You are in Airplane mode.\nSyncing and downloading of articles won't work.";
                else if (!getEntryManager().isBackgroundDataEnabled())
                    message = "The 'Background Data' setting is turned off.\nNo auto-sync will occur. You can sync manually though.";
                else if (!getEntryManager().isSystemwideSyncEnabled())
                    message = "The Android 'Auto-Sync' setting is turned off.\nNo auto-sync will occur. You can sync manually though.";

                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    getEntryManager().updateLastShownThatSyncIsNotPossible();
                }
            }
        }
    }

    protected void hideSortOrderToggle() {
        if (shouldActionBarBeHidden())
            return;

        findViewById(R.id.toggle_order_button).setVisibility(View.GONE);
        findViewById(R.id.toggle_order_divider).setVisibility(View.GONE);
    }

    private void setupButtons() {

        if (shouldActionBarBeHidden())
            return;

        // In Progress Cancel Sync Button
        Button cancelSyncButton = (Button) findViewById(R.id.cancel_sync);
        if (cancelSyncButton != null) {
            cancelSyncButton.setBackgroundResource(R.drawable.custom_button);
            cancelSyncButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    entryManager.cancel();
                }
            });
        }

        refreshButton = (ImageButton) findViewById(R.id.refresh);
        if (refreshButton == null)
            return;

        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if ("Refresh".equals(v.getTag())) {
                    requestRefresh();
                    Toast.makeText(AbstractNewsRobListActivity.this, "Refresh", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showHideButton = (ImageButton) findViewById(R.id.show_hide_button);
        showHideButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                requestToggleHideItems();
                String toastText = getDbQuery().shouldHideReadItems() ? "Unread Articles only" : "All Articles";
                Toast.makeText(AbstractNewsRobListActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }
        });

        markAllReadButton = (ImageButton) findViewById(R.id.mark_all_read_button);
        markAllReadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AbstractNewsRobListActivity.this.instantiateMarkAllReadDialog();
            }
        });

        toggleOrderButton = (ImageButton) findViewById(R.id.toggle_order_button);
        toggleOrderButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                requestToggleSortOrder();
                String toastText = getDbQuery().isSortOrderAscending() ? "Oldest first" : "Newest first";
                Toast.makeText(AbstractNewsRobListActivity.this, toastText, Toast.LENGTH_SHORT).show();
            }
        });

        updateButtons();

    }

    protected void updateButtons() {

        if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation())) {
            ProgressBar progressStatusBar = (ProgressBar) findViewById(R.id.progress_status_bar);

            progressStatusBar.setVisibility(entryManager.isModelCurrentlyUpdated() ? View.VISIBLE : View.INVISIBLE);
        }

        if (shouldActionBarBeHidden())
            return;

        if (refreshButton == null)
            setupButtons();

        if (refreshButton == null)
            return;

        View cancelButton = (View) findViewById(R.id.cancel_sync);
        if (cancelButton != null)
            cancelButton.setEnabled(entryManager.isModelCurrentlyUpdated() && !entryManager.isCancelRequested());

        refreshButton.setTag("Refresh");
        refreshButton.setImageResource(R.drawable.gen_toolbar_icon_sync);
        refreshButton.setEnabled(shouldRefreshButtonBeEnabled());
        refreshButton.setFocusable(refreshButton.isEnabled());

        DBQuery dbq = getDbQuery();
        boolean shouldHideReadItems = dbq.shouldHideReadItems();
        showHideButton.setImageResource(shouldHideReadItems ? R.drawable.gen_toolbar_icon_show
                : R.drawable.gen_toolbar_icon_hide);
        toggleOrderButton
                .setImageResource(getDbQuery().isSortOrderAscending() ? R.drawable.gen_toolbar_icon_sort_order_ascending
                        : R.drawable.gen_toolbar_icon_sort_order_descending);
        markAllReadButton.setEnabled(shouldMarkAllReadButtonBeEnabled());
        markAllReadButton.setFocusable(markAllReadButton.isEnabled());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        // if the action bar is not shown the actions that would go into the bar
        // need to be shown in the menu
        if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation())) {

            menu.add(0, MENU_ITEM_REFRESH_ID, 0, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate)
                    .setShortcut('1', 'r');

            menu.add(0, MENU_ITEM_CANCEL_ID, 0, R.string.menu_cancel).setIcon(
                    android.R.drawable.ic_menu_close_clear_cancel);

            menu.add(0, MENU_ITEM_MARK_ALL_READ_ID, 0, R.string.menu_item_mark_all_read).setShortcut('3', 'm')
                    .setIcon(android.R.drawable.ic_menu_agenda);

            menu.add(0, MENU_ITEM_HIDE_ID, 0, "").setShortcut('4', 'h');

            menu.add(0, MENU_ITEM_SORT_ID, 0, "").setShortcut('5', 'o')
                    .setIcon(android.R.drawable.ic_menu_sort_by_size);

        }

        /*
         * menu.add(0, MENU_ITEM_SEARCH_ID, 0, R.string.menu_item_search)
         * .setIcon(android.R.drawable.ic_search_category_default)
         * .setAlphabeticShortcut(SearchManager.MENU_KEY);
         */

        Intent intent = new Intent();
        intent.setClass(this, SubscribeFeedActivity.class);

        menu.add(0, MENU_ITEM_SUBSCRIBE_FEED_ID, 0, "Subscribe Feed").setIcon(android.R.drawable.ic_menu_add)
                .setShortcut('4', 'f').setIntent(intent);

        menu.add(0, MENU_ITEM_TOGGLE_THEME_ID, 0, R.string.menu_toggle_theme)
                .setIcon(android.R.drawable.ic_menu_slideshow).setShortcut('6', 't');

        menu.add(0, MENU_ITEM_SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences)
                .setShortcut('7', 's');

        menu.add(0, MENU_ITEM_CLEAR_CACHE_ID, 0, R.string.menu_clear_cache).setIcon(android.R.drawable.ic_menu_delete)
                .setShortcut('8', 'c');
        menu.add(0, MENU_ITEM_LOGOUT_ID, 0, R.string.menu_logout).setIcon(android.R.drawable.ic_lock_power_off)
                .setShortcut('9', 'l').setTitleCondensed("Logout");
        if (NewsRob.isDebuggingEnabled(this))
            menu.add(0, MENU_ITEM_SHOW_FILTER_ID, 99, "Show Filter (Debug)")
                    .setIcon(android.R.drawable.ic_menu_info_details).setShortcut('8', 'c');

        //

        Uri uri = Uri.parse("http://bit.ly/nrfaq");
        // Uri uri =
        // Uri.parse("http://groups.google.com/group/newsrob/web/frequently-asked-questions");
        menu.add(0, MENU_ITEM_FAQ_ID, 0, R.string.menu_faq).setIcon(android.R.drawable.ic_menu_help)
                .setTitleCondensed("FAQ").setIntent(new Intent(Intent.ACTION_VIEW, uri));

        if (entryManager.canNewsRobProBeBought()) {
            final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(Uri.parse("market://details?id=" + EntryManager.PRO_PACKAGE_NAME));
            menu.add(0, ArticleViewHelper.MENU_ITEM_BUY_NEWSROB_PRO, 30, "Buy NewsRob Pro!").setIntent(viewIntent)
                    .setTitleCondensed("Buy").setIcon(R.drawable.gen_auto_app_icon);
        }

        return result;
    }

    protected boolean shouldRefreshButtonBeEnabled() {
        return getEntryManager().canRefresh();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean canRefresh = getEntryManager().canRefresh();

        if (EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation())) {
            menu.findItem(MENU_ITEM_REFRESH_ID).setEnabled(canRefresh);
            menu.findItem(MENU_ITEM_MARK_ALL_READ_ID).setEnabled(shouldMarkAllReadButtonBeEnabled());

            MenuItem hideMenuItem = menu.findItem(MENU_ITEM_HIDE_ID);
            menu.findItem(MENU_ITEM_CANCEL_ID).setEnabled(
                    !getEntryManager().isCancelRequested() && getEntryManager().isModelCurrentlyUpdated());

            if (getDbQuery().shouldHideReadItems()) {
                hideMenuItem.setTitle("Show Read Articles");
                hideMenuItem.setTitleCondensed("Show Read Articles");
                hideMenuItem.setIcon(android.R.drawable.ic_lock_silent_mode_off);
            } else {
                hideMenuItem.setTitle("Hide Read Articles");
                hideMenuItem.setTitleCondensed("Hide Read Articles"); // I18N
                hideMenuItem.setIcon(android.R.drawable.ic_lock_silent_mode);
            }

            MenuItem sortMenuItem = menu.findItem(MENU_ITEM_SORT_ID);
            if (getDbQuery().isSortOrderAscending()) {
                sortMenuItem.setTitle("Newest first");
            } else {
                sortMenuItem.setTitle("Oldest first");
            }

        }

        menu.findItem(MENU_ITEM_CLEAR_CACHE_ID).setEnabled(canRefresh);
        menu.findItem(MENU_ITEM_LOGOUT_ID).setEnabled(!getEntryManager().needsSession() && canRefresh);

        return super.onPrepareOptionsMenu(menu);
    }

    private boolean shouldActionBarBeHidden() {
        return EntryManager.ACTION_BAR_GONE.equals(getEntryManager().getActionBarLocation());
    }

    protected boolean shouldMarkAllReadButtonBeEnabled() {

        DBQuery dbq = new DBQuery(getDbQuery());
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
        return getEntryManager().isMarkAllReadPossible(dbq);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // AdUtil.onConfigurationChanged(this, newConfig);
        googleAdsUtil.showAds(this);
        refreshUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case MENU_ITEM_SETTINGS_ID:
            if (false) {
                Intent intent = new Intent();

                intent.setClassName("com.newsrob", "com.newsrob.activities.ArticleListActivity");

                intent.putExtra("FEED_URL", "xxx"); // http://www.spiegel.de/schlagzeilen/index.rss

                startActivity(intent);
                return true;
            } else {

                Intent i = new Intent().setClass(this, SettingsActivity.class);
                this.startActivity(i);
                return true;

            }

        case MENU_ITEM_CLEAR_CACHE_ID:
            showConfirmationDialog("Clear the Cache?", new Runnable() {

                @Override
                public void run() {
                    getEntryManager().requestClearCache(handler);
                }
            });

            return true;

        case MENU_ITEM_LOGOUT_ID:
            // final ProgressDialog dialog2 = ProgressDialog.show(this,
            // U.t(this,
            // R.string.logout_and_clear_cache_dialog_title), U.t(this,
            // R.string.logout_and_clear_cache_dialog_message), true);

            showConfirmationDialog("Logout and Clear Cache?", new Runnable() {

                public void run() {

                    Log.d(TAG, "Logging out ...");
                    getEntryManager().logout();
                    getEntryManager().requestClearCache(handler);
                    // dialog2.dismiss();
                }
            });

            return true;

        case MENU_ITEM_HIDE_ID:
            requestToggleHideItems();
            return true;

        case MENU_ITEM_REFRESH_ID:
            requestRefresh();
            // refreshButton.performClick();
            return true;

        case MENU_ITEM_CANCEL_ID:
            getEntryManager().cancel();
            return true;

        case MENU_ITEM_MARK_ALL_READ_ID:
            instantiateMarkAllReadDialog();
            return true;

        case MENU_ITEM_TOGGLE_THEME_ID:
            getEntryManager().toggleTheme();
            reopenIfThemeOrActionBarLocationChanged();
            return true;

        case MENU_ITEM_SORT_ID:
            requestToggleSortOrder();
            return true;

        case MENU_ITEM_SEARCH_ID:
            onSearchRequested();
            return true;

        case MENU_ITEM_SHOW_FILTER_ID:
            showDialog(DIALOG_SHOW_FILTER_INFO_ID);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void instantiateMarkAllReadDialog() {
        instantiateMarkAllReadDialog(getDbQuery());
    }

    protected void requestToggleHideItems() {
        getDbQuery().toggleHideItems();
        modelUpdated();
    }

    protected void requestToggleSortOrder() {
        getDbQuery().toggleSortOrder();
        modelUpdated();
    }

    protected void requestRefresh() {
        if (NewsRob.isDebuggingEnabled(this))
            PL.log("ANRLA: User requested refresh manually.", this);

        if (getEntryManager().needsSession())
            this.startActivity(new Intent().setClass(this, LoginActivity.class));
        else
            getEntryManager().requestSynchronization(false);
    }

    protected void instantiateMarkAllReadDialog(DBQuery dbq) {
        instantiateMarkAllReadDialog(dbq.getFilterLabel(), dbq.getFilterFeedId(), dbq.getStartDate(),
                dbq.getDateLimit(), dbq.isSortOrderAscending(), dbq.getLimit());
    }

    protected void instantiateMarkAllReadDialog(final String filterLabel, final Long filterFeedId,
            final long startDate, final long dateLimit, boolean sortDateAscending, int limit) {

        final DBQuery dbq = new DBQuery(getEntryManager(), filterLabel, filterFeedId);
        dbq.setStartDate(startDate);
        dbq.setDateLimit(dateLimit);
        dbq.setSortOrderAscending(sortDateAscending);
        dbq.setLimit(limit);
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

        final long noOfArticlesMarkedAsRead = getEntryManager().getMarkAllReadCount(dbq);

        final Runnable action = new Runnable() {
            public void run() {

                Toast.makeText(
                        AbstractNewsRobListActivity.this,
                        noOfArticlesMarkedAsRead + " article" + (noOfArticlesMarkedAsRead > 1 ? "s" : "")
                                + " marked as read.", Toast.LENGTH_LONG).show();
                if (getEntryManager().shouldHideReadItems())
                    getListView().setSelection(0);
                getEntryManager().requestMarkAllAsRead(dbq);
            }
        };

        if (noOfArticlesMarkedAsRead >= getEntryManager().getMarkAllReadConfirmationDialogThreshold()) {

            boolean pl = noOfArticlesMarkedAsRead > 1;
            String message = "Mark " + (pl ? "all " : "") + noOfArticlesMarkedAsRead + " article" + (pl ? "s" : "")
                    + " read?";

            showConfirmationDialog(message, action);
        } else
            action.run();

    }

    void showConfirmationDialog(String message, final Runnable action) {

        AlertDialog dialog = new AlertDialog(this) {
        };

        dialog.setMessage(message);

        dialog.setButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                action.run();
            }

        });
        dialog.setButton2(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }

        });
        dialog.setTitle("Please Confirm!");
        dialog.setIcon(android.R.drawable.ic_dialog_alert);

        dialog.show();
    }

    public boolean onLongClick(View v) {
        try {
            positionOfSelectedItemOnLongPress = getListView().getPositionForView(v);
            if (positionOfSelectedItemOnLongPress > -1)
                openContextMenu(getListView());
            return true;
        } catch (NullPointerException npe) {
            //
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (menuInfo != null && menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            positionOfSelectedItemOnLongPress = mi.position;
        }
        if (positionOfSelectedItemOnLongPress > -1)
            onCreateContextMenu(menu, v, menuInfo, positionOfSelectedItemOnLongPress);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (positionOfSelectedItemOnLongPress > -1 && onContextItemSelected(item, positionOfSelectedItemOnLongPress))
            return true;
        return super.onContextItemSelected(item);
    }

    protected abstract boolean onContextItemSelected(MenuItem item, int positionOfSelectedItemOnLongPress);

    protected abstract void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo,
            int positionOfSelectedItemOnLongPress);

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_SD_CARD_ADVISABLE_ID:
            return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.advise_sdcard_read_only).setTitle(R.string.advise_sdcard_read_only_title)
                    .setPositiveButton(android.R.string.ok, null).create();

        case DIALOG_CLEAR_CACHE:
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle(R.string.clear_cache_dialog_title);
            pd.setMessage(U.t(this, R.string.clear_cache_dialog_message));
            pd.setIndeterminate(true);
            return pd;

        case DIALOG_SHOW_FILTER_INFO_ID:
            return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getDbQuery().toString()).setTitle("Current Filter")
                    .setPositiveButton(android.R.string.ok, null).create();
        }

        return super.onCreateDialog(id);

    }

    protected void deactivateProgressIndicator() {

        PL.log("AbstractNewsRobList.deactivateProgressIndicator(" + progressIndicator + ")", this);

        if (progressIndicator == null)
            return;

        if (!shouldActionBarBeHidden()) {
            progressIndicator.setVisibility(View.GONE);
            if (refreshButton != null)
                refreshButton.setVisibility(View.VISIBLE);
        }

        setTitle(getDefaultStatusBarTitle());
        hideProgressBar();
    }

    public abstract String getDefaultStatusBarTitle();

    public abstract String getDefaultControlPanelTitle();

    protected void activateProgressIndicator() {
        if (shouldActionBarBeHidden())
            return;

        if (progressIndicator != null)
            progressIndicator.setVisibility(View.VISIBLE);

        if (refreshButton != null)
            refreshButton.setVisibility(View.GONE);

        String status = "Background Operation In Progress";
        Job runningJob = entryManager.getCurrentRunningJob();

        if (runningJob != null) {
            status = runningJob.getJobDescription();
            if (runningJob.isProgressMeassurable()) {
                int[] progress = runningJob.getProgress();
                int currentArticle = progress[0];
                int allArticles = progress[1];
                progressBar.setMax(allArticles);
                progressBar.setProgress(currentArticle);
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.VISIBLE);

                status = runningJob.getJobDescription() + " (" + currentArticle + "/" + allArticles + ")";
            } else {
                progressBar.setMax(0);
                progressBar.setProgress(0);
                progressBar.setIndeterminate(true);

            }

        } else {
            progressBar.setMax(0);
            progressBar.setProgress(0);
            progressBar.setIndeterminate(true);
        }
        progressDescription.setText(status);
        // progressBar.set

        /*
         * 
         * Job runningJob = getEntryManager().getCurrentRunningJob(); if
         * (runningJob == null) { setTitle(getDefaultStatusBarTitle()); return;
         * }
         * 
         * if (runningJob.isProgressMeassurable()) { int[] progress =
         * runningJob.getProgress(); int currentArticle = progress[0]; int
         * allArticles = progress[1]; setTitle(runningJob.getJobDescription() +
         * " (" + currentArticle + "/" + allArticles + ")"); } else {
         * setTitle(runningJob.getJobDescription()); }
         */
    }

    protected void refreshProgressBar() {

        if (entryManager.getCurrentRunningJob() != null || entryManager.isModelCurrentlyUpdated())
            activateProgressIndicator();
        else
            deactivateProgressIndicator();
    }

    public void modelUpdateFinished(final ModelUpdateResult result) {
        handler.post(new Runnable() {

            public void run() {
                updateButtons();
                deactivateProgressIndicator();

                if (result instanceof SynchronizeModelSucceeded) {
                    SynchronizeModelSucceeded succeeded = (SynchronizeModelSucceeded) result;

                    if (succeeded.getNoOfEntriesUpdated() > 0)
                        refreshUI();
                    // Toast.makeText(AbstractNewsRobListActivity.this,
                    // succeeded.getMessage(),
                    // Toast.LENGTH_LONG).show(); // I18N
                } else if (result instanceof ClearModelSucceeded) {
                    ClearModelSucceeded succeeded = (ClearModelSucceeded) result;
                    // Toast.makeText(AbstractNewsRobListActivity.this,
                    // succeeded.getMessage(),
                    // Toast.LENGTH_LONG).show(); // I18N
                    if (succeeded.noOfEntriesDeleted > 0)
                        refreshUI();

                } else if (result instanceof EntryUpdateSucceeded) {
                    refreshUI();
                } else {
                    refreshUI();
                    Toast.makeText(AbstractNewsRobListActivity.this, result.getMessage(), Toast.LENGTH_LONG).show(); // I18N
                }
            }
        });
    }

    public void modelUpdateStarted(boolean fastSyncOnly) {
        runOnUiThread(refreshUIRunnable);

        if (!fastSyncOnly && EntryManager.ACTION_BAR_TOP.equals(entryManager.getActionBarLocation())
                && View.VISIBLE != progressContainer.getVisibility() && !entryManager.hasProgressReportBeenOpened()) {
            refreshButton.postDelayed(new Runnable() {

                @Override
                public void run() {
                    Animation inAnimation = AnimationUtils.loadAnimation(AbstractNewsRobListActivity.this,
                            R.anim.bouncing);

                    final ImageView arrow = (ImageView) findViewById(R.id.bouncing_arrow);
                    final View hint = findViewById(R.id.show_open_progress_hint);

                    if (arrow != null) {

                        arrow.setImageResource(R.drawable.gen_arrow_up); // Cupcake
                        inAnimation.setAnimationListener(new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {

                                int[] loc = new int[2];
                                refreshButton.getLocationOnScreen(loc);

                                int y = (loc[0] + refreshButton.getWidth() / 2) - arrow.getWidth() / 2;

                                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, y, 0);
                                arrow.setLayoutParams(lp);
                                // findViewById(R.id.list_parent).requestLayout();

                                arrow.setVisibility(View.VISIBLE);
                                hint.setVisibility(View.VISIBLE);

                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                arrow.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        arrow.setVisibility(View.INVISIBLE);
                                        hint.setVisibility(View.INVISIBLE);

                                    }
                                }, 6000);

                            }
                        });

                        arrow.startAnimation(inAnimation);
                    }
                }
            }, 2500);

        }
    }

    public void modelUpdated() {
        final String className = getClass().getSimpleName();

        new Thread(new Runnable() {

            @Override
            public void run() {
                showDataUpdateProgressMonitor();
                try {
                    Timing t = new Timing(className + "::modelUpdated", AbstractNewsRobListActivity.this);
                    Cursor newCursor = createCursorFromQuery(getDbQuery());

                    // force the cursor to be loaded,
                    // so
                    // that this needn't be done on the UI
                    // thread
                    newCursor.moveToFirst();

                    newCursor(newCursor, (CursorAdapter) getListAdapter());

                    runOnUiThread(refreshUIRunnable);
                    t.stop();
                } finally {
                    hideDataUpdateProgressMonitor();
                }
            }

        }).start();

        runOnUiThread(refreshUIRunnable);
    }

    void showDataUpdateProgressMonitor() {
        if (true)
            return;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ProgressBar pb = (ProgressBar) findViewById(R.id.data_update_progress);
                pb.setVisibility(View.VISIBLE);
            }
        });

    }

    void hideDataUpdateProgressMonitor() {
        if (true)
            return;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                findViewById(R.id.data_update_progress).setVisibility(View.INVISIBLE);
            }
        });

    }

    void refreshUI() {
        // ((BaseAdapter) getListAdapter()).notifyDataSetChanged(); // TODO what
        // is
        // this good
        // for?

        updateButtons();
        updateControlPanelTitle();
        refreshProgressBar();
    }

    public void statusUpdated() {
        runOnUiThread(new Runnable() {
            public void run() {
                refreshProgressBar();
                updateButtons();
            }
        });
    }

    @Override
    protected void onPause() {
        hideProgressBar();
        getEntryManager().removeListener(this);

        if (false) {
            googleAdsUtil.hideAds(this);

            UIHelper.pauseWebViews(this);
        }
        super.onPause();

    }

    @Override
    protected void onResume() {
        NewsRob.lastActivity = this;

        super.onResume();

        if (false)
            UIHelper.resumeWebViews(this);

        getEntryManager().addListener(this);
        if (!reopenIfThemeOrActionBarLocationChanged()) {

            getDbQuery().updateShouldHideReadItems();
            modelUpdated();

            refreshUI(); // LATER Maybe I should maintain and check lastModified
            // and
            // conditionally do refreshUI();

            checkIfSDCardAccessible();
            // AdUtil.publishAd(this);

            googleAdsUtil.showAds(this);

        }

    }

    private void updateControlPanelTitle() {
        TextView controlPanelTitle = (TextView) findViewById(R.id.control_panel_title);
        controlPanelTitle.setText(getDefaultStatusBarTitle());
    }

    private boolean reopenIfThemeOrActionBarLocationChanged() {
        if (currentTheme != getEntryManager().getCurrentThemeResourceId()
                || !getEntryManager().getActionBarLocation().equals(currentActionBarLocation)) {
            finish();
            PL.log("AbstractNewsRobListActivity. Change detected that makes a relaunch necessary.", this);
            startActivity(getIntent());
            return true;
        }
        return false;
    }

    private void checkIfSDCardAccessible() {
        if (entryManager.getStorageAdapter() instanceof SdCardStorageAdapter)
            if (isTaskRoot() && !entryManager.getStorageAdapter().canWrite())
                showDialog(DIALOG_SD_CARD_ADVISABLE_ID);

    }

    protected void startShowEntryActivityForPosition(int position, DBQuery dbq) {
        Intent i = new Intent(AbstractNewsRobListActivity.this, ShowArticleActivity.class);
        i.putExtra(UIHelper.EXTRA_KEY_POSITION, position);
        UIHelper.addExtrasFromDBQuery(i, dbq);
        startActivity(i);
    }

    abstract protected DBQuery getDbQuery();

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

    abstract protected Cursor createCursorFromQuery(DBQuery dbq);

    /** This will run on the UI Thread */
    protected void newCursor(final Cursor newCursor, final CursorAdapter adapter) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Timing t = new Timing(this.getClass().getSimpleName() + "::newCursor()",
                        AbstractNewsRobListActivity.this);

                Timing t2 = new Timing("CursorAdapter::changeCursor", AbstractNewsRobListActivity.this);
                Cursor existingCursor = adapter.getCursor();
                adapter.changeCursor(newCursor);
                t2.stop();
                startManagingCursor(newCursor);

                if (existingCursor != null) {
                    stopManagingCursor(existingCursor);
                    existingCursor.close();
                }
                t.stop();
            }
        });
    }
}
