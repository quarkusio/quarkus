/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jboss.resteasy.reactive.common.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.regex.Pattern;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Utilities for dealing with URLs
 *
 * @author Stuart Douglas
 * @author Andre Schaefer
 */
public class URLUtils {

    private static final char PATH_SEPARATOR = '/';

    private static final QueryStringParser QUERY_STRING_PARSER = new QueryStringParser('&', false) {
        @Override
        void handle(MultivaluedMap<String, String> exchange, String key, String value) {
            exchange.add(key, value);
        }
    };
    private static final QueryStringParser PATH_PARAM_PARSER = new QueryStringParser(';', true) {
        @Override
        void handle(MultivaluedMap<String, String> exchange, String key, String value) {
            exchange.add(key, value);
        }
    };

    // RFC-3986 (URI Generic Syntax) states:
    // URI         = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
    // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
    // "The scheme and path components are required, though the path may be empty (no characters)."
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+-.]*:.*");

    private URLUtils() {

    }

    public static void parseQueryString(final String string, final MultivaluedMap<String, String> exchange,
            final Charset charset, final boolean doDecode, int maxParameters) {
        QUERY_STRING_PARSER.parse(string, exchange, charset, doDecode, maxParameters);
    }

    @Deprecated
    public static void parsePathParms(final String string, final MultivaluedMap<String, String> exchange, final Charset charset,
            final boolean doDecode, int maxParameters) {
        parsePathParams(string, exchange, charset, doDecode, maxParameters);
    }

    public static int parsePathParams(final String string, final MultivaluedMap<String, String> exchange, final Charset charset,
            final boolean doDecode, int maxParameters) {
        return PATH_PARAM_PARSER.parse(string, exchange, charset, doDecode, maxParameters);
    }

    /**
     * Decodes a URL. If the decoding fails for any reason then an IllegalArgumentException will be thrown.
     *
     * @param s The string to decode
     * @param enc The encoding
     * @param decodeSlash If slash characters should be decoded
     * @param buffer The string builder to use as a buffer.
     * @return The decoded URL
     */
    public static String decode(String s, Charset enc, boolean decodeSlash, StringBuilder buffer) {
        return decode(s, enc, decodeSlash, true, buffer);
    }

    /**
     * Decodes a URL. If the decoding fails for any reason then an IllegalArgumentException will be thrown.
     *
     * @param s The string to decode
     * @param enc The encoding
     * @param decodeSlash If slash characters should be decoded
     * @param buffer The string builder to use as a buffer.
     * @return The decoded URL
     */
    public static String decode(String s, Charset enc, boolean decodeSlash, boolean formEncoding, StringBuilder buffer) {
        if (buffer != null) {
            buffer.setLength(0);
        }
        boolean needToChange = false;
        int numChars = s.length();
        int i = 0;
        while (i < numChars) {
            char c = s.charAt(i);
            if (c == '+') {
                if (formEncoding) {
                    if (!needToChange) {
                        if (buffer == null) {
                            buffer = new StringBuilder();
                        }
                        buffer.append(s, 0, i);
                    }
                    buffer.append(' ');
                    i++;
                } else {
                    i++;
                    if (needToChange) {
                        buffer.append(c);
                    }
                }
            } else if (c == '%' || c > 127) {
                if (!needToChange) {
                    if (buffer == null) {
                        buffer = new StringBuilder();
                    }
                    buffer.append(s, 0, i);
                    needToChange = true;
                }
                /*
                 * Starting with this instance of a character
                 * that needs to be encoded, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 *
                 * Note that we need to decode the whole rest of the value, we can't just decode
                 * three characters. For multi code point characters there if the code point can be
                 * represented as an alphanumeric
                 */
                try {
                    // guess the size of the remaining bytes
                    // of remaining bytes
                    // this works for percent encoded characters,
                    // not so much for unencoded bytes
                    byte[] bytes = new byte[numChars - i + 1];

                    int pos = 0;

                    while ((i < numChars)) {
                        if (c == '%') {
                            // we need 2 more characters to decode the % construct
                            if ((i + 2) >= s.length()) {
                                throw failedToDecodeURL(s, enc, null);
                            }
                            char p1 = Character.toLowerCase(s.charAt(i + 1));
                            char p2 = Character.toLowerCase(s.charAt(i + 2));
                            if (!decodeSlash && ((p1 == '2' && p2 == 'f') || (p1 == '5' && p2 == 'c'))) {
                                if (pos + 2 >= bytes.length) {
                                    bytes = expandBytes(bytes);
                                }
                                bytes[pos++] = (byte) c;
                                // should be copied with preserved upper/lower case
                                bytes[pos++] = (byte) s.charAt(i + 1);
                                bytes[pos++] = (byte) s.charAt(i + 2);
                                i += 3;

                                if (i < numChars) {
                                    c = s.charAt(i);
                                }
                                continue;
                            }
                            int v = 0;
                            if (p1 >= '0' && p1 <= '9') {
                                v = (p1 - '0') << 4;
                            } else if (p1 >= 'a' && p1 <= 'f') {
                                v = (p1 - 'a' + 10) << 4;
                            } else {
                                throw failedToDecodeURL(s, enc, null);
                            }
                            if (p2 >= '0' && p2 <= '9') {
                                v += (p2 - '0');
                            } else if (p2 >= 'a' && p2 <= 'f') {
                                v += (p2 - 'a' + 10);
                            } else {
                                throw failedToDecodeURL(s, enc, null);
                            }
                            if (v < 0) {
                                throw failedToDecodeURL(s, enc, null);
                            }

                            if (pos == bytes.length) {
                                bytes = expandBytes(bytes);
                            }
                            bytes[pos++] = (byte) v;
                            i += 3;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        } else if (c == '+' && formEncoding) {
                            if (pos == bytes.length) {
                                bytes = expandBytes(bytes);
                            }
                            bytes[pos++] = (byte) ' ';
                            ++i;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        } else {
                            if (pos == bytes.length) {
                                bytes = expandBytes(bytes);
                            }
                            ++i;
                            if (c >> 8 != 0) {
                                bytes[pos++] = (byte) (c >> 8);
                                if (pos == bytes.length) {
                                    bytes = expandBytes(bytes);
                                }
                                bytes[pos++] = (byte) c;
                            } else {
                                bytes[pos++] = (byte) c;
                                if (i < numChars) {
                                    c = s.charAt(i);
                                }
                            }

                        }
                    }

                    String decoded = new String(bytes, 0, pos, enc);
                    buffer.append(decoded);
                } catch (NumberFormatException e) {
                    throw failedToDecodeURL(s, enc, e);
                }
                break;
            } else {
                i++;
            }
        }

        return (needToChange ? buffer.toString() : s);
    }

    private static RuntimeException failedToDecodeURL(String s, Charset enc, Throwable o) {
        return new RuntimeException("Failed to decode URL " + s + " to " + enc, o);
    }

    private static byte[] expandBytes(byte[] bytes) {
        byte[] newBytes = new byte[bytes.length + 10];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        return newBytes;
    }

    /**
     * Extract path param names out of a JAX-RS path. Does not detect errors, but should
     * be fast and correct if the input is correct.
     */
    public static void parsePathParameters(String path, Set<String> pathParameters) {
        if (path == null || path.isEmpty() || path.indexOf('{') == -1)
            return;
        int len = path.length();
        int open = 0;
        int startName = -1;
        int endName = -1;
        for (int i = 0; i < len; i++) {
            switch (path.charAt(i)) {
                case '{':
                    if (open == 0) {
                        startName = i + 1;
                        endName = -1;
                    }
                    open++;
                    break;
                case '}':
                    // ignore extra closing brackets
                    if (open > 0) {
                        open--;
                        if (open == 0) {
                            // mark the end, unless we already marked it due to regex
                            if (endName == -1)
                                endName = i;
                            String pathParam = path.substring(startName, endName);
                            pathParameters.add(pathParam);
                        }
                    }
                    break;
                case ':':
                    // mark the end of regex path param name
                    if (open == 1 && endName == -1)
                        endName = i;
                    break;
            }
        }
    }

    private abstract static class QueryStringParser {

        private final char separator;
        private final boolean parseUntilSeparator;

        QueryStringParser(final char separator, final boolean parseUntilSeparator) {
            this.separator = separator;
            this.parseUntilSeparator = parseUntilSeparator;
        }

        int parse(final String string, final MultivaluedMap<String, String> exchange, final Charset charset,
                final boolean doDecode, int max) {
            int count = 0;
            int i = 0;
            try {
                int stringStart = 0;
                String attrName = null;
                for (i = 0; i < string.length(); ++i) {
                    char c = string.charAt(i);
                    if (c == '=' && attrName == null) {
                        attrName = string.substring(stringStart, i);
                        stringStart = i + 1;
                    } else if (c == separator) {
                        if (attrName != null) {
                            handle(exchange, decode(charset, attrName, doDecode),
                                    decode(charset, string.substring(stringStart, i), doDecode));
                            if (++count > max) {
                                throw tooManyParameters(max);
                            }
                        } else {
                            handle(exchange, decode(charset, string.substring(stringStart, i), doDecode), "");
                            if (++count > max) {
                                throw tooManyParameters(max);
                            }
                        }
                        stringStart = i + 1;
                        attrName = null;
                    } else if (parseUntilSeparator && (c == '?' || c == '/')) {
                        break;
                    }
                }
                if (attrName != null) {
                    handle(exchange, decode(charset, attrName, doDecode),
                            decode(charset, string.substring(stringStart, i), doDecode));
                    if (++count > max) {
                        throw tooManyParameters(max);
                    }
                } else if (string.length() != stringStart) {
                    handle(exchange, decode(charset, string.substring(stringStart, i), doDecode), "");
                    if (++count > max) {
                        throw tooManyParameters(max);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return i;
        }

        private RuntimeException tooManyParameters(int max) {
            return new RuntimeException("Too many parameters: max=" + max);
        }

        private String decode(Charset charset, String attrName, final boolean doDecode) throws UnsupportedEncodingException {
            if (doDecode) {
                return URLUtils.decode(attrName, charset, true, true, new StringBuilder());
            }
            return attrName;
        }

        abstract void handle(final MultivaluedMap<String, String> exchange, final String key, final String value);
    }

    /**
     * Adds a '/' prefix to the beginning of a path if one isn't present
     * and removes trailing slashes if any are present.
     *
     * @param path the path to normalize
     * @return a normalized (with respect to slashes) result
     */
    public static String normalizeSlashes(final String path) {
        // prepare
        final StringBuilder builder = new StringBuilder(path);
        boolean modified = false;

        // remove all trailing '/'s except the first one
        while (builder.length() > 0 && builder.length() != 1 && PATH_SEPARATOR == builder.charAt(builder.length() - 1)) {
            builder.deleteCharAt(builder.length() - 1);
            modified = true;
        }

        // add a slash at the beginning if one isn't present
        if (builder.length() == 0 || PATH_SEPARATOR != builder.charAt(0)) {
            builder.insert(0, PATH_SEPARATOR);
            modified = true;
        }

        // only create string when it was modified
        if (modified) {
            return builder.toString();
        }

        return path;
    }

    /**
     * Test if provided location is an absolute URI or not.
     *
     * @param location location to check, null = relative, having scheme = absolute
     * @return true if location is considered absolute
     */
    public static boolean isAbsoluteUrl(String location) {
        if (location != null && location.length() > 0 && location.contains(":")) {
            // consider it absolute URL if location contains valid scheme part
            return SCHEME_PATTERN.matcher(location).matches();
        }
        return false;
    }
}
