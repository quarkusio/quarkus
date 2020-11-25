package org.jboss.resteasy.reactive.common.util;

import java.util.Locale;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class LocaleHelper {
    public static Locale extractLocale(String lang) {
        int q = lang.indexOf(';');
        if (q > -1) {
            lang = lang.substring(0, q);
        }
        String[] split = lang.trim().split("-");
        if (split.length == 1) {
            return new Locale(split[0].toLowerCase());
        } else if (split.length == 2) {
            return new Locale(split[0].toLowerCase(), split[1].toLowerCase());
        } else if (split.length > 2) {
            return new Locale(split[0], split[1], split[2]);
        }
        return null; // unreachable
    }

    /**
     * HTTP 1.1 has different String format for language than what java.util.Locale does '-' instead of '_'
     * as a separator
     *
     * @param value locale
     * @return converted language format string
     */
    public static String toLanguageString(Locale value) {
        StringBuffer buf = new StringBuffer(value.getLanguage().toLowerCase());
        if (value.getCountry() != null && !value.getCountry().equals("")) {
            buf.append("-").append(value.getCountry().toLowerCase());
        }
        return buf.toString();
    }
}
