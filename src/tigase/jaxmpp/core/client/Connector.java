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
package tigase.jaxmpp.core.client;

import javax.net.ssl.TrustManager;

import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.EventType;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

/**
 * Main Connector interface.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>{@link Connector#Connected Connected}</b> : {@link ConnectorEvent
 * ConnectorEvent} ()<br>
 * <div>Fires after creates XMPP Stream</div>
 * <ul>
 * </ul></dd>
 * 
 * <dd><b>{@link Connector#EncryptionEstablished EncryptionEstablished}</b> :
 * {@link ConnectorEvent ConnectorEvent} ()<br>
 * <div>Fires after encrypted connection is established.</div>
 * <ul>
 * </ul></dd>
 * 
 * <dd><b>{@link Connector#Error Error}</b> : {@link ConnectorEvent
 * ConnectorEvent} (caught)<br>
 * <div>Fires on XMPP Stream error</div>
 * <ul>
 * <li>caught : exception</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>{@link Connector#StateChanged StateChanged}</b> :
 * {@link ConnectorEvent ConnectorEvent} ()<br>
 * <div>Fires after connection state is changed</div>
 * <ul>
 * </ul></dd>
 * 
 * <dd><b>{@link Connector#StanzaReceived StanzaReceived}</b> :
 * {@link ConnectorEvent ConnectorEvent} (stanza)<br>
 * <div>Fires after next stanza is received</div>
 * <ul>
 * <li>stanza : received stanza</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>{@link Connector#StreamTerminated StreamTerminated}</b> :
 * {@link ConnectorEvent ConnectorEvent} ()<br>
 * <div>Fires after XMPP Stream is terminated</div>
 * <ul>
 * </ul></dd>
 * 
 * 
 * <br/>
 * <dt><b>Properties:</b></dt>
 * 
 * <dd><b>{@link Connector#TRUST_MANAGERS_KEY TRUST_MANAGER}</b>: Custom
 * {@link TrustManager TrustManager} instead of dummy (accespts all
 * certificates) builded in.</dd>
 * 
 * 
 * </dl>
 * 
 */
public interface Connector {

	/**
	 * Event generated by Connector.
	 * 
	 */
	public static class ConnectorEvent extends BaseEvent {

		private static final long serialVersionUID = 1L;

		public static long getSerialversionuid() {
			return serialVersionUID;
		}

		private Throwable caught;

		private Element stanza;

		private StreamError streamError;

		private Element streamErrorElement;

		public ConnectorEvent(EventType type, SessionObject sessionObject) {
			super(type, sessionObject);
		}

		/**
		 * Reuturn exception if happend.
		 * 
		 * @return exception. <code>null</code> if event isn't fired because of
		 *         exception.
		 */
		public Throwable getCaught() {
			return caught;
		}

		/**
		 * Return received stanza
		 * 
		 * @return stanza {@linkplain Element} or <code>null</code> if event
		 *         isn't fired because of stanza processing.
		 */
		public Element getStanza() {
			return stanza;
		}

		/**
		 * Returns stream error cause.
		 * 
		 * @return {@linkplain StreamError} <code>null</code> if event isn't
		 *         fired because of stream error.
		 */
		public StreamError getStreamError() {
			return this.streamError;
		}

		/**
		 * Returns element wrapper around &lt;error&gt; receved element.
		 * 
		 * @return error element or <code>null</code> if event isn't fired
		 *         because of stream error.
		 */
		public Element getStreamErrorElement() {
			return this.streamErrorElement;
		}

		public void setCaught(Throwable caught) {
			this.caught = caught;
		}

		public void setStanza(Element stanza) {
			this.stanza = stanza;
		}

		public void setStreamError(StreamError streamError) {
			this.streamError = streamError;
		}

		public void setStreamErrorElement(Element streamErrorElement) {
			this.streamErrorElement = streamErrorElement;
		}
	}

	public static enum State {
		connected,
		connecting,
		disconnected,
		disconnecting
	}

	public final static EventType BodyReceived = new EventType();

	/**
	 * Event fires after creates XMPP Stream
	 */
	public final static EventType Connected = new EventType();

	/**
	 * Key set to true to determine if connection is already compressed
	 */
	public final static String COMPRESSED_KEY = "CONNECTOR#COMPRESSED_KEY";
	
	public final static String CONNECTOR_STAGE_KEY = "CONNECTOR#STAGE_KEY";

	public final static String DISABLE_KEEPALIVE_KEY = "CONNECTOR#DISABLEKEEPALIVE";

	// public final static String DISABLE_SOCKET_TIMEOUT_KEY =
	// "CONNECTOR#DISABLE_SOCKET_TIMEOUT_KEY";

	public final static String ENCRYPTED_KEY = "CONNECTOR#ENCRYPTED_KEY";

	/**
	 * Event fires after encrypted connection is established.
	 */
	public final static EventType EncryptionEstablished = new EventType();

	/**
	 * Event fires on XMPP Stream error.
	 * <p>
	 * Filled fields:
	 * <ul>
	 * <li>caught : exception</li>
	 * </ul>
	 * </p>
	 */
	public final static EventType Error = new EventType();

	public final static String EXTERNAL_KEEPALIVE_KEY = "CONNECTOR#EXTERNAL_KEEPALIVE_KEY";

	public static final String SEE_OTHER_HOST_KEY = "BOSH#SEE_OTHER_HOST_KEY";

	/**
	 * Event fires after creates XMPP Stream.
	 * <p>
	 * Filled fields:
	 * <ul>
	 * <li>{@link ConnectorEvent#getStanza() stanza} : received stanza</li>
	 * </ul>
	 * </p>
	 */
	public final static EventType StanzaReceived = new EventType();

	public final static EventType StanzaSending = new EventType();

	/**
	 * Event fires after connection state is changed.
	 */
	public final static EventType StateChanged = new EventType();

	/**
	 * Event fires after XMPP Stream is terminated.
	 */
	public final static EventType StreamTerminated = new EventType();

	/**
	 * Key for define {@linkplain SessionObject#setUserProperty(String, Object)
	 * property}. Custom array of {@link TrustManager TrustManagers[]} instead
	 * of dummy (accepts all certificates) builded in.
	 */
	public static final String TRUST_MANAGERS_KEY = "TRUST_MANAGERS_KEY";

	/**
	 * Adds a listener bound by the given event type.
	 * 
	 * @param eventType
	 *            type of event
	 * @param listener
	 *            the listener
	 */
	public void addListener(EventType eventType, Listener<? extends ConnectorEvent> listener);

	/**
	 * Returns instance of {@linkplain XmppSessionLogic} to work with this
	 * connector.
	 * 
	 * @param modulesManager
	 *            module manager
	 * @param writer
	 *            writer
	 * @return {@linkplain XmppSessionLogic}
	 */
	public XmppSessionLogic createSessionLogic(XmppModulesManager modulesManager, PacketWriter writer);

	/**
	 * Returns observable
	 * 
	 * @return
	 */
	public Observable getObservable();

	public State getState();

	boolean isCompressed();
	
	boolean isSecure();

	/**
	 * Whitespace ping.
	 * 
	 * @throws JaxmppException
	 */
	public void keepalive() throws JaxmppException;

	public void removeAllListeners();

	public void removeListener(EventType eventType, Listener<ConnectorEvent> listener);

	public void restartStream() throws XMLException, JaxmppException;

	public void send(final Element stanza) throws XMLException, JaxmppException;

	public void setObservable(Observable observable);

	public void start() throws XMLException, JaxmppException;

	public void stop() throws XMLException, JaxmppException;

	public void stop(boolean terminate) throws XMLException, JaxmppException;

}