package com.newsrob.test.unit;

import java.util.Date;

import android.test.InstrumentationTestCase;

import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.Label;
import com.newsrob.jobs.ModelUpdateResult;

public class LikeStateTests extends InstrumentationTestCase {

	private EntryManager entryManager;
	private boolean modelUpdatedCalled;
	private boolean modelUpdatedStringCalled;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());

		Label l1 = new Label();
		l1.setName("l1");
		Label l2 = new Label();
		l2.setName("l2");

		// 1st article
		Entry e = new Entry();
		e.setAtomId("atomid");
		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id");
		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		entryManager.insert(e);

		// 2nd article
		e = new Entry();
		e.setAtomId("atomid2");
		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id");
		e.setTitle("title");
		e.setUpdated(new Date().getTime() * 1000);

		entryManager.insert(e);

		System.out.println("Loaded db.");
	}

	private Entry findEntryNotLikedNotLikePending() {
		return entryManager.findEntryByAtomId("atomid");
	}

	private Entry findEntryLikedNotLikePending() {
		return entryManager.findEntryByAtomId("atomid2");
	}

	public void testFindEntryReturnsNewInstanceOnEveryCall() {
		assertFalse(findEntryNotLikedNotLikePending() == findEntryNotLikedNotLikePending());
	}



	public void testUpdateNotification() {
		entryManager.addListener(new IEntryModelUpdateListener() {

			@Override
			public void statusUpdated() {
			}

			@Override
			public void modelUpdated(String atomId) {
				modelUpdatedStringCalled = true;
			}

			@Override
			public void modelUpdated() {
				modelUpdatedCalled = true;
			}

			@Override
			public void modelUpdateStarted(boolean fastSyncOnly) {
			}

			@Override
			public void modelUpdateFinished(ModelUpdateResult result) {

			}
		});

		Entry e = findEntryNotLikedNotLikePending();

		assertTrue(modelUpdatedCalled);

	}

}
