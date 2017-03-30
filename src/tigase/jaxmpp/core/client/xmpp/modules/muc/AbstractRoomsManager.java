/*
 * Tigase XMPP Client Library
 * Copyright (C) 2006-2012 "Bartosz Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.jaxmpp.core.client.xmpp.modules.muc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.observer.Observable;

public abstract class AbstractRoomsManager {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	protected Observable observable;

	protected PacketWriter packetWriter;

	protected final Map<BareJID, Room> rooms = new HashMap<BareJID, Room>();

	protected SessionObject sessionObject;

	public boolean contains(BareJID roomJid) {
		log.finer("RoomsManager, rooms: " + rooms.keySet() + ", contains("
				+ roomJid + "): " + this.rooms.containsKey(roomJid));
		return this.rooms.containsKey(roomJid);
	}

	protected abstract Room createRoomInstance(final BareJID roomJid,
			final String nickname, final String password);

	public Room get(BareJID roomJid) {
		return this.rooms.get(roomJid);
	}

	Observable getObservable() {
		return observable;
	}

	PacketWriter getPacketWriter() {
		return packetWriter;
	}

	public Collection<Room> getRooms() {
		return this.rooms.values();
	}

	public Map<BareJID, Room> getRoomsMap() {
		return this.rooms;
	}

	SessionObject getSessionObject() {
		return sessionObject;
	}

	protected void initialize() {

	}

	public void register(Room room) {
		log.finer("RoomsManager, register room: " + room.getRoomJid());
		this.rooms.put(room.getRoomJid(), room);
	}

	public boolean remove(Room room) {
		log.finer("RoomsManager, removing room: " + room.getRoomJid());
		return this.rooms.remove(room.getRoomJid()) != null;
	}

	void setObservable(Observable observable) {
		this.observable = observable;
	}

	void setPacketWriter(PacketWriter packetWriter) {
		this.packetWriter = packetWriter;
	}

	void setSessionObject(SessionObject sessionObject) {
		this.sessionObject = sessionObject;
	}
}