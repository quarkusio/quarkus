package io.quarkus.gradle.application.internal.deployment;

import java.util.Map;

import org.gradle.api.GradleException;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

public final class DeploymentConfigValidator {

    public void validate(String buildName, String deploymentName, QuarkusApplicationDeploymentTarget target,
            QuarkusApplicationDeploymentImageSource imageSource, String imageReference, Map<String, String> config) {
        String targetName = target.quarkusDeployTarget();
        failIfDifferent(buildName, deploymentName, targetName, "quarkus.deploy.target", config);
        failIfDifferent(buildName, deploymentName, targetName, "quarkus.kubernetes.deployment-target", config);
        failIfDifferent(buildName, deploymentName, imageReference, "quarkus.container-image.image", config);
        failIfFalse(buildName, deploymentName, targetName, "quarkus." + targetName + ".deploy", config);
        if (imageSource == QuarkusApplicationDeploymentImageSource.EXISTING_IMAGE) {
            failIfTrue(buildName, deploymentName, "false", "quarkus.container-image.build", config);
            failIfTrue(buildName, deploymentName, "false", "quarkus.container-image.push", config);
        }
    }

    private static void failIfDifferent(String buildName, String deploymentName, String expected, String property,
            Map<String, String> config) {
        String actual = config.get(property);
        if (actual != null && !actual.equals(expected)) {
            fail(buildName, deploymentName, expected, property, actual);
        }
    }

    private static void failIfFalse(String buildName, String deploymentName, String expected, String property,
            Map<String, String> config) {
        String actual = config.get(property);
        if ("false".equals(actual)) {
            fail(buildName, deploymentName, expected, property, actual);
        }
    }

    private static void failIfTrue(String buildName, String deploymentName, String expected, String property,
            Map<String, String> config) {
        String actual = config.get(property);
        if ("true".equals(actual)) {
            fail(buildName, deploymentName, expected, property, actual);
        }
    }

    private static void fail(String buildName, String deploymentName, String expected, String property, String actual) {
        throw new GradleException("Quarkus application deployment '" + buildName + "/" + deploymentName
                + "' owns value '" + expected + "', but user configuration sets " + property + "=" + actual);
    }
}
