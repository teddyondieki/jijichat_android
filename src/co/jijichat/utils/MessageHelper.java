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
package co.jijichat.utils;

import java.util.HashMap;
import java.util.Map;

import tigase.jaxmpp.core.client.BareJID;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import co.jijichat.R;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;

//import tigase.jaxmpp.R;

public class MessageHelper {

	private static Map<BareJID, String> messageCache;

	private static Context context;
	public static String mPlaceHolderMessage;
	private static final String TAG = "MessageHelper";

	public static void clearMessage(BareJID jid) {
		messageCache.remove(jid);
	}

	public static String getMessage(BareJID jid) {
		String message = messageCache.get(jid);
		if (message == null) {
			message = loadMessage(jid);
		}
		return message;
	}

	public static void setMessage(String bareJid, String message) {
		messageCache.put(BareJID.bareJIDInstance(bareJid), message);
	}

	public static void initialize(Context context_) {
		if (messageCache == null) {
			context = context_;

			messageCache = new HashMap<BareJID, String>();

			mPlaceHolderMessage = context.getResources().getString(
					R.string.default_no_message);
		}
	}

	protected static String loadMessage(BareJID jid) {
		String message = loadMessageFromDB(jid);
		if (message == null) {
			message = mPlaceHolderMessage;
		}
		messageCache.put(jid, message);
		return message;
	}

	protected static String loadMessageFromDB(BareJID jid) {
		String message = null;

		final String[] projection = new String[] { ChatTableMetaData.TABLE_NAME
				+ "." + ChatTableMetaData.FIELD_BODY };

		final String selection = ChatTableMetaData.FIELD_JID + " = '"
				+ jid.toString() + "'";

		Cursor cursor = context.getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI),
				projection,
				selection,
				null,
				ChatTableMetaData.TABLE_NAME + "." + ChatTableMetaData.FIELD_ID
						+ " DESC LIMIT 1");

		try {
			if (cursor.moveToNext()) {
				// we found status in our store
				message = cursor.getString(cursor
						.getColumnIndex(ChatTableMetaData.FIELD_BODY));

			}
		} catch (Exception ex) {
			Log.v(TAG, "exception retrieving status for " + jid.toString(), ex);
		} finally {
			cursor.close();
		}
		return message;
	}
}
