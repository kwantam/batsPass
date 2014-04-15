package org.jfet.batsPass;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

@SuppressLint("ValidFragment")
public abstract class BatsItemMenuFragment extends DialogFragment {
	final String service;
	final String id;
	final static CharSequence[] options = new CharSequence[]{"Edit","Duplicate","Delete","Cancel"};
	
	public abstract void onEdit(DialogInterface dialog);
	public abstract void onDuplicate(DialogInterface dialog);
	public abstract void onDelete(DialogInterface dialog);

	public BatsItemMenuFragment (String id, String service) {
		super();
		this.id = id;
		this.service = service;
	}

	public BatsItemMenuFragment () {
		super();
		this.id = "";
		this.service = "";
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		
		ab.setTitle(service);
		ab.setItems(options, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0: onEdit(dialog); break;
				case 1: onDuplicate(dialog); break;
				case 2: onDelete(dialog); break;
				default:
					dialog.cancel();
				}
			}
		});
		
		return ab.create();
	}
}