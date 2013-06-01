package com.newsrob.auth;

import java.io.IOException;
import java.util.List;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;

public interface IAccountManagementUtils {

    public List<String> getAccounts(Context context);

    public void getAuthToken(Activity waitingActivity, Handler handler, final IAuthenticationCallback callback,
            String googleAccount);

    public boolean supportsGoogleAuth(Context context);

    public void invalidateAuthToken(Context context, String authToken);

    public void addAccount(Activity owningActivity); // LATER return value

    public String blockingGetAuthToken(Context ctx, String googleAccount) throws OperationCanceledException,
            AuthenticatorException, IOException;

}