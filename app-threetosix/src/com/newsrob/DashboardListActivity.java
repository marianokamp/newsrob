package com.newsrob;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.newsrob.activities.AbstractNewsRobListActivity;
import com.newsrob.activities.ArticleListActivity;
import com.newsrob.activities.FeedListActivity;
import com.newsrob.activities.LoginActivity;
import com.newsrob.activities.UIHelper;
import com.newsrob.threetosix.R;

public class DashboardListActivity extends AbstractNewsRobListActivity {

    static final String TAG = DashboardListActivity.class.getSimpleName();

    private static final int DIALOG_SHOW_LICENSE = 200;
    private static final int DIALOG_SHOW_USAGE_DATA_COLLECTION = 201;
    private static final int DIALOG_SHOW_REINSTALL_NEWSROB = 202;

    SimpleCursorAdapter sca;
    private DBQuery dbQuery;
    int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_list);

        initialize(getIntent());

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        hideSortOrderToggle();

    }

    private void initialize(Intent i) {
        dbQuery = UIHelper.createDBQueryFromIntentExtras(getEntryManager(), i);

        Cursor c = getEntryManager().getDashboardContentCursor(dbQuery);
        startManagingCursor(c);

        final int readIndicator = getEntryManager().isLightColorSchemeSelected() ? R.drawable.read_indicator
                : R.drawable.read_indicator_dark;
        sca = new SimpleCursorAdapter(this, R.layout.dashboard_list_row, c, new String[] { "_id", "frequency",
                "sum_unread_freq" }, new int[] { R.id.item_title, R.id.item_count, R.id.unread });
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

        if (!getEntryManager().isLicenseAccepted())
            showDialog(DIALOG_SHOW_LICENSE);

        else {

            if (getEntryManager().getDaysInstalled() >= 3
                    && getEntryManager().shouldAskForUsageDataCollectionPermission())
                showDialog(DIALOG_SHOW_USAGE_DATA_COLLECTION);

            // Skip this activity when now labels are displayed
            if (sca.getCount() == 1 && i.getBooleanExtra("skip", true)) {

                Intent intent = new Intent(this, FeedListActivity.class);
                UIHelper.addExtrasFromDBQuery(intent, dbQuery);
                startActivity(intent);
                if (!isTaskRoot())
                    finish();
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initialize(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        Cursor c = (Cursor) getListAdapter().getItem(position);
        String labelName = c.getString(0);
        int frequency = c.getInt(1);

        int ord = c.getInt(3);

        boolean showOnlyNotes = ord == -7 && "notes".equals(labelName);
        Long feedId = null;
        if (showOnlyNotes) {
            feedId = getEntryManager().findNotesFeedId();
            labelName = null;
        }

        DBQuery dbq = new DBQuery(getDbQuery());
        dbq.setFilterLabel(labelName);
        dbq.setFilterFeedId(feedId);

        if (showOnlyNotes) {
            Intent intent = new Intent(this, ArticleListActivity.class);
            UIHelper.addExtrasFromDBQuery(intent, dbq);
            startActivity(intent);
        } else {

            if (frequency == 1) {
                startShowEntryActivityForPosition(0, dbq);
            } else {
                // Intent intent = new Intent(this, EntryListActivity.class);
                Intent intent = new Intent(this, FeedListActivity.class);
                UIHelper.addExtrasFromDBQuery(intent, dbq);
                startActivity(intent);
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (DIALOG_SHOW_LICENSE == id) {
            DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON1) {
                        getEntryManager().acceptLicense();
                        DashboardListActivity.this.startActivity(new Intent().setClass(DashboardListActivity.this,
                                LoginActivity.class));
                    } else
                        finish();
                }
            };

            return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setMessage(
                    R.string.license_text).setTitle(R.string.license).setPositiveButton(android.R.string.ok,
                    negativeListener).setNegativeButton(android.R.string.cancel, negativeListener).create();

        } else if (DIALOG_SHOW_USAGE_DATA_COLLECTION == id) {
            return createUsageDataCollectionPermissionDialog(getEntryManager(), this);
        } else if (DIALOG_SHOW_REINSTALL_NEWSROB == id) {
            return createShowReinstallDialog(getEntryManager(), this);
        }

        return super.onCreateDialog(id);
    }

    public static Dialog createUsageDataCollectionPermissionDialog(final EntryManager entryManager,
            final Activity enclosingActivity) {
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON1) {
                    entryManager.saveUsageDataCollectionPermission(true);
                } else
                    entryManager.saveUsageDataCollectionPermission(false);
                if (enclosingActivity.getClass() != DashboardListActivity.class)
                    enclosingActivity.finish();
            }
        };

        return new AlertDialog.Builder(enclosingActivity).setIcon(android.R.drawable.ic_dialog_info).setMessage(
                enclosingActivity.getResources().getText(R.string.usage_data_collection_text)).setTitle(
                R.string.usage_data_collection).setPositiveButton("Allow", negativeListener).setNegativeButton("Deny",
                negativeListener).create();
    }

    public static Dialog createShowReinstallDialog(final EntryManager entryManager, final Activity enclosingActivity) {
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                entryManager.maintainLastTimeProposedReinstall();
                if (which == DialogInterface.BUTTON1) {

                    final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse("market://details?id=" + EntryManager.LEGACY_PACKAGE_NAME));
                    enclosingActivity.startActivity(viewIntent);
                }

            }
        };

        return new AlertDialog.Builder(enclosingActivity).setIcon(android.R.drawable.ic_dialog_info).setMessage(
                enclosingActivity.getResources().getText(R.string.newsrob_three_to_six_text)).setTitle(
                R.string.newsrob_three_to_six).setPositiveButton("Install", negativeListener).setNegativeButton(
                "Later", negativeListener).create();
    }

    @Override
    public String getDefaultStatusBarTitle() {
        String appName = getResources().getString(R.string.app_name);
        return String.format("%s %s %s", appName, getEntryManager().getMyVersionName(), getLastSyncTime());
    }

    @Override
    protected CharSequence getToastMessage() {
        return getResources().getString(R.string.app_name) + " " + getEntryManager().getMyVersionName()
                + getLastSyncTime();
    }

    private String getLastSyncTime() {
        StringBuilder lastSynced = new StringBuilder();

        long lastSyncTime = getSharedPreferences().getLong(EntryManager.SETTINGS_LAST_SYNC_TIME, 0);
        boolean lastSyncComplete = getSharedPreferences().getBoolean(EntryManager.SETTINGS_LAST_SYNC_COMPLETE, false);

        if (lastSyncTime > 0) {
            lastSynced.append("\nlast sync - ");
            lastSynced.append(getTimeDistance(lastSyncTime));
            lastSynced.append(lastSyncComplete ? "" : " - incomplete");
        }
        return lastSynced.toString();
    }

    public String getDefaultControlPanelTitle() {
        String pro = getEntryManager().isProVersion() ? " Pro" : "";
        return getResources().getString(R.string.app_name) + pro;
    }

    private String getTimeDistance(long t) {

        float diff = (System.currentTimeMillis() - t) / 1000 / 60;
        if (diff < 1)
            return "less than a minute ago";
        else if (diff < 5)
            return "a couple of minutes ago";
        else if (diff < 50)
            return "ca. " + ((int) diff) + " minutes ago";
        else {
            diff /= 60;
            if (diff < 1.2)
                return "ca. an hour ago";
            else if (diff < 2) {
                return "less than two hours ago";
            } else if (diff < 20) {
                return "ca. " + ((int) diff) + " hours ago";
            } else {
                diff /= 24;
                if (diff < 1.2)
                    return "a day ago";
                else if (diff < 4)
                    return ((int) diff) + " days ago";
                else if (diff < 6) {
                    return "too long ago";
                } else
                    return "when dinosaurs were walking the earth";
            }
        }
    }

    @Override
    protected DBQuery getDbQuery() {
        return dbQuery;
    }

    @Override
    protected boolean onContextItemSelected(MenuItem item, int selectedPosition) {
        String label = null;
        int ord = -99;
        try {
            Cursor c = (Cursor) sca.getItem(selectedPosition);
            label = c.getString(0);
            ord = c.getInt(3);
        } catch (CursorIndexOutOfBoundsException cioobe) {
            // label stays null
        }
        if (label == null)
            return false;

        if (item.getItemId() == MENU_ITEM_MARK_ALL_READ_ID) {
            boolean showOnlyNotes = ord == -7 && "notes".equals(label);
            Long feedId = null;
            if (showOnlyNotes) {
                feedId = getEntryManager().findNotesFeedId();
                label = null;
            }
            DBQuery dbq = getDbQuery();
            instantiateMarkAllReadDialog(label, feedId, dbq.getStartDate(), dbq.getDateLimit(), dbq
                    .isSortOrderAscending(), dbq.getLimit());
        }

        // getEntryManager().requestMarkAllAsRead(label, null, 0, handler);
        return true;
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, int selectedPosition) {

        Cursor c = (Cursor) sca.getItem(selectedPosition);
        String label = c.getString(0);
        int ord = c.getInt(3);

        menu.setHeaderTitle(label);
        menu.add(0, MENU_ITEM_MARK_ALL_READ_ID, 0, R.string.menu_item_mark_all_read);

        DBQuery dbq = new DBQuery(getDbQuery());
        dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

        boolean showOnlyNotes = ord == -7 && "notes".equals(label);
        if (showOnlyNotes)
            dbq.setFilterFeedId(getEntryManager().findNotesFeedId());
        else
            dbq.setFilterLabel(label);

        if (!getEntryManager().isMarkAllReadPossible(dbq))
            menu.getItem(0).setEnabled(false);

    }

    public void modelUpdated(String atomId) {
    }

    @Override
    protected Cursor createCursorFromQuery(DBQuery dbq) {
        return getEntryManager().getDashboardContentCursor(dbq);
    }

}
