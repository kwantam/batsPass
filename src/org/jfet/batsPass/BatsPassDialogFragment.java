package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

@SuppressLint("ValidFragment")
public class BatsPassDialogFragment extends DialogFragment {
	final char[] msg;
	final char[] title;
	final Thread sTimeout;

	public BatsPassDialogFragment (char[] msg, char[] title, Thread sTimeout) {
		super();
		this.msg = msg;
		this.title = title;
		this.sTimeout = sTimeout;
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final TimerAlertDialog alert = new TimerAlertDialog(getActivity(),sTimeout);
		
		alert.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(R.string.done_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsPassDialogFragment.this.getDialog().cancel(); }
		});
		alert.setTitle(CharBuffer.wrap(title));
		alert.setMessage(CharBuffer.wrap(msg));
		
		return alert;
	}

	public void onPause() {
		super.onPause();
		Arrays.fill(msg, 'Z');
		Arrays.fill(title, 'Z');
	}
}