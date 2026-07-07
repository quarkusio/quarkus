package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * A named deployment whose fixed target is Kubernetes.
 */
public abstract class QuarkusKubernetesDeployment extends QuarkusApplicationDeployment {

    /**
     * Creates the Gradle-managed deployment element.
     *
     * @param name the deployment name
     */
    @Inject
    public QuarkusKubernetesDeployment(String name) {
        super(name, QuarkusApplicationDeploymentTarget.KUBERNETES);
    }
}
