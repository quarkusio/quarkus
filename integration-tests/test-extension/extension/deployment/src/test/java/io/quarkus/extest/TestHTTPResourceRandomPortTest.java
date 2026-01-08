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

class TestHTTPResourceRandomPortTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.test-port", "0");

    @TestHTTPResource
    URI uri;
    @Inject
    SmallRyeConfig config;

    @Test
    void randomPort() {
        assertTrue(uri.getPort() > 0);
        assertTrue(uri.getPort() != 8080);
        assertTrue(uri.getPort() != 8081);
    }

    @Test
    void testUrl() {
        ConfigValue testUrl = config.getConfigValue("test.url");
        assertNotNull(testUrl.getValue());
        assertTrue(uri.toString().contains(testUrl.getValue()));
        assertEquals("ValueRegistryConfigSource", testUrl.getConfigSourceName());
    }
}
