package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NativeConfigTest {

    @Test
    public void testBuilderImageProperlyDetected() {
        assertThat(createConfig("graalvm").getEffectiveBuilderImage()).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GraalVM").getEffectiveBuilderImage()).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GraalVM").getEffectiveBuilderImage()).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GRAALVM").getEffectiveBuilderImage()).contains("ubi-quarkus-native-image")
                .contains("java17");

        assertThat(createConfig("mandrel").getEffectiveBuilderImage()).contains("ubi-quarkus-mandrel").contains("java17");
        assertThat(createConfig("Mandrel").getEffectiveBuilderImage()).contains("ubi-quarkus-mandrel").contains("java17");
        assertThat(createConfig("MANDREL").getEffectiveBuilderImage()).contains("ubi-quarkus-mandrel").contains("java17");

        assertThat(createConfig("aRandomString").getEffectiveBuilderImage()).isEqualTo("aRandomString");
    }

    private NativeConfig createConfig(String configValue) {
        NativeConfig nativeConfig = new NativeConfig();
        nativeConfig.builderImage = configValue;
        return nativeConfig;
    }
}
