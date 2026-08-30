package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * A named deployment whose fixed target is OpenShift.
 */
public abstract class QuarkusOpenShiftDeployment extends QuarkusApplicationDeployment {

    /**
     * Creates the Gradle-managed deployment element.
     *
     * @param name the deployment name
     */
    @Inject
    public QuarkusOpenShiftDeployment(String name) {
        super(name, QuarkusApplicationDeploymentTarget.OPENSHIFT);
    }
}
