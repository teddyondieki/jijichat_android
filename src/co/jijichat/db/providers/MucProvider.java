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

import tigase.jaxmpp.core.client.BareJID;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import co.jijichat.db.MucTableMetaData;
import co.jijichat.utils.AvatarHelper;
import co.jijichat.utils.RosterNameHelper;

public class MucProvider extends ContentProvider {

	public static final String AUTHORITY = "co.jijichat.db.providers.MucProvider";

	public static final String CONTENT_URI = "content://" + AUTHORITY + "/muc";

	protected static final int MUC_ITEM_URI_INDICATOR = 2;

	protected static final int MUC_URI_INDICATOR = 1;

	private MessengerDatabaseHelper dbHelper;

	protected final UriMatcher uriMatcher;

	public MucProvider() {
		this.uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		this.uriMatcher.addURI(AUTHORITY, "muc", MUC_URI_INDICATOR);
		this.uriMatcher.addURI(AUTHORITY, "muc/*", MUC_ITEM_URI_INDICATOR);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case MUC_URI_INDICATOR:
			return MucTableMetaData.CONTENT_TYPE;
		case MUC_ITEM_URI_INDICATOR:
			return MucTableMetaData.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new MessengerDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			final String[] selectionArgs, String sortOrder) {
		Cursor c;
		switch (uriMatcher.match(uri)) {
		case MUC_URI_INDICATOR:

			c = dbHelper.getReadableDatabase()
					.query(MucTableMetaData.TABLE_NAME,
							new String[] { MucTableMetaData.FIELD_ID,
									MucTableMetaData.FIELD_ROOM_JID,
									MucTableMetaData.FIELD_ROOM_NAME,
									MucTableMetaData.FIELD_ROOM_DESCRIPTION },
							null, null, null, null,
							MucTableMetaData.FIELD_ROOM_NAME + " ASC");

			break;
		case MUC_ITEM_URI_INDICATOR:
			c = dbHelper.getReadableDatabase()
					.query(MucTableMetaData.TABLE_NAME,
							new String[] { MucTableMetaData.FIELD_ID,
									MucTableMetaData.FIELD_ROOM_JID,
									MucTableMetaData.FIELD_ROOM_NAME },
							MucTableMetaData.FIELD_ROOM_JID + "=?",
							new String[] { uri.getLastPathSegment() }, null,
							null, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		switch (uriMatcher.match(uri)) {
		case MUC_ITEM_URI_INDICATOR: {

			Log.d("Updated", uri.getLastPathSegment());

			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			final BareJID jid = BareJID.bareJIDInstance(uri
					.getLastPathSegment());

			int changed = 0;
			changed = db.update(MucTableMetaData.TABLE_NAME, values,
					MucTableMetaData.FIELD_ROOM_JID + " = '" + jid.toString()
							+ "'", null);

			if (changed > 0) {
				AvatarHelper.clearAvatar(jid);
				RosterNameHelper.updateName(jid,
						values.getAsString(MucTableMetaData.FIELD_ROOM_NAME));
				RosterNameHelper.updateDesc(jid, values
						.getAsString(MucTableMetaData.FIELD_ROOM_DESCRIPTION));
				Uri u = Uri.parse(CONTENT_URI + "/" + jid);
				getContext().getContentResolver().notifyChange(u, null);
			}
			break;
		}
		default: {
			throw new RuntimeException("There is nothing to update! uri=" + uri);

		}
		}
		return 0;
	}

}
