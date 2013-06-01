package com.newsrob.jobs;

import com.newsrob.EntryManager;

public abstract class Job {
    private static final int[] EMPTY_PROGRESS_ARRAY = new int[2];
    private boolean cancelled = false;
    private String jobDescription;
    private EntryManager entryManager;

    public Job(String description, EntryManager entryManager) {
        this.entryManager = entryManager;
        this.jobDescription = description;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isProgressMeassurable() {
        return false;
    };

    public int[] getProgress() {
        return EMPTY_PROGRESS_ARRAY;
    };

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String description) {
        this.jobDescription = description;
        entryManager.fireStatusUpdated();
    }

    public abstract void run() throws Throwable;

    public void cancel() {
        cancelled = true;
    }
}
