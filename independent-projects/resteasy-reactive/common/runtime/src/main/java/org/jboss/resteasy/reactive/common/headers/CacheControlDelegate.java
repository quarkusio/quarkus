package org.jboss.resteasy.reactive.common.headers;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.reactive.common.util.ExtendedCacheControl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class CacheControlDelegate implements RuntimeDelegate.HeaderDelegate<CacheControl> {
    public static final CacheControlDelegate INSTANCE = new CacheControlDelegate();

    public CacheControl fromString(String value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("Param was null");
        ExtendedCacheControl result = new ExtendedCacheControl();
        result.setNoTransform(false);

        for (String directive : splitDirectives(value)) {
            directive = directive.trim();
            if (directive.isEmpty()) {
                continue;
            }

            // Split on the first '=' only so that values containing '=' are preserved.
            int eq = directive.indexOf('=');
            String name;
            String val = null;
            if (eq == -1) {
                name = directive;
            } else {
                name = directive.substring(0, eq).trim();
                val = unquote(directive.substring(eq + 1).trim());
            }

            String lowercase = name.toLowerCase();
            if ("no-cache".equals(lowercase)) {
                result.setNoCache(true);
                addFieldNames(val, result.getNoCacheFields());
            } else if ("private".equals(lowercase)) {
                result.setPrivate(true);
                addFieldNames(val, result.getPrivateFields());
            } else if ("no-store".equals(lowercase)) {
                result.setNoStore(true);
            } else if ("max-age".equals(lowercase)) {
                if (val == null)
                    throw new IllegalArgumentException("null max age");
                result.setMaxAge(Integer.valueOf(val));
            } else if ("s-maxage".equals(lowercase)) {
                if (val == null)
                    throw new IllegalArgumentException("null s-maxage");
                result.setSMaxAge(Integer.valueOf(val));
            } else if ("no-transform".equals(lowercase)) {
                result.setNoTransform(true);
            } else if ("must-revalidate".equals(lowercase)) {
                result.setMustRevalidate(true);
            } else if ("proxy-revalidate".equals(lowercase)) {
                result.setProxyRevalidate(true);
            } else if ("public".equals(lowercase)) {
                result.setPublic(true);
            } else {
                if (val == null)
                    val = "";
                result.getCacheExtension().put(name, val);
            }
        }
        return result;
    }

    /**
     * Splits a Cache-Control header value into its directives, ignoring commas that appear inside a
     * quoted string. A quoted directive such as {@code private="Set-Cookie, Authorization"} carries a
     * comma-separated field list (see RFC 7234 section 5.2) and must stay in a single directive.
     */
    private static List<String> splitDirectives(String value) {
        List<String> directives = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                directives.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        directives.add(current.toString());
        return directives;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void addFieldNames(String value, List<String> fields) {
        if (value == null || value.isEmpty()) {
            return;
        }
        for (String field : value.split(",")) {
            field = field.trim();
            if (!field.isEmpty()) {
                fields.add(field);
            }
        }
    }

    private static StringBuilder addDirective(String directive, StringBuilder buffer) {
        if (buffer.length() > 0)
            buffer.append(", ");
        buffer.append(directive);
        return buffer;
    }

    public String toString(CacheControl value) {
        if (value == null)
            throw new IllegalArgumentException("param was null");
        StringBuilder buffer = new StringBuilder();
        if (value.isNoCache()) {
            List<String> fields = value.getNoCacheFields();
            if (fields.size() < 1) {
                addDirective("no-cache", buffer);
            } else {
                for (String field : value.getNoCacheFields()) {
                    addDirective("no-cache", buffer).append("=\"").append(field).append("\"");
                }
            }
        }
        if (value instanceof ExtendedCacheControl) {
            ExtendedCacheControl ecc = (ExtendedCacheControl) value;
            if (ecc.isPublic()) {
                addDirective("public", buffer);
            }
        }
        if (value.isMustRevalidate())
            addDirective("must-revalidate", buffer);
        if (value.isNoTransform())
            addDirective("no-transform", buffer);
        if (value.isNoStore())
            addDirective("no-store", buffer);
        if (value.isProxyRevalidate())
            addDirective("proxy-revalidate", buffer);
        if (value.getSMaxAge() > -1)
            addDirective("s-maxage", buffer).append("=").append(value.getSMaxAge());
        if (value.getMaxAge() > -1)
            addDirective("max-age", buffer).append("=").append(value.getMaxAge());
        if (value.isPrivate()) {
            List<String> fields = value.getPrivateFields();
            if (fields.size() < 1)
                addDirective("private", buffer);
            else {
                for (String field : value.getPrivateFields()) {
                    addDirective("private", buffer).append("=\"").append(field).append("\"");
                }
            }
        }
        for (String key : value.getCacheExtension().keySet()) {
            String val = value.getCacheExtension().get(key);
            addDirective(key, buffer);
            if (val != null && !"".equals(val)) {
                buffer.append("=\"").append(val).append("\"");
            }
        }
        return buffer.toString();
    }

}
