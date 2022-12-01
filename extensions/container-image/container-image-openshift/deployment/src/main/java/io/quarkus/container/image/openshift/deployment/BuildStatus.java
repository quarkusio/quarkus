
package io.quarkus.container.image.openshift.deployment;

public enum BuildStatus {

    New,
    Pending,
    Running,
    Complete,
    Failed,
    Error,
    Cancelled;

}
