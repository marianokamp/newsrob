package com.newsrob;

public class SyncAPIException extends Exception {
	
	private static final long serialVersionUID = -4038203280616398790L;

	public SyncAPIException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public SyncAPIException(String message) {
		super(message);
	}

}
