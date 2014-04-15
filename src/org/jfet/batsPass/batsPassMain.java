package org.jfet.batsPass;

import java.io.File;
import java.util.Arrays;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.CharArrayBuffer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.view.MotionEvent;
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
	final static String[] fragTags = new String[]{ "bpItemDlg", "bpItemMenu", "bpItemNew", "bpItemEdit", "bpConfirm", "bpTopMenu" };
	final static long dlgTimeout = 10000;

	private SQLiteDatabase passDB = null;
	private SecretTimeout sTimeout = null;
	private Handler uiHandler = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

		setContentView(R.layout.main);
		
		uiHandler = new QuitHandler(Looper.getMainLooper());
		sTimeout = new SecretTimeout();
		sTimeout.start();
	}
	
	public boolean dispatchTouchEvent(MotionEvent ev) {
		sTimeout.interrupt();
		return super.dispatchTouchEvent(ev);
	}

	public void openDatabase (View v) {
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

		showPassList(v);
	}

	private void showPassList(View v) {
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
			final Button b = new Button(v.getContext());
			b.setLayoutParams(((ImageButton) findViewById(R.id.addButton)).getLayoutParams());
			final String idStr = Integer.toString(c.getInt(0));
			final String srvStr = c.getString(1);
			b.setText(srvStr);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) { showPassInfo(v, idStr); }
			} );
			b.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(final View v) {
					final DialogFragment dlg = new BatsItemMenuFragment(idStr, srvStr) {
						public void onEdit(DialogInterface dialog) { editPass(v, idStr); }
						public void onDuplicate(DialogInterface dialog) { duplicatePass(v, idStr); }
						public void onDelete(DialogInterface dialog) { deletePass(v, idStr); }
					};
					dlg.show(getFragmentManager(), fragTags[1]);

					return true;
				}
			});
			ll.addView(b);
			c.moveToNext();
		} while (! c.isAfterLast());
		c.close();    	
	}
	
	private void editPass(View v, String id) {
		return;
	}
	
	private void duplicatePass(View v, String id) {
		return;
	}
	
	private void deletePass(View v, String id) {
		return;
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

		strcopy(msg,tmp1,0);
		strcopy(msg,uid.data,tmp1.length);
		strcopy(msg,tmp2,tmp1.length+uid.data.length);
		strcopy(msg,pw.data,tmp1.length+uid.data.length+tmp2.length);
		Arrays.fill(uid.data,'Z');
		Arrays.fill(pw.data,'Z'); 

		final DialogFragment dlg = new BatsPassDialogFragment(msg,service.data);
		dlg.show(getFragmentManager(), fragTags[0]);
	}

	private void strcopy(char[] to, char[] from, int toOffset) {
		for (int i=0; i<from.length; i++) {
			to[toOffset + i] = from[i];
		}
	}

	public void addButton(View view) {
		return;
	}
	
	private void clearSecrets() {
		// clear all the fragments we might have shown
		for (int i=0; i<fragTags.length; i++) {
			final DialogFragment dlg = (DialogFragment) getFragmentManager().findFragmentByTag(fragTags[i]);
			if (null != dlg) {
				dlg.dismiss();
			}
		}
		getFragmentManager().executePendingTransactions();

		if ( (null != passDB) && passDB.isOpen() ) {
			passDB.close();
			passDB = null;
		}

		setContentView(R.layout.main);		
	}

	public void onPause() {
		super.onPause();
		sTimeout.signalQuit();
		sTimeout.interrupt();
		clearSecrets();
		finish();
	}
	
	private class SecretTimeout extends Thread {
		private volatile boolean quitTimer = false;
		public void signalQuit() { quitTimer = true; }

		public void run() {
			while(true) {
				try {
					Thread.sleep(2*batsPassMain.dlgTimeout);
				} catch (InterruptedException ex) {
					if (quitTimer) {
						return;
					} else {
						continue;
					}
				}

				break;
			}
			
			batsPassMain.this.uiHandler.obtainMessage(0,0,0).sendToTarget();
			return;
		}
	}
	
	private class QuitHandler extends Handler {
		public QuitHandler (Looper l) { super(l); }
		
		public void handleMessage (Message m) {
			clearSecrets();
			finish();
		}
	}
}