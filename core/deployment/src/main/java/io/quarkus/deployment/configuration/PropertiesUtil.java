package io.quarkus.deployment.configuration;

public class PropertiesUtil {
    private PropertiesUtil() {
    }

    public static boolean escape(int codePoint) {
        return codePoint == '#' || codePoint == '!' || codePoint == '=' || codePoint == ':';
    }

    public static boolean escapeForKey(int codePoint) {
        return Character.isSpaceChar(codePoint) || escape(codePoint);
    }

    public static boolean escapeForValueFirst(int codePoint) {
        return escapeForKey(codePoint);
    }

    public static boolean escapeForValueSubsequent(int codePoint) {
        return escape(codePoint);
    }

    public static String quotePropertyName(String name) {
        final int length = name.length();
        int cp;
        for (int i = 0; i < length; i = name.offsetByCodePoints(i, 1)) {
            cp = name.codePointAt(i);
            if (escapeForKey(cp)) {
                final StringBuilder b = new StringBuilder(length + (length >> 2));
                // get leading section
                b.append(name, 0, i);
                // and continue with escaping as needed
                b.append('\\').appendCodePoint(cp);
                for (i = name.offsetByCodePoints(i, 1); i < length; i = name.offsetByCodePoints(i, 1)) {
                    cp = name.codePointAt(i);
                    if (escapeForKey(cp)) {
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
            if (i == 0 ? escapeForValueFirst(cp) : escapeForValueSubsequent(cp)) {
                final StringBuilder b = new StringBuilder(length + (length >> 2));
                // get leading section
                b.append(value, 0, i);
                // and continue with escaping as needed
                b.append('\\').appendCodePoint(cp);
                for (i = value.offsetByCodePoints(i, 1); i < length; i = value.offsetByCodePoints(i, 1)) {
                    cp = value.codePointAt(i);
                    if (escapeForValueSubsequent(cp)) {
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
