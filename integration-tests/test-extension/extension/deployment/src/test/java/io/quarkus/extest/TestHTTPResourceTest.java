package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

class TestHTTPResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI uri;
    @TestHTTPResource(value = "foo/bar")
    URI uriPath;
    @Inject
    SmallRyeConfig config;

    @Test
    void httpResource() {
        assertEquals("localhost", uri.getHost());
        assertEquals(8081, uri.getPort());
        assertEquals("/", uri.getPath());
        assertEquals("/foo/bar", uriPath.getPath());
    }

    @Test
    void testUrl() {
        ConfigValue testUrl = config.getConfigValue("test.url");
        assertNotNull(testUrl.getValue());
        assertTrue(uri.toString().contains(testUrl.getValue()));
        assertEquals("ValueRegistryConfigSource", testUrl.getConfigSourceName());
    }
}
