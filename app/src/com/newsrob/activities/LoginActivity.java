package com.newsrob.activities;

import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.newsrob.EntryManager;
import com.newsrob.LoginWithCaptchaRequiredException;
import com.newsrob.R;
import com.newsrob.auth.AccountManagementUtils;
import com.newsrob.auth.IAccountManagementUtils;
import com.newsrob.util.U;

public class LoginActivity extends Activity implements OnClickListener {

    private EntryManager entryManager;

    private EditText emailEditText;
    private EditText passwordEditText;
    private CheckBox shouldRememberPasswordCheckBox;

    private ProgressBar mProgress;

    private EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(this.getApplicationContext());
        return entryManager;

    }

    private Button loginButton;

    private AsyncTask<LoginParameters, Void, Object> runningTask;

    private EditText captchaAnswerEditText;

    private TextView descriptionTextView;

    private WebView captchaImageView;

    private String captchaToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(getEntryManager().getCurrentThemeResourceId());

        getEntryManager().getNewsRobNotificationManager().cancelSyncProblemNotification();

        IAccountManagementUtils accountManagement = null;

        accountManagement = AccountManagementUtils.getAccountManagementUtils(this);

        boolean useClassic = getIntent().hasExtra("USE_CLASSIC") && getIntent().getBooleanExtra("USE_CLASSIC", false);
        if (false && !useClassic && accountManagement != null) {
            Intent i = new Intent(this, AccountListActivity.class);
            startActivity(i);
            finish();
        } else {
            setContentView(R.layout.login);

            // components

            captchaImageView = (WebView) findViewById(R.id.captcha_image);
            descriptionTextView = (TextView) findViewById(R.id.enter_email_password_text);

            /*
             * TODO: This doesn't work, figure it out later. String
             * email_password_text =
             * getResources().getString(R.string.enter_email_password);
             * email_password_text =
             * email_password_text.replaceAll("\\$SERVICE\\$",
             * SyncInterfaceFactory
             * .getSyncInterface(getApplicationContext()).getServiceName());
             * email_password_text =
             * email_password_text.replaceAll("\\$SERVICE_URL\\$",
             * SyncInterfaceFactory
             * .getSyncInterface(getApplicationContext()).getServiceUrl());
             * descriptionTextView.setText("wtf");
             */
            captchaAnswerEditText = (EditText) findViewById(R.id.captcha_answer);

            loginButton = (Button) findViewById(R.id.login);
            emailEditText = (EditText) findViewById(R.id.email);
            passwordEditText = (EditText) findViewById(R.id.password);
            shouldRememberPasswordCheckBox = (CheckBox) findViewById(R.id.remember_password);
            mProgress = (ProgressBar) findViewById(R.id.progress);
            final Collection<TextView> textViews = new ArrayList<TextView>(2);
            textViews.add(emailEditText);
            textViews.add(passwordEditText);

            // listeners
            loginButton.setOnClickListener(this);

            TextWatcher textWatcher = new TextWatcher() {

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void afterTextChanged(Editable s) {
                    loginButton.setEnabled(shouldLoginButtonBeEnabled(textViews));
                }
            };

            loginButton.setEnabled(shouldLoginButtonBeEnabled(textViews));

            emailEditText.addTextChangedListener(textWatcher);
            passwordEditText.addTextChangedListener(textWatcher);

            shouldRememberPasswordCheckBox.setChecked(getEntryManager().shouldRememberPassword());

            if (passwordEditText.getText().length() == 0) {
                String storedPassword = getEntryManager().getStoredPassword();
                if (storedPassword != null && storedPassword.length() > 0)
                    passwordEditText.setText(storedPassword);
            }

            if (emailEditText.getText().length() == 0) {
                String storedEmail = getEntryManager().getEmail();
                emailEditText.setText(storedEmail != null ? storedEmail : "");
                if (emailEditText.getText().length() > 0)
                    passwordEditText.requestFocus();
            }

            configureView(null, null);
        }

    }

    private void configureView(String captchaToken, String captchaUrl) {
        boolean loginExpired = getIntent().getBooleanExtra(EntryManager.EXTRA_LOGIN_EXPIRED, false);

        int descriptionTextId = loginExpired ? R.string.enter_email_password_expired : R.string.enter_email_password;

        this.captchaToken = captchaToken;
        if (captchaToken != null) {
            descriptionTextId = R.string.enter_email_password_captcha;
            captchaImageView.loadUrl(captchaUrl);
            captchaAnswerEditText.requestFocus();
        }

        int captchaViewsVisibility = captchaToken != null ? View.VISIBLE : View.GONE;
        findViewById(R.id.captcha_answer_label).setVisibility(captchaViewsVisibility);
        captchaAnswerEditText.setVisibility(captchaViewsVisibility);
        captchaAnswerEditText.setText("");
        captchaImageView.setVisibility(captchaViewsVisibility);

        descriptionTextView.setText(descriptionTextId);

    }

    private boolean shouldLoginButtonBeEnabled(Collection<TextView> views) {
        for (TextView textView : views) {
            if (textView.getText().length() == 0)
                return false;
        }
        return true;
    }

    public void onClick(View v) {
        final String email = emailEditText.getText().toString();
        final String password = passwordEditText.getText().toString();

        LoginParameters credentials = new LoginParameters();
        credentials.email = email;
        credentials.password = password;

        getEntryManager().setRememberPassword(shouldRememberPasswordCheckBox.isChecked());

        if (shouldRememberPasswordCheckBox.isChecked()) {
            getEntryManager().storePassword(credentials.password);
            getEntryManager().saveEmail(credentials.email);
        }

        runningTask = new LoginTask(getEntryManager()).execute(credentials);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mProgress.getVisibility() == View.VISIBLE) {
            mProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        AsyncTask<LoginParameters, Void, Object> task = runningTask;
        if (task != null)
            task.cancel(false);

    }

    private void onError(Exception ex) {
        if (ex != null)
            ex.printStackTrace();
        if (ex instanceof LoginWithCaptchaRequiredException) {
            LoginWithCaptchaRequiredException e = (LoginWithCaptchaRequiredException) ex;
            configureView(e.getCaptchaToken(), e.getCaptchaUrl());
            return;
        }
        configureView(null, null);

        final AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this).create();
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        }); // i18n
        dialog.setTitle(U.t(LoginActivity.this, R.string.login_error_dialog_title));
        dialog.setMessage(U.t(LoginActivity.this, R.string.login_error_dialog_message) + " " + ex.getMessage() + "/n("
                + ex.getClass() + ")"); // i18n
        try {
            dialog.show();
        } catch (BadTokenException e) {
            //
        }
        // mUsername.setError(getString(R.string.screen_login_error));
    }

    private void hideProgress() {
        if (mProgress.getVisibility() != View.GONE) {
            final Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            mProgress.setVisibility(View.GONE);
            mProgress.startAnimation(fadeOut);
        }
    }

    private void showProgress() {
        if (mProgress.getVisibility() != View.VISIBLE) {
            final Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            mProgress.setVisibility(View.VISIBLE);
            mProgress.startAnimation(fadeIn);
        }
    }

    void onSuccess() {
        onSuccess(getEntryManager(), emailEditText.getText().toString());
        finish();
    }

    static void onSuccess(EntryManager entryManager, String googleAccountUsed) {
        String oldEmail = entryManager.getEmail();
        if (oldEmail != null && oldEmail.indexOf('@') > -1)
            oldEmail = oldEmail.substring(0, oldEmail.indexOf('@'));

        entryManager.saveEmail(googleAccountUsed);
        entryManager.saveLastSuccessfulLogin();
        String newEmail = googleAccountUsed;
        if (newEmail.indexOf('@') > -1)
            newEmail = newEmail.substring(0, newEmail.indexOf('@'));

        if (!newEmail.equals(oldEmail)) {
            Log.d("NewsRobLogin", "Change of google account. Clearing cache.");
            if (entryManager.syncCurrentlyEnabled(false))
                entryManager.requestClearCacheAndSync();
            else
                entryManager.requestClearCache(null);
        } else {
            Log.d("NewsRobLogin", "No change of google account. Skipping clear cache.");
            if (entryManager.syncCurrentlyEnabled(false))
                entryManager.requestSynchronization(false);
        }

    }

    private class LoginTask extends AsyncTask<LoginParameters, Void, Object> {

        private EntryManager entryManager;

        LoginTask(EntryManager em) {
            this.entryManager = em;
        }

        @Override
        protected Object doInBackground(LoginParameters... uc) {
            Object result = null;

            if (uc.length != 1)
                throw new RuntimeException("Need to pass in one instance of UserCredentials.");
            try {

                entryManager.doLogin(entryManager.getContext(), uc[0].email, uc[0].password, uc[0].loginToken,
                        uc[0].loginCaptcha);

            } catch (Exception e) {
                result = e;
            }
            return result;
        }

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            hideProgress();
            runningTask = null;
            captchaToken = null;
            if (result instanceof Exception) {
                onError((Exception) result);
            } else {
                // changed user
                LoginActivity.this.onSuccess();
            }
        }
    }

}

class LoginParameters {
    public String loginCaptcha;
    public String loginToken;
    String email;
    String password;
}
