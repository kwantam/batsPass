package org.jfet.batsPass;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import android.content.Context;

public class BatsPassGenDict {
    private static BatsPassGenDict instance = null;
    private final List<String> dict;

    private BatsPassGenDict(Context c) throws IOException {
        final Scanner s = new Scanner(new GZIPInputStream(new BufferedInputStream(c.getResources().openRawResource(R.raw.dict))));
        this.dict = new ArrayList<String>(31000);

        while (s.hasNext()) {
            dict.add(s.next());
        }
        s.close();
    }

    public String get(int i) {
        return dict.get(i);
    }

    public int size() {
        return dict.size();
    }

    public static BatsPassGenDict getInstance(Context c) {
        if (null == instance) {
            try {
                instance = new BatsPassGenDict(c);
            } catch (IOException ex) {
                instance = null;
            }
        }
        return instance;
    }
}
