package com.newsrob.appwidget;

import com.newsrob.DBQuery;

public class WidgetPreferences {

	private String label;
	private DBQuery dbq;
	private String startingActivityName;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public DBQuery getDBQuery() {
		return dbq;

	}

	public void setDBQuery(DBQuery dbq) {
		this.dbq = dbq;
	}

	public void setStartingActivityName(String startingActivityName) {
		this.startingActivityName = startingActivityName;

	}

	public String getStartingActivityName() {
		return startingActivityName;
	}
}
