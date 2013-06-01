package com.newsrob.jobs;

import com.newsrob.Entry;

public class EntryUpdateSucceeded extends ModelUpdateResult {
	private Entry entry;

	public EntryUpdateSucceeded(Entry entry) {
		this.entry = entry;
	}

	Entry getEntry() {
		return entry;
	}

	@Override
	public String getMessage() {
		return "Entry update ok.";
	}
}