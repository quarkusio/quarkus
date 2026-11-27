package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

class TestHTTPResourceRandomPortTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.test-port", "0");

    @TestHTTPResource
    URI uri;

    @Test
    void randomPort() {
        assertTrue(uri.getPort() > 0);
        assertTrue(uri.getPort() != 8080);
        assertTrue(uri.getPort() != 8081);
    }
}
