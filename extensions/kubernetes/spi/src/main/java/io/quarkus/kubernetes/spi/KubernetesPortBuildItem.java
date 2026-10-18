package io.quarkus.kubernetes.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;

public final class KubernetesPortBuildItem extends MultiBuildItem {

    private final int port;
    private final String name;
    /**
     * Indicates when the port is enabled vs simply configured.
     * For example the presence `quarkus.http.ssl-port` also is not enought to tell us if enabled.
     * Still, we need to communicate its value and let `quarkus-kubernetes` extension decide.
     **/
    private final boolean enabled;
    private final Optional<Property<Integer>> source;

    public KubernetesPortBuildItem(int port, Feature feature) {
        this(port, feature.getName(), true, Optional.empty());
    }

    public KubernetesPortBuildItem(int port, String name) {
        this(port, name, true, Optional.empty());
    }

    public KubernetesPortBuildItem(int port, String name, boolean enabled, Optional<Property<Integer>> source) {
        this.port = port;
        this.name = name;
        this.source = source;
        this.enabled = enabled;
    }

    public static KubernetesPortBuildItem fromRuntimeConfiguration(String name, String propertyName, Integer defaultValue,
            boolean enabled) {
        Property<Integer> origin = Property.fromRuntimeConfiguration(propertyName, Integer.class, defaultValue);
        Integer port = origin.getValue().orElse(defaultValue);
        return new KubernetesPortBuildItem(port, name, enabled, Optional.of(origin));
    }

    public static KubernetesPortBuildItem fromRuntimeConfiguration(String name, String propertyName, Integer defaultValue) {
        Property<Integer> origin = Property.fromRuntimeConfiguration(propertyName, Integer.class, defaultValue);
        Integer port = origin.getValue().orElse(defaultValue);
        return new KubernetesPortBuildItem(port, name, origin.getValue().isPresent(), Optional.of(origin));
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<Property<Integer>> getSource() {
        return source;
    }
}
