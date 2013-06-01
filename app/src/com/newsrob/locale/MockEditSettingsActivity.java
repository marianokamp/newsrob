package com.newsrob.locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class MockEditSettingsActivity extends Activity {

    private static String SYNC_BLURB = "Synchronize";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new Intent();

        intent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, SYNC_BLURB);

        Bundle bundle = new Bundle();
        bundle.putString(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, SYNC_BLURB);
        intent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);

        setResult(RESULT_OK, intent);
        finish();
    }
}