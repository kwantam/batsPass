package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.CharArrayBuffer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

@SuppressLint("ValidFragment")
public class BatsItemFragment extends DialogFragment {
	final String id;
	final char[] sTxt;
	final char[] uTxt;
	final char[] pTxt;
	final batsPassMain main;

	public BatsItemFragment (batsPassMain main, String id, CharArrayBuffer[] sup) {
	    super();
	    this.main = main;
		this.id = id;
		this.sTxt = sup[0].data;
		this.uTxt = sup[1].data;
		this.pTxt = sup[2].data;
	}

	public BatsItemFragment (batsPassMain main) {
	    super();
	    this.main = main;
		this.id = null;
		this.sTxt = null;
		this.uTxt = null;
		this.pTxt = null;
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final TimerAlertDialog alert = new TimerAlertDialog(getActivity(),main.sTimeout);

		if (null == id) {
			alert.setTitle(getActivity().getString(R.string.new_login));
		} else {
			alert.setTitle(getActivity().getString(R.string.edit_login));
		}
		
		final View nView = getActivity().getLayoutInflater().inflate(R.layout.itemfrag, null);
		
		final EditText s = (EditText) nView.findViewById(R.id.serviceinput);
		final EditText u = (EditText) nView.findViewById(R.id.uidinput);
		final EditText p = (EditText) nView.findViewById(R.id.pwinput);
		
		final TextWatcher intWatcher = new TextWatcher() {
			public void afterTextChanged(Editable arg0) { main.sTimeout.interrupt(); }
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
					main.addEntry(cVals);
				}
			}
		});
		*/

		alert.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(R.string.cancel_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsItemFragment.this.getDialog().cancel(); }
		});

		return alert;
	}
	
	public void onPause() {
	    super.onPause();
	    if (null != sTxt) { Arrays.fill(sTxt, 'Z'); }
	    if (null != uTxt) { Arrays.fill(uTxt, 'Z'); }
	    if (null != pTxt) { Arrays.fill(pTxt, 'Z'); }
	}
}
