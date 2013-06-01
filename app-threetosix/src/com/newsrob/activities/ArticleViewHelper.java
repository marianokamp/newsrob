package com.newsrob.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.newsrob.Entry;
import com.newsrob.EntryManager;
import com.newsrob.NewsRob;
import com.newsrob.PL;
import com.newsrob.threetosix.R;
import com.newsrob.ReadState;
import com.newsrob.util.U;
import com.newsrob.widget.SwipeRelativeLayout;

class ArticleViewHelper {

    static final int MENU_ITEM_SHOW_IN_BROWSER_ID = 100;
    static final int MENU_ITEM_MARK_ENTRY_READ_ID = 101;
    static final int MENU_ITEM_MARK_ENTRY_UNREAD_ID = 102;
    static final int MENU_ITEM_SHARE_LINK_ID = 103;
    static final int MENU_ITEM_SHARE_IN_READER_ID = 104;
    static final int MENU_ITEM_UNSHARE_IN_READER_ID = 105;
    static final int MENU_ITEM_STAR_ID = 106;
    static final int MENU_ITEM_UNSTAR_ID = 107;
    static final int MENU_ITEM_SHOW_ARTICLE_INFO_ID = 108;
    static final int MENU_ITEM_BOOM_ID = 109;
    static final int MENU_ITEM_MANAGE_FEED_ID = 110;
    static final int MENU_ITEM_SHARE_WITH_NOTE_ID = 111;
    static final int MENU_ITEM_REFRESH_CONTENT_ID = 113;

    static final int MENU_ITEM_BUY_NEWSROB_PRO = 199;

    private static final String TAG = ArticleViewHelper.class.getName();

    static final int SOURCE_SHOW_ENTRY = 0;
    static final int SOURCE_ENTRY_LIST = 1;

    static void populateEntryView(final View view, final Entry entry, final EntryManager entryManager,
            final UIHelper uiHelper) {
        final Resources resources = entryManager.getContext().getResources();

        // swipe
        final SwipeRelativeLayout swipeContainer = (SwipeRelativeLayout) view.findViewById(R.id.swipe_container);

        final View outterContainer = view.findViewById(R.id.outter_container);
        outterContainer.setPadding(0, 0, 0, 0);

        swipeContainer.setSwipeListener(new SwipeRelativeLayout.ISwipeListener() {

            private final int selectionFeedbackColor = resources.getColor(R.color.selection_feedback);

            public boolean swipeTopToBottom(final View v) {
                return false;
            }

            public boolean swipeRightToLeft(final View v) {

                view.setBackgroundColor(selectionFeedbackColor);
                entryManager.increaseUnreadLevel(entry);
                return true;
            }

            public boolean swipeLeftToRight(final View v) {

                view.setBackgroundColor(selectionFeedbackColor);
                entryManager.increaseReadLevel(entry);
                return true;
            }

            public boolean swipeBottomToTop(final View v) {
                return false;
            }

            public boolean onLongClick(final View v, final MotionEvent e) {
                return view.performLongClick();
            }

            public boolean onClick(final View v, final MotionEvent e) {
                return view.performClick();
            }

        });

        swipeContainer.setSwipeEnabeld(entryManager.isSwipeOnArticleDetailViewEnabled());
        swipeContainer.setBackgroundResource(R.drawable.article_header_background_thin);

        final TextView entryTitleView = (TextView) view.findViewById(R.id.entry_title);
        final TextView feedTitleView = (TextView) view.findViewById(R.id.feed_title);

        if (feedTitleView == null || entryTitleView == null) {
            Log.e(TAG, "feedTitleView or entryTitleView were null.");
            return;
        }

        if (entry == null) {
            Log.d(TAG, "entry was null");
            return;
        }

        if (entry.getFeedTitle() == null) {
            Log.e(TAG, "entry.getFeedTitle() was null.");
            return;
        }

        feedTitleView.setText(entry.getFeedTitle());
        entryTitleView.setText(entry.getTitle());

        if (entryManager.shouldTitlesBeEllipsized()) {
            entryTitleView.setEllipsize(TruncateAt.MIDDLE);
            entryTitleView.setLines(2);
        }

        // final int backgroundColor = resources.getColor(entry.isRead() ?
        // R.color.article_read_background
        // : R.color.article_unread_background);
        // final int textColor = resources.getColor(entry.isRead() ?
        // R.color.article_read_text
        // : R.color.article_unread_text);

        final TextView readIndicator = (TextView) view.findViewById(R.id.read_status_indicator);
        final int readIndicatorBackground = entryManager.isLightColorSchemeSelected() ? R.drawable.read_indicator
                : R.drawable.read_indicator_dark;
        final int pinnedIndicatorBackground = entryManager.isLightColorSchemeSelected() ? R.drawable.pinned_indicator
                : R.drawable.pinned_indicator_dark;

        int bgReadIndicator = -1;

        switch (entry.getReadState()) {
        case READ:
            bgReadIndicator = R.drawable.read_indicator_invisible;
            break;
        case UNREAD:
            bgReadIndicator = readIndicatorBackground;
            break;
        default:
            bgReadIndicator = pinnedIndicatorBackground;
        }

        readIndicator.setBackgroundResource(bgReadIndicator);

        // view.setBackgroundColor(backgroundColor);
        View container = view.findViewById(R.id.outter_container);
        if (entryManager.isLightColorSchemeSelected())
            container.setBackgroundColor(resources.getColor(R.color.article_read_background));
        else
            container.setBackgroundDrawable(resources.getDrawable(R.drawable.article_header_background_dark));

        // entryTitleView.setTextColor(textColor);
        // feedTitleView.setTextColor(textColor);

        //
        feedTitleView.setCompoundDrawablePadding(3);
        feedTitleView.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(uiHelper
                .getArticleDownloadIndicatorDrawable(entry.getDownloaded(), entry.getDownloadPref(), resources)), null,
                null, null);

        // star check box
        final CheckBox starCheckBox = (CheckBox) view.findViewById(R.id.star_checkbox);
        starCheckBox.setVisibility(View.VISIBLE);
        starCheckBox.setChecked(entry.isStarred());
        starCheckBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                entryManager.updateStarredState(entry, starCheckBox.isChecked());
            }
        });
        starCheckBox.requestFocus();

        // shared
        final TextView sharedView = (TextView) view.findViewById(R.id.article_shared);
        sharedView.setVisibility(entry.isShared() ? View.VISIBLE : View.GONE);

        // liked
        final TextView likedView = (TextView) view.findViewById(R.id.article_liked);
        // likedView.setVisibility(entry.isLiked() ? View.VISIBLE : View.GONE);

        // annotated
        final TextView annotatedView = (TextView) view.findViewById(R.id.article_annotated);
        annotatedView.setVisibility(entry.getNote() != null ? View.VISIBLE : View.GONE);

        if (false) {
            // changed
            final TextView changedView = (TextView) view.findViewById(R.id.article_changed);

            final boolean stateChanged = entry.isReadStatePending() || entry.isStarredStatePending()
                    || entry.isSharedStatePending() || entry.isLikedStatePending()
                    || (entry.getNote() != null && !entry.isNoteSubmitted());
            changedView.setText(stateChanged ? "*" : "");
        }

        final TextView sharedByFriend = (TextView) view.findViewById(R.id.shared_by_friend);
        if (entry.isFriendsShared() && entry.getSharedByFriend() != null) {
            sharedByFriend.setVisibility(View.VISIBLE);
            sharedByFriend.setText("Shared by " + entry.getSharedByFriend());
        } else
            sharedByFriend.setVisibility(View.GONE);

    }

    static void createArticleMenu(final Menu menu, final Activity owningActivity, final Entry selectedEntry) {
        final EntryManager entryManager = EntryManager.getInstance(owningActivity);

        final boolean isArticleContextMenu = menu instanceof ContextMenu;
        if (isArticleContextMenu)
            ((ContextMenu) menu).setHeaderTitle("Article");

        final boolean alternateHRefAvailable = (selectedEntry != null && selectedEntry.getAlternateHRef() != null);

        if (selectedEntry == null) {
            Log.e(TAG, "Oops. SelectedEntry was null.");
            return;
        }

        if (selectedEntry.getReadState() == ReadState.READ)
            menu.add(0, MENU_ITEM_MARK_ENTRY_UNREAD_ID, 1, R.string.menu_mark_as_unread).setIcon(
                    android.R.drawable.checkbox_on_background);
        else
            menu.add(0, MENU_ITEM_MARK_ENTRY_READ_ID, 1, R.string.menu_mark_as_read).setIcon(
                    android.R.drawable.checkbox_on_background);

        if (isArticleContextMenu) {
            if (selectedEntry.isStarred())
                menu.add(0, MENU_ITEM_UNSTAR_ID, 2, R.string.menu_item_unstar).setIcon(
                        android.R.drawable.btn_star_big_off);
            else
                menu.add(0, MENU_ITEM_STAR_ID, 2, R.string.menu_item_star).setIcon(android.R.drawable.btn_star_big_off);
        }

        Uri uri = null;
        if (alternateHRefAvailable)
            uri = Uri.parse(selectedEntry.getAlternateHRef());

        menu.add(0, MENU_ITEM_SHOW_IN_BROWSER_ID, 4, R.string.menu_show_in_browser).setTitleCondensed(
                U.t(owningActivity, R.string.menu_show_in_browser_condensed)).setIntent(
                new Intent(Intent.ACTION_VIEW, uri)).setIcon(android.R.drawable.ic_menu_view).setEnabled(
                alternateHRefAvailable);

        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        // sendIntent.setType("message/rfc822");
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, selectedEntry.getTitle());
        sendIntent.putExtra(Intent.EXTRA_TEXT, String.valueOf(uri));

        menu.add(0, MENU_ITEM_SHARE_LINK_ID, 5, R.string.menu_item_share_link).setIntent(
                Intent.createChooser(sendIntent, "Share Link")).setIcon(android.R.drawable.ic_menu_share).setEnabled(
                alternateHRefAvailable);
        if (selectedEntry.isShared())
            menu.add(0, MENU_ITEM_UNSHARE_IN_READER_ID, 6, R.string.menu_item_unshare_in_reader).setIcon(
                    android.R.drawable.ic_menu_share).setEnabled(alternateHRefAvailable);
        else
            menu.add(0, MENU_ITEM_SHARE_IN_READER_ID, 6, R.string.menu_item_share_in_reader).setIcon(
                    android.R.drawable.ic_menu_share).setEnabled(alternateHRefAvailable);

        final boolean isProVersion = entryManager.isProVersion();

        menu.add(0, MENU_ITEM_SHARE_WITH_NOTE_ID, isProVersion ? 7 : 70, R.string.menu_item_share_with_note).setIcon(
                owningActivity.getResources().getDrawable(android.R.drawable.ic_menu_share).mutate()).setEnabled(
                isProVersion && selectedEntry.getNote() == null && !selectedEntry.isNote());

        menu.add(0, MENU_ITEM_REFRESH_CONTENT_ID, 8, "Refresh Content").setIcon(
                android.R.drawable.ic_menu_close_clear_cancel).setEnabled(
                selectedEntry.getDownloaded() != Entry.STATE_NOT_DOWNLOADED
                        && entryManager.getStorageAdapter().canWrite());

        menu.add(0, MENU_ITEM_MANAGE_FEED_ID, 11, R.string.menu_item_manage_feed).setIcon(
                android.R.drawable.ic_menu_manage).setEnabled(selectedEntry.getFeedId() != 0l);

        menu.add(0, MENU_ITEM_SHOW_ARTICLE_INFO_ID, 12, R.string.menu_item_show_article_info);

        if ("1".equals(NewsRob.getDebugProperties(owningActivity).getProperty("enableBoom", "0")))
            menu.add(0, MENU_ITEM_BOOM_ID, 20, "Boom!");

        if (!isProVersion && entryManager.isAndroidMarketInstalled()) {
            final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(Uri.parse("market://details?id=" + EntryManager.PRO_PACKAGE_NAME));
            menu.add(0, MENU_ITEM_BUY_NEWSROB_PRO, 30, "Buy NewsRob Pro!").setIntent(viewIntent);
        }

    }

    static boolean articleActionSelected(final Activity owningActivity, final MenuItem item,
            final EntryManager entryManager, final Entry selectedEntry) {

        if (selectedEntry == null)
            return false;

        switch (item.getItemId()) {
        case ArticleViewHelper.MENU_ITEM_MARK_ENTRY_READ_ID:
            new Thread(new Runnable() {
                public void run() {
                    PL.log("AVH:Submitting READ", owningActivity);
                    entryManager.updateReadState(selectedEntry, ReadState.READ);
                }
            }).start();

            return true;

        case ArticleViewHelper.MENU_ITEM_MARK_ENTRY_UNREAD_ID:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PL.log("AVH:Submitting UNREAD", owningActivity);
                    entryManager.updateReadState(selectedEntry, ReadState.UNREAD);
                }
            }).start();
            return true;

        case ArticleViewHelper.MENU_ITEM_SHARE_IN_READER_ID:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PL.log("AVH:Submitting SHARE", owningActivity);
                    entryManager.updateSharedState(selectedEntry, true);
                }
            }).start();
            return true;

        case ArticleViewHelper.MENU_ITEM_UNSHARE_IN_READER_ID:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PL.log("AVH:Submitting UNSHARE", owningActivity);
                    entryManager.updateSharedState(selectedEntry, false);
                }
            }).start();
            return true;

        case ArticleViewHelper.MENU_ITEM_STAR_ID:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PL.log("AVH:Submitting STAR", owningActivity);
                    entryManager.updateStarredState(selectedEntry, true);
                }
            }).start();
            return true;

        case ArticleViewHelper.MENU_ITEM_UNSTAR_ID:
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PL.log("AVH:Submitting UNSTAR", owningActivity);
                    entryManager.updateStarredState(selectedEntry, false);
                }
            }).start();
            return true;

        case ArticleViewHelper.MENU_ITEM_SHOW_ARTICLE_INFO_ID:
            owningActivity.startActivity(new Intent(entryManager.getContext(), ShowArticleInfoActivity.class).putExtra(
                    ShowArticleInfoActivity.EXTRA_ATOM_ID, selectedEntry.getAtomId()));
            return true;
        case ArticleViewHelper.MENU_ITEM_MANAGE_FEED_ID:
            owningActivity.startActivity(new Intent(entryManager.getContext(), ManageFeedActivity.class).putExtra(
                    ManageFeedActivity.EXTRA_FEED_ID, selectedEntry.getFeedId()));
            return true;
        case ArticleViewHelper.MENU_ITEM_REFRESH_CONTENT_ID:
            Toast.makeText(owningActivity,
                    "The article's content is being removed. NewsRob will try to re-download it during the next sync.",
                    Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (selectedEntry.isRead())
                        entryManager.updateReadState(selectedEntry, ReadState.UNREAD);
                    entryManager.removeArticleContent(selectedEntry);
                }
            }).start();
            return true;

        case MENU_ITEM_BOOM_ID:
            // throw new OutOfMemoryError("Just testing!");
            throw new RuntimeException("Just testing!");
        case MENU_ITEM_SHARE_WITH_NOTE_ID:
            showShareWithNoteDialog(owningActivity, entryManager, selectedEntry);
            return true;
        }
        return false;
    }

    private static void showShareWithNoteDialog(final Context context, final EntryManager entryManager,
            final Entry selectedEntry) {

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(5, 5, 5, 5);

        final TextView noteLabel = new TextView(context);
        noteLabel.setText("Note");
        noteLabel.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        linearLayout.addView(noteLabel);

        final EditText noteEntry = new EditText(context);
        noteEntry.setLines(2);
        noteEntry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        linearLayout.addView(noteEntry);

        final LinearLayout shareNoteContainer = new LinearLayout(context);

        final CheckBox shareNoteCheckBox = new CheckBox(context);
        shareNoteCheckBox.setChecked(true);
        shareNoteContainer.addView(shareNoteCheckBox);

        final TextView shareLabel = new TextView(context);
        shareLabel.setText("Also share the new note");
        shareLabel.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        shareNoteContainer.addView(shareLabel);

        linearLayout.addView(shareNoteContainer);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share With Note").setView(linearLayout).setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(final DialogInterface dialog, final int which) {
                selectedEntry.setNote(noteEntry.getText().toString().trim());
                selectedEntry.setShouldNoteBeShared(shareNoteCheckBox.isChecked());
                entryManager.update(selectedEntry);
                entryManager.fireModelUpdated();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show(); // .getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
    }
}