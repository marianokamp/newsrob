package com.newsrob;

import com.newsrob.jobs.ModelUpdateResult;

public interface IEntryModelUpdateListener {

	void modelUpdateStarted(boolean fastSyncOnly);

	void modelUpdated();
	
	void modelUpdated(String atomId);

	void modelUpdateFinished(ModelUpdateResult result);

	void statusUpdated();

}
