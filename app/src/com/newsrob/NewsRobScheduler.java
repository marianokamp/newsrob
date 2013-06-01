package com.newsrob;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

class NewsRobScheduler {

    private Context context;
    private EntryManager entryManager;
    SharedPreferences sharedPreferences;

    private static final String TAG = NewsRobScheduler.class.getSimpleName();

    NewsRobScheduler(Context context, EntryManager entryManager) {
        this.context = context;
        this.entryManager = entryManager;
        this.sharedPreferences = entryManager.getSharedPreferences();
    }

    void ensureSchedulingIsEnabled() {

        PL.log("EnsuringSchedulingIsEnabled", context);
        if (!entryManager.isAutoSyncEnabled()) {
            cancelBackgroundSchedule(false);
            cancelBackgroundSchedule(true);
            return;
        }

        PendingIntent pendingIntent = getBackgroundSynchronizationPendingIntent(false, true);

        if (pendingIntent == null) {
            // previously not scheduled

            int intervalMinutes = getScheduleInterval(-1);
            doScheduleRecurringSync(intervalMinutes, false);
            PL.log("Ensure schedule lead to re-creation of schedule.", context);
        }
    }

    private void doScheduleRecurringSync(int intervalMinutes, boolean startNow) {
        PL.log("doScheduleReccuringSync called with startNow=" + startNow + " intervalMinutes=" + intervalMinutes,
                context);
        long nextSyncTime = entryManager.getNextScheduledSyncTime();

        long calculatedNextSyncTime = System.currentTimeMillis() + 120 * 1000;

        if (!startNow)
            calculatedNextSyncTime += intervalMinutes * 60 * 1000;

        // next sync would be in the past?
        if (nextSyncTime < System.currentTimeMillis())
            nextSyncTime = calculatedNextSyncTime;

        if (startNow)
            nextSyncTime = calculatedNextSyncTime;

        final long intervalMs = ((long) intervalMinutes) * 60l * 1000l;

        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, nextSyncTime, intervalMs,
                getBackgroundSynchronizationPendingIntent(false, false));
        PL.log("doScheduleReccuringSync called with nextSyncTime=" + new Date(nextSyncTime) + " time=" + nextSyncTime
                + "(" + new Date(nextSyncTime) + ") interval in min=" + (intervalMs / 1000.0 / 60.0)
                + " intervalMinutes=" + intervalMinutes, context);

        updateNextSyncTime(nextSyncTime);
    }

    private void adjustBackgroundSchedule() {
        adjustBackgroundSchedule(-1, true);
    }

    public void resetBackgroundSchedule() {
        if (!entryManager.isAutoSyncEnabled())
            return;
        entryManager.updateNextScheduledSyncTime(-1l);
        scheduleSynchronization(getScheduleInterval(-1), false, false);
    }

    void adjustBackgroundSchedule(int minutes, boolean startNow) {
        if (entryManager.isAutoSyncEnabled())
            scheduleSynchronization(getScheduleInterval(minutes), false, startNow);
        else
            cancelBackgroundSchedule(false);

    }

    void scheduleUploadOnlySynchonization() {
        if (entryManager.isAutoSyncEnabled())
            new Thread(new Runnable() {
                public void run() {
                    scheduleSynchronization(3, true, false);
                }
            }).start();

    }

    private int getScheduleInterval(int minutes) {
        int intervalMinutes;
        if (minutes > 0)
            intervalMinutes = minutes;
        else {
            intervalMinutes = new Integer(sharedPreferences.getString(EntryManager.SETTINGS_AUTOMATIC_REFRESH_INTERVAL,
                    "20"));
            if (intervalMinutes < 5)
                intervalMinutes = 20;
        }
        return intervalMinutes;
    }

    private void cancelBackgroundSchedule(boolean uploadOnly) {
        getAlarmManager().cancel(getBackgroundSynchronizationPendingIntent(uploadOnly, false));
        Log.d(TAG, "Synchronization schedule cancelled. Upload Only=" + uploadOnly);
    }

    private void scheduleSynchronization(int synchIntervalMinutes, boolean uploadOnly, boolean startNow) {
        if (uploadOnly) {
            getAlarmManager().set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 60 * 1000 * synchIntervalMinutes,
                    getBackgroundSynchronizationPendingIntent(uploadOnly, false));
        } else
            doScheduleRecurringSync(synchIntervalMinutes, startNow);

        // Log.d(TAG, "Called with synchIntervalMinutes " + synchIntervalMinutes
        // + " currently "
        // + new Date(System.currentTimeMillis()) + " new Time " + new
        // Date(scheduledTime)
        // + " UploadOnly=" + uploadOnly);

    }

    public void updateNextSyncTime(long nextSyncTime) {
        long t = nextSyncTime == -1 ? System.currentTimeMillis() + getScheduleInterval(-1) * 1000 * 60 : nextSyncTime;
        entryManager.updateNextScheduledSyncTime(t);

    }

    private PendingIntent getBackgroundSynchronizationPendingIntent(boolean uploadOnly, boolean noCreate) {
        Intent i = new Intent();
        i.setClass(context, WakeupAndSynchronizeReceiver.class);
        i.setAction(uploadOnly ? SynchronizationService.ACTION_SYNC_UPLOAD_ONLY : "Full");
        if (noCreate)
            i.setFlags(PendingIntent.FLAG_NO_CREATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, noCreate ? PendingIntent.FLAG_NO_CREATE : 0);
        return pi;
    }

    private AlarmManager getAlarmManager() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager;
    }

    void setNeedsSynchronizationNotification() {
        entryManager.getNewsRobNotificationManager().sendSynchronizationProblemNotification(false);
        Log.d(TAG, "Notification for requesting login sent.");
    }

}
