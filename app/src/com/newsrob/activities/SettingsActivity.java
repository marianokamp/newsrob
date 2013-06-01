package com.newsrob.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.newsrob.DashboardListActivity;
import com.newsrob.EntryManager;
import com.newsrob.IEntryModelUpdateListener;
import com.newsrob.R;
import com.newsrob.jobs.ModelUpdateResult;
import com.newsrob.preference.ListPreference;
import com.newsrob.util.SDKVersionUtil;

public class SettingsActivity extends PreferenceActivity implements IEntryModelUpdateListener {

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final EntryManager em = EntryManager.getInstance(this);

        addPreferencesFromResource(R.xml.settings);

        getPreferenceScreen().setOnPreferenceChangeListener(em);

        if (SDKVersionUtil.getVersion() < 8)
            disableSetting(em, EntryManager.SETTINGS_PLUGINS, "Froyo+");

        if (SDKVersionUtil.getVersion() < 11) {
            disableSetting(em, EntryManager.SETTINGS_HW_ACCEL_LISTS_ENABLED, "HC+ only");
            disableSetting(em, EntryManager.SETTINGS_HW_ACCEL_ADV_ENABLED, "HC+ only");
        }

        if (!em.isProVersion()) {
            disableSetting(em, EntryManager.SETTINGS_KEEP_STARRED);
            disableSetting(em, EntryManager.SETTINGS_KEEP_SHARED);
            disableSetting(em, EntryManager.SETTINGS_KEEP_NOTES);
            disableSetting(em, EntryManager.SETTINGS_UI_THEME);
            disableSetting(em, EntryManager.SETTINGS_SYNC_NEWSROB_ONLY_ENABLED);
        }

        if (em.shouldHWZoomControlsBeDisabled()) {
            Preference pref = getPreferenceScreen()
                    .findPreference(EntryManager.SETTINGS_HOVERING_ZOOM_CONTROLS_ENABLED);
            if (pref != null) {
                pref.setEnabled(false);
                if (pref.getSummary() != null)
                    pref.setSummary("Disabled until HTC fixes a bug that hurts this function. Sorry.");
            }
        }

        if (em.shouldSyncInProgressNotificationBeDisabled()) {
            Preference pref = getPreferenceScreen().findPreference(EntryManager.SETTINGS_SYNC_IN_PROGRESS_NOTIFICATION);
            if (pref != null) {
                pref.setEnabled(false);
                if (pref.getSummary() != null)
                    pref.setSummary("Disabled until HTC/Dell fixes a bug that hurts this function. Sorry.");
            }
        }

        if (em.shouldActionBarLocationOnlyAllowGone()) {
            ListPreference pref = (ListPreference) getPreferenceScreen().findPreference(
                    EntryManager.SETTINGS_UI_ACTION_BAR_LOCATION);
            if (pref != null) {
                pref.setEnabled(false);
                CharSequence[] seq = pref.getEntries();
                CharSequence[] newSeq = new CharSequence[] { seq[2] };
                pref.setEntries(newSeq);

                getPreferenceScreen().removePreference(pref);
            }
        }

        final Preference p = getPreferenceScreen().findPreference(EntryManager.SETTINGS_USAGE_DATA_PERMISSION_GRANTED);

        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Dialog dialog = DashboardListActivity.createUsageDataCollectionPermissionDialog(em,
                        SettingsActivity.this);
                dialog.show();
                return true;
            }
        });

    }

    private void disableSetting(EntryManager em, String keyOfPref) {
        disableSetting(em, keyOfPref, "PRO");
    }

    private void disableSetting(EntryManager em, String keyOfPref, String reason) {
        Preference pref = getPreferenceScreen().findPreference(keyOfPref);
        if (pref != null) {
            pref.setEnabled(false);
            if (pref.getTitle() != null)
                pref.setTitle(pref.getTitle() + " (" + reason + ")");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EntryManager em = EntryManager.getInstance(this);
        manageModelRelatedSettingsState(em.isModelCurrentlyUpdated());
        em.addListener(this);
    }

    @Override
    protected void onPause() {
        EntryManager.getInstance(this).removeListener(this);
        super.onPause();
    }

    public void modelUpdateFinished(ModelUpdateResult result) {
        manageModelRelatedSettingsState(false);
    }

    public void modelUpdateStarted(boolean fastSyncOnly) {
        manageModelRelatedSettingsState(true);
    }

    public void modelUpdated() {
    }

    private void manageModelRelatedSettingsState(final boolean newState) {
        handler.post(new Runnable() {
            public void run() {
                Preference storageProviderPref = getPreferenceManager().findPreference(
                        EntryManager.SETTINGS_STORAGE_PROVIDER_KEY);
                storageProviderPref.setEnabled(!newState);
            }
        });
    }

    public void statusUpdated() {
    }

    public void modelUpdated(String atomId) {

    }
}
