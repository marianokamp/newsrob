package com.newsrob;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;

import com.newsblur.network.APIManager;
import com.newsblur.network.domain.FeedFolderResponse;
import com.newsblur.network.domain.LoginResponse;
import com.newsrob.jobs.Job;
import com.newsrob.util.Timing;

public class NewsBlurBackendProvider implements BackendProvider {
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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateSubscriptionList(EntryManager entryManager, Job job) throws IOException,
            ParserConfigurationException, SAXException, ServerBadRequestException, AuthenticationExpiredException {

        if (apiManager == null) {
            try {
                authenticate(null, null, null, null, null);
            } catch (AuthenticationFailedException e) {
                e.printStackTrace();
                return;
            }
        }

        if (job.isCancelled())
            return;

        /*
         * if (entryManager.getLastSyncedSubscriptions() != -1l &&
         * System.currentTimeMillis() <
         * entryManager.getLastSyncedSubscriptions() + ONE_DAY_IN_MS) {
         * PL.log("Not updating subscription list this time.", context); return;
         * }
         */

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
