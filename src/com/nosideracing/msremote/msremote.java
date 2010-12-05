package com.nosideracing.msremote;

import com.nosideracing.msremote.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
public class msremote extends Activity {

	/* the following numeric constants are for the Activity results */
	protected static final int RC_MUSIC = 0x0031;
	/* LOG_TAG for standard logging */
	private String LOG_TAG = "msremote";
	/* This is the Pref window from MENU->Settings */
	SharedPreferences settings;
	/* For the services the service then the actual connection */
	private backendservice backendService;
	private BackendServiceConnection conn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Log.w("msremote", "Opps it's null!!");
		}
		setContentView(R.layout.main);
		Context f_context = getApplicationContext();

		/*
		 * We put the LOG_TAG in the string.xml file, so we can change it one
		 * place not everyplace in our code
		 */
		LOG_TAG = f_context.getString(R.string.log_name);
		Log.d(LOG_TAG, "onCreate: msremote");
		/*
		 * We pull the settings from the prefmanager, then we pull the telephone
		 * number to use as a HOST_ID the reason i used the telephone number was
		 * so that if we ever use registration it doesn't change if you get a
		 * new phone
		 */
		settings = PreferenceManager.getDefaultSharedPreferences(f_context);
		TelephonyManager tManager = (TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber;
		phoneNumber = tManager.getLine1Number();
		/* If there is no phone number, we use (111) 111-1111 */
		if (phoneNumber == null) {
			phoneNumber = "1111111111";
		}
		/* creates the music button */
		final Button button_music = (Button) findViewById(R.id.music);
		button_music.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				Log.d(LOG_TAG,"Music Pushed");
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
			Log.e(LOG_TAG, "Error:" + e.getMessage());
		}
		/* creates the Torrent button */
		// final Button button_torrent = (Button) findViewById(R.id.torrent2);
		// button_torrent.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		/* Perform action on clicks */
		// activity_torrent();
		// }
		// });
		/* Pulls the URL, and Destination Host Name from the settings */
		String msb_url = settings.getString("serverurl",
				"http://192.168.1.4:8080/");
		String msb_desthost = settings.getString("serverhostname", "Tomoya");
		Boolean msb_ktornot = settings.getBoolean("ktorrentcheck", false);
		/*
		 * Starting Service Creates an Intent, sets some extra's (settings)
		 * connects and binds to said service
		 */
		new startService().execute(msb_url, phoneNumber, msb_desthost,
				msb_ktornot.toString(), LOG_TAG);

	}

	public void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause: msremote");
	}

	public void onResume() {
		super.onResume();
		/*
		 * if we get to this spot, we clear the notifications, we should also be
		 * doing this in any other activity that potentialy could be called.
		 */
		try {
			backendService.clearNotifications();
		} catch (Exception e) {
			Log
					.e(
							LOG_TAG,
							"Error Clearing Notifications(We get this on startup sometimes, because the service hasn't get been started):"
									+ e.getLocalizedMessage());
		}
		Log.d(LOG_TAG, "onResume: msremote");
	}

	public void onRestart() {
		super.onRestart();
		Log.d(LOG_TAG, "onRestart: msremote");
	}

	public void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop: msremote");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy: msremote");
	}

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, "onOptionsItemSelected: msremote");
		int calledMenuItem = item.getItemId();
		if (calledMenuItem == R.id.settings) {
			startActivity(new Intent(this, msprefs.class));
			return true;
		} else if (calledMenuItem == R.id.quit) {
			quit();
			return true;
		}
		return false;
	}

	private void activity_music() {
		Intent i = new Intent(this, msmusic.class);
		startActivityForResult(i, RC_MUSIC);
	}

	private void activity_torrent() {
		Intent i = new Intent(this, mstorrent.class);
		startActivity(i);
	}

	private void quit() {
		Log.i(LOG_TAG, "Quitting");
		/* Unbinds the service, and closes the program */
		unbindService(conn);
		this.finish();
	}

	class BackendServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			backendService = backendservice.Stub
					.asInterface((IBinder) boundService);
			Log.d(LOG_TAG, "onServiceConnected: msremote");
		}

		public void onServiceDisconnected(ComponentName className) {
			backendService = null;
			Log.d(LOG_TAG, "onServiceDisconnected: msremote");
		}
	}

	private class startService extends AsyncTask<String, Integer, Boolean> {

		protected Boolean doInBackground(String... incoming) {
			String msb_url = incoming[0];
			String phoneNumber = incoming[1];
			String msb_desthost = incoming[2];
			Boolean msb_ktornot = Boolean.parseBoolean(incoming[3]);
			String log_tag = incoming[4];
			Intent i1 = new Intent();
			i1.setAction("com.nosideracing.msremote.msservice");
			i1.putExtra("SETTING_URL", msb_url);
			i1.putExtra("SETTING_SOURCENAME", phoneNumber);
			i1.putExtra("SETTING_DESTNAME", msb_desthost);
			i1.putExtra("SETTING_KTORRENTNOTIFICATION", msb_ktornot);
			i1.putExtra("SETTING_LOG_TAG", log_tag);
			startService(i1);
			conn = new BackendServiceConnection();
			Intent i = new Intent();
			i.setClassName("com.nosideracing.msremote",
					"com.nosideracing.msremote.msservice");
			try {
				bindService(i, conn, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.e(LOG_TAG, "Error binding service");
				Log.e(LOG_TAG, "Error:" + e.getMessage());
				return false;
			}
			return true;
		}
	}
}
