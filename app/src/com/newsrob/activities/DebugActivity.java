package com.newsrob.activities;

import android.app.Activity;
import android.os.Bundle;

public class DebugActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getIntent().getExtras();
		for (String key : b.keySet()) {
			System.out.println("key=" + key + " val=" + b.get(key));
		}
		finish();

	}

}
