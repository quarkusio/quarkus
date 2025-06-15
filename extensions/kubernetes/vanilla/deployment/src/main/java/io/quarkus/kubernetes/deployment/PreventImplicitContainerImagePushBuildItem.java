package io.quarkus.kubernetes.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that is used to prevent the Kubernetes processing from requesting a container image push request. This
 * is useful for cases where the kubernetes cluster is local and the container image is built directly into a context
 * (i.e. a docker daemon) which the cluster has access to.
 */
public final class PreventImplicitContainerImagePushBuildItem extends MultiBuildItem {

}
