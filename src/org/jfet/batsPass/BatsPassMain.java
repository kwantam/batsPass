package org.jfet.batsPass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.CharArrayBuffer;
import android.net.Uri;
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

	private File databaseFile;
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
		
		// if we are being started but somehow another instance is running, kill that instance
		if ( (null != bpMain) && (null != bpMain.get()) && (!this.equals(bpMain.get())) ) {
			bpMain.get().finish();
		}

		bpMain = new WeakReference<BatsPassMain>(this);
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// handler for messages from other threads
		uiHandler = new QuitHandler(Looper.getMainLooper());
		// preload the dictionary in another thread
		new DictionaryLoader().start();

		// load SQLCipher libraries
		SQLiteDatabase.loadLibs(this);
		databaseFile = getDatabasePath("password.db");
		clearSecrets(); // shows the main window, too
	}
	
	// new intent - since we're running in singleTask mode
	public void onNewIntent(Intent i) {
		super.onNewIntent(i);
		setIntent(i);
	}
	
	// start a timeout when we become visible
	public void onResume() {
		super.onResume();
		startTimer();
		clearSecrets();
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
		if (bpMain != null) {
			bpMain.clear();
			bpMain = null;
		}
	}

	// intercept touch events and reset the watchdog timer
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (null != sTimeout) {
			sTimeout.interrupt();
		}
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
		case R.id.action_delete:
			dbDelete();
			return true;
		case R.id.action_export:
			dbExport();
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

		Cursor c = null;
		try {
			c = passDB.query(DB_NAME,new String[]{ID_KEY,SERVICE_KEY},null,null,null,null,null,null);
		} catch (SQLiteException ex) {
			c = null;
		}

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
		
		if ( (! databaseFile.exists()) || ((null != getIntent()) && (null != getIntent().getData())) ) {
			dbCreate();
		}
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
		final char[] thePass = BatsKeyDialog.getEditableCharValue(((EditText) findViewById(R.id.password)).getText());

		if (thePass.length < 1) {
			return;
		}

		// try to open the database; figure out if the password is correct
		try {
			passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),thePass,null);
		} catch (SQLiteException ex) {
			passDB = null;
			((EditText) findViewById(R.id.password)).setHint(R.string.wrong_password);
			return;	// yes, finally block is still executed
		} finally {
			Arrays.fill(thePass, 'Z');
		}

		showPassList();
	}
	
	private void dbCreate() {
		if (databaseFile.exists()) {
			dbImport();
			return;
		}

		// if we were called with a file intent, try to copy that database, else make a new one
		if (! dbCopy(databaseFile)) {
			showDialog(new BatsKeyDialog(this,true));
		} else {
			getIntent().setData(null);
		}
	}
	
	void dbDoCreate(char[] pass) {
		boolean tryAgain = false;
		if (! databaseFile.exists()) {
			databaseFile.getParentFile().mkdirs();
			try {
				// create db
				passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),pass,null);
			} catch (SQLiteException ex) {
				tryAgain = true;
			}
			
			if (! tryAgain) {
				try {
					// create initial table
					passDB.execSQL("CREATE TABLE "+ DB_NAME + 
							" ( " + ID_KEY + " integer primary key autoincrement not null, " +
							SERVICE_KEY + " string not null, " +
							UID_KEY + " string, " +
							PASS_KEY + " string not null ); ");
					
					// insert one test entry
					final ContentValues initContent = new ContentValues();
					initContent.put(SERVICE_KEY,"test");
					initContent.put(UID_KEY,"quux");
					initContent.put(PASS_KEY,"bazbar");
					passDB.insert(DB_NAME, null, initContent);
				} catch (SQLiteException ex) { }
			}
		}

		Arrays.fill(pass, 'Z');
		clearSecrets();
		if (tryAgain) {
			dbCreate();
		}
		return;
	}
	
	private void dbImport() {
		// if we already imported this db, don't do it again
		if (null == getIntent().getData()) {
			return;
		}
		
		// create temporary database file
		final File tmpFile = getDatabasePath("temp.db");
		if (tmpFile.exists()) { tmpFile.delete(); }
		if (! dbCopy(tmpFile)) {
			tmpFile.delete();
			return;
		}
		
		// OK, so there was an intent, *and* it was successfully copied into temp.db
		// get the passwords and then we'll do the import
		showDialog(new BatsImportDialog(this));
	}
	
	void dbDoImport(char[] srcPass, char[] dstPass) {
		// we have temp.db; open both databases and off we go
		SQLiteDatabase tmpDB = null;
		Cursor c = null;
		boolean retry = false;
		try {
			tmpDB = SQLiteDatabase.openOrCreateDatabase(getDatabasePath("temp.db").getPath(), srcPass, null);
			passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(), dstPass, null);
			
			c = tmpDB.query(DB_NAME,new String[]{SERVICE_KEY,UID_KEY,PASS_KEY},null,null,null,null,null,null);	
			if ( (null != c)  && (0 != c.getCount()) ) {
				c.moveToFirst();
				do {
					final String srv = c.getString(0);
					final String uid = c.getString(1);
					final String pw = c.getString(2);
					final Cursor c2 = passDB.query(DB_NAME, new String[]{ID_KEY}, 
							String.format("%s='%s' AND %s='%s' AND %s='%s'",
									      SERVICE_KEY,srv.replaceAll("'", "''"),
									      UID_KEY,uid.replaceAll("'", "''"),
									      PASS_KEY,pw.replaceAll("'", "''")),
						    null, null, null, null, null);

					if ( (null == c2) || (0 == c2.getCount()) ) {
						final ContentValues cVals = new ContentValues();
						cVals.put(SERVICE_KEY,srv);
						cVals.put(UID_KEY, uid);
						cVals.put(PASS_KEY, pw);
						passDB.insert(DB_NAME, null, cVals);
					}
					
					if (null != c2) {
						c2.close();
					}
					c.moveToNext();
				} while (! c.isAfterLast());
			}
		} catch (SQLiteException ex) {
			retry = true;
		} finally {
			if ( (null != tmpDB) && (tmpDB.isOpen()) ) {
				tmpDB.close();
			}
			if (null != c) {
				c.close();
			}
			Arrays.fill(srcPass, 'Z');
			Arrays.fill(dstPass, 'Z');
			getDatabasePath("temp.db").delete();
			clearSecrets();
			if (retry) {
				dbImport();
				return;
			}
		}

		// if we got here, we were successful
		getIntent().setData(null);
		return;
	}
	
	private boolean dbCopy(File dstFile) {
		final Intent i = getIntent();
		if ( (null == i) || (null == i.getData()) ) {
			return false;
		}

		// OK, so we 
		AssetFileDescriptor inAfd = null;
		FileInputStream inS = null;
		FileOutputStream outS = null;
		try {
			inAfd = getContentResolver().openAssetFileDescriptor(i.getData(), "r");
			inS = inAfd.createInputStream();
			dstFile.getParentFile().mkdirs();
			outS = new FileOutputStream(dstFile);
			inS.getChannel().transferTo(0, inS.getChannel().size(), outS.getChannel());
		} catch (IOException ex) {
			return false;
		} finally {
			try { inAfd.close(); }
			catch (Exception ex) {}

			try { inS.close(); }
			catch (Exception ex) {}

			try { outS.close(); }
			catch (Exception ex) {}
		}

		return true;
	}

	private void dbDelete() {
		final AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle(R.string.action_delete);
		ab.setMessage(R.string.action_delete_confirm);
		ab.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				BatsPassMain.this.dbDoDelete();
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
	
	private void dbDoDelete() {
		clearSecrets();
		databaseFile.delete();
		dbCreate();
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
		showDialog(new BatsKeyDialog(this,false));
	}

	void dbDoRekey(char[] oS, String nS) {
		if ( (null != passDB) && passDB.isOpen() ) {
			passDB.close();
		}

		try {
			passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),oS,null);
		} catch (SQLiteException ex) {
			clearSecrets();
			return;	// yes, finally block is still executed
		} finally {
			Arrays.fill(oS, 'Z');
		}

		passDB.rawExecSQL("PRAGMA rekey = '" + nS.replaceAll("'","''") + "';");
		clearSecrets();
	}
	
	private void dbExport() {
		final String fileName = BatsPassGen.genRandPath(new Random());
		final Uri u = Uri.parse("content://" + BatsPassProvider.authName + "/" + fileName + "/" + BatsPassProvider.fileName);

		final Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("application/octet-stream");
		i.putExtra(Intent.EXTRA_SUBJECT, BatsPassProvider.fileName);
		i.putExtra(Intent.EXTRA_TEXT, "Your Bats! Password database is attached.");
		i.putExtra(Intent.EXTRA_STREAM, u);
		i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		final Intent c = Intent.createChooser(i, getString(R.string.export_chooser));
		
		if (null != i.resolveActivity(getPackageManager())) {
			PreferenceManager.getDefaultSharedPreferences(this).edit().putString("EXPORT", fileName).apply();
			startActivity(c);
		}
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
	public void afterTextChanged(Editable arg0) { resetTimer(); }
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { return; }	
	
	static void resetTimer() {
		if ( (null != bpMain) && (null != bpMain.get()) && (null != bpMain.get().sTimeout) ) {
			bpMain.get().sTimeout.interrupt();
		}
	}
}