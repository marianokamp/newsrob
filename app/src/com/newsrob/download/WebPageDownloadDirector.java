package com.newsrob.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;

import com.newsrob.AssetContentProvider;
import com.newsrob.EntryManager;
import com.newsrob.NewsRob;
import com.newsrob.PL;
import com.newsrob.jobs.Job;
import com.newsrob.storage.IStorageAdapter;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public class WebPageDownloadDirector {
    private Map<URL, Asset> assetUrls2download = new HashMap<URL, Asset>(15);

    private URL pageUrl;
    private String id;
    private int assetCounter = 1;
    private EntryManager entryManager;

    private boolean isDetailedLoggingEnabled;

    static final String TAG = WebPageDownloadDirector.class.getSimpleName();
    private IStorageAdapter fileContext;

    private Context context;
    private long started = System.currentTimeMillis();
    static final long PAGE_DOWNLOAD_TIMEOUT_MS = 180 * 1000;

    private static final Pattern PATTERN_LINK_HREF = Pattern.compile(
            "<\\s*?link.*?href.*?[\"']([^\"]*?\\.css).*?[\"'].*?>", Pattern.CASE_INSENSITIVE); // |
    // Pattern.MULTILINE);
    // "<\\s*?link.*?href.*?\"(.*?\\.css)\".*?>"
    private static final Pattern PATTERN_IMG_SRC = Pattern.compile("<\\s*?img[^><]*?src[^><]*?[\"'](.*?)[\"'][^<]*?>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE); // Removed

    // a
    // tailing
    // $
    // "^.*<\\s*?img.*?src.*?\"(.*?)\".*?>.*$"
    // private static final Pattern PATTERN_IMG_SRC =
    // Pattern.compile("<\\s*?img.*?src.*?\"(.*?)\".*?>",
    // Pattern.CASE_INSENSITIVE); // | Pattern.MULTILINE
    private static final Pattern PATTERN_EXTENSION = Pattern.compile("(\\.\\w+).*?$");

    private static final Pattern PATTERN_BACKGROUND_IMG = Pattern.compile(
            "background\\s*?:\\s*?url\\([\"']?(.*?)[\"']?\\)", Pattern.CASE_INSENSITIVE); // |
    // Pattern.MULTILINE
    static final Pattern PATTERN_CHARSET = Pattern.compile(
            "^.*?meta[^>]*?http-equiv[^>]*?Content-Type[^>]*?charset=([a-z0-9-]*).*$", Pattern.CASE_INSENSITIVE
                    | Pattern.MULTILINE);

    private boolean shouldDownloadImagesInParallel;

    public static WebPageDownloadDirector downloadWebPage(String id, URL pageUrl, IStorageAdapter fileContext, Job job,
            String summary, boolean downloadCompleteWebPage, EntryManager entryManager, boolean manualSync)
            throws DownloadException, DownloadTimedOutException, DownloadCancelledException {

        try {
            return new WebPageDownloadDirector(id, pageUrl, fileContext, job, summary, downloadCompleteWebPage,
                    entryManager, manualSync);
        } catch (OutOfMemoryError oome) {
            throw new DownloadException("OutOfMemory when processing " + pageUrl + ".", oome);
        }
    }

    public static int removeAllAssets(IStorageAdapter fileContext, Context context) {
        Timing t = new Timing("RemoveAllAssets", context);
        int noOfDeletedAssets = 0;
        if (fileContext == null)
            throw new IllegalStateException("fileContext cannot be null.");
        fileContext.clear();
        return noOfDeletedAssets;
    }

    // LATER move out to IStorage...
    public static int removeAssetsForId(String id, IStorageAdapter fileContext, Context context) {
        Timing t = new Timing("RemoveAssetsForId " + id, context);
        int noOfDeletedAssets = 0;
        try {

            if (fileContext == null)
                throw new IllegalStateException("fileContext cannot be null.");

            noOfDeletedAssets = fileContext.removeAllAssets(id);
            return noOfDeletedAssets;
        } finally {
            t.stop("RemoveAssetsForId " + id + " (" + noOfDeletedAssets + " assets)");
        }
    }

    private WebPageDownloadDirector(String id, URL pageUrl, IStorageAdapter fileContext, Job job, CharSequence summary,
            boolean downloadCompleteWebPage, EntryManager entryManager, boolean manualSync) throws DownloadException,
            DownloadTimedOutException, DownloadCancelledException {

        this.context = entryManager.getContext();

        this.shouldDownloadImagesInParallel = "1".equals(NewsRob.getDebugProperties(context).getProperty(
                "downloadImagesInParallel", "1"));

        Timing t = null;
        if (isDetailedLoggingEnabled)
            t = new Timing("WPDD: Downloading " + pageUrl, context);

        this.entryManager = entryManager;
        try {
            pageUrl.toURI();
        } catch (URISyntaxException e1) {
            throw new DownloadException("Problem with a URI: " + pageUrl, e1);
        }

        isDetailedLoggingEnabled = "1".equals(NewsRob.getDebugProperties(context).getProperty(
                "webpageDownloadDirector", "0"));
        if (isDetailedLoggingEnabled)
            PL.log("WPDD: Making offline: " + pageUrl, context);
        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(true, context);

        try {
            this.id = id;
            this.pageUrl = pageUrl;
            this.fileContext = fileContext;

            Timing pageProcessingTiming = null;
            if (downloadCompleteWebPage) {

                assertDownloadShouldContinue(manualSync);
                NewsRobHttpClient httpC = NewsRobHttpClient.newInstance(true, context);
                try {

                    Map<String, String> results = Downloader.loadTextFromUrl(httpC, pageUrl, started, job, context);

                    CharSequence pageContent = results.get("content");
                    pageUrl = new URL(results.get("url"));
                    this.pageUrl = pageUrl; // LATER
                    assertDownloadShouldContinue(manualSync);

                    // pageContent =
                    // pageContent.toString().replace("iso-8859-1",
                    // "utf-8");

                    pageProcessingTiming = new Timing("Processing page " + pageUrl, context);

                    pageContent = convertImageTags(pageContent, job);

                    pageContent = convertStyleSheetLinks(pageContent, job);
                    pageContent = convertStyleSheetImageTags(pageUrl, pageContent, job);

                    savePage(pageContent, "x");

                } catch (Exception e) {
                    Log.w(WebPageDownloadDirector.class.getSimpleName(), e);
                    throw e;
                } finally {
                    httpC.close();
                }
            }

            if (summary != null) {

                if (pageProcessingTiming == null)
                    pageProcessingTiming = new Timing("Processing page (summary only) " + pageUrl, context);

                summary = convertImageTags(summary, job);
                summary = convertStyleSheetLinks(summary, job);
                summary = convertStyleSheetImageTags(pageUrl, summary, job);
                if (true)
                    summary = summary;
                else
                    summary = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head><body>"
                            + summary + "</body></html>";
                savePage(summary, "s");
            }
            if (pageProcessingTiming != null)
                pageProcessingTiming.stop();

            downloadAndConvertAllImageTagsInStyleSheets(httpClient, job, manualSync);
            downloadAllImages(httpClient, started, job, pageUrl, manualSync);

        } catch (SocketTimeoutException ste) {
            throw new DownloadTimedOutException();
        } catch (DownloadException de) {
            throw de;
        } catch (DownloadCancelledException dce) {
            throw dce;
        } catch (DownloadTimedOutException dce) {
            throw dce;
        } catch (Exception e) {
            throw new DownloadException("Problem while downloading " + pageUrl + ".", e);
        } finally {
            httpClient.close();
        }
        if (t != null) {
            t.stop();
            Log.d(TAG, "Assets downloaded for " + pageUrl + ": " + assetCounter);
        }

    }

    private void assertDownloadShouldContinue(boolean manualSync) throws DownloadCancelledException {
        if (!entryManager.downloadContentCurrentlyEnabled(manualSync))
            throw new DownloadCancelledException("WiFi no longer available.");
    }

    private void savePage(CharSequence convertedPageContent, String postfix) throws DownloadException {
        final String aId = "a" + id;
        String fileName = aId + "/" + aId + "_" + postfix + ".html";
        try {
            U2.saveTextFile(fileContext.openFileOutput(fileName + "nr"), convertedPageContent);
        } catch (IOException e) {
            throw new DownloadException("Problem during writing of " + fileName + " for page " + pageUrl + ".", e);
        }
    }

    private void downloadAllImages(final NewsRobHttpClient httpClient, final long started, final Job job,
            final URL pageUrl, boolean manualSync) throws DownloadCancelledException, DownloadTimedOutException {

        assertDownloadShouldContinue(manualSync);

        Timing t = new Timing("Downloading all images for " + pageUrl, context);

        final int numberOfThreads = shouldDownloadImagesInParallel && !U.isScreenOn(context) ? 15 : 1;
        final ScheduledExecutorService pool = Executors.newScheduledThreadPool(numberOfThreads);

        PL.log("Instantiated for " + pageUrl + " pool=" + pool, context);

        try {
            int count = 0;
            for (Asset asset : assetUrls2download.values())
                if (Asset.TYPE_IMAGE == asset.type) {
                    DownloadImageTask task = new DownloadImageTask(context, this, httpClient, job, pageUrl, started,
                            asset);
                    pool.schedule(task, count++ * 250, TimeUnit.MILLISECONDS);
                    // pool.submit(task);
                }
            pool.shutdown();

        } finally {

            boolean terminated = false;
            try {
                final long elapsed = (System.currentTimeMillis() - started);
                PL.log("Elapsed for downloading images for " + pageUrl + " =" + elapsed + " ms.", context);
                terminated = pool.awaitTermination(WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS - elapsed,
                        TimeUnit.MILLISECONDS);
                PL.log("Termination of pool for " + pageUrl + " succeeded=" + terminated, context);
            } catch (InterruptedException e) {
                // Ignore
                e.printStackTrace();
            } finally {
                // if (!terminated)
                pool.shutdownNow();
            }
        }

        t.stop();

    }

    private void downloadAndConvertAllImageTagsInStyleSheets(NewsRobHttpClient httpClient, Job job, boolean manualSync)
            throws DownloadException, DownloadCancelledException, IOException, DownloadTimedOutException {
        Collection<Asset> assets2downloadCopy = new ArrayList<Asset>(assetUrls2download.values());

        for (Asset asset : assets2downloadCopy) {
            assertDownloadShouldContinue(manualSync);

            if (Asset.TYPE_STYLESHEET == asset.type) {
                CharSequence content;
                try {

                    if (job.isCancelled())
                        throw new DownloadCancelledException();

                    Thread.yield();
                    Map<String, String> results = Downloader.loadTextFromUrl(httpClient, asset.remoteUrl, started, job,
                            context);
                    // pageUrl = new URL(results.get("url"));
                    content = results.get("content");
                } catch (URISyntaxException e) {
                    // ignore
                    continue;
                } catch (WrongStatusException wsr) {
                    continue;
                }

                content = convertStyleSheetImageTags(asset.remoteUrl, content, job);

                U2.saveTextFile(fileContext.openFileOutput(asset.localName + "nr"), content);
                asset.downloaded = true;
            }
        }

    }

    private CharSequence convertStyleSheetImageTags(URL baseUrl, CharSequence input, Job job)
            throws DownloadCancelledException {
        return convertRemoteToLocalNameAndRegisterAssetForDownload(PATTERN_BACKGROUND_IMG, input, Asset.TYPE_IMAGE,
                baseUrl, job);
    }

    // LATER consolidate text and binary downloads
    // LATER Also a better understanding is needed if
    // I need to take care of the content type from the server?!
    // LATER GZIP für Stylesheets und HTML?
    void downloadBinaryAsset(NewsRobHttpClient httpClient, Asset asset, long started, Job job, URL pageUrl)
            throws URISyntaxException, DownloadTimedOutException {

        if (System.currentTimeMillis() - started > WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS)
            throw new DownloadTimedOutException(pageUrl.toString(), WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS);

        final int BUFFER_SIZE = 8 * 1024;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            Timing t = null;

            HttpGet getAssetRequest = new HttpGet(asset.remoteUrl.toURI());
            getAssetRequest.setHeader("Referer", pageUrl.toExternalForm());
            if (isDetailedLoggingEnabled) {
                PL.log("WPDD: Downloading as part of " + "(" + pageUrl + ") remote:" + asset.remoteUrl.toURI()
                        + " local: " + asset.localName, context);
                t = new Timing("Downloading as part of " + "(" + pageUrl + ") remote:" + asset.remoteUrl.toURI()
                        + " local: " + asset.localName, context);
            }
            HttpResponse response = httpClient.execute(getAssetRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            if (isDetailedLoggingEnabled)
                PL.log("WPDD: HTTP_STATUS_CODE=" + statusCode, context);
            if (statusCode == HttpStatus.SC_OK) {

                bis = new BufferedInputStream(response.getEntity().getContent(), BUFFER_SIZE);
                bos = new BufferedOutputStream(fileContext.openFileOutput(asset.localName + "nr"), BUFFER_SIZE);

                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    if (job.isCancelled())
                        throw new DownloadCancelledException();

                    if (System.currentTimeMillis() - started > WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS)
                        throw new DownloadTimedOutException(pageUrl.toString(),
                                WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS);

                    int noReadBytes = bis.read(buffer);
                    if (noReadBytes == -1)
                        break;
                    bos.write(buffer, 0, noReadBytes);
                    Thread.yield();
                }
                Log.w(TAG, asset.remoteUrl + " did download ok.");
            } else
                Log.w(TAG, asset.remoteUrl + " did not download. Status code=" + statusCode);
            response.getEntity().consumeContent();
            if (isDetailedLoggingEnabled && t != null)
                t.stop();

        } catch (URISyntaxException e) {
            throw e;
        } catch (DownloadTimedOutException e) {
            throw e;
        } catch (Exception e) {
            if (isDetailedLoggingEnabled) {
                PL.log("WPDD: Downloading as part of " + "(" + pageUrl + "):" + asset.remoteUrl.toURI()
                        + " Resulting exception=" + e.getClass().getName() + " " + e.getMessage(), context);
                e.printStackTrace();
            }
            asset.exception = e;
            String path = fileContext.getAbsolutePathForAsset(asset.localName);
            File f = new File(path);

            if (f.exists()) {
                boolean success = f.delete();
                Log.d("DEBUG", "Deleting file " + f + " was successful: " + success);
            }

        } finally {
            try {
                if (bis != null)
                    bis.close();
                if (bos != null)
                    bos.close();
            } catch (IOException e) {
            }
        }
        asset.downloaded = true;

    }

    private CharSequence convertStyleSheetLinks(CharSequence input, Job job) throws DownloadCancelledException {
        return convertRemoteToLocalNameAndRegisterAssetForDownload(PATTERN_LINK_HREF, input, Asset.TYPE_STYLESHEET, job);
    }

    private CharSequence convertRemoteToLocalNameAndRegisterAssetForDownload(final Pattern p, final CharSequence input,
            int assetType, Job job) throws DownloadCancelledException {
        return convertRemoteToLocalNameAndRegisterAssetForDownload(p, input, assetType, pageUrl, job);
    }

    private CharSequence convertRemoteToLocalNameAndRegisterAssetForDownload(final Pattern p, final CharSequence input,
            int assetType, URL baseUrl, Job job) throws DownloadCancelledException {

        StringBuffer result = new StringBuffer();

        Matcher m = p.matcher(input);

        while (m.find()) {
            if (job.isCancelled())
                throw new DownloadCancelledException();

            String tag = decodeString(m.group());
            String assetUrl = decodeString(m.group(1));

            if (tag != null && assetUrl != null && assetUrl.length() > 0) {
                try {
                    Thread.yield();// LATER
                    String newName = AssetContentProvider.CONTENT_URI + "/"
                            + translateAndRegisterAssetLocation(assetUrl, baseUrl, assetType);
                    String replaced = tag.replace(assetUrl, newName);
                    /*
                     * if (isDetailedLoggingEnabled) PL.log("WPDD: assetUrl=" +
                     * assetUrl + "\n      tag=" + tag + "\n      replaced=" +
                     * replaced + "\n      newName=" + newName);
                     */
                    m.appendReplacement(result, Matcher.quoteReplacement(replaced));

                } catch (MalformedURLException e) {
                    // Ignoring malformed asset urls
                    System.err.println("Ooops. Malformed URL " + e);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    aioobe.printStackTrace();
                    continue;
                    // throw aioobe;
                }
            }
        }
        m.appendTail(result);

        return result;

    }

    private static final String decodeString(String input) {
        String tag = HtmlEntitiesDecoder.decodeString(input);
        // tag = tag.replaceAll("%3A", ":");
        // tag = tag.replaceAll("%2F", "/");
        return tag;
    }

    private CharSequence convertImageTags(CharSequence input, Job job) throws DownloadCancelledException {
        return convertRemoteToLocalNameAndRegisterAssetForDownload(PATTERN_IMG_SRC, input, Asset.TYPE_IMAGE, job);
    }

    private CharSequence translateAndRegisterAssetLocation(final String assetUrl, URL baseUrl, int assetType)
            throws MalformedURLException {

        // avoid duplicated by checking if this
        // asset is already known

        URL remoteUrl = new URL(baseUrl, assetUrl); // make rurl fully qualified

        Asset asset = assetUrls2download.get(remoteUrl);

        if (asset == null) {

            asset = new Asset();
            asset.remoteUrl = remoteUrl;
            asset.type = assetType;
            String aId = "a" + id.replace('/', '_');

            String ad = "";
            String url = asset.remoteUrl.toString().toLowerCase();

            if ((url.indexOf("www.readability.com/media/images") > -1)
                    || (url.indexOf("ad") > -1 && url.replace("gadget", "gatget").replace("load", "loat")
                            .replace("pad", "pat").replace("adobe", "atobe").replace("add", "att")
                            .replace("ead", "eat").indexOf("ad") > -1))
                ad = "_ad";
            asset.localName = aId + "/" + aId + "_" + assetCounter++ + ad;

            Matcher extensionMatch = PATTERN_EXTENSION.matcher(assetUrl);
            if (extensionMatch != null && extensionMatch.matches()) {
                String extension = extensionMatch.group(1);
                asset.localName += extension;
            }

            assetUrls2download.put(remoteUrl, asset);
        }
        return asset.localName;
    }
}

class Downloader {
    static Map<String, String> loadTextFromUrl(NewsRobHttpClient httpClient, URL pageUrl, long started, Job job,
            Context context) throws DownloadException, DownloadCancelledException, URISyntaxException, SocketException,
            SocketTimeoutException, DownloadTimedOutException {

        Map<String, String> returnValues = new HashMap<String, String>(2);

        CharSequence result = null;

        HttpResponse response;
        HttpContext localContext = new BasicHttpContext();

        try {
            HttpGet loadRequest = new HttpGet(pageUrl.toURI());
            response = httpClient.executeZipped(loadRequest, localContext);

        } catch (IOException e) {
            throw new DownloadException("Problem during download of " + pageUrl + ".", e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK)
            throw new WrongStatusException(pageUrl, statusCode);

        String newUri = extractUriFromHttpContext(localContext);

        if (!pageUrl.toString().equals(newUri)) {
            PL.log("WPDD Downloader: Changed page's url after redirect from " + pageUrl + " to " + newUri + ".",
                    context);
            try {
                pageUrl = new URL(newUri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                // keep the existing pageUrl
            }

        }

        try {
            String charsetName = null;
            for (HeaderElement he : response.getEntity().getContentType().getElements()) {
                NameValuePair nvp = he.getParameterByName("charset");
                if (nvp != null) {
                    charsetName = nvp.getValue();
                    break;
                }
            }
            result = U2.readInputStreamIntoString(NewsRobHttpClient.getUngzippedContent(response.getEntity(), context),
                    charsetName, started, job);
            response.getEntity().consumeContent();
        } catch (IOException e) {
            throw new DownloadException("Problem during reading of InputStream when loading " + pageUrl + ".", e);
        }

        returnValues.put("url", pageUrl.toExternalForm());
        returnValues.put("content", result.toString());
        return returnValues;
    }

    private static String extractUriFromHttpContext(HttpContext localContext) {
        String newHost = ((HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST)).toURI();
        String path = ((HttpRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST)).getRequestLine()
                .getUri();
        String newUri = newHost + path;
        return newUri;
    }
}

class U2 {

    static CharSequence readInputStreamIntoString(InputStream is, String charsetName, long started, Job job)
            throws IOException, DownloadCancelledException, DownloadTimedOutException {
        // LATER throw an Exception when the buffer get's to big

        // Matcher m = WebPageDownloadDirector.PATTERN_CHARSET.matcher(result);
        // if (m.find()) {
        // String tag = m.group();
        // String charsetValue = m.group(1);
        // m.appendReplacement(rv, tag.replace(charsetValue, "UTF-8"));
        // }
        // m.appendTail(rv);

        StringBuilder result = new StringBuilder();
        Charset charset = Charset.forName("UTF-8"); // Used to be ISO-8859-1
        if (charsetName != null)
            try {
                charset = Charset.forName(charsetName);
            } catch (Exception e) {
                // stick with the default
            }
        InputStreamReader isr = charset != null ? new InputStreamReader(is, charset) : new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr, 8 * 1024);
        while (true) {

            if (System.currentTimeMillis() - started > WebPageDownloadDirector.PAGE_DOWNLOAD_TIMEOUT_MS)
                throw new DownloadTimedOutException();

            if (job.isCancelled())
                throw new DownloadCancelledException();

            String line = br.readLine();

            if (line != null) {
                Matcher m = WebPageDownloadDirector.PATTERN_CHARSET.matcher(line);
                if (m.find()) {
                    StringBuffer sb = new StringBuffer();
                    String tag = m.group();
                    String charsetValue = m.group(1);
                    try {
                        if (!charsetValue.toLowerCase().equals("utf-8")) {
                            m.appendReplacement(sb, Matcher.quoteReplacement(tag.replace(charsetValue, "UTF-8")));
                            m.appendTail(sb);
                            line = sb.toString();
                        }
                    } catch (ArrayIndexOutOfBoundsException aioobe) {
                        Log.e(WebPageDownloadDirector.TAG, "Ooh. ArrayIndexOutOfBoundsException", aioobe);
                    }
                }
                result.append(line + "\n");
            } else
                break;
        }
        br.close();
        // Timing t = new Timing("replacing charset");

        // StringBuffer rv = new StringBuffer();
        // Matcher m = WebPageDownloadDirector.PATTERN_CHARSET.matcher(result);
        // if (m.find()) {
        // String tag = m.group();
        // String charsetValue = m.group(1);
        // m.appendReplacement(rv, tag.replace(charsetValue, "UTF-8"));
        // }
        // m.appendTail(rv);
        // t.stop();
        return result;
    }

    static void saveTextFile(OutputStream os, CharSequence content) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(os));
            pw.print(content);
        } finally {
            if (pw != null)
                pw.close();
        }
    }
}

class Asset {
    static final int TYPE_UNDEFINED = 0;
    static final int TYPE_IMAGE = 1;
    static final int TYPE_STYLESHEET = 2;

    String localName;
    URL remoteUrl;
    boolean downloaded;
    int type;
    Exception exception;

    @Override
    public String toString() {
        return remoteUrl + " -> " + localName;
    }
}