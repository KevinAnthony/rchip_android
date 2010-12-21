package com.nosideracing.msremote;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
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
public class MS_remote extends Activity {

	/* This is the Pref window from MENU->Settings */
	SharedPreferences settings;
	/* For the services the service then the actual connection */
	static backendservice backendService;
	private BackendServiceConnection conn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Log.w(MS_constants.LOG_TAG, "Opps it's null!!");
		}
		setContentView(R.layout.main);
		Context f_context = getApplicationContext();

		/*
		 * We put the LOG_TAG in the string.xml file, so we can change it one
		 * place not everyplace in our code
		 */
		Log.d(MS_constants.LOG_TAG, "onCreate: msremote");
		/*
		 * We pull the settings from the prefmanager, then we pull the telephone
		 * number to use as a HOST_ID the reason i used the telephone number was
		 * so that if we ever use registration it doesn't change if you get a
		 * new phone
		 */
		settings = PreferenceManager.getDefaultSharedPreferences(f_context);
		settings.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(
							SharedPreferences prefs, String key) {
						Log.w(MS_constants.LOG_TAG, "SharePrefsChanged");
						if (conn != null){
							restartService();
						}
						System.out.println(key);
					}
				});
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
				Log.d(MS_constants.LOG_TAG, "Music Pushed");
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
			Log.e(MS_constants.LOG_TAG, "Error:" + e.getMessage());
		}
		
		SharedPreferences.Editor editor = settings.edit();
		
		/* Pulls the URL, and Destination Host Name from the settings */
		if (settings.getBoolean("firstRun", true)) {
			startActivity(new Intent(this, MS_preferences.class));
			
		    editor.putBoolean("firstRun", false);
		    editor.commit();
		}
		String msb_url_external = settings.getString("serverurlexternal",
				"http://173.3.14.224:500/");
		String msb_url_internal = settings.getString("serverurlinternal",
				"http://192.168.1.3:500/");
		String msb_desthost = settings.getString("serverhostname", "Tomoya");
		Boolean msb_ktornot = settings.getBoolean("ktorrentcheck", false);
		String int_net_name = settings.getString("internalnetname", "Node_77");
		String int_delay = settings.getString("internaldelay", "1000");
		String ext_delay = settings.getString("externaldelay", "5000");
		/*
		 * Starting Service Creates an Intent, sets some extra's (settings)
		 * connects and binds to said service
		 */
		
		new startService().execute(msb_url_external, msb_url_internal,
				phoneNumber, msb_desthost, msb_ktornot.toString(),
				int_net_name, int_delay, ext_delay);
	}

	public void onPause() {
		super.onPause();
		Log.d(MS_constants.LOG_TAG, "onPause: msremote");
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
							MS_constants.LOG_TAG,
							"Error Clearing Notifications(We get this on startup sometimes, because the service hasn't get been started):"
									+ e.getLocalizedMessage());
		}
		Log.d(MS_constants.LOG_TAG, "onResume: msremote");
	}

	public void onRestart() {
		super.onRestart();
		restartService();
		Log.d(MS_constants.LOG_TAG, "onRestart: msremote");
	}

	public void onStop() {
		super.onStop();
		Log.d(MS_constants.LOG_TAG, "onStop: msremote");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(MS_constants.LOG_TAG, "onDestroy: msremote");
	}

	
	
	/* Creates the menu items */
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
	
	protected void restartService() {
		Log.i(MS_constants.LOG_TAG, "Restarting Service(i think)");
		unbindService(conn);
			
		String msb_url_external = settings.getString("serverurlexternal",
				"http://173.3.14.224:500/");
		String msb_url_internal = settings.getString("serverurlinternal",
				"http://192.168.1.3:500/");
		String msb_desthost = settings.getString("serverhostname", "Tomoya");
		Boolean msb_ktornot = settings.getBoolean("ktorrentcheck", false);
		String int_net_name = settings.getString("internalnetname", "Node_77");
		String int_delay = settings.getString("internaldelay", "1000");
		String ext_delay = settings.getString("externaldelay", "5000");
		/*
		 * Starting Service Creates an Intent, sets some extra's (settings)
		 * connects and binds to said service
		 */
		Context f_context = getApplicationContext();
		TelephonyManager tManager = (TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber;
		phoneNumber = tManager.getLine1Number();
		/* If there is no phone number, we use (111) 111-1111 */
		if (phoneNumber == null) {
			phoneNumber = "1111111111";
		}
		new startService().execute(msb_url_external, msb_url_internal,
				phoneNumber, msb_desthost, msb_ktornot.toString(),
				int_net_name, int_delay, ext_delay);
	}

	private void activity_music() {
		Intent i = new Intent(this, MS_music_remote.class);
		startActivityForResult(i, MS_constants.RC_MUSIC);
	}

	private void activity_torrent() {
		Intent i = new Intent(this, MS_show_list.class);
		startActivityForResult(i, MS_constants.RC_SHOW);
	}

	private void quit() {
		Log.i(MS_constants.LOG_TAG, "Quitting");
		/* Unbinds the service, and closes the program */
		unbindService(conn);
		setResult(MS_constants.QUITREMOTE);
		this.finish();
	}

	class BackendServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			backendService = backendservice.Stub
					.asInterface((IBinder) boundService);
			Log.d(MS_constants.LOG_TAG, "onServiceConnected: msremote");
		}

		public void onServiceDisconnected(ComponentName className) {
			backendService = null;
			Log.d(MS_constants.LOG_TAG, "onServiceDisconnected: msremote");
		}
	}

	public class startService extends AsyncTask<String, Integer, Boolean> {

		protected Boolean doInBackground(String... incoming) {
			String msb_url_external = incoming[0];
			String msb_url_internal = incoming[1];
			String phoneNumber = incoming[2];
			String msb_desthost = incoming[3];
			Boolean msb_ktornot = Boolean.parseBoolean(incoming[4]);
			String msb_int_net_name = incoming[5];
			int msb_int_delay = Integer.parseInt(incoming[6]);
			int msb_ext_delay = Integer.parseInt(incoming[7]);
			Intent i1 = new Intent();
			i1.setAction("com.nosideracing.msremote.MS_soap_service");
			i1.putExtra("SETTING_URL_EXTERNAL", msb_url_external);
			i1.putExtra("SETTING_URL_INTERNAL", msb_url_internal);
			i1.putExtra("SETTING_INTERNAL_NETWORK_NAME", msb_int_net_name);
			i1.putExtra("SETTING_SOURCENAME", phoneNumber);
			i1.putExtra("SETTING_DESTNAME", msb_desthost);
			i1.putExtra("SETTING_KTORRENTNOTIFICATION", msb_ktornot);
			i1.putExtra("SETTING_EXTERNAL_DELAY", msb_ext_delay);
			i1.putExtra("SETTING_INTERNAL_DELAY", msb_int_delay);
			startService(i1);
			conn = new BackendServiceConnection();
			Intent i = new Intent();
			i.setClassName("com.nosideracing.msremote",
					"com.nosideracing.msremote.MS_soap_service");
			try {
				bindService(i, conn, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.e(MS_constants.LOG_TAG, "Error binding service");
				Log.e(MS_constants.LOG_TAG, "Error:" + e.getMessage());
				return false;
			}
			return true;
		}
	}
}
