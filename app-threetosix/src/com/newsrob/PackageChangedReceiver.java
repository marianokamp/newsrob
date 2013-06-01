package com.newsrob;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.newsrob.threetosix.R;

public class PackageChangedReceiver extends BroadcastReceiver {

	private static final String TAG = PackageChangedReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (NewsRob.isDebuggingEnabled(context))
			PL.log("onReceive called with " + intent.getAction() + ". NewsRob was upgraded.", context);

		Bundle extras = intent.getExtras();
		if (extras == null)
			return;

		String affectedPackageName = context.getPackageManager().getNameForUid(extras.getInt(Intent.EXTRA_UID));

		String myName = context.getPackageName();
		if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
			if (EntryManager.PRO_PACKAGE_NAME.equals(affectedPackageName)) {
				EntryManager.getInstance(context).maintainPremiumDependencies();
				PL.log("Enabling Locale integration.", context);
			}
			return;
		}

		// ACTION = REPLACED

		if (!myName.equals(affectedPackageName))
			return;

		if (NewsRob.isDebuggingEnabled(context))
			PL.log("Re-establishing alarms.", context);
		EntryManager.getInstance(context).getScheduler().ensureSchedulingIsEnabled();

	}
}
