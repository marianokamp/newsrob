package com.newsrob.activities;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.newsrob.DB;
import com.newsrob.DBQuery;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Feed;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.NewsRob;
import com.newsrob.R;
import com.newsrob.ReadState;
import com.newsrob.util.Timing;
import com.newsrob.util.U;
import com.newsrob.widget.SwipeRelativeLayout;

public class ArticleListActivity extends AbstractNewsRobListActivity implements IEntryModelUpdateListener,
        View.OnClickListener {

    private static final String TAG = ArticleListActivity.class.getSimpleName();

    protected static final int MENU_ITEM_MARK_READ_UNTIL_HERE_ID = 400;
    protected static final int MENU_ITEM_UNSUBSCRIBE_FEED_ID = 401;

    private DBQuery dbQuery;

    private MyListAdapter listAdapter;

    private UIHelper uiHelper;

    private String title;

    private String feedName;

    private Float minimumFlingTravel;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard_list);

        // AdUtil.publishAd(this);

        uiHelper = new UIHelper(getEntryManager());

        initialize(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initialize(intent);
    }

    private void initialize(Intent i) {
        if (i.hasExtra("FEED_URL")) {
            String feedUrl = i.getStringExtra("FEED_URL");
            long feedId = getEntryManager().findFeedIdByFeedUrl(feedUrl);
            if (feedId == -1) {
                setResult(-1);
                finish();
                return;
            } else
                i.putExtra(UIHelper.EXTRA_KEY_FILTER_FEED, feedId);
        }

        dbQuery = UIHelper.createDBQueryFromIntentExtras(getEntryManager(), i);
        dbQuery.setLimit(250);

        if (dbQuery.getFilterFeedId() != null && dbQuery.getFilterFeedId() > -1l) {
            final Feed f = getEntryManager().findFeedById(dbQuery.getFilterFeedId());
            if (f != null) {
                feedName = f.getTitle();
            } else {
                finish();
                return;
            }
        }

        final Bundle bundle = i.getExtras();
        if (bundle != null && bundle.containsKey(UIHelper.EXTRA_KEY_TITLE))
            title = bundle.getString(UIHelper.EXTRA_KEY_TITLE);

        // ---

        listAdapter = new MyListAdapter();
        setListAdapter(listAdapter);

        updateCursor();

    }

    private void updateCursor() {

        if (dbQuery.hasChanged()) {
            Timing t = null;
            if (NewsRob.isDebuggingEnabled(this))
                t = new Timing("ArticleListActivity.updateCursor() change of cursor", this);
            final Cursor oldCursor = listAdapter.getCursor();
            if (oldCursor != null) {
                stopManagingCursor(oldCursor);
                listAdapter.notifyDataSetInvalidated();
                oldCursor.close();
            }

            final Cursor cursor = getEntryManager().getContentCursor(dbQuery);
            startManagingCursor(cursor);
            listAdapter.changeCursor(cursor);
            if (t != null)
                t.stop();
            // listAdapter.notifyDataSetChanged();

        } else {
            Timing t = null;
            if (NewsRob.isDebuggingEnabled(this))
                t = new Timing("ArticleListActivity.updateCursor() requery", this);
            // listAdapter.notifyDataSetChanged(); doesn't work. use requery
            // instead
            listAdapter.getCursor().requery();

            if (t != null)
                t.stop();
        }
    }

    @Override
    protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
        onClick(v);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item, final int position) {
        Entry entry = findEntryByPosition(position);

        if (entry == null)
            return false;

        if (position > -1 && item.getItemId() == MENU_ITEM_MARK_READ_UNTIL_HERE_ID) {
            DBQuery dbq = new DBQuery(getDbQuery());
            dbq.setDateLimit(entry.getUpdatedInHighResolution());
            instantiateMarkAllReadDialog(dbq);
            return true;
        }
        if (position > -1 && item.getItemId() == MENU_ITEM_UNSUBSCRIBE_FEED_ID) {
            Feed f = getEntryManager().findFeedById(entry.getFeedId());

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

        if (ArticleViewHelper.articleActionSelected(this, item, getEntryManager(), entry))
            return true;
        return false;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo,
            final int position) {
        final Entry entry = findEntryByPosition(position);
        if (entry != null) {
            ArticleViewHelper.createArticleMenu(menu, this, entry);
            menu.add(0, MENU_ITEM_MARK_READ_UNTIL_HERE_ID, 3, "Mark Read Until Here");

            boolean feedCanBeUnsubscribed = false;
            Feed f = getEntryManager().findFeedById(entry.getFeedId());
            if (f != null)
                feedCanBeUnsubscribed = !getEntryManager().isModelCurrentlyUpdated()
                        && getEntryManager().canFeedBeUnsubscribed(f.getAtomId());

            menu.add(0, MENU_ITEM_UNSUBSCRIBE_FEED_ID, 10, "Unsubscribe Feed").setEnabled(feedCanBeUnsubscribed);
        }
    }

    private Entry findEntryByPosition(final int position) {
        final Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
        String atomId = null;

        try {
            atomId = cursor.getString(1);
        } catch (final CursorIndexOutOfBoundsException cioobe) {
            // atomId stays null
        }
        if (atomId == null)
            return null;
        final Entry selectedEntry = getEntryManager().findEntryByAtomId(atomId);
        return selectedEntry;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (!getEntryManager().isSwipeOnArticleListEnabled())
            return super.dispatchTouchEvent(event);

        switch (event.getAction()) {
        case MotionEvent.ACTION_CANCEL:
            inDown = false;
            startX = 0f;
            startY = 0f;
            return super.dispatchTouchEvent(event);

        case MotionEvent.ACTION_MOVE:
            return super.dispatchTouchEvent(event) || inDown;

        case MotionEvent.ACTION_UP:
            boolean consumed = doFling(startX, startY, event.getX(), event.getY());

            inDown = false;

            startX = 0f;
            startY = 0f;

            if (consumed) {
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancelEvent);
                return true;
            } else {
                boolean result = super.dispatchTouchEvent(event);
                return result;
            }
        case MotionEvent.ACTION_DOWN:
            inDown = true;
            startX = event.getX();
            startY = event.getY();

            super.dispatchTouchEvent(event);
            return true;

        }
        boolean consumed = super.dispatchTouchEvent(event);
        return consumed;
    }

    private boolean doFling(float x1, float y1, float x2, float y2) {

        final float xDiff = x1 - x2;
        final float yDiff = y1 - y2;

        final float xTravel = abs(xDiff);
        final float yTravel = abs(yDiff);

        final float maxFingerTravel = max(xTravel, yTravel);

        if (minimumFlingTravel == null)
            minimumFlingTravel = 18 * U.getDensity(this);

        if (maxFingerTravel < minimumFlingTravel)
            return false;

        final float longerFingerTravel = maxFingerTravel;
        final float shorterFingerTravel = min(xTravel, yTravel);

        final float longerFingerTravelDominance = longerFingerTravel / shorterFingerTravel;

        if (longerFingerTravelDominance < 1.3f)
            return false;

        boolean consumed = false;

        if (xTravel > yTravel) {

            int[] location = new int[2];
            getListView().getLocationOnScreen(location);
            int localX = (int) (x1 - location[0]);
            int localY = (int) (y1 - location[1]);
            int position = getListView().pointToPosition(localX, localY);
            if (position == -1)
                return true;

            final Entry entry = findEntryByPosition(position);

            if (entry == null)
                return true;

            consumed = true;

            if (xDiff < 0) {
                // LTR
                if (entry == null)
                    return consumed;

                getEntryManager().increaseReadLevel(entry);
            } else {
                // RTL
                if (entry == null)
                    return consumed;

                getEntryManager().increaseUnreadLevel(entry);
            }
        } else {
            if (yDiff < 0) {
                // TTB
            }

            else {
                // BTT
            }
        }
        return consumed;
    }

    private boolean inDown = false;
    private float startX;
    private float startY;

    public void onClick(final View v) {

        int position = -1;

        // TODO Checkout NPE in
        // android.widget.AdapterView.getPositionForView(AdapterView.java:581)
        // // Android 2.2
        try {
            ListView lv = getListView();
            position = lv.getPositionForView(v);
        } catch (final NullPointerException npe) {
            Log.e(getClass().getSimpleName(), "onClick", npe);
            return;
        }
        if (position < 0)
            return;

        startShowEntryActivityForPosition(position, dbQuery);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getListAdapter().isEmpty() && !isTaskRoot()) {
            Log.d(TAG, "No articles found.");
            finish();
        }
        // listAdapter.getCursor().requery();
        // refreshProgressBar();
        // Debug.stopMethodTracing();
    }

    @Override
    public String getDefaultStatusBarTitle() {

        int c = getListAdapter().getCount();
        String count = (c == dbQuery.getLimit() ? c + "+" : c + "");

        if (title != null)
            return title + " (" + count + ")";
        String labelAndFeed = "";
        if (dbQuery.getFilterLabel() != null && !"all articles".equals(dbQuery.getFilterLabel()))
            labelAndFeed = "- " + dbQuery.getFilterLabel() + " ";
        if (feedName != null)
            labelAndFeed += "- " + feedName;
        return String.format("%s %s (%s)", getResources().getString(R.string.app_name), labelAndFeed, count);
    }

    @Override
    protected CharSequence getToastMessage() {
        int c = getListAdapter().getCount();
        String count = (c == dbQuery.getLimit() ? c + "+" : c + "");

        if (title != null)
            return title + " (" + count + ")";

        String labelAndFeed = "";
        if (feedName != null)
            labelAndFeed += feedName;

        if (labelAndFeed.length() == 0)
            labelAndFeed = "All Articles";
        return String.format("%s (%s)", labelAndFeed, count).toLowerCase();

    }

    @Override
    void refreshUI() {
        super.refreshUI();
        if (getListAdapter().getCount() == 0 && !isTaskRoot())
            finish();
    }

    @Override
    protected Cursor createCursorFromQuery(DBQuery dbq) {
        return getEntryManager().getContentCursor(dbQuery);
    }

    public void modelUpdated(final String atomId) {
        modelUpdated();
    }

    private class MyListAdapter extends ResourceCursorAdapter {

        DBColumnIndices columnIndices;
        private int readIndicatorBackground;
        private int pinnedIndicatorBackground;

        public MyListAdapter() {
            super(ArticleListActivity.this, R.layout.rich_article_row, null);
            readIndicatorBackground = getEntryManager().isLightColorSchemeSelected() ? R.drawable.read_indicator
                    : R.drawable.read_indicator_dark;
            pinnedIndicatorBackground = getEntryManager().isLightColorSchemeSelected() ? R.drawable.pinned_indicator
                    : R.drawable.pinned_indicator_dark;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            bindView(view, context, cursor, getEntryManager());
        }

        void bindView(final View view, final Context context, final Cursor cursor, final EntryManager entryManager) {

            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder(view, entryManager);
                view.setTag(holder);
            }

            if (columnIndices == null)
                columnIndices = new DBColumnIndices(cursor);

            // data
            // final long id = cursor.getLong(0);

            final String articleTitle = DB.getStringValueFromCursor(cursor, columnIndices.articleTitleIndex);
            final String feedTitle = DB.getStringValueFromCursor(cursor, columnIndices.feedTitleIndex);

            final ReadState readState = ReadState
                    .fromInt(DB.getIntegerFromCursor(cursor, columnIndices.readStateIndex));
            final boolean isStarred = DB.getBooleanValueFromCursor(cursor, columnIndices.starredStateIndex);

            final int downloaded = DB.getIntegerFromCursor(cursor, columnIndices.downloadedIndex);
            final int downloadPref = DB.getIntegerFromCursor(cursor, columnIndices.downloadPrefIndex);

            // widgets

            holder.feedTitleTextView.setText(feedTitle);
            holder.feedTitleTextView.setCompoundDrawablePadding(3);
            holder.feedTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(
                            uiHelper.getArticleDownloadIndicatorDrawable(downloaded, downloadPref, getResources())),
                    null, null, null);

            holder.articleTitleTextView.setText(articleTitle);

            if (NewsRob.SHOW_CHANGED) {
                final boolean isStatePending = DB
                        .getBooleanValueFromCursor(cursor, columnIndices.readStatePendingIndex)

                        || DB.getBooleanValueFromCursor(cursor, columnIndices.starredStatePendingIndex)
                        || DB.getBooleanValueFromCursor(cursor, columnIndices.pinnedStatePendingIndex);

                holder.articleChangedTextView.setVisibility(isStatePending ? View.VISIBLE : View.GONE);
            }

            holder.articleStarredTextView.setVisibility(isStarred ? View.VISIBLE : View.GONE);

            int bgReadIndicator = -1;
            switch (readState) {
            case READ:
                bgReadIndicator = R.drawable.read_indicator_invisible;
                break;
            case UNREAD:
                bgReadIndicator = readIndicatorBackground;
                break;
            default:
                bgReadIndicator = pinnedIndicatorBackground;
            }

            holder.readIndicatorImageView.setBackgroundResource(bgReadIndicator);

            // previews

            if (getEntryManager().isRichArticleListEnabled()) {
                if (holder.thumbnailImageView != null) {

                    // snippet
                    TextView snippetView = (TextView) view.findViewById(R.id.entry_snippet);
                    snippetView.setVisibility(View.GONE);
                    String snippet = cursor.getString(columnIndices.snippetIndex);

                    if (snippet != null && snippet.length() > 0) {
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                                && U.getScreenSize(context) == 1)
                            snippetView.setMaxLines(4);
                        else
                            snippetView.setMaxLines(3);
                        snippetView.setVisibility(View.VISIBLE);
                        snippetView.setText(snippet);
                    } else {
                        snippetView.setText("");
                    }

                    // image
                    holder.thumbnailImageView.setVisibility(View.GONE);
                    String atomId = cursor.getString(columnIndices.atomIdIndex);
                    File f = Entry.getThumbnailFile(entryManager, Entry.getShortAtomId(atomId));
                    if (f == null)
                        holder.thumbnailImageView.setImageURI(null);
                    else {
                        Uri uri = new Uri.Builder().path(f.getAbsolutePath()).build();
                        if (uri != null) {
                            holder.thumbnailImageView.setVisibility(View.VISIBLE);

                            holder.thumbnailImageView.setImageURI(uri);
                        } else
                            holder.thumbnailImageView.setImageURI(null);
                    }
                }
            }

        }
    }

    static class DBColumnIndices {
        int articleTitleIndex;
        int feedTitleIndex;
        int readStateIndex;
        int starredStateIndex;
        int downloadedIndex;
        int downloadPrefIndex;
        int readStatePendingIndex;
        int starredStatePendingIndex;
        int atomIdIndex;
        int snippetIndex;
        int pinnedStatePendingIndex;

        DBColumnIndices(final Cursor c) {

            articleTitleIndex = c.getColumnIndex(DB.Entries.TITLE);
            feedTitleIndex = c.getColumnIndex("derived_feed_title");
            readStateIndex = c.getColumnIndex(DB.Entries.READ_STATE);
            starredStateIndex = c.getColumnIndex(DB.Entries.STARRED_STATE);
            downloadedIndex = c.getColumnIndex(DB.Entries.DOWNLOADED);
            downloadPrefIndex = c.getColumnIndex("DOWNLOAD_PREF");
            readStatePendingIndex = c.getColumnIndex(DB.Entries.READ_STATE_PENDING);
            starredStatePendingIndex = c.getColumnIndex(DB.Entries.STARRED_STATE_PENDING);
            atomIdIndex = c.getColumnIndex(DB.Entries.ATOM_ID);
            snippetIndex = c.getColumnIndex(DB.Entries.SNIPPET);
            pinnedStatePendingIndex = c.getColumnIndex(DB.Entries.PINNED_STATE_PENDING);
        }

    }

    static class ViewHolder {

        TextView articleStarredTextView;
        TextView feedTitleTextView;
        TextView articleTitleTextView;
        TextView articleSharedTextView;
        TextView articleChangedTextView;
        TextView readIndicatorImageView;
        TextView sharedByFriendTextView;
        TextView articleAnnotatedTextView;

        TextView articleLikedTextView;

        ImageView thumbnailImageView;

        SwipeRelativeLayout swipeContainer;

        ViewHolder(final View parent, final EntryManager entryManager) {
            articleStarredTextView = (TextView) parent.findViewById(R.id.article_starred);
            feedTitleTextView = (TextView) parent.findViewById(R.id.feed_title);

            articleTitleTextView = (TextView) parent.findViewById(R.id.entry_title);
            if (entryManager.shouldTitlesBeEllipsized()) {
                articleTitleTextView.setEllipsize(TruncateAt.MIDDLE);
                articleTitleTextView.setLines(2);
            }

            articleSharedTextView = (TextView) parent.findViewById(R.id.article_shared);
            articleChangedTextView = (TextView) parent.findViewById(R.id.article_changed);
            articleAnnotatedTextView = (TextView) parent.findViewById(R.id.article_annotated);
            articleLikedTextView = (TextView) parent.findViewById(R.id.article_liked);
            readIndicatorImageView = (TextView) parent.findViewById(R.id.read_status_indicator);
            swipeContainer = (SwipeRelativeLayout) parent.findViewById(R.id.swipe_container);
            swipeContainer.setSwipeEnabeld(false);
            sharedByFriendTextView = (TextView) parent.findViewById(R.id.shared_by_friend);
            thumbnailImageView = (ImageView) parent.findViewById(R.id.thumbnail);

        }

    }

    /** public because of unit test */
    @Override
    public DBQuery getDbQuery() {
        return dbQuery;
    }

    @Override
    public String getDefaultControlPanelTitle() {
        return "Articles";
    }

}

class UnsubscribeFeedTask extends AsyncTask<String, Void, Void> {
    private EntryManager entryManager;

    public UnsubscribeFeedTask(EntryManager entryManager) {
        this.entryManager = entryManager;
    }

    @Override
    protected Void doInBackground(String... params) {
        entryManager.doUnsubscribeFeed(params[0]);
        return null;
    }
}
