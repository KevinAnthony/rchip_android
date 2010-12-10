package com.nosideracing.msremote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import android.content.Context;
import android.content.Intent;

public class msservice extends Service {

	private Handler serviceHandler = null;
	/* primary song info, Tag and Info */
	public Hashtable<String, String> songinfo = new Hashtable<String, String>();
	/* We use static Strings here so if it changes, we can change it up here */
	private static final String SOAP_ACTION = "getSongInfo";
	private static final String METHOD_NAME_GETINFO = "getSongInfo";
	private static final String METHOD_NAME_SENDCMD = "sendCmd";
	private static final String METHOD_NAME_GETCMD = "getCmd";
	private static final String METHOD_NAME_REGISTERACTIVEDEVICE = "registerActiveDevice";
	private static final String METHOD_NAME_REGISTERMESSAGES = "registerMessages";
	private static final String NAMESPACE = "http://192.168.1.3/";
	private String LOG_TAG = "msremote";
	/* These are global so that we can reference them from many a method */
	private String URL_EXT;
	private String URL_INT;
	private String INT_NETWORK_NAME;
	private String HOSTNAME;
	private String DESTHOSTNAME;
	private int EXT_DELAY;
	private int INT_DELAY;

	/* We use this to set the ID number of the current notification */
	private static final int NOTIFICATION_ID = 0x0081;
	private int numberOfNotifications = 0;
	/* Are we updating? */
	private boolean update = false;
	/* Are we running? */
	private boolean running = false;
	List<Integer> currentNotifcationIDs = new ArrayList<Integer>();

	@Override
	public int onStartCommand(Intent intent, int startFlags, int startId) {
		super.onStart(intent, startId);
		/*
		 * If running is false (we are not running) start a new thread
		 */
		if (!running) {
			serviceHandler = new Handler();
			serviceHandler.postDelayed(new RunTask(), 1000L);
			running = true;
		} else {
			return START_NOT_STICKY;
		}
		/*
		 * Unpack Settings URL is the url of SOAP server Hostname is our
		 * Identifier, in this case, our phonenumber Desthostname is the
		 * Destination identifier that the daemon is running (we can have
		 * multiple destinations on one SOAP server LOG_TAG is just that
		 */
		Bundle incoming = intent.getExtras();
		URL_EXT = incoming.get("SETTING_URL_EXTERNAL").toString();
		// URL = "http://173.3.14.224:500";
		URL_INT = incoming.get("SETTING_URL_INTERNAL").toString();
		HOSTNAME = incoming.get("SETTING_SOURCENAME").toString();
		DESTHOSTNAME = incoming.get("SETTING_DESTNAME").toString();
		INT_NETWORK_NAME = incoming.get("SETTING_INTERNAL_NETWORK_NAME")
				.toString();
		LOG_TAG = incoming.get("SETTING_LOG_TAG").toString();
		EXT_DELAY = incoming.getInt("SETTING_EXTERNAL_DELAY");
		INT_DELAY = incoming.getInt("SETTING_INTERNAL_DELAY");
		boolean ktornot = incoming.getBoolean("SETTING_KTORRENTNOTIFICATION");
		set_ktorrent_notifications(ktornot);
		/* Initialze SongInfo to default values */
		songinfo.put("artist", "Nobody");
		songinfo.put("album", "Nothing");
		songinfo.put("title", "Nothing Playing");
		songinfo.put("etime", "0");
		songinfo.put("tottime", "9999");
		Log.i(LOG_TAG, "Soap Messaging Service Started");
		/* Start Streaming Rhythmbox and Start getting messages from kTorrent */
		registerDevice(true);
		/*
		 * Prime the Song Info Field NOTE: this is usually called from msremote
		 * NOT msmusic, this way, when msmusic calls it the above default values
		 * shouldn't be seen, although the song info/name can be drastically
		 * wrong
		 */
		getSongInfo();
		return START_STICKY;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		/*
		 * ohh now that we are rebound, start streaming again (since we stop on
		 * unbind)
		 */
		registerDevice(true);
		/* Reprime and relaunch the update thread */
		getSongInfo();
		if (!running) {
			serviceHandler = new Handler();
			serviceHandler.postDelayed(new RunTask(), 1000L);
			running = true;
		} else {
			/*
			 * The thread should not die on unbind, so you should see this
			 * message
			 */
			Log.v(LOG_TAG, "Already running (you should see this message");
		}
		Log.d(LOG_TAG, "onRebind: msserivce");

	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "OnBind: msserivce");
		return interfaceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(LOG_TAG, "onUnbind: msservice");
		/*
		 * whence we unbind, we disconnect from Rhythmbox Streaming NOTE: we
		 * don't ask KTorrent to stop, we only do that on destroy
		 */
		registerDevice(false);
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "onCreate: msservice");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy: msservice");
	}

	/* These are when you call backend.method, these are the methods */
	private final backendservice.Stub interfaceBinder = new backendservice.Stub() {
		/* We send the command to soap */
		public boolean sendCmd(String cmd, String cmdText) {
			return sendCmdToSoap(cmd, cmdText);
		}

		/* We set a notificaion, */
		public boolean setNotification(String tickerString,
				String notificationTitle, String noticicationText) {
			return setStatusNotification(tickerString, notificationTitle,
					noticicationText);
		}

		public void clearNotifications() {
			clearAllNotifications();
		}

		/* We start calling SOAP update every X seconds */
		public void startMusicUpdating() {
			update = true;
		}

		/* We stop Updating every X seconds */
		public void stopMusicUpdating() {
			update = false;
		}

		/* The following only return a value from SongInfo */
		public String getArtest() {
			String text = songinfo.get("artist").toString();
			return text;
		}

		public String getAlbum() {
			return songinfo.get("album").toString();
		}

		public String getSongName() {
			return songinfo.get("title").toString();
		}

		public String getTimeElapised() {
			return songinfo.get("etime").toString();
		}

		public String getSongLength() {
			return songinfo.get("tottime").toString();
		}

		public int getIsPlaying() {
			return Integer.parseInt(songinfo.get("isPlaying"));
		}

		public boolean setKtorrentNotifications(boolean ktornot) {
			return set_ktorrent_notifications(ktornot);
		}
	};

	private boolean set_ktorrent_notifications(boolean ktornot) {
		try {
			boolean result = false;
			if (ktornot) {
				result = registerForMessages("TRUE");
			} else {
				result = registerForMessages("FALSE");
			}
			return result;
		} catch (Exception e) {
			Log.e(LOG_TAG, "set_ktorrent_notifications:Something Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
	}

	private boolean getSongInfo() {
		/*
		 * We encase the entire fucntion in a try/catch so that if the soap
		 * fails or we get illegal chaircters it does not kill the main program
		 */
		try {
			/* Create a SOAP package using host name */
			SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_GETINFO);
			request.addProperty("host", HOSTNAME);
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			HttpTransportSE androidHttpTransport = get_transport();
			androidHttpTransport.call(SOAP_ACTION, envelope);
			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			/* We get the result */
			String result = resultsRequestSOAP.getProperty("Result").toString();
			/* We get the length 1-end so we subtrack one so it's 0-end */
			int end = result.length() - 1;
			/* First 7 chars are garbage */
			result = result.substring(8, end);
			/* Split it via the ';' */
			Log.i(LOG_TAG, "Result:" + result);
			String[] info = result.split("; ");
			for (int i = 0; i < info.length - 1; i++) {
				/*
				 * Split the key,value pair, and assuming value is a good value,
				 * stick it in SongInfo
				 */
				String[] temp = info[i].split("=");
				String key = temp[0];
				String value = temp[1];
				if ((value == null) || (value == "")) {
					value = " ";
				}
				songinfo.put(key, value);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "SongInfo:Something Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
		return true;
	}

	private boolean registerForMessages(String active) {
		/*
		 * Creating the SOAP envelope using the registerMessage SOAP command
		 */
		SoapObject request = new SoapObject(NAMESPACE,
				METHOD_NAME_REGISTERMESSAGES);
		request.addProperty("shost", HOSTNAME);
		request.addProperty("dhost", DESTHOSTNAME);
		request.addProperty("active", active);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
			return true;
		} catch (Exception e) {

			Log.e(LOG_TAG, "registerForMessages: Http Transport Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
	}

	private boolean registerDevice(boolean state) {
		/*
		 * Creating the SOAP envelope using the registerMessage SOAP command
		 */
		SoapObject request = new SoapObject(NAMESPACE,
				METHOD_NAME_REGISTERACTIVEDEVICE);
		request.addProperty("host", HOSTNAME);
		request.addProperty("onoff", state);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
			return true;
		} catch (Exception e) {

			Log.e(LOG_TAG, "registerDevice: Http Transport Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
	}

	private boolean sendCmdToSoap(String cmd, String cmdTxt) {
		/*
		 * This function send a command to the SOAP method sendCmd, which passes
		 * a command into the database defaulting to default destination
		 */
		/* Again sets up the SOAP envelope */
		if (cmd == "STRB") {
			Log.e(LOG_TAG, "STRB DEPRECIATED");
			return registerDevice(true);
		} else if (cmd == "SPRB") {
			Log.e(LOG_TAG, "SPRB DEPRECIATED");
			return registerDevice(false);
		}
		String destination = DESTHOSTNAME;
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_SENDCMD);
		request.addProperty("cmd", cmd);
		request.addProperty("txt", cmdTxt);
		request.addProperty("shost", HOSTNAME);
		request.addProperty("dhost", destination);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
		} catch (Exception e) {

			Log.e(LOG_TAG, "sendCmdToSoap: Http Transport Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	/* because we never use it. saved for later use */
	private boolean sendCmdToSoap(String cmd, String cmdTxt, String destination) {
		/*
		 * This function send a command to the SOAP method sendCmd, which passes
		 * a command into the database defaulting to default destination
		 */

		/* Again sets up the SOAP envelope */
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_SENDCMD);
		request.addProperty("cmd", cmd);
		request.addProperty("txt", cmdTxt);
		request.addProperty("shost", HOSTNAME);
		request.addProperty("dhost", destination);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
		} catch (Exception e) {

			Log.e(LOG_TAG, "sendCmdToSoap: Http Transport Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
		return true;
	}

	private HttpTransportSE get_transport() {
		Context f_context = getApplicationContext();
		NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		HttpTransportSE androidHttpTransport = null;
		if ((networkinfo != null) && (networkinfo.isConnected())) {
			int itype = networkinfo.getType();
			if (itype == 1) {
				androidHttpTransport = new HttpTransportSE(URL_INT);
			} else {
				androidHttpTransport = new HttpTransportSE(URL_EXT);
			}
		} else {
			androidHttpTransport = new HttpTransportSE(URL_EXT);
		}
		return androidHttpTransport;
	}

	private boolean setStatusNotification(String tickerString,
			String notificationTitle, String noticicationText) {
		/*
		 * We get three String Varables representing the three Strings we need
		 * to set to use Notification Manager
		 */
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		/* Here we set the notification icon equal to the program icon */
		int icon = R.drawable.icon;
		/* Notification Manager uses CharSequence instead of Strings */
		String[] tickerTextTemp = tickerString.split("\\/");
		CharSequence tickerText = tickerTextTemp[tickerTextTemp.length - 1];
		CharSequence contentTitle = notificationTitle.split("\\/")[notificationTitle
				.split("\\/").length - 1];
		CharSequence contentText = noticicationText;
		String t1 = (String) contentTitle;
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
		Context context = getApplicationContext();

		/**/
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(this, mstorrent.class), 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.defaults |= Notification.DEFAULT_ALL;
		mNotificationManager.cancel(NOTIFICATION_ID);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		mNotificationManager.notify(NOTIFICATION_ID, notification);
		currentNotifcationIDs.add(NOTIFICATION_ID);
		// NOTIFICATION_ID++;
		numberOfNotifications++;
		String name = "ERROR";
		String SeasonInfo = "ERROR";
		String EpsName = "ERROR";
		try {
			String[] temp = (t1.split("\\."));
			name = temp[0].replace('_', ' ');
			SeasonInfo = temp[1];
			EpsName = temp[2].replace('_', ' ');
		} catch (Exception e) {
			Log.e(LOG_TAG, "Spliting t1: " + t1);
			Log.e(LOG_TAG, "Message" + e.getMessage());
		}
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File gpxfile = new File(root, "msremote/torrent.gpx");
				FileWriter gpxwriter = new FileWriter(gpxfile, true);
				BufferedWriter out = new BufferedWriter(gpxwriter);
				// String[] tempName=name.split("\\/");
				// name = tempName[tempName.length-1];
				String outputString = name + "|" + SeasonInfo + "|" + EpsName
						+ "|" + contentText;
				Log.e(LOG_TAG, "String:" + outputString);
				out.write(outputString + "\n");
				out.close();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not write file " + e.getMessage());
		}
		return true;
	}

	@SuppressWarnings("unused")
	private boolean checkMessages() {
		Log.i(LOG_TAG, "checking Messages");
		/*
		 * This function send a command to the SOAP method sendCmd, which checks
		 * for messages sitting on the SOAP server's Database
		 */
		SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_GETCMD);
		request.addProperty("host", HOSTNAME);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(SOAP_ACTION, envelope);
		} catch (Exception e) {

			Log.e(LOG_TAG, "Check Messages: Http Transport Failed");
			Log.e(LOG_TAG, e.getMessage());
			return false;
		}
		/*
		 * Here we pull the return results from the incoming envelope and parses
		 * it so we get the full information
		 */
		SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
		String result = resultsRequestSOAP.getProperty("Result").toString();
		int end = result.length() - 1;
		if (result.length() < 10) {
			return false;
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
		for (int j = 0; j < info.size(); j++) {
			String[] infoString = info.get(j).split("; ");
			Log.i(LOG_TAG, "Info:" + info.get(j));
			int id = 0;
			String cmd = "";
			String cmdTxt = "";
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
			Log.v(LOG_TAG, "Command: " + cmd);
			if (cmd.equals("TMSG")) {
				String[] cmdTemp = cmdTxt.split("\\|");
				if (cmdTemp.length == 3) {
					setStatusNotification(cmdTemp[0], cmdTemp[1], cmdTemp[2]);
				}
			}

		}
		return true;
	}

	private void clearAllNotifications() {
		/* If currentNotificationIDs is has data */
		if (currentNotifcationIDs != null) {
			/**/
			Iterator<Integer> stepValue = currentNotifcationIDs.iterator();
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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

	class RunTask implements Runnable {
		long checkInterval = 30000L; // 5 min
		/*
		 * Time since last checked, so we check every checkINterval, we do that
		 * 1000000L so we check instantly
		 */
		long timeSinceCheckedMessages = System.currentTimeMillis()
				- checkInterval - 100000L;

		public void run() {
			long sleep = 1000L;
			Context f_context = getApplicationContext();
			NetworkInfo info = (NetworkInfo) ((ConnectivityManager) f_context
					.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getActiveNetworkInfo();
			String SSID = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();

			if (info != null && info.isConnected()) {
				int itype = info.getType();
				
				sleep = (long) EXT_DELAY;
				if ((SSID != null) && ((itype == 1) && (SSID.compareTo(INT_NETWORK_NAME) == 0))) {
					sleep = (long) INT_DELAY;
				}
				if (update) {
					getSongInfo();
				}
				Log.v(LOG_TAG, "Sleep : " + sleep);
			}

			if (System.currentTimeMillis() - timeSinceCheckedMessages > checkInterval) {
				timeSinceCheckedMessages = System.currentTimeMillis();
				checkMessages();
			}
			serviceHandler.postDelayed(this, sleep);
		}

		private boolean checkMessages() {
			Log.i(LOG_TAG, "checking Messages within runnable");
			/*
			 * This function send a command to the SOAP method sendCmd, which
			 * checks for messages sitting on the SOAP server's Database
			 */
			SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_GETCMD);
			request.addProperty("host", HOSTNAME);

			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			HttpTransportSE androidHttpTransport = get_transport();
			/*
			 * Added so that if the SOAP servers crash, we don't crash this
			 * remote
			 */
			try {
				androidHttpTransport.call(SOAP_ACTION, envelope);
			} catch (Exception e) {

				Log.e(LOG_TAG, "Check Messages: Http Transport Failed");
				Log.e(LOG_TAG, e.getMessage());
				return false;
			}
			/*
			 * Here we pull the return results from the incoming envelope and
			 * parses it so we get the full information
			 */
			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			String result = resultsRequestSOAP.getProperty("Result").toString();
			int end = result.length() - 1;
			if (result.length() < 10) {
				return false;
			}
			result = result.substring(8, end);
			int start = 0;
			end = 0;
			ArrayList<String> info = new ArrayList<String>();
			/*
			 * results look like {someinfo}garbage{someinfo}... So we split it
			 * up into each message, inserting it into the ArrayList for later
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
			 * We Loop threw the ArrayList info, pulling the data Then we get
			 * the info, spliting if by ';' so that we get all the values in
			 * separate variables
			 */
			for (int j = 0; j < info.size(); j++) {
				String[] infoString = info.get(j).split("; ");
				Log.i(LOG_TAG, "Info:" + info.get(j));
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
				 * TMSG is Torrent Message(the only kind of message right now
				 * Since the cmdTxt acually equals all three Status Notification
				 * Fields
				 */
				Log.v(LOG_TAG, "Command: " + cmd);
				if (cmd.equals("TMSG")) {
					String[] cmdTemp = cmdTxt.split("\\|");
					Log.i(LOG_TAG, cmdTemp.toString());
					Log.i(LOG_TAG, Integer.toString(cmdTemp.length));
					if (cmdTemp.length == 3) {
						setStatusNotification(cmdTemp[0], cmdTemp[1],
								cmdTemp[2]);
					}
				}

			}
			return true;
		}

		private boolean getSongInfo() {
			/*
			 * We encase the entire fucntion in a try/catch so that if the soap
			 * fails or we get illegal chaircters it does not kill the main
			 * program
			 */
			try {
				/* Create a SOAP package using host name */
				SoapObject request = new SoapObject(NAMESPACE,
						METHOD_NAME_GETINFO);
				request.addProperty("host", HOSTNAME);
				SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
						SoapEnvelope.VER11);
				envelope.setOutputSoapObject(request);
				HttpTransportSE androidHttpTransport = get_transport();
				androidHttpTransport.call(SOAP_ACTION, envelope);
				SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
				/* We get the result */
				String result = resultsRequestSOAP.getProperty("Result")
						.toString();
				/* We get the length 1-end so we subtrack one so it's 0-end */
				int end = result.length() - 1;
				/* First 7 chars are garbage */
				result = result.substring(8, end);
				/* Split it via the ';' */
				Log.i(LOG_TAG, "Result:" + result);
				String[] info = result.split("; ");
				for (int i = 0; i < info.length - 1; i++) {
					/*
					 * Split the key,value pair, and assuming value is a good
					 * value, stick it in SongInfo
					 */
					String[] temp = info[i].split("=");
					String key = temp[0];
					String value = temp[1];
					if ((value == null) || (value == "")) {
						value = " ";
					}
					songinfo.put(key, value);
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "SongInfo:Something Failed");
				Log.e(LOG_TAG, e.getMessage());
				return false;
			}
			return true;
		}
	}

}
