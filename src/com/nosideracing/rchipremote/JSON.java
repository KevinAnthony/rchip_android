package com.nosideracing.rchipremote;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class JSON {

	public Hashtable<String, String> songinfo = new Hashtable<String, String>();
	private String URL;
	private String HOSTNAME;
	private long DELAY;
	private Context f_context;
	DefaultHttpClient httpClient;
	private String ret;

	HttpResponse response = null;
	HttpPost httpPost = null;
	HttpGet httpGet = null;

	/*
	 * protected String getHostNames() { return getHostNamesJSON(); }
	 */
	public JSON(Context context) {
		f_context = context;
		updateSettings();
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, 10000);
		HttpConnectionParams.setSoTimeout(myParams, 10000);
		httpClient = new DefaultHttpClient(myParams);
	}

	/* We set a notificaion, */
	public boolean setNotification(String tickerString,
			String notificationTitle, String noticicationText, Context context) {
		if (Notifications.setStatusNotification(tickerString,
				notificationTitle, noticicationText, context) == null) {
			return false;
		}
		return true;
	}

	public void clearNotifications(Context context) {
		Notifications.clearAllNotifications(context);
	}

	/* The following only return a value from SongInfo */
	public String getArtest() {
		try {
			return songinfo.get("artist").toString();
		} catch (Exception e) {
			return "Artist";
		}
	}

	public String getAlbum() {
		try {
			return songinfo.get("album").toString();
		} catch (Exception e) {
			return "Artist";
		}
	}

	public String getSongName() {
		try {
			return songinfo.get("song").toString();
		} catch (Exception e) {
			return "Artist";
		}
	}

	public String getTimeElapised() {
		try {
			return songinfo.get("elapsed_time").toString();
		} catch (Exception e) {
			return "0000";
		}
	}

	public String getSongLength() {
		try {
			return songinfo.get("total_time").toString();
		} catch (Exception e) {
			return "0000";
		}
	}

	public int getIsPlaying() {
		try {
			return Integer.parseInt(songinfo.get("is_playing"));
		} catch (Exception e) {
			return 0;
		}
	}

	// public String getRootValue(String hn) { return getRootValueJSON(hn); }

	public long getDelay() {
		return DELAY;
	}

	public Boolean UpdateSongInfo() {
		return getSongInfo();
	}

	public String getHostNames() {
		String retval = "";
		String response = JSONSendCmd("getdaemons");
		if (!response.equals("")) {
			Log.d(Consts.LOG_TAG, response);
			JSONArray jsonArray;
			try {
				jsonArray = new JSONArray(response);

				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					String object = (String) jsonObject.get("hostname");
					if (retval.equals("")) {
						retval = object;
					} else {
						retval = retval + "|" + object;
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				Log.e(Consts.LOG_TAG, "Error in Get Hosts Name", e);
			}
		}
		return retval;
	}

	public String getRootPath() {
		String retval = "";
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("host", RemoteMain.msb_desthost);
			String response = JSONSendCmd("getvideopath", params);
			if (!response.equals("")) {
				JSONArray jsonArray = new JSONArray(response);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					retval = (String) jsonObject.get("path_to_root");
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.e(Consts.LOG_TAG, "Error in Get Hosts Name", e);
		}
		if (!retval.endsWith("/")) {
			retval = retval + "/";
		}
		return retval;

	}

	public JSONArray getCommands(String host) throws JSONException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("host", host);
		String response = JSONSendCmd("getcommand", params);
		if (!response.equals("")) {
			return new JSONArray(response);
		} else {
			return null;
		}
	}

	public String JSONSendCmd(String methodName, Map<String, String> params) {

		String getUrl = URL + "json/" + methodName;
		int i = 0;
		for (Map.Entry<String, String> param : params.entrySet()) {
			if (i == 0) {
				getUrl += "?";
			} else {
				getUrl += "&";
			}
			try {
				getUrl += param.getKey() + "="
						+ URLEncoder.encode(param.getValue(), "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;
		}
		Log.i(Consts.LOG_TAG, "getUrl:" + getUrl);
		httpGet = new HttpGet(getUrl);

		try {
			response = httpClient.execute(httpGet);
		} catch (Exception e) {
			Log.w(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}

		// we assume that the response body contains the error message
		try {
			ret = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			Log.w(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			ret = "";
		}
		return ret;
	}

	public String JSONSendCmd(String methodName) {

		String getUrl = URL + "json/" + methodName;

		Log.i(Consts.LOG_TAG, "getUrl:" + getUrl);
		httpGet = new HttpGet(getUrl);

		try {
			response = httpClient.execute(httpGet);
		} catch (Exception e) {
			Log.w(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}

		// we assume that the response body contains the error message
		try {
			ret = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			Log.w(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			ret = "";
		}
		return ret;
	}

	protected void updateSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(f_context);
		URL = settings.getString("serverurl", "http://www.nosideholdings.com/");
		Log.d(Consts.LOG_TAG,"URL:"+URL);
		HOSTNAME = ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		// DESTHOSTNAME = settings.getString("serverhostname", "Tomoya");
		DELAY = Integer.parseInt(settings.getString("delay", "5000"));
	}

	private Boolean getSongInfo() {
		try {
			updateSettings();
			Log.d(Consts.LOG_TAG, "JSON:SongInfo:Starting Song Info");
			Map<String, String> params = new HashMap<String, String>();
			params.put("host", HOSTNAME);
			Log.e(Consts.LOG_TAG, "hostname:" + HOSTNAME);
			String response = JSONSendCmd("getsonginfo", params);
			if (!response.equals("")) {
				Log.d(Consts.LOG_TAG, response);
				JSONArray jsonArray = new JSONArray(response);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					songinfo.put("album", (String) jsonObject.get("album"));
					songinfo.put("total_time",
							jsonObject.get("total_time").toString());
					songinfo.put("song", (String) jsonObject.get("song"));
					songinfo.put("artist", (String) jsonObject.get("artist"));
					songinfo.put("elapsed_time",
							jsonObject.get("elapsed_time").toString());
					songinfo.put("is_playing", (jsonObject.get("is_playing")
							.equals("false")) ? "0" : "1");
				}
			}
			// songinfo.put(key, value);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "SongInfo:Something Failed");
			Log.e(Consts.LOG_TAG, "" + e.getMessage());
			Log.e(Consts.LOG_TAG, "", e);
			return false;
		}
		return true;

	}

}
