package org.jfet.batsPass;

import android.annotation.SuppressLint;
import java.security.SecureRandom;

public class BatsPassGen {
	final private BatsPassGenDict dict;
	final private SecureRandom rand;
	final static char[] symbols = new char[]{' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~'};

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

			int gpLen = b.length() + nWords;
			final char[] gPass = new char[gpLen];
			final int[] nSkips = new int[nWords];
			for (int i=0; i<nWords-1; i++) {
				nSkips[i] = (int) (rand.nextDouble() * (double) gpLen * ((double) (2 + i) / (double) nWords));
				gpLen -= nSkips[i];
			}
			nSkips[nWords-1] = gpLen;
			
			// go through the buffer, inserting the punctuation etc at the randomly-selected places
			int skipNum = 0;
			int thisSkip = nSkips[0];
			for (int i=0; i<gPass.length; i++) {
				if ( (0 == thisSkip) || (i - skipNum >= b.length()) ) {
					if (++skipNum == nSkips.length) {
						thisSkip = -1;
					} else {
						thisSkip = nSkips[skipNum];
					}
					gPass[i] = symbols[(int) (rand.nextDouble() * (double) symbols.length)];
				} else {
					thisSkip--;
					gPass[i] = b.charAt(i - skipNum);
					if (rand.nextDouble() < 0.5) {
						gPass[i] = Character.toUpperCase(gPass[i]);
					}
				}
			}

			return gPass;
		}
	}

	public char[] genRandPass(int nChars) {
		final char[] gPass = new char[nChars];

		for (int i=0; i<nChars; i++) {
			gPass[i] = Character.toChars(32 + (int) (rand.nextDouble() * 95.0))[0];
		}
		
		return gPass;
	}
}