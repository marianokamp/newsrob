package com.newsrob.pro;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ShowMarketInformationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isFreeVersionInstalled = false;
		try {
			isFreeVersionInstalled = (getPackageManager().getPackageInfo(
					"com.newsrob", 0)) != null;
		} catch (Throwable t) {
			//
		}

		setContentView(R.layout.show_market_information);
		findViewById(R.id.all_done).setVisibility(
				isFreeVersionInstalled ? View.VISIBLE : View.GONE);
		findViewById(R.id.need_to_install_free_version).setVisibility(
				isFreeVersionInstalled ? View.GONE : View.VISIBLE);
		Button b = (Button) findViewById(R.id.close_button);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();

			}
		});

	}
}
