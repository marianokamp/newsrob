/**
 * 
 */
package com.newsrob.appwidget;

abstract class AbstractWizardPage {
    private boolean enabled = true;

    public AbstractWizardPage() {
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean newState) {
        this.enabled = newState;
    }

    void onEnter() {
    }

    abstract void updateState();

}