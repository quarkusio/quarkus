package io.quarkus.qute;

/**
 * Makes it possible to replace chars from Basic Multilingual Plane (BMP).
 *
 * @see Character#isBmpCodePoint(int)
 */
abstract class CharReplacementResultMapper implements ResultMapper {

    @Override
    public String map(Object result, Expression expression) {
        return escape(result.toString());
    }

    String escape(String value) {
        if (value.length() == 0) {
            return value;
        }
        for (int i = 0; i < value.length(); i++) {
            String replacement = replacementFor(value.charAt(i));
            if (replacement != null) {
                var builder = new StringBuilder(replacement.length() + (value.length() - 1));
                builder.append(value, 0, i);
                builder.append(replacement);
                return doEscape(value, i, builder);
            }
        }
        return value.toString();
    }

    private String doEscape(String value, int index, StringBuilder builder) {
        int length = value.length();
        while (++index < length) {
            char c = value.charAt(index);
            String replacement = replacementFor(c);
            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    protected abstract String replacementFor(char c);

}
