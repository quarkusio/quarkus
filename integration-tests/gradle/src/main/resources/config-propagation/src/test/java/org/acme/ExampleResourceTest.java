package org.acme;

import io.smallrye.config.ConfigValue;
import jakarta.inject.Inject;
import io.smallrye.config.Config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
class ExampleResourceTest {
    @Inject
    Config config;

    @Test
    void propagateConfig() {
        ConfigValue fromPlugin = Config.get().getConfigValue("quarkus.http.enable-compression");
        assertEquals("true", fromPlugin.getValue());
        assertEquals("BuildTime RunTime Fixed", fromPlugin.getConfigSourceName());

        ConfigValue fromGradleProperties = Config.get().getConfigValue("quarkus.http.enable-decompression");
        assertEquals("true", fromGradleProperties.getValue());
        assertEquals("BuildTime RunTime Fixed", fromGradleProperties.getConfigSourceName());

        ConfigValue fromApplication = Config.get().getConfigValue("quarkus.http.read-timeout");
        assertEquals("30s", fromApplication.getValue());
        assertNotEquals("SysPropConfigSource", fromApplication.getConfigSourceName());
    }
}