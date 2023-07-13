package io.quarkus.deployment.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NativeConfigTest {

    @Test
    public void testBuilderImageProperlyDetected() {
        assertThat(createConfig("graalvm").builderImage().getEffectiveImage()).contains("ubi-quarkus-graalvmce-builder-image")
                .contains("jdk-17");
        assertThat(createConfig("GraalVM").builderImage().getEffectiveImage()).contains("ubi-quarkus-graalvmce-builder-image")
                .contains("jdk-17");
        assertThat(createConfig("GraalVM").builderImage().getEffectiveImage()).contains("ubi-quarkus-graalvmce-builder-image")
                .contains("jdk-17");
        assertThat(createConfig("GRAALVM").builderImage().getEffectiveImage()).contains("ubi-quarkus-graalvmce-builder-image")
                .contains("jdk-17");

        assertThat(createConfig("mandrel").builderImage().getEffectiveImage()).contains("ubi-quarkus-mandrel-builder-image")
                .contains("jdk-17");
        assertThat(createConfig("Mandrel").builderImage().getEffectiveImage()).contains("ubi-quarkus-mandrel-builder-image")
                .contains("jdk-17");
        assertThat(createConfig("MANDREL").builderImage().getEffectiveImage()).contains("ubi-quarkus-mandrel-builder-image")
                .contains("jdk-17");

        assertThat(createConfig("aRandomString").builderImage().getEffectiveImage()).isEqualTo("aRandomString");
    }

    private NativeConfig createConfig(String builderImage) {
        return new TestNativeConfig(builderImage);
    }
}
