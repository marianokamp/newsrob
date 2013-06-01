package com.newsrob.activities;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.newsrob.DiscoveredFeed;
import com.newsrob.EntryManager;
import com.newsrob.threetosix.R;
import com.newsrob.ReaderAPIException;

public class SubscribeFeedActivity extends Activity {

    private ListView listView;
    private View progressMonitor;
    private View empty;
    private EditText query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_feed);

        progressMonitor = findViewById(R.id.progress);
        listView = (ListView) findViewById(R.id.discovered_feeds_list);
        empty = findViewById(R.id.empty);
        query = (EditText) findViewById(R.id.query);

        Button searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateSearch();
            }
        });
        query.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                initiateSearch();
                return true;
            }
        });

        // TEXT & SUBJECT are available per se
        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            query.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT));
            searchButton.performClick();
        }

    }

    void initiateSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(query.getWindowToken(), 0);
        new DiscoverFeedsTask().execute(query.getText().toString().replaceAll("\\s", ""));
    }

    private void showProgressMonitor() {
        listView.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        progressMonitor.setVisibility(View.VISIBLE);
    }

    private void showList() {
        progressMonitor.setVisibility(View.GONE);
        if (listView.getCount() == 0)
            empty.setVisibility(View.VISIBLE);
        else {
            listView.setVisibility(View.VISIBLE);
        }
    }

    class SubscribeFeedTask extends AsyncTask<String, Void, Void> {

        private Exception exception;

        @Override
        protected Void doInBackground(String... feedUrls) {

            for (String feedUrl : feedUrls)
                try {
                    EntryManager.getInstance(SubscribeFeedActivity.this).getEntriesRetriever().submitSubscribe(feedUrl);
                } catch (ReaderAPIException e) {
                    exception = e;
                    e.printStackTrace();
                    break;
                }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            showList();

            if (exception != null)
                Toast.makeText(SubscribeFeedActivity.this,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage(), Toast.LENGTH_LONG).show();
            else
                Toast.makeText(SubscribeFeedActivity.this, "Feed subscribed.\nSync/Refresh to fetch articles.",
                        Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showProgressMonitor();
        }

    }

    class DiscoverFeedsTask extends AsyncTask<String, Void, List<DiscoveredFeed>> {

        private Exception exception;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showProgressMonitor();
        }

        @Override
        protected void onPostExecute(final List<DiscoveredFeed> result) {

            super.onPostExecute(result);

            if (result == null) {
                progressMonitor.setVisibility(View.GONE);

                empty.setVisibility(View.VISIBLE);

                if (exception != null)
                    Toast.makeText(SubscribeFeedActivity.this,
                            exception.getClass().getSimpleName() + ": " + exception.getMessage(), Toast.LENGTH_LONG)
                            .show();
            } else {

                ArrayAdapter<DiscoveredFeed> listAdapter = populateList(result);
                listView.setAdapter(listAdapter);

                showList();
            }

        }

        private ArrayAdapter<DiscoveredFeed> populateList(final List<DiscoveredFeed> result) {
            ArrayAdapter<DiscoveredFeed> listAdapter = new ArrayAdapter<DiscoveredFeed>(SubscribeFeedActivity.this,
                    R.layout.discovered_feed, result) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final DiscoveredFeed df = result.get(position);
                    if (convertView == null)
                        convertView = getLayoutInflater().inflate(R.layout.discovered_feed, null);

                    TextView tv = (TextView) convertView.findViewById(R.id.title);
                    SpannableString content = new SpannableString(df.title);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    tv.setText(content);

                    if (df.alternateUrl != null) {
                        tv.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(df.alternateUrl)));
                            }
                        });
                    } else
                        tv.setOnClickListener(null);

                    tv = (TextView) convertView.findViewById(R.id.url);
                    tv.setText(df.feedUrl);

                    tv = (TextView) convertView.findViewById(R.id.snippet);
                    tv.setText(df.snippet != null ? df.snippet : "");
                    tv.setVisibility(df.snippet != null ? View.VISIBLE : View.GONE);

                    Button b = (Button) convertView.findViewById(R.id.subscribe_button);
                    b.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            new SubscribeFeedTask().execute(df.feedUrl);

                        }
                    });

                    return convertView;
                }
            };
            return listAdapter;
        }

        @Override
        protected List<DiscoveredFeed> doInBackground(String... params) {
            List<DiscoveredFeed> result = null;

            try {
                result = EntryManager.getInstance(SubscribeFeedActivity.this).getEntriesRetriever().discoverFeeds(
                        params[0]);
            } catch (Exception e) {
                this.exception = e;
            }
            return result;
        }

    }
}
