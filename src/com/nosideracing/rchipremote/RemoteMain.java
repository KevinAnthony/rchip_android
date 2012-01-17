package com.nosideracing.rchipremote;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
 * duplicate-> start rchip, Music home, rchip,Music(two instances of window now)
 *
 */
public class RemoteMain extends Activity {
	PendingIntent CheckMessagesPendingIntent;
	AlarmManager alarm;
	/* This is the Pref window from MENU->Settings */
	SharedPreferences settings;
	private static Context f_context;
	/* For the services the service then the actual connection */
	public static String msb_desthost;
	public static String phoneNumber;
	public static JSON json;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Log.w(Consts.LOG_TAG,
					"SaveInstantsState is NULL if the program isn't starting for the first time, this is a problem");
		}
		setContentView(R.layout.main);
		f_context = getApplicationContext();

		/*
		 * we have to authenticate json early
		 */
		json = new JSON(f_context);
		if (!json.authenticate()) {
			bad_password();
		}
		/*
		 * We pull the settings from the prefmanager, then we pull the telephone
		 * number to use as a HOST_ID the reason i used the telephone number was
		 * so that if we ever use registration it doesn't change if you get a
		 * new phone
		 */
		settings = PreferenceManager.getDefaultSharedPreferences(f_context);
		settings.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs,
					String key) {
				// TODO:Set somekind of flag here
			}
		});
		TelephonyManager tManager = (TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE);
		phoneNumber = tManager.getLine1Number();
		/* If there is no phone number, we use (111) 111-1111 */
		if (phoneNumber == null) {
			phoneNumber = "1111111111";
		}
		msb_desthost = settings.getString("serverhostname", "Tomoya");
		final Button button_music = (Button) findViewById(R.id.music);
		button_music.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity_music();
			}
		});
		Button button_tor = (Button) findViewById(R.id.torrent);
		button_tor.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity_torrent();
			}
		});
		/*Button button_shows = (Button) findViewById(R.id.shows);
		button_shows.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity_show_list();
			}
		});*/

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
		CheckMessagesPendingIntent = PendingIntent.getBroadcast(this, 192837,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
		int delay = Integer.parseInt(settings.getString("networkdelay",
				"300000"));
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
				delay, CheckMessagesPendingIntent);
		Map<String, String> params = new HashMap<String, String>();
		params.put("device_name", phoneNumber);
		params.put("state", "true");
		json.JSONSendCmd("registerremotedevice", params);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		/*
		 * if we get to this spot, we clear the notifications, we should also be
		 * doing this in any other activity that potentialy could be called.
		 */
		Notifications.clearAllNotifications(getApplicationContext());
	}

	@Override
	public void onRestart() {
		super.onRestart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		alarm.cancel(CheckMessagesPendingIntent);
		json.deauthenticate();
	}

	/* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int calledMenuItem = item.getItemId();
		if (calledMenuItem == R.id.settings) {
			startActivity(new Intent(this, Preferences.class));
			return true;
		} else if (calledMenuItem == R.id.quit) {
			quit();
			return true;
		}
		return false;
	}

	@Override
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

	/*private void activity_show_list() {
		Intent i = new Intent(this, UpcomingShowList.class);
		startActivityForResult(i, Consts.RC_SHOW_LIST);
	}*/

	private void bad_password() {
		AlertDialog alert;
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
		alt_bld.setMessage("Bad Password")
				.setCancelable(false)
				.setPositiveButton("Fix",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						})
				.setNegativeButton("Exit",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								quit();
							}
						});
		alert = alt_bld.create();
		// Title for AlertDialog
		alert.setTitle("ERROR");
		// Icon for AlertDialog
		alert.setIcon(R.drawable.icon);
		alert.show();
		startActivity(new Intent(this, Preferences.class));
	}

	private void quit() {
		alarm.cancel(CheckMessagesPendingIntent);
		setResult(Consts.QUITREMOTE);
		this.finish();
	}

}
