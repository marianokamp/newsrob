package com.newsrob.jobs;

public class SynchronizeModelFailed extends ModelUpdateFailed {
	public SynchronizeModelFailed(Throwable rootCause) {
		super(rootCause);
	}

	@Override
	public
	String getMessage() {
		return String.format("Synchronize Model failed: %s: %s", getRootCause().getClass().getName(),
				getRootCause().getMessage());
	}
}
