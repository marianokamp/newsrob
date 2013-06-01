package com.newsrob.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.newsrob.DBQuery;
import com.newsrob.Feed;
import com.newsrob.threetosix.R;

public class FeedListActivity extends AbstractNewsRobListActivity {

    static final String TAG = FeedListActivity.class.getSimpleName();
    protected static final int MENU_ITEM_UNSUBSCRIBE_FEED_ID = 122;

    DBQuery dbQuery;

    SimpleCursorAdapter sca;
    boolean hideReadItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_list);

        // AdUtil.publishAd(this);

        initialize(getIntent());

    }

    private void initialize(Intent i) {

        dbQuery = UIHelper.createDBQueryFromIntentExtras(getEntryManager(), i);

        Cursor c = getEntryManager().getFeedListContentCursor(dbQuery);
        startManagingCursor(c);

        sca = new SimpleCursorAdapter(this, R.layout.dashboard_list_row, c, new String[] { "_id", "frequency",
                "sum_unread_freq" }, new int[] { R.id.item_title, R.id.item_count, R.id.unread });
        final int readIndicator = getEntryManager().isLightColorSchemeSelected() ? R.drawable.read_indicator
                : R.drawable.read_indicator_dark;

        sca.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 2) {
                    TextView tv = (TextView) view;
                    boolean containsUnread = cursor.getInt(2) > 0;
                    tv.setBackgroundResource(containsUnread ? readIndicator : R.drawable.read_indicator_invisible);
                    return true;
                }

                return false;
            }
        });

        setListAdapter(sca);
        int noOfListRows = getListView().getAdapter().getCount();
        if (noOfListRows < 3 || dbQuery.getFilterFeedId() != null) {
            // all articles or just a single feed?
            getListView().performItemClick(getListView(), noOfListRows - 1, -1l);

            if (!isTaskRoot())
                finish();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        hideSortOrderToggle();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initialize(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isTaskRoot() && getListAdapter().getCount() == 1) {
            Log.d(TAG, "Only 'all articles' found.");
            finish();
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor c = (Cursor) getListAdapter().getItem(position);

        Long filterFeedId = c.getLong(c.getColumnCount() - 1);
        int frequency = c.getInt(1);

        DBQuery dbq = new DBQuery(getDbQuery());
        dbq.setFilterFeedId(filterFeedId);

        if (frequency == 1) {
            startShowEntryActivityForPosition(0, dbq);
        } else {
            // new File(Environment.getExternalStorageDirectory(),
            // "t").mkdirs();
            // Debug.startMethodTracing("t/launch_al_"
            // + new SimpleDateFormat("yyMMdd-hh:mm:ss").format(new
            // Date()).replace(':', '-'));

            Intent intent = new Intent(this, ArticleListActivity.class);
            UIHelper.addExtrasFromDBQuery(intent, dbq);
            startActivity(intent);
        }

    }

    @Override
    void refreshUI() {
        if (!isTaskRoot() && getListAdapter().getCount() == 1)
            finish();
        else
            super.refreshUI();
    }

    private Long getSelectedFeedId(int selectedPosition) {
        Object o = null;
        Long feedId = null;
        try {
            o = sca.getItem(selectedPosition);
            if (o != null) {

                Cursor c = (Cursor) o;
                feedId = c.getLong(c.getColumnCount() - 1);
            }
        } catch (RuntimeException rte) {
            // ignored
        }
        return feedId;
    }

    @Override
    public String getDefaultStatusBarTitle() {
        StringBuilder sb = new StringBuilder();
        DBQuery dbq = getDbQuery();
        if (dbq.getFilterLabel() != null && !"all articles".equals(dbq.getFilterLabel()))
            sb.append("- " + dbq.getFilterLabel());
        if (sb.length() == 0)
            sb.append("- Feeds");
        return getResources().getString(R.string.app_name) + " " + sb.toString();
    }

    @Override
    protected CharSequence getToastMessage() {
        StringBuilder sb = new StringBuilder();
        DBQuery dbq = getDbQuery();
        if (dbq.getFilterLabel() != null && !"all articles".equals(dbq.getFilterLabel()))
            sb.append(dbq.getFilterLabel());
        if (sb.length() == 0)
            sb.append("All Feeds");

        return sb.toString().toLowerCase();

    }

    @Override
    protected Cursor createCursorFromQuery(DBQuery dbq) {
        return getEntryManager().getFeedListContentCursor(dbQuery);
    }

    @Override
    protected DBQuery getDbQuery() {
        return dbQuery;
    }

    @Override
    protected boolean onContextItemSelected(MenuItem item, int selectedPosition) {
        Long feedId = getSelectedFeedId(selectedPosition);

        if (feedId == null || feedId == -1l)
            return false;

        if (item.getItemId() == MENU_ITEM_MANAGE_FEED_ID) {
            startActivity(new Intent(this, ManageFeedActivity.class).putExtra(ManageFeedActivity.EXTRA_FEED_ID, feedId));
        } else if (item.getItemId() == MENU_ITEM_MARK_ALL_READ_ID) {
            DBQuery dbq = getDbQuery();
            instantiateMarkAllReadDialog(dbq.getFilterLabel(), feedId, dbq.getStartDate(), dbq.getDateLimit(), dbq
                    .isSortOrderAscending(), dbq.getLimit());
        }

        if (item.getItemId() == MENU_ITEM_UNSUBSCRIBE_FEED_ID) {
            Feed f = getEntryManager().findFeedById(feedId);

            if (f == null)
                return true;

            final String feedAtomId = f.getAtomId();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    new UnsubscribeFeedTask(getEntryManager()).execute(feedAtomId);
                }
            };
            showConfirmationDialog("Unsubscribe from \'" + f.getTitle()
                    + "\' during the next sync and mark all remaining articles read?", r);
            return true;
        }

        return true;
    }

    private String getSelectedTitle(int selectedPosition) {
        Object o = null;
        String title = null;
        try {
            o = sca.getItem(selectedPosition);
        } catch (RuntimeException rte) {
            // ignored
        }
        if (o != null) {
            Cursor c = (Cursor) o;
            title = c.getString(0);
        }
        return title;
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, int selectedPosition) {

        String title = getSelectedTitle(selectedPosition);
        if (title == null)
            return;

        long feedId = getSelectedFeedId(selectedPosition);

        menu.setHeaderTitle(title);
        menu.add(0, MENU_ITEM_MANAGE_FEED_ID, 0, R.string.menu_item_manage_feed);
        menu.add(0, MENU_ITEM_MARK_ALL_READ_ID, 0, R.string.menu_item_mark_all_read);

        DBQuery dbq = new DBQuery(getDbQuery());
        dbq.setFilterFeedId(feedId);
        if (!getEntryManager().isMarkAllReadPossible(dbq))
            menu.getItem(1).setEnabled(false);

        boolean feedCanBeUnsubscribed = false;
        Feed f = getEntryManager().findFeedById(feedId);
        if (f != null)
            feedCanBeUnsubscribed = !getEntryManager().isModelCurrentlyUpdated()
                    && getEntryManager().canFeedBeUnsubscribed(f.getAtomId());

        menu.add(0, MENU_ITEM_UNSUBSCRIBE_FEED_ID, 10, "Unsubscribe Feed").setEnabled(feedCanBeUnsubscribed);

    }

    public void modelUpdated(String atomId) {

    }

    @Override
    public String getDefaultControlPanelTitle() {
        return "Feeds";
    }

}
