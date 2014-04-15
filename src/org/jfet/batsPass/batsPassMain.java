package org.jfet.batsPass;

import java.io.File;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
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

public class batsPassMain extends Activity
{
	private boolean didLoad = false;
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
    	// now that we've overwritten the bytes in memory
    	// (we hope... the GC will fight us on this), set
    	// the length to zero
    	passEdit.clear();
    	
    	if (! didLoad) {
    		didLoad = true;
    		SQLiteDatabase.loadLibs(this);
    	}
    	
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
    	    clearPassword(thePass);
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
        final String service = c.getString(0);
        final String uid = c.getString(1);
        final String pw = c.getString(2);
        c.close();
        
        final DialogFragment dlg = new PassDlg() {
            protected void setData(AlertDialog.Builder ab) {
                ab.setTitle(service);
                ab.setMessage("Username:\n" + uid + "\n\nPassword:\n" + pw);
            }
        };
        dlg.show(getFragmentManager(), "ZZZZZZZZ");
    }
    
    public void addButton(View view) {
        return;
    }
    
    private void clearPassword(char[] p) {
        for (int i=0; i<p.length; i++) {
            p[i]='Z';
        }
    }
    
    public static abstract class PassDlg extends DialogFragment {
        protected abstract void setData(AlertDialog.Builder ab);
        
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
            ab.setNegativeButton(R.string.done_string, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int i) { PassDlg.this.getDialog().cancel(); }
            });
            setData(ab);
            return ab.create();
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