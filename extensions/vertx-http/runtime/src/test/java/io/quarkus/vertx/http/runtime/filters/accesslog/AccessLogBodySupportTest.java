package io.quarkus.vertx.http.runtime.filters.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

class AccessLogBodySupportTest {

    @Test
    void formatBodyReturnsNullForEmptyBuffer() {
        assertThat(AccessLogBodySupport.formatBody(null, 100)).isNull();
        assertThat(AccessLogBodySupport.formatBody(Buffer.buffer(), 100)).isNull();
    }

    @Test
    void formatBodyReturnsText() {
        assertThat(AccessLogBodySupport.formatBody(Buffer.buffer("hello"), 100)).isEqualTo("hello");
    }

    @Test
    void formatBodyTruncatesLargeBodies() {
        String body = "a".repeat(20);
        assertThat(AccessLogBodySupport.formatBody(Buffer.buffer(body), 10)).isEqualTo("aaaaaaaaaa...(truncated)");
    }

    @Test
    void formatBodyDetectsBinaryContent() {
        assertThat(AccessLogBodySupport.formatBody(Buffer.buffer(new byte[] { 0x00, 0x01, 0x02 }), 100))
                .isEqualTo("<binary, 3 bytes>");
    }

    @Test
    void patternContainsRequestBodyToken() {
        assertThat(AccessLogBodySupport.resolveNamedPattern("common")).doesNotContain(AccessLogBodySupport.REQUEST_BODY_TOKEN);
        assertThat(AccessLogBodySupport.resolveNamedPattern("%{REQUEST_BODY}"))
                .contains(AccessLogBodySupport.REQUEST_BODY_TOKEN);
    }
}
