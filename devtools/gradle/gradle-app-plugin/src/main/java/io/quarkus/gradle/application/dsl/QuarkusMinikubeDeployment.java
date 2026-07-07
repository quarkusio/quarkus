package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * A named deployment whose fixed target is Minikube.
 */
public abstract class QuarkusMinikubeDeployment extends QuarkusApplicationDeployment {

    /**
     * Creates the Gradle-managed deployment element.
     *
     * @param name the deployment name
     */
    @Inject
    public QuarkusMinikubeDeployment(String name) {
        super(name, QuarkusApplicationDeploymentTarget.MINIKUBE);
    }
}
