package com.newsrob.appwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.newsrob.DBQuery;
import com.newsrob.DashboardListActivity;
import com.newsrob.EntryManager;
import com.newsrob.PL;
import com.newsrob.R;
import com.newsrob.activities.AbstractNewsRobListActivity;
import com.newsrob.activities.ArticleListActivity;
import com.newsrob.activities.FeedListActivity;
import com.newsrob.appwidget.UnreadWidgetPrefWizard.Scope;
import com.newsrob.appwidget.UnreadWidgetPrefWizard.StartingActivity;

public class UnreadWidgetPrefActivity extends Activity {

    private int appWidgetId;

    private UnreadWidgetPrefWizard wizard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_unread_widget);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        setResult(RESULT_CANCELED);

        EntryManager entryManager = EntryManager.getInstance(this);
        if (!entryManager.isProVersion()) {
            Toast.makeText(this, "Simple Launcher Widget created.\nWidget configurable in the Pro version.",
                    Toast.LENGTH_LONG).show();
            // finish();
            WidgetPreferences widgetPrefs = new WidgetPreferences();
            widgetPrefs.setLabel("NewsRob");
            widgetPrefs.setStartingActivityName(DashboardListActivity.class.getName());
            doCreateWidget(widgetPrefs, entryManager, null, null);
        }

        wizard = new UnreadWidgetPrefWizard(this) {

            @Override
            void wizardFinished() {
                createWidget();
            }
        };

    }

    private void createWidget() {
        WidgetPreferences widgetPrefs = new WidgetPreferences();
        widgetPrefs.setLabel(wizard.getWidgetLabel());

        EntryManager entryManager = EntryManager.getInstance(this);

        String filterLabel = null;
        if (wizard.getScope() == Scope.LABEL && wizard.getSelectedLabelName().length() > 0) {
            filterLabel = wizard.getSelectedLabelName();
            if (filterLabel.length() == 0)
                filterLabel = null;
        }
        Long filterFeedId = null;
        if (wizard.getScope() == Scope.FEED && wizard.getSelectedFeedName().length() > 0)
            filterFeedId = wizard.getSelectedFeedId();

        if (wizard.getScope() == Scope.READING_LIST) {
            Class<? extends AbstractNewsRobListActivity> startingActivity = ArticleListActivity.class;
            if (wizard.getStartingActivity() == StartingActivity.DASHBOARD)
                startingActivity = DashboardListActivity.class;
            else if (wizard.getStartingActivity() == StartingActivity.FEEDS)
                startingActivity = FeedListActivity.class;
            widgetPrefs.setStartingActivityName(startingActivity.getName());
        }

        doCreateWidget(widgetPrefs, entryManager, filterLabel, filterFeedId);

    }

    private void doCreateWidget(WidgetPreferences widgetPrefs, EntryManager entryManager, String filterLabel,
            Long filterFeedId) {
        DBQuery dbq = new DBQuery(entryManager, filterLabel, filterFeedId);
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

        widgetPrefs.setDBQuery(dbq);

        entryManager.saveWidgetPreferences(appWidgetId, widgetPrefs);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();

        PL.log("doCreateWidget, about to call updateAppWidget", entryManager.getContext());
        RemoteViews remoteViews = UnreadWidgetProvider.buildUpdate(this, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        widgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
