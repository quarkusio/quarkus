package io.quarkus.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.json.JsonArray;
import io.quarkus.builder.json.JsonBoolean;
import io.quarkus.builder.json.JsonDouble;
import io.quarkus.builder.json.JsonInteger;
import io.quarkus.builder.json.JsonNull;
import io.quarkus.builder.json.JsonObject;
import io.quarkus.builder.json.JsonString;
import io.quarkus.builder.json.JsonValue;

/**
 * A json format reader.
 * It follows the <a href="https://www.json.org/json-en.html">ECMA-404 The JSON Data Interchange Standard.</a>.
 */
public class JsonReader {

    private final String text;
    private final int length;
    private int position;

    private JsonReader(String text) {
        this.text = text;
        this.length = text.length();
    }

    public static JsonReader of(String source) {
        return new JsonReader(source);
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T read() {
        return (T) readElement();
    }

    /**
     * element
     * |---- ws value ws
     */
    private JsonValue readElement() {
        ignoreWhitespace();
        JsonValue result = readValue();
        ignoreWhitespace();
        return result;
    }

    /**
     * value
     * |---- object
     * |---- array
     * |---- string
     * |---- number
     * |---- "true"
     * |---- "false"
     * |---- "null"
     */
    private JsonValue readValue() {
        final int ch = peekChar();
        if (ch < 0) {
            throw new IllegalArgumentException("Unable to fully read json value");
        }

        switch (ch) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                return readConstant("true", JsonBoolean.TRUE);
            case 'f':
                return readConstant("false", JsonBoolean.FALSE);
            case 'n':
                return readConstant("null", JsonNull.INSTANCE);
            default:
                if (Character.isDigit(ch) || '-' == ch) {
                    return readNumber(position);
                }
                throw new IllegalArgumentException("Unknown start character for json value: " + ch);
        }
    }

    /**
     * object
     * |---- '{' ws '}'
     * |---- '{' members '}'
     * </p>
     * members
     * |----- member
     * |----- member ',' members
     */
    private JsonValue readObject() {
        position++;

        Map<JsonString, JsonValue> members = new HashMap<>();

        while (position < length) {
            ignoreWhitespace();
            switch (peekChar()) {
                case '}':
                    position++;
                    return new JsonObject(members);
                case ',':
                    position++;
                    break;
                case '"':
                    readMember(members);
                    break;
            }
        }

        throw new IllegalArgumentException("Json object ended without }");
    }

    /**
     * member
     * |----- ws string ws ':' element
     */
    private void readMember(Map<JsonString, JsonValue> members) {
        final JsonString attribute = readString();
        ignoreWhitespace();
        final int colon = nextChar();
        if (':' != colon) {
            throw new IllegalArgumentException("Expected : after attribute");
        }
        final JsonValue element = readElement();
        members.put(attribute, element);
    }

    /**
     * array
     * |---- '[' ws ']'
     * |---- '[' elements ']'
     * </p>
     * elements
     * |----- element
     * |----- element ',' elements
     */
    private JsonValue readArray() {
        position++;

        final List<JsonValue> elements = new ArrayList<>();

        while (position < length) {
            ignoreWhitespace();
            switch (peekChar()) {
                case ']':
                    position++;
                    return new JsonArray(elements);
                case ',':
                    position++;
                    break;
                default:
                    elements.add(readElement());
                    break;
            }
        }

        throw new IllegalArgumentException("Json array ended without ]");
    }

    /**
     * string
     * |---- '"' characters '"'
     * </p>
     * characters
     * |----- ""
     * |----- character characters
     * </p>
     * character
     * |----- '0020' . '10FFFF' - '"' - '\'
     * |----- '\' escape
     * |----- escape
     * |----- '"'
     * |----- '\'
     * |----- '/'
     * |----- 'b'
     * |----- 'f'
     * |----- 'n'
     * |----- 'r'
     * |----- 't'
     * |----- 'u' hex hex hex hex
     */
    private JsonString readString() {
        position++;

        int start = position;
        // Substring on string values that contain unicode characters won't work,
        // because there are more characters read than actual characters represented.
        // Use StringBuilder to buffer any string read up to unicode,
        // then add unicode values into it and continue as usual.
        StringBuilder unicodeString = null;

        while (position < length) {
            final int ch = nextChar();

            if (Character.isISOControl(ch)) {
                throw new IllegalArgumentException("Control characters not allowed in json string");
            }

            if ('"' == ch) {
                final String chunk = text.substring(start, position - 1);
                final String result = unicodeString != null
                        ? unicodeString.append(chunk).toString()
                        : chunk;

                // End of string
                return new JsonString(result);
            }

            if ('\\' == ch) {
                switch (nextChar()) {
                    case '"': // quotation mark
                    case '\\': // reverse solidus
                    case '/': // solidus
                    case 'b': // backspace
                    case 'f': // formfeed
                    case 'n': // linefeed
                    case 'r': // carriage return
                    case 't': // horizontal tab
                        break;
                    case 'u': // unicode
                        if (unicodeString == null) {
                            unicodeString = new StringBuilder(position - start);
                        }
                        unicodeString.append(text, start, position - 1);
                        unicodeString.append(readUnicode());
                        start = position;
                }
            }
        }

        throw new IllegalArgumentException("String not closed");
    }

    private char readUnicode() {
        final char digit1 = Character.forDigit(nextChar(), 16);
        final char digit2 = Character.forDigit(nextChar(), 16);
        final char digit3 = Character.forDigit(nextChar(), 16);
        final char digit4 = Character.forDigit(nextChar(), 16);
        return (char) (digit1 << 12 | digit2 << 8 | digit3 << 4 | digit4);
    }

    /**
     * number
     * |---- integer fraction exponent
     */
    private JsonValue readNumber(int numStartIndex) {
        final boolean isFraction = skipToEndOfNumber();
        final String number = text.substring(numStartIndex, position);
        return isFraction
                ? new JsonDouble(Double.parseDouble(number))
                : new JsonInteger(Long.parseLong(number));
    }

    private boolean skipToEndOfNumber() {
        // Find the end of a number then parse with library methods
        int ch = nextChar();
        if ('-' == ch) {
            ch = nextChar();
        }

        if (Character.isDigit(ch) && '0' != ch) {
            ignoreDigits();
        }

        boolean isFraction = false;
        ch = peekChar();
        if ('.' == ch) {
            isFraction = true;
            position++;
            ignoreDigits();
        }

        ch = peekChar();
        switch (ch) {
            case 'e':
            case 'E':
                position++;
                ch = nextChar();
                switch (ch) {
                    case '-':
                    case '+':
                        position++;
                }
                ignoreDigits();
        }

        return isFraction;
    }

    private void ignoreDigits() {
        while (position < length) {
            final int ch = peekChar();
            if (!Character.isDigit(ch)) {
                break;
            }
            position++;
        }
    }

    private JsonValue readConstant(String expected, JsonValue result) {
        if (text.regionMatches(position, expected, 0, expected.length())) {
            position += expected.length();
            return result;
        }
        throw new IllegalArgumentException("Unable to read json constant for: " + expected);
    }

    /**
     * ws
     * |---- ""
     * |---- '0020' ws
     * |---- '000A' ws
     * |---- '000D' ws
     * |---- '0009' ws
     */
    private void ignoreWhitespace() {
        while (position < length) {
            final int ch = peekChar();
            switch (ch) {
                case ' ': // '0020' SPACE
                case '\n': // '000A' LINE FEED
                case '\r': // '000D' CARRIAGE RETURN
                case '\t': // '0009' CHARACTER TABULATION
                    position++;
                    break;
                default:
                    return;
            }
        }
    }

    private int peekChar() {
        return position < length
                ? text.charAt(position)
                : -1;
    }

    private int nextChar() {
        final int ch = peekChar();
        position++;
        return ch;
    }
}
