package io.quarkus.runtime.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternUtil {

    public static boolean isExpression(String s) {
        return s == null || s.isEmpty() ? false : s.contains("*") || s.contains("?");
    }

    public static Pattern toRegex(final String str) {
        try {
            String wildcardToRegex = wildcardToRegex(str);
            if (wildcardToRegex != null && !wildcardToRegex.isEmpty()) {
                return Pattern.compile(wildcardToRegex);
            }
        } catch (PatternSyntaxException e) {
            //ignore it
        }
        return null;
    }

    private static String wildcardToRegex(String wildcard) {
        // don't try with file match char in pattern
        if (!isExpression(wildcard)) {
            return null;
        }
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append("^.*");
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                case '^': // escape character in cmd.exe
                    s.append("\\");
                    break;
                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append(".*$");
        return (s.toString());
    }

    /**
     * Checks if the specified candiate matches the pattern.
     *
     * @param candiate The candidate.
     * @param pattern The pattern.
     * @return true if candidate matches the pattern.
     */
    public static boolean matches(String candidate, String pattern) {
        if (StringUtil.isNullOrEmpty(pattern)) {
            return true;
        }
        if (!isExpression(pattern)) {
            return candidate.contains(pattern);
        }
        Pattern p = toRegex(pattern);
        return p.matcher(candidate).matches();
    }
}
