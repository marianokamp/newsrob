package com.newsrob.test.unit;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;

import com.newsrob.DBQuery;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.ReadState;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.test.TestUtil;

public class PinTests extends InstrumentationTestCase {

	private EntryManager entryManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());

		entryManager.doClearCache();

	}

	@Override
	protected void tearDown() throws Exception {
		entryManager.doClearCache();
		super.tearDown();
	}

	private Entry createArticleWithReadState(ReadState desiredReadState) {
		Entry e = new Entry();
		e.setAtomId("atomid1");
		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id1");
		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		e.setReadState(desiredReadState);

		entryManager.insert(e);

		return entryManager.findEntryByAtomId("atomid1");
	}

	/**
	 * In this test only it is also validated that the in-memory and on-disk
	 * version of the article are updated. In the other tests only the in-memory
	 * version is validated as this test's subject are the state transitions.
	 * 
	 * Also this test contains increasing the read level and then increasing the
	 * unread level again.
	 */
	public void testIncreaseReadLevelFromPinned() {
		Entry article = createArticleWithReadState(ReadState.PINNED);
		assertFalse(article.isReadStatePending());

		entryManager.increaseReadLevel(article);
		assertEquals(ReadState.READ, article.getReadState());
		assertTrue(article.isReadStatePending());

		article = entryManager.findEntryByAtomId("atomid1");
		assertEquals(ReadState.READ, article.getReadState());
		assertTrue(article.isReadStatePending());

		// no change when increasing the Read Level again
		entryManager.increaseReadLevel(article);
		assertEquals(ReadState.READ, article.getReadState());
		assertTrue(article.isReadStatePending());

		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.UNREAD, article.getReadState());
		assertFalse(article.isReadStatePending());

		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertFalse(article.isReadStatePending());

	}

	public void testIncreaseReadLevelFromUnread() {
		Entry article = createArticleWithReadState(ReadState.UNREAD);
		entryManager.increaseReadLevel(article);
		assertEquals(ReadState.READ, article.getReadState());
		assertTrue(article.isReadStatePending());

		// no change when increasing the read level again
		entryManager.increaseReadLevel(article);
		assertEquals(ReadState.READ, article.getReadState());
		assertTrue(article.isReadStatePending());

	}

	public void testIncreaseReadLevelFromRead() {
		Entry article = createArticleWithReadState(ReadState.READ);
		entryManager.increaseReadLevel(article);
		assertEquals(ReadState.READ, article.getReadState());
		assertFalse(article.isReadStatePending());
	}

	public void testIncreaseUnreadLevelFromPinned() {
		Entry article = createArticleWithReadState(ReadState.PINNED);
		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertFalse(article.isReadStatePending());
	}

	public void testIncreaseUnreadLevelFromUnread() {
		Entry article = createArticleWithReadState(ReadState.UNREAD);
		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertFalse(article.isReadStatePending());

		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertFalse(article.isReadStatePending());
	}

	public void testIncreaseUnreadLevelFromRead() {
		Entry article = createArticleWithReadState(ReadState.READ);
		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.UNREAD, article.getReadState());
		assertTrue(article.isReadStatePending());

		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertTrue(article.isReadStatePending());

		entryManager.increaseUnreadLevel(article);
		assertEquals(ReadState.PINNED, article.getReadState());
		assertTrue(article.isReadStatePending());
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

	public void testMarkAllReadDoesntMarkPinnedArticlesRead()
			throws InterruptedException {
		TestUtil.populateDatabaseWithDataSet1(entryManager);

		Entry article = entryManager.findEntryByAtomId("atomid5");
		entryManager.increaseUnreadLevel(article);

		article = entryManager.findEntryByAtomId("atomid5");

		assertEquals(ReadState.PINNED, article.getReadState());

		DBQuery dbq = new DBQuery(entryManager, null, null);
		executeMarkAllRead(dbq);
		assertEquals(1, entryManager.getUnreadArticleCount());
	}

}
