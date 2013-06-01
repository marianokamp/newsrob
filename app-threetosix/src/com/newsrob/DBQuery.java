package com.newsrob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.newsrob.threetosix.R;
import com.newsrob.util.SDK9Helper;

public class DBQuery {
    private boolean hideReadItems;
    private String filterLabel;
    private Long filterFeed;
    private long startDate;
    private boolean changed;
    private boolean sortAscending;
    private long dateLimit;
    private EntryManager entryManager;
    private int limit;

    private String internalRepresentation;

    private String getInternalRepresentation() {
        if (internalRepresentation == null)
            internalRepresentation = "" + hideReadItems + "!" + filterLabel + "!" + filterFeed + "!" + startDate + "!"
                    + changed + "!" + sortAscending + "!" + dateLimit + " limit=" + limit;
        return internalRepresentation;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DBQuery))
            return false;
        DBQuery other = (DBQuery) o;
        return getInternalRepresentation().equals(other.getInternalRepresentation());
    }

    @Override
    public int hashCode() {
        return getInternalRepresentation().hashCode();
    }

    public DBQuery(EntryManager entryManager, String filterLabel, Long filterFeedId) {
        this.hideReadItems = entryManager.shouldHideReadItems();
        this.filterLabel = filterLabel;
        this.filterFeed = filterFeedId;
        this.sortAscending = !entryManager.shouldShowNewestArticlesFirst();
        this.entryManager = entryManager;
        changed = true;
    }

    public DBQuery(DBQuery dbQuery) {
        if (dbQuery == null)
            throw new NullPointerException("dbQuery cannot be null at this point.");
        this.hideReadItems = dbQuery.shouldHideReadItems();
        this.filterLabel = dbQuery.getFilterLabel();
        this.filterFeed = dbQuery.getFilterFeedId();
        this.entryManager = dbQuery.entryManager;
        this.startDate = dbQuery.getStartDate();
        this.sortAscending = dbQuery.isSortOrderAscending();
        this.dateLimit = dbQuery.dateLimit;
        this.limit = dbQuery.limit;
        changed = true;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isSortOrderAscending() {
        return sortAscending;
    }

    public void setSortOrderAscending(boolean newValue) {
        this.sortAscending = newValue;
        changed = true;
        internalRepresentation = null;
    }

    public boolean shouldHideReadItems() {
        return hideReadItems;
    }

    public String getFilterLabel() {
        return filterLabel;
    }

    public Long getFilterFeedId() {
        return filterFeed;
    }

    public boolean hasChanged() {
        if (changed) {
            changed = false;
            return true;
        }
        return false;
    }

    public void toggleHideItems() {
        internalRepresentation = null;
        setShouldHideReadItems(!shouldHideReadItems());
    }

    public void toggleSortOrder() {
        internalRepresentation = null;
        setSortOrderAscending(!isSortOrderAscending());
    }

    public void setShouldHideReadItems(boolean shouldHideReadItems) {
        internalRepresentation = null;
        if (this.hideReadItems != shouldHideReadItems) {
            this.hideReadItems = shouldHideReadItems;
            SDK9Helper.apply(entryManager.getSharedPreferences().edit().putBoolean(
                    EntryManager.SETTINGS_HIDE_READ_ITEMS, shouldHideReadItems));
            changed = true;
        }
    }

    public void setShouldHideReadItemsWithoutUpdatingThePreference(boolean shouldHideReadItems) {
        internalRepresentation = null;
        if (this.hideReadItems != shouldHideReadItems) {
            this.hideReadItems = shouldHideReadItems;
            changed = true;
        }

    }

    public void updateShouldHideReadItems() {
        internalRepresentation = null;
        setShouldHideReadItems(entryManager.getSharedPreferences().getBoolean(EntryManager.SETTINGS_HIDE_READ_ITEMS,
                shouldHideReadItems()));
    }

    public void setFilterFeedId(Long feedId) {
        internalRepresentation = null;
        this.filterFeed = feedId;
        changed = true;
    }

    public void setFilterLabel(String labelName) {
        internalRepresentation = null;
        this.filterLabel = labelName;
        changed = true;
    }

    public void setStartDate(long startDate) {
        internalRepresentation = null;
        this.startDate = startDate;
    }

    // Date.getTime() * 1000
    public void setDateLimit(long dateLimit) {
        this.dateLimit = dateLimit;
        internalRepresentation = null;
    }

    public long getDateLimit() {
        return dateLimit;
    }

    public long getStartDate() {
        return startDate;
    }

    @Override
    public String toString() {

        return "\n\t\tDBQ\n\t\tfilterLabel=" + filterLabel + "\n\t\tfilterFeed=" + filterFeed + "\n\t\thideReadItems="
                + hideReadItems + "\n\t\tstartDate=" + startDate + "\n\t\tsortAscending=" + sortAscending
                + "\n\t\tdateLimit=" + dateLimit + "\n\t\tlimit=" + limit;
    }

    public void store(OutputStream os) {
        /*
         * OutputStream output = new OutputStream() { private StringBuilder
         * buffer = new StringBuilder();
         * 
         * @Override public void write(int b) throws IOException {
         * buffer.append((char) b); }
         * 
         * @Override public String toString() { return buffer.toString(); } };
         */
        try {
            Properties p = new Properties();

            if (getFilterFeedId() != null)
                p.setProperty("filterFeedId", String.valueOf(getFilterFeedId()));
            if (getFilterLabel() != null)
                p.setProperty("filterLabel", getFilterLabel());
            // p.setProperty("sortAscending",
            // String.valueOf(isSortOrderAscending()));
            p.setProperty("hideReadArticles", String.valueOf(shouldHideReadItems()));
            p.storeToXML(os, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DBQuery restore(EntryManager entryManager, InputStream is) throws IOException {
        DBQuery dbq = null;
        try {
            Properties p = new Properties();
            p.loadFromXML(is);
            Long tmpFeedId = p.containsKey("filterFeedId") ? Long.parseLong(p.getProperty("filterFeedId")) : null;
            String tmpLabel = p.containsKey("filterLabel") ? p.getProperty("filterLabel") : null;
            Boolean tmpHideReadArticles = p.containsKey("hideReadArticles") ? Boolean.parseBoolean(p
                    .getProperty("hideReadArticles")) : false;
            dbq = new DBQuery(entryManager, tmpLabel, tmpFeedId);
            dbq.setShouldHideReadItemsWithoutUpdatingThePreference(tmpHideReadArticles);
            // dbq.setSortOrderAscending(p.containsKey("sortAscending") ?
            // Boolean.parseBoolean(p
            // .getProperty("sortAscending")) : false);
        } catch (RuntimeException rte) {
            rte.printStackTrace();
        }
        return dbq;
    }

}
