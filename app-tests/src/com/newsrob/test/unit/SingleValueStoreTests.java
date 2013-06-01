package com.newsrob.test.unit;

import java.io.File;

import android.content.Context;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;

import com.newsrob.util.SingleValueStore;

public class SingleValueStoreTests extends InstrumentationTestCase {

	private SingleValueStore svs;
	private Context ctx;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ctx = getInstrumentation().getTargetContext();
		svs = new SingleValueStore(ctx);

		for (File f : ctx.getFilesDir().listFiles())
			if (f.getName().startsWith("svs_"))
				System.out.println("Removed " + (f.getName() + f.delete()));
	}

	public void testSaveAndRestoreString() {
		svs.putString("key1", "val1");
		assertEquals("val1", svs.getString("key1"));
	}

	public void testSaveAndRestoreStringWithNewInstance() {
		svs.putString("key2", "hello");
		svs = new SingleValueStore(ctx);
		SystemClock.sleep(300);
		assertEquals("hello", svs.getString("key2"));
	}

	public void testValueNotThere() {
		assertNull(svs.getString("null"));
	}

	public void test1000() {
		svs.putString("key", "value");
		for (int i = 0; i < 1000; i++) {
			assertEquals("value", svs.getString("key"));
		}
	}

	public void testOverwriteValue() {
		svs.putString("key", "value1");
		SystemClock.sleep(300);
		assertEquals("value1", svs.getString("key"));

		svs.putString("key", "value2");
		SystemClock.sleep(300);
		assertEquals("value2", svs.getString("key"));
	}

	public void testLong() {
		svs.putLong("key", 100l);
		assertEquals(Long.valueOf(100l), svs.getLong("key"));
	}

	public void testLongWithDefault() {
		assertEquals(Long.valueOf(-99l), svs.getLong("non-existing-key", -99l));
	}

}
