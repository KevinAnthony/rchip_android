package com.nosideracing.rchipremote;

public class Consts {
	
	protected static final int RC_REMOTE = 0x0030;
	protected static final int RC_MUSIC = 0x0031;
	protected static final int RC_SHOW = 0x0032;
	protected static final int RC_WATCHMOVE= 0x0033;
	
	protected static final int PREFS_UPDATED = 0x0041;
	protected static final int UPDATEGUI = 0x0042;
	protected static final int QUITREMOTE = 0x0043;
	protected static final int REMOVESHOW = 0x0044;
	protected static final int NOTIFICATION_ID = 0x0081;
	
	protected static final String SOAP_ACTION = "getSongInfo";
	protected static final String METHOD_NAME_GETINFO = "getSongInfo";
	protected static final String METHOD_NAME_SENDCMD = "sendCmd";
	protected static final String METHOD_NAME_GETCMD = "getCmd";
	protected static final String METHOD_NAME_REGISTERACTIVEDEVICE = "registerActiveDevice";
	protected static final String METHOD_NAME_REGISTERMESSAGES = "registerMessages";
	protected static final String METHOD_NAME_GETVIDEOPATH = "getVideoPath";
	protected static final String METHOD_NAME_GETDAEMONS = "getDaemons";
	protected static final String NAMESPACE = "http://192.168.1.3/";
	
	protected static final long DELAY_LENGTH_ACTIVE = 30000;
	protected static final long DELAY_LENGTH_INACTIVE = 300000;
	
	protected static String LOG_TAG = "rchip";
		
}
