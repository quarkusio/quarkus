package io.quarkus.infinispan.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class InfinispanDevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a database when running in Dev or Test mode and when Docker is running.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * Indicates if the Infinispan server managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Infinispan starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-infinispan} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-infinispan} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Infinispan looks for a container with the
     * {@code quarkus-dev-service-infinispan} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-infinispan} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Infinispan servers.
     */
    @ConfigItem(defaultValue = InfinispanClientUtil.DEFAULT_INFINISPAN_DEV_SERVICE_NAME)
    public String serviceName;

    /**
     * The image to use.
     * Note that only official Infinispan images are supported.
     */
    @ConfigItem
    public Optional<String> imageName = Optional.empty();

    /**
     * List of the artifacts to automatically download and add to the Infinispan server libraries.
     * <p>
     * For example a Maven coordinate
     * (org.postgresql:postgresql:42.3.1) or a dependency location url.
     * <p>
     * If an invalid value is passed, the Infinispan server will throw an error when trying to start.
     */
    @ConfigItem
    public Optional<List<String>> artifacts;

    /**
     * Add a site name to start the Infinispan Server Container with Cross Site Replication enabled (ex. lon).
     * Cross Site Replication is the capability to connect two separate Infinispan Server Clusters that might run
     * in different Data Centers, and configure backup caches to copy the data across the clusters with active-active
     * or active-passive replication.
     * See more about Cross Site Replication in the Infinispan Documentation
     * https://infinispan.org/docs/stable/titles/xsite/xsite.html
     * Configure {@link #mcastPort} to avoid forming a cluster with any other running Infinispan Server container.
     */
    @ConfigItem
    public Optional<String> site;

    /**
     * If you are running an Infinispan Server already in docker, if the containers use the same mcastPort they will form a
     * cluster.
     * Set a different mcastPort to create a separate cluster in Docker (e. 46656).
     * A common use case in a local Docker development mode, is the need of having two different Infinispan Clusters
     * with Cross Site Replication enabled.
     * see
     * https://github.com/infinispan/infinispan-simple-tutorials/blob/main/infinispan-remote/cross-site-replication/docker-compose/
     */
    @ConfigItem
    public OptionalInt mcastPort;

    /**
     * Runs the Infinispan Server container with tracing enabled. Traces are disabled by default
     */
    @ConfigItem(name = "tracing.enabled", defaultValue = "false")
    public Optional<Boolean> tracing;

    /**
     * Sets Infinispan Server otlp endpoint. Default value is http://localhost:4317
     */
    @ConfigItem(name = "tracing.exporter.otlp.endpoint", defaultValue = "http://localhost:4317")
    public Optional<String> exporterOtlpEndpoint;

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigItem
    public Map<String, String> containerEnv;

    /**
     * Infinispan Server configuration chunks to be passed to the container.
     */
    @ConfigItem
    public Optional<List<String>> configFiles;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InfinispanDevServicesConfig that = (InfinispanDevServicesConfig) o;
        return enabled == that.enabled &&
                Objects.equals(port, that.port) &&
                Objects.equals(shared, that.shared) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(imageName, that.imageName) &&
                Objects.equals(artifacts, this.artifacts) &&
                Objects.equals(containerEnv, that.containerEnv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, port, shared, serviceName, imageName, artifacts, containerEnv);
    }

    @Override
    public String toString() {
        return "InfinispanDevServicesConfig{" + "enabled=" + enabled + ", port=" + port + ", shared=" + shared
                + ", serviceName='" + serviceName + '\'' + ", imageName=" + imageName + ", artifacts=" + artifacts
                + ", site=" + site + ", mcastPort=" + mcastPort + ", tracing=" + tracing + ", exporterOtlpEndpoint="
                + exporterOtlpEndpoint + '}';
    }
}
