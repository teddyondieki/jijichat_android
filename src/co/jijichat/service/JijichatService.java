package co.jijichat.service;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocketFactory;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.JaxmppCore.JaxmppEvent;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.DiscoInfoAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoInfoModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import co.jijichat.Constants;
import co.jijichat.Jaxmpp;
import co.jijichat.MessengerApplication;
import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.Preferences;
import co.jijichat.R;
import co.jijichat.authenticator.AuthenticatorActivity;
import co.jijichat.db.AccountsTableMetaData;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.MucTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;
import co.jijichat.db.providers.MucProvider;
import co.jijichat.ui.NotificationHelper;

public class JijichatService extends Service {

	private static boolean serviceActive = false;
	private static final boolean DEBUG = false;
	private static final String TAG = "JijichatService";
	private static final String ACTION_KEEPALIVE = "co.jijichat.service.JijichatService.KEEP_ALIVE";
	public static final String MUC_ERROR_MSG = "co.jijichat.MUC_ERROR_MSG";

	private static String nickname = "";

	protected static boolean focused;

	private long keepAliveInterval = 1 * 60 * 1000; // 60 seconds

	private final HashMap<BareJID, Integer> connectionErrorsCounter = new HashMap<BareJID, Integer>();

	private int usedNetworkType = -1;
	private boolean reconnect = true;

	private static Executor executor = new StanzaExecutor();
	private AccountModifyReceiver accountModifyReceiver;
	private ClientFocusReceiver focusChangeReceiver;

	protected NotificationHelper notificationHelper;
	protected final Timer timer = new Timer();

	public long currentRoomIdFocus = -1;

	private final Listener<JaxmppEvent> jaxmppConnected;
	private final Listener<Connector.ConnectorEvent> stateChangeListener;
	private final Listener<PresenceEvent> presenceSendListener;
	private Listener<MucEvent> mucListener;

	private Listener<ConnectorEvent> connectorListener;

	private final static Set<SessionObject> locked = new HashSet<SessionObject>();

	private ConnReceiver jijiConnReceiver;

	private ConnectivityManager connManager;

	public JijichatService() {
		super();
		if (DEBUG) {
			Log.i(TAG, "creating");
		}

		this.mucListener = new Listener<MucModule.MucEvent>() {

			@Override
			public void handleEvent(MucModule.MucEvent be)
					throws JaxmppException {

				if (be.getType() == MucModule.MucMessageReceived
						&& be.getRoom() != null && be.getMessage() != null
						&& be.getMessage().getBody() != null) {

					if (be.getNickname().equals(nickname)) {
						storeReceivedReceiptMuc(be);
					} else {
						String msg = be.getMessage().getBody();
						Uri uri = Uri.parse(ChatHistoryProvider.ROOM_URI + "/"
								+ be.getRoom().getRoomJid().toString());

						ContentValues values = new ContentValues();
						values.put(ChatTableMetaData.FIELD_JID, be.getRoom()
								.getRoomJid().toString());

						values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME,
								be.getNickname());

						values.put(ChatTableMetaData.FIELD_TIMESTAMP, be
								.getDate().getTime());

						// update room's last date to be used in sending message
						// in
						// mucroomfragment
						be.getRoom().setLastMessageDate(be.getDate());

						values.put(ChatTableMetaData.FIELD_CHAT_ROOM_ID, be
								.getRoom().getId());
						values.put(ChatTableMetaData.FIELD_BODY, msg);
						values.put(ChatTableMetaData.FIELD_ACCOUNT, be
								.getSessionObject().getUserBareJid().toString());
						values.put(
								ChatTableMetaData.FIELD_STATE,
								currentRoomIdFocus != be.getRoom().getId() ? ChatTableMetaData.STATE_INCOMING_UNREAD
										: ChatTableMetaData.STATE_INCOMING);

						showMucMessageNotification(be);

						getContentResolver().insert(uri, values);
					}
				} else if (be.getType() == MucModule.RoomConfigurationChanged) {
					onMucRoomConfigChanged(be);
				}
			}
		};

		this.stateChangeListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(final Connector.ConnectorEvent be)
					throws JaxmppException {
				State st = getState(be.getSessionObject());
				if (DEBUG)
					Log.d(TAG, "New connection state for "
							+ be.getSessionObject().getUserBareJid() + ": "
							+ st);

				if (st == State.connected)
					setConnectionError(be.getSessionObject().getUserBareJid(),
							0);
				if (st == State.disconnected)
					reconnectIfAvailable(be.getSessionObject());
			}
		};

		this.connectorListener = new Listener<Connector.ConnectorEvent>() {

			@Override
			public void handleEvent(ConnectorEvent be) throws JaxmppException {
				if (be.getType() == Connector.Error) {
					if (DEBUG)
						Log.d(TAG, "Connection error ("
								+ be.getSessionObject().getUserBareJid() + ") "
								+ be.getCaught() + "  " + be.getStreamError());

					onConnectorError(be);
				} else if (be.getType() == Connector.StreamTerminated) {
					if (DEBUG) {
						Log.d(TAG, "Stream terminated ("
								+ be.getSessionObject().getUserBareJid() + ") "
								+ be.getStreamError());
					}

				}
			}
		};

		this.presenceSendListener = new Listener<PresenceModule.PresenceEvent>() {

			@Override
			public void handleEvent(PresenceEvent be) throws JaxmppException {
				updateStatus(be);
			}
		};

		this.jaxmppConnected = new Listener<JaxmppEvent>() {

			@Override
			public void handleEvent(JaxmppEvent be) throws JaxmppException {
				nickname = be.getSessionObject().getUserProperty(
						SessionObject.NICKNAME);
				rejoinToRooms(be.getSessionObject());
				// allow 3 seconds for room to rejoin before sending unsent
				// messages
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				sendUnsentMessages();
			}
		};

	}

	private void rejoinToRooms(final SessionObject sessionObject) {

		if (DEBUG) {
			Log.d(TAG, "Rejoining rooms.");

		}
		try {

			for (Room r : getRooms()) {
				if (r.getSessionObject() == sessionObject
						&& r.getState() != tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State.joined) {
					r.rejoin();
				}
			}
		} catch (JaxmppException e) {
			if (DEBUG) {
				Log.e(TAG, "Problem on rejoining", e);
			}
		}
	}

	public static boolean isServiceActive() {
		return serviceActive;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		if (DEBUG) {
			Log.i(TAG, "onCreate()");
		}
		setUsedNetworkType(-1);
		setReconnect(true);

		this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		this.jijiConnReceiver = new ConnReceiver();
		IntentFilter filter = new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(jijiConnReceiver, filter);
		this.accountModifyReceiver = new AccountModifyReceiver();
		filter = new IntentFilter(AuthenticatorActivity.ACCOUNT_MODIFIED_MSG);
		registerReceiver(accountModifyReceiver, filter);
		this.focusChangeReceiver = new ClientFocusReceiver();
		filter = new IntentFilter(
				JijichatMobileMessengerActivity.CLIENT_FOCUS_MSG);
		registerReceiver(focusChangeReceiver, filter);

		getJaxmpp().addListener(JaxmppCore.Connected, this.jaxmppConnected);

		getJaxmpp().addListener(PresenceModule.BeforeInitialPresence,
				this.presenceSendListener);

		getJaxmpp().addListener(MucModule.MucMessageReceived, this.mucListener);

		getJaxmpp().addListener(MucModule.RoomConfigurationChanged,
				this.mucListener);

		getJaxmpp().addListener(Connector.StateChanged,
				this.stateChangeListener);
		getJaxmpp().addListener(Connector.Error, this.connectorListener);
		getJaxmpp().addListener(Connector.StreamTerminated,
				this.connectorListener);

		updateJaxmppInstance(getJaxmpp(), getResources(),
				getApplicationContext());

		startKeepAlive();

		notificationHelper = NotificationHelper.createInstance(this);

	}

	// added to fix Eclipse error
	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (intent != null && intent.getAction() != null) {
			if (intent.getAction().equals(ACTION_KEEPALIVE)) {
				keepAlive();
			}
		} else {
			if (intent != null) {
				JijichatService.focused = intent.getBooleanExtra("focused",
						false);
			}

			serviceActive = true;
			connectJaxmpp(getJaxmpp(), (Long) null);
		}
		return START_STICKY;
	}

	private class AccountModifyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			final Jaxmpp j = getJaxmpp();
			updateJaxmppInstance(getJaxmpp(), getResources(),
					getApplicationContext());
			nickname = j.getSessionObject().getUserProperty(
					SessionObject.NICKNAME);
			State st = getState(j.getSessionObject());
			if (st == State.disconnected || st == null) {
				connectJaxmpp(j, (Long) null);
			}
			final Runnable r = new Runnable() {

				@Override
				public void run() {
					try {
						for (Room r : getRooms()) {
							Presence presence = Presence.create();
							presence.setFrom(j.getSessionObject()
									.getBindedJid());
							presence.setTo(r.getRoomJid() + "/" + nickname);
							j.send(presence);
						}
					} catch (JaxmppException e) {
						if (DEBUG)
							Log.e(TAG, "Problem updating nickname", e);
					}
				}

			};

			(new Thread(r)).start();

		}
	}

	private class ConnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo netInfo = ((ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getActiveNetworkInfo();
			onNetworkChanged(netInfo);
		}
	}

	public static void updateJaxmppInstance(Jaxmpp jaxmpp, Resources resources,
			Context context) {

		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager
				.getAccountsByType(Constants.ACCOUNT_TYPE);
		if (accounts.length > 0) {

			Account account = accounts[0];

			final MessengerApplication app = (MessengerApplication) context
					.getApplicationContext();

			app.getJaxmpp().setExecutor(executor);

			SessionObject sessionObject = app.getJaxmpp().getSessionObject();

			BareJID jid = BareJID.bareJIDInstance(accountManager.getUserData(
					account, AccountsTableMetaData.FIELD_JID));
			String password = accountManager.getPassword(account);
			// Log.d("password", password);

			String nickname = accountManager.getUserData(account,
					AccountsTableMetaData.FIELD_NICKNAME);

			int currentVersion;
			try {
				currentVersion = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
				currentVersion = 0;
			}

			String clientKey = jid.toString() + currentVersion;

			sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY,
					resources.getString(R.string.app_version));

			// bloater?? Nope. User in client-ver xml stream
			sessionObject.setUserProperty(Constants.VERSION_CODE,
					currentVersion);

			sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY,
					resources.getString(R.string.app_name));
			sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY,
					"Android " + android.os.Build.VERSION.RELEASE);

			sessionObject.setUserProperty(
					DiscoInfoModule.IDENTITY_CATEGORY_KEY, "client");
			sessionObject.setUserProperty(DiscoInfoModule.IDENTITY_TYPE_KEY,
					"phone");
			// sessionObject.setUserProperty(CapabilitiesModule.NODE_NAME_KEY,
			// "http://jijichat.com/messenger");

			sessionObject.setUserProperty("ID", (long) account.hashCode());
			sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
			sessionObject.setUserProperty(
					tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
			sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY,
					true);

			SSLSessionCache sslSessionCache = new SSLSessionCache(context);
			SSLSocketFactory sslSocketFactory = SSLCertificateSocketFactory
					.getDefault(0, sslSessionCache);
			sessionObject.setUserProperty(
					SocketConnector.SSL_SOCKET_FACTORY_KEY, sslSocketFactory);

			sessionObject.setUserProperty(SessionObject.USER_BARE_JID, jid);
			sessionObject.setUserProperty(SessionObject.NICKNAME, nickname);

			sessionObject.setUserProperty(SessionObject.PASSWORD, password);
			sessionObject.setUserProperty(Constants.CLIENT_KEY,
					encrypt(clientKey, "MD5"));

			sessionObject.setUserProperty(SocketConnector.SERVER_HOST,
					Constants.HOST_NAME);

			sessionObject.setUserProperty(SessionObject.RESOURCE,
					Constants.RESOURCE);
			sessionObject.setUserProperty(JaxmppCore.AUTOADD_STANZA_ID_KEY,
					Boolean.TRUE);
		}

	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Date delay) {
		if (DEBUG)
			Log.d(TAG, "Preparing to start account "
					+ jaxmpp.getSessionObject().getUserBareJid());

		if (isLocked(jaxmpp.getSessionObject())) {
			if (DEBUG)
				Log.d(TAG, "Skip connection for account "
						+ jaxmpp.getSessionObject().getUserBareJid()
						+ ". Locked.");
			return;
		}

		if (jaxmpp.getSessionObject().getUserProperty(SessionObject.NICKNAME) == null) {
			return;
		}

		final Runnable r = new Runnable() {

			@Override
			public void run() {
				if (isDisabled(jaxmpp.getSessionObject())) {
					if (DEBUG)
						Log.d(TAG, "Account"
								+ jaxmpp.getSessionObject().getUserBareJid()
								+ " disabled. Connection skipped.");
					return;
				}
				if (DEBUG)
					Log.d(TAG, "Start connection for account "
							+ jaxmpp.getSessionObject().getUserBareJid());
				lock(jaxmpp.getSessionObject(), false);
				setUsedNetworkType(getActiveNetworkConnectionType());
				if (getUsedNetworkType() != -1) {
					final State state = jaxmpp.getSessionObject().getProperty(
							Connector.CONNECTOR_STAGE_KEY);
					if (state == null || state == State.disconnected)
						(new Thread() {
							@Override
							public void run() {

								try {
									jaxmpp.getSessionObject().setProperty(
											"messenger#error", null);
									jaxmpp.login(true);
								} catch (Exception e) {
									incrementConnectionError(jaxmpp
											.getSessionObject()
											.getUserBareJid());

									Log.e(TAG, "Can't connect account "
											+ jaxmpp.getSessionObject()
													.getUserBareJid(), e);
								}
							}
						}).start();
				}
			}
		};

		lock(jaxmpp.getSessionObject(), true);
		if (delay == null)
			r.run();
		else {
			if (DEBUG)
				Log.d(TAG, "Shedule (time=" + delay
						+ ") connection for account "
						+ jaxmpp.getSessionObject().getUserBareJid());
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					r.run();
				}
			}, delay);
		}
	}

	protected final State getState(SessionObject object) {
		State state = getJaxmpp().getSessionObject().getProperty(
				Connector.CONNECTOR_STAGE_KEY);
		return state == null ? State.disconnected : state;
	}

	protected final Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	@Override
	public void onDestroy() {
		serviceActive = false;
		timer.cancel();

		if (focusChangeReceiver != null)
			unregisterReceiver(focusChangeReceiver);

		if (jijiConnReceiver != null)
			unregisterReceiver(jijiConnReceiver);

		if (accountModifyReceiver != null)
			unregisterReceiver(accountModifyReceiver);

		setReconnect(false);
		disconnectJaxmpp(true);
		stopKeepAlive();
		setUsedNetworkType(-1);

		getJaxmpp().removeListener(PresenceModule.BeforeInitialPresence,
				this.presenceSendListener);
		getJaxmpp().removeListener(JaxmppCore.Connected, this.jaxmppConnected);

		getJaxmpp().removeListener(MucModule.MucMessageReceived,
				this.mucListener);
		getJaxmpp().removeListener(MucModule.RoomConfigurationChanged,
				this.mucListener);

		getJaxmpp().removeListener(Connector.StateChanged,
				this.stateChangeListener);
		getJaxmpp().removeListener(Connector.Error, this.connectorListener);
		getJaxmpp().removeListener(Connector.StreamTerminated,
				this.connectorListener);

		notificationHelper.cancelNotification();

		super.onDestroy();
	}

	private void disconnectJaxmpp(final boolean cleaning) {
		final Jaxmpp j = getJaxmpp();
		(new Thread() {
			@Override
			public void run() {
				try {
					j.disconnect(false);
				} catch (Exception e) {
					Log.e(TAG, "cant; disconnect account "
							+ j.getSessionObject().getUserBareJid(), e);
				}
			}
		}).start();
		;
	}

	public void onNetworkChanged(final NetworkInfo netInfo) {
		if (DEBUG) {
			Log.d(TAG,
					"Network "
							+ (netInfo == null ? null : netInfo.getTypeName())
							+ " ("
							+ (netInfo == null ? null : netInfo.getType())
							+ ") state changed! Currently used="
							+ getUsedNetworkType()
							+ " detailed state = "
							+ (netInfo != null ? netInfo.getDetailedState()
									: null));
		}

		if (netInfo != null && netInfo.isConnected()) {
			if (DEBUG)
				Log.d(TAG, "Network became available");
			setReconnect(true);
			connectJaxmpp(getJaxmpp(), Long.valueOf(5000));
		} else {
			if (DEBUG)
				Log.d(TAG, "No internet connection");
			setReconnect(false);
			disconnectJaxmpp(false);
		}
	}

	private void setUsedNetworkType(int type) {
		if (DEBUG)
			Log.d(TAG, "Used NetworkType is now " + type,
					new Exception("TRACE"));
		usedNetworkType = type;
	}

	private int getUsedNetworkType() {
		return usedNetworkType;
	}

	private void setReconnect(boolean reconnectAvailable) {
		if (DEBUG)
			Log.d(TAG, "Reconnect is now set to " + reconnectAvailable,
					new Exception("TRACE"));
		this.reconnect = reconnectAvailable;
	}

	private void startKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JijichatService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + keepAliveInterval,
				keepAliveInterval, pi);
	}

	private void stopKeepAlive() {
		Intent i = new Intent();
		i.setClass(this, JijichatService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	protected void updateStatus(PresenceEvent be) throws XMLException {
		if (focused) {
			be.setShow(Show.online);
		} else {
			be.setShow(Show.away);
		}
	}

	private boolean isReconnect() {
		return reconnect;
	}

	protected void reconnectIfAvailable(final SessionObject sessionObject) {
		if (!isReconnect()) {
			if (DEBUG)
				Log.d(TAG,
						"Reconnect disabled for: "
								+ sessionObject.getUserBareJid());
			return;
		}

		if (DEBUG)
			Log.d(TAG,
					"Preparing for reconnect " + sessionObject.getUserBareJid());

		final Jaxmpp j = getJaxmpp();

		int connectionErrors = getConnectionError(j.getSessionObject()
				.getUserBareJid());

		if (connectionErrors > 30) {
			disable(sessionObject, true);
		} else
			connectJaxmpp(j, calculateNextRestart(5, connectionErrors));
	}

	public static void disable(SessionObject jaxmpp, boolean disabled) {
		if (DEBUG)
			Log.d(TAG, "Account " + jaxmpp.getUserBareJid() + " disabled="
					+ disabled);
		jaxmpp.setProperty("CC:DISABLED", disabled);
	}

	private int getConnectionError(final BareJID jid) {
		synchronized (connectionErrorsCounter) {
			Integer x = connectionErrorsCounter.get(jid);
			return x == null ? 0 : x.intValue();
		}
	}

	protected void onConnectorError(final ConnectorEvent be) {
		if (DEBUG)
			Log.d("TAG", "onConnectorError: " + be.getStreamError().toString());
		be.getSessionObject()
				.setProperty(
						"messenger#error",
						be.getStreamError() != null ? be.getStreamError()
								.name() : null);
		if ((be.getStreamError() == StreamError.unsupported_version)) {
			disable(be.getSessionObject(), true);
			try {
				int currentVersion = getPackageManager().getPackageInfo(
						getPackageName(), 0).versionCode;
				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putInt(Preferences.DISABLED_VERSION, currentVersion)
						.commit();
			} catch (Exception e) {
			}
		} else if ((be.getStreamError() == StreamError.host_unknown)
				|| (be.getStreamError() == StreamError.improper_addressing)) {
			disable(be.getSessionObject(), true);
		} else if (be.getCaught() != null) {
			Throwable throwable = extractCauseException(be.getCaught());
			be.getSessionObject().setProperty("messenger#error",
					throwable.getMessage());

			if (throwable instanceof UnknownHostException) {
				if (DEBUG)
					Log.w(TAG, "Skipped UnknownHostException exception",
							throwable);
				// notificationUpdateFail(be.getSessionObject(),
				// "Connection error: unknown host " + throwable.getMessage(),
				// null,
				// null);
				// disable(be.getSessionObject(), true);
			} else if (throwable instanceof SocketException) {
				if (DEBUG)
					Log.w(TAG, "Skipped SocketException exception", throwable);
			} else {
				if (DEBUG)
					Log.w(TAG, "Skipped exception", throwable);
				// Log.e(TAG, "Connection error!", throwable);
				// notificationUpdateFail(be.getSessionObject(), null, null,
				// throwable);
				// disable(be.getSessionObject(), true);
			}
		} else {
			try {
				if (DEBUG)
					Log.w(TAG, "Ignored ConnectorError: "
							+ (be.getStanza() == null ? "???" : be.getStanza()
									.getAsString()));

			} catch (XMLException e) {
				if (DEBUG)
					Log.e(TAG, "Can't display exception", e);
			}
		}
	}

	private static Throwable extractCauseException(Throwable ex) {
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

	private void setConnectionError(final BareJID jid, final int count) {
		if (DEBUG)
			Log.d(TAG, "Error counter for " + jid + " is now " + count);
		synchronized (connectionErrorsCounter) {
			if (count == 0)
				connectionErrorsCounter.remove(jid);
			else
				connectionErrorsCounter.put(jid, count);
		}
	}

	private static boolean isLocked(SessionObject jaxmpp) {
		synchronized (locked) {
			return locked.contains(jaxmpp);
		}
	}

	private static void lock(SessionObject jaxmpp, boolean value) {
		synchronized (locked) {
			if (DEBUG)
				Log.d(TAG, "Account " + jaxmpp.getUserBareJid() + " locked="
						+ value);

			if (value)
				locked.add(jaxmpp);
			else
				locked.remove(jaxmpp);
			// jaxmpp.setProperty("CC:LOCKED", locked);
		}
	}

	private void keepAlive() {
		new Thread() {
			@Override
			public void run() {
				JaxmppCore jaxmpp = getJaxmpp();
				try {
					if (jaxmpp.isConnected()) {
						jaxmpp.getConnector().keepalive();
					}
				} catch (JaxmppException ex) {
					if (DEBUG)
						Log.e(TAG, "error sending keep alive for = "
								+ jaxmpp.getSessionObject().getUserBareJid()
										.toString(), ex);
				}
			}
		}.start();
	}

	public static boolean isDisabled(SessionObject jaxmpp) {
		Boolean x = jaxmpp.getProperty("CC:DISABLED");
		return x == null ? false : x;
	}

	private int getActiveNetworkConnectionType() {
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info == null)
			return -1;
		if (!info.isConnected())
			return -1;
		return info.getType();
	}

	private int incrementConnectionError(final BareJID jid) {
		synchronized (connectionErrorsCounter) {
			Integer x = connectionErrorsCounter.get(jid);
			int z = x == null ? 0 : x.intValue();
			++z;
			connectionErrorsCounter.put(jid, z);
			if (DEBUG) {
				Log.d(TAG, "Error counter for " + jid + " is now " + z);
			}
			return z;
		}
	}

	private void connectJaxmpp(final Jaxmpp jaxmpp, final Long delay) {
		connectJaxmpp(
				jaxmpp,
				delay == null ? null : new Date(delay
						+ System.currentTimeMillis()));
	}

	private static Date calculateNextRestart(final int delayInSecs,
			final int errorCounter) {
		long timeInSecs = delayInSecs;
		if (errorCounter > 20) {
			timeInSecs += 60 * 5;
		} else if (errorCounter > 10) {
			timeInSecs += 120;
		} else if (errorCounter > 5) {
			timeInSecs += 60;
		}

		Date d = new Date((new Date()).getTime() + 1000 * timeInSecs);
		return d;
	}

	private class ClientFocusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			notificationHelper.cancelAllNotifications();

			final long roomId = intent.getLongExtra("roomId", -1);

			if (roomId != -1) {
				currentRoomIdFocus = roomId;
			} else {
				currentRoomIdFocus = -1;
			}
		}
	}

	protected void onMucRoomConfigChanged(final MucEvent be)
			throws XMLException {

		Runnable r = new Runnable() {
			@Override
			public void run() {
				final Jaxmpp jaxmpp = getJaxmpp();

				try {
					jaxmpp.getModule(DiscoInfoModule.class).getInfo(
							JID.jidInstance(be.getRoom().getRoomJid()),
							new DiscoInfoAsyncCallback(null) {

								@Override
								public void onError(Stanza responseStanza,
										ErrorCondition error)
										throws JaxmppException {
									// TODO Auto-generated method stub

								}

								@Override
								public void onTimeout() throws JaxmppException {
									// TODO Auto-generated method stub

								}

								@Override
								protected void onInfoReceived(String node,
										Collection<Identity> identities,
										Collection<String> features)
										throws XMLException {
									for (Identity i : identities) {
										final BareJID roomJid = be.getRoom()
												.getRoomJid();

										Uri uri = Uri
												.parse(MucProvider.CONTENT_URI
														+ "/"
														+ roomJid.toString());
										ContentValues values = new ContentValues();
										values.put(
												MucTableMetaData.FIELD_ROOM_NAME,
												i.getName());
										values.put(
												MucTableMetaData.FIELD_ROOM_DESCRIPTION,
												i.getDesc());

										getContentResolver().update(uri,
												values, null, null);
										break;
									}
								}
							});

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		(new Thread(r)).start();
	}

	protected void showMucMessageNotification(final MucEvent be)
			throws XMLException {
		String nick = be.getSessionObject().getUserProperty(
				SessionObject.NICKNAME);
		String msg = be.getMessage().getBody();

		if (msg.toLowerCase(Locale.US).contains(nick.toLowerCase(Locale.US))) {
			if (currentRoomIdFocus != be.getRoom().getId()) {
				notificationHelper.notifyNewMucMessage(be);
			}
		} else {
			if (currentRoomIdFocus != be.getRoom().getId()) {
				notificationHelper.notifyNewMucMention(be);
			}
		}
	}

	protected Collection<Room> getRooms() {
		return getJaxmpp().getModule(MucModule.class).getRooms();
	}

	public static String encrypt(String data, String algorithm) {
		String md5 = null;
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			if (data != null) {
				md.update(data.getBytes());
			}
			md5 = bytesToHex(md.digest());
		} catch (Exception ex) {
		}
		return md5;
	}

	public static final String bytesToHex(final byte[] buff) {
		StringBuilder res = new StringBuilder();
		for (byte b : buff) {
			char ch = Character.forDigit((b >> 4) & 0xF, 16);
			res.append(ch);
			ch = Character.forDigit(b & 0xF, 16);
			res.append(ch);
		} // end of for (b : digest)
		return res.toString();
	}

	protected void storeReceivedReceiptMuc(MucModule.MucEvent be)
			throws XMLException {
		final String confirmedId = be.getMessage().getId();
		final BareJID roomJID = be.getRoom().getRoomJid();

		Uri uri = Uri.parse(ChatHistoryProvider.CONFIRM_RECEIVING_URI + "/"
				+ roomJID.toString() + ("?id=" + confirmedId));
		ContentValues values = new ContentValues();
		values.put(ChatTableMetaData.FIELD_STATE,
				ChatTableMetaData.STATE_OUT_SENT);
		values.put(ChatTableMetaData.FIELD_TIMESTAMP, be.getDate().getTime());

		getContentResolver().update(uri, values, null, null);
	}

	protected void sendUnsentMessages() {
		final Cursor c = getApplication().getContentResolver().query(
				Uri.parse(ChatHistoryProvider.UNSENT_MESSAGES_URI), null, null,
				null, null);

		try {
			c.moveToFirst();
			if (c.isAfterLast())
				return;
			do {
				String messageId = c.getString(c
						.getColumnIndex(ChatTableMetaData.FIELD_MESSAGE_ID));
				String body = c.getString(c
						.getColumnIndex(ChatTableMetaData.FIELD_BODY));
				final BareJID roomJID = BareJID.bareJIDInstance(c.getString(c
						.getColumnIndex(ChatTableMetaData.FIELD_JID)));

				if (DEBUG) {
					Log.i(TAG, "Found unsent message: " + roomJID.toString()
							+ " :: " + body);
				}
				try {
					findRoom(roomJID).sendMessage(body, messageId);
				} catch (Exception e) {
					if (DEBUG)
						Log.d(TAG, "Can't send message");
				}

				c.moveToNext();
			} while (!c.isAfterLast());
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "WTF??", e);
			}
		} finally {
			c.close();
		}
	}

	protected Room findRoom(BareJID roomJID) {
		return getJaxmpp().getModule(MucModule.class).getRoomsMap()
				.get(roomJID);
	}

}
