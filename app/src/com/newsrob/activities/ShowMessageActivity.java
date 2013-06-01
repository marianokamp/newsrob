package com.newsrob.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.newsrob.DashboardListActivity;
import com.newsrob.R;

public class ShowMessageActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_message);

		Button closeButton = (Button) findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent i = new Intent(ShowMessageActivity.this, DashboardListActivity.class);
				startActivity(i);
				finish();
			}
		});

		Bundle extras = getIntent().getExtras();

		setTitle(extras.getString("title"));

		TextView body = (TextView) findViewById(R.id.body);
		body.setText(extras.getString("body"));

	}
}
