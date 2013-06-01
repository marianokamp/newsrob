package com.newsrob.util;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;

public class MessageHelper {

    public static void showMessage(Activity owningActivity, int titleRessourceId, int messageRessourceId,
            String messageId) {

        File f = new File(owningActivity.getFilesDir(), "seen_" + messageId);
        try {
            // when the file can be created the message was not seen before and
            // should be shown
            if (f.createNewFile())
                new AlertDialog.Builder(owningActivity).setIcon(android.R.drawable.ic_dialog_info).setMessage(
                        messageRessourceId).setTitle(titleRessourceId).setPositiveButton(android.R.string.ok, null)
                        .create().show();
        } catch (IOException e) {
        }
    }
}
