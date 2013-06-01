package com.newsrob.jobs;

public class SynchronizeModelSucceeded extends ModelUpdateResult {
	int noOfEntriesUpdated = -1;

	public SynchronizeModelSucceeded(int noOfNewEntriesUpdated) {
		this.noOfEntriesUpdated = noOfNewEntriesUpdated;
	}

	public int getNoOfEntriesUpdated() {
		return noOfEntriesUpdated;
	}

	@Override
	public
	String getMessage() {
		return String.format("%s entries updated.", noOfEntriesUpdated);
	}
}
