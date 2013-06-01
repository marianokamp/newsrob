package com.newsrob.download;

@SuppressWarnings("serial")
public class DownloadCancelledException extends Exception {

	DownloadCancelledException() {
		this("Download was cancelled by the user.");
	}

	DownloadCancelledException(String message) {
		super(message);
	}

}
