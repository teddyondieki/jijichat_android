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
package co.jijichat.muc;

import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.Item;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import co.jijichat.Constants;
import co.jijichat.MessengerApplication;
import co.jijichat.R;
import co.jijichat.utils.RosterNameHelper;

public class JoinMucDialog extends DialogFragment {

	private Item roomItem;

	public void setItem(Item roomItem) {
		this.roomItem = roomItem;
	}

	public static JoinMucDialog newInstance() {
		Bundle args = new Bundle();
		return newInstance(args);
	}

	public static JoinMucDialog newInstance(Bundle args) {
		JoinMucDialog frag = new JoinMucDialog();
		frag.setArguments(args);
		return frag;
	}

	private AsyncTask<Room, Void, Void> task;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(roomItem.getName());
		builder.setMessage(roomItem.getDescription());

		builder.setCancelable(true);
		builder.setPositiveButton(R.string.join_muc_room,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						final Jaxmpp jaxmpp = ((MessengerApplication) getActivity()
								.getApplicationContext()).getJaxmpp();
						Runnable r = new Runnable() {

							@Override
							public void run() {
								try {
									RosterNameHelper.updateName(roomItem
											.getJid().getBareJid(), roomItem
											.getName());
									RosterNameHelper.updateDesc(roomItem
											.getJid().getBareJid(), roomItem
											.getDescription());
									Room room = jaxmpp.getModule(
											MucModule.class).join(
											roomItem.getJid().getLocalpart(),
											Constants.MUC_SERVER, null);
									if (task != null) {
										task.execute(room);
									}
								} catch (Exception e) {
									Log.w("MUC", "", e);
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						(new Thread(r)).start();
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

	public void setAsyncTask(AsyncTask<Room, Void, Void> r) {
		this.task = r;
	}
}
