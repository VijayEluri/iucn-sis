/*
 * Dedicated to the public domain by the author, Rob Heittman,
 * Solertium Corporation, December 2007
 * 
 * http://creativecommons.org/licenses/publicdomain/
 */

package org.gogoego.util.text;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Optimized public-domain implementation of a Java alphanumeric sort.
 * <p>
 * 
 * This implementation uses a single comparison pass over the characters in a
 * CharSequence, and returns as soon as a differing character is found, unless
 * the difference occurs in a series of numeric characters, in which case that
 * series is followed to its end. Numeric series of equal length are compared
 * numerically, that is, according to the most significant (leftmost) differing
 * digit. Series of unequal length are compared by their length.
 * <p>
 * 
 * This implementation appears to be 2-5 times faster than alphanumeric
 * comparators based based on substring analysis, with a lighter memory
 * footprint.
 * <p>
 * 
 * This alphanumeric comparator has approximately 20%-50% the performance of the
 * lexical String.compareTo() operation. Character sequences without numeric
 * data are compared more quickly.
 * <p>
 * 
 * @author <a href="mailto:rob.heittman@solertium.com">Rob Heittman</a>, <a
 *         href="http://www.solertium.com">Solertium Corporation</a>
 */
public class AlphanumericComparator implements Comparator<CharSequence>,
		Serializable {
	private static final long serialVersionUID = 1L;

	public int compare(final CharSequence l, final CharSequence r) {
		int ptr = 0;
		int msd = 0;
		int diff = 0;
		char a, b;

		final int llength = l.length();
		final int rlength = r.length();
		final int min;

		if (rlength < llength)
			min = rlength;
		else
			min = llength;

		boolean rAtEnd, rHasNoMoreDigits;

		while (ptr < min) {
			a = l.charAt(ptr);
			b = r.charAt(ptr);
			diff = a - b;
			if ((a > '9') || (b > '9') || (a < '0') || (b < '0')) {
				if (diff != 0)
					return diff;
				msd = 0;
			} else {
				if (msd == 0)
					msd = diff;
				rAtEnd = rlength - ptr < 2;
				if (llength - ptr < 2) {
					if (rAtEnd)
						return msd;
					if(!isNotDigit(a) && !isNotDigit(b))
						return diff;
					else
						return -1;
				}
				if (rAtEnd)
					if(!isNotDigit(a) && !isNotDigit(b))
						return diff;
					else
						return -1;
				rHasNoMoreDigits = isNotDigit(r.charAt(ptr + 1));
				if (isNotDigit(l.charAt(ptr + 1))) {
					if (rHasNoMoreDigits && (msd != 0))
						return msd;
					if (!rHasNoMoreDigits)
						return -1;
				} else if (rHasNoMoreDigits)
					return 1;
			}
			ptr++;
		}
		return llength - rlength;
	}

	boolean isNotDigit(final char x) {
		return (x > '9') || (x < '0');
	}

}
