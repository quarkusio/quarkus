package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationImageDescriptor;

class ImagePlannerTest {

    private final ImagePlanner planner = new ImagePlanner();

    @Test
    void mapsBuilderEnumToQuarkusNames() {
        assertThat(QuarkusApplicationImageBuilder.JIB.quarkusBuilderName()).isEqualTo("jib");
        assertThat(QuarkusApplicationImageBuilder.DOCKER.quarkusBuilderName()).isEqualTo("docker");
        assertThat(QuarkusApplicationImageBuilder.PODMAN.quarkusBuilderName()).isEqualTo("podman");
        assertThat(QuarkusApplicationImageBuilder.OPENSHIFT.quarkusBuilderName()).isEqualTo("openshift");
        assertThat(QuarkusApplicationImageBuilder.BUILDPACK.quarkusBuilderName()).isEqualTo("buildpack");
    }

    @Test
    void effectiveReferenceIsRepositoryAndTag() {
        var image = new QuarkusApplicationImageDescriptor("quay.io/acme/app", "1.0",
                QuarkusApplicationImageBuilder.JIB);

        assertThat(image.effectiveReference()).isEqualTo("quay.io/acme/app:1.0");
    }

    @Test
    void rejectsDuplicateReferencesForUnrelatedOwners() {
        var image = new QuarkusApplicationImageDescriptor("quay.io/acme/app", "1.0",
                QuarkusApplicationImageBuilder.JIB);
        var app = QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR);
        var nativeApp = QuarkusApplicationBuildDescriptor.of("native1", QuarkusApplicationBuildType.NATIVE_EXECUTABLE);

        assertThatThrownBy(() -> planner.validateEffectiveReferences(List.of(
                new ImagePlan(app, image, false, false),
                new ImagePlan(nativeApp, image, false, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quay.io/acme/app:1.0");
    }

    @Test
    void allowsSameReferenceForExplicitOrderedOwnerFlow() {
        var image = new QuarkusApplicationImageDescriptor("quay.io/acme/app", "1.0",
                QuarkusApplicationImageBuilder.JIB);
        var app = QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR);

        planner.validateEffectiveReferences(List.of(
                new ImagePlan(app, image, false, false),
                new ImagePlan(app, image, true, true)));
    }
}
