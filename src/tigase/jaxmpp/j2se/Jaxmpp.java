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
package tigase.jaxmpp.j2se;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.Processor;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.SessionObject.Scope;
import tigase.jaxmpp.core.client.XmppSessionLogic.SessionListener;
import tigase.jaxmpp.core.client.connector.AbstractBoshConnector;
import tigase.jaxmpp.core.client.connector.BoshXmppSessionLogic;
import tigase.jaxmpp.core.client.connector.ConnectorWrapper;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.observer.ObservableFactory;
import tigase.jaxmpp.core.client.observer.ObservableFactory.FactorySpi;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule.ResourceBindEvent;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.filetransfer.FileTransferModule;
import tigase.jaxmpp.core.client.xmpp.modules.jingle.JingleModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.socks5.Socks5BytestreamsModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule.StreamResumedEvent;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;
import tigase.jaxmpp.j2se.connectors.bosh.BoshConnector;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import tigase.jaxmpp.j2se.filetransfer.FileTransferManager;
import tigase.jaxmpp.j2se.filetransfer.JingleFileTransferNegotiator;
import tigase.jaxmpp.j2se.filetransfer.Socks5FileTransferNegotiator;
import tigase.jaxmpp.j2se.observer.ThreadSafeObservable;
import tigase.jaxmpp.j2se.xmpp.modules.auth.saslmechanisms.ExternalMechanism;

/**
 * Main library class for using in standalone, Android and other J2SE compatible
 * application.
 */
public class Jaxmpp extends JaxmppCore {

	private class LoginTimeoutTask extends TimerTask {

		@Override
		public void run() {
			synchronized (Jaxmpp.this) {
				Jaxmpp.this.notify();
			}
		}
	}

	public static final String CONNECTOR_TYPE = "connectorType";

	private static final Executor DEFAULT_EXECUTOR = new Executor() {

		@Override
		public synchronized void execute(Runnable command) {
			(new Thread(command)).start();
		}
	};

	public static final String EXCEPTION_KEY = "jaxmpp#ThrowedException";

	public static final String LOGIN_TIMEOUT_KEY = "LOGIN_TIMEOUT_KEY";;

	public static final String SYNCHRONIZED_MODE = "jaxmpp#synchronized";

	static {
		ObservableFactory.setFactorySpi(new FactorySpi() {

			@Override
			public Observable create() {
				return create(null);
			}

			@Override
			public Observable create(Observable parent) {
				return new ThreadSafeObservable(parent);
			}
		});
		DateTimeFormat.setProvider(new DateTimeFormatProviderImpl());
	}

	private final ConnectorWrapper connectorWrapper;

	private Executor executor;

	private FileTransferManager fileTransferManager;

	private TimerTask loginTimeoutTask;

	private final Timer timer = new Timer(true);

	public Jaxmpp() {
		this(new J2SESessionObject());
		setExecutor(DEFAULT_EXECUTOR);
	}

	public Jaxmpp(SessionObject sessionObject) {
		super(sessionObject);
		setExecutor(DEFAULT_EXECUTOR);
		TimerTask checkTimeouts = new TimerTask() {

			@Override
			public void run() {
				try {
					checkTimeouts();
				} catch (JaxmppException e) {
					e.printStackTrace();
				}
			}
		};
		timer.schedule(checkTimeouts, 30 * 1000, 30 * 1000);

		this.connectorWrapper = new ConnectorWrapper(observable);
		this.connector = this.connectorWrapper;
		this.connector.addListener(Connector.StanzaReceived, this.stanzaReceivedListener);
		this.connector.addListener(Connector.StreamTerminated, this.streamTerminateListener);
		this.connector.addListener(Connector.Error, this.streamErrorListener);

		this.processor = new Processor(this.modulesManager, this.sessionObject, this.writer);

		modulesInit();

		ResourceBinderModule r = this.modulesManager.getModule(ResourceBinderModule.class);
		r.addListener(ResourceBinderModule.ResourceBindSuccess, resourceBindListener);

		StreamManagementModule sm = this.modulesManager.getModule(StreamManagementModule.class);
		if (sm != null)
			sm.addListener(StreamManagementModule.StreamResumed, this.streamResumedListener);

	}

	protected void checkTimeouts() throws JaxmppException {
		sessionObject.checkHandlersTimeout();
	}

	protected Connector createConnector() throws JaxmppException {
		if (sessionObject.getProperty(CONNECTOR_TYPE) == null || "socket".equals(sessionObject.getProperty(CONNECTOR_TYPE))) {
			log.info("Using SocketConnector");
			return new SocketConnector(observable, this.sessionObject);
		} else if ("bosh".equals(sessionObject.getProperty(CONNECTOR_TYPE))) {
			log.info("Using BOSHConnector");
			return new BoshConnector(observable, this.sessionObject);
		} else
			throw new JaxmppException("Unknown connector type");
	}

	@Override
	public void disconnect() throws JaxmppException {
		disconnect(false);
	}

	public void disconnect(boolean snc) throws JaxmppException {
		disconnect(snc, true);
	}

	public void disconnect(boolean snc, boolean resetStreamManagement) throws JaxmppException {
		try {
			if (this.connector != null) {
				try {
					this.connector.stop();
				} catch (XMLException e) {
					throw new JaxmppException(e);
				}
				Boolean sync = (Boolean) this.sessionObject.getProperty(SYNCHRONIZED_MODE);
				if (sync != null && sync) {
					synchronized (Jaxmpp.this) {
						// Jaxmpp.this.wait();
					}
				}
			}
		} finally {
			if (resetStreamManagement) {
				StreamManagementModule.reset(sessionObject);
			}
		}
	}

	@Override
	public void execute(Runnable runnable) {
		if (runnable != null)
			executor.execute(runnable);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConnectionConfiguration getConnectionConfiguration() {
		return new ConnectionConfiguration(this.sessionObject);
	}

	public Executor getExecutor() {
		return executor;
	}

	public FileTransferManager getFileTransferManager() {
		return fileTransferManager;
	}

	public void initFileTransferManager(boolean experimental) throws JaxmppException {
		CapabilitiesModule capsModule = getModule(CapabilitiesModule.class);
		if (capsModule != null && capsModule.getCache() == null) {
			capsModule.setCache(new J2SECapabiliesCache());
		}

		fileTransferManager = new FileTransferManager();
		fileTransferManager.setObservable(observable);
		fileTransferManager.setJaxmpp(this);

		getModulesManager().register(new FileTransferModule(sessionObject));
		getModulesManager().register(new Socks5BytestreamsModule(sessionObject));
		if (experimental) {
			getModulesManager().register(new JingleModule(sessionObject));
			fileTransferManager.addNegotiator(new JingleFileTransferNegotiator());
		}
		fileTransferManager.addNegotiator(new Socks5FileTransferNegotiator());
	}

	@Override
	/**
	 * Connects to server in sync mode. 
	 */
	public void login() throws JaxmppException {
		login(true);
	}

	/**
	 * Connects to server.
	 * 
	 * @param sync
	 *            <code>true</code> to start method in sync mode. In sync mode
	 *            whole connecting process will be done in this method.
	 */
	public void login(boolean sync) throws JaxmppException {
		this.sessionObject.clear(Scope.stream);

		if (this.sessionLogic != null) {
			this.sessionLogic.unbind();
			this.sessionLogic = null;
		}

		this.connectorWrapper.setConnector(createConnector());

		this.sessionLogic = connector.createSessionLogic(modulesManager, this.writer);
		this.sessionLogic.setSessionListener(new SessionListener() {

			@Override
			public void onException(JaxmppException e) throws JaxmppException {
				Jaxmpp.this.onException(e);
			}
		});

		try {
			this.sessionLogic.beforeStart();
			this.connector.start();
			this.sessionObject.setProperty(SYNCHRONIZED_MODE, Boolean.valueOf(sync));
			if (sync) {
				loginTimeoutTask = new LoginTimeoutTask();
				Long delay = sessionObject.getProperty(LOGIN_TIMEOUT_KEY);
				log.finest("Starting LoginTimeoutTask");
				timer.schedule(loginTimeoutTask, delay == null ? 1000 * 60 * 5 : delay);
				synchronized (Jaxmpp.this) {
					Jaxmpp.this.wait();
					log.finest("Waked up");
					Jaxmpp.this.wait(512);
				}

				if (loginTimeoutTask != null) {
					log.finest("Canceling LoginTimeoutTask");
					loginTimeoutTask.cancel();
					loginTimeoutTask = null;
				}
			}
			if (sessionObject.getProperty(EXCEPTION_KEY) != null) {
				JaxmppException r = (JaxmppException) sessionObject.getProperty(EXCEPTION_KEY);
				JaxmppException e = new JaxmppException(r.getMessage(), r.getCause());
				throw e;
			}
		} catch (JaxmppException e) {
			// onException(e);
			throw e;
		} catch (Exception e1) {
			JaxmppException e = new JaxmppException(e1);
			// onException(e);
			throw e;
		}
	}

	public void login(String sid, Long rid, JID bindedJID) throws JaxmppException {

		this.sessionObject.clear(Scope.stream);

		if (this.sessionLogic != null) {
			this.sessionLogic.unbind();
			this.sessionLogic = null;
		}
		
		
		sessionObject.setProperty(Jaxmpp.CONNECTOR_TYPE, "bosh");
		sessionObject.setProperty(AbstractBoshConnector.RID_KEY, rid);
		sessionObject.setProperty(AbstractBoshConnector.SID_KEY, sid);
		sessionObject.setProperty(ResourceBinderModule.BINDED_RESOURCE_JID, bindedJID);

		this.connectorWrapper.setConnector(createConnector());

		sessionObject.setProperty(Connector.CONNECTOR_STAGE_KEY, State.connected);

		this.sessionLogic = connector.createSessionLogic(modulesManager, this.writer);
		this.sessionLogic.setSessionListener(new SessionListener() {

			@Override
			public void onException(JaxmppException e) throws JaxmppException {
				Jaxmpp.this.onException(e);
			}
		});

		try {
			this.sessionLogic.beforeStart();
			this.connector.start();
			if (sessionObject.getProperty(EXCEPTION_KEY) != null) {
				JaxmppException r = (JaxmppException) sessionObject.getProperty(EXCEPTION_KEY);
				JaxmppException e = new JaxmppException(r.getMessage(), r.getCause());
				throw e;
			}
			onResourceBinded(null);
			((BoshXmppSessionLogic) sessionLogic).processResourceBindEvent();
		} catch (JaxmppException e) {
			// onException(e);
			throw e;
		} catch (Exception e1) {
			JaxmppException e = new JaxmppException(e1);
			// onException(e);
			throw e;
		}

	}

	@Override
	protected void modulesInit() {
		super.modulesInit();

		this.modulesManager.register(new CapabilitiesModule(sessionObject, writer,
				this.modulesManager.getModule(DiscoInfoModule.class), this.modulesManager.getModule(PresenceModule.class),
				this.modulesManager));

		SaslModule saslModule = this.modulesManager.getModule(SaslModule.class);
		saslModule.addMechanism(new ExternalMechanism(), true);
	}

	@Override
	protected void onException(JaxmppException e) throws JaxmppException {
		log.log(Level.FINE, "Catching exception", e);
		sessionObject.setProperty(EXCEPTION_KEY, e);
		try {
			connector.stop();
		} catch (Exception e1) {
			log.log(Level.FINE, "Disconnecting error", e1);
		}
		synchronized (Jaxmpp.this) {
			// (new Exception("DEBUG")).printStackTrace();
			Jaxmpp.this.notify();
		}
		JaxmppEvent event = new JaxmppEvent(Disconnected, sessionObject);
		observable.fireEvent(event);
	}

	@Override
	protected void onResourceBinded(ResourceBindEvent be) throws JaxmppException {
		synchronized (Jaxmpp.this) {
			// (new Exception("DEBUG")).printStackTrace();
			Jaxmpp.this.notify();
		}
		JaxmppEvent event = new JaxmppEvent(Connected, sessionObject);
		observable.fireEvent(event);
	}

	@Override
	protected void onStreamError(ConnectorEvent be) throws JaxmppException {
		synchronized (Jaxmpp.this) {
			Jaxmpp.this.notify();
		}
		JaxmppEvent event = new JaxmppEvent(Disconnected, sessionObject);
		observable.fireEvent(event);
	}

	@Override
	protected void onStreamResumed(StreamResumedEvent be) throws JaxmppException {
		synchronized (Jaxmpp.this) {
			// (new Exception("DEBUG")).printStackTrace();
			Jaxmpp.this.notify();
		}
		JaxmppEvent event = new JaxmppEvent(Connected, sessionObject);
		observable.fireEvent(event);
	}

	@Override
	protected void onStreamTerminated(ConnectorEvent be) throws JaxmppException {
		synchronized (Jaxmpp.this) {
			// (new Exception("DEBUG")).printStackTrace();
			Jaxmpp.this.notify();
		}
		JaxmppEvent event = new JaxmppEvent(Disconnected, sessionObject);
		observable.fireEvent(event);
	}

	/**
	 * Sets custom {@linkplain Executor} for processing incoming stanzas in
	 * modules.
	 * 
	 * @param executor
	 *            executor
	 */
	public void setExecutor(Executor executor) {
		if (executor == null)
			this.executor = DEFAULT_EXECUTOR;
		else
			this.executor = executor;
	}

}
