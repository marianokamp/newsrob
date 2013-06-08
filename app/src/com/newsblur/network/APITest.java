package com.newsblur.network;

import android.content.Context;

import com.newsblur.network.domain.FeedFolderResponse;
import com.newsblur.network.domain.LoginResponse;
import com.newsrob.NewsRob;

public class APITest {

    public static void startTest(final Context context) {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                String userId = NewsRob.getDebugProperties(context).getProperty("syncUserId", null);
                String password = NewsRob.getDebugProperties(context).getProperty("syncPassword", null);

                APIManager manager = new APIManager(context);
                LoginResponse login = manager.login(userId, password);
                FeedFolderResponse list = manager.getFolderFeedMapping(true);

                System.out.println();
            }
        });
        t.start();
    }

}
