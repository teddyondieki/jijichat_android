package co.jijichat.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.DiscoItemsAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoItemsModule.Item;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import co.jijichat.Constants;
import co.jijichat.MessengerApplication;
import co.jijichat.R;
import co.jijichat.db.providers.MergeSort;
import co.jijichat.db.providers.MucProvider;
import co.jijichat.muc.JoinMucDialog;
import co.jijichat.muc.MucRoomActivity;
import co.jijichat.utils.AvatarHelper;

public class RoomSearchActivity extends SherlockFragmentActivity {

	private ListView lv;
	private List<Item> roomsList = new ArrayList<Item>();
	private RoomItemAdapter adapter = null;
	private ProgressDialog dialog;
	private TextView tvReloadText;
	private ImageView ivReloadButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_room_search);

		lv = (ListView) findViewById(R.id.lvRoomSearchResults);
		tvReloadText = (TextView) findViewById(R.id.refreshRooms);
		ivReloadButton = (ImageView) findViewById(R.id.refreshButton);

		ivReloadButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				loadRooms();
			}
		});

		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Item roomItem = roomsList.get(position);

				Room room = getRoomByJid(roomItem.getJid().getBareJid());
				if (room != null) {
					Intent i = new Intent(RoomSearchActivity.this,
							MucRoomActivity.class);
					i.putExtra("roomId", room.getId());
					startActivity(i);
					finish();
				} else {
					JoinMucDialog newFragment = JoinMucDialog.newInstance();
					newFragment.setItem(roomItem);
					AsyncTask<Room, Void, Void> r = new AsyncTask<Room, Void, Void>() {

						@Override
						protected Void doInBackground(Room... params) {
							final long roomId = params[0].getId();

							lv.post(new Runnable() {

								@Override
								public void run() {
									Intent i = new Intent(
											RoomSearchActivity.this,
											MucRoomActivity.class);
									i.putExtra("roomId", roomId);
									startActivity(i);
									Uri insertedItem = ContentUris.withAppendedId(
											Uri.parse(MucProvider.CONTENT_URI),
											roomId);
									getApplicationContext()
											.getContentResolver().notifyChange(
													insertedItem, null);

									finish();
								}
							});
							return null;
						}
					};
					newFragment.setAsyncTask(r);
					newFragment.show(getSupportFragmentManager(), "dialog");
				}
			}
		});

		dialog = new ProgressDialog(this);
		dialog.setMessage("Loading rooms...");
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		loadRooms();
	}

	private void loadRooms() {
		tvReloadText.setVisibility(View.GONE);
		ivReloadButton.setVisibility(View.GONE);
		new SearchRoomTask().execute();
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

	class RoomItemAdapter extends ArrayAdapter<Item> {
		RoomItemAdapter() {
			super(RoomSearchActivity.this, R.layout.room_search_item, roomsList);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			RoomItemHolder holder = null;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater
						.inflate(R.layout.room_search_item, parent, false);
				holder = new RoomItemHolder(row);
				row.setTag(holder);
			} else {
				holder = (RoomItemHolder) row.getTag();
			}
			holder.populateRow(roomsList.get(position));
			return (row);
		}
	}

	class RoomItemHolder {
		private TextView tvRoomName = null;
		private TextView tvRoomDescription = null;
		private TextView tvHint = null;
		private ImageView ivRoomAvatar = null;

		RoomItemHolder(View row) {
			tvRoomName = (TextView) row.findViewById(R.id.search_item_title);
			tvRoomDescription = (TextView) row
					.findViewById(R.id.search_item_subtitle);
			tvHint = (TextView) row.findViewById(R.id.search_item_hint);
			ivRoomAvatar = (ImageView) row
					.findViewById(R.id.search_item_avatar);

		}

		void populateRow(Item roomItem) {
			String roomName = roomItem.getName();
			if (isJoined(roomItem.getJid().getBareJid())) {
				tvHint.setText("JOINED");
			} else {
				tvHint.setText("");
			}
			String roomDesc = roomItem.getDescription();
			tvRoomName.setText(roomName);
			tvRoomDescription.setText(roomDesc);

			BareJID jid = roomItem.getJid().getBareJid();
			ivRoomAvatar.setImageDrawable(AvatarHelper.getAvatar(jid));
		}
	}

	private class SearchRoomTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			lv.setAdapter(null);
			roomsList.clear();
			dialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(String... params) {
			getRoomsList();
			return null;
		}

		// @Override
		// protected void onPostExecute(Void result) {
		// // adapter = new RoomItemAdapter();
		// // if (adapter.isEmpty()) {
		// // onLoadFailed();
		// // } else {
		// // lv.setAdapter(adapter);
		// // }
		//
		// // dialog.dismiss();
		// super.onPostExecute(result);
		// }
	}

	private void onLoadFailed() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tvReloadText.setVisibility(View.VISIBLE);
				ivReloadButton.setVisibility(View.VISIBLE);
			}
		});
	}
	

	protected void getRoomsList() {
		final Jaxmpp jaxmpp = getJaxmpp();
		final BareJID from = jaxmpp.getSessionObject().getUserBareJid();

		try {
			jaxmpp.getModule(DiscoItemsModule.class).getItems(
					JID.jidInstance(Constants.MUC_SERVER), from,
					new DiscoItemsAsyncCallback() {

						@Override
						public void onError(Stanza responseStanza,
								ErrorCondition error) throws JaxmppException {
							// Log.d("Error", responseStanza.getAsString());
							onLoadFailed();
							hideProgress();
						}

						@Override
						public void onTimeout() throws JaxmppException {
							// Log.d("Error", "Timeout");
							onLoadFailed();
							hideProgress();

						}

						@Override
						public void onInfoReceived(String attribute,
								ArrayList<Item> items) throws XMLException {

							roomsList.addAll(items);

							MergeSort.sort(roomsList, new Comparator<Item>() {
								@Override
								public int compare(Item object1, Item object2) {
									try {
										String n1 = object1.getName()
												.toLowerCase(
														Locale.getDefault());
										String n2 = object2.getName()
												.toLowerCase(
														Locale.getDefault());
										return n1.compareTo(n2);
									} catch (Exception e) {
										return 0;
									}
								}
							});

							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									// TODO Auto-generated method stub
									adapter = new RoomItemAdapter();
									// if (adapter.isEmpty()) {
									// onLoadFailed();
									// } else {
									lv.setAdapter(adapter);
									// }
								}

							});

							hideProgress();
							// Log.d("Response", items.toString());
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void hideProgress() {
		if (dialog != null) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.dismiss();
				}
			});
		}
	}

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getApplicationContext()).getJaxmpp();
	}

	protected boolean isJoined(BareJID jid) {
		return getJaxmpp().getModule(MucModule.class).getRoomsMap()
				.containsKey(jid);
	}

	public Room getRoomByJid(final BareJID jid) {
		final Collection<Room> rooms = getRooms();
		synchronized (rooms) {
			for (Room r : rooms) {
				if (r.getRoomJid().equals(jid)) {
					return r;
				}
			}
		}
		return null;
	}

	protected Collection<Room> getRooms() {
		return getJaxmpp().getModule(MucModule.class).getRooms();
	}
}
