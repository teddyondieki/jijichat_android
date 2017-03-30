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

public class ChatTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.chatitem";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.chat";

	public static final String FIELD_ACCOUNT = "account";

	public static final String FIELD_AUTHOR_NICKNAME = "author_nickname";

	public static final String FIELD_BODY = "body";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_MESSAGE_ID = "message_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_CHAT_ROOM_ID = "chat_room_id";

	/**
	 * <ul>
	 * <li><code>0</code> - incoming, read</li>
	 * <li><code>1</code> - outgoing, not sent</li>
	 * <li><code>2</code> - outgoing, sent</li>
	 * <li><code>3</code> - incoming, unread</li>
	 * </ul>
	 */
	public static final String FIELD_STATE = "state";

	public static final String FIELD_TIMESTAMP = "timestamp";

	public static final String INDEX_JID = "chat_history_jid_index";

	public final static int STATE_INCOMING = 0;

	public final static int STATE_INCOMING_UNREAD = 3;

	public final static int STATE_OUT_SENT = 2;

	public final static int STATE_OUT_NOT_SENT = 4;

	public static final String TABLE_NAME = "chat_history";

}
