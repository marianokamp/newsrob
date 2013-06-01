package com.newsrob;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.newsrob.storage.SdCardStorageAdapter;
import com.newsrob.util.SimpleStringExtractorHandler;

public class Feed {

    private long id = -1l;
    private String title;
    private String atomId;
    private String url;
    private int downloadPref = -1;
    private int displayPref = -1;
    private boolean notificationEnabled = false;
    private String alternateUrl;

    private static final String FEED_SETTINGS_FILE_NAME = "feed_preferences.settings";

    public static final int DOWNLOAD_PREF_DEFAULT = 0;
    public static final int DOWNLOAD_HEADERS_ONLY = 1;
    public static final int DOWNLOAD_PREF_FEED_ONLY = 2;
    public static final int DOWNLOAD_PREF_FEED_AND_MOBILE_WEBPAGE = 3;
    public static final int DOWNLOAD_PREF_FEED_AND_WEBPAGE = 4;

    public static final int DISPLAY_PREF_DEFAULT = 0;
    public static final int DISPLAY_PREF_FEED = 1;
    public static final int DISPLAY_PREF_WEBPAGE = 2;

    private float webScale = -1.0f;
    private float feedScale = -1.0f;

    private boolean javaScriptEnabled = false;
    private boolean fitToWidthEnabled = true;

    public String getAtomId() {
        return atomId;
    };

    public void setAtomId(String atomId) {
        this.atomId = atomId;
    }

    public long getId() {
        return id;
    }

    void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public int getDownloadPref() {
        return downloadPref;
    }

    public void setDownloadPref(int downloadPref) {
        this.downloadPref = downloadPref;
    }

    public int getDisplayPref() {
        return displayPref;
    }

    public void setDisplayPref(int displayPref) {
        this.displayPref = displayPref;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setWebScale(float webScale) {
        this.webScale = webScale;
    }

    public float getWebScale() {
        return webScale;
    }

    public void setFeedScale(float feedScale) {
        this.feedScale = feedScale;
    }

    public float getFeedScale() {
        if (feedScale > 0.001f || feedScale < -0.001f)
            return feedScale;
        return -1.0f;
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

    public final static void saveFeedSettings(Context context) {

        SdCardStorageAdapter storageAdapter = new SdCardStorageAdapter(context.getApplicationContext(), false);
        if (!storageAdapter.canWrite())
            return;

        List<Feed> feeds = EntryManager.getInstance(context).findAllFeeds();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(storageAdapter
                    .openFileOutput(FEED_SETTINGS_FILE_NAME), 8192))));

            pw.println("<feeds version='1'>");
            for (Feed feed : feeds) {
                StringBuilder sb = new StringBuilder("  <feed ");

                addAttribute(sb, "atomId", URLEncoder.encode(feed.getAtomId()));
                addAttribute(sb, "title", URLEncoder.encode(feed.getTitle()));

                addAttribute(sb, "downloadPref", String.valueOf(feed.getDownloadPref()));
                addAttribute(sb, "displayPref", String.valueOf(feed.getDisplayPref()));

                addAttribute(sb, "webScale", String.valueOf(feed.getWebScale()));
                addAttribute(sb, "feedScale", String.valueOf(feed.getFeedScale()));

                addAttribute(sb, "fitToWidthEnabled", String.valueOf(feed.isFitToWidthEnabled()));
                addAttribute(sb, "javaScriptEnabled", String.valueOf(feed.isJavaScriptEnabled()));
                addAttribute(sb, "notificationEnabled", String.valueOf(feed.isNotificationEnabled()));

                addAttribute(sb, "altUrl", String.valueOf(feed.getAlternateUrl()));

                sb.append("/>");
                pw.println(sb.toString());
            }

            pw.println("</feeds>");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (pw != null)
                pw.close();

        }
        Log.d("Feed", "Saved feeds.");

    }

    public final static boolean restoreFeedsIfNeccesary(Context context) {
        // initial startup?
        final EntryManager em = EntryManager.getInstance(context);
        final boolean feedListEmpty = em.getFeedCount() == 0;
        PL.log("Feed.restore() feedListEmpty=" + feedListEmpty, context);
        if (!feedListEmpty)
            return false;

        SdCardStorageAdapter storageAdapter = new SdCardStorageAdapter(context.getApplicationContext(), false);
        String fileName = storageAdapter.getAbsolutePathForAsset(FEED_SETTINGS_FILE_NAME);
        PL.log("File " + fileName + " exists? " + new File(fileName).exists(), context);
        if (!new File(fileName).exists()) {
            fileName = Environment.getExternalStorageDirectory().getPath() + "/newsrob/" + FEED_SETTINGS_FILE_NAME;
            PL.log("File(2) " + fileName + " exists? " + new File(fileName).exists(), context);
            if (!new File(fileName).exists()) {
                Log.w("Feed", "No " + fileName + " existing. Not trying to restore feeds.");
                return false;
            }
        }
        Log.i("Feed", "Trying to restore feeds.");

        try {

            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            SAXParser parser = saxParserFactory.newSAXParser();
            DefaultHandler handler = new SimpleStringExtractorHandler() {

                @Override
                public final void startElement(String uri, String localName, String name, Attributes attributes)
                        throws SAXException {
                    super.startElement(uri, localName, name, attributes);

                    if (!"feed".equals(localName))
                        return;

                    Feed f = new Feed();

                    f.setAtomId(URLDecoder.decode(attributes.getValue("atomId")));
                    f.setTitle(URLDecoder.decode(attributes.getValue("title")));

                    f.setDownloadPref(Integer.parseInt(attributes.getValue("downloadPref")));
                    f.setDisplayPref(Integer.parseInt(attributes.getValue("displayPref")));

                    f.setWebScale(Float.parseFloat(attributes.getValue("webScale")));
                    f.setFeedScale(Float.parseFloat(attributes.getValue("feedScale")));

                    f.setJavaScriptEnabled(Boolean.parseBoolean(attributes.getValue("javaScriptEnabled")));
                    try {
                        f.setFitToWidthEnabled(Boolean.parseBoolean(attributes.getValue("fitToWidthEnabled")));
                    } catch (RuntimeException rte) {
                        // skip as it may be missing. Default is true then.
                    }
                    f.setNotificationEnabled(Boolean.parseBoolean(attributes.getValue("notificationEnabled")));

                    f.setAlternateUrl(attributes.getValue("altUrl"));
                    em.insert(f);

                }

                @Override
                public void receivedString(String localTagName, String fullyQualifiedLocalName, String value) {
                }

            };

            parser.parse(new File(fileName), handler);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Log.i("Feed", "Restored feeds. Now " + em.getFeedCount() + " feeds in database.");
        return true;

    }

    private static final void addAttribute(StringBuilder sb, String key, String value) {
        sb.append(key + "='" + value + "' ");
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAlternateUrl(String alternateUrl) {
        this.alternateUrl = alternateUrl;
    }

    public String getAlternateUrl() {
        return alternateUrl;
    }

}
