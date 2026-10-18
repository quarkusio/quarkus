package org.jboss.resteasy.reactive.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EncodeTest {
    @Test
    void encodeEmoji() {
        String emoji = "\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00";
        String encodedEmoji = URLEncoder.encode(emoji, StandardCharsets.UTF_8);
        assertEquals(encodedEmoji, Encode.encodePath(emoji));
        assertEquals(encodedEmoji, Encode.encodeQueryParam(emoji));
    }

    @Test
    void encodeQuestionMarkQueryParameterValue() {
        String uriQueryValue = "bar?a=b";
        String encoded = URLEncoder.encode(uriQueryValue, StandardCharsets.UTF_8);
        assertEquals(encoded, Encode.encodeQueryParam(uriQueryValue));
    }
}
