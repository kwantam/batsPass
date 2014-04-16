package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public abstract class BatsPassDialogFragment extends DialogFragment {
	final char[] msg;
	final char[] title;

	public BatsPassDialogFragment (char[] msg, char[] title) {
		super();
		this.msg = msg;
		this.title = title;
	}

	public BatsPassDialogFragment () {
		super();
		this.msg = "".toCharArray();
		this.title = "".toCharArray();
	}
	
	public abstract void sendInterrupt();

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final TimerAlertDialog alert = new TimerAlertDialog(getActivity()) {
			public void sendInterrupt() { BatsPassDialogFragment.this.sendInterrupt(); }
		};
		
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