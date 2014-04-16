package org.jfet.batsPass;

import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;

public abstract class TimerAlertDialog extends AlertDialog {
	public TimerAlertDialog (Context c) {
		super(c, AlertDialog.THEME_HOLO_DARK);
		
		super.setCancelable(true);
		super.setCanceledOnTouchOutside(true);
	}
	
	public abstract void sendInterrupt();
	
	public boolean dispatchTouchEvent(MotionEvent ev) {
		sendInterrupt();
		return super.dispatchTouchEvent(ev);
	}
}
