package org.jfet.batsPass;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class BatsImportDialog extends TimerAlertDialog implements TextWatcher {
    private final EditText n, m;

    public BatsImportDialog (final Context c) {
        super(c);

        this.setTitle(c.getString(R.string.action_import));
        this.setCancelable(false);
        this.setCanceledOnTouchOutside(false);

        final View nView = ((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.importdlg, null);

        n = (EditText) nView.findViewById(R.id.srcpass);
        n.addTextChangedListener(this);
        m = (EditText) nView.findViewById(R.id.dstpass);
        m.addTextChangedListener(this);

        this.setView(nView);

        this.setButton(DialogInterface.BUTTON_POSITIVE, c.getString(R.string.ok_string), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int what) {
                if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) ) {
                    BatsPassMain.bpMain.get().dbDoImport(BatsKeyDialog.getEditableCharValue(n.getText()),BatsKeyDialog.getEditableCharValue(m.getText()));
                }
            }
        });
    }

    // TEXTWATCHER METHODS
    public void afterTextChanged(Editable arg0) { BatsPassMain.resetTimer(); }
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
}
