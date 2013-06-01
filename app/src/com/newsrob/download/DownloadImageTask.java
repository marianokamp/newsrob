package com.newsrob.download;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Callable;

import android.content.Context;

import com.newsrob.PL;
import com.newsrob.jobs.Job;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public class DownloadImageTask implements Callable<Asset> {

    private WebPageDownloadDirector wpdd;
    private NewsRobHttpClient httpClient;
    private Asset asset;
    private long started;
    private URL pageUrl;
    private Job job;
    private Context context;

    DownloadImageTask(Context context, WebPageDownloadDirector wpdd, NewsRobHttpClient httpClient, Job job,
            URL pageUrl, long started, Asset asset) {
        this.context = context;
        this.httpClient = httpClient;
        this.wpdd = wpdd;
        this.job = job;
        this.pageUrl = pageUrl;
        this.asset = asset;
        this.started = started;

        PL.log("DownloadImageTask " + asset.remoteUrl + " as part of " + pageUrl + " created", context);

    }

    @Override
    public Asset call() throws Exception {
        try {
            U.setLowPrio();

            PL.log("DownloadImageTask " + asset.remoteUrl + " started", context);
            Timing t = new Timing("DownloadImageTask " + asset.remoteUrl + " as part of " + pageUrl, context);
            try {
                wpdd.downloadBinaryAsset(httpClient, asset, started, job, pageUrl);
            } catch (URISyntaxException e) {
                asset.exception = e;
            }
            t.stop();
            PL.log("DownloadImageTask " + asset.remoteUrl + " done", context);
        } catch (Exception e) {
            throw e;
        }
        return asset;
    }
}
