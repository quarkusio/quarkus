package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

class DeploymentPlannerTest {

    private final DeploymentPlanner planner = new DeploymentPlanner();

    @Test
    void derivesDeployToTaskNameAndUsesNormalImagePushByDefault() {
        var plan = planner.plan(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                QuarkusApplicationDeploymentDescriptor.of("dev", QuarkusApplicationDeploymentTarget.KUBERNETES));

        assertThat(plan.taskName()).isEqualTo("quarkusAppDeployToDev");
        assertThat(plan.imageSource()).isEqualTo(QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH);
        assertThat(plan.imageReference()).isEmpty();
        assertThat(plan.deployment().target().quarkusDeployTarget()).isEqualTo("kubernetes");
    }

    @Test
    void explicitStartupOptimizedImageSourceIsModeled() {
        var plan = planner.plan(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                new QuarkusApplicationDeploymentDescriptor(
                        "prod",
                        QuarkusApplicationDeploymentTarget.OPENSHIFT,
                        QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH,
                        Optional.empty()));

        assertThat(plan.taskName()).isEqualTo("quarkusAppDeployToProd");
        assertThat(plan.imageSource()).isEqualTo(QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH);
        assertThat(plan.deployment().target().quarkusDeployTarget()).isEqualTo("openshift");
    }

    @Test
    void existingImageSourceRequiresImageReference() {
        assertThatThrownBy(() -> new QuarkusApplicationDeploymentDescriptor(
                "prod",
                QuarkusApplicationDeploymentTarget.KUBERNETES,
                QuarkusApplicationDeploymentImageSource.EXISTING_IMAGE,
                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image reference");
    }

    @Test
    void allDeploymentTargetsMapToQuarkusTargetNames() {
        assertThat(QuarkusApplicationDeploymentTarget.KUBERNETES.quarkusDeployTarget()).isEqualTo("kubernetes");
        assertThat(QuarkusApplicationDeploymentTarget.OPENSHIFT.quarkusDeployTarget()).isEqualTo("openshift");
        assertThat(QuarkusApplicationDeploymentTarget.KNATIVE.quarkusDeployTarget()).isEqualTo("knative");
        assertThat(QuarkusApplicationDeploymentTarget.KIND.quarkusDeployTarget()).isEqualTo("kind");
        assertThat(QuarkusApplicationDeploymentTarget.MINIKUBE.quarkusDeployTarget()).isEqualTo("minikube");
    }
}
