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

    String escape(CharSequence value) {
        if (value.length() == 0) {
            return value.toString();
        }
        for (int i = 0; i < value.length(); i++) {
            String replacement = replacementFor(value.charAt(i));
            if (replacement != null) {
                // In most cases we will not need to escape the value at all
                return doEscape(value, i, new StringBuilder(value.subSequence(0, i)).append(replacement));
            }
        }
        return value.toString();
    }

    private String doEscape(CharSequence value, int index, StringBuilder builder) {
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
