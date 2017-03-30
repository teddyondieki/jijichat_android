package co.jijichat.muc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule.MucEvent;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import co.jijichat.MessengerApplication;
import co.jijichat.R;
import co.jijichat.db.providers.MergeSort;

public class OccupantsListDialog extends DialogFragment {

	private class OccupantsAdapter extends BaseAdapter {

		private static final String TAG = "OccupantsAdapter";

		private final LayoutInflater mInflater;

		private final ArrayList<Occupant> occupants = new ArrayList<Occupant>();

		public OccupantsAdapter(Context mContext, Room room) {
			mInflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			occupants.addAll(room.getPresences().values());
			sortList();
			notifyDataSetChanged();
		}

		public void add(Occupant occupant) {
			occupants.add(occupant);
			sortList();
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return occupants.size();
		}

		@Override
		public Object getItem(int arg0) {
			Occupant o = occupants.get(arg0);
			return o;
		}

		@Override
		public long getItemId(int position) {
			return occupants.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.muc_occupants_list_item,
						parent, false);
			} else {
				view = convertView;
			}

			final Occupant occupant = (Occupant) getItem(position);

			final TextView nicknameTextView = (TextView) view
					.findViewById(R.id.occupant_nick);

			try {
				final String nickname = occupant.getNickname();
				nicknameTextView.setText(nickname);

				if (!nickname.equals(room.getSessionObject().getUserProperty(
						SessionObject.NICKNAME))) {
					nicknameTextView.setTextColor(getResources().getColor(
							android.R.color.black));
				} else {
					nicknameTextView.setTextColor(getResources().getColor(
							R.color.textsecure_primary));
				}
			} catch (XMLException e) {
				Log.e(TAG, "Can't show occupant", e);
			}

			return view;
		}

		public void remove(Occupant occupant) {
			occupants.remove(occupant);
			sortList();
			notifyDataSetChanged();
		}

		public void update(Occupant occupant) {
			occupants.remove(occupant);
			occupants.add(occupant);
			sortList();
			notifyDataSetChanged();
		}

		private void sortList() {
			MergeSort.sort(occupants, new Comparator<Occupant>() {
				@Override
				public int compare(Occupant object1, Occupant object2) {
					try {
						String n1 = object1.getNickname().toLowerCase(
								Locale.getDefault());
						String n2 = object2.getNickname().toLowerCase(
								Locale.getDefault());
						return n1.compareTo(n2);
					} catch (Exception e) {
						return 0;
					}
				}
			});
		}

	}

	public static OccupantsListDialog newInstance() {
		Bundle args = new Bundle();
		return newInstance(args);
	}

	public static OccupantsListDialog newInstance(Bundle args) {
		OccupantsListDialog frag = new OccupantsListDialog();
		frag.setArguments(args);
		return frag;
	}

	private OccupantsAdapter adapter;
	private final Listener<MucEvent> mucListener;
	private MucModule mucModule;
	private ListView occupantsList;
	private Room room;

	public OccupantsListDialog() {
		mucListener = new Listener<MucEvent>() {

			@Override
			public void handleEvent(MucEvent be) throws JaxmppException {
				if (be.getRoom() == room && adapter != null)
					onRoomEvent(be);
			}
		};
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Dialog dialog = new Dialog(getActivity());
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		dialog.setContentView(R.layout.muc_occupants_list);
		dialog.setTitle(getString(R.string.occupants_title));

		Bundle data = getArguments();
		long roomId = data.getLong("roomId", -1);
		room = findRoomById(roomId);

		mucModule = getJaxmpp().getModule(MucModule.class);
		mucModule.addListener(mucListener);

		occupantsList = (ListView) dialog.findViewById(R.id.occupants_list);
		occupantsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Occupant occupant = (Occupant) parent
						.getItemAtPosition(position);

				Intent intent = new Intent();

				try {
					intent.putExtra("nickname", occupant.getNickname());
				} catch (XMLException e) {
				}

				getTargetFragment().onActivityResult(getTargetRequestCode(),
						Activity.RESULT_OK, intent);

				dialog.dismiss();
			}
		});

		adapter = new OccupantsAdapter(getActivity(), room);
		occupantsList.setAdapter(adapter);

		return dialog;
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

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getActivity().getApplicationContext())
				.getJaxmpp();
	}

	private Collection<Room> getRooms() {
		return getJaxmpp().getModule(MucModule.class).getRooms();
	}

	protected void onRoomEvent(final MucEvent be) {
		occupantsList.post(new Runnable() {

			@Override
			public void run() {
				if (be.getType() == MucModule.OccupantComes) {
					adapter.add(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantLeaved) {
					adapter.remove(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantChangedPresence) {
					adapter.update(be.getOccupant());
				} else if (be.getType() == MucModule.OccupantChangedNick) {
					adapter.update(be.getOccupant());
				}
				adapter.notifyDataSetChanged();
			}
		});
	}
}
