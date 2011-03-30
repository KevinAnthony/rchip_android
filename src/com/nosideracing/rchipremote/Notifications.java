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

	protected static void clearAllNotifications() {
		/* If currentNotificationIDs is has data */
		if (currentNotifcationIDs != null) {
			/**/
			Iterator<Integer> stepValue = currentNotifcationIDs.iterator();
			NotificationManager mNotificationManager = (NotificationManager) RemoteMain.f_context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			while (stepValue.hasNext()) {
				/*
				 * Here we clear all Notifications that are posted, and then
				 * remove them from currentNotifcation
				 */
				int id = stepValue.next();
				mNotificationManager.cancel(id);
			}
			currentNotifcationIDs.clear();
		}
		numberOfNotifications = 0;
	}

	protected static String[] setStatusNotification(String tickerString,
			String notificationTitle, String noticicationText) {
		/*
		 * We get three String Varables representing the three Strings we need
		 * to set to use Notification Manager
		 */
		try {
			String Name = "";
			String epsName = "";
			String epsNumber = "";
			try {
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
					Log.v("msremote", temp[i]);
					Name = Name + temp[i] + " ";
				}
				Name = Name.substring(0, Name.length() - 1);
				Log.v("msremote", Name);
				epsNumber = temp[temp.length - 1];
				epsName = filename[1].substring(0, filename[1].length() - 1);
				epsName = epsName.substring(0, epsName.length() - 1);
			}
			String loc = notificationTitle;
			NotificationManager mNotificationManager = (NotificationManager) RemoteMain.f_context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			/* Here we set the notification icon equal to the program icon */
			int icon = R.drawable.icon;
			/* Notification Manager uses CharSequence instead of Strings */
			String[] tickerTextTemp = tickerString.split("\\/");
			CharSequence tickerText = tickerTextTemp[tickerTextTemp.length - 1];
			CharSequence contentTitle = notificationTitle.split("\\/")[notificationTitle
					.split("\\/").length - 1];
			CharSequence contentText = noticicationText;
			/* we need to know the current time to set the notification time */
			long when = System.currentTimeMillis();
			if (currentNotifcationIDs.isEmpty()) {
				numberOfNotifications = 0;
			}
			if (numberOfNotifications > 1) {
				contentTitle = "Torrents Done";
				contentText = numberOfNotifications + " torrents done";
			}
			/*
			 * We declare notifications then the current application Context
			 */
			Notification notification = new Notification(icon, tickerText, when);

			/**/
			PendingIntent contentIntent = PendingIntent.getActivity(
					RemoteMain.f_context, 0, new Intent(RemoteMain.f_context,
							VideoList.class), 0);
			notification.setLatestEventInfo(RemoteMain.f_context, contentTitle,
					contentText, contentIntent);
			notification.defaults |= Notification.DEFAULT_ALL;
			mNotificationManager.cancel(Consts.NOTIFICATION_ID);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			mNotificationManager.notify(Consts.NOTIFICATION_ID, notification);
			currentNotifcationIDs.add(Consts.NOTIFICATION_ID);
			// NOTIFICATION_ID++;
			numberOfNotifications++;
			String[] temp = { Name, epsNumber, epsName, loc };
			return temp;
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "SetStatusNotification:" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return null;
		}
	}
}
