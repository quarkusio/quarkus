
package io.quarkus.kubernetes.deployment;

public enum DeploymentTarget {

    KUBERNETES("Deployment"),
    OPENSHIFT("DeploymentConfig"),
    KNATIVE("Service");

    private String kind;

    DeploymentTarget(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return this.kind;
    }
}
