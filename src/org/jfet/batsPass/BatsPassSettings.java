package org.jfet.batsPass;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.WindowManager;

public class BatsPassSettings extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

		addPreferencesFromResource(R.xml.preferences);
	}
}
