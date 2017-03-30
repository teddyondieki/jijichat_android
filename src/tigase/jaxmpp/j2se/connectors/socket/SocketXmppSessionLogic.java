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
package tigase.jaxmpp.j2se.connectors.socket;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.SessionObject.Scope;
import tigase.jaxmpp.core.client.XmppModulesManager;
import tigase.jaxmpp.core.client.XmppSessionLogic;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.SessionEstablishmentModule;
import tigase.jaxmpp.core.client.xmpp.modules.SessionEstablishmentModule.SessionEstablishmentEvent;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule.StreamFeaturesReceivedEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.NonSaslAuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.NonSaslAuthModule.NonSaslAuthEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule.SaslEvent;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule.StreamManagementFailedEvent;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule.StreamResumedEvent;

public class SocketXmppSessionLogic implements XmppSessionLogic {

	static Throwable extractCauseException(Throwable ex) {
		Throwable th = ex.getCause();
		if (th == null)
			return ex;

		for (int i = 0; i < 4; i++) {
			if (!(th instanceof JaxmppException))
				return th;
			if (th.getCause() == null)
				return th;
			th = th.getCause();
		}
		return ex;
	}

	private AuthModule authModule;

	private final SocketConnector connector;

	private final Listener<ConnectorEvent> connectorListener;

	private StreamFeaturesModule featuresModule;

	private final XmppModulesManager modulesManager;

	private ResourceBinderModule resourceBinder;

	private Listener<ResourceBindEvent> resourceBindListener;

	private final Listener<AuthModule.AuthEvent> saslEventListener;

	private Listener<SessionEstablishmentEvent> sessionEstablishmentListener;

	private SessionEstablishmentModule sessionEstablishmentModule;

	private SessionListener sessionListener;

	private final SessionObject sessionObject;

	private final Listener<StreamManagementModule.StreamManagementFailedEvent> smFailedListener;

	private final Listener<StreamManagementModule.StreamResumedEvent> smResumedListener;

	private final Listener<StreamFeaturesReceivedEvent> streamFeaturesEventListener;

	private StreamManagementModule streamManaegmentModule;

	public SocketXmppSessionLogic(SocketConnector connector, XmppModulesManager modulesManager, SessionObject so,
			PacketWriter writer) {
		this.connector = connector;
		this.modulesManager = modulesManager;
		this.sessionObject = so;

		this.connectorListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(Connector.ConnectorEvent be) throws JaxmppException {
				processConnectorEvents(be);
			}
		};

		this.streamFeaturesEventListener = new Listener<StreamFeaturesModule.StreamFeaturesReceivedEvent>() {

			@Override
			public void handleEvent(StreamFeaturesReceivedEvent be) throws JaxmppException {
				try {
					processStreamFeatures(be);
				} catch (JaxmppException e) {
					processException(e);
				}
			}
		};
		this.saslEventListener = new Listener<AuthModule.AuthEvent>() {

			@Override
			public void handleEvent(AuthModule.AuthEvent be) throws JaxmppException {
				try {
					if (be instanceof SaslEvent) {
						processSaslEvent((SaslEvent) be);
					} else if (be instanceof NonSaslAuthEvent) {
						processNonSaslEvent((NonSaslAuthEvent) be);
					}
				} catch (JaxmppException e) {
					processException(e);
				}
			}

		};
		this.resourceBindListener = new Listener<ResourceBindEvent>() {

			@Override
			public void handleEvent(ResourceBindEvent be) throws JaxmppException {
				try {
					processResourceBindEvent(be);
				} catch (JaxmppException e) {
					processException(e);
				}

			}
		};
		this.sessionEstablishmentListener = new Listener<SessionEstablishmentModule.SessionEstablishmentEvent>() {

			@Override
			public void handleEvent(SessionEstablishmentEvent be) throws JaxmppException {
				sessionBindedAndEstablished();
			}
		};
		this.smFailedListener = new Listener<StreamManagementModule.StreamManagementFailedEvent>() {

			@Override
			public void handleEvent(StreamManagementFailedEvent be) throws JaxmppException {
				sessionObject.clear(Scope.session);
				resourceBinder.bind();
			}
		};
		this.smResumedListener = new Listener<StreamManagementModule.StreamResumedEvent>() {

			@Override
			public void handleEvent(StreamResumedEvent be) throws JaxmppException {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public void beforeStart() throws JaxmppException {
		if (sessionObject.getProperty(SessionObject.DOMAIN_NAME) == null
				&& sessionObject.getProperty(SessionObject.USER_BARE_JID) == null)
			throw new JaxmppException("No user JID or server name specified");

		if (sessionObject.getProperty(SessionObject.DOMAIN_NAME) == null)
			sessionObject.setProperty(SessionObject.DOMAIN_NAME,
					((BareJID) sessionObject.getProperty(SessionObject.USER_BARE_JID)).getDomain());

	}

	protected void processConnectorEvents(ConnectorEvent be) throws JaxmppException {
		if (be.getType() == Connector.Error && be.getCaught() != null) {
			Throwable e1 = extractCauseException(be.getCaught());
			JaxmppException e = (JaxmppException) (e1 instanceof JaxmppException ? e1 : new JaxmppException(e1));
			processException(e);
		}
	}

	protected void processException(JaxmppException e) throws JaxmppException {
		if (sessionListener != null)
			sessionListener.onException(e);
	}

	protected void processNonSaslEvent(final NonSaslAuthModule.NonSaslAuthEvent be) throws JaxmppException {
		if (be.getType() == AuthModule.AuthFailed) {
			throw new JaxmppException("Unauthorized with condition=" + be.getError());
		} else if (be.getType() == AuthModule.AuthSuccess) {
			connector.restartStream();
		}
	}

	protected void processResourceBindEvent(ResourceBindEvent be) throws JaxmppException {
		if (SessionEstablishmentModule.isSessionEstablishingAvailable(sessionObject)) {
			modulesManager.getModule(SessionEstablishmentModule.class).establish();
		} else
			sessionBindedAndEstablished();
	}

	protected void processSaslEvent(SaslEvent be) throws JaxmppException {
		if (be.getType() == AuthModule.AuthFailed) {
			throw new JaxmppException("Unauthorized with condition=" + be.getError());
		} else if (be.getType() == AuthModule.AuthSuccess) {
			connector.restartStream();
		}
	}

	protected void processStreamFeatures(StreamFeaturesReceivedEvent be) throws JaxmppException {
		try {
			final Boolean tlsDisabled = sessionObject.getProperty(SocketConnector.TLS_DISABLED_KEY);
			final boolean authAvailable = AuthModule.isAuthAvailable(sessionObject);
			final boolean tlsAvailable = SocketConnector.isTLSAvailable(sessionObject);
			final Boolean compressionDisabled = sessionObject.getProperty(SocketConnector.COMPRESSION_DISABLED_KEY);
			final boolean zlibAvailable = SocketConnector.isZLibAvailable(sessionObject);

			final boolean isAuthorized = sessionObject.getProperty(AuthModule.AUTHORIZED) == Boolean.TRUE;
			final boolean isConnectionSecure = connector.isSecure();
			final boolean isConnectionCompressed = connector.isCompressed();

			final boolean resumption = StreamManagementModule.isStreamManagementAvailable(sessionObject)
					&& StreamManagementModule.isResumptionEnabled(sessionObject);

			if (!isConnectionSecure && tlsAvailable && (tlsDisabled == null || !tlsDisabled)) {
				connector.startTLS();
			} else if (!isConnectionCompressed && zlibAvailable && (compressionDisabled == null || !compressionDisabled)) {
				connector.startZLib();
			} else if (!isAuthorized && authAvailable) {
				authModule.login();
			} else if (isAuthorized && resumption) {
				streamManaegmentModule.resume();
			} else if (isAuthorized) {
				resourceBinder.bind();
			}
		} catch (XMLException e) {
			e.printStackTrace();
		}
	}

	private void sessionBindedAndEstablished() throws JaxmppException {
		try {
			DiscoInfoModule discoInfo = this.modulesManager.getModule(DiscoInfoModule.class);
			if (discoInfo != null) {
				discoInfo.discoverServerFeatures(null);
			}

			RosterModule roster = this.modulesManager.getModule(RosterModule.class);
			if (roster != null) {
				roster.rosterRequest();
			}

			PresenceModule presence = this.modulesManager.getModule(PresenceModule.class);
			if (presence != null) {
				presence.sendInitialPresence();
			}

			if (StreamManagementModule.isStreamManagementAvailable(sessionObject)) {
				if (sessionObject.getProperty(StreamManagementModule.STREAM_MANAGEMENT_DISABLED_KEY) == null
						|| !((Boolean) sessionObject.getProperty(StreamManagementModule.STREAM_MANAGEMENT_DISABLED_KEY)).booleanValue()) {
					StreamManagementModule streamManagement = this.modulesManager.getModule(StreamManagementModule.class);
					streamManagement.enable();
				}
			}
		} catch (XMLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setSessionListener(SessionListener sessionListener) throws JaxmppException {
		this.sessionListener = sessionListener;
		featuresModule = this.modulesManager.getModule(StreamFeaturesModule.class);
		authModule = this.modulesManager.getModule(AuthModule.class);
		resourceBinder = this.modulesManager.getModule(ResourceBinderModule.class);
		this.sessionEstablishmentModule = this.modulesManager.getModule(SessionEstablishmentModule.class);
		this.streamManaegmentModule = this.modulesManager.getModule(StreamManagementModule.class);

		connector.addListener(Connector.Error, connectorListener);
		featuresModule.addListener(StreamFeaturesModule.StreamFeaturesReceived, streamFeaturesEventListener);
		authModule.addListener(AuthModule.AuthSuccess, this.saslEventListener);
		authModule.addListener(AuthModule.AuthFailed, this.saslEventListener);
		resourceBinder.addListener(ResourceBinderModule.ResourceBindSuccess, resourceBindListener);
		this.sessionEstablishmentModule.addListener(SessionEstablishmentModule.SessionEstablishmentSuccess,
				this.sessionEstablishmentListener);
		this.sessionEstablishmentModule.addListener(SessionEstablishmentModule.SessionEstablishmentError,
				this.sessionEstablishmentListener);
		this.streamManaegmentModule.addListener(StreamManagementModule.StreamManagementFailed, smFailedListener);
		this.streamManaegmentModule.addListener(StreamManagementModule.StreamResumed, smResumedListener);
	}

	@Override
	public void unbind() throws JaxmppException {
		connector.removeListener(Connector.Error, connectorListener);
		featuresModule.removeListener(StreamFeaturesModule.StreamFeaturesReceived, streamFeaturesEventListener);
		authModule.removeListener(AuthModule.AuthSuccess, this.saslEventListener);
		authModule.removeListener(AuthModule.AuthFailed, this.saslEventListener);
		resourceBinder.removeListener(ResourceBinderModule.ResourceBindSuccess, resourceBindListener);

		this.sessionEstablishmentModule.removeListener(SessionEstablishmentModule.SessionEstablishmentSuccess,
				this.sessionEstablishmentListener);
		this.sessionEstablishmentModule.removeListener(SessionEstablishmentModule.SessionEstablishmentError,
				this.sessionEstablishmentListener);

		this.streamManaegmentModule.removeListener(StreamManagementModule.StreamManagementFailed, smFailedListener);
		this.streamManaegmentModule.removeListener(StreamManagementModule.StreamResumed, smResumedListener);

	}

}