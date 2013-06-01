package com.newsrob.test.unit;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;

import com.newsrob.DBQuery;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Feed;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.test.TestUtil;

public class MarkAllReadDbQueryTests extends InstrumentationTestCase {

	private EntryManager entryManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());

		entryManager.doClearCache();
		TestUtil.populateDatabaseWithDataSet1(entryManager);
	}


	@Override
	protected void tearDown() throws Exception {
		entryManager.doClearCache();
		super.tearDown();
	}

	public void testMarkAllReadWithMoreRecordsThanLimit()
			throws InterruptedException {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setLimit(3);
		executeMarkAllRead(dbq);
		assertEquals(1, entryManager.getUnreadArticleCount());
	}

	public void testMarkAllReadWithFewerRecordsThanLimit()
			throws InterruptedException {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setLimit(44);
		executeMarkAllRead(dbq);
		assertEquals(0, entryManager.getUnreadArticleCount());
	}

	public void testMarkAllReadWithSameNoOfRecordsAsLimit()
			throws InterruptedException {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setLimit(4);
		executeMarkAllRead(dbq);
		assertEquals(0, entryManager.getUnreadArticleCount());
	}

	public void testMarkAllReadWithLimitAscending() throws InterruptedException {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setLimit(1);
		executeMarkAllRead(dbq);

		assertEquals(3, entryManager.getUnreadArticleCount());
	}

	public void testMarkAllReadWithLimitDescending()
			throws InterruptedException {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(false);
		dbq.setLimit(1);
		executeMarkAllRead(dbq);

		assertEquals(3, entryManager.getUnreadArticleCount());
	}


	private void executeMarkAllRead(DBQuery dbq) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		entryManager.addListener(new IEntryModelUpdateListener() {

			@Override
			public void statusUpdated() {
			}

			@Override
			public void modelUpdated(String atomId) {
			}

			@Override
			public void modelUpdated() {
				latch.countDown();
			}

			@Override
			public void modelUpdateStarted(boolean fastSyncOnly) {
			}

			@Override
			public void modelUpdateFinished(ModelUpdateResult result) {
			}
		});
		entryManager.requestMarkAllAsRead(dbq);
		latch.await(3, TimeUnit.SECONDS);
	}

	public void testDateLimitSortAscendingDescending() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		dbq.setSortOrderAscending(true);
		dbq.setDateLimit(new Date(2010, 10, 16).getTime() * 1000);

		assertEquals(2, entryManager.getMarkAllReadCount(dbq));

		dbq.setSortOrderAscending(false);
		assertEquals(3, entryManager.getMarkAllReadCount(dbq));

	}

	public void testOnlyUnreadArticlesAreCounted() {
		DBQuery dbq = new DBQuery(entryManager, null, null);
		assertEquals(4, entryManager.getMarkAllReadCount(dbq));
	}

	public void testMarkAllReadIgnoresHideReadItemsPreference() {
		DBQuery dbq = new DBQuery(entryManager, null, null);

		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(true);
		assertEquals(4, entryManager.getMarkAllReadCount(dbq));

		dbq.setShouldHideReadItemsWithoutUpdatingThePreference(false);
		assertEquals(4, entryManager.getMarkAllReadCount(dbq));
	}

	public void testMarkAllReadWithLabel() {
		DBQuery dbq = new DBQuery(entryManager, null, null);

		dbq.setFilterLabel("l1");
		assertEquals(1, entryManager.getMarkAllReadCount(dbq));

		dbq.setFilterLabel("l2");
		assertEquals(3, entryManager.getMarkAllReadCount(dbq));

		dbq.setFilterLabel("l3"); // non-existing
		assertEquals(0, entryManager.getMarkAllReadCount(dbq));
	}

	public void testMarkAllReadWithFeed() {
		Feed f1 = findFeedByArticleAtomId("atomid1");
		Feed f2 = findFeedByArticleAtomId("atomid2");

		DBQuery dbq = new DBQuery(entryManager, null, null);

		dbq.setFilterFeedId(f1.getId());
		assertEquals(4, entryManager.getMarkAllReadCount(dbq));

		// one read and two unread articles in this feed
		dbq.setFilterFeedId(f2.getId());
		assertEquals(0, entryManager.getMarkAllReadCount(dbq));

		// non-existent feed
		dbq.setFilterFeedId(999l);
		assertEquals(0, entryManager.getMarkAllReadCount(dbq));
	}

	public void testIsMarkAllReadPossibleNegative() {
		Feed f2 = findFeedByArticleAtomId("atomid2");
		DBQuery dbq = new DBQuery(entryManager, null, f2.getId());
		assertFalse(entryManager.isMarkAllReadPossible(dbq));
	}

	public void testIsMarkAllReadPossible() {
		Feed f1 = findFeedByArticleAtomId("atomid1");
		DBQuery dbq = new DBQuery(entryManager, null, f1.getId());
		assertTrue(entryManager.isMarkAllReadPossible(dbq));
	}

	public void testIsMarkAllReadPossibleWithLimit() {
		Feed f1 = findFeedByArticleAtomId("atomid1");
		DBQuery dbq = new DBQuery(entryManager, null, f1.getId());
		dbq.setLimit(4);
		assertTrue(entryManager.isMarkAllReadPossible(dbq));
	}

	private Feed findFeedByArticleAtomId(String atomId) {
		Entry e = entryManager.findEntryByAtomId(atomId);
		return entryManager.findFeedById(e.getFeedId());
	}

}
