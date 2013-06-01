package com.newsrob.locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class MockEditSettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = new Intent();
		intent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, "Synchronize");
		setResult(RESULT_OK, intent);
		finish();
	}
}