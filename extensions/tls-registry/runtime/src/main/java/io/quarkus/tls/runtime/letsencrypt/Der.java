package io.quarkus.tls.runtime.letsencrypt;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A minimal DER encoder and decoder for the ASN.1 structures of a certificate signing request, a self-signed
 * certificate and an authority key identifier.
 */
final class Der {

    static final int TAG_BOOLEAN = 0x01;
    static final int TAG_INTEGER = 0x02;
    static final int TAG_BIT_STRING = 0x03;
    static final int TAG_OCTET_STRING = 0x04;
    static final int TAG_NULL = 0x05;
    static final int TAG_OID = 0x06;
    static final int TAG_UTF8_STRING = 0x0C;
    static final int TAG_UTC_TIME = 0x17;
    static final int TAG_GENERALIZED_TIME = 0x18;
    static final int TAG_SEQUENCE = 0x30;
    static final int TAG_SET = 0x31;

    private static final DateTimeFormatter UTC_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'");
    private static final DateTimeFormatter GENERALIZED_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'");

    private Der() {
    }

    /**
     * Encode a value with its tag and its length.
     *
     * @param tag the tag byte
     * @param content the encoded content
     * @return the encoded value
     */
    static byte[] tlv(int tag, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length + 6);
        out.write(tag);
        int length = content.length;
        if (length < 0x80) {
            out.write(length);
        } else {
            int bytes = (Integer.SIZE - Integer.numberOfLeadingZeros(length) + 7) / 8;
            out.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) {
                out.write(length >>> (8 * i));
            }
        }
        out.writeBytes(content);
        return out.toByteArray();
    }

    /**
     * Encode a {@code SEQUENCE}.
     *
     * @param elements the encoded elements
     * @return the encoded sequence
     */
    static byte[] sequence(byte[]... elements) {
        return tlv(TAG_SEQUENCE, concat(elements));
    }

    /**
     * Encode a {@code SET} with the elements in the given order.
     *
     * @param elements the encoded elements
     * @return the encoded set
     */
    static byte[] set(byte[]... elements) {
        return tlv(TAG_SET, concat(elements));
    }

    /**
     * Encode an {@code INTEGER}.
     *
     * @param value the value
     * @return the encoded integer
     */
    static byte[] integer(BigInteger value) {
        return tlv(TAG_INTEGER, value.toByteArray());
    }

    /**
     * Encode an {@code OBJECT IDENTIFIER}.
     *
     * @param dotted the identifier in dotted form, for example {@code 1.2.840.10045.4.3.2}
     * @return the encoded identifier
     */
    static byte[] oid(String dotted) {
        String[] parts = dotted.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            long value = Long.parseLong(parts[i]);
            int groups = Math.max(1, (Long.SIZE - Long.numberOfLeadingZeros(value) + 6) / 7);
            for (int g = groups - 1; g >= 0; g--) {
                int b = (int) ((value >>> (7 * g)) & 0x7F);
                out.write(g == 0 ? b : b | 0x80);
            }
        }
        return tlv(TAG_OID, out.toByteArray());
    }

    /**
     * Encode a {@code UTF8String}.
     *
     * @param value the value
     * @return the encoded string
     */
    static byte[] utf8String(String value) {
        return tlv(TAG_UTF8_STRING, value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encode a {@code BIT STRING} with no unused bits.
     *
     * @param value the bytes
     * @return the encoded bit string
     */
    static byte[] bitString(byte[] value) {
        byte[] content = new byte[value.length + 1];
        System.arraycopy(value, 0, content, 1, value.length);
        return tlv(TAG_BIT_STRING, content);
    }

    /**
     * Encode an {@code OCTET STRING}.
     *
     * @param value the bytes
     * @return the encoded octet string
     */
    static byte[] octetString(byte[] value) {
        return tlv(TAG_OCTET_STRING, value);
    }

    /**
     * Encode a {@code BOOLEAN}.
     *
     * @param value the value
     * @return the encoded boolean
     */
    static byte[] bool(boolean value) {
        return tlv(TAG_BOOLEAN, new byte[] { (byte) (value ? 0xFF : 0x00) });
    }

    /**
     * Encode a time as {@code UTCTime} before 2050 and as {@code GeneralizedTime} after, as RFC 5280 requires.
     *
     * @param time the time
     * @return the encoded time
     */
    static byte[] time(ZonedDateTime time) {
        ZonedDateTime utc = time.withZoneSameInstant(ZoneOffset.UTC).withNano(0);
        if (utc.getYear() < 2050) {
            return tlv(TAG_UTC_TIME, UTC_TIME.format(utc).getBytes(StandardCharsets.US_ASCII));
        }
        return tlv(TAG_GENERALIZED_TIME, GENERALIZED_TIME.format(utc).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Encode a constructed value with an explicit context-specific tag, for example {@code [0]}.
     *
     * @param number the tag number
     * @param content the encoded inner value
     * @return the encoded value
     */
    static byte[] explicit(int number, byte[] content) {
        return tlv(0xA0 | number, content);
    }

    /**
     * Encode a primitive value with an implicit context-specific tag, for example the {@code dNSName [2]}
     * of a {@code GeneralName}.
     *
     * @param number the tag number
     * @param content the raw content
     * @return the encoded value
     */
    static byte[] implicitPrimitive(int number, byte[] content) {
        return tlv(0x80 | number, content);
    }

    /**
     * Concatenate encoded values.
     *
     * @param elements the encoded values
     * @return the concatenation
     */
    static byte[] concat(byte[]... elements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] element : elements) {
            out.writeBytes(element);
        }
        return out.toByteArray();
    }

    /**
     * A decoded value: its tag and the position of its content in the input.
     *
     * @param tag the tag byte
     * @param input the input
     * @param contentStart the index of the first content byte
     * @param contentEnd the index after the last content byte
     */
    record Element(int tag, byte[] input, int contentStart, int contentEnd) {

        /**
         * Get a copy of the content.
         *
         * @return the content bytes
         */
        byte[] content() {
            byte[] result = new byte[contentEnd - contentStart];
            System.arraycopy(input, contentStart, result, 0, result.length);
            return result;
        }

        /**
         * Decode the first element inside this constructed value.
         *
         * @return the first child, or {@code null} if the value is empty
         */
        Element firstChild() {
            return contentStart < contentEnd ? read(input, contentStart) : null;
        }

        /**
         * Decode the element that follows this one in the same input.
         *
         * @param parentEnd the end of the enclosing value
         * @return the next element, or {@code null} at the end of the enclosing value
         */
        Element next(int parentEnd) {
            return contentEnd < parentEnd ? read(input, contentEnd) : null;
        }
    }

    /**
     * Decode the element at the given position.
     *
     * @param input the input
     * @param offset the position of the tag byte
     * @return the decoded element
     * @throws IllegalArgumentException if the input is not valid DER
     */
    static Element read(byte[] input, int offset) {
        if (offset + 2 > input.length) {
            throw new IllegalArgumentException("Truncated DER value");
        }
        int tag = input[offset] & 0xFF;
        int first = input[offset + 1] & 0xFF;
        int position = offset + 2;
        int length;
        if (first < 0x80) {
            length = first;
        } else {
            int bytes = first & 0x7F;
            if (bytes == 0 || bytes > 4 || position + bytes > input.length) {
                throw new IllegalArgumentException("Unsupported DER length");
            }
            length = 0;
            for (int i = 0; i < bytes; i++) {
                length = (length << 8) | (input[position++] & 0xFF);
            }
        }
        if (length < 0 || position + length > input.length) {
            throw new IllegalArgumentException("Truncated DER value");
        }
        return new Element(tag, input, position, position + length);
    }
}
