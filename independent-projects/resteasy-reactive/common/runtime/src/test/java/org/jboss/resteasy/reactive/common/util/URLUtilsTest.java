package org.jboss.resteasy.reactive.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class URLUtilsTest {
    @Test
    void decodeInvalidPercentEncoding() {
        String incomplete = "invalid%2";
        String invalidHex = "invalid%zz";

        assertThrows(IllegalArgumentException.class,
                () -> URLUtils.decode(incomplete, StandardCharsets.UTF_8, true, new StringBuilder()));
        assertThrows(IllegalArgumentException.class,
                () -> URLUtils.decode(invalidHex, StandardCharsets.UTF_8, true, new StringBuilder()));
    }

    @Test
    void decodeGrayAreaInvalidUtf8() {
        String invalidUtf8 = "invalid%80";

        // This is a gray area: %80 is not valid in UTF-8 as a standalone byte,
        // but Java's default decoding behavior does not throw an exception.
        // Instead, it replaces it with a special character (�).
        //
        // To enforce strict decoding, CharsetDecoder with CodingErrorAction.REPORT
        // should be used inside URLUtils.decode.
        String decoded = URLUtils.decode(invalidUtf8, StandardCharsets.UTF_8, true, new StringBuilder());

        assertEquals("invalid�", decoded); // Note: This may vary depending on the JVM.
    }

    @Test
    void decodeValidValues() {
        String path = "test%20path";
        String formEncoded = "test+path";
        String japanese = "%E3%83%86%E3%82%B9%E3%83%88"; // テスト

        assertEquals("test path",
                URLUtils.decode(path, StandardCharsets.UTF_8, true, new StringBuilder()));
        assertEquals("test path",
                URLUtils.decode(formEncoded, StandardCharsets.UTF_8, true, true, new StringBuilder()));
        assertEquals("テスト",
                URLUtils.decode(japanese, StandardCharsets.UTF_8, true, new StringBuilder()));
    }
}
