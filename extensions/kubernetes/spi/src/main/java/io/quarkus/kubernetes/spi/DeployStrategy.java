package io.quarkus.kubernetes.spi;

public enum DeployStrategy {
    /**
     * To create or replace the resources.
     */
    CreateOrUpdate,
    /**
     * To create the resources if it does not exist. If resources already exist, it will fail.
     */
    Create,
    /**
     * To update the existing resources in the target Kubernetes cluster. If no resources exist, it will fail.
     */
    Replace,
    /**
     * To perform patch updates to the existing resources. If no resources exist, it will fail. More information in
     * <a href="https://kubernetes.io/docs/reference/using-api/server-side-apply/">server-side-apply</a>.
     */
    ServerSideApply;
}
