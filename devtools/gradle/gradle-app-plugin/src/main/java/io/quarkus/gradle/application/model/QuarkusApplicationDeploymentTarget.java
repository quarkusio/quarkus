package io.quarkus.gradle.application.model;

/**
 * Deployment providers supported by named application deployments.
 */
public enum QuarkusApplicationDeploymentTarget {
    /**
     * Kubernetes.
     */
    KUBERNETES("kubernetes"),
    /**
     * Red Hat OpenShift.
     */
    OPENSHIFT("openshift"),
    /**
     * Knative.
     */
    KNATIVE("knative"),
    /**
     * A local kind cluster.
     */
    KIND("kind"),
    /**
     * A local Minikube cluster.
     */
    MINIKUBE("minikube");

    private final String quarkusDeployTarget;

    QuarkusApplicationDeploymentTarget(String quarkusDeployTarget) {
        this.quarkusDeployTarget = quarkusDeployTarget;
    }

    /**
     * Returns the Quarkus deployment-target property value.
     *
     * @return the value passed to the Quarkus deployment operation
     */
    public String quarkusDeployTarget() {
        return quarkusDeployTarget;
    }
}
