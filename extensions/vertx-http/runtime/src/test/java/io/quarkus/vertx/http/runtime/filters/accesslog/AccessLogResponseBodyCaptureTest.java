package io.quarkus.vertx.http.runtime.filters.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

class AccessLogResponseBodyCaptureTest {

    @Test
    void captureVertxBuffer() {
        AccessLogResponseBodyCapture capture = new AccessLogResponseBodyCapture(4096);
        capture.capture(Buffer.buffer("hello"));
        assertThat(capture.getCapturedBody()).isEqualTo("hello");
    }

    @Test
    void captureStopsAtMaxSize() {
        AccessLogResponseBodyCapture capture = new AccessLogResponseBodyCapture(5);
        capture.capture(Buffer.buffer("hello world"));
        assertThat(capture.getCapturedBody()).isEqualTo("hello");
    }
}
