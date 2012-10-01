/*
\ * rchip remote - android application for RCHIP interface
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class VideoRemote extends Activity implements OnClickListener {

	private Button button_quit;
	private Button button_mute;
	private Button button_stop;
	private Button button_foward;
	private Button button_rewind;
	private CompoundButton button_fullscreen;
	private CompoundButton button_playpause;
	private TextView topText;
	private boolean firstPlay;
	private String loc;
	private String showString;
	private String destHost;
	public static String DEVICE_ID;
	private long ID = -1;
	PowerManager.WakeLock wl;
	JSON json;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		json = JSON.getInstance();
		setContentView(R.layout.watch);
		Bundle incoming = getIntent().getExtras();
		showString = incoming.getString("showString");
		ID = incoming.getLong("showID");
		loc = incoming.getString("Location");
		destHost = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("serverhostname", "Tomoya");
		DEVICE_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		topText = (TextView) findViewById(R.id.MSWMTopText);
		topText.setText(showString);
		topText.setSelected(true);
		firstPlay = true;
		createButtons();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		wl = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, "VideoRemote");
		wl.acquire();
		set_sizes();
	}

	@Override
	public void onStop() {
		super.onStop();
		wl.release();
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
			quit(true);
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Consts.QUITREMOTE) {
			quit(true);
		}
	}

	public void onClick(View v) {
		CompoundButton btn = (ToggleButton) v;
		if (btn.getId() == R.id.MSWMfullscreen) {
			new runCmd().execute(Consts.VIDEO_FULLSCREEN_TOGGLE, "");
			
		} else if (btn.getId() == R.id.MSWMplaypause) {

			if (btn.isChecked()) {
				if (firstPlay) {
					String rootPath = json.getRootPath();
					if (rootPath == null){
						json.set_context(getApplicationContext(),DEVICE_ID);
						rootPath = json.getRootPath();
					}
					try {
						loc = loc.replace("/mnt/raid/", rootPath);
						new runCmd().execute(Consts.VIDEO_OPEN, loc);
						Thread.sleep(500);
						firstPlay = false;
					} catch (Exception e) {
						Log.e(Consts.LOG_TAG, "Problem with playing " + loc, e);
					}

				}
				try {
					json.UpdateSongInfo();
					if (json.getIsPlaying()) {
						Log.v(Consts.LOG_TAG, "Stopping Music to play Video");
						new runCmd().execute(Consts.MUSIC_STOP, "");
						Thread.sleep(500);
					}
				} catch (Exception e) {
				}
				new runCmd().execute(Consts.VIDEO_PLAY, "");
			} else {
				new runCmd().execute(Consts.VIDEO_PAUSE, "");
			}
		}
	};

	private void createButtons() {

		button_fullscreen = (ToggleButton) findViewById(R.id.MSWMfullscreen);
		button_fullscreen.setOnClickListener(this);
		button_playpause = (ToggleButton) findViewById(R.id.MSWMplaypause);
		button_playpause.setOnClickListener(this);
		button_rewind = (Button) findViewById(R.id.MSWMrewind);
		button_rewind.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.VIDEO_SKIP_BACKWARDS, "");
			}
		});
		button_foward = (Button) findViewById(R.id.MSWMfoward);
		button_foward.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.VIDEO_SKIP_FOWARD, "");
			}
		});
		button_stop = (Button) findViewById(R.id.MSWMstop);
		button_stop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.VIDEO_STOP, "");
				CompoundButton btn = (ToggleButton) findViewById(R.id.MSWMfullscreen);
				if (btn.isChecked()) {
					btn.setChecked(false);
				}
				btn = (ToggleButton) findViewById(R.id.MSWMplaypause);
				if (btn.isChecked()) {
					btn.setChecked(false);
				}
			}
		});
		button_mute = (Button) findViewById(R.id.MSWMmute);
		button_mute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.VIDEO_MUTE, "");
			}
		});
		button_quit = (Button) findViewById(R.id.MSWMquit);
		button_quit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new runCmd().execute(Consts.VIDEO_QUIT, "");
				quit(false);
			}
		});
		set_sizes();
	}

	private void set_sizes() {
		int button_width = (((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getWidth())/3;
		int height = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getHeight();

		LinearLayout ll1 = (LinearLayout) findViewById(R.id.LL_buttons_first);
		ll1.setMinimumHeight(height / 3);
		LinearLayout ll2 = (LinearLayout) findViewById(R.id.LL_buttons_second);
		ll2.setMinimumHeight(height / 3);
		button_rewind.setWidth(button_width);
		button_foward.setWidth(button_width);
		button_stop.setWidth(button_width);
		button_mute.setWidth(button_width);
		button_quit.setWidth(button_width);
		button_fullscreen.setWidth(button_width);
		button_playpause.setWidth(button_width);
	}

	private void quit(Boolean quitProgram) {
		if (quitProgram) {
			exit(1);
		} else {
			AlertDialog alert;
			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
			alt_bld.setMessage(getString(R.string.done_watching_show))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									exit(2);
								}
							})
					.setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									exit(0);
								}
							});
			alert = alt_bld.create();
			alert.setTitle(showString);
			alert.setIcon(R.drawable.application_icon);
			alert.show();
		}

	}

	private void exit(int flag) {
		if (flag == 1) {
			setResult(Consts.QUITREMOTE);
		} else if (flag == 2) {
			Intent i = new Intent();
			i.putExtra("showID", ID);
			setResult(Consts.REMOVESHOW, i);
		}
		this.finish();
	}

	private class runCmd extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... incoming) {
			String cmd = incoming[0];
			String cmdTxt = incoming[1];
			Map<String, String> params = new HashMap<String, String>();
			params.put("command", cmd);
			params.put("command_text", cmdTxt);
			params.put("source_hostname", DEVICE_ID);
			params.put("destination_hostname", destHost);
			json.JSONSendCmd("sendcommand", params);
			return true;
		}
	}

}
