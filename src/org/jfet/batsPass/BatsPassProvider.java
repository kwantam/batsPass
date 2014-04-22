package org.jfet.batsPass;

import java.io.FileNotFoundException;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

public class BatsPassProvider extends ContentProvider {
	final static String authName = "org.jfet.batsPass.BatsPassProvider"; 
	final static String mimeType = "application/octet-stream";
	final static String fileName = "password.db";

	// we return nothing but application/octet-stream
	public String getType(Uri arg0) {
		return mimeType;
	}
	
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		final List<String> segs = uri.getPathSegments();
		if ( (! authName.equals(uri.getAuthority())) ||
			 (2 != segs.size()) ||
			 (! fileName.equals(segs.get(1))) ||
			 (! sPrefs.getString("EXPORT","").equals(segs.get(0))) ) {
			throw new FileNotFoundException("Unshared URI error.");
		}
		
		return ParcelFileDescriptor.open(getContext().getDatabasePath("password.db"),ParcelFileDescriptor.MODE_READ_ONLY);
	}

	// nothing to set up
	public boolean onCreate() {
		return true;
	}

	// we don't support database-like operations
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
		return null;
	}

	// we do not support updating
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		return 0;
	}

	// we don't support inserting
	public Uri insert(Uri arg0, ContentValues arg1) {
		return null;
	}
	
	// we do not support deleting
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}
}