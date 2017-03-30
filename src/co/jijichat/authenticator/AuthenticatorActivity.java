package co.jijichat.authenticator;

import io.fabric.sdk.android.Fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.Digits;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;

import co.jijichat.Constants;
import co.jijichat.Preferences;
import co.jijichat.R;
import co.jijichat.db.AccountsTableMetaData;
import co.jijichat.service.JijichatService;
import co.jijichat.utils.JSONParser;
import co.jijichat.utils.RandomStringUtils;

import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	private AccountManager accountManager = null;

	private ProgressDialog dialog = null;
	private AuthCallback authCallback = null;

	private boolean userVerified = false;

	private String username = null;
	private String password = null;
	private String phone = null;
	private String clientVer = null;
	private String serverMessage = null;

	public static final String ACCOUNT_MODIFIED_MSG = "co.jijichat.ACCOUNT_MODIFIED_MSG";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	// Note: Your consumer key and secret should be obfuscated in your source
	// code before shipping.
	private static final String TWITTER_KEY = "9bBsj8fPYwv9ofmzxyR8gDF99";
	private static final String TWITTER_SECRET = "ivn6XqwHSrvoHLmpY0In7rsZoOW9iwFl68vBF3XTQM36OZztlo";

	private static final String VERIFY_USER_URL = "https://jijichat.co/user/verify";
	private static final String TAG = "AuthenticatorActivity";
	private static final boolean DEBUG = true;

	private Button authButton = null;
	private TextView introText = null;

	@Override
	public void onCreate(Bundle icicle) {
		if (DEBUG) {
			Log.d(TAG, "onCreate()");
		}
		super.onCreate(icicle);

		TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY,
				TWITTER_SECRET);
		Fabric.with(this, new TwitterCore(authConfig), new Digits());

		accountManager = AccountManager.get(this);

		setContentView(R.layout.account_screen);

		dialog = new ProgressDialog(AuthenticatorActivity.this);

		initializeAuthCallback();
		authButton = (Button) findViewById(R.id.auth_button);
		introText = (TextView) findViewById(R.id.introText);
		authButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				verifyPhone();
			}
		});
		authButton.setVisibility(View.VISIBLE);
		introText.setVisibility(View.VISIBLE);
		verifyPhone();

	}

	private void verifyPhone() {
		Digits.authenticate(authCallback);
	}

	private void logInUser() {

		int currentVersion;
		try {
			currentVersion = getPackageManager().getPackageInfo(
					AuthenticatorActivity.this.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			currentVersion = 0;
		}
		clientVer = Integer.toString(currentVersion);

		String clientKey = JijichatService.encrypt(phone + clientVer, "MD5");

		password = getRandomPassword();
		new LogInUserTask().execute(phone, password, clientVer, clientKey);
	}

	private class LogInUserTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			authButton.setVisibility(View.INVISIBLE);
			introText.setVisibility(View.INVISIBLE);
			dialog.setMessage("Signing in...");
			dialog.setCancelable(false);
			dialog.show();
		}

		// params - phone, password, clientVer, clientKey
		@Override
		protected Void doInBackground(final String... params) {
			try {
				JSONParser jsonParser = new JSONParser();
				List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(
						1);
				nameValuePair.add(new BasicNameValuePair("phone", params[0]));
				nameValuePair.add(new BasicNameValuePair("pass", params[1]));
				nameValuePair
						.add(new BasicNameValuePair("clientVer", params[2]));
				nameValuePair
						.add(new BasicNameValuePair("clientKey", params[3]));

				JSONObject jObj = jsonParser.getJSON(VERIFY_USER_URL,
						nameValuePair);

				userVerified = jObj.getBoolean("result");

				if (userVerified) {
					username = jObj.getString("u");
				} else {
					serverMessage = jObj.getString("message");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void authToken) {
			super.onPostExecute(authToken);
			hideProgress();
			if (userVerified) {
				startInitialization();
			} else {
				Toast.makeText(AuthenticatorActivity.this, serverMessage,
						Toast.LENGTH_SHORT).show();
				authButton.setVisibility(View.VISIBLE);
				introText.setVisibility(View.VISIBLE);
			}
		}
	}

	private void initializeAuthCallback() {
		this.authCallback = new AuthCallback() {
			@Override
			public void failure(DigitsException ex) {
				Toast.makeText(AuthenticatorActivity.this, "Failed to verify.",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void success(DigitsSession session, String phoneNumber) {
				if (phoneNumber != null) {
					phone = phoneNumber;
					PreferenceManager
							.getDefaultSharedPreferences(
									getApplicationContext()).edit()
							.putString(Preferences.PHONE_KEY, phoneNumber)
							.commit();
				} else {
					phone = PreferenceManager.getDefaultSharedPreferences(
							getApplicationContext()).getString(
							Preferences.PHONE_KEY, null);
				}
				logInUser();
			}

		};
	}

	public String getRandomPassword() {
		return RandomStringUtils.randomAlphanumeric(15).toUpperCase(
				Locale.getDefault());
	}

	@Override
	public void onResume() {
		super.onResume();
		// authButton.setVisibility(View.VISIBLE);
		// introText.setVisibility(View.VISIBLE);
	}

	private void startInitialization() {

		final Account account = new Account(getResources().getString(
				R.string.app_name), Constants.ACCOUNT_TYPE);

		accountManager.addAccountExplicitly(account, password, null);
		accountManager.setUserData(account, AccountsTableMetaData.FIELD_JID,
				username);

		// nothing to sync for this account, as yet
		ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);

		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);

		// Start JijichatService if not active
		if (!JijichatService.isServiceActive()) {
			Intent serviceIntent = new Intent(this, JijichatService.class);
			serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startService(serviceIntent);
		}

		Intent initIntent = new Intent(this, InitializationActivity.class);
		initIntent.putExtra("startActivity", true);
		startActivity(initIntent);
		finish();
	}

	private void hideProgress() {
		if (dialog != null) {
			dialog.dismiss();
		}
	}

}
