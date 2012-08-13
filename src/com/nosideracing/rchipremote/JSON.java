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

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.nosideracing.rchipremote.Consts;

import java.util.Date;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

public class JSON extends Application {


	public static Hashtable<String, String> songinfo = new Hashtable<String, String>();
	private static String URL;
	private static String HOSTNAME;

	private static Context f_context;

	private static DefaultHttpClient httpClient;
	private static CookieStore cookieStore;
	private static HttpContext httpContext;

	protected static boolean Authenticated = false;
	protected static long Authenticate_timeout = 0;
	private static JSON instance = null;


	public JSON(Context context) {
		f_context = context;
		updateSettings();
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams
				.setConnectionTimeout(myParams, Consts.http_timeout);
		HttpConnectionParams.setSoTimeout(myParams, Consts.http_timeout);
		httpClient = new DefaultHttpClient(myParams);
		cookieStore = new BasicCookieStore();
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	public static void initInstance(Context context) {
		instance = new JSON(context);
	}
	public static JSON getInstance() {
	    return instance;
	  }


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

	public Boolean UpdateSongInfo() {
		return getSongInfo();
	}

	public String getHostNames() {
		String retval = "";
		String response = JSONSendCmd("getdaemons");
		if (!response.equals("")) {
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
		if ((response != null) && (!response.equals(""))) {
			if (response.equalsIgnoreCase("Not Authorized")) {
				authenticate();
				return getCommands(host);
			}
			return new JSONArray(response);
		} else {
			return null;
		}
	}

	public boolean authenticate() {
		long temp_timeout = Authenticate_timeout + 60000;
		if (Authenticated) {
			String data = JSONSendCmd("checkauthentication");
			if (data != null) {
				try {
					JSONObject json_object = (JSONObject) new JSONTokener(data)
							.nextValue();
					if (json_object.getBoolean("authenticated")) {
						/* timeout in one hour */
						Authenticate_timeout = System.currentTimeMillis() + 3600000;
						return true;
					} else {
						Authenticated = false;
						Authenticate_timeout = 0;
					}
				} catch (JSONException e) {
					Log.e(Consts.LOG_TAG, "Error in SendCmd getting response",
							e);
				}
			}
		}
		Map<String, String> params = new HashMap<String, String>();
		String uname = PreferenceManager.getDefaultSharedPreferences(f_context)
				.getString("username", "");
		String pword = PreferenceManager.getDefaultSharedPreferences(f_context)
				.getString("password", "");
		if ((uname.equals("")) || (pword.equals(""))) {
			return false;
		}
		params.put("username", uname);
		params.put("password", pword);
		if (JSONSendCmd("authenticate", params) != null) {
			Authenticated = true;
			/* timeout in one hour */
			Authenticate_timeout = System.currentTimeMillis() + 3600000;
		} else {
			Authenticate_timeout = temp_timeout - 60000;
		}
		return true;
	}

	public void deauthenticate() {
		Authenticated = false;
		Authenticate_timeout = 0;
		JSONSendCmd("deauthenticate");
	}

	public String JSONSendCmd(String methodName, Map<String, String> params) {
		String getUrl = URL + "json/" + methodName;
		HttpResponse response = null;
		HttpGet httpGet = null;
		String json_string;
		boolean first_param = true;

		for (Map.Entry<String, String> param : params.entrySet()) {
			if (first_param) {
				getUrl += "/?";
				first_param = false;
			} else {
				getUrl += "&";
			}
			try {
				getUrl += param.getKey() + "="
						+ URLEncoder.encode(param.getValue(), "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		httpGet = new HttpGet(getUrl);
		try {
			response = httpClient.execute(httpGet, httpContext);
		} catch (ConnectTimeoutException e) {
			Toast.makeText(f_context, "Connection Timeout on " + methodName,
					Toast.LENGTH_SHORT).show();
			Log.e(Consts.LOG_TAG,
					"Connection timeout in command " + methodName, e);
		} catch (SocketTimeoutException e) {
			Toast.makeText(f_context, "Socket Timeout on " + methodName,
					Toast.LENGTH_SHORT).show();
			Log.e(Consts.LOG_TAG, "Socket timeout in command " + methodName, e);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}
		process_cookies();
		try {
			if (response != null) {
				json_string = EntityUtils.toString(response.getEntity());
				if (json_string != null) {
					JSONObject json_object = (JSONObject) new JSONTokener(
							json_string).nextValue();
					if (json_object.getBoolean("success")) {
						return json_object.getString("data");
					}
				}
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			return null;
		}
		return null;
	}

	public String JSONSendCmd(String methodName) {
		HttpResponse response = null;
		HttpGet httpGet = null;
		String json_string;
		String getUrl = URL + "json/" + methodName + '/';
		httpGet = new HttpGet(getUrl);
		if (Authenticate_timeout < System.currentTimeMillis()) {
			authenticate();
		}
		try {
			response = httpClient.execute(httpGet, httpContext);
		} catch (ConnectTimeoutException e) {
			Toast.makeText(f_context, "Connection Timeout on " + methodName,
					Toast.LENGTH_SHORT).show();
			Log.e(Consts.LOG_TAG,
					"Connection timeout in command " + methodName, e);
		} catch (SocketTimeoutException e) {
			Toast.makeText(f_context, "Socket Timeout on " + methodName,
					Toast.LENGTH_SHORT).show();
			Log.e(Consts.LOG_TAG, "Socket timeout in command " + methodName, e);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}
		process_cookies();
		try {
			json_string = EntityUtils.toString(response.getEntity());
			Log.d(Consts.LOG_TAG, new JSONTokener(json_string).nextValue()
					.toString());
			JSONObject json_object = (JSONObject) new JSONTokener(json_string)
					.nextValue();
			if (json_object.getBoolean("success")) {
				return json_object.getString("data");
			} else {
				return null;
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			return null;
		}
	}

	public ArrayList<UpcomingShowInfo> getUpcomingShows() {
		try {
			ArrayList<UpcomingShowInfo> upcoming = new ArrayList<UpcomingShowInfo>();
			String json_array = JSONSendCmd("getupcomingshows");
			if (!json_array.equals("")) {
				JSONArray jsonArray = new JSONArray(json_array);
				for (int i = 0; i < jsonArray.length(); i++) {
					UpcomingShowInfo show = new UpcomingShowInfo();
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					show.EpisodeName = jsonObject.getString("eps_name");
					show.EpisodeNumber = jsonObject.getString("eps_number");
					show.ShowName = jsonObject.getString("show__name");
					show.AirDate = (Date) new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss").parse((jsonObject
							.getString("air_date")));
					show.AirTime = jsonObject.getInt("show__air_time");
					upcoming.add(show);
				}
			}
			return upcoming;
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in getUpcommingShows", e);
		}
		return null;
	}

	private void process_cookies() {
		List<Cookie> cookies = httpClient.getCookieStore().getCookies();

		if (!cookies.isEmpty()) {
			CookieSyncManager.createInstance(f_context);
			CookieManager cookieManager = CookieManager.getInstance();
			for (Cookie cookie : cookies) {
				Cookie sessionInfo = cookie;
				String cookieString = sessionInfo.getName() + "="
						+ sessionInfo.getValue() + ";    domain="
						+ sessionInfo.getDomain();
				cookieManager.setCookie("http://www.nosideracing.com",
						cookieString);
				CookieSyncManager.getInstance().sync();
			}
		}
	}

	protected void updateSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(f_context);
		URL = settings.getString("serverurl", "http://www.nosideholdings.com/");
		if (URL.charAt(URL.length() - 1) != '/') {
			URL = URL + '/';
		}
		HOSTNAME = ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
	}

	private Boolean getSongInfo() {
		try {
			updateSettings();
			Map<String, String> params = new HashMap<String, String>();
			params.put("host", HOSTNAME);
			String response = JSONSendCmd("getsonginfo", params);
			if (!response.equals("")) {
				JSONArray jsonArray = new JSONArray(response);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					songinfo.put("album", (String) jsonObject.get("album"));
					songinfo.put("total_time", jsonObject.get("total_time")
							.toString());
					songinfo.put("song", (String) jsonObject.get("song"));
					songinfo.put("artist", (String) jsonObject.get("artist"));
					songinfo.put("elapsed_time", jsonObject.get("elapsed_time")
							.toString());
					songinfo.put("is_playing", (jsonObject.get("is_playing")
							.equals("false")) ? "0" : "1");
				}
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "SongInfo:Something Failed", e);
			return false;
		}
		return true;

	}



}
