/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * A simple utility to resolve the default locale to use for internationalized loggers and message bundles.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("SameParameterValue")
class LoggingLocale {

    private static final Locale LOCALE = getDefaultLocale();

    /**
     * Attempts to create a {@link Locale} based on the {@code org.jboss.logging.locale} system property. If the value
     * is not defined the {@linkplain Locale#getDefault() default locale} will be used.
     * <p>
     * The value should be in the <a href="https://tools.ietf.org/html/bcp47">BCP 47</a> format.
     * </p>
     * <p>
     * <strong>Note:</strong> Currently this uses a custom parser to attempt to parse the BCP 47 format. This will be
     * changed to use the {@code Locale.forLanguageTag()} once a move to JDK 7. Currently only the language, region and
     * variant are used to construct the locale.
     * </p>
     *
     * @return the locale created or the default locale
     */
    static Locale getLocale() {
        return LOCALE;
    }

    private static Locale getDefaultLocale() {
        final String bcp47Tag = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("org.jboss.logging.locale", "");
            }
        });
        if (bcp47Tag.trim().isEmpty()) {
            return Locale.getDefault();
        }
        // When we upgrade to Java 7 we can use the Locale.forLanguageTag(locale) which will reliably parse the
        // the value. For now we have to attempt to parse it the best we can.
        return forLanguageTag(bcp47Tag);
    }

    private static Locale forLanguageTag(final String locale) {
        // First check known locales
        if ("en-CA".equalsIgnoreCase(locale)) {
            return Locale.CANADA;
        } else if ("fr-CA".equalsIgnoreCase(locale)) {
            return Locale.CANADA_FRENCH;
        } else if ("zh".equalsIgnoreCase(locale)) {
            return Locale.CHINESE;
        } else if ("en".equalsIgnoreCase(locale)) {
            return Locale.ENGLISH;
        } else if ("fr-FR".equalsIgnoreCase(locale)) {
            return Locale.FRANCE;
        } else if ("fr".equalsIgnoreCase(locale)) {
            return Locale.FRENCH;
        } else if ("de".equalsIgnoreCase(locale)) {
            return Locale.GERMAN;
        } else if ("de-DE".equalsIgnoreCase(locale)) {
            return Locale.GERMANY;
        } else if ("it".equalsIgnoreCase(locale)) {
            return Locale.ITALIAN;
        } else if ("it-IT".equalsIgnoreCase(locale)) {
            return Locale.ITALY;
        } else if ("ja-JP".equalsIgnoreCase(locale)) {
            return Locale.JAPAN;
        } else if ("ja".equalsIgnoreCase(locale)) {
            return Locale.JAPANESE;
        } else if ("ko-KR".equalsIgnoreCase(locale)) {
            return Locale.KOREA;
        } else if ("ko".equalsIgnoreCase(locale)) {
            return Locale.KOREAN;
        } else if ("zh-CN".equalsIgnoreCase(locale)) {
            return Locale.SIMPLIFIED_CHINESE;
        } else if ("zh-TW".equalsIgnoreCase(locale)) {
            return Locale.TRADITIONAL_CHINESE;
        } else if ("en-UK".equalsIgnoreCase(locale)) {
            return Locale.UK;
        } else if ("en-US".equalsIgnoreCase(locale)) {
            return Locale.US;
        }

        // Split the string into parts and attempt
        final String[] parts = locale.split("-");
        final int len = parts.length;
        int index = 0;
        int count = 0;
        final String language = parts[index++];
        String region = ""; // country
        String variant = "";
        // The next 3 sections may be extended languages, we're just going to ignore them
        while (index < len) {
            if (count++ == 2 || !isAlpha(parts[index], 3, 3)) {
                break;
            }
            index++;
        }
        // Check for a script, we'll skip it however a script is not supported until Java 7
        if (index != len && isAlpha(parts[index], 4, 4)) {
            index++;
        }
        // Next should be the region, 3 digit is allowed but may not work with Java 6
        if (index != len && (isAlpha(parts[index], 2, 2) || isNumeric(parts[index], 3, 3))) {
            region = parts[index++];
        }
        // Next should be the variant and we will just use the first one found, all other parts will be ignored
        if (index != len && (isAlphaOrNumeric(parts[index], 5, 8))) {
            variant = parts[index];
        }
        return new Locale(language, region, variant);
    }

    private static boolean isAlpha(final String value, final int minLen, final int maxLen) {
        final int len = value.length();
        if (len < minLen || len > maxLen) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isLetter(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNumeric(final String value, final int minLen, final int maxLen) {
        final int len = value.length();
        if (len < minLen || len > maxLen) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAlphaOrNumeric(final String value, final int minLen, final int maxLen) {

        final int len = value.length();
        if (len < minLen || len > maxLen) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isLetterOrDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
