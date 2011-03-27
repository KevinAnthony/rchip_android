package com.nosideracing.rchipremote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CheckMessages extends BroadcastReceiver {

	Context f_context = null;
	private String URL_EXT;
	private String URL_INT;
	private String INT_NETWORK_NAME;
	private String HOSTNAME;
	/* We use this to set the ID number of the current notification */

	private int numberOfNotifications = 0;
	List<Integer> currentNotifcationIDs = new ArrayList<Integer>();
	private Database mOpenHelper;

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Context context, Intent intent) {
		f_context = context;
		updateSettings();
		SoapObject request = new SoapObject(Consts.NAMESPACE,
				Consts.METHOD_NAME_GETCMD);
		request.addProperty("host", HOSTNAME);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/*
		 * Added so that if the SOAP servers crash, we don't crash this remote
		 */
		try {
			androidHttpTransport.call(Consts.SOAP_ACTION, envelope);
		} catch (Exception e) {

			Log.e(Consts.LOG_TAG, "Check Messages: Http Transport Failed");
			Log.e(Consts.LOG_TAG, "" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return;
		}
		/*
		 * Here we pull the return results from the incoming envelope and parses
		 * it so we get the full information
		 */
		String result;
		int end;
		try {
			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			result = resultsRequestSOAP.getProperty("Result").toString();
			end = result.length() - 1;
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Check Messages:Soap Failed");
			Log.e(Consts.LOG_TAG, "" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return;
		}
		if (result.length() < 10) {
			return;
		}
		result = result.substring(8, end);
		int start = 0;
		end = 0;
		ArrayList<String> info = new ArrayList<String>();
		/*
		 * results look like {someinfo}garbage{someinfo}... So we split it up
		 * into each message, inserting it into the ArrayList for later
		 * proccessing
		 */
		while (result.length() > 0) {
			start = result.indexOf('{');
			end = result.indexOf('}');
			if (start <= end) {
				info.add(result.substring(start + 1, end));
				result = result.substring(end + 3);
			}
		}
		/*
		 * We Loop threw the ArrayList info, pulling the data Then we get the
		 * info, spliting if by ';' so that we get all the values in separate
		 * variables
		 */
		List<String[]> shows = new ArrayList<String[]>();
		for (int j = 0; j < info.size(); j++) {
			String[] infoString = info.get(j).split("; ");
			Log.i(Consts.LOG_TAG, "Info:" + info.get(j));
			@SuppressWarnings("unused")
			int id = 0;
			String cmd = "";
			String cmdTxt = "";
			@SuppressWarnings("unused")
			String timeupdated = "";
			String[] temp = infoString[0].split("=");
			cmd = temp[1];
			temp = infoString[1].split("=");
			id = Integer.parseInt(temp[1]);
			temp = infoString[2].split("=");
			cmdTxt = temp[1];
			temp = infoString[3].split("=");
			timeupdated = temp[1];
			/*
			 * TMSG is Torrent Message(the only kind of message right now Since
			 * the cmdTxt acually equals all three Status Notification Fields
			 */
			String[] curShow = new String[4];
			if (cmd.equals("TMSG")) {
				String[] cmdTemp = cmdTxt.split("\\|");
				Log.i(Consts.LOG_TAG, cmdTemp.toString());
				Log.i(Consts.LOG_TAG, Integer.toString(cmdTemp.length));
				if (cmdTemp.length == 3) {
					curShow = setStatusNotification(cmdTemp[0], cmdTemp[1],
							cmdTemp[2]);
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
		return;
	}

	protected void clearAllNotifications() {
		/* If currentNotificationIDs is has data */
		if (currentNotifcationIDs != null) {
			/**/
			Iterator<Integer> stepValue = currentNotifcationIDs.iterator();
			NotificationManager mNotificationManager = (NotificationManager) f_context
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

	protected String[] setStatusNotification(String tickerString,
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
			NotificationManager mNotificationManager = (NotificationManager) f_context
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
			PendingIntent contentIntent = PendingIntent.getActivity(f_context,
					0, new Intent(f_context, VideoList.class), 0);
			notification.setLatestEventInfo(f_context, contentTitle,
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

	private void updateSettings() {
		if (f_context == null) {
			f_context = RemoteMain.f_context;
		}
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(f_context);

		URL_EXT = settings.getString("serverurlexternal",
				"http://173.3.14.224:500/");
		URL_INT = settings.getString("serverurlinternal",
				"http://192.168.1.3:500/");
		INT_NETWORK_NAME = settings.getString("internalnetname", "Node_77");
		HOSTNAME = ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();

		URL_EXT = "http://173.3.14.224:500/";
		URL_INT = "http://192.168.1.3:500/";
		INT_NETWORK_NAME = "Node_77";

	}

	private HttpTransportSE get_transport() {
		NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		HttpTransportSE androidHttpTransport = null;
		if ((networkinfo != null) && (networkinfo.isConnected())) {
			int itype = networkinfo.getType();
			String SSID = ((WifiManager) f_context
					.getSystemService(Context.WIFI_SERVICE))
					.getConnectionInfo().getSSID();
			if ((SSID != null)
					&& ((itype == 1) && (SSID.compareTo(INT_NETWORK_NAME) == 0))) {
				androidHttpTransport = new HttpTransportSE(URL_INT);
			} else {
				androidHttpTransport = new HttpTransportSE(URL_EXT);
			}
		} else {
			androidHttpTransport = new HttpTransportSE(URL_EXT);
		}
		return androidHttpTransport;
	}

	class passDataToShowWindow extends
			AsyncTask<List<String[]>, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(List<String[]>... params) {
			List<String[]> shows = params[0];
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
