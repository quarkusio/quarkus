package io.quarkus.deployment.configuration;

public class PropertiesUtil {
    private PropertiesUtil() {
    }

    public static boolean needsEscape(int codePoint) {
        return codePoint == '#' || codePoint == '!' || codePoint == '=' || codePoint == ':';
    }

    public static boolean needsEscapeForKey(int codePoint) {
        return Character.isSpaceChar(codePoint) || needsEscape(codePoint);
    }

    public static boolean needsEscapeForValueFirst(int codePoint) {
        return needsEscapeForKey(codePoint);
    }

    public static boolean needsEscapeForValueSubsequent(int codePoint) {
        return needsEscape(codePoint);
    }

    public static String quotePropertyName(String name) {
        final int length = name.length();
        int cp;
        for (int i = 0; i < length; i = name.offsetByCodePoints(i, 1)) {
            cp = name.codePointAt(i);
            if (needsEscapeForKey(cp)) {
                final StringBuilder b = new StringBuilder(length + (length >> 2));
                // get leading section
                b.append(name, 0, i);
                // and continue with escaping as needed
                b.append('\\').appendCodePoint(cp);
                for (i = name.offsetByCodePoints(i, 1); i < length; i = name.offsetByCodePoints(i, 1)) {
                    cp = name.codePointAt(i);
                    if (needsEscapeForKey(cp)) {
                        b.append('\\');
                    }
                    b.appendCodePoint(cp);
                }
                return b.toString();
            }
        }
        // no escaping needed - majority case
        return name;
    }

    public static String quotePropertyValue(String value) {
        final int length = value.length();
        int cp;
        for (int i = 0; i < length; i = value.offsetByCodePoints(i, 1)) {
            cp = value.codePointAt(i);
            if (i == 0 ? needsEscapeForValueFirst(cp) : needsEscapeForValueSubsequent(cp)) {
                final StringBuilder b = new StringBuilder(length + (length >> 2));
                // get leading section
                b.append(value, 0, i);
                // and continue with escaping as needed
                b.append('\\').appendCodePoint(cp);
                for (i = value.offsetByCodePoints(i, 1); i < length; i = value.offsetByCodePoints(i, 1)) {
                    cp = value.codePointAt(i);
                    if (needsEscapeForValueSubsequent(cp)) {
                        b.append('\\');
                    }
                    b.appendCodePoint(cp);
                }
                return b.toString();
            }
        }
        // no escaping needed - majority case
        return value;
    }
}
