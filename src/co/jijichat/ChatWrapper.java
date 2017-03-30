package co.jijichat;

import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;

public class ChatWrapper {

	private final Object data;

	public ChatWrapper(Chat chat) {
		this.data = chat;
	}

	public ChatWrapper(Room room) {
		this.data = room;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChatWrapper) {
			return data.equals(((ChatWrapper) o).data);
		} else
			return data.equals(o);
	}

	public Chat getChat() {
		if (data instanceof Chat)
			return (Chat) data;
		else
			return null;
	}

	public Room getRoom() {
		if (data instanceof Room)
			return (Room) data;
		else
			return null;
	}

	@Override
	public int hashCode() {
		return data.hashCode();
	}

	public boolean isChat() {
		return data instanceof Chat;
	}

	public boolean isRoom() {
		return data instanceof Room;
	}

	@Override
	public String toString() {
		if (data instanceof Chat) {
			return "chatid:" + ((Chat) data).getId();
		} else if (data instanceof Room) {
			return "roomid:" + ((Room) data).getId();
		} else
			return super.toString();
	}
}
