package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class GeneratedResourceBuildItemTest {

    @Test
    void acceptsServiceProviderPath() {
        assertDoesNotThrow(
                () -> new GeneratedResourceBuildItem("META-INF/services/com.example.Foo", new byte[0]));
    }

    @Test
    void acceptsNormalPath() {
        assertDoesNotThrow(
                () -> new GeneratedResourceBuildItem("META-INF/native-image/resource-config.json", new byte[0]));
    }
}
