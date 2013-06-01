package com.newsrob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import com.newsrob.EntriesRetriever.StateChange;
import com.newsrob.search.SearchProvider;
import com.newsrob.threetosix.R;
import com.newsrob.util.SQLiteOpenHelper;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public class DB extends SQLiteOpenHelper {

    private static final String TAG = DB.class.getName();

    private static final String ENTRIES_VIEW = "entries_view";
    private static final String DATABASE_NAME = "newsrob.db";
    private static final int DATABASE_VERSION = 28;
    private static final String CREATE_TABLE_TEMP_IDS_SQL = "CREATE TABLE temp_ids (atom_id TEXT PRIMARY KEY);";
    private static final String CLEAR_TEMP_TABLE_SQL = "DELETE FROM temp_ids;";

    private static class Feeds {

        private static final String TABLE_NAME = "feeds";
        private static final String[] FIELD_NAMES;
        private static final String __ID = "_id";
        private static final String ATOM_ID = "ATOM_ID";
        private static final String TITLE = "TITLE";
        private static final String ALTERNATE_URL = "ALT_URL";
        private static final String DOWNLOAD_PREF = "DOWNLOAD_PREF";
        private static final String DISPLAY_PREF = "DISPLAY_PREF";
        private static final String NOTIFICATION_ENABLED = "NOTIFICATION";
        private static final String WEB_SCALE = "WEB_SCALE";
        private static final String FEED_SCALE = "FEED_SCALE";
        private static final String JAVASCRIPT_ENABLED = "JS_ENABLED";
        private static final String FIT_TO_WIDTH_ENABLED = "FTW_ENABLED";

        private static final String[][] FIELDS = { { __ID, "INTEGER PRIMARY KEY" }, { ATOM_ID, "TEXT" },
                { TITLE, "TEXT" }, { DOWNLOAD_PREF, "INTEGER" }, { DISPLAY_PREF, "INTEGER" },
                { NOTIFICATION_ENABLED, "INTEGER" }, { WEB_SCALE, "REAL" }, { FEED_SCALE, "REAL" },
                { JAVASCRIPT_ENABLED, "INTEGER" }, { FIT_TO_WIDTH_ENABLED, "INTEGER" }, { ALTERNATE_URL, "TEXT" } };

        static {
            FIELD_NAMES = new String[Feeds.FIELDS.length];
            for (int i = 0; i < Feeds.FIELD_NAMES.length; i++) {
                Feeds.FIELD_NAMES[i] = Feeds.FIELDS[i][0];
            }
        }
    }

    private static class UnsubscribeFeeds {
        private static final String TABLE_NAME = "unsubscribe_feeds";
        private static final String[] FIELD_NAMES;
        private static final String __ID = "_id";

        private static final String FEED_ATOM_ID = "FEED_ATOM_ID";

        private static final String[][] FIELDS = { { __ID, "INTEGER PRIMARY KEY" }, { FEED_ATOM_ID, "TEXT" } };

        static {
            FIELD_NAMES = new String[UnsubscribeFeeds.FIELDS.length];
            for (int i = 0; i < UnsubscribeFeeds.FIELD_NAMES.length; i++) {
                UnsubscribeFeeds.FIELD_NAMES[i] = UnsubscribeFeeds.FIELDS[i][0];
            }
        }

    }

    static class EntryLabelAssociations {

        static final String TABLE_NAME = "entry_label_associations";
        private static final String[] FIELD_NAMES;
        private static final String __ID = "_id";
        private static final String LABEL_ID = "LABEL_ID";
        static final String ENTRY_ID = "ENTRY_ID";
        private static final String[][] FIELDS = { { __ID, "INTEGER PRIMARY KEY" }, { ENTRY_ID, "INTEGER" },
                { LABEL_ID, "INTEGER" } };

        static {
            FIELD_NAMES = new String[EntryLabelAssociations.FIELDS.length];
            for (int i = 0; i < EntryLabelAssociations.FIELD_NAMES.length; i++) {
                EntryLabelAssociations.FIELD_NAMES[i] = EntryLabelAssociations.FIELDS[i][0];
            }
        }
    }

    private static class Labels {

        private static final String TABLE_NAME = "labels";
        private static final String[] FIELD_NAMES;
        private static final String __ID = "_id";
        private static final String NAME = "NAME";
        private static final String ORD = "ORD"; // LATER REMOVE ME
        private static final String[][] FIELDS = { { __ID, "INTEGER PRIMARY KEY" }, { NAME, "TEXT" },
                { ORD, "INTEGER" } };

        static {
            FIELD_NAMES = new String[Labels.FIELDS.length];
            for (int i = 0; i < Labels.FIELD_NAMES.length; i++) {
                Labels.FIELD_NAMES[i] = Labels.FIELDS[i][0];
            }
        }

    }

    public static class Entries {

        private static final String[] FIELD_NAMES;

        static final String TABLE_NAME = "entries";
        static final String __ID = "_id";

        private static final String ALTERNATE_URL = "ALTERNATE_URL";
        public static final String ATOM_ID = "ATOM_ID";
        public static final String CONTENT = "CONTENT";
        public static final String SNIPPET = "SNIPPET";
        private static final String CONTENT_TYPE = "CONTENT_TYPE";
        private static final String CONTENT_URL = "CONTENT_URL";
        public static final String TITLE = "TITLE";
        private static final String TITLE_TYPE = "TITLE_TYPE"; // Remove me!
        static final String FEED_TITLE = "FEED_TITLE";
        private static final String FEED_TITLE_TYPE = "FEED_TITLE_TYPE"; // R
        // me!

        private static final String FEED_ID = "FEED_ID";
        public static final String READ_STATE = "READ_STATE";
        public static final String READ_STATE_PENDING = "READ_STATE_PENDING";

        public static final String STARRED_STATE = "STARRED_STATE";
        public static final String STARRED_STATE_PENDING = "STARRED_STATE_PENDING";

        public static final String LIKED_STATE = "LIKED_STATE";
        public static final String LIKED_STATE_PENDING = "LIKED_STATE_PENDING";

        public static final String SHARED_STATE = "SHARED_STATE";
        public static final String SHARED_STATE_PENDING = "SHARED_STATE_PENDING";

        public static final String FRIENDS_SHARED_STATE = "FRIENDS_SHARED_STATE";
        public static final String SHARED_BY_FRIEND = "SHARED_BY_FRIEND";

        private static final String UPDATED_UTC = "UPDATED_UTC";
        private static final String INSERTED_AT = "INSERTED_AT";

        public static final String DOWNLOADED = "DOWNLOADED";
        public static final String AUTHOR = "AUTHOR";

        static final String ERROR = "ERROR";

        static final String TYPE = "TYPE";
        public static final String NOTE_SUBMITTED_STATE = "NOTE_SUBMITTED_STATE";
        public static final String NOTE = "NOTE";
        static final String NOTE_SHOULD_BE_SHARED = "NOTE_SHOULD_BE_SHARED";

        private static final String[][] FIELDS = { { __ID, "INTEGER PRIMARY KEY" }, { ATOM_ID, "TEXT" },
                { ALTERNATE_URL, "TEXT" }, { CONTENT, "TEXT" }, { CONTENT_TYPE, "TEXT" }, { CONTENT_URL, "TEXT" },
                { TITLE, "TEXT" }, { TITLE_TYPE, "TEXT" }, { FEED_TITLE, "TEXT" }, { FEED_TITLE_TYPE, "TEXT" },
                { FEED_ID, "INTEGER" }, { READ_STATE, "INTEGER" }, { READ_STATE_PENDING, "INTEGER" },
                { STARRED_STATE, "INTEGER" }, { STARRED_STATE_PENDING, "INTEGER" }, { SHARED_STATE, "INTEGER" },
                { LIKED_STATE, "INTEGER" }, { SHARED_STATE_PENDING, "INTEGER" }, { LIKED_STATE_PENDING, "INTEGER" },
                { FRIENDS_SHARED_STATE, "INTEGER" }, { SHARED_BY_FRIEND, "TEXT" }, { UPDATED_UTC, "INTEGER" },
                { DOWNLOADED, "INTEGER" }, { ERROR, "TEXT" }, { AUTHOR, "TEXT" }, { INSERTED_AT, "INTEGER" },
                { TYPE, "TEXT" }, { NOTE_SUBMITTED_STATE, "INTEGER" }, { NOTE, "TEXT" },
                { NOTE_SHOULD_BE_SHARED, "INTEGER" }, { SNIPPET, "TEXT" } };

        static {
            FIELD_NAMES = new String[Entries.FIELDS.length];
            for (int i = 0; i < Entries.FIELD_NAMES.length; i++) {
                Entries.FIELD_NAMES[i] = Entries.FIELDS[i][0];
            }
        }

    }

    static String sqlQueryFindLabelsByEntry;
    static String sqlQueryLabelsSummary;
    static String sqlQueryLabelsSummaryReadItemsHidden;
    static String sqlQueryContent;
    static String sqlQueryContentReadItemsHidden;
    static String sqlQueryContentWithLabels;
    static String sqlQueryContentWithLabelsReadItemsHidden;

    static final String[] CREATE_INDICES = new String[] { "CREATE INDEX e1 ON entries (read_state, updated_utc desc);",
            "CREATE INDEX e2 ON entries (updated_utc desc, read_state, starred_state);",
            "CREATE INDEX e3 ON entries (atom_id);", "CREATE INDEX e4 ON entries (read_state, starred_state);",
            "CREATE INDEX e5 ON entries (read_state, friends_shared_state);",
            "CREATE INDEX e6 ON entries (type, read_state asc, updated_utc desc);",
            "CREATE INDEX l1 ON labels (name);", "CREATE INDEX f2 ON feeds (_id);",
            "CREATE INDEX ela3 ON entry_label_associations (entry_id);" };

    private Context context;

    private SQLiteDatabase readOnlyDB;

    DB(Context context, String path) {
        super(context, path, DATABASE_NAME, null, DATABASE_VERSION);
        // NewsRob.installNewsRobDefaultExceptionHandler(context);
        this.context = context.getApplicationContext();
        getDb();

    }

    SQLiteDatabase getDb() {
        // Timing t = new Timing("DB.getDB()", context);
        try {
            return getWritableDatabase();
        } finally {
            // t.stop();
        }
    }

    SQLiteDatabase getReadOnlyDb() {
        // return getReadOnlyDatabase();
        return getDb();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // NewsRob.installNewsRobDefaultExceptionHandler(context);

        // tables
        db.execSQL(prepareCreateTableSQL(Entries.TABLE_NAME, Entries.FIELDS));
        db.execSQL(prepareCreateTableSQL(Labels.TABLE_NAME, Labels.FIELDS));
        db.execSQL(prepareCreateTableSQL(EntryLabelAssociations.TABLE_NAME, EntryLabelAssociations.FIELDS));
        db.execSQL(prepareCreateTableSQL(Feeds.TABLE_NAME, Feeds.FIELDS));
        db.execSQL(prepareCreateTableSQL(UnsubscribeFeeds.TABLE_NAME, UnsubscribeFeeds.FIELDS));
        db.execSQL(CREATE_TABLE_TEMP_IDS_SQL);

        // indices
        for (String sql : CREATE_INDICES)
            db.execSQL(sql);

        // views
        db.execSQL(context.getString(R.string.sql_create_view));
        db.execSQL(context.getString(R.string.sql_create_dashboard_view));

        Log.d(TAG, "Database initialized from scratch!");

    }

    private String prepareCreateTableSQL(String tableName, String[][] fields) {

        List<String> fieldExpressions = new ArrayList<String>(fields.length);
        for (String[] field : fields) {
            String fieldName = field[0];
            String fieldType = field[1];
            fieldExpressions.add(fieldName + " " + fieldType);
        }

        return "CREATE TABLE " + tableName + " (" + U.join(fieldExpressions, ", ") + ");";

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // NewsRob.installNewsRobDefaultExceptionHandler(context);
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion <= 1) {
            db.execSQL(prepareCreateTableSQL(Labels.TABLE_NAME, Labels.FIELDS));
            db.execSQL(prepareCreateTableSQL(EntryLabelAssociations.TABLE_NAME, EntryLabelAssociations.FIELDS));
        }
        if (oldVersion <= 2) {
            String sqlBegin = "ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN ";
            String sqlEnd = " " + "INTEGER;";
            db.execSQL(sqlBegin + Entries.SHARED_STATE + sqlEnd);
            db.execSQL(sqlBegin + Entries.SHARED_STATE_PENDING + sqlEnd);
        }
        if (oldVersion <= 3) {
            for (String sql : CREATE_INDICES)
                db.execSQL(sql);
        }
        if (oldVersion <= 4) {
            String sql = "ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.ERROR + " TEXT;";
            db.execSQL(sql);
        }
        if (oldVersion < 6) {

            // structure
            db.execSQL(prepareCreateTableSQL(Feeds.TABLE_NAME, Feeds.FIELDS));
            db.execSQL("drop index e1;");
            db.execSQL("create index e1 on entries (read_state, updated_utc desc); ");
            db.execSQL("create index e2 on entries (updated_utc desc);");
            db.execSQL("drop index if exists ela1;");
            db.execSQL("drop index if exists ela2;");
            db.execSQL("CREATE INDEX ela3 on entry_label_associations (entry_id);");

            db.execSQL("create index f1 on feeds (atom_id);");
            db.execSQL("ALTER TABLE entries ADD COLUMN feed_id INTEGER;");

            // data
            Log.d(TAG, "Starting data migration (creating feeds) done.");
            Cursor allEntriesCursor = db.rawQuery("select _id, feed_title from entries", null);

            if (allEntriesCursor.moveToFirst()) {
                do {
                    long feedId = -1l;
                    Cursor c = db.rawQuery("select _id from feeds where title = ?", new String[] { allEntriesCursor
                            .getString(1) });

                    // Feed with title already there?
                    if (c.moveToFirst())
                        feedId = c.getLong(0);
                    c.close();

                    // Create one otherwise
                    if (feedId == -1l) {
                        ContentValues cv = new ContentValues();
                        cv.put(Feeds.TITLE, allEntriesCursor.getString(1));
                        feedId = db.insert(Feeds.TABLE_NAME, null, cv);
                    }

                    if (feedId == -1l)
                        throw new IllegalStateException("Failed to create feed.");

                    // Assign the feed to the entry
                    ContentValues cv = new ContentValues();
                    cv.put(Entries.FEED_ID, feedId);
                    db.update(Entries.TABLE_NAME, cv, Entries.__ID + "=" + allEntriesCursor.getLong(0), null);

                } while (allEntriesCursor.moveToNext());
            }
            allEntriesCursor.close();

            Log.d(TAG, "Data migration (creating feeds) done.");

            Log.d(TAG, "Now migrating download status.");
            ContentValues cv = new ContentValues();
            cv.put(Entries.DOWNLOADED, Entry.STATE_DOWNLOAD_ERROR);
            db.update(Entries.TABLE_NAME, cv, Entries.DOWNLOADED + "=?", new String[] { "2" });

            cv.put(Entries.DOWNLOADED, Entry.STATE_DOWNLOADED_FULL_PAGE);
            db.update(Entries.TABLE_NAME, cv, Entries.DOWNLOADED + "=?", new String[] { "1" });

            Log.d(TAG, "Migration of download status done.");

            // --
            Log.d(TAG, "Creating view.");
            db.execSQL(context.getString(R.string.sql_create_view));
            Log.d(TAG, "Creating view done.");
            // structure
        }
        if (oldVersion < 7) {
            db.execSQL("drop index f1;");
            db.execSQL("create index f2 on feeds (_id);");
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE entries ADD COLUMN AUTHOR TEXT;");
            /*
             * db.execSQL("DROP VIEW IF EXISTS entries_view;"); Log.d(TAG,
             * "Re-Creating view.");
             * db.execSQL(context.getString(R.string.sql_create_view));
             */
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE feeds ADD COLUMN NOTIFICATION INTEGER;");
        }
        if (oldVersion < 10) { // 5th of July 2009 developed
            db.execSQL("ALTER TABLE entries ADD COLUMN INSERTED_AT INTEGER;");
        }
        if (oldVersion < 11) { // 5th of July 2009 developed
            db.execSQL("UPDATE entries SET INSERTED_AT = 1 WHERE INSERTED_AT IS NULL;");
        }
        if (oldVersion < 12) { // 5th of September 2009 developed
            db.execSQL("ALTER TABLE labels ADD COLUMN " + Labels.ORD + " INTEGER;");
            db.execSQL("UPDATE labels SET ord = 0 WHERE ord IS NULL;");
        }
        if (oldVersion < 13) { // 11th of September 2009 developed
            db.execSQL("ALTER TABLE feeds ADD COLUMN " + Feeds.WEB_SCALE + " DEFAULT 1.0;");
            db.execSQL("ALTER TABLE feeds ADD COLUMN " + Feeds.FEED_SCALE + " DEFAULT 1.0;");
        }
        if (oldVersion < 14) { // 18th of September 2009 developed
            /*
             * db.execSQL("DROP VIEW IF EXISTS dashboard_view;"); Log.d(TAG,
             * "Re-creating dashboard_view.");
             * db.execSQL(context.getString(R.string
             * .sql_create_dashboard_view));
             */

        }
        if (oldVersion < 15) { // 21th of September 2009 developed
            db.execSQL(CREATE_TABLE_TEMP_IDS_SQL);
        }
        if (oldVersion < 16) { // 24th of September 2009 developed
            db.execSQL("ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.FRIENDS_SHARED_STATE
                    + " INTEGER;");

            db.execSQL("UPDATE entries SET friends_shared_state = 0;");
            db.execSQL("UPDATE entries SET friends_shared_state = 1 "
                    + "WHERE EXISTS(SELECT 1 FROM entry_label_associations AS elas, labels "
                    + "WHERE elas.entry_id = entries._id AND elas.label_id = labels._id "
                    + "AND labels.name = \"friends' recently shared\");");

            String deleteElas = "DELETE FROM entry_label_associations " + "WHERE EXISTS(SELECT 1 FROM labels "
                    + "WHERE (labels._id = entry_label_associations.label_id "
                    + "AND labels.name IN(\"my recently starred\", \"friends' recently shared\")));";
            String deleteLabels = "DELETE FROM labels "
                    + "WHERE labels.name IN(\"my recently starred\", \"friends' recently shared\");";
            db.execSQL(deleteElas);
            db.execSQL(deleteLabels);

            /*
             * db.execSQL("DROP VIEW IF EXISTS dashboard_view;"); Log.d(TAG,
             * "Re-Creating dashboard view.");
             * db.execSQL(context.getString(R.string
             * .sql_create_dashboard_view));
             * 
             * db.execSQL("DROP VIEW IF EXISTS entries_view;"); Log.d(TAG,
             * "Re-Creating entries view.");
             * db.execSQL(context.getString(R.string.sql_create_view));
             */

        }

        if (oldVersion < 17) { // 2th of October 2009 developed

            db.execSQL("CREATE INDEX e4 on entries (read_state, starred_state);");
            db.execSQL("CREATE INDEX e5 on entries (read_state, friends_shared_state);");
            db.execSQL("DROP INDEX e2;");
            db.execSQL("CREATE INDEX e2 ON entries (updated_utc desc, read_state, starred_state);");

            // db.execSQL("DROP VIEW IF EXISTS dashboard_view;");
            // Log.d(TAG, "Re-Creating dashboard view.");
            // db.execSQL(context.getString(R.string.sql_create_dashboard_view));

        }
        if (oldVersion < 19) { // 25th of November 2009 developed

            String sql = "ALTER TABLE " + Feeds.TABLE_NAME + " ADD COLUMN " + Feeds.JAVASCRIPT_ENABLED + " INTEGER;";
            db.execSQL(sql);

            // db.execSQL("DROP VIEW IF EXISTS entries_view;");
            // Log.d(TAG, "Re-Creating entries view.");
            // db.execSQL(context.getString(R.string.sql_create_view));

        }

        if (oldVersion < 20) { // 3 dec 09
            String sql = "ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.SHARED_BY_FRIEND + " TEXT;";
            db.execSQL(sql);

            // db.execSQL("DROP VIEW IF EXISTS entries_view;");
            // Log.d(TAG, "Re-Creating entries view.");
            // db.execSQL(context.getString(R.string.sql_create_view));

        }

        if (oldVersion < 21) { // 19 jan 10

            db.execSQL("ALTER TABLE " + Feeds.TABLE_NAME + " ADD COLUMN " + Feeds.ALTERNATE_URL + " TEXT;");

            db.execSQL("ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.TYPE + " TEXT;");
            db.execSQL("ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.NOTE_SUBMITTED_STATE
                    + " INTEGER;");
            db.execSQL("ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.NOTE + " TEXT;");
            db.execSQL("ALTER TABLE " + Entries.TABLE_NAME + " ADD COLUMN " + Entries.NOTE_SHOULD_BE_SHARED
                    + " INTEGER;");

            // db.execSQL("DROP VIEW IF EXISTS dashboard_view;");
            // Log.d(TAG, "Re-Creating dashboard view.");
            // db.execSQL(context.getString(R.string.sql_create_dashboard_view));

            db.execSQL("CREATE INDEX e6 on entries (type, read_state asc, updated_utc desc);");

            db.execSQL("UPDATE entries SET " + Entries.DOWNLOADED + " = 0;");

            Log.d(TAG, "Done migrating.");

        }
        if (oldVersion < 22) { // 26th of May 2010 developed

            db.execSQL("ALTER TABLE entries ADD COLUMN " + Entries.LIKED_STATE + " INTEGER;");
            db.execSQL("ALTER TABLE entries ADD COLUMN " + Entries.LIKED_STATE_PENDING + " INTEGER;");

            String sql = "ALTER TABLE " + Feeds.TABLE_NAME + " ADD COLUMN " + Feeds.FIT_TO_WIDTH_ENABLED + " INTEGER;";
            db.execSQL(sql);

            // db.execSQL("DROP VIEW IF EXISTS entries_view;");
            // Log.d(TAG, "Re-Creating entries view.");
            // db.execSQL(context.getString(R.string.sql_create_view));
            db.execSQL("UPDATE " + Feeds.TABLE_NAME + " SET " + Feeds.FIT_TO_WIDTH_ENABLED + " = 1");

        }
        if (oldVersion < 23) { // 23th of July 2010 developed
            db.execSQL("DROP VIEW IF EXISTS dashboard_view;");
            Log.d(TAG, "Re-Creating dashboard view.");
            db.execSQL(context.getString(R.string.sql_create_dashboard_view));
        }
        if (oldVersion < 24) {// 27th of July 2010 developed
            db.execSQL(prepareCreateTableSQL(UnsubscribeFeeds.TABLE_NAME, UnsubscribeFeeds.FIELDS));
        }
        if (oldVersion < 25) { // 16th of September 2010 developed
            // Now using a higher resolution updated date
            // db.execSQL("UPDATE " + Entries.TABLE_NAME + " SET " +
            // Entries.UPDATED_UTC + " = " + Entries.UPDATED_UTC
            // + " * 1000;");
        }
        if (oldVersion < 26) {
            String whereClause = Entries.UPDATED_UTC + " > ?";
            String value = String.valueOf(new Date(2015, 1, 1).getTime());

            int affectedRows = getRowCount(db, Entries.TABLE_NAME, whereClause, new String[] { value });
            Log.d("DB Migration", affectedRows + " rows affected.");

            db.execSQL("UPDATE " + Entries.TABLE_NAME + " SET " + Entries.UPDATED_UTC + " = " + Entries.UPDATED_UTC
                    + " / 1000 WHERE " + whereClause, new String[] { value });
        }
        if (oldVersion < 27) { // 9th of October 2010 developed
        }
        if (oldVersion < 28) { // 13th of October 2010 developed
            db.execSQL("ALTER TABLE entries ADD COLUMN " + Entries.SNIPPET + " TEXT;");

            db.execSQL("DROP VIEW IF EXISTS entries_view;");
            Log.d(TAG, "Re-Creating entries view.");
            db.execSQL(context.getString(R.string.sql_create_view));
        }

    }

    private Label findLabelByName(String name) {
        String queryString = Labels.NAME + " = ?";
        Cursor cursor = getReadOnlyDb().query(true, Labels.TABLE_NAME, Labels.FIELD_NAMES, queryString,
                new String[] { name }, null, null, null, null);
        Label label = null;
        if (cursor.moveToFirst()) {
            label = new Label(cursor.getLong(0));
            label.setName(cursor.getString(1));
        }
        cursor.close();
        return label;
    }

    Entry findArticleById(Long id) {
        Cursor c = getReadOnlyDb().rawQuery("SELECT * FROM " + ENTRIES_VIEW + " where _id=?",
                new String[] { String.valueOf(id) });
        Entry entry = null;
        if (c.moveToFirst())
            entry = createEntryFromCursor(c);
        c.close();
        return entry;
    }

    Entry findEntryByAtomId(String entryAtomId) {
        Cursor c = getReadOnlyDb().rawQuery("SELECT * FROM " + ENTRIES_VIEW + " where ATOM_ID=?",
                new String[] { entryAtomId });
        Entry entry = null;
        if (c.moveToFirst())
            entry = createEntryFromCursor(c);
        c.close();
        return entry;
    }

    List<Long> findAllArticleIdsToDownload() {
        Timing t = new Timing("findAllArticleIdsToDownload", context);
        List<Long> rv = null;
        String sql = context.getString(R.string.sql_articles_to_download);
        Cursor c = getReadOnlyDb().rawQuery(sql, null);
        try {
            rv = new ArrayList<Long>(c.getCount());
            while (c.moveToNext())
                rv.add(c.getLong(0));
            return rv;
        } finally {
            c.close();
            t.stop();
        }
    }

    List<Entry> findAllByPendingState(String column, String desiredState) {
        Timing t = new Timing("findAllByPendingState: " + column, context);
        String valueColumn = column.substring(0, column.lastIndexOf('_'));
        List<Entry> result = findAllByQueryString(column + "='1' AND " + valueColumn + "='" + desiredState + "'");
        t.stop();
        return result;
    }

    Cursor findCursorByQueryString(String queryString) {
        String sql = "SELECT * FROM entries_view WHERE " + queryString;
        return getReadOnlyDb().rawQuery(sql, null);

    }

    List<Entry> findAllByQueryString(String queryString) {
        Timing t = new Timing("findAllByQueryString " + queryString, context);
        List<Entry> entries = new ArrayList<Entry>();

        Cursor cursor = findCursorByQueryString(queryString);

        if (cursor.moveToFirst()) {
            do {
                entries.add(createEntryFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        t.stop();
        return entries;

    }

    private Entry createEntryFromCursor(Cursor cursor) {

        Entry entry = new Entry(cursor.getLong(cursor.getColumnIndex(Entries.__ID)));
        entry.setAtomId(getStringValueFromCursor(cursor, Entries.ATOM_ID));

        entry.setAlternateHRef(getStringValueFromCursor(cursor, Entries.ALTERNATE_URL));

        entry.setContent(getStringValueFromCursor(cursor, Entries.CONTENT));
        entry.setContentType(getStringValueFromCursor(cursor, Entries.CONTENT_TYPE));
        entry.setContentURL(getStringValueFromCursor(cursor, Entries.CONTENT_URL));

        entry.setTitle(getStringValueFromCursor(cursor, Entries.TITLE));

        entry.setFeedTitle(getStringValueFromCursor(cursor, Entries.FEED_TITLE));

        entry.setReadState(ReadState.fromInt(getIntegerFromCursor(cursor, Entries.READ_STATE)));

        entry.setReadStatePending(getBooleanValueFromCursor(cursor, Entries.READ_STATE_PENDING));

        entry.setStarred(getBooleanValueFromCursor(cursor, Entries.STARRED_STATE));
        entry.setStarredStatePending(getBooleanValueFromCursor(cursor, Entries.STARRED_STATE_PENDING));

        entry.setShared(getBooleanValueFromCursor(cursor, Entries.SHARED_STATE));
        entry.setSharedStatePending(getBooleanValueFromCursor(cursor, Entries.SHARED_STATE_PENDING));

        entry.setLiked(getBooleanValueFromCursor(cursor, Entries.LIKED_STATE));
        entry.setLikedStatePending(getBooleanValueFromCursor(cursor, Entries.LIKED_STATE_PENDING));

        entry.setFriendsShared(getBooleanValueFromCursor(cursor, Entries.FRIENDS_SHARED_STATE));

        entry.setUpdated(getLongFromCursor(cursor, Entries.UPDATED_UTC));

        entry.setDownloaded(getIntegerFromCursor(cursor, Entries.DOWNLOADED));
        entry.setError(getStringValueFromCursor(cursor, Entries.ERROR));

        entry.setFeedId(DB.getLongFromCursor(cursor, Entries.FEED_ID));
        entry.setFeedAtomId(DB.getStringValueFromCursor(cursor, "FEED_ATOM_ID"));

        entry.setAuthor(DB.getStringValueFromCursor(cursor, Entries.AUTHOR));

        entry.setDownloadPref(DB.getIntegerFromCursor(cursor, Feeds.DOWNLOAD_PREF));
        entry.setDisplayPref(DB.getIntegerFromCursor(cursor, Feeds.DISPLAY_PREF));

        entry.setJavaScriptEnabled(DB.getBooleanValueFromCursor(cursor, Feeds.JAVASCRIPT_ENABLED));
        entry.setFitToWidthEnabled(DB.getBooleanValueFromCursor(cursor, Feeds.FIT_TO_WIDTH_ENABLED));

        entry.setSharedByFriend(DB.getStringValueFromCursor(cursor, Entries.SHARED_BY_FRIEND));

        entry.setNoteSubmitted(DB.getBooleanValueFromCursor(cursor, Entries.NOTE_SUBMITTED_STATE));
        entry.setNote(DB.getStringValueFromCursor(cursor, Entries.NOTE));
        entry.setNote("N".equals(DB.getStringValueFromCursor(cursor, Entries.TYPE)));
        entry.setShouldNoteBeShared(DB.getBooleanValueFromCursor(cursor, Entries.NOTE_SHOULD_BE_SHARED));

        entry.setFeedAlternateUrl(DB.getStringValueFromCursor(cursor, "FEED_ALTERNATE_URL"));
        entry.setSnippet(DB.getStringValueFromCursor(cursor, Entries.SNIPPET));

        return entry;
    }

    void update(Entry entry) {
        ContentValues cv = mapEntryToContentValues(entry);
        getDb().update(Entries.TABLE_NAME, cv, Entries.__ID + " = ?", new String[] { String.valueOf(entry.getId()) });
    }

    public void insert(Entry entry) {
        ArrayList<Entry> oneEntryList = new ArrayList<Entry>(1);
        oneEntryList.add(entry);
        insert(oneEntryList);
    }

    void insert(List<Entry> entries) {
        Timing t = new Timing("DB.insert for " + entries.size() + " records.", context);

        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();
        try {
            for (Entry entry : entries) {
                if (entry.getFeedTitle() == null || entry.getFeedAtomId() == null)
                    throw new IllegalStateException("Neither feed title nor feed atom id can be null at this point.");

                Cursor feedCursorAtomId = dbase.rawQuery("SELECT " + Feeds.__ID + ", " + Feeds.ATOM_ID + ", "
                        + Feeds.ALTERNATE_URL + ", " + DB.Feeds.TITLE + " FROM " + Feeds.TABLE_NAME
                        + " WHERE atom_id = ?", new String[] { entry.getFeedAtomId() });

                // does the feed already exists with an atom_id?
                if (feedCursorAtomId.moveToFirst()) {
                    entry.setFeedId(feedCursorAtomId.getLong(0));
                    if (feedCursorAtomId.getString(2) == null) {
                        // set alternate url when not there yet.
                        ContentValues cv = new ContentValues();
                        cv.put(Feeds.ALTERNATE_URL, entry.getFeedAlternateUrl());
                        dbase.update(Feeds.TABLE_NAME, cv, Feeds.__ID + "=" + entry.getFeedId(), null);
                    }
                    // fix for Henrik's issue with badp
                    if (false && entry.getFeedAtomId().endsWith("/source/com.google/link")
                            && !feedCursorAtomId.getString(3).equals(entry.getFeedTitle())) {
                        PL.log("DB: Fixing feed title" + " oldTitle=" + feedCursorAtomId.getString(3) + " newTitle="
                                + entry.getFeedTitle(), context);
                        ContentValues cv = new ContentValues();
                        cv.put(Feeds.TITLE, entry.getFeedTitle());
                        dbase.update(Feeds.TABLE_NAME, cv, Feeds.__ID + "=?", new String[] { String.valueOf(entry
                                .getFeedId()) });
                    }
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put(Feeds.ATOM_ID, entry.getFeedAtomId());
                    cv.put(Feeds.TITLE, entry.getFeedTitle());
                    cv.put(Feeds.ALTERNATE_URL, entry.getFeedAlternateUrl());
                    cv.put(Feeds.WEB_SCALE, -1f);
                    cv.put(Feeds.FEED_SCALE, -1f);
                    cv.put(Feeds.FIT_TO_WIDTH_ENABLED, 1);

                    entry.setFeedId(dbase.insert(Feeds.TABLE_NAME, null, cv));
                }

                feedCursorAtomId.close();

                // make sure the updated timestamp is unique
                long proposedUpdated = entry.getUpdatedInHighResolution();

                while (!isTimestampeUnique(dbase, proposedUpdated))
                    proposedUpdated++;

                entry.setUpdated(proposedUpdated);

                long newEntryId = dbase.insert(Entries.TABLE_NAME, null, mapEntryToContentValues(entry));

                for (Label label : entry.getLabels())
                    associateLabelToEntry(newEntryId, label, dbase);

            }
        } finally {
            dbase.setTransactionSuccessful();
            dbase.endTransaction();
        }

        t.stop();
    }

    private final boolean isTimestampeUnique(SQLiteDatabase dbase, long proposedUpdated) {
        Cursor c = null;
        try {
            c = dbase.rawQuery("SELECT _id FROM " + Entries.TABLE_NAME + " WHERE " + Entries.UPDATED_UTC
                    + " = ? LIMIT 1", new String[] { String.valueOf(proposedUpdated) });
            return c.getCount() == 0;
        } finally {
            if (c != null)
                c.close();
        }
    }

    private void associateLabelToEntry(long entryId, Label label, SQLiteDatabase dbase) {

        long labelId = label.getId();
        if (labelId == -1) {

            // try to find an existing label first
            Label l = findLabelByName(label.getName());
            if (l != null)
                labelId = l.getId();
            else {
                // Otherwise write a new one
                labelId = createLabel(label).getId();
            }
        }

        dbase.delete(DB.EntryLabelAssociations.TABLE_NAME, EntryLabelAssociations.ENTRY_ID + " = ? AND "
                + EntryLabelAssociations.LABEL_ID + " = ?", new String[] { Long.toString(entryId),
                Long.toString(label.getId()) });

        ContentValues cv = new ContentValues();
        cv.put(EntryLabelAssociations.ENTRY_ID, entryId);
        cv.put(EntryLabelAssociations.LABEL_ID, labelId);
        dbase.insert(EntryLabelAssociations.TABLE_NAME, null, cv);
    }

    private Label createLabel(Label label) {

        if (label.getId() != -1)
            return label;

        ContentValues cv = new ContentValues();
        cv.put(Labels.NAME, label.getName());
        cv.put(Labels.ORD, label.getOrder());

        label.setId(getDb().insert(Labels.TABLE_NAME, null, cv));

        return label;
    }

    Cursor getLabelsSummaryCursor(boolean hideReadItems) {
        String queryString = hideReadItems ? sqlQueryLabelsSummaryReadItemsHidden : sqlQueryLabelsSummary;
        // Timing t = new Timing("getLabelsSummaryCursor "+queryString);
        Cursor c = getReadOnlyDb().rawQuery(queryString, null);
        // t.stop();
        return c;
    }

    private ContentValues mapEntryToContentValues(Entry entry) {

        ContentValues cv = new ContentValues();
        if (entry.getId() >= 0l)
            cv.put(Entries.__ID, entry.getId());

        cv.put(Entries.ATOM_ID, entry.getAtomId());

        cv.put(Entries.ALTERNATE_URL, entry.getAlternateHRef());

        cv.put(Entries.CONTENT, entry.getContent());
        cv.put(Entries.CONTENT_TYPE, entry.getContentType());
        cv.put(Entries.CONTENT_URL, entry.getContentURL());

        cv.put(Entries.TITLE, entry.getTitle());

        cv.put(Entries.FEED_TITLE, entry.getFeedTitle());
        cv.put(Entries.FEED_ID, entry.getFeedId());

        cv.put(Entries.READ_STATE, ReadState.toInt(entry.getReadState()));
        cv.put(Entries.READ_STATE_PENDING, entry.isReadStatePending() ? 1 : 0);

        cv.put(Entries.STARRED_STATE, entry.isStarred() ? 1 : 0);
        cv.put(Entries.STARRED_STATE_PENDING, entry.isStarredStatePending() ? 1 : 0);

        cv.put(Entries.LIKED_STATE, entry.isLiked() ? 1 : 0);
        cv.put(Entries.LIKED_STATE_PENDING, entry.isLikedStatePending() ? 1 : 0);

        cv.put(Entries.SHARED_STATE, entry.isShared() ? 1 : 0);
        cv.put(Entries.SHARED_STATE_PENDING, entry.isSharedStatePending() ? 1 : 0);

        cv.put(Entries.FRIENDS_SHARED_STATE, entry.isFriendsShared() ? 1 : 0);

        cv.put(Entries.UPDATED_UTC, entry.getUpdatedInHighResolution());

        cv.put(Entries.DOWNLOADED, entry.getDownloaded());

        cv.put(Entries.AUTHOR, entry.getAuthor());

        cv.put(Entries.ERROR, entry.getError());

        cv.put(Entries.SHARED_BY_FRIEND, entry.getSharedByFriend());

        cv.put(Entries.INSERTED_AT, System.currentTimeMillis());

        cv.put(Entries.NOTE, entry.getNote());
        if (entry.isNote())
            cv.put(Entries.TYPE, "N");
        cv.put(Entries.NOTE_SUBMITTED_STATE, entry.isNoteSubmitted() ? 1 : 0);
        cv.put(Entries.NOTE_SHOULD_BE_SHARED, entry.shouldNoteBeShared() ? 1 : 0);
        cv.put(Entries.SNIPPET, entry.getSnippet());

        return cv;
    }

    final static long getLongFromCursor(Cursor cursor, String fieldName) {
        return cursor.getLong(cursor.getColumnIndex(fieldName));
    }

    final static Date getDateFromCursor(Cursor cursor, String fieldName) {
        return new Date(cursor.getLong(cursor.getColumnIndex(fieldName)));
    }

    final public static String getStringValueFromCursor(final Cursor cursor, final String fieldName) {
        return getStringValueFromCursor(cursor, cursor.getColumnIndex(fieldName));
    }

    final public static String getStringValueFromCursor(final Cursor cursor, final int columnIndex) {
        return cursor.getString(columnIndex);
    }

    final public static int getIntegerFromCursor(final Cursor cursor, final String fieldName) {
        return getIntegerFromCursor(cursor, cursor.getColumnIndex(fieldName));
    }

    final public static int getIntegerFromCursor(final Cursor cursor, final int columnIndex) {
        return cursor.getInt(columnIndex);
    }

    private static float getFloatFromCursor(Cursor c, String fieldName) {
        return getFloatFromCursor(c, c.getColumnIndex(fieldName));
    }

    private static float getFloatFromCursor(Cursor c, int columnIndex) {
        return c.getFloat(columnIndex);
    }

    final public static boolean getBooleanValueFromCursor(final Cursor cursor, String fieldName) {
        int columnIndex = cursor.getColumnIndex(fieldName);
        return getBooleanValueFromCursor(cursor, columnIndex);
    }

    final public static boolean getBooleanValueFromCursor(final Cursor cursor, final int columnIndex) {
        return cursor.getInt(columnIndex) != 0;
    }

    int deleteAll() {
        // LATER Really delete Labels, in particular later on when they hold
        // configurations?
        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();
        try {
            dbase.delete(Labels.TABLE_NAME, "1", null);
            dbase.delete(EntryLabelAssociations.TABLE_NAME, "1", null);
            dbase.delete(UnsubscribeFeeds.TABLE_NAME, "1", null);
            return dbase.delete(Entries.TABLE_NAME, "1", null);
        } finally {
            dbase.setTransactionSuccessful();
            dbase.endTransaction();
        }
    }

    void updateReadState(Entry entry) {

        ContentValues cv = new ContentValues();
        cv.put(Entries.READ_STATE, ReadState.toInt(entry.getReadState()));
        cv.put(Entries.READ_STATE_PENDING, entry.isReadStatePending() ? 1 : 0);

        SQLiteDatabase db = getDb();
        Timing t = new Timing("DB.updateReadState execution", context);
        db.update(Entries.TABLE_NAME, cv, Entries.__ID + "= ?", new String[] { String.valueOf(entry.getId()) });
        t.stop();
    }

    void updateStarredState(Entry entry) {
        ContentValues cv = new ContentValues();
        cv.put(Entries.STARRED_STATE, entry.isStarred() ? 1 : 0);
        cv.put(Entries.STARRED_STATE_PENDING, entry.isStarredStatePending() ? 1 : 0);
        getDb().update(Entries.TABLE_NAME, cv, Entries.__ID + "=?", new String[] { String.valueOf(entry.getId()) });
    }

    void updateLikedState(Entry entry) {
        ContentValues cv = new ContentValues();
        cv.put(Entries.LIKED_STATE, entry.isLiked() ? 1 : 0);
        cv.put(Entries.LIKED_STATE_PENDING, entry.isLikedStatePending() ? 1 : 0);
        getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + "='" + entry.getAtomId() + "'", null);
    }

    void updateSharedState(Entry entry) {
        ContentValues cv = new ContentValues();
        cv.put(Entries.SHARED_STATE, entry.isShared() ? 1 : 0);
        cv.put(Entries.SHARED_STATE_PENDING, entry.isSharedStatePending() ? 1 : 0);
        getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + "='" + entry.getAtomId() + "'", null);
    }

    public void updateFriendsSharedState(Entry entry) {
        ContentValues cv = new ContentValues();
        cv.put(Entries.FRIENDS_SHARED_STATE, entry.isFriendsShared() ? 1 : 0);
        getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + "='" + entry.getAtomId() + "'", null);
    }

    boolean updateDownloaded(Entry entry) {
        ContentValues cv = new ContentValues();
        cv.put(Entries.DOWNLOADED, entry.getDownloaded());
        cv.put(Entries.ERROR, entry.getError());
        int result = getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + "='" + entry.getAtomId() + "'", null);
        return result == 1;
    }

    private int deleteEntry(Entry entry, SQLiteDatabase dbase) {
        final String[] whereArgs = new String[] { String.valueOf(entry.getId()) };
        dbase.delete(EntryLabelAssociations.TABLE_NAME, EntryLabelAssociations.ENTRY_ID + "=?", whereArgs);
        return dbase.delete(Entries.TABLE_NAME, Entries.__ID + "=?", whereArgs);

    }

    int deleteEntry(Entry entry) {
        return deleteEntry(entry, getDb());
    }

    Cursor getOverCapacityIds(final int capacity, final int keepStarred, final int keepShared, final int keepNotes) {

        String sql = context.getString(R.string.sql_get_ids_to_delete);
        sql = Pattern.compile("-- MARK BEGIN.*?-- MARK END", Pattern.DOTALL).matcher(sql).replaceAll("");

        Cursor cursor = getReadOnlyDb().rawQuery(
                sql,
                new String[] { Integer.toString(keepStarred), Integer.toString(keepShared),
                        Integer.toString(keepNotes), Integer.toString(capacity) });

        return cursor;
    }

    Cursor getReadArticlesIdsForDeletion(int numberOfStarredArticlesToKeep, int numberOfSharedArticlesToKeep,
            int numberOfNotesToKeep) {

        String sql = context.getString(R.string.sql_get_ids_to_delete);
        Cursor cursor = getReadOnlyDb().rawQuery(
                sql,
                new String[] { Integer.toString(numberOfStarredArticlesToKeep),
                        Integer.toString(numberOfSharedArticlesToKeep), Integer.toString(numberOfNotesToKeep),
                        Integer.toString(0) });

        return cursor;
    }

    void removePendingStateMarkers(Collection<String> atomIds, String column) {
        ContentValues cv = new ContentValues();
        if (Entries.READ_STATE_PENDING.equals(column))
            cv.put(Entries.READ_STATE_PENDING, false);
        else if (Entries.STARRED_STATE_PENDING.equals(column))
            cv.put(Entries.STARRED_STATE_PENDING, false);
        else if (Entries.LIKED_STATE_PENDING.equals(column))
            cv.put(Entries.LIKED_STATE_PENDING, false);
        else if (Entries.SHARED_STATE_PENDING.equals(column))
            cv.put(Entries.SHARED_STATE_PENDING, false);

        for (String atomId : atomIds)
            getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + "=?", new String[] { atomId });

    }

    Cursor getDashboardContentCursor(DBQuery dbq) {
        boolean hideReadItems = dbq.shouldHideReadItems();
        String sql = context.getString(hideReadItems ? R.string.sql_query_dashboard_unread_only
                : R.string.sql_query_dashboard_all);
        return getReadOnlyDb().rawQuery(sql, null);
    }

    public Cursor getFeedListContentCursor(DBQuery query) {
        Timing t = new Timing("DB.getFeedListContentCursor()", context);

        query = new DBQuery(query);

        List<String> selectionArgs = new ArrayList<String>(5);
        selectionArgs.add(query.shouldHideReadItems() ? "1" : "2");

        selectionArgs.addAll(addFakeLabelsToFilter(query));

        String sql = context.getString(R.string.sql_feeds_query);

        if (query.shouldHideReadItems())
            sql = Pattern.compile("-- read-all-mark.*?-- read-all-mark-end", Pattern.DOTALL).matcher(sql).replaceFirst(
                    "");
        else
            sql = Pattern.compile("-- read-unread-only-mark.*?-- read-unread-only-mark-end", Pattern.DOTALL).matcher(
                    sql).replaceFirst("");

        // label
        if (query.getFilterLabel() == null || "all articles".equals(query.getFilterLabel()))
            sql = Pattern.compile("-- labels-mark.*?-- labels-mark-end", Pattern.DOTALL).matcher(sql).replaceAll("");
        else
            selectionArgs.add(query.getFilterLabel());

        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);

        Cursor c = getReadOnlyDb().rawQuery(sql, sArgs);
        t.stop();

        return c;
    }

    public int getContentCount(DBQuery query) {
        Timing t = new Timing("getContentCount", context);

        query = new DBQuery(query);

        List<String> selectionArgs = new ArrayList<String>(4);
        selectionArgs.add(query.shouldHideReadItems() ? "1" : "2");

        // startDate
        selectionArgs.add(String.valueOf(query.getStartDate()));

        selectionArgs.addAll(addFakeLabelsToFilter(query));

        String sql = getContentCursorSQL(query, selectionArgs);
        sql = Pattern.compile("SELECT(.*)FROM", Pattern.DOTALL).matcher(sql).replaceFirst("SELECT COUNT(*) FROM");
        sql = Pattern.compile("(ORDER.*)", Pattern.DOTALL).matcher(sql).replaceAll("");
        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);

        Cursor c = getReadOnlyDb().rawQuery(sql, sArgs);
        try {
            if (c.moveToFirst())
                return c.getInt(0);
            else
                return -1;
        } finally {
            c.close();
            t.stop();
        }

    }

    public Cursor getContentCursor(DBQuery query) {
        Timing t = new Timing("getContentCursor", context);

        query = new DBQuery(query);

        List<String> selectionArgs = new ArrayList<String>(4);
        selectionArgs.add(query.shouldHideReadItems() ? "1" : "2");

        // startDate
        selectionArgs.add(String.valueOf(query.getStartDate()));

        selectionArgs.addAll(addFakeLabelsToFilter(query));

        String sql = getContentCursorSQL(query, selectionArgs);
        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);

        Timing t2 = new Timing("rawQuery", context);

        Cursor c = getReadOnlyDb().rawQuery(sql, sArgs);
        t2.stop();
        t.stop();

        return c;

    }

    private String getContentCursorSQL(DBQuery query, List<String> selectionArgs) {
        // feed
        String sql = context.getString(R.string.sql_content_cursor_query);
        if (query.getFilterFeedId() == null || "all articles".equals(query.getFilterFeedId()))
            sql = Pattern.compile("-- feeds-mark.*?-- feeds-mark-end", Pattern.DOTALL).matcher(sql).replaceAll("");
        else
            selectionArgs.add(String.valueOf(query.getFilterFeedId()));

        // label
        if (query.getFilterLabel() == null || "all articles".equals(query.getFilterLabel()))
            sql = Pattern.compile("-- labels-mark.*?-- labels-mark-end", Pattern.DOTALL).matcher(sql).replaceAll("");
        else
            selectionArgs.add(query.getFilterLabel());

        // notifications
        if (query.getStartDate() <= 0)
            sql = Pattern.compile("-- notification-feeds-mark.*?-- notification-feeds-mark-end", Pattern.DOTALL)
                    .matcher(sql).replaceAll("");

        // sort order
        sql += (query.isSortOrderAscending() ? " ASC" : " DESC") + ",\n  entries._id\n";

        if (query.getLimit() > 0)
            sql += "\n  LIMIT " + query.getLimit();
        return sql;
    }

    private List<String> addFakeLabelsToFilter(DBQuery query) {
        boolean showMyRecentlyStarredOnly = false;
        boolean showRecentlySharedByFriendsOnly = false;

        if (NewsRob.CONSTANT_FRIENDS_RECENTLY_SHARED.equals(query.getFilterLabel())) {
            showRecentlySharedByFriendsOnly = true;
            query.setFilterLabel(null);
        } else if (NewsRob.CONSTANT_MY_RECENTLY_STARRED.equals(query.getFilterLabel())) {
            showMyRecentlyStarredOnly = true;
            query.setFilterLabel(null);
        }
        List<String> sArgs = new ArrayList<String>(2);
        sArgs.add(showMyRecentlyStarredOnly ? "0" : "-1");
        sArgs.add(showRecentlySharedByFriendsOnly ? "0" : "-1");

        return sArgs;
    }

    Feed findFeedById(long feedId) {
        Cursor c = null;
        try {
            c = getReadOnlyDb().query(Feeds.TABLE_NAME, Feeds.FIELD_NAMES, Feeds.__ID + " = ?",
                    new String[] { String.valueOf(feedId) }, null, null, null);
            if (c.moveToFirst()) {
                return createFeedFromCursor(c);
            } else
                return null;
        } finally {
            if (c != null)
                c.close();
        }

    }

    public long findFeedIdByFeedUrl(String feedUrl) {
        Cursor c = null;
        try {
            c = getReadOnlyDb().query(Feeds.TABLE_NAME, new String[] { Feeds.__ID }, Feeds.ATOM_ID + " LIKE ?",
                    new String[] { "%" + feedUrl }, null, null, null);
            if (c.moveToFirst()) {
                return c.getLong(0);
            } else
                return -1;
        } finally {
            if (c != null)
                c.close();
        }
    }

    long findFeedIdByFeedAtomId(String feedAtomId) {
        Cursor c = null;
        try {
            c = getReadOnlyDb().query(Feeds.TABLE_NAME, new String[] { Feeds.__ID }, Feeds.ATOM_ID + " = ?",
                    new String[] { feedAtomId }, null, null, null);
            if (c.moveToFirst()) {
                return c.getLong(0);
            } else
                return -1;
        } finally {
            if (c != null)
                c.close();
        }

    }

    boolean doesFeedExist(String feedAtomId) {
        Cursor c = null;
        try {
            c = getReadOnlyDb().query(Feeds.TABLE_NAME, new String[] { Feeds.__ID }, Feeds.ATOM_ID + " = ?",
                    new String[] { feedAtomId }, null, null, null);
            return c.moveToFirst();
        } finally {
            if (c != null)
                c.close();
        }

    }

    List<Feed> findAllFeeds() {
        Cursor c = null;
        List<Feed> feeds = new ArrayList<Feed>(500);
        try {
            c = getReadOnlyDb().query(Feeds.TABLE_NAME, Feeds.FIELD_NAMES, "1=1", null, null, null, null);
            while (c.moveToNext())
                feeds.add(createFeedFromCursor(c));
        } finally {
            if (c != null)
                c.close();
        }
        return feeds;
    }

    Cursor getAllFeedsCursor() {
        return getReadOnlyDb().query(Feeds.TABLE_NAME, new String[] { "_id", "lower(" + Feeds.TITLE + ") AS TITLE" },
                "1=1", null, null, null, Feeds.TITLE);
    }

    Cursor getAllLabelsCursor() {
        return getReadOnlyDb().query(Labels.TABLE_NAME, new String[] { "_id", "lower(" + Labels.NAME + ") AS NAME" },
                "1=1", null, null, null, Labels.NAME);
    }

    private Feed createFeedFromCursor(Cursor c) {

        Feed feed = new Feed();

        feed.setId(DB.getLongFromCursor(c, Feeds.__ID));
        feed.setTitle(DB.getStringValueFromCursor(c, Feeds.TITLE));
        feed.setDownloadPref(DB.getIntegerFromCursor(c, Feeds.DOWNLOAD_PREF));
        feed.setDisplayPref(DB.getIntegerFromCursor(c, Feeds.DISPLAY_PREF));
        feed.setNotificationEnabled(DB.getBooleanValueFromCursor(c, Feeds.NOTIFICATION_ENABLED));
        feed.setWebScale(DB.getFloatFromCursor(c, Feeds.WEB_SCALE));
        feed.setFeedScale(DB.getFloatFromCursor(c, Feeds.FEED_SCALE));
        feed.setJavaScriptEnabled(DB.getBooleanValueFromCursor(c, Feeds.JAVASCRIPT_ENABLED));
        feed.setFitToWidthEnabled(DB.getBooleanValueFromCursor(c, Feeds.FIT_TO_WIDTH_ENABLED));
        feed.setAtomId(DB.getStringValueFromCursor(c, Feeds.ATOM_ID));

        return feed;
    }

    boolean updateFeed(Feed feed) {

        ContentValues cv = mapFeedToContentValues(feed);

        return getDb().update(Feeds.TABLE_NAME, cv, Feeds.__ID + " = ?", new String[] { String.valueOf(feed.getId()) }) == 1;
    }

    private ContentValues mapFeedToContentValues(Feed feed) {
        ContentValues cv = new ContentValues();

        cv.put(Feeds.ATOM_ID, feed.getAtomId());
        cv.put(Feeds.TITLE, feed.getTitle());

        cv.put(Feeds.DOWNLOAD_PREF, feed.getDownloadPref());
        cv.put(Feeds.DISPLAY_PREF, feed.getDisplayPref());

        cv.put(Feeds.NOTIFICATION_ENABLED, feed.isNotificationEnabled() ? "1" : "0");

        cv.put(Feeds.WEB_SCALE, feed.getWebScale());
        cv.put(Feeds.FEED_SCALE, feed.getFeedScale());

        cv.put(Feeds.JAVASCRIPT_ENABLED, feed.isJavaScriptEnabled() ? "1" : "0");
        cv.put(Feeds.FIT_TO_WIDTH_ENABLED, feed.isFitToWidthEnabled() ? "1" : "0");

        cv.put(Feeds.ALTERNATE_URL, feed.getAlternateUrl());
        return cv;
    }

    void markAllRead(DBQuery query) {
        Timing t = new Timing("markAllRead - total", context);

        List<String> selectionArgs = new ArrayList<String>(2);
        if (query.getLimit() > 0) {
            // also a date limit is set?
            // then remove the limit
            if (query.getDateLimit() != 0l) {
                query = new DBQuery(query);
                query.setLimit(0);
            } else {
                query = prepareDBQueryForUpdateWithLimit(query);
            }
        }

        // problem? can't get limit?
        if (query == null)
            return;

        String sql = createMarkAllReadSQLStatementAndParametersAndAddSelectionArgs(query, selectionArgs);
        sql = Pattern.compile("LIMIT.*--END-LIMIT").matcher(sql).replaceFirst("");

        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);
        getDb().execSQL(sql, sArgs);
        t.stop();
    }

    // This is a workaround for Android's limitation to update with LIMIT
    // the nth article's date is used as the new boundary
    private DBQuery prepareDBQueryForUpdateWithLimit(DBQuery query) {
        Timing t = new Timing("prepareDBQueryForUpdateWithLimit", context);
        // more than limit records, otherwise return?
        if (getMarkAllReadCount(query) <= query.getLimit())
            return query;

        // find LIMITth article
        List<String> sArgs = new ArrayList<String>(0);
        String sql = createMarkAllReadSQLStatementAndParametersAndAddSelectionArgs(query, sArgs);
        sql = Pattern.compile("UPDATE.*?-- END-OF-UPDATE", Pattern.DOTALL).matcher(sql).replaceAll(
                "SELECT atom_id\nFROM entries\n");
        sql = Pattern.compile("LIMIT.*--END-LIMIT").matcher(sql).replaceFirst(
                "\n " + ("\nORDER BY entries.updated_utc " + (query.isSortOrderAscending() ? "ASC" : "DESC"))
                        + "\nLIMIT " + query.getLimit() + " OFFSET " + (query.getLimit() - 1));

        Cursor c = getDb().rawQuery(sql, sArgs.toArray(new String[sArgs.size()]));

        // if this fails another update has been done concurrently. Return null
        // then.
        try {
            if (!c.moveToNext())
                return null;
            else {
                Entry entry = findEntryByAtomId(c.getString(0));
                if (entry == null)
                    return null;

                query = new DBQuery(query);
                // remove the now no longer needed limit
                query.setLimit(0);
                // and replace it with an attribute based limit
                query.setDateLimit(entry.getUpdatedInHighResolution());
                return query;
            }
        } finally {
            c.close();
            t.stop();
        }
    }

    Boolean isMarkAllReadPossible(DBQuery query) {
        Timing t = new Timing("isMarkAllReadPossible - total", context);

        List<String> selectionArgs = new ArrayList<String>(2);
        String sql = createMarkAllReadSQLStatementAndParametersAndAddSelectionArgs(query, selectionArgs);
        sql = Pattern.compile("LIMIT .*--END-LIMIT", Pattern.DOTALL).matcher(sql).replaceAll(" ");
        sql = Pattern.compile("UPDATE.*?-- END-OF-UPDATE", Pattern.DOTALL).matcher(sql).replaceAll(
                "SELECT _id\nFROM entries\n")
                + "\n LIMIT 1 --END-LIMIT\n";
        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);
        Cursor c = null;
        try {
            c = getReadOnlyDb().rawQuery(sql, sArgs);
            return Boolean.valueOf(c.moveToFirst());
        } finally {
            if (c != null)
                c.close();
            t.stop();
        }
    }

    long getMarkAllReadCount(DBQuery query) {
        Timing t = new Timing("markAllReadCount - total", context);

        List<String> selectionArgs = new ArrayList<String>(2);
        String sql = createMarkAllReadSQLStatementAndParametersAndAddSelectionArgs(query, selectionArgs);
        sql = Pattern.compile("UPDATE.*?-- END-OF-UPDATE", Pattern.DOTALL).matcher(sql).replaceAll(
                "SELECT COUNT(*)\nFROM entries\n");

        String[] sArgs = selectionArgs.toArray(new String[selectionArgs.size()]);
        Cursor c = getReadOnlyDb().rawQuery(sql, sArgs);
        long count = -1l;
        if (c.moveToFirst())
            count = c.getInt(0);
        c.close();

        t.stop();

        return count;

    }

    private String createMarkAllReadSQLStatementAndParametersAndAddSelectionArgs(DBQuery query,
            List<String> selectionArgs) {
        query = new DBQuery(query);

        boolean feedsNeeded = false;

        String sql = context.getString(R.string.sql_mark_all_read_label);

        // startDate
        selectionArgs.add(String.valueOf(query.getStartDate()));

        selectionArgs.addAll(addFakeLabelsToFilter(query));

        // date limit
        if (query.getDateLimit() != 0l) {
            final String dateLimitClause = "\n  updated_utc " + (query.isSortOrderAscending() ? " <= " : " >= ")
                    + query.getDateLimit() + " AND\n";
            sql = Pattern.compile("-- D_L --", Pattern.DOTALL).matcher(sql).replaceAll(dateLimitClause);
        }

        // feed
        if (query.getFilterFeedId() == null || "all articles".equals(query.getFilterFeedId()))
            sql = Pattern.compile("-- feeds-mark.*?-- feeds-mark-end", Pattern.DOTALL).matcher(sql).replaceAll("");
        else {
            selectionArgs.add(String.valueOf(query.getFilterFeedId()));
            feedsNeeded = true;
        }

        // label
        if (query.getFilterLabel() == null || "all articles".equals(query.getFilterLabel()))
            sql = Pattern.compile("-- labels-mark.*?-- labels-mark-end", Pattern.DOTALL).matcher(sql).replaceAll("");
        else
            selectionArgs.add(query.getFilterLabel());

        // notifications
        if (query.getStartDate() <= 0)
            sql = Pattern.compile("-- notification-feeds-mark.*?-- notification-feeds-mark-end", Pattern.DOTALL)
                    .matcher(sql).replaceAll("");
        else
            feedsNeeded = true;

        if (!feedsNeeded)
            sql = Pattern.compile("-- feeds-needed.*?-- feeds-needed-end", Pattern.DOTALL).matcher(sql).replaceAll("");

        // only add LIMIT if a limit is specified in the DBQuery and not already
        // in the SQL
        if (query.getLimit() > 0 && sql.indexOf(" LIMIT") == -1)
            sql += "\n  LIMIT " + query.getLimit() + "--END-LIMIT\n";
        return sql;
    }

    public boolean updateStates(Collection<StateChange> stateChanges) {
        boolean updated = false;
        for (StateChange stateChange : stateChanges) {
            Cursor c = null;
            String valueColumnName = null;
            String stateColumnName = null;

            switch (stateChange.getState()) {
            case EntriesRetriever.StateChange.STATE_READ:
                valueColumnName = Entries.READ_STATE;
                stateColumnName = Entries.READ_STATE_PENDING;
                break;
            case EntriesRetriever.StateChange.STATE_STARRED:
                valueColumnName = Entries.STARRED_STATE;
                stateColumnName = Entries.STARRED_STATE_PENDING;
                break;
            case EntriesRetriever.StateChange.STATE_LIKED:
                valueColumnName = Entries.LIKED_STATE;
                stateColumnName = Entries.LIKED_STATE_PENDING;
                break;
            case EntriesRetriever.StateChange.STATE_BROADCAST:
                valueColumnName = Entries.SHARED_STATE;
                stateColumnName = Entries.SHARED_STATE_PENDING;
                break;
            case EntriesRetriever.StateChange.STATE_BROADCAST_FRIENDS:
                valueColumnName = Entries.FRIENDS_SHARED_STATE;
                stateColumnName = "0";
                break;
            default:
                throw new IllegalArgumentException("state invalid: " + stateChange.getState());
            }
            try {
                String newValue = stateChange.getOperation() == EntriesRetriever.StateChange.OPERATION_REMOVE ? "0"
                        : "1";
                c = getDb().query(
                        Entries.TABLE_NAME,
                        Entries.FIELD_NAMES,
                        "atom_id = ? " + "AND ((" + valueColumnName + " != " + newValue + " AND " + stateColumnName
                                + " = 0) OR " + "(" + valueColumnName + " = " + newValue + " AND " + stateColumnName
                                + " = 1))", new String[] { stateChange.getAtomId() }, null, null, null);

                if (c.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    cv.put(valueColumnName, newValue);
                    if (!stateColumnName.equals("0"))
                        cv.put(stateColumnName, "0");
                    int rowsAffected = getDb().update(Entries.TABLE_NAME, cv, Entries.ATOM_ID + " = ?",
                            new String[] { stateChange.getAtomId() });
                    if (rowsAffected > 0)
                        updated = true;
                } // else nothing to do.
            } finally {
                if (c != null)
                    c.close();
            }
        }
        return updated;
    }

    public int getFeedCount() {
        return getRowCount(Feeds.TABLE_NAME);
    }

    public int getArticleCount() {
        return getRowCount(Entries.TABLE_NAME);
    }

    public int getUnreadArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "read_state <= ?", new String[] { "0" });
    }

    public int getUnreadArticleCountExact() {
        return getRowCount(Entries.TABLE_NAME, "read_state = ?", new String[] { "0" });
    }

    public int getReadArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "read_state = ?", new String[] { "1" });
    }

    public int getPinnedArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "read_state = ?", new String[] { "-1" });
    }

    public int getStarredArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "starred_state > ?", new String[] { "0" });
    }

    public int getSharedArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "shared_state > ?", new String[] { "0" });
    }

    public int getNotesCount() {
        return getRowCount(Entries.TABLE_NAME, "type = ?", new String[] { "N" });
    }

    public int getChangedArticleCount() {
        Timing t = new Timing("getChangedArticleCount", context);
        try {
            return getRowCount(
                    Entries.TABLE_NAME,
                    "read_state_pending <> 0 OR starred_state_pending <> 0 OR shared_state_pending <> 0 OR liked_state_pending <> 0 OR (note_submitted_state = 0 AND note IS NOT null)",
                    null);
        } finally {
            t.stop();
        }
    }

    public int getFeeds2UnsubscribeCount() {
        return getRowCount(UnsubscribeFeeds.TABLE_NAME);
    }

    Cursor getFeeds2UnsubscribeCursor() {
        Cursor c = getReadOnlyDb().rawQuery("SELECT _ID, SUBSTR(FEED_ATOM_ID,28) FROM " + UnsubscribeFeeds.TABLE_NAME,
                null);
        return c;
    }

    public boolean isFeedMarkedToBeUnsubscribed(String feedAtomId) {
        return getRowCount(UnsubscribeFeeds.TABLE_NAME, UnsubscribeFeeds.FEED_ATOM_ID + " = ?",
                new String[] { feedAtomId }) > 0;
    }

    public int getPendingReadStateArticleCount() {
        return getRowCount(Entries.TABLE_NAME, "read_state_pending = ?", new String[] { "1" });
    }

    public void populateTempIds(long[] articleIds) {
        clearTempTable();

        Timing t = new Timing("DB.populateTempIds count=" + articleIds.length, context);

        PL.log("EntriesRetriever: number of article ids=" + articleIds.length, context);
        SQLiteDatabase dbase = getDb();
        int i = 0;
        while (i < articleIds.length) {

            dbase.beginTransaction();
            SQLiteStatement stmt = dbase.compileStatement("INSERT INTO temp_ids values(?);");
            for (int j = 0; j < 10000 && i < articleIds.length; j++, i++) {
                long l = articleIds[i];
                String id = "tag:google.com,2005:reader/item/" + U.longToHex(l);
                stmt.bindString(1, id);
                stmt.execute();
            }
            stmt.close();
            dbase.setTransactionSuccessful();
            dbase.endTransaction();
        }
        t.stop();
    }

    public void updateStatesFromTempTable(ArticleDbState state) {
        Timing t = new Timing("DB.updateStatesFromTempTable state=" + state, context);

        String stateColumn = null;
        String statePendingColumn = null;
        int targetValueOn = 1;
        int targetValueOff = 0;

        if (state == ArticleDbState.READ) {
            stateColumn = Entries.READ_STATE;
            statePendingColumn = Entries.READ_STATE_PENDING;
            targetValueOn = 0;
            targetValueOff = 1;

        } else if (state == ArticleDbState.STARRED) {
            stateColumn = Entries.STARRED_STATE;
            statePendingColumn = Entries.STARRED_STATE_PENDING;

        } else if (state == ArticleDbState.SHARED) {
            stateColumn = Entries.SHARED_STATE;
            statePendingColumn = Entries.SHARED_STATE_PENDING;

        } else if (state == ArticleDbState.LIKED) {
            stateColumn = Entries.LIKED_STATE;
            statePendingColumn = Entries.LIKED_STATE_PENDING;
        }

        if (stateColumn == null)
            throw new IllegalStateException("stateColumn must not be null here.");

        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();

        // Mark all articles as read where the read_state is not pending
        // and they were not read before
        Timing t3 = new Timing("DB.updateStatesFromTempTable - mark existing read", context);
        final String markExistingSQL = "UPDATE " + Entries.TABLE_NAME + " SET " + stateColumn + " = " + targetValueOff
                + " WHERE " + stateColumn + " = " + targetValueOn + " AND " + statePendingColumn + " = 0;";
        dbase.execSQL(markExistingSQL);
        t3.stop();

        // Mark all articles unread that exists in the temp table and are not
        // read state pending

        Timing t4 = new Timing("DB.updateStatesFromTempTable - mark as x", context);

        String sql = context.getString(R.string.sql_mark_as_x);
        sql = sql.replaceAll("-STATE-", stateColumn);
        sql = sql.replaceAll("-STATE_PENDING-", statePendingColumn);
        sql = sql.replaceAll("-SET-", targetValueOn + "");
        sql = sql.replaceAll("-CLEAR-", targetValueOff + "");

        dbase.execSQL(sql);
        t4.stop();

        if (state == ArticleDbState.READ) {
            Timing t5 = new Timing("DB.updateReadStates - mark as read even when pinned", context);

            dbase.execSQL(context.getString(R.string.sql_mark_as_read_even_when_pinned));
            t5.stop();
        }

        dbase.setTransactionSuccessful();
        dbase.endTransaction();
        t.stop();
    }

    public void clearTempTable() {
        Timing t = new Timing("DB.clearTempTable", context);
        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();

        dbase.execSQL(CLEAR_TEMP_TABLE_SQL);

        dbase.setTransactionSuccessful();
        dbase.endTransaction();
        t.stop();
    }

    public void removeLocallyExistingArticlesFromTempTable() {
        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();

        dbase.execSQL(context.getString(R.string.sql_delete_existing_from_temp_table));

        dbase.setTransactionSuccessful();
        dbase.endTransaction();
    }

    public int getTempIdsCount() {
        return getRowCount("temp_ids");
    }

    public List<String> getNewArticleAtomIdsToFetch(int noOfArticles2Fetch) {
        final String sql = context.getString(R.string.sql_select_next_articles_to_fetch) + " " + noOfArticles2Fetch;

        List<String> rv = new ArrayList<String>();

        Cursor c = getReadOnlyDb().rawQuery(sql, null);

        if (c.moveToFirst()) {
            do {
                rv.add(c.getString(0));
            } while (c.moveToNext());
        }

        c.close();

        return rv;
    }

    private int getRowCount(String tableName) {
        return getRowCount(tableName, null, null);
    }

    private int getRowCount(SQLiteDatabase db, String tableName, String whereClause, String[] parameters) {
        int count = -1;
        Cursor c = db.query(tableName, new String[] { "COUNT(*)" }, whereClause, parameters, null, null, null);
        if (c.moveToFirst())
            count = c.getInt(0);
        c.close();
        return count;
    }

    private int getRowCount(String tableName, String whereClause, String[] parameters) {
        return getRowCount(getReadOnlyDb(), tableName, whereClause, parameters);
    }

    public Cursor findByFullText(String query) {
        Timing t = new Timing("DB.findByFullText " + query + ".", context);

        // 1.6 version
        /*
         * StringBuilder sb = new StringBuilder("SELECT _id, title AS " +
         * SearchManager.SUGGEST_COLUMN_TEXT_1 + ", feed_title AS " +
         * SearchManager.SUGGEST_COLUMN_TEXT_2 + ", atom_id AS " +
         * SearchManager.SUGGEST_COLUMN_INTENT_DATA +
         * ", \"com.newsrob.VIEW\" AS " +
         * SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ", \"" +
         * SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT + "\" AS " +
         * SearchManager.SUGGEST_COLUMN_SHORTCUT_ID +
         * " FROM entries_view WHERE ");
         */

        StringBuilder sb = new StringBuilder("SELECT _id, title AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
                + ", feed_title AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ", atom_id AS "
                + SearchManager.SUGGEST_COLUMN_INTENT_DATA + ", \"com.newsrob.VIEW\" AS "
                + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + " FROM entries_view WHERE ");
        sb.append(processFullTextQueryString(query) + " LIMIT 10");
        // if (readOnlyDB == null)
        // readOnlyDB = getReadableDatabase();
        Cursor cursor = readOnlyDB.rawQuery(sb.toString(), null);

        MatrixCursor mc = new MatrixCursor(SearchProvider.COLUMNS);
        while (cursor.moveToNext()) {
            String[] values = new String[SearchProvider.COLUMNS.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = cursor.getString(i);
            }
            mc.addRow(values);
        }
        cursor.close();
        // readOnlyDB.close();

        t.stop();

        return mc;
    }

    private String processFullTextQueryString(String query) {
        if (query == null)
            query = "";

        StringBuilder processedQuery = new StringBuilder("(");

        String[] columns = { "content", "title", "feed_title" };

        for (String columnName : columns) {
            StringTokenizer st = new StringTokenizer(query);
            List<String> expressions = new ArrayList<String>(6);
            while (st.hasMoreTokens()) {
                String keyWord = st.nextToken();
                expressions.add(columnName + " LIKE \'%" + keyWord + "%\'");
                if (expressions.size() >= 2)
                    break;
            }

            processedQuery.append(TextUtils.join(" AND ", expressions));
            processedQuery.append(") OR (");
        }
        processedQuery.append(")");
        return processedQuery.toString().replace("OR ()", "").trim();
    }

    public long insert(Feed feed) {
        ContentValues cv = mapFeedToContentValues(feed);
        return getDb().insert(Feeds.TABLE_NAME, null, cv);
    }

    public void updateFeedNames(Map<String, String> remoteFeedAtomIdsAndFeedTitles) {
        Timing t = new Timing("Updating feed names in DB", context);

        int updateCount = 0;
        Cursor c = null;
        try {
            SQLiteDatabase dbase = getDb();
            c = dbase.query(Feeds.TABLE_NAME, new String[] { Feeds.__ID, Feeds.ATOM_ID, Feeds.TITLE }, "1=1", null,
                    null, null, null);
            while (c.moveToNext()) {
                final long localId = c.getLong(0);
                final String localeAtomId = c.getString(1);
                final String localTitle = c.getString(2);

                if (remoteFeedAtomIdsAndFeedTitles.containsKey(localeAtomId)) {
                    final String remoteTitle = remoteFeedAtomIdsAndFeedTitles.get(localeAtomId);
                    if (localTitle == null || !localTitle.equals(remoteTitle)) {

                        ContentValues cv = new ContentValues();
                        cv.put(Feeds.TITLE, remoteTitle);

                        dbase.update(Feeds.TABLE_NAME, cv, Feeds.__ID + " = ?",
                                new String[] { String.valueOf(localId) });

                        updateCount++;
                    }

                }
            }

        } finally {
            if (c != null)
                c.close();
            t.stop();
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("DB::updateFeedNames updated " + updateCount + " feeds.", context);
        }
    }

    public Long findNotesFeedId(String googleUserId) {
        Cursor c = null;
        try {
            c = getReadOnlyDb().query(
                    Feeds.TABLE_NAME,
                    Feeds.FIELD_NAMES,
                    Feeds.TITLE + " = \"Your Notes\" AND " + Feeds.ATOM_ID + " = \"tag:google.com,2005:reader/user/"
                            + googleUserId + "/source/com.google/link\"", null, null, null, null);
            if (c.moveToFirst()) {
                return c.getLong(0);
            } else
                return null;
        } finally {
            if (c != null)
                c.close();
        }

    }

    public void clearNotesSubmissionStateForAllSubmittedNotes() {
        ContentValues cv = new ContentValues();
        String note = null;
        cv.put(Entries.NOTE, note);
        cv.put(Entries.NOTE_SUBMITTED_STATE, 0);
        cv.put(Entries.NOTE_SHOULD_BE_SHARED, 0);

        getDb().update(Entries.TABLE_NAME, cv,
                Entries.NOTE + " IS NOT NULL AND " + Entries.NOTE_SUBMITTED_STATE + " = 1", null);

    }

    public List<Entry> findEntriesWithNotesToBeSubmitted() {
        Timing t = new Timing("findEntriesWithNotesToBeSubmitted", context);
        List<Entry> entries = findAllByQueryString(Entries.NOTE + " IS NOT NULL AND " + Entries.NOTE_SUBMITTED_STATE
                + " = 0");
        t.stop();
        return entries;
    }

    public void removeDeletedNotes() {
        Timing t = new Timing("DB.removeDeletedNotes", context);
        SQLiteDatabase dbase = getDb();
        dbase.beginTransaction();

        dbase.execSQL(context.getString(R.string.sql_remove_deleted_notes));

        dbase.setTransactionSuccessful();
        dbase.endTransaction();
        t.stop();
    }

    public void addFeed2Unsubscribe(String feedAtomId) {
        ContentValues cv = new ContentValues();
        cv.put(UnsubscribeFeeds.FEED_ATOM_ID, feedAtomId);
        getDb().insert(UnsubscribeFeeds.TABLE_NAME, null, cv);
    }

    public void removeFeedFromFeeds2Unsubscribe(String feedAtomId) {
        getDb()
                .delete(UnsubscribeFeeds.TABLE_NAME, UnsubscribeFeeds.FEED_ATOM_ID + " = ?",
                        new String[] { feedAtomId });
    }

}