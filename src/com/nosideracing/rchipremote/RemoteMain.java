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

public class RemoteMain extends Activity {
	PendingIntent CheckMessagesPendingIntent;
	AlarmManager alarm;
	SharedPreferences settings;
	private static Context f_context;
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
		json = new JSON(f_context);
		if (!json.authenticate()) {
			bad_password();
		}
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
		Button button_shows = (Button) findViewById(R.id.shows);
		button_shows.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity_show_list();
			}
		});

		SharedPreferences.Editor editor = settings.edit();
		if (settings.getBoolean("firstRun", true)) {
			startActivity(new Intent(this, Preferences.class));
			editor.putBoolean("firstRun", false);
			editor.commit();
		}
		Intent intent = new Intent(f_context, CheckMessages.class);
		CheckMessagesPendingIntent = PendingIntent.getBroadcast(this, 192837,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

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

	private void activity_show_list() {
		Intent i = new Intent(this, UpcomingShowList.class);
		startActivityForResult(i, Consts.RC_SHOW_LIST);
	}

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
		alert.setTitle("ERROR");
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
