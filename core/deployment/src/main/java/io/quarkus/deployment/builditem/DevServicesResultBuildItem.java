package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem for running dev services.
 * Combines injected configs to the application with container id (if it exists).
 *
 * Processors are expected to return this build item not only when the dev service first starts,
 * but also if a running dev service already exists.
 *
 * {@link RunningDevService} helps to manage the lifecycle of the running dev service.
 */
public final class DevServicesResultBuildItem extends MultiBuildItem {

    private final String name;
    private final String description;
    private final String containerId;
    private final Map<String, String> config;

    public DevServicesResultBuildItem(String name, String containerId, Map<String, String> config) {
        this(name, null, containerId, config);
    }

    public DevServicesResultBuildItem(String name, String description, String containerId, Map<String, String> config) {
        this.name = name;
        this.description = description;
        this.containerId = containerId;
        this.config = config;
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

    public static class RunningDevService implements Closeable {

        private final String name;
        private final String description;
        private final String containerId;
        private final Map<String, String> config;
        private final Closeable closeable;

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

        public boolean isOwner() {
            return closeable != null;
        }

        @Override
        public void close() throws IOException {
            if (this.closeable != null) {
                this.closeable.close();
            }
        }

        public DevServicesResultBuildItem toBuildItem() {
            return new DevServicesResultBuildItem(name, description, containerId, config);
        }
    }
}
