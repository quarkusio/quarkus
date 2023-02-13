package org.jboss.resteasy.reactive.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class Encode {
    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private static final Pattern PARAM_REPLACEMENT = Pattern.compile("_resteasy_uri_parameter");

    private static final String[] pathEncoding = new String[128];
    private static final String[] pathSegmentEncoding = new String[128];
    private static final String[] matrixParameterEncoding = new String[128];
    private static final String[] queryNameValueEncoding = new String[128];
    private static final String[] queryStringEncoding = new String[128];

    static {
        /*
         * Encode via <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>. PCHAR is allowed allong with '/'
         *
         * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
         * sub-delims = "!" / "$" / "&" / "'" / "(" / ")"
         * / "*" / "+" / "," / ";" / "="
         * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
         *
         */
        for (int i = 0; i < 128; i++) {
            if (i >= 'a' && i <= 'z')
                continue;
            if (i >= 'A' && i <= 'Z')
                continue;
            if (i >= '0' && i <= '9')
                continue;
            switch ((char) i) {
                case '-':
                case '.':
                case '_':
                case '~':
                case '!':
                case '$':
                case '&':
                case '\'':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case '/':
                case ';':
                case '=':
                case ':':
                case '@':
                    continue;
            }
            pathEncoding[i] = encodeString(String.valueOf((char) i));
        }
        pathEncoding[' '] = "%20";
        System.arraycopy(pathEncoding, 0, matrixParameterEncoding, 0, pathEncoding.length);
        matrixParameterEncoding[';'] = "%3B";
        matrixParameterEncoding['='] = "%3D";
        matrixParameterEncoding['/'] = "%2F"; // RESTEASY-729
        System.arraycopy(pathEncoding, 0, pathSegmentEncoding, 0, pathEncoding.length);
        pathSegmentEncoding['/'] = "%2F";
        /*
         * Encode via <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
         *
         * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
         * space encoded as '+'
         *
         */
        for (int i = 0; i < 128; i++) {
            if (i >= 'a' && i <= 'z')
                continue;
            if (i >= 'A' && i <= 'Z')
                continue;
            if (i >= '0' && i <= '9')
                continue;
            switch ((char) i) {
                case '-':
                case '.':
                case '_':
                case '~':
                case '?':
                    continue;
                case ' ':
                    queryNameValueEncoding[i] = "+";
                    continue;
            }
            queryNameValueEncoding[i] = encodeString(String.valueOf((char) i));
        }

        /*
         * query = *( pchar / "/" / "?" )
         *
         */
        for (int i = 0; i < 128; i++) {
            if (i >= 'a' && i <= 'z')
                continue;
            if (i >= 'A' && i <= 'Z')
                continue;
            if (i >= '0' && i <= '9')
                continue;
            switch ((char) i) {
                case '-':
                case '.':
                case '_':
                case '~':
                case '!':
                case '$':
                case '&':
                case '\'':
                case '(':
                case ')':
                case '*':
                case '+':
                case ',':
                case ';':
                case '=':
                case ':':
                case '@':
                case '?':
                case '/':
                    continue;
                case ' ':
                    queryStringEncoding[i] = "%20";
                    continue;
            }
            queryStringEncoding[i] = encodeString(String.valueOf((char) i));
        }
    }

    /**
     * Keep encoded values "%..." and template parameters intact.
     *
     * @param value query string
     * @return encoded query string
     */
    public static String encodeQueryString(String value) {
        return encodeValue(value, queryStringEncoding);
    }

    /**
     * Keep encoded values "%...", matrix parameters, template parameters, and '/' characters intact.
     *
     * @param value path
     * @return encoded path
     */
    public static String encodePath(String value) {
        return encodeValue(value, pathEncoding);
    }

    /**
     * Keep encoded values "%...", matrix parameters and template parameters intact.
     *
     * @param value path segment
     * @return encoded path segment
     */
    public static String encodePathSegment(String value) {
        return encodeValue(value, pathSegmentEncoding);
    }

    /**
     * Keep encoded values "%..." and template parameters intact.
     *
     * @param value uri fragment
     * @return encoded uri fragment
     */
    public static String encodeFragment(String value) {
        return encodeValue(value, queryStringEncoding);
    }

    /**
     * Keep encoded values "%..." and template parameters intact.
     *
     * @param value matrix parameter
     * @return encoded matrix parameter
     */
    public static String encodeMatrixParam(String value) {
        return encodeValue(value, matrixParameterEncoding);
    }

    /**
     * Keep encoded values "%..." and template parameters intact.
     *
     * @param value query parameter
     * @return encoded query parameter
     */
    public static String encodeQueryParam(String value) {
        return encodeValue(value, queryNameValueEncoding);
    }

    //private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|$)");
    private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|[a-fA-F0-9]$|$|[a-fA-F0-9][^a-fA-F0-9])");
    private static final Pattern encodedChars = Pattern.compile("%([a-fA-F0-9][a-fA-F0-9])");
    private static final Pattern encodedCharsMulti = Pattern.compile("((%[a-fA-F0-9][a-fA-F0-9])+)");

    public static String decodePath(String path) {
        // FIXME: this doesn't appear to pass the TCK, because it fails to decode what it throws at it
        // also it doesn't decode slashes (it should) and it decodes + (it should not)
        //        return URLUtils.decode(path, StandardCharsets.UTF_8, false, null);
        // So let's use the Vertx decoder for now
        return URIDecoder.decodeURIComponent(path, false);
    }

    private static String decodeBytes(String enc, CharsetDecoder decoder) {
        Matcher matcher = encodedChars.matcher(enc);
        ByteBuffer bytes = ByteBuffer.allocate(enc.length() / 3);
        while (matcher.find()) {
            int b = Integer.parseInt(matcher.group(1), 16);
            bytes.put((byte) b);
        }
        bytes.flip();
        try {
            return decoder.decode(bytes).toString();
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encode '%' if it is not an encoding sequence
     *
     * @param string value to encode
     * @return encoded value
     */
    public static String encodeNonCodes(String string) {
        Matcher matcher = nonCodes.matcher(string);
        StringBuilder builder = new StringBuilder();

        // FYI: we do not use the no-arg matcher.find()
        //      coupled with matcher.appendReplacement()
        //      because the matched text may contain
        //      a second % and we must make sure we
        //      encode it (if necessary).
        int idx = 0;
        while (matcher.find(idx)) {
            int start = matcher.start();
            builder.append(string.substring(idx, start));
            builder.append("%25");
            idx = start + 1;
        }
        builder.append(string.substring(idx));
        return builder.toString();
    }

    public static boolean savePathParams(String segmentString, StringBuilder newSegment, List<String> params) {
        boolean foundParam = false;
        // Regular expressions can have '{' and '}' characters.  Replace them to do match
        CharSequence segment = PathHelper.replaceEnclosedCurlyBracesCS(segmentString);
        Matcher matcher = PathHelper.URI_TEMPLATE_PATTERN.matcher(segment);
        int start = 0;
        while (matcher.find()) {
            newSegment.append(segment, start, matcher.start());
            foundParam = true;
            String group = matcher.group();
            // Regular expressions can have '{' and '}' characters.  Recover earlier replacement
            params.add(PathHelper.recoverEnclosedCurlyBraces(group));
            newSegment.append("_resteasy_uri_parameter");
            start = matcher.end();
        }
        newSegment.append(segment, start, segment.length());
        return foundParam;
    }

    /**
     * Keep encoded values "%..." and template parameters intact i.e. "{x}"
     *
     * @param segment value to encode
     * @param encoding encoding
     * @return encoded value
     */
    public static String encodeValue(String segment, String[] encoding) {
        ArrayList<String> params = new ArrayList<String>();
        boolean foundParam = false;
        StringBuilder newSegment = new StringBuilder();
        if (savePathParams(segment, newSegment, params)) {
            foundParam = true;
            segment = newSegment.toString();
        }
        String result = encodeFromArray(segment, encoding, false);
        result = encodeNonCodes(result);
        segment = result;
        if (foundParam) {
            segment = pathParamReplacement(segment, params);
        }
        return segment;
    }

    /**
     * Encode via <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>. PCHAR is allowed allong with '/'
     * <p>
     * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * sub-delims = "!" / "$" / "&#x26;" / "'" / "(" / ")"
     * / "*" / "+" / "," / ";" / "="
     * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
     *
     * @param segment value to encode
     * @return encoded value
     */
    public static String encodePathAsIs(String segment) {
        return encodeFromArray(segment, pathEncoding, true);
    }

    /**
     * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
     *
     * @param segment value to encode
     * @return encoded value
     */
    public static String encodePathSaveEncodings(String segment) {
        String result = encodeFromArray(segment, pathEncoding, false);
        result = encodeNonCodes(result);
        return result;
    }

    /**
     * Encode via <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>. PCHAR is allowed allong with '/'
     * <p>
     * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * sub-delims = "!" / "$" / "&#x26;" / "'" / "(" / ")"
     * / "*" / "+" / "," / ";" / "="
     * pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
     *
     * @param segment value to encode
     * @return encoded value
     */
    public static String encodePathSegmentAsIs(String segment) {
        return encodeFromArray(segment, pathSegmentEncoding, true);
    }

    /**
     * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
     *
     * @param segment value to encode
     * @return encoded value
     */
    public static String encodePathSegmentSaveEncodings(String segment) {
        String result = encodeFromArray(segment, pathSegmentEncoding, false);
        result = encodeNonCodes(result);
        return result;
    }

    /**
     * Encodes everything of a query parameter name or value.
     *
     * @param nameOrValue value to encode
     * @return encoded value
     */
    public static String encodeQueryParamAsIs(String nameOrValue) {
        return encodeFromArray(nameOrValue, queryNameValueEncoding, true);
    }

    /**
     * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
     *
     * @param segment value to encode
     * @return encoded value
     */
    public static String encodeQueryParamSaveEncodings(String segment) {
        String result = encodeFromArray(segment, queryNameValueEncoding, false);
        result = encodeNonCodes(result);
        return result;
    }

    public static String encodeFragmentAsIs(String nameOrValue) {
        return encodeFromArray(nameOrValue, queryNameValueEncoding, true);
    }

    protected static String encodeFromArray(String segment, String[] encodingMap, boolean encodePercent) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segment.length(); i++) {
            char currentChar = segment.charAt(i);
            if (!encodePercent && currentChar == '%') {
                result.append(currentChar);
                continue;
            }
            String encoding = encode(currentChar, encodingMap);
            if (encoding == null) {
                result.append(currentChar);
            } else {
                result.append(encoding);
            }
        }
        return result.toString();
    }

    /**
     * @param zhar integer representation of character
     * @param encodingMap encoding map
     * @return URL encoded character
     */
    private static String encode(int zhar, String[] encodingMap) {
        String encoded;
        if (zhar < encodingMap.length) {
            encoded = encodingMap[zhar];
        } else {
            encoded = encodeString(Character.toString((char) zhar));
        }
        return encoded;
    }

    /**
     * Calls URLEncoder.encode(s, "UTF-8") on given input.
     *
     * @param s string to encode
     * @return encoded string returned by URLEncoder.encode(s, "UTF-8")
     */
    public static String encodeString(String s) {
        try {
            return URLEncoder.encode(s, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String pathParamReplacement(String segment, List<String> params) {
        StringBuilder newSegment = new StringBuilder();
        Matcher matcher = PARAM_REPLACEMENT.matcher(segment);
        int i = 0;
        int start = 0;
        while (matcher.find()) {
            newSegment.append(segment, start, matcher.start());
            String replacement = params.get(i++);
            newSegment.append(replacement);
            start = matcher.end();
        }
        newSegment.append(segment, start, segment.length());
        segment = newSegment.toString();
        return segment;
    }

    /**
     * decode an encoded map
     *
     * @param map map
     * @return decoded map
     */
    public static MultivaluedMap<String, String> decode(MultivaluedMap<String, String> map) {
        MultivaluedMap<String, String> decoded = new QuarkusMultivaluedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                try {
                    decoded.add(URLDecoder.decode(entry.getKey(), UTF_8), URLDecoder.decode(value, UTF_8));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return decoded;
    }

    /**
     * decode an encoded map
     *
     * @param map map
     * @param charset charset
     * @return decoded map
     */
    public static MultivaluedMap<String, String> decode(MultivaluedMap<String, String> map, String charset) {
        if (charset == null) {
            charset = UTF_8;
        }
        MultivaluedMap<String, String> decoded = new QuarkusMultivaluedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                try {
                    decoded.add(URLDecoder.decode(entry.getKey(), charset), URLDecoder.decode(value, charset));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return decoded;
    }

    public static MultivaluedMap<String, String> encode(MultivaluedMap<String, String> map) {
        MultivaluedMap<String, String> decoded = new QuarkusMultivaluedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                try {
                    decoded.add(URLEncoder.encode(entry.getKey(), UTF_8), URLEncoder.encode(value, UTF_8));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return decoded;
    }

    public static String decode(String string) {
        try {
            return URLDecoder.decode(string, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
