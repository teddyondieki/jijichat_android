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
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import co.jijichat.Preferences;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;

@TargetApi(11)
public class NotificationHelperHoneycomb extends NotificationHelper {

	protected NotificationHelperHoneycomb(Context context) {
		super(context);
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title,
			String text, PendingIntent pendingIntent, MessageEvent event)
			throws XMLException {
		Notification.Builder builder = prepareChatNotificationInt(ico, title,
				text, pendingIntent, event);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	@Override
	protected Notification prepareChatNotification(int ico, String title,
			String text, PendingIntent pendingIntent, MucEvent event)
			throws XMLException {
		Notification.Builder builder = prepareChatNotificationInt(ico, title,
				text, pendingIntent, event);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		return notification;
	}

	protected Notification.Builder prepareChatNotificationInt(int ico,
			String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException {
		Notification.Builder builder = new Notification.Builder(context);
		builder.setContentTitle(title).setContentText(text);
		updateSound(builder, Preferences.NOTIFICATION_CHAT_KEY);
		updateLight(builder, Preferences.NOTIFICATION_CHAT_KEY);
		updateVibrate(builder, Preferences.NOTIFICATION_CHAT_KEY);
//		Bitmap avatar = AvatarHelper.getAvatar(event.getChat().getJid()
//				.getBareJid());
		builder.setSmallIcon(ico).setContentIntent(pendingIntent)
				.setAutoCancel(true);
//		if (avatar != AvatarHelper.mPlaceHolderBitmap) {
//			builder.setLargeIcon(avatar);
//		}

		Uri uri = Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
				+ Uri.encode(event.getChat().getJid().getBareJid().toString()));
		Cursor c = context.getContentResolver().query(
				uri,
				null,
				ChatTableMetaData.FIELD_STATE + "="
						+ ChatTableMetaData.STATE_INCOMING_UNREAD, null, null);
		try {
			int count = c.getCount();
			builder.setNumber(count);

			prepareChatNotificationUnreadMessages(builder, c);
		} catch (Exception ex) {
			Log.e(TAG, "exception preparing notification", ex);
		} finally {
			c.close();
		}

		return builder;
	}

	protected Notification.Builder prepareChatNotificationInt(int ico,
			String title, String text, PendingIntent pendingIntent,
			MucEvent event) throws XMLException {
		Notification.Builder builder = new Notification.Builder(context);
		builder.setContentTitle(title).setContentText(text);
		updateSound(builder, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);
		updateLight(builder, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);
		updateVibrate(builder, Preferences.NOTIFICATION_MUC_MENTIONED_KEY);
		builder.setSmallIcon(ico).setContentIntent(pendingIntent)
				.setAutoCancel(true);

		return builder;
	}

	protected void prepareChatNotificationUnreadMessages(
			Notification.Builder builder, Cursor c) {

	}

	protected void updateLight(Builder builder, String lightKey) {
		builder.setLights(Color.BLUE, 500, 500);
	}

	protected void updateSound(Builder builder, String soundKey) {
		String notificationSound = PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						soundKey + "_sound", DEFAULT_NOTIFICATION_URI);
		if (DEFAULT_NOTIFICATION_URI.equals(notificationSound)) {
			notificationSound = PreferenceManager.getDefaultSharedPreferences(
					context).getString(Preferences.NOTIFICATION_SOUND_KEY,
					DEFAULT_NOTIFICATION_URI);
		}

		builder.setSound(Uri.parse(notificationSound));
	}

	protected void updateVibrate(Builder builder, String vibrateKey) {
		String vibrate = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(vibrateKey + "_vibrate", "default");

		if ("default".equals(vibrate)) {
			vibrate = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(Preferences.NOTIFICATION_VIBRATE_KEY, "default");
		}

		if ("default".equals(vibrate)) {
			builder.setDefaults(Notification.DEFAULT_VIBRATE);
		} else if ("yes".equals(vibrate)) {
			builder.setVibrate(new long[] { 0l, 300l, 200l, 300l, 200l });
		} else {
			builder.setVibrate(new long[] {});
		}

	}
}
