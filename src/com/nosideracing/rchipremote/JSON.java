package com.nosideracing.rchipremote;


import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
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

import com.nosideracing.rchipremote.UpcomingShowList.UpcomingShowInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class JSON {

	public Hashtable<String, String> songinfo = new Hashtable<String, String>();
	private String URL;
	private String HOSTNAME;
	private Context f_context;
	
	DefaultHttpClient httpClient;
	CookieStore cookieStore;
	HttpContext httpContext;

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
		cookieStore = new BasicCookieStore();
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
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
		if (!response.equals("")) {
			if (response.equalsIgnoreCase("Not Authorized")){
				authenticate();
				return getCommands(host);
			}
			return new JSONArray(response);
		} else {
			return null;
		}
	}

	public boolean authenticate() {
		Map<String, String> params = new HashMap<String, String>();
		String uname = PreferenceManager.getDefaultSharedPreferences(f_context).getString("username", "");
		String pword = PreferenceManager.getDefaultSharedPreferences(f_context).getString("password", "");
		if ((uname.equals("")) || (pword.equals(""))){
			return false;
		}
		Log.v(Consts.LOG_TAG,"username & password both set");
		JSONSendCmd("authenticate",params);
		return true;
	}
	
	public void deauthenticate() {
		JSONSendCmd("deauthenticate");
	}

	public String JSONSendCmd(String methodName, Map<String, String> params) {
		String getUrl = URL + "json/" + methodName;
		int i = 0;
		for (Map.Entry<String, String> param : params.entrySet()) {
			if (i == 0) {
				getUrl += "/?";
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
		httpGet = new HttpGet(getUrl);
		try {
			response = httpClient.execute(httpGet,httpContext);			
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}
		process_cookies();
		// we assume that the response body contains the error message
		try {
			ret = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			ret = "";
		}
		return ret;
	}

	public String JSONSendCmd(String methodName) {

		String getUrl = URL + "json/" + methodName+'/';

		httpGet = new HttpGet(getUrl);

		try {
			response = httpClient.execute(httpGet,httpContext);
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd sending command", e);
		}
		process_cookies();
		// we assume that the response body contains the error message
		try {
			ret = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in SendCmd getting response", e);
			ret = "";
		}
		return ret;
	}

	public ArrayList<UpcomingShowInfo> getUpcomingShows(){
		ArrayList<UpcomingShowInfo> upcoming = new ArrayList<UpcomingShowInfo>();
		return upcoming;
		
	}
	private void process_cookies() {
		List<Cookie> cookies = httpClient.getCookieStore().getCookies();

		if (!cookies.isEmpty())
		{
		    CookieSyncManager.createInstance(f_context);
		    CookieManager cookieManager = CookieManager.getInstance();

		    // sync all the cookies in the httpclient with the webview
		    // by generating cookie string
		    for (Cookie cookie : cookies)
		    {
		        Cookie sessionInfo = cookie;

		        String cookieString = sessionInfo.getName() + "=" + sessionInfo.getValue() + ";    domain=" + sessionInfo.getDomain();
		        cookieManager.setCookie("http://www.nosideracing.com", cookieString);
		        CookieSyncManager.getInstance().sync();
		    }
		}
	}

	protected void updateSettings() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(f_context);
		URL = settings.getString("serverurl", "http://www.nosideholdings.com/");
		if (URL.charAt(URL.length()-1) != '/'){
			URL = URL+'/';
		}
		HOSTNAME = ((TelephonyManager) f_context
				.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		// DESTHOSTNAME = settings.getString("serverhostname", "Tomoya");
		
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
			Log.e(Consts.LOG_TAG, "SongInfo:Something Failed",e);
			return false;
		}
		return true;

	}

}
