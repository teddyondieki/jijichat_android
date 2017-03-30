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
package co.jijichat.db;

import android.provider.BaseColumns;

public class MucTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.open_chats_item";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.open_chats";

	public static final String FIELD_ACCOUNT = "account";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_ROOM_JID = "room_jid";

	public static final String FIELD_ROOM_NAME = "room_name";

	public static final String FIELD_ROOM_DESCRIPTION = "room_description";

	public static final String FIELD_TIMESTAMP = "timestamp";

	public static final String TABLE_NAME = "open_muc";

	public static final String INDEX_JID = "open_muc_jid_index";

}
