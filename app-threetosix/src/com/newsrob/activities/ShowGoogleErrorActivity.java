package com.newsrob.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.newsrob.threetosix.R;

public class ShowGoogleErrorActivity extends Activity {
	public static final String KEY_GOOGLE_ERROR_HTML = "google error html";
	public static final String KEY_GOOGLE_ERROR_BASE_URL = "google error base url";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.show_google_error);
		
		String content = getIntent().getExtras().getString(KEY_GOOGLE_ERROR_HTML);
		String baseUrl = getIntent().getExtras().getString(KEY_GOOGLE_ERROR_BASE_URL);
		WebView contentWebView = (WebView) findViewById(R.id.content_web_view);
		contentWebView.loadDataWithBaseURL(baseUrl, content, "text/html","utf-8",null);
		
	}

}
