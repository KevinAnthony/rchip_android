package com.nosideracing.msremote;

interface backendservice {
	boolean sendCmd(in String cmd,in String cmdText);
	boolean setNotification(in String tickerString,in String notificationTitle,in String noticicationText);
	void clearNotifications();
	void startMusicUpdating();
	void stopMusicUpdating();
	String getArtest();
	String getAlbum();
	String getSongName();
	String getTimeElapised();
	String getSongLength();
	int getIsPlaying();
	boolean setKtorrentNotifications(in boolean ktornot);
	String getRootValue(in String hn);
	String getHostNames();
	void UpdateSongInfo_Once();
}