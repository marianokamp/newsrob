package com.newsrob.test.unit.ui;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.ActivityInstrumentationTestCase2;

import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.activities.ArticleListActivity;
import com.newsrob.jobs.ModelUpdateResult;

public class MarkReadUntilHereUITests extends
		ActivityInstrumentationTestCase2<ArticleListActivity> {

	private EntryManager entryManager;

	public MarkReadUntilHereUITests() {
		super(ArticleListActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(false);

		entryManager = EntryManager.getInstance(getActivity());

		entryManager.doClearCache();

		setMarkAllReadConfirmationThreshold(50);

		for (String atomId : new String[] { "a1", "a2", "a3", "a4", "a5" }) {
			Entry e = new Entry();
			e.setAtomId(atomId);
			e.setFeedTitle("feed_title " + atomId);
			e.setFeedAtomId("feed_atom_id " + atomId);
			e.setTitle("title " + atomId);
			e.setUpdated(new Date().getTime() * 1000);

			entryManager.insert(e);
		}
		entryManager.fireModelUpdated();

		assertEquals(5, entryManager.getUnreadArticleCount());
		assertEquals(5, entryManager.getArticleCount());
	}

	private void setMarkAllReadConfirmationThreshold(int x) {
		entryManager.getSharedPreferences().edit().putString(
				"settings_ui_mark_all_read_confirmation_threshold2",
				String.valueOf(x)).commit();
	}

	@Override
	protected void tearDown() throws Exception {
		entryManager.doClearCache();
		super.tearDown();
	}

	private void executeMarkReadUntilHereOnCurrentSelection()
			throws InterruptedException {
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

		getInstrumentation().invokeContextMenuAction(getActivity(), 112, 0);
		getInstrumentation().waitForIdleSync();

		assertTrue(latch.await(3, TimeUnit.SECONDS)); // Scream if timeout

	}

	private void clickToggleSortOrder(boolean shouldBeAscending) {
		if (getActivity().getDbQuery().isSortOrderAscending() == shouldBeAscending)
			return;

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				getActivity()
						.findViewById(com.newsrob.R.id.toggle_order_button)
						.performClick();
			}
		});
		getInstrumentation().waitForIdleSync();
		assertEquals(shouldBeAscending, getActivity().getDbQuery()
				.isSortOrderAscending());
	}

	private void doReadUntilHereWithConfirmationDialog(boolean positive)
			throws InterruptedException {
		setMarkAllReadConfirmationThreshold(1);

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

		sendKeys("DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN");
		assertEquals(3, getActivity().getListView().getSelectedItemPosition());

		getInstrumentation().waitForIdleSync();

		getInstrumentation().invokeContextMenuAction(getActivity(), 112, 0);
		getInstrumentation().waitForIdleSync();
		if (positive)
			sendKeys("DPAD_CENTER");
		else
			sendKeys("DPAD_RIGHT DPAD_RIGHT DPAD_CENTER");
		getInstrumentation().waitForIdleSync();

		boolean reachedZero = latch.await(3000, TimeUnit.MILLISECONDS);
		if (positive)
			assertTrue(reachedZero); // Scream if timeout

	}

	/**
	 * As the latch is only counted down when modelUpdated() is called this test
	 * also checks if modelUpdated is called.
	 */
	public void testMarkReadUntilHereAscending() throws InterruptedException {
		clickToggleSortOrder(true);

		sendKeys("DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN");
		assertEquals(3, getActivity().getListView().getSelectedItemPosition());

		executeMarkReadUntilHereOnCurrentSelection();

		assertEquals(1, entryManager.getUnreadArticleCount());
		assertEquals(5, entryManager.getArticleCount());
	}

	public void testMarkReadUntilHereWithConfirmationDialogPositive()
			throws InterruptedException {

		doReadUntilHereWithConfirmationDialog(true);

		assertEquals(1, entryManager.getUnreadArticleCount());
		assertEquals(5, entryManager.getArticleCount());

	}

	public void testMarkReadUntilHereWithConfirmationDialogNegative()
			throws InterruptedException {

		doReadUntilHereWithConfirmationDialog(false);

		assertEquals(5, entryManager.getUnreadArticleCount());
		assertEquals(5, entryManager.getArticleCount());

	}

	public void testMarkReadUntilHereDescending() throws InterruptedException {

		clickToggleSortOrder(false);

		sendKeys("DPAD_DOWN DPAD_DOWN");
		assertEquals(1, getActivity().getListView().getSelectedItemPosition());

		executeMarkReadUntilHereOnCurrentSelection();

		assertEquals(3, entryManager.getUnreadArticleCount());
		assertEquals(5, entryManager.getArticleCount());
	}

}
