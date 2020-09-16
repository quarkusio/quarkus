package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Expressions {

    public static final String TYPECHECK_NAMESPACE_PLACEHOLDER = "$$namespace$$";

    static final String LEFT_BRACKET = "(";
    static final String RIGHT_BRACKET = ")";
    static final String SQUARE_LEFT_BRACKET = "[";
    static final String SQUARE_RIGHT_BRACKET = "]";
    public static final char TYPE_INFO_SEPARATOR = '|';

    private Expressions() {
    }

    public static boolean isVirtualMethod(String value) {
        return value.indexOf(LEFT_BRACKET) != -1;
    }

    public static boolean isBracketNotation(String value) {
        return value.startsWith(SQUARE_LEFT_BRACKET);
    }

    public static String parseVirtualMethodName(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        return value.substring(0, start);
    }

    public static List<String> parseVirtualMethodParams(String value) {
        int start = value.indexOf(LEFT_BRACKET);
        if (start != -1 && value.endsWith(RIGHT_BRACKET)) {
            String params = value.substring(start + 1, value.length() - 1);
            return splitParts(params, PARAMS_SPLIT_CONFIG);
        }
        throw new IllegalArgumentException("Not a virtual method: " + value);
    }

    public static String parseBracketContent(String value) {
        if (value.endsWith(SQUARE_RIGHT_BRACKET)) {
            return value.substring(1, value.length() - 1);
        }
        throw new IllegalArgumentException("Not a bracket notation expression: " + value);
    }

    public static String buildVirtualMethodSignature(String name, List<String> params) {
        return name + LEFT_BRACKET + params.stream().collect(Collectors.joining(",")) + RIGHT_BRACKET;
    }

    public static List<String> splitParts(String value) {
        return splitParts(value, DEFAULT_SPLIT_CONFIG);
    }

    /**
     * 
     * @param value
     * @return the parts
     */
    public static List<String> splitTypeInfoParts(String value) {
        return splitParts(value, TYPE_INFO_SPLIT_CONFIG);
    }

    public static List<String> splitParts(String value, SplitConfig splitConfig) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        boolean literal = false;
        char separator = 0;
        byte infix = 0;
        byte brackets = 0;
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (splitConfig.isSeparator(c)) {
                // Adjacent separators may be ignored
                if (separator == 0 || separator != c) {
                    if (!literal && brackets == 0) {
                        if (splitConfig.shouldPrependSeparator(c)) {
                            buffer.append(c);
                        }
                        if (buffer.length() > 0) {
                            // Flush the part
                            builder.add(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        if (splitConfig.shouldAppendSeparator(c)) {
                            buffer.append(c);
                        }
                        separator = c;
                    } else {
                        buffer.append(c);
                    }
                }
            } else {
                if (splitConfig.isLiteralSeparator(c)) {
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
                    separator = 0;
                } else {
                    buffer.append(c);
                    separator = 0;
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

    private static final SplitConfig DEFAULT_SPLIT_CONFIG = new DefaultSplitConfig();

    private static final SplitConfig PARAMS_SPLIT_CONFIG = new SplitConfig() {

        @Override
        public boolean isSeparator(char candidate) {
            return ',' == candidate;
        }

    };

    private static final SplitConfig TYPE_INFO_SPLIT_CONFIG = new DefaultSplitConfig() {

        @Override
        public boolean isLiteralSeparator(char candidate) {
            return candidate == TYPE_INFO_SEPARATOR || LiteralSupport.isStringLiteralSeparator(candidate);
        }
    };

    private static class DefaultSplitConfig implements SplitConfig {

        @Override
        public boolean isSeparator(char candidate) {
            return candidate == '.' || candidate == '[' || candidate == ']';
        }

        @Override
        public boolean shouldPrependSeparator(char candidate) {
            return candidate == ']';
        }

        @Override
        public boolean shouldAppendSeparator(char candidate) {
            return candidate == '[';
        }

    }

    interface SplitConfig {

        boolean isSeparator(char candidate);

        default boolean isLiteralSeparator(char candidate) {
            return LiteralSupport.isStringLiteralSeparator(candidate);
        }

        default boolean shouldPrependSeparator(char candidate) {
            return false;
        }

        default boolean shouldAppendSeparator(char candidate) {
            return false;
        }

    }

}
