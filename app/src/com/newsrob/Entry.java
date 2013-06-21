package com.newsrob;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.newsrob.storage.AbstractStorageAdapter;
import com.newsrob.util.U;

public class Entry {

    private String alternateHRef;

    private String content;
    private String contentType;
    private String contentURL;

    private String error;

    private String atomId;
    private long id = -1;
    private String hash;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    private String title;

    private String feedTitle;
    private long feedId;
    private String feedAtomId;

    private boolean isStarred = false;
    private boolean isStarredStatePending = false;

    private boolean isReadStatePending = false;

    private String feedAlternateUrl;
    private String snippet;

    private int downloaded = STATE_NOT_DOWNLOADED;

    public static final int STATE_NOT_DOWNLOADED = 0;
    public static final int STATE_DOWNLOADED_FEED_CONTENT = 1;
    public static final int STATE_DOWNLOADED_FULL_PAGE = 2;
    public static final int STATE_DOWNLOAD_ERROR = 3;

    private static final Map<String, String> mobilizersMap = new HashMap<String, String>();

    private List<Label> labels = new ArrayList<Label>();
    private long updated;

    private String author;

    private int downloadPref;

    private int displayPref;

    private boolean javaScriptEnabled;
    private boolean fitToWidthEnabled = true;

    private ReadState readState = ReadState.UNREAD;

    private boolean isPinnedStatePending;

    public Entry(long id) {
        this.id = id;

        mobilizersMap.put("instapaper", "http://www.instapaper.com/m?u=");
        mobilizersMap.put("gwt", "http://www.google.com/gwt/n?u=");
        mobilizersMap.put("readability", "https://www.readability.com/read?url=");
    }

    public Entry() {
        this(-1l);
    }

    List<Label> getLabels() {
        return labels;
    }

    public void addLabel(Label label) {
        labels.add(label);
    }

    void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedAtomId(String feedAtomId) {
        this.feedAtomId = feedAtomId;
    }

    String getFeedAtomId() {
        return feedAtomId;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public boolean isStarredStatePending() {
        return isStarredStatePending;
    }

    void setStarred(boolean newValue) {
        isStarred = newValue;
    }

    void setStarredStatePending(boolean newValue) {
        isStarredStatePending = newValue;
    }

    public boolean isReadStatePending() {
        return isReadStatePending;
    }

    public boolean isPinnedStatePending() {
        return isPinnedStatePending;
    }

    public void setPinnedStatePending(boolean newValue) {
        isPinnedStatePending = newValue;
    }

    public void setReadStatePending(boolean newValue) {
        isReadStatePending = newValue;
    }

    void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public String getFeedTitle() {
        return feedTitle != null ? feedTitle : "Untitled";
    }

    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public long getId() {
        return id;
    }

    public String getAlternateHRef() {
        return alternateHRef;
    }

    public String getContentURL() {
        return contentURL;
    }

    void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public String getContent() {
        return content;
    }

    long getContentSize() {
        long contentSize = 0;
        if (content != null)
            contentSize = content.length();
        return contentSize;
    }

    public String getContentType() {
        if (contentType == null)
            return "html";
        return contentType;
    }

    public String getAtomId() {
        return this.atomId;
    }

    public void setAtomId(String atomId) {
        this.atomId = atomId;
    }

    public String getAuthor() {
        return author;
    }

    void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    void setAlternateHRef(String alternateHRef) {
        this.alternateHRef = alternateHRef;
    }

    void setContent(String content) {
        this.content = content;
        setSnippet(null);
    }

    void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSnippet() {
        if (snippet == null) {
            String s = null;
            if (content != null) {
                s = content;

                s = U.htmlToText(s);
                if (s.length() > 700)
                    s = s.substring(0, 700);

                int idx = s.indexOf(getTitle());
                if (idx > -1 && idx < (70 + getTitle().length()))
                    // eliminate content that is only the
                    // repeated title,
                    // but keep the content if the title is repeated
                    // later on in the body
                    s = null;
            }
            snippet = s;
        }
        return snippet;
    }

    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        StringBuilder labelsRepresentation = new StringBuilder();
        for (Label label : labels) {
            labelsRepresentation.append(label.getName() + ", ");
        }
        String labelsString;
        if (labelsRepresentation.length() > 0)
            labelsString = labelsRepresentation.substring(0, labelsRepresentation.length() - 2);
        else
            labelsString = labelsRepresentation.toString();

        return String
                .format("Entry title: %s, row-id: %s, atom-id: %s labels: %s, alternate: %s, content-size: %10d, content-type: %s content:\n%s",
                        title, id, atomId, labelsString, alternateHRef, getContentSize(), contentType, content);
    }

    public void setUpdated(long entryUpdated) {
        this.updated = entryUpdated;
    }

    public Date getUpdated() {
        return new Date(updated);
    }

    public long getUpdatedInHighResolution() {
        return updated;
    }

    void setDownloaded(int downloaded) {
        this.downloaded = downloaded;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public String getShortAtomId() {
        return getShortAtomId(atomId);
    }

    public static String getShortAtomId(String fullAtomId) {
        return AbstractStorageAdapter.longAtomIdToShortAtomId(fullAtomId);
    }

    public void setDownloadPref(int downloadPref) {
        this.downloadPref = downloadPref;
    }

    public void setDisplayPref(int displayPref) {
        this.displayPref = displayPref;
    }

    public int getDisplayPref() {
        return displayPref;
    }

    public int getDownloadPref() {
        return downloadPref;
    }

    public int getResolvedDownloadPref(EntryManager entryManager) {
        return (getDownloadPref() == Feed.DOWNLOAD_PREF_DEFAULT ? entryManager.getDefaultDownloadPref()
                : getDownloadPref());

    }

    public int getResolvedDisplayPref(EntryManager entryManager) {
        if (getDisplayPref() == Feed.DISPLAY_PREF_DEFAULT) {
            int resolvedDownloadPref = getResolvedDownloadPref(entryManager);
            if (resolvedDownloadPref == Feed.DOWNLOAD_HEADERS_ONLY
                    || resolvedDownloadPref == Feed.DOWNLOAD_PREF_FEED_ONLY)
                return Feed.DISPLAY_PREF_FEED;
            return Feed.DISPLAY_PREF_WEBPAGE;
        }
        return getDisplayPref();
    }

    public String getBaseUrl(EntryManager entryManager) {
        if (getAlternateHRef() == null)
            return "";

        if (shouldUseMobilizer(entryManager)) {

            final String mobilizer = entryManager.getMobilizer();
            final String mobilizerUrl = mobilizersMap.get(mobilizer);

            if (mobilizerUrl != null)
                return mobilizerUrl + URLEncoder.encode(getAlternateHRef());
        }

        return getAlternateHRef();
    }

    private boolean shouldUseMobilizer(EntryManager entryManager) {
        return getResolvedDownloadPref(entryManager) == Feed.DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public void setJavaScriptEnabled(boolean enabled) {
        this.javaScriptEnabled = enabled;
    }

    public boolean isFitToWidthEnabled() {
        return fitToWidthEnabled;
    }

    public void setFitToWidthEnabled(boolean enabled) {
        this.fitToWidthEnabled = enabled;
    }

    public void setFeedAlternateUrl(String feedAlternateUrl) {
        this.feedAlternateUrl = feedAlternateUrl;
    }

    public String getFeedAlternateUrl() {
        return feedAlternateUrl;
    }

    public void setReadState(ReadState readState) {
        this.readState = readState;
    }

    public ReadState getReadState() {
        return readState;
    }

    public final boolean isRead() {
        return readState == ReadState.READ;
    }

    public final File getAssetsDir(EntryManager em) {
        return new File(em.getStorageAdapter().getAbsolutePathForAsset("a" + this.getShortAtomId()));
    }

    public static final File getAssetsDir(EntryManager em, String shortAtomId) {
        return new File(em.getStorageAdapter().getAbsolutePathForAsset("a" + shortAtomId));
    }

    public static final File getThumbnailFile(EntryManager em, String shortAtomId) {

        File f = new File(Entry.getAssetsDir(em, shortAtomId), "preview.pngnr");
        if (f.exists())
            return f;
        else
            return null;
    }
}
