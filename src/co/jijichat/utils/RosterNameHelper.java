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
import co.jijichat.MessengerApplication;

//import tigase.jaxmpp.R;

public class RosterNameHelper {

	private static Map<BareJID, String> nameCache;
	// primarily for room descriptions
	private static Map<BareJID, String> descCache;

	public static void clearName(BareJID jid) {
		nameCache.remove(jid);
	}

	public static void clearDescription(BareJID jid) {
		descCache.remove(jid);
	}

	public static String getName(BareJID jid) {
		String status = nameCache.get(jid);
		if (status == null) {
			status = loadName(jid);
		}
		return status;
	}

	public static String getDesc(BareJID jid) {
		String desc = descCache.get(jid);
		if (desc == null) {
			desc = "";
		}
		return desc;
	}

	public static void updateName(BareJID jid, String name) {
		nameCache.put(jid, name);
	}

	public static void updateDesc(BareJID jid, String desc) {
		descCache.put(jid, desc);
	}

	public static void initialize() {
		if (nameCache == null) {
			nameCache = new HashMap<BareJID, String>();
		}
		if (descCache == null) {
			descCache = new HashMap<BareJID, String>();
		}
	}

	protected static String loadName(BareJID jid) {
		String name = loadNameFromRoster(jid);
		nameCache.put(jid, name);
		return name;
	}

	protected static String loadNameFromRoster(BareJID jid) {
		String name = null;
		try {
			name = MessengerApplication.app.getJaxmpp().getRoster().get(jid)
					.getName();
		} catch (NullPointerException ex) {
			name = jid.getLocalpart();
		}
		return name;
	}

}
