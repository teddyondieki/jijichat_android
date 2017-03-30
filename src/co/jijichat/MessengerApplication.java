package co.jijichat;

import co.jijichat.db.providers.DBMUCManager;
import co.jijichat.service.JijichatService;
import co.jijichat.utils.AvatarHelper;
import co.jijichat.utils.MessageHelper;
import co.jijichat.utils.RosterNameHelper;

import com.digits.sdk.android.Digits;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;

import io.fabric.sdk.android.Fabric;

import java.util.Timer;
import java.util.TimerTask;

import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.Connector.ConnectorEvent;
import tigase.jaxmpp.core.client.Connector.State;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.factory.UniversalFactory;
import tigase.jaxmpp.core.client.factory.UniversalFactory.FactorySpi;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.muc.AbstractRoomsManager;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MessengerApplication extends Application {

	// Note: Your consumer key and secret should be obfuscated in your source code before shipping.
	private static final String TWITTER_KEY = "";
	private static final String TWITTER_SECRET = "";
	
	public static MessengerApplication app;
	private static Jaxmpp jaxmpp;
	private final Timer timer = new Timer(true);

	public MessengerApplication() {
		super();
		app = this;

		final Context context = this;

		UniversalFactory.setSpi(AbstractRoomsManager.class.getName(),
				new FactorySpi<AbstractRoomsManager>() {

					@Override
					public AbstractRoomsManager create() {
						return new DBMUCManager(context);
					}
				});

	}

	public Jaxmpp getJaxmpp() {
		if (jaxmpp == null) {
			createJaxmpp();
		}
		return jaxmpp;
	}

	public void setJaxmpp(final Jaxmpp newJaxmpp) {
		jaxmpp = null;
		jaxmpp = newJaxmpp;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
		Fabric.with(this, new TwitterCore(authConfig), new Digits());

		MessageHelper.initialize(getApplicationContext());
		RosterNameHelper.initialize();
		AvatarHelper.initialize();
	}

	private void createJaxmpp() {
		jaxmpp = new Jaxmpp();

		jaxmpp.addListener(Connector.StateChanged,
				new Listener<Connector.ConnectorEvent>() {

					@Override
					public void handleEvent(final ConnectorEvent be)
							throws JaxmppException {

						if (getState(be.getSessionObject()) == State.disconnected) {
							clearPresences(be.getSessionObject(), true);
						}
					}
				});
		JijichatService.updateJaxmppInstance(getJaxmpp(), getResources(), this);
	}

	protected final State getState(SessionObject object) {
		State state = jaxmpp.getSessionObject().getProperty(
				Connector.CONNECTOR_STAGE_KEY);
		return state == null ? State.disconnected : state;
	}

	public synchronized void clearPresences(final SessionObject sessionObject,
			boolean delayed) {
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				try {
					if (getState(sessionObject) == State.disconnected) {
						jaxmpp.getPresence().clear();
					}
				} catch (JaxmppException e) {
					e.printStackTrace();
				}
			}
		};

		if (delayed) {
			timer.schedule(tt, 1000 * StreamManagementModule.getResumptionTime(
					sessionObject, 1));
		} else
			tt.run();
	}
}
