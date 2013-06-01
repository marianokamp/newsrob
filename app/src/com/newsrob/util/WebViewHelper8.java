package com.newsrob.util;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.PluginState;

import com.newsrob.EntryManager;
import com.newsrob.PL;

public class WebViewHelper8 {
    public static void setupWebView(EntryManager entryManager, WebView webView) {
        WebSettings settings = webView.getSettings();
        String state = entryManager.getPlugins();
        settings.setPluginState(PluginState.valueOf(state));
        PL.log("SetupWebView. Plugin State=" + settings.getPluginState(), webView.getContext());

    }
}
