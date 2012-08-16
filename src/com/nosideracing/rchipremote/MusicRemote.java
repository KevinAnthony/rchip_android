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
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MusicRemote extends Activity implements OnClickListener {

	private TextView ARTIST;
	private TextView ALBUM;
	private TextView TITLE;
	private TextView TOTTIME;
	private long dont_switch_play_button_timer;
	private Boolean update = false;

	private Button back;
	private Button next;
	private Button stop;
	private CompoundButton play;

	PowerManager pm;
	PowerManager.WakeLock wl;
	JSON json;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music);
		json = JSON.getInstance();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "msmusic");
		ARTIST = (TextView) findViewById(R.id.artest);
		ALBUM = (TextView) findViewById(R.id.album);
		TITLE = (TextView) findViewById(R.id.title);
		TOTTIME = (TextView) findViewById(R.id.tottime);

		play = (ToggleButton) findViewById(R.id.play);
		play.setOnClickListener(this);

		back = (Button) findViewById(R.id.back);
		back.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.MUSIC_BACK, "");
			}
		});
		next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.MUSIC_NEXT, "");
			}
		});

		stop = (Button) findViewById(R.id.stop);
		stop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.MUSIC_STOP, "");
			}
		});

		final Handler handler = new Handler();
		Timer timer = new Timer();
		int speed = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext())
				.getString("delay", "5000"));
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						json.UpdateSongInfo();
						updateTags();
					}
				});
			}
		}, 0, speed);

		set_sizes();
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
	}

	@Override
	public void onStop() {
		super.onStop();
		wl.release();
		update = false;
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

	private void set_sizes() {
		int width = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getWidth();
		int button_height = ((((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getHeight()) - play.getTop()) / 6;

		LinearLayout ll1 = (LinearLayout) findViewById(R.id.LL_Next_Prev);
		ll1.setMinimumHeight(button_height);

		play.setMinHeight(button_height);
		stop.setMinHeight(button_height);

		back.setMinWidth(width / 2);
		next.setMinWidth(width / 2);
	}

	private void updateTags() {
		try {
			String text = json.getArtest();
			ARTIST.setText(text == null ? " " : text);
			text = json.getAlbum();
			ALBUM.setText(text == null ? " " : text);
			text = json.getSongName();
			TITLE.setText(text == null ? " " : text);
			if (json.getSongLength() != null) {
				TOTTIME.setText(formatIntoHHMMSS(Integer.parseInt(json
						.getSongLength())));
			}

			if (System.currentTimeMillis() > dont_switch_play_button_timer + 7000L) {
				if (json.getIsPlaying() == 1) {
					play.setChecked(true);
				} else {
					play.setChecked(false);
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
		new runCmd().execute(Consts.MUSIC_PLAYPAUSE_TOGGLE, "");
		dont_switch_play_button_timer = System.currentTimeMillis();
		updateTags();
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
