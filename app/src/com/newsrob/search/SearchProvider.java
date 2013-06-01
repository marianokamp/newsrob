package com.newsrob.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

import com.newsrob.EntryManager;

public class SearchProvider extends ContentProvider {

	public static String AUTHORITY = "articles";

	private static final int SEARCH_SUGGEST = 0;
	private static final int SHORTCUT_REFRESH = 1;
	private static final UriMatcher sURIMatcher = buildUriMatcher();

	public static final String[] COLUMNS = {
			"_id", // must include this column
			SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_INTENT_DATA, };

	/**
	 * Sets up a uri matcher.
	 */
	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		return matcher;
	}

	private EntryManager entryManager;

	// LATER Remove me?
	@Override
	public boolean onCreate() {
		this.entryManager = EntryManager.getInstance(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (!TextUtils.isEmpty(selection)) {
			throw new IllegalArgumentException("selection not allowed for " + uri);
		}
		if (selectionArgs != null && selectionArgs.length != 0) {
			throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
		}
		if (!TextUtils.isEmpty(sortOrder)) {
			throw new IllegalArgumentException("sortOrder not allowed for " + uri);
		}

		switch (sURIMatcher.match(uri)) {
		case SEARCH_SUGGEST:
			String query = null;
			if (uri.getPathSegments().size() > 1) {
				query = uri.getLastPathSegment().toLowerCase();
			}
			return getSuggestions(query, projection);
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	private Cursor getSuggestions(String query, String[] projection) {
		if (query == null || query.trim().length() < 3)
			return new MatrixCursor(new String[] { "_id", });

		Cursor mc = entryManager.searchFullText(query);
		return mc;
	}

	/**
	 * All queries for this provider are for the search suggestion and shortcut
	 * refresh mime type.
	 */
	@Override
	public String getType(Uri uri) {
		switch (sURIMatcher.match(uri)) {
		case SEARCH_SUGGEST:
			return SearchManager.SUGGEST_MIME_TYPE;
			/*
			 * case SHORTCUT_REFRESH: return SearchManager.SHORTCUT_MIME_TYPE;
			 */
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}
}
