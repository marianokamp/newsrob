package com.newsrob;

import com.newsrob.threetosix.R;

public class DiscoveredFeed {

    public String title;
    public String feedUrl;
    public String alternateUrl;
    public String snippet;

    public String toString() {
        return "DiscoveredFeed title=" + title + " feedUrl=" + feedUrl + " alternateUrl=" + alternateUrl + " snippet="
                + snippet;
    }

}
