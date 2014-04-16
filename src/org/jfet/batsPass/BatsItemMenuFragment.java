package org.jfet.batsPass;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

@SuppressLint("ValidFragment")
public class BatsItemMenuFragment extends DialogFragment {
    final static CharSequence[] options = new CharSequence[]{"Edit","Delete","Cancel"};

    final String service;
    final String id;
    final batsPassMain main;

    public BatsItemMenuFragment (batsPassMain main, String id, String service) {
        super();
        this.main = main;
        this.id = id;
        this.service = service;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());

        ab.setTitle(service);
        ab.setItems(options, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0: 
                    main.editPass(id);
                    break;
                case 1:
                    main.deletePass(id);
                    break;
                default:
                    dialog.cancel();
                }
            }
        });

        return ab.create();
    }
}