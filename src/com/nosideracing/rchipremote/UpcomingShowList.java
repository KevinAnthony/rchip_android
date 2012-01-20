package com.nosideracing.rchipremote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class UpcomingShowList extends ListActivity {
	private ArrayList<UpcomingShowInfo> upcoming;
	private ListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upcomingshows);
		upcoming = RemoteMain.json.getUpcomingShows();
		Collections.sort(upcoming);
		mAdapter = new MyListAdapter(this);
		setListAdapter(mAdapter);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
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
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void quit() {
		setResult(Consts.QUITREMOTE);
		this.finish();
	}

	public class MyListAdapter extends BaseAdapter {

		private Context fContext;
		private LayoutInflater mInflater;

		public MyListAdapter(Context context) {
			fContext = context;
			mInflater = LayoutInflater.from(fContext);
			Log.v(Consts.LOG_TAG, "Got Here");
		}

		public int getCount() {
			return upcoming.size();
		}

		public Object getItem(int position) {
			return upcoming.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder info;
			Log.v(Consts.LOG_TAG, "Got Here " + position);
			convertView = mInflater.inflate(R.layout.upcomingshows_listview,
					null);
			info = new ViewHolder();
			info.ShowNameView = (TextView) convertView
					.findViewById(R.id.show_name);
			info.EpisodeNameView = (TextView) convertView
					.findViewById(R.id.episode_name);
			info.EpisodeNumberView = (TextView) convertView
					.findViewById(R.id.episode_number);
			info.AirDateView = (TextView) convertView
					.findViewById(R.id.show_date);
			convertView.setTag(info);

			info.ShowNameView.setText(upcoming.get(position).ShowName);
			info.EpisodeNameView.setText(upcoming.get(position).EpisodeName);
			info.EpisodeNumberView
					.setText(upcoming.get(position).EpisodeNumber);
			Date date = upcoming.get(position).AirDate;
			String date_str = new SimpleDateFormat("hh:mm aa  LLL dd yyyy", Locale.US).format(date);
			info.AirDateView.setText(date_str);
			return convertView;

		}

		class ViewHolder {
			TextView ShowNameView;
			TextView EpisodeNameView;
			TextView EpisodeNumberView;
			TextView AirDateView;
		}

	}
}

class UpcomingShowInfo implements Comparable<Object> {
	public String ShowName;
	public String EpisodeName;
	public String EpisodeNumber;
	public Date AirDate;

	public int compareTo(Object incomingObject) {
		if (!(incomingObject instanceof UpcomingShowInfo))
			throw new ClassCastException();
		UpcomingShowInfo incomingClass = (UpcomingShowInfo) incomingObject;
		return AirDate.compareTo(incomingClass.AirDate);
	}

}
