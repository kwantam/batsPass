package org.jfet.batsPass;

import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;

public class TimerAlertDialog extends AlertDialog {
    final private Thread sTimeout;
    
	public TimerAlertDialog (Context c, Thread sTimeout) {
		super(c, AlertDialog.THEME_HOLO_DARK);
		super.setCancelable(true);
		super.setCanceledOnTouchOutside(true);
		
		this.sTimeout = sTimeout;
	}
	
	// intercept touch events and reset the watchdog timer
	public boolean dispatchTouchEvent(MotionEvent ev) {
	    sTimeout.interrupt();
		return super.dispatchTouchEvent(ev);
	}
}
