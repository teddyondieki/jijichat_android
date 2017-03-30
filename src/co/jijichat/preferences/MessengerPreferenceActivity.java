/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package co.jijichat.preferences;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import co.jijichat.Constants;
import co.jijichat.R;
import co.jijichat.db.AccountsTableMetaData;

public class MessengerPreferenceActivity extends SherlockPreferenceActivity
		implements OnSharedPreferenceChangeListener {

	// private static final boolean DEBUG = false;

	private static final int MISSING_SETTING = 0;

	private static final String TAG = "tigase";

	private void initSummary(Preference p) {
		if (p instanceof PreferenceScreen) {
			PreferenceScreen pCat = (PreferenceScreen) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updateSummary(p.getKey());
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@TargetApi(14)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate() " + savedInstanceState);
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_preference);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.main_preferences);

		AccountManager accManager = AccountManager.get(this);
		Account account = accManager.getAccountsByType(Constants.ACCOUNT_TYPE)[0];

		String accountName = accManager.getUserData(account,
				AccountsTableMetaData.FIELD_JID);

		Log.v("PREF", "got account = " + accountName);

		Preference pref = this.findPreference("vcard");
		pref.getIntent().putExtra("account_jid", accountName);

		initSummary(getPreferenceScreen());
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MISSING_SETTING: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Please set login data").setCancelable(true)
					.setIcon(android.R.drawable.ic_dialog_alert);
			return builder.create();
		}
		default:
			return null;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(TAG, "New INtent!!! " + intent);
		super.onNewIntent(intent);
		if (intent.getBooleanExtra("missingLogin", false)) {
			showDialog(MISSING_SETTING);
		}
	}

	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updateSummary(key);
	}

	private void updateSummary(String key) {
		Preference p = findPreference(key);
		if (p instanceof EditTextPreference) {
			final EditTextPreference pref = (EditTextPreference) p;
			if ("reconnect_time".equals(key)) {
				pref.setSummary(getResources().getString(
						R.string.pref_reconnect_time_summary, pref.getText()));
				this.onContentChanged();
			} else if ("default_priority".equals(key)) {
				pref.setSummary(getResources().getString(
						R.string.pref_default_priority_summary, pref.getText()));
				this.onContentChanged();
			} else if ("away_priority".equals(key)) {
				pref.setSummary(getResources().getString(
						R.string.pref_auto_away_priority_summary,
						pref.getText()));
				this.onContentChanged();
			} else if ("keepalive_time".equals(key)) {
				pref.setSummary(getResources().getString(
						R.string.pref_keepalive_time_summary, pref.getText()));
				this.onContentChanged();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
