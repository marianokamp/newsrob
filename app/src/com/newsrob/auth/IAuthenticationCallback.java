package com.newsrob.auth;

public interface IAuthenticationCallback {

	public void onAuthTokenReceived(String googleAccount, String authToken);

	public void onError(Exception e);
}
