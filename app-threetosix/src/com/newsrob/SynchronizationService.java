package com.newsrob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.newsrob.EntryManager.SyncJobStatus;
import com.newsrob.activities.UIHelper;
import com.newsrob.download.DownloadCancelledException;
import com.newsrob.download.DownloadException;
import com.newsrob.download.DownloadTimedOutException;
import com.newsrob.download.WebPageDownloadDirector;
import com.newsrob.jobs.Job;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.jobs.SynchronizeModelFailed;
import com.newsrob.jobs.SynchronizeModelSucceeded;
import com.newsrob.storage.IStorageAdapter;
import com.newsrob.threetosix.R;
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

    @Override
    public void onCreate() {
        super.onCreate();

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
                if (wl == null)
                    // throw new
                    // RuntimeException("Oh, oh. No wake lock acquired!");
                    acquireWakeLock(getApplicationContext());
                startNotifying(uploadOnly);
                try {
                    doSync(uploadOnly, manualSync);
                } finally {
                    stopNotifying();
                    handler.post(new Runnable() {
                        public void run() {
                            stopSelf();
                        }
                    });
                }
            }

        }).start();

    }

    void startNotifying(boolean fastSyncOnly) {
        /*
         * if (false && getEntryManager().isSyncInProgressNotificationEnabled())
         * startForegroundCompat(999,
         * getEntryManager().getNewsRobNotificationManager()
         * .createSynchronizationRunningNotificationUsingRemoteViews
         * (fastSyncOnly));
         */
        // setForeground(true);

    }

    void stopNotifying() {
        if (false && getEntryManager().isSyncInProgressNotificationEnabled())
            stopForegroundCompat(999);
        // setForeground(false);
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
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onLowMemory() called.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onDestroy() called.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onBind() called.");
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onRebind() called.");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (NewsRob.isDebuggingEnabled(this))
            Log.d(TAG, "onUnbind() called.");
        return super.onUnbind(intent);
    }

    protected void doSync(final boolean uploadOnly, final boolean manualSync) {

        final EntryManager entryManager = getEntryManager();
        final EntriesRetriever grf = entryManager.getEntriesRetriever();
        final IStorageAdapter fileContextAdapter = entryManager.getStorageAdapter();

        if (entryManager.isModelCurrentlyUpdated())
            return;

        entryManager.lockModel("SSer.doSync");

        PL.log("SynchronizationService. Used settings: "
                + SettingsRenderer.renderSettings(entryManager, new StringBuilder("\n")), SynchronizationService.this);

        // entryManager.runningThread = new Thread(new Runnable() {
        Throwable caughtThrowable = null;

        // public void run() {
        final Context ctx = getApplicationContext();

        PL.log("SynchronizationService - start", SynchronizationService.this);
        PL.log("Last successful login: " + entryManager.getLastSuccessfulLogin(), SynchronizationService.this);
        // Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        Timing t = new Timing("Synchronization Runnable", this);

        long started = System.currentTimeMillis();
        Log.i(TAG, "Synchronization started at " + new Date().toString() + ". started=" + started);
        final SyncJobStatus syncJobStatus = new SyncJobStatus();

        // last used
        long lastUsed = entryManager.getLastUsed();

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
            Job deleteReadArticlesJob = new DeleteArticlesJob(SynchronizationService.this, entryManager, syncJobStatus);

            Job reduceToCapacityJob = new ReduceToCapacityJob(SynchronizationService.this, entryManager, syncJobStatus);

            if (!uploadOnly) {
                if (entryManager.shouldReadItemsBeDeleted())
                    jobList.add(deleteReadArticlesJob);
                jobList.add(reduceToCapacityJob);
            }

            jobList.add(new Job("Submitting annotated articles", entryManager) {

                @Override
                public void run() throws Exception {

                    if (entryManager.syncCurrentlyEnabled(manualSync))
                        entryManager.getEntriesRetriever().submitNotes(this);
                }

            });

            jobList.add(new SyncChangedArticlesStatusJob(SynchronizationService.this, entryManager, syncJobStatus,
                    manualSync));

            if (!uploadOnly && entryManager.shouldReadItemsBeDeleted())
                jobList.add(deleteReadArticlesJob);

            Job removeDeletedNotes = new Job("Removing submitted notes", entryManager) {

                @Override
                public void run() throws Exception {

                    if (entryManager.syncCurrentlyEnabled(manualSync))
                        entryManager.getEntriesRetriever().removeDeletedNotes();
                }

            };

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
                                entryManager.getEntriesRetriever().unsubscribeFeed(feedAtomId);
                            }
                        } finally {
                            c.close();
                        }
                    }
                });
            }

            if (!uploadOnly)
                jobList.add(removeDeletedNotes);

            if (!uploadOnly)
                jobList.add(new FetchUnreadArticlesJob(SynchronizationService.this, entryManager, syncJobStatus,
                        manualSync));

            if (!uploadOnly)
                jobList.add(new Job("Daily update of subscriptions (feed titles)", entryManager) {

                    @Override
                    public void run() throws IOException, ParserConfigurationException, SAXException,
                            GRTokenExpiredException {

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

            if (!uploadOnly) {

                // make sure that a manual sync moves the automatic sync
                // forward,
                // i.e. when pushing "Refresh" in the middle of a 24h sync,
                // reset timer to 0, so that it will be another 24h from this
                // point on
                entryManager.getScheduler().resetBackgroundSchedule();

                final float screenSizeFactor = getScreenSizeFactor(ctx);

                final float previewScaleFactor = ctx.getResources().getDisplayMetrics().density;

                jobList.add(new Job("Downloading articles", entryManager) {

                    private int currentArticle = 0;
                    private Collection<Long> entries2Download;

                    @Override
                    public void run() {
                        if (entryManager.getSharedPreferences().getString(EntryManager.SETTINGS_STORAGE_ASSET_DOWNLOAD,
                                EntryManager.DOWNLOAD_YES).equals(EntryManager.DOWNLOAD_NO)) {
                            Log
                                    .d(TAG,
                                            "Downloading of assets is disabled in the settings. Therefore skipping downloading webpages.");
                            return;
                        }

                        Timing tSql = new Timing("SQL Query findAllToDownload", SynchronizationService.this);
                        entries2Download = entryManager.findAllArticleIds2Download();
                        tSql.stop();
                        Timing tOutter = new Timing("Downloading all " + entries2Download.size()
                                + " pages or well, the ones that were downloaded", SynchronizationService.this);

                        for (Long articleId : entries2Download) {
                            // get the current data
                            // LATER use a real cursor and somehow find out when
                            // data became stale
                            Entry entry = entryManager.findArticleById(articleId);
                            if (entry == null)
                                continue;

                            if (!entryManager.downloadContentCurrentlyEnabled(manualSync))
                                return;

                            if (!fileContextAdapter.canWrite()) {
                                Log
                                        .d(
                                                TAG,
                                                "File context adapter ("
                                                        + fileContextAdapter.getClass().getName()
                                                        + ") cannot be written to at the moment. Mounted? Read Only? Not downloading web pages.");
                                return;
                            }

                            if (isCancelled())
                                break;
                            // System.out.println("----------------- " +
                            // entry.getTitle());
                            currentArticle++;
                            entryManager.fireStatusUpdated();
                            // don't download read entries, except the starred
                            // ones
                            if (entry.getReadState() == ReadState.READ && !entry.isStarred() && !entry.isNote())
                                continue;

                            int resolvedDownloadPref = entry.getResolvedDownloadPref(entryManager);

                            if (resolvedDownloadPref == Feed.DOWNLOAD_HEADERS_ONLY) {
                                // entry.setDownloaded(Entry.STATE_DOWNLOADED_FULL_PAGE
                                // : Entry.STATE_DOWNLOADED_FEED_CONTENT);
                                // entry.setError(null);
                                // entryManager.fireModelUpdated();

                                continue;
                            }

                            // check against the db, because in the
                            // meantime
                            // the
                            // read status might have changed

                            Timing tInner = new Timing("Downloading page " + entry.getAlternateHRef(),
                                    SynchronizationService.this);
                            try {
                                // check free space
                                float freeSpaceLeft = fileContextAdapter.megaBytesFree();
                                Log
                                        .d(TAG, String.format("Free space remaining for downloads: %.2f MB.",
                                                freeSpaceLeft));
                                if (freeSpaceLeft < 0) {
                                    PL.log(TAG + ": Oh no, free space left is a negative value ;-( Ignoring it.", ctx);

                                } else if (freeSpaceLeft < fileContextAdapter.megaBytesThreshold()) {
                                    PL.log(TAG + ": Not enough space left to download page.", ctx);

                                    entryManager.getNewsRobNotificationManager()
                                            .createSyncSpaceExceededProblemNotification(
                                                    fileContextAdapter.megaBytesThreshold());
                                    break;
                                }

                                boolean downloadTheWholePage = (resolvedDownloadPref == Feed.DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE || resolvedDownloadPref == Feed.DOWNLOAD_PREF_FEED_AND_WEBPAGE);

                                String summary = entry.getContent() != null ? entry.getContent() : UIHelper.linkize(
                                        entry.getAlternateHRef(), entry.getTitle());
                                WebPageDownloadDirector.downloadWebPage(entry.getShortAtomId(), new URL(entry
                                        .getBaseUrl(entryManager)), fileContextAdapter, this, summary,
                                        downloadTheWholePage, entryManager, manualSync);
                                if (true) {

                                    File assetsDir = entry.getAssetsDir(entryManager);
                                    System.out.println("generatePreview="
                                            + new PreviewGenerator(ctx, assetsDir,
                                                    (int) (100 * previewScaleFactor * screenSizeFactor),
                                                    (int) (100 * previewScaleFactor * screenSizeFactor),
                                                    (int) (6 * previewScaleFactor)).generatePreview());
                                }
                                // TODO
                                // only
                                // one
                                // instance
                                // TODO use display metrics?
                                // TODO use orientation? Larger thumbs for
                                // larger screens?
                                entry.setDownloaded(downloadTheWholePage ? Entry.STATE_DOWNLOADED_FULL_PAGE
                                        : Entry.STATE_DOWNLOADED_FEED_CONTENT);
                                entry.setError(null);
                                entryManager.fireModelUpdated();
                            } catch (Exception e) {
                                Log.e(TAG, "Problem dowloading page " + entry.getAlternateHRef() + ".", e);

                                Throwable cause = null;

                                if (e instanceof DownloadException) {
                                    cause = ((DownloadException) e).getCause();
                                    Log.d(TAG, "DownloadException cause=" + cause);
                                } else
                                    Log.d(TAG, "Exception=" + e);
                                boolean downloadError = false;
                                if (e instanceof DownloadCancelledException
                                        || cause != null
                                        && (cause instanceof FileNotFoundException
                                                || cause instanceof SocketTimeoutException
                                                || cause instanceof SocketException
                                                || cause instanceof NoHttpResponseException
                                                || cause instanceof UnknownHostException
                                                || cause instanceof DownloadCancelledException || cause instanceof DownloadTimedOutException)) {
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
                                entry.setDownloaded(downloadError ? Entry.STATE_DOWNLOAD_ERROR
                                        : Entry.STATE_NOT_DOWNLOADED);
                            }
                            entryManager.updatedDownloaded(entry);
                            tInner.stop();
                        }
                        tOutter.stop();

                    }

                    @Override
                    public boolean isProgressMeassurable() {
                        return true;
                    }

                    @Override
                    public int[] getProgress() {
                        return new int[] { currentArticle, entries2Download != null ? entries2Download.size() : 0 };
                    }

                });
            }

            PL.log("Run Mark - Jobs added", this);

            entryManager.runJobs(jobList);

            Log.d(TAG, "NoOfEntriesUpdated=" + syncJobStatus.noOfEntriesUpdated);
            Log.d(TAG, "NoOfEntriesFetched=" + syncJobStatus.noOfEntriesFetched);

            PL.log("Run Mark - Mission accomplished. -> complete ", this);

            result = new SynchronizeModelSucceeded(syncJobStatus.noOfEntriesUpdated);
        } catch (Throwable throwable) {
            result = new SynchronizeModelFailed(throwable);
            Log.d(TAG, "Problem during synchronization.", throwable);
        } finally {
            PL.log("Run Mark - In Finally", this);
            entryManager.unlockModel("SSer.doSync");
            entryManager.clearCancelState();
            entryManager.fireModelUpdateFinished(result);
            entryManager.fireStatusUpdated();
            Log.i(TAG, "Synchronization finished at " + new Date().toString() + ". started=" + started);

            t.stop();
            if (!uploadOnly)
                entryManager.setLastSync(caughtThrowable == null);

            int noOfNewArticles = entryManager.getNoOfNewArticlesSinceLastUsed(lastUsed);
            entryManager.getNewsRobNotificationManager().notifyNewArticles(entryManager, lastUsed, noOfNewArticles);

            PL.log("Run Mark - End of Finally", this);
            resetLastStarted();
            releaseWakeLock();
        }
    }

    private float getScreenSizeFactor(final Context ctx) {
        int size = U.getScreenSize(ctx);
        if (size > 0)
            return 1.25f;
        else if (size < 0)
            return 0.75f;
        return 1.0f;
    }

    private EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(getApplicationContext());

        return entryManager;
    }

    public static void acquireWakeLock(Context ctx) {
        if (wl != null)
            return;

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
    }

    public static void releaseWakeLock() {
        try {
            if (wl != null)
                wl.release();
            wl = null;
        } catch (Exception e) {
            Log.e(TAG, "Oops. Problem when releasing wake lock.", e);
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            }
            return;
        }

        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            }
            return;
        }

        // Fall back on the old API. Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        setForeground(false);
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
        super(context, entryManager, status, "Fetching new articles from Google Reader");
        this.manualSync = manualSync;
    }

    @Override
    public int doRun() throws ClientProtocolException, IllegalStateException, IOException, NeedsSessionException,
            SAXException, ParserConfigurationException, FactoryConfigurationError, ReaderAPIException,
            GRTokenExpiredException {

        if (!getEntryManager().syncCurrentlyEnabled(manualSync))
            return 0;

        final EntriesRetriever grf = getEntryManager().getEntriesRetriever();

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

        int noOfEntriesUpdated = getEntryManager().getEntriesRetriever().synchronizeWithGoogleReader(getEntryManager(),
                this);
        getSyncJobStatus().noOfEntriesUpdated += noOfEntriesUpdated;
        if (noOfEntriesUpdated > 0)
            getEntryManager().fireModelUpdated();

        return noOfEntriesUpdated;

    }
}