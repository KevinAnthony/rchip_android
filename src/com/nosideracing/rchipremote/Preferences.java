/*
 * rchip remote - android application for RCHIP interface
 * Copyright (C) 2012  Kevin Anthony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nosideracing.rchipremote;

import com.nosideracing.rchipremote.Consts;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class Preferences extends Activity {
	private static CharSequence[] hostsListEntryValues = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Log.e(Consts.LOG_TAG, "Got Here\n");
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);
			ListPreference hostnameList = (ListPreference) findPreference("serverhostname");
			try {
				hostsListEntryValues = JSON.getInstance().getHostNames()
						.split("\\|");
				hostnameList.setEntries(hostsListEntryValues);
				hostnameList.setEntryValues(hostsListEntryValues);
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "Problem getting HostName", e);
			}
		}

		public void onSaveInstanceState(Bundle savedInstanceState) {
			Log.e(Consts.LOG_TAG, "SavedInstance");
		}
	}
}
