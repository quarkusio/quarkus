package io.quarkus.observability.devresource;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;

/**
 * Compatible with {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager}
 * so that classes can implement both interfaces at the same time.
 */
public interface DevResourceLifecycleManager<T extends ContainerConfig> {

    // Put order constants here -- order by dependency

    int METRICS = 5000;
    int SCRAPER = 7500;
    int GRAFANA = 10000;
    int JAEGER = 20000;
    int OTEL = 20000;

    //----

    /**
     * Get resource's config from main observability configuration.
     *
     * @param configuration main observability configuration
     * @return module's config
     */
    T config(ModulesConfiguration configuration);

    /**
     * Should we enable / start this dev resource.
     * e.g. we could already have actual service running
     * Each impl should provide its own reason on why it disabled dev service.
     *
     * @return true if ok to start new dev service, false otherwise
     */
    default boolean enable() {
        return true;
    }

    /**
     * Create container from config.
     *
     * @param config the config
     * @return container id
     */
    default GenericContainer<?> container(T config) {
        throw new IllegalStateException("Should be implemented!");
    }

    /**
     * Create container from config.
     *
     * @param config the config
     * @param root the all modules config
     * @return container id
     */
    default GenericContainer<?> container(T config, ModulesConfiguration root) {
        return container(config);
    }

    /**
     * Deduct current config from params.
     *
     * @return A map of system properties that should be set for the running dev-mode app
     */
    Map<String, String> config(int privatePort, String host, int publicPort);

    /**
     * Start the dev resource.
     *
     * @return A map of system properties that should be set for the running dev-mode app
     */
    Map<String, String> start();

    /**
     * Stop the dev resource.
     */
    void stop();

    /**
     * Called even before {@link #start()} so that the implementation can prepare itself
     * to be used as dev resource (as opposed to test resource which uses a different
     * init() method).
     */
    default void initDev() {
    }

    /**
     * If multiple dev resources are located,
     * this control the order of which they will be executed.
     *
     * @return The order to be executed. The larger the number, the later the resource is invoked.
     */
    default int order() {
        return 0;
    }
}
