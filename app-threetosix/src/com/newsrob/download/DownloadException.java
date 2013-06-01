package com.newsrob.download;

@SuppressWarnings("serial")
public class DownloadException extends Exception {
	DownloadException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
	DownloadException(String message) {
		super(message);
	}
}
