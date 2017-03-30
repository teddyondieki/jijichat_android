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

import java.util.Date;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.muc.AbstractRoomsManager;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.MucTableMetaData;
import co.jijichat.utils.AvatarHelper;
import co.jijichat.utils.RosterNameHelper;

public class DBMUCManager extends AbstractRoomsManager {

	private final Context context;

	private final MessengerDatabaseHelper helper;

	public DBMUCManager(Context context) {
		this.context = context;
		this.helper = new MessengerDatabaseHelper(this.context);
	}

	@Override
	protected Room createRoomInstance(final BareJID jid, final String nickname,
			final String password) {
		SQLiteDatabase db = helper.getWritableDatabase();

		String sql = "SELECT " + MucTableMetaData.FIELD_ID + " FROM "
				+ MucTableMetaData.TABLE_NAME + " WHERE "
				+ MucTableMetaData.FIELD_ROOM_JID + " = '" + jid + "'";
		long rowId;

		final Cursor c = db.rawQuery(sql, null);
		try {
			if (c.moveToFirst()) {
				rowId = c.getLong(c.getColumnIndex(MucTableMetaData.FIELD_ID));
			} else {
				final ContentValues values = new ContentValues();
				values.put(MucTableMetaData.FIELD_ROOM_JID, jid.toString());
				values.put(MucTableMetaData.FIELD_ROOM_NAME,
						RosterNameHelper.getName(jid));
				values.put(MucTableMetaData.FIELD_ROOM_DESCRIPTION,
						RosterNameHelper.getDesc(jid));
				values.put(MucTableMetaData.FIELD_TIMESTAMP, 0);
				values.put(MucTableMetaData.FIELD_ACCOUNT, sessionObject
						.getUserBareJid().toString());

				rowId = db.insert(MucTableMetaData.TABLE_NAME, null, values);

			}
		} finally {
			c.close();
		}

		Room room = new Room(rowId, packetWriter, jid, nickname, sessionObject);
		room.setPassword(password);
		room.setObservable(observable);

		long lastMsgTmstmp = 0;
		String sql1 = "SELECT MAX(" + ChatTableMetaData.FIELD_TIMESTAMP
				+ ") FROM " + ChatTableMetaData.TABLE_NAME + " WHERE "
				+ ChatTableMetaData.FIELD_JID + "='" + jid.toString() + "'";
		final Cursor c1 = db.rawQuery(sql1, null);

		try {
			if (c1.moveToNext()) {
				lastMsgTmstmp = c1.getLong(0);
			}
		} finally {
			c1.close();
		}

		room.setLastMessageDate(new Date(lastMsgTmstmp));
		return room;
	}

	@Override
	protected void initialize() {
		super.initialize();
		rooms.clear();
		SQLiteDatabase db = this.helper.getReadableDatabase();
		String sql = "SELECT " + MucTableMetaData.FIELD_ID + ", "
				+ MucTableMetaData.FIELD_ROOM_JID + ", "
				+ MucTableMetaData.FIELD_ROOM_NAME + ", "
				+ MucTableMetaData.FIELD_ROOM_DESCRIPTION + ", "
				+ MucTableMetaData.FIELD_TIMESTAMP + " FROM "
				+ MucTableMetaData.TABLE_NAME;
		final Cursor c = db.rawQuery(sql, null);
		try {
			while (c.moveToNext()) {
				final long id = c.getLong(c
						.getColumnIndex(MucTableMetaData.FIELD_ID));

				final BareJID roomJID = BareJID.bareJIDInstance(c.getString(c
						.getColumnIndex(MucTableMetaData.FIELD_ROOM_JID)));

				final String name = c.getString(c
						.getColumnIndex(MucTableMetaData.FIELD_ROOM_NAME));

				final String desc = c
						.getString(c
								.getColumnIndex(MucTableMetaData.FIELD_ROOM_DESCRIPTION));

				final String nickname = sessionObject
						.getProperty(SessionObject.NICKNAME);

				long lastMsgTmstmp = 0;
				String sql1 = "SELECT MAX(" + ChatTableMetaData.FIELD_TIMESTAMP
						+ ") FROM " + ChatTableMetaData.TABLE_NAME + " WHERE "
						+ ChatTableMetaData.FIELD_JID + "='"
						+ roomJID.toString() + "'";

				final Cursor c1 = db.rawQuery(sql1, null);
				try {
					if (c1.moveToNext()) {
						lastMsgTmstmp = c1.getLong(0);
					}
				} finally {
					c1.close();
				}

				Room room = new Room(id, packetWriter, roomJID, nickname,
						sessionObject);

				room.setObservable(observable);
				if (lastMsgTmstmp != 0)
					room.setLastMessageDate(new Date(lastMsgTmstmp));
				rooms.put(room.getRoomJid(), room);

				RosterNameHelper.updateName(roomJID, name);
				RosterNameHelper.updateDesc(roomJID, desc);
			}
		} finally {
			c.close();
		}
	}

	@Override
	public boolean remove(final Room room) {
		boolean x = super.remove(room);
		if (x) {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.delete(MucTableMetaData.TABLE_NAME, MucTableMetaData.FIELD_ID
					+ "=" + room.getId(), null);
		}
		AvatarHelper.clearAvatar(room.getRoomJid());
		return x;
	}

}
