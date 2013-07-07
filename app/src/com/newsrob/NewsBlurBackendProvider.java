package com.newsrob;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.content.Context;

import com.newsblur.domain.FeedResult;
import com.newsblur.domain.Story;
import com.newsblur.domain.ValueMultimap;
import com.newsblur.network.APIConstants;
import com.newsblur.network.APIManager;
import com.newsblur.network.ServerErrorException;
import com.newsblur.network.domain.FeedFolderResponse;
import com.newsblur.network.domain.LoginResponse;
import com.newsblur.network.domain.StoriesResponse;
import com.newsblur.network.domain.UnreadHashResponse;
import com.newsrob.DB.TempTable;
import com.newsrob.download.HtmlEntitiesDecoder;
import com.newsrob.jobs.Job;
import com.newsrob.util.Timing;

public class NewsBlurBackendProvider implements BackendProvider {
    private APIManager apiManager = null;
    private Context context;
    private EntryManager entryManager = null;

    public NewsBlurBackendProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public List<DiscoveredFeed> discoverFeeds(String query) throws SyncAPIException, IOException,
            ServerBadRequestException, ParserConfigurationException, SAXException, ServerBadRequestException,
            AuthenticationExpiredException {

        if (handleAuthenticate(getEntryManager()) == false)
            return null;

        try {
            FeedResult[] result = apiManager.searchForFeed(query);

            if (result != null) {
                List<DiscoveredFeed> ret = new ArrayList<DiscoveredFeed>();

                for (FeedResult f : result) {
                    DiscoveredFeed df = new DiscoveredFeed();
                    df.title = f.label;
                    df.feedUrl = f.url;
                    df.alternateUrl = f.url;
                    ret.add(df);
                }

                return ret;
            }
        } catch (ServerErrorException e) {
            PL.log("Error searching feeds", e, context);
        }
        return null;
    }

    @Override
    public boolean submitSubscribe(String url2subscribe) throws SyncAPIException {
        if (handleAuthenticate(entryManager) == false)
            return false;

        return apiManager.addFeed(url2subscribe, "");
    }

    @Override
    public void differentialUpdateOfArticlesStates(EntryManager entryManager, Job job, String stream,
            String excludeState, ArticleDbState articleDbState) throws SAXException, IOException,
            ParserConfigurationException, ServerBadRequestException, ServerBadRequestException,
            AuthenticationExpiredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsubscribeFeed(String feedAtomId) throws IOException, NeedsSessionException, SyncAPIException {
        if (handleAuthenticate(entryManager) == false)
            return;

        // The API gets grumpy if we don't send the folder, even though it's an
        // optional parameter
        FeedFolderResponse feedResponse = apiManager.getFolderFeedMapping(false);
        List<String> folders = getFolderNamesForFeed(feedResponse, feedAtomId);

        for (String folder : folders) {
            if (apiManager.deleteFeed(feedAtomId, folder)) {
                entryManager.removeFeedFromFeeds2Unsubscribe(feedAtomId);
            }
        }
    }

    @Override
    public boolean authenticate(Context context, String userId, String password, String captchaToken,
            String captchaAnswer) throws ClientProtocolException, IOException, AuthenticationFailedException {
        try {
            apiManager = new APIManager(context);
            LoginResponse login = apiManager.login(userId, password);
            return login.authenticated;
        } catch (Exception e) {
            String message = "Problem during authenticate: " + e.getMessage();
            PL.log(message, context);
        }

        return false;
    }

    private void syncServerReadStates(EntryManager entryManager, SyncJob job) {
        try {
            job.setJobDescription("Syncing server read states");
            UnreadHashResponse hashes = apiManager.getUnreadStoryHashes();

            entryManager.populateTempTableHashes(TempTable.READ_HASHES, hashes.flatHashList);
            entryManager.updateStatesFromTempTableHash(TempTable.READ_HASHES, ArticleDbState.READ);
            job.setJobDescription("Server read states synced");
        } catch (Exception e) {
            String message = "Problem during syncServerReadStates: " + e.getMessage();
            PL.log(message, context);
        }
    }

    @Override
    public int fetchNewEntries(EntryManager entryManager, SyncJob job, boolean manualSync)
            throws ClientProtocolException, IOException, NeedsSessionException, SAXException, IllegalStateException,
            ParserConfigurationException, FactoryConfigurationError, SyncAPIException, ServerBadRequestException,
            AuthenticationExpiredException {

        try {
            if (handleAuthenticate(entryManager) == false)
                return 0;

            job.setJobDescription("Fetching feed information");

            // Update the feed list, make sure we have feed records for
            // everything...
            List<Feed> feeds = entryManager.findAllFeeds();
            List<String> feedIds = new ArrayList<String>();
            FeedFolderResponse feedResponse = apiManager.getFolderFeedMapping(false);

            for (com.newsblur.domain.Feed nbFeed : feedResponse.feeds.values()) {
                feedIds.add(nbFeed.feedId);

                boolean found = false;

                for (Feed nrFeed : feeds) {
                    if (nrFeed != null && nrFeed.getAtomId().equals(nbFeed.feedId)) {
                        found = true;
                        break;
                    }
                }

                if (found == false) {
                    Feed newFeed = new Feed();
                    newFeed.setAtomId(nbFeed.feedId);
                    newFeed.setTitle(nbFeed.title);
                    newFeed.setUrl(nbFeed.address);
                    newFeed.setDownloadPref(Feed.DOWNLOAD_PREF_DEFAULT);
                    newFeed.setDisplayPref(Feed.DISPLAY_PREF_DEFAULT);

                    long id = entryManager.insert(newFeed);
                    newFeed.setId(id);

                    feeds.add(newFeed);
                }
            }

            job.setJobDescription("Fetching starred articles");
            fetchStarredStories(feeds, feedResponse);

            // Here we start getting stories.
            job.setJobDescription("Fetching new articles");
            job.actual = 0;
            entryManager.fireStatusUpdated();

            int count = fetchAndStoreArticles(entryManager, feedIds, feeds, feedResponse, job);
            job.actual = job.target;
            entryManager.fireStatusUpdated();

            return count;
        } catch (Exception e) {
            String message = "Problem during fetchNewEntries: " + e.getMessage();
            PL.log(message, context);
        }

        return 0;
    }

    private int fetchAndStoreArticles(EntryManager entryManager, List<String> feedIds, List<Feed> feeds,
            FeedFolderResponse feedResponse, SyncJob job) throws InterruptedException, ExecutionException,
            MalformedURLException, IOException, ParserConfigurationException, FactoryConfigurationError, SAXException,
            ParseException {

        int offset = 0;
        int fetchedStoryCount = 0;

        int maxCapacity = entryManager.getNewsRobSettings().getStorageCapacity();
        int currentUnreadArticlesCount = entryManager.getUnreadArticleCountExcludingPinned();

        // Store the unread hashes in the temp table, remove the ones we have,
        // then get a new list
        UnreadHashResponse hashes = apiManager.getUnreadStoryHashes();
        List<String> unreadHashes = hashes.flatHashList;
        entryManager.populateTempTableHashes(TempTable.READ_HASHES, unreadHashes);
        entryManager.removeLocallyExistingHashesFromTempTable();
        List<String> hashesToFetch = entryManager.getNewHashesToFetch(maxCapacity - currentUnreadArticlesCount);

        job.target = hashesToFetch.size();
        job.setJobDescription("Fetching Unread Articles");

        // Download and parse/store up to 100 articles at a time
        while (offset < hashesToFetch.size()) {
            int nextPackSize = Math.min(hashesToFetch.size() - offset, 100);
            if (nextPackSize == 0)
                break;

            List<String> currentPack = new ArrayList<String>(hashesToFetch.subList(offset, offset + nextPackSize));
            offset += nextPackSize;

            StoriesResponse stories = apiManager.getStoriesByHash(currentPack.toArray(new String[currentPack.size()]));
            fetchedStoryCount += parseStoriesResponse(stories, feeds, feedResponse, false);

            job.actual = fetchedStoryCount;
        }

        job.actual = job.target;
        return fetchedStoryCount;
    }

    private int fetchStarredStories(List<Feed> feeds, FeedFolderResponse feedResponse) throws SyncAPIException {
        int fetchedArticlesCount = 0;
        int maxStarredArticles = getEntryManager().getNoOfStarredArticlesToKeep();

        for (Integer pageNumber = 1; fetchedArticlesCount <= maxStarredArticles; pageNumber++) {
            StoriesResponse storiesResp = apiManager.getStarredStories(pageNumber.toString());

            if (storiesResp == null) {
                throw new SyncAPIException("Newsblur API returned a null response.");
            }

            // No stories? Just stop. The server does this sometimes....
            if (storiesResp.stories.length == 0)
                break;

            fetchedArticlesCount += parseStoriesResponse(storiesResp, feeds, feedResponse, true);
        }

        return fetchedArticlesCount;
    }

    private int parseStoriesResponse(StoriesResponse storiesResp, List<Feed> feeds, FeedFolderResponse feedResponse,
            boolean starred) {
        List<Entry> entriesToBeInserted = new ArrayList<Entry>(20);
        int articlesFetchedCount = 0;

        for (Story story : storiesResp.stories) {
            // Don't save one we already have.
            if (entryManager.findEntryByHash(story.storyHash) != null)
                continue;

            Entry newEntry = new Entry();
            newEntry.setAtomId(story.id);
            newEntry.setContentURL(story.permalink);
            newEntry.setContent(story.content);
            newEntry.setTitle(HtmlEntitiesDecoder.decodeString(story.title));
            newEntry.setReadState(story.read ? ReadState.READ : ReadState.UNREAD);
            newEntry.setFeedAtomId(story.feedId);
            newEntry.setAuthor(story.authors);
            newEntry.setAlternateHRef(story.permalink);
            newEntry.setHash(story.storyHash);
            newEntry.setStarred(starred);
            newEntry.setUpdated(story.date == null ? new Date().getTime() : story.date.getTime());

            if (starred) {
                newEntry.addLabel(new Label("Starred"));
            }

            // Fill in some data from the feed record....
            Feed nrFeed = getFeedFromAtomId(feeds, story.feedId);

            if (nrFeed != null) {
                newEntry.setFeedId(nrFeed.getId());
                newEntry.setDownloadPref(nrFeed.getDownloadPref());
                newEntry.setDisplayPref(nrFeed.getDisplayPref());

                List<String> labelNames = getFolderNamesForFeed(feedResponse, nrFeed.getAtomId());

                if (labelNames != null) {
                    for (String labelName : labelNames) {
                        // Skip label from NB client
                        if (labelName.contains("0000"))
                            continue;

                        newEntry.addLabel(new Label(labelName));
                    }
                }
            }

            entriesToBeInserted.add(newEntry);
            articlesFetchedCount++;
        }

        entryManager.insert(entriesToBeInserted);
        entriesToBeInserted.clear();
        entryManager.fireModelUpdated();

        return articlesFetchedCount;
    }

    private List<String> getFolderNamesForFeed(FeedFolderResponse folders, String feedId) {
        List<String> folderList = new ArrayList<String>();

        try {
            for (String folderName : folders.folders.keySet()) {
                for (Long id : folders.folders.get(folderName)) {
                    if (feedId.equals(id.toString())) {
                        folderList.add(folderName);
                    }
                }
            }

            return folderList;
        } catch (Exception e) {
            String message = "Problem during getFolderNameForFeed: " + e.getMessage();
            PL.log(message, context);
        }

        return null;
    }

    private Feed getFeedFromAtomId(List<Feed> feeds, String atomId) {
        try {
            for (Feed feed : feeds) {
                if (atomId.equals(feed.getAtomId()))
                    return feed;
            }
        } catch (Exception e) {
            String message = "Problem during getFeedFromAtomId: " + e.getMessage();
            PL.log(message, context);
        }

        return null;
    }

    private boolean handleAuthenticate(EntryManager entryManager) {
        try {
            return authenticate(this.context, entryManager.getEmail(), entryManager.getAuthToken().getAuthToken(),
                    null, null);
        } catch (Exception e) {
            String message = "Problem during handleAuthenticate: " + e.getMessage();
            PL.log(message, context);
        }

        return false;
    }

    @Override
    public void updateSubscriptionList(EntryManager entryManager, Job job) throws IOException,
            ParserConfigurationException, SAXException, ServerBadRequestException, AuthenticationExpiredException {
        Timing t = null;

        try {
            if (handleAuthenticate(entryManager) == false)
                return;

            if (job.isCancelled())
                return;

            if (entryManager.getLastSyncedSubscriptions() != -1l
                    && System.currentTimeMillis() < entryManager.getLastSyncedSubscriptions() + ONE_DAY_IN_MS) {
                PL.log("Not updating subscription list this time.", context);
                return;
            }

            PL.log("Updating subscription list.", context);

            t = new Timing("UpdateSubscriptionList", context);
            final Map<String, String> remoteTitlesAndIds = new HashMap<String, String>(107);

            FeedFolderResponse list = apiManager.getFolderFeedMapping(false);

            for (com.newsblur.domain.Feed feed : list.feeds.values()) {
                remoteTitlesAndIds.put(feed.feedId, feed.title);
            }

            if (NewsRob.isDebuggingEnabled(context))
                PL.log("Got subscription list with " + remoteTitlesAndIds.size() + " feeds.", context);

            entryManager.updateFeedNames(remoteTitlesAndIds);
            entryManager.updateLastSyncedSubscriptions(System.currentTimeMillis());
        } catch (NullPointerException e) {
            String message = "Problem during updateSubscriptionList: " + e.getMessage();
            PL.log(message, context);
        } finally {
            if (t != null)
                t.stop();
        }
    }

    @Override
    public void logout() {
        apiManager = null;
        getEntryManager().clearAuthToken();
        getEntryManager().setGoogleUserId(null);
    }

    @Override
    public int synchronizeArticles(EntryManager entryManager, SyncJob syncJob) throws MalformedURLException,
            IOException, ParserConfigurationException, FactoryConfigurationError, SAXException, ParseException,
            NeedsSessionException, ParseException {
        try {
            if (handleAuthenticate(entryManager) == false)
                return 0;

            syncServerReadStates(entryManager, syncJob);

            int noOfUpdated = 0;

            String[] fields = { DB.Entries.READ_STATE_PENDING, DB.Entries.STARRED_STATE_PENDING
            // DB.Entries.PINNED_STATE_PENDING
            };
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

                        List<Entry> currentPack = new ArrayList<Entry>(
                                allEntries.subList(offset, offset + nextPackSize));
                        offset += nextPackSize;
                        noOfUpdated += remotelyAlterState(currentPack, f, desiredState);
                        syncJob.actual = noOfUpdated;
                        entryManager.fireStatusUpdated();
                    }
                }
            }
            return noOfUpdated;
        } catch (NullPointerException e) {
            String message = "Problem during syncArticles: " + e.getMessage();
            PL.log(message, context);
        }
        return 0;
    }

    private final EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(context);
        return entryManager;
    }

    private int remotelyAlterState(Collection<Entry> entries, final String column, String desiredState) {
        if (column.equals(DB.Entries.READ_STATE_PENDING)) {
            return remotelyAlterReadState(entries, column, desiredState);
        } else if (column.equals(DB.Entries.STARRED_STATE_PENDING)) {
            return remotelyAlterStarredState(entries, column, desiredState);
        }

        return 0;
    }

    private int remotelyAlterStarredState(Collection<Entry> entries, final String column, String desiredState) {
        int numMarked = 0;
        List<String> ids = new ArrayList<String>(1);

        for (Entry entry : entries) {
            if (desiredState.equals("1")) {
                if (apiManager.markStoryAsStarred(entry.getFeedAtomId(), entry.getAtomId())) {
                    ids.clear();
                    ids.add(entry.getAtomId());
                    getEntryManager().removePendingStateMarkers(ids, column);
                    numMarked++;
                }
            }

            if (desiredState.equals("0")) {
                if (apiManager.markStoryAsUnStarred(entry.getFeedAtomId(), entry.getAtomId())) {
                    ids.clear();
                    ids.add(entry.getAtomId());
                    getEntryManager().removePendingStateMarkers(ids, column);
                    numMarked++;
                }
            }
        }

        return numMarked;
    }

    private int remotelyAlterReadState(Collection<Entry> entries, final String column, String desiredState) {
        try {
            if (desiredState.equals("1")) {
                ValueMultimap list = new ValueMultimap();

                for (Entry e : entries) {
                    list.put(e.getFeedAtomId(), e.getAtomId());
                }

                ContentValues values = new ContentValues();
                values.put(APIConstants.PARAMETER_FEEDS_STORIES, list.getJsonString());

                if (apiManager.markMultipleStoriesAsRead(values)) {
                    List<String> atomIds = new ArrayList<String>(entries.size());

                    for (Entry entry : entries) {
                        atomIds.add(entry.getAtomId());
                    }

                    getEntryManager().removePendingStateMarkers(atomIds, column);

                    return entries.size();
                } else {
                    String message = "Problem during marking entry as un-/read: ";
                    PL.log(message, context);
                }
            }

            if (desiredState.equals("0")) {
                int numMarked = 0;
                List<String> ids = new ArrayList<String>(1);
                for (Entry entry : entries) {
                    if (apiManager.markStoryAsUnRead(entry.getFeedAtomId(), entry.getAtomId())) {
                        ids.clear();
                        ids.add(entry.getAtomId());
                        getEntryManager().removePendingStateMarkers(ids, column);
                        numMarked++;
                    }
                }

                return numMarked;
            }
        } catch (Exception e) {
            String message = "Problem during marking entry as un-/read: " + e.getMessage();
            PL.log(message, context);
        } finally {
        }

        return 0;
    }

    @Override
    public String getServiceName() {
        return "NewsBlur";
    }

    @Override
    public String getServiceUrl() {
        return "http://www.newsblur.com";
    }
}
