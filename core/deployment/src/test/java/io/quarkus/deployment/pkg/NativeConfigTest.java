package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NativeConfigTest {

    @Test
    public void testBuilderImageProperlyDetected() {
        assertThat(createConfig("graalvm").getEffectiveBuilderImage(false)).contains("ubi-quarkus-native-image")
                .contains("java11");
        assertThat(createConfig("graalvm").getEffectiveBuilderImage(true)).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GraalVM").getEffectiveBuilderImage(true)).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GraalVM").getEffectiveBuilderImage(true)).contains("ubi-quarkus-native-image")
                .contains("java17");
        assertThat(createConfig("GRAALVM").getEffectiveBuilderImage(false)).contains("ubi-quarkus-native-image")
                .contains("java11");
        assertThat(createConfig("GRAALVM").getEffectiveBuilderImage(true)).contains("ubi-quarkus-native-image")
                .contains("java17");

        assertThat(createConfig("mandrel").getEffectiveBuilderImage(false)).contains("ubi-quarkus-mandrel").contains("java11");
        assertThat(createConfig("mandrel").getEffectiveBuilderImage(true)).contains("ubi-quarkus-mandrel").contains("java17");
        assertThat(createConfig("Mandrel").getEffectiveBuilderImage(false)).contains("ubi-quarkus-mandrel").contains("java11");
        assertThat(createConfig("Mandrel").getEffectiveBuilderImage(true)).contains("ubi-quarkus-mandrel").contains("java17");
        assertThat(createConfig("MANDREL").getEffectiveBuilderImage(false)).contains("ubi-quarkus-mandrel").contains("java11");
        assertThat(createConfig("MANDREL").getEffectiveBuilderImage(true)).contains("ubi-quarkus-mandrel").contains("java17");

        assertThat(createConfig("aRandomString").getEffectiveBuilderImage(false)).isEqualTo("aRandomString");
    }

    private NativeConfig createConfig(String configValue) {
        NativeConfig nativeConfig = new NativeConfig();
        nativeConfig.builderImage = configValue;
        return nativeConfig;
    }
}
