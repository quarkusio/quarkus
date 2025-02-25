package io.quarkus.observability.devresource;

import java.util.Map;

import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Extends {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager}
 * so that classes implement both interfaces at the same time - simplifying testing.
 */
public interface DevResourceLifecycleManager<T extends ContainerConfig> extends QuarkusTestResourceLifecycleManager {

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
    @Deprecated
    T config(ModulesConfiguration configuration);

    /**
     * Get resource's config from main observability configuration and extension catalog
     *
     * @param configuration main observability configuration
     * @param catalog observability catalog. If OpenTelemetry or Micrometer are enabled.
     * @return module's config
     */
    default T config(ModulesConfiguration configuration, ExtensionsCatalog catalog) {
        return config(configuration);
    }

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
    default Container<T> container(T config) {
        throw new IllegalStateException("Should be implemented!");
    }

    /**
     * Create container from config.
     *
     * @param config the config
     * @param root the all modules config
     * @return container id
     */
    default Container<T> container(T config, ModulesConfiguration root) {
        return container(config);
    }

    /**
     * Deduce current config from params.
     * If port are too dynamic / configured, it's hard to deduce,
     * since configuration is not part of the devservice state.
     * e.g. different ports then usual - Grafana UI is 3000, if you do not use 3000,
     * it's hard or impossible to know which port belongs to certain property.
     *
     * @return A map of system properties that should be set for the running dev-mode app
     */
    Map<String, String> config(int privatePort, String host, int publicPort);

    /**
     * Called even before {@link #start()} so that the implementation can prepare itself
     * to be used as dev resource (as opposed to test resource which uses a different
     * init() method).
     */
    default void initDev() {
    }
}
