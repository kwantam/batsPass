package org.jfet.batsPass;

import java.nio.CharBuffer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

public abstract class BatsItemFragment extends DialogFragment {
	final String id;
	final char[] sTxt = null;
	final char[] uTxt = null;
	final char[] pTxt = null;

	public abstract void onComplete(ContentValues cVals);
	public abstract void sendInterrupt();

	public BatsItemFragment (String id) {
		this.id = id;
	}

	public BatsItemFragment () {
		this.id = null;
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final TimerAlertDialog alert = new TimerAlertDialog(getActivity()) {
			public void sendInterrupt() { BatsItemFragment.this.sendInterrupt(); }
		};

		if (null == id) {
			alert.setTitle("Create new service");
		} else {
			alert.setTitle("Edit service");
		}
		
		final View nView = getActivity().getLayoutInflater().inflate(R.layout.itemfrag, null);
		
		final EditText s = (EditText) nView.findViewById(R.id.serviceinput);
		final EditText u = (EditText) nView.findViewById(R.id.uidinput);
		final EditText p = (EditText) nView.findViewById(R.id.pwinput);
		
		final TextWatcher intWatcher = new TextWatcher() {
			public void afterTextChanged(Editable arg0) { sendInterrupt(); }
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
		};
		
		s.addTextChangedListener(intWatcher);
		u.addTextChangedListener(intWatcher);
		p.addTextChangedListener(intWatcher);
		
		if (null != id) {
			s.setText(CharBuffer.wrap(sTxt));
			u.setText(CharBuffer.wrap(uTxt));
			p.setText(CharBuffer.wrap(pTxt));
		}
		
		alert.setView(nView);

		/*
		alert.setButton(DialogInterface.BUTTON_POSITIVE, getActivity().getString(R.string.ok_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) {
				final ContentValues cVals = BatsItemFragment.this.getValues();
				if (null == cVals) {
					BatsItemFragment.this.getDialog().cancel();
				} else {
					onComplete(cVals);
				}
			}
		});
		*/
		alert.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(R.string.cancel_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsItemFragment.this.getDialog().cancel(); }
		});

		return alert;
	}
}
