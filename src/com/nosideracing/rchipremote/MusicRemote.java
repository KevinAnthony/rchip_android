package com.nosideracing.rchipremote;

import com.nosideracing.rchipremote.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
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

public class MusicRemote extends Activity implements Runnable, OnClickListener {

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

	// We use pm and wl so that if when on the remote screen, we don't goto
	// sleep
	// bad for battery, but it's a remote, you don't want it going to sleep
	PowerManager pm;
	PowerManager.WakeLock wl;

	Handler musicHandler = new Handler() {
		/** Gets called on every message that is received */
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Consts.UPDATEGUI:
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
		Consts.LOG_TAG = this.getString(R.string.log_name);
		setContentView(R.layout.music);
		Log.d(Consts.LOG_TAG, "onCreate: Got to start");
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
		new Thread(new Runnable() {
			public void run() {
				while (update) {
					Soap.UpdateSongInfo();
					try {
						Thread.sleep(Soap.getDelay());
					} catch (InterruptedException e) {
						Log.w(Consts.LOG_TAG,e);
					}
				}
			}
		}).start();
	}

	public void onPause() {
		super.onPause();
		Log.d(Consts.LOG_TAG, "onPause: msmusic");
	}

	public void onResume() {
		super.onResume();
		Log.d(Consts.LOG_TAG, "onResume: msmusic");
		/* if update is false (we are not updating) update and set to true) */
		if (!update) {
			updateTags();
			update = true;
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
		// release the wake, lock and set update = false (so we don't keep
		// updating the screen when we don't have to
		wl.release();
		update = false;
		Log.d(Consts.LOG_TAG, "onStop: msmusic");
		Log.d(Consts.LOG_TAG, "Stoping Thread");
		// Finally we stop the thread
		stopThread();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Consts.LOG_TAG, "onDestroy msmusic");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu_child, menu);
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// See which child activity is calling us back.
		if (resultCode == Consts.QUITREMOTE) {
			quit();
		}
	}

	/* If we don't already have a thread running, start it */
	private synchronized void startThread() {
		Log.i(Consts.LOG_TAG, "msMusic: Starting New Thread");
		if (updater == null) {
			updater = new Thread(this);
			updater.start();
		} else {
			Log.w(Consts.LOG_TAG,
					"We Tried to start a thread when one existed already");
		}
	}

	private synchronized void stopThread() {
		Log.i(Consts.LOG_TAG, "msMusic: Stoping Old Thread");
		if (updater != null) {
			// moribund is being in the state of dieing
			Thread moribund = updater;
			updater = null;
			moribund.interrupt();
		} else {
			Log.w(Consts.LOG_TAG,
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
					m.what = Consts.UPDATEGUI;
					musicHandler.sendMessage(m);
					Thread.sleep(sleep);
				} catch (Exception ex) {
					Log.e(Consts.LOG_TAG, "ERROR in message sender, sleep");
					Log.e(Consts.LOG_TAG, "error:" + ex.getMessage());
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
			String text = Soap.getArtest();
			ARTIST.setText(text == null ? " " : text);
			text = Soap.getAlbum();
			ALBUM.setText(text == null ? " " : text);
			text = Soap.getSongName();
			TITLE.setText(text == null ? " " : text);
			if (Soap.getTimeElapised() != null) {
				ETIME.setText(formatIntoHHMMSS(Integer.parseInt(Soap
						.getTimeElapised())));
			}
			if (Soap.getSongLength() != null) {
				TOTTIME.setText(formatIntoHHMMSS(Integer.parseInt(Soap
						.getSongLength())));
			}

			/*
			 * Simply put, there was a problem with when you hit stop or play
			 * the button would switch but because of latency between all the
			 * components it would sometimes waffle between play and stop, so
			 * for 7 seconds after we hit the button we don't allow the
			 * updateTags to automatically change the button state
			 */
			if (System.currentTimeMillis() > dontSwitch + 7000L) {
				if (Soap.getIsPlaying() == 1) {
					// turn button one
					btn.setChecked(true);
				} else {
					btn.setChecked(false);
				}
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error Update Tags:" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
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
		Log.i(Consts.LOG_TAG, "Quitting");
		/* Unbinds the service, and closes the program */
		setResult(Consts.QUITREMOTE);
		this.finish();
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
			Soap.sendCmd(cmd, txt);
			return true;
		}
	}
}