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

import com.nosideracing.rchipremote.Consts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

	private TextView ARTIST;
	private TextView ALBUM;
	private TextView TITLE;
	private TextView ETIME;
	private TextView TOTTIME;
	private long dontSwitch;
	private Boolean update = false;
	private volatile Thread updater;
	PowerManager pm;
	PowerManager.WakeLock wl;
	JSON json;
	Handler musicHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Consts.UPDATEGUI:
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
		setContentView(R.layout.music);
		json = JSON.getInstance();
		if (json == null){
			JSON.initInstance(getApplicationContext());
			json = JSON.getInstance();
		}
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "msmusic");
		ARTIST = (TextView) findViewById(R.id.artest);
		ALBUM = (TextView) findViewById(R.id.album);
		TITLE = (TextView) findViewById(R.id.title);
		ETIME = (TextView) findViewById(R.id.etime);
		TOTTIME = (TextView) findViewById(R.id.tottime);
		CompoundButton btn = (ToggleButton) findViewById(R.id.play);
		btn.setOnClickListener(this);

		final Button button3 = (Button) findViewById(R.id.back);
		button3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute("BACKRB", "");
			}
		});
		final Button button4 = (Button) findViewById(R.id.next);
		button4.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute("NEXTRB", "");
			}
		});
		new Thread(new Runnable() {
			public void run() {
				while (update) {
					json.UpdateSongInfo();
					try {
						Thread.sleep(Integer.parseInt(PreferenceManager
								.getDefaultSharedPreferences(
										getApplicationContext()).getString(
										"delay", "5000")));
					} catch (InterruptedException e) {
						Log.w(Consts.LOG_TAG, e);
					}
				}
			}
		}).start();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!update) {
			updateTags();
			update = true;
		}
		wl.acquire();
		startThread();
	}

	@Override
	public void onStop() {
		super.onStop();
		wl.release();
		update = false;
		stopThread();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu_child, menu);
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

	private synchronized void startThread() {
		if (updater == null) {
			updater = new Thread(this);
			updater.start();
		} else {
			Log.w(Consts.LOG_TAG,
					"We Tried to start a thread when one existed already, MusicRemote->startThread()");
		}
	}

	private synchronized void stopThread() {
		if (updater != null) {
			Thread moribund = updater;
			updater = null;
			moribund.interrupt();
		} else {
			Log.w(Consts.LOG_TAG,
					"We Tried to kill a thread when one did not already exist");
		}
	}

	public void run() {
		NetworkInfo info = ((ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		while ((Thread.currentThread() == updater) && (update)) {
			if (info.isConnected()) {
				// TODO: Replace this with preferences
				int itype = info.getType();
				int sleep = 5000;
				if (itype == 1) {
					sleep = 1000;
				}
				try {
					Message m = new Message();
					m.what = Consts.UPDATEGUI;
					musicHandler.sendMessage(m);
					Thread.sleep(sleep);
				} catch (Exception e) {
					Log.e(Consts.LOG_TAG, "ERROR in message sender, sleep", e);
				}
			}
		}
	}

	private void updateTags() {
		try {
			CompoundButton btn = (ToggleButton) findViewById(R.id.play);
			String text = json.getArtest();
			ARTIST.setText(text == null ? " " : text);
			text = json.getAlbum();
			ALBUM.setText(text == null ? " " : text);
			text = json.getSongName();
			TITLE.setText(text == null ? " " : text);
			if (json.getTimeElapised() != null) {
				ETIME.setText(formatIntoHHMMSS(Integer.parseInt(json
						.getTimeElapised())));
			}
			if (json.getSongLength() != null) {
				TOTTIME.setText(formatIntoHHMMSS(Integer
						.parseInt(json.getSongLength())));
			}

			if (System.currentTimeMillis() > dontSwitch + 7000L) {
				if (json.getIsPlaying() == 1) {
					btn.setChecked(true);
				} else {
					btn.setChecked(false);
				}
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error Update Tags:", e);
		}

	}

	private String formatIntoHHMMSS(int secsIn) {
		int hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;
		String disHour = (hours < 10 ? "0" : "") + hours, disMinu = (minutes < 10
				& hours > 0 ? "0" : "")
				+ minutes, disSec = (seconds < 10 ? "0" : "") + seconds;
		return ((hours > 0 ? disHour + ":" : "") + disMinu + ":" + disSec);
	}

	private void quit() {
		setResult(Consts.QUITREMOTE);
		this.finish();
	}

	public void onClick(View v) {
		CompoundButton btn = (ToggleButton) v;
		if (btn.isChecked()) {
			new runCmd().execute("PLAYRB", "");
			dontSwitch = System.currentTimeMillis();
		} else {
			new runCmd().execute("STOPRB", "");
			dontSwitch = System.currentTimeMillis();
			updateTags();
		}
	};

	private class runCmd extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... incoming) {
			String cmd = incoming[0];
			String cmdTxt = incoming[1];
			Map<String, String> params = new HashMap<String, String>();
			params.put("command", cmd);
			params.put("command_text", cmdTxt);
			params.put("source_hostname", RemoteMain.phoneNumber);
			params.put("destination_hostname", RemoteMain.msb_desthost);
			json.JSONSendCmd("sendcommand", params);
			return true;
		}
	}
}
