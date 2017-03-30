package co.jijichat.muc;

import github.ankushsachdeva.emojicon.EmojiconTextView;
import tigase.jaxmpp.core.client.BareJID;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import co.jijichat.JijichatMobileMessengerActivity;
import co.jijichat.R;
import co.jijichat.db.MucTableMetaData;
import co.jijichat.utils.AvatarHelper;
import co.jijichat.utils.MessageHelper;

public class MucAdapter extends SimpleCursorAdapter {
	private final static String[] cols = new String[] { MucTableMetaData.FIELD_ROOM_JID };
	private final static int[] names = new int[] { R.id.roomName };

	public MucAdapter(Context context, int layout) {
		super(context, layout, null, cols, names,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
	}

	static class ViewHolder {
		ImageView roomAvatar;
		TextView itemName;
		EmojiconTextView itemDescription;
		TextView tvUnreadCounter;

	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			view.setTag(holder);
			holder.itemName = (TextView) view.findViewById(R.id.roomName);
			holder.itemDescription = (EmojiconTextView) view
					.findViewById(R.id.roomDescription);
			holder.roomAvatar = (ImageView) view.findViewById(R.id.roomAvatar);
			holder.tvUnreadCounter = (TextView) view
					.findViewById(R.id.roomUnreadCounter);

		}

		holder.itemName.setTransformationMethod(SingleLineTransformationMethod
				.getInstance());
		final String name = cursor.getString(cursor
				.getColumnIndex(MucTableMetaData.FIELD_ROOM_NAME));

		holder.itemName.setText(name);

		final String description = cursor.getString(cursor
				.getColumnIndex(MucTableMetaData.FIELD_ROOM_DESCRIPTION));
		holder.itemDescription.setText(description);

		final BareJID bareJid = BareJID.bareJIDInstance(cursor.getString(cursor
				.getColumnIndex(MucTableMetaData.FIELD_ROOM_JID)));

		holder.roomAvatar.setImageDrawable(AvatarHelper.getAvatar(bareJid));

		Integer count = JijichatMobileMessengerActivity.mUnreadCounters.get(bareJid
				.toString());
		if (count == null)
			count = 0;
		holder.tvUnreadCounter.setText(count.toString());

		if (count > 0) {
			holder.tvUnreadCounter.setVisibility(View.VISIBLE);
			holder.tvUnreadCounter.bringToFront();
			holder.itemDescription.setTypeface(null, Typeface.BOLD);
		} else if (count == 0) {
			holder.tvUnreadCounter.setVisibility(View.INVISIBLE);
			holder.itemDescription.setTypeface(null, Typeface.NORMAL);
		}

		String lastMessage = MessageHelper.getMessage(bareJid);
		try {
			holder.itemDescription.setText(lastMessage);
		} catch (Exception ex) {
			// ex.printStackTrace();
		}

	}

	public static int getOccupantColor(final String nick) {
		ColorGenerator generator = ColorGenerator.DEFAULT;
		int color = generator.getColor(nick);
		return color;
	}

}
