package com.newsrob.storage;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class PhoneMemoryStorageAdapter extends AbstractStorageAdapter {

	public PhoneMemoryStorageAdapter(Context ctx) {
		super(ctx);
	}

	public String getAbsolutePathForAsset(String fileName) {
		return getBaseDir().getAbsolutePath() + "/" + fileName;
	}

	public float megaBytesFree() {
		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
		return statFs.getAvailableBlocks() / 1024.0f / 1024.0f * statFs.getBlockSize();
	}

	public boolean canWrite() {
		return true;
	}

	@Override
	File getBaseDir() {
		File f = getContext().getFilesDir();
		if (!f.exists())
			f.mkdirs();
		return f;
	}

	@Override
	public int megaBytesThreshold() {
		return 9;
	}

}
