package io.quarkus.micrometer.runtime.binder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Tag;

/**
 * Test tag creation
 * Disabled on Java 8 because of Mocks
 */
public class HttpCommonTagsTest {

    @Test
    public void testStatus() {
        Assertions.assertEquals(Tag.of("status", "200"), HttpCommonTags.status(200));
        Assertions.assertEquals(Tag.of("status", "301"), HttpCommonTags.status(301));
        Assertions.assertEquals(Tag.of("status", "304"), HttpCommonTags.status(304));
        Assertions.assertEquals(Tag.of("status", "404"), HttpCommonTags.status(404));
    }

    @Test
    public void testUriRedirect() {
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 301));
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 302));
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 304));
        Assertions.assertEquals(Tag.of("uri", "/moved/{id}"), HttpCommonTags.uri("/moved/{id}", "/moved/111", 304));
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 304));
    }

    @Test
    public void testUriDefaults() {
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 200));
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 404));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), HttpCommonTags.uri("/known/ok", null, 200));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", null, 404));
        Assertions.assertEquals(Tag.of("uri", "/invalid/{id}"), HttpCommonTags.uri("/invalid/{id}", "/invalid/111", 404));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/request"), HttpCommonTags.uri("/known/bad/request", null, 400));
        Assertions.assertEquals(Tag.of("uri", "/known/server/error"), HttpCommonTags.uri("/known/server/error", null, 500));
    }
}
