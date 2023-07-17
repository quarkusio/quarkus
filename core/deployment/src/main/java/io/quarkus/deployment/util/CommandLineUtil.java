package io.quarkus.deployment.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class contains code coming from org.apache.maven.shared.utils.cli.CommandLineUtils.
 * <p>
 * We don't want to directly use code coming from Maven as this artifact should be Maven-agnostic.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l </a>
 */
public final class CommandLineUtil {

    private CommandLineUtil() {
    }

    /**
     * @param toProcess The command line to translate.
     * @return The array of translated parts.
     * @throws IllegalStateException in case of unbalanced quotes.
     */
    public static String[] translateCommandline(String toProcess) {
        if ((toProcess == null) || (toProcess.length() == 0)) {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        boolean inEscape = false;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' \\", true);
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        if (inEscape) {
                            current.append(nextTok);
                            inEscape = false;
                        } else {
                            state = normal;
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        if (inEscape) {
                            current.append(nextTok);
                            inEscape = false;
                        } else {
                            state = normal;
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        if (inEscape) {
                            inEscape = false;
                            current.append(nextTok);
                        } else {
                            state = inQuote;
                        }
                    } else if ("\"".equals(nextTok)) {
                        if (inEscape) {
                            inEscape = false;
                            current.append(nextTok);
                        } else {
                            state = inDoubleQuote;
                        }
                    } else if (" ".equals(nextTok)) {
                        if (current.length() != 0) {
                            tokens.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
            }
        }

        if (current.length() != 0) {
            tokens.add(current.toString());
        }

        if ((state == inQuote) || (state == inDoubleQuote)) {
            throw new IllegalStateException("unbalanced quotes in " + toProcess);
        }

        return tokens.toArray(new String[tokens.size()]);
    }
}
