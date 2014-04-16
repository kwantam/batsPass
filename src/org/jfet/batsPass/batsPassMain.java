package org.jfet.batsPass;

import java.io.File;
import java.nio.CharBuffer;
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
    final static String[] fragTags = new String[]{ "bpItemDlg", "bpItemMenu", "bpItem", "bpConfirm", "bpTopMenu" };

    private SQLiteDatabase passDB = null;
    SecretTimeout sTimeout = null;
    private Handler uiHandler = null;
    private BatsPassGen gen = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.main);

        // handler for messages from other threads
        uiHandler = new QuitHandler(Looper.getMainLooper());
        // preload the dictionary in another thread
        (new Thread() {
            public void run() {
                BatsPassGenDict.getInstance(batsPassMain.this);
                batsPassMain.this.uiHandler.obtainMessage(1,0,0).sendToTarget();
            }
        }).start();
        // timer thread
        sTimeout = new SecretTimeout();
        sTimeout.start();
    }

    // intercept touch events and reset the watchdog timer
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

    // show the list of credentials available in the database
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
                public void onClick(View v) { showPass(v, idStr); }
            } );
            b.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(final View v) {
                    final DialogFragment dlg = new BatsItemMenuFragment(batsPassMain.this, idStr, srvStr);
                    dlg.show(getFragmentManager(), fragTags[1]);

                    return true;
                }
            });
            ll.addView(b);
            c.moveToNext();
        } while (! c.isAfterLast());
        c.close();    	
    }

    // called from BatsItemMenuFragment
    void editPass(String id) {
        if (null == passDB) {
            clearSecrets();
            return;
        }

        final Cursor c = passDB.query("pass",new String[]{"service","uid","pw"},"_id="+id,null,null,null,null,null);

        if ( (null == c) || (0 == c.getCount()) ) {
            newPass(null);
        } else {
            final DialogFragment dlg = new BatsItemFragment(this,id,getCurData(c));
            dlg.show(getFragmentManager(), fragTags[2]);
        }
        return;
    }

    // called from BatsItemMenuFragment
    void deletePass(String id) {
        // TODO unimplemented
        return;
    }

    // takes View because it's a button callback
    // but we don't actually use it!
    public void newPass(View v) {
        final DialogFragment dlg = new BatsItemFragment(this);
        dlg.show(getFragmentManager(), fragTags[2]);
    }

    // callback point from BatsItemFragment to add or update
    void addEntry(ContentValues cVals) {
        passDB.insert("pass", null, cVals);
        cVals.clear();
    }

    // callback from the service list when we're asked to display
    private void showPass(View v, String id) {
        if (null == passDB) {
            clearSecrets();
            return;
        }

        final Cursor c = passDB.query("pass",new String[]{"service","uid","pw"},"_id="+id,null,null,null,null,null);

        if ( (null == c) || (0 == c.getCount()) ) {
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

        final DialogFragment dlg = new BatsPassDialogFragment(msg,sup[0].data,sTimeout);
        dlg.show(getFragmentManager(), fragTags[0]);
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

    // yes, strcopy. We are using char[] here to
    // try to control propagation of pw data in memory
    private void strcopy(char[] to, char[] from, int toOffset) {
        for (int i=0; i<from.length; i++) {
            to[toOffset + i] = from[i];
        }
    }

    // clear fragments, return to "login" screen
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

    // once we've loaded the dictionary in the background, update the gen instance
    private void populateGen() { gen = new BatsPassGen(BatsPassGenDict.getInstance(this)); }
    
    // callbacks from BatsItemFragment that use the generator
    public void fillPwDict(View v) { if (null != gen) { ((EditText) v.getRootView().findViewById(R.id.pwinput)).setText(CharBuffer.wrap(gen.genDictPass(4))); } }
    public void fillPwRand(View v) { if (null != gen) { ((EditText) v.getRootView().findViewById(R.id.pwinput)).setText(CharBuffer.wrap(gen.genRandPass(16))); } }

    // a rather harsh onPause() method - we never want this program resuming
    public void onPause() {
        super.onPause();
        sTimeout.signalQuit();
        sTimeout.interrupt();
        clearSecrets();
        finish();
    }

    // a watchdog timer that will clear secrets if we idle
    final static long dlgTimeout = 30000;
    class SecretTimeout extends Thread {
        private volatile boolean quitTimer = false;
        public void signalQuit() { quitTimer = true; }

        public void run() {
            while(true) {
                try {
                    Thread.sleep(batsPassMain.dlgTimeout);
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

            batsPassMain.this.uiHandler.obtainMessage(0,0,0).sendToTarget();
            return;
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
}