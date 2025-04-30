package io.quarkus.kubernetes.client.runtime.internal;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface KubernetesDevServicesBuildTimeConfig {

    /**
     * If Dev Services for Kubernetes should be used. (default to true)
     *
     * If this is true and kubernetes client is not configured then a kubernetes cluster
     * will be started and will be used.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The kubernetes api server version to use.
     *
     * If not set, Dev Services for Kubernetes will use the latest supported version of the given flavor.
     * see https://github.com/dajudge/kindcontainer/blob/master/k8s-versions.json
     */
    Optional<String> apiVersion();

    /**
     * The flavor to use (kind, k3s or api-only).
     *
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
         * kind (needs priviledge docker)
         */
        kind,
        /**
         * k3s (needs priviledge docker)
         */
        k3s,
        /**
         * api only
         */
        api_only;
    }
}
