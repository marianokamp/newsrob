package com.newsrob;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;

import com.newsblur.domain.Story;
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
        List<Entry> entriesToBeInserted = new ArrayList<Entry>(20);

        if (handleAuthenticate() == false)
            return 0;

        // Update the feed list, make sure we have feed records for
        // everything...
        List<Feed> feeds = entryManager.findAllFeeds();
        List<String> feedIds = new ArrayList<String>();
        Map<String, Long> feedAtomIdToId = new HashMap<String, Long>();
        FeedFolderResponse feedResponse = apiManager.getFolderFeedMapping(false);

        for (com.newsblur.domain.Feed nbFeed : feedResponse.feeds.values()) {
            feedIds.add(nbFeed.feedId);

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

        // Here we start getting stories. Need to limit based on the UI config
        // and figure out when we're really done from the API.
        for (Integer page = 1; articlesFetchedCount < 100; page++) {
            StoriesResponse storiesResp = apiManager.getStoriesForFeeds(feedIds.toArray(new String[feedIds.size()]),
                    page.toString(), StoryOrder.OLDEST, ReadFilter.UNREAD);

            for (Story story : storiesResp.stories) {
                // Skip importing stories we already have.
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

            // Handle leftovers from the 10 at a time insert block
            if (entriesToBeInserted.size() > 0) {
                entryManager.insert(entriesToBeInserted);
                entriesToBeInserted.clear();
                entryManager.fireModelUpdated();
            }
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
        // TODO Auto-generated method stub
        return 0;
    }

}
