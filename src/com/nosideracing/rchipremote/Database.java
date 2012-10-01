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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.nosideracing.rchipremote.Consts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private static final String DATABASE_NAME = "msdb.db";
	private static final int DATABASE_VERSION = 2;
	private static final String TABLE_NAME_SL = "show_list";

	private SQLiteDatabase db;

	public void deleteOneEpisode(String ShowName, String EpisodeName,
			String EpisodeNumber) {
		db = this.getWritableDatabase();
		Cursor C = db.query(TABLE_NAME_SL, new String[] { "ID" },
				"ShowName = ? and EpisodeNumber = ?", new String[] { ShowName,
						EpisodeNumber }, null, null, null);
		Log.v(Consts.LOG_TAG, "Deleting " + ShowName + " - " + EpisodeNumber
				+ ":" + EpisodeName + ":");
		Log.v(Consts.LOG_TAG, "Total of " + C.getCount() + " Rows Deleted");
		if (C.moveToFirst()) {
			do {
				deleteOneSL(C.getInt(0), db);
			} while (C.moveToNext());
		}
		db.close();
	}

	public void deleteOneSL(int id) {
		Log.d(Consts.LOG_TAG, "Deleting row #" + id + " from " + TABLE_NAME_SL);
		db = this.getWritableDatabase();
		db.execSQL("Delete from " + TABLE_NAME_SL + " where id = " + id);
		db.close();
	}

	public void deleteOneSL(int id, SQLiteDatabase db) {
		Log.d(Consts.LOG_TAG, "Deleting row #" + id + " from " + TABLE_NAME_SL);
		db.execSQL("Delete from " + TABLE_NAME_SL + " where id = " + id);
	}

	public void deleteAllSL() {
		Log.d(Consts.LOG_TAG, "Deleting all rows from " + TABLE_NAME_SL);
		db = this.getWritableDatabase();
		db.execSQL("Delete from " + TABLE_NAME_SL);
		db.close();
	}

	public String[] getShows() {

		int index = 0;
		db = this.getReadableDatabase();
		Cursor C = db.query(TABLE_NAME_SL, new String[] { "id", "ShowName",
				"EpisodeNumber", "EpisodeName", "Location" }, null, null, null,
				null, "ShowName,EpisodeNUmber");
		Log.v(Consts.LOG_TAG, "Got " + C.getCount() + " Rows from table "
				+ TABLE_NAME_SL);
		String[] retval = new String[C.getCount()];
		if (C.moveToFirst()) {
			do {
				retval[index] = C.getString(0) + "|" + C.getString(1) + "|"
						+ C.getString(2) + "|" + C.getString(3) + "|"
						+ C.getString(4);
				index++;
			} while (C.moveToNext());
		}
		if (C != null && !C.isClosed()) {
			C.close();
		}
		db.close();
		return retval;
	}

	public void insertShow(String ShowName, String EpsName, String EpsNumber,
			String LOC) {
		try {
			db = this.getWritableDatabase();
			Cursor C = db.query(TABLE_NAME_SL, new String[] { "ID" },
					"ShowName = ? and EpisodeNumber = ?", new String[] {
							ShowName, EpsNumber }, null, null, null);
			if (C.getCount() == 0) {
				ContentValues values = new ContentValues();
				values.put("ShowName", ShowName.replace("_", " "));
				values.put("EpisodeNumber", EpsNumber);
				values.put("EpisodeName", EpsName);
				values.put("Location", LOC);
				long rows = db.insert(TABLE_NAME_SL, null, values);
				if (rows < 1) {
					Log.e(Consts.LOG_TAG, "Couldn't insert into database");
				} else {
					Log.v(Consts.LOG_TAG, "Inserted " + rows + " into table "
							+ TABLE_NAME_SL);
				}
			}
		} catch (Exception e) {
			Log.e(Consts.LOG_TAG, "Error in insertShow", e);
		} finally {
			db.close();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE `"
				+ TABLE_NAME_SL
				+ "` ( `ID` INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "  `ShowName` VARCHAR( 64 ) NOT NULL , `EpisodeNumber` VARCHAR( 10 ) NOT NULL ,  "
				+ "`EpisodeName` VARCHAR( 128 ) NOT NULL , `Location` VARCHAR( 1024 ) NOT NULL ,  "
				+ "`Updated` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(Consts.LOG_TAG, "Upgrading Databased oldVersion:" + oldVersion
				+ "-> newVersion:" + newVersion);
		if (newVersion > 1) {
			Log.v(Consts.LOG_TAG, "Renaming Table ms_show_list -> "
					+ TABLE_NAME_SL);
			db.execSQL("ALTER TABLE ms_show_list RENAME TO " + TABLE_NAME_SL
					+ ";");
		}
	}

	public void backupDatabase() throws IOException {
		db = this.getReadableDatabase();
		File root = new File(Environment.getExternalStorageDirectory(), "rchip");
		if (!root.exists()) {
			root.mkdirs();
		}
		File gpxfile = new File(root, "database_Backip.gpx");
		if (gpxfile.exists()) {
			gpxfile.delete();
			gpxfile.createNewFile();
		}
		FileWriter writer = new FileWriter(gpxfile);
		Cursor C = db.query(TABLE_NAME_SL, new String[] { "ShowName",
				"EpisodeNumber", "EpisodeName", "Location", "Updated" }, null,
				null, null, null, null);
		Log.v(Consts.LOG_TAG, "Got " + C.getCount() + " Rows from table "
				+ TABLE_NAME_SL);
		if (C.moveToFirst()) {
			do {
				writer.append(C.getString(0) + "|" + C.getString(1) + "|"
						+ C.getString(2) + "|" + C.getString(3) + "|"
						+ C.getString(4) + "\n");
				writer.flush();

			} while (C.moveToNext());
		}
		writer.flush();
		writer.close();
		if (C != null && !C.isClosed()) {
			C.close();
		}
		db.close();
	}

	public void restoreDatabase() throws IOException {
		db = this.getWritableDatabase();
		File root = new File(Environment.getExternalStorageDirectory(), "rchip");
		if (!root.exists()) {
			// TODO toast here
			return;
		}
		File gpxfile = new File(root, "database_Backip.gpx");
		if (!gpxfile.exists()) {
			// TODO toast here
			return;
		}
		FileReader reader = new FileReader(gpxfile);
		BufferedReader in = new BufferedReader(reader);
		db.delete(TABLE_NAME_SL, null, null);
		String line;
		while ((line = in.readLine()) != null) {
			ContentValues values = new ContentValues();
			Log.d(Consts.LOG_TAG, line);
			String[] line_parsed = line.split("\\|");
			values.put("ShowName", line_parsed[0]);
			values.put("EpisodeNumber", line_parsed[1]);
			values.put("EpisodeName", line_parsed[2]);
			values.put("Location", line_parsed[3]);
			values.put("Updated", line_parsed[4]);
			db.insert(TABLE_NAME_SL, null, values);
		}
		in.close();
		reader.close();
		db.close();
	}

}
