package org.jfet.batsPass;

import java.io.File;
import java.nio.CharBuffer;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.CharArrayBuffer;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

public class batsPassMain extends Activity {
	private SQLiteDatabase passDB = null;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        
        setContentView(R.layout.main);
    }
    
    public void openDatabase (View view) {
    	final Editable passEdit = ((EditText) findViewById(R.id.password)).getText();
    	final char[] thePass = new char[passEdit.length()];	
    	// grab password, and zero it out from the Editable where it previously existed
    	for (int i = 0; i<passEdit.length(); i++) {
    		thePass[i] = passEdit.charAt(i);
    		passEdit.replace(i,i+1,"Z");
    	}
    	passEdit.clear();
    	
    	SQLiteDatabase.loadLibs(this);
    	
    	final File databaseFile = getDatabasePath("password.db");
    	final boolean fExists = databaseFile.exists();

    	// make sure the parent directories exist
    	if (! fExists) {
    	    databaseFile.getParentFile().mkdirs();
    	}

    	// try to open the database; figure out if the password is correct
    	try {
    	    passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),thePass,null);
    	} catch (SQLiteException ex) {
    	    passDB = null;
    	} finally {
    	    Arrays.fill(thePass, 'Z');
    	    if (null == passDB) {
    	        ((EditText) findViewById(R.id.password)).setHint(R.string.wrong_password);
    	        return;
    	    }
    	}

    	// create password table if it doesn't yet exist
    	if (! fExists) {
    	    passDB.execSQL("CREATE TABLE pass ( _id integer primary key autoincrement not null, service string not null, uid string, pw string not null ); ");
    	    final ContentValues initContent = new ContentValues();
    	    initContent.put("service","test");
    	    initContent.put("uid","quux");
    	    initContent.put("pw","foobar");
    	    passDB.insert("pass", null, initContent);
    	}
    	
    	// go to the password list view
    	setContentView(R.layout.passlist);
    	final Cursor c = passDB.query("pass",new String[]{"_id","service"},null,null,null,null,null,null);
    	if ( (null == c) || (0 == c.getCount()) ) {
    	    return;
    	}

    	// put in buttons for each item in the db
    	final LinearLayout ll = (LinearLayout) findViewById(R.id.buttonLayout);

    	c.moveToFirst();
    	do {
    	    final Button b = new Button(view.getContext());
    	    b.setLayoutParams(((ImageButton) findViewById(R.id.addButton)).getLayoutParams());
    	    b.setText(c.getString(1));
    	    final String idStr = Integer.toString(c.getInt(0));
    	    b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showPassInfo(v, idStr); }
    	    } );
    	    ll.addView(b);
    	    c.moveToNext();
    	} while (! c.isAfterLast());
    	c.close();
    }

    private void showPassInfo(View v, String id) {
        if (null == passDB) {
            setContentView(R.layout.main);
            return;
        }

        final Cursor c = passDB.query("pass",new String[]{"service","uid","pw"},"_id="+id,null,null,null,null,null);

        if ( (null == c) || (0 == c.getCount()) ) {
            return;
        }
        
        c.moveToFirst();
        final char[] dummy = new char[0];
        final CharArrayBuffer service = new CharArrayBuffer(dummy);
        final CharArrayBuffer uid = new CharArrayBuffer(dummy);
        final CharArrayBuffer pw = new CharArrayBuffer(dummy);
        c.copyStringToBuffer(0,service);
        c.copyStringToBuffer(1,uid);
        c.copyStringToBuffer(2,pw);
        c.close();
        
        final char[] tmp1 = "Username:\n".toCharArray();
        final char[] tmp2 = "\n\nPassword:\n".toCharArray();
        final char[] msg = new char[uid.data.length + pw.data.length + tmp1.length + tmp2.length];
        
        for (int i=0; i<tmp1.length; i++) {
            msg[i] = tmp1[i];
        }
        for (int i=0; i<uid.data.length; i++) {
            msg[tmp1.length + i] = uid.data[i];
            uid.data[i] = 'Z';
        }
        for (int i=0; i<tmp2.length; i++) {
            msg[tmp1.length + uid.data.length + i] = tmp2[i];
        }
        for (int i=0; i<pw.data.length; i++) {
            msg[tmp1.length + uid.data.length + tmp2.length + i] = pw.data[i];
            pw.data[i] = 'Z';
        }
        
        final DialogFragment dlg = new PassDlg(msg,service.data);
        dlg.show(getFragmentManager(), "ZZZZZZZZ");
    }
    
    public void addButton(View view) {
        return;
    }
    
    @SuppressLint("ValidFragment")
    public static class PassDlg extends DialogFragment {
        final char[] msg;
        final char[] title;
        
        public PassDlg (char[] msg, char[] title) {
            super();
            this.msg = msg;
            this.title = title;
        }
        
        public PassDlg () {
            super();
            this.msg = "".toCharArray();
            this.title = "".toCharArray();
        }
        
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
            ab.setNegativeButton(R.string.done_string, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int i) { PassDlg.this.getDialog().cancel(); }
            });
            ab.setTitle(CharBuffer.wrap(title));
            ab.setMessage(CharBuffer.wrap(msg));
            return ab.create();
        }
        
        public void onPause() {
            super.onPause();
            Arrays.fill(msg, 'Z');
            Arrays.fill(title, 'Z');
        }
    }
    
    public void onPause() {
        super.onPause();
        
        final DialogFragment dlg = (DialogFragment) getFragmentManager().findFragmentByTag("ZZZZZZZZ");
        if (null != dlg) {
            dlg.dismiss();
            getFragmentManager().executePendingTransactions();
        }

        if ( (null != passDB) && passDB.isOpen() ) {
            passDB.close();
            passDB = null;
        }
        
        setContentView(R.layout.main);
    }
}
