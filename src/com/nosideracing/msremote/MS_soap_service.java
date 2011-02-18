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
import android.database.sqlite.SQLiteDatabase;

public class MS_soap_service extends Service {

    private Handler serviceHandler = null;
    /* primary song info, Tag and Info */
    public Hashtable<String, String> songinfo = new Hashtable<String, String>();
    /* We use static Strings here so if it changes, we can change it up here */

    /* These are global so that we can reference them from many a method */
    private String URL_EXT;
    private String URL_INT;
    private String INT_NETWORK_NAME;
    private String HOSTNAME;
    private String DESTHOSTNAME;
    private int EXT_DELAY;
    private int INT_DELAY;

    /* We use this to set the ID number of the current notification */

    private int numberOfNotifications = 0;
    /* Are we updating? */
    private boolean update = false;
    /* Are we running? */
    private boolean running = false;
    List<Integer> currentNotifcationIDs = new ArrayList<Integer>();
    private MS_database mOpenHelper;
    
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
	try {
	    mOpenHelper = new MS_database(getApplicationContext());
	    Bundle incoming = intent.getExtras();
	    URL_EXT = incoming.get("SETTING_URL_EXTERNAL").toString();
	    // URL = "http://173.3.14.224:500";
	    URL_INT = incoming.get("SETTING_URL_INTERNAL").toString();
	    HOSTNAME = incoming.get("SETTING_SOURCENAME").toString();
	    DESTHOSTNAME = incoming.get("SETTING_DESTNAME").toString();
	    INT_NETWORK_NAME = incoming.get("SETTING_INTERNAL_NETWORK_NAME").toString();
	    EXT_DELAY = incoming.getInt("SETTING_EXTERNAL_DELAY");
	    INT_DELAY = incoming.getInt("SETTING_INTERNAL_DELAY");
	    boolean ktornot = incoming.getBoolean("SETTING_KTORRENTNOTIFICATION");
	    set_ktorrent_notifications(ktornot);
	} catch (Exception e) {
	    Log.e(MS_constants.LOG_TAG, "Could not read file 1 " + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return START_FLAG_RETRY;
	}

	/* Initialze SongInfo to default values */
	songinfo.put("artist", "Nobody");
	songinfo.put("album", "Nothing");
	songinfo.put("title", "Nothing Playing");
	songinfo.put("etime", "0");
	songinfo.put("tottime", "9999");
	Log.i(MS_constants.LOG_TAG, "Soap Messaging Service Started");
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
	Bundle incoming = intent.getExtras();
	if (incoming != null) {
	    try {
		URL_EXT = incoming.get("SETTING_URL_EXTERNAL").toString();
		// URL = "http://173.3.14.224:500";
		URL_INT = incoming.get("SETTING_URL_INTERNAL").toString();
		HOSTNAME = incoming.get("SETTING_SOURCENAME").toString();
		DESTHOSTNAME = incoming.get("SETTING_DESTNAME").toString();
		INT_NETWORK_NAME = incoming.get("SETTING_INTERNAL_NETWORK_NAME").toString();
		EXT_DELAY = incoming.getInt("SETTING_EXTERNAL_DELAY");
		INT_DELAY = incoming.getInt("SETTING_INTERNAL_DELAY");
		boolean ktornot = incoming.getBoolean("SETTING_KTORRENTNOTIFICATION");
		set_ktorrent_notifications(ktornot);
	    } catch (Exception e) {
		Log.w(MS_constants.LOG_TAG, "onRebind Error");
		Log.w(MS_constants.LOG_TAG, e);
	    }
	}
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
	    Log.v(MS_constants.LOG_TAG, "Already running (you should see this message");
	}
	Log.d(MS_constants.LOG_TAG, "onRebind: msserivce");

    }

    @Override
    public IBinder onBind(Intent intent) {
	Log.d(MS_constants.LOG_TAG, "OnBind: msserivce");
	return interfaceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
	Log.d(MS_constants.LOG_TAG, "onUnbind: msservice");
	/*
	 * whence we unbind, we disconnect from Rhythmbox Streaming NOTE: we
	 * don't ask KTorrent to stop, we only do that on destroy
	 */
	registerDevice(false);
	running = false;
	return true;
    }

    @Override
    public void onCreate() {
	super.onCreate();
	Log.d(MS_constants.LOG_TAG, "onCreate: msservice");
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	Log.d(MS_constants.LOG_TAG, "onDestroy: msservice");
    }

    protected String get_host_names() {
	return "Tomoya|PROTOCOL17|something|somethingelse|onemoretime";
    }

    protected String get_root_value(String hn) {
	try {
	    SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_GETVIDEOPATH);
	    request.addProperty("host", hn);
	    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	    envelope.setOutputSoapObject(request);
	    HttpTransportSE androidHttpTransport = get_transport();

	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);

	    SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
	    /* We get the result */
	    return resultsRequestSOAP.getProperty("Result").toString();
	} catch (Exception e) {
	    return null;
	}
    }

    /* These are when you call backend.method, these are the methods */
    private final backendservice.Stub interfaceBinder = new backendservice.Stub() {
	/* We send the command to soap */
	public boolean sendCmd(String cmd, String cmdText) {
	    return sendCmdToSoap(cmd, cmdText);
	}

	/* We set a notificaion, */
	public boolean setNotification(String tickerString, String notificationTitle,
		String noticicationText) {
	    if (setStatusNotification(tickerString, notificationTitle, noticicationText) == null) {
		return false;
	    }
	    return true;
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

	public String getRootValue(String hn) {
	    return get_root_value(hn);
	}

	public String getHostNames() {
	    return get_host_names();
	}

	public void UpdateSongInfo_Once() {
	    getSongInfo();
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
	    Log.e(MS_constants.LOG_TAG, "set_ktorrent_notifications:Something Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return false;
	}
    }

    private boolean getSongInfo() {
	/*
	 * We encase the entire fucntion in a try/catch so that if the soap
	 * fails or we get illegal chaircters it does not kill the main program
	 */
	try {
	    Log.d(MS_constants.LOG_TAG, "SongInfo:Starting Song Info");
	    /* Create a SOAP package using host name */
	    SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_GETINFO);
	    request.addProperty("host", HOSTNAME);
	    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	    envelope.setOutputSoapObject(request);
	    HttpTransportSE androidHttpTransport = get_transport();
	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
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
	    Log.e(MS_constants.LOG_TAG, "SongInfo:Something Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return false;
	}
	return true;
    }

    private boolean registerForMessages(String active) {
	/*
	 * Creating the SOAP envelope using the registerMessage SOAP command
	 */
	SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_REGISTERMESSAGES);
	request.addProperty("shost", HOSTNAME);
	request.addProperty("dhost", DESTHOSTNAME);
	request.addProperty("active", active);
	SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	envelope.setOutputSoapObject(request);
	HttpTransportSE androidHttpTransport = get_transport();
	/* Added so that if the SOAP servers crash, we don't crash this remote */
	try {
	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
	    return true;
	} catch (Exception e) {

	    Log.e(MS_constants.LOG_TAG, "registerForMessages: Http Transport Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return false;
	}
    }

    private boolean registerDevice(boolean state) {
	/*
	 * Creating the SOAP envelope using the registerMessage SOAP command
	 */
	state = true;
	SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_REGISTERACTIVEDEVICE);
	request.addProperty("host", HOSTNAME);
	request.addProperty("onoff", state);
	SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	envelope.setOutputSoapObject(request);
	HttpTransportSE androidHttpTransport = get_transport();
	/* Added so that if the SOAP servers crash, we don't crash this remote */
	try {
	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
	    return true;
	} catch (Exception e) {

	    Log.e(MS_constants.LOG_TAG, "registerDevice: Http Transport Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
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
	    Log.e(MS_constants.LOG_TAG, "STRB DEPRECIATED");
	    return registerDevice(true);
	} else if (cmd == "SPRB") {
	    Log.e(MS_constants.LOG_TAG, "SPRB DEPRECIATED");
	    return registerDevice(false);
	}
	String destination = DESTHOSTNAME;
	SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_SENDCMD);
	request.addProperty("cmd", cmd);
	request.addProperty("txt", cmdTxt);
	request.addProperty("shost", HOSTNAME);
	request.addProperty("dhost", destination);

	SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	envelope.setOutputSoapObject(request);
	HttpTransportSE androidHttpTransport = get_transport();
	/* Added so that if the SOAP servers crash, we don't crash this remote */
	try {
	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
	} catch (Exception e) {

	    Log.e(MS_constants.LOG_TAG, "sendCmdToSoap: Http Transport Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
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
	SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_SENDCMD);
	request.addProperty("cmd", cmd);
	request.addProperty("txt", cmdTxt);
	request.addProperty("shost", HOSTNAME);
	request.addProperty("dhost", destination);

	SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	envelope.setOutputSoapObject(request);
	HttpTransportSE androidHttpTransport = get_transport();
	/* Added so that if the SOAP servers crash, we don't crash this remote */
	try {
	    androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
	} catch (Exception e) {

	    Log.e(MS_constants.LOG_TAG, "sendCmdToSoap: Http Transport Failed");
	    Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return false;
	}
	return true;
    }

    private HttpTransportSE get_transport() {
	Context f_context = getApplicationContext();
	NetworkInfo networkinfo = (NetworkInfo) ((ConnectivityManager) f_context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
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

    private String[] setStatusNotification(String tickerString, String notificationTitle,
	    String noticicationText) {
	/*
	 * We get three String Varables representing the three Strings we need
	 * to set to use Notification Manager
	 */
	try {
	    String Name = "";
	    String epsName = "";
	    String epsNumber = "";
	    try {
		String[] filename = notificationTitle.split("\\/")[notificationTitle.split("\\/").length - 1].split("\\.");
		Name = filename[0].replace("_", " ");
		epsName = filename[2].replace("_", " ");
		epsNumber = filename[1];
	    } catch (Exception e) {
		String[] filename = notificationTitle.split("\\/")[notificationTitle.split("\\/").length - 1].split("\\[");
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
	    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    /* Here we set the notification icon equal to the program icon */
	    int icon = R.drawable.icon;
	    /* Notification Manager uses CharSequence instead of Strings */
	    String[] tickerTextTemp = tickerString.split("\\/");
	    CharSequence tickerText = tickerTextTemp[tickerTextTemp.length - 1];
	    CharSequence contentTitle = notificationTitle.split("\\/")[notificationTitle.split("\\/").length - 1];
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
	    Context context = getApplicationContext();

	    /**/
	    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(this, MS_show_list.class), 0);
	    notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	    notification.defaults |= Notification.DEFAULT_ALL;
	    mNotificationManager.cancel(MS_constants.NOTIFICATION_ID);
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e1) {
		e1.printStackTrace();
	    }
	    mNotificationManager.notify(MS_constants.NOTIFICATION_ID, notification);
	    currentNotifcationIDs.add(MS_constants.NOTIFICATION_ID);
	    // NOTIFICATION_ID++;
	    numberOfNotifications++;
	    String[] temp = { Name, epsNumber, epsName, loc };
	    return temp;
	} catch (Exception e) {
	    Log.e(MS_constants.LOG_TAG, "SetStatusNotification:" + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	    return null;
	}
    }

    @SuppressWarnings("unused")
    private void passDataToShowWindow(String Name, String SeasonNumber, String EpsName,
	    String Location) {
	try {
	    mOpenHelper.insertShow(Name,EpsName,SeasonNumber,Location);
	} catch (Exception e) {
	    Log.e(MS_constants.LOG_TAG, "Could not insert " + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	}
    }

    private void passDataToShowWindow(List<String[]> shows) {
	try {
	    	for (int i = 0; i < shows.size(); i++) {
		    String[] curShow = shows.get(i);
		    mOpenHelper.insertShow(curShow[0],curShow[2],curShow[1],curShow[3]);
		}
		} catch (Exception e) {
	    Log.e(MS_constants.LOG_TAG, "Could not insert " + e.getMessage());
	    Log.e(MS_constants.LOG_TAG, "", e);
	}
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
	long timeSinceCheckedMessages = System.currentTimeMillis() - checkInterval - 100000L;

	public void run() {
	    long sleep = 1000L;
	    Context f_context = getApplicationContext();
	    NetworkInfo info = (NetworkInfo) ((ConnectivityManager) f_context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
	    String SSID = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
	    if (running) {
		if (info != null && info.isConnected()) {
		    int itype = info.getType();

		    sleep = (long) EXT_DELAY;
		    if ((SSID != null) && ((itype == 1) && (SSID.compareTo(INT_NETWORK_NAME) == 0))) {
			sleep = (long) INT_DELAY;
		    }
		    if (update) {
			getSongInfo();
		    }
		}

		if (System.currentTimeMillis() - timeSinceCheckedMessages > checkInterval) {
		    timeSinceCheckedMessages = System.currentTimeMillis();
		    checkMessages();
		}
		serviceHandler.postDelayed(this, sleep);
	    } else {
		Log.i(MS_constants.LOG_TAG, "Exiting runnable");
	    }
	}

	private boolean checkMessages() {
	    Log.i(MS_constants.LOG_TAG, "checking Messages within runnable");
	    /*
	     * This function send a command to the SOAP method sendCmd, which
	     * checks for messages sitting on the SOAP server's Database
	     */
	    SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_GETCMD);
	    request.addProperty("host", HOSTNAME);

	    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
	    envelope.setOutputSoapObject(request);
	    HttpTransportSE androidHttpTransport = get_transport();
	    /*
	     * Added so that if the SOAP servers crash, we don't crash this
	     * remote
	     */
	    try {
		androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
	    } catch (Exception e) {

		Log.e(MS_constants.LOG_TAG, "Check Messages: Http Transport Failed");
		Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
		Log.e(MS_constants.LOG_TAG, "", e);
		return false;
	    }
	    /*
	     * Here we pull the return results from the incoming envelope and
	     * parses it so we get the full information
	     */
	    String result;
	    int end;
	    try {
		SoapObject resultsRequestSOAP = (SoapObject) envelope.bodyIn;
		result = resultsRequestSOAP.getProperty("Result").toString();
		end = result.length() - 1;
	    } catch (Exception e) {
		Log.e(MS_constants.LOG_TAG, "Check Messages:Soap Failed");
		Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
		Log.e(MS_constants.LOG_TAG, "", e);
		return false;
	    }
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
	    List<String[]> shows = new ArrayList<String[]>();
	    for (int j = 0; j < info.size(); j++) {
		String[] infoString = info.get(j).split("; ");
		Log.i(MS_constants.LOG_TAG, "Info:" + info.get(j));
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
		String[] curShow = new String[4];
		if (cmd.equals("TMSG")) {
		    String[] cmdTemp = cmdTxt.split("\\|");
		    Log.i(MS_constants.LOG_TAG, cmdTemp.toString());
		    Log.i(MS_constants.LOG_TAG, Integer.toString(cmdTemp.length));
		    if (cmdTemp.length == 3) {
			curShow = setStatusNotification(cmdTemp[0], cmdTemp[1], cmdTemp[2]);
			Log.d(MS_constants.LOG_TAG, "curShow:" + curShow);
		    }
		} else if (cmd.equals("ADDS")) {
		    String[] cmdTemp = cmdTxt.split("\\|");
		    Log.v(MS_constants.LOG_TAG, cmdTxt);
		    if (cmdTemp.length == 4) {
			curShow[0] = cmdTemp[0];
			curShow[1] = cmdTemp[1];
			curShow[2] = cmdTemp[2];
			curShow[3] = cmdTemp[3];

		    }
		} else {
		    Log.w(MS_constants.LOG_TAG, "Commad:" + cmd + " Not supported");
		}
		if (curShow != null) {
		    shows.add(curShow);
		}

	    }
	    passDataToShowWindow(shows);
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
		SoapObject request = new SoapObject(MS_constants.NAMESPACE, MS_constants.METHOD_NAME_GETINFO);
		request.addProperty("host", HOSTNAME);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		HttpTransportSE androidHttpTransport = get_transport();
		androidHttpTransport.call(MS_constants.SOAP_ACTION, envelope);
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
		Log.e(MS_constants.LOG_TAG, "SongInfo:Something Failed");
		Log.e(MS_constants.LOG_TAG, "" + e.getMessage());
		Log.e(MS_constants.LOG_TAG, "", e);
		return false;
	    }
	    return true;
	}
    }

}
