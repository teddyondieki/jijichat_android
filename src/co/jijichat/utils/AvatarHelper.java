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

import java.util.Locale;

import tigase.jaxmpp.core.client.BareJID;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import co.jijichat.muc.MucAdapter;

import com.amulyakhare.textdrawable.TextDrawable;

public class AvatarHelper {

	private static LruCache<BareJID, Drawable> avatarCache;

	public static Drawable placeholderDrawable;

	public static void clearAvatar(BareJID jid) {
		avatarCache.remove(jid);
	}

	public static Drawable getAvatar(BareJID jid) {
		Drawable drawable = avatarCache.get(jid);
		if (drawable == null) {
			drawable = loadAvatar(jid);
		}
		return drawable;
	}

	public static void initialize() {
		if (avatarCache == null) {
			avatarCache = new LruCache<BareJID, Drawable>(15);
		}
	}

	protected static Drawable loadAvatar(BareJID jid) {

		String name = RosterNameHelper.getName(jid);
		int colorRes = MucAdapter.getOccupantColor(jid.toString());
		String firstChar = name.substring(0, 1)
				.toUpperCase(Locale.getDefault());

		TextDrawable drawable = TextDrawable.builder().beginConfig().width(50)
				.height(50).endConfig().buildRound(firstChar, colorRes);

		avatarCache.put(jid, drawable);

		return drawable;
	}

}
