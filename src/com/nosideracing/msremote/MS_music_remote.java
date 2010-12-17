package com.nosideracing.msremote;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MS_music_remote extends Activity implements Runnable, OnClickListener {

	// Tags from the GUI
	private TextView ARTIST;
	private TextView ALBUM;
	private TextView TITLE;
	private TextView ETIME;
	private TextView TOTTIME;
	private long dontSwitch;
	// We use update true/false so that if there's a problem, we don't try and
	// update with bad data
	private Boolean update = false;
	// updater is the update Thread
	private volatile Thread updater;
	// For the services the service then the actual connection
	private backendservice backendService;
	private BackendServiceConnection conn;
	// We use pm and wl so that if when on the remote screen, we don't goto
	// sleep
	// bad for battery, but it's a remote, you don't want it going to sleep
	PowerManager pm;
	PowerManager.WakeLock wl;

	Handler musicHandler = new Handler() {
		/** Gets called on every message that is received */
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MS_constants.UPDATEGUI:
				// We identified the Message by its What-ID
				if (update) {
					updateTags();
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MS_constants.LOG_TAG = this.getString(R.string.log_name);
		setContentView(R.layout.music);
		Log.d(MS_constants.LOG_TAG, "onCreate: Got to start");
		// get the wakelock from the PowerManager
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "msmusic");
		// Set the TextViews based in ID
		ARTIST = (TextView) findViewById(R.id.artest);
		ALBUM = (TextView) findViewById(R.id.album);
		TITLE = (TextView) findViewById(R.id.title);
		ETIME = (TextView) findViewById(R.id.etime);
		TOTTIME = (TextView) findViewById(R.id.tottime);
		// Create the 4 buttons, and when clicked, send commands to the SOAP
		// server
		// Play Stop(same thing) and next back
		CompoundButton btn = (ToggleButton) findViewById(R.id.play);
		btn.setOnClickListener(this);

		final Button button3 = (Button) findViewById(R.id.back);
		button3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Perform action on clicks
				new runCmd().execute("BACKRB", null);
			}
		});
		final Button button4 = (Button) findViewById(R.id.next);
		button4.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Perform action on clicks
				new runCmd().execute("NEXTRB", null);
			}
		});
		new startService().execute();
	}

	public void onPause() {
		super.onPause();
		Log.d(MS_constants.LOG_TAG, "onPause: msmusic");
	}

	public void onResume() {
		super.onResume();
		Log.d(MS_constants.LOG_TAG, "onResume: msmusic");
		/* if update is false (we are not updating) update and set to true) */
		if (!update) {
			updateTags();
			update = true;
		}
		// bind to the service
		conn = new BackendServiceConnection();
		Intent i = new Intent();
		i.setClassName("com.nosideracing.msremote",
				"com.nosideracing.msremote.msservice");
		try {
			bindService(i, conn, Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			Log.e(MS_constants.LOG_TAG, "Error binding service");
			Log.e(MS_constants.LOG_TAG, "Error:" + e.getMessage());
		}
		// lock the screen and start the update thread
		wl.acquire();
		startThread();
	}

	@Override
	public void onStop() {
		super.onStop();
		// signal to stop calling soap commands ever interval(interval varies
		// based on coverage and speed)
		try {
			backendService.stopMusicUpdating();
		} catch (Exception e) {
			Log.e(MS_constants.LOG_TAG, "Error StopMusicUpdating:" + e.getMessage());
		}
		// release the wake, lock and set update = false (so we don't keep
		// updating the screen when we don't have to
		wl.release();
		update = false;
		Log.d(MS_constants.LOG_TAG, "onStop: msmusic");
		Log.d(MS_constants.LOG_TAG, "Stoping Thread");
		// Finally we stop the thread
		stopThread();
		this.finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(conn);
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

	/* If we don't already have a thread running, start it */
	private synchronized void startThread() {
		Log.i(MS_constants.LOG_TAG, "msMusic: Starting New Thread");
		if (updater == null) {
			updater = new Thread(this);
			updater.start();
		} else {
			Log.w(MS_constants.LOG_TAG,
					"We Tried to start a thread when one existed already");
		}
	}

	private synchronized void stopThread() {
		Log.i(MS_constants.LOG_TAG, "msMusic: Stoping Old Thread");
		if (updater != null) {
			// moribund is being in the state of dieing
			Thread moribund = updater;
			updater = null;
			moribund.interrupt();
		} else {
			Log.w(MS_constants.LOG_TAG,
					"We Tried to kill a thread when one did not already exist");
		}
	}

	public void run() {
		// here we read the connectivity info, so we can update based on
		// connectivity
		// also if we are not connected, we don't try and fail to get soap
		// commands
		Context f_context = getApplicationContext();
		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		while ((Thread.currentThread() == updater) && (update)) {
			if (info.isConnected()) {
				int itype = info.getType();
				int sleep = 5000; // every 5 seconds
				if (itype == 1) { // type 1 is wifi
					sleep = 1000; // every 1 second
				}
				try {
					Message m = new Message();
					m.what = MS_constants.UPDATEGUI;
					musicHandler.sendMessage(m);
					Thread.sleep(sleep);
				} catch (Exception ex) {
					Log.e(MS_constants.LOG_TAG, "ERROR in message sender, sleep");
					Log.e(MS_constants.LOG_TAG, "error:" + ex.getMessage());
				}
			}
		}
	}

	/*
	 * WE update the tages buy pulling the info from the backend service however
	 * if we get null back, we set it to " " so we don't try and update with bad
	 * data
	 */
	private void updateTags() {
		try {
			CompoundButton btn = (ToggleButton) findViewById(R.id.play);
			String text = backendService.getArtest();
			ARTIST.setText(text == null ? " " : text);
			text = backendService.getAlbum();
			ALBUM.setText(text == null ? " " : text);
			text = backendService.getSongName();
			TITLE.setText(text == null ? " " : text);
			if (backendService.getTimeElapised() != null) {
				ETIME.setText(formatIntoHHMMSS(Integer.parseInt(backendService
						.getTimeElapised())));
			}
			if (backendService.getSongLength() != null) {
				TOTTIME.setText(formatIntoHHMMSS(Integer
						.parseInt(backendService.getSongLength())));
			}

			/*
			 * Simply put, there was a problem with when you hit stop or play
			 * the button would switch but because of latency between all the
			 * components it would sometimes waffle between play and stop, so
			 * for 7 seconds after we hit the button we don't allow the
			 * updateTags to automatically change the button state
			 */
			if (System.currentTimeMillis() > dontSwitch + 7000L) {
				if (backendService.getIsPlaying() == 1) {
					// turn button one
					btn.setChecked(true);
				} else {
					btn.setChecked(false);
				}
			}
		} catch (Exception e) {
			Log.e(MS_constants.LOG_TAG, "Error Update Tags:" + e.getMessage());
		}

	}

	/* Simple string paring number of seconds, to Hours:Min:seconds format */
	private String formatIntoHHMMSS(int secsIn) {
		int hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;
		String disHour = (hours < 10 ? "0" : "") + hours, disMinu = (minutes < 10
				& hours > 0 ? "0" : "")
				+ minutes, disSec = (seconds < 10 ? "0" : "") + seconds;
		return ((hours > 0 ? disHour + ":" : "") + disMinu + ":" + disSec);
	}

	private void quit() {
		Log.i(MS_constants.LOG_TAG, "Quitting");
		/* Unbinds the service, and closes the program */
		setResult(MS_constants.QUITREMOTE);
		this.finish();
	}

	class BackendServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			backendService = backendservice.Stub
					.asInterface((IBinder) boundService);
			Log.d(MS_constants.LOG_TAG, "onServiceConnected");
			try {
				// Here we start calling the soap server for music updates.
				backendService.startMusicUpdating();
			} catch (RemoteException e) {
				Log.e(MS_constants.LOG_TAG, "Error Start Music Updating:" + e.getMessage());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			backendService = null;
			Log.d(MS_constants.LOG_TAG, "onServiceDisconnected");
		}
	}

	public void onClick(View v) {
		CompoundButton btn = (ToggleButton) v;
		if (btn.isChecked()) {
			new runCmd().execute("PLAYRB", null);
			dontSwitch = System.currentTimeMillis();
		} else {
			new runCmd().execute("STOPRB", null);
			dontSwitch = System.currentTimeMillis();
			updateTags();
		}
	};

	private class runCmd extends AsyncTask<String, Integer, Boolean> {

		protected Boolean doInBackground(String... incoming) {
			String cmd = incoming[0];
			String txt = incoming[1];
			try {
				backendService.sendCmd(cmd, txt);
			} catch (RemoteException e) {
				Log.e(MS_constants.LOG_TAG, "Error sendcmd " + cmd + ":" + e.getMessage());
			}
			return true;
		}
	}
	
	public class startService extends AsyncTask<String, Integer, Boolean> {

		protected Boolean doInBackground(String... incoming) {
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

