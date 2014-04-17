package org.jfet.batsPass;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class BatsKeyDialog extends TimerAlertDialog implements TextWatcher {
    private final EditText o, n, m;

    public BatsKeyDialog (final Context c) {
        super(c);

        this.setTitle(c.getString(R.string.action_rekey));

        final View nView = ((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.keydlg, null);

        o = (EditText) nView.findViewById(R.id.oldpass);
        n = (EditText) nView.findViewById(R.id.newpass);
        m = (EditText) nView.findViewById(R.id.confirm);

        o.addTextChangedListener(this);
        n.addTextChangedListener(this);
        m.addTextChangedListener(this);

        this.setView(nView);

        this.setButton(DialogInterface.BUTTON_NEGATIVE, c.getString(R.string.cancel_string), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) { BatsKeyDialog.this.cancel(); }
        });

        this.setButton(DialogInterface.BUTTON_POSITIVE, c.getString(R.string.ok_string), (DialogInterface.OnClickListener) null);

        this.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface dialog) {
                final Button b = BatsKeyDialog.this.getButton(DialogInterface.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (! passMatch()) {
                        	clearEditable(m.getText());
                            n.setHint(R.string.hint_newpass);
                            m.setHint(R.string.hint_confirm_match);
                        } else if (n.getText().length() < BatsPassMain.MIN_PASS_LENGTH) {
                        	clearEditable(n.getText());
                        	clearEditable(m.getText());
                            n.setHint(R.string.min_password);
                            m.setHint(R.string.hint_confirm);
                        } else if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) ) {
                            final String oS = getEditableValue(o.getText());
                            final String nS = getEditableValue(n.getText());
                            clearEditable(m.getText());
                            BatsPassMain.bpMain.get().dbDoRekey(oS,nS);
                            BatsKeyDialog.this.dismiss();
                        }
                    }                    
                });
            }
        });
    }
    
    // check whether these passwords match without stringifying them
    // fewer sensitive strings floating around in memory
    private boolean passMatch() {
    	final Editable nT = n.getText();
    	final Editable mT = m.getText();
    	if (nT.length() != mT.length()) {
    		return false;
    	}
    	
    	for (int i=0; i<nT.length(); i++) {
    		if (nT.charAt(i) != mT.charAt(i)) {
    			return false;
    		}
    	}
    	
    	return true;
    }

    private String getEditableValue (Editable e) {
        final String retString = e.toString();
        clearEditable(e);
        return retString;
    }
    
    private void clearEditable (Editable e) {
        for (int i=0; i<e.length(); i++) { e.replace(i, i+1, "Z"); }
        e.clear();
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
