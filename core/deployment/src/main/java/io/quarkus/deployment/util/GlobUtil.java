package io.quarkus.deployment.util;

import java.util.regex.Pattern;

public class GlobUtil {

    private GlobUtil() {
    }

    /**
     * Transforms the given {@code glob} to a regular expression suitable for passing to
     * {@link Pattern#compile(String)}.
     *
     * <h2>Glob syntax
     * <h2>
     *
     * <table>
     * <tr>
     * <th>Construct</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td><code>*</code></td>
     * <td>Matches a (possibly empty) sequence of characters that does not contain slash ({@code /})</td>
     * </tr>
     * <tr>
     * <td><code>**</code></td>
     * <td>Matches a (possibly empty) sequence of characters that may contain slash ({@code /})</td>
     * </tr>
     * <tr>
     * <td><code>?</code></td>
     * <td>Matches one character, but not slash</td>
     * </tr>
     * <tr>
     * <td><code>[abc]</code></td>
     * <td>Matches one character given in the bracket, but not slash</td>
     * </tr>
     * <tr>
     * <td><code>[a-z]</code></td>
     * <td>Matches one character from the range given in the bracket, but not slash</td>
     * </tr>
     * <tr>
     * <td><code>[!abc]</code></td>
     * <td>Matches one character not named in the bracket; does not match slash</td>
     * </tr>
     * <tr>
     * <td><code>[a-z]</code></td>
     * <td>Matches one character outside the range given in the bracket; does not match slash</td>
     * </tr>
     * <tr>
     * <td><code>{one,two,three}</code></td>
     * <td>Matches any of the alternating tokens separated by comma; the tokens may contain wildcards, nested
     * alternations and ranges</td>
     * </tr>
     * <tr>
     * <td><code>\</code></td>
     * <td>The escape character</td>
     * </tr>
     * </table>
     *
     * @param glob the glob expression to transform
     * @return a regular expression suitable for {@link Pattern}
     * @throws IllegalStateException in case the {@code glob} is syntactically invalid
     */
    public static String toRegexPattern(String glob) {
        final int length = glob.length();
        final StringBuilder result = new StringBuilder(length + 4);
        glob(glob, 0, length, null, result);
        return result.toString();
    }

    private static int glob(String glob, int i, int length, String stopChars, StringBuilder result) {
        while (i < length) {
            char current = glob.charAt(i++);
            switch (current) {
                case '*':
                    if (i < length && glob.charAt(i) == '*') {
                        result.append(".*");
                        i++;
                    } else {
                        result.append("[^/]*");
                    }
                    break;
                case '?':
                    result.append("[^/]");
                    break;
                case '[':
                    i = charClass(glob, i, length, result);
                    break;
                case '{':
                    i = alternation(glob, i, length, result);
                    break;
                case '\\':
                    i = unescape(glob, i, length, result, false);
                    break;
                default:
                    if (stopChars != null && stopChars.indexOf(current) >= 0) {
                        i--;
                        return i;
                    } else {
                        escapeIfNeeded(current, result);
                    }
                    break;
            }
        }
        return i;
    }

    private static int alternation(String glob, int i, int length, StringBuilder result) {
        result.append("(?:");
        while (i < length) {
            char current = glob.charAt(i++);
            switch (current) {
                case '}':
                    result.append(')');
                    return i;
                case ',':
                    result.append('|');
                    i = glob(glob, i, length, ",}", result);
                    break;
                default:
                    i--;
                    i = glob(glob, i, length, ",}", result);
                    break;
            }
        }
        throw new IllegalStateException(String.format("Missing } at the end of input in glob %s", glob));
    }

    private static int unescape(String glob, int i, int length, StringBuilder result, boolean charClass) {
        if (i < length) {
            final char current = glob.charAt(i++);
            if (charClass) {
                escapeCharClassIfNeeded(current, result);
            } else {
                escapeIfNeeded(current, result);
            }
            return i;
        } else {
            throw new IllegalStateException(
                    String.format("Incomplete escape sequence at the end of input in glob %s", glob));
        }
    }

    private static int charClass(String glob, int i, int length, StringBuilder result) {
        result.append("[[^/]&&[");
        if (i < length && glob.charAt(i) == '!') {
            i++;
            result.append('^');
        }
        while (i < length) {
            char current = glob.charAt(i++);
            switch (current) {
                case ']':
                    result.append("]]");
                    return i;
                case '-':
                    result.append('-');
                    break;
                case '\\':
                    i = unescape(glob, i, length, result, true);
                    break;
                default:
                    escapeCharClassIfNeeded(current, result);
                    break;
            }
        }
        throw new IllegalStateException(String.format("Missing ] at the end of input in glob %s", glob));
    }

    private static void escapeIfNeeded(char current, StringBuilder result) {
        switch (current) {
            case '*':
            case '?':
            case '+':
            case '.':
            case '^':
            case '$':
            case '{':
            case '[':
            case ']':
            case '|':
            case '(':
            case ')':
            case '\\':
                result.append('\\');
                break;
            default:
                break;
        }
        result.append(current);
    }

    private static void escapeCharClassIfNeeded(char current, StringBuilder result) {
        switch (current) {
            case '^':
            case '[':
            case ']':
            case '&':
            case '-':
            case '\\':
                result.append('\\');
                break;
            default:
                break;
        }
        result.append(current);
    }
}
