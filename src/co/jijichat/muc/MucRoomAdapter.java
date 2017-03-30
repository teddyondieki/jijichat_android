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

import github.ankushsachdeva.emojicon.EmojiconTextView;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import co.jijichat.R;
import co.jijichat.db.ChatTableMetaData;
import co.jijichat.utils.DateUtils;

public class MucRoomAdapter extends SimpleCursorAdapter {

	static class ViewHolder {
		EmojiconTextView tvBody;
		TextView timestamp;
		TextView roomMsgNick;
		ImageView msgStatus;
		LinearLayout msgHolder;
	}

	private final static String[] cols = new String[] {
			ChatTableMetaData.FIELD_TIMESTAMP, ChatTableMetaData.FIELD_BODY,
			ChatTableMetaData.FIELD_STATE, ChatTableMetaData.FIELD_JID };

	private final static int[] names = new int[] { R.id.roomMsgBody };

	private final OnClickListener nicknameClickListener;

	private final Room room;

	public MucRoomAdapter(Context context, int layout, Room room,
			OnClickListener nicknameClickListener) {
		super(context, layout, null, cols, names,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		this.nicknameClickListener = nicknameClickListener;
		this.room = room;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.tvBody = (EmojiconTextView) view
					.findViewById(R.id.roomMsgBody);
			holder.timestamp = (TextView) view.findViewById(R.id.roomMsgTime);
			holder.roomMsgNick = (TextView) view.findViewById(R.id.roomMsgNick);
			holder.msgStatus = (ImageView) view.findViewById(R.id.msgStatus);

			holder.msgHolder = (LinearLayout) view
					.findViewById(R.id.mucMsgHolder);
		}

		holder.roomMsgNick.setOnClickListener(nicknameClickListener);
		LayoutParams lp = (LayoutParams) holder.msgHolder.getLayoutParams();

		final String bd = cursor.getString(cursor
				.getColumnIndex(ChatTableMetaData.FIELD_BODY));

		holder.roomMsgNick.setVisibility(View.VISIBLE);
		holder.timestamp.setVisibility(View.VISIBLE);
		final int state = cursor.getInt(cursor
				.getColumnIndex(ChatTableMetaData.FIELD_STATE));

		holder.timestamp.setTextColor(context.getResources().getColor(
				R.color.message_timestamp_dark_gray));
		holder.roomMsgNick.setTextColor(context.getResources().getColor(
				R.color.message_timestamp_dark_gray));

		if ((state == ChatTableMetaData.STATE_INCOMING)
				|| (state == ChatTableMetaData.STATE_INCOMING_UNREAD)) {
			((LayoutParams) holder.tvBody.getLayoutParams()).gravity = Gravity.LEFT;
			holder.tvBody.setTextColor(context.getResources().getColor(
					R.color.message_his_text));
			lp.gravity = Gravity.LEFT;

			view.setPadding(5, 5, 30, 5);
			final String nickname = cursor.getString(cursor
					.getColumnIndex(ChatTableMetaData.FIELD_AUTHOR_NICKNAME));
			holder.roomMsgNick.setText(nickname);
			if (bd.toLowerCase().contains(
					((String) room.getSessionObject().getUserProperty(
							SessionObject.NICKNAME)).toLowerCase())) {
				holder.msgHolder
						.setBackgroundColor(context
								.getResources()
								.getColor(
										R.color.conversation_item_sent_pending_background_light));
			} else {
				holder.msgHolder.setBackgroundColor(context.getResources()
						.getColor(R.color.message_his_background));
			}

			holder.timestamp.setVisibility(View.VISIBLE);
			holder.msgStatus.setVisibility(View.GONE);

		} else if (state == ChatTableMetaData.STATE_OUT_SENT) {
			((LayoutParams) holder.tvBody.getLayoutParams()).gravity = Gravity.RIGHT;
			lp.gravity = Gravity.RIGHT;
			holder.msgHolder.setBackgroundColor(context.getResources()
					.getColor(R.color.message_mine_background));

			view.setPadding(30, 5, 5, 5);
			holder.roomMsgNick.setText("");
			holder.timestamp.setVisibility(View.VISIBLE);
			holder.msgStatus.setVisibility(View.VISIBLE);
			holder.msgStatus.setImageResource(R.drawable.message_sent);

		} else if (state == ChatTableMetaData.STATE_OUT_NOT_SENT) {
			((LayoutParams) holder.tvBody.getLayoutParams()).gravity = Gravity.RIGHT;
			lp.gravity = Gravity.RIGHT;
			holder.msgHolder.setBackgroundColor(context.getResources()
					.getColor(R.color.message_mine_background));

			view.setPadding(30, 5, 5, 5);
			holder.roomMsgNick.setText("");
			holder.timestamp.setVisibility(View.INVISIBLE);
			holder.msgStatus.setVisibility(View.VISIBLE);
			holder.msgStatus.setImageResource(R.drawable.message_not_sent);

		}

		try {
			holder.tvBody.setText(bd);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		long ts = cursor.getLong(cursor
				.getColumnIndex(ChatTableMetaData.FIELD_TIMESTAMP));
		CharSequence tsStr = DateUtils.getBetterRelativeTimeSpanString(
				mContext, ts);
		holder.timestamp.setText(tsStr);

	}
}
