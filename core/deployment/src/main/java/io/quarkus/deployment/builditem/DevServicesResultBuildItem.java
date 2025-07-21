package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.SupplierMap;

/**
 * BuildItem for discovered (running) or to be started dev services.
 * <p>
 * Processors are expected to return this build item not only when the dev service first starts,
 * but also if a running dev service already exists.
 * <p>
 * Two builders are provided to create this build item:
 * <p>
 * - {@link DevServicesResultBuildItem#discovered()} for discovered dev services, provides config to be injected to the
 * application with container id (if it exists).
 * <p>
 * - {@link DevServicesResultBuildItem#owned()} for owned dev services, that will be started before application start,
 * provides the startable supplier and config injected to the application and post-start action.
 * <p>
 * {@link RunningDevService} is deprecated in favor of builder flavors.
 */
public final class DevServicesResultBuildItem extends MultiBuildItem {

    /**
     * The name of the dev service, usually feature name.
     */
    private final String name;

    /**
     * A description of the dev service, usually feature name with additional information.
     */
    private final String description;

    /**
     * The container id of the dev service, if it is running in a container.
     * If the dev service is not running in a container, this will be null.
     */
    private final String containerId;

    /**
     * The map of static application config
     */
    private final Map<String, String> config;

    /**
     * If the feature provides multiple dev services, this is the name of the service
     */
    private final String serviceName;

    /**
     * The config object that is used to identify the dev service
     */
    private final Object serviceConfig;

    /**
     * Supplier of a startable dev service
     */
    private final Supplier<Startable> startableSupplier;

    /**
     * An action to perform after the dev service has started.
     */
    private final Consumer<Startable> postStartAction;

    /**
     * A map of application config that is dependent on the started service
     */
    private final Map<String, Function<Startable, String>> applicationConfigProvider;

    public static DiscoveredServiceBuilder discovered() {
        return new DiscoveredServiceBuilder();
    }

    public static <T extends Startable> OwnedServiceBuilder<T> owned() {
        return new OwnedServiceBuilder<>();
    }

    /**
     * @deprecated use DevServicesResultBuildItem.builder() instead
     */
    @Deprecated
    public DevServicesResultBuildItem(String name, String containerId, Map<String, String> config) {
        this(name, null, containerId, config);
    }

    /**
     * @deprecated use DevServicesResultBuildItem.builder() instead
     */
    @Deprecated
    public DevServicesResultBuildItem(String name, String description, String containerId, Map<String, String> config) {
        this.name = name;
        this.description = description;
        this.containerId = containerId;
        this.config = config;
        this.serviceName = null;
        this.serviceConfig = null;
        this.applicationConfigProvider = null;
        this.startableSupplier = null;
        this.postStartAction = null;
    }

    /**
     * @deprecated use DevServicesResultBuildItem.builder() instead
     */
    @Deprecated
    public DevServicesResultBuildItem(String name,
            String description,
            String serviceName,
            Object serviceConfig,
            Map<String, String> config,
            Supplier<Startable> startableSupplier,
            Consumer<Startable> postStartAction,
            Map<String, Function<Startable, String>> applicationConfigProvider) {
        this.name = name;
        this.description = description;
        this.containerId = null;
        this.config = config == null ? Collections.emptyMap() : Collections.unmodifiableMap(config);
        this.serviceName = serviceName;
        this.serviceConfig = serviceConfig;
        this.startableSupplier = startableSupplier;
        this.postStartAction = postStartAction;
        this.applicationConfigProvider = applicationConfigProvider;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContainerId() {
        return containerId;
    }

    public boolean isStartable() {
        return startableSupplier != null;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Object getServiceConfig() {
        return serviceConfig;
    }

    public Supplier<Startable> getStartableSupplier() {
        return startableSupplier;
    }

    public Consumer<Startable> getPostStartAction() {
        return postStartAction;
    }

    public Map<String, Function<Startable, String>> getApplicationConfigProvider() {
        return applicationConfigProvider;
    }

    public Map<String, String> getConfig(Startable startable) {
        SupplierMap<String, String> map = new SupplierMap<>();
        if (config != null && !config.isEmpty()) {
            map.putAll(config);
        }
        for (Map.Entry<String, Function<Startable, String>> entry : applicationConfigProvider.entrySet()) {
            map.put(entry.getKey(), () -> entry.getValue().apply(startable));
        }
        return map;
    }

    public static class DiscoveredServiceBuilder {
        private String name;
        private String containerId;
        private Map<String, String> config;
        private String description;

        public DiscoveredServiceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DiscoveredServiceBuilder feature(Feature feature) {
            this.name = feature.getName();
            return this;
        }

        public DiscoveredServiceBuilder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public DiscoveredServiceBuilder config(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public DiscoveredServiceBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DevServicesResultBuildItem build() {
            return new DevServicesResultBuildItem(name, description, containerId, config);
        }
    }

    public static class OwnedServiceBuilder<T extends Startable> {
        private String name;
        private String description;
        private Map<String, String> config;
        private String serviceName;
        private Object serviceConfig;
        private Supplier<? extends Startable> startableSupplier;
        private Consumer<? extends Startable> postStartAction;
        private Map<String, Function<Startable, String>> applicationConfigProvider;

        public OwnedServiceBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        public OwnedServiceBuilder<T> feature(Feature feature) {
            this.name = feature.getName();
            return this;
        }

        public OwnedServiceBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        public OwnedServiceBuilder<T> config(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public OwnedServiceBuilder<T> serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public OwnedServiceBuilder<T> serviceConfig(Object serviceConfig) {
            this.serviceConfig = serviceConfig;
            return this;
        }

        public <S extends Startable> OwnedServiceBuilder<S> startable(Supplier<S> startableSupplier) {
            this.startableSupplier = startableSupplier;
            return (OwnedServiceBuilder<S>) this;
        }

        public OwnedServiceBuilder<T> postStartHook(Consumer<T> postStartAction) {
            this.postStartAction = postStartAction;
            return this;
        }

        public OwnedServiceBuilder<T> configProvider(Map<String, Function<T, String>> applicationConfigProvider) {
            this.applicationConfigProvider = (Map<String, Function<Startable, String>>) (Map) applicationConfigProvider;
            return this;
        }

        public DevServicesResultBuildItem build() {
            return new DevServicesResultBuildItem(name, description, serviceName, serviceConfig, config,
                    (Supplier<Startable>) startableSupplier,
                    (Consumer<Startable>) postStartAction,
                    applicationConfigProvider);
        }
    }

    /**
     * @deprecated Use {@link DevServicesResultBuildItem#discovered()} instead.
     */
    @Deprecated
    public static class RunningDevService implements Closeable {

        protected final String name;
        protected final String description;
        protected final String containerId;
        protected final Map<String, String> config;
        protected final Closeable closeable;
        protected volatile boolean isRunning = true;

        private static Map<String, String> mapOf(String key, String value) {
            Map<String, String> map = new HashMap<>();
            map.put(key, value);
            return map;
        }

        public RunningDevService(String name, String containerId, Closeable closeable, String key,
                String value) {
            this(name, null, containerId, closeable, mapOf(key, value));
        }

        public RunningDevService(String name, String description, String containerId, Closeable closeable, String key,
                String value) {
            this(name, description, containerId, closeable, mapOf(key, value));
        }

        public RunningDevService(String name, String containerId, Closeable closeable,
                Map<String, String> config) {
            this(name, null, containerId, closeable, config);
        }

        public RunningDevService(String name, String description, String containerId, Closeable closeable,
                Map<String, String> config) {
            this.name = name;
            this.description = description;
            this.containerId = containerId;
            this.closeable = closeable;
            this.config = Collections.unmodifiableMap(config);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getContainerId() {
            return containerId;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public Closeable getCloseable() {
            return closeable;
        }

        // This method should be on RunningDevService, but not on RunnableDevService, where we use different logic to
        // decide when it's time to close a container. For now, leave it where it is and hope it doesn't get called when it shouldn't.
        // We can either make a common parent class or throw unsupported when this is called from Runnable.
        public boolean isOwner() {
            return closeable != null;
        }

        @Override
        public void close() throws IOException {
            if (this.closeable != null) {
                this.closeable.close();
                isRunning = false;
            }
        }

        public DevServicesResultBuildItem toBuildItem() {
            return DevServicesResultBuildItem.discovered()
                    .name(name)
                    .description(description)
                    .containerId(getContainerId())
                    .config(getConfig())
                    .build();
        }
    }

}
