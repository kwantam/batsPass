package org.jfet.batsPass;

import java.nio.CharBuffer;
import java.util.Arrays;

import android.content.Context;
import android.content.DialogInterface;

public class BatsPassDialog extends TimerAlertDialog {
    public BatsPassDialog (Context c, final char[] msg, final char[] title) {
        super(c);

        this.setButton(DialogInterface.BUTTON_NEGATIVE, c.getString(R.string.done_string), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int i) { BatsPassDialog.this.cancel(); }
        });

        this.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface arg0) {
                Arrays.fill(msg, 'Z');
                Arrays.fill(title, 'Z');
            }
        });

        this.setTitle(CharBuffer.wrap(title));
        this.setMessage(CharBuffer.wrap(msg));
    }
}
