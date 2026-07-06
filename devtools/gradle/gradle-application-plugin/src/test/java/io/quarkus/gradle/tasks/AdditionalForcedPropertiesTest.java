package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AdditionalForcedPropertiesTest {

    @Test
    void nativeArgumentsShouldBeNormalizedAndOverriddenByTaskProperties() {
        Map<String, String> result = AdditionalForcedProperties.of(
                Map.of(
                        "containerBuild", "true",
                        "quarkus.native.builderImage", "builder-image",
                        "debug.enabled", "true"),
                Map.of(
                        "quarkus.native.container-build", "false",
                        "quarkus.package.jar.enabled", "false"));

        assertThat(result).containsOnly(
                Map.entry("quarkus.native.container-build", "false"),
                Map.entry("quarkus.native.builder-image", "builder-image"),
                Map.entry("quarkus.native.debug.enabled", "true"),
                Map.entry("quarkus.package.jar.enabled", "false"));
    }
}
