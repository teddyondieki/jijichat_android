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

import java.util.Collection;

import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import co.jijichat.MessengerApplication;
import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.R;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;
import co.jijichat.utils.AvatarHelper;
import co.jijichat.utils.RosterNameHelper;

public class MucRoomActivity extends SherlockFragmentActivity {

	private static final String TAG = "RoomActivity";
	private Room room;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			long id = extras.getLong("roomId");
			Room rm = getRoomById(id);
			if (rm == null) {
				Log.v(TAG, "Room is null with id = " + id);
			} else {
				if (rm.getSessionObject() == null) {
					throw new NullPointerException(
							"Room.getSessionObject() is null with id = " + id);
				}
				this.room = rm;
			}
		}

		notifyFocus(true);

		String roomName = RosterNameHelper.getName(room.getRoomJid());

		actionBar.setIcon(AvatarHelper.getAvatar(room.getRoomJid()));
		actionBar.setTitle(roomName);
		actionBar.setSubtitle(RosterNameHelper.getDesc(room.getRoomJid()));

		if (savedInstanceState == null) {
			MucRoomFragment fragment = new MucRoomFragment();
			fragment.setRoom(room);
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content, fragment,
							fragment.getClass().getSimpleName()).commit();
		}

	}

	private void notifyFocus(boolean focused) {
		Intent intent = new Intent();
		intent.setAction(JijichatMobileMessengerActivity.CLIENT_FOCUS_MSG);

		if ((room != null) && focused) {
			Uri uri = Uri.parse(ChatHistoryProvider.ROOM_URI + "/"
					+ room.getRoomJid().toString());
			ContentValues values = new ContentValues();
			values.put(ChatTableMetaData.FIELD_STATE,
					ChatTableMetaData.STATE_INCOMING);
			getContentResolver().update(uri, values, null, null);
			intent.putExtra("roomId", room.getId());
		}
		sendBroadcast(intent);
	}

	@Override
	public void onPause() {
		notifyFocus(false);
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		notifyFocus(true);
	}

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	public Room getRoomById(final long id) {
		final Collection<Room> rooms = getRoomList();
		synchronized (rooms) {
			for (Room r : rooms) {
				if (r.getId() == id)
					return r;
			}
		}
		return null;
	}

	protected Collection<Room> getRoomList() {
		return getJaxmpp().getModule(MucModule.class).getRooms();
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
