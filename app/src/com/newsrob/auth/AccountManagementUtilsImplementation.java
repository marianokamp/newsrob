package com.newsrob.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.newsrob.NewsRob;
import com.newsrob.PL;

public class AccountManagementUtilsImplementation implements IAccountManagementUtils {

    static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String[] GOOGLE_ACCOUNT = new String[] { "google" }; // hosted_or_google
    private static final String AUTH_TOKEN_TYPE_READER = "reader";

    private static final String TAG = "NewsRob Auth";

    public List<String> getAccounts(Context context) {
        Account[] accounts = getAccounts(context, getAccountManager(context));
        List<String> accountNames = new ArrayList<String>(accounts.length);

        for (Account account : accounts)
            accountNames.add(account.name);

        return accountNames;
    }

    AccountManager getAccountManager(Context context) {
        return AccountManager.get(context);
    }

    public String blockingGetAuthToken(Context ctx, String googleAccount) throws OperationCanceledException,
            AuthenticatorException, IOException {

        PL.log("AMU.blockingGetAuthToken for " + googleAccount + ".", ctx);
        AccountManager am = getAccountManager(ctx);
        Account selectedAccount = findAccountByGoogleAccountId(ctx, am, googleAccount);
        String token = am.blockingGetAuthToken(selectedAccount, AUTH_TOKEN_TYPE_READER, true);
        PL.log("AMU.blockingGetAuthToken for " + googleAccount + "(2) returns " + token + ".", ctx);

        return token;

    }

    public void getAuthToken(final Activity waitingActivity, Handler handler, final IAuthenticationCallback callback,
            String googleAccount) {

        PL.log("AMU.getAuthToken for " + googleAccount + ".", waitingActivity);
        AccountManager accountManager = AccountManager.get(waitingActivity);

        // find the selected account
        Account selectedAccount = findAccountByGoogleAccountId(waitingActivity, accountManager, googleAccount);

        if (selectedAccount == null)
            throw new RuntimeException("No account found for " + googleAccount + ".");

        final Account selAccount = selectedAccount;
        // do the authentication

        // hosted
        // google_or_hosted
        // legacy
        // prefer_hosted
        // google_prefer_hosted
        // legacy_hosted
        // require_google
        // google_or_legacy_hosted
        // "google", "legacy_hosted"

        // google/google works -> mariano.kamp, androidnewsreader
        // legacy_hosted_or_google

        if (false)
            accountManager.getAuthTokenByFeatures(GOOGLE_ACCOUNT_TYPE, "reader", GOOGLE_ACCOUNT, waitingActivity,
                    new Bundle(), null, new AccountManagerCallback<Bundle>() {

                        public void run(android.accounts.AccountManagerFuture<Bundle> f) {
                            try {
                                Bundle b = f.getResult();

                                for (String key : b.keySet())
                                    System.out.println("key " + key + " value " + b.getByte(key));
                                String authToken = f.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                                if (authToken == null)
                                    throw new RuntimeException("AuthToken was null.");
                                callback.onAuthTokenReceived(selAccount.name, authToken);
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (e instanceof OperationCanceledException)
                                    e = new Exception("Operation canceled by user.");
                                callback.onError(e);
                            }
                        }
                    }, handler);

        if (true)
            accountManager.getAuthToken(selectedAccount, AUTH_TOKEN_TYPE_READER, null, waitingActivity,
                    new AccountManagerCallback<Bundle>() {

                        public void run(android.accounts.AccountManagerFuture<Bundle> f) {
                            try {

                                String authToken = f.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                                if (authToken == null)
                                    throw new RuntimeException("AuthToken was null.");
                                PL.log("AMU.getAuthToken for " + selAccount.name + "received token="
                                        + authToken.substring(0, 4) + "(2).", waitingActivity);

                                callback.onAuthTokenReceived(selAccount.name, authToken);
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (e instanceof OperationCanceledException)
                                    e = new Exception("Operation canceled by user.");
                                callback.onError(e);
                            }
                        }
                    }, handler);
    }

    public boolean supportsGoogleAuth(Context context) {
        for (AuthenticatorDescription ad : getAccountManager(context).getAuthenticatorTypes()) {
            if (GOOGLE_ACCOUNT_TYPE.equals(ad.type))
                return true;
        }
        return false;
    }

    @Override
    public void invalidateAuthToken(Context context, String authToken) {
        Log.d(TAG, "Token invalidated.");
        PL.log("AMU.invalidateAuthToken", context);
        getAccountManager(context).invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, authToken);
    }

    public void addAccount(Activity owningActivity) {
        Log.d(TAG, "addAccount() called.");
        try {

            getAccountManager(owningActivity).addAccount(GOOGLE_ACCOUNT_TYPE, null, null, null, owningActivity, null,
                    null);
            // Intent i = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            // i.putExtra("EXTRA_AUTHORITIES", GOOGLE_ACCOUNT);
            // owningActivity.startActivity(i);
            // b =
            // getAccountManager(owningActivity).addAccount(GOOGLE_ACCOUNT_TYPE,
            // AUTH_TOKEN_TYPE_READER,
            // GOOGLE_ACCOUNT, null, owningActivity, null, null).getResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    Account findAccountByGoogleAccountId(Context context, AccountManager accountManager, String googleAccount) {

        Account[] accounts = getAccounts(context, accountManager);

        Account foundAccount = null;

        for (Account account : accounts) {
            if (account.name != null && account.name.equals(googleAccount)) {
                foundAccount = account;
                // break
            }
        }
        return foundAccount;
    }

    private Account[] getAccounts(Context context, AccountManager accountManager) {
        Account[] accounts = null;

        try {
            accounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
            // accountManager.getAccountsByTypeAndFeatures(GOOGLE_ACCOUNT_TYPE,
            // GOOGLE_ACCOUNT, null, null)
            // .getResult();

            if (false && NewsRob.isDebuggingEnabled(context))
                for (Account account : accounts) {
                    System.out.println("Account======" + account.name);
                }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return accounts;
    }
}
