package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class Expressions {

    public static final String TYPECHECK_NAMESPACE_PLACEHOLDER = "$$namespace$$";

    static final String LEFT_BRACKET = "(";
    static final String RIGHT_BRACKET = ")";

    private Expressions() {
    }

    public static boolean isVirtualMethod(String value) {
        return value.indexOf(LEFT_BRACKET) != -1;
    }

    public static String parserVirtualMethodName(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        return value.substring(0, start);
    }

    public static List<String> parseVirtualMethodParams(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        if (start != -1 && value.endsWith(RIGHT_BRACKET)) {
            String params = value.substring(start + 1, value.length() - 1);
            return splitParts(params, Expressions::isParamSeparator);
        }
        throw new IllegalArgumentException("Not a virtual method: " + value);
    }

    public static List<String> splitParts(String value) {
        return splitParts(value, Parser::isSeparator);
    }

    public static List<String> splitParts(String value, Predicate<Character> separatorPredicate) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        boolean stringLiteral = false;
        boolean separator = false;
        boolean infix = false;
        boolean brackets = false;
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (separatorPredicate.test(c)) {
                // Adjacent separators are ignored
                if (!separator) {
                    if (!stringLiteral) {
                        if (buffer.length() > 0) {
                            builder.add(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        separator = true;
                    } else {
                        buffer.append(c);
                    }
                }
            } else {
                if (Parser.isStringLiteralSeparator(c)) {
                    stringLiteral = !stringLiteral;
                }
                // Non-separator char
                if (!stringLiteral) {
                    if (!brackets && c == ' ') {
                        if (infix) {
                            buffer.append(LEFT_BRACKET);
                        } else {
                            // Separator
                            infix = true;
                            if (buffer.length() > 0) {
                                builder.add(buffer.toString());
                                buffer = new StringBuilder();
                            }
                        }
                    } else {
                        if (Parser.isBracket(c)) {
                            brackets = !brackets;
                        }
                        buffer.append(c);
                    }
                    separator = false;
                } else {
                    buffer.append(c);
                    separator = false;
                }
            }
        }
        if (infix) {
            buffer.append(RIGHT_BRACKET);
        }
        if (buffer.length() > 0) {
            builder.add(buffer.toString());
        }
        return builder.build();
    }

    private static boolean isParamSeparator(char candidate) {
        return ',' == candidate;
    }

}
