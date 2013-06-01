package com.newsrob.jobs;

public class SwitchStorageProviderFailed extends ModelUpdateFailed {

	public SwitchStorageProviderFailed(Throwable rootCause) {
		super(rootCause);
	}

	@Override
	public
	String getMessage() {
		return "Switching storage providers failed: " + getRootCause().getMessage() + " "
				+ getRootCause().getClass().getName();
	}
}