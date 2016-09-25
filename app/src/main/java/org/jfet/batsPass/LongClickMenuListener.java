package org.jfet.batsPass;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnLongClickListener;

public class LongClickMenuListener implements OnLongClickListener {
	final static CharSequence[] itemMenuOptions = new CharSequence[] { "Edit", "Delete", "Cancel" };

	private final String idStr;
	private final String srvStr;

	public LongClickMenuListener(String idStr, String srvStr) {
		this.idStr = idStr;
		this.srvStr = srvStr;
	}

	public boolean onLongClick(final View v) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(v.getContext());
		ab.setTitle(srvStr);
		ab.setItems(itemMenuOptions, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get())  ) {
					switch (which) {
					case 0:
						BatsPassMain.bpMain.get().editPass(idStr);
						break;
					case 1:
						BatsPassMain.bpMain.get().confirmDeletePass(idStr);
						break;
					default:
						dialog.cancel();
					}
				} else {
					dialog.cancel();
				}
			}
		});

		if ( (null != BatsPassMain.bpMain) && (null != BatsPassMain.bpMain.get()) ) {
			BatsPassMain.bpMain.get().showDialog(ab.create());
		}

		return true;
	}	
}
