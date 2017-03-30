package co.jijichat;

import java.util.HashMap;
import java.util.List;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.AbstractMessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import co.jijichat.R;
import co.jijichat.authenticator.AuthenticatorActivity;
import co.jijichat.authenticator.InitializationActivity;
import co.jijichat.db.AccountsTableMetaData;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;
import co.jijichat.db.providers.MucProvider;
import co.jijichat.muc.MucRoomActivity;
import co.jijichat.preferences.MessengerPreferenceActivity;
import co.jijichat.search.RoomSearchActivity;
import co.jijichat.service.JijichatService;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

public class JijichatMobileMessengerActivity extends SherlockFragmentActivity {

	// for notification intents :)))
	public static final String MUC_ERROR_ACTION = "co.jijichat.MUC_ERROR_ACTION";
	public static final String MUC_MESSAGE_ACTION = "co.jijichat.MUC_MESSAGE_ACTION";

	private static final boolean DEBUG = false;
	private static final String TAG = "JijichatMobileMessengerActivity";
	// I don't know this either. For notification??
	public static final String ERROR_ACTION = null;

	public static final String CLIENT_FOCUS_MSG = "co.jijichat.CLIENT_FOCUS_MSG";

	private final Listener<BaseEvent> chatListener;

	// protected static boolean changePresence = true;

	private ViewPager pager;
	private ActionBar actionBar;

	private SharedPreferences mPreferences;

	public JijichatMobileMessengerActivity() {
		super();
		if (DEBUG)
			Log.d(TAG, "JijichatMobileMessengerActivity()");

		this.chatListener = new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) throws JaxmppException {
				if (be instanceof AbstractMessageEvent) {
					onMessageEvent((AbstractMessageEvent) be);
				}
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);

		this.mPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		try {
			int disabledVersion = mPreferences.getInt(
					Preferences.DISABLED_VERSION, 0);
			// assuming current version value can be at least 1
			int currentVersion = getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionCode;
			if (currentVersion <= disabledVersion) {
				Intent intent = new Intent(this,
						UnsupportedVersionActivity.class);
				startActivity(intent);
				finish();
			}
		} catch (Exception e) {
			// do nothing
		}

		// Start JijichatService if not active
		if (!JijichatService.isServiceActive()) {
			Intent intent = new Intent(this, JijichatService.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(intent);
		}

		// Set account/nickname if empty
		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager
				.getAccountsByType(Constants.ACCOUNT_TYPE);
		if ((accounts == null) || (accounts.length == 0)) {
			Intent intent = new Intent(this, AuthenticatorActivity.class);
			startActivity(intent);
			finish();
		} else {
			Account account = accounts[0];
			String nickname = accountManager.getUserData(account,
					AccountsTableMetaData.FIELD_NICKNAME);
			if (nickname == null) {
				Intent intent = new Intent(this, InitializationActivity.class);
				intent.putExtra("startActivity", true);
				startActivity(intent);
				finish();
			}
		}

		actionBar = getSupportActionBar();
		actionBar.setTitle("Chats");

		setContentView(R.layout.activity_jiji_mobile_messenger);

		// TODO rid pager
		this.pager = (ViewPager) findViewById(R.id.pager);
		processingNotificationIntent(getIntent());

		ViewPager.SimpleOnPageChangeListener ViewPagerListener = new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				actionBar.setSelectedNavigationItem(position);
			}
		};

		this.pager.setOnPageChangeListener(ViewPagerListener);

		MyFragmentPagerAdapter viewpageradapter = new MyFragmentPagerAdapter(
				getSupportFragmentManager());

		this.pager.setAdapter(viewpageradapter);

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) {
			Log.d(TAG, "onResume()");
		}

		final Jaxmpp jaxmpp = getJaxmpp();
		jaxmpp.addListener(this.chatListener);
		loadUnreadCounters();
		notifyInsertedMessage(MucProvider.CONTENT_URI);
	}

	@Override
	protected void onPause() {
		final Jaxmpp jaxmpp = getJaxmpp();
		jaxmpp.removeListener(this.chatListener);

		super.onPause();
		if (DEBUG)
			Log.d(TAG, "onPause()");
	}

	@Override
	public void onActivityResult(int resultCode, int requestCode, Intent data) {
		if (DEBUG)
			Log.d("ResultCode", "resultCode: " + resultCode);
		super.onActivityResult(resultCode, requestCode, data);

	}

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	protected List<Chat> getChatList() {
		return getJaxmpp().getModule(MessageModule.class).getChatManager()
				.getChats();
	}

	protected boolean findRoom(BareJID roomJID) {
		return getJaxmpp().getModule(MucModule.class).getRoomsMap()
				.containsKey(roomJID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {

		if (item.getItemId() == R.id.settings) {
			Intent intent = new Intent().setClass(this,
					MessengerPreferenceActivity.class);
			this.startActivityForResult(intent, 0);
			return true;
		} else if (item.getItemId() == R.id.refreshRoomsList) {
			Intent intent = new Intent(this, RoomSearchActivity.class);
			this.startActivity(intent);
			return true;
		}

		return false;

	}

	protected void onMessageEvent(final AbstractMessageEvent be)
			throws XMLException {

		try {
			if (DEBUG)
				Log.v(TAG, "onMessageEvent():  "
						+ be.getMessage().getAsString());

			if ((((MucEvent) be).getRoom() != null)
					&& (be.getMessage().getBody() != null)) {
				int unreadCount = loadUnreadCounters();
				if (unreadCount > 0) {
					notifyInsertedMessage(MucProvider.CONTENT_URI + "/"
							+ ((MucEvent) be).getRoom().getRoomJid().toString());

				}
			}
		} catch (Exception ex) {
		}
	}

	private void notifyInsertedMessage(String URI) {
		Uri insertedItem = Uri.parse(URI);
		getApplicationContext().getContentResolver().notifyChange(insertedItem,
				null);
	}

	public static HashMap<String, Integer> mUnreadCounters = new HashMap<String, Integer>();

	public int loadUnreadCounters() {
		final String[] projection = new String[] { ChatTableMetaData.FIELD_JID,
				"count(*)" };
		final String selection = ChatTableMetaData.FIELD_STATE + " = "
				+ ChatTableMetaData.STATE_INCOMING_UNREAD + " ) GROUP BY ("
				+ ChatTableMetaData.FIELD_JID; // hack!

		Cursor c = getContentResolver().query(
				Uri.parse(ChatHistoryProvider.CHAT_URI), projection, selection,
				null, null);
		mUnreadCounters.clear();
		if (c != null) {
			while (c.moveToNext()) {
				mUnreadCounters.put(c.getString(0), c.getInt(1));
			}
			c.close();
		}
		if (DEBUG)
			Log.d("unreadcounter", mUnreadCounters.toString());
		return mUnreadCounters.size();
	}

	protected Integer findChat(final long chatId) {
		List<Chat> l = getChatList();
		for (int i = 0; i < l.size(); i++) {
			Chat c = l.get(i);
			if (c.getId() == chatId)
				return i;
		}
		return null;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		processingNotificationIntent(intent);
	}

	private void processingNotificationIntent(final Intent intent) {
		if (intent == null)
			return;

		final Bundle bundle = intent.getExtras();
		if (bundle != null) {
			pager.post(new Runnable() {
				@Override
				public void run() {
					if (intent.getAction() != null
							&& MUC_MESSAGE_ACTION.equals(intent.getAction())) {

						long roomId = bundle.getLong("roomId", -1);
						Intent i = new Intent(
								JijichatMobileMessengerActivity.this,
								MucRoomActivity.class);
						i.putExtra("roomId", roomId);
						startActivity(i);

					}
				}
			});
		}

	}

	// private void startActivity(Class<Activity> activity){
	// Intent intent = new Intent(this,activity);
	// startActivity(intent);
	// finish();
	// }
}
