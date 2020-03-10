package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Expressions {

    public static final String TYPECHECK_NAMESPACE_PLACEHOLDER = "$$namespace$$";

    static final String LEFT_BRACKET = "(";
    static final String RIGHT_BRACKET = ")";
    public static final char TYPE_INFO_SEPARATOR = '|';

    private Expressions() {
    }

    public static boolean isVirtualMethod(String value) {
        return value.indexOf(LEFT_BRACKET) != -1;
    }

    public static String parseVirtualMethodName(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        return value.substring(0, start);
    }

    public static List<String> parseVirtualMethodParams(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        if (start != -1 && value.endsWith(RIGHT_BRACKET)) {
            String params = value.substring(start + 1, value.length() - 1);
            return splitParts(params, Expressions::isParamSeparator, Parser::isStringLiteralSeparator);
        }
        throw new IllegalArgumentException("Not a virtual method: " + value);
    }

    public static String buildVirtualMethodSignature(String name, List<String> params) {
        return name + LEFT_BRACKET + params.stream().collect(Collectors.joining(",")) + RIGHT_BRACKET;
    }

    public static List<String> splitParts(String value) {
        return splitParts(value, Parser::isSeparator, Parser::isStringLiteralSeparator);
    }

    /**
     * 
     * @param value
     * @return the parts
     */
    public static List<String> splitTypeInfoParts(String value) {
        return splitParts(value, Parser::isSeparator, new Predicate<Character>() {

            @Override
            public boolean test(Character t) {
                return t == TYPE_INFO_SEPARATOR;
            }
        }.or(Parser::isStringLiteralSeparator));
    }

    public static List<String> splitParts(String value, Predicate<Character> separatorPredicate,
            Predicate<Character> literalSeparatorPredicate) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        boolean literal = false;
        boolean separator = false;
        byte infix = 0;
        byte brackets = 0;
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (separatorPredicate.test(c)) {
                // Adjacent separators are ignored
                if (!separator) {
                    if (!literal && brackets == 0) {
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
                if (literalSeparatorPredicate.test(c)) {
                    literal = !literal;
                }
                // Non-separator char
                if (!literal) {
                    if (brackets == 0 && buffer.length() > 0 && c == ' ') {
                        if (infix == 1) {
                            // The second space after the infix method
                            buffer.append(LEFT_BRACKET);
                            infix++;
                        } else if (infix == 2) {
                            // Next infix method
                            infix = 1;
                            buffer.append(RIGHT_BRACKET);
                            builder.add(buffer.toString());
                            buffer = new StringBuilder();
                        } else {
                            // First space - start infix method
                            infix++;
                            if (buffer.length() > 0) {
                                builder.add(buffer.toString());
                                buffer = new StringBuilder();
                            }
                        }
                    } else {
                        if (Parser.isLeftBracket(c)) {
                            brackets++;
                        } else if (Parser.isRightBracket(c)) {
                            brackets--;
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
        if (infix > 0) {
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
