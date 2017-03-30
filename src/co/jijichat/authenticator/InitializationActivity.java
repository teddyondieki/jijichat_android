package co.jijichat.authenticator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import co.jijichat.Constants;
import co.jijichat.Jaxmpp;
import co.jijichat.MessengerApplication;
import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.R;
import co.jijichat.db.AccountsTableMetaData;
import co.jijichat.utils.JSONParser;

public class InitializationActivity extends SherlockActivity {
	// private static final String TAG = "InitializationActivity";

	private String nickname = null;
	private String jid = null;
	private String pass = null;
	private EditText etNickname;
	private static final int MAX_LENGTH = 25;
	private static boolean startMainActivity = true;
	private static final String PROFILE_UPDATE_URL = "https://jijichat.co/profile/updateProfile";
	private static final String PROFILE_DOWNLOAD_URL = "https://jijichat.co/profile/myProfile";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);

		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager
				.getAccountsByType(Constants.ACCOUNT_TYPE);

		Account account = accounts[0];

		setContentView(R.layout.activity_initialization);

		this.jid = accountManager.getUserData(account,
				AccountsTableMetaData.FIELD_JID);
		this.pass = accountManager.getPassword(account);

		InputFilter filters = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				boolean keepOriginal = true;
				StringBuilder sb = new StringBuilder(end - start);
				for (int i = start; i < end; i++) {
					char c = source.charAt(i);
					if (isCharAllowed(c)) // put your condition here
					{
						sb.append(c);
					} else {
						keepOriginal = false;
					}
				}
				if (keepOriginal)
					return null;
				else {
					if (source instanceof Spanned) {
						SpannableString sp = new SpannableString(sb);
						TextUtils.copySpansFrom((Spanned) source, start,
								sb.length(), null, sp, 0);
						return sp;
					} else {
						return sb;
					}
				}
			}

			private boolean isCharAllowed(char c) {
				return Character.isLetterOrDigit(c)/* || c == '\'' || c == '"' */;
			}
		};

		etNickname = (EditText) findViewById(R.id.nickname);
		etNickname.setFilters(new InputFilter[] { filters });

		// use static string reference
		startMainActivity = getIntent().getBooleanExtra("startActivity", false);
		if (!startMainActivity) {
			ActionBar actionBar = getSupportActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		// downloadNick();
		new ProfileDownloadTask().execute();

	}

	protected final Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	private class ProfileDownloadTask extends AsyncTask<Void, Void, Void> {

		final ProgressDialog dialog = createProgress(R.string.vcard_retrieving);

		@Override
		protected void onPreExecute() {
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {

			try {
				JSONParser jsonParser = new JSONParser();

				List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(
						1);
				nameValuePair.add(new BasicNameValuePair("jid", jid));
				nameValuePair.add(new BasicNameValuePair("pass", pass));
				JSONObject profile = jsonParser.getJSON(PROFILE_DOWNLOAD_URL,
						nameValuePair);

				nickname = profile.getString("nick");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();

			if (nickname == null) {
				Toast.makeText(getApplication(), "Failed to connect.",
						Toast.LENGTH_SHORT).show();
				finish();
			} else {
				fillFields(nickname);
			}

			super.onPostExecute(result);
		}
	}

	/**
	 * Create progress dialog based on id of resource string
	 * 
	 * @param resourceString
	 * @return
	 */
	private ProgressDialog createProgress(int resourceString) {
		final ProgressDialog dialog = ProgressDialog.show(
				InitializationActivity.this, "",
				getResources().getString(resourceString), true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Intent result = new Intent();
				setResult(Activity.RESULT_CANCELED, result);
				finish();
			}
		});
		return dialog;
	}

	/**
	 * Fill activity for editing vcard from vcard instance
	 * 
	 * @param activity
	 * @param contentResolver
	 * @param resources
	 * @param jid
	 * @param vcard
	 */
	private void fillFields(final String nickname) {
		etNickname.setText(nickname);
		etNickname.selectAll();
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		} else if (item.getItemId() == R.id.vcard_editor_publish) {
			publishProfile();
		}
		return true;
	}

	private void publishProfile() {
		nickname = ((TextView) findViewById(R.id.nickname)).getText()
				.toString();
		Pattern pattern = Pattern.compile("^[a-zA-Z0-9]+$");
		Matcher matcher = pattern.matcher(nickname);
		if (TextUtils.isEmpty(nickname)) {
			Toast.makeText(getApplicationContext(), "Nickname can't be empty",
					Toast.LENGTH_LONG).show();
		} else if (nickname.length() > MAX_LENGTH) {
			Toast.makeText(
					getApplicationContext(),
					"Nickname can't be more than " + MAX_LENGTH + " characters",
					Toast.LENGTH_LONG).show();
		} else if (!matcher.matches()) {
			Toast.makeText(
					getApplicationContext(),
					"Invalid input. Only alpha-numeric characters and spaces are allowed.",
					Toast.LENGTH_LONG).show();
		} else {
			new ProfileUpdateTask().execute();
		}

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.vcard_editor_menu, menu);
		return true;
	}

	public class ProfileUpdateTask extends AsyncTask<Void, Void, Integer> {

		final ProgressDialog dialog = createProgress(R.string.vcard_publishing);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setCancelable(false);
			dialog.setMessage("Saving nickname...");
			dialog.show();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {

				JSONParser jsonParser = new JSONParser();

				List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(
						6);

				nameValuePair.add(new BasicNameValuePair("u", jid));
				nameValuePair.add(new BasicNameValuePair("pass", pass));
				nameValuePair.add(new BasicNameValuePair("p[nick]", nickname));

				JSONObject serverResponse = jsonParser.getJSON(
						PROFILE_UPDATE_URL, nameValuePair);

				Integer result = serverResponse.getInt("result");
				final String message = serverResponse.getString("message");
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(getApplication(), message,
								Toast.LENGTH_SHORT).show();
					}
				});

				return result;

			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			try {
				if (result.equals(Constants.RESPONSE_CODE_SUCCESS)) {
					saveBroadcastNick();
					if (startMainActivity) {
						Intent intent = new Intent(InitializationActivity.this,
								JijichatMobileMessengerActivity.class);
						startActivity(intent);
					}
					finish();
				}
			} catch (Exception ex) {
				Toast.makeText(getApplication(), "Failed to connect.",
						Toast.LENGTH_SHORT).show();
			}

			super.onPostExecute(result);

		}
	}

	private void saveBroadcastNick() {
		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager
				.getAccountsByType(Constants.ACCOUNT_TYPE);
		Account account = accounts[0];
		accountManager.setUserData(account,
				AccountsTableMetaData.FIELD_NICKNAME, nickname);

		Intent i = new Intent();
		i.setAction(AuthenticatorActivity.ACCOUNT_MODIFIED_MSG);
		sendBroadcast(i);
	}

}
