package com.newsrob.test.unit;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.newsrob.EntriesRetriever;
import com.newsrob.EntryManager;
import com.newsrob.jobs.Job;

public class ImportTests extends InstrumentationTestCase {

	private EntryManager entryManager;
	private EntriesRetrieverStub entriesRetriever;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		entryManager = EntryManager.getInstance(getInstrumentation()
				.getTargetContext());

		entryManager.doClearCache();
		entriesRetriever = new EntriesRetrieverStub(getInstrumentation()
				.getTargetContext());
	}

	public void testImport() throws IOException, ParserConfigurationException,
			SAXException {
		assertEquals(0, entryManager.getArticleCount());

		Job job = new Job("SomeDescription", entryManager) {
			@Override
			public void run() throws Exception {
				fail("Should not be called.");
			}
		};

		InputStream is = getInstrumentation().getContext().getAssets().open(
				"simple.xml");

		EntriesRetriever.FetchContext fetchContext = new EntriesRetriever.FetchContext();

		entriesRetriever.processInputStream(job, fetchContext, is);

		is.close();

		assertEquals(20, entryManager.getArticleCount());
	}
}

class EntriesRetrieverStub extends EntriesRetriever {

	public EntriesRetrieverStub(Context context) {
		super(context);
	}

	@Override
	protected void processInputStream(Job job, FetchContext fetchCtx,
			InputStream is) throws ParserConfigurationException, SAXException,
			IOException {
		super.processInputStream(job, fetchCtx, is);
	}
}
