package com.newsrob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.http.client.ClientProtocolException;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.Toast;

import com.google.ads.AdSenseSpec;
import com.google.ads.AdSenseSpec.AdType;
import com.google.ads.AdSenseSpec.ExpandDirection;
import com.newsrob.DB.Entries;
import com.newsrob.DB.EntryLabelAssociations;
import com.newsrob.EntriesRetriever.AuthToken;
import com.newsrob.EntriesRetriever.StateChange;
import com.newsrob.EntriesRetriever.AuthToken.AuthType;
import com.newsrob.appwidget.UnreadWidgetProvider;
import com.newsrob.appwidget.WidgetPreferences;
import com.newsrob.auth.AccountManagementUtils;
import com.newsrob.auth.IAccountManagementUtils;
import com.newsrob.download.WebPageDownloadDirector;
import com.newsrob.jobs.Job;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.jobs.SwitchStorageProviderFailed;
import com.newsrob.jobs.SwitchStorageProviderResult;
import com.newsrob.locale.FireReceiver;
import com.newsrob.locale.MockEditSettingsActivity;
import com.newsrob.storage.IStorageAdapter;
import com.newsrob.storage.PhoneMemoryStorageAdapter;
import com.newsrob.storage.SdCardStorageAdapter;
import com.newsrob.util.Base64;
import com.newsrob.util.SDK9Helper;
import com.newsrob.util.SDKVersionUtil;
import com.newsrob.util.SingleValueStore;
import com.newsrob.util.Timing;

/**
 * Entry Manager is the public interface to entries. The views go through the
 * Entry Manager to query for Entries.
 * 
 * It used the EntriesRetriever to retrieve entries from the GoogleReader
 * service. It stores the retrieved entries in a SQLiteDatabase using the
 * DatabaseHelper. The Entries are instantiated by the DatabaseHelper.
 * 
 * @author mkamp
 * 
 */
public class EntryManager implements SharedPreferences.OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
    public static final String SETTINGS_SYNC_NEWSROB_ONLY_ENABLED = "settings_sync_newsrob_only_enabled";

    private static final String DEVICE_MODEL_DESIRE = "HTC Desire";
    private static final String DEVICE_MODEL_EVO = "PC36100";
    private static final String DEVICE_MODEL_DROID_INCREDIBLE = "ADR6300";
    private static final String DEVICE_MODEL_DELL_STREAK = "Dell Streak";
    private static final String DEVICE_MODEL_ARCHOS_7 = "A70HB";

    private static final String SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY = "unread_articles_only";

    public static final String THEME_LIGHT = "Light";
    public static final String THEME_DARK = "Dark";

    public static final String ACTION_BAR_TOP = "top";
    public static final String ACTION_BAR_BOTTOM = "bottom";
    public static final String ACTION_BAR_GONE = "gone";

    private static final String SETTINGS_SYNC_TYPE = "settings_sync_type";

    static final String DOWNLOAD_NO = "no";

    public static final String DOWNLOAD_YES = "yes";

    static final String DOWNLOAD_WIFI_ONLY = "wifi-only";

    private static final String TAG = EntryManager.class.getSimpleName();

    public static final String SETTINGS_AUTOMATIC_REFRESHING_ENABLED = "settings_automatic_refreshing_enabled2";
    static final String SETTINGS_ENTRY_MANAGER_CAPACITY = "settings_entry_manager_entries_capacity";
    public static final String SETTINGS_AUTOMATIC_REFRESH_INTERVAL = "settings_automatic_refresh_interval";
    static final String SETTINGS_HIDE_READ_ITEMS = "settings_hide_read_items";
    static final String SETTINGS_LAST_SYNCED_SUBSCRIPTIONS = "last_synced_subscriptions";

    public static final String SETTINGS_STORAGE_ASSET_DOWNLOAD = "storage_asset_download";
    public static final String SETTINGS_STORAGE_PROVIDER_KEY = "settings_storage_provider";

    private static final String SETTINGS_MARK_ALL_READ_CONFIRMATION_DIALOG_THRESHOLD = "settings_ui_mark_all_read_confirmation_threshold2";

    static final String SETTINGS_GLOBAL_DOWNLOAD_PREF_KEY = "settings_global_download_pref";
    public static final String SETTINGS_PASS = "settings_last_used_u";
    static final String SETTINGS_REMEMBER_PASSWORD = "settings_remember_password";

    static final String SETTINGS_INSTALLED_AT = "com.newsrob.installed_at";

    public static final String SETTINGS_NEXT_SCHEDULED_SYNC_TIME = "next_scheduled_sync_time";
    public static final String SETTINGS_LAST_SYNC_TIME = "last_sync_time";
    public static final String SETTINGS_LAST_SYNC_COMPLETE = "last_sync_complete";

    private static final String SETTINGS_OPENED_PROGRESS_REPORT = "settings_has_opened_progress_report";

    static final String SETTINGS_LICENSE_ACCEPTED = "settings_license_accepted";
    static final String SETTINGS_ELLIPSIZE_TITLES_ENABLED = "settings_ellipsize_titles_enabled2";

    public static final String SETTINGS_HOVERING_ZOOM_CONTROLS_ENABLED = "settings_hovering_zoom_controls_enabled";
    static final String SETTINGS_HOVERING_BUTTONS_NAVIGATION_ENABLED = "settings_hovering_buttons_navigation_enabled";
    static final String SETTINGS_VOLUME_CONTROL_NAVIGATION_ENABLED = "settings_volume_control_navigation_enabled";
    static final String SETTINGS_CAMERA_BUTTON_CONTROLS_READ_STATE_ENABLED = "settings_camera_button_controls_read_state_enabled";

    static final String SETTINGS_ALWAYS_USE_SSL = "settings_always_use_ssl";

    static final String STORAGE_PROVIDER_SD_CARD = "sdcard";
    static final String STORAGE_PROVIDER_PHONE = "phone";

    private static final String SETTINGS_GOOGLE_USER_ID = "settings_google_user_id";

    public static final int LAST_VERSION_CHECK_INTERVAL_MINUTES = 24 * 60;

    static final String SETTINGS_AUTH_TOKEN = "settings_auth_token";
    static final String SETTINGS_AUTH_TYPE = "settings_auth_type";

    static final String SETTINGS_EMAIL = "email";

    private static final String SETTINGS_GR_UPDATED_KEY = "com.newsrob.gr_updated";

    public static final String SETTINGS_KEEP_STARRED = "settings_keep_starred";
    public static final String SETTINGS_KEEP_SHARED = "settings_keep_shared";
    public static final String SETTINGS_KEEP_NOTES = "settings_keep_notes";

    public static final String SETTINGS_INCREMENTAL_SYNC_ENABLED = "settings_incremental_syncing_enabled";

    public static final String SETTINGS_MOBILIZER = "settings_mobilizer";
    public static final String SETTINGS_PLUGINS = "settings_plugins";

    public static final String SETTINGS_UI_THEME = "settings_ui_theme";
    public static final String SETTINGS_UI_RICH_ARTICLE_LIST_ENABLED = "settings_ui_rich_articles_enabled";

    private static final String SETTINGS_UI_SORT_DEFAULT_NEWEST_FIRST = "settings_ui_sort_newest_first";

    private static final String SETTINGS_VIBRATE_FIRST_LAST_ENABLED = "settings_vibrate_first_last_enabled";

    private static final int MAX_ARTICLES_IN_ARTICLE_LIST = 250;

    private final DB databaseHelper;

    private EntriesRetriever grf;

    private static EntryManager instance;

    private final Context ctx;

    private boolean isModelCurrentlyUpdated = false;

    private final Collection<IEntryModelUpdateListener> listeners = new ArrayList<IEntryModelUpdateListener>(1);

    private IStorageAdapter fileContextAdapter;

    private final NewsRobNotificationManager newsRobNotificationManager;

    private final SharedPreferences sharedPreferences;

    protected Job currentRunningJob;

    private boolean cancelRequested;

    Thread runningThread;

    private final NewsRobScheduler scheduler;

    long modelLockedAt;
    String modelLockedBy;

    private ConnectivityManager connectivityManager;

    private Boolean proVersion;

    private Boolean isAndroidMarketInstalled;

    private TimerTask updateWidgetsTimerTask;
    private Timer updateWidgetsTimer;
    private Long lastUpdateWidgetUpdate;

    public static final String PRO_PACKAGE_NAME = "com.newsrob.pro";
    public static final String LEGACY_PACKAGE_NAME = "com.newsrob.threetosix";
    public static final String MARKET_PACKAGE_NAME = "com.android.vending";

    public static final String EXTRA_LOGIN_EXPIRED = "EXTRA_LOGIN_EXPIRED";

    public static final String EXTRA_CAPTCHA_TOKEN = "EXTRA_CAPTCHA_TOKEN";
    public static final String EXTRA_CAPTCHA_URL = "EXTRA_CAPTCHA_URL";

    private static final String SETTINGS_UI_TEXT_SIZE = "settings_ui_text_size";

    private static final String SETTING_SWIPE_ARTICLE_DETAIL_VIEW = "settings_swipe_article_detail_view";
    private static final String SETTING_SWIPE_ARTICLE_LIST = "settings_swipe_article_list";

    public static final String SETTINGS_SYNC_IN_PROGRESS_NOTIFICATION = "settings_sync_in_progress_enabled";

    private static final String SETTINGS_LAST_SUCCESSFUL_LOGIN = "settings_last_successful_login";

    private static final String SETTINGS_FIRST_INSTALLED_VERSION = "settings_first_installed_version";

    public static final String SETTINGS_UI_ACTION_BAR_LOCATION = "settings_ui_action_bar";

    private static final String SETTINGS_USAGE_DATA_COLLECTION_PERMISSION_VERSION = "usage_data_collection_version";
    public static final String SETTINGS_USAGE_DATA_PERMISSION_GRANTED = "usage_data_permission_granted";

    private static final String SETTINGS_ALWAYS_EXACT_SYNC = "settings_always_exact_sync";

    private int currentThemeId = 0;

    private Map<DBQuery, Boolean> isMarkAllReadPossibleCache;

    private SingleValueStore singleValueStore;

    private AdSenseSpec adSenseSpec;

    private Integer articleCountCache;

    private HashMap<DBQuery, Integer> contentCountCache;

    private EntryManager(final Context context) {

        singleValueStore = new SingleValueStore(context);

        isMarkAllReadPossibleCache = new HashMap<DBQuery, Boolean>();
        contentCountCache = new HashMap<DBQuery, Integer>();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ctx = context;

        databaseHelper = new DB(context, NewsRob.getDebugProperties(context).getProperty("databasePath", null));

        String storageProviderPreference = getSharedPreferences().getString(SETTINGS_STORAGE_PROVIDER_KEY, null);
        if (storageProviderPreference == null) {// no default set yet
            storageProviderPreference = SdCardStorageAdapter.isAdvisable() ? STORAGE_PROVIDER_SD_CARD
                    : STORAGE_PROVIDER_PHONE;

            SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_STORAGE_PROVIDER_KEY,
                    storageProviderPreference));
        }

        fileContextAdapter = STORAGE_PROVIDER_SD_CARD.equals(storageProviderPreference) ? new SdCardStorageAdapter(
                context) : new PhoneMemoryStorageAdapter(context);

        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        scheduler = new NewsRobScheduler(context, this);

        newsRobNotificationManager = new NewsRobNotificationManager(ctx);
        addListener(newsRobNotificationManager);
        setUpAdSenseSpec();

    }

    private void setUpAdSenseSpec() {
        // ("ca-mb-app-pub-2595622705667578") ca-mb-app-test expandedvideotest
        // android news technology tech google reader rss atom mobile
        adSenseSpec = new AdSenseSpec("ca-mb-app-pub-2595622705667578")
                .setCompanyName("Mariano Kamp")
                .setAppName("NewsRob")
                .setChannel("7593515733")
                .setAdType(AdType.TEXT_IMAGE)
                .setExpandDirection(ExpandDirection.TOP)
                .setKeywords(
                        "android news technology tech finance vip gossip photography friends google reader newsreader rss atom mobile offline")
                .setAdTestEnabled(false);

    }

    public AdSenseSpec getAdSenseSpec() {
        return adSenseSpec;
    }

    public int getMyVersionCode() {
        PackageInfo packageInfo;
        try {
            packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            throw new IllegalStateException(ctx.getPackageName() + " was not found when quering the Package Manager.",
                    e);
        }
        return packageInfo.versionCode;

    }

    public String getMyVersionName() {
        PackageInfo packageInfo;
        try {
            packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            throw new IllegalStateException(ctx.getPackageName() + " was not found when quering the Package Manager.",
                    e);
        }
        return packageInfo.versionName + (isProVersion() ? " Pro" : "");
    }

    public synchronized EntriesRetriever getEntriesRetriever() {
        if (grf == null)
            grf = new EntriesRetriever(getContext());
        return grf;
    }

    private void switchStorageProvider() {

        Log.d(TAG, "Switch Storage Provider");

        if (isModelCurrentlyUpdated())
            return;

        final String newPrefValue = getSharedPreferences().getString(SETTINGS_STORAGE_PROVIDER_KEY, null);
        final String oldStorageProviderClass = fileContextAdapter.getClass().getName();

        final String newStorageProviderClass = STORAGE_PROVIDER_SD_CARD.equals(newPrefValue) ? SdCardStorageAdapter.class
                .getName()
                : PhoneMemoryStorageAdapter.class.getName();
        if (!oldStorageProviderClass.equals(newStorageProviderClass)) {

            runningThread = new Thread(new Runnable() {

                public void run() {
                    final PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                    final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                    Log.i(TAG, "Wake lock acquired at " + new Date().toString() + ".");
                    wl.acquire();
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    final Timing t = new Timing("Storage Provider Switch", ctx);
                    ModelUpdateResult result = null;
                    if (isModelCurrentlyUpdated())
                        return;
                    try {
                        lockModel("EM.switchStorageProvider.run");
                    } catch (final IllegalStateException ise) {
                        return;
                    }
                    try {

                        Log.i(TAG, "Switching storage providers started at " + new Date().toString() + ".");

                        fireModelUpdateStarted("Switching storage providers", false, true);
                        Log.d(TAG, "Change of storage provider detected.");

                        final List<Job> jobList = new LinkedList<Job>();

                        final Job clearOldStorageProvider = new Job("Clearing Old Storage Provider", EntryManager.this) {

                            @Override
                            public void run() {
                                Log.d(TAG, "Clearing the old storage provider.");
                                doClearCache();
                                if (fileContextAdapter.canWrite())
                                    WebPageDownloadDirector.removeAllAssets(fileContextAdapter);
                            }

                        };
                        jobList.add(clearOldStorageProvider);
                        final Job switchStorageProviders = new Job("Switching Storage Providers", EntryManager.this) {

                            @Override
                            public void run() throws Exception {
                                Log.d(TAG, "Establishing new storage provider: " + newStorageProviderClass);
                                fileContextAdapter = newStorageProviderClass.equals(SdCardStorageAdapter.class
                                        .getName()) ? new SdCardStorageAdapter(ctx)
                                        : new PhoneMemoryStorageAdapter(ctx);

                                Log.d(TAG, "New storage provider established.");
                            }

                        };
                        jobList.add(switchStorageProviders);

                        final Job clearNewStorageProvider = new Job("Clearing New Storage Provider", EntryManager.this) {

                            @Override
                            public void run() {
                                Log.d(TAG, "Clearing the new storage provider.");
                                doClearCache();
                                if (fileContextAdapter.canWrite())
                                    WebPageDownloadDirector.removeAllAssets(fileContextAdapter);
                            }

                        };
                        jobList.add(clearNewStorageProvider);

                        runJobs(jobList);

                        result = new SwitchStorageProviderResult();

                    } catch (final Throwable throwable) {
                        result = new SwitchStorageProviderFailed(throwable);
                        Log.d(TAG, "Problem during switching storage providers.", throwable);
                        t.stop();
                    } finally {
                        unlockModel("EM.switchStorageProvider.run");
                        clearCancelState();
                        fireModelUpdateFinished(result);
                        fireStatusUpdated();
                        Log.i(TAG, "Switching storage providers finished at " + new Date().toString() + ".");

                        wl.release();
                        t.stop();
                    }
                }

            }, "Storage Provider Switch Worker");
            runningThread.start();
        }
    }

    void runJob(final Job job) throws Throwable {
        final ArrayList<Job> jobList = new ArrayList<Job>(1);
        jobList.add(job);
        runJobs(jobList);
    }

    void runJobs(final List<Job> jobList) throws Throwable {
        Throwable caughtThrowable = null;
        for (final Job job : jobList) {
            try {
                if (isCancelRequested())
                    break;
                Log.d(TAG, "Started job: " + job.getJobDescription());
                EntryManager.this.currentRunningJob = job;
                fireStatusUpdated();
                if (NewsRob.isDebuggingEnabled(ctx))
                    PL.log("Existing Articles (before " + job.getJobDescription() + ")="
                            + EntryManager.this.getArticleCount(), ctx);
                job.run();

                if (NewsRob.isDebuggingEnabled(ctx))
                    PL.log("Existing Articles (after " + job.getJobDescription() + ")="
                            + EntryManager.this.getArticleCount(), ctx);

            } catch (final Throwable throwable) {
                Log.d(TAG, "Caught throwable.", throwable);
                if (caughtThrowable != null) {
                    Log.d(TAG, "Rethrowing it!");
                    throw throwable;
                } else {
                    caughtThrowable = throwable;
                    Log.d(TAG, "Stashing it.");
                }
            } finally {
                Log.d(TAG, "Finished job: " + job.getJobDescription());
                EntryManager.this.currentRunningJob = null;
                fireStatusUpdated();
            }
        }
        if (caughtThrowable != null)
            throw caughtThrowable;
    }

    public boolean canRefresh() {
        return !isModelCurrentlyUpdated() && fileContextAdapter.canWrite();
    }

    public boolean needsSession() {
        String token = getSharedPreferences().getString(SETTINGS_AUTH_TOKEN, null);
        return (token == null || "EXPIRED".equals(token));
    }

    public void doLogin(final Context context, final String email, final String password, String captchaToken,
            String captchaAnswer) throws ClientProtocolException, IOException, AuthenticationFailedException {
        getEntriesRetriever().authenticate(context, email, password, captchaToken, captchaAnswer);
    }

    public void doLogin(String googleUserId, String authToken) {
        // googleUserId is currently already set in onSuccess of LoginActivity
        // LATER
        saveAuthToken(new EntriesRetriever.AuthToken(AuthType.AUTH, authToken));
    }

    public void expireAuthToken() {
        AuthToken authToken = getAuthToken();
        IAccountManagementUtils accountManagementUtils = AccountManagementUtils.getAccountManagementUtils(ctx);
        if (accountManagementUtils != null && authToken != null
                && authToken.getAuthType() == EntriesRetriever.AuthToken.AuthType.AUTH) {
            AccountManagementUtils.getAccountManagementUtils(ctx).invalidateAuthToken(ctx, authToken.getAuthToken());
        }
        SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_AUTH_TOKEN, "EXPIRED"));
    }

    public void clearAuthToken() {
        AuthToken authToken = getAuthToken();
        IAccountManagementUtils accountManagementUtils = AccountManagementUtils.getAccountManagementUtils(ctx);

        if (accountManagementUtils != null && authToken != null
                && authToken.getAuthType() == EntriesRetriever.AuthToken.AuthType.AUTH) {
            AccountManagementUtils.getAccountManagementUtils(ctx).invalidateAuthToken(ctx, authToken.getAuthToken());
        }
        SharedPreferences.Editor editor = getSharedPreferences().edit().remove(SETTINGS_AUTH_TOKEN);
        editor.remove(SETTINGS_AUTH_TYPE);
        SDK9Helper.apply(editor);

    }

    public void saveAuthToken(AuthToken authToken) {
        // Android 2.0 token doesn't need to be saved, can be re-aquired easily
        // update: or so I thought
        /*
         * if (authToken != null && authToken.getAuthType() ==
         * EntriesRetriever.AuthToken.AuthType.AUTH) return;
         */
        PL.log("EM.saveAuthToken token=" + authToken, ctx);
        String authType = String.valueOf(authToken.getAuthType());
        String newAuthToken = authToken.getAuthToken();

        SharedPreferences.Editor editor = getSharedPreferences().edit().putString(SETTINGS_AUTH_TOKEN, newAuthToken);
        editor.putString(SETTINGS_AUTH_TYPE, authType);
        SDK9Helper.apply(editor);

        PL.log("EM.saveAuthToken(2)", ctx);

    }

    public void regetAuthToken() throws OperationCanceledException, AuthenticatorException, IOException {

        PL.log("EM.regetAuthToken()", ctx);
        IAccountManagementUtils accountManagementUtils = AccountManagementUtils.getAccountManagementUtils(ctx);
        AuthToken authToken = new AuthToken(AuthType.AUTH, accountManagementUtils.blockingGetAuthToken(ctx, getEmail()));
        saveAuthToken(authToken);

    }

    public AuthToken getAuthToken() {

        // no registered auth token with the Android 2.0 account management
        // so let's try the old schemes then
        String authToken = getSharedPreferences().getString(SETTINGS_AUTH_TOKEN, null);
        String authType = getSharedPreferences().getString(SETTINGS_AUTH_TYPE, null);

        PL.log("EM.getAuthToken() token=" + authToken.substring(0, 4) + " type=" + authType, ctx);
        if (authToken != null && authType != null) {
            AuthToken token = new EntriesRetriever.AuthToken(EntriesRetriever.AuthToken.AuthType.valueOf(authType),
                    authToken);
            PL.log("EM.getAuthToken()2 ton=" + token, ctx);
            if ("EXPIRED".equals(token.getAuthToken())) {
                try {
                    regetAuthToken();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                token = new EntriesRetriever.AuthToken(EntriesRetriever.AuthToken.AuthType.valueOf(authType), authToken);
            }
            return token;
        }

        return null; // no new-auth, old-auth or sid
    }

    public void addListener(final IEntryModelUpdateListener newListener) {
        listeners.add(newListener);
    }

    boolean entryExists(final String entryAtomId) {
        // Timing t = new Timing("EntryManager.entryExists");
        final boolean result = databaseHelper.findEntryByAtomId(entryAtomId) != null;
        // t.stop();
        return result;
    }

    public Entry findArticleById(Long id) {
        return databaseHelper.findArticleById(id);
    }

    public Entry findEntryByAtomId(final String entryAtomId) {
        // Timing t = new Timing("EntryManager.findEntryByAtomId "+entryAtomId);
        final Entry result = databaseHelper.findEntryByAtomId(entryAtomId);
        // t.stop();
        return result;
    }

    public void fireStatusUpdated() {
        try {
            for (final IEntryModelUpdateListener listener : new ArrayList<IEntryModelUpdateListener>(listeners))
                listener.statusUpdated();
        } catch (final RuntimeException nsee) {
            // ignored
        }
    }

    void fireModelUpdateFinished(final ModelUpdateResult result) {
        PL.log("MODEL_UPDATE(" + result != null ? result.getClass().getName() : "null" + "): Finished. Result: "
                + (result != null ? result.getMessage() : "Result was null"), ctx);
        try {
            for (final IEntryModelUpdateListener listener : new ArrayList<IEntryModelUpdateListener>(listeners)) {
                listener.modelUpdateFinished(result);
            }
        } catch (final RuntimeException nsee) {
            // ignored
        }
        updateWidgets();
    }

    void fireModelUpdateStarted(final String message, final boolean fastSyncOnly, final boolean manualSync) {
        PL.log(
                "MODEL_UPDATE_STARTED (" + message + "): Started fastSync=" + fastSyncOnly + " manualSync="
                        + manualSync, ctx);

        try {
            for (final IEntryModelUpdateListener listener : new ArrayList<IEntryModelUpdateListener>(listeners)) {
                listener.modelUpdateStarted(fastSyncOnly);
            }

        } catch (final RuntimeException nsee) {
            // ignored
        }
    }

    public void fireModelUpdated() {

        fireModelUpdated(null);

    }

    void fireModelUpdated(final String atomId) {
        synchronized (this) {
            if (NewsRob.fireModelUpdateInProgress)
                return;
            else
                NewsRob.fireModelUpdateInProgress = true;
        }
        try {

            // reset the cache when something
            // has changed.
            isMarkAllReadPossibleCache.clear();
            articleCountCache = null;
            contentCountCache.clear();

            try {
                for (final IEntryModelUpdateListener listener : new ArrayList<IEntryModelUpdateListener>(listeners)) {
                    if (atomId != null)
                        listener.modelUpdated(atomId);
                    else
                        listener.modelUpdated();
                }
            } catch (final RuntimeException nsee) {
                // ignored
            }
            updateWidgets();
        } finally {
            NewsRob.fireModelUpdateInProgress = false;
        }
    }

    private synchronized void updateWidgets() {

        if (updateWidgetsTimer == null)
            updateWidgetsTimer = new Timer("UpdateWidgetsTimer", false);

        if (updateWidgetsTimerTask != null) {
            updateWidgetsTimerTask.cancel();
            updateWidgetsTimerTask = null;
        }

        if (lastUpdateWidgetUpdate == null || (lastUpdateWidgetUpdate + (3 * 60 * 1000)) < System.currentTimeMillis()) {
            UnreadWidgetProvider.requestWidgetsUpdate(ctx);
            lastUpdateWidgetUpdate = System.currentTimeMillis();
        } else {
            updateWidgetsTimerTask = new TimerTask() {

                @Override
                public void run() {
                    UnreadWidgetProvider.requestWidgetsUpdate(ctx);
                    lastUpdateWidgetUpdate = System.currentTimeMillis();

                    // ADW Notification
                    if (isADWLauncherInterestedInNotifications()) {

                        Intent i = new Intent();
                        i.setAction("org.adw.launcher.counter.SEND");
                        i.putExtra("PNAME", "com.newsrob");
                        i.putExtra("COUNT", getUnreadArticleCount());
                        ctx.sendBroadcast(i);
                        PL.log("EntryManager.updateWidgets sent notification to ADW.", ctx);
                    }

                }

            };
            updateWidgetsTimer.schedule(updateWidgetsTimerTask, 35 * 1000);
        }
    }

    public Context getContext() {
        return ctx;
    }

    public void insert(List<Entry> entries) {
        databaseHelper.insert(entries);
    }

    public void insert(final Entry entry) {
        databaseHelper.insert(entry);
    }

    /** make an article more read */
    public void increaseReadLevel(Entry entry) {

        switch (entry.getReadState()) {

        case READ:
            break;

        default:
            updateReadState(entry, ReadState.READ);
            return;
        }

    }

    public void increaseUnreadLevel(Entry entry) {
        switch (entry.getReadState()) {
        case READ:
            updateReadState(entry, ReadState.UNREAD);
            break;
        case UNREAD:
            if (isProVersion())
                updateReadState(entry, ReadState.PINNED);
            break;
        default:
            // stay pinned
        }

    }

    void updateReadState(final Entry entry, final ReadState newReadState, final boolean isReadStatePending) {
        Timing t = new Timing("EntryManager.updateReadState(3) - total", ctx);
        try {
            if (entry.getReadState() == newReadState)
                return;
            Timing t2 = new Timing("EntryManager.updateReadState(3) - updateReadState", ctx);

            entry.setReadState(newReadState);
            entry.setReadStatePending(isReadStatePending);
            databaseHelper.updateReadState(entry);
            t2.stop();

            Timing t3 = new Timing("EntryManager.updateReadState(3) - fireModelUpdated", ctx);

            fireModelUpdated();
            t3.stop();

            Timing t4 = new Timing("EntryManager.updateReadState(3) - scheduleUploadOnlySync", ctx);

            if (isReadStatePending)
                scheduler.scheduleUploadOnlySynchonization();

            t4.stop();
        } finally {
            t.stop();
        }
    }

    public void updateReadState(Entry entry, ReadState newReadState) {

        ReadState existingReadState = entry.getReadState();

        // GR state unread on both sides or read on both sides?
        boolean existingUnread = existingReadState == ReadState.UNREAD || existingReadState == ReadState.PINNED;
        boolean newUnread = newReadState == ReadState.UNREAD || newReadState == ReadState.PINNED;

        boolean stateChanged = existingUnread != newUnread;

        boolean existingReadStatePending = entry.isReadStatePending();
        boolean newReadStatePending = stateChanged ? !existingReadStatePending : existingReadStatePending;

        updateReadState(entry, newReadState, newReadStatePending);
    }

    public void updateSharedState(final Entry entry, final boolean isShared) {
        updateSharedState(entry, isShared, !entry.isSharedStatePending());
    }

    void updateStarredState(final Entry entry, final boolean isStarred, final boolean isStarredStatePending) {
        if (entry.isStarred() == isStarred)
            return;

        entry.setStarred(isStarred);
        entry.setStarredStatePending(isStarredStatePending);
        databaseHelper.updateStarredState(entry);
        fireModelUpdated();
        if (isStarredStatePending)
            scheduler.scheduleUploadOnlySynchonization();

    }

    void updateLikedState(final Entry entry, final boolean isLiked, final boolean isLikedStatePending) {
        if (entry.isLiked() == isLiked)
            return;

        entry.setLiked(isLiked);
        entry.setLikedStatePending(isLikedStatePending);
        databaseHelper.updateLikedState(entry);
        fireModelUpdated();
        if (isLikedStatePending)
            scheduler.scheduleUploadOnlySynchonization();

    }

    void updateSharedState(final Entry entry, final boolean isShared, final boolean isSharedStatePending) {
        if (entry.isShared() == isShared)
            return;

        entry.setShared(isShared);
        entry.setSharedStatePending(isSharedStatePending);
        databaseHelper.updateSharedState(entry);
        fireModelUpdated();
        if (isSharedStatePending)
            scheduler.scheduleUploadOnlySynchonization();

    }

    public void updateFriendsSharedState(final Entry existingEntry, final boolean isSharedByFriends) {
        existingEntry.setFriendsShared(isSharedByFriends);
        databaseHelper.updateFriendsSharedState(existingEntry);
        fireModelUpdated();
    }

    public void updateStarredState(final Entry entry, final boolean isStarred) {
        updateStarredState(entry, isStarred, !entry.isStarredStatePending());
    }

    public void updateLikedState(final Entry entry, final boolean isLiked) {
        updateLikedState(entry, isLiked, !entry.isLikedStatePending());
    }

    public boolean isModelCurrentlyUpdated() {
        return isModelCurrentlyUpdated;
    }

    public void removeListener(final IEntryModelUpdateListener listenerToRemove) {
        listeners.remove(listenerToRemove);
    }

    public boolean isLicenseAccepted() {
        return getSharedPreferences().getBoolean(SETTINGS_LICENSE_ACCEPTED, false);
    }

    public void acceptLicense() {
        SDK9Helper.apply(getSharedPreferences().edit().putBoolean(SETTINGS_LICENSE_ACCEPTED, true));
        Log.d(TAG, "License accepted!");
    }

    synchronized void lockModel(final String lockedBy) {
        if (isModelCurrentlyUpdated)
            throw new IllegalStateException(String.format(
                    "Trying to lock locked model by %s. Model was already locked by %s since %s seconds.", lockedBy,
                    modelLockedBy, String.valueOf((System.currentTimeMillis() - modelLockedAt) / 1000)));
        isModelCurrentlyUpdated = true;

        modelLockedAt = System.currentTimeMillis();
        modelLockedBy = lockedBy;
        fireStatusUpdated();
    }

    synchronized void unlockModel(final String unlockedBy) {
        if (!isModelCurrentlyUpdated)
            Log.w(TAG, "Unlocking model, but model wasn't locked! unlockedBy=" + unlockedBy);
        isModelCurrentlyUpdated = false;
        modelLockedAt = -1;
        modelLockedBy = null; // NOPMD by mkamp on 1/18/10 8:29 PM
        fireStatusUpdated();
    }

    public void requestClearCacheAndSync() {
        try {
            runJob(new Job("Clearing Cache", EntryManager.this) {

                @Override
                public void run() throws Exception {

                    try {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

                        synchronized (this) {
                            if (isModelCurrentlyUpdated())
                                return;
                            lockModel("EM.requestClearCacheAndSync");
                        }
                        doClearCache();
                    } finally {
                        synchronized (this) {
                            unlockModel("EM.requestClearCacheAndSync");
                        }
                    }
                    fireModelUpdated();
                    requestSynchronization(false);
                }

            });
        } catch (final Throwable e) {
            showExceptionToast("Clear Cache And Sync", e);
        }

    }

    public void requestSynchronization(final boolean uploadOnly) {
        if (isModelCurrentlyUpdated()) {
            PL.log("Model is currently updated. Returning. Locked by " + modelLockedBy + " at " + modelLockedAt, ctx);
            return;
        }

        final Intent i = new Intent(ctx, SynchronizationService.class);
        if (uploadOnly)
            i.setAction(SynchronizationService.ACTION_SYNC_UPLOAD_ONLY);
        i.putExtra(SynchronizationService.EXTRA_MANUAL_SYNC, true);
        SynchronizationService.acquireWakeLock(ctx);
        ctx.startService(i);

    }

    public void requestMarkAllAsRead(final DBQuery dbq) {
        new Thread(new Runnable() {
            public void run() {
                doMarkAllRead(dbq);
            }
        }).start();
    }

    protected void doMarkAllRead(final DBQuery dbq) {
        if (NewsRob.isDebuggingEnabled(ctx))
            PL.log("Mark All Read: dbq=" + dbq, ctx);
        if (NewsRob.isDebuggingEnabled(ctx)) {
            PL.log("Before mark all read: " + getUnreadArticleCount() + "/" + getArticleCount(), ctx);
            PL.log("Articles with pending read state: " + getPendingReadStateArticleCount(), ctx);
        }
        databaseHelper.markAllRead(dbq);
        if (NewsRob.isDebuggingEnabled(ctx)) {
            PL.log("After mark all read: " + getUnreadArticleCount() + "/" + getArticleCount(), ctx);
            PL.log("Articles with pending read state: " + getPendingReadStateArticleCount(), ctx);
        }
        fireModelUpdated();

        scheduler.scheduleUploadOnlySynchonization();
    }

    public void requestClearCache(final Handler handler) {
        runSimpleJob("Clear Cache", new Runnable() {
            public void run() {
                doClearCache();
            }
        }, handler);
    }

    private void runSimpleJob(final String name, final Runnable runnable, final Handler handler) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    runJob(new Job(name, EntryManager.this) {

                        @Override
                        public void run() throws Exception {

                            try {
                                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

                                synchronized (this) {
                                    if (isModelCurrentlyUpdated())
                                        return;
                                    lockModel(name);
                                }
                                runnable.run();
                            } finally {
                                synchronized (this) {
                                    unlockModel(name);
                                }
                            }
                        }
                    });
                } catch (final Throwable e) {
                    if (handler != null) {
                        handler.post(new Runnable() {

                            public void run() {
                                showExceptionToast(name, e);
                            }
                        });
                    }
                }

            }
        }).start();
    }

    public void showExceptionToast(final String message, final Throwable e) {
        try {
            Toast.makeText(ctx, message + e.getClass() + ":" + e.getMessage(), Toast.LENGTH_LONG).show(); // I18N
        } catch (final Exception ex) {
            Log.e(TAG, "Caught exception when trying to show an error Toast.", e);
        }

    }

    // LATER public b/o testing
    public void doClearCache() {
        fireStatusUpdated();
        setGRUpdated(-1l);
        updateLastSyncedSubscriptions(-1l);
        databaseHelper.deleteAll();
        final int noOfAssetsDeleted = WebPageDownloadDirector.removeAllAssets(fileContextAdapter);
        Log.d(TAG, noOfAssetsDeleted + " assets deleted.");
        fireModelUpdated();
        fireStatusUpdated();
    }

    void setLastSync(final boolean complete) {
        SDK9Helper.apply(getSharedPreferences().edit().putBoolean(SETTINGS_LAST_SYNC_COMPLETE, complete).putLong(
                SETTINGS_LAST_SYNC_TIME, System.currentTimeMillis()));
    }

    public static synchronized EntryManager getInstance(final Context context) {
        if (instance == null)
            instance = new EntryManager(context.getApplicationContext());
        return instance;
    }

    List<Entry> findAllStatePendingEntries(final String column, final String desiredState) {
        return databaseHelper.findAllByPendingState(column, desiredState);
    }

    boolean delete(final Entry entry) {

        if (!fileContextAdapter.canWrite())
            return false;

        final int noOfAssetsDeleted = WebPageDownloadDirector.removeAssetsForId(entry.getAtomId(), fileContextAdapter);
        Log.d(TAG, noOfAssetsDeleted + " assets deleted for entry " + entry.getAtomId() + ".");

        return databaseHelper.deleteEntry(entry) == 1 ? true : false;
    }

    void resetPendingStates(final Entry entry) {
        databaseHelper.updateReadState(entry);
        databaseHelper.updateStarredState(entry);
    }

    public int getStorageCapacity() {
        return Integer.parseInt(sharedPreferences.getString(SETTINGS_ENTRY_MANAGER_CAPACITY, "50"));

    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public int deleteReadArticles(final SyncJob job) {
        return deleteArticles(job, databaseHelper.getReadArticlesIdsForDeletion(getNoOfStarredArticlesToKeep(),
                getNoOfSharedArticlesToKeep(), getNoOfNotesToKeep()));
    }

    public int getNoOfStarredArticlesToKeep() {
        return getNoOfArticlesToKeep(SETTINGS_KEEP_STARRED, 20);
    }

    public int getNoOfSharedArticlesToKeep() {
        return getNoOfArticlesToKeep(SETTINGS_KEEP_SHARED, 20);
    }

    public int getNoOfNotesToKeep() {
        return getNoOfArticlesToKeep(SETTINGS_KEEP_NOTES, 5);
    }

    public boolean shouldHWZoomControlsBeDisabled() {

        if (DEVICE_MODEL_DROID_INCREDIBLE.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        if (DEVICE_MODEL_EVO.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        if (DEVICE_MODEL_DESIRE.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        return false;
    }

    public boolean shouldSyncInProgressNotificationBeDisabled() {

        if (true)
            return false;
        if (DEVICE_MODEL_DROID_INCREDIBLE.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        if (DEVICE_MODEL_DELL_STREAK.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 7)
            return true;

        if (DEVICE_MODEL_EVO.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        if (DEVICE_MODEL_DESIRE.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 8)
            return true;

        return false;

    }

    private int getNoOfArticlesToKeep(final String setting, final int def) {
        if (!isProVersion())
            return 0;
        return Integer.parseInt(getSharedPreferences().getString(setting, String.valueOf(def)));
    }

    int reduceToCapacity(final SyncJob job) {
        return deleteArticles(job, databaseHelper.getOverCapacityIds(getStorageCapacity(),
                getNoOfStarredArticlesToKeep(), getNoOfSharedArticlesToKeep(), getNoOfNotesToKeep()));
    }

    private int deleteArticles(final SyncJob job, final Cursor idsOfEntriesToDeleteCursor) {
        Timing t = new Timing("DeleteArticles's assets", ctx);

        final List<String> articleIdsToDeleteInDatabase = new ArrayList<String>(idsOfEntriesToDeleteCursor.getCount());
        int noOfEntriesDeleted = 0;

        try {

            job.target = idsOfEntriesToDeleteCursor.getCount(); // TODO too
            // eager?

            // Debug.startMethodTracing("delete");

            if (!fileContextAdapter.canWrite()) {
                Log.w(TAG, "EntryManager.deleteArticles even though fileContext.canWrite() returns false. Skipping.");
            } else {

                while (idsOfEntriesToDeleteCursor.moveToNext()) {
                    if (job.isCancelled())
                        return noOfEntriesDeleted;
                    if (!fileContextAdapter.canWrite())
                        return noOfEntriesDeleted;

                    // PERFORMANCE ooh. performance
                    // problem. skip
                    // this step. don't load an entry first
                    // to delete it LATER

                    String mostRecentArticleAtomId = getMostRecentArticleAtomId();

                    String atomId = idsOfEntriesToDeleteCursor.getString(1);

                    // don't delete the article currently viewed by the user
                    if (mostRecentArticleAtomId == null || !atomId.endsWith(mostRecentArticleAtomId)) {
                        WebPageDownloadDirector.removeAssetsForId(atomId, fileContextAdapter);
                        articleIdsToDeleteInDatabase.add(idsOfEntriesToDeleteCursor.getString(0));
                        noOfEntriesDeleted++;
                    }

                    job.actual = noOfEntriesDeleted;
                    if (job.actual % 5 == 0)
                        fireStatusUpdated();
                }
                fireModelUpdated();
                Log.d(TAG, "EntryManager.deleteArticles() cleaned up " + noOfEntriesDeleted + " articles' assets.");
            }
            // Debug.stopMethodTracing();

        } finally {
            idsOfEntriesToDeleteCursor.close();
            t.stop();
        }

        deleteArticlesFromDb(job, articleIdsToDeleteInDatabase);

        return noOfEntriesDeleted;
    }

    private void deleteArticlesFromDb(final SyncJob job, final List<String> articleIdsToDeleteInDatabase) {
        if (articleIdsToDeleteInDatabase.isEmpty())
            return;

        Timing t2 = new Timing("Delete Articles From Db", ctx);

        job.setJobDescription("Cleaning up database");
        job.target = articleIdsToDeleteInDatabase.size();
        job.actual = 0;
        fireStatusUpdated();

        SQLiteDatabase db = databaseHelper.getDb();

        final String sql1 = "DELETE FROM " + Entries.TABLE_NAME + " WHERE " + Entries.__ID + "=?;";
        final String sql2 = "DELETE FROM " + EntryLabelAssociations.TABLE_NAME + " WHERE "
                + EntryLabelAssociations.ENTRY_ID + "=?;";

        final SQLiteStatement stmt1 = db.compileStatement(sql1);
        final SQLiteStatement stmt2 = db.compileStatement(sql2);

        try {

            // outter loop does the chunking and holds the transaction context
            while (!articleIdsToDeleteInDatabase.isEmpty()) {

                db.beginTransaction();

                while (!articleIdsToDeleteInDatabase.isEmpty()) {

                    String id = articleIdsToDeleteInDatabase.remove(0);
                    stmt1.bindString(1, id);
                    stmt1.execute();
                    stmt2.bindString(1, id);
                    stmt2.execute();

                    job.actual++;

                    if (job.actual % 10 == 0)
                        fireStatusUpdated();

                    // commit every 35 articles
                    if (job.actual >= 35)
                        break;
                }

                db.setTransactionSuccessful();
                db.endTransaction();
            }
        } finally {
            stmt1.close();
            stmt2.close();
        }
        fireStatusUpdated();
        t2.stop();
    }

    void removePendingStateMarkers(final Collection<String> atomIds, final String column) {
        databaseHelper.removePendingStateMarkers(atomIds, column);
        fireModelUpdated();
    }

    public void logout() {
        getEntriesRetriever().logout();
    }

    public IStorageAdapter getStorageAdapter() {
        return fileContextAdapter;
    }

    public Job getCurrentRunningJob() {
        return currentRunningJob;
    }

    void clearCancelState() {
        cancelRequested = false;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public void cancel() {
        cancelRequested = true;

        if (currentRunningJob != null)
            currentRunningJob.cancel();
        fireStatusUpdated();

        if (runningThread != null) {
            final Thread t = new Thread(new Runnable() {

                public void run() {
                    try {

                        if (!runningThread.isAlive())
                            return;

                        runningThread.join(8000);
                        if (!runningThread.isAlive())
                            return;

                        runningThread.interrupt();
                        runningThread.join(2500);

                        if (!runningThread.isAlive())
                            return;

                    } catch (final Throwable t) {
                        PL.log("CANCEL: " + t.getMessage() + " " + t.getClass(), ctx);
                    } finally {
                        if (runningThread == null || !runningThread.isAlive()) {
                            runningThread = null; // NOPMD by mkamp on 1/18/10
                            // 8:29 PM
                            clearCancelState();

                        } else {
                            PL.log("CANCEL: Didn't work out ;-( " + runningThread.getState(), ctx);

                            for (final StackTraceElement ste : new ArrayList<StackTraceElement>(Arrays
                                    .asList(runningThread.getStackTrace()))) {
                                PL.log("CANCEL: " + ste.toString(), ctx);

                            }

                        }
                    }
                }
            });
            t.start();
        }
    }

    static class SyncJobStatus {
        int noOfEntriesUpdated = 0;
        int noOfEntriesFetched = 0;
    }

    public NewsRobNotificationManager getNewsRobNotificationManager() {
        return newsRobNotificationManager;
    }

    NewsRobScheduler getScheduler() {
        return scheduler;
    }

    Collection<Long> findAllArticleIds2Download() {
        return databaseHelper.findAllArticleIdsToDownload();
    }

    boolean updatedDownloaded(final Entry entry) {
        return databaseHelper.updateDownloaded(entry);
    }

    public Feed findFeedById(final long feedId) {
        return databaseHelper.findFeedById(feedId);
    }

    public boolean updateFeed(final Feed feed) {
        return databaseHelper.updateFeed(feed);
    }

    public int getDefaultDownloadPref() {
        return new Integer(getSharedPreferences().getString(SETTINGS_GLOBAL_DOWNLOAD_PREF_KEY,
                String.valueOf(Feed.DOWNLOAD_PREF_FEED_ONLY)));
    }

    public boolean downloadContentCurrentlyEnabled(boolean manualSync) {
        return downloadOrSyncCurrentlyEnabled(getSharedPreferences().getString(
                EntryManager.SETTINGS_STORAGE_ASSET_DOWNLOAD, DOWNLOAD_YES), manualSync);
    }

    public boolean syncCurrentlyEnabled(final boolean manualSync) {
        return downloadOrSyncCurrentlyEnabled(getSharedPreferences().getString(
                EntryManager.SETTINGS_AUTOMATIC_REFRESHING_ENABLED, DOWNLOAD_YES), manualSync);
    }

    private boolean downloadOrSyncCurrentlyEnabled(final String syncPref, final boolean manualSync) {

        if (isInAirplaneMode()) {
            PL
                    .log(
                            "Sync/Download: Airplane mode is turned on by the user. Will result in the sync/download to be interrupted.",
                            ctx);

            // Toast.makeText(ctx,
            // "Device in Airplane mode.\nNo sync or download possible.",
            // Toast.LENGTH_LONG).show();
            return false;
        }

        if (manualSync)
            return true;

        if (DOWNLOAD_NO.equals(syncPref))
            return false;

        if (DOWNLOAD_WIFI_ONLY.equals(syncPref) && !isOnWiFi()) {
            PL
                    .log(
                            "Sync/Downlod: WiFi requested, but not available. Will result in the sync/download to be interrupted.",
                            ctx);

            return false;
        }

        // DOWNLOAD_YES OR (DOWNLOAD_WIFI and WiFi is available)

        boolean backgroundDataEnabled = isBackgroundDataEnabled() && isSystemwideSyncEnabled();

        if (!backgroundDataEnabled)
            PL
                    .log(
                            "Sync/Download: Background data or systemwide syncing turned off by the user. Will result in the sync/download to be interrupted.",
                            ctx);

        if (!backgroundDataEnabled)
            return false;

        return true;
    }

    boolean isOnWiFi() {
        if (connectivityManager == null)
            connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            PL.log("EntryManager. Wasn't able to get CONNECTIVITY_SERVICE.", ctx);
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null)
                PL.log("WiFi Info=" + wifiInfo + " SSID=" + wifiInfo.getSSID(), ctx);
            return false;
        }

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            PL.log("EntryManager. Wasn't able to get Network Info.", ctx);
            return false;

        }

        if (ConnectivityManager.TYPE_WIFI != networkInfo.getType()) {
            PL.log("EntryManager. Network Info Type was not WiFi, but " + networkInfo.getType() + ".", ctx);
            return false;
        }

        return true;
    }

    public boolean isNetworkConnected(Context ctx) {

        if (connectivityManager == null)
            connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        if (ni == null) {
            PL.log("EntryManager. Wasn't able to get NI.", ctx);
            return false;
        }

        if (NewsRob.isDebuggingEnabled(ctx)) {
            PL.log("ActiveNetwork: " + ni.getTypeName(), ctx);
            PL.log("ActiveNetworkState: " + ni.getDetailedState(), ctx);
            PL.log("isNetworkConnected? " + ni.isConnected(), ctx);
        }

        return ni.isConnected();
    }

    public boolean isBackgroundDataEnabled() {
        if (connectivityManager == null)
            connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getBackgroundDataSetting();
    }

    public boolean isSystemwideSyncEnabled() {
        Method m;
        try {
            m = ContentResolver.class.getMethod("getMasterSyncAutomatically", new Class[] {});
            if (m != null) {

                Object o = m.invoke(ContentResolver.class, new Object[] {});

                if (o != null) {
                    return (Boolean) o;
                }
            }
        } catch (Exception e) {
        }

        return true;
    }

    /** in Airplane Mode and Wifi is included? */
    public boolean isInAirplaneMode() {
        return false;
    }

    public Cursor getDashboardContentCursor(DBQuery dbq) {
        return databaseHelper.getDashboardContentCursor(dbq);
    }

    public Cursor getContentCursor(final DBQuery query) {
        return databaseHelper.getContentCursor(query);
    }

    public Cursor getFeedListContentCursor(final DBQuery query) {
        return databaseHelper.getFeedListContentCursor(query);
    }

    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        final String key = preference.getKey();
        onSharedPreferenceChanged(sharedPreferences, key);
        return true;
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        if (SETTINGS_ENTRY_MANAGER_CAPACITY.equals(key) || SETTINGS_KEEP_SHARED.equals(key)
                || SETTINGS_KEEP_STARRED.equals(key) || SETTINGS_SYNC_TYPE.equals(key)) {
            setGRUpdated(-1l);
        } else if (SETTINGS_STORAGE_PROVIDER_KEY.equals(key)) {
            setGRUpdated(-1l);
            switchStorageProvider();
        } else if (SETTINGS_INCREMENTAL_SYNC_ENABLED.equals(key))
            setGRUpdated(-1l);

        // When the setting is changed from the settings dialog also maintain
        // the version code here
        if (SETTINGS_USAGE_DATA_PERMISSION_GRANTED.equals(key)) {
            SDK9Helper.apply(getSharedPreferences().edit().putInt(SETTINGS_USAGE_DATA_COLLECTION_PERMISSION_VERSION,
                    getMyVersionCode()));

            PL.log("EntryManager: Usage Data Collection Permission changed from the settings dialog. New setting="
                    + getSharedPreferences().getBoolean(SETTINGS_USAGE_DATA_PERMISSION_GRANTED, false) + " version="
                    + getSharedPreferences().getInt(SETTINGS_USAGE_DATA_COLLECTION_PERMISSION_VERSION, -99)
                    + " collectionPermitted=" + isUsageDataCollectionPermitted(), ctx);

        }

        if (SETTINGS_AUTOMATIC_REFRESHING_ENABLED.equals(key)) {
            maintainBootReceiverState();
        }
        if (SETTINGS_UI_THEME.equals(key)) {
            getContext().setTheme(getCurrentThemeResourceId());
        }
        currentThemeId = 0; // reset in case this has been changed
    }

    public void maintainBootReceiverState() {
        final ComponentName cName = new ComponentName(ctx, BootReceiver.class);
        final PackageManager pm = ctx.getPackageManager();

        final int newComponentState = isAutoSyncEnabled() ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        if (pm.getComponentEnabledSetting(cName) != newComponentState) {
            Log.d(TAG, "Setting new component enabled state on BootReceiver: " + isAutoSyncEnabled());
            pm.setComponentEnabledSetting(cName, newComponentState, PackageManager.DONT_KILL_APP);
        }
        PL.log("EntryManager.maintainBootReceiverState(): Component enabled=" + pm.getComponentEnabledSetting(cName),
                ctx);

    }

    @SuppressWarnings("unchecked")
    public void maintainPremiumDependencies() {

        this.proVersion = null;

        if (false) {
            final int desiredComponentState = isProVersion() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            final Class[] componentsToDeactivate = { FireReceiver.class, MockEditSettingsActivity.class };

            final PackageManager pm = ctx.getPackageManager();
            for (final Class cls : componentsToDeactivate) {
                final ComponentName cName = new ComponentName(ctx, cls);
                pm.setComponentEnabledSetting(cName, desiredComponentState, PackageManager.DONT_KILL_APP);
            }
        }
    }

    public boolean shouldHideReadItems() {
        return getSharedPreferences().getBoolean(EntryManager.SETTINGS_HIDE_READ_ITEMS, false);
    }

    public int getDaysInstalled() {

        long l = getSharedPreferences().getLong(SETTINGS_INSTALLED_AT, 0l);

        if (l == 0l) {
            l = System.currentTimeMillis();
            SDK9Helper.apply(getSharedPreferences().edit().putLong(SETTINGS_INSTALLED_AT, l));
        }

        final int daysInstalled = (int) ((System.currentTimeMillis() - l) / (1000 * 60 * 60 * 24));

        return daysInstalled;
    }

    public boolean isAutoSyncEnabled() {
        final String pref = sharedPreferences.getString(EntryManager.SETTINGS_AUTOMATIC_REFRESHING_ENABLED,
                EntryManager.DOWNLOAD_YES);
        if (EntryManager.DOWNLOAD_YES.equals(pref) || EntryManager.DOWNLOAD_WIFI_ONLY.equals(pref))
            return true;
        return false;
    }

    public int getNoOfNewArticlesSinceLastUsed(final long lastUsed) {

        final DBQuery dbq = new DBQuery(this, null, null);
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
        dbq.setStartDate(lastUsed);

        return getContentCount(dbq);
    }

    public boolean shouldTitlesBeEllipsized() {
        return getSharedPreferences().getBoolean(SETTINGS_ELLIPSIZE_TITLES_ENABLED, false);
    }

    public void setGRUpdated(final long lastUpdated) {
        if (NewsRob.isDebuggingEnabled(ctx))
            PL.log("setGRUpdated=" + lastUpdated, ctx);

        SDK9Helper.apply(getSharedPreferences().edit().putLong(SETTINGS_GR_UPDATED_KEY, lastUpdated));
    }

    public long getGRUpdated() {
        return getSharedPreferences().getLong(SETTINGS_GR_UPDATED_KEY, -1l);
    }

    public void updateStates(final Collection<StateChange> stateChanges) {
        final Timing t = new Timing("EntryManager.updateStates()", ctx);
        try {
            if (databaseHelper.updateStates(stateChanges))
                fireStatusUpdated();
        } finally {
            t.stop();
        }
    }

    public void updateStatesFromTempTable(ArticleDbState articleDbState) {
        databaseHelper.updateStatesFromTempTable(articleDbState);
        fireStatusUpdated();
    }

    public String getGoogleUserId() {
        return getSharedPreferences().getString(SETTINGS_GOOGLE_USER_ID, null);
    }

    public void setGoogleUserId(final String newUserId) {

        if (newUserId == null)
            SDK9Helper.apply(getSharedPreferences().edit().remove(SETTINGS_GOOGLE_USER_ID));
        else
            SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_GOOGLE_USER_ID, newUserId));

    }

    public boolean isLightColorSchemeSelected() {
        return THEME_LIGHT.equals(getThemeColorScheme());
    }

    private String getThemeColorScheme() {
        return getSharedPreferences().getString(SETTINGS_UI_THEME, THEME_LIGHT);
    }

    public void toggleTheme() {

        SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_UI_THEME,
                isLightColorSchemeSelected() ? THEME_DARK : THEME_LIGHT));
        currentThemeId = 0;

    }

    public final int getCurrentThemeResourceId() {
        // Caching the theme resolution
        if (currentThemeId == 0)
            currentThemeId = getThemeResourceId(getThemeColorScheme());

        return currentThemeId;
    }

    public final int getThemeResourceId(String colorScheme) {
        String key = "Theme.NewsRob_" + colorScheme + "_Normal";
        return ctx.getResources().getIdentifier(key, "style", "com.newsrob.threetosix");
    }

    public Cursor searchFullText(final String query) {
        return databaseHelper.findByFullText(query);
    }

    public Cursor getArticleAsCursor(final String atomId) {
        return databaseHelper.findCursorByQueryString("ATOM_ID = \'" + atomId + "\'");
    }

    public int getUnreadArticleCount() {
        return databaseHelper.getUnreadArticleCount();
    }

    public long getNextScheduledSyncTime() {
        return getSharedPreferences().getLong(SETTINGS_NEXT_SCHEDULED_SYNC_TIME, -1l);
    }

    // Only called for the bookkeeping
    public void updateNextScheduledSyncTime(final long nextSyncTime) {
        SDK9Helper.apply(getSharedPreferences().edit().putLong(SETTINGS_NEXT_SCHEDULED_SYNC_TIME, nextSyncTime));
        PL.log("EntryManager.updateNextScheduledSyncTime() = " + new Date(nextSyncTime), ctx);
    }

    public int getMarkAllReadConfirmationDialogThreshold() {
        return Integer.parseInt(getSharedPreferences().getString(SETTINGS_MARK_ALL_READ_CONFIRMATION_DIALOG_THRESHOLD,
                "0"));
    }

    public void populateTempTable(final long[] articleIds) {
        databaseHelper.populateTempIds(articleIds);
    }

    public String getMostRecentArticleAtomId() {
        return singleValueStore.getString("current_article");
    }

    public void setMostRecentArticleAtomId(final String atomId) {
        try {
            singleValueStore.putString("current_article", atomId.substring(33));
        } catch (RuntimeException re) {
            re.printStackTrace();
        }
    }

    public void removeLocallyExistingArticlesFromTempTable() {
        databaseHelper.removeLocallyExistingArticlesFromTempTable();
    }

    public List<String> getNewArticleIdsToFetch(final int noOfArticles2Fetch) {
        return databaseHelper.getNewArticleAtomIdsToFetch(noOfArticles2Fetch);
    }

    public int getTempIdsCount() {
        return databaseHelper.getTempIdsCount();
    }

    public boolean shouldReadItemsBeDeleted() {
        return (SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY.equals(getSharedPreferences().getString(SETTINGS_SYNC_TYPE,
                SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY)));
    }

    public boolean shouldOnlyUnreadArticlesBeDownloaded() {
        return (SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY.equals(getSharedPreferences().getString(SETTINGS_SYNC_TYPE,
                SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY)));
    }

    public boolean areHoveringZoomControlsEnabled() {
        return !shouldHWZoomControlsBeDisabled()
                && getSharedPreferences().getBoolean(SETTINGS_HOVERING_ZOOM_CONTROLS_ENABLED, false);
    }

    public boolean isRichArticleListEnabled() {
        return getSharedPreferences().getBoolean(SETTINGS_UI_RICH_ARTICLE_LIST_ENABLED, true);
    }

    public boolean isHoveringButtonsNavigationEnabled() {
        return getSharedPreferences().getBoolean(SETTINGS_HOVERING_BUTTONS_NAVIGATION_ENABLED, true);
    }

    public boolean isVolumeControlNavigationEnabled() {
        return getSharedPreferences().getBoolean(SETTINGS_VOLUME_CONTROL_NAVIGATION_ENABLED, true);
    }

    public boolean isCameraButtonControllingReadStateEnabled() {
        return getSharedPreferences().getBoolean(SETTINGS_CAMERA_BUTTON_CONTROLS_READ_STATE_ENABLED, false);
    }

    public List<Feed> findAllFeeds() {
        return databaseHelper.findAllFeeds();
    }

    public int getFeedCount() {
        return databaseHelper.getFeedCount();
    }

    public long insert(final Feed f) {
        return databaseHelper.insert(f);
    }

    public void updateFeedNames(final Map<String, String> remoteFeedAtomIdsAndFeedTitles) {
        databaseHelper.updateFeedNames(remoteFeedAtomIdsAndFeedTitles);
    }

    public void updateLastUsed() {
        singleValueStore.putLong("last_used", System.currentTimeMillis());
        getNewsRobNotificationManager().cancelNewArticlesNotification();
    }

    public long getLastUsed() {
        return singleValueStore.getLong("last_used", 0l);
    }

    public void maintainLastTimeProposedReinstall() {
        singleValueStore.putLong("last_proposed_reinstall", System.currentTimeMillis());
    }

    public long getLastTimeProposedReinstall() {
        return singleValueStore.getLong("last_proposed_reinstall", 0l);
    }

    public void updateLastExactSync() {
        singleValueStore.putLong("last_exact_sync", System.currentTimeMillis());
        getNewsRobNotificationManager().cancelNewArticlesNotification();
    }

    public long getLastExactSync() {
        return singleValueStore.getLong("last_exact_sync", 0l);
    }

    public void updateLastShownThatSyncIsNotPossible() {
        singleValueStore.putLong("sync_not_possible_shown", System.currentTimeMillis());
        getNewsRobNotificationManager().cancelNewArticlesNotification();
    }

    public long getLastShownThatSyncIsNotPossible() {
        return singleValueStore.getLong("sync_not_possible_shown", 0l);
    }

    public boolean shouldVibrateOnFirstLast() {
        return getSharedPreferences().getBoolean(SETTINGS_VIBRATE_FIRST_LAST_ENABLED, true);
    }

    public void updateLastSyncedSubscriptions(final long timestamp) {
        SDK9Helper.apply(getSharedPreferences().edit().putLong(SETTINGS_LAST_SYNCED_SUBSCRIPTIONS, timestamp));
    }

    public long getLastSyncedSubscriptions() {
        return getSharedPreferences().getLong(SETTINGS_LAST_SYNCED_SUBSCRIPTIONS, -1l);
    }

    public boolean isMarkAllReadPossible(final DBQuery dbq) {
        Boolean b = isMarkAllReadPossibleCache.get(dbq);
        if (b == null) {
            b = databaseHelper.isMarkAllReadPossible(dbq);
            isMarkAllReadPossibleCache.put(dbq, b);
        }
        return b;
    }

    public int getContentCount(DBQuery dbq) {
        Integer count = contentCountCache.get(dbq);
        if (count == null) {
            count = databaseHelper.getContentCount(dbq);
            contentCountCache.put(dbq, count);
        }
        return count;
    }

    public int getArticleCount() {
        Integer count = articleCountCache;
        if (count == null) {
            count = databaseHelper.getArticleCount();
            articleCountCache = count;
        }

        return count;
    }

    public long getMarkAllReadCount(final DBQuery dbq) {
        return databaseHelper.getMarkAllReadCount(dbq);
    }

    public int getPendingReadStateArticleCount() {
        return databaseHelper.getPendingReadStateArticleCount();
    }

    public boolean shouldAlwaysUseSsl() {
        return getSharedPreferences().getBoolean(SETTINGS_ALWAYS_USE_SSL, true);
    }

    public final boolean isProVersion() {

        if (proVersion == null) {
            final int checkSignature = ctx.getPackageManager().checkSignatures(LEGACY_PACKAGE_NAME,
                    EntryManager.PRO_PACKAGE_NAME);
            proVersion = checkSignature == PackageManager.SIGNATURE_MATCH
                    || checkSignature == PackageManager.SIGNATURE_NEITHER_SIGNED;
        }
        return proVersion;
    }

    public boolean isAndroidMarketInstalled() {
        if (isAndroidMarketInstalled == null) {
            try {
                ctx.getPackageManager().getPackageInfo(MARKET_PACKAGE_NAME, 0);
                isAndroidMarketInstalled = true;
            } catch (NameNotFoundException nnfe) {
                isAndroidMarketInstalled = false;
            }
            Log.d(TAG, "Android Market is " + (isAndroidMarketInstalled ? "" : "not ") + "installed.");
        }
        return isAndroidMarketInstalled;

    }

    public boolean shouldAdsBeShown() {

        final boolean shouldAdsBeShown = !isProVersion()
                || "1".equals(NewsRob.getDebugProperties(ctx).getProperty("enableAds", "0"));
        if (false)
            PL.log("EntryManager.shouldAdsBeShown() isProVersion=" + isProVersion() + " debugEnabledAds="
                    + NewsRob.getDebugProperties(ctx).getProperty("enableAds", "0") + " result=" + shouldAdsBeShown,
                    ctx);
        return shouldAdsBeShown;
    }

    public Long findNotesFeedId() {
        return databaseHelper.findNotesFeedId(getGoogleUserId());
    }

    public void update(final Entry entry) {
        databaseHelper.update(entry);
        fireModelUpdated(entry.getAtomId());
        if (entry.getNote() != null && !entry.isNoteSubmitted())
            scheduler.scheduleUploadOnlySynchonization();
    }

    public List<Entry> getEntriesWithNotesToBeSubmitted() {
        return databaseHelper.findEntriesWithNotesToBeSubmitted();
    }

    public void clearNotesSubmissionStateForAllSubmittedNotes() {
        databaseHelper.clearNotesSubmissionStateForAllSubmittedNotes();
    }

    public void removeDeletedNotes() {
        databaseHelper.removeDeletedNotes();
    }

    private static String encryptPassword(String clearTextPassword) {
        String encryptedPassword = null;

        if (clearTextPassword == null || clearTextPassword.length() == 0)
            return null;

        try {
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            encryptedPassword = new String(com.newsrob.util.Base64.encodeBytes((cipher.doFinal(clearTextPassword
                    .getBytes("UTF-8")))));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedPassword;
    }

    private static String decryptPassword(String encryptedPw) {
        String clearTextPassword = null;

        if (encryptedPw == null || encryptedPw.length() == 0)
            return null;

        try {
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());

            clearTextPassword = new String(cipher.doFinal(Base64.decode(encryptedPw)), "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return clearTextPassword;
    }

    private static SecretKey getSecretKey() throws InvalidKeyException, UnsupportedEncodingException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        DESKeySpec keySpec = new DESKeySpec("EntryManager.class".getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(keySpec);
        return secretKey;
    }

    public boolean shouldRememberPassword() {
        return getSharedPreferences().getBoolean(SETTINGS_REMEMBER_PASSWORD, false);
    }

    public String getMobilizer() {
        return getSharedPreferences().getString(SETTINGS_MOBILIZER, "gwt");
    }

    public String getPlugins() {
        return getSharedPreferences().getString(SETTINGS_PLUGINS, "OFF");
    }

    public void setRememberPassword(boolean enabled) {
        SDK9Helper.apply(getSharedPreferences().edit().putBoolean(SETTINGS_REMEMBER_PASSWORD, enabled));
        if (!enabled)
            storePassword(null);
    }

    public String getStoredPassword() {
        String encryptedPw = getSharedPreferences().getString(SETTINGS_PASS, null);
        if (encryptedPw == null || encryptedPw.length() == 0)
            return null;

        return decryptPassword(encryptedPw);
    }

    public void storePassword(String password) {
        SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_PASS, encryptPassword(password)));
    }

    public String getDefaultTextSize() {
        return getSharedPreferences().getString(SETTINGS_UI_TEXT_SIZE, "NORMAL");
    }

    public boolean isSyncInProgressNotificationEnabled() {
        return shouldSyncInProgressNotificationBeDisabled() ? false : getSharedPreferences().getBoolean(
                SETTINGS_SYNC_IN_PROGRESS_NOTIFICATION, false);
    }

    public String getEmail() {
        return getSharedPreferences().getString(SETTINGS_EMAIL, null);
    }

    public void saveEmail(String googleAccount) {
        SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_EMAIL, googleAccount));
    }

    public Cursor getAllFeedsCursor() {
        return databaseHelper.getAllFeedsCursor();
    }

    public Cursor getAllLabelsCursor() {
        return databaseHelper.getAllLabelsCursor();
    }

    public WidgetPreferences getWidgetPreferences(int appWidgetId) {
        WidgetPreferences wp = new WidgetPreferences();

        wp.setLabel(getSharedPreferences().getString(getWidgetPreferenceKey(appWidgetId, "label"), null));
        wp.setStartingActivityName(getSharedPreferences().getString(
                getWidgetPreferenceKey(appWidgetId, "startingActivity"), null));
        try {
            InputStream is = null;
            try {
                is = ctx.openFileInput(getWidgetPreferenceKey(appWidgetId, "query"));
            } catch (IOException ioe) {
                is = ctx.openFileInput(getWidgetPreferenceKey(appWidgetId, "label"));
            }
            wp.setDBQuery(DBQuery.restore(this, is));
            is.close();
        } catch (IOException ioe) {
            Log.d(TAG, "Couldn't load dbq for widget " + appWidgetId + ".");
            // ioe.printStackTrace();
            wp = null;
        }
        return wp;
    }

    public void saveWidgetPreferences(int appWidgetId, WidgetPreferences wp) {
        SDK9Helper.apply(getSharedPreferences().edit().putString(getWidgetPreferenceKey(appWidgetId, "label"),
                wp.getLabel()));

        SDK9Helper.apply(getSharedPreferences().edit().putString(
                getWidgetPreferenceKey(appWidgetId, "startingActivity"), wp.getStartingActivityName()));

        try {
            if (wp.getDBQuery() != null) {
                OutputStream os = null;
                os = ctx.openFileOutput(getWidgetPreferenceKey(appWidgetId, "query"), Context.MODE_PRIVATE);
                wp.getDBQuery().store(os);
                os.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String getWidgetPreferenceKey(int appWidgetId, String key) {
        return "widget" + appWidgetId + key;
    }

    public void clearWidgetPreferences(int appWidgetId) {
        SDK9Helper.apply(getSharedPreferences().edit().remove(getWidgetPreferenceKey(appWidgetId, "label")));
        SDK9Helper.apply(getSharedPreferences().edit().remove(getWidgetPreferenceKey(appWidgetId, "query")));
    }

    public boolean isSwipeOnArticleDetailViewEnabled() {
        return getSharedPreferences().getBoolean(SETTING_SWIPE_ARTICLE_DETAIL_VIEW, true);
    }

    public boolean isSwipeOnArticleListEnabled() {
        return getSharedPreferences().getBoolean(SETTING_SWIPE_ARTICLE_LIST, true);
    }

    public boolean isNewsRobOnlySyncingEnabled() {
        return isProVersion() && getSharedPreferences().getBoolean(SETTINGS_SYNC_NEWSROB_ONLY_ENABLED, false);
    }

    public boolean isFriendsArticlesSyncingEnabled() {
        return getSharedPreferences().getBoolean("settings_sync_broadcast_friends_enabled", true);
    }

    public void saveLastSuccessfulLogin() {
        SDK9Helper.apply(getSharedPreferences().edit().putLong(SETTINGS_LAST_SUCCESSFUL_LOGIN, new Date().getTime()));
    }

    public Date getLastSuccessfulLogin() {
        return new Date(getSharedPreferences().getLong(SETTINGS_LAST_SUCCESSFUL_LOGIN, 0l));
    }

    public boolean shouldShowNewestArticlesFirst() {
        return getSharedPreferences().getBoolean(SETTINGS_UI_SORT_DEFAULT_NEWEST_FIRST, true);
    }

    public int getFirstInstalledVersion() {
        return getSharedPreferences().getInt(SETTINGS_FIRST_INSTALLED_VERSION, -1);
    }

    public void maintainFirstInstalledVersion() {
        if (getSharedPreferences().contains(SETTINGS_FIRST_INSTALLED_VERSION))
            return;
        int versionCode = getMyVersionCode();
        // set to a previous version when the app
        // was already installed
        if (getDaysInstalled() > 0)
            versionCode--;
        SDK9Helper.apply(getSharedPreferences().edit().putInt(SETTINGS_FIRST_INSTALLED_VERSION, versionCode));
    }

    public void migrateSyncType() {
        if (!getSharedPreferences().contains(SETTINGS_SYNC_TYPE)) {
            String newValue = SYNC_TYPE_VALUE_UNREAD_ARTICLES_ONLY;
            if (getFirstInstalledVersion() < 390)
                newValue = "all_articles";
            SDK9Helper.apply(getSharedPreferences().edit().putString(SETTINGS_SYNC_TYPE, newValue));
        }
    }

    public void showReleaseNotes() {
        // don't show release notes after install
        if (getMyVersionCode() == getFirstInstalledVersion())
            return;

        int shortVersionCode = getMyVersionCode() / 10;

        String releaseNotesKey = "release_notes_seen." + shortVersionCode;
        if (getSharedPreferences().contains(releaseNotesKey))
            return;

        Uri uri = Uri.parse("http://bit.ly/nr_" + shortVersionCode);
        if (false) {
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } else {
            getNewsRobNotificationManager().createCheckReleaseNotesNotification(uri);

        }

        SDK9Helper.apply(getSharedPreferences().edit().putBoolean(releaseNotesKey, true));
    }

    public void removeArticleContent(Entry entry) {
        if (!fileContextAdapter.canWrite())
            return;

        final int noOfAssetsDeleted = WebPageDownloadDirector.removeAssetsForId(entry.getAtomId(), fileContextAdapter);
        Log.d(TAG, noOfAssetsDeleted + " assets deleted for entry " + entry.getAtomId() + ".");
        entry.setDownloaded(0);
        entry.setError(null);
        updatedDownloaded(entry);
        fireModelUpdated(entry.getAtomId());
    }

    public Object getFeeds2UnsubscribeCount() {
        return databaseHelper.getFeeds2UnsubscribeCount();
    }

    public Cursor getFeeds2UnsubscribeCursor() {
        return databaseHelper.getFeeds2UnsubscribeCursor();
    }

    public void requestUnsubscribeFeed(final String feedAtomId) {
        // TODO remove this whole method if it doesn't do anything but run the
        // job

        try {
            runJob(new Job("UnsubscribingFeed", EntryManager.this) {

                @Override
                public void run() throws Exception {
                    doUnsubscribeFeed(feedAtomId);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public void doUnsubscribeFeed(String feedAtomId) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

            synchronized (this) {
                if (isModelCurrentlyUpdated())
                    return;
                lockModel("EM.requestUnsubscribeFeed");
            }
            if (canFeedBeUnsubscribed(feedAtomId)) {
                databaseHelper.addFeed2Unsubscribe(feedAtomId);
                DBQuery dbq = prepareMarkFeedReadQuery(feedAtomId);
                if (dbq != null)
                    doMarkAllRead(dbq);
            }
        } finally {
            synchronized (this) {
                unlockModel("EM.requestUnsubscribeFeed");
            }
        }

    }

    public boolean canFeedBeUnsubscribed(String feedAtomId) {
        if (databaseHelper.isFeedMarkedToBeUnsubscribed(feedAtomId))
            return false;

        if (!databaseHelper.doesFeedExist(feedAtomId))
            return false;

        return true;
    }

    public long getArticleCountThatWouldBeMarkedAsReadWhenFeedWouldBeUnsubscribed(String feedAtomId) {
        DBQuery dbq = prepareMarkFeedReadQuery(feedAtomId);
        if (dbq == null)
            return -1;
        return getMarkAllReadCount(dbq);
    }

    private DBQuery prepareMarkFeedReadQuery(String feedAtomId) {
        long feedId = databaseHelper.findFeedIdByFeedAtomId(feedAtomId);
        if (feedId == -1)
            return null;
        DBQuery dbq = new DBQuery(this, null, feedId);
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
        return dbq;
    }

    public void removeFeedFromFeeds2Unsubscribe(String feedAtomId) {
        databaseHelper.removeFeedFromFeeds2Unsubscribe(feedAtomId);
    }

    public int getMaxArticlesInArticleList() {
        if (DEVICE_MODEL_ARCHOS_7.equals(Build.MODEL) && SDKVersionUtil.getVersion() < 4
                || "HTC Gratia A6380".equals(Build.MODEL))
            return 200;
        return MAX_ARTICLES_IN_ARTICLE_LIST;
    }

    public boolean shouldActionBarLocationOnlyAllowGone() {

        int sdkLevel = SDKVersionUtil.getVersion();
        if (sdkLevel < 4 || sdkLevel == 5 || sdkLevel == 6)
            return true;

        // Tiny screens
        if (true)
            return false;

        Configuration cfg = ctx.getResources().getConfiguration();
        int screenLayout = -1;
        try {
            screenLayout = (Integer) cfg.getClass().getField("screenLayout").get(cfg);
        } catch (Exception e) {
            return false;
        }
        // if ((cfg.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) ==
        // Configuration.SCREENLAYOUT_SIZE_SMALL)
        if ((screenLayout & 0x0000000f) == 0x00000001)
            return true;

        return false;
    }

    public String getActionBarLocation() {

        if (shouldActionBarLocationOnlyAllowGone())
            return ACTION_BAR_GONE;

        return getSharedPreferences().getString(SETTINGS_UI_ACTION_BAR_LOCATION, ACTION_BAR_TOP);
    }

    public boolean shouldAskForUsageDataCollectionPermission() {
        SharedPreferences sp = getSharedPreferences();
        if (!sp.contains(SETTINGS_USAGE_DATA_COLLECTION_PERMISSION_VERSION))
            return true;

        return false;

    }

    public boolean isUsageDataCollectionPermitted() {
        return getSharedPreferences().getBoolean(SETTINGS_USAGE_DATA_PERMISSION_GRANTED, false);
    }

    public void saveUsageDataCollectionPermission(boolean permitted) {

        Editor editor = getSharedPreferences().edit();
        editor.putInt(SETTINGS_USAGE_DATA_COLLECTION_PERMISSION_VERSION, getMyVersionCode());
        editor.putBoolean(SETTINGS_USAGE_DATA_PERMISSION_GRANTED, permitted);
        SDK9Helper.apply(editor);

        Log.d(TAG, "User Data Collection preference saved: Permitted? " + permitted);
    }

    public boolean hasProgressReportBeenOpened() {
        return getSharedPreferences().contains(SETTINGS_OPENED_PROGRESS_REPORT);
    }

    public void updateProgressReportBeenOpened() {
        SDK9Helper.apply(getSharedPreferences().edit().putBoolean(SETTINGS_OPENED_PROGRESS_REPORT, true));
    }

    public int getUnreadArticleCountExact() {
        return databaseHelper.getUnreadArticleCountExact();
    }

    public int getReadArticleCount() {
        return databaseHelper.getReadArticleCount();
    }

    public int getPinnedArticleCount() {
        return databaseHelper.getPinnedArticleCount();
    }

    public int getStarredArticleCount() {
        return databaseHelper.getStarredArticleCount();
    }

    public int getSharedArticleCount() {
        return databaseHelper.getSharedArticleCount();
    }

    public int getNotesCount() {
        return databaseHelper.getNotesCount();
    }

    public int getChangedArticleCount() {
        return databaseHelper.getChangedArticleCount();
    }

    public boolean isADWLauncherInterestedInNotifications() {

        boolean adwInstalled;
        try {
            adwInstalled = (ctx.getPackageManager().getPackageInfo("org.adw.launcher.notifications", 0) != null);
        } catch (NameNotFoundException e) {
            return false;
        }
        return adwInstalled;
    }

    public boolean shouldAlwaysExactSync() {
        return getSharedPreferences().getBoolean(SETTINGS_ALWAYS_EXACT_SYNC, false);
    }

    public long findFeedIdByFeedUrl(String feedUrl) {
        return databaseHelper.findFeedIdByFeedUrl(feedUrl);
    }

}