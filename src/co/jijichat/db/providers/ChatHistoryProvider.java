/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package co.jijichat.db.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tigase.jaxmpp.core.client.BareJID;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.utils.MessageHelper;

public class ChatHistoryProvider extends ContentProvider {

	public static final String AUTHORITY = "co.jijichat.db.providers.ChatHistoryProvider";

	private static final int CHAT_URI_INDICATOR = 1;

	protected static final int CHAT_ITEM_URI_INDICATOR = 2;

	private static final int ROOM_URI_INDICATOR = 6;

	private static final int CHAT_RECEIPT_URI_INDICATOR = 4;

	public static final String CHAT_URI = "content://" + AUTHORITY + "/chat";

	// ADDED
	public static final String ROOM_URI = "content://" + AUTHORITY + "/room";

	public static final String CONFIRM_RECEIVING_URI = "content://" + AUTHORITY
			+ "/confirm";

	public static final String CONFIRM_MEDIA_DOWNLOAD_URI = "content://"
			+ AUTHORITY + "/media_download";

	public static final String UNSENT_MESSAGES_URI = "content://" + AUTHORITY
			+ "/unsent";

	protected static final int UNSENT_MESSAGES_URI_INDICATOR = 5;

	private static final boolean DEBUG = false;

	private static final String TAG = "ChatHistroyProvider";

	private final static Map<String, String> roomChatHistoryProjectionMap = new HashMap<String, String>() {

		private static final long serialVersionUID = 1L;
		{
			put(ChatTableMetaData.FIELD_BODY, ChatTableMetaData.TABLE_NAME
					+ "." + ChatTableMetaData.FIELD_BODY);
			put(ChatTableMetaData.FIELD_ID, ChatTableMetaData.TABLE_NAME + "."
					+ ChatTableMetaData.FIELD_ID);
			put(ChatTableMetaData.FIELD_ACCOUNT, ChatTableMetaData.TABLE_NAME
					+ "." + ChatTableMetaData.FIELD_ACCOUNT);
			put(ChatTableMetaData.FIELD_MESSAGE_ID,
					ChatTableMetaData.TABLE_NAME + "."
							+ ChatTableMetaData.FIELD_MESSAGE_ID);
			put(ChatTableMetaData.FIELD_JID, ChatTableMetaData.TABLE_NAME + "."
					+ ChatTableMetaData.FIELD_JID);
			put(ChatTableMetaData.FIELD_STATE, ChatTableMetaData.TABLE_NAME
					+ "." + ChatTableMetaData.FIELD_STATE);
			put(ChatTableMetaData.FIELD_CHAT_ROOM_ID,
					ChatTableMetaData.TABLE_NAME + "."
							+ ChatTableMetaData.FIELD_CHAT_ROOM_ID);
			put(ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.TABLE_NAME
					+ "." + ChatTableMetaData.FIELD_TIMESTAMP);
			put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME,
					ChatTableMetaData.TABLE_NAME + "."
							+ ChatTableMetaData.FIELD_AUTHOR_NICKNAME);
		}
	};

	private MessengerDatabaseHelper dbHelper;

	public ChatHistoryProvider() {
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (match(uri)) {
		case ROOM_URI_INDICATOR: {
			db.delete(ChatTableMetaData.TABLE_NAME, ChatTableMetaData.FIELD_ID
					+ "=?", new String[] { uri.getPathSegments().get(2) });
			MessageHelper.clearMessage(BareJID.bareJIDInstance(uri
					.getPathSegments().get(1)));
			getContext().getContentResolver().notifyChange(uri, null);
			break;
		}
		case CHAT_URI_INDICATOR: {
			final String roomJid = uri.getPathSegments().get(1);
			db.delete(ChatTableMetaData.TABLE_NAME, ChatTableMetaData.FIELD_JID
					+ "=?", new String[] { roomJid });
			MessageHelper.clearMessage(BareJID.bareJIDInstance(roomJid));
			JijichatMobileMessengerActivity.mUnreadCounters.remove(roomJid);
			// getContext().getContentResolver().notifyChange(uri, null);
			Uri insertedItem = Uri.parse(MucProvider.CONTENT_URI + "/"
					+ roomJid);
			getContext().getContentResolver().notifyChange(insertedItem, null);
			break;
		}
		case CHAT_ITEM_URI_INDICATOR: {
			db.delete(ChatTableMetaData.TABLE_NAME, ChatTableMetaData.FIELD_ID
					+ "=?", new String[] { uri.getPathSegments().get(2) });
			getContext().getContentResolver().notifyChange(uri, null);
			break;
		}

		default: {
			throw new IllegalArgumentException("Unknown URI ");
		}
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (match(uri)) {
		case CHAT_URI_INDICATOR:
			return ChatTableMetaData.CONTENT_TYPE;
		case CHAT_ITEM_URI_INDICATOR:
			return ChatTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		final long rowId;
		switch (match(uri)) {
		case CHAT_URI_INDICATOR:
			rowId = db.insert(ChatTableMetaData.TABLE_NAME,
					ChatTableMetaData.FIELD_JID, values);
			if (rowId > 0) {
				MessageHelper.setMessage(
						values.getAsString(ChatTableMetaData.FIELD_JID),
						values.getAsString(ChatTableMetaData.FIELD_BODY));

				Uri insertedItem = ContentUris
						.withAppendedId(
								Uri.parse(ChatHistoryProvider.CHAT_URI
										+ "/"
										+ values.getAsString(ChatTableMetaData.FIELD_JID)),
								rowId);
				getContext().getContentResolver().notifyChange(insertedItem,
						null);

				Uri insertedItemChat = ContentUris.withAppendedId(Uri
						.parse(MucProvider.CONTENT_URI), values
						.getAsInteger(ChatTableMetaData.FIELD_CHAT_ROOM_ID));
				getContext().getContentResolver().notifyChange(
						insertedItemChat, null);

				return insertedItem;
			}

			break;
		case ROOM_URI_INDICATOR:
			rowId = db.insert(ChatTableMetaData.TABLE_NAME,
					ChatTableMetaData.FIELD_JID, values);
			if (rowId > 0) {
				MessageHelper.setMessage(
						values.getAsString(ChatTableMetaData.FIELD_JID),
						values.getAsString(ChatTableMetaData.FIELD_BODY));

				Uri insertedItem = ContentUris
						.withAppendedId(
								Uri.parse(ChatHistoryProvider.ROOM_URI
										+ "/"
										+ values.getAsString(ChatTableMetaData.FIELD_JID)),
								rowId);
				getContext().getContentResolver().notifyChange(insertedItem,
						null);

				// Uri insertedItemChat = ContentUris.withAppendedId(
				// Uri.parse(OpenChatsProvider.CONTENT_URI),
				// values.getAsInteger(ChatTableMetaData.FIELD_CHAT_ID));
				// getContext().getContentResolver().notifyChange(
				// insertedItemChat, null);

				return insertedItem;
			}

			break;
		default:
			throw new IllegalArgumentException("Unsupported URI " + uri);
		}

		return null;
	}

	private int match(Uri uri) {
		// /chat/${JID}
		// /chat/${JID}/#

		List<String> l = uri.getPathSegments();

		if (l.get(0).equals("room"))
			return ROOM_URI_INDICATOR;
		else if (l.get(0).equals("confirm"))
			return CHAT_RECEIPT_URI_INDICATOR;
		else if (l.get(0).equals("unsent"))
			return UNSENT_MESSAGES_URI_INDICATOR;
		else if (!l.get(0).equals("chat"))
			return 0;

		else if (l.size() == 2)
			return CHAT_URI_INDICATOR;
		else if (l.size() == 3)
			return CHAT_ITEM_URI_INDICATOR;
		else
			return 0;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch (match(uri)) {

		case CHAT_URI_INDICATOR: {
			final Map<String, String> x = new HashMap<String, String>(
					roomChatHistoryProjectionMap);
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(x);
			String jid = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.TABLE_NAME + "."
					+ ChatTableMetaData.FIELD_JID + "='" + jid + "'");
			break;
		}
		case ROOM_URI_INDICATOR: {
			final Map<String, String> x = new HashMap<String, String>(
					roomChatHistoryProjectionMap);
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(x);
			String jid = uri.getPathSegments().get(1);
			qb.appendWhere(ChatTableMetaData.TABLE_NAME + "."
					+ ChatTableMetaData.FIELD_JID + "='" + jid + "'");
			break;
		}
		case CHAT_ITEM_URI_INDICATOR: {
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(roomChatHistoryProjectionMap);
			List<String> segments = uri.getPathSegments();
			String id = segments.get(2);
			qb.appendWhere(ChatTableMetaData.FIELD_ID + "='" + id + "'");
			break;
		}
		case UNSENT_MESSAGES_URI_INDICATOR: {
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			qb.setProjectionMap(roomChatHistoryProjectionMap);
			qb.appendWhere(ChatTableMetaData.FIELD_STATE + "="
					+ ChatTableMetaData.STATE_OUT_NOT_SENT);
			break;
		}
		case 0: {
			qb.setTables(ChatTableMetaData.TABLE_NAME);
			// qb.setProjectionMap(chatHistoryProjectionMap);
			break;
		}
		default:
			throw new IllegalArgumentException("Unknown URI '"
					+ (uri != null ? uri.toString() : "null") + "'");
		}
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c;
		if (sortOrder != null) {
			c = qb.query(db, projection, selection, selectionArgs, null, null,
					sortOrder);
		} else {
			c = qb.query(db, projection, selection, selectionArgs, null, null,
					ChatTableMetaData.TABLE_NAME + "."
							+ ChatTableMetaData.FIELD_TIMESTAMP + " ASC, "
							+ ChatTableMetaData.TABLE_NAME + "."
							+ ChatTableMetaData.FIELD_ID + " ASC");
		}

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (DEBUG) {
			Log.d(TAG, "update(), URI:  " + uri.toString() + " matched to "
					+ match(uri));
		}
		if (match(uri) == ROOM_URI_INDICATOR) {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			String jid = uri.getLastPathSegment();

			int changed = db.update(ChatTableMetaData.TABLE_NAME, values,
					ChatTableMetaData.FIELD_JID + "='" + jid + "' AND "
							+ ChatTableMetaData.FIELD_STATE + "="
							+ ChatTableMetaData.STATE_INCOMING_UNREAD, null);

			if (changed > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
			}

			// Log.d(TAG, "changed: " + changed + " for jid: " + jid);

			return changed;
		} else if (match(uri) == CHAT_RECEIPT_URI_INDICATOR) {
			final SQLiteDatabase db = dbHelper.getWritableDatabase();

			final String jid = uri.getLastPathSegment();
			final String messageId = uri.getQueryParameter("id");

			int changed = 0;

			changed = db.update(ChatTableMetaData.TABLE_NAME, values,
					ChatTableMetaData.FIELD_JID + "='" + jid + "' AND "
							+ ChatTableMetaData.FIELD_STATE + "= "
							+ ChatTableMetaData.STATE_OUT_NOT_SENT + " AND "
							+ ChatTableMetaData.FIELD_MESSAGE_ID + " = '"
							+ messageId + "'", null);

			if (changed > 0) {
				Uri u = Uri.parse(ROOM_URI + "/" + jid);
				getContext().getContentResolver().notifyChange(u, null);
			}
			return changed;
		} else {
			throw new IllegalArgumentException("Unknown URI ");
		}

	}

}
