package com.nosideracing.rchipremote;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class CheckMessages extends BroadcastReceiver {

	Context f_context = null;

	/* We use this to set the ID number of the current notification */

	@Override
	@SuppressWarnings("unchecked")
	public void onReceive(Context context, Intent intent) {
		f_context = context;
		JSON jSon = new JSON(f_context);
		List<String[]> shows = new ArrayList<String[]>();
		try {
			JSONArray jsonArray = jSon.getCommands("7327943473");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String cmd = (String) jsonObject.get("command");
				String cmdTxt = (String) jsonObject.get("command_text");
				/*
				 * TMSG is Torrent Message(the only kind of message right now
				 * Since the cmdTxt acually equals all three Status Notification
				 * Fields
				 */
				String[] curShow = new String[4];
				if (cmd.equals("TMSG")) {
					String[] cmdTemp = cmdTxt.split("\\|");
					Log.i(Consts.LOG_TAG, cmdTemp.toString());
					Log.i(Consts.LOG_TAG, Integer.toString(cmdTemp.length));
					if (cmdTemp.length == 3) {
						curShow = Notifications.setStatusNotification(
								cmdTemp[0], cmdTemp[1], cmdTemp[2], context);
						Log.d(Consts.LOG_TAG, "curShow:" + curShow);
					}
				} else if (cmd.equals("ADDS")) {
					String[] cmdTemp = cmdTxt.split("\\|");
					Log.v(Consts.LOG_TAG, cmdTxt);
					if (cmdTemp.length == 4) {
						curShow[0] = cmdTemp[0];
						curShow[1] = cmdTemp[1];
						curShow[2] = cmdTemp[2];
						curShow[3] = cmdTemp[3];
					}
				} else {
					Log.w(Consts.LOG_TAG, "Commad:" + cmd + " Not supported");
				}
				if (curShow != null) {
					shows.add(curShow);
				}

			}
			new passDataToShowWindow().execute(shows);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Problem With Check Message", e);
		}
		return;
	}

	class passDataToShowWindow extends
			AsyncTask<List<String[]>, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(List<String[]>... params) {
			List<String[]> shows = params[0];
			Database mOpenHelper = new Database(f_context);
			try {
				for (int i = 0; i < shows.size(); i++) {
					String[] curShow = shows.get(i);
					mOpenHelper.insertShow(curShow[0], curShow[2], curShow[1],
							curShow[3]);
				}
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "Could not insert " + e.getMessage());
				Log.e(Consts.LOG_TAG, "", e);
			}
			return true;
		}
	}
}
