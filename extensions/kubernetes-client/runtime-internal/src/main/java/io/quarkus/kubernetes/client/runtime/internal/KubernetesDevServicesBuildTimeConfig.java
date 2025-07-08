package io.quarkus.kubernetes.client.runtime.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface KubernetesDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Kubernetes should be used. (default to true)
     * <p>
     * If this is true and kubernetes client is not configured then a kubernetes cluster
     * will be started and will be used.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The kubernetes api server version to use.
     * <p>
     * If not set, Dev Services for Kubernetes will use the
     * <a href="https://github.com/dajudge/kindcontainer/blob/master/k8s-versions.json">latest supported version</a> of the
     * given flavor.
     */
    Optional<String> apiVersion();

    /**
     * The kubernetes image to use.
     * <p>
     * If not set, Dev Services for Kubernetes will use default image for the specified {@link #apiVersion()} for the given
     * {@link #flavor()}.
     */

    Optional<String> imageName();

    /**
     * The flavor to use (kind, k3s or api-only).
     * <p>
     * If not set, Dev Services for Kubernetes will set it to: api-only.
     */
    Optional<Flavor> flavor();

    /**
     * By default, if a kubeconfig is found, Dev Services for Kubernetes will not start.
     * Set this to true to override the kubeconfig config.
     */
    @WithDefault("false")
    boolean overrideKubeconfig();

    /**
     * A list of manifest file paths that should be applied to the Kubernetes cluster Dev Service on startup.
     *
     * If not set, no manifests are applied.
     */
    Optional<List<String>> manifests();

    /**
     * Indicates if the Kubernetes cluster managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Kubernetes starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-kubernetes} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-kubernetes} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Kubernetes looks for a container with the
     * {@code quarkus-dev-service-kubernetes} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-kubernetes} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Kubernetes clusters.
     */
    @WithDefault("kubernetes")
    String serviceName();

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();

    enum Flavor {
        /**
         * kind (needs privileged docker)
         */
        kind,
        /**
         * k3s (needs privileged docker)
         */
        k3s,
        /**
         * api only
         */
        api_only
    }
}
