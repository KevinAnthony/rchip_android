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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

@SuppressWarnings("unchecked")
public class JSON {

	private Hashtable<String, Object> songinfo = new Hashtable<String, Object>();
	private Hashtable<String, Object> networkInfomation = new Hashtable<String, Object>();
	private ArrayList<UpcomingShowInfo> upcoming = new ArrayList<UpcomingShowInfo>();
	private Context f_context;
	public ProgressDialog dialog = null;
	private CookieStore cookieStore;

	private boolean Authenticated = false;
	private long Authenticate_timeout = 0;

	private static JSON instance = null;

	public JSON(Context context) {
		f_context = context;
		cookieStore = new BasicCookieStore();
	}

	public JSON() {
		cookieStore = new BasicCookieStore();
	}

	public static void initInstance() {
		instance = new JSON();
	}

	public static JSON getInstance() {
		return instance;
	}

	public void set_context(Context context) {
		f_context = context;
		update_network_info();
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

	public String getSongLength() {
		try {
			return songinfo.get("total_time").toString();
		} catch (Exception e) {
			return "0000";
		}
	}

	// TODO this should RETURN Boolean
	public Boolean getIsPlaying() {
		return (Boolean) songinfo.get("is_playing");
	}

	public Boolean UpdateSongInfo() {
		return getSongInfo();
	}

	public String getHostNames() {
		return (String) networkInfomation.get("daemons");
	}

	public String getRootPath() {
		return (String) networkInfomation.get("path_to_root");
	}

	public void getCommands(String host) {
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("host", host);
		tasks.add(new networkTask("authenticate", null));
		tasks.add(new networkTask("checkauthentication", null));
		tasks.add(new networkTask("getcommand", params));
		tasks.add(new networkTask("deauthenticate", null));
		new doNetworking().execute(tasks);
	}

	public void authenticate() {
		Log.i(Consts.LOG_TAG, "Authenicating");
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		Map<String, String> params = new HashMap<String, String>();
		String uname = PreferenceManager.getDefaultSharedPreferences(f_context)
				.getString("username", "");
		String pword = PreferenceManager.getDefaultSharedPreferences(f_context)
				.getString("password", "");
		params.put("username", uname);
		params.put("password", pword);
		tasks.add(new networkTask("authenticate", params));
		new doNetworking().execute(tasks);
	}

	public void deauthenticate() {
		Log.i(Consts.LOG_TAG, "Deauthenicating");
		Authenticated = false;
		Authenticate_timeout = 0;
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		tasks.add(new networkTask("deauthenticate", null));
		new doNetworking().execute(tasks);
	}

	public ArrayList<UpcomingShowInfo> getUpcomingShows() {
		try {
			return upcoming;
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in getUpcommingShows", e);
		}
		return null;
	}

	public void JSONSendCmd(String command) {
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		tasks.add(new networkTask(command, null));
		new doNetworking().execute(tasks);
	}

	public void JSONSendCmd(String command, Map<String, String> params) {
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		tasks.add(new networkTask(command, params));
		new doNetworking().execute(tasks);
	}


	private void update_network_info() {
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		String hostname = ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		String daemon = PreferenceManager
				.getDefaultSharedPreferences(f_context).getString("serverhostname", null);
		Map<String, String> getVideoPathParams = new HashMap<String, String>();
		getVideoPathParams.put("host", daemon);
		tasks.add(new networkTask("getvideopath", getVideoPathParams));
		Map<String, String> getSongInfoParams = new HashMap<String, String>();
		getSongInfoParams.put("host", hostname);
		tasks.add(new networkTask("getsonginfo", getSongInfoParams));
		tasks.add(new networkTask("getupcomingshows", null));
		tasks.add(new networkTask("getdaemons", null));
		Map<String, String> RegisterRemoteDeviceParams = new HashMap<String, String>();
		RegisterRemoteDeviceParams.put("device_name", hostname);
		RegisterRemoteDeviceParams.put("state", "true");
		tasks.add(new networkTask("registerremotedevice",
				RegisterRemoteDeviceParams));

		new doNetworking().execute(tasks);
	}

	private Boolean getSongInfo() {
		Log.i(Consts.LOG_TAG, "Updating Song Info");
		Map<String, String> params = new HashMap<String, String>();
		params.put("host", ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
		ArrayList<networkTask> tasks = new ArrayList<networkTask>();
		tasks.add(new networkTask("getsonginfo", params));
		new doNetworking().execute(tasks);
		return true;

	}

	@SuppressWarnings("unused")
	private class networkTask {
		private String method;
		private Object paramiters;

		networkTask(String m, Object p) {
			method = m;
			paramiters = p;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String m) {
			method = m;
		}

		public Object getParamiter() {
			return paramiters;
		}

		public void setParamiter(Object p) {
			paramiters = p;
		}

		public String toString() {
			return "<method: " + method + ">";
		}
	}

	private class doNetworking
			extends
			AsyncTask<ArrayList<networkTask>, Boolean, Hashtable<String, Object>> {

		private DefaultHttpClient httpClient;
		private HttpContext httpContext;

		protected Hashtable<String, Object> doInBackground(
				ArrayList<networkTask>... incoming) {
			Hashtable<String, Object> returnVal = new Hashtable<String, Object>();
			ArrayList<networkTask> tasks = incoming[0];
			for (networkTask task : tasks) {
				String key = task.getMethod();
				Object value = task.getParamiter();
				if (key.equalsIgnoreCase("")) {
					continue;
				} else if (key.equalsIgnoreCase("authenticate")
						&& value == null) {
					force_auth();
				} else if (key.equalsIgnoreCase("getsonginfo")
						|| key.equalsIgnoreCase("authenticate")
						|| key.equalsIgnoreCase("getvideopath")
						|| key.equalsIgnoreCase("getcommand")
						|| key.equalsIgnoreCase("registerremotedevice")
						|| key.equalsIgnoreCase("sendcommand")) {
					Map<String, String> params = (Map<String, String>) value;
					String networkString = JSONSendCommand(key, params);
					if (networkString != null)
						returnVal.put(key, networkString);
					else
						Log.w(Consts.LOG_TAG, "Problem with command " + key);

				} else if (key != null) {
					String networkString = JSONSendCommand(key);
					if (networkString != null)
						returnVal.put(key, networkString);
					else
						Log.w(Consts.LOG_TAG, "Problem with command " + key);
				}
			}
			return returnVal;
		}

		protected void onPreExecute() {
			super.onPreExecute();
			HttpParams myParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(myParams,
					Consts.http_timeout);
			HttpConnectionParams.setSoTimeout(myParams, Consts.http_timeout);
			httpClient = new DefaultHttpClient(myParams);
			httpContext = new BasicHttpContext();
			httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		}

		protected void onPostExecute(Hashtable<String, Object> result) {
			Enumeration<String> keys = result.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				Log.i(Consts.LOG_TAG, "Proccessing "+key);
				Object value = result.get(key);
				try {
					if (key.equalsIgnoreCase("")) {
						continue;
					} else if (key.equalsIgnoreCase("getsonginfo")) {
						String response = (String) value;
						if ((response != null) && (!response.equals(""))) {
							JSONArray jsonArray = new JSONArray(response);
							for (int i = 0; i < jsonArray.length(); i++) {
								JSONObject jsonObject = jsonArray
										.getJSONObject(i);
								songinfo.put("album",
										(String) jsonObject.get("album"));
								songinfo.put("total_time",
										jsonObject.get("total_time").toString());
								songinfo.put("song",
										(String) jsonObject.get("song"));
								songinfo.put("artist",
										(String) jsonObject.get("artist"));
								songinfo.put("is_playing",
										(Boolean) jsonObject.get("is_playing"));
							}
						}
					} else if (key.equalsIgnoreCase("getupcomingshows")) {
						String json_array = (String) value;
						if (!json_array.equals("")) {
							JSONArray jsonArray = new JSONArray(json_array);
							for (int i = 0; i < jsonArray.length(); i++) {
								UpcomingShowInfo show = new UpcomingShowInfo();
								JSONObject jsonObject = jsonArray
										.getJSONObject(i);
								show.EpisodeName = jsonObject
										.getString("eps_name");
								show.EpisodeNumber = jsonObject
										.getString("eps_number");
								show.ShowName = jsonObject
										.getString("show__name");
								show.AirDate = (Date) new SimpleDateFormat(
										"yyyy-MM-dd HH:mm:ss")
										.parse((jsonObject
												.getString("air_date")));
								show.AirTime = jsonObject
										.getInt("show__air_time");
								upcoming.add(show);
							}
						}
					} else if (key.equalsIgnoreCase("authenticate")) {
						if ((String) value != null) {
							Authenticated = true;
							Authenticate_timeout = System.currentTimeMillis() + 3600000;
						} else {
							Authenticated = false;
						}
					} else if (key.equalsIgnoreCase("getvideopath")) {
						JSONArray jsonArray = new JSONArray((String) value);
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							String path = (String) jsonObject
									.get("path_to_root");
							if (path.charAt(path.length() - 1) != '/') {
								path = path + '/';
							}
							networkInfomation.put("path_to_root", path);
						}
					} else if (key.equalsIgnoreCase("getdaemons")) {
						JSONArray jsonArray = new JSONArray((String) value);
						String daemons = "";
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							String object = (String) jsonObject.get("hostname");
							
							if (!daemons.equals("")) {
								daemons = daemons + "|" + object;
							} else {
								daemons = object;
							}
						}
						networkInfomation.put("daemons", daemons);
					} else if (key.equalsIgnoreCase("getcommand")) {
						JSONArray jsonArray = new JSONArray((String) value);
						List<String[]> shows = new ArrayList<String[]>();
						if (jsonArray != null) {
							for (int i = 0; i < jsonArray.length(); i++) {
								JSONObject jsonObject = jsonArray
										.getJSONObject(i);
								String cmd = (String) jsonObject.get("command");
								String cmdTxt = (String) jsonObject
										.get("command_text");
								String[] curShow = new String[4];
								if (cmd.equals("TMSG")) {
									String[] cmdTemp = cmdTxt.split("\\|");
									if (cmdTemp.length == 3) {
										curShow = Notifications
												.setStatusNotification(
														cmdTemp[0], cmdTemp[1],
														cmdTemp[2], f_context);
									}
								} else if (cmd.equals("ADDS")) {
									String[] cmdTemp = cmdTxt.split("\\|");
									if (cmdTemp.length == 4) {
										curShow[0] = cmdTemp[0];
										curShow[1] = cmdTemp[1];
										curShow[2] = cmdTemp[2];
										curShow[3] = cmdTemp[3];
									}
								} else {
									Log.w(Consts.LOG_TAG, "Commad:" + cmd
											+ " Not supported");
								}
								if (curShow != null) {
									shows.add(curShow);
								}
							}
							Database mOpenHelper = new Database(f_context);
							try {
								for (int i = 0; i < shows.size(); i++) {
									String[] curShow = shows.get(i);
									mOpenHelper.insertShow(curShow[0],
											curShow[2], curShow[1], curShow[3]);
								}
							} catch (Exception e) {
								Log.e(Consts.LOG_TAG,
										"Could not pass data to show window "
												+ e);
							}
						}
					}
				} catch (Exception e) {

				}
			}
			if (dialog != null){
				dialog.cancel();
				dialog = null;
			}
		}

		private String JSONSendCommand(String methodName,
				Map<String, String> params) {
			if (!Authenticated
					|| Authenticate_timeout < System.currentTimeMillis()) {
				Log.w(Consts.LOG_TAG, "Forcing Authentication");
				if (methodName.equalsIgnoreCase("getcommand")) {
					Log.w(Consts.LOG_TAG, "this is a get command");
				}
				force_auth();
			}
			String url = PreferenceManager.getDefaultSharedPreferences(f_context)
					.getString("serverurl", "http://www.nosideholdings.com/");
			if (url.charAt(url.length() - 1) != '/') {
				url = url + '/';
			}
			String getUrl = url + "json/" + methodName;

			HttpResponse response = null;
			HttpGet httpGet = null;
			String json_string = null;
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
					Log.w(Consts.LOG_TAG, "JSON URL:" + getUrl);
					Log.w(Consts.LOG_TAG, "Couldn't encode parameter", e);
				}
			}
			httpGet = new HttpGet(getUrl);
			try {
				response = httpClient.execute(httpGet, httpContext);
			} catch (ConnectTimeoutException e) {
				Log.e(Consts.LOG_TAG, "Connection timeout in command "
						+ methodName, e);
				return null;
			} catch (SocketTimeoutException e) {
				Log.e(Consts.LOG_TAG,
						"Socket timeout in command " + methodName, e);
				return null;
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
				return null;
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
						} else {
							Log.w(Consts.LOG_TAG, "JSON not successful");
							Log.w(Consts.LOG_TAG, json_string);
						}
					} else {
						Log.e(Consts.LOG_TAG, "Returning json NULL");
					}
				}
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "JSON URL:" + getUrl);
				Log.e(Consts.LOG_TAG, "JSON STRING:" + json_string);
				Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
				return null;
			}
			return null;
		}

		private String JSONSendCommand(String methodName) {
			String url = PreferenceManager.getDefaultSharedPreferences(f_context)
					.getString("serverurl", "http://www.nosideholdings.com/");
			if (url.charAt(url.length() - 1) != '/') {
				url = url + '/';
			}
			if (!Authenticated
					|| Authenticate_timeout < System.currentTimeMillis()) {
				Log.w(Consts.LOG_TAG, "Forcing Authentication");
				force_auth();
			}

			HttpResponse response = null;
			HttpGet httpGet = null;
			String json_string = null;

			String getUrl = url + "json/" + methodName + '/';
			httpGet = new HttpGet(getUrl);
			try {
				response = httpClient.execute(httpGet, httpContext);
			} catch (ConnectTimeoutException e) {
				Log.e(Consts.LOG_TAG, "Connection timeout in command "
						+ methodName, e);
			} catch (SocketTimeoutException e) {
				Log.e(Consts.LOG_TAG,
						"Socket timeout in command " + methodName, e);
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
			}
			if (response == null) {
				Log.w(Consts.LOG_TAG, "Response was null for command "
						+ methodName);
				return null;
			}
			process_cookies();
			try {
				json_string = EntityUtils.toString(response.getEntity());
				JSONTokener json_token = null;
				json_token = new JSONTokener(json_string);
				JSONObject json_object = (JSONObject) json_token.nextValue();
				if (json_object.getBoolean("success")) {
					return json_object.getString("data");
				} else {
					Log.w(Consts.LOG_TAG, "No data tag for " + methodName);
					return null;
				}
			} catch (Exception e) {
				Log.e(Consts.LOG_TAG, "JSON URL:" + getUrl);
				Log.e(Consts.LOG_TAG, "JSON STRING:" + json_string);
				Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
				return null;
			}
		}

		private void process_cookies() {
			List<Cookie> cookies = httpClient.getCookieStore().getCookies();
			if (!cookies.isEmpty()) {
				if (CookieSyncManager.getInstance() == null){
					CookieSyncManager.createInstance(f_context);
				}
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

		private void force_auth() {
			String uname = PreferenceManager.getDefaultSharedPreferences(
					f_context).getString(Consts.PREF_USERNAME, "");
			String pword = PreferenceManager.getDefaultSharedPreferences(
					f_context).getString(Consts.PREF_PASSWORD, "");
			String url = PreferenceManager.getDefaultSharedPreferences(f_context)
					.getString(Consts.PREF_URL, "http://www.nosideholdings.com/");
			if (url.charAt(url.length() - 1) != '/') {
				url = url + '/';
			}
			String getUrl = url + "json/authenticate/?username=" + uname
					+ "&password=" + pword;
			HttpGet httpGet = new HttpGet(getUrl);
			HttpResponse response = null;
			try {
				response = httpClient.execute(httpGet, httpContext);
			} catch (ConnectTimeoutException e) {
				Log.e(Consts.LOG_TAG,
						"Connection timeout in command force_auth", e);
			} catch (ClientProtocolException e) {
				Log.e(Consts.LOG_TAG, "Protocol Exception force_auth", e);
			} catch (IOException e) {
				Log.e(Consts.LOG_TAG, "IOException force_auth", e);
			}
			process_cookies();
			if (response != null) {
				String json_string = null;
				try {
					json_string = EntityUtils.toString(response.getEntity());
					if (json_string != null) {
						JSONTokener json_token = null;
						json_token = new JSONTokener(json_string);
						JSONObject json_object = (JSONObject) json_token
								.nextValue();
						if (json_object.getBoolean("success")) {
							Authenticated = true;
							Authenticate_timeout = System.currentTimeMillis() + 3600000;
						}
					}
				} catch (Exception e) {
				}
			}
		}
	}
}
