package org.jfet.batsPass;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import net.sqlcipher.database.SQLiteDatabase;

public class batsPassMain extends Activity
{
	private boolean didLoad = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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
    	final SQLiteDatabase passDB;
    	if (! databaseFile.exists()) {
    		passDB = SQLiteDatabase.openOrCreateDatabase(databaseFile.getPath(),thePass,null);
    	} else {
    	}
    }
}