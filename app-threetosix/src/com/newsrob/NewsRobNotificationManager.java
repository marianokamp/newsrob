package com.newsrob;

import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.newsrob.activities.ArticleListActivity;
import com.newsrob.activities.LoginActivity;
import com.newsrob.activities.UIHelper;
import com.newsrob.jobs.Job;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.locale.FireReceiver;
import com.newsrob.threetosix.R;
import com.newsrob.util.U;

public class NewsRobNotificationManager implements IEntryModelUpdateListener {

    static final int NOTIFICATION_SYNCHRONIZATION_RUNNING = 8;
    static final int NOTIFICATION_SYNCHRONIZATION_STOPPED_WITH_ERROR = 1;
    static final int NOTIFICATION_SYNCHRONIZATION_STOPPED_SPACE_EXCEEDED = 2;
    static final int NOTIFICATION_NEW_ARTICLES = 2;

    private NotificationManager nm;
    private Context context;

    private boolean displaysNotification;

    NewsRobNotificationManager(Context context) {
        this.nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context.getApplicationContext();
    }

    public void cancelSyncProblemNotification() {
        nm.cancel(NOTIFICATION_SYNCHRONIZATION_STOPPED_WITH_ERROR);
        nm.cancel(NOTIFICATION_SYNCHRONIZATION_STOPPED_SPACE_EXCEEDED);
    }

    void cancelSyncInProgressNotification() {
        nm.cancel(NOTIFICATION_SYNCHRONIZATION_RUNNING);
        displaysNotification = false;
        PL.log("NOTIFICATION: Running: unset", context);
    }

    public void createCheckReleaseNotesNotification(Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification n = new Notification(R.drawable.gen_auto_notification_icon, "NewsRob has been updated", new Date()
                .getTime());
        n.setLatestEventInfo(context, "NewsRob has been updated", "Tap to open release notes.", PendingIntent
                .getActivity(context, 0, i, 0));
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        nm.notify(9292, n);
    }

    public void createSyncSpaceExceededProblemNotification(int reservedSpaceInMB) {

        Intent intent = new Intent(context, DashboardListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String message = "Not enough space left to download articles.\n<" + reservedSpaceInMB + " MB free.";
        Notification n = new Notification(R.drawable.gen_auto_notification_sync_problem, message, new Date().getTime());
        // Notification n = new Notification(R.drawable.sync_problem,
        // U.t(context,
        // R.string.login_to_google_needed), new Date().getTime());
        n.setLatestEventInfo(context, U.t(context, R.string.app_name), message, pendingIntent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        nm.notify(NOTIFICATION_SYNCHRONIZATION_STOPPED_WITH_ERROR, n);

    }

    private Notification createSynchronizationProblemNotification(String captchaToken, String captchaUrl,
            boolean loginExpired) {

        Intent intent = new Intent().setClass(context, LoginActivity.class);
        intent.putExtra(EntryManager.EXTRA_LOGIN_EXPIRED, true);
        if (captchaToken != null) {
            intent.putExtra(EntryManager.EXTRA_CAPTCHA_TOKEN, captchaToken);
            intent.putExtra(EntryManager.EXTRA_CAPTCHA_URL, captchaUrl);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification n = new Notification(R.drawable.gen_auto_notification_sync_problem, U.t(context,
                R.string.login_to_google_needed), new Date().getTime());
        // Notification n = new Notification(R.drawable.sync_problem,
        // U.t(context,
        // R.string.login_to_google_needed), new Date().getTime());
        n.setLatestEventInfo(context, U.t(context, R.string.app_name), U.t(context, R.string.login_to_google_needed),
                pendingIntent); // LATER
        // i18n
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        return n;
    }

    private Notification createSynchronizationRunningNotification(boolean fastSyncOnly) {

        Notification n = new Notification(R.drawable.gen_auto_notification_icon, context.getResources().getString(
                fastSyncOnly ? R.string.fast_synchronization_running_notification_title
                        : R.string.synchronization_running_notification_title), new Date().getTime());
        Intent intent = new Intent(context, DashboardListActivity.class);
        intent.putExtra("showProgress", true);

        n.setLatestEventInfo(context, U.t(context,
                fastSyncOnly ? R.string.fast_synchronization_running_notification_title
                        : R.string.synchronization_running_notification_title), U.t(context,
                fastSyncOnly ? R.string.fast_synchronization_running_notification_summary
                        : R.string.synchronization_running_notification_summary), PendingIntent.getActivity(context, 0,
                intent, 0));
        n.flags = Notification.FLAG_ONGOING_EVENT;

        return n;
    }

    Notification createSynchronizationRunningNotificationOld(boolean fastSyncOnly) {

        final EntryManager entryManager = EntryManager.getInstance(context);

        final Notification n = new Notification(R.drawable.gen_auto_notification_icon, context.getResources()
                .getString(
                        fastSyncOnly ? R.string.fast_synchronization_running_notification_title
                                : R.string.synchronization_running_notification_title), new Date().getTime());

        n.flags = Notification.FLAG_ONGOING_EVENT;

        final RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.in_progress_notification);
        n.contentView = contentView;

        Intent cancelSyncIntent = new Intent("com.newsrob.CANCEL_SYNC");
        // Intent cancelSyncIntent = new Intent();
        cancelSyncIntent.setClass(context, FireReceiver.class);
        PendingIntent pendingCancelSyncIntent = PendingIntent.getBroadcast(context, 0, cancelSyncIntent, 0);
        contentView.setOnClickPendingIntent(R.id.cancel_sync, pendingCancelSyncIntent);

        Intent showDashboardIntent = new Intent(context, DashboardListActivity.class);
        PendingIntent showDashboardPendingIntent = PendingIntent.getActivity(context, 0, showDashboardIntent, 0);
        n.contentIntent = pendingCancelSyncIntent;// showDashboardPendingIntent;

        updateContentView(entryManager, contentView);

        entryManager.addListener(new IEntryModelUpdateListener() {

            @Override
            public void statusUpdated() {
                updateContentView(entryManager, contentView);
                nm.notify(NOTIFICATION_SYNCHRONIZATION_RUNNING, n);
            }

            @Override
            public void modelUpdated(String atomId) {

            }

            @Override
            public void modelUpdated() {

            }

            @Override
            public void modelUpdateStarted(boolean fastSyncOnly) {

            }

            @Override
            public void modelUpdateFinished(ModelUpdateResult result) {
                entryManager.removeListener(this);
            }
        });

        return n;
    }

    private void updateContentView(EntryManager entryManager, RemoteViews remoteViews) {
        String status = "...";
        Job runningJob = entryManager.getCurrentRunningJob();
        if (runningJob != null) {
            status = runningJob.getJobDescription();
            if (runningJob.isProgressMeassurable()) {
                int[] progress = runningJob.getProgress();
                int currentArticle = progress[0];
                int allArticles = progress[1];
                remoteViews.setProgressBar(R.id.progress_bar, allArticles, currentArticle, false);
                status = runningJob.getJobDescription() + " (" + currentArticle + "/" + allArticles + ")" + ".";
            } else
                remoteViews.setProgressBar(R.id.progress_bar, 0, 0, true);

        } else
            remoteViews.setProgressBar(R.id.progress_bar, 0, 0, true);
        remoteViews.setViewVisibility(R.id.cancel_sync, entryManager.isCancelRequested() ? View.GONE : View.VISIBLE);
        remoteViews.setTextViewText(R.id.status_text, status);
    }

    public void sendSynchronizationProblemNotification(boolean loginExpired) {
        nm.notify(NOTIFICATION_SYNCHRONIZATION_STOPPED_WITH_ERROR, createSynchronizationProblemNotification(null, null,
                loginExpired));
    }

    /** LATER this method is not called at all? */
    public void sendSynchronizationProblemNotification(String captchaToken, String captchaUrl) {
        nm.notify(NOTIFICATION_SYNCHRONIZATION_STOPPED_WITH_ERROR, createSynchronizationProblemNotification(
                captchaToken, captchaUrl, false));
    }

    @Override
    public void finalize() {
        if (displaysNotification)
            PL.log("WTF? Notification wasn't cleared.", context);
    }

    public void notifyNewArticles(EntryManager entryManager, long startDate, int noOfNewArticles) {
        cancelNewArticlesNotification();

        if (noOfNewArticles < 1)
            return;

        SharedPreferences prefs = entryManager.getSharedPreferences();
        if (!prefs.getBoolean("settings_notifications_enabled", true))
            return;

        Intent intent = new Intent(context, ArticleListActivity.class);

        DBQuery dbq = new DBQuery(entryManager, null, null);
        dbq.setStartDate(startDate);
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
        intent.putExtra(UIHelper.EXTRA_KEY_TITLE, "New Articles");
        UIHelper.addExtrasFromDBQuery(intent, dbq);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification n = new Notification(R.drawable.gen_auto_notification_icon, noOfNewArticles + " new "
                + U.pluralize(noOfNewArticles, "article"), System.currentTimeMillis());
        n.setLatestEventInfo(context, "New articles!",

        noOfNewArticles + " new " + U.pluralize(noOfNewArticles, "article") + " in monitored feeds.", pendingIntent);
        n.number = noOfNewArticles;
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        if (prefs.getBoolean("settings_notify_with_led_enabled", true)) {

            n.ledOnMS = 100;
            n.ledOffMS = 3000;
            n.ledARGB = 0xff0000ff;

            n.flags |= Notification.FLAG_SHOW_LIGHTS;
        }

        // if (prefs.getBoolean("settings_notify_with_sound_enabled", false))
        // n.defaults |= Notification.DEFAULT_SOUND;

        if (prefs.getString("settings_notify_with_sound_url", "").length() != 0)
            n.sound = Uri.parse(prefs.getString("settings_notify_with_sound_url", ""));

        if (prefs.getBoolean("settings_notify_with_vibration_enabled", true))
            n.vibrate = new long[] { 0, 100, 1000, 100, 1000, 100 };

        nm.notify(NOTIFICATION_NEW_ARTICLES, n);

    }

    public void cancelNewArticlesNotification() {
        nm.cancel(NOTIFICATION_NEW_ARTICLES);
    }

    private void sendSynchronizationRunningNotification(boolean fastSyncOnly) {

        nm.notify(NOTIFICATION_SYNCHRONIZATION_RUNNING, createSynchronizationRunningNotification(fastSyncOnly));
        displaysNotification = true;
        PL.log("NOTIFICATION: Running: set", context);
    }

    public void modelUpdated(String atomId) {
    }

    public void modelUpdateFinished(ModelUpdateResult result) {
        cancelSyncInProgressNotification();
    }

    public void modelUpdateStarted(boolean fastSyncOnly) {
        // clear old and error notifications
        // cancelAllNotifications();
        cancelSyncProblemNotification();

        // set during notification
        if (EntryManager.getInstance(context).isSyncInProgressNotificationEnabled())
            sendSynchronizationRunningNotification(fastSyncOnly);
    }

    public void modelUpdated() {
    }

    public void statusUpdated() {

    }
}
