package io.quarkus.gradle.application.internal.tooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;

class GradleApplicationModelSidecarBuilderTest {

    private final GradleApplicationModelSidecarBuilder builder = new GradleApplicationModelSidecarBuilder(
            mock(ApplicationModelResolutionViews.class),
            (project, mode) -> {
                throw new AssertionError("Invalid requests must fail before application-model assembly");
            });

    @Test
    void advertisesOnlyTheSidecarModelAndTheSharedParameter() {
        assertThat(builder.canBuild(GradleApplicationModelSidecar.class.getName())).isTrue();
        assertThat(builder.canBuild("unrelated.Model")).isFalse();
        assertThat(builder.getParameterType()).isEqualTo(ModelParameter.class);
    }

    @Test
    void rejectsAnUnexpectedModelNameBeforeAssembly() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.buildAll("unrelated.Model", mock(ModelParameter.class), mock()))
                .withMessage("Unsupported tooling model unrelated.Model");
    }

    @Test
    void rejectsAnUnsupportedModeBeforeAssembly() {
        ModelParameterImpl parameter = new ModelParameterImpl();
        parameter.setMode("UNKNOWN");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.buildAll(GradleApplicationModelSidecar.class.getName(), parameter, mock()))
                .withMessage("Unsupported Quarkus launch mode UNKNOWN");
    }
}
