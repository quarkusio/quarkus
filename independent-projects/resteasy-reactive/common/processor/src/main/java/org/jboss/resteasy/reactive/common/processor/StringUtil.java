package org.jboss.resteasy.reactive.common.processor;

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

public class StringUtil {

    public static Iterator<String> camelHumpsIterator(final String str) {
        return new Iterator<String>() {
            int idx;

            public boolean hasNext() {
                return idx < str.length();
            }

            public String next() {
                if (idx == str.length())
                    throw new NoSuchElementException();
                // known mixed-case rule-breakers
                if (str.startsWith("JBoss", idx)) {
                    idx += 5;
                    return "JBoss";
                }
                final int start = idx;
                int c = str.codePointAt(idx);
                if (Character.isUpperCase(c)) {
                    // an uppercase-starting word
                    idx = str.offsetByCodePoints(idx, 1);
                    if (idx < str.length()) {
                        c = str.codePointAt(idx);
                        if (Character.isUpperCase(c)) {
                            // all-caps word; need one look-ahead
                            int nextIdx = str.offsetByCodePoints(idx, 1);
                            while (nextIdx < str.length()) {
                                c = str.codePointAt(nextIdx);
                                if (Character.isLowerCase(c)) {
                                    // ended at idx
                                    return str.substring(start, idx);
                                }
                                idx = nextIdx;
                                nextIdx = str.offsetByCodePoints(idx, 1);
                            }
                            // consumed the whole remainder, update idx to length
                            idx = str.length();
                            return str.substring(start);
                        } else {
                            // initial caps, trailing lowercase
                            idx = str.offsetByCodePoints(idx, 1);
                            while (idx < str.length()) {
                                c = str.codePointAt(idx);
                                if (Character.isUpperCase(c)) {
                                    // end
                                    return str.substring(start, idx);
                                }
                                idx = str.offsetByCodePoints(idx, 1);
                            }
                            // consumed the whole remainder
                            return str.substring(start);
                        }
                    } else {
                        // one-letter word
                        return str.substring(start);
                    }
                } else {
                    // a lowercase-starting word
                    idx = str.offsetByCodePoints(idx, 1);
                    while (idx < str.length()) {
                        c = str.codePointAt(idx);
                        if (Character.isUpperCase(c)) {
                            // end
                            return str.substring(start, idx);
                        }
                        idx = str.offsetByCodePoints(idx, 1);
                    }
                    // consumed the whole remainder
                    return str.substring(start);
                }
            }
        };
    }

    public static String hyphenate(final String orig) {
        return String.join("-", (Iterable<String>) () -> camelHumpsIterator(orig));
    }

    /**
     * Hyphenates every part of the original string, but also converts the first letter of every part into a capital
     * letter
     */
    public static String hyphenateWithCapitalFirstLetter(final String orig) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        Iterator<String> it = StringUtil.camelHumpsIterator(orig);
        while (it.hasNext()) {
            String part = it.next();
            part = part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1);
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("-");
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
