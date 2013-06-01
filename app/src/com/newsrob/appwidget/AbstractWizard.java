package com.newsrob.appwidget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.newsrob.EntryManager;
import com.newsrob.R;

/**
 * The Wizard is responsible for the UI It should be possible to use it outside
 * of the context of a wizard.
 */

abstract class AbstractWizard implements View.OnClickListener {

    private List<AbstractWizardPage> wizardPages;
    private AbstractWizardPage currentPage;
    private Button left;
    private Button right;
    private ViewFlipper contentArea;

    private EntryManager entryManager;

    AbstractWizard(Activity owningActivity) {

        entryManager = EntryManager.getInstance(owningActivity);

        contentArea = (ViewFlipper) owningActivity.findViewById(R.id.content_area);

        left = (Button) owningActivity.findViewById(R.id.left);
        right = (Button) owningActivity.findViewById(R.id.right);

        left.setOnClickListener(this);
        right.setOnClickListener(this);

        wizardPages = new ArrayList<AbstractWizardPage>(5);
    }

    void addWizardPage(AbstractWizardPage page) {
        wizardPages.add(page);

        // Make sure initialization runs
        setCurrentPage(getCurrentPage());

    }

    EntryManager getEntryManager() {
        return entryManager;
    }

    private void setCurrentPage(AbstractWizardPage wizardPage) {

        left.setText("Previous");
        if (isFirstPage(wizardPage))
            left.setVisibility(View.INVISIBLE);
        else
            left.setVisibility(View.VISIBLE);

        if (isLastPage(wizardPage))
            right.setText("Create");
        else
            right.setText("Next");
        right.setVisibility(View.VISIBLE);

        // now change them as appropriate for the current status

        updateChildrenState();

        currentPage = wizardPage;
        contentArea.setDisplayedChild(wizardPages.indexOf(currentPage));

        currentPage.updateState();
        currentPage.onEnter();
    }

    private boolean isLastPage(AbstractWizardPage wizardPage) {
        return wizardPages.lastIndexOf(wizardPage) == wizardPages.size() - 1;
    }

    private boolean isFirstPage(AbstractWizardPage wizardPage) {
        return wizardPages.lastIndexOf(wizardPage) == 0;
    }

    void updateChildrenState() {
        for (AbstractWizardPage p : wizardPages)
            p.updateState();
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        String label = b.getText().toString();

        updateChildrenState();

        if ("Previous".equals(label)) {
            previous();
        } else if ("Next".equals(label)) {
            next();
        } else if ("Create".equals(label)) {
            wizardFinished();
        }
    }

    private void previous() {
        int index = wizardPages.indexOf(getCurrentPage()) - 1;
        while (index >= 0 && !wizardPages.get(index).isEnabled())
            index--;
        setCurrentPage(wizardPages.get(index));
    }

    private void next() {
        int index = wizardPages.indexOf(getCurrentPage()) + 1;
        while (index < wizardPages.size() && !wizardPages.get(index).isEnabled())
            index++;
        setCurrentPage(wizardPages.get(index));
    }

    abstract void wizardFinished();

    private AbstractWizardPage getCurrentPage() {
        if (currentPage == null)
            setCurrentPage(wizardPages.get(0));
        return currentPage;
    }

}