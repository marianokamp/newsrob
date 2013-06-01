package com.newsrob.test.unit;

import android.os.SystemClock;
import android.test.InstrumentationTestCase;

import com.newsrob.EntryManager;
import com.newsrob.test.TestUtil;

public class UnsubscribeFeedTests extends InstrumentationTestCase {

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

	public void testUnsubscribeFeed() {
		assertEquals(0, entryManager.getFeeds2UnsubscribeCount());
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		SystemClock.sleep(350);
		assertEquals(1, entryManager.getFeeds2UnsubscribeCount());
	}

	public void testUnsubscribeFeedAgain() {
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		SystemClock.sleep(350);
		assertEquals(1, entryManager.getFeeds2UnsubscribeCount());
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		SystemClock.sleep(350);
		assertEquals(1, entryManager.getFeeds2UnsubscribeCount());
	}

	public void testUnsubscribeNonExistingFeed() {
		entryManager.requestUnsubscribeFeed("non-existing");
		SystemClock.sleep(350);
		assertEquals(0, entryManager.getFeeds2UnsubscribeCount());
	}

	public void testUnsubscribeTwoFeeds() {
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		entryManager.requestUnsubscribeFeed("feed_atom_id2");
		SystemClock.sleep(350);
		assertEquals(2, entryManager.getFeeds2UnsubscribeCount());
	}

	public void testFeedCanBeUnsubscribed() {
		assertTrue(entryManager.canFeedBeUnsubscribed("feed_atom_id1"));
	}

	public void testNonExistingFeedCannotBeUnsubscribed() {
		assertFalse(entryManager.canFeedBeUnsubscribed("non-existing"));
	}

	public void testUnsubscribeFeedCannotBeUnsubscribed() {
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		SystemClock.sleep(350);
		assertFalse(entryManager.canFeedBeUnsubscribed("feed_atom_id1"));
	}

	public void testNumberOfArticlesToBeMarkedAsRead() {
		assertEquals(
				4,
				entryManager
						.getArticleCountThatWouldBeMarkedAsReadWhenFeedWouldBeUnsubscribed("feed_atom_id1"));
	}

	public void testNumberOfArticlesToBeMarkedAsReadForANonExistingFeed() {
		assertEquals(
				-1,
				entryManager
						.getArticleCountThatWouldBeMarkedAsReadWhenFeedWouldBeUnsubscribed("non-existing"));
	}

	public void testNumberOfArticlesToBeMarkedAsReadForAFeedWithOnlyReadArticles() {
		assertEquals(
				0,
				entryManager
						.getArticleCountThatWouldBeMarkedAsReadWhenFeedWouldBeUnsubscribed("feed_atom_id2"));
	}

	public void testAllArticlesOfAFeedShouldBeMarkedAsReadWhenUnsubscribed() {
		assertEquals(4, entryManager.getUnreadArticleCount());
		entryManager.requestUnsubscribeFeed("feed_atom_id1");
		SystemClock.sleep(350);
		assertEquals(0, entryManager.getUnreadArticleCount());
	}

}
