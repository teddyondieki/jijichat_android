package co.jijichat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import co.jijichat.service.JijichatService;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent i) {
		Intent intent = new Intent(context, JijichatService.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(intent);

	}

}
