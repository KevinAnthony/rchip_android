package com.nosideracing.rchipremote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private static final String DATABASE_NAME = "msdb.db";
	private static final int DATABASE_VERSION = 2;
	private static final String TABLE_NAME_SL = "show_list";

	private SQLiteDatabase db;

	public void deleteOneSL(int id) {
		Log.d(Consts.LOG_TAG, "Deleting row #" + id + " from " + TABLE_NAME_SL);
		db = this.getWritableDatabase();
		db.execSQL("Delete from " + TABLE_NAME_SL + " where id = " + id);
		db.close();
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
		Log.d(Consts.LOG_TAG,"Upgrading Databased oldVersion:"+oldVersion+"-> newVersion:"+newVersion);
		if (newVersion > 1){
			Log.v(Consts.LOG_TAG,"Renaming Table ms_show_list -> " + TABLE_NAME_SL);
			db.execSQL("ALTER TABLE ms_show_list RENAME TO "+ TABLE_NAME_SL +";");
		}
	}
}
