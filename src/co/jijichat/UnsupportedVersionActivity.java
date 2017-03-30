package co.jijichat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import co.jijichat.R;
import com.actionbarsherlock.app.SherlockActivity;

public class UnsupportedVersionActivity extends SherlockActivity {
	// private SharedPreferences prefs;

	// private static final String TAG = "UnsupportedVersionActivity";
	EditText statusText;
	Button updateButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.JijiLightTheme);
		super.onCreate(savedInstanceState);
		// this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.activity_unsupported_version);
		updateButton = (Button) findViewById(R.id.updateButton);
		updateButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent browse = new Intent(Intent.ACTION_VIEW, Uri
						.parse("https://jijichat.co/download"));
				startActivity(browse);

			}
		});
	}

}
