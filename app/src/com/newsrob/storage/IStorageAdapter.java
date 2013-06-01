package com.newsrob.storage;

import java.io.FileNotFoundException;
import java.io.OutputStream;

public interface IStorageAdapter {
	OutputStream openFileOutput(String fileName) throws FileNotFoundException;

	String getAbsolutePathForAsset(String fileName);

	float megaBytesFree();

	boolean canWrite();

	void clear();

	int removeAllAssets(String atomId);

	int megaBytesThreshold();

}