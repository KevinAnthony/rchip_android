package com.nosideracing.rchipremote;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/*
 * NOTE: Starts MSREMOTE when icon clicked, exit doesn't close
 * duplicate-> start msremote, Music home, msremote,Music(two instances of window now)
 *
 */
public class RemoteMain extends Activity {
	PendingIntent CheckMessagesPendingIntent;
	AlarmManager alarm;
	/* This is the Pref window from MENU->Settings */
	SharedPreferences settings;
	static Context f_context;
	/* For the services the service then the actual connection */
	public static String msb_desthost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Log.w(Consts.LOG_TAG, "Opps it's null!!");
		}
		setContentView(R.layout.main);
		f_context = getApplicationContext();

		/*
		 * We put the LOG_TAG in the string.xml file, so we can change it one
		 * place not everyplace in our code
		 */
		Log.d(Consts.LOG_TAG, "onCreate: msremote");
		/*
		 * We pull the settings from the prefmanager, then we pull the telephone
		 * number to use as a HOST_ID the reason i used the telephone number was
		 * so that if we ever use registration it doesn't change if you get a
		 * new phone
		 */
		settings = PreferenceManager.getDefaultSharedPreferences(f_context);
		settings
				.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(
							SharedPreferences prefs, String key) {
						Log.w(Consts.LOG_TAG, "SharePrefsChanged");
					}
				});
		TelephonyManager tManager = (TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = tManager.getLine1Number();
		/* If there is no phone number, we use (111) 111-1111 */
		if (phoneNumber == null) {
			phoneNumber = "1111111111";
		}
		msb_desthost = settings.getString("serverhostname","Tomoya");
		/* creates the music button */
		final Button button_music = (Button) findViewById(R.id.music);
		button_music.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				Log.d(Consts.LOG_TAG, "Music Pushed");
				activity_music();
			}
		});
		/* creates the Torrent Button */
		try {
			Button button_tor = (Button) findViewById(R.id.torrent);
			button_tor.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					/* Perform action on clicks */
					activity_torrent();
				}
			});
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error:" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
		}

		SharedPreferences.Editor editor = settings.edit();

		/* Pulls the URL, and Destination Host Name from the settings */
		if (settings.getBoolean("firstRun", true)) {
			startActivity(new Intent(this, Preferences.class));

			editor.putBoolean("firstRun", false);
			editor.commit();
		}
		Intent intent = new Intent(f_context, CheckMessages.class);
		// In reality, you would want to have a static variable for the request
		// code instead of 192837
		CheckMessagesPendingIntent = PendingIntent.getBroadcast(this, 192837, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),Consts.DELAY_LENGTH_ACTIVE, CheckMessagesPendingIntent);
	}

	public void onPause() {
		super.onPause();
		Log.d(Consts.LOG_TAG, "onPause: msremote");
	}

	public void onResume() {
		super.onResume();
		/*
		 * if we get to this spot, we clear the notifications, we should also be
		 * doing this in any other activity that potentialy could be called.
		 */
		Log.d(Consts.LOG_TAG, "onResume: msremote");
		alarm.cancel(CheckMessagesPendingIntent);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),Consts.DELAY_LENGTH_ACTIVE, CheckMessagesPendingIntent);
	}

	public void onRestart() {
		super.onRestart();
		Log.d(Consts.LOG_TAG, "onRestart: msremote");
	}

	public void onStop() {
		super.onStop();
		alarm.cancel(CheckMessagesPendingIntent);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),Consts.DELAY_LENGTH_INACTIVE, CheckMessagesPendingIntent);
		Log.d(Consts.LOG_TAG, "onStop: msremote");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Consts.LOG_TAG, "onDestroy: msremote");
	}

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(Consts.LOG_TAG, "onOptionsItemSelected: msremote");
		int calledMenuItem = item.getItemId();
		if (calledMenuItem == R.id.settings) {
			startActivity(new Intent(this, Preferences.class));
			return true;
		} else if (calledMenuItem == R.id.quit) {
			quit();
			return true;
		} else if (calledMenuItem == R.id.wifiset) {
			SharedPreferences.Editor editor = PreferenceManager
					.getDefaultSharedPreferences(f_context).edit();
			editor.putString("internalnetname",
					((WifiManager) getSystemService(Context.WIFI_SERVICE))
							.getConnectionInfo().getSSID());
			editor.commit();
			return true;
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// See which child activity is calling us back.
		if (resultCode == Consts.QUITREMOTE) {
			quit();
		}
	}

	private void activity_music() {
		Intent i = new Intent(this, MusicRemote.class);
		startActivityForResult(i, Consts.RC_MUSIC);
	}

	private void activity_torrent() {
		Intent i = new Intent(this, VideoList.class);
		startActivityForResult(i, Consts.RC_SHOW);
	}

	private void quit() {
		Log.i(Consts.LOG_TAG, "Quitting");
		alarm.cancel(CheckMessagesPendingIntent);
		setResult(Consts.QUITREMOTE);
		this.finish();
	}

}
