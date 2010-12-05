package com.nosideracing.msremote;

import com.nosideracing.msremote.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class msprefs extends PreferenceActivity{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
