package com.nosideracing.rchipremote;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	private CharSequence[] hostsListEntryValues = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.d(Consts.LOG_TAG, "OnCreate: Preferences");
		addPreferencesFromResource(R.xml.preferences);
		ListPreference hostnameList = (ListPreference) findPreference("serverhostname");
		try {
			hostsListEntryValues = RemoteMain.json.getHostNames().split("\\|");
			hostnameList.setEntries(hostsListEntryValues);
			hostnameList.setEntryValues(hostsListEntryValues);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Problem getting HostName", e);
		}

	}

}
