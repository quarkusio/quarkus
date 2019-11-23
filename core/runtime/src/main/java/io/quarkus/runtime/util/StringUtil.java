package io.quarkus.runtime.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 */
public final class StringUtil {
    private StringUtil() {
    }

    public static Iterator<String> camelHumpsIterator(String str) {
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

    public static Iterator<String> lowerCase(Iterator<String> orig) {
        return new Iterator<String>() {
            public boolean hasNext() {
                return orig.hasNext();
            }

            public String next() {
                return orig.next().toLowerCase(Locale.ROOT);
            }
        };
    }

    /**
     * @deprecated Use {@link String#join} instead.
     * @param delim delimiter
     * @param it iterator
     * @return the joined string
     */
    @Deprecated
    public static String join(String delim, Iterator<String> it) {
        final StringBuilder b = new StringBuilder();
        if (it.hasNext()) {
            b.append(it.next());
            while (it.hasNext()) {
                b.append(delim);
                b.append(it.next());
            }
        }
        return b.toString();
    }

    public static String join(Iterator<String> it) {
        final StringBuilder b = new StringBuilder();
        if (it.hasNext()) {
            b.append(it.next());
            while (it.hasNext()) {
                b.append(it.next());
            }
        }
        return b.toString();
    }

    public static Iterator<String> lowerCaseFirst(Iterator<String> orig) {
        return new Iterator<String>() {
            boolean first = true;

            public boolean hasNext() {
                return orig.hasNext();
            }

            public String next() {
                final String next = orig.next();
                if (first) {
                    first = false;
                    return next.toLowerCase(Locale.ROOT);
                } else {
                    return next;
                }
            }
        };
    }

    public static Iterator<String> withoutSuffix(Iterator<String> orig, String... suffixes) {
        return new Iterator<String>() {
            String next = null;

            public boolean hasNext() {
                if (next == null) {
                    if (!orig.hasNext())
                        return false;
                    final String next = orig.next();
                    if (!orig.hasNext() && arrayContains(next, suffixes)) {
                        return false;
                    }
                    this.next = next;
                }
                return true;
            }

            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                final String next = this.next;
                this.next = null;
                return next;
            }
        };
    }

    @SafeVarargs
    public static <T> List<T> withoutSuffix(List<T> list, T... segments) {
        if (list.size() < segments.length) {
            return list;
        }
        for (int i = 0; i < segments.length; i++) {
            if (!list.get(list.size() - i - 1).equals(segments[segments.length - i - 1])) {
                return list;
            }
        }
        return list.subList(0, list.size() - segments.length);
    }

    public static List<String> toList(Iterator<String> orig) {
        return toList(orig, 0);
    }

    private static List<String> toList(Iterator<String> orig, int idx) {
        if (orig.hasNext()) {
            final String item = orig.next();
            final List<String> list = toList(orig, idx + 1);
            list.set(idx, item);
            return list;
        } else {
            return Arrays.asList(new String[idx]);
        }
    }

    @SafeVarargs
    private static <T> boolean arrayContains(final T item, final T... array) {
        for (T arrayItem : array) {
            if (Objects.equals(arrayItem, item))
                return true;
        }
        return false;
    }

    public static String hyphenate(String orig) {
        return join("-", lowerCase(camelHumpsIterator(orig)));
    }
}
