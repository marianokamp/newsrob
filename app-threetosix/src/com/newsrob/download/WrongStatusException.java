package com.newsrob.download;

import java.net.URL;

@SuppressWarnings("serial")
public class WrongStatusException extends DownloadException {

	private int statusCode;
	private URL url;

	public WrongStatusException(URL url, int statusCode) {
		super("Wrong Status Code, expected 200, but was " + statusCode);
		this.statusCode = statusCode;
		this.url = url;
	}

	URL getURL() {
		return url;
	}

	int getStatusCode() {
		return statusCode;
	}

}
