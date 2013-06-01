package com.newsrob;

public class ReaderAPIException extends Exception {
	
	private static final long serialVersionUID = -4038203280616398790L;

	public ReaderAPIException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public ReaderAPIException(String message) {
		super(message);
	}

}
