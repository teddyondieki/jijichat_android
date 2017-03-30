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
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import co.jijichat.MessengerApplication;
import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.Preferences;
import co.jijichat.R;
import co.jijichat.utils.RosterNameHelper;

public abstract class NotificationHelper {

	public static final int AUTH_REQUEST_NOTIFICATION_ID = 132108;

	public static final int CHAT_NOTIFICATION_ID = 132008;

	public static final String DEFAULT_NOTIFICATION_URI = "content://settings/system/notification_sound";

	public static final int ERROR_NOTIFICATION_ID = 5398717;

	public static final int FILE_TRANSFER_NOTIFICATION_ID = 132009;

	public static final int NOTIFICATION_ID = 5398777;

	protected static final String TAG = "NotificationHelper";

	public static NotificationHelper createInstance(Context context) {
		if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
			return new NotificationHelperJellyBean(context);
		} else if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT) {
			return new NotificationHelperICS(context);
		} else if (Build.VERSION_CODES.HONEYCOMB <= Build.VERSION.SDK_INT) {
			return new NotificationHelperHoneycomb(context);
		} else {
			return new NotificationHelperBase(context);
		}
	}

	protected final Context context;

	private Notification foregroundNotification;

	protected final NotificationManager notificationManager;

	protected NotificationHelper(Context context) {
		this.context = context;
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void cancelChatNotification(String tag) {
		notificationManager
				.cancel(tag, NotificationHelper.CHAT_NOTIFICATION_ID);
	}

	public void cancelAllNotifications() {
		notificationManager.cancelAll();
	}

	public void cancelNotification() {
		notificationManager.cancel(NOTIFICATION_ID);
	}

	public Notification getForegroundNotification(int ico,
			String notiticationTitle, String expandedNotificationText) {
		if (foregroundNotification == null) {
			long whenNotify = System.currentTimeMillis();
			foregroundNotification = new Notification(ico, notiticationTitle,
					whenNotify);
		}

		foregroundNotification.icon = ico;
		foregroundNotification.tickerText = notiticationTitle;

		// notification.flags = Notification.FLAG_AUTO_CANCEL;
		foregroundNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		foregroundNotification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		Context context = this.context.getApplicationContext();
		String expandedNotificationTitle = context.getResources().getString(
				R.string.app_name);
		Intent intent = new Intent(context,
				JijichatMobileMessengerActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				(int) System.currentTimeMillis(), intent, 0);

		foregroundNotification.setLatestEventInfo(context,
				expandedNotificationTitle, expandedNotificationText,
				pendingIntent);

		return foregroundNotification;
	}

	private final Jaxmpp getJaxmpp() {
		return ((MessengerApplication) context.getApplicationContext())
				.getJaxmpp();
	}

	protected abstract Notification prepareChatNotification(int ico,
			String title, String text, PendingIntent pendingIntent,
			MessageEvent event) throws XMLException;

	protected abstract Notification prepareChatNotification(int ico,
			String title, String text, PendingIntent pendingIntent,
			MucEvent event) throws XMLException;

	protected void updateLight(Notification notification, String lightKey) {
		// notification.defaults |= Notification.DEFAULT_LIGHTS;

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.BLUE;
		notification.ledOffMS = 500;
		notification.ledOnMS = 500;
	}

	protected void updateSound(Notification notification, String soundKey) {
		String notificationSound = PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						soundKey + "_sound", DEFAULT_NOTIFICATION_URI);

		if (DEFAULT_NOTIFICATION_URI.equals(notificationSound)) {
			notificationSound = PreferenceManager.getDefaultSharedPreferences(
					context).getString(Preferences.NOTIFICATION_SOUND_KEY,
					DEFAULT_NOTIFICATION_URI);
		}

		notification.sound = Uri.parse(notificationSound);
	}

	protected void updateVibrate(Notification notification, String vibrateKey) {
		String vibrate = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(vibrateKey + "_vibrate", "default");

		if ("default".equals(vibrate)) {
			vibrate = PreferenceManager.getDefaultSharedPreferences(context)
					.getString(Preferences.NOTIFICATION_VIBRATE_KEY, "default");
		}

		if ("default".equals(vibrate)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		} else if ("yes".equals(vibrate)) {
			notification.vibrate = new long[] { 0l, 300l, 200l, 300l, 200l };
		} else {
			notification.vibrate = new long[] {};
		}
	}

	public void notifyNewMucMessage(MucEvent event) throws XMLException {
		int ico = R.drawable.ic_launcher;
		String n = RosterNameHelper.getName(event.getMessage().getFrom()
				.getBareJid());

		if (n == null) {
			n = event.getMessage().getFrom().getBareJid().toString();
		}

		String notificationTitle = n;
		String notificationText = context.getResources().getString(
				R.string.service_mentioned_you_in_message,
				event.getMessage().getFrom().getResource());

		Intent intent = new Intent(context,
				JijichatMobileMessengerActivity.class);
		intent.setAction(JijichatMobileMessengerActivity.MUC_MESSAGE_ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("roomJid", "" + event.getRoom().getRoomJid().toString());
		if (event.getRoom() != null)
			intent.putExtra("roomId", event.getRoom().getId());

		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				(int) System.currentTimeMillis(), intent, 0);

		Notification notification = prepareChatNotification(ico,
				notificationTitle, notificationText, pendingIntent, event);

		notificationManager.notify("roomId:" + event.getRoom().getId(),
				CHAT_NOTIFICATION_ID, notification);

	}

	public void notifyNewMucMention(MucEvent event) throws XMLException {
		int ico = R.drawable.ic_launcher;
		String n = RosterNameHelper.getName(event.getMessage().getFrom()
				.getBareJid());

		if (n == null) {
			n = event.getMessage().getFrom().getBareJid().toString();
		}

		String notificationTitle = n;
		String notificationText = event.getMessage().getBody();

		Intent intent = new Intent(context,
				JijichatMobileMessengerActivity.class);
		intent.setAction(JijichatMobileMessengerActivity.MUC_MESSAGE_ACTION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("roomJid", "" + event.getRoom().getRoomJid().toString());
		if (event.getRoom() != null)
			intent.putExtra("roomId", event.getRoom().getId());

		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				(int) System.currentTimeMillis(), intent, 0);

		Notification notification = prepareChatNotification(ico,
				notificationTitle, notificationText, pendingIntent, event);

		notificationManager.notify("roomId:" + event.getRoom().getId(),
				CHAT_NOTIFICATION_ID, notification);

	}

}
