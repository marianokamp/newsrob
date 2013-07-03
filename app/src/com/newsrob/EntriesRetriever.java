/**
 * 
 */
package com.newsrob;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

import com.newsrob.DB.TempTable;
import com.newsrob.auth.AccountManagementUtils;
import com.newsrob.auth.IAccountManagementUtils;
import com.newsrob.download.NewsRobHttpClient;
import com.newsrob.jobs.Job;
import com.newsrob.util.SimpleStringExtractorHandler;
import com.newsrob.util.Timing;
import com.newsrob.util.U;

public class EntriesRetriever implements BackendProvider {

    private static final int MIN_EXACT_SYNC_FREQUENCY_MIN = 29;
    private static final int MAX_ARTICLES_ON_GOOGLE_READER_ACCOUNT = 10000;
    private static final String NEWSROB_LABEL = "user/-/label/newsrob";
    public static final String TAG_GR = "tag:google.com,2005:reader/";
    public static final String TAG_GR_ITEM = TAG_GR + "item/";
    private static final String NEWSROB_PINNED_STATE = "user/-/label/pinned";
    private static final String GOOGLE_STATE = "user/-/state/com.google/";
    public static final String GOOGLE_STATE_READ = GOOGLE_STATE + "read";
    private static final String GOOGLE_STATE_STARRED = GOOGLE_STATE + "starred";

    public static final String GOOGLE_STATE_READING_LIST = GOOGLE_STATE + "reading-list";
    private static final Pattern PATTERN_GOOGLE_USER_ID_IN_FEED_ID = Pattern.compile("\\/user\\/(.*?)\\/");
    private static final String TAG = EntriesRetriever.class.getName();
    private static final String CLIENT_NAME = "newsrob";
    private static final String GOOGLE_SCHEME = "http://www.google.com/reader/";
    private static final String GOOGLE_SCHEMA = "http://www.google.com/schemas/reader/atom/";

    private static final String EXCLUDE_READ = "xt=" + GOOGLE_STATE_READ;

    // private static final String XT_NEWSROB_IGNORE = "&xt=" +
    // GOOGLE_STATE_READ;// "&xt=user/-/label/"
    // +
    // NEWSROB_IGNORE_LABEL;

    private String token;
    private Context context;
    private EntryManager entryManager;
    private String apiProtocolAndHost;

    public EntriesRetriever(Context context) {
        this.context = context.getApplicationContext();
    }

    private final EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(context);
        return entryManager;
    }

    boolean hasActiveSession() {
        return !getEntryManager().needsSession();
    }

    @Override
    public boolean authenticate(Context context, String email, String password, String captchaToken,
            String captchaAnswer) throws ClientProtocolException, IOException, AuthenticationFailedException {

        getEntryManager().getNewsRobNotificationManager().cancelSyncProblemNotification();

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {
            HttpPost authenticateRequest = new HttpPost("https://www.google.com/accounts/ClientLogin"); // ?client="
            // +
            // CLIENT_NAME);

            List<NameValuePair> keyValuePairs = new ArrayList<NameValuePair>();
            keyValuePairs.add(new BasicNameValuePair("accountType", "GOOGLE")); // HOSTED_OR_GOOGLE
            keyValuePairs.add(new BasicNameValuePair("Email", email));
            keyValuePairs.add(new BasicNameValuePair("Passwd", password));
            keyValuePairs.add(new BasicNameValuePair("source", CLIENT_NAME));
            keyValuePairs.add(new BasicNameValuePair("service", "reader"));
            if (captchaToken != null) {
                keyValuePairs.add(new BasicNameValuePair("logintoken", captchaToken));
                keyValuePairs.add(new BasicNameValuePair("logincaptcha", captchaAnswer));
            }

            authenticateRequest.setEntity(new UrlEncodedFormEntity(keyValuePairs, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(authenticateRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            String content = EntityUtils.toString(response.getEntity());

            switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
            case HttpStatus.SC_FORBIDDEN:
                Map<String, String> keyValues = U.parseKeyValuePairsFromString(content);

                String error = keyValues.get("Error");
                String requiredCaptchaToken = keyValues.get("CaptchaToken");
                String captchaUrl = keyValues.get("CaptchaUrl");

                if (captchaUrl != null) {
                    captchaUrl = "https://www.google.com/accounts/" + captchaUrl;
                    String msg = "Error=" + error + " CaptchaToken=" + captchaToken + " CaptchaUrl=" + captchaUrl;
                    Log.d(TAG, msg);
                    PL.log(msg, context);
                    throw new LoginWithCaptchaRequiredException(requiredCaptchaToken, captchaUrl);
                }
                if ("BadAuthentication".equals(error))
                    error = "Username / Password wrong?";
                else
                    error += "\n" + response.getStatusLine();
                throw new AuthenticationFailedException("Authentication failed:\n" + error);

            case HttpStatus.SC_OK:
                break;
            default:
                String msg = "Oh, status code was " + statusCode + " but unexpected.";
                PL.log(msg, context);
                Log.w(TAG, msg);
                throw new AuthenticationFailedException("Autentication failed:\n"
                        + EntityUtils.toString(response.getEntity()));
            }

            getEntryManager().saveAuthToken(
                    new AuthToken(AuthToken.AuthType.AUTH_STANDALONE, U.parseKeyValuePairsFromString(content).get(
                            "Auth")));

            response.getEntity().consumeContent();
            getEntryManager().getNewsRobNotificationManager().cancelSyncProblemNotification();
            return true;
        } finally {
            httpClient.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.newsrob.SyncInterface#discoverFeeds(java.lang.String)
     */
    @Override
    public List<DiscoveredFeed> discoverFeeds(final String query) throws SyncAPIException, IOException,
            AuthenticationExpiredException, ParserConfigurationException, SAXException, ServerBadRequestException {
        Timing t = new Timing("discoverFeeds()", context);

        final List<DiscoveredFeed> results = new ArrayList<DiscoveredFeed>();
        if (query == null || query.length() == 0)
            return results;

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);

        try {

            final String queryPath = "/reader/api/0/feed-finder?q=";
            HttpRequestBase req = createGRRequest(httpClient, getGoogleHost() + queryPath + query);
            HttpResponse response = executeGRRequest(httpClient, req, true);

            throwExceptionWhenNotStatusOK(response);

            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            SAXParser parser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new SimpleStringExtractorHandler() {

                private DiscoveredFeed discoveredFeed;

                @Override
                public void endElement(String uri, String localName, String name) throws SAXException {
                    super.endElement(uri, localName, name);

                    if (discoveredFeed != null && "entry".equals(localName)) {
                        // System.out.println("Added discovered feed " +
                        // discoveredFeed);
                        results.add(discoveredFeed);
                        discoveredFeed = null;
                    }
                }

                @Override
                public void startElement(String uri, String localName, String name, Attributes attributes)
                        throws SAXException {
                    super.startElement(uri, localName, name, attributes);

                    // System.out.println("startElement=" + localName);
                    if ("entry".equals(localName)) {
                        discoveredFeed = new DiscoveredFeed();
                        // System.out.println("Created new Discovered Feed");
                        return;
                    }

                    if ("link".equals(localName)) {
                        String rel = attributes.getValue("rel");
                        String href = attributes.getValue("href");
                        if ("self".equals(rel))
                            return;

                        if (discoveredFeed != null) {
                            // System.out.println("Found link");

                            if (rel != null) {
                                if ("alternate".equals(rel))
                                    discoveredFeed.alternateUrl = href;
                                else if ("http://www.google.com/reader/atom/relation/feed".equals(rel))
                                    discoveredFeed.feedUrl = href;
                            }
                        } else {
                            DiscoveredFeed df = new DiscoveredFeed();
                            df.title = query;
                            df.feedUrl = href;
                            results.add(df);
                        }
                    }
                    // System.out.println("startElement2=" + localName);
                }

                @Override
                public void receivedString(String localName, String fqn, String s) {

                    if (discoveredFeed == null)
                        return;

                    if ("title".equals(localName)) {
                        discoveredFeed.title = s;
                    } else if ("content".equals(localName)) {
                        discoveredFeed.snippet = s;
                    }
                }
            };

            parser.parse(NewsRobHttpClient.getUngzippedContent(response.getEntity(), context), handler);

            // for (DiscoveredFeed discoveredFeed : results)
            // System.out.println("DF=" + discoveredFeed);

            return results;
        } finally {
            httpClient.close();
            t.stop();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.newsrob.SyncInterface#submitSubscribe(java.lang.String)
     */
    @Override
    public boolean submitSubscribe(String url2subscribe) throws SyncAPIException {
        Timing t = new Timing("Submit Subscribe", context);

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {

            HttpPost editApiRequest = new HttpPost(getGoogleHost() + "/reader/api/0/subscription/edit");
            // quickadd

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("client", CLIENT_NAME));

            nameValuePairs.add(new BasicNameValuePair("ac", "subscribe"));
            nameValuePairs.add(new BasicNameValuePair("s", "feed/" + url2subscribe)); // quickadd

            HttpResponse resp = submitPostRequest(httpClient, editApiRequest, nameValuePairs, false);

            return resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;

        } catch (Exception e) {
            String message = "Problem during submission of subscribe: " + e.getMessage();
            Log.e(TAG, message, e);
            throw new SyncAPIException(message, e);
        } finally {
            httpClient.close();
            t.stop();
        }

    }

    private int remotelyAlterState(Collection<Entry> entries, final String column, String desiredState)
            throws SyncAPIException {

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {

            HttpPost editApiRequest = new HttpPost(getGoogleHost() + "/reader/api/0/edit-tag?client=" + CLIENT_NAME);
            submitPostRequest(httpClient, editApiRequest, createNVPForAlterRemoteState(entries, column, desiredState),
                    false);

            // At this point the submitPost is already done without errors
            // otherwise an exception would have been raised

            List<String> atomIds = new ArrayList<String>(entries.size());
            for (Entry entry : entries)
                atomIds.add(entry.getAtomId());
            getEntryManager().removePendingStateMarkers(atomIds, column);

            return entries.size();

        } catch (Exception e) {
            String message = "Problem during marking entry as un-/read: " + e.getMessage();
            Log.e(TAG, message, e);
            throw new SyncAPIException(message, e);
        } finally {
            httpClient.close();
        }

    }

    private HttpResponse submitPostRequest(NewsRobHttpClient httpClient, HttpPost postRequest,
            List<NameValuePair> nameValuePairs, boolean zipped) throws IOException, NeedsSessionException,
            SyncAPIException, ServerBadRequestException {

        setAuthInRequest(postRequest);

        boolean tokenIsFresh = false;
        if (token == null) {
            token = acquireToken(httpClient);
            tokenIsFresh = true;
        }

        addParametersIncludingTokenToPostRequest(postRequest, nameValuePairs);

        HttpResponse response = null;
        try {
            response = executeGRRequest(httpClient, postRequest, zipped);
        } catch (AuthenticationExpiredException e) {
            Log.w(TAG, "PostRequest to uri " + postRequest.getURI() + " resulted in GRTokenExpired.");
            Log.w(TAG, "Token is fresh? " + tokenIsFresh);
            if (tokenIsFresh)
                throw new SyncAPIException("Problem during post request to Google.");
            else {
                Log.w(TAG, "Retrying post with new token.");
                token = acquireToken(httpClient);
                addParametersIncludingTokenToPostRequest(postRequest, nameValuePairs);
                try {
                    response = executeGRRequest(httpClient, postRequest, zipped);
                } catch (AuthenticationExpiredException e1) {
                    // Can't help it, if it also doesn't work on the 2nd attempt
                }
                throwExceptionWhenNotStatusOK(response);
            }
        }
        return response;
    }

    private void addParametersIncludingTokenToPostRequest(HttpPost postRequest, List<NameValuePair> nameValuePairs) {
        // add token to the parameters, encode them and put them in the post
        // request

        List<NameValuePair> nvps = new ArrayList<NameValuePair>(nameValuePairs.size() + 1);
        nvps.addAll(nameValuePairs);
        nvps.add(new BasicNameValuePair("T", token));

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String acquireToken(NewsRobHttpClient httpClient) throws ClientProtocolException, IOException,
            NeedsSessionException {

        assertSessionAvailable();

        HttpGet acquireTokenRequest = new HttpGet(getGoogleHost() + "/reader/api/0/token?client=" + CLIENT_NAME);
        setAuthInRequest(acquireTokenRequest);

        HttpResponse response = httpClient.execute(acquireTokenRequest);
        String token = EntityUtils.toString(response.getEntity());

        response.getEntity().consumeContent();
        return token;
    }

    private void setAuthInRequest(HttpRequestBase req) {
        AuthToken authToken = getAuthToken();

        if (authToken == null)
            return; // LATER Callback/Exception to acquire it here?

        if (authToken.getAuthType().equals(AuthToken.AuthType.AUTH_STANDALONE)
                || authToken.getAuthType().equals(AuthToken.AuthType.AUTH)) {
            req.removeHeaders("Authorization");
            req.setHeader("Authorization", "GoogleLogin auth=" + authToken.getAuthToken());
            return;
        }

        throw new RuntimeException("Trying to access GoogleReader without having an authToken.");
    }

    private String getGoogleHost() {
        if (apiProtocolAndHost == null) {
            apiProtocolAndHost = NewsRob.getDebugProperties(context).getProperty("apiProtocolAndHost",
                    "https://www.google.com");
        }
        return apiProtocolAndHost;
    }

    private void assertSessionAvailable() throws NeedsSessionException {
        if (!hasActiveSession())
            throw new NeedsSessionException("Operation needs a valid Google session.");
    }

    private List<NameValuePair> createNVPForAlterRemoteState(Collection<Entry> entries, String column,
            String desiredState) throws UnsupportedEncodingException, IOException, ClientProtocolException {

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("client", CLIENT_NAME));

        String globalGoogleStateName = null;
        if (DB.Entries.READ_STATE_PENDING.equals(column))
            globalGoogleStateName = GOOGLE_STATE_READ;
        else if (DB.Entries.STARRED_STATE_PENDING.equals(column))
            globalGoogleStateName = GOOGLE_STATE_STARRED;
        else if (DB.Entries.PINNED_STATE_PENDING.equals(column))
            globalGoogleStateName = NEWSROB_PINNED_STATE;

        if (globalGoogleStateName == null)
            throw new RuntimeException("Assertion failed. globalGoogleStateName could not be set for column !" + column
                    + "!");

        nameValuePairs.add(new BasicNameValuePair("1".equals(desiredState) ? "a" : "r", globalGoogleStateName));

        for (Entry entry : entries) {
            if (NewsRob.isDebuggingEnabled(context))
                PL.log(TAG + ": Preparing states for " + entry.getTitle() + " (" + entry.getAtomId() + ")", context);

            String googleStateName = null;
            if (DB.Entries.READ_STATE_PENDING.equals(column) && entry.isReadStatePending()) {
                googleStateName = GOOGLE_STATE_READ;
            } else if (DB.Entries.STARRED_STATE_PENDING.equals(column) && entry.isStarredStatePending()) {
                googleStateName = GOOGLE_STATE_STARRED;
            } else if (DB.Entries.PINNED_STATE_PENDING.equals(column) && entry.isPinnedStatePending()) {
                googleStateName = NEWSROB_PINNED_STATE;
            }

            if (googleStateName == null) {
                Log.e(TAG,
                        "Oh. stateName was null. column was " + column + " " + entry.getAtomId() + " title="
                                + entry.getTitle());
                continue;
            }

            if (!googleStateName.equals(globalGoogleStateName)) {
                PL.log(TAG + ": Oh. Assertion failed globalStateName and globalGoogleStateName are not equal. "
                        + entry.getAtomId() + " title=" + entry.getTitle() + "!" + googleStateName + "!!"
                        + globalGoogleStateName + "!", context);
                continue;
            }

            nameValuePairs.add(new BasicNameValuePair("i", entry.getAtomId()));
            // nameValuePairs.add(new BasicNameValuePair("s",
            // entry.getFeedAtomId().substring(27)));

            if (NewsRob.isDebuggingEnabled(context)) {
                PL.log(TAG + ": i=" + entry.getAtomId(), context);
                PL.log(TAG + ": s=" + entry.getFeedAtomId().substring(27), context);
            }

        }

        return nameValuePairs;
    }

    /**
     * 
     * @param entryManager
     * @param syncJob
     * @return No of entries updated
     * @throws NeedsSessionException
     */
    @Override
    public int synchronizeArticles(EntryManager entryManager, SyncJob syncJob) throws MalformedURLException,
            IOException, ParserConfigurationException, FactoryConfigurationError, SAXException, ParseException,
            NeedsSessionException {

        int noOfStateSyncAffectedEntries = 0;
        try {
            noOfStateSyncAffectedEntries = syncStates(entryManager, syncJob);
        } catch (SyncAPIException e) {
            // silently ignored, just logging for debugging/support purposes
            Log.e(TAG, "Problem during synching read states with Google Reader: " + e.getMessage() + " ("
                    + e.getCause().getClass().getName() + ")", e);
        }

        return noOfStateSyncAffectedEntries;
    }

    private int syncStates(EntryManager entryManager, SyncJob syncJob) throws SyncAPIException {
        int noOfUpdated = 0;

        String[] fields = { DB.Entries.READ_STATE_PENDING, DB.Entries.STARRED_STATE_PENDING,
                DB.Entries.PINNED_STATE_PENDING };
        for (String f : fields) {

            String progressLabel;
            if (f == DB.Entries.READ_STATE_PENDING)
                progressLabel = "read";
            else if (f == DB.Entries.STARRED_STATE_PENDING)
                progressLabel = "starred";
            else if (f == DB.Entries.PINNED_STATE_PENDING)
                progressLabel = "pinned";
            else
                progressLabel = "unknown";

            String[] desiredStates = { "0", "1" };
            for (String desiredState : desiredStates) {
                List<Entry> allEntries = entryManager.findAllStatePendingEntries(f, desiredState);

                if (allEntries.size() == 0)
                    continue;

                syncJob.setJobDescription("Syncing state: " + progressLabel);
                syncJob.target = allEntries.size();
                syncJob.actual = 0;
                entryManager.fireStatusUpdated();

                // LATER make this cancelable? Add Job here.

                int offset = 0;

                while (offset < allEntries.size()) {
                    int nextPackSize = Math.min(allEntries.size() - offset, 25);
                    if (nextPackSize == 0)
                        break;

                    List<Entry> currentPack = new ArrayList<Entry>(allEntries.subList(offset, offset + nextPackSize));
                    offset += nextPackSize;
                    noOfUpdated += remotelyAlterState(currentPack, f, desiredState);
                    syncJob.actual = noOfUpdated;
                    entryManager.fireStatusUpdated();

                }
            }
        }
        return noOfUpdated;
    }

    @Override
    public int fetchNewEntries(final EntryManager entryManager, final SyncJob job, boolean manualSync)
            throws ClientProtocolException, IOException, NeedsSessionException, SAXException, IllegalStateException,
            ParserConfigurationException, FactoryConfigurationError, SyncAPIException, AuthenticationExpiredException {

        String originalJobDescription = job.getJobDescription();
        Timing t = new Timing("fetchEntries", context);

        final FetchContext fetchCtx = new FetchContext();

        assertSessionAvailable();

        long lastUpdated = entryManager.getGRUpdated();

        PL.log("Before querying GR, last updated: " + lastUpdated, context);

        boolean incrementalUpdate = lastUpdated > -1l;
        final String otUrlParameter = incrementalUpdate ? "&ot=" + (lastUpdated) : "";

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);

        try {

            if (job.isCancelled())
                return fetchCtx.countFetchedEntries;

            if (!entryManager.syncCurrentlyEnabled(manualSync))
                return fetchCtx.countFetchedEntries;

            if (getEntryManager().isNewsRobOnlySyncingEnabled()) {
                job.setJobDescription("Fetching 'newsrob' articles.");
                requestArticlesFromGoogleReader(job, fetchCtx, httpClient, NEWSROB_LABEL, entryManager
                        .getNewsRobSettings().getStorageCapacity(),
                        otUrlParameter
                                + (entryManager.shouldOnlyUnreadArticlesBeDownloaded() ? "&" + EXCLUDE_READ : "")); // +
                // (true
                // ?
                // "&"
                // +
                // EXCLUDE_FRIENDS
                // :
                // "")

            } else {

                job.setJobDescription("Fetching reading list articles");
                requestArticlesFromGoogleReader(job, fetchCtx, httpClient, GOOGLE_STATE_READING_LIST, entryManager
                        .getNewsRobSettings().getStorageCapacity(),
                        otUrlParameter
                                + (entryManager.shouldOnlyUnreadArticlesBeDownloaded() ? "&" + EXCLUDE_READ : "")); // +
                // (true
                // ?
                // "&"
                // +
                // EXCLUDE_FRIENDS
                // :
                // "")
            }

            if (!entryManager.syncCurrentlyEnabled(manualSync))
                return fetchCtx.countFetchedEntries;

            job.setJobDescription("Fetching starred articles");
            requestArticlesFromGoogleReader(job, fetchCtx, httpClient, GOOGLE_STATE_STARRED,
                    entryManager.getNoOfStarredArticlesToKeep(), otUrlParameter);

            if (!entryManager.syncCurrentlyEnabled(manualSync))
                return fetchCtx.countFetchedEntries;

            if (incrementalUpdate) {
                job.setJobDescription("Incrementally updating article states");
                performIncrementalUpdate(entryManager, job, fetchCtx, lastUpdated);
            } else {
                final long minutesSinceLastSync = (System.currentTimeMillis() - entryManager.getLastSyncTime()) / 1000 / 60;

                if ((true || minutesSinceLastSync < MIN_EXACT_SYNC_FREQUENCY_MIN)
                        && isLocalStorageCapacityForMoreArticlesAvailable(entryManager)) {
                    PL.log("EntriesRetriever: articleCount < storageCapacity == true", context);
                    job.setJobDescription("Fetching more unread articles");

                    long[] articleIds = fetchStreamIds(entryManager, GOOGLE_STATE_READING_LIST, GOOGLE_STATE_READ);
                    entryManager.populateTempTable(TempTable.READ, articleIds);

                    if (!entryManager.syncCurrentlyEnabled(manualSync))
                        return fetchCtx.countFetchedEntries;

                    fetchOlderUnreadToMatchCapacity(entryManager, job, fetchCtx);
                } else
                    PL.log("EntriesRetriever: articleCount < storageCapacity == false", context);

            }

            // Synchronization done successfully, now set the last updated date

            if (fetchCtx.tempLastUpdated > lastUpdated)
                maintainLastUpdated(fetchCtx.tempLastUpdated);

            // job.setJobDescription("Resetting articles with submitted notes.");
            // entryManager.clearNotesSubmissionStateForAllSubmittedNotes();
            t.stop();

        } catch (SAXException e) {
            throw e;
        } catch (FetchCancelledException fce) {
            // user interruption -> ignored
        } catch (ServerBadRequestException e) {
            throw new IOException("GR: Bad Request.");
        } finally {
            httpClient.close();
            job.setJobDescription(originalJobDescription);
        }
        PL.log("EntriesRetriever: Count seen entries=" + fetchCtx.countSeenEntries, context);

        return fetchCtx.countFetchedEntries;
    }

    private void requestArticlesFromGoogleReader(Job job, FetchContext fetchCtx, NewsRobHttpClient httpClient,
            String state, int n, String urlPostfix) throws IOException, FactoryConfigurationError,
            ParserConfigurationException, SAXException, AuthenticationExpiredException, ServerBadRequestException {

        if (n == 0)
            return;

        String url = getGoogleHost() + "/reader/atom/" + state + "?n=" + n + "&r=n" + urlPostfix;

        HttpRequestBase req = createGRRequest(httpClient, url);

        HttpResponse response = executeGRRequest(httpClient, req, true);
        throwExceptionWhenNotStatusOK(response);

        processReadingList(job, fetchCtx, response);

        Log.d(TAG, "totalFetchedArticleCount=" + fetchCtx.countFetchedEntries);
        Log.d(TAG, "totalSeenArticleCount=" + fetchCtx.countFetchedEntries);

    }

    private HttpRequestBase createGRRequest(NewsRobHttpClient httpClient, String url) throws IOException {

        url += (url.indexOf("?") == -1 ? "?" : "&") + "client=" + CLIENT_NAME;

        HttpGet readingListRequest = new HttpGet(url);

        setAuthInRequest(readingListRequest);

        Log.d(TAG, "Accessing Google Reader service.");
        return readingListRequest;
    }

    private void performIncrementalUpdate(final EntryManager entryManager, final SyncJob job,
            final FetchContext fetchCtx, long lastUpdated) throws IOException, ParserConfigurationException,
            SAXException, NeedsSessionException, SyncAPIException, AuthenticationExpiredException,
            ServerBadRequestException {

        if (job.isCancelled())
            return;

        Timing incrementalTiming = new Timing("Incremental Update", context);

        boolean shouldDoExactSyncing = entryManager.shouldAlwaysExactSync();
        final long minutesSinceLastExactSync = (System.currentTimeMillis() - entryManager.getLastExactSync()) / 1000 / 60;
        PL.log("EntriesRetriever: Exact Syncing, minutes since last sync=" + minutesSinceLastExactSync, context);

        // Skip exact syncing when the last sync is less than 15 minutes ago
        if (true || minutesSinceLastExactSync > MIN_EXACT_SYNC_FREQUENCY_MIN) {
            if (entryManager.isOnWiFi())
                shouldDoExactSyncing = true;
            else {
                PL.log("ER.performIncrementalUpdate last exact sync " + minutesSinceLastExactSync + "min ago.", context);
                if (minutesSinceLastExactSync > (24 * 60))
                    shouldDoExactSyncing = true;
            }
        } else {
            PL.log("EntriesRetriever: Skipping Exact Syncing b/o <" + MIN_EXACT_SYNC_FREQUENCY_MIN + " min.", context);
            shouldDoExactSyncing = false;
        }

        if (!shouldDoExactSyncing) {

            Timing deltaUpdates = new Timing("Delta Syncing", context);
            job.setJobDescription("Delta syncing states");
            // process state changes
            Collection<StateChange> stateChanges = getStateChangesFromGR(lastUpdated);
            entryManager.updateStates(stateChanges);
            // workaround for missing read state changes as a result of mark
            // as read
            deltaUpdates.stop();
        } else {

            Timing differentialUpdates = new Timing("Differential Updates", context);

            job.setJobDescription("Exact syncing states");

            ExecutorService executorService = Executors.newCachedThreadPool();

            FutureTask<Void> futureReadUpdateResult = submitDifferentialStateUpdate(entryManager, executorService, job,
                    GOOGLE_STATE_READING_LIST, GOOGLE_STATE_READ, ArticleDbState.READ);
            FutureTask<Void> futureStarredUpdateResult = submitDifferentialStateUpdate(entryManager, executorService,
                    job, GOOGLE_STATE_STARRED, null, ArticleDbState.STARRED);

            waitForFuture(futureStarredUpdateResult);
            waitForFuture(futureReadUpdateResult);

            executorService.shutdown();
            entryManager.updateLastExactSync();
            differentialUpdates.stop();

        }

        if (entryManager.shouldReadItemsBeDeleted()) {
            PL.log("EntriesRetriever:deleting read articles.", context);
            job.setJobDescription("Deleting read articles");
            entryManager.deleteReadArticles(job);
            PL.log("EntriesRetriever:deleting read articles done.", context);
        }

        PL.log("Existing Articles 0=" + entryManager.getArticleCount(), context);
        fetchOlderUnreadToMatchCapacity(entryManager, job, fetchCtx);

        incrementalTiming.stop();
    }

    private void waitForFuture(FutureTask<Void> futureTask) throws IOException, ParserConfigurationException,
            SAXException, SyncAPIException, NeedsSessionException, AuthenticationExpiredException,
            ServerBadRequestException {
        try {
            PL.log("Waiting for future " + futureTask, context);
            futureTask.get();
            PL.log("Waiting for future " + futureTask + " done.", context);

        } catch (ExecutionException ee) {
            PL.log("Waiting for future " + futureTask + " interrupted by exception. " + ee.getClass().getSimpleName()
                    + " " + ee.getMessage(), context);
            Throwable root = ee.getCause();
            if (root instanceof IOException)
                throw (IOException) root;
            else if (root instanceof ParserConfigurationException)
                throw (ParserConfigurationException) root;

            else if (root instanceof SAXException)
                throw (SAXException) root;

            else if (root instanceof SyncAPIException)
                throw (SyncAPIException) root;

            else if (root instanceof NeedsSessionException)
                throw (NeedsSessionException) root;

            else if (root instanceof AuthenticationExpiredException)
                throw (AuthenticationExpiredException) root;

            else if (root instanceof ServerBadRequestException)
                throw (ServerBadRequestException) root;

        } catch (InterruptedException e) {
            e.printStackTrace();

        }
    }

    private FutureTask<Void> submitDifferentialStateUpdate(final EntryManager entryManager,
            ExecutorService executorService, final Job job, final String stream, final String excludeState,
            final ArticleDbState articleDbState) {
        FutureTask<Void> futureReadUpdateResult = new FutureTask<Void>(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                differentialUpdateOfArticlesStates(entryManager, job, stream, excludeState, articleDbState);
                return null;
            }
        });
        executorService.execute(futureReadUpdateResult);
        return futureReadUpdateResult;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.newsrob.SyncInterface#differentialUpdateOfArticlesStates(com.newsrob
     * .EntryManager, com.newsrob.jobs.Job, java.lang.String, java.lang.String,
     * com.newsrob.ArticleDbState)
     */
    @Override
    public void differentialUpdateOfArticlesStates(final EntryManager entryManager, Job job, String stream,
            String excludeState, ArticleDbState articleDbState) throws SAXException, IOException,
            ParserConfigurationException, ServerBadRequestException, AuthenticationExpiredException {

        // Map read state to temp table
        TempTable tempTableType = TempTable.READ;
        if (articleDbState == ArticleDbState.STARRED)
            tempTableType = TempTable.STARRED;

        Timing t = new Timing("ER.differentialUpdateOfArticleStates for " + articleDbState + ".", context);
        PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (1)", context);
        long[] articleIds = fetchStreamIds(entryManager, stream, excludeState);
        long[] pinnedArticleIds = new long[0];
        if (articleDbState == ArticleDbState.READ)
            pinnedArticleIds = fetchStreamIds(entryManager, NEWSROB_PINNED_STATE, GOOGLE_STATE_READ);

        PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (2.5)\narticleIds="
                + articleIds.length + " pinnedArticleIds=" + pinnedArticleIds.length, context);

        PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (2)", context);

        if (job.isCancelled())
            return;

        synchronized (DB.class) {
            if (job.isCancelled())
                return;

            PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (3E)", context);

            entryManager.populateTempTable(tempTableType, articleIds);
            PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (4E)", context);

            if (pinnedArticleIds.length > 0) {
                entryManager.populateTempTable(TempTable.PINNED, pinnedArticleIds);
                PL.log("ER.differentialUpdateOfArticleStates(pinned) for " + articleDbState + ". (4.1E)", context);
            }

            entryManager.updateStatesFromTempTable(articleDbState, tempTableType);
            PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (5E)", context);

        }
        PL.log("ER.differentialUpdateOfArticleStates for " + articleDbState + ". (6)", context);
        entryManager.clearTempTable(tempTableType);
        entryManager.clearTempTable(TempTable.PINNED);
        t.stop();
    }

    private long[] fetchStreamIds(final EntryManager entryManager, String tag, final String googleStateToExclude)
            throws SAXException, IOException, ParserConfigurationException, AuthenticationExpiredException,
            ServerBadRequestException {

        Timing t = new Timing("ER.fetchStreamIds for " + tag + " xt=" + googleStateToExclude, context);
        PL.log("ER.fetchStreamIds for " + tag + " xt=" + googleStateToExclude + " (1)", context);
        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {
            List<String> tags = new ArrayList<String>(2);
            // tags.add(GOOGLE_STATE_READING_LIST);
            if (tag != null)
                tags.add(tag);
            long[] unreadIds = getStreamIDsFromGR(httpClient, tags, googleStateToExclude,
                    MAX_ARTICLES_ON_GOOGLE_READER_ACCOUNT);
            PL.log("EntriesRetriever.getUnreadIDsFromGR done.", context);
            return unreadIds;

        } finally {
            httpClient.close();
            t.stop();
            PL.log("ER.fetchStreamIds for " + tag + " xt=" + googleStateToExclude + " (2)", context);
        }
    }

    private long[] getStreamIDsFromGR(NewsRobHttpClient httpClient, String tag, String xt, int noOfNotesToKeep)
            throws IOException, SAXException, ParserConfigurationException, AuthenticationExpiredException,
            ServerBadRequestException {
        List<String> tags = new ArrayList<String>(1);
        tags.add(tag);
        return getStreamIDsFromGR(httpClient, tags, xt, noOfNotesToKeep);
    }

    private void fetchOlderUnreadToMatchCapacity(final EntryManager entryManager, final Job job,
            final FetchContext fetchCtx) throws SAXException, IOException, ParserConfigurationException,
            NeedsSessionException, SyncAPIException, AuthenticationExpiredException, ServerBadRequestException {

        final long minutesSinceLastSync = (System.currentTimeMillis() - entryManager.getLastSyncTime()) / 1000 / 60;
        if (minutesSinceLastSync < MIN_EXACT_SYNC_FREQUENCY_MIN)
            return;

        job.setJobDescription("Fetching more unread articles - preparation");

        long[] article_ids = null;
        if (getEntryManager().isNewsRobOnlySyncingEnabled())
            article_ids = fetchStreamIds(entryManager, NEWSROB_LABEL, GOOGLE_STATE_READ);
        else
            article_ids = fetchStreamIds(entryManager, GOOGLE_STATE_READING_LIST, GOOGLE_STATE_READ);

        PL.log("Fetching more unread articles - found " + article_ids.length + " articles server-side.",
                entryManager.getContext());

        entryManager.populateTempTable(TempTable.READ, article_ids);
        // TODO Pinned also fetch older unread pinned, necessary?
        PL.log("Fetching more unread articles - stored the ids in the temp table of those " + article_ids.length
                + " articles server-side.", entryManager.getContext());

        job.setJobDescription("Fetching more unread articles - execution");

        while (isLocalStorageCapacityForMoreArticlesAvailable(entryManager)) {

            if (NewsRob.isDebuggingEnabled(context))
                PL.log("EntriesRetriever::fetchOlderUnreadToMatchCapacity noOfArticlesInTemptable before clean="
                        + entryManager.getTempIdsCount(TempTable.READ), context);

            entryManager.removeLocallyExistingArticlesFromTempTable();

            if (NewsRob.isDebuggingEnabled(context))
                PL.log("EntriesRetriever::fetchOlderUnreadToMatchCapacity noOfArticlesInTemptable after clean="
                        + entryManager.getTempIdsCount(TempTable.READ), context);
            // need to get more articles
            int articlesGap = entryManager.getNewsRobSettings().getStorageCapacity() - entryManager.getArticleCount();
            int noOfArticles2Fetch = Math.min(articlesGap, 50);

            if (NewsRob.isDebuggingEnabled(context))
                PL.log("EntriesRetriever::fetchOlderUnreadToMatchCapacity noOfArticles2Fetch=" + noOfArticles2Fetch,
                        context);

            List<String> newArticleAtomIds = entryManager.getNewArticleIdsToFetch(noOfArticles2Fetch);
            // exit when no more entries are in there
            if (newArticleAtomIds.size() == 0)
                break;

            fetchNewArticlesByAtomIds(job, fetchCtx, newArticleAtomIds);

        }
    }

    private boolean isLocalStorageCapacityForMoreArticlesAvailable(final EntryManager entryManager) {
        final boolean isAvailable = entryManager.getArticleCount() < entryManager.getNewsRobSettings()
                .getStorageCapacity();
        PL.log("isLocalStorageCapacityForMoreArticlesAvailable? " + isAvailable + " articleCount="
                + entryManager.getArticleCount() + " capacity="
                + entryManager.getNewsRobSettings().getStorageCapacity(), entryManager.getContext());

        return isAvailable;
    }

    private void processReadingList(final Job job, final FetchContext fetchCtx, HttpResponse response)
            throws IOException, FactoryConfigurationError, ParserConfigurationException, SAXException {

        InputStream is = NewsRobHttpClient.getUngzippedContent(response.getEntity(), context);
        processInputStream(job, fetchCtx, is);
        response.getEntity().consumeContent();
    }

    protected void processInputStream(final Job job, final FetchContext fetchCtx, InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser parser = saxParserFactory.newSAXParser();

        DefaultHandler handler = new ReadingListStreamHandler(getEntryManager(), fetchCtx, job);

        parser.parse(is, handler);
    }

    private void fetchNewArticlesByAtomIds(Job job, FetchContext fetchCtx, List<String> atomIds) throws SAXException,
            IOException, ParserConfigurationException, NeedsSessionException, SyncAPIException {

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {

            String url = getGoogleHost() + "/reader/api/0/stream/items/contents?output=atom&xt=" + GOOGLE_STATE_READ
                    + "&client=" + CLIENT_NAME;

            HttpPost getNewArticlesRequest = new HttpPost(url);

            List<NameValuePair> keyValuePairs = new ArrayList<NameValuePair>();
            for (String unreadId : atomIds) {
                keyValuePairs.add(new BasicNameValuePair("i", unreadId));
            }

            keyValuePairs.add(new BasicNameValuePair("client", CLIENT_NAME));

            HttpResponse response;
            try {
                response = submitPostRequest(httpClient, getNewArticlesRequest, keyValuePairs, true);
            } catch (ServerBadRequestException e) {
                try {
                    response = submitPostRequest(httpClient, getNewArticlesRequest, keyValuePairs, true);
                } catch (ServerBadRequestException e1) {
                    throw new SyncAPIException("GR believes to have received a bad request!");
                }
            }

            processReadingList(job, fetchCtx, response);
        } finally {
            httpClient.close();
        }
    }

    private HttpResponse executeGRRequest(NewsRobHttpClient httpClient, HttpRequestBase articleRequest, boolean zipped)
            throws IOException, AuthenticationExpiredException, ServerBadRequestException {

        HttpResponse response = zipped ? httpClient.executeZipped(articleRequest) : httpClient.execute(articleRequest);
        /*
         * count++; // LATER if (count == 3) response.setStatusCode(401);
         */
        if (checkStatusCodeForReloginAndExpiredToken(response, true)) {
            retryLogin(); // LATER ask for a refresh of the Account 2.0
            // credentials?
            // LATER or skip the re-try attempt?
            setAuthInRequest(articleRequest);
            response = zipped ? httpClient.executeZipped(articleRequest) : httpClient.execute(articleRequest);
            checkStatusCodeForReloginAndExpiredToken(response, false);
        }

        return response;
    }

    private void retryLogin() {

        if (BackendProvider.AuthToken.AuthType.AUTH == entryManager.getAuthToken().getType()) {
            getAuthToken();
        } else {
            try {
                EntryManager entryManager = getEntryManager();
                String msg = "Relogin succesful= ";
                if (entryManager.getStoredPassword() == null) {
                    PL.log("EntriesRetriever: Re-login. Password not stored. Not re-logging in.", context);
                    return;
                }

                PL.log("EntriesRetriever.retryLogin() with " + entryManager.getEmail(), context);
                msg += authenticate(this.context, entryManager.getEmail(), entryManager.getStoredPassword(), null, null);
                Log.d(TAG, msg);
                PL.log(msg, context);

            } catch (Exception e) {
                Log.e(TAG, "Re-login didn't work.", e);
                PL.log("Relogin failed. " + e.getMessage(), context);
                e.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.newsrob.SyncInterface#unsubscribeFeed(java.lang.String)
     */
    @Override
    public void unsubscribeFeed(String feedAtomId) throws IOException, NeedsSessionException, SyncAPIException {

        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {
            Timing t = new Timing("EntriesRetriever.unsubcribeFeed()", context);

            HttpPost editApiRequest = new HttpPost(getGoogleHost() + "/reader/api/0/subscription/edit?client="
                    + CLIENT_NAME);
            LinkedList<NameValuePair> nameValuePairs = new LinkedList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("s", feedAtomId));
            nameValuePairs.add(new BasicNameValuePair("ac", "unsubscribe"));

            HttpResponse result;
            try {
                result = submitPostRequest(httpClient, editApiRequest, nameValuePairs, false);
            } catch (ServerBadRequestException e) {
                try {
                    result = submitPostRequest(httpClient, editApiRequest, nameValuePairs, false);
                } catch (ServerBadRequestException e1) {
                    throw new SyncAPIException("GR believes it received a bad request.");
                }
            }
            if (HttpStatus.SC_OK == result.getStatusLine().getStatusCode())
                entryManager.removeFeedFromFeeds2Unsubscribe("tag:google.com,2005:reader/" + feedAtomId); // LATER
            // Clean
            // up
            // the
            // tag:google
            // business
            t.stop();
        } finally {
            httpClient.close();
        }
    }

    /**
     * @param xt
     *            can be null
     */
    protected long[] getStreamIDsFromGR(NewsRobHttpClient httpClient, final List<String> tags, String xt, int max)
            throws IOException, SAXException, ParserConfigurationException, AuthenticationExpiredException,
            ServerBadRequestException {

        if (max == 0)
            return new long[0];

        final String tagsLabel = String.valueOf(tags);
        Timing t = new Timing("EntriesRetriever.getStreamIDsFromGR(" + tagsLabel + ") (-" + xt + ")", context);

        int currentCapacity = getEntryManager().getArticleCount();

        String url = getGoogleHost() + "/reader/api/0/stream/items/ids";
        url += "?s=" + tags.remove(0);
        for (String tag : tags)
            url += "&s=" + tag;

        if (xt != null)
            url += "&xt=" + xt;
        url += "&n=" + max;

        try {

            HttpRequestBase req = createGRRequest(httpClient, url);
            HttpResponse response = executeGRRequest(httpClient, req, true);
            throwExceptionWhenNotStatusOK(response);

            final List<Long> unreadIds = new ArrayList<Long>(currentCapacity * 80 / 100);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            SAXParser parser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new SimpleStringExtractorHandler() {

                String currentName;
                boolean validResponse;

                @Override
                public final void startElement(String uri, String localName, String name, Attributes attributes)
                        throws SAXException {
                    super.startElement(uri, localName, name, attributes);
                    currentName = attributes.getValue("name");
                    if (!validResponse && "/object".equals(getFullyQualifiedPathName()))
                        validResponse = true;
                }

                @Override
                public final void receivedString(String localTagName, String fqn, String value) {
                    if ("number".equals(localTagName) && "id".equals(currentName))
                        unreadIds.add(Long.parseLong(value));
                }

                @Override
                public void endDocument() throws SAXException {
                    super.endDocument();
                    if (!validResponse)
                        throw new RuntimeException("Google Reader response was invalid. Proxy issue?");
                }
            };

            InputStream is = NewsRobHttpClient.getUngzippedContent(response.getEntity(), context);
            parser.parse(is, handler);

            if (NewsRob.isDebuggingEnabled(context))
                PL.log(TAG + ": GR returned number of articles(" + tagsLabel + ") (-" + xt + ")=" + unreadIds.size(),
                        context);

            long[] rv = new long[unreadIds.size()];
            int idx = 0;
            for (Long unreadId : unreadIds)
                rv[idx++] = unreadId;

            return rv;
        } finally {
            t.stop();
        }
    }

    /**
     * @return should retry
     * @throws AuthenticationExpiredException
     * @throws ServerBadRequestException
     */
    private boolean checkStatusCodeForReloginAndExpiredToken(HttpResponse response, boolean firstTry)
            throws IOException, AuthenticationExpiredException, ServerBadRequestException {

        // check for bad token
        Header googleBadTokenHeader = response.getFirstHeader("X-Reader-Google-Bad-Token");

        if (googleBadTokenHeader != null && "true".equals(googleBadTokenHeader.getValue()))
            throw new AuthenticationExpiredException();

        int statusCode = response.getStatusLine().getStatusCode();

        switch (statusCode) {
        case HttpStatus.SC_BAD_REQUEST:
            throw new ServerBadRequestException();
        case HttpStatus.SC_MOVED_TEMPORARILY:
        case HttpStatus.SC_UNAUTHORIZED:
        case HttpStatus.SC_FORBIDDEN:
            EntryManager entryManager = getEntryManager();
            // retry possible?
            String msg = "302 or 401 or 403 or 400:" + response.getStatusLine();

            Log.w(TAG, msg);
            PL.log(msg, context);
            IAccountManagementUtils amu = AccountManagementUtils.getAccountManagementUtils(context);
            PL.log("Login: firstTry=" + firstTry + " shouldRemember=" + entryManager.shouldRememberPassword()
                    + " passwordStored="
                    + (entryManager.getStoredPassword() != null + " emailStored=" + (entryManager.getEmail() != null)),
                    context);
            if (amu != null) {
                Log.w(TAG, "Trying relogin with new auth.");
                PL.log("Trying relogin with new auth.", context);
                try {
                    entryManager.expireAuthToken();

                } catch (Exception e) {
                    Log.w(TAG, "Re-login not possible as we caught an exception: " + e.getClass().getSimpleName()
                            + " : " + e.getMessage());
                    getEntryManager().getNewsRobNotificationManager().sendSynchronizationProblemNotification(
                            getAuthToken() != null);
                    return false;
                }
                return true;
            } else if (firstTry
                    && amu == null // amu exists mean that the login should be
                    // done using the new method
                    && entryManager.shouldRememberPassword() && entryManager.getStoredPassword() != null
                    && entryManager.getEmail() != null) {
                Log.w(TAG, "Issuing re-login.");
                PL.log("Issuing re-login.", context);
                return true;
            }
            Log.w(TAG, "Re-login not possible.");
            getEntryManager().getNewsRobNotificationManager().sendSynchronizationProblemNotification(
                    getAuthToken() != null);
            // entryManager.clearAuthToken();

            throw new RuntimeException("Login needed. Check the status bar for a notification. "
                    + response.getStatusLine().getStatusCode());
        }
        return false;
    }

    private AuthToken getAuthToken() {
        return getEntryManager().getAuthToken();
    }

    private Collection<StateChange> getStateChangesFromGR(long lastUpdated) throws IOException,
            ParserConfigurationException, SAXException, AuthenticationExpiredException, ServerBadRequestException {

        Timing t = new Timing("EntriesRetriever.getStateChangesFromGR()", context);

        String url = getGoogleHost() + "/reader/api/0/stream/items/ids";

        url += "?s=user/-/state/com.google/starred"; // &s=deleted/user/-/state/com.google/starred";
        url += "&s=user/-/state/com.google/read";
        url += "&s=" + NEWSROB_PINNED_STATE;
        // &s=deleted/user/-/state/com.google/read";
        url += "&n=10000&ot=" + lastUpdated;
        NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {

            HttpRequestBase req = createGRRequest(httpClient, url);
            HttpResponse response = executeGRRequest(httpClient, req, true);

            throwExceptionWhenNotStatusOK(response);

            final List<StateChange> stateChanges = new ArrayList<StateChange>(25);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            SAXParser parser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new SimpleStringExtractorHandler() {

                private String currentAtomId;
                String googleUserId = null; // cache

                @Override
                public void receivedString(String localName, String fqn, String s) {

                    if ("number".equals(localName)) {
                        long l = Long.parseLong(s);
                        currentAtomId = TAG_GR_ITEM + U.longToHex(l);
                    } else if ("string".equals(localName)) {

                        boolean delete = s.startsWith("delete");

                        int state = -1;

                        if (s.endsWith("read"))
                            state = BackendProvider.StateChange.STATE_READ;

                        else if (s.endsWith("starred"))
                            state = BackendProvider.StateChange.STATE_STARRED;

                        if (state > -1) {
                            EntriesRetriever.StateChange sc = new EntriesRetriever.StateChange(currentAtomId, state,
                                    delete ? BackendProvider.StateChange.OPERATION_REMOVE
                                            : BackendProvider.StateChange.OPERATION_ADD);
                            stateChanges.add(sc);
                        }
                    }
                }
            };

            parser.parse(NewsRobHttpClient.getUngzippedContent(response.getEntity(), context), handler);
            PL.log("Entries Retriever: Number of state changes=" + stateChanges.size(), context);
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("State Changes: " + stateChanges, context);
            return stateChanges;
        } finally {
            httpClient.close();
            t.stop();
        }

    }

    private void throwExceptionWhenNotStatusOK(HttpResponse response) throws IOException {
        final int statusCode = response.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK != statusCode)
            throw new IOException("Statuscode should have been 200, but was " + statusCode);
    }

    private void maintainLastUpdated(long lastUpdated) {
        getEntryManager().setGRUpdated(lastUpdated);
    }

    public void logout() {
        clearAuthToken();
        getEntryManager().setGoogleUserId(null);
    }

    private void clearAuthToken() {
        getEntryManager().clearAuthToken();
    }

    protected static class ReadingListStreamHandler extends SimpleStringExtractorHandler {
        private final FetchContext fetchCtx;
        private final Job job;

        String googleUserId;
        Entry newEntry;
        boolean authorUnknown;
        private boolean filterOutNewsRobIgnore;

        private List<Entry> entriesToBeInserted = new ArrayList<Entry>(20);

        private EntryManager entryManager;

        private boolean skip = false;

        protected ReadingListStreamHandler(EntryManager entryManager, FetchContext fetchCtx, Job job) {
            this.fetchCtx = fetchCtx;
            this.job = job;
            this.entryManager = entryManager;
            this.googleUserId = entryManager.getGoogleUserId();
            this.filterOutNewsRobIgnore = "1".equals(NewsRob.getDebugProperties(entryManager.getContext()).getProperty(
                    "filterOutNewsRobIgnore", "0"));

        }

        @Override
        public void receivedString(String localTagName, String fullyQualifiedTagName, String value) {

            // fullyQualifiedTagName + " = " + value);
            if ("/feed/id".equals(fullyQualifiedTagName)) {
                Matcher m = PATTERN_GOOGLE_USER_ID_IN_FEED_ID.matcher(value);
                if (m.find())
                    entryManager.setGoogleUserId(m.group(1));
                googleUserId = entryManager.getGoogleUserId();
                if (googleUserId == null)
                    throw new IllegalStateException("No Google ID found in /feed/id element.");
            } else {
                if (newEntry == null)
                    return;

                if ("/feed/entry/id".equals(fullyQualifiedTagName)) {
                    newEntry.setAtomId(value);

                } else if ("/feed/entry/title".equals(fullyQualifiedTagName)) {
                    newEntry.setTitle(U.htmlToText(value));

                } else if (!authorUnknown && "/feed/entry/author/name".equals(fullyQualifiedTagName)) {
                    newEntry.setAuthor(value);

                } else if ("/feed/entry/content".equals(fullyQualifiedTagName)
                        || "/feed/entry/summary".equals(fullyQualifiedTagName)) {
                    if ("summary".equals(localTagName)
                            && (newEntry.getContent() != null || newEntry.getContentURL() != null))
                        return; // don't overwrite the actual content
                    newEntry.setContent(value);

                } else if ("/feed/entry/source/title".equals(fullyQualifiedTagName)) {
                    newEntry.setFeedTitle(U.htmlToText(value));

                } else if ("/feed/entry/source/id".equals(fullyQualifiedTagName)) {
                    newEntry.setFeedAtomId(value);
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, name, attributes);

            final String fqpn = getFullyQualifiedPathName();
            if (job.isCancelled())
                throw new FetchCancelledException();

            if ("category".equals(localName) && GOOGLE_SCHEME.equals(attributes.getValue("scheme"))) {
                String term = attributes.getValue("term");
                if (term != null && term.indexOf(googleUserId) > -1) {

                    // PINNED ISSUE (SOLVED)
                    boolean isStatus = term.indexOf("state/com.google") > -1 || term.endsWith("/pinned");
                    String label = attributes.getValue("label");
                    if (label != null) {
                        if (filterOutNewsRobIgnore && "newsrob-ignore".equals(label))
                            skip = true;
                        if (isStatus) {
                            if (label.equals("read")) {
                                newEntry.setReadState(ReadState.READ);
                            } else if (label.equals("starred")) {
                                newEntry.setStarred(true);
                            } else if (label.equals("pinned")) {
                                System.out.println("xxx " + newEntry.getAtomId() + " pinned");
                            }
                        } else {

                            if (!label.equals("newsrob")) {
                                // not a source link, then it must be a user
                                // label

                                if (term.indexOf("/source/com.google/link") > -1) {
                                } else {

                                    // ignore pinned state labels
                                    if (!term.endsWith("/pinned")) {
                                        // skip bundles
                                        boolean isBundleLabel = (term.indexOf("label") == -1 && term.indexOf("bundle") > -1);

                                        Label l = new Label();
                                        l.setName(isBundleLabel ? label + " bundle" : label);
                                        newEntry.addLabel(l);
                                    }

                                }
                            }
                        }
                    }
                }

            } else if ("entry".equals(localName)) {

                newEntry = new Entry();
                authorUnknown = false;

                String s = attributes.getValue(GOOGLE_SCHEMA, "crawl-timestamp-msec");

                if (s != null) {

                    long l = Long.parseLong(s);
                    newEntry.setUpdated(l);

                    long crawlTime = l / 1000;
                    if (crawlTime > fetchCtx.tempLastUpdated)
                        fetchCtx.tempLastUpdated = crawlTime;

                }

            } else if ("/feed/entry/link".equals(fqpn) && "alternate".equals(attributes.getValue("rel"))
                    && attributes.getValue("href") != null) {
                newEntry.setAlternateHRef(attributes.getValue("href"));

            } else if ("/feed/entry/content".equals(fqpn) || "/feed/entry/summary".equals(fqpn)) {
                if ("summary".equals(localName) && newEntry.getContentType() != null)
                    return; // don't overwrite content from "content"

                newEntry.setContentType(attributes.getValue("type"));
                if (newEntry.getContentType() == null)
                    newEntry.setContentType("text"); // LATER Does this
                // make any
                // sense?

                newEntry.setContentURL(attributes.getValue("src"));
            } else if ("/feed/entry/author".equals(fqpn)) {

                String s = attributes.getValue(GOOGLE_SCHEMA, "unknown-author");
                if (s != null)
                    authorUnknown = "true".equals(s);
            } else if ("/feed/entry/source/link".equals(fqpn)) {
                if ("alternate".equals(attributes.getValue("rel")))
                    newEntry.setFeedAlternateUrl(attributes.getValue("href"));

            }

        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            super.endElement(uri, localName, name);
            if ("entry".equals(localName)) {
                fetchCtx.countSeenEntries++;

                // TOD Why exactly do I do this?
                if (newEntry.getAlternateHRef() != null
                        && (newEntry.getAlternateHRef().indexOf("twitter.com") >= 0 || newEntry.getAlternateHRef()
                                .indexOf("www.facebook.com") >= 0))
                    newEntry.setContent(null);

                else if (newEntry.getFeedAtomId().endsWith("/source/com.google/link")
                        && newEntry.getFeedAtomId().indexOf(googleUserId) > -1) {
                    newEntry.setTitle(newEntry.getTitle() + " (" + newEntry.getFeedTitle() + ")");
                    newEntry.setFeedTitle("Your Notes");
                }

                if (!skip) {
                    // check for update or re-entry
                    Entry existingEntry = entryManager.findEntryByAtomId(newEntry.getAtomId());
                    boolean stateUpdated = false;

                    if (existingEntry != null) {

                        // PINNED ISSUE TODO
                        if (existingEntry.isRead() != newEntry.isRead() && !existingEntry.isReadStatePending()) {
                            entryManager.updateReadState(existingEntry, newEntry.getReadState(), false, false);
                            stateUpdated = true;
                        }

                        if (existingEntry.isStarred() != newEntry.isStarred() && !existingEntry.isStarredStatePending()) {
                            entryManager.updateStarredState(existingEntry, newEntry.isStarred(), false);
                            stateUpdated = true;
                        }

                        if (stateUpdated)
                            fetchCtx.countFetchedEntries++;
                    } else {
                        // save it
                        entriesToBeInserted.add(newEntry);
                        if (entriesToBeInserted.size() == 10) {
                            entryManager.insert(entriesToBeInserted);
                            entriesToBeInserted.clear();
                            entryManager.fireModelUpdated();
                        }

                    }
                }
                skip = false;

                //

                fetchCtx.countFetchedEntries++;
                // if (fetchCtx.countFetchedEntries % 20 == 0)
                // entryManager.fireModelUpdated();

                newEntry = null;

                if (job.isCancelled())
                    throw new FetchCancelledException();

            }

        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if (entriesToBeInserted.size() > 0) {
                entryManager.insert(entriesToBeInserted);
                entryManager.fireModelUpdated();
            }
        }
    }

    public static class FetchContext {
        // raw number of entries processed
        int countSeenEntries;

        // number of entries that changed the database
        int countFetchedEntries;

        long tempLastUpdated = -1L;
    }

    @SuppressWarnings("serial")
    static class FetchCancelledException extends RuntimeException {
    }

    @SuppressWarnings("serial")
    static class UpdateSubscriptionsCancelledException extends RuntimeException {
    }

    public void updateSubscriptionList(final EntryManager entryManager, final Job job) throws IOException,
            ParserConfigurationException, SAXException, AuthenticationExpiredException {

        if (job.isCancelled())
            return;

        if (entryManager.getLastSyncedSubscriptions() != -1l
                && System.currentTimeMillis() < entryManager.getLastSyncedSubscriptions() + ONE_DAY_IN_MS) {
            PL.log("Not updating subscription list this time.", context);
            return;
        }
        PL.log("Updating subscription list.", context);

        Timing t = new Timing("UpdateSubscriptionList", context);
        final NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false, context);
        try {

            final String url = getGoogleHost() + "/reader/api/0/subscription/list";
            HttpRequestBase req = createGRRequest(httpClient, url);
            HttpResponse response;
            try {
                response = executeGRRequest(httpClient, req, true);
            } catch (ServerBadRequestException e) {
                throw new IOException("GR: Bad Request.");
            }

            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser parser = saxParserFactory.newSAXParser();

            final Map<String, String> remoteTitlesAndIds = new HashMap<String, String>(107);

            DefaultHandler handler = new SimpleStringExtractorHandler() {

                private String currentFeedAtomId;
                private String currentString;

                @Override
                public void startElement(String uri, String localName, String name, Attributes attributes)
                        throws SAXException {
                    if (job.isCancelled())
                        throw new UpdateSubscriptionsCancelledException();
                    super.startElement(uri, localName, name, attributes);
                    String fqn = getFullyQualifiedPathName();

                    if ("/object/list/object".equals(fqn)) {
                        currentFeedAtomId = null;
                    } else if ("/object/list/object/string".equals(fqn)) {
                        currentString = attributes.getValue("name");
                    }
                }

                @Override
                public void receivedString(String localName, String fqn, String s) {

                    if (!"/object/list/object/string".equals(fqn))
                        return;

                    if ("id".equals(currentString))
                        currentFeedAtomId = TAG_GR + s;
                    else if ("title".equals(currentString)) {
                        if (currentFeedAtomId != null)
                            remoteTitlesAndIds.put(currentFeedAtomId, s);
                        // entryManager.updateFeedName(currentFeedAtomId, s);
                    }
                }
            };

            parser.parse(NewsRobHttpClient.getUngzippedContent(response.getEntity(), context), handler);
            if (NewsRob.isDebuggingEnabled(context))
                PL.log("Got subscription list with " + remoteTitlesAndIds.size() + " feeds.", context);
            if (job.isCancelled())
                return;

            entryManager.updateFeedNames(remoteTitlesAndIds);
        } finally {
            httpClient.close();
            t.stop();
        }
        entryManager.updateLastSyncedSubscriptions(System.currentTimeMillis());
    }

    @Override
    public String getServiceName() {
        return "Google Reader";
    }

    @Override
    public String getServiceUrl() {
        return "http://www.google.com/reader";
    }

}
