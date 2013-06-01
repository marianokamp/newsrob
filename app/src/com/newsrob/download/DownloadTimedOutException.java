package com.newsrob.download;

@SuppressWarnings("serial")
public class DownloadTimedOutException extends Exception {
    DownloadTimedOutException() {
        super("Download timed out.");
    }

    DownloadTimedOutException(String pageUrl, long timeoutMs) {
        super("The download of page " + pageUrl + " timed out after " + (timeoutMs / 1000) + " seconds.");
    }
}
