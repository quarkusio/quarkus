package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * A named deployment whose fixed target is Knative.
 */
public abstract class QuarkusKnativeDeployment extends QuarkusApplicationDeployment {

    /**
     * Creates the Gradle-managed deployment element.
     *
     * @param name the deployment name
     */
    @Inject
    public QuarkusKnativeDeployment(String name) {
        super(name, QuarkusApplicationDeploymentTarget.KNATIVE);
    }
}
