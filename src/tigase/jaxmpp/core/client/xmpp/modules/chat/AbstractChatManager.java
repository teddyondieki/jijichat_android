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
package tigase.jaxmpp.core.client.xmpp.modules.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.SessionObject.Scope;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import android.util.Log;

public abstract class AbstractChatManager {

	protected final ArrayList<Chat> chats = new ArrayList<Chat>();

	protected ChatSelector chatSelector;

	protected Observable observable;

	protected PacketWriter packetWriter;

	protected SessionObject sessionObject;

	protected AbstractChatManager() {
		ChatSelector x = UniversalFactory.createInstance(ChatSelector.class
				.getName());
		this.chatSelector = x == null ? new DefaultChatSelector() : x;
	}

	public boolean close(Chat chat) throws JaxmppException {
		boolean x = this.chats.remove(chat);
		if (x) {
			MessageModule.MessageEvent event = new MessageEvent(
					MessageModule.ChatClosed, sessionObject);
			event.setChat(chat);
			observable.fireEvent(event);
		}
		return x;
	}

	public Chat createChat(JID jid) throws JaxmppException {
		Chat chat = createChatInstance(jid);

		this.chats.add(chat);

		MessageEvent event = new MessageModule.MessageEvent(
				MessageModule.ChatCreated, sessionObject);
		event.setChat(chat);

		observable.fireEvent(event.getType(), event);

		return chat;
	}

	protected abstract Chat createChatInstance(final JID fromJid);

	protected Chat getChat(JID jid) {
		return chatSelector.getChat(chats, jid);
	}

	public List<Chat> getChats() {
		return this.chats;
	}

	Observable getObservable() {
		return observable;
	}

	PacketWriter getPacketWriter() {
		return packetWriter;
	}

	SessionObject getSessionObject() {
		return sessionObject;
	}

	protected void initialize() {

	}

	public boolean isChatOpenFor(final BareJID jid) {
		for (Chat chat : this.chats) {
			if (chat.getJid().getBareJid().equals(jid))
				return true;
		}
		return false;
	}

	public void onSessionObjectCleared(SessionObject sessionObject,
			Set<Scope> scopes) throws JaxmppException {
		if (scopes != null && scopes.contains(Scope.session)) {
			resetChatStates();
		}
	}

	public Chat process(Message message, JID interlocutorJid,
			Observable observable) throws JaxmppException {
		// Log.d("MESSAGE ALERT", message.getAsString());
		// if (message.getType() != StanzaType.chat
		// && message.getType() != StanzaType.error
		// && message.getType() != StanzaType.headline)
		// return null;
		if (message.getType() != StanzaType.chat)
			return null;
		// final String threadId = message.getThread();

		Chat chat = getChat(interlocutorJid);

		if (chat == null && message.getBody() == null) {
			return null;
		}

		if (chat == null) {
			chat = createChatInstance(interlocutorJid);
			chat.setJid(interlocutorJid);
			this.chats.add(chat);
			MessageEvent event = new MessageModule.MessageEvent(
					MessageModule.ChatCreated, sessionObject);
			event.setChat(chat);
			event.setMessage(message);

			observable.fireEvent(event.getType(), event);
		} else {
			update(chat, interlocutorJid);
		}

		if (!ChatState.isChatStateDisabled(sessionObject)) {
			List<Element> stateElems = message.getChildrenNS(ChatState.XMLNS);
			if (stateElems != null && stateElems.size() > 0) {
				Element stateElem = stateElems.get(0);
				chat.setChatState(ChatState.fromElement(stateElem));

				MessageEvent event = new MessageModule.MessageEvent(
						MessageModule.ChatStateChanged, sessionObject);
				event.setChat(chat);
				event.setMessage(message);
				observable.fireEvent(event.getType(), event);
			}
		}
		return chat;
	}

	public Chat process(Message message, Observable observable)
			throws JaxmppException {
		final JID interlocutorJid = message.getFrom();
		return process(message, interlocutorJid, observable);
	}

	protected void resetChatStates() {
		for (Chat chat : this.chats) {
			try {
				chat.setLocalChatState(null);
			} catch (JaxmppException ex) {
				// should not happen
			}
			chat.setChatState(null);
			try {
				MessageModule.MessageEvent event = new MessageEvent(
						MessageModule.ChatStateChanged, sessionObject);
				event.setChat(chat);
				observable.fireEvent(event);
			} catch (JaxmppException ex) {
				// there is nothing we can do, but this should not happen as
				// well
			}
		}
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

	protected boolean update(final Chat chat, final JID fromJid)
			throws JaxmppException {
		boolean changed = false;

		if (!chat.getJid().equals(fromJid)) {
			chat.setJid(fromJid);
			changed = true;
		}

		if (changed) {
			Log.d("chatChanged", "ChatChanged");
			MessageEvent event = new MessageModule.MessageEvent(
					MessageModule.ChatUpdated, sessionObject);
			event.setChat(chat);
			observable.fireEvent(event.getType(), event);
		}

		return changed;
	}

}