package com.newsrob.test.unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.test.InstrumentationTestCase;

import com.newsrob.DBQuery;
import com.newsrob.EntryManager;
import com.newsrob.test.TestUtil;

public class DBQueryContentCursorTests extends InstrumentationTestCase {

	private EntryManager entryManager;
	private List<String> atomIdsOrderedAscending = new ArrayList<String>();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());

		entryManager.doClearCache();
		TestUtil.populateDatabaseWithDataSet1(entryManager);
		atomIdsOrderedAscending.addAll(Arrays.asList(new String[] { "atomid2",
				"atomid1", "atomid3", "atomid4", "atomid5" }));
	}

	@Override
	protected void tearDown() throws Exception {
		entryManager.doClearCache();
		super.tearDown();
	}

	private void executeAndVerify(DBQuery dbq, List<String> expectedIds) {

		Cursor c = entryManager.getContentCursor(dbq);

		assertEquals(expectedIds.size(), c.getCount());

		try {
			for (int i = 0; i < expectedIds.size(); i++) {
				assertTrue(c.moveToNext());
				String expected = expectedIds.get(i);
				String actual = c.getString(1);
				assertEquals(expected, actual);
			}
		} finally {
			c.close();
		}
	}

	public void testContentCursorOrderAscending() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);

		List<String> expectedIds = atomIdsOrderedAscending;

		executeAndVerify(dbq, expectedIds);
	}

	public void testContentCursorOrderDescending() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(false);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);

		List<String> expectedIds = new ArrayList<String>(
				atomIdsOrderedAscending.size());
		for (String s : atomIdsOrderedAscending)
			expectedIds.add(0, s);

		executeAndVerify(dbq, expectedIds);
	}

	public void testContentCursorOrderAscendingWithLimit() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);
		dbq.setLimit(3);

		List<String> expectedIds = atomIdsOrderedAscending;
		expectedIds = expectedIds.subList(0, 3);

		executeAndVerify(dbq, expectedIds);
	}

	public void testContentCursorOrderDescendingWithLimit() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(false);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);
		dbq.setLimit(1);

		List<String> expectedIds = new ArrayList<String>(
				atomIdsOrderedAscending.size());
		for (String s : atomIdsOrderedAscending)
			expectedIds.add(0, s);
		expectedIds = expectedIds.subList(0, 1);

		executeAndVerify(dbq, expectedIds);
	}

	public void testContentCursorUnreadCount() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);

		assertEquals(4, entryManager.getContentCount(dbq));
	}

	public void testContentCursorCount() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);

		assertEquals(5, entryManager.getContentCount(dbq));

	}

}
