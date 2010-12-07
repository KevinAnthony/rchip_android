package com.nosideracing.msremote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class mstorrent extends Activity {
	private static String LOG_TAG = "msremote";
	private ListView torrents;
	private static List<String> torName = new ArrayList<String>();
	private static List<String> torNumber = new ArrayList<String>();
	private static List<String> torEpsName = new ArrayList<String>();
	private static List<String> torLocation = new ArrayList<String>();
	public FileObserver observer;

	private static class EfficientAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public EfficientAdapter(Context context) {
			mInflater = LayoutInflater.from(context);

		}

		public int getCount() {
			return torName.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview, null);
				holder = new ViewHolder();
				holder.text = (TextView) convertView
						.findViewById(R.id.TextView01);
				holder.text2 = (TextView) convertView
						.findViewById(R.id.TextView02);
				holder.text3 = (TextView) convertView
						.findViewById(R.id.TextView03);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Log.w(LOG_TAG, "TorName Postion" + torName.get(position));
			holder.text.setText(torName.get(position));
			holder.text2.setText(torNumber.get(position));
			holder.text3.setText(torEpsName.get(position));

			return convertView;
		}

		static class ViewHolder {
			TextView text;
			TextView text2;
			TextView text3;
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.torrent);
		torrents = (ListView) findViewById(R.id.ListView01);
		// By using setAdpater method in listview we an add string array in
		// list.
		refreshList();
		registerForContextMenu(torrents);
		File root = Environment.getExternalStorageDirectory();
		File gpxfile = new File(root, "msremote/torrent.gpx");
		observer = new FileObserver(gpxfile.toString()) {
			@Override
			public void onEvent(int event, String path) {
				if (event == FileObserver.MODIFY) {
					Log.e(LOG_TAG, "onevent, file done something");
					updateList();
				}
			}
		};
		observer.startWatching();

	}

	public void onResume() {
		super.onResume();
		Log.w(LOG_TAG, "Got to onStop, Torrent");
		try {
			File root = Environment.getExternalStorageDirectory();
			File gpxfile = new File(root, "msremote/torrent.gpx");
			BufferedReader in = new BufferedReader(new FileReader(gpxfile));
			try {
				Log.w(LOG_TAG, "Opened Buffer");
				int i = 0;
				String line = null;
				while ((line = in.readLine()) != null) {
					Log.w(LOG_TAG, "Read Line: " + line);
					String[] temp = (line.split("\\|"));
					String name = temp[0].replace('_', ' ');
					String SeasonInfo = temp[1];
					String EpsName = temp[2].replace('_', ' ');
					String location;
					try {
						location = temp[3];
					} catch (Exception e) {
						location = "Unknown";
					}
					if (!(torEpsName.contains(EpsName))) {
						torName.add(name);
						torNumber.add(SeasonInfo);
						torEpsName.add(EpsName);
						torLocation.add(location);
					}
					i++;
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not read file 1 " + e.getMessage());
		}
		refreshList();

	}

	public void onStop() {
		super.onStop();
		Log.w(LOG_TAG, "Got to onStop, Torrent");
		writeFile();
		observer.stopWatching();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.e(LOG_TAG, "GOT TO CONTEXT MENU");
		menu.setHeaderTitle("<3");
		menu.add(0, v.getId(), 0, "Watch");
		menu.add(0, v.getId(), 1, "Delete");
		menu.add(0, v.getId(), 3, "Remove Show");
		menu.add(0, v.getId(), 2, "Delete All");

	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		if (item.getTitle() == "Watch") {
			watch(info.id);
		} else if (item.getTitle() == "Delete") {
			delete(info.id);
		} else if (item.getTitle() == "Delete All") {
			deleteall(info.id);
		} else if (item.getTitle() == "Remove Show"){
			deleteShow(info.id);
		} else
			return false;
		return true;
	}

	public void watch(long id) {
		delete(id);
		return;
	}

	public void delete(long id) {
		torName.remove(torName.get((int) id));
		torNumber.remove(torNumber.get((int) id));
		torEpsName.remove(torEpsName.get((int) id));
		torLocation.remove(torLocation.get((int) id));
		writeFile();
		refreshList();
		return;
	}
	public void deleteShow(long id){
		String show = torName.get((int)id);
		for (int i = 0; i < torName.size();) {
			Log.v(LOG_TAG,"TorName:"+torName.get(i)+":");
			Log.v(LOG_TAG,"REMOAVE:"+show+":");
			String curShow = torName.get(i);
			if (curShow.equalsIgnoreCase(show)){
				torName.remove(i);
				torNumber.remove(i);
				torEpsName.remove(i);
				torLocation.remove(i);
			} else {
				i++;
			}
		}
		writeFile();
		refreshList();
		return;
	}

	public void deleteall(long id) {
		torName.clear();
		torNumber.clear();
		torEpsName.remove(torEpsName.get((int) id));
		torLocation.remove(torLocation.get((int) id));
		writeFile();
		refreshList();
	}

	private void updateList() {
		try {
			File root = Environment.getExternalStorageDirectory();
			File gpxfile = new File(root, "msremote/torrent.gpx");
			BufferedReader in = new BufferedReader(new FileReader(gpxfile));
			try {
				Log.w(LOG_TAG, "Opened Buffer");
				int i = 0;
				String line = null;
				while ((line = in.readLine()) != null) {
					Log.w(LOG_TAG, line);
					String[] temp = (line.split("\\|"));
					String name = temp[0].replace('_', ' ');
					String SeasonInfo = temp[1];
					String EpsName = temp[2].replace('_', ' ');
					String location;
					try {
						location = temp[3];
					} catch (Exception e) {
						location = "Unknown";
					}
					if (!(torEpsName.contains(EpsName))) {
						torName.add(name);
						torNumber.add(SeasonInfo);
						torEpsName.add(EpsName);
						torLocation.add(location);
					}
					i++;
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not read file 2" + e.getMessage());
		}
		refreshList();
	}

	private void refreshList() {
		List<String> tempArray = new ArrayList<String>();
		for (int i = 0; i < torName.size(); i++) {
			tempArray.add(torName.get(i) + "|" + torNumber.get(i) + "|"
					+ torEpsName.get(i));
		}
		Collections.sort(tempArray);
		torName.clear();
		torNumber.clear();
		torEpsName.clear();
		for (int i = 0; i < tempArray.size(); i++) {
			String[] temp = (tempArray.get(i).split("\\|"));
			String name = temp[0];
			String SeasonInfo = temp[1];
			String EpsName = temp[2].replace('_', ' ');
			String location;
			try {
				location = temp[3];
			} catch (Exception e) {
				location = "Unknown";
			}
			if (!(torEpsName.contains(EpsName))) {
				torName.add(name);
				torNumber.add(SeasonInfo);
				torEpsName.add(EpsName);
				torLocation.add(location);
			}
		}
		torrents.setAdapter(new EfficientAdapter(this));
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
	private void writeFile(){
		observer.stopWatching();
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				Log.w(LOG_TAG, "GOT HERE!");
				File gpxfile = new File(root, "msremote/torrent.gpx");
				FileWriter gpxwriter = new FileWriter(gpxfile);
				BufferedWriter out = new BufferedWriter(gpxwriter);
				for (int i = 0; i < torName.size(); i++) {
					String writeOutput = torName.get(i) + "|"
							+ torNumber.get(i) + "|" + torEpsName.get(i) + "\n";
					Log.w(LOG_TAG, "Wrote: " + writeOutput);
					out.write(writeOutput);
					Log.w(LOG_TAG, torName.get(i));
				}
				out.close();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not write file " + e.getMessage());
		}
		observer.startWatching();
	}
	private void quit() {
		Log.i(LOG_TAG, "Quitting");
		/* Unbinds the service, and closes the program */
		this.finish();
	}
}
