package org.jboss.resteasy.reactive.common.headers;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Server-side cookie representation. Stolen from Tomcat.
 */
public class ServerCookie implements Serializable {
    private static final String tspecials = ",; ";
    private static final String tspecials2 = "()<>@,;:\\\"/[]?={} \t";

    /*
     * Tests a string and returns true if the string counts as a
     * reserved token in the Java language.
     *
     * @param value the <code>String</code> to be tested
     *
     * @return <code>true</code> if the <code>String</code> is a reserved
     * token; <code>false</code> if it is not
     */
    public static boolean isToken(String value) {
        if (value == null)
            return true;
        int len = value.length();

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);

            if (tspecials.indexOf(c) != -1)
                return false;
        }
        return true;
    }

    public static boolean containsCTL(String value, int version) {
        if (value == null)
            return false;
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f) {
                if (c == 0x09)
                    continue; //allow horizontal tabs
                return true;
            }
        }
        return false;
    }

    public static boolean isToken2(String value) {
        if (value == null)
            return true;
        int len = value.length();

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (tspecials2.indexOf(c) != -1)
                return false;
        }
        return true;
    }

    /**
     * @deprecated Not used: Deprecated in the original org.apache.tomcat.util.http.ServerCookie class.
     * @param name name
     * @return boolean flag
     */
    public static boolean checkName(String name) {
        if (!isToken(name)
                || name.equalsIgnoreCase("Comment") // rfc2019
                || name.equalsIgnoreCase("Discard") // rfc2965
                || name.equalsIgnoreCase("Domain") // rfc2019
                || name.equalsIgnoreCase("Expires") // Netscape
                || name.equalsIgnoreCase("Max-Age") // rfc2019
                || name.equalsIgnoreCase("Path") // rfc2019
                || name.equalsIgnoreCase("Secure") // rfc2019
                || name.equalsIgnoreCase("Version") // rfc2019
        // TODO remaining RFC2965 attributes
        ) {
            return false;
        }
        return true;
    }

    // -------------------- Cookie parsing tools

    /**
     * Return the header name to set the cookie, based on cookie version.
     * 
     * @param version cookie version
     * @return cookie header name
     */
    public static String getCookieHeaderName(int version) {
        // TODO Re-enable logging when RFC2965 is implemented
        // log( (version==1) ? "Set-Cookie2" : "Set-Cookie");
        if (version == 1) {
            // XXX RFC2965 not referenced in Servlet Spec
            // Set-Cookie2 is not supported by Netscape 4, 6, IE 3, 5
            // Set-Cookie2 is supported by Lynx and Opera
            // Need to check on later IE and FF releases but for now...
            // RFC2109
            return "Set-Cookie";
            // return "Set-Cookie2";
        } else {
            // Old Netscape
            return "Set-Cookie";
        }
    }

    /**
     * US locale - all HTTP dates are in english
     */
    private static final Locale LOCALE_US = Locale.US;

    /**
     * GMT timezone - all HTTP dates are on GMT
     */
    public static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");
    /**
     * Pattern used for old cookies
     */
    private static final String OLD_COOKIE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";

    private static final DateFormat oldCookieFormat = new SimpleDateFormat(OLD_COOKIE_PATTERN, LOCALE_US);

    public static String formatOldCookie(Date d) {
        String ocf = null;
        synchronized (oldCookieFormat) {
            ocf = oldCookieFormat.format(d);
        }
        return ocf;
    }

    public static void formatOldCookie(Date d, StringBuffer sb,
            FieldPosition fp) {
        synchronized (oldCookieFormat) {
            oldCookieFormat.format(d, sb, fp);
        }
    }

    private static final String ancientDate = formatOldCookie(new Date(10000));

    // TODO RFC2965 fields also need to be passed
    public static void appendCookieValue(StringBuffer headerBuf,
            int version,
            String name,
            String value,
            String path,
            String domain,
            String comment,
            int maxAge,
            boolean isSecure) {
        StringBuffer buf = new StringBuffer();
        // Servlet implementation checks name
        buf.append(name);
        buf.append("=");
        // Servlet implementation does not check anything else

        maybeQuote2(version, buf, value);

        // Add version 1 specific information
        if (version == 1) {
            // Version=1 ... required
            buf.append("; Version=1");

            // Comment=comment
            if (comment != null) {
                buf.append("; Comment=");
                maybeQuote2(version, buf, comment);
            }
        }

        // Add domain information, if present
        if (domain != null) {
            buf.append("; Domain=");
            maybeQuote2(version, buf, domain);
        }

        // Max-Age=secs ... or use old "Expires" format
        // TODO RFC2965 Discard
        if (maxAge >= 0) {
            if (version == 0) {
                // Wdy, DD-Mon-YY HH:MM:SS GMT ( Expires Netscape format )
                buf.append("; Expires=");
                // To expire immediately we need to set the time in past
                if (maxAge == 0)
                    buf.append(ancientDate);
                else
                    formatOldCookie(new Date(System.currentTimeMillis() +
                            maxAge * 1000L), buf,
                            new FieldPosition(0));

            } else {
                buf.append("; Max-Age=");
                buf.append(maxAge);
            }
        }

        // Path=path
        if (path != null) {
            buf.append("; Path=");
            maybeQuote2(version, buf, path);
        }

        // Secure
        if (isSecure) {
            buf.append("; Secure");
        }

        headerBuf.append(buf);
    }

    public static boolean alreadyQuoted(String value) {
        if (value == null || value.length() == 0)
            return false;
        return (value.charAt(0) == '\"' && value.charAt(value.length() - 1) == '\"');
    }

    /**
     * Quotes values using rules that vary depending on Cookie version.
     *
     * @param version cookie version
     * @param buf buffer
     * @param value value
     */
    public static void maybeQuote2(int version, StringBuffer buf, String value) {
        if (value == null || value.length() == 0) {
            buf.append("\"\"");
        } else if (containsCTL(value, version))
            throw new IllegalArgumentException("control character in cookie value");
        else if (alreadyQuoted(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 1, value.length() - 1));
            buf.append('"');
        } else if (version == 0 && !isToken(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 0, value.length()));
            buf.append('"');
        } else if (version == 1 && !isToken2(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 0, value.length()));
            buf.append('"');
        } else {
            buf.append(value);
        }
    }

    /**
     * Escapes any double quotes in the given string.
     *
     * @param s the input string
     * @param beginIndex start index inclusive
     * @param endIndex exclusive
     * @return The (possibly) escaped string
     */
    private static String escapeDoubleQuotes(String s, int beginIndex, int endIndex) {

        if (s == null || s.length() == 0 || s.indexOf('"') == -1) {
            return s;
        }

        StringBuffer b = new StringBuffer();
        for (int i = beginIndex; i < endIndex; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                b.append(c);
                //ignore the character after an escape, just append it
                if (++i >= endIndex)
                    throw new IllegalArgumentException("Invalid escape character in cookie value");
                b.append(s.charAt(i));
            } else if (c == '"')
                b.append('\\').append('"');
            else
                b.append(c);
        }

        return b.toString();
    }

}
