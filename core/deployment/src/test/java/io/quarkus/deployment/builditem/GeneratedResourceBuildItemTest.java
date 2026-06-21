package io.quarkus.deployment.builditem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class GeneratedResourceBuildItemTest {

    @Test
    void throwsForServiceProviderPath() {
        assertThatThrownBy(
                () -> new GeneratedResourceBuildItem("META-INF/services/com.example.Foo", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("META-INF/services/com.example.Foo");
    }

    @Test
    void throwsForServiceProviderPathWithDeprecatedConstructor() {
        assertThatThrownBy(
                () -> new GeneratedResourceBuildItem("META-INF/services/com.example.Foo", new byte[0], false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("META-INF/services/com.example.Foo");
    }

    @Test
    void acceptsNormalPath() {
        assertDoesNotThrow(
                () -> new GeneratedResourceBuildItem("META-INF/native-image/resource-config.json", new byte[0]));
    }

    @Test
    void allowsMetaInfServicesWhenExplicitlyEnabled() {
        assertDoesNotThrow(
                () -> GeneratedResourceBuildItem.allowingMetaInfServices(
                        "META-INF/services/org.apache.cxf.bus.factory", new byte[0]));
    }

    @Test
    void escapeHatchWorksForNormalPaths() {
        assertDoesNotThrow(
                () -> GeneratedResourceBuildItem.allowingMetaInfServices(
                        "META-INF/native-image/resource-config.json", new byte[0]));
    }
}
