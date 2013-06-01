/**
 * 
 */
package com.newsrob.appwidget;

import android.app.Activity;
import android.database.Cursor;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.newsrob.R;

abstract class UnreadWidgetPrefWizard extends AbstractWizard {

    private RadioGroup scopeRadioGroup;
    private EditText widgetLabelEditText;
    private Spinner labelSpinner;
    private Spinner feedSpinner;
    private RadioGroup startingActivityRadioGroup;

    enum Scope {
        LABEL, FEED, READING_LIST
    };

    enum StartingActivity {
        DASHBOARD, FEEDS, ARTICLES
    }

    UnreadWidgetPrefWizard(Activity owningActivity) {
        super(owningActivity);

        scopeRadioGroup = (RadioGroup) owningActivity.findViewById(R.id.scope);

        widgetLabelEditText = (EditText) owningActivity.findViewById(R.id.widget_label);

        startingActivityRadioGroup = (RadioGroup) owningActivity.findViewById(R.id.starting_activity);

        // feed
        feedSpinner = (Spinner) owningActivity.findViewById(R.id.feed_spinner);

        Cursor c = getEntryManager().getAllFeedsCursor();
        owningActivity.startManagingCursor(c);

        SimpleCursorAdapter sca = new SimpleCursorAdapter(owningActivity, R.layout.text_view, c,
                new String[] { "TITLE" }, new int[] { R.id.text_view });
        feedSpinner.setAdapter(sca);

        // label
        labelSpinner = (Spinner) owningActivity.findViewById(R.id.label_spinner);
        c = getEntryManager().getAllLabelsCursor();
        owningActivity.startManagingCursor(c);

        sca = new SimpleCursorAdapter(owningActivity, R.layout.text_view, c, new String[] { "NAME" },
                new int[] { R.id.text_view });
        labelSpinner.setAdapter(sca);

        // pick scope
        addWizardPage(new AbstractWizardPage() {
            @Override
            void updateState() {
                setEnabled(true);
            }

            @Override
            void onEnter() {

            }
        });

        // pick label
        addWizardPage(new AbstractWizardPage() {
            @Override
            void updateState() {
                setEnabled(R.id.scope_label == scopeRadioGroup.getCheckedRadioButtonId());
            }
        });

        // pick feed
        addWizardPage(new AbstractWizardPage() {
            @Override
            void updateState() {
                setEnabled(R.id.scope_feed == scopeRadioGroup.getCheckedRadioButtonId());
            }
        });

        // select start activity
        addWizardPage(new AbstractWizardPage() {
            @Override
            void updateState() {
                setEnabled(R.id.scope_reading_list == scopeRadioGroup.getCheckedRadioButtonId());
            }
        });

        // enter name
        addWizardPage(new AbstractWizardPage() {
            @Override
            void updateState() {
                setEnabled(true);
            }

            @Override
            void onEnter() {

                if (widgetLabelEditText.getText().length() == 0) {
                    switch (scopeRadioGroup.getCheckedRadioButtonId()) {
                    case R.id.scope_label:
                        widgetLabelEditText.setText(getSelectedLabelName());
                        break;
                    case R.id.scope_feed:
                        widgetLabelEditText.setText(getSelectedFeedName());
                        break;
                    case R.id.scope_reading_list:
                        widgetLabelEditText.setText("All Articles");
                        break;
                    }
                }
            }
        });

    }

    UnreadWidgetPrefWizard.StartingActivity getStartingActivity() {
        if (startingActivityRadioGroup.getCheckedRadioButtonId() == R.id.starting_activity_dashboard)
            return StartingActivity.DASHBOARD;

        if (startingActivityRadioGroup.getCheckedRadioButtonId() == R.id.starting_activity_feed_list)
            return StartingActivity.FEEDS;

        return StartingActivity.ARTICLES;
    }

    String getWidgetLabel() {
        return widgetLabelEditText.getText().toString();
    }

    String getSelectedLabelName() {
        Object o = labelSpinner.getSelectedItem();
        if (o == null)
            return "";
        Cursor c = (Cursor) o;
        return c.getString(1);
    }

    String getSelectedFeedName() {
        Object o = feedSpinner.getSelectedItem();
        if (o == null)
            return "";
        Cursor c = (Cursor) o;
        return c.getString(1);
    }

    Long getSelectedFeedId() {
        Object o = feedSpinner.getSelectedItem();
        Cursor c = (Cursor) o;
        return c.getLong(0);
    }

    UnreadWidgetPrefWizard.Scope getScope() {

        if (scopeRadioGroup.getCheckedRadioButtonId() == R.id.scope_label)
            return Scope.LABEL;

        if (scopeRadioGroup.getCheckedRadioButtonId() == R.id.scope_feed)
            return Scope.FEED;

        if (scopeRadioGroup.getCheckedRadioButtonId() == R.id.scope_reading_list)
            return Scope.READING_LIST;

        throw new RuntimeException("No valid scope was not selected.");
    }
}