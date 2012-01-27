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

	protected static final int PREFS_UPDATED = 0x0041;
	protected static final int UPDATEGUI = 0x0042;
	protected static final int QUITREMOTE = 0x0043;
	protected static final int REMOVESHOW = 0x0044;
	protected static final int NOTIFICATION_ID = 0x0081;

	protected static final int START_MUSIC = 0x0051;
	protected static final int START_SHOW_LIST = 0x0052;
	protected static final int START_UPCOMING_SHOW_LIST = 0x0053;

	protected static final String SOAP_ACTION = "getSongInfo";
	protected static final String METHOD_NAME_GETINFO = "getSongInfo";
	protected static final String METHOD_NAME_SENDCMD = "sendCmd";
	protected static final String METHOD_NAME_GETCMD = "getCmd";
	protected static final String METHOD_NAME_REGISTERACTIVEDEVICE = "registerActiveDevice";
	protected static final String METHOD_NAME_REGISTERMESSAGES = "registerMessages";
	protected static final String METHOD_NAME_GETVIDEOPATH = "getVideoPath";
	protected static final String METHOD_NAME_GETDAEMONS = "getDaemons";

	protected static String LOG_TAG = "rchip";

}
