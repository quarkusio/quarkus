package org.jboss.resteasy.reactive.common.headers;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.jboss.resteasy.reactive.common.util.CookieParser;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class CookieHeaderDelegate implements RuntimeDelegate.HeaderDelegate<Cookie> {
    public static final CookieHeaderDelegate INSTANCE = new CookieHeaderDelegate();

    public Cookie fromString(String value) throws IllegalArgumentException {
        return CookieParser.parseCookies(value).get(0);
    }

    public String toString(Cookie value) {
        StringBuffer buf = new StringBuffer();
        ServerCookie.appendCookieValue(buf, 0, value.getName(), value.getValue(), value.getPath(), value.getDomain(), null, -1,
                false);
        return buf.toString();
    }
}
