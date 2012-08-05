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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import com.nosideracing.rchipremote.Consts;

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
		Notifications.clearAllNotifications(getApplicationContext());
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
		switch(item.getItemId()){
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.quit:
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
			int air_time = upcoming.get(position).AirTime - (date.getTimezoneOffset()/60*100);
			Log.d(Consts.LOG_TAG,upcoming.get(position).ShowName);
			Log.d(Consts.LOG_TAG,"offset = "+date.getTimezoneOffset()/60*100);
			Log.d(Consts.LOG_TAG,"Air_time " + air_time/100);
			Log.e(Consts.LOG_TAG,"Date:"+date);
			int hours = date.getHours()+(air_time/100);
			int minutes = date.getMinutes()+(air_time%100);
			date.setHours(hours);
			date.setMinutes(minutes);
			String date_str = new SimpleDateFormat("hh:mm aa  LLL dd yyyy",
					Locale.US).format(date);
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
	public int AirTime; 

	public int compareTo(Object incomingObject) {
		if (!(incomingObject instanceof UpcomingShowInfo))
			throw new ClassCastException();
		UpcomingShowInfo incomingClass = (UpcomingShowInfo) incomingObject;
		return AirDate.compareTo(incomingClass.AirDate);
	}

}
