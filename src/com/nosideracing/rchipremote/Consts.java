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

public class Consts {

	protected static final int RC_REMOTE = 0x0030;
	protected static final int RC_MUSIC = 0x0031;
	protected static final int RC_SHOW = 0x0032;
	protected static final int RC_WATCHMOVE = 0x0033;
	protected static final int RC_SHOW_LIST = 0x0034;
	protected static final int RC_AUTOMATION = 0x0035;

	protected static final int PREFS_UPDATED = 0x0041;
	protected static final int UPDATEGUI = 0x0042;
	protected static final int QUITREMOTE = 0x0043;
	protected static final int REMOVESHOW = 0x0044;

	protected static final int START_MUSIC = 0x0051;
	protected static final int START_SHOW_LIST = 0x0052;
	protected static final int START_UPCOMING_SHOW_LIST = 0x0053;
	protected static final int START_AUTOMATION = 0x0054;
	
	protected static final String MUSIC_STOP = "STOP";
	protected static final String MUSIC_NEXT = "NEXT";
	protected static final String MUSIC_BACK = "BACK";
	protected static final String MUSIC_PLAYPAUSE_TOGGLE = "PLAY";
	
	protected static final String VIDEO_FULLSCREEN_TOGGLE = "FULLONSM";
	protected static final String VIDEO_OPEN = "OPENSM";
	protected static final String VIDEO_STOP = "STOPSM";
	protected static final String VIDEO_PLAY = "PLAYSM";
	protected static final String VIDEO_PAUSE = "PAUSESM";
	protected static final String VIDEO_SKIP_FOWARD = "SKIPFSM";
	protected static final String VIDEO_SKIP_BACKWARDS = "SKIPBSM";
	protected static final String VIDEO_MUTE = "MUTESM";
	protected static final String VIDEO_QUIT = "QUITSM";
	
	protected static final String PREF_DAEMONS = "STORED_DAEMONS";
	protected static final String PREF_USERNAME = "username";
	protected static final String PREF_PASSWORD = "password";
	protected static final String PREF_DAEMON = "serverhostname";
	protected static final String PREF_URL = "serverurl";
	
	protected static final int http_timeout = 10000;

	protected static final String LOG_TAG = "rchip";

}
