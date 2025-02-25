package io.quarkus.qute;

import java.util.Arrays;
import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public final class JsonEscaper implements ResultMapper {

    private static final int LENGTH_BITS_OFFSET = 24;
    private static final int SECOND_CHAR_OFFSET = 8;
    private static final int MAX_LATIN_CHAR = 255;
    private static final int NO_REPLACEMENT_DATA = packReplacementData(0, 0, 1);
    private static final int[] REPLACEMENTS_DATA = createReplacementData();

    /**
     * Packs the replacement data into a single int.<br>
     * The replacement data is packed as follows:<br>
     * write an ASCII art of the int:<br>
     * The visual order chosen reflect what Integer::toHexString would print since Java ints are stored big-endian.<br>
     *
     * <pre>
     *         |----------|-----------|-------------|------------|
     *  bits   |   24-31  |   16-23   |    8-15     |    0-7     |
     *  field  |  length  |  padding  |   2nd char  |  1st char  |
     *  values |  {1,2,6} |    [0]    |   [0-255]   |   [0-255]  |
     *         |----------|-----------|-------------|------------|
     * </pre>
     *
     */
    private static int packReplacementData(int first, int second, int length) {
        if (length != 1 && length != 2 && length != 6) {
            throw new IllegalArgumentException("Length must be 1, 2 or 6 but was: " + length);
        }
        if (first < 0 || first > 255) {
            throw new IllegalArgumentException("First char must be in range [0, 255] but was: " + first);
        }
        if (second < 0 || second > 255) {
            throw new IllegalArgumentException("Second char must be in range [0, 255] but was: " + second);
        }
        return (first | (second << SECOND_CHAR_OFFSET)) | (length << LENGTH_BITS_OFFSET);
    }

    private static int replacementLength(int replacementData) {
        // length isn't bigger than 127, which means preserving sign (which is faster) won't affect the shift
        return replacementData >> LENGTH_BITS_OFFSET;
    }

    private static char secondChar(int replacementData) {
        // since past the second char we have padding === 0 we can just cast to char
        return (char) (replacementData >> SECOND_CHAR_OFFSET);
    }

    private static char firstChar(int replacementData) {
        // we need to filter the first byte
        return (char) (replacementData & 0xFF);
    }

    private static int toLatinChar(int c) {
        return c & 0xFF;
    }

    private static int replacementDataOf(char c) {
        // NOTE: char type cannot be negative
        // Both non latin and latin char with length 1 doesn't need replacement
        if (c > MAX_LATIN_CHAR) {
            return NO_REPLACEMENT_DATA;
        }
        return REPLACEMENTS_DATA[toLatinChar(c)];
    }

    private static void writeReplacementData(char[] out, int pos, int replacementData) {
        out[pos] = firstChar(replacementData);
        out[pos + 1] = secondChar(replacementData);
    }

    /**
     * All Unicode characters may be placed within the quotation marks,
     * except for the characters that MUST be escaped: quotation mark,
     * reverse solidus, and the control characters (U+0000 through U+001F).
     * See also https://datatracker.ietf.org/doc/html/rfc8259#autoid-10
     */
    private static int[] createReplacementData() {
        int[] table = new int[256];
        // by default ctrl ASCII chars replace 6 chars
        Arrays.fill(table, 0, 32, packReplacementData(0, 0, 6));
        // default Latin chars just replace themselves
        for (int i = 32; i < 256; i++) {
            table[i] = packReplacementData(i, 0, 1);
        }
        // special ASCII chars - which include some control chars: replace 2 chars
        table['"'] = packReplacementData('\\', '"', 2);
        table['\\'] = packReplacementData('\\', '\\', 2);
        table['\r'] = packReplacementData('\\', 'r', 2);
        table['\b'] = packReplacementData('\\', 'b', 2);
        table['\n'] = packReplacementData('\\', 'n', 2);
        table['\t'] = packReplacementData('\\', 't', 2);
        table['\f'] = packReplacementData('\\', 'f', 2);
        table['/'] = packReplacementData('\\', '/', 2);
        return table;
    }

    // This is a cache for the control chars replacements, which are [0-31]
    private static final char[][] CTRL_REPLACEMENTS = new char[32][];

    private static char[] doEscapeCtrl(int c) {
        var replacement = CTRL_REPLACEMENTS[c];
        if (replacement == null) {
            replacement = String.format("\\u%04x", c).toCharArray();
            assert replacement.length == 6;
            CTRL_REPLACEMENTS[c] = replacement;
        }
        return replacement;
    }

    static String escape(String toEscape) {
        for (int i = 0; i < toEscape.length(); i++) {
            char c = toEscape.charAt(i);
            int replacementLength = replacementLength(replacementDataOf(c));
            if (replacementLength > 1) {
                return doEscape(toEscape, i, replacementLength);
            }
        }
        return toEscape;
    }

    private static String doEscape(String value, int firstToReplace, int firstReplacementLength) {
        assert firstReplacementLength > 1;
        int remainingChars = (value.length() - firstToReplace);
        assert remainingChars >= 1;
        // assume we want to replace all remaining chars with 2 chars
        char[] buffer = new char[firstToReplace + firstReplacementLength + ((remainingChars - 1) * 2)];
        value.getChars(0, firstToReplace, buffer, 0);
        int outputLength = firstToReplace;
        for (int i = 0; i < remainingChars; i++) {
            char c = value.charAt(firstToReplace + i);
            if (c <= MAX_LATIN_CHAR) {
                int latinChar = toLatinChar(c);
                int replacementData = REPLACEMENTS_DATA[latinChar];
                int replacementLength = replacementLength(replacementData);
                if (replacementLength == 6) {
                    var ctrlEscape = doEscapeCtrl(c);
                    buffer = ensureCapacity(buffer, outputLength, 6, (remainingChars - i) - 1);
                    System.arraycopy(ctrlEscape, 0, buffer, outputLength, ctrlEscape.length);
                    outputLength += 6;
                } else {
                    assert replacementLength == 1 || replacementLength == 2;
                    buffer = ensureCapacity(buffer, outputLength, 2, (remainingChars - i) - 1);
                    writeReplacementData(buffer, outputLength, replacementData);
                    outputLength += replacementLength;
                }
            } else {
                buffer = ensureCapacity(buffer, outputLength, 1, (remainingChars - i) - 1);
                buffer[outputLength++] = c;
            }
        }
        return new String(buffer, 0, outputLength);
    }

    private static char[] ensureCapacity(char[] buffer, int currentLength, int additionalLength, int remainingChars) {
        if (currentLength + additionalLength > buffer.length) {
            assert remainingChars >= 0;
            return enlargeBuffer(buffer, currentLength, additionalLength, remainingChars);
        }
        return buffer;
    }

    private static char[] enlargeBuffer(char[] buffer, int currentLength, int additionalLength, int remainingChars) {
        int newLength = currentLength + additionalLength + (remainingChars * 2);
        char[] newBuffer = new char[newLength];
        System.arraycopy(buffer, 0, newBuffer, 0, currentLength);
        return newBuffer;
    }

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        if (result instanceof RawString) {
            return false;
        }
        Optional<Variant> variant = origin.getVariant();
        if (variant.isPresent()) {
            String contentType = variant.get().getContentType();
            if (contentType != null) {
                return contentType.startsWith(Variant.APPLICATION_JSON);
            }
        }
        return false;
    }

    @Override
    public String map(Object result, Expression expression) {
        return escape(result.toString());
    }
}