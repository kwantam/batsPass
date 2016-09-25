package org.jfet.batsPass;

import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;

public class TimerAlertDialog extends AlertDialog {
	public TimerAlertDialog (Context c) {
		super(c, THEME_HOLO_DARK);
		super.setCancelable(true);
		super.setCanceledOnTouchOutside(true);
	}
	
	// intercept touch events and reset the watchdog timer
	public boolean dispatchTouchEvent(MotionEvent ev) {
		BatsPassMain.resetTimer();
		return super.dispatchTouchEvent(ev);
	}
}
