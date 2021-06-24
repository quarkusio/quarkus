package io.quarkus.qute;

import java.util.regex.Pattern;
import org.jboss.logging.Logger;

class LiteralSupport {

    private static final Logger LOGGER = Logger.getLogger(LiteralSupport.class);

    static final Pattern INTEGER_LITERAL_PATTERN = Pattern.compile("[-+]?\\d{1,10}");
    static final Pattern LONG_LITERAL_PATTERN = Pattern.compile("[-+]?\\d{1,19}(L|l)");
    static final Pattern DOUBLE_LITERAL_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(d|D)");
    static final Pattern FLOAT_LITERAL_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(f|F)");

    /**
     * 
     * @param literal
     * @return {@link Results.NotFound.EMPTY} if no literal was found, otherwise the literal value
     */
    static Object getLiteralValue(String literal) {
        Object value = Results.NotFound.EMPTY;
        if (literal == null || literal.isEmpty()) {
            return value;
        }
        if (isStringLiteralSeparator(literal.charAt(0))) {
            value = literal.substring(1, literal.length() - 1);
        } else if (literal.equals("true")) {
            value = Boolean.TRUE;
        } else if (literal.equals("false")) {
            value = Boolean.FALSE;
        } else if (literal.equals("null")) {
            value = null;
        } else {
            char firstChar = literal.charAt(0);
            if (Character.isDigit(firstChar) || firstChar == '-' || firstChar == '+') {
                if (INTEGER_LITERAL_PATTERN.matcher(literal).matches()) {
                    try {
                        value = Integer.parseInt(literal);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse integer literal: " + literal, e);
                    }
                } else if (LONG_LITERAL_PATTERN.matcher(literal).matches()) {
                    try {
                        value = Long
                                .parseLong(literal.substring(0, literal.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse long literal: " + literal, e);
                    }
                } else if (DOUBLE_LITERAL_PATTERN.matcher(literal).matches()) {
                    try {
                        value = Double
                                .parseDouble(literal.substring(0, literal.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse double literal: " + literal, e);
                    }
                } else if (FLOAT_LITERAL_PATTERN.matcher(literal).matches()) {
                    try {
                        value = Float
                                .parseFloat(literal.substring(0, literal.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse float literal: " + literal, e);
                    }
                }
            }
        }
        return value;
    }

    /**
     *
     * @param character
     * @return <code>true</code> if the char is a string literal separator,
     *         <code>false</code> otherwise
     */
    static boolean isStringLiteralSeparator(char character) {
        return character == '"' || character == '\'';
    }

}
