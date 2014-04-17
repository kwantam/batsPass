package org.jfet.batsPass;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
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

public class BatsPassMain extends Activity implements TextWatcher {
	final static String ID_KEY = "_id";
	final static String SERVICE_KEY = "service";
	final static String UID_KEY = "uid";
	final static String PASS_KEY = "pw";
	final static String DB_NAME = "pass";
	final static int RESULT_SETTINGS = 1337;
	final static int MIN_PASS_LENGTH = 10;
	final static long dlgTimeout = 40000;

	static WeakReference<BatsPassMain> bpMain;

	private WeakReference<Dialog> activeDialog = null;
	private SQLiteDatabase passDB = null;
	Thread sTimeout = null;
	private Handler uiHandler = null;
	private BatsPassGen gen = null;

	// OVERRIDDEN ACTIVITY METHODS
	// start us off, Jerry
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		clearSecrets();

		if ( (null != bpMain) && (null != bpMain.get()) && (!this.equals(bpMain.get())) ) {
			throw new RuntimeException("BatsPass already running!");
		} else {
			bpMain = new WeakReference<BatsPassMain>(this);
		}

		// handler for messages from other threads
		uiHandler = new QuitHandler(Looper.getMainLooper());
		// preload the dictionary in another thread
		new DictionaryLoader().start();
		// timer thread
	}
	
	// start a timeout when we become visible
	public void onResume() {
		super.onResume();
		startTimer();
	}

	// kill the timeout when we become invisible, and clearSecrets()
	public void onPause() {
		super.onPause();
		stopTimer();
		clearSecrets();
	}
	
	// when we are going away, remove the static weakref
	public void onDestroy() {
		super.onDestroy();
		bpMain.clear();
		bpMain = null;
	}

	// intercept touch events and reset the watchdog timer
	public boolean dispatchTouchEvent(MotionEvent ev) {
		sTimeout.interrupt();
		return super.dispatchTouchEvent(ev);
	}

	// MENU STUFF
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// launch the menu thing
	public boolean onOptionsItemSelected(MenuItem mi) {
		switch (mi.getItemId()) {
		case R.id.action_about:
			showHelpDialog();
			return true;
		case R.id.action_rekey:
			dbRekey();
			return true;
		case R.id.action_settings:
			startActivityForResult(new Intent(this, BatsPassSettings.class), RESULT_SETTINGS);
			return true;
		default:
			return super.onOptionsItemSelected(mi);
		}
	}

	// OTHER UI METHODS
	// show the list of credentials available in the database
	private void showPassList() {
		// go to the password list view
		setContentView(R.layout.passlist);

		final Cursor c = passDB.query(DB_NAME,new String[]{ID_KEY,SERVICE_KEY},null,null,null,null,null,null);
		if ( (null == c) || (0 == c.getCount()) ) {
			if (null != c) {
				c.close();
			}
			return;
		}

		// put in buttons for each item in the db
		final LinearLayout ll = (LinearLayout) findViewById(R.id.buttonLayout);

		c.moveToFirst();
		do {
			final Button b = new Button(this);
			b.setLayoutParams(((ImageButton) findViewById(R.id.addButton)).getLayoutParams());
			final String idStr = Integer.toString(c.getInt(0));
			final String srvStr = c.getString(1);
			b.setText(srvStr);
			b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showPass(v, idStr); } } );
			b.setOnLongClickListener(new LongClickMenuListener(idStr,srvStr));
			ll.addView(b);
			c.moveToNext();
		} while (! c.isAfterLast());
		c.close();    	
	}
	
	// stop the timer
	private void stopTimer() {
		if (null != sTimeout) {
			((SecretTimeout) sTimeout).signalQuit();
			sTimeout.interrupt();
			sTimeout = null;	
		}
	}
	
	// start the timer
	private void startTimer() {
		if ( (null != sTimeout) && (sTimeout.isAlive()) ) {
			stopTimer();
		}
		sTimeout = new SecretTimeout();
		sTimeout.start();		
	}

	// clear fragments, return to "login" screen
	private void clearSecrets() {
		clearDialog();

		if (null != passDB) {
			passDB.close();
		}
		passDB = null;

		setContentView(R.layout.main);		

		((EditText) findViewById(R.id.password)).addTextChangedListener(this);
	}

	private void showHelpDialog() {
		final TimerAlertDialog dlg = new TimerAlertDialog(this);
		dlg.setTitle(getString(R.string.action_about));
		dlg.setMessage(getString(R.string.action_about_message));
		dlg.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.ok_string),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int what) { dlg.dismiss(); }
		});
		showDialog(dlg);
	}

	// DATABASE METHODS
	// open the database
	public void openDatabase (View v) {
		final Editable passEdit = ((EditText) findViewById(R.id.password)).getText();
		final char[] thePass = new char[passEdit.length()];	
		// grab password, and zero it out from the Editable where it previously existed
		for (int i = 0; i<passEdit.length(); i++) {
			thePass[i] = passEdit.charAt(i);
			passEdit.replace(i,i+1,"Z");
		}
		passEdit.clear();

		if (thePass.length < BatsPassMain.MIN_PASS_LENGTH) {
			((EditText) findViewById(R.id.password)).setHint(R.string.min_password);
			return;
		}

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
			passDB.execSQL("CREATE TABLE "+ DB_NAME + 
					" ( " + ID_KEY + " integer primary key autoincrement not null, " +
					SERVICE_KEY + " string not null, " +
					UID_KEY + " string, " +
					PASS_KEY + " string not null ); ");
			final ContentValues initContent = new ContentValues();
			initContent.put(SERVICE_KEY,"test");
			initContent.put(UID_KEY,"quux");
			initContent.put(PASS_KEY,"foobar");
			passDB.insert(DB_NAME, null, initContent);
		}

		showPassList();
	}

	private Cursor getByID(String id) {
		return passDB.query(DB_NAME,new String[]{SERVICE_KEY,UID_KEY,PASS_KEY},ID_KEY+"="+id,null,null,null,null,null);	
	}

	// given a cursor, make a CharArrayBuffer[] from the first item
	private CharArrayBuffer[] getCurData(Cursor c) {
		final char[] dummy = new char[0];
		final CharArrayBuffer[] outs = new CharArrayBuffer[3];

		c.moveToFirst();
		for (int i=0; i<3; i++) { 
			outs[i] = new CharArrayBuffer(dummy);
			c.copyStringToBuffer(i, outs[i]);
		}
		c.close();

		return outs;
	}

	// called from BatsItemMenu
	void editPass(String id) {
		final Cursor c = getByID(id);
		if ( (null == c) || (0 == c.getCount())) {
			if (null != c) {
				c.close();
			}
			newPass(null);
		} else {
			showDialog(new BatsItemDialog(this,id,getCurData(c)));
		}
		return;
	}


	// called from BatsItemMenu
	void confirmDeletePass(final String id) {
		final AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle(R.string.confirm_delete);
		ab.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				BatsPassMain.this.deletePass(id);
			}
		});
		ab.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		showDialog(ab.create());
		return;
	}

	private void deletePass (String id) {
		passDB.delete(DB_NAME, ID_KEY+"="+id, null);
		showPassList();
	}

	// takes View because it's a button callback
	// but we don't actually use it!
	public void newPass(View v) {
		showDialog(new BatsItemDialog(this,null,null));
	}

	// callback point from BatsItemDialog to add or update
	void addEntry(ContentValues cVals) {
		if (null == cVals.getAsInteger(ID_KEY)) {
			passDB.insert(DB_NAME, null, cVals);
		} else {
			passDB.replace(DB_NAME, null, cVals);
		}

		cVals.clear();
		showPassList();
	}

	void dbRekey() {
		final Dialog dlg = new BatsKeyDialog(this);
		showDialog(dlg);
	}

	void dbDoRekey(String oS, String nS) {
		if ( (null != passDB) && passDB.isOpen() ) {
			passDB.close();
		}

		final File databaseFile = getDatabasePath("password.db");

		try {
			passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),oS,null);
		} catch (SQLiteException ex) {
			clearSecrets();
			return;
		}

		passDB.rawExecSQL("PRAGMA rekey = '" + nS.replaceAll("'","''") + "';");
		clearSecrets();
	}

	// callback from the service list when we're asked to display
	private void showPass(View v, String id) {
		if (null == passDB) {
			clearSecrets();
			return;
		}

		final Cursor c = getByID(id);
		if ( (null == c) || (0 == c.getCount()) ) {
			if (null != c) {
				c.close();
			}
			return;
		}

		final CharArrayBuffer[] sup = getCurData(c);

		final char[] tmp1 = "Username:\n".toCharArray();
		final char[] tmp2 = "\n\nPassword:\n".toCharArray();
		final char[] msg = new char[sup[1].data.length + sup[2].data.length + tmp1.length + tmp2.length];

		strcopy(msg,tmp1,0);
		strcopy(msg,sup[1].data,tmp1.length);
		strcopy(msg,tmp2,tmp1.length+sup[1].data.length);
		strcopy(msg,sup[2].data,tmp1.length+sup[1].data.length+tmp2.length);
		Arrays.fill(sup[1].data,'Z');
		Arrays.fill(sup[2].data,'Z'); 

		showDialog(new BatsPassDialog(this,msg,sup[0].data));
	}


	// PASSWORD GENERATOR FUNCTIONS
	// once we've loaded the dictionary in the background, update the gen instance
	private void populateGen() { gen = new BatsPassGen(BatsPassGenDict.getInstance(this)); }

	// generate a dict-based password
	public void fillPwDict(View v) {
		if (null != gen) {
			final int targetId;
			switch (v.getId()) {
			case R.id.pwinput_dict:
				targetId = R.id.pwinput;
				break;
			case R.id.newpass_dict:
				targetId = R.id.newpass;
				break;
			default:
				return;
			}
			((EditText) v.getRootView().findViewById(targetId)).setText(CharBuffer.wrap(gen.genDictPass(getPwLengthPref())));
		}
	}

	// generate a truly random password
	public void fillPwRand(View v) {
		if (null != gen) {
			final int targetId;
			switch (v.getId()) {
			case R.id.pwinput_rand:
				targetId = R.id.pwinput;
				break;
			case R.id.newpass_rand:
				targetId = R.id.newpass;
				break;
			default:
				return;
			}
			((EditText) v.getRootView().findViewById(targetId)).setText(CharBuffer.wrap(gen.genRandPass(4*getPwLengthPref())));
		}
	}
	
	// get the user's preferred password length
	private int getPwLengthPref() {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pwlength","3"));
	}

	// THREADING STUFF
	// a watchdog timer that will clear secrets if we idle
	private class SecretTimeout extends Thread {
		private volatile boolean quitTimer = false;
		public void signalQuit() { quitTimer = true; }

		public void run() {
			while(true) {
				try {
					Thread.sleep(BatsPassMain.dlgTimeout);
				} catch (InterruptedException ex) {
					if (quitTimer) {
						quitTimer = false;
						return;
					} else {
						continue;
					}
				}

				break;
			}

			BatsPassMain.this.uiHandler.obtainMessage(0,0,0).sendToTarget();
			return;
		}
	}

	// load the password dictionary in a separate thread
	private class DictionaryLoader extends Thread {
		public void run() {
			BatsPassGenDict.getInstance(BatsPassMain.this);
			BatsPassMain.this.uiHandler.obtainMessage(1,0,0).sendToTarget();
		}	
	}

	// a thread handler for messages from our babies
	private class QuitHandler extends Handler {
		public QuitHandler (Looper l) { super(l); }

		public void handleMessage (Message m) {
			switch (m.what){ 
			case 0:
				clearSecrets();
				sTimeout = new SecretTimeout();
				sTimeout.start();
				break;
			case 1:
				populateGen();
				break;
			}
		}
	}

	// DIALOG HANDLING
	// yes, I am aware that the Fragment manager "should" handle
	// this for us. But we don't want to have to parcel up the
	// password data and leak it all over hell, and any other
	// way of getting shit into the Fragments without leaking
	// references to the batsPassMain class is *really*
	// cumbersome. So we do it manually instead.
	void showDialog(Dialog d) {
		clearDialog();
		activeDialog = new WeakReference<Dialog>(d);
		d.show();
	}

	private void clearDialog() {
		if (null != activeDialog) {
			final Dialog dlg = activeDialog.get();

			if ( (null != dlg) && (dlg.isShowing()) ) {
				dlg.cancel();
			}

			activeDialog.clear();		
		}
	}

	// MISCELLANEOUS FUNCTIONS	
	// yes, strcopy. We are using char[] here to
	// try to control propagation of pw data in memory
	private void strcopy(char[] to, char[] from, int toOffset) {
		for (int i=0; i<from.length; i++) {
			to[toOffset + i] = from[i];
		}
	}

	// TEXTWATCHER METHODS
	public void afterTextChanged(Editable arg0) { sTimeout.interrupt(); }
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }	
}
