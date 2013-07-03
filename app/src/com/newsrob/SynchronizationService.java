package com.newsrob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.newsrob.BackendProvider.AuthenticationExpiredException;
import com.newsrob.BackendProvider.ServerBadRequestException;
import com.newsrob.BackendProvider.SyncAPIException;
import com.newsrob.EntryManager.SyncJobStatus;
import com.newsrob.activities.UIHelper;
import com.newsrob.download.DownloadCancelledException;
import com.newsrob.download.DownloadContext;
import com.newsrob.download.DownloadException;
import com.newsrob.download.DownloadTimedOutException;
import com.newsrob.download.WebPageDownloadDirector;
import com.newsrob.jobs.Job;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.jobs.SynchronizeModelFailed;
import com.newsrob.jobs.SynchronizeModelSucceeded;
import com.newsrob.storage.IStorageAdapter;
import com.newsrob.util.PreviewGenerator;
import com.newsrob.util.SDK9Helper;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public class SynchronizationService extends Service {

    private static final String TAG = SynchronizationService.class.getSimpleName();

    static final String ACTION_SYNC_UPLOAD_ONLY = "upload_only";

    public static final String EXTRA_MANUAL_SYNC = "manual_sync";

    private static final String PREF_KEY_LAST_STARTED = "com.newsrob.synchronization.lastStarted";

    private static WakeLock wl;
    private Handler handler;

    private static final Class[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
    private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    private EntryManager entryManager;

    private boolean shouldDownloadArticlesInParallel;

    @Override
    public void onCreate() {
        super.onCreate();

        shouldDownloadArticlesInParallel = "1".equals(NewsRob.getDebugProperties(this).getProperty(
                "downloadArticlesInParallel", "1"));

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onCreate() called.");

        handler = new Handler();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent == null) {
            stopSelf();
            getEntryManager().getNewsRobNotificationManager().cancelSyncInProgressNotification();
            Log.d(TAG, "onStart() called with intent == null. Stopping self.");
        }

        if (getEntryManager().isModelCurrentlyUpdated())
            return;

        String lastStarted = getEntryManager().getSharedPreferences().getString(PREF_KEY_LAST_STARTED, "");
        if (!"".equals(lastStarted)) {
            String message = "The synchronization started at " + lastStarted + " was ended prematurely.";
            if (NewsRob.isDebuggingEnabled(this))
                PL.log(message, this);
            else
                Log.w(TAG, message);

        }
        setLastStarted(new Date().toString());

        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onStart() called. startId=" + startId + " intent=" + intent);

        boolean uO = false;
        boolean mS = false;
        try {
            uO = ACTION_SYNC_UPLOAD_ONLY.equals(intent.getAction());
            mS = intent.getBooleanExtra(EXTRA_MANUAL_SYNC, false);
        } catch (NullPointerException npe) {
            //
        }
        final boolean uploadOnly = uO;
        final boolean manualSync = mS;
        new Thread(new Runnable() {

            public void run() {
                try {
                    if (wl == null)
                        // throw new
                        // RuntimeException("Oh, oh. No wake lock acquired!");

                        acquireWakeLock(getApplicationContext());

                    doSync(uploadOnly, manualSync);
                } finally {
                    handler.post(new Runnable() {
                        public void run() {
                            stopSelf();
                        }
                    });
                }
            }

        }).start();

    }

    private void setLastStarted(String value) {
        SDK9Helper.apply(getEntryManager().getSharedPreferences().edit().putString(PREF_KEY_LAST_STARTED, value));
    }

    private void resetLastStarted() {
        setLastStarted("");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        PL.log(this, "onLowMemory() called.", null, getApplicationContext());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PL.log(this, "SynchronizationService.onDestroy() called.", null, getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        PL.log(this, "onBind() called.", null, getApplicationContext());
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        PL.log(this, "onRebind() called.", null, getApplicationContext());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        PL.log(this, "onUnbind() called.", null, getApplicationContext());
        return super.onUnbind(intent);
    }

    protected synchronized void doSync(final boolean uploadOnly, final boolean manualSync) {

        PL.log(this, "doSync invoked. (-1) wl=" + wl, null, getApplicationContext());
        final Context ctx = getApplicationContext();

        try {

            PL.log(this, "doSync invoked. (0)", null, getApplicationContext());

            WifiManager wifiManager = null; // (WifiManager)
                                            // getSystemService(Context.WIFI_SERVICE);

            if (false) {
                WifiLock wiFiLock = wifiManager.createWifiLock("NewsRobSync");
                wiFiLock.acquire();
            }
            PL.log(this, "doSync invoked. (1)", null, getApplicationContext());

            U.setLowPrio();
            PL.log(this, "doSync invoked. (2)", null, getApplicationContext());

            final EntryManager entryManager = getEntryManager();
            final BackendProvider grf = entryManager.getSyncInterface();
            final IStorageAdapter fileContextAdapter = entryManager.getStorageAdapter();
            PL.log(this, "doSync invoked. (3)", null, getApplicationContext());

            if (entryManager.isModelCurrentlyUpdated()) {
                PL.log(this, "doSync invoked. (3.4)", null, getApplicationContext());
                return;
            }
            PL.log(this, "doSync invoked. (3.5)", null, getApplicationContext());

            entryManager.lockModel("SSer.doSync");
            PL.log(this, "doSync invoked. (4)", null, getApplicationContext());

            PL.log("SynchronizationService. Used settings: "
                    + SettingsRenderer.renderSettings(entryManager, new StringBuilder("\n")),
                    SynchronizationService.this);

            Throwable caughtThrowable = null;

            PL.log("SynchronizationService - start", SynchronizationService.this);
            PL.log("Battery level=" + U.getBatteryChargedPercent(ctx) + "%.", ctx);
            PL.log("Last successful login: " + entryManager.getLastSuccessfulLogin(), SynchronizationService.this);
            Timing t = new Timing("Synchronization Runnable", this);

            long started = System.currentTimeMillis();
            Log.i(TAG, "Synchronization started at " + new Date().toString() + ". started=" + started);
            final SyncJobStatus syncJobStatus = new SyncJobStatus();

            // last used
            long lastUsed = entryManager.getLastUsed();

            final DownloadContext downloadContext = new DownloadContext();
            PL.log(this, "doSync invoked. (5)", null, getApplicationContext());

            ModelUpdateResult result = null;
            try {

                PL.log("Run Mark - in Try", SynchronizationService.this);
                if (!uploadOnly) {
                    try {
                        if (!Feed.restoreFeedsIfNeccesary(this))
                            Feed.saveFeedSettings(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                entryManager.fireModelUpdateStarted("Synchronization", uploadOnly, manualSync);
                List<Job> jobList = new LinkedList<Job>();
                Job deleteReadArticlesJob = new DeleteArticlesJob(SynchronizationService.this, entryManager,
                        syncJobStatus);

                Job reduceToCapacityJob = new ReduceToCapacityJob(SynchronizationService.this, entryManager,
                        syncJobStatus);

                if (!uploadOnly) {
                    if (entryManager.shouldReadItemsBeDeleted())
                        jobList.add(deleteReadArticlesJob);
                    jobList.add(reduceToCapacityJob);
                }

                jobList.add(new SyncChangedArticlesStatusJob(SynchronizationService.this, entryManager, syncJobStatus,
                        manualSync));

                if (!uploadOnly && entryManager.shouldReadItemsBeDeleted())
                    jobList.add(deleteReadArticlesJob);

                if (!uploadOnly) {
                    jobList.add(new Job("Unsubscribing from feeds", entryManager) {

                        @Override
                        public void run() throws Exception {
                            if (!entryManager.syncCurrentlyEnabled(manualSync))
                                return;

                            Cursor c = entryManager.getFeeds2UnsubscribeCursor();
                            try {
                                while (c.moveToNext()) {
                                    String feedAtomId = c.getString(1);
                                    PL.log("Unsubscribing: " + feedAtomId, SynchronizationService.this);
                                    entryManager.getSyncInterface().unsubscribeFeed(feedAtomId);
                                }
                            } finally {
                                c.close();
                            }
                        }
                    });
                }

                if (!uploadOnly)
                    jobList.add(new FetchUnreadArticlesJob(SynchronizationService.this, entryManager, syncJobStatus,
                            manualSync));

                if (!uploadOnly)
                    jobList.add(new Job("Daily update of subscriptions (feed titles)", entryManager) {

                        @Override
                        public void run() throws IOException, ParserConfigurationException, SAXException,
                                ServerBadRequestException, AuthenticationExpiredException {

                            if (entryManager.syncCurrentlyEnabled(manualSync)) {
                                grf.updateSubscriptionList(entryManager, this);
                                entryManager.fireModelUpdated();
                            }
                        }

                    });

                if (!uploadOnly)
                    jobList.add(reduceToCapacityJob);

                if (!uploadOnly && entryManager.shouldReadItemsBeDeleted())
                    jobList.add(deleteReadArticlesJob);

                PL.log(this, "doSync invoked. (6)", null, getApplicationContext());

                if (!uploadOnly) {

                    // make sure that a manual sync moves the automatic sync
                    // forward,
                    // i.e. when pushing "Refresh" in the middle of a 24h sync,
                    // reset timer to 0, so that it will be another 24h from
                    // this
                    // point on
                    entryManager.getScheduler().resetBackgroundSchedule();

                    final SyncJobStatus sjStatus = new SyncJobStatus();
                    jobList.add(new SyncJob(ctx, entryManager, sjStatus, "Downloading articles") {

                        private Collection<Long> entries2Download;

                        @Override
                        public int doRun() {

                            if (entryManager.getSharedPreferences()
                                    .getString(EntryManager.SETTINGS_STORAGE_ASSET_DOWNLOAD, EntryManager.DOWNLOAD_YES)
                                    .equals(EntryManager.DOWNLOAD_NO)) {
                                Log.d(TAG,
                                        "Downloading of assets is disabled in the settings. Therefore skipping downloading webpages.");
                                return actual;
                            }

                            Timing tSql = new Timing("SQL Query findAllToDownload", SynchronizationService.this);
                            entries2Download = entryManager.findAllArticleIds2Download();
                            target = entries2Download.size();

                            tSql.stop();
                            Timing tOutter = new Timing("Downloading all " + entries2Download.size()
                                    + " pages or well, the ones that were downloaded", SynchronizationService.this);

                            // shouldDownloadArticlesInParallel = true;
                            final int numberOfThreads = shouldDownloadArticlesInParallel && !U.isScreenOn(ctx) ? 3 : 1;

                            PL.log("Instantiating Download Articles ScheduledExecutorService for " + numberOfThreads
                                    + " threads.", ctx);
                            final ScheduledExecutorService pool = Executors.newScheduledThreadPool(numberOfThreads);

                            int count = 0;
                            try {
                                actual = 1;
                                entryManager.fireStatusUpdated();
                                for (Long articleId : entries2Download) {

                                    // get the current data
                                    // LATER use a real cursor and somehow find
                                    // out
                                    // when
                                    // data became stale
                                    Entry entry = entryManager.findArticleById(articleId);
                                    if (entry == null)
                                        continue;

                                    if (!entryManager.downloadContentCurrentlyEnabled(manualSync))
                                        return actual;

                                    if (!fileContextAdapter.canWrite()) {
                                        Log.d(TAG,
                                                "File context adapter ("
                                                        + fileContextAdapter.getClass().getName()
                                                        + ") cannot be written to at the moment. Mounted? Read Only? Not downloading web pages.");
                                        return actual;
                                    }

                                    if (isCancelled())
                                        break;

                                    if (entry.getReadState() == ReadState.READ && !entry.isStarred())
                                        continue;

                                    int resolvedDownloadPref = entry.getResolvedDownloadPref(entryManager);

                                    if (resolvedDownloadPref == Feed.DOWNLOAD_HEADERS_ONLY) {
                                        // entry.setDownloaded(Entry.STATE_DOWNLOADED_FULL_PAGE
                                        // :
                                        // Entry.STATE_DOWNLOADED_FEED_CONTENT);
                                        // entry.setError(null);
                                        // entryManager.fireModelUpdated();

                                        continue;
                                    }

                                    // check against the db, because in the
                                    // meantime
                                    // the
                                    // read status might have changed

                                    boolean downloadTheWholePage = (resolvedDownloadPref == Feed.DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE || resolvedDownloadPref == Feed.DOWNLOAD_PREF_FEED_AND_WEBPAGE);

                                    String summary = entry.getContent() != null ? entry.getContent() : UIHelper
                                            .linkize(entry.getAlternateHRef(), entry.getTitle());

                                    WebPageDownloadTask task = new WebPageDownloadTask(entryManager,
                                            fileContextAdapter, this, entry, summary, downloadTheWholePage, manualSync,
                                            downloadContext);

                                    if (true)
                                        // pool.submit(task);
                                        pool.schedule(task, count++ * 500, TimeUnit.MILLISECONDS);
                                    else
                                        try {
                                            task.call();
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                }
                            } finally {
                                PL.log(this, "doSync invoked. (6.1 Pool Shutdown 1)", null, getApplicationContext());

                                pool.shutdown();
                                PL.log(this, "doSync invoked. (6.2 Pool Shutdown 2)", null, getApplicationContext());

                                try {
                                    while (true) {
                                        // wait and check if the pool is done.
                                        boolean terminated = pool.awaitTermination(2, TimeUnit.SECONDS);

                                        // done?
                                        if (terminated)
                                            break;

                                        boolean terminate = false;

                                        if (!entryManager.downloadContentCurrentlyEnabled(manualSync))
                                            terminate = true;

                                        if (!terminate && !fileContextAdapter.canWrite()) {
                                            Log.d(TAG,
                                                    "File context adapter ("
                                                            + fileContextAdapter.getClass().getName()
                                                            + ") cannot be written to at the moment. Mounted? Read Only? Not downloading web pages.");
                                            terminate = true;
                                        }

                                        if (!terminate && isCancelled())
                                            terminate = true;

                                        if (terminate) {
                                            PL.log("Terminating downloadpagetask pool", ctx);
                                            pool.shutdownNow();
                                            break;
                                        }

                                        // all good so far go back to the
                                        // beginning
                                        // and check those
                                    }
                                } catch (InterruptedException e) {
                                    // Ignore
                                    e.printStackTrace();
                                    PL.log(this, "Interrupted Exception", e, ctx);

                                } finally {
                                    PL.log(this, "doSync invoked. (6.3 Pool ShutdownNow 1)", null,
                                            getApplicationContext());

                                    pool.shutdownNow();
                                    PL.log(this, "doSync invoked. (6.4 Pool ShutdownNow 2)", null,
                                            getApplicationContext());
                                }
                            }
                            tOutter.stop();
                            return actual;
                        }

                    });
                    jobList.add(new Job("Vacuuming database ... ", entryManager) {

                        @Override
                        public void run() throws Throwable {
                            entryManager.vacuumDb();
                        }
                    });
                }

                PL.log("Run Mark - Jobs added", this);
                PL.log(this, "doSync invoked. (7)", null, getApplicationContext());

                entryManager.runJobs(jobList);

                PL.log(this, "doSync invoked. (7.1 After Run Jobs)", null, getApplicationContext());

                Log.d(TAG, "NoOfEntriesUpdated=" + syncJobStatus.noOfEntriesUpdated);
                Log.d(TAG, "NoOfEntriesFetched=" + syncJobStatus.noOfEntriesFetched);

                PL.log("Run Mark - Mission accomplished. -> complete ", this);

                result = new SynchronizeModelSucceeded(syncJobStatus.noOfEntriesUpdated);
            } catch (Throwable throwable) {
                result = new SynchronizeModelFailed(throwable);
                Log.d(TAG, "Problem during synchronization.", throwable);
                PL.log(this, "Problem during synchronization", throwable, ctx);
            } finally {
                PL.log("Run Mark - In Finally", this);
                entryManager.unlockModel("SSer.doSync");
                entryManager.clearCancelState();
                entryManager.fireModelUpdateFinished(result);
                entryManager.fireStatusUpdated();
                Log.i(TAG, "Synchronization finished at " + new Date().toString() + ". started=" + started);

                t.stop();
                PL.log(this, "doSync invoked. (7.2)", null, getApplicationContext());

                if (!uploadOnly)
                    entryManager.setLastSync(caughtThrowable == null);

                int noOfNewArticles = entryManager.getNoOfNewArticlesSinceLastUsed(lastUsed);
                entryManager.getNewsRobNotificationManager().notifyNewArticles(entryManager, lastUsed, noOfNewArticles);

                PL.log("Run Mark - End of Finally", this);
                PL.log("Battery level=" + U.getBatteryChargedPercent(ctx) + "%.", ctx);
                resetLastStarted();
                PL.log(this, "doSync invoked. (8)", null, getApplicationContext());
            }
        } finally {
            PL.log(this, "doSync invoked. (9)", null, getApplicationContext());
            releaseWakeLock(ctx);

            PL.log(this, "doSync invoked. (9.1)", null, getApplicationContext());
            // wiFiLock.release(); // TODO
        }
        PL.log(this, "doSync invoked. (10)", null, getApplicationContext());
        PL.log("SynchronizationService. Used settings: "
                + SettingsRenderer.renderSettings(entryManager, new StringBuilder("\n")), SynchronizationService.this);

    }

    private EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(getApplicationContext());

        return entryManager;
    }

    public static synchronized void acquireWakeLock(Context ctx) {
        PL.log(SynchronizationService.class.getSimpleName() + ": Trying to acquire WakeLock wl=" + wl, ctx);
        if (wl != null) {
            PL.log(SynchronizationService.class.getSimpleName()
                    + ": Trying to acquire WakeLock, even though it exists already. wl=" + wl, ctx);
            return;

        }

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
        PL.log(SynchronizationService.class.getSimpleName() + ": Trying to acquire WakeLock: success wl=" + wl, ctx);

    }

    public static synchronized void releaseWakeLock(Context ctx) {
        PL.log(SynchronizationService.class.getSimpleName() + ": Trying to release WakeLock wl=" + wl, ctx);

        try {
            if (wl != null)
                wl.release();
            else
                PL.log(SynchronizationService.class.getSimpleName() + ": Trying to release WakeLock: WL Was null. wl="
                        + wl, ctx);

            wl = null;
            PL.log(SynchronizationService.class.getSimpleName() + ": Trying to release WakeLock: success wl=" + wl, ctx);

        } catch (Exception e) {
            Log.e(TAG, "Oops. Problem when releasing wake lock.", e);
            PL.log(SynchronizationService.class.getSimpleName() + ": Trying to release WakeLock: error wl=" + wl, e,
                    ctx);
        }
    }

}

abstract class SyncJob extends Job {
    private EntryManager entryManager;
    private Context context;
    private SyncJobStatus status;
    public int target;
    public int actual;

    SyncJob(Context context, EntryManager entryManager, SyncJobStatus status, String message) {
        super(message, entryManager);
        this.entryManager = entryManager;
        this.context = context;
        this.status = status;
    }

    protected EntryManager getEntryManager() {
        return entryManager;
    }

    protected Context getContext() {
        return context;
    }

    protected SyncJobStatus getSyncJobStatus() {
        return status;
    }

    @Override
    public boolean isProgressMeassurable() {
        return target != -1;
    }

    @Override
    public int[] getProgress() {
        return new int[] { actual, target };
    }

    protected abstract int doRun() throws Throwable;

    public void run() throws Throwable {
        PL.log("About to be executed: " + getJobDescription(), context);
        target = -1;
        actual = -1;
        int noOfArticlesAffected = doRun();
        PL.log("No of articles affected=" + noOfArticlesAffected, context);
        status.noOfEntriesUpdated += noOfArticlesAffected;
        if (status.noOfEntriesUpdated > 0)
            entryManager.fireModelUpdated();

    }
}

class DeleteArticlesJob extends SyncJob {

    public DeleteArticlesJob(Context context, EntryManager entryManager, SyncJobStatus status) {
        super(context, entryManager, status, "Deleting read articles");
    }

    @Override
    public int doRun() {
        return getEntryManager().deleteReadArticles(this);
    }
}

class ReduceToCapacityJob extends SyncJob {

    public ReduceToCapacityJob(Context context, EntryManager entryManager, SyncJobStatus status) {
        super(context, entryManager, status, "Deleting oldest articles over capacity");
    }

    @Override
    public int doRun() {
        return getEntryManager().reduceToCapacity(this);
    }

}

class FetchUnreadArticlesJob extends SyncJob {
    private boolean manualSync;

    public FetchUnreadArticlesJob(Context context, EntryManager entryManager, SyncJobStatus status, boolean manualSync) {
        super(context, entryManager, status, "Fetching new articles from "
                + SyncInterfaceFactory.getSyncInterface(context).getServiceName());
        this.manualSync = manualSync;
    }

    @Override
    public int doRun() throws ClientProtocolException, IllegalStateException, IOException, NeedsSessionException,
            SAXException, ParserConfigurationException, FactoryConfigurationError, SyncAPIException,
            ServerBadRequestException, AuthenticationExpiredException {

        if (!getEntryManager().syncCurrentlyEnabled(manualSync))
            return 0;

        final BackendProvider grf = getEntryManager().getSyncInterface();

        int noOfEntriesFetched = 0;

        noOfEntriesFetched = grf.fetchNewEntries(getEntryManager(), this, manualSync);

        getSyncJobStatus().noOfEntriesFetched = noOfEntriesFetched;

        getEntryManager().fireModelUpdated();
        return noOfEntriesFetched;

    }
}

class SyncChangedArticlesStatusJob extends SyncJob {
    private boolean manualSync;

    SyncChangedArticlesStatusJob(Context context, EntryManager entryManager, SyncJobStatus status, boolean manualSync) {
        super(context, entryManager, status, "Sync status of changed articles");
        this.manualSync = manualSync;
    }

    @Override
    public int doRun() throws MalformedURLException, IOException, ParserConfigurationException,
            FactoryConfigurationError, SAXException, ParseException, NeedsSessionException {
        if (!getEntryManager().syncCurrentlyEnabled(manualSync))
            return 0;

        int noOfEntriesUpdated = getEntryManager().getSyncInterface().synchronizeArticles(getEntryManager(), this);
        getSyncJobStatus().noOfEntriesUpdated += noOfEntriesUpdated;
        if (noOfEntriesUpdated > 0)
            getEntryManager().fireModelUpdated();

        return noOfEntriesUpdated;

    }
}

class WebPageDownloadTask implements Callable<Void> {

    private static final String TAG = WebPageDownloadTask.class.getSimpleName();

    private static ReentrantLock instapaperLock = new ReentrantLock();

    private EntryManager entryManager;
    private IStorageAdapter fileContextAdapter;
    private SyncJob job;
    private String entryShortAtomId;
    private String pageUrl;
    Entry entry;
    private String summary;
    private boolean downloadCompleteWebPage;
    private boolean manualSync;

    private DownloadContext downloadContext;

    public WebPageDownloadTask(EntryManager entryManager, IStorageAdapter fileContextAdapter, SyncJob job, Entry entry,
            String summary, boolean downloadCompleteWebPage, boolean manualSync, DownloadContext downloadContext) {

        this.entryManager = entryManager;
        this.fileContextAdapter = fileContextAdapter;
        this.job = job;
        this.entryShortAtomId = entry.getShortAtomId();
        this.entry = entry;
        this.pageUrl = entry.getBaseUrl(entryManager);
        this.summary = summary;
        this.downloadCompleteWebPage = downloadCompleteWebPage;
        this.manualSync = manualSync;
        this.downloadContext = downloadContext;
    }

    @Override
    public Void call() throws Exception {
        try {
            U.setLowPrio();

            Context ctx = entryManager.getContext();

            // Tracking the hosts that timed out once, so
            // that we then can skip trying other articles for that host.
            // This is for pages like FAZ.net

            Timing tInner = new Timing("Downloading page " + pageUrl, ctx);
            final String downloadHost = new URL(entry.getAlternateHRef()).getHost().toString();

            boolean downloadingFromInstapaper = false;
            try {

                // Single download only / Instapaper

                if (downloadHost.contains("instapaper")) {

                    instapaperLock.lock();
                    downloadingFromInstapaper = true;
                }

                if (downloadContext.containsTimedOutHost(downloadHost)) {
                    Log.w(SynchronizationService.class.getSimpleName(), "Article " + entry.getTitle()
                            + " not downloaded, because the host is on the timeout list.");
                    entry.setError("This host (" + downloadHost
                            + ") timed out during the sync. We'll try again during next sync.");
                } else {

                    // check free space
                    float freeSpaceLeft = fileContextAdapter.megaBytesFree();
                    Log.d(TAG, String.format("Free space remaining for downloads: %.2f MB.", freeSpaceLeft));
                    if (freeSpaceLeft < 0) {
                        PL.log(TAG + ": Oh no, free space left is a negative value ;-( Ignoring it.", ctx);

                    } else if (freeSpaceLeft < fileContextAdapter.megaBytesThreshold()) {
                        PL.log(TAG + ": Not enough space left to download page.", ctx);

                        entryManager.getNewsRobNotificationManager().createSyncSpaceExceededProblemNotification(
                                fileContextAdapter.megaBytesThreshold());
                        return null;
                    }

                    final long downloadStartedAt = System.currentTimeMillis();
                    URL url = new URL(pageUrl);
                    PL.log("NewsRob Downloader: pageUrl: " + pageUrl + " URL: " + url.toString(), ctx);
                    WebPageDownloadDirector.downloadWebPage(entryShortAtomId, url, fileContextAdapter, job, summary,
                            downloadCompleteWebPage, entryManager, manualSync);

                    generatePreview(ctx);

                    entry.setDownloaded(downloadCompleteWebPage ? Entry.STATE_DOWNLOADED_FULL_PAGE
                            : Entry.STATE_DOWNLOADED_FEED_CONTENT);
                    entry.setError("Download took " + (System.currentTimeMillis() - downloadStartedAt) + " ms.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Problem dowloading page " + entry.getAlternateHRef() + ".", e);

                Throwable cause = null;

                if (e instanceof DownloadException) {
                    cause = ((DownloadException) e).getCause();
                    Log.d(TAG, "DownloadException cause=" + cause);
                } else
                    Log.d(TAG, "Exception=" + e);
                boolean downloadError = false;

                if (e instanceof DownloadTimedOutException) {

                    Log.w(SynchronizationService.class.getSimpleName(), "Download for " + entry.getAlternateHRef()
                            + " timed out. Adding host to timed out hosts list.");
                    downloadContext.addTimedOutHost(downloadHost);
                    entry.setError("Download timed out.");
                    entry.setDownloaded(Entry.STATE_DOWNLOAD_ERROR);
                } else {

                    if (e instanceof DownloadCancelledException
                            || cause != null
                            && (cause instanceof FileNotFoundException || cause instanceof SocketTimeoutException
                                    || cause instanceof SocketException || cause instanceof NoHttpResponseException
                                    || cause instanceof UnknownHostException || cause instanceof DownloadCancelledException)) {
                        Log.d(TAG, "Caught a FNFE");

                    } else {
                        Log.d(TAG, "Marked download as error.");
                        downloadError = true;
                    }
                    StringBuilder renderedStackTrace = new StringBuilder();
                    U.renderStackTrace(e, renderedStackTrace);
                    entry.setError(cause != null ? "Cause: " + cause.getClass().getSimpleName() + ": "
                            + cause.getMessage() : e.getClass().getSimpleName() + ": " + e.getMessage()
                            + "\nStacktrace: " + renderedStackTrace);
                    entry.setDownloaded(downloadError ? Entry.STATE_DOWNLOAD_ERROR : Entry.STATE_NOT_DOWNLOADED);
                }
            } finally {
                if (instapaperLock.isHeldByCurrentThread())
                    instapaperLock.unlock();
            }

            entryManager.updatedDownloaded(entry);
            job.actual++;
            entryManager.fireModelUpdated(entry.getAtomId());
            entryManager.fireStatusUpdated();

            tInner.stop();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    private void generatePreview(Context ctx) {

        // TODO
        // only
        // one
        // instance
        // TODO use display metrics?
        // TODO use orientation? Larger thumbs for
        // larger screens?

        final float screenSizeFactor = getScreenSizeFactor(ctx);
        final float previewScaleFactor = ctx.getResources().getDisplayMetrics().density;

        File assetsDir = entry.getAssetsDir(entryManager);
        PL.log("Generating preview for page "
                + entry.getAlternateHRef()
                + " successful?="
                + new PreviewGenerator(ctx, assetsDir, (int) (100 * previewScaleFactor * screenSizeFactor),
                        (int) (100 * previewScaleFactor * screenSizeFactor), (int) (6 * previewScaleFactor))
                        .generatePreview(), ctx);
    }

    private float getScreenSizeFactor(final Context ctx) {
        int size = U.getScreenSize(ctx);
        if (size > 0)
            return 1.25f;
        else if (size < 0)
            return 0.75f;
        return 1.0f;
    }

}