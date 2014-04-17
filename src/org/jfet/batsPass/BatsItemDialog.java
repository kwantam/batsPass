package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.CharArrayBuffer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class BatsItemDialog extends TimerAlertDialog implements TextWatcher {
	private final Integer id;
	private final EditText s, u, p;

	public BatsItemDialog (final Context c, final String id, final CharArrayBuffer[] sup) {
	    super(c);
		
		if (null == id) {
			this.setTitle(c.getString(R.string.new_login));
			this.id = null;
		} else {
			this.setTitle(c.getString(R.string.edit_login));
			this.id = Integer.valueOf(id);
		}
		
		final View nView = ((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.itemfrag, null);
		
		s = (EditText) nView.findViewById(R.id.serviceinput);
		u = (EditText) nView.findViewById(R.id.uidinput);
		p = (EditText) nView.findViewById(R.id.pwinput);
		
		s.addTextChangedListener(this);
		u.addTextChangedListener(this);
		p.addTextChangedListener(this);
		
		if ( (null != sup) && (sup.length == 3) ) {
			s.setText(CharBuffer.wrap(sup[0].data));
			u.setText(CharBuffer.wrap(sup[1].data));
			p.setText(CharBuffer.wrap(sup[2].data));
			this.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					Arrays.fill(sup[0].data, 'Z');
					Arrays.fill(sup[1].data, 'Z');
					Arrays.fill(sup[2].data, 'Z');
				}
			});
		}
		
		this.setView(nView);
		
		this.setButton(DialogInterface.BUTTON_NEGATIVE, c.getString(R.string.cancel_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsItemDialog.this.cancel(); }
		});
		
		this.setButton(DialogInterface.BUTTON_POSITIVE, c.getString(R.string.ok_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) {
				final ContentValues cVals = BatsItemDialog.this.getValues();
				if (null == cVals) {
					BatsItemDialog.this.cancel();
				} else if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) ) {
					BatsPassMain.bpMain.get().addEntry(cVals);
				}
			}
		});
	}
	
	private ContentValues getValues() {
		final ContentValues cVals = new ContentValues();
		
		if (null != id) {
			cVals.put(BatsPassMain.ID_KEY, id);
		}
		
		getEditableValue(u.getText(), cVals, BatsPassMain.UID_KEY);
		
		if ( (! getEditableValue(s.getText(), cVals, BatsPassMain.SERVICE_KEY)) || 
			 (! getEditableValue(p.getText(), cVals, BatsPassMain.PASS_KEY)) ) {
			return null;
		} else {
			return cVals;
		}
	}
	
	private boolean getEditableValue (Editable e, ContentValues cVals, String key) {
		if (0 == e.length()) {
			return false;
		}
		
		cVals.put(key, e.toString());
		
		for (int i=0; i<e.length(); i++) { e.replace(i, i+1, "Z"); }
		e.clear();
		
		return true;
	}

// TEXTWATCHER METHODS
	public void afterTextChanged(Editable arg0) {
		if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) && (null != BatsPassMain.bpMain.get().sTimeout) ) {
			BatsPassMain.bpMain.get().sTimeout.interrupt();
		}
	}
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }	
}
