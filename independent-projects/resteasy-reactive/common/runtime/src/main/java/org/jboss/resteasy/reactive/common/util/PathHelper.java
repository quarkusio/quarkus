/**
 *
 */
package org.jboss.resteasy.reactive.common.util;

import java.util.regex.Pattern;

/**
 * A utility class for handling URI template parameters. As the Java
 * regulare expressions package does not handle named groups, this
 * class attempts to simulate that functionality by using groups.
 *
 * @author Ryan J. McDonough
 * @author Bill Burke
 */
public class PathHelper {
    public static final String URI_PARAM_NAME_REGEX = "\\w[\\w\\.-]*";
    public static final String URI_PARAM_REGEX_REGEX = "[^{}][^{}]*";
    public static final String URI_PARAM_REGEX = "\\{\\s*(" + URI_PARAM_NAME_REGEX + ")\\s*(:\\s*(" + URI_PARAM_REGEX_REGEX
            + "))?\\}";
    public static final String URI_PARAM_WITH_REGEX = "\\{\\s*(" + URI_PARAM_NAME_REGEX + ")\\s*(:\\s*(" + URI_PARAM_REGEX_REGEX
            + "))\\}";
    public static final String URI_PARAM_WITHOUT_REGEX = "\\{(" + URI_PARAM_NAME_REGEX + ")\\}";
    public static final Pattern URI_PARAM_PATTERN = Pattern.compile(URI_PARAM_REGEX);
    public static final Pattern URI_PARAM_WITH_REGEX_PATTERN = Pattern.compile(URI_PARAM_WITH_REGEX);
    public static final Pattern URI_PARAM_WITHOUT_REGEX_PATTERN = Pattern.compile(URI_PARAM_WITHOUT_REGEX);

    /**
     * A regex pattern that searches for a URI template parameter in the form of {*}
     */
    public static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("(\\{([^}]+)\\})");

    public static final String URI_TEMPLATE_REPLACE_PATTERN = "(.*?)";

    public static String getEncodedPathInfo(String path, String contextPath) {
        if (contextPath != null && !"".equals(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;

    }

    public static final char openCurlyReplacement = 6;
    public static final char closeCurlyReplacement = 7;

    public static String replaceEnclosedCurlyBraces(String str) {
        char[] chars = str.toCharArray();
        int open = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{') {
                if (open != 0)
                    chars[i] = openCurlyReplacement;
                open++;
            } else if (chars[i] == '}') {
                open--;
                if (open != 0) {
                    chars[i] = closeCurlyReplacement;
                }
            }
        }
        return new String(chars);
    }

    /**
     * A cheaper (memory-wise) version of replaceEnclosedCurlyBraces(String str).
     * 
     * @param str input string
     * @return replaced output
     */
    public static CharSequence replaceEnclosedCurlyBracesCS(String str) {
        int open = 0;
        CharSequence cs = str;
        char[] chars = null;
        for (int i = 0; i < str.length(); i++) {
            if (cs.charAt(i) == '{') {
                if (open != 0) {
                    if (cs == str) {
                        chars = str.toCharArray();
                        cs = new ArrayCharSequence(chars);
                    }
                    chars[i] = openCurlyReplacement;
                }
                open++;
            } else if (cs.charAt(i) == '}') {
                open--;
                if (open != 0) {
                    if (cs == str) {
                        chars = str.toCharArray();
                        cs = new ArrayCharSequence(chars);
                    }
                    chars[i] = closeCurlyReplacement;
                }
            }
        }
        return cs;
    }

    public static String recoverEnclosedCurlyBraces(String str) {
        return str.replace(openCurlyReplacement, '{').replace(closeCurlyReplacement, '}');
    }

}
