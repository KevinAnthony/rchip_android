package com.nosideracing.msremote;

import java.util.ArrayList;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

	private static List<Integer> torID = new ArrayList<Integer>();
	private static List<String> torName = new ArrayList<String>();
	private static List<String> torNumber = new ArrayList<String>();
	private static List<String> torEpsName = new ArrayList<String>();
	private static List<String> torLocation = new ArrayList<String>();
	private static List<String> group = new ArrayList<String>();
	ExpandableListAdapter mAdapter;
	MS_database msdb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up our adapter
		msdb = new MS_database(getBaseContext());
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
		registerForContextMenu(getExpandableListView());
		try {
			MS_remote.backendService.clearNotifications();
		} catch (Exception e) {
			Log
					.e(
							MS_constants.LOG_TAG,
							"Error Clearing Notifications(We get this on startup sometimes, because the service hasn't get been started):"
									+ e.getLocalizedMessage());
		}

	}

	public void onResume() {
		super.onResume();
		Log.d(MS_constants.LOG_TAG, "onResume: MS_show_list");
		refreshList();
	}

	public void onPause() {
		super.onPause();
		Log.d(MS_constants.LOG_TAG, "onPause: MS_show_list");
	}

	public boolean onChildClick(ExpandableListView info, View v, int groupPos,
			int childPos, long id) {
		Log.d(MS_constants.LOG_TAG, "Got to OnChildClick");
		int Listid = ((MyExpandableListAdapter) mAdapter).getlongID(groupPos,
				childPos);
		watch(Listid);
		return true;
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

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu_show_list, menu);
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
		} else if (resultCode == MS_constants.REMOVESHOW) {
			long id = data.getLongExtra("showID", -1L);
			Log.d(MS_constants.LOG_TAG, "Returned REMOVESHOW");
			Log.d(MS_constants.LOG_TAG, "Deleting ID :" + id);
			delete(id);
		}
	}

	private void watch(long id) {
		Intent i = new Intent(this, MS_watch_movie.class);
		i.putExtra("showString", torName.get((int) id) + " - "
				+ torNumber.get((int) id) + " - " + torEpsName.get((int) id));
		i.putExtra("Location", torLocation.get((int) id));
		i.putExtra("showID", id);
		MS_show_list.this.startActivityForResult(i, MS_constants.RC_WATCHMOVE);
		return;
	}

	private void delete(long id) {
		msdb.deleteOneSL(torID.get((int) id));
		refreshList();
		return;
	}

	private void deleteShow(long id) {
		String show = torName.get((int) id);
		new DeleteShow().execute(show);
		refreshList();
		return;
	}

	private void deleteall(long id) {
		msdb.deleteAllSL();
		refreshList();
	}

	private void refreshList() {
		torID.clear();
		torName.clear();
		torNumber.clear();
		torEpsName.clear();
		torLocation.clear();
		group.clear();
		String[] showArray = msdb.getShows();
		if ((showArray != null) && (showArray.length > 0)) {
			for (int i = 0; i < showArray.length; i++) {
				String[] temp = (showArray[i].split("\\|"));
				int id = Integer.parseInt(temp[0]);
				String name = temp[1];
				String SeasonInfo = temp[2];
				String EpsName = temp[3].replace('_', ' ');
				String location = temp[4];
				if (!((torEpsName.contains(EpsName)) && (torNumber
						.contains(SeasonInfo)))) {
					torID.add(id);
					torName.add(name);
					torNumber.add(SeasonInfo);
					torEpsName.add(EpsName);
					torLocation.add(location);
					if (!(group.contains(name))) {
						group.add(name);
					}
				} else {
					msdb.deleteOneSL(id);
				}
			}
		}
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
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
				convertView.setClickable(true);
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
	class DeleteShow extends AsyncTask<String,Integer,Boolean> {

	@Override
	protected Boolean doInBackground(String... params) {
		for (int i = 0; i < torName.size();) {
			String curShow = torName.get(i);
			if (curShow.equalsIgnoreCase(params[0])) {
				delete(i);
			} else {
				i++;
			}
		}
		return true;
	}
	}
}
