package com.newsrob.test.unit;

import java.util.Date;
import java.util.List;

import android.test.InstrumentationTestCase;

import com.newsrob.ArticleDbState;
import com.newsrob.EntriesRetriever;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Label;
import com.newsrob.ReadState;
import com.newsrob.download.NewsRobHttpClient;
import com.newsrob.test.TestUtil;
import com.newsrob.util.U;

public class DifferentialUpdateTests extends InstrumentationTestCase {

	private EntryManager entryManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());
		entryManager.doClearCache();

	}

	private void createTestDataSet1() {

		Label l1 = new Label();
		l1.setName("l1");
		Label l2 = new Label();
		l2.setName("l2");

		// 1st article
		Entry e = new Entry();
		e.setAtomId(getAtomIdForArticle(1));

		e.setReadState(ReadState.READ);
		e.setReadStatePending(false);

		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id");
		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		entryManager.insert(e);

		// 2nd article
		e = new Entry();
		e.setAtomId(getAtomIdForArticle(2));

		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id");

		e.setReadState(ReadState.UNREAD);

		e.setReadStatePending(false);

		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		entryManager.insert(e);

		// 3rd article
		e = new Entry();
		e.setAtomId(getAtomIdForArticle(3));

		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id");

		e.setReadState(ReadState.PINNED);
		e.setReadStatePending(false);

		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		entryManager.insert(e);

	}

	private String getAtomIdForArticle(int no) {
		return EntriesRetriever.TAG_GR_ITEM + U.longToHex(no);
	}

	private Entry findEntryNotLikedNotLikePending() {
		return entryManager.findEntryByAtomId("atomid");
	}

	private Entry findEntryLikedNotLikePending() {
		return entryManager.findEntryByAtomId("atomid2");
	}

	public void testDifferentialUpdateRead1() throws Exception {
		createTestDataSet1();
		// One pinned, one unread, one read

		assertEquals(3, entryManager.getArticleCount());
		assertEquals(2, entryManager.getUnreadArticleCount());
		assertEquals(1, entryManager.getUnreadArticleCountExcludingPinned());

		// do
		final EntriesRetriever entriesRetriever = getEntriesRetriever(new String[] { getAtomIdForArticle(1) });

		// First article should now be unread,
		// 2nd article should now be read,
		// 3rd article should now be read.

		entriesRetriever.differentialUpdateOfArticlesStates(entryManager,
				TestUtil.getDummyJob(entryManager),
				EntriesRetriever.GOOGLE_STATE_READING_LIST,
				EntriesRetriever.GOOGLE_STATE_READ, ArticleDbState.READ);

		assertEquals(3, entryManager.getArticleCount()); // Unchanged
		assertEquals(1, entryManager.getUnreadArticleCount());
		assertEquals(1, entryManager.getUnreadArticleCountExcludingPinned());

		Entry firstArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(1));
		assertEquals(ReadState.UNREAD, firstArticle.getReadState());
		assertFalse(firstArticle.isReadStatePending());

		Entry secondArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(2));
		assertEquals(ReadState.READ, secondArticle.getReadState());
		assertFalse(secondArticle.isReadStatePending());

		Entry thirdArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(3));
		assertEquals(ReadState.READ, thirdArticle.getReadState());
		assertFalse(thirdArticle.isReadStatePending());

	}
	
	
	/** Is the pinned state preserved */
	public void testDifferentialUpdateRead2() throws Exception {
		createTestDataSet1();
		// One pinned, one unread, one read

		assertEquals(3, entryManager.getArticleCount());
		assertEquals(2, entryManager.getUnreadArticleCount());
		assertEquals(1, entryManager.getUnreadArticleCountExcludingPinned());

		// do
		final EntriesRetriever entriesRetriever = getEntriesRetriever(new String[] { getAtomIdForArticle(3) });

		// First article should still be read,
		// 2nd article should now be read,
		// 3rd article should still be unread and pinned.

		entriesRetriever.differentialUpdateOfArticlesStates(entryManager,
				TestUtil.getDummyJob(entryManager),
				EntriesRetriever.GOOGLE_STATE_READING_LIST,
				EntriesRetriever.GOOGLE_STATE_READ, ArticleDbState.READ);

		assertEquals(3, entryManager.getArticleCount()); // Unchanged
		assertEquals(1, entryManager.getUnreadArticleCount());
		assertEquals(0, entryManager.getUnreadArticleCountExcludingPinned());

		Entry firstArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(1));
		assertEquals(ReadState.READ, firstArticle.getReadState());
		assertFalse(firstArticle.isReadStatePending());

		Entry secondArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(2));
		assertEquals(ReadState.READ, secondArticle.getReadState());
		assertFalse(secondArticle.isReadStatePending());

		Entry thirdArticle = entryManager
				.findEntryByAtomId(getAtomIdForArticle(3));
		assertEquals(ReadState.PINNED, thirdArticle.getReadState());
		assertFalse(thirdArticle.isReadStatePending());

	}



	private EntriesRetriever getEntriesRetriever(final String[] atomIds) {
		final long[] ids = new long[atomIds.length];
		for (int i = 0; i < atomIds.length; i++) {
			ids[i] = entryManager.findEntryByAtomId(atomIds[i]).getId();
		}

		return new EntriesRetriever(entryManager.getContext()) {
			@Override
			protected long[] getStreamIDsFromGR(NewsRobHttpClient httpClient,
					List<String> tags, String xt, int max) {
				return ids;
			}
		};

	}

	public void testPendingRecordsAreIgnored() {

	}

}
