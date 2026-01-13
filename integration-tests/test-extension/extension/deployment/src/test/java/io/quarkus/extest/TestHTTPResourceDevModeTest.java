package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;

class TestHTTPResourceDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @TestHTTPResource
    URI uri;
    @TestHTTPResource(value = "foo/bar")
    URI uriPath;

    @Test
    void httpResource() {
        assertEquals("localhost", uri.getHost());
        assertEquals(8080, uri.getPort());
        assertEquals("/", uri.getPath());
        assertEquals("/foo/bar", uriPath.getPath());
    }
}
