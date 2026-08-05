package io.quarkus.micrometer.runtime.binder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Tag;

/**
 * Test tag creation
 */
public class HttpCommonTagsTest {

    @Test
    public void testStandardMethodsAreAllowed() {
        Assertions.assertEquals(Tag.of("method", "GET"), HttpCommonTags.method("GET"));
        Assertions.assertEquals(Tag.of("method", "HEAD"), HttpCommonTags.method("HEAD"));
        Assertions.assertEquals(Tag.of("method", "POST"), HttpCommonTags.method("POST"));
        Assertions.assertEquals(Tag.of("method", "PUT"), HttpCommonTags.method("PUT"));
        Assertions.assertEquals(Tag.of("method", "DELETE"), HttpCommonTags.method("DELETE"));
        Assertions.assertEquals(Tag.of("method", "CONNECT"), HttpCommonTags.method("CONNECT"));
        Assertions.assertEquals(Tag.of("method", "OPTIONS"), HttpCommonTags.method("OPTIONS"));
        Assertions.assertEquals(Tag.of("method", "TRACE"), HttpCommonTags.method("TRACE"));
        Assertions.assertEquals(Tag.of("method", "PATCH"), HttpCommonTags.method("PATCH"));
        Assertions.assertEquals(Tag.of("method", "QUERY"), HttpCommonTags.method("QUERY"));
    }

    @Test
    public void testNonStandardMethodsAreRejected() {
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method(null));
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("WELL"));
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("FOOBAR"));
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("get"));
    }

    @Test
    public void testAdditionalMethodsAreAllowed() {
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("PROPFIND"));

        HttpCommonTags.setAdditionalHttpMethods(List.of("PROPFIND", "MKCOL"));

        Assertions.assertEquals(Tag.of("method", "PROPFIND"), HttpCommonTags.method("PROPFIND"));
        Assertions.assertEquals(Tag.of("method", "MKCOL"), HttpCommonTags.method("MKCOL"));
        Assertions.assertEquals(Tag.of("method", "GET"), HttpCommonTags.method("GET"));
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("FOOBAR"));
    }

    @Test
    public void testAdditionalMethodsCappedAt32() {
        // 10 standard methods + 23 additional = 33, exceeding the cap of 32
        List<String> methods = new ArrayList<>(23);
        for (int i = 1; i <= 23; i++) {
            methods.add("CUSTOM" + i);
        }
        HttpCommonTags.setAdditionalHttpMethods(methods);

        // CUSTOM1 fits within the 32 cap
        Assertions.assertEquals(Tag.of("method", "CUSTOM1"), HttpCommonTags.method("CUSTOM1"));
        // CUSTOM23 is the 33rd method and should be rejected
        Assertions.assertEquals(HttpCommonTags.METHOD_UNKNOWN, HttpCommonTags.method("CUSTOM23"));
    }

    @Test
    public void testStatus() {
        Assertions.assertEquals(Tag.of("status", "200"), HttpCommonTags.status(200));
        Assertions.assertEquals(Tag.of("status", "301"), HttpCommonTags.status(301));
        Assertions.assertEquals(Tag.of("status", "304"), HttpCommonTags.status(304));
        Assertions.assertEquals(Tag.of("status", "404"), HttpCommonTags.status(404));
    }

    @Test
    public void testUriRedirect() {
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 301, false));
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 302, false));
        Assertions.assertEquals(HttpCommonTags.URI_REDIRECTION, HttpCommonTags.uri("/moved", null, 304, false));
        Assertions.assertEquals(Tag.of("uri", "/moved/{id}"), HttpCommonTags.uri("/moved/{id}", "/moved/111", 304, false));
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 304, false));
    }

    @Test
    public void testUriDefaults() {
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 200, false));
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 404, false));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), HttpCommonTags.uri("/known/ok", null, 200, false));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", null, 404, false));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", "/invalid", 404, false));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid/", "/invalid/", 404, false));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", "/invalid/", 404, false));
        Assertions.assertEquals(Tag.of("uri", "/invalid/{id}"),
                HttpCommonTags.uri("/invalid/{id}", "/invalid/111", 404, false));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/request"),
                HttpCommonTags.uri("/known/bad/request", null, 400, false));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/{request}"),
                HttpCommonTags.uri("/known/bad/{request}", "/known/bad/request", 400, false));
        Assertions.assertEquals(Tag.of("uri", "/known/server/error"),
                HttpCommonTags.uri("/known/server/error", null, 500, false));
    }

    @Test
    public void testUriDefaultsWithSuppression() {
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 200, true));
        Assertions.assertEquals(HttpCommonTags.URI_ROOT, HttpCommonTags.uri("/", null, 404, true));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), HttpCommonTags.uri("/known/ok", null, 200, true));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", null, 404, true));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid/", null, 404, true));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", "/invalid", 404, true));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid/", "/invalid/", 404, true));
        Assertions.assertEquals(HttpCommonTags.URI_NOT_FOUND, HttpCommonTags.uri("/invalid", "/invalid/", 404, true));
        Assertions.assertEquals(Tag.of("uri", "/invalid/{id}"), HttpCommonTags.uri("/invalid/{id}", "/invalid/111", 404, true));
        Assertions.assertEquals(HttpCommonTags.URI_UNKNOWN, HttpCommonTags.uri("/known/bad/request", null, 400, true));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/{request}"),
                HttpCommonTags.uri("/known/bad/{request}", "/known/bad/request", 400, true));
        Assertions.assertEquals(HttpCommonTags.URI_UNKNOWN, HttpCommonTags.uri("/known/server/error", null, 500, true));
    }
}
