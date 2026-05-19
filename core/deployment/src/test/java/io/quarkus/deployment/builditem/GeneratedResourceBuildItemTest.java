package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeneratedResourceBuildItemTest {

    @Test
    void throwsForServiceProviderPath() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedResourceBuildItem("META-INF/services/com.example.Foo", new byte[0]));
    }

    @Test
    void throwsForServiceProviderPathWithDeprecatedConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedResourceBuildItem("META-INF/services/com.example.Foo", new byte[0], false));
    }

    @Test
    void acceptsNormalPath() {
        assertDoesNotThrow(
                () -> new GeneratedResourceBuildItem("META-INF/native-image/resource-config.json", new byte[0]));
    }
}
