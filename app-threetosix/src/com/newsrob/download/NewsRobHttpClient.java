package com.newsrob.download;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;

import com.newsrob.NewsRob;
import com.newsrob.PL;

public class NewsRobHttpClient implements HttpClient {

    private static final String TAG = NewsRobHttpClient.class.getSimpleName();

    private static final String PRETEND_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 2.0; en-us; Milestone Build/SHOLS_U2_01.03.1) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17";
    // "Mozilla/5.0 (iPhone; U; CPU iPhone 2_1 like Mac OS X; en)"
    // + " AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2"
    // + " Mobile/5F136 Safari/525.20.1";
    private static final String USER_AGENT = PRETEND_USER_AGENT + " " + "NewsRob (http://newsrob.com) gzip";
    private final HttpClient delegate;
    private RuntimeException mLeakedException = new IllegalStateException("NewsRobHttpClient created and never closed");

    private static Boolean countingEnabled;
    private Context context;

    public static NewsRobHttpClient newInstance(Context ctx) {
        return newInstance(true, ctx);
    }

    public static NewsRobHttpClient newInstance(boolean followRedirects, Context ctx) {
        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setStaleCheckingEnabled(params, true);

        HttpConnectionParams.setConnectionTimeout(params, 45 * 1000);
        HttpConnectionParams.setSoTimeout(params, 45 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        // HttpConnectionParams.setTcpNoDelay(params, true);

        // Don't handle redirects -- return them to the caller. Our code
        // often wants to re-POST after a redirect, which we must do ourselves.
        // HttpClientParams.setRedirecting(params, false);

        HttpClientParams.setRedirecting(params, followRedirects);
        if (followRedirects)
            params.setIntParameter(ClientPNames.MAX_REDIRECTS, 10);

        // Set the specified user agent and register standard protocols.
        HttpProtocolParams.setUserAgent(params, USER_AGENT);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
        // We use a factory method to modify superclass initialization
        // parameters without the funny call-a-static-method dance.
        return new NewsRobHttpClient(manager, params, ctx);
    }

    private NewsRobHttpClient(ClientConnectionManager ccm, HttpParams params, Context ctx) {
        this.context = ctx.getApplicationContext();
        this.delegate = new DefaultHttpClient(ccm, params);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mLeakedException != null) {
            Log.e(TAG, "Leak found", mLeakedException);
            mLeakedException = null;
        }
    }

    /**
     * Release resources associated with this client. You must call this, or
     * significant resources (sockets and memory) may be leaked.
     */
    public void close() {
        if (mLeakedException != null) {
            getConnectionManager().shutdown();
            mLeakedException = null;
        }
    }

    public HttpParams getParams() {
        return delegate.getParams();
    }

    public ClientConnectionManager getConnectionManager() {
        return delegate.getConnectionManager();
    }

    public HttpResponse executeZipped(HttpUriRequest req, HttpContext httpContext) throws IOException {
        modifyRequestToAcceptGzipResponse(req);
        return this.execute(req, httpContext);
    }

    public HttpResponse executeZipped(HttpUriRequest req) throws IOException {
        modifyRequestToAcceptGzipResponse(req);
        return this.execute(req);
    }

    public HttpResponse execute(HttpUriRequest request) throws IOException {
        HttpResponse resp = delegate.execute(request);
        outputResponseDebugInfo(request, resp);
        return resp;
    }

    public HttpResponse execute(HttpUriRequest request, HttpContext httpContext) throws IOException {
        HttpResponse resp = delegate.execute(request, httpContext);
        outputResponseDebugInfo(request, resp);
        return resp;
    }

    private void outputResponseDebugInfo(HttpUriRequest request, HttpResponse resp) throws IOException {
        String status = "-> HTTP STATUS: " + resp.getStatusLine().getStatusCode();
        status += " " + resp.getStatusLine();
        status += " length=" + resp.getEntity().getContentLength();
        if ("1".equals(NewsRob.getDebugProperties(context).getProperty("printCurls", "0"))) {
            PL.log("Curl= " + NewsRobHttpClient.toCurl(request) + status, context);
        } else {
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("NewsRobHttpClient: " + request.getURI() + status, context);
        }
        if (NewsRob.isDebuggingEnabled(context) && resp.getStatusLine().getStatusCode() >= 400) {
            PL.log("Status " + resp.getStatusLine().getStatusCode() + " for " + request.getURI() + ":", context);
            PL.log("  headers=", context);
            for (Header header : resp.getAllHeaders()) {
                PL.log("    " + header.getName() + "=" + header.getValue(), context);
                /*
                 * if (header.getElements().length > 0) for (HeaderElement he :
                 * header.getElements()) { PL.log("      " + he.getName() + "="
                 * + he.getValue(), context); if (he.getParameters().length > 0)
                 * for (NameValuePair nvp : he.getParameters())
                 * PL.log("        " + nvp.getName() + "=" + nvp.getValue(),
                 * context);
                 * 
                 * }
                 */
            }

            if ("1".equals(NewsRob.getDebugProperties(context).getProperty("dumpPayload", "0")))
                PL.log("  Payload=" + EntityUtils.toString(resp.getEntity()), context);
        }
    }

    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        return delegate.execute(target, request);
    }

    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return delegate.execute(target, request, context);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException,
            ClientProtocolException {
        return delegate.execute(request, responseHandler);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context)
            throws IOException, ClientProtocolException {
        return delegate.execute(request, responseHandler, context);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler)
            throws IOException, ClientProtocolException {
        return delegate.execute(target, request, responseHandler);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler,
            HttpContext context) throws IOException, ClientProtocolException {
        return delegate.execute(target, request, responseHandler, context);
    }

    /**
     * Generates a cURL command equivalent to the given request.
     */
    public static String toCurl(HttpUriRequest request) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("curl ");

        for (Header header : request.getAllHeaders()) {
            builder.append("--header \"");
            builder.append(header.toString().trim());
            builder.append("\" ");
        }

        URI uri = request.getURI();

        // If this is a wrapped request, use the URI from the original
        // request instead. getURI() on the wrapper seems to return a
        // relative URI. We want an absolute URI.
        if (request instanceof RequestWrapper) {
            HttpRequest original = ((RequestWrapper) request).getOriginal();
            if (original instanceof HttpUriRequest) {
                uri = ((HttpUriRequest) original).getURI();
            }
        }

        builder.append("\"");
        builder.append(uri);
        builder.append("\"");

        if (request instanceof HttpEntityEnclosingRequest && !uri.toString().contains("ClientLogin")) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
            HttpEntity entity = entityRequest.getEntity();
            if (entity != null && entity.isRepeatable()) {
                if (entity.getContentLength() < (8192 * 4)) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    entity.writeTo(stream);
                    String entityString = stream.toString();

                    builder.append(" --data-ascii \"").append(entityString).append("\"");
                } else {
                    builder.append(" [TOO MUCH DATA TO INCLUDE]");
                }
            }
        }

        return builder.toString();
    }

    /**
     * Modifies a request to indicate to the server that we would like a gzipped
     * response. (Uses the "Accept-Encoding" HTTP header.)
     * 
     * @param request
     *            the request to modify
     * @see #getUngzippedContent
     */
    public static void modifyRequestToAcceptGzipResponse(HttpRequest request) {
        request.addHeader("Accept-Encoding", "gzip");
    }

    /**
     * Gets the input stream from a response entity. If the entity is gzipped
     * then this will get a stream over the uncompressed data.
     * 
     * @param entity
     *            the entity whose content should be read
     * @return the input stream to read from
     * @throws IOException
     */
    public static InputStream getUngzippedContent(HttpEntity entity, Context context) throws IOException {
        InputStream responseStream = entity.getContent();
        if (isCountingEnabled(context))
            responseStream = new CountingInputStream(responseStream, "OUTER", context);
        if (responseStream == null)
            return responseStream;
        Header header = entity.getContentEncoding();
        if (header == null)
            return responseStream;
        String contentEncoding = header.getValue();
        if (contentEncoding == null)
            return responseStream;
        if (contentEncoding.contains("gzip"))
            responseStream = new GZIPInputStream(responseStream);
        if (isCountingEnabled(context))
            responseStream = new CountingInputStream(responseStream, "INNER ", context);
        return responseStream;
    }

    private static boolean isCountingEnabled(Context context) {
        if (countingEnabled == null)
            countingEnabled = "1".equals(NewsRob.getDebugProperties(context).getProperty("countBytesTransferred", "0"));
        return countingEnabled;
    }

}

class CountingInputStream extends FilterInputStream {

    private long countedBytes;
    private String label;
    private long started;
    private Context context;

    public CountingInputStream(InputStream is, String label, Context context) {
        super(is);
        this.label = label;
        started = System.currentTimeMillis();
        this.context = context;
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        int readBytesCount = super.read(buffer, offset, count);
        if (readBytesCount > 0)
            countedBytes += readBytesCount;
        return readBytesCount;
    }

    @Override
    public void close() throws IOException {
        super.close();
        PL.log(String.format("-------- [%s] transferred: %8.3f KB in %8.3f seconds.", label, (countedBytes / 1024.0),
                ((System.currentTimeMillis() - started) / 1000.0)), context);
    }

}
