package com.newsrob.appwidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.newsrob.DBQuery;
import com.newsrob.EntryManager;
import com.newsrob.PL;
import com.newsrob.threetosix.R;
import com.newsrob.activities.ArticleListActivity;
import com.newsrob.activities.UIHelper;
import com.newsrob.util.Timing;

public class UnreadWidgetProvider extends AppWidgetProvider {
    static final String TAG = UnreadWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, UpdateService.class));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        EntryManager entryManager = EntryManager.getInstance(context);
        for (int appWidgetId : appWidgetIds)
            entryManager.clearWidgetPreferences(appWidgetId);
    }

    public static RemoteViews buildUpdate(Context context, int appWidgetId) {

        RemoteViews updateViews = null;
        Timing t = new Timing("buildUpdate for widget " + appWidgetId, context);
        try {
            EntryManager entryManager = EntryManager.getInstance(context);
            WidgetPreferences wp = entryManager.getWidgetPreferences(appWidgetId);
            updateViews = new RemoteViews(context.getPackageName(), R.layout.unread_widget);

            if (wp == null) {
                PL.log(TAG + "Nothing stored for appWidgetId=" + appWidgetId, context);
                updateViews.setTextViewText(R.id.unread_count, "oops!");
                updateViews.setViewVisibility(R.id.unread_count, View.VISIBLE);
                return updateViews;
            }

            int count = getCount(entryManager, wp.getDBQuery());

            updateViews.setViewVisibility(R.id.unread_count, count > 0 ? View.VISIBLE : View.INVISIBLE);
            updateViews.setTextViewText(R.id.unread_count, String.valueOf(count));

            updateViews.setViewVisibility(R.id.label,
                    wp.getLabel() != null && wp.getLabel().trim().length() > 0 ? View.VISIBLE : View.INVISIBLE);

            updateViews.setTextViewText(R.id.label, wp.getLabel() == null ? "" : wp.getLabel());

            Class startingActivityClass = ArticleListActivity.class;
            if (wp.getStartingActivityName() != null) {
                try {
                    startingActivityClass = Class.forName(wp.getStartingActivityName());
                } catch (ClassNotFoundException e) {
                    throw e;
                }
            }

            Intent intent = new Intent(context, startingActivityClass);
            // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
            // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // intent.addCategory(Intent.CATEGORY_LAUNCHER);
            // intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
            // Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
            // | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            UIHelper.addExtrasFromDBQuery(intent, wp.getDBQuery());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0); // 
            // | Intent.FLAG_DEBUG_LOG_RESOLUTION

            updateViews.setOnClickPendingIntent(R.id.container, pendingIntent);
        } catch (Throwable throwable) {
            PL.log("Exception during buildUpdate()", throwable, context);
        } finally {
            t.stop();
            return updateViews;
        }
    }

    private static int getCount(EntryManager entryManager, DBQuery dbq) {
        return entryManager.getContentCount(dbq);
    }

    public static void requestWidgetsUpdate(Context ctx) {
        AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
        int[] appWidgetIds = awm.getAppWidgetIds(new ComponentName(ctx, UnreadWidgetProvider.class));
        Timing t = new Timing(TAG + " requestWidgetsUpdate for " + appWidgetIds.length + " widget(s)", ctx);
        for (int appWidgetId : appWidgetIds) {
            awm.updateAppWidget(appWidgetId, UnreadWidgetProvider.buildUpdate(ctx, appWidgetId));
        }
        t.stop();
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
            requestWidgetsUpdate(this);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

}
