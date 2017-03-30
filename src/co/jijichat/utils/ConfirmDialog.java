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
package co.jijichat.utils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ConfirmDialog extends DialogFragment {

	private String message;

	public void setMessage(String message) {
		this.message = message;
	}

	public static ConfirmDialog newInstance() {
		Bundle args = new Bundle();
		return newInstance(args);
	}

	public static ConfirmDialog newInstance(Bundle args) {
		ConfirmDialog frag = new ConfirmDialog();
		frag.setArguments(args);
		return frag;
	}

	private AsyncTask<Void, Void, Void> task;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Confirm");
		builder.setMessage(message);

		builder.setCancelable(true);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						if (task != null) {
							task.execute();
						}
					}
				});
		builder.setNegativeButton(android.R.string.no,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
					}
				});

		return builder.create();
	}

	public void setAsyncTask(AsyncTask<Void, Void, Void> r) {
		this.task = r;
	}
}
