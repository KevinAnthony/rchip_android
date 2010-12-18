package com.nosideracing.msremote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class MS_show_list extends ExpandableListActivity {

	public static FileObserver observer;

	private backendservice backendService;
	// private BackendServiceConnection conn;

	private static List<String> torName = new ArrayList<String>();
	private static List<String> torNumber = new ArrayList<String>();
	private static List<String> torEpsName = new ArrayList<String>();
	private static List<String> torLocation = new ArrayList<String>();
	private static List<String> group = new ArrayList<String>();
	ExpandableListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		readFile();
		// Set up our adapter
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
		registerForContextMenu(getExpandableListView());

		File root = Environment.getExternalStorageDirectory();
		File gpxfile = new File(root, "msremote/torrent.gpx");
		observer = new FileObserver(gpxfile.toString()) {

			@Override
			public void onEvent(int event, String path) {
				if (event == FileObserver.CLOSE_WRITE) {
					Log.v(MS_constants.LOG_TAG, "onevent, file done something");
					updateList();
				}
			}
		};

		try {
			backendService.clearNotifications();
		} catch (Exception e) {
			Log
					.e(
							MS_constants.LOG_TAG,
							"Error Clearing Notifications(We get this on startup sometimes, because the service hasn't get been started):"
									+ e.getLocalizedMessage());
		}
		observer.startWatching();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.v(MS_constants.LOG_TAG, "GOT TO CONTEXT MENU");
		menu.setHeaderTitle("<3");
		menu.add(0, v.getId(), 0, "Watch");
		menu.add(0, v.getId(), 1, "Delete");
		menu.add(0, v.getId(), 3, "Remove Show");
		menu.add(0, v.getId(), 2, "Delete All");

	}

	public boolean onContextItemSelected(MenuItem item) {
		int id = 0;
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		int type = ExpandableListView
				.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int groupPos = ExpandableListView
					.getPackedPositionGroup(info.packedPosition);
			int childPos = ExpandableListView
					.getPackedPositionChild(info.packedPosition);
			id = ((MyExpandableListAdapter) mAdapter).getlongID(groupPos,
					childPos);
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			Toast.makeText(this, "Please Select a Valid Show",
					Toast.LENGTH_SHORT).show();
			return true;
		}

		if (item.getTitle() == "Watch") {
			watch(id);
		} else if (item.getTitle() == "Delete") {
			delete(id);
		} else if (item.getTitle() == "Delete All") {
			deleteall(id);
		} else if (item.getTitle() == "Remove Show") {
			deleteShow(id);
		} else
			return false;
		return true;

	}

	public void watch(long id) {
		Intent i1 = new Intent(this, MS_watch_movie.class);
		i1.putExtra("ShowName", torName.get((int) id));
		i1.putExtra("EpsNumber", torNumber.get((int) id));
		i1.putExtra("EpsName", torEpsName.get((int) id));
		i1.putExtra("Location", torLocation.get((int) id));
		MS_show_list.this.startActivityForResult(i1, MS_constants.RC_WATCHMOVE);
		delete(id);
		return;
	}

	public void delete(long id) {
		Log.i(MS_constants.LOG_TAG, "Got to Delete with Id " + id);
		torName.remove(torName.get((int) id));
		torNumber.remove(torNumber.get((int) id));
		torEpsName.remove(torEpsName.get((int) id));
		torLocation.remove(torLocation.get((int) id));
		writeFile();
		refreshList();
		return;
	}

	public void deleteShow(long id) {
		String show = torName.get((int) id);
		for (int i = 0; i < torName.size();) {
			String curShow = torName.get(i);
			if (curShow.equalsIgnoreCase(show)) {
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// See which child activity is calling us back.
		if (resultCode == MS_constants.QUITREMOTE) {
			quit();
		}
	}

	private void updateList() {
		readFile();
		refreshList();
	}

	private void refreshList() {
		List<String> tempArray = new ArrayList<String>();
		for (int i = 0; i < torName.size(); i++) {
			tempArray.add(torName.get(i) + "|" + torNumber.get(i) + "|"
					+ torEpsName.get(i) + "|" + torLocation.get(i));
		}
		Collections.sort(tempArray);
		torName.clear();
		torNumber.clear();
		torEpsName.clear();
		torLocation.clear();
		group.clear();
		for (int i = 0; i < tempArray.size(); i++) {
			String[] temp = (tempArray.get(i).split("\\|"));
			String name = temp[0];
			String SeasonInfo = temp[1];
			String EpsName = temp[2].replace('_', ' ');
			String location = temp[3];
			if (!((torEpsName.contains(EpsName)) && (torNumber
					.contains(SeasonInfo)))) {
				torName.add(name);
				torNumber.add(SeasonInfo);
				torEpsName.add(EpsName);
				torLocation.add(location);
				if (!(group.contains(name))) {
					group.add(name);
				}
			}
		}
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
	}

	private void writeFile() {
		observer.stopWatching();
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File gpxfile = new File(root, "msremote/torrent.gpx");
				FileWriter gpxwriter = new FileWriter(gpxfile);
				BufferedWriter out = new BufferedWriter(gpxwriter);
				for (int i = 0; i < torName.size(); i++) {
					String writeOutput = torName.get(i) + "|"
							+ torNumber.get(i) + "|" + torEpsName.get(i) + "|"
							+ torLocation.get(i) + "\n";
					out.write(writeOutput);
				}
				out.close();
			}
		} catch (Exception e) {
			Log.e(MS_constants.LOG_TAG, "Could not write file "
					+ e.getMessage());
		}
		observer.startWatching();
	}

	private void readFile() {
		try {
			File root = Environment.getExternalStorageDirectory();
			File gpxfile = new File(root, "msremote/torrent.gpx");
			BufferedReader in = new BufferedReader(new FileReader(gpxfile));
			try {
				Log.w(MS_constants.LOG_TAG, "Opened Buffer");
				int i = 0;
				String line = null;
				while ((line = in.readLine()) != null) {
					String[] temp = (line.split("\\|"));
					String name = temp[0].replace('_', ' ');
					String SeasonInfo = temp[1];
					String EpsName = temp[2].replace('_', ' ');
					String location = temp[3];
					if (!((torEpsName.contains(EpsName)) && (torNumber
							.contains(SeasonInfo)))) {
						torName.add(name);
						torNumber.add(SeasonInfo);
						torEpsName.add(EpsName);
						torLocation.add(location);
					} else {
						Log.w(MS_constants.LOG_TAG, name + " " + SeasonInfo
								+ " " + EpsName);
					}
					if (!(group.contains(name))) {
						group.add(name);
					}
					i++;
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			Log.e(MS_constants.LOG_TAG, "Could not read file 1 "
					+ e.getMessage());
		}
		refreshList();
	}

	private void quit() {
		Log.i(MS_constants.LOG_TAG, "Quitting");
		setResult(MS_constants.QUITREMOTE);
		this.finish();
	}

	public class MyExpandableListAdapter extends BaseExpandableListAdapter {

		private ArrayList<String> groups = new ArrayList<String>();
		private ArrayList<ArrayList<ArrayList<String>>> children = new ArrayList<ArrayList<ArrayList<String>>>();
		private LayoutInflater mInflater;

		public MyExpandableListAdapter(Context context) {
			groups.clear();
			mInflater = LayoutInflater.from(context);
			for (int i = 0; i < group.size(); i++) {
				groups.add(group.get(i));
			}
			int k = 0;
			for (int i = 0; i < group.size(); i++) {
				ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
				while ((k < torName.size())
						&& (torName.get(k).equals(groups.get(i)))) {
					ArrayList<String> temp2 = new ArrayList<String>();
					temp2.add(torName.get(k));
					temp2.add(torNumber.get(k));
					temp2.add(torEpsName.get(k));
					temp2.add(torLocation.get(k));
					temp.add(temp2);
					k++;
				}
				children.add(temp);
			}
		}

		public Object getChild(int groupPosition, int childPosition) {
			return children.get(groupPosition).get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public int getChildrenCount(int groupPosition) {
			return children.get(groupPosition).size();
		}

		public TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 64);

			TextView textView = new TextView(MS_show_list.this);
			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(86, 0, 0, 0);
			return textView;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

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
			holder.text.setText(children.get(groupPosition).get(childPosition)
					.get(0));
			holder.text2.setText(children.get(groupPosition).get(childPosition)
					.get(1));
			holder.text3.setText(children.get(groupPosition).get(childPosition)
					.get(2));

			return convertView;
		}

		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		public int getGroupCount() {
			return groups.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText(getGroup(groupPosition).toString());
			return textView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
		}

		public int getlongID(int groupPosition, int childPosition) {
			int retval = 0;
			for (int i = 0; i < groupPosition; i++) {
				retval += children.get(i).size();
			}
			retval += childPosition;

			return retval;
		}

		class ViewHolder {
			TextView text;
			TextView text2;
			TextView text3;
		}
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
}