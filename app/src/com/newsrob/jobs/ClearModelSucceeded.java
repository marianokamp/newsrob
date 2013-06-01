package com.newsrob.jobs;

public class ClearModelSucceeded extends ModelUpdateResult {
	public int noOfEntriesDeleted = -1;

	public ClearModelSucceeded(int noOfEntriesDeleted) {
		this.noOfEntriesDeleted = noOfEntriesDeleted;
	}

	int getNoOfEntriesDeleted() {
		return noOfEntriesDeleted;
	}

	@Override
	public
	String getMessage() {
		return String.format("%s entries deleted.", noOfEntriesDeleted); // LATER
		// Pluralize?
	}
}