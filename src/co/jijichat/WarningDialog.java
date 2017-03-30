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
package co.jijichat;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import co.jijichat.R;

public class WarningDialog extends DialogFragment {

	private static WarningDialog newInstance(final int messageId) {
		WarningDialog frag = new WarningDialog();
		Bundle args = new Bundle();
		args.putInt("messageId", messageId);
		frag.setArguments(args);
		return frag;
	}

	private static WarningDialog newInstance(final String message) {
		WarningDialog frag = new WarningDialog();
		Bundle args = new Bundle();
		args.putString("message", message);
		frag.setArguments(args);
		return frag;
	}

	private static void showNotification(FragmentActivity activity,
			String message, Integer messageId) {
		// TODO
		// NotificationHelper helper =
		// NotificationHelper.createIntstance(activity);
		//
		// Intent intent = new Intent();
		// intent.setAction(JijichatMobileMessengerActivity.WARNING_ACTION);
		// if (message != null)
		// intent.putExtra("message", message);
		// if (messageId != null)
		// intent.putExtra("messageId", messageId);
		//
		// intent.setClass(activity, JijichatMobileMessengerActivity.class);

		// helper.showWarning("warning:" + (++counter), message, intent);

	}

	public static void showWarning(FragmentActivity activity, int message) {
		try {
			DialogFragment newFragment = WarningDialog.newInstance(message);
			newFragment.show(activity.getSupportFragmentManager(), "dialog");
		} catch (IllegalStateException e) {
			showNotification(activity, null, message);
		}
	}

	public static void showWarning(FragmentActivity activity, String message) {
		try {
			DialogFragment newFragment = WarningDialog.newInstance(message);
			newFragment.show(activity.getSupportFragmentManager(), "dialog");
		} catch (IllegalStateException e) {
			showNotification(activity, message, null);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final String message = getArguments().getString("message");
		final int messageId = getArguments().getInt("messageId", -1);

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning_dialog_title);
		if (messageId != -1) {
			builder.setMessage(messageId);
		} else {
			builder.setMessage(message);
		}
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		return builder.create();
	}
}
