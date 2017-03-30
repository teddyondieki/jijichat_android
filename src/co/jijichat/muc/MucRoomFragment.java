package co.jijichat.muc;

import github.ankushsachdeva.emojicon.EmojiconEditText;
import github.ankushsachdeva.emojicon.EmojiconGridView.OnEmojiconClickedListener;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.EmojiconsPopup.OnEmojiconBackspaceClickedListener;
import github.ankushsachdeva.emojicon.EmojiconsPopup.OnSoftKeyboardOpenCloseListener;
import github.ankushsachdeva.emojicon.emoji.Emojicon;

import java.text.BreakIterator;
import java.util.Date;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.UIDGenerator;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.j2se.Jaxmpp;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;

import co.jijichat.MessengerApplication;
import co.jijichat.Preferences;
import co.jijichat.R;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.db.providers.ChatHistoryProvider;
import co.jijichat.utils.ConfirmDialog;
import co.jijichat.utils.RosterNameHelper;

public class MucRoomFragment extends SherlockFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Room room;
	private EmojiconEditText etEmojicon;
	private ListView lv;
	private View view;
	private ImageView sendButton;
	private TextView msgLength;

	private MucRoomAdapter mucRoomAdapter;
	private SharedPreferences prefs;

	public static final String TAG = "MucRoomFragment";
	public static final int SHOW_OCCUPANTS = 3;
	private static final int MAX_LENGTH = 150;

	void addNicknameToEdit(String n) {
		String ttt = etEmojicon.getText().toString();
		if (ttt == null || ttt.length() == 0) {
			etEmojicon.append(n + ": ");
		} else {
			etEmojicon.append(" " + n);
		}
	}

	void cancelEdit() {
		if (etEmojicon == null)
			return;
		final InputMethodManager imm = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		etEmojicon.post(new Runnable() {

			@Override
			public void run() {
				etEmojicon.clearComposingText();
				imm.hideSoftInputFromWindow(etEmojicon.getWindowToken(), 0);
			}
		});
	}

	private final OnClickListener nicknameClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			addNicknameToEdit((((TextView) v).getText()).toString());
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.mucRoomAdapter = new MucRoomAdapter(getActivity(),
				R.layout.muc_chat_item, room, nicknameClickListener);
		getLoaderManager().initLoader(57, null, this);
		mucRoomAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				if (lv != null)
					lv.post(new Runnable() {

						@Override
						public void run() {
							lv.setSelection(Integer.MAX_VALUE);
						}
					});
			}
		});

		// getSherlockActivity().getSupportActionBar().setTitle(
		// room.getRoomJid().toString());

		// etEmojicon.setEnabled(room.getState() == State.joined);
		// sendButton.setEnabled(room.getState() == State.joined);
		lv.setAdapter(mucRoomAdapter);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SHOW_OCCUPANTS && resultCode == Activity.RESULT_OK) {
			String n = data.getStringExtra("nickname");
			if (n != null) {
				addNicknameToEdit(n);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		this.setHasOptionsMenu(true);

		this.prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity().getApplicationContext(),
				Uri.parse(ChatHistoryProvider.ROOM_URI + "/"
						+ room.getRoomJid()), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.muc_main_menu, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.view = inflater.inflate(R.layout.muc_conversation, container,
				false);

		this.msgLength = (TextView) view.findViewById(R.id.msgLength);
		msgLength.setText(MAX_LENGTH + "");

		this.etEmojicon = (EmojiconEditText) view
				.findViewById(R.id.chat_message_entry);

		this.etEmojicon.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					boolean ets = prefs.getBoolean(
							Preferences.ENTER_TO_SEND_KEY, true);
					if (ets) {
						sendMessage();
						return true;
					}
				}
				return false;
			}
		});

		this.etEmojicon.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				int remaining = MAX_LENGTH - graphemeLength(s.toString());
				msgLength.setText(remaining + "");

				if (remaining < 0) {
					msgLength.setTextColor(Color.RED);
				} else {
					msgLength.setTextColor(getResources().getColor(
							R.color.textsecure_primary));
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		this.etEmojicon.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus)
					cancelEdit();
			}
		});

		this.sendButton = (ImageView) view.findViewById(R.id.chat_send_button);

		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});

		this.lv = (ListView) view.findViewById(R.id.chat_conversation_history);
		registerForContextMenu(lv);
		lv.post(new Runnable() {

			@Override
			public void run() {
				lv.setSelection(Integer.MAX_VALUE);
			}
		});

		// final View rootView = view.findViewById(R.id.root_view);

		final ImageView emojiButton = (ImageView) view
				.findViewById(R.id.emoji_button);

		// Give the topmost view of your activity layout hierarchy. This will be
		// used to measure soft keyboard height
		final EmojiconsPopup popup = new EmojiconsPopup(view.getRootView(),
				getActivity());

		// Will automatically set size according to the soft keyboard size
		popup.setSizeForSoftKeyboard();

		// If the emoji popup is dismissed, change emojiButton to smiley icon
		popup.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss() {
				changeEmojiKeyboardIcon(emojiButton, R.drawable.smiley);
			}
		});

		// If the text keyboard closes, also dismiss the emoji popup
		popup.setOnSoftKeyboardOpenCloseListener(new OnSoftKeyboardOpenCloseListener() {

			@Override
			public void onKeyboardOpen(int keyBoardHeight) {

			}

			@Override
			public void onKeyboardClose() {
				if (popup.isShowing())
					popup.dismiss();
			}
		});

		// On emoji clicked, add it to edittext
		popup.setOnEmojiconClickedListener(new OnEmojiconClickedListener() {

			@Override
			public void onEmojiconClicked(Emojicon emojicon) {
				etEmojicon.getText().insert(etEmojicon.getSelectionStart(),
						emojicon.getEmoji());
			}
		});

		// On backspace clicked, emulate the KEYCODE_DEL key event
		popup.setOnEmojiconBackspaceClickedListener(new OnEmojiconBackspaceClickedListener() {

			@Override
			public void onEmojiconBackspaceClicked(View v) {
				KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0,
						0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
				etEmojicon.dispatchKeyEvent(event);
			}
		});

		// To toggle between text keyboard and emoji keyboard keyboard(Popup)
		emojiButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				// If popup is not showing => emoji keyboard is not visible, we
				// need to show it
				if (!popup.isShowing()) {

					// If keyboard is visible, simply show the emoji popup
					if (popup.isKeyBoardOpen()) {
						popup.showAtBottom();
						changeEmojiKeyboardIcon(emojiButton,
								R.drawable.ic_action_keyboard);
					}

					// else, open the text keyboard first and immediately after
					// that show the emoji popup
					else {
						etEmojicon.setFocusableInTouchMode(true);
						etEmojicon.requestFocus();
						popup.showAtBottomPending();
						final InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
								.getSystemService(Context.INPUT_METHOD_SERVICE);
						inputMethodManager.showSoftInput(etEmojicon,
								InputMethodManager.SHOW_IMPLICIT);
						changeEmojiKeyboardIcon(emojiButton,
								R.drawable.ic_action_keyboard);
					}
				}

				// If popup is showing, simply dismiss it to show the undelying
				// text keyboard
				else {
					popup.dismiss();
				}
			}
		});

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == R.id.showOccupantsButton) {
			final boolean isConnected = getJaxmpp().isConnected();
			if (!isConnected) {
				Toast.makeText(
						getActivity(),
						"Service not connected. Please check your internet connection and try again.",
						Toast.LENGTH_SHORT).show();
			} else {
				Bundle args = new Bundle();
				args.putLong("roomId", room.getId());
				OccupantsListDialog newFragment = OccupantsListDialog
						.newInstance(args);

				newFragment.setTargetFragment(this, SHOW_OCCUPANTS);
				newFragment.show(getActivity().getSupportFragmentManager(),
						"dialog");
			}
		} else if (item.getItemId() == R.id.leaveRoom) {
			leaveRoom();
		} else if (item.getItemId() == R.id.clearConvo) {
			clearMessageHistory(true);
		}
		return true;

	}

	private void leaveRoom() {
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
					clearMessageHistory(false);
					getActivity().finish();
				}
			}
		};

		newFragment.setAsyncTask(t);
		newFragment.show(getFragmentManager(), "dialog");
	}

	private void clearMessageHistory(boolean showDialog) {
		AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				Uri toDelete = Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
						+ Uri.encode(room.getRoomJid().toString()));
				getActivity().getApplicationContext().getContentResolver()
						.delete(toDelete, null, null);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				getActivity()
						.getApplicationContext()
						.getContentResolver()
						.notifyChange(
								Uri.parse(ChatHistoryProvider.ROOM_URI
										+ "/"
										+ Uri.encode(room.getRoomJid()
												.toString()) + "/"
										+ room.getId()), null);
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
	public void onPrepareOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = new com.actionbarsherlock.view.MenuInflater(
				this.getActivity().getApplicationContext());
		onCreateOptionsMenu(menu, inflater);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	protected void sendMessage() {
		if (etEmojicon == null)
			return;

		String t = etEmojicon.getText().toString().trim();

		if (t == null || t.length() == 0) {
			etEmojicon.setText("");
			return;
		}

		if (graphemeLength(t) > MAX_LENGTH) {
			Toast.makeText(
					getActivity(),
					"Message has exceed the limit of " + MAX_LENGTH
							+ " characters.", Toast.LENGTH_SHORT).show();
			return;
		}

		etEmojicon.setText("");

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
			@Override
			public Void doInBackground(String... ts) {
				String t = ts[0];
				final String messageId = UIDGenerator.next();
				try {

					// Store outgoing message in db for id comparison in
					// delivery
					Uri uri = Uri.parse(ChatHistoryProvider.ROOM_URI + "/"
							+ room.getRoomJid().toString());

					ContentValues values = new ContentValues();
					values.put(ChatTableMetaData.FIELD_JID, room.getRoomJid()
							.toString());
					values.put(ChatTableMetaData.FIELD_AUTHOR_NICKNAME,
							room.getNickname());
					values.put(ChatTableMetaData.FIELD_TIMESTAMP, (room
							.getLastMessageDate().getTime() + 2000));
					values.put(ChatTableMetaData.FIELD_CHAT_ROOM_ID,
							room.getId());
					values.put(ChatTableMetaData.FIELD_BODY, t);
					values.put(ChatTableMetaData.FIELD_ACCOUNT, room
							.getSessionObject().getUserBareJid().toString());
					values.put(ChatTableMetaData.FIELD_STATE,
							ChatTableMetaData.STATE_OUT_NOT_SENT);
					values.put(ChatTableMetaData.FIELD_MESSAGE_ID, messageId);

					getActivity().getContentResolver().insert(uri, values);

					room.sendMessage(t, messageId);

				} catch (Exception e) {
					Log.d(TAG, "Could not send room message");
//					e.printStackTrace();
				}
				return null;
			}
		};

		task.execute(t);
	}

	private Jaxmpp getJaxmpp() {
		return ((MessengerApplication) getActivity().getApplicationContext())
				.getJaxmpp();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mucRoomAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mucRoomAdapter.swapCursor(cursor);
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	private void changeEmojiKeyboardIcon(ImageView iconToBeChanged,
			int drawableResourceId) {
		iconToBeChanged.setImageResource(drawableResourceId);
	}

	private void copyMessageBody(final long id) {
		ClipboardManager clipMan = (ClipboardManager) getActivity()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		Cursor cc = null;
		try {
			cc = getChatEntry(id);
			String t = cc.getString(cc
					.getColumnIndex(ChatTableMetaData.FIELD_BODY));
			clipMan.setText(t);
			Toast.makeText(getActivity(), "Message copied", Toast.LENGTH_SHORT)
					.show();
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}

	}

	private Cursor getChatEntry(long id) {
		Cursor cursor = getActivity()
				.getApplicationContext()
				.getContentResolver()
				.query(Uri.parse(ChatHistoryProvider.CHAT_URI + "/"
						+ Uri.encode(room.getRoomJid().toString()) + "/" + id),
						null, null, null, null);
		cursor.moveToNext();
		return cursor;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.copyMessage) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			copyMessageBody(info.id);
			return true;
		} else if (item.getItemId() == R.id.detailsMessage) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			showMessageDetails(info.id);
			return true;
		} else if (item.getItemId() == R.id.deleteMessage) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			deleteMessage(info.id);
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater m = new MenuInflater(getActivity());
		m.inflate(R.menu.chat_context_menu, menu);
	}

	private void showMessageDetails(final long id) {
		Cursor cc = null;
		final java.text.DateFormat df = DateFormat.getDateFormat(getActivity());
		final java.text.DateFormat tf = DateFormat.getTimeFormat(getActivity());

		try {
			cc = getChatEntry(id);

			Dialog alertDialog = new Dialog(getActivity());

			alertDialog.setContentView(R.layout.chat_item_details_dialog);
			alertDialog.setCancelable(true);
			alertDialog.setCanceledOnTouchOutside(true);
			alertDialog.setTitle("Message details");

			TextView msgDetSender = (TextView) alertDialog
					.findViewById(R.id.msgDetSender);
			String senderJID = cc.getString(cc
					.getColumnIndex(ChatTableMetaData.FIELD_JID));
			msgDetSender.setText(RosterNameHelper.getName(BareJID
					.bareJIDInstance(senderJID)));

			Date timestamp = new Date(cc.getLong(cc
					.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP)));
			TextView msgDetReceived = (TextView) alertDialog
					.findViewById(R.id.msgDetReceived);
			msgDetReceived.setText(df.format(timestamp) + " "
					+ tf.format(timestamp));

			final int state = cc.getInt(cc
					.getColumnIndex(ChatTableMetaData.FIELD_STATE));
			final String nick = cc.getString(cc
					.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));

			TextView msgDetState = (TextView) alertDialog
					.findViewById(R.id.msgDetState);
			switch (state) {
			case ChatTableMetaData.STATE_INCOMING:
				msgDetState.setText(nick);
				break;
			case ChatTableMetaData.STATE_OUT_SENT:
				msgDetState.setText("You");
				break;
			case ChatTableMetaData.STATE_OUT_NOT_SENT:
				msgDetState.setText("You");
				msgDetReceived.setText("Pending");
				break;
			default:
				msgDetState.setText("?");
				break;
			}
			alertDialog.show();
		} finally {
			if (cc != null && !cc.isClosed())
				cc.close();
		}
	}

	private void deleteMessage(final long id) {

		AsyncTask<Void, Void, Void> t = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				getActivity()
						.getApplicationContext()
						.getContentResolver()
						.delete(Uri.parse(ChatHistoryProvider.ROOM_URI + "/"
								+ Uri.encode(room.getRoomJid().toString())
								+ "/" + id), null, null);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				Toast.makeText(getActivity(), "Message deleted",
						Toast.LENGTH_SHORT).show();
			}

		};

		ConfirmDialog newFragment = ConfirmDialog.newInstance();
		newFragment.setMessage("Delete message?");

		newFragment.setAsyncTask(t);
		newFragment.show(getFragmentManager(), "dialog");
	}

	private int graphemeLength(String str) {
		BreakIterator iter = BreakIterator.getCharacterInstance();
		iter.setText(str);
		int count = 0;
		while (iter.next() != BreakIterator.DONE)
			count++;

		return count;
	}

}
