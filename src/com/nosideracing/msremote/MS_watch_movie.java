package com.nosideracing.msremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class MS_watch_movie extends Activity {

	private TextView sName;
	private TextView eNum;
	private TextView eName;
	private TextView loc;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.watch);
		Log.d(MS_constants.LOG_TAG, "onCreate: MS_Watch_Movie");
		Bundle incoming = getIntent().getExtras();
		String ShowName = incoming.getString("ShowName");
		String EpsNumber = incoming.getString("EpsNumber");
		String EpsName = incoming.getString("EpsName");
		String Location = incoming.getString("Location");
		sName = (TextView) findViewById(R.id.sName);
		sName.setText(ShowName);
		eNum = (TextView) findViewById(R.id.eNum);
		eNum.setText(EpsNumber);
		eName = (TextView) findViewById(R.id.eName);
		eName.setText(EpsName);
		loc = (TextView) findViewById(R.id.loc);
		loc.setText(Location);
	}

	public void onPause() {
		super.onPause();
		Log.d(MS_constants.LOG_TAG, "onPause: MS_Watch_Movie");
	}

	public void onResume() {
		super.onResume();
		Log.d(MS_constants.LOG_TAG, "onResume: MS_Watch_Movie");
		/* if update is false (we are not updating) update and set to true) */

	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(MS_constants.LOG_TAG, "onStop: MS_Watch_Movie");
		this.finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(MS_constants.LOG_TAG, "onDestroy msmusic");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(MS_constants.LOG_TAG, "onOptionsItemSelected: msremote");
		int calledMenuItem = item.getItemId();
		if (calledMenuItem == R.id.settings) {
			startActivity(new Intent(this, MS_preferences.class));
			return true;
		} else if (calledMenuItem == R.id.quit) {
			quit();
			return true;
		} else if (calledMenuItem == R.id.wifiset) {
			SharedPreferences.Editor editor = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext())
					.edit();
			editor.putString("internalnetname",
					((WifiManager) getSystemService(Context.WIFI_SERVICE))
							.getConnectionInfo().getSSID());
			editor.commit();
			return true;
		}
		return false;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
	    // See which child activity is calling us back.
	    if (resultCode == MS_constants.QUITREMOTE){
	                quit();
	            } 
		}

	private void quit() {
		Log.i(MS_constants.LOG_TAG, "Quitting");
		setResult(MS_constants.QUITREMOTE);
		this.finish();
	}
}
