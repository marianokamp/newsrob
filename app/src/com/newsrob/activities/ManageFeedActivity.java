package com.newsrob.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.newsrob.EntryManager;
import com.newsrob.Feed;
import com.newsrob.R;

public class ManageFeedActivity extends Activity {
    public static final String EXTRA_FEED_ID = "extra_feed_id";

    static final int DOWNLOAD_PREF_DEFAULT = 0;
    static final int DOWNLOAD_PREF_DONT_DOWNLOAD = 1;
    static final int DOWNLOAD_PREF_FEED_ONLY = 2;
    static final int DOWNLOAD_PREF_FEED_AND_WEBPAGE = 3;

    static final int DISPLAY_PREF_DEFAULT = 0;
    static final int DISPLAY_PREF_FEED = 1;
    static final int DISPLAY_PREF_WEBPAGE = 2;

    private Feed feed;

    private Spinner downloadTypeSpinner;

    private Spinner displayTypeSpinner;

    private CheckBox notificationEnabledCheckBox;
    private CheckBox javaScriptEnabledCheckBox;
    private CheckBox ftwEnabledCheckBox;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_feed);

        long feedId = getIntent().getExtras().getLong(EXTRA_FEED_ID);
        if (feedId == 0l)
            finish();

        feed = EntryManager.getInstance(this).findFeedById(feedId);
        if (feed == null)
            finish();

        TextView feedTitle = (TextView) findViewById(R.id.feed_title);
        feedTitle.setText(feed.getTitle());

        // download type
        downloadTypeSpinner = (Spinner) findViewById(R.id.download_type_spinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.download_prefs,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        downloadTypeSpinner.setAdapter(adapter);

        downloadTypeSpinner.setSelection(feed.getDownloadPref());

        // display type
        displayTypeSpinner = (Spinner) findViewById(R.id.display_type_spinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.display_prefs, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        displayTypeSpinner.setAdapter(adapter);

        displayTypeSpinner.setSelection(feed.getDisplayPref());

        notificationEnabledCheckBox = (CheckBox) findViewById(R.id.notification_enabled_checkbox);
        notificationEnabledCheckBox.setChecked(feed.isNotificationEnabled());

        javaScriptEnabledCheckBox = (CheckBox) findViewById(R.id.javascript_enabled_checkbox);
        javaScriptEnabledCheckBox.setChecked(feed.isJavaScriptEnabled());

        ftwEnabledCheckBox = (CheckBox) findViewById(R.id.ftw_enabled_checkbox);
        ftwEnabledCheckBox.setChecked(feed.isFitToWidthEnabled());

        Button closeButton = (Button) findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (feed != null) {

                    feed.setDownloadPref(downloadTypeSpinner.getSelectedItemPosition());
                    feed.setDisplayPref(displayTypeSpinner.getSelectedItemPosition());
                    feed.setNotificationEnabled(notificationEnabledCheckBox.isChecked());
                    feed.setJavaScriptEnabled(javaScriptEnabledCheckBox.isChecked());
                    feed.setFitToWidthEnabled(ftwEnabledCheckBox.isChecked());

                    getEntryManager().updateFeed(feed);
                }
                finish();
            }
        });
    }

    private EntryManager getEntryManager() {
        return EntryManager.getInstance(this);
    }
}
