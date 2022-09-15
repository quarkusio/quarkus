package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.Cookie;
import java.util.ArrayList;
import java.util.List;

public class CookieParser {
    public static List<Cookie> parseCookies(String cookieHeader) {
        if (cookieHeader == null) {
            throw new IllegalArgumentException("Cookie value was null");
        }
        // cookie headers can be separated by "," (HTTP header separator), or ";" (Cookie separator)
        // FIXME: the current cookie RFC doesn't mention params for cookies sent by the client
        // doesn't mention $ as a prefix either
        // FIXME: make this faster if we have a single cookie
        try {
            List<Cookie> cookies = new ArrayList<>();

            int version = 0;
            String domain = null;
            String path = null;
            String cookieName = null;
            String cookieValue = null;

            String[] parts = cookieHeader.split("[;,]");
            for (String part : parts) {
                String[] nv = part.split("=", 2);
                String name = nv.length > 0 ? nv[0].trim() : "";
                String value = nv.length > 1 ? nv[1].trim() : "";
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1)
                    value = value.substring(1, value.length() - 1);
                if (!name.startsWith("$")) {
                    if (cookieName != null) {
                        cookies.add(new Cookie(cookieName, cookieValue, path, domain, version));
                        cookieName = cookieValue = path = domain = null;
                    }

                    cookieName = name;
                    cookieValue = value;
                } else if (name.equalsIgnoreCase("$Version")) {
                    version = Integer.parseInt(value);
                } else if (name.equalsIgnoreCase("$Path")) {
                    path = value;
                } else if (name.equalsIgnoreCase("$Domain")) {
                    domain = value;
                }
            }
            if (cookieName != null) {
                cookies.add(new Cookie(cookieName, cookieValue, path, domain, version));

            }
            return cookies;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse cookie: " + cookieHeader, ex);
        }
    }
}
