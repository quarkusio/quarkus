package io.quarkus.gradle.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.GradleException;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

class GradleVersionEnforcerTest {

    @Test
    void shouldFailForGradleOlderThanMinimumVersion() {
        assertThatThrownBy(() -> GradleVersionEnforcer.verifyGradleVersion(GradleVersion.version("6.0"), null))
                .isInstanceOf(GradleException.class)
                .hasMessage("Quarkus plugin requires Gradle 6.1 or later. Current version is: Gradle 6.0");
    }

    @Test
    void shouldWarnForGradleVersionsOlderThanTestedVersionAndQuarkus4MinimumVersion() {
        assertThat(GradleVersionEnforcer.warningsFor(GradleVersion.version("7.6.4")))
                .containsExactly(
                        "This version of Quarkus is tested with Gradle 8.14 or later. This build is using Gradle 7.6.4. Please upgrade the Gradle wrapper.",
                        "Quarkus 4 will require Gradle 9.6 or later. This build is using Gradle 7.6.4. Please upgrade the Gradle wrapper before upgrading to Quarkus 4.");
    }

    @Test
    void shouldWarnForGradleVersionsOlderThanTestedVersion() {
        assertThat(GradleVersionEnforcer.warningsFor(GradleVersion.version("8.13")))
                .containsExactly(
                        "This version of Quarkus is tested with Gradle 8.14 or later. This build is using Gradle 8.13. Please upgrade the Gradle wrapper.",
                        "Quarkus 4 will require Gradle 9.6 or later. This build is using Gradle 8.13. Please upgrade the Gradle wrapper before upgrading to Quarkus 4.");
    }

    @Test
    void shouldWarnForGradleVersionsOlderThanQuarkus4MinimumVersion() {
        assertThat(GradleVersionEnforcer.warningsFor(GradleVersion.version("8.14")))
                .containsExactly(
                        "Quarkus 4 will require Gradle 9.6 or later. This build is using Gradle 8.14. Please upgrade the Gradle wrapper before upgrading to Quarkus 4.");
    }

    @Test
    void shouldNotWarnForGradleVersionsSupportedByQuarkus4() {
        assertThat(GradleVersionEnforcer.warningsFor(GradleVersion.version("9.6"))).isEmpty();
        assertThat(GradleVersionEnforcer.warningsFor(GradleVersion.version("10.0"))).isEmpty();
    }
}
