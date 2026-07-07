package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

class PackageOutputNameTest {

    @Test
    void assemblesArchiveStyleName() {
        assertThat(PackageOutputName.assemble("app", "-cli", "1.0")).isEqualTo("app-cli-1.0");
    }

    @Test
    void omitsBlankSuffixAndVersion() {
        assertThat(PackageOutputName.assemble("app", "", "")).isEqualTo("app");
    }

    @Test
    void rejectsBlankBaseName() {
        assertThatThrownBy(() -> PackageOutputName.assemble(" ", "", "1.0"))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("archiveBaseName must not be blank");
    }

    @Test
    void rejectsUnspecifiedVersionInAssembledConvention() {
        assertThatThrownBy(() -> PackageOutputName.assemble("app", "", "unspecified"))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("project.version is unspecified");
    }
}
