package org.jboss.resteasy.reactive.common.util;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public final class URIDecoder {

    private URIDecoder() {
        throw new RuntimeException("Static Class");
    }

    /**
     * Decodes a segment of an URI encoded by a browser.
     *
     * The string is expected to be encoded as per RFC 3986, Section 2. This is the encoding used by JavaScript functions
     * encodeURI and encodeURIComponent, but not escape. For example in this encoding, é (in Unicode U+00E9 or in
     * UTF-8 0xC3 0xA9) is encoded as %C3%A9 or %c3%a9.
     *
     * Plus signs '+' will be handled as spaces and encoded using the default JDK URLEncoder class.
     *
     * @param s string to decode
     *
     * @return decoded string
     */
    public static String decodeURIComponent(String s) {
        return decodeURIComponent(s, true);
    }

    /**
     * Decodes a segment of an URI encoded by a browser.
     *
     * The string is expected to be encoded as per RFC 3986, Section 2. This is the encoding used by JavaScript functions
     * encodeURI and encodeURIComponent, but not escape. For example in this encoding, é (in Unicode U+00E9 or in
     * UTF-8 0xC3 0xA9) is encoded as %C3%A9 or %c3%a9.
     *
     * @param s string to decode
     * @param plus weather or not to transform plus signs into spaces
     *
     * @return decoded string
     */
    public static String decodeURIComponent(String s, boolean plus) {
        if (s == null) {
            return null;
        }

        final int size = s.length();
        boolean modified = false;
        int i;
        for (i = 0; i < size; i++) {
            final char c = s.charAt(i);
            if (c == '%' || (plus && c == '+')) {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return s;
        }
        final byte[] buf = s.getBytes(StandardCharsets.UTF_8);
        int pos = i; // position in `buf'.
        for (; i < size; i++) {
            char c = s.charAt(i);
            if (c == '%') {
                if (i == size - 1) {
                    throw new IllegalArgumentException("unterminated escape"
                            + " sequence at end of string: " + s);
                }
                c = s.charAt(++i);
                if (c == '%') {
                    buf[pos++] = '%'; // "%%" -> "%"
                    break;
                }
                if (i >= size - 1) {
                    throw new IllegalArgumentException("partial escape"
                            + " sequence at end of string: " + s);
                }
                c = decodeHexNibble(c);
                final char c2 = decodeHexNibble(s.charAt(++i));
                if (c == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "invalid escape sequence `%" + s.charAt(i - 1)
                                    + s.charAt(i) + "' at index " + (i - 2)
                                    + " of: " + s);
                }
                c = (char) (c * 16 + c2);
                // shouldn't check for plus since it would be a double decoding
                buf[pos++] = (byte) c;
            } else {
                buf[pos++] = (byte) (plus && c == '+' ? ' ' : c);
            }
        }
        return new String(buf, 0, pos, StandardCharsets.UTF_8);
    }

    /**
     * Helper to decode half of a hexadecimal number from a string.
     * 
     * @param c The ASCII character of the hexadecimal number to decode.
     *        Must be in the range {@code [0-9a-fA-F]}.
     * @return The hexadecimal value represented in the ASCII character
     *         given, or {@link Character#MAX_VALUE} if the character is invalid.
     */
    private static char decodeHexNibble(final char c) {
        if ('0' <= c && c <= '9') {
            return (char) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (char) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (char) (c - 'A' + 10);
        } else {
            return Character.MAX_VALUE;
        }
    }
}
