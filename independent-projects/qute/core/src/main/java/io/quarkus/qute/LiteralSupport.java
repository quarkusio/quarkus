package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

class LiteralSupport {

    private static final Logger LOGGER = Logger.getLogger(LiteralSupport.class);

    static final Pattern INTEGER_LITERAL_PATTERN = Pattern.compile("[-+]?\\d{1,10}");
    static final Pattern LONG_LITERAL_PATTERN = Pattern.compile("[-+]?\\d{1,19}(L|l)");
    static final Pattern DOUBLE_LITERAL_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(d|D)");
    static final Pattern FLOAT_LITERAL_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(f|F)");

    static Object getLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return Result.NOT_FOUND;
        }
        Object literal = Result.NOT_FOUND;
        if (Parser.isStringLiteralSeparator(value.charAt(0))) {
            literal = value.substring(1, value.length() - 1);
        } else if (value.equals("true")) {
            literal = Boolean.TRUE;
        } else if (value.equals("false")) {
            literal = Boolean.FALSE;
        } else if (value.equals("null")) {
            literal = null;
        } else {
            char firstChar = value.charAt(0);
            if (Character.isDigit(firstChar) || firstChar == '-' || firstChar == '+') {
                if (INTEGER_LITERAL_PATTERN.matcher(value).matches()) {
                    try {
                        literal = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse integer literal: " + value, e);
                    }
                } else if (LONG_LITERAL_PATTERN.matcher(value).matches()) {
                    try {
                        literal = Long
                                .parseLong(value.substring(0, value.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse long literal: " + value, e);
                    }
                } else if (DOUBLE_LITERAL_PATTERN.matcher(value).matches()) {
                    try {
                        literal = Double
                                .parseDouble(value.substring(0, value.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse double literal: " + value, e);
                    }
                } else if (FLOAT_LITERAL_PATTERN.matcher(value).matches()) {
                    try {
                        literal = Float
                                .parseFloat(value.substring(0, value.length() - 1));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Unable to parse float literal: " + value, e);
                    }
                }
            }
        }
        return literal;
    }

}
