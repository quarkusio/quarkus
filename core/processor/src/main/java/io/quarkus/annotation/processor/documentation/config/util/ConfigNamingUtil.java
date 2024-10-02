package io.quarkus.annotation.processor.documentation.config.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;

public final class ConfigNamingUtil {

    private static final String CONFIG = "Config";
    private static final String CONFIGURATION = "Configuration";
    private static final String HYPHEN = "-";
    private static final Pattern ENUM_SEPARATOR_PATTERN = Pattern.compile("([-_]+)");
    private static final String NAMED_MAP_CONFIG_ITEM_FORMAT = ".\"%s\"";

    private ConfigNamingUtil() {
    }

    public static String getRootPrefix(String prefix, String name, String simpleClassName, ConfigPhase configPhase) {
        String rootPrefix;

        if (name.equals(Markers.HYPHENATED_ELEMENT_NAME)) {
            rootPrefix = deriveConfigRootName(simpleClassName, prefix, configPhase);
        } else if (!prefix.isEmpty()) {
            if (!name.isEmpty()) {
                rootPrefix = prefix + Markers.DOT + name;
            } else {
                rootPrefix = prefix;
            }
        } else {
            rootPrefix = name;
        }

        if (rootPrefix.endsWith(Markers.DOT + Markers.PARENT)) {
            // take into account the root case which would contain characters that can't be used to create the final file
            rootPrefix = rootPrefix.replace(Markers.DOT + Markers.PARENT, "");
        }

        return rootPrefix;
    }

    static String deriveConfigRootName(String simpleClassName, String prefix, ConfigPhase configPhase) {
        String simpleNameInLowerCase = simpleClassName.toLowerCase();
        int length = simpleNameInLowerCase.length();

        if (simpleNameInLowerCase.endsWith(CONFIG.toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - CONFIG.length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        } else if (simpleNameInLowerCase.endsWith(CONFIGURATION.toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - CONFIGURATION.length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        } else if (simpleNameInLowerCase.endsWith(configPhase.getConfigSuffix().toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - configPhase.getConfigSuffix().length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        }

        return !prefix.isEmpty() ? prefix + Markers.DOT + ConfigNamingUtil.hyphenate(simpleClassName)
                : Markers.DEFAULT_PREFIX + Markers.DOT + ConfigNamingUtil.hyphenate(simpleClassName);
    }

    public static Iterator<String> camelHumpsIterator(String str) {
        return new Iterator<String>() {
            int idx;

            @Override
            public boolean hasNext() {
                return idx < str.length();
            }

            @Override
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

    static Iterator<String> lowerCase(Iterator<String> orig) {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return orig.hasNext();
            }

            @Override
            public String next() {
                return orig.next().toLowerCase(Locale.ROOT);
            }
        };
    }

    static String join(Iterator<String> it) {
        final StringBuilder b = new StringBuilder();
        if (it.hasNext()) {
            b.append(it.next());
            while (it.hasNext()) {
                b.append("-");
                b.append(it.next());
            }
        }
        return b.toString();
    }

    public static String hyphenate(String orig) {
        return join(lowerCase(camelHumpsIterator(orig)));
    }

    /**
     * This needs to be consistent with io.quarkus.runtime.configuration.HyphenateEnumConverter.
     */
    public static String hyphenateEnumValue(String orig) {
        StringBuffer target = new StringBuffer();
        String hyphenate = hyphenate(orig);
        Matcher matcher = ENUM_SEPARATOR_PATTERN.matcher(hyphenate);
        while (matcher.find()) {
            matcher.appendReplacement(target, HYPHEN);
        }
        matcher.appendTail(target);
        return target.toString();
    }

    static String normalizeDurationValue(String value) {
        if (!value.isEmpty() && Character.isDigit(value.charAt(value.length() - 1))) {
            try {
                value = Integer.parseInt(value) + "S";
            } catch (NumberFormatException ignore) {
            }
        }
        value = value.toUpperCase(Locale.ROOT);
        return value;
    }

    /**
     * Replace each character that is neither alphanumeric nor _ with _ then convert the name to upper case, e.g.
     * quarkus.datasource.jdbc.initial-size -> QUARKUS_DATASOURCE_JDBC_INITIAL_SIZE
     * See also: io.smallrye.config.common.utils.StringUtil#replaceNonAlphanumericByUnderscores(java.lang.String)
     */
    public static String toEnvVarName(final String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('a' <= c && c <= 'z' ||
                    'A' <= c && c <= 'Z' ||
                    '0' <= c && c <= '9') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString().toUpperCase();
    }

    public static String getMapKey(String mapKey) {
        return String.format(NAMED_MAP_CONFIG_ITEM_FORMAT, mapKey);
    }
}
