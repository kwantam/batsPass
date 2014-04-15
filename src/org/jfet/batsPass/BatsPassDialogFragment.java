package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

@SuppressLint("ValidFragment")
public class BatsPassDialogFragment extends DialogFragment {
	final char[] msg;
	final char[] title;
	DialogTimeout dTimeout = null;

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

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setNegativeButton(R.string.done_string, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsPassDialogFragment.this.getDialog().cancel(); }
		});
		ab.setTitle(CharBuffer.wrap(title));
		ab.setMessage(CharBuffer.wrap(msg));
		
		dTimeout = new DialogTimeout();
		dTimeout.start();
		return ab.create();
	}

	public void onPause() {
		super.onPause();
		Arrays.fill(msg, 'Z');
		Arrays.fill(title, 'Z');
		if (null != dTimeout) {
			dTimeout.interrupt();
		}
	}
	
	// make sure that this popup cancels itself after a few seconds
	private class DialogTimeout extends Thread {
		public void run() {
			final Dialog dlg = BatsPassDialogFragment.this.getDialog();

			try {
				Thread.sleep(batsPassMain.dlgTimeout);
			} catch (InterruptedException ex) { }
			
			if ( (null != dlg) && dlg.isShowing() ) {
				dlg.cancel();
			}
		}
	}
}