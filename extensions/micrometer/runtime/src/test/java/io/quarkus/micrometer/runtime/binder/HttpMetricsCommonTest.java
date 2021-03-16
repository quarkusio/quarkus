package io.quarkus.micrometer.runtime.binder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Tag;

/**
 * Test tag creation
 * Disabled on Java 8 because of Mocks
 */
public class HttpMetricsCommonTest {

    @Test
    public void testStatus() {
        Assertions.assertEquals(Tag.of("status", "200"), HttpMetricsCommon.status(200));
        Assertions.assertEquals(Tag.of("status", "301"), HttpMetricsCommon.status(301));
        Assertions.assertEquals(Tag.of("status", "304"), HttpMetricsCommon.status(304));
        Assertions.assertEquals(Tag.of("status", "404"), HttpMetricsCommon.status(404));
    }

    @Test
    public void testUriRedirect() {
        Assertions.assertEquals(HttpMetricsCommon.URI_REDIRECTION, HttpMetricsCommon.uri("/moved", 301));
        Assertions.assertEquals(HttpMetricsCommon.URI_REDIRECTION, HttpMetricsCommon.uri("/moved", 302));
        Assertions.assertEquals(HttpMetricsCommon.URI_REDIRECTION, HttpMetricsCommon.uri("/moved", 304));
    }

    @Test
    public void testUriDefaults() {
        Assertions.assertEquals(HttpMetricsCommon.URI_ROOT, HttpMetricsCommon.uri("/", 200));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), HttpMetricsCommon.uri("/known/ok", 200));
        Assertions.assertEquals(HttpMetricsCommon.URI_NOT_FOUND, HttpMetricsCommon.uri("/invalid", 404));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/request"), HttpMetricsCommon.uri("/known/bad/request", 400));
        Assertions.assertEquals(Tag.of("uri", "/known/server/error"), HttpMetricsCommon.uri("/known/server/error", 500));
    }
}
