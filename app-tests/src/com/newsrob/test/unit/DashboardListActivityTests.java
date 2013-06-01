package com.newsrob.test.unit;

import java.util.Date;

import android.app.Instrumentation.ActivityMonitor;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.newsrob.DashboardListActivity;
import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Label;
import com.newsrob.activities.FeedListActivity;

public class DashboardListActivityTests extends
		ActivityInstrumentationTestCase2<DashboardListActivity> {
	private DashboardListActivity activity;
	private ListView listView;
	private EntryManager entryManager;

	public DashboardListActivityTests() {
		super("com.newsrob.activities", DashboardListActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(false);
		activity = getActivity();
		listView = (ListView) activity.findViewById(android.R.id.list);
		entryManager = EntryManager.getInstance(activity);

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
		e.setAtomId("atomid_2");
		e.setTitle("title2");
		e.addLabel(l1);

		entryManager.insert(e);

		// 3rd article
		e.setAtomId("atomid_3");
		e.setTitle("title3");
		e.setFeedTitle("feed_title2");
		e.setFeedAtomId("feed_atom_id2");
		e.addLabel(l2);

		entryManager.insert(e);
		entryManager.fireModelUpdated();

		System.out.println("Loaded db.");
	}

	@Override
	protected void tearDown() throws Exception {
		entryManager.doClearCache();
		super.tearDown();
	}

	public void testPreconditions() {

	}

	@UiThreadTest
	public void xtestCorrectLabelsAndCounts() {
		// all articles (3), l1 (2), l2 (1)
		assertEquals(3, listView.getCount());

		assertLabelCount(0, "all articles", "3");
		assertLabelCount(1, "l1", "2");
		assertLabelCount(2, "l2", "1");
	}

	@UiThreadTest
	public void xtestLaunchSingleArticleInShowArticleActivity() {
		fail();
	}

	public void xtestLaunchFeedListActivity() {

		String className = FeedListActivity.class.getName();
		System.out.println("className=" + className);
		ActivityMonitor am = new ActivityMonitor(className, null, true);

		sendKeys("DPAD_DOWN DPAD_CENTER"); // "all articles"
		SystemClock.sleep(3000);
		getInstrumentation().waitForIdleSync();

		if (am.getHits() == 0)
			fail("FeedListActitivy not called.");
		
	}

	private void assertLabelCount(int idx, String label, String count) {
		View lv = listView.getChildAt(idx);
		TextView labelTextView = (TextView) lv
				.findViewById(com.newsrob.R.id.item_title);
		TextView countTextView = (TextView) lv
				.findViewById(com.newsrob.R.id.item_count);
		assertEquals(label, labelTextView.getText().toString());
		assertEquals(count, countTextView.getText().toString());
	}

}
