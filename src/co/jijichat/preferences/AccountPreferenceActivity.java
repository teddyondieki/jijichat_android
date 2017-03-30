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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import co.jijichat.Constants;
import co.jijichat.R;
import co.jijichat.db.AccountsTableMetaData;

public class AccountPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.account_preferences);

		AccountManager accManager = AccountManager.get(this);
		Account account = accManager.getAccountsByType(Constants.ACCOUNT_TYPE)[0];

		String accountName = accManager.getUserData(account,
				AccountsTableMetaData.FIELD_JID);

		Log.v("PREF", "got account = " + accountName);

		Preference pref = this.findPreference("auth");
		pref.getIntent().putExtra("account_jid", accountName);
		pref = this.findPreference("vcard");
		pref.getIntent().putExtra("account_jid", accountName);
		pref = this.findPreference("advanced");
		pref.getIntent().putExtra("account_jid", accountName);

	}

}
