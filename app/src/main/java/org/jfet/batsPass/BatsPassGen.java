package org.jfet.batsPass;

import android.annotation.SuppressLint;

import java.security.SecureRandom;
import java.util.Random;

public class BatsPassGen {

    final private BatsPassGenDict dict;
    final private SecureRandom rand;
    final static char[] symbols = new char[]{'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~'};

    @SuppressLint("TrulyRandom")
    public BatsPassGen (BatsPassGenDict dict) {
        this.dict = dict;
        this.rand = new SecureRandom();
    }

    public char[] genDictPass(final int nWords) {
        if (0 == nWords) { return new char[0]; }

        if (null == dict) {
            // we don't have a dictionary present, so we have to do random strings instead
            return genRandPass(nWords * 4);
        } else {
            // a password of nWords random words from the dictionary
            final StringBuffer b = new StringBuffer();
            for (int i=0; i<nWords; i++) {
                b.append(dict.get((int) (((double) dict.size()) * rand.nextDouble())));
            }

            final int gpLen = b.length() + nWords + 1;
            final char[] gPass = new char[gpLen];
            for (int i=0; i<=nWords; i++) {
                int rIdx;
                do { 
                    rIdx = (int) (rand.nextDouble() * (double) gpLen);
                } while (gPass[rIdx] != '\0');

                gPass[rIdx] = symbols[(int) (rand.nextDouble() * (double) symbols.length)];
            }

            // go through the buffer, inserting the punctuation etc at the randomly-selected places
            int skipNum = 0;
            for (int i=0; i<gPass.length; i++) {
                if (gPass[i] == '\0') {
                    gPass[i] = b.charAt(i - skipNum);

                    if (rand.nextDouble() < 0.5) {
                        gPass[i] = Character.toUpperCase(gPass[i]);
                    }
                } else {
                    skipNum++;
                }
            }

            return gPass;
        }
    }

    public char[] genRandPass(int nChars) {
        final char[] gPass = new char[nChars];

        for (int i=0; i<nChars; i++) {
            gPass[i] = Character.toChars(33 + (int) (rand.nextDouble() * 94.0))[0];
        }

        return gPass;
    }

    static public String genRandPath(Random rand) {
        final StringBuilder s = new StringBuilder();
        for (int i=0; i<32; i++) {
            s.append(Character.toChars(65 + (int) (rand.nextDouble() * 26.0) + (rand.nextDouble() < 0.5 ? 0 : 32)));
        }

        return s.toString();
    }
}
