package com.nosideracing.msremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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

public class MS_watch_movie extends Activity implements OnClickListener {
	/*
	class BackendService_Connection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			myService = backendservice.Stub.asInterface(boundService);
			Log.d(MS_constants.LOG_TAG, "onServiceConnected:MS_watch_movie");
			try {
				// Here we start calling the soap server for music updates.
				myService.startMusicUpdating();
			} catch (RemoteException e) {
				Log.e(MS_constants.LOG_TAG, "Error Start Music Updating:"
						+ e.getMessage());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			myService = null;
			Log.d(MS_constants.LOG_TAG, "onServiceDisconnected");
		}
	}
*/
	private Button button_quit;
	private Button button_mute;
	private Button button_pause;
	private Button button_foward;
	private Button button_rewind;
	private CompoundButton button_fullscreen;
	private CompoundButton button_playpause;
	private TextView sName;
	private TextView eNum;
	private boolean firstPlay;
	private String loc;

	PowerManager.WakeLock wl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.watch);
		Log.d(MS_constants.LOG_TAG, "onCreate: MS_Watch_Movie");
		Bundle incoming = getIntent().getExtras();
		String ShowName = incoming.getString("ShowName");
		String EpsNumber = incoming.getString("EpsNumber");
		loc = incoming.getString("Location");
		sName = (TextView) findViewById(R.id.sName);
		sName.setText(ShowName);
		eNum = (TextView) findViewById(R.id.eNum);
		eNum.setText(EpsNumber);
		firstPlay = true;
		createButtons();
		// bind to the service

	}

	public void onPause() {
		super.onPause();
		Log.d(MS_constants.LOG_TAG, "onPause: MS_Watch_Movie");
	}

	public void onResume() {
		super.onResume();
		Log.d(MS_constants.LOG_TAG, "onResume: MS_Watch_Movie");
		wl = (WakeLock) ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
						| PowerManager.ON_AFTER_RELEASE, "MS_watch_movie");
		wl.acquire();
		set_sizes();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(MS_constants.LOG_TAG, "onStop: MS_Watch_Movie");
		wl.release();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		quit(false);
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
			quit(true);
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
		if (resultCode == MS_constants.QUITREMOTE) {
			quit(true);
		}
	}

	public void onClick(View v) {
		CompoundButton btn = (ToggleButton) v;
		if (btn.getId() == R.id.fullscreen) {
			if (btn.isChecked()) {
				new runCmd().execute("FULLONSM", null);
			} else {
				new runCmd().execute("FULLONSM", null);
			}
		} else if (btn.getId() == R.id.playpause) {

			if (btn.isChecked()) {
				if (firstPlay) {
					String rootPath= PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("videopath","/mnt/raid/");;
					new runCmd().execute("OPENSM", loc.replace("/mnt/raid/", rootPath));
				}
				new runCmd().execute("PLAYSM", null);
			} else {
				new runCmd().execute("STOPSM", null);
			}
		}
	};

	private void createButtons() {
		
		
		button_fullscreen = (ToggleButton) findViewById(R.id.fullscreen);
		button_fullscreen.setOnClickListener(this);
		button_playpause = (ToggleButton) findViewById(R.id.playpause);
		button_playpause.setOnClickListener(this);
		button_rewind = (Button) findViewById(R.id.rewind);
		button_rewind.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				new runCmd().execute("SKIPBSM", null);
			}
		});
		button_foward = (Button) findViewById(R.id.foward);
		button_foward.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				new runCmd().execute("SKIPFSM", null);
			}
		});
		button_pause = (Button) findViewById(R.id.pause);
		button_pause.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				new runCmd().execute("PAUSESM", null);
			}
		});
		button_mute= (Button) findViewById(R.id.mute);
		button_mute.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				new runCmd().execute("MUTESM", null);
			}
		});
		button_quit = (Button) findViewById(R.id.quit);
		button_quit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/* Perform action on clicks */
				new runCmd().execute("QUITSM", null);
				quit(false);
			}
		});
		set_sizes();
	}
	
	private void set_sizes(){
		int width = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth(); 
		int height = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight(); 
		

		LinearLayout ll1 = (LinearLayout) findViewById(R.id.LL_buttons_first);
		ll1.setMinimumHeight(height/3);
		LinearLayout ll2 = (LinearLayout) findViewById(R.id.LL_buttons_second);
		ll2.setMinimumHeight(height/3);
		button_rewind.setWidth(width/3);
		button_foward.setWidth(width/3);
		button_pause.setWidth(width/3);
		button_mute.setWidth(width/3);
		button_quit.setWidth(width/3);
		button_fullscreen.setWidth(width/3);
		button_playpause.setWidth(width/3);
	}
	private void quit(Boolean quitProgram) {
		Log.i(MS_constants.LOG_TAG, "Quitting");
		if (quitProgram){
			setResult(MS_constants.QUITREMOTE);
		}
		this.finish();
	}

	
	private class runCmd extends AsyncTask<String, Integer, Boolean> {

		protected Boolean doInBackground(String... incoming) {
			String cmd = incoming[0];
			String txt = incoming[1];
			try {
				MS_remote.backendService.sendCmd(cmd, txt);
			} catch (Exception e) {
				Log.e(MS_constants.LOG_TAG, "Error sendcmd " + cmd + ":"
						+ e.getMessage());
			}
			return true;
		}
	}
}
