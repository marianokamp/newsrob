package com.newsrob;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.newsblur.domain.Story;
import com.newsblur.domain.ValueMultimap;
import com.newsblur.network.APIConstants;
import com.newsblur.network.APIManager;
import com.newsblur.network.domain.FeedFolderResponse;
import com.newsblur.network.domain.LoginResponse;
import com.newsblur.network.domain.StoriesResponse;
import com.newsblur.util.ReadFilter;
import com.newsblur.util.StoryOrder;
import com.newsrob.jobs.Job;
import com.newsrob.util.Timing;

public class NewsBlurBackendProvider implements BackendProvider {
    private static final String TAG = NewsBlurBackendProvider.class.getName();

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean submitSubscribe(String url2subscribe) throws SyncAPIException {
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub

    }

    @Override
    public boolean authenticate(Context context, String email, String password, String captchaToken,
            String captchaAnswer) throws ClientProtocolException, IOException, AuthenticationFailedException {

        String userId = NewsRob.getDebugProperties(context).getProperty("syncUserId", null);
        String pass = NewsRob.getDebugProperties(context).getProperty("syncPassword", null);

        apiManager = new APIManager(this.context);
        LoginResponse login = apiManager.login(userId, pass);
        return login.authenticated;
    }

    @Override
    public int fetchNewEntries(EntryManager entryManager, SyncJob job, boolean manualSync)
            throws ClientProtocolException, IOException, NeedsSessionException, SAXException, IllegalStateException,
            ParserConfigurationException, FactoryConfigurationError, SyncAPIException, ServerBadRequestException,
            AuthenticationExpiredException {

        int articlesFetchedCount = 0;
        int nbUnreadCount = 0;
        int currentArticlesCount = entryManager.getArticleCount();
        List<Entry> entriesToBeInserted = new ArrayList<Entry>(20);

        if (handleAuthenticate() == false)
            return 0;

        // Update the feed list, make sure we have feed records for
        // everything...
        List<Feed> feeds = entryManager.findAllFeeds();
        List<String> feedIds = new ArrayList<String>();
        Map<String, Long> feedAtomIdToId = new HashMap<String, Long>();
        FeedFolderResponse feedResponse = apiManager.getFolderFeedMapping(true);

        for (com.newsblur.domain.Feed nbFeed : feedResponse.feeds.values()) {
            feedIds.add(nbFeed.feedId);
            nbUnreadCount += nbFeed.neutralCount += nbFeed.positiveCount += nbFeed.negativeCount;

            boolean found = false;

            for (Feed nrFeed : feeds) {
                if (nrFeed != null && nrFeed.getAtomId().equals(nbFeed.feedId)) {
                    found = true;
                    feedAtomIdToId.put(nrFeed.getAtomId(), nrFeed.getId());
                    break;
                }
            }

            if (found == false) {
                Feed newFeed = new Feed();
                newFeed.setAtomId(nbFeed.feedId);
                newFeed.setTitle(nbFeed.title);
                newFeed.setUrl(nbFeed.address);

                long id = entryManager.insert(newFeed);

                feedAtomIdToId.put(newFeed.getAtomId(), id);
            }
        }

        // Here we start getting stories.
        int maxCapacity = entryManager.getNewsRobSettings().getStorageCapacity();
        int seenArticlesCount = 0;

        for (Integer page = 1; articlesFetchedCount + currentArticlesCount <= maxCapacity
                && seenArticlesCount < nbUnreadCount; page++) {

            if (job.isCancelled())
                break;

            StoriesResponse storiesResp = apiManager.getStoriesForFeeds(feedIds.toArray(new String[feedIds.size()]),
                    page.toString(), StoryOrder.NEWEST, ReadFilter.UNREAD);

            for (Story story : storiesResp.stories) {
                seenArticlesCount++;

                // Don't save ones we already have.
                if (entryManager.entryExists(story.id))
                    continue;

                Entry newEntry = new Entry();
                newEntry.setAtomId(story.id);
                newEntry.setContentURL(story.permalink);
                newEntry.setContent(story.content);
                newEntry.setTitle(story.title);
                newEntry.setReadState(story.read ? ReadState.READ : ReadState.UNREAD);
                newEntry.setFeedAtomId(story.feedId);
                newEntry.setAuthor(story.authors);
                newEntry.setAlternateHRef(story.permalink);

                // Fill in some data from the feed record....
                Feed nrFeed = getFeedFromAtomId(feeds, feedAtomIdToId, story.feedId);

                if (nrFeed != null) {
                    newEntry.setFeedId(nrFeed.getId());
                    newEntry.setDownloadPref(nrFeed.getDownloadPref());
                    newEntry.setDisplayPref(nrFeed.getDisplayPref());

                    String labelName = getFolderNameForFeed(feedResponse, nrFeed.getAtomId());

                    if (labelName != null) {
                        Label l = new Label();
                        l.setName(labelName);
                        newEntry.addLabel(l);
                    }
                }

                entriesToBeInserted.add(newEntry);
                articlesFetchedCount++;

                if (entriesToBeInserted.size() == 10) {
                    entryManager.insert(entriesToBeInserted);
                    entriesToBeInserted.clear();
                    entryManager.fireModelUpdated();
                }
            }
        }

        // Handle leftovers from the 10 at a time insert block
        if (entriesToBeInserted.size() > 0) {
            entryManager.insert(entriesToBeInserted);
            entriesToBeInserted.clear();
            entryManager.fireModelUpdated();
        }

        return articlesFetchedCount;
    }

    private String getFolderNameForFeed(FeedFolderResponse folders, String feedId) {
        for (String folderName : folders.folders.keySet()) {
            for (Long id : folders.folders.get(folderName)) {
                if (feedId.equals(id.toString())) {
                    return folderName;
                }
            }
        }

        return null;
    }

    private Feed getFeedFromAtomId(List<Feed> feeds, Map<String, Long> feedAtomIdToId, String atomId) {
        Long id = feedAtomIdToId.get(atomId);
        if (id != null) {
            for (Feed feed : feeds) {
                if (id.equals(feed.getId()))
                    return feed;
            }
        }

        return null;
    }

    private boolean handleAuthenticate() {
        try {
            return authenticate(null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void updateSubscriptionList(EntryManager entryManager, Job job) throws IOException,
            ParserConfigurationException, SAXException, ServerBadRequestException, AuthenticationExpiredException {

        if (handleAuthenticate() == false)
            return;

        if (job.isCancelled())
            return;

        if (entryManager.getLastSyncedSubscriptions() != -1l
                && System.currentTimeMillis() < entryManager.getLastSyncedSubscriptions() + ONE_DAY_IN_MS) {
            PL.log("Not updating subscription list this time.", context);
            return;
        }

        PL.log("Updating subscription list.", context);

        Timing t = new Timing("UpdateSubscriptionList", context);
        try {
            final Map<String, String> remoteTitlesAndIds = new HashMap<String, String>(107);

            FeedFolderResponse list = apiManager.getFolderFeedMapping(false);

            for (com.newsblur.domain.Feed feed : list.feeds.values()) {
                remoteTitlesAndIds.put(feed.feedId, feed.title);
            }

            if (NewsRob.isDebuggingEnabled(context))
                PL.log("Got subscription list with " + remoteTitlesAndIds.size() + " feeds.", context);

            entryManager.updateFeedNames(remoteTitlesAndIds);
            entryManager.updateLastSyncedSubscriptions(System.currentTimeMillis());
        } finally {
            t.stop();
        }
    }

    @Override
    public void logout() {
        apiManager = null;
    }

    @Override
    public int synchronizeArticles(EntryManager entryManager, SyncJob syncJob) throws MalformedURLException,
            IOException, ParserConfigurationException, FactoryConfigurationError, SAXException, ParseException,
            NeedsSessionException, ParseException {

        if (handleAuthenticate() == false)
            return 0;

        int noOfUpdated = 0;

        String[] fields = { DB.Entries.READ_STATE_PENDING // ,
        // DB.Entries.STARRED_STATE_PENDING,
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

    private final EntryManager getEntryManager() {
        if (entryManager == null)
            entryManager = EntryManager.getInstance(context);
        return entryManager;
    }

    private int remotelyAlterState(Collection<Entry> entries, final String column, String desiredState) {
        // NewsRobHttpClient httpClient = NewsRobHttpClient.newInstance(false,
        // context);

        try {
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
                Log.e(TAG, message);
            }
        } catch (Exception e) {
            String message = "Problem during marking entry as un-/read: " + e.getMessage();
            Log.e(TAG, message, e);
        } finally {
            // httpClient.close();
        }

        return 0;
    }
}
