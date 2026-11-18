package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

class TestHTTPResourceTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest();

    @TestHTTPResource
    URI uri;
    @TestHTTPResource(value = "foo/bar")
    URI uriPath;

    @Test
    void httpResource() {
        assertEquals("localhost", uri.getHost());
        assertEquals(8081, uri.getPort());
        assertEquals("/", uri.getPath());
        assertEquals("/foo/bar", uriPath.getPath());
    }
}
