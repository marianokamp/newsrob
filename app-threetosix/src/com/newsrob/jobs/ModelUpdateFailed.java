package com.newsrob.jobs;

public abstract class ModelUpdateFailed extends ModelUpdateResult {
	private Throwable rootCause;

	Throwable getRootCause() {
		return rootCause;
	}

	public ModelUpdateFailed(Throwable rootCause) {
		if (rootCause == null)
			throw new IllegalArgumentException("rootCause is mandatory.");
		this.rootCause = rootCause;
	}

}
