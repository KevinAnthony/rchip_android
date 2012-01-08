package com.nosideracing.rchipremote;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Preferences extends PreferenceActivity {
	private CharSequence[] hostsListEntryValues = null;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		JSON jSon = new JSON(getApplicationContext());
		Log.d(Consts.LOG_TAG, "OnCreate: Preferences");
		addPreferencesFromResource(R.xml.preferences);
		ListPreference hostnameList = (ListPreference) findPreference("serverhostname");
		try {
			hostsListEntryValues = jSon.getHostNames().split("\\|");
			hostnameList.setEntries(hostsListEntryValues);
			hostnameList.setEntryValues(hostsListEntryValues);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Problem getting HostName", e);
		}

	}

}
