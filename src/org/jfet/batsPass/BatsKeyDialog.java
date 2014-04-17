package org.jfet.batsPass;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class BatsKeyDialog extends TimerAlertDialog implements TextWatcher {
	private final EditText o, n;

	public BatsKeyDialog (final Context c) {
	    super(c);
		
	    this.setTitle(c.getString(R.string.action_rekey));
		
		final View nView = ((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.keydlg, null);
		
		o = (EditText) nView.findViewById(R.id.oldpass);
		n = (EditText) nView.findViewById(R.id.newpass);
		
		o.addTextChangedListener(this);
		n.addTextChangedListener(this);
		
		this.setView(nView);
		
		this.setButton(DialogInterface.BUTTON_NEGATIVE, c.getString(R.string.cancel_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) { BatsKeyDialog.this.cancel(); }
		});
		
		this.setButton(DialogInterface.BUTTON_POSITIVE, c.getString(R.string.ok_string), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface d, int i) {
				final String oS = getEditableValue(o.getText());
				final String nS = getEditableValue(n.getText());

				if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) ) {
					BatsPassMain.bpMain.get().dbDoRekey(oS,nS);
				}
			}
		});
	}
	
	private String getEditableValue (Editable e) {
		final String retString = e.toString();
		
		for (int i=0; i<e.length(); i++) { e.replace(i, i+1, "Z"); }
		e.clear();
		
		return retString;
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
