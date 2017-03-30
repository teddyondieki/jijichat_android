package co.jijichat.db.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.MucTableMetaData;

public class MessengerDatabaseHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "mobile_messenger.db";

	public static final Integer DATABASE_VERSION = 5;

	private static final String TAG = "jijichat";

	public MessengerDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql;

		sql = "CREATE TABLE " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += ChatTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += ChatTableMetaData.FIELD_JID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_AUTHOR_NICKNAME + " TEXT, ";
		sql += ChatTableMetaData.FIELD_TIMESTAMP + " DATETIME, ";
		sql += ChatTableMetaData.FIELD_BODY + " TEXT, ";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID + " TEXT, ";
		sql += ChatTableMetaData.FIELD_CHAT_ROOM_ID + " INTEGER, ";
		sql += ChatTableMetaData.FIELD_STATE + " INTEGER";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += ChatTableMetaData.INDEX_JID;
		sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_JID;
		sql += ")";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID;
		sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
		sql += ChatTableMetaData.FIELD_MESSAGE_ID;
		sql += ")";
		db.execSQL(sql);

		sql = "CREATE TABLE " + MucTableMetaData.TABLE_NAME + " (";
		sql += MucTableMetaData.FIELD_ID + " INTEGER PRIMARY KEY, ";
		sql += MucTableMetaData.FIELD_ACCOUNT + " TEXT, ";
		sql += MucTableMetaData.FIELD_ROOM_JID + " TEXT, ";
		sql += MucTableMetaData.FIELD_ROOM_NAME + " TEXT, ";
		sql += MucTableMetaData.FIELD_ROOM_DESCRIPTION + " TEXT, ";
		sql += MucTableMetaData.FIELD_TIMESTAMP + " DATETIME";
		sql += ");";
		db.execSQL(sql);

		sql = "CREATE INDEX IF NOT EXISTS ";
		sql += MucTableMetaData.INDEX_JID;
		sql += " ON " + MucTableMetaData.TABLE_NAME + " (";
		sql += MucTableMetaData.FIELD_ROOM_JID;
		sql += ")";
		db.execSQL(sql);

	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Database upgrade from version " + oldVersion + " to "
				+ newVersion);

		if (oldVersion < 5) {
			String sql = "ALTER TABLE " + ChatTableMetaData.TABLE_NAME
					+ " ADD COLUMN " + ChatTableMetaData.FIELD_MESSAGE_ID
					+ " TEXT ";
			db.execSQL(sql);

			sql = "CREATE INDEX IF NOT EXISTS ";
			sql += ChatTableMetaData.FIELD_MESSAGE_ID;
			sql += " ON " + ChatTableMetaData.TABLE_NAME + " (";
			sql += ChatTableMetaData.FIELD_MESSAGE_ID;
			sql += ")";
			db.execSQL(sql);
		}
	}
}
