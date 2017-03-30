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
package co.jijichat.ui;

import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import co.jijichat.db.ChatTableMetaData;

@TargetApi(16)
public class NotificationHelperJellyBean extends NotificationHelperICS {

	protected NotificationHelperJellyBean(Context context) {
		super(context);
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title,
			String text, PendingIntent pendingIntent, MessageEvent event)
			throws XMLException {
		Notification.Builder builder = prepareChatNotificationInt(ico, title,
				text, pendingIntent, event);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	@Override
	protected void prepareChatNotificationUnreadMessages(
			Notification.Builder builder, Cursor c) {
		Notification.InboxStyle style = new Notification.InboxStyle();
		int count = c.getCount();
		int used = 0;
		int fieldBodyIdx = c.getColumnIndex(ChatTableMetaData.FIELD_BODY);
		while (c.moveToNext() && used < 3) {
			String body = c.getString(fieldBodyIdx);
			style.addLine(body);
			used++;
		}
		if (count > 3) {
			style.setSummaryText("...");
		} else if (count <= 3) {
			style.setSummaryText("");
		}
		builder.setStyle(style);
	}

}
