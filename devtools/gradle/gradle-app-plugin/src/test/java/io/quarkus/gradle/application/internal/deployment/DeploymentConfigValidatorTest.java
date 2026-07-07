package io.quarkus.gradle.application.internal.deployment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

class DeploymentConfigValidatorTest {

    private final DeploymentConfigValidator validator = new DeploymentConfigValidator();

    @Test
    void matchingAndAbsentValuesPass() {
        validator.validate("app", "prod", QuarkusApplicationDeploymentTarget.OPENSHIFT,
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH, "registry/app:1",
                Map.of("quarkus.deploy.target", "openshift"));
        validator.validate("app", "prod", QuarkusApplicationDeploymentTarget.OPENSHIFT,
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH, "registry/app:1", Map.of());
    }

    @Test
    void conflictingDeploymentTargetFails() {
        assertThatThrownBy(() -> validator.validate("app", "prod", QuarkusApplicationDeploymentTarget.OPENSHIFT,
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH, "registry/app:1",
                Map.of("quarkus.kubernetes.deployment-target", "kubernetes")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("app/prod")
                .hasMessageContaining("quarkus.kubernetes.deployment-target=kubernetes");
    }

    @Test
    void existingImageCannotRequestImageBuildOrPush() {
        assertThatThrownBy(() -> validator.validate("app", "prod", QuarkusApplicationDeploymentTarget.KUBERNETES,
                QuarkusApplicationDeploymentImageSource.EXISTING_IMAGE, "registry/app:1",
                Map.of("quarkus.container-image.build", "true")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("quarkus.container-image.build=true");
    }
}
