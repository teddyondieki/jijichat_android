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
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import co.jijichat.Preferences;

public class NotificationHelperBase extends NotificationHelper {

	protected NotificationHelperBase(Context context) {
		super(context);
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title,
			String text, PendingIntent pendingIntent, MessageEvent event)
			throws XMLException {
		long whenNotify = System.currentTimeMillis();
		Notification notification = new Notification(ico, title, whenNotify);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
		updateSound(notification, Preferences.NOTIFICATION_CHAT_KEY);
		updateLight(notification, Preferences.NOTIFICATION_CHAT_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_CHAT_KEY);

		notification.setLatestEventInfo(context, title, text, pendingIntent);

		return notification;
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title,
			String text, PendingIntent pendingIntent, MucEvent event)
			throws XMLException {
		long whenNotify = System.currentTimeMillis();
		Notification notification = new Notification(ico, title, whenNotify);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;

		updateSound(notification, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);
		updateLight(notification, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);
		updateVibrate(notification, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);

		notification.setLatestEventInfo(context, title, text, pendingIntent);

		return notification;
	}

}
