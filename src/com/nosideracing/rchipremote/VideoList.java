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

import java.util.ArrayList;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

public class VideoList extends ExpandableListActivity {

	private static List<Integer> torID = new ArrayList<Integer>();
	private static List<String> torName = new ArrayList<String>();
	private static List<String> torNumber = new ArrayList<String>();
	private static List<String> torEpsName = new ArrayList<String>();
	private static List<String> torLocation = new ArrayList<String>();
	private static List<String> group = new ArrayList<String>();
	ExpandableListAdapter mAdapter;
	Database rchipDB;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		rchipDB = new Database(getBaseContext());
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
		registerForContextMenu(getExpandableListView());
		try {
			RemoteMain.json.clearNotifications(getApplicationContext());
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG,
					"Error Clearing Notifications(We get this on startup sometimes, because the service hasn't get been started):",
							e);
		}
		Notifications.clearAllNotifications(getApplicationContext());
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshList();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public boolean onChildClick(ExpandableListView info, View v, int groupPos,
			int childPos, long id) {
		int Listid = ((MyExpandableListAdapter) mAdapter).getlongID(groupPos,
				childPos);
		watch(Listid);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Show");
		menu.add(0, v.getId(), 0, getString(R.string.watch));
		menu.add(0, v.getId(), 1, getString(R.string.delete));
		menu.add(0, v.getId(), 3, getString(R.string.remove_show));
		menu.add(0, v.getId(), 2, getString(R.string.delete_all));

	}

	@Override
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
			Toast.makeText(this, getString(R.string.select_valid_show),
					Toast.LENGTH_SHORT).show();
			return true;
		}

		if (item.getOrder() == 0) {
			watch(id);
		} else if (item.getOrder() == 1) {
			delete(id);
		} else if (item.getOrder() == 2) {
			deleteall(id);
		} else if (item.getOrder() == 3) {
			deleteShow(id);
		} else
			return false;
		return true;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu_show_list, menu);
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
		} else if (resultCode == Consts.REMOVESHOW) {
			long id = data.getLongExtra("showID", -1L);
			delete(id);
		}
	}

	private void watch(long id) {
		Intent i = new Intent(this, VideoRemote.class);
		i.putExtra("showString",
				torName.get((int) id) + " - " + torNumber.get((int) id) + " - "
						+ torEpsName.get((int) id));
		i.putExtra("Location", torLocation.get((int) id));
		i.putExtra("showID", id);
		VideoList.this.startActivityForResult(i, Consts.RC_WATCHMOVE);
		return;
	}

	private void delete(long id) {
		rchipDB.deleteOneSL(torID.get((int) id));
		refreshList();
		return;
	}

	private void deleteWithoutRefresh(long id) {
		rchipDB.deleteOneSL(torID.get((int) id));
		return;
	}

	private void deleteShow(long id) {
		String show = torName.get((int) id);
		new DeleteShow().execute(show);
		return;
	}

	private void deleteall(long id) {
		rchipDB.deleteAllSL();
	}

	private void refreshList() {
		torID.clear();
		torName.clear();
		torNumber.clear();
		torEpsName.clear();
		torLocation.clear();
		group.clear();
		String[] showArray = rchipDB.getShows();
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
					rchipDB.deleteOneSL(id);
				}
			}
		}
		mAdapter = new MyExpandableListAdapter(this);
		setListAdapter(mAdapter);
	}

	private void quit() {
		setResult(Consts.QUITREMOTE);
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
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 64);
			TextView textView = new TextView(VideoList.this);
			textView.setLayoutParams(lp);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			textView.setPadding(86, 0, 0, 0);
			return textView;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {

			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.playlist_listview, null);
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

	class DeleteShow extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			for (int i = 0; i < torName.size();) {
				String curShow = torName.get(i);
				if (curShow.equalsIgnoreCase(params[0])) {
					deleteWithoutRefresh(i);
					i++;
				} else {
					i++;
				}
			}
			refreshList();
			return true;
		}
	}
}
