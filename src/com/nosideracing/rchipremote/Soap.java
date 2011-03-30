package com.nosideracing.rchipremote;

import java.util.Hashtable;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Soap {

	public static Hashtable<String, String> songinfo = new Hashtable<String, String>();
	private static String URL_EXT;
	private static String URL_INT;
	private static String INT_NETWORK_NAME;
	private static String HOSTNAME;
	private static String DESTHOSTNAME;
	private static long EXT_DELAY;
	private static long INT_DELAY;

	public static boolean sendCmd(String cmd, String cmdText) {
		return sendCmdToSoap(cmd, cmdText);
	}

	/* We set a notificaion, */
	public static boolean setNotification(String tickerString,
			String notificationTitle, String noticicationText) {
		if (Notifications.setStatusNotification(tickerString,
				notificationTitle, noticicationText) == null) {
			return false;
		}
		return true;
	}

	public static void clearNotifications() {
		Notifications.clearAllNotifications();
	}

	/* The following only return a value from SongInfo */
	public static String getArtest() {
		String text = songinfo.get("artist").toString();
		return text;
	}

	public static String getAlbum() {
		return songinfo.get("album").toString();
	}

	public static String getSongName() {
		return songinfo.get("title").toString();
	}

	public static String getTimeElapised() {
		return songinfo.get("etime").toString();
	}

	public static String getSongLength() {
		return songinfo.get("tottime").toString();
	}

	public static int getIsPlaying() {
		return Integer.parseInt(songinfo.get("isPlaying"));
	}

	public static String getRootValue(String hn) {
		return get_root_value(hn);
	}

	public static Boolean UpdateSongInfo() {
		return getSongInfo();
	}

	protected static void updateSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(RemoteMain.f_context);
		URL_EXT = settings.getString("serverurlexternal",
				"http://173.3.14.224:500/");
		;
		URL_INT = settings.getString("serverurlinternal",
				"http://192.168.1.3:500/");
		INT_NETWORK_NAME = settings.getString("internalnetname", "Node_77");
		;
		HOSTNAME = ((TelephonyManager) RemoteMain.f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		DESTHOSTNAME = settings.getString("serverhostname", "Tomoya");
		EXT_DELAY = Integer.parseInt(settings
				.getString("externaldelay", "5000"));
		INT_DELAY = Integer.parseInt(settings
				.getString("internaldelay", "1000"));
	}

	private static Boolean getSongInfo() {
		try {
			updateSettings();
			Log.d(Consts.LOG_TAG, "SongInfo:Starting Song Info");
			/* Create a SOAP package using host name */
			SoapObject request = new SoapObject(Consts.NAMESPACE,
					Consts.METHOD_NAME_GETINFO);
			request.addProperty("host", HOSTNAME);
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			HttpTransportSE androidHttpTransport = get_transport();
			androidHttpTransport.call(Consts.SOAP_ACTION, envelope);
			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			/* We get the result */
			String result = resultsRequestSOAP.getProperty("Result").toString();
			/* We get the length 1-end so we subtrack one so it's 0-end */
			int end = result.length() - 1;
			/* First 7 chars are garbage */
			result = result.substring(8, end);
			/* Split it via the ';' */
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
			Log.e(Consts.LOG_TAG, "SongInfo:Something Failed");
			Log.e(Consts.LOG_TAG, "" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return false;
		}
		return true;

	}

	private static boolean sendCmdToSoap(String cmd, String cmdText) {
		/*
		 * This function send a command to the SOAP method sendCmd, which passes
		 * a command into the database defaulting to default destination
		 */
		/* Again sets up the SOAP envelope */
		String destination = DESTHOSTNAME;
		SoapObject request = new SoapObject(Consts.NAMESPACE,
				Consts.METHOD_NAME_SENDCMD);
		request.addProperty("cmd", cmd);
		request.addProperty("txt", cmdText);
		request.addProperty("shost", HOSTNAME);
		request.addProperty("dhost", destination);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		/* Added so that if the SOAP servers crash, we don't crash this remote */
		try {
			androidHttpTransport.call(Consts.SOAP_ACTION, envelope);
		} catch (Exception e) {

			Log.e(Consts.LOG_TAG, "sendCmdToSoap: Http Transport Failed");
			Log.e(Consts.LOG_TAG, "" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return false;
		}
		return true;
	}

	private static String get_root_value(String hn) {
		try {
			SoapObject request = new SoapObject(Consts.NAMESPACE,
					Consts.METHOD_NAME_GETVIDEOPATH);
			request.addProperty("host", hn);
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			HttpTransportSE androidHttpTransport = get_transport();

			androidHttpTransport.call(Consts.SOAP_ACTION, envelope);

			SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
			/* We get the result */
			return resultsRequestSOAP.getProperty("Result").toString();
		} catch (Exception e) {
			return null;
		}
	}

	private static HttpTransportSE get_transport() {
		NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) RemoteMain.f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		HttpTransportSE androidHttpTransport = null;
		if ((networkinfo != null) && (networkinfo.isConnected())) {
			int itype = networkinfo.getType();
			String SSID = ((WifiManager) RemoteMain.f_context
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

	public static long getDelay() {
		NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) RemoteMain.f_context
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();
		if ((networkinfo != null) && (networkinfo.isConnected())) {
			int itype = networkinfo.getType();
			String SSID = ((WifiManager) RemoteMain.f_context
					.getSystemService(Context.WIFI_SERVICE))
					.getConnectionInfo().getSSID();
			if ((SSID != null)
					&& ((itype == 1) && (SSID.compareTo(INT_NETWORK_NAME) == 0))) {
				return INT_DELAY;
			} else {
				return EXT_DELAY;
			}
		} else {
			return EXT_DELAY;
		}
	}
}
