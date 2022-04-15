package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NativeConfigTest {

    @Test
    public void testBuilderImageProperlyDetected() {
        NativeConfig nativeConfig = new NativeConfig();
        nativeConfig.builderImage = "graalvm";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-native-image")).isTrue();
        nativeConfig.builderImage = "GraalVM";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-native-image")).isTrue();
        nativeConfig.builderImage = "GRAALVM";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-native-image")).isTrue();
        nativeConfig.builderImage = "mandrel";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-mandrel")).isTrue();
        nativeConfig.builderImage = "Mandrel";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-mandrel")).isTrue();
        nativeConfig.builderImage = "MANDREL";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("ubi-quarkus-mandrel")).isTrue();
        nativeConfig.builderImage = "aRandomString";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("aRandomString")).isTrue();
        nativeConfig.builderImage = "aRandomStr32ng";
        assertThat(nativeConfig.getEffectiveBuilderImage().contains("aRandomString")).isFalse();
    }
}
