package co.jijichat.muc;

import java.util.Collection;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import co.jijichat.MessengerApplication;
import co.jijichat.R;
import co.jijichat.db.providers.ChatHistoryProvider;
import co.jijichat.db.providers.MucProvider;
import co.jijichat.utils.ConfirmDialog;

public class MucFragment extends SherlockFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "MucFragment";
	private MucAdapter mucAdapter;
	private ListView listView;
	private TextView tvAddRoom;

	public static MucFragment newInstance() {
		MucFragment f = new MucFragment();
		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(23, null, this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mucAdapter = new MucAdapter(getActivity(), R.layout.muc_item);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.muc_main, container, false);
		this.listView = (ListView) view.findViewById(R.id.roomList);
		this.tvAddRoom = (TextView) view.findViewById(R.id.addRoom);

		registerForContextMenu(listView);
		listView.setAdapter(mucAdapter);

		mucAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				if (mucAdapter.isEmpty()) {
					tvAddRoom.setVisibility(View.VISIBLE);
					tvAddRoom.bringToFront();
				} else {
					tvAddRoom.setVisibility(View.GONE);
				}
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent i = new Intent(getActivity(), MucRoomActivity.class);
				i.putExtra("roomId", id);
				startActivity(i);
			}
		});
		return view;

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.leaveRoom) {
			Long roomId = extractId(item.getMenuInfo());
			leaveRoom(roomId);
			return true;
		} else if (item.getItemId() == R.id.clearConvo) {
			Long roomId = extractId(item.getMenuInfo());
			clearMessageHistory(roomId, true);
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	private void clearMessageHistory(final long roomId, boolean showDialog) {
		final Room room = findRoomById(roomId);

		AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					getActivity()
							.getApplicationContext()
							.getContentResolver()
							.delete(Uri.parse(ChatHistoryProvider.CHAT_URI
									+ "/"
									+ Uri.encode(room.getRoomJid().toString())),
									null, null);
				} catch (NullPointerException ex) {

				}
				return null;
			}

		};

		if (showDialog) {
			ConfirmDialog newFragment = ConfirmDialog.newInstance();
			newFragment.setMessage("Clear conversation?");

			newFragment.setAsyncTask(t);
			newFragment.show(getFragmentManager(), "dialog");
		} else {
			t.execute();
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		final Long id = extractId(menuInfo);
		if (id != null && id != -1) {
			MenuInflater m = new MenuInflater(this.getActivity());
			m.inflate(R.menu.muc_context_menu, menu);
		}
	}

	private static long extractId(ContextMenuInfo menuInfo) {
		if (menuInfo instanceof ExpandableListContextMenuInfo) {
			int type = ExpandableListView
					.getPackedPositionType(((ExpandableListContextMenuInfo) menuInfo).packedPosition);
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				return ((ExpandableListContextMenuInfo) menuInfo).id;
			} else
				return -1;
		} else if (menuInfo instanceof AdapterContextMenuInfo) {
			return ((AdapterContextMenuInfo) menuInfo).id;
		} else {
			return -1;
		}
	}

	private void leaveRoom(final long roomId) {

		final Room room = findRoomById(roomId);

		if (room != null) {

			ConfirmDialog newFragment = ConfirmDialog.newInstance();
			newFragment.setMessage("Leave room?");

			final MucModule cm = getJaxmpp().getModule(MucModule.class);
			AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

				private boolean outcome;

				@Override
				protected Void doInBackground(Void... params) {
					try {
						cm.leave(room);
						outcome = true;
					} catch (JaxmppException e) {
						outcome = false;
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					if (outcome == true) {
						clearMessageHistory(room.getId(), false);
						Uri insertedItem = ContentUris.withAppendedId(
								Uri.parse(MucProvider.CONTENT_URI), roomId);
						getActivity().getApplicationContext()
								.getContentResolver()
								.notifyChange(insertedItem, null);
					}
				}
			};

			newFragment.setAsyncTask(t);
			newFragment.show(getFragmentManager(), "dialog");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(),
				Uri.parse(MucProvider.CONTENT_URI), null, null, null, null);
	}

	private Room findRoomById(long roomId) {
		Collection<Room> rooms = getRooms();
		synchronized (rooms) {
			for (Room r : rooms) {
				if (r.getId() == roomId)
					return r;
			}
		}
		return null;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mucAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mucAdapter.swapCursor(cursor);
	}

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getActivity().getApplicationContext())
				.getJaxmpp();
	}

	private Collection<Room> getRooms() {
		return getJaxmpp().getModule(MucModule.class).getRooms();
	}
}
