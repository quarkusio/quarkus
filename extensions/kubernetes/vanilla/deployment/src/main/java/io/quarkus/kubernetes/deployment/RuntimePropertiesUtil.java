package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.dekorate.kubernetes.config.Port;

/**
 * Utility to handling runtime properties.
 */
public final class RuntimePropertiesUtil {

    private static final Logger LOG = Logger.getLogger(KubernetesDeployer.class);

    private static final String CONTAINER_PORT = "the container port";
    /**
     * List of ports that are linked to a runtime property.
     */
    private static final Map<String, RuntimeProperty> PORTS = Map.of(
            "http", new RuntimeProperty<>("quarkus.http.port", Integer.class, 8080, CONTAINER_PORT),
            "https", new RuntimeProperty<>("quarkus.http.ssl-port", Integer.class, 8443, CONTAINER_PORT),
            "management", new RuntimeProperty<>("quarkus.management.port", Integer.class, 9000, CONTAINER_PORT));

    private RuntimePropertiesUtil() {

    }

    /**
     * This method will return the port number that is configured from the runtime property. If the runtime property does not
     * return any value, it will return the default value that is defined in the {@link RuntimePropertiesUtil#PORTS} map.
     */
    public static Integer getPortNumberFromRuntime(String name) {
        RuntimeProperty runtimeProperty = PORTS.get(name);
        if (runtimeProperty != null) {
            return (Integer) runtimeProperty.getValue().orElse(runtimeProperty.defaultValue);
        }

        return null;
    }

    /**
     * This method will trace an informative message to let users know that runtime properties that are used in the generated
     * resources by Kubernetes can't change again at runtime.
     *
     * For example, for users that set a runtime property "quarkus.http.port=9000" at build time, Kubernetes will use this value
     * in the generated resources. Then, when running the application in Kubernetes, if users try to modify again the
     * runtime property "quarkus.http.port" to a different value, this won't work because the generated resources already took
     * the 9000 value.
     *
     * Note that this message won't be printed if the users didn't provide the runtime property at build time.
     */
    public static void printTraceIfRuntimePropertyIsSet(String target, Port port) {
        RuntimeProperty runtimeProperty = PORTS.get(port.getName());
        if (runtimeProperty != null) {
            var runtimePropertyValue = runtimeProperty.getValue();
            if (runtimePropertyValue.isPresent()) {
                LOG.info(String.format("The '%s' manifests are generated with %s '%s' having value '%d'. "
                        + "The app and manifests will get out of sync if the property '%s' is changed at runtime.",
                        target, runtimeProperty.usage, port.getName(), port.getContainerPort(), runtimeProperty.propertyName));
            }
        }
    }

    private static class RuntimeProperty<T> {
        private final String propertyName;
        private final Class<T> clazzOfValue;
        private final T defaultValue;
        private final String usage;

        public RuntimeProperty(String propertyName, Class<T> clazzOfValue, T defaultValue, String usage) {
            this.propertyName = propertyName;
            this.clazzOfValue = clazzOfValue;
            this.defaultValue = defaultValue;
            this.usage = usage;
        }

        public Optional<T> getValue() {
            return ConfigProvider.getConfig().getOptionalValue(propertyName, clazzOfValue);
        }
    }
}
