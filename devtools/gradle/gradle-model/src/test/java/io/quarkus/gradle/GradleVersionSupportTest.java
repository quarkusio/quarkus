package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.GradleException;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

public class GradleVersionSupportTest {

    @Test
    public void shouldExposeSupportedGradleVersions() {
        assertThat(GradleVersionSupport.MINIMUM_GRADLE_VERSION).isEqualTo("9.6");
        assertThat(GradleVersionSupport.SUPPORTED_GRADLE_VERSIONS).isEqualTo("[9.6,)");
    }

    @Test
    public void shouldAcceptMinimumGradleVersion() {
        GradleVersionSupport.requireMinimumGradleVersion(GradleVersion.version("9.6"));
    }

    @Test
    public void shouldAcceptLaterGradleVersion() {
        GradleVersionSupport.requireMinimumGradleVersion(GradleVersion.version("10.0"));
    }

    @Test
    public void shouldRejectOlderGradleVersion() {
        assertThatThrownBy(() -> GradleVersionSupport.requireMinimumGradleVersion(GradleVersion.version("9.5.1")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Quarkus Gradle plugins require Gradle 9.6 or later")
                .hasMessageContaining("Current version is: Gradle 9.5.1");
    }
}
