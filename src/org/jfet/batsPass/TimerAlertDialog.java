package org.jfet.batsPass;

import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;

public class TimerAlertDialog extends AlertDialog {
	public TimerAlertDialog (Context c) {
		super(c, AlertDialog.THEME_HOLO_DARK);
		super.setCancelable(true);
		super.setCanceledOnTouchOutside(true);
	}
	
	// intercept touch events and reset the watchdog timer
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) && (null != BatsPassMain.bpMain.get().sTimeout) ) {
			BatsPassMain.bpMain.get().sTimeout.interrupt();
		}
		return super.dispatchTouchEvent(ev);
	}
}
