package com.newsrob;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;

import com.newsrob.jobs.Job;

public interface SyncInterface {

    static class StateChange {
        static final int OPERATION_REMOVE = 0;
        static final int OPERATION_ADD = 1;
        static final int STATE_READ = 2;
        static final int STATE_STARRED = 3;

        private int state;
        private int operation;

        int getState() {
            return state;
        }

        int getOperation() {
            return operation;
        }

        String getAtomId() {
            return atomId;
        }

        private String atomId;

        StateChange(String atomId, int state, int operation) {
            this.atomId = atomId;
            this.state = state;
            this.operation = operation;
        }

        @Override
        public String toString() {
            String stateLabel = "State?";
            switch (state) {
            case STATE_READ:
                stateLabel = "read";
                break;
            case STATE_STARRED:
                stateLabel = "starred";
                break;
            }

            String operationLabel = operation == OPERATION_ADD ? "add" : "remove";
            return "State: " + operationLabel + " " + stateLabel + " for " + getAtomId() + ".";
        }

    }

    public static class AuthToken {
        enum AuthType {
            AUTH_STANDALONE, AUTH
        };

        private AuthType type;
        private String authToken;

        AuthToken(AuthType type, String authToken) {
            this.setType(type);
            this.authToken = authToken;
        }

        AuthType getAuthType() {
            return getType();
        }

        String getAuthToken() {
            return authToken;
        }

        public String toString() {
            return "AuthToken " + authToken.substring(0, 4) + " of type " + getType() + ".";
        }

        public AuthType getType() {
            return type;
        }

        public void setType(AuthType type) {
            this.type = type;
        }
    }

    public List<DiscoveredFeed> discoverFeeds(final String query) throws ReaderAPIException, IOException,
            GRTokenExpiredException, ParserConfigurationException, SAXException, GRAnsweredBadRequestException;

    public boolean submitSubscribe(String url2subscribe) throws ReaderAPIException;

    public void submitNotes(Job job) throws ReaderAPIException;

    /**
     * differentialUpdateOfArticlesStates is where the actual exact syncing
     * magic happens
     */
    public void differentialUpdateOfArticlesStates(final EntryManager entryManager, Job job, String stream,
            String excludeState, ArticleDbState articleDbState) throws SAXException, IOException,
            ParserConfigurationException, GRTokenExpiredException, GRAnsweredBadRequestException;

    public void unsubscribeFeed(String feedAtomId) throws IOException, NeedsSessionException, ReaderAPIException;

    public boolean authenticate(Context context, String email, String password, String captchaToken,
            String captchaAnswer) throws ClientProtocolException, IOException, AuthenticationFailedException;

    public int fetchNewEntries(final EntryManager entryManager, final SyncJob job, boolean manualSync)
            throws ClientProtocolException, IOException, NeedsSessionException, SAXException, IllegalStateException,
            ParserConfigurationException, FactoryConfigurationError, ReaderAPIException, GRTokenExpiredException;

    public void updateSubscriptionList(EntryManager entryManager, Job job) throws IOException,
            ParserConfigurationException, SAXException, GRTokenExpiredException;

    public void logout();

    public abstract int synchronizeWithGoogleReader(EntryManager entryManager, SyncJob syncJob)
            throws MalformedURLException, IOException, ParserConfigurationException, FactoryConfigurationError,
            SAXException, ParseException, NeedsSessionException, ParseException;

}