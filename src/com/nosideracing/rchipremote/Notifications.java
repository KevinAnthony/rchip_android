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
import java.util.Iterator;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Notifications {
	private static int numberOfNotifications = 0;
	static List<Integer> currentNotifcationIDs = new ArrayList<Integer>();

	protected static void clearAllNotifications(Context context) {
		if (currentNotifcationIDs != null) {
			Iterator<Integer> stepValue = currentNotifcationIDs.iterator();
			NotificationManager mNotificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			while (stepValue.hasNext()) {
				int id = stepValue.next();
				mNotificationManager.cancel(id);
			}
			currentNotifcationIDs.clear();
		}
		numberOfNotifications = 0;
	}

	protected static String[] setStatusNotification(String tickerString,
			String notificationTitle, String noticicationText, Context context) {
		try {
			String Name = "";
			String epsName = "";
			String epsNumber = "";
			try {
				Log.v(Consts.LOG_TAG, "Ticker String:" + tickerString);
				Log.v(Consts.LOG_TAG, "Not Title:" + notificationTitle);
				Log.v(Consts.LOG_TAG, "Not Text:" + noticicationText);
				String[] filename = notificationTitle.split("\\/")[notificationTitle
						.split("\\/").length - 1].split("\\.");
				Name = filename[0].replace("_", " ");
				epsName = filename[2].replace("_", " ");
				epsNumber = filename[1];
			} catch (Exception e) {
				String[] filename = notificationTitle.split("\\/")[notificationTitle
						.split("\\/").length - 1].split("\\[");
				String[] temp = filename[0].split("_");
				Name = "";
				epsName = "";
				epsNumber = "";
				for (int i = 0; i < temp.length - 1; i++) {
					Name = Name + temp[i] + " ";
				}
				Name = Name.substring(0, Name.length() - 1);
				epsNumber = temp[temp.length - 1];
				epsName = filename[1].substring(0, filename[1].length() - 1);
				epsName = epsName.substring(0, epsName.length() - 1);
			}
			String loc = notificationTitle;
			NotificationManager mNotificationManager;
			if (context != null) {
				mNotificationManager = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
			} else {
				String[] temp = { Name, epsNumber, epsName, loc };
				return temp;
			}
			
			int icon = R.drawable.notification_icon;
			String[] tickerTextTemp = tickerString.split("\\/");
			CharSequence tickerText = tickerTextTemp[tickerTextTemp.length - 1];
			CharSequence contentTitle = notificationTitle.split("\\/")[notificationTitle
					.split("\\/").length - 1];
			CharSequence contentText = noticicationText;
			long when = System.currentTimeMillis();
			if (currentNotifcationIDs.isEmpty()) {
				numberOfNotifications = 0;
			}
			if (numberOfNotifications > 1) {
				contentTitle = "Torrents Done";
				contentText = numberOfNotifications + " torrents done";
			}
			Notification notification = new Notification(icon, tickerText, when);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
					new Intent(context, VideoList.class), 0);
			notification.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);
			notification.defaults |= Notification.DEFAULT_ALL;
			mNotificationManager.cancel(Consts.NOTIFICATION_ID);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			mNotificationManager.notify(Consts.NOTIFICATION_ID, notification);
			currentNotifcationIDs.add(Consts.NOTIFICATION_ID);
			numberOfNotifications++;
			String[] temp = { Name, epsNumber, epsName, loc };
			return temp;
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "SetStatusNotification:", e);
			return null;
		}
	}
}
