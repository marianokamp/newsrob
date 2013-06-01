package com.newsrob.test;

import java.util.Date;

import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.Label;
import com.newsrob.ReadState;
import com.newsrob.jobs.Job;

public class TestUtil {

	public static void setUp(EntryManager entryManager) {
	}

	public static void populateDatabaseWithDataSet1(EntryManager entryManager) {

		Label l1 = new Label();
		l1.setName("l1");
		Label l2 = new Label();
		l2.setName("l2");

		// 1st article
		Entry e = new Entry();
		e.setAtomId("atomid1");
		e.setFeedTitle("feed_title");
		e.setFeedAtomId("feed_atom_id1");
		e.setTitle("title");
		e.setUpdated(new Date(2010, 10, 15).getTime() * 1000);
		e.addLabel(l1);

		entryManager.insert(e);

		// 2nd article --- READ
		e = new Entry();
		e.setAtomId("atomid2");
		e.setFeedTitle("feed_title2");
		e.setFeedAtomId("feed_atom_id2");
		e.setTitle("title2");
		e.setUpdated(new Date(2010, 10, 14).getTime() * 1000);
		e.addLabel(l1);
		e.addLabel(l2);
		e.setReadState(ReadState.READ);
		entryManager.insert(e);

		// 3rd article
		e = new Entry();
		e.setAtomId("atomid3");
		e.setFeedTitle("feed_title1");
		e.setFeedAtomId("feed_atom_id1");
		e.setTitle("title3");
		e.setUpdated(new Date(2010, 10, 16).getTime() * 1000);
		e.addLabel(l2);

		entryManager.insert(e);

		// 4th article
		e = new Entry();
		e.setAtomId("atomid4");
		e.setFeedTitle("feed_title1");
		e.setFeedAtomId("feed_atom_id1");
		e.setTitle("title4");
		e.setUpdated(new Date(2010, 10, 17).getTime() * 1000);
		e.addLabel(l2);

		entryManager.insert(e);

		// 5th article
		e = new Entry();
		e.setAtomId("atomid5");
		e.setFeedTitle("feed_title1");
		e.setFeedAtomId("feed_atom_id1");
		e.setTitle("title5");
		e.setUpdated(new Date(2010, 10, 18).getTime() * 1000);
		e.addLabel(l2);

		entryManager.insert(e);
	}

	public static Job getDummyJob(EntryManager entryManager) {
		return new Job("Dummy", entryManager) {

			@Override
			public void run() throws Throwable {

			}
		};
	}

}
